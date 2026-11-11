# UI rules v1.1

## Fonts
Bundle and actually use: **Geist** (UI), **Geist Mono** (only dates, timecodes, counts), **Instrument Serif** (big titles: onboarding, project name, film title cards).
The screenshots show Roboto everywhere — the font resources aren't applied. Fix at theme level: `SliverTheme` must pass the families into `Typography` and every `Text` must use theme styles.
Sizes: buttons/chips 15–17 sp Geist 500; chip labels never uppercase-mono; date headers mono 12 sp caps.

## Icons
One icon set: **Lucide** geometry (1.5 stroke, round caps). Convert the needed SVGs to `ImageVector` and commit them under `core/icons/` (e.g. with the Valkyrie or svg-to-compose tool) — no runtime icon fonts.
Needed: x, chevron-down, chevron-left, chevron-right, volume-2, volume-x, grid-3x3, timer, zap, zap-off, switch-camera (refresh-ccw), sliders-horizontal (filters), image-plus (add media), calendar, film, settings, check, plus, play, pause, share, download, trash-2, rotate-ccw (undo), smartphone (rotate hint), folder, cloud-upload, bell.
Icon buttons: 44 dp hit area, 22 dp icon, optional 9–10 sp label under it (Geist 500, not mono, not letter-spaced).

## Chips / menus (gallery filter row bug)
- One horizontal row, `LazyRow`, never wraps, never shrinks text to one letter per line (screenshot gallery-filters.jpg).
- Chip height 36 dp, padding 14 dp, label maxLines = 1.
- Two groups separated by a 1 dp divider: [Recents | Favorites] · [All | Videos | Photos]. Selected = ember outline + tint.
- No empty space between header and grid.

## Loading
Any media screen: skeleton within 100 ms, then progress with words ("Preparing video… 42%"). Never a blank black box with a disabled button (screenshot edit-loading.jpg).

## Single-flight actions
Wrap primary actions in a `rememberSingleFlight {}` helper / ViewModel guard: first tap disables the button and shows inline progress; repeated taps are ignored; on finish navigate per NAVIGATION.md. Navigation calls are de-duplicated (ignore if already navigating).

## Rotation
Activity stays portrait-locked. Icons rotate to stay upright (0/90/180/270 from OrientationEventListener, animated 240 ms). Recording uses the real device rotation (see F08). Previews size to the clip's real aspect ratio.
