You are a senior product designer. Design a mobile app end-to-end, working in STAGES.

WORKFLOW RULE: complete ONE stage at a time, present the result with a short rationale, then STOP and wait for my approval or feedback ("go" / change requests) before starting the next stage. Do not skip ahead. At the end of every stage, list 2–3 decisions you want me to confirm.

════════════════════════════════════════
PRODUCT BRIEF (applies to all stages)
════════════════════════════════════════
App name: "SLIVER" (final, do not rename). A mobile app (iOS-first at 390×844, adaptable to Material 3 for Android) for a "1 second a day" video diary. Users capture ONE second of each day (or turn a photo into a 1s clip). The app stores them on a calendar and compiles them into a film (weekly / monthly / yearly / custom range).

Name concept: a sliver = a thin slice of life. Visual idea to explore: a thin vertical light slit / narrow frame as the core brand motif (logo, icon, trim window, loading, dividers).
Store subtitle: "Daily 1-Second Video Diary".
Tagline directions: "One second. Every day." / "A sliver of every day."

Target user: 18–35, aesthetically driven (VSCO, Lightroom, Letterboxd, Halide users). Wants a private, cinematic life archive — NOT a social network.

Three ways to add a day (all end in the same Trim & Pick screen):
 A. Record in app — any length, then trim.
 B. Import video from gallery — any length, then trim to 1s.
 C. Import photo(s) from gallery — becomes a 1s clip (subtle motion).

Core differentiators:
 1. Dark, cinematic aesthetic as the DNA (not an afterthought).
 2. Powerful, intuitive filters/looks (dark, high sharpness, grain, etc.).
 3. Extremely intuitive trimming with smart auto-selection of the best second.
 4. Capture in under 2 seconds from opening the app. Zero friction.
 5. Your data stays yours: backup goes to the user's own Google Drive.

VISUAL DIRECTION
- Dark-first (true black / near-black), no light mode in v1.
- Mood: A24 title cards, Leica/VSCO, Halide, Nothing OS. Moody, premium.
- One restrained accent color — compare an ember/amber option vs a cold electric violet option and recommend one.
- Type: expressive display face (condensed or serif, title-card feel) + clean grotesk/mono for UI. Mono for dates/timecodes ("DAY 142 · 00:01.00").
- Subtle film grain, soft glows, 1px hairline borders, large radii, generous negative space, haptic-feel microinteractions. Thin-line iconography.

UX PRINCIPLES
- One-handed: primary actions in the bottom 40% of the screen.
- Never homework: forgiving streaks, no guilt mechanics, supportive tone.
- The compiled film is the emotional payoff — treat it like a premiere.
- Privacy-first: no feed, no followers; sharing is opt-in; local by default.
- Accessibility: contrast on dark, dynamic type, reduced-motion alternatives.

ACCOUNT, BACKUP & NOTIFICATIONS
- Auth via Firebase: Sign in with Google, Sign in with Apple, email/password. GUEST-FIRST: the user can capture immediately as a guest; account creation is offered later (e.g. when enabling backup) and guest data is linked to the new account seamlessly. Never block the first capture with a login wall.
- Backup: media is backed up to the USER'S OWN Google Drive (private folder in their account), not to our servers. Make this a visible trust feature: "Your seconds live in your Drive. We never store your videos." Only lightweight metadata (account, settings, day→file mapping) is kept in Firebase. Restore on a new device must be a first-class flow.
- Notifications: (a) local daily reminders — fixed time or random-moment window, (b) push for system events: film ready, backup done/failed, streak at risk, "On this day" memories. Tone: warm, short, never guilt-tripping.

════════════════════════════════════════
STAGE 1 — BRAND
════════════════════════════════════════
- Logo/wordmark for SLIVER: 3 directions built around the "thin slice / light slit" motif, then recommend one.
- 3 app icon variants for the recommended direction.
- Accent color comparison (ember/amber vs electric violet) on a sample screen.
- Moodboard description (references, textures, lighting, typography feel).
- Tagline options (3).
STOP. Wait for my approval.

════════════════════════════════════════
STAGE 2 — DESIGN SYSTEM
════════════════════════════════════════
Using the approved brand: color tokens (surfaces, text, accent, states), type scale and pairing, spacing, radii, elevation/glow, grain/texture usage, iconography, motion principles (curves, durations, haptics), and a component library: buttons, record button, chips, sliders, tab bar, bottom sheets, cards, calendar cell states (empty / filled / today / missed), toasts, coach marks, the signature "sliver" selection frame. Present as a single design-system page.
STOP. Wait for my approval.

