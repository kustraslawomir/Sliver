# F03 — Projects: model + migration

Depends on: —

## Goal
Introduce projects per docs/PROJECTS.md.

## Do
- Project entity, Day→Clip keyed by (projectId, date), Film.projectId, settings.activeProjectId.
- Room migration creating 'My <year>' and moving existing data into it; migration test with a fixture DB.
- Repositories filter by active project; Flow of active project.

## Don't
- No UI yet.

## Done when
- [ ] Migration test passes
- [ ] Existing installs keep all clips
- [ ] Build + tests pass

Refs: CLAUDE.md, docs/NAVIGATION.md, docs/TERMS.md, docs/PROJECTS.md, docs/UI-RULES.md
