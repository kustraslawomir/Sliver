# P-brand — Brand assets + record button

Follow PROMPT.md. Read design/INDEX.md ("Added in v1.2"), android-res/README.md, brand/README.md.
NOTE: this repo has no androidApp module; the Android resources live in composeApp/src/androidMain/res and the manifest in composeApp/src/androidMain/AndroidManifest.xml. Use those paths wherever the brief says androidApp.

1. APP ICON: copy android-res/ into composeApp/src/androidMain/res exactly (merge with existing values/ etc.; do not redraw). Delete the template's default ic_launcher* files and fix manifest references (icon, roundIcon).
2. SPLASH: Theme.Sliver.Splash on the launcher activity; installSplashScreen() before super.onCreate() (add androidx.core:core-splashscreen if missing, say so).
3. NOTIFICATIONS: setSmallIcon(R.drawable.ic_stat_sliver) on every notification (reminders, progress/live updates, backup problem, film ready).
4. LOGO: @drawable/sliver_wordmark exists; never write "SLIVER" as a styled Text() logo. Replace any such Text.
5. RECORD BUTTON = option D "Ember" per design/INDEX.md and CameraV2.dc.html / CameraV2Rec.dc.html / CameraV2Land.dc.html (+ RecordButtons.dc.html):
   - composable RecordButton(state: Photo | VideoIdle | Recording)
   - 72 dp disc in 80 dp hit area; glow via drawBehind radial gradient (no blur)
   - Recording: 24 dp dark stop square, 1.6 s breathing glow, off when system animations are disabled
   - while recording hide ADD, flip, tools and mode switch
   - timer pill: grey until 1 s, then ember + "CLIP OK" + ONE haptic tick (HapticFeedbackConstants.CONFIRM)
   - tap again = stop -> Edit clip

DONE WHEN: screenshots in docs/qa/brand/ from emulator-5554: home screen (themed icons off/on), splash, a notification, camera idle, camera recording <1 s and >1 s, landscape. Compare with design/src via tools/compare.py where a render exists and list anything that does not match.
