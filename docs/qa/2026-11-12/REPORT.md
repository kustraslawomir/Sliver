# Device QA report

- device: xiaomi (b798509a)
- date: 2026-11-12
- duration: 584s

Score: 1.000 = identical to the design board. Data is synthetic (seeder), fonts and thumbnails differ, so scores are indicative only.

| Screen | dark-1.0 | dark-1.3 | light-1.0 | light-1.3 |
|---|---|---|---|---|
| DayDetail | 0.813 ([shot](dark-1.0/DayDetail.png), [overlay](compare/dark-1.0/DayDetail.png)) | 0.813 ([shot](dark-1.3/DayDetail.png), [overlay](compare/dark-1.3/DayDetail.png)) | 0.744 ([shot](light-1.0/DayDetail.png), [overlay](compare/light-1.0/DayDetail.png)) | 0.750 ([shot](light-1.3/DayDetail.png), [overlay](compare/light-1.3/DayDetail.png)) |
| FilmPlayer | 0.744 ([shot](dark-1.0/FilmPlayer.png), [overlay](compare/dark-1.0/FilmPlayer.png)) | 0.802 ([shot](dark-1.3/FilmPlayer.png), [overlay](compare/dark-1.3/FilmPlayer.png)) | [shot](light-1.0/FilmPlayer.png) n/a | [shot](light-1.3/FilmPlayer.png) n/a |
| ProjectHub | 0.930 ([shot](dark-1.0/ProjectHub.png), [overlay](compare/dark-1.0/ProjectHub.png)) | 0.923 ([shot](dark-1.3/ProjectHub.png), [overlay](compare/dark-1.3/ProjectHub.png)) | 0.859 ([shot](light-1.0/ProjectHub.png), [overlay](compare/light-1.0/ProjectHub.png)) | 0.852 ([shot](light-1.3/ProjectHub.png), [overlay](compare/light-1.3/ProjectHub.png)) |
| ProjectHubYear | 0.893 ([shot](dark-1.0/ProjectHubYear.png), [overlay](compare/dark-1.0/ProjectHubYear.png)) | 0.892 ([shot](dark-1.3/ProjectHubYear.png), [overlay](compare/dark-1.3/ProjectHubYear.png)) | 0.891 ([shot](light-1.0/ProjectHubYear.png), [overlay](compare/light-1.0/ProjectHubYear.png)) | 0.886 ([shot](light-1.3/ProjectHubYear.png), [overlay](compare/light-1.3/ProjectHubYear.png)) |
| Reminders | 0.923 ([shot](dark-1.0/Reminders.png), [overlay](compare/dark-1.0/Reminders.png)) | 0.906 ([shot](dark-1.3/Reminders.png), [overlay](compare/dark-1.3/Reminders.png)) | 0.923 ([shot](light-1.0/Reminders.png), [overlay](compare/light-1.0/Reminders.png)) | 0.907 ([shot](light-1.3/Reminders.png), [overlay](compare/light-1.3/Reminders.png)) |
| Settings | 0.952 ([shot](dark-1.0/Settings.png), [overlay](compare/dark-1.0/Settings.png)) | 0.944 ([shot](dark-1.3/Settings.png), [overlay](compare/dark-1.3/Settings.png)) | 0.955 ([shot](light-1.0/Settings.png), [overlay](compare/light-1.0/Settings.png)) | 0.947 ([shot](light-1.3/Settings.png), [overlay](compare/light-1.3/Settings.png)) |

## Flows

```
PASS dark-1.0 10_camera
PASS dark-1.0 01_home
PASS dark-1.0 02_year
PASS dark-1.0 03_projects
PASS dark-1.0 04_day
PASS dark-1.0 05_settings
PASS dark-1.0 06_film
PASS light-1.0 01_home
PASS light-1.0 02_year
PASS light-1.0 03_projects
PASS light-1.0 04_day
PASS light-1.0 05_settings
PASS light-1.0 06_film
PASS dark-1.3 01_home
PASS dark-1.3 02_year
PASS dark-1.3 03_projects
PASS dark-1.3 04_day
PASS dark-1.3 05_settings
PASS dark-1.3 06_film
PASS light-1.3 01_home
PASS light-1.3 02_year
PASS light-1.3 03_projects
PASS light-1.3 04_day
PASS light-1.3 05_settings
PASS light-1.3 06_film
PASS light-1.3 11_record_save
```

Not screenshotted by design (privacy): camera, recording, edit clip, system Photo Picker, projects sheet when real projects exist.
