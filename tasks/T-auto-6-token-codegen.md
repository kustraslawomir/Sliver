# T-auto-6: tokens.json → Kotlin

- Gradle task `generateDesignTokens` (runs before compile) reads `design/tokens.json` and writes
  `core/designsystem/generated/Tokens.kt`: `DarkColors`, `LightColors` (from `themeDarkMap`,
  `themeLight`, `accent`), the record red, type scale (sizes, line heights, weights, tracking),
  spacing and radii. Generated file is not edited by hand (header comment says so).
- `SliverTheme` builds both `ColorScheme`s and the Typography only from the generated values.
- After this, changing the accent or a colour = edit `tokens.json`, rebuild. Nothing else.
Done when: deleting `Tokens.kt` and building regenerates it identically, and design-lint (T-auto-3)
finds no colour literals outside the design system.
