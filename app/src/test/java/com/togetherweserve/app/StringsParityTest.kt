package com.togetherweserve.app

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/**
 * Multi-language guard: every string in the default (English) resources must also
 * exist in isiZulu (values-zu) and Sesotho (values-st), so no screen silently falls
 * back to English. Runs in GitHub Actions on every push.
 */
class StringsParityTest {

    private fun keys(folder: String): Set<String> {
        val file = File("src/main/res/$folder/strings.xml")
        assertThat(file.exists()).isTrue()
        return Regex("<string\\s+name=\"([^\"]+)\"").findAll(file.readText()).map { it.groupValues[1] }.toSet()
    }

    private fun placeholders(folder: String): Map<String, List<String>> {
        val text = File("src/main/res/$folder/strings.xml").readText()
        return Regex("<string\\s+name=\"([^\"]+)\"[^>]*>(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
            .findAll(text).associate { m ->
                m.groupValues[1] to Regex("%\\d\\$[sd]").findAll(m.groupValues[2]).map { it.value }.sorted().toList()
            }
    }

    @Test fun `isiZulu contains every default string`() =
        assertThat(keys("values-zu")).containsAtLeastElementsIn(keys("values"))

    @Test fun `Sesotho contains every default string`() =
        assertThat(keys("values-st")).containsAtLeastElementsIn(keys("values"))

    @Test fun `translations keep the same format placeholders as English`() {
        val base = placeholders("values")
        for (folder in listOf("values-zu", "values-st")) {
            val translated = placeholders(folder)
            for ((key, expected) in base) {
                assertThat(translated[key]).isEqualTo(expected)
            }
        }
    }
}
