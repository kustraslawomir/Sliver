package app.sliver

import app.sliver.feature.onboarding.OnboardingFlow
import app.sliver.feature.onboarding.OnboardingStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingFlowTest {
    @Test
    fun threeScreens() {
        assertEquals(listOf(OnboardingStep.Welcome, OnboardingStep.Project, OnboardingStep.Camera), OnboardingFlow.steps())
    }

    @Test
    fun backIsHiddenOnFirstStep() {
        assertFalse(OnboardingFlow.canGoBack(0))
        assertTrue(OnboardingFlow.canGoBack(1))
    }
}
