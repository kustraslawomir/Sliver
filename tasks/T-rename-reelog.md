# T-rename-reelog — the app is called Reelog

Read: `design/INDEX.md` sections v1.5 + v1.6, `CLAUDE.md` "Test device". Do this BEFORE `tasks/T-design-v1.5.md` (that task uses the name).

## Do, one commit per item
1. **Name.** `app_name` = "Reelog" (Android `strings.xml`, Compose `Res.string`, launcher label, widget + QS tile labels, notification channel names, share-sheet / export file names "Reelog …", Drive folder name for NEW installs = "Reelog"). Grep for "Fragments" and "SLIVER" in user-facing strings and fix all of them. `docs/FUNCTIONAL.md`: replace the identity line.
2. **Keep the ids.** Do NOT change `applicationId` (`skustra.fragments`), the Kotlin namespace `app.sliver.*`, the OAuth client or the Drive `appProperties` keys. Changing the applicationId breaks the Google sign-in client and every existing install's backup. Existing Drive folder named "Fragments": keep using it (find by appProperties id, not by name); do not rename it.
3. **Icon.** Replace launcher foreground / monochrome with `brand/reelog/ic_launcher_*.xml`, background #0A0A0B. Regenerate legacy PNG mipmaps from `brand/reelog/reelog-icon.svg`. Play icon 512 px from the same SVG.
4. **Logo.** Add `brand/reelog/reelog_wordmark.xml` as `R.drawable.reelog_wordmark`; use it in Settings footer (height 20dp) and on the onboarding welcome above the headline only if it fits the board — otherwise leave the red dot.
5. **Splash.** Implement as in INDEX v1.6: `installSplashScreen()` with `splash_icon.xml` on #0A0A0B, then `setOnExitAnimationListener` → a Compose overlay that runs the 1.5 s sequence (`design/motion/SplashReelog.html` → `frame(t)` has the exact maths) and ends with the disc landing on the Record today button. Skip the exit animation when the start destination is not the Project screen (onboarding) — then just fade. Respect reduced motion.
6. **Store text** in `docs/store/`: title "Reelog: 1s Video Diary", short description "One clip a day. One film a year."

## Done when
- No "Fragments" / "SLIVER" left in any user-visible string (`grep -ri` over `composeApp/src/**/res` and string resources; add it to `scripts/design-lint.sh`).
- Splash recorded on the Xiaomi `b798509a` (or emulator `tracker13`) with `adb shell screenrecord`, saved to `docs/qa/reelog/splash.mp4`, compared side by side with `design/motion/SplashReelog.mp4`. **Never the Samsung.**
- `./gradlew check` green, goldens re-recorded.
