# v1.1 fix tasks

One per session, in this order. F01, F03 and F14 have no dependencies and can start in parallel.

| Task | Title | Depends on |
|---|---|---|
| [F01](F01-fonts-icons.md) | Fonts + icon set | — |
| [F02](F02-terminology.md) | Terminology | F01 |
| [F03](F03-projects-model.md) | Projects: model + migration | — |
| [F04](F04-projects-ui.md) | Projects sheet: switch + create | F03, F01 |
| [F05](F05-navigation.md) | Navigation model | F03 |
| [F06](F06-project-screen.md) | Project screen | F05, F04 |
| [F07](F07-camera-layout.md) | Camera layout v2 | F01, F04 |
| [F08](F08-record-orientation.md) | Record in the real orientation | F07 |
| [F09](F09-clip-aspect.md) | Previews use the clip's real aspect | F08 |
| [F10](F10-edit-clip-fixes.md) | Edit clip: loading, sound, save | F05, F09 |
| [F11](F11-media-picker.md) | Media picker fixes | F01, F05 |
| [F12](F12-filters.md) | Looks → Filters | F02 |
| [F13](F13-onboarding-v2.md) | Onboarding: 3 screens | F04, F05 |
| [F14](F14-photo-still-default.md) | Photo clip: Still by default | — |
| [F15](F15-qa-pass.md) | Device QA pass | all above |
