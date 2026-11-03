package com.togetherweserve.app.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SupportedLanguagesTest {
    @Test fun `ships English and two South African languages`() =
        assertThat(SupportedLanguages.all.map { it.code }).containsExactly("en", "zu", "st").inOrder()

    @Test fun `unknown or null language falls back to English`() {
        assertThat(SupportedLanguages.normalise("xx")).isEqualTo("en")
        assertThat(SupportedLanguages.normalise(null)).isEqualTo("en")
    }

    @Test fun `supported codes are kept`() {
        assertThat(SupportedLanguages.normalise("zu")).isEqualTo("zu")
        assertThat(SupportedLanguages.normalise("st")).isEqualTo("st")
    }

    @Test fun `index lookup defaults to first entry`() {
        assertThat(SupportedLanguages.indexOf("st")).isEqualTo(2)
        assertThat(SupportedLanguages.indexOf("nope")).isEqualTo(0)
    }
}
