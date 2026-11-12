"""Writes <qa dir>/REPORT.md: per screen and theme-scale a similarity score against design/png and an
overlay from tools/compare.py. usage: python3 -I qa/report.py docs/qa/<date>
Score = 1 - mean absolute difference of 195x422 grayscale thumbnails (same as tools/design-report.py).
Light screenshots are compared with <Screen>L.png when that board exists, otherwise n/a."""
import subprocess
import sys
from pathlib import Path
from PIL import Image, ImageChops, ImageStat

ROOT = Path(__file__).resolve().parent.parent
DESIGN = ROOT / "design/png"
out = Path(sys.argv[1]).resolve()
SIZE = (195, 422)


def gray(p):
    return Image.open(p).convert("L").resize(SIZE, Image.LANCZOS)


def board(name, theme):
    for cand in ([f"{name}L.png", f"{name}.png"] if theme == "light" else [f"{name}.png"]):
        if (DESIGN / cand).exists():
            if theme == "light" and cand == f"{name}.png":
                return None
            return DESIGN / cand
    return None


combos = sorted(d for d in out.iterdir() if d.is_dir() and d.name != "compare" and any(d.glob("*.png")))
screens = sorted({p.stem for d in combos for p in d.glob("*.png")})
rel = lambda p: p.relative_to(out).as_posix()
head = {}
for line in (out / "run.txt").read_text().splitlines() if (out / "run.txt").exists() else []:
    k, _, v = line.partition(": ")
    head[k] = v

lines = ["# Device QA report", ""]
lines += [f"- {k}: {v}" for k, v in head.items()]
lines += ["", "Score: 1.000 = identical to the design board. Data is synthetic (seeder), fonts and thumbnails differ, so scores are indicative only.", ""]
lines += ["| Screen | " + " | ".join(d.name for d in combos) + " |", "|---|" + "---|" * len(combos)]
for s in screens:
    cells = []
    for d in combos:
        shot = d / f"{s}.png"
        if not shot.exists():
            cells.append("missing")
            continue
        b = board(s, "light" if d.name.startswith("light") else "dark")
        if b is None:
            cells.append(f"[shot]({rel(shot)}) n/a")
            continue
        score = 1 - ImageStat.Stat(ImageChops.difference(gray(shot), gray(b))).mean[0] / 255
        ov = out / "compare" / d.name / f"{s}.png"
        ov.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run([sys.executable, str(ROOT / "tools/compare.py"), str(shot), str(b), str(ov)],
                       check=True, stdout=subprocess.DEVNULL)
        cells.append(f"{score:.3f} ([shot]({rel(shot)}), [overlay]({rel(ov)}))")
    lines.append(f"| {s} | " + " | ".join(cells) + " |")

flows = out / "flows.txt"
if flows.exists():
    lines += ["", "## Flows", "", "```", flows.read_text().rstrip(), "```"]
lines += ["", "Not screenshotted by design (privacy): camera, recording, edit clip, system Photo Picker, projects sheet when real projects exist."]
(out / "REPORT.md").write_text("\n".join(lines) + "\n")
print(f"wrote {out / 'REPORT.md'}")
