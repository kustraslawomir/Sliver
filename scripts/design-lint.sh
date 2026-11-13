#!/usr/bin/env bash
# Design lint: fails with file:line on forbidden design patterns. Uses ripgrep if present, else grep.
cd "$(dirname "$0")/.." || exit 2
SRC=composeApp/src
RES=composeApp/src/main/res
FAIL=0

# search <pattern> <ext-regex> : prints "file:line:text" for matches in $SRC.
search() {
  if command -v rg >/dev/null 2>&1; then
    rg -n --no-heading -e "$1" -g "*.$2" $SRC 2>/dev/null
  else
    grep -rnE --include="*.$2" -e "$1" $SRC 2>/dev/null
  fi
}

# report <message> : reads matches on stdin, drops allowlisted lines, prints rest.
report() {
  local out
  out=$(grep -vE "$ALLOW_FILES" | grep -v "lint-ok" || true)
  [ -n "$ALLOW_EXTRA" ] && out=$(printf '%s\n' "$out" | grep -vE "$ALLOW_EXTRA" || true)
  out=$(printf '%s\n' "$out" | sed '/^$/d')
  if [ -n "$out" ]; then
    echo "DESIGN LINT: $1"; printf '%s\n' "$out" | sed 's/^/  /'; FAIL=1
  fi
}

# Brand assets (icon, splash, logo, shortcut, widget) intentionally keep #FF8A3D (v1.4: "Brand stays as is").
ALLOW_FILES='ic_launcher_foreground|ic_shortcut_slit|widget_slit|/brand/'
ALLOW_EXTRA=''

ALLOW_EXTRA='' ; report "legacy ember colours FF8A3D/FFD2AE are forbidden; use tokens" < <(search 'FF8A3D|FFD2AE' '(kt|xml)')
# Geist Mono is allowed only for the burned-in date stamp (SliverType.Stamp) and its font definition.
ALLOW_EXTRA='Stamp' ; report "Geist Mono is forbidden (only SliverType.Stamp may use it)" < <(search 'Geist ?Mono|GeistMono' 'kt')
ALLOW_EXTRA='' ; report ".glow( is forbidden" < <(search '\.glow\(' 'kt')
ALLOW_EXTRA='' ; report "shadow( as colour light is forbidden; add '// lint-ok: elevation' for real elevation" < <(search 'Modifier\.shadow\(|\.shadow\(' 'kt')
ALLOW_EXTRA='' ; report "FontFamily.Default / SansSerif are forbidden; use SliverTheme.typography" < <(search 'FontFamily\.Default|SansSerif' 'kt')
ALLOW_EXTRA='core/(designsystem|theme)/' ; report "Color(0x literals only allowed in core/designsystem or core/theme (token files); use tokens" < <(search 'Color\(0x' 'kt')
ALLOW_EXTRA='' ; report "wordmark must be a drawable, not Text(\"SLIVER\")" < <(search 'Text\([^)]*"SLIVER"' 'kt')
# The app name comes from design/tokens.json (APP_NAME / @string/app_name); old names must not reach the UI.
ALLOW_EXTRA='LEGACY_DRIVE_FOLDER_NAMES|/(commonTest|androidUnitTest|debug)/' ; report "old app name in a user-visible string; use APP_NAME" < <(search '"[^"]*(Fragments|SLIVER)[^"]*"' 'kt')
ALLOW_EXTRA='' ; report "old app name in a resource; use @string/app_name" < <(search '>[^<]*(Fragments|SLIVER)[^<]*<|label="(Fragments|SLIVER)' 'xml')
# v1.5: no mono uppercase labels. uppercase() only for the first letter; all-caps literals only for formats (MP4, HDR…).
ALLOW_EXTRA='replaceFirstChar|stampText|Stamp' ; report "uppercase() on UI text is forbidden (sentence case, Geist)" < <(search '\.uppercase\(\)' 'kt')
ALLOW_EXTRA='LEGACY_DRIVE_FOLDER_NAMES|const val|@Suppress|"(MP4|HDR|HEIC|JPEG|UTC|GET|POST|PUT|PATCH|DELETE)"|/(commonTest|androidUnitTest|debug)/|/data/' ; report "ALL CAPS text in UI code; use sentence case" < <(search '"[A-Z][A-Z ·]{2,}[A-Z]"' 'kt')

# v1.7 type scale: sizes live only in core/theme/Type.kt (ReelogType); nothing under 13 sp.
ALLOW_EXTRA='core/theme/Type\.kt|/(commonTest|androidUnitTest|debug)/' ; report "raw fontSize/lineHeight literal; use ReelogType" < <(search '(fontSize|lineHeight) *= *[0-9.]+\.sp' 'kt')
ALLOW_EXTRA='core/theme/Type\.kt|letterSpacing|/(commonTest|androidUnitTest|debug)/' ; report "text size under 13 sp" < <(search '[^0-9.a-zA-Z_](1[0-2]|[0-9])(\.[0-9]+)?\.sp' 'kt')

# strings.xml copy rules
STR=$(find $SRC -name strings.xml -path '*values/*' 2>/dev/null)
if [ -n "$STR" ]; then
  # shellcheck disable=SC2086
    W=$(grep -nEi '>[^<]*\bseconds?\b[^<]*<' $STR | sed 's/^/  /' || true)
  [ -n "$W" ] && { echo 'DESIGN LINT: "second(s)" in strings.xml; say "clip" or use "1 s"'; echo "$W"; FAIL=1; }
  # ALL CAPS strings (3+ letters, no lowercase), excluding the date-stamp format.
  C=$(grep -nE '<string [^>]*>[^a-z<]*[A-Z]{3,}[^a-z<]*</string>' $STR | grep -vi 'stamp' | sed 's/^/  /' || true)
  [ -n "$C" ] && { echo "DESIGN LINT: ALL CAPS string in strings.xml"; echo "$C"; FAIL=1; }
fi

[ $FAIL -eq 0 ] && echo "design-lint: OK"
exit $FAIL
