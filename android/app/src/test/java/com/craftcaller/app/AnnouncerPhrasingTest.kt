package com.craftcaller.app

import com.craftcaller.app.util.AnnounceKind
import com.craftcaller.app.util.AnnounceLang
import com.craftcaller.app.util.Announcer
import com.craftcaller.app.util.Gender
import com.craftcaller.app.util.Honorific
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM phrasing tests: honorifics, kind-aware greetings, ordinals and
 * the custom speech template with its token fallbacks.
 */
class AnnouncerPhrasingTest {

    // ---------------- honorifics (onboarding persistence format) -------------

    @Test
    fun `addressed appends each honorific suffix`() {
        assertEquals("Umesh sir", Announcer.addressed("Umesh", Honorific.SIR))
        assertEquals("Umesh ma'am", Announcer.addressed("Umesh", Honorific.MAAM))
        assertEquals("Umesh ji", Announcer.addressed("Umesh", Honorific.JI))
        assertEquals("Umesh", Announcer.addressed("Umesh", Honorific.NONE))
    }

    @Test
    fun `addressed keeps only the first name`() {
        assertEquals("Deepa ma'am", Announcer.addressed("Deepa Sharma Malhotra", Honorific.MAAM))
    }

    @Test
    fun `default honorific follows gender`() {
        assertEquals(Honorific.MAAM, Announcer.defaultHonorific(Gender.FEMALE))
        assertEquals(Honorific.SIR, Announcer.defaultHonorific(Gender.MALE))
    }

    @Test
    fun `honorific roundtrips through persisted key`() {
        Honorific.entries.forEach { honorific ->
            assertEquals(honorific, Honorific.fromKey(honorific.key))
        }
        // Unknown stored text never crashes onboarding restore.
        assertEquals(Honorific.NONE, Honorific.fromKey("something-else"))
    }

    // ---------------- kind-aware greetings -----------------------------------

    @Test
    fun `saved greeting in all three languages`() {
        val hindi = Announcer.greeting("Umesh sir", "Rahul Sharma", AnnounceLang.HINDI, AnnounceKind.SAVED, 1)
        assertEquals("Umesh sir, aapko Rahul Sharma ka call aa raha hai.", hindi)
        val english = Announcer.greeting("Umesh sir", "Rahul Sharma", AnnounceLang.ENGLISH, AnnounceKind.SAVED, 1)
        assertEquals("Umesh sir, you have a call from Rahul Sharma.", english)
        val hinglish = Announcer.greeting("Umesh sir", "Rahul Sharma", AnnounceLang.HINGLISH, AnnounceKind.SAVED, 1)
        assertEquals("Umesh sir, aapke liye Rahul Sharma ka call aaya hai.", hinglish)
    }

    @Test
    fun `unknown greeting never mentions a caller name`() {
        AnnounceLang.entries.forEach { lang ->
            val text = Announcer.greeting("Umesh sir", "", lang, AnnounceKind.UNKNOWN, 1)
            assertTrue("unexpected: $text", !text.contains("Rahul"))
        }
        assertEquals(
            "Umesh sir, ek unknown number se call aa raha hai.",
            Announcer.greeting("Umesh sir", "", AnnounceLang.HINDI, AnnounceKind.UNKNOWN, 1)
        )
        assertEquals(
            "Umesh sir, ek unknown number se call aa raha hai — dekh lo.",
            Announcer.greeting("Umesh sir", "", AnnounceLang.HINGLISH, AnnounceKind.UNKNOWN, 1)
        )
        assertEquals(
            "Umesh sir, you have a call from an unknown number.",
            Announcer.greeting("Umesh sir", "", AnnounceLang.ENGLISH, AnnounceKind.UNKNOWN, 1)
        )
    }

    @Test
    fun `repeat greeting uses hindi ordinal`() {
        val twice = Announcer.greeting("Umesh sir", "Rahul", AnnounceLang.HINDI, AnnounceKind.REPEAT, 2)
        assertTrue(twice.contains("doosri"))
        val third = Announcer.greeting("Umesh sir", "Rahul", AnnounceLang.HINDI, AnnounceKind.REPEAT, 3)
        assertTrue(third.contains("teesri"))
    }

    @Test
    fun `english ordinal suffixes`() {
        assertEquals("2nd", Announcer.ordinalEnglish(2))
        assertEquals("3rd", Announcer.ordinalEnglish(3))
        assertEquals("11th", Announcer.ordinalEnglish(11))
        assertEquals("21st", Announcer.ordinalEnglish(21))
    }

    // ---------------- custom speech template ---------------------------------

    @Test
    fun `custom template replaces tokens`() {
        val text = Announcer.greeting(
            "Umesh sir", "Rahul Sharma", AnnounceLang.HINDI, AnnounceKind.SAVED, 1,
            customTemplate = "{you}, {caller} ki call"
        )
        assertEquals("Umesh sir, Rahul Sharma ki call.", text)
    }

    @Test
    fun `custom template unknown caller falls back to unknown phrase`() {
        val hindi = Announcer.greeting(
            "Umesh sir", "", AnnounceLang.HINDI, AnnounceKind.UNKNOWN, 1,
            customTemplate = "{caller} se call"
        )
        assertEquals("ek unknown number se call.", hindi)
        val english = Announcer.greeting(
            "Umesh sir", "", AnnounceLang.ENGLISH, AnnounceKind.UNKNOWN, 1,
            customTemplate = "you have a call from {caller}"
        )
        // The {caller} token itself becomes the phrase; the surrounding text
        // is the literal the user wrote.
        assertEquals("you have a call from an unknown number.", english)
    }

    @Test
    fun `custom template repeat token holds ordinal only on repeats`() {
        val first = Announcer.greeting(
            "Umesh sir", "Rahul", AnnounceLang.HINDI, AnnounceKind.SAVED, 1,
            customTemplate = "{caller} ka call{repeat}"
        )
        assertEquals("Rahul ka call.", first)
        val repeat = Announcer.greeting(
            "Umesh sir", "Rahul", AnnounceLang.HINDI, AnnounceKind.REPEAT, 2,
            customTemplate = "{caller} ka {repeat} call"
        )
        assertEquals("Rahul ka (doosri) call.", repeat)
    }

    @Test
    fun `custom template is case-insensitive and punctuation is finalised`() {
        val text = Announcer.greeting(
            "Umesh sir", "Rahul", AnnounceLang.ENGLISH, AnnounceKind.SAVED, 1,
            customTemplate = "Hello {YOU} - Call from {CALLER}"
        )
        assertEquals("Hello Umesh sir - Call from Rahul.", text)
    }

    @Test
    fun `empty template falls back to built-in phrasing`() {
        val builtIn = Announcer.greeting(
            "Umesh sir", "Rahul", AnnounceLang.HINDI, AnnounceKind.SAVED, 1,
            customTemplate = "   "
        )
        assertEquals("Umesh sir, aapko Rahul ka call aa raha hai.", builtIn)
    }
}
