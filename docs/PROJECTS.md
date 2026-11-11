# Projects (new in v1.1)

A **project** is a timeline of clips with its own format, e.g. "My 2026" (Cinema 16:9), "Lisbon trip" (Story 9:16), "Garden". Films are made *from a project*.

## Model
Project: id (uuid) · name · format CINEMA | STORY · startDate · endDate? · createdAt · archived:Boolean · colorSeed
Day → becomes **Clip**: PK (projectId, date) — one clip per project per day. Same day can have clips in several projects.
Film: + projectId (films belong to a project; range inside the project).
Settings: activeProjectId (replaces camera.activeFilmId).

## Migration
- Create project "My <year of first clip>" (format = settings.newFilmFormat) and attach all existing days and films to it. Make it active.
- Room migration with a test (old schema fixture → new).

## Behaviour
- Camera saves to the **active project**; the project chip on Camera switches it.
- Project screen shows the active project (calendar + films). Switching project there switches it everywhere.
- New project: name (default "Project <n>"), format, start date (today default). Becomes active.
- Rename / change format / archive from the project's ⋯ menu. Delete project = confirm, deletes its clips + films (Drive copies trashed).
- Backup: Drive folder `SLIVER/<project name>/`, manifest lists projects.
- Year in Seconds = the yearly film of the project that has the most clips that year (or each project — DECISION by agent, keep simple).
