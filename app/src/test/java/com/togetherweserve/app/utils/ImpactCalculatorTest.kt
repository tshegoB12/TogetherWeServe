package com.togetherweserve.app.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ImpactCalculatorTest {
    @Test fun `counts only confirmed registrations`() =
        assertThat(ImpactCalculator.confirmedCount(listOf("confirmed", "pending_sync", "confirmed", "cancelled"))).isEqualTo(2)

    @Test fun `empty schedule gives zero impact`() {
        assertThat(ImpactCalculator.confirmedCount(emptyList())).isEqualTo(0)
        assertThat(ImpactCalculator.estimatedHours(0)).isEqualTo(0)
    }

    @Test fun `hours are events times hours per event`() =
        assertThat(ImpactCalculator.estimatedHours(3)).isEqualTo(3 * ImpactCalculator.HOURS_PER_EVENT)

    @Test fun `negative input never gives negative hours`() =
        assertThat(ImpactCalculator.estimatedHours(-4)).isEqualTo(0)
}
