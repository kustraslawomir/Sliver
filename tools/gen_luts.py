#!/usr/bin/env python3
"""Generates 33^3 .cube LUTs for the look presets. Requires numpy only.

Usage: python3 tools/gen_luts.py [out_dir]
Default out_dir: composeApp/src/androidMain/assets/luts
"""
import sys
from pathlib import Path

import numpy as np

SIZE = 33
LUMA = np.array([0.2126, 0.7152, 0.0722])

# name: (grayscale, contrast, brightness, saturation, sepia, hue_deg, tint_hex, tint_amount)
RECIPES = {
    "noir": dict(gray=1.0, contrast=1.35, brightness=0.92),
    "blackout": dict(contrast=1.6, brightness=0.7, sat=0.75),
    "night_neon": dict(sat=1.7, contrast=1.2, brightness=0.88, hue=-12, tint=("7A2AC8", 0.45)),
    "cold_steel": dict(sat=0.45, contrast=1.18, tint=("3A8AC8", 0.55)),
    "ember": dict(sepia=0.3, sat=1.35, contrast=1.08, tint=("FF7A2A", 0.45)),
    "vhs_94": dict(sat=1.35, contrast=0.85, brightness=1.05, tint=("B04AC8", 0.20)),
    "hard_light": dict(contrast=1.65, brightness=1.06, sat=1.12),
    "ghost": dict(gray=0.65, brightness=1.15, contrast=0.72),
}

SEPIA = np.array([[0.393, 0.769, 0.189], [0.349, 0.686, 0.168], [0.272, 0.534, 0.131]])


def luma(c):
    return (c @ LUMA)[:, None]


def hue_rotate(c, deg):
    a = np.radians(deg)
    cos, sin = np.cos(a), np.sin(a)
    m = np.array([
        [0.213 + cos * 0.787 - sin * 0.213, 0.715 - cos * 0.715 - sin * 0.715, 0.072 - cos * 0.072 + sin * 0.928],
        [0.213 - cos * 0.213 + sin * 0.143, 0.715 + cos * 0.285 + sin * 0.140, 0.072 - cos * 0.072 - sin * 0.283],
        [0.213 - cos * 0.213 - sin * 0.787, 0.715 - cos * 0.715 + sin * 0.715, 0.072 + cos * 0.928 + sin * 0.072],
    ])
    return c @ m.T


def soft_light(base, blend):
    return np.where(
        blend <= 0.5,
        base - (1 - 2 * blend) * base * (1 - base),
        base + (2 * blend - 1) * (np.sqrt(base) - base),
    )


def apply(c, gray=0.0, contrast=1.0, brightness=1.0, sat=1.0, sepia=0.0, hue=0.0, tint=None):
    if gray:
        c = c * (1 - gray) + luma(c) * gray
    if sepia:
        c = c * (1 - sepia) + np.clip(c @ SEPIA.T, 0, 1) * sepia
    if hue:
        c = hue_rotate(c, hue)
    if sat != 1.0:
        c = luma(c) + (c - luma(c)) * sat
    c = np.clip(c, 0, 1)
    c = (c - 0.5) * contrast + 0.5
    c = np.clip(c * brightness, 0, 1)
    if tint:
        hx, amt = tint
        t = np.array([int(hx[i:i + 2], 16) / 255 for i in (0, 2, 4)])
        c = c * (1 - amt) + soft_light(c, np.broadcast_to(t, c.shape)) * amt
    return np.clip(c, 0, 1)


def write_cube(path, name, recipe):
    axis = np.linspace(0, 1, SIZE)
    # .cube order: red fastest, then green, then blue
    b, g, r = np.meshgrid(axis, axis, axis, indexing="ij")
    rgb = np.stack([r.ravel(), g.ravel(), b.ravel()], axis=1)
    out = apply(rgb, **recipe)
    lines = [f'TITLE "{name}"', f"LUT_3D_SIZE {SIZE}", "DOMAIN_MIN 0 0 0", "DOMAIN_MAX 1 1 1"]
    lines += [f"{x:.6f} {y:.6f} {z:.6f}" for x, y, z in out]
    path.write_text("\n".join(lines) + "\n")


def main():
    root = Path(__file__).resolve().parent.parent
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else root / "composeApp/src/androidMain/assets/luts"
    out.mkdir(parents=True, exist_ok=True)
    for name, recipe in RECIPES.items():
        write_cube(out / f"{name}.cube", name, recipe)
        print("wrote", out / f"{name}.cube")


if __name__ == "__main__":
    main()
