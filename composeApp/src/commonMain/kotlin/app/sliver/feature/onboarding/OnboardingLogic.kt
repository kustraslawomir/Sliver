package app.sliver.feature.onboarding

enum class OnboardingStep { Welcome, Project, Camera }

object OnboardingFlow {
    fun steps(): List<OnboardingStep> = OnboardingStep.entries.toList()

    fun canGoBack(index: Int) = index > 0
}
