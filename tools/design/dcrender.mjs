// Static renderer for design/src/*.dc.html boards. Node built-ins only.
// usage: node dcrender.mjs <Board> <out.html>   (prints "<width> <height> <scale>")
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const SRC = path.resolve(here, '../../design/src');
const FONTS = path.resolve(here, '../../composeApp/src/commonMain/composeResources/font');
const SVGVOID = new Set(['path', 'circle', 'rect', 'line', 'ellipse', 'polygon', 'polyline', 'stop']);
const VOID = new Set(['area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input', 'link', 'meta', 'source', 'track', 'wbr', ...SVGVOID]);
const RAW = new Set(['script', 'style']);

function parse(html) {
  const root = { tag: '#root', attrs: [], children: [] };
  const stack = [root];
  const re = /<!--[\s\S]*?-->|<!doctype[^>]*>|<\/([a-zA-Z][\w:-]*)\s*>|<([a-zA-Z][\w:-]*)((?:\s+[^\s"'<>\/=]+(?:\s*=\s*(?:"[^"]*"|'[^']*'|[^\s"'>]+))?)*)\s*(\/?)>/gi;
  let last = 0, m;
  const top = () => stack[stack.length - 1];
  const text = (s) => { if (s) top().children.push({ text: s }); };
  while ((m = re.exec(html))) {
    text(html.slice(last, m.index));
    last = re.lastIndex;
    if (m[1]) {
      const name = m[1].toLowerCase();
      for (let i = stack.length - 1; i > 0; i--) if (stack[i].tag === name) { stack.length = i; break; }
    } else if (m[2]) {
      const tag = m[2].toLowerCase();
      const attrs = [];
      const ar = /([^\s"'<>\/=]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+)))?/g;
      let a;
      while ((a = ar.exec(m[3]))) attrs.push([a[1], a[2] ?? a[3] ?? a[4] ?? '']);
      const el = { tag, attrs, children: [] };
      top().children.push(el);
      if (RAW.has(tag) && !m[4]) {
        const end = html.toLowerCase().indexOf('</' + tag, re.lastIndex);
        el.children.push({ text: html.slice(re.lastIndex, end) });
        re.lastIndex = html.indexOf('>', end) + 1;
        last = re.lastIndex;
      } else if (!m[4] && !VOID.has(tag)) stack.push(el);
    }
  }
  text(html.slice(last));
  return root;
}

const find = (n, pred) => {
  if (pred(n)) return n;
  for (const c of n.children || []) { const r = find(c, pred); if (r) return r; }
  return null;
};
const attr = (el, name) => (el.attrs.find((a) => a[0] === name) || [])[1];

class DCLogic {
  constructor(props) { this.props = props || {}; this.state = {}; }
  setState() {}
}

const cache = new Map();
function load(name) {
  if (cache.has(name)) return cache.get(name);
  const tree = parse(fs.readFileSync(path.join(SRC, name + '.dc.html'), 'utf8'));
  const xdc = find(tree, (n) => n.tag === 'x-dc');
  const script = find(tree, (n) => n.tag === 'script' && attr(n, 'data-dc-script') !== undefined);
  const defs = JSON.parse(attr(script, 'data-props') || '{}');
  const Cls = new Function('DCLogic', script.children[0].text + '\nreturn Component;')(DCLogic);
  const comp = { name, xdc, defs, Cls };
  cache.set(name, comp);
  return comp;
}

const camel = (s) => s.replace(/-([a-z])/g, (_, c) => c.toUpperCase());
const HOLE = /\{\{([^}]*)\}\}/g;

