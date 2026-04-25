package com.sindromradiospb.ttsprototypev2.feature.classic

internal fun transliterateHebrewWithProfile(text: String, profile: ClassicTranslitProfile): String =
    when (profile) {
        ClassicTranslitProfile.Sbl -> transliterateSbl(text)
        ClassicTranslitProfile.RuPhonetic -> transliterateRuPhonetic(text)
    }.replace(Regex("\\s+"), " ").trim()

private data class HebrewCluster(
    val base: Char,
    val marks: String,
)

private val HebrewMarks: Set<Char> = ('\u0591'..'\u05C7').toSet()

private const val SHEVA = '\u05B0'
private const val HATAF_SEGOL = '\u05B1'
private const val HATAF_PATAH = '\u05B2'
private const val HATAF_QAMATS = '\u05B3'
private const val HIRIQ = '\u05B4'
private const val TSERE = '\u05B5'
private const val SEGOL = '\u05B6'
private const val PATAH = '\u05B7'
private const val QAMATS = '\u05B8'
private const val HOLAM = '\u05B9'
private const val HOLAM_HASER = '\u05BA'
private const val QUBUTS = '\u05BB'
private const val DAGESH = '\u05BC'
private const val SHIN_DOT = '\u05C1'

private fun transliterateSbl(text: String): String =
    transliterate(text, ::sblConsonant, ::sblVowel)

private fun transliterateRuPhonetic(text: String): String =
    transliterate(text, ::ruConsonant, ::ruVowel)

private fun transliterate(
    text: String,
    consonant: (HebrewCluster) -> String,
    vowel: (HebrewCluster, HebrewCluster?) -> String,
): String {
    val clusters = text.toClusters()
    val output = StringBuilder()
    var index = 0
    while (index < clusters.size) {
        val cluster = clusters[index]
        val previous = clusters.getOrNull(index - 1)
        val next = clusters.getOrNull(index + 1)
        if (cluster.isMaterFor(previous)) {
            index += 1
            continue
        }
        output.append(consonant(cluster))
        output.append(vowel(cluster, next))
        index += 1
    }
    return output.toString()
}

private fun String.toClusters(): List<HebrewCluster> {
    val clusters = mutableListOf<HebrewCluster>()
    var index = 0
    while (index < length) {
        val base = this[index]
        if (base in HebrewMarks) {
            index += 1
            continue
        }
        val marks = StringBuilder()
        var markIndex = index + 1
        while (markIndex < length && this[markIndex] in HebrewMarks) {
            marks.append(this[markIndex])
            markIndex += 1
        }
        clusters += HebrewCluster(base, marks.toString())
        index = markIndex
    }
    return clusters
}

private fun HebrewCluster.isMaterFor(previous: HebrewCluster?): Boolean =
    when (base) {
        'י' -> previous != null && previous.hasAny(HIRIQ, TSERE, SEGOL)
        'ה' -> previous != null && previous.hasAny(QAMATS, PATAH, TSERE, SEGOL) && marks.isEmpty()
        else -> false
    }

private fun HebrewCluster.has(mark: Char): Boolean = mark in marks

private fun HebrewCluster.hasAny(vararg candidates: Char): Boolean =
    candidates.any { it in marks }

private fun sblConsonant(cluster: HebrewCluster): String =
    when (cluster.base) {
        'א' -> "ʾ"
        'ב' -> if (cluster.has(DAGESH)) "b" else "ḇ"
        'ג' -> if (cluster.has(DAGESH)) "g" else "ḡ"
        'ד' -> if (cluster.has(DAGESH)) "d" else "ḏ"
        'ה' -> "h"
        'ו' -> if (cluster.hasAny(DAGESH, HOLAM, HOLAM_HASER)) "" else "w"
        'ז' -> "z"
        'ח' -> "ḥ"
        'ט' -> "ṭ"
        'י' -> "y"
        'כ', 'ך' -> if (cluster.has(DAGESH)) "k" else "ḵ"
        'ל' -> "l"
        'מ', 'ם' -> "m"
        'נ', 'ן' -> "n"
        'ס' -> "s"
        'ע' -> "ʿ"
        'פ', 'ף' -> if (cluster.has(DAGESH)) "p" else "p̄"
        'צ', 'ץ' -> "ṣ"
        'ק' -> "q"
        'ר' -> "r"
        'ש' -> if (cluster.has(SHIN_DOT)) "š" else "ś"
        'ת' -> if (cluster.has(DAGESH)) "t" else "ṯ"
        else -> cluster.base.toString()
    }

