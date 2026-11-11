package app.sliver

import app.sliver.feature.onboarding.OnboardingFlow
import app.sliver.feature.onboarding.OnboardingStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingFlowTest {
    @Test
    fun googleStepHiddenUnlessAvailable() {
        assertEquals(6, OnboardingFlow.steps(false).size)
        assertEquals(OnboardingStep.Google, OnboardingFlow.steps(true).last())
    }

    @Test
    fun skipOnlyOnFirstThreeSteps() {
        assertTrue(OnboardingFlow.canSkip(OnboardingStep.Format))
        assertFalse(OnboardingFlow.canSkip(OnboardingStep.CameraMic))
    }

    @Test
    fun backNotOnFirstStep() {
        assertFalse(OnboardingFlow.canGoBack(0))
        assertTrue(OnboardingFlow.canGoBack(1))
    }
}
