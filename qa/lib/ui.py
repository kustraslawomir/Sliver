"""uiautomator helper. usage: ui.py find <xml> <exact|sub> <needle>  -> prints "x y" of the first match
(text or content-desc), exit 1 if none. ui.py has <xml> <needle> -> exit 0/1. ui.py list <xml>."""
import re
import sys
import xml.etree.ElementTree as ET


def nodes(path):
    raw = open(path, encoding="utf-8", errors="replace").read()
    raw = raw[raw.find("<?xml"):] if "<?xml" in raw else raw
    raw = raw[: raw.rfind("</hierarchy>") + 12]
    for n in ET.fromstring(raw).iter("node"):
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds", ""))
        if m:
            a, b, c, d = map(int, m.groups())
            yield n.get("text", ""), n.get("content-desc", ""), (a + c) // 2, (b + d) // 2, c > a and d > b


def match(mode, needle, t, d):
    ls = [s.lower() for s in (t, d) if s]
    n = needle.lower()
    return any(s == n for s in ls) if mode == "exact" else any(n in s for s in ls)


cmd, path = sys.argv[1], sys.argv[2]
if cmd == "list":
    for t, d, x, y, ok in nodes(path):
        if t or d:
            print(f"{x},{y}\t{t}\t{d}")
elif cmd == "find":
    for t, d, x, y, ok in nodes(path):
        if ok and match(sys.argv[3], sys.argv[4], t, d):
            print(x, y)
            sys.exit(0)
    sys.exit(1)
elif cmd == "has":
    sys.exit(0 if any(match("sub", sys.argv[3], t, d) for t, d, *_ in nodes(path)) else 1)
elif cmd == "projects":
    prev = ""
    for t, d, *_ in nodes(path):
        if t and re.search(r"· \d+ clips? ·", t) and prev:
            print(prev)
        if t:
            prev = t
