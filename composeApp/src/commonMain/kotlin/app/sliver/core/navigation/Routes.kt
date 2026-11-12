package app.sliver.core.navigation

enum class Route(val path: String, val title: String) {
    Onboarding("onboarding", "Onboarding"),
    Camera("camera", "Camera"),
    Trim("trim", "Edit clip"),
    PhotoTrim("photo-trim", "Photo trim"),
    Project("project", "Project"),
    DayDetail("day", "Clip detail"),
    FillThePast("fill-the-past", "Auto-import"),
    FilmBuilder("film-builder", "Film builder"),
    Export("export", "Export"),
    FilmPlayer("film-player", "Film player"),
    Premiere("premiere", "Premiere"),
    YearInClips("year-in-clips", "Year in clips"),
    Settings("settings", "Settings"),
    Backup("backup", "Backup & account"),
    Restore("restore", "Restore"),
    Reminders("reminders", "Daily reminder"),
    ;

    companion object {
        fun fromPath(path: String?): Route? = entries.firstOrNull { it.path == path }

        fun start(onboardingDone: Boolean): Route = if (onboardingDone) Project else Onboarding
    }
}
