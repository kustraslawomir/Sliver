# T-auto-3: design lint (fails the build)

`scripts/design-lint.sh`, run in `./gradlew check` and in CI. Fail with file:line when it finds:
- any of `FF8A3D`, `FFD2AE`, `Geist Mono`, `GeistMono`, `.glow(`, `Modifier.shadow(` used for
  colour light (allow normal elevation via an explicit `// lint-ok: elevation` comment)
- the words `second`/`seconds` in `strings.xml` UI copy (allow "1 s"-style durations)
- strings that are ALL CAPS in `strings.xml` (except the date-stamp format)
- `FontFamily.Default` or `SansSerif`
- `Color(0x` literals outside `core/designsystem/` (colours only from tokens)
- `Text(` with a `"SLIVER"` literal (the wordmark is a drawable)
Keep it a plain grep script (ripgrep), fast, with a clear message per rule.
