package com.safeflow.app

import com.safeflow.app.core.domain.*
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.inventory.domain.*
import com.safeflow.app.features.monitoring.domain.*
import org.junit.Assert.*
import org.junit.Test

class BusinessRulesTest {
    @Test fun stockAndReservationsBelongToEachLot() {
        val lots = listOf(Lot("a", "product", "A", 10, 3), Lot("b", "product", "B", 20, 5), Lot("c", "other", "C", 100, 10))
        assertEquals(7, lots[0].available); assertEquals(15, lots[1].available)
        val total = productTotals("product", lots)
        assertEquals(30L, total.stock); assertEquals(8L, total.reserved); assertEquals(22L, total.available)
        assertEquals(InventoryTotals(), productTotals("missing", lots))
        assertEquals(0, Lot("new", "product", "NEW").available)
    }
    @Test fun productTotalsDoNotOverflowAcrossLots() {
        val total = productTotals("product", listOf(Lot("a", "product", "A", Int.MAX_VALUE), Lot("b", "product", "B", Int.MAX_VALUE, 1)))
        assertEquals(2L * Int.MAX_VALUE, total.stock); assertEquals(2L * Int.MAX_VALUE - 1, total.available)
    }
    @Test fun requestedPasswordsDoNotRequireUppercase() {
        listOf("@operadortest1", "@supervisortest1", "@admintest1").forEach { assertTrue(IdentityRules.validPassword(it)) }
        listOf("abc", "password123", "@abcdefghi", "12345678@").forEach { assertFalse(IdentityRules.validPassword(it)) }
    }
    @Test fun validatesEmail() {
        assertTrue(IdentityRules.validEmail("operador.test1@gmail.com"))
        listOf("bad", "a@", "a b@gmail.com", "a@gmail").forEach { assertFalse(IdentityRules.validEmail(it)) }
    }
    @Test fun quantitiesAreFinitePositiveIntegers() {
        assertEquals(10, positiveQuantity("10"))
        listOf("0", "-1", "1.5", "NaN", "Infinity", "2147483648", "").forEach {
            assertThrows(BusinessException::class.java) { positiveQuantity(it) }
        }
    }
    @Test fun administrationIsNotAnAssignablePermission() {
        assertEquals(emptySet<Permission>(), IdentityRules.defaults(Role.ADMIN))
        assertTrue(Permission.WRITE_INVENTORY in IdentityRules.defaults(Role.OPERADOR))
        assertTrue(Permission.RESOLVE in IdentityRules.defaults(Role.SUPERVISOR))
        assertFalse(Permission.RESOLVE in IdentityRules.defaults(Role.OPERADOR))
    }
}
