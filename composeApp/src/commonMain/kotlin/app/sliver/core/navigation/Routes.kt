package app.sliver.core.navigation

enum class Route(val path: String, val title: String) {
    Onboarding("onboarding", "Onboarding"),
    Camera("camera", "Camera"),
    Trim("trim", "Edit clip"),
    PhotoTrim("photo-trim", "Photo trim"),
    GalleryPicker("gallery", "Gallery picker"),
    Looks("looks", "Filters editor"),
    Project("project", "Project"),
    DayDetail("day", "Clip detail"),
    FillThePast("fill-the-past", "Fill empty days"),
    FilmBuilder("film-builder", "Film builder"),
    Export("export", "Export"),
    FilmPlayer("film-player", "Film player"),
    Premiere("premiere", "Premiere"),
    YearInSeconds("year-in-seconds", "Year in clips"),
    Settings("settings", "Settings"),
    Backup("backup", "Backup & account"),
    Restore("restore", "Restore"),
    Notifications("notifications", "Notifications"),
    ;

    companion object {
        fun fromPath(path: String?): Route? = entries.firstOrNull { it.path == path }

        fun start(onboardingDone: Boolean): Route = if (onboardingDone) Camera else Onboarding
    }
}