════════════════════════════════════════
STAGE 3 — ONBOARDING, CAPTURE & TRIM (hero flow)
════════════════════════════════════════
3.1 Onboarding (max 3 screens + optional steps): one-line concept each; permission asks (camera, mic, photos, notifications) each with a friendly "why" pre-prompt shown BEFORE the system dialog; reminder setup (fixed time or random-moment window, with preview of the notification); optional "Create account to back up" step with a clear "Skip, start as guest" option; optional "Backfill your last week from gallery" so the calendar is not empty on day one.

3.2 CAMERA (home screen, app opens here):
- Full-bleed viewfinder, minimal chrome. PHOTO / VIDEO mode switch.
- Record: tap to start/stop for free-length recording (also hold-to-record for quick moments), subtle running timer. No 1s limit while recording.
- Gallery shortcut = small thumbnail of latest gallery item (bottom-left).
- Live filter preview (swipe or bottom strip), flip, flash, grid, timer.
- Top bar: date + streak, subtle. State when today is already captured (thumbnail + replace/retake).

3.3 GALLERY PICKER (custom, not the plain system one): recents, favorites, videos/photos filter, multi-select for photos, "for [date]" indicator.

3.4 TRIM & PICK — THE HERO SCREEN. Must be understandable with zero instructions and doable in under 5 seconds.
- Top: large looping preview of the selected 1.0s (live filter applied), audio toggle.
- Middle: filmstrip of the full clip with a FIXED-WIDTH 1-second selection window (the glowing "sliver" frame in accent color). Window is draggable, strip scrolls beneath, haptic ticks, snap to interesting moments. Pinch to zoom for long videos.
- Bottom: huge CTA "Use this second" + secondary: Retake, Filters, Replace day.
- SMART AUTO-SELECT: window is pre-placed on the best second (motion, faces, audio peaks, sharpness, scene change). Badge "✦ Auto-picked" + "Try another" cycling the top 3 suggestions, shown as small glowing markers on the strip. Dragging switches the badge to "Custom" — user always in control.
- Audio: keep original audio on/off for this second. Optional speed (0.5x/1x) while output stays 1.0s.
- Optional one-line note or voice note for the day (so context survives years later).
- Photo variant: "Photo → 1s" with motion chips (Still, Slow zoom in, Slow zoom out, Pan, Parallax) and live loop preview; no timeline.
- Edge cases: clip shorter than 1s, very long video (several minutes), multi-photo import (fast carousel to assign each photo to a date).
- Microinteractions: window "locks in" with glow pulse + haptic; after "Use this second" the clip visibly drops into today's calendar cell; undo toast.
- One-time coach mark: "Drag to choose your second."

Deliver as a clickable prototype: Camera → (Record | Gallery) → Trim & Pick → Save, plus photo and multi-import variants.
STOP. Wait for my approval.

════════════════════════════════════════
STAGE 4 — FILTERS & LOOKS
════════════════════════════════════════
- Post-capture editor: looping preview, preset strip, adjustment panel.
- 8 preset looks with names and swatches: Noir, Blackout, Night Neon, Cold Steel, Ember, VHS 94, Hard Light, Ghost (refine names if you have better ones). Per-preset intensity slider.
- Sliders: Sharpness (feature it prominently), Contrast, Shadows, Highlights, Grain, Vignette, Fade, Temperature, Saturation. Reset per slider.
- Hold-to-compare before/after gesture. Option "Set as default look".
- Show the same sample frame across all 8 looks in one overview board.
STOP. Wait for my approval.

════════════════════════════════════════
STAGE 5 — ARCHIVE: CALENDAR, DAY DETAIL, MEMORIES
════════════════════════════════════════
- Calendar: month grid where each day is that day's clip thumbnail (mosaic), empty days as dim dots, today in accent. Views: Grid / Year heatmap / Vertical film-strip timeline.
- Tap empty day → "Add from gallery" → Trim & Pick for that specific date.
- Day detail: loop playback, look used, note/caption, location, mood tag, replace, share single clip.
- "On this day" memories surface (a year ago today) as a card on the calendar/home.
- Missed-day and broken-streak states (supportive tone). Backfill entry point.
STOP. Wait for my approval.

