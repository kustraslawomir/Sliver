# Task index

One task per agent session. Tasks in the same milestone can run in parallel when their dependencies are done.


## M0 Foundation
| Task | Title | Depends on |
|---|---|---|
| [T01](T01-project-setup.md) | Project setup | — |
| [T02](T02-theme-components.md) | Theme + base components | T01 |
| [T03](T03-navigation-shell.md) | Navigation shell | T02 |
| [T04](T04-database-settings.md) | Database, settings, repositories | T01 |
| [T05](T05-file-store.md) | FileStore + date rules | T04 |

## M1 Capture
| Task | Title | Depends on |
|---|---|---|
| [T06](T06-permissions.md) | Permission pre-prompts | T03 |
| [T07](T07-camera-screen.md) | Camera screen | T05, T06 |
| [T08](T08-clip-exporter.md) | Clip exporter (1 s) | T05 |
| [T09](T09-auto-pick.md) | Auto-pick scoring | T01 |
| [T10](T10-trim-screen.md) | Trim & Pick screen | T07, T08, T09 |
| [T11](T11-gallery-picker.md) | Gallery picker | T06, T10 |
| [T12](T12-photo-to-second.md) | Photo → 1 s | T08, T11 |
| [T13](T13-camera-today-state.md) | Camera: today captured + replace | T10 |

## M2 Looks
| Task | Title | Depends on |
|---|---|---|
| [T15](T15-lut-generator.md) | Look presets + LUT files | T01 |
| [T16](T16-look-effects.md) | Look effects engine | T15 |
| [T17](T17-looks-editor.md) | Looks editor screen | T16, T10 |

## M3 Archive
| Task | Title | Depends on |
|---|---|---|
| [T18](T18-archive-month-year.md) | Archive: month + year | T05, T10 |
| [T19](T19-day-detail.md) | Day detail | T18, T16 |
| [T20](T20-share-second.md) | Share one second | T19 |
| [T21](T21-fill-the-past.md) | Fill the past | T11, T12, T09 |

## M4 Films
| Task | Title | Depends on |
|---|---|---|
| [T22](T22-films-library.md) | Films: model + library | T04, T03 |
| [T23](T23-film-builder.md) | Film builder | T22, T16 |
| [T14](T14-format-hints.md) | Film format hints + reframe | T10, T22 |
| [T24](T24-film-render.md) | Film render | T23 |
| [T25](T25-export-premiere-player.md) | Export, premiere, player, share | T24 |
| [T26](T26-year-in-seconds.md) | Year in Seconds | T25 |

## M5 App
| Task | Title | Depends on |
|---|---|---|
| [T27](T27-onboarding.md) | Onboarding | T06, T28 |
| [T28](T28-notifications.md) | Local notifications | T04, T03 |
| [T29](T29-settings.md) | Settings screen | T03, T04 |
| [T30](T30-splash-edge-states.md) | Splash + edge states | T02, T18 |
| [T31](T31-widgets.md) | Widget + Quick Settings tile | T13 |
| [T32](T32-polish-a11y.md) | Accessibility + motion pass | T25, T18 |

## M6 Backup
| Task | Title | Depends on |
|---|---|---|
| [T33](T33-google-auth.md) | Google sign-in + Drive scope | T03, T04 |
| [T34](T34-drive-client.md) | Drive REST client | T33 |
| [T35](T35-backup-worker.md) | Backup worker + screen | T34 |
| [T36](T36-restore.md) | Restore on a new phone | T35 |
| [T37](T37-account-delete.md) | Account: sign out + delete | T35 |

## First playable milestone
After T01–T10 + T18 you can record, trim, save and browse days. Use the app daily from there — real use will tell you more than any spec.
