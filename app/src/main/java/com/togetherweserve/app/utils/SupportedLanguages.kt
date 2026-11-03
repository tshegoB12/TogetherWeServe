package com.togetherweserve.app.utils

/** The three languages shipped with the app: English plus two South African languages. */
object SupportedLanguages {
    data class Language(val code: String, val label: String)

    val all = listOf(
        Language("en", "English"),
        Language("zu", "isiZulu"),
        Language("st", "Sesotho")
    )

    fun isSupported(code: String?): Boolean = all.any { it.code == code }

    /** Falls back to English for null/unknown values (e.g. corrupted preferences). */
    fun normalise(code: String?): String = if (isSupported(code)) code!! else "en"

    fun indexOf(code: String?): Int = all.indexOfFirst { it.code == code }.coerceAtLeast(0)
}
