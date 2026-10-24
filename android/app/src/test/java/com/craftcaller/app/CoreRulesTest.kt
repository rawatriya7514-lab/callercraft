package com.craftcaller.app

import com.craftcaller.app.audio.AlertStyle
import com.craftcaller.app.service.CraftCallScreeningService
import com.craftcaller.app.util.AnnounceLang
import com.craftcaller.app.util.Craft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure constants + fallback behavior that needs no Android framework:
 *  - E.164 normalization keys (unknown-number handling),
 *  - the fixed spam threshold,
 *  - the screening decision budget (<= Android's 5s, ~300ms REAL budget),
 *  - alert-style key fallbacks (audio settings persistence contract).
 */
class CoreRulesTest {

    // ---------------- phone-number normalization (libphonenumber) -----------

    @Test
    fun `indian mobile formats normalize to the same E164 key`() {
        val expected = "919876543210"
        assertEquals(expected, Craft.normalizeNumber("+91 98765 43210"))
        assertEquals(expected, Craft.normalizeNumber("098765 43210"))
        assertEquals(expected, Craft.normalizeNumber("98765 43210"))
        assertEquals(expected, Craft.normalizeNumber("+919876543210"))
    }

    @Test
    fun `landline with area code normalizes to E164`() {
        assertEquals("918012345678", Craft.normalizeNumber("080 1234 5678"))
        assertEquals("911119059059", Craft.normalizeNumber("+91-11-19059059"))
    }

    @Test
    fun `unparseable garbage degrades to digits-only and never throws`() {
        assertEquals("123456789012", Craft.normalizeNumber("aa-123456789012-bb"))
        assertEquals("", Craft.normalizeNumber(""))
        assertEquals("", Craft.normalizeNumber("   "))
        assertEquals("42", Craft.normalizeNumber("only 42"))
    }

    @Test
    fun `different valid numbers never collide`() {
        val a = Craft.normalizeNumber("+91 98765 63210")
        val b = Craft.normalizeNumber("+91 98765 43210")
        assertTrue(a != b)
    }

    // ---------------- spam threshold is a fixed, honest constant -------------

    @Test
    fun `spam threshold stays 10 community reports`() {
        assertEquals(10, Craft.SPAM_REPORTS_THRESHOLD)
    }

    // ---------------- screening budget: no incoming-call delay ---------------

    @Test
    fun `local decision budget is far below Android's 5 second contract`() {
        assertTrue(
            "budget was ${CraftCallScreeningService.LOCAL_DECISION_BUDGET_MS}ms",
            CraftCallScreeningService.LOCAL_DECISION_BUDGET_MS <= 300L
        )
    }

    // ---------------- audio settings: style keys -----------------------------

    @Test
    fun `alert style keys roundtrip`() {
        AlertStyle.entries.forEach { style ->
            assertEquals(style, AlertStyle.fromKey(style.key))
        }
    }

    @Test
    fun `unknown persisted style falls back to chime instead of crashing`() {
        assertEquals(AlertStyle.CHIME, AlertStyle.fromKey("totally-unknown-style"))
    }

    @Test
    fun `speech-only style carries no bundled sound`() {
        assertEquals(null, AlertStyle.VOICE_ONLY.resId)
    }

    // ---------------- language keys ------------------------------------------

    @Test
    fun `announcement language roundtrips`() {
        AnnounceLang.entries.forEach { lang ->
            assertEquals(lang, AnnounceLang.fromKey(lang.key))
        }
        assertEquals(AnnounceLang.HINDI, AnnounceLang.fromKey("unknown-key"))
    }
}
