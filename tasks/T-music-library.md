# T-music-library — free music inside Reelog, legally clean

Owner wants better free music than YouTube Studio samples, credit in the app is fine, but no legal risk. Builds on the music list (T-ui-audit-v1.8 §5) and the audio layers editor (4d39861).

## Source
- Openverse API, `GET https://api.openverse.org/v1/audio/?q=<query>&category=music&license=cc0,pdm,by&page_size=20` (anonymous works; register an app key for higher limits, keep the key out of the repo — local.properties/BuildConfig).
- **Hard filter in our code too** (don't trust only the query): accept a track only if `license` ∈ {cc0, pdm, by}. Reject anything with nc, nd or sa — ND/SA are illegal or messy because putting music under video is an adaptation in CC 4.0; NC breaks for anyone who monetises.
- Accept only providers: jamendo, ccmixter, freesound, wikimedia_audio. Skip others.
- Skip tracks shorter than 20 s or with no duration.

## UI (music list sheet)
- Tabs (segmented): **My files** · **Free music**.
- Free music: search field + mood chips (Calm, Happy, Piano, Acoustic, Cinematic, Lo-fi) that fill the query. Rows like the music list: 40 dp preview play (streams from the source URL, no download), cleaned title, "Author · 2:31 · CC BY" caption (CC0/PD shows "No credit needed"), radio to pick.
- Picking a track downloads it to `filesDir/music/<openverseId>.<ext>` and adds it to My files with a small "CC BY" / "CC0" badge.
- Footer line: "Music from Openverse. CC0, public domain and CC BY only." Long-press a free track → "Report a problem" (opens its source page) / Remove.
- My files footer: "You're responsible for music you add from your files."

## Licence record (proof of good faith)
For every downloaded track store: openverseId, title, creator, creatorUrl, license, licenseVersion, licenseUrl, foreignLandingUrl (source page), provider, downloadedAt. DB migration + kept in backup manifest (metadata only, not the audio).

## Credit travels with the film
- If a film uses any CC BY track: the rendered film ends with a 1.5 s end card (black, 13 sp #B9B9B3, centred): "Music: <Title> — <Creator> · CC BY 4.0". Several tracks → one line each (max 3, then "+ N more").
- Share sheet: after Share/Save, a snackbar "Music credit copied — paste it in your post's description" and the text is on the clipboard: "Music: <Title> by <Creator> (<licenseUrl>) via <source page>".
- CC0/PD tracks: no end card needed (still stored in the record).
- Film details (player ⋯ → Info) lists the credits.

## Don'ts
- Never host or re-serve audio from our own server. Never bundle Openverse tracks in the APK.
- Never show NC/ND/SA tracks, even greyed out.

## Done
Unit tests: licence filter (cc0/pdm/by pass; by-nc, by-nd, by-sa, by-nc-sa, sampling+ rejected), credit text builder, end card presence only with CC BY. Screenshots of both tabs, an end card and the copied-credit snackbar in `qa/v1.8/`. Emulator/Xiaomi only, never the Samsung.