private fun sblVowel(cluster: HebrewCluster, next: HebrewCluster?): String =
    when {
        cluster.base == 'ו' && cluster.has(DAGESH) -> "û"
        cluster.base == 'ו' && cluster.hasAny(HOLAM, HOLAM_HASER) -> "ô"
        cluster.has(HIRIQ) && next?.base == 'י' -> "î"
        cluster.has(TSERE) && next?.base == 'י' -> "ê"
        cluster.has(SEGOL) && next?.base == 'י' -> "ê"
        cluster.has(TSERE) && next?.base == 'ה' -> "ê"
        cluster.has(SEGOL) && next?.base == 'ה' -> "ê"
        cluster.has(QAMATS) && next?.base == 'ה' -> "â"
        cluster.has(PATAH) && next?.base == 'ה' -> "â"
        cluster.has(SHEVA) -> ""
        cluster.has(HATAF_SEGOL) -> "ĕ"
        cluster.has(HATAF_PATAH) -> "ă"
        cluster.has(HATAF_QAMATS) -> "ŏ"
        cluster.has(HIRIQ) -> "i"
        cluster.has(TSERE) -> "ē"
        cluster.has(SEGOL) -> "e"
        cluster.has(PATAH) -> "a"
        cluster.has(QAMATS) -> "ā"
        cluster.hasAny(HOLAM, HOLAM_HASER) -> "ō"
        cluster.has(QUBUTS) -> "ū"
        else -> ""
    }

private fun ruConsonant(cluster: HebrewCluster): String =
    when (cluster.base) {
        'א', 'ע' -> ""
        'ב' -> if (cluster.has(DAGESH)) "б" else "в"
        'ג' -> "г"
        'ד' -> "д"
        'ה' -> "х"
        'ו' -> if (cluster.hasAny(DAGESH, HOLAM, HOLAM_HASER)) "" else "в"
        'ז' -> "з"
        'ח' -> "х"
        'ט' -> "т"
        'י' -> "й"
        'כ', 'ך' -> if (cluster.has(DAGESH)) "к" else "х"
        'ל' -> "л"
        'מ', 'ם' -> "м"
        'נ', 'ן' -> "н"
        'ס' -> "с"
        'פ', 'ף' -> if (cluster.has(DAGESH)) "п" else "ф"
        'צ', 'ץ' -> "ц"
        'ק' -> "к"
        'ר' -> "р"
        'ש' -> if (cluster.has(SHIN_DOT)) "ш" else "с"
        'ת' -> "т"
        else -> cluster.base.toString()
    }

private fun ruVowel(cluster: HebrewCluster, next: HebrewCluster?): String =
    when {
        cluster.base == 'ו' && cluster.has(DAGESH) -> "у"
        cluster.base == 'ו' && cluster.hasAny(HOLAM, HOLAM_HASER) -> "о"
        cluster.has(HIRIQ) && next?.base == 'י' -> "и"
        cluster.has(TSERE) && next?.base == 'י' -> "е"
        cluster.has(SEGOL) && next?.base == 'י' -> "е"
        cluster.has(QAMATS) && next?.base == 'ה' -> "а"
        cluster.has(PATAH) && next?.base == 'ה' -> "а"
        cluster.has(SHEVA) -> ""
        cluster.has(HATAF_SEGOL) -> "э"
        cluster.has(HATAF_PATAH) -> "а"
        cluster.has(HATAF_QAMATS) -> "о"
        cluster.has(HIRIQ) -> "и"
        cluster.has(TSERE) -> "е"
        cluster.has(SEGOL) -> "э"
        cluster.has(PATAH) -> "а"
        cluster.has(QAMATS) -> "а"
        cluster.hasAny(HOLAM, HOLAM_HASER) -> "о"
        cluster.has(QUBUTS) -> "у"
        else -> ""
    }
