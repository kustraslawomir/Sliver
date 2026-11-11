# U01 — System Photo Picker (policy fix)

Depends on: —

## Goal
Comply with Play's Photo and Video Permissions policy without losing features.

## Do
- Remove READ_MEDIA_IMAGES / READ_MEDIA_VIDEO / READ_MEDIA_VISUAL_USER_SELECTED from the manifest and code.
- 'Add media' launches `PickVisualMedia(ImageAndVideo)` — single item. The 'for <date>' context stays in our UI (sheet title before launching the picker).
- 'Fill empty days': user picks many items with `PickMultipleVisualMedia` ('Select photos and videos from the days you missed'); we read each item's taken date (picker columns / ExifInterface / MediaMetadataRetriever) and map to empty days of the active project, then the existing results screen.
- Delete the custom picker screen from v1.1 F11 and the limited-access banners.
- Optional, behind a flag: embedded picker (`androidx.photopicker` is still alpha, runtime API 34+) — don't ship it until stable.

## Don't
- No custom gallery grid.
- No broad permission requests.

## Done when
- [ ] Manifest has no READ_MEDIA_* permissions
- [ ] Add media and Fill empty days work on Android 10–16
- [ ] Build passes

Refs: CLAUDE.md (+ v1.2 addendum), docs/UX-2026.md, v1.1 docs
