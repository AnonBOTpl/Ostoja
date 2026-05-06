package com.example.trzezwadroga

import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.TimeUnit

class SobrietyLogicTest {
    @Test
    fun testDaysCalculation() {
        val now = System.currentTimeMillis()
        val twoDaysAgo = now - TimeUnit.DAYS.toMillis(2)
        val days = TimeUnit.MILLISECONDS.toDays(now - twoDaysAgo)
        assertEquals(2, days)
    }

    @Test
    fun testHealthTipLogic() {
        fun getTip(days: Long): String {
            return when {
                days >= 365 -> "Rok"
                days >= 100 -> "100 dni"
                days >= 14 -> "2 tygodnie"
                days >= 2 -> "2 dni"
                else -> "Każda godzina"
            }
        }

        assertEquals("2 dni", getTip(2))
        assertEquals("2 dni", getTip(5))
        assertEquals("2 tygodnie", getTip(14))
        assertEquals("100 dni", getTip(150))
        assertEquals("Rok", getTip(400))
        assertEquals("Każda godzina", getTip(1))
    }
}
