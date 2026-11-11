package app.sliver.core.navigation

enum class Route(val path: String, val title: String, val tabBar: Boolean = false) {
    Onboarding("onboarding", "Onboarding"),
    Camera("camera", "Camera"),
    Trim("trim", "Edit clip"),
    PhotoTrim("photo-trim", "Photo trim"),
    GalleryPicker("gallery", "Gallery picker"),
    Looks("looks", "Filters editor"),
    Archive("archive", "Archive", tabBar = true),
    DayDetail("day", "Day detail"),
    FillThePast("fill-the-past", "Fill empty days"),
    Films("films", "Films", tabBar = true),
    FilmBuilder("film-builder", "Film builder"),
    Export("export", "Export"),
    FilmPlayer("film-player", "Film player"),
    Premiere("premiere", "Premiere"),
    YearInSeconds("year-in-seconds", "Year in Seconds"),
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
