# Landscape verification

Owner records 2 s clips with the phone held sideways (right) and upside-down landscape, in-app.

1. Pull the saved clip (debug app files dir): `adb exec-out run-as app.sliver cat files/<project>/<day>.mp4 > clip.mp4`
   (or use the exported file from Photos via `adb pull`). Use only SliverDemo / your own test clips.
2. `scripts/check-landscape.sh clip.mp4` prints width, height, rotation and fails when width <= height after rotation.
   Without ffprobe: `MediaMetadataRetriever` METADATA_KEY_VIDEO_WIDTH / HEIGHT / ROTATION, swap w/h when rotation is 90 or 270.
3. Open the clip in Edit clip; the preview must be 16:9.
4. Paste the output below.

## Right landscape
_pending recording_

## Upside-down landscape
_pending recording_
