# T-media-any-source: photos and videos from anywhere, as photo or video (owner request, 2026-11-12)

Owner: "What if my girlfriend sends me photos from an iPhone? Photos, videos and Live Photos must work.
Android motion photos should load as a photo or as a video, as the user wants. If I move the files off
the phone, Sliver must still keep the project."

## 1. Motion photo: user chooses photo or video
Today (U08) a Motion Photo always becomes a video clip. Add the choice:
- When the picked item is a Motion Photo, Edit clip shows a two-option control "Video · Photo"
  (Video default). "Photo" switches to the Photo clip flow (Still / Slow zoom) using the still image.
  Same control in Photo clip to switch back. Use existing chip components (Geist, no mono, no uppercase).
- Fill empty days (backfill) gets the same per-item default: Video for motion photos (setting not needed).

## 2. iPhone media
Test fixtures must be synthetic (generate them; never use real personal media).
- HEIC/HEIF stills: decode via ImageDecoder (API 28+); thumbnails, Photo clip and export work.
- iPhone video: HEVC in .mov, often HDR (HLG or Dolby Vision 8.4). Make sure Edit clip preview and the
  1 s export work; when the source is HDR, tone-map to SDR in the Transformer composition
  (Media3 `Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL` or MediaCodec mode where supported)
  so colours don't look washed out. Check `Composition.Builder.setHdrMode` exists in our Media3 version.
- Rotation metadata from .mov (90/270) must be honoured (displaySize already handles MP4).
- Live Photo: arrives as a HEIC/JPEG + .MOV pair (AirDrop-to-Mac, iCloud download, Google Photos
  "download original"); messengers drop the motion and send only the still. Support the pair: allow
  multi-select in Add media; if the user picks a still + a short .mov with the same base name or
  capture time within 2 s, treat them as one Live Photo → video clip by default (photo option as in §1).
  A still alone is a normal photo.
- Capture date: read EXIF DateTimeOriginal / QuickTime creationdate so the clip lands on the right day
  (check existing CaptureDate logic handles .mov and HEIC).

## 3. Originals can disappear
The app already copies each day into app storage. Add a test: save a clip from a content Uri, delete the
source file, then the day, its thumbnail, the film render and backup still work. Also check that
Edit clip of an existing day never needs the original.

## 4. Owner action needed (not code)
Drive backup/restore only works with a real OAuth client: `GOOGLE_WEB_CLIENT_ID` (see HANDOFF T33).
Until then the app uses fake auth and a phone change loses data unless the user exports the zip.
Put a clear line in HANDOFF.md "Owner TODO".

## Done when
Build + tests + lint + designLint + verifyRoborazziDebug green; device check on Xiaomi/emulator with
synthetic HEIC, HEVC-HDR .mov, a Motion Photo and a Live Photo pair placed in the SliverDemo folders
(only those folders are safe); screenshots of the Video · Photo choice (dark + light).