function lookup(expr, scope) {
  expr = expr.trim();
  if (/^-?\d+(\.\d+)?$/.test(expr)) return Number(expr);
  if (expr === 'true') return true;
  if (expr === 'false') return false;
  if (expr === 'null' || expr === 'undefined') return undefined;
  if (/^(['"]).*\1$/.test(expr)) return expr.slice(1, -1);
  const parts = expr.split('.');
  let v;
  for (let i = scope.length - 1; i >= 0; i--) if (parts[0] in scope[i]) { v = scope[i][parts[0]]; break; }
  for (const p of parts.slice(1)) v = v == null ? undefined : v[p];
  return v;
}
const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
const escAttr = (s) => esc(s).replace(/"/g, '&quot;');
const whole = (s) => { const m = /^\s*\{\{([^}]*)\}\}\s*$/.exec(s); return m ? m[1] : null; };
const interp = (s, scope, e) => s.replace(HOLE, (_, x) => { const v = lookup(x, scope); return v == null || v === false ? '' : e(v); });

const styles = new Set();
function renderComp(name, props, depth = 0) {
  if (depth > 8) throw new Error('import depth');
  const c = load(name);
  const p = {};
  for (const [k, d] of Object.entries(c.defs)) if (k[0] !== '$' && d && 'default' in d) p[k] = d.default;
  Object.assign(p, props);
  const vals = new c.Cls(p).renderVals();
  const out = [];
  for (const n of c.xdc.children) {
    if (n.tag === 'helmet') { for (const h of n.children) if (h.tag === 'style') styles.add(h.children[0].text); continue; }
    out.push(node(n, [vals], depth));
  }
  return out.join('');
}

function node(n, scope, depth) {
  if (n.text !== undefined) return interp(n.text, scope, esc);
  const t = n.tag;
  const kids = (sc) => n.children.map((c) => node(c, sc, depth)).join('');
  if (t === 'sc-for') {
    const list = lookup(whole(attr(n, 'list')), scope) || [];
    const as = attr(n, 'as');
    return list.map((item, i) => kids(scope.concat({ [as]: item, index: i }))).join('');
  }
  if (t === 'sc-if') return lookup(whole(attr(n, 'value')), scope) ? kids(scope) : '';
  if (t === 'dc-import') {
    const name = attr(n, 'name');
    const defs = load(name).defs;
    const props = {};
    for (const [k, v] of n.attrs) {
      if (k === 'name' || k.startsWith('hint-')) continue;
      const key = camel(k), w = whole(v);
      let val = w !== null ? lookup(w, scope) : v;
      const ed = defs[key] && defs[key].editor;
      if (w === null && (ed === 'int' || ed === 'range' || ed === 'number')) val = Number(v);
      if (w === null && ed === 'boolean') val = v !== 'false';
      props[key] = val;
    }
    return renderComp(name, props, depth + 1);
  }
  if (t === 'script') return '';
  let s = '<' + t;
  for (const [k, v] of n.attrs) {
    if (/^on/i.test(k) || k.startsWith('hint-')) continue;
    const w = whole(v);
    if (w !== null) {
      const val = lookup(w, scope);
      if (val == null || val === false) continue;
      s += val === true && !k.startsWith('aria-') ? ` ${k}` : ` ${k}="${escAttr(val)}"`;
    } else s += ` ${k}="${interp(v, scope, escAttr)}"`;
  }
  s += '>';
  if (VOID.has(t) && !SVGVOID.has(t)) return s;
  return s + kids(scope) + `</${t}>`;
}

const [board, outFile] = process.argv.slice(2);
const top = load(board);
const body = renderComp(board, {});
const ff = (family, weight, file) => `@font-face{font-family:'${family}';font-weight:${weight};src:url('${pathToFileURL(path.join(FONTS, file))}')}`;
const fonts = [
  ff('Geist', 300, 'geist_regular.ttf'), ff('Geist', 400, 'geist_regular.ttf'), ff('Geist', 500, 'geist_medium.ttf'), ff('Geist', 600, 'geist_semibold.ttf'),
  ff('Geist Mono', 400, 'geist_mono_regular.ttf'), ff('Geist Mono', 500, 'geist_mono_medium.ttf'),
].join('\n');
fs.writeFileSync(outFile, `<!doctype html><html><head><meta charset="utf-8"><style>${fonts}</style><style>${[...styles].join('\n')}</style><style>*{animation:none!important;transition:none!important}</style></head><body>${body}</body></html>`);
const pv = top.defs.$preview;
let w = pv && pv.width, h = pv && pv.height;
if (!w) {
  const first = top.xdc.children.find((c) => c.tag && c.tag !== 'helmet');
  const st = attr(first, 'style') || '';
  w = +(/(?:^|;)\s*width:\s*(\d+)px/.exec(st) || [])[1];
  h = +(/(?:^|;)\s*height:\s*(\d+)px/.exec(st) || [])[1];
}
console.log(w, h, w < 1000 ? 3 : 1);
