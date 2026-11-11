# Which design file is the spec for which screen

Newest board wins. Boards marked *superseded* are kept only for parts not redrawn yet.

| Screen | Design file(s) | Notes |
|---|---|---|
| Design system: tokens, components | `DesignSystem.dc.html`, `tokens.json` | buttons, chips, sliders, sheets, toasts, calendar cells, sliver frame |
| Camera | `CameraV2.dc.html`, `CameraV2Land.dc.html` | supersedes `Camera.dc.html`, `CameraLandscape.dc.html`, `CameraDone.dc.html` |
| Edit clip | `EditClipV2.dc.html` (`variant` ready / loading) | supersedes `Trim.dc.html`. Take ONLY the short-clip chips and the reframe band from `Trim.dc.html`, nothing else |
| Photo clip | `PhotoTrim.dc.html` | button text = "Save clip"; motion chips Still (default) / Slow zoom |
| Filters editor | `Looks.dc.html`, `LooksAdjust.dc.html`, `Frame.dc.html` | title "Filters"; sliders: Sharpness, Contrast, Grain, Vignette, Fade, Saturation |
| Projects sheet + new project | `ProjectsV2.dc.html` | |
| Onboarding | `OnbV2.dc.html` (`step` 0–2) | supersedes `Onboarding.dc.html` |
| Media picker | none — Android system Photo Picker | `PickerV2.dc.html` only shows context, don't build it |
| Project screen (calendar + films) | `ProjectHub.dc.html` (month, ⋯ menu, add-to-empty-day sheet) · `ProjectHubYear.dc.html` (year + daily-nudge card) | Opened from the calendar chip on the camera. Header: chevron-down back, project name → project switcher, settings, ⋯. No tab bar. Archive*/Films.dc.html are old v1.0 — do not use. |
| Clip detail + share | `DayDetail.dc.html`, `DayShare.dc.html` | no mood chips |
| Add to an empty day | the sheet in `ProjectHub.dc.html` (tap an empty day) | `ArchiveAdd.dc.html` is old, don't use it |
| Fill empty days | `Backfill.dc.html` | after picking in the system picker: results mosaic → filling → done. `BackfillLimited.dc.html` is obsolete (system picker has no 'limited access'), so don't build it |
| Film builder / export / player | `FilmBuilder.dc.html`, `Export.dc.html`, `ExportPremiere.dc.html`, `FilmPlayer.dc.html` | |
| Year in Seconds | `YearInSeconds.dc.html` | |
| Back up / Drive / restore / account | `SaveSheet.dc.html`, `DriveConnect.dc.html`, `Backup.dc.html`, `BackupStates.dc.html`, `Restore.dc.html`, `AccountSettings.dc.html` | Google only |
| Notifications settings + look | `Notifications.dc.html`, `NotifDenied.dc.html`, `NotifBoard.dc.html` | |
| Settings | `Settings.dc.html` | |
| Splash, edge states, widgets | `Splash.dc.html`, `EdgeStates.dc.html`, `Widgets.dc.html` | |
| Before/after explanation | `Changes.dc.html` | reading only |

`design/png/` — put PNG exports of the boards here, named like the source file (`CameraV2.png`). Without PNGs, agents measure from the HTML.

## Added in v1.2

| Item | Design | Notes |
|---|---|---|
| App icon (launcher, themed, splash, notification, Play) | `AppIcon.dc.html` | Ready resources in `android-res/`. Copy as described in `android-res/README.md`. Do not redraw. |
| Record button | **Option D "Ember"**: `CameraV2.dc.html` (idle, tap to record), `CameraV2Rec.dc.html` (recording) · options board `RecordButtons.dc.html` | Own composable `RecordButton(state)`. Video idle: 72dp ember disc in an 80dp hit area, soft glow (drawBehind radial gradient, no blur). Photo: white disc. Recording: same disc + 24dp dark stop square, slow 1.6 s breathing glow, OFF when animations are disabled in system settings. While recording, hide ADD, flip, tools and the mode switch. Timer pill at the top: grey until 1 s, then ember + "CLIP OK" and ONE haptic tick (HapticFeedbackConstants.CONFIRM). Tap again = stop → Edit clip. |
| Logo / wordmark | `Logo.dc.html` | Files in `brand/` (SVG + VectorDrawable). Not shown on app screens. If you need it anywhere, use `@drawable/sliver_wordmark`, never Text(). |

## v1.2.1 — save flow + buttons

| Item | Design | Notes |
|---|---|---|
| After "Save clip" | `ProjectHubSaved.dc.html` | Save → navigate to the Project screen (pop the edit screen, don't keep it in the back stack). Today's cell pops in once (scale 0.6→1, 420ms) with a 2dp ember ring; count +1. **No toast or snackbar anywhere in the save flow.** Back from here → Camera. Undo = tap today → Day detail → Replace/Delete. |
| Buttons & chips | all | Text is ONE line: `maxLines = 1`, `softWrap = false`, width = wrap content, height 48dp (chips 32–40dp). Never set a fixed width on a button with text. If buttons don't fit side by side (large font scale), stack them full-width in a Column. Don't shrink the text. Test at font scale 1.3. |

## v1.2.2: fonts, camera top, focus, glow, copy

| Item | Design | Notes |
|---|---|---|
| **Fonts** | all boards, `tokens.json` | **Instrument Serif is gone.** Every title uses Geist 600 with negative tracking (display XL 52/56 −3%, L 36/40 −2.5%, M 24/30 −2%). Remove the serif font file from the project. Labels stay Geist Mono. |
| Camera top | `CameraV2.dc.html`, `CameraV2Land.dc.html` | 150dp top scrim (black 70% → 0) so chips read on any video. Chips: 36dp pill, black 55% + 12dp blur behind (RenderEffect on API 31+, plain 70% black below that), Geist 15/500 white, no border. Project chip has no format text. Calendar chip says "Today". |
| Tap to focus | `CameraV2.dc.html` (tap the viewfinder) | Tap: 88dp ring shrinks to 64dp in 220ms with a center dot, then fades to 35% after 1.2 s. CameraX `FocusMeteringAction` (AF+AE, auto-cancel 3 s) from `PreviewView.meteringPointFactory`. Hold 600ms: AE/AF lock, ring + dot turn ember with `glow.lock`, "AE/AF LOCK" pill 54dp above, no auto-cancel until the next tap. Not while recording. |
| Glow | `GLOW.md` | Mandatory. See GLOW.md. |
| Copy | all | The word "second" no longer exists in the UI: it's "clip" ("Save clip", "Share this clip", "Your year in clips"). "Looks" → "Filters". If you find "second(s)" in UI copy anywhere, it's a bug (durations like "1 s" are fine). |
| No toasts | all | Removed from Trim/PhotoTrim/Camera. Nothing may float over the UI after an action. Show the result in place (ProjectHubSaved). |
