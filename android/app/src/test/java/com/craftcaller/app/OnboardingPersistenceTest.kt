package com.craftcaller.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.craftcaller.app.util.Gender
import com.craftcaller.app.util.Honorific
import com.craftcaller.app.util.Prefs
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Onboarding persistence: every wizard field (secure profile + switches)
 * must survive the prefs INSTANCE being recreated (what happens between
 * app restarts). Also exercises SecurePrefs' graceful-degradation path on
 * a device/VM without the required Keystore.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingPersistenceTest {

    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        // start each test from a clean store
        app.getSharedPreferences("craftcaller_prefs", 0).edit().clear().commit()
        app.getSharedPreferences("craftcaller_profile_secure", 0).edit().clear().commit()
        Prefs(app).let {
            it.onboardingDone = false
            it.profileReady = false
            it.userName = ""
        }
    }

    @Test
    fun `onboarding completion flags persist across instance recreation`() {
        val first = Prefs(app)
        first.onboardingDone = true
        first.profileReady = true
        first.userName = "Umesh Kumar"
        first.gender = Gender.MALE
        first.honorific = Honorific.SIR

        val reloaded = Prefs(app)  // simulates process restart
        assertEquals(true, reloaded.onboardingDone)
        assertEquals(true, reloaded.profileReady)
        assertEquals("Umesh Kumar", reloaded.userName)
        assertEquals(Gender.MALE, reloaded.gender)
        assertEquals(Honorific.SIR, reloaded.honorific)
    }

    @Test
    fun `announcement routing switches persist`() {
        val first = Prefs(app)
        first.announceCallerEnabled = false
        first.announceSavedContacts = true
        first.announceUnknown = false
        first.announceRepeat = false
        first.announceUseContacts = false

        val reloaded = Prefs(app)
        assertEquals(false, reloaded.announceCallerEnabled)
        assertEquals(true, reloaded.announceSavedContacts)
        assertEquals(false, reloaded.announceUnknown)
        assertEquals(false, reloaded.announceRepeat)
        assertEquals(false, reloaded.announceUseContacts)
    }

    @Test
    fun `voice delivery settings persist with clamping`() {
        val first = Prefs(app)
        first.announceRate = 1.75f
        first.announcePitch = 0.6f
        first.alertStyle = "melody"
        first.alertVolume = 0.35f
        first.vibrateOnAnnounce = false

        val reloaded = Prefs(app)
        assertEquals(1.75f, reloaded.announceRate, 0.001f)
        assertEquals(0.6f, reloaded.announcePitch, 0.001f)
        assertEquals("melody", reloaded.alertStyle)
        assertEquals(0.35f, reloaded.alertVolume, 0.001f)
        assertEquals(false, reloaded.vibrateOnAnnounce)
    }

    @Test
    fun `audio prefs clamp to designed ranges`() {
        val first = Prefs(app)
        first.announceRate = 9f     // beyond 2.0
        first.announcePitch = -1f   // below 0.5
        first.alertVolume = 5f      // beyond 1.0
        val reloaded = Prefs(app)
        assertEquals(2.0f, reloaded.announceRate, 0.001f)
        assertEquals(0.5f, reloaded.announcePitch, 0.001f)
        assertEquals(1.0f, reloaded.alertVolume, 0.001f)
    }

    @Test
    fun `community consent defaults are safe and persist`() {
        val first = Prefs(app)
        // Hard privacy contract: upload defaults OFF, read defaults ON.
        assertEquals(false, first.communityUploadConsent)
        assertEquals(true, first.communityReadEnabled)
        first.communityUploadConsent = true
        assertEquals(true, Prefs(app).communityUploadConsent)
    }
}