════════════════════════════════════════
STAGE 6 — FILM BUILDER, PREMIERE & YEAR IN SECONDS
════════════════════════════════════════
- Compile: range picker (week / month / year / custom), include/exclude days.
- Options: music (library + own), AUDIO MODE per film (original sound only / music only / mix with automatic ducking of music under original audio), transitions (hard cut, flash cut, glitch, fade-through-black), date overlay styles (mono timecode, title cards), optional day notes as subtitles, aspect ratio (9:16, 1:1, 16:9), one master look over the whole film, end card.
- Live preview player with scrubber showing day markers.
- Export: progress, then the "premiere" reveal (title card with year/date range) and share sheet (Instagram, TikTok, Photos, link).
- "Your Year in Seconds": a special end-of-year (late December) flow and shareable cinematic recap, designed as the biggest emotional + sales moment of the app.
- Films library: poster-style cards with runtime and date range.
STOP. Wait for my approval.

════════════════════════════════════════
STAGE 7 — ACCOUNT, BACKUP & NOTIFICATIONS
════════════════════════════════════════
7.1 Auth screens: welcome/sign-in sheet (Google, Apple, email), guest-to-account upgrade sheet ("Save your seconds"), sign-out, delete account (clear, honest data-deletion flow).
7.2 Google Drive backup:
- Connect flow: explain permissions in plain language BEFORE Google's consent screen; show which folder is used and that only the app's files are accessed.
- Backup settings screen: status (last backup, items backed up / pending, storage used in Drive), toggles: auto-backup, Wi-Fi only, originals vs trimmed 1s clips only, backup while charging.
- States: first backup in progress (resumable), up to date, paused (no Wi-Fi), failed (retry), Drive storage full, permission revoked / token expired (re-connect), offline.
- Restore flow: new device → sign in → "We found 214 seconds in your Drive" → restore progress → calendar populates with a fill-in animation.
- Manual actions: Back up now, Disconnect Drive, Export everything.
7.3 Notifications:
- Settings: daily reminder (fixed time / random window), days of week, quiet hours, toggles per type (film ready, backup status, streak at risk, On this day).
- Design notification visuals for iOS lock screen and Android: daily reminder, random-moment version, film ready (poster thumbnail), streak at risk, backup failed, On this day (that day's clip thumbnail). Include warm, short copy for each.
- Permission-denied state with a gentle path to re-enable; in-app fallback banner when notifications are off.
STOP. Wait for my approval.

════════════════════════════════════════
STAGE 8 — WIDGETS, SETTINGS, MONETIZATION, STATES
════════════════════════════════════════
- Widgets: home-screen widget (today's status + streak + tap to open camera) in small/medium sizes; lock-screen widget/shortcut straight to the camera (iOS lock screen widget + Android equivalent).
- Settings: Account (profile, linked providers, sign out, delete account), Backup (summary row → 7.2), Notifications (summary row → 7.3), default look, video quality, privacy, Pro card.
- Paywall: tasteful, dark. Free = watermark + 720p + manual backup of trimmed clips. Pro = unlimited films, premium looks, 4K export, no watermark, auto-backup, originals, cross-device restore.
- Edge states: first day, permission denied, export failed, storage full.
- Splash/launch animation concept built on the "sliver" motif.
STOP. Wait for my approval.

════════════════════════════════════════
STAGE 9 — FINAL PACKAGE
════════════════════════════════════════
- Assemble everything into one clickable end-to-end prototype: Onboarding → Camera → Trim & Pick → Filters → Save → Calendar → Film Builder → Export, plus the Backup/Restore path.
- One-page summary of key design decisions and rationale.
- Developer handoff notes: tokens, component specs, animation specs, suggested implementation: on-device auto-pick via AVFoundation/Vision on iOS and MediaPipe/ML Kit on Android; local-first storage; Firebase Auth (anonymous → linked account); Google Drive API with narrowest scope (drive.file, visible app folder; alternative appDataFolder); resumable chunked background uploads (BGTaskScheduler / WorkManager); local notifications for reminders vs FCM for server events; Android 13+ and iOS notification permission handling; Firestore for metadata only; real-time filter rendering considerations.

Begin with STAGE 1 now. Do not start Stage 2 until I say "go".
