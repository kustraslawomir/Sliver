package app.sliver.feature.onboarding

enum class OnboardingStep { Concept, AutoPick, Format, CameraMic, Photos, Reminder, Google }

/** Google step stays hidden until Drive sign-in (T33) exists. */
const val GOOGLE_AVAILABLE = false

object OnboardingFlow {
    fun steps(googleAvailable: Boolean = GOOGLE_AVAILABLE): List<OnboardingStep> =
        OnboardingStep.entries.filter { it != OnboardingStep.Google || googleAvailable }

    fun canSkip(step: OnboardingStep) = step.ordinal <= OnboardingStep.Format.ordinal

    fun canGoBack(index: Int) = index > 0

    fun isLast(index: Int, steps: List<OnboardingStep>) = index == steps.lastIndex
}
