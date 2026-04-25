package com.sindromradiospb.ttsprototypev2.feature.classic

import org.junit.Assert.assertEquals
import org.junit.Test

class HebrewTransliteratorTest {
    @Test
    fun transliterationMatchesSourcePrototypeFixtures() {
        val fixtures = listOf(
            Fixture("שָׁלוֹם", "šālôm", "шалом"),
            Fixture("בָּרוּךְ אַתָּה", "bārûḵ ʾatâ", "барух ата"),
            Fixture("מֶלֶךְ הָעוֹלָם", "meleḵ hāʿôlām", "мэлэх хаолам"),
            Fixture("כֻּלָּם גַּנָּבִים", "kūlām ganāḇîm", "кулам ганавим"),
            Fixture("אֲנִי רוֹצֶה לִלְמֹד עִבְרִית", "ʾănî rôṣê lilmōḏ ʿiḇrîṯ", "ани роцэ лилмод иврит"),
        )

        fixtures.forEach { fixture ->
            assertEquals(fixture.sbl, transliterateHebrewWithProfile(fixture.hebrew, ClassicTranslitProfile.Sbl))
            assertEquals(fixture.ru, transliterateHebrewWithProfile(fixture.hebrew, ClassicTranslitProfile.RuPhonetic))
        }
    }

    private data class Fixture(
        val hebrew: String,
        val sbl: String,
        val ru: String,
    )
}
