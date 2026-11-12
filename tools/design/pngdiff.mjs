// Pixel similarity of two PNGs (8-bit RGB/RGBA, non-interlaced). Node built-ins only.
// usage: node pngdiff.mjs <a.png> <b.png> [tol=16]  -> prints "<similarity %> <meanAbsDiff> <note>"
import fs from 'node:fs';
import zlib from 'node:zlib';

export function decode(file) {
  const buf = fs.readFileSync(file);
  let p = 8, w, h, ct, idat = [];
  while (p < buf.length) {
    const len = buf.readUInt32BE(p), type = buf.toString('latin1', p + 4, p + 8), d = buf.subarray(p + 8, p + 8 + len);
    if (type === 'IHDR') { w = d.readUInt32BE(0); h = d.readUInt32BE(4); ct = d[9]; if (d[8] !== 8 || d[12] !== 0) throw new Error('unsupported PNG'); }
    if (type === 'IDAT') idat.push(d);
    p += 12 + len;
  }
  const bpp = ct === 6 ? 4 : ct === 2 ? 3 : 0;
  if (!bpp) throw new Error('unsupported color type ' + ct);
  const raw = zlib.inflateSync(Buffer.concat(idat));
  const stride = w * bpp, out = Buffer.alloc(h * stride);
  for (let y = 0; y < h; y++) {
    const f = raw[y * (stride + 1)], src = y * (stride + 1) + 1, dst = y * stride;
    for (let x = 0; x < stride; x++) {
      const a = x >= bpp ? out[dst + x - bpp] : 0, b = y ? out[dst - stride + x] : 0, c = x >= bpp && y ? out[dst - stride + x - bpp] : 0;
      let v = raw[src + x];
      if (f === 1) v += a; else if (f === 2) v += b; else if (f === 3) v += (a + b) >> 1;
      else if (f === 4) { const pp = a + b - c, pa = Math.abs(pp - a), pb = Math.abs(pp - b), pc = Math.abs(pp - c); v += pa <= pb && pa <= pc ? a : pb <= pc ? b : c; }
      out[dst + x] = v & 255;
    }
  }
  return { w, h, bpp, data: out };
}

export function compare(fa, fb, tol = 16) {
  const a = decode(fa), b = decode(fb);
  if (a.w !== b.w || a.h !== b.h) return { sim: 0, mad: 255, note: `size ${a.w}x${a.h} vs ${b.w}x${b.h}` };
  let ok = 0, sum = 0;
  for (let i = 0; i < a.w * a.h; i++) {
    let m = 0;
    for (let c = 0; c < 3; c++) { const d = Math.abs(a.data[i * a.bpp + c] - b.data[i * b.bpp + c]); sum += d; if (d > m) m = d; }
    if (m <= tol) ok++;
  }
  return { sim: (100 * ok) / (a.w * a.h), mad: sum / (3 * a.w * a.h), note: '' };
}

if (process.argv[1] && process.argv[1].endsWith('pngdiff.mjs')) {
  const r = compare(process.argv[2], process.argv[3], Number(process.argv[4] || 16));
  console.log(r.sim.toFixed(2), r.mad.toFixed(2), r.note);
}
