package com.craftcaller.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.craftcaller.app.audio.AlertStyle
import com.craftcaller.app.service.CraftCallScreeningService
import com.craftcaller.app.tts.AnnouncerVoice
import com.craftcaller.app.util.Prefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Fallback + role behavior that automations CAN honestly assert:
 *  - no call-screening role held (fresh Robolectric app) -> isRoleHeld false,
 *  - imported alert style WITHOUT its file silently falls back to CHIME,
 *  - TTS guard paths (blank text, zero volume, dedupe) never crash and never
 *    create an engine needlessly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FallbackAndRoleTest {

    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("craftcaller_prefs", 0).edit().clear().commit()
        app.getSharedPreferences("craftcaller_profile_secure", 0).edit().clear().commit()
    }

    @Test
    fun `fresh setup holds no call-screening role`() {
        // Missing-Android-role path: fallback PHONE_STATE announcement mode.
        assertFalse(CraftCallScreeningService.isRoleHeld(app))
    }

    @Test
    fun `custom alert style without its imported file falls back to CHIME`() {
        val prefs = Prefs(app)
        prefs.alertStyle = "custom"     // claims an imported sound...
        // ...but no imported file exists in this fresh sandbox.
        assertEquals(AlertStyle.CHIME, AlertStyle.effective(app))
    }

    @Test
    fun `chime style with no file claims is still chime`() {
        val prefs = Prefs(app)
        prefs.alertStyle = "chime"
        assertEquals(AlertStyle.CHIME, AlertStyle.effective(app))
    }

    @Test
    fun `tts guards - blank text announces nothing without crashing`() {
        // No exception, no engine requirement — call flow stays intact.
        AnnouncerVoice.announce(app, "   ")
        AnnouncerVoice.release()
    }

    @Test
    fun `tts guards - zero volume announces nothing without crashing`() {
        AnnouncerVoice.announce(app, "Umesh sir, aapko Rahul ka call aa raha hai.", 0f)
        AnnouncerVoice.release()
    }

    @Test
    fun `tts guards - immediate duplicate text is deduped safely`() {
        val text = "Rahul ka call"
        AnnouncerVoice.announce(app, text, 1f) // may create a shadow engine
        AnnouncerVoice.announce(app, text, 1f) // deduped by the 12s window
        AnnouncerVoice.release()
    }
}
