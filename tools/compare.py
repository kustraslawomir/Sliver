"""Side-by-side + 50% overlay of an app screenshot and a design PNG.
usage: python3 tools/compare.py app.png design.png out.png
Both are scaled to the same width (1170 px = 390 dp @3x). Needs Pillow (pip install pillow).
A red 8 dp grid is drawn on the overlay to help read offsets."""
import sys
from PIL import Image, ImageDraw

W = 1170
def load(p):
    im = Image.open(p).convert("RGB")
    return im.resize((W, round(im.height * W / im.width)))

app, des = load(sys.argv[1]), load(sys.argv[2])
h = max(app.height, des.height)
over = Image.blend(app.crop((0, 0, W, h)), des.crop((0, 0, W, h)), 0.5)
d = ImageDraw.Draw(over)
for x in range(0, W, 24):
    d.line([(x, 0), (x, h)], fill=(255, 0, 0) if x % 120 == 0 else (90, 0, 0))
for y in range(0, h, 24):
    d.line([(0, y), (W, y)], fill=(255, 0, 0) if y % 120 == 0 else (90, 0, 0))
out = Image.new("RGB", (W * 3 + 80, h), (20, 20, 20))
out.paste(app, (0, 0)); out.paste(des, (W + 40, 0)); out.paste(over, (2 * W + 80, 0))
out.save(sys.argv[3])
print("app | design | overlay  ->", sys.argv[3])
