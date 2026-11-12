"""Similarity of Roborazzi dark goldens to design/png boards. usage: python3 tools/design-report.py [threshold] [out.md]
Both images are scaled to 195x422 grayscale; score = 1 - mean absolute difference / 255.
Below the threshold is a WARNING only (fonts, synthetic thumbnails and seeded data differ), never a failure.
Needs Pillow."""
import sys
from pathlib import Path
from PIL import Image, ImageChops, ImageStat

ROOT = Path(__file__).resolve().parent.parent
SNAPS = ROOT / "composeApp/src/test/snapshots"
DESIGN = ROOT / "design/png"
threshold = float(sys.argv[1]) if len(sys.argv) > 1 else 0.85
out = Path(sys.argv[2]) if len(sys.argv) > 2 else ROOT / "composeApp/build/reports/design-similarity.md"
SIZE = (195, 422)

def gray(p):
    return Image.open(p).convert("L").resize(SIZE, Image.LANCZOS)

rows = []
for snap in sorted(SNAPS.glob("*-dark.png")):
    name = snap.name.removesuffix("-dark.png")
    board = DESIGN / f"{name}.png"
    if not board.exists():
        rows.append((name, None))
        continue
    diff = ImageChops.difference(gray(snap), gray(board))
    score = 1 - ImageStat.Stat(diff).mean[0] / 255
    rows.append((name, score))

rows.sort(key=lambda r: (r[1] is None, r[1] if r[1] is not None else 0))
lines = [f"# Design similarity (threshold {threshold})", "", "| Screen | Score | |", "|---|---|---|"]
warn = 0
for name, score in rows:
    if score is None:
        lines.append(f"| {name} | n/a | no design/png/{name}.png |")
    else:
        bad = score < threshold
        warn += bad
        lines.append(f"| {name} | {score:.3f} | {'WARNING' if bad else 'ok'} |")
lines += ["", f"{warn} of {len(rows)} below threshold (warnings only)."]
text = "\n".join(lines)
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(text + "\n")
print(text)
