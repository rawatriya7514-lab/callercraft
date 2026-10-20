#!/usr/bin/env python3
"""Minimal Maven POM resolver for the CraftCaller on-device build (Termux).
Follows parent chains, dependencyManagement (incl. BOM imports), properties and
exclusions. Downloads jars/aars into ~/craftcaller/m2cache and writes
m2cache/classpath.txt with the full runtime classpath.
"""
import concurrent.futures as cf
import os
import re
import sys
import threading
import urllib.request
import xml.etree.ElementTree as ET

M2 = os.path.expanduser("~/craftcaller/m2cache")
GOOGLE = "https://dl.google.com/android/maven2"
CENTRAL = "https://repo1.maven.org/maven2"
MISSLOG = os.path.join(M2, "missing.log")
LOG_LOCK = threading.Lock()

def gpath(g):
    return g.replace(".", "/")

def repo_for(g):
    if g.startswith("androidx.") or g.startswith("com.google.firebase") or \
       g.startswith("com.google.android"):
        return GOOGLE
    return CENTRAL

def url(repo, g, a, v, ext):
    return f"{repo}/{gpath(g)}/{a}/{v}/{a}-{v}.{ext}"

def find_text(el, tag):
    e = el.find(tag)
    return e.text.strip() if el is not None and e is not None and e.text else None

def fetch(u, dest):
    req = urllib.request.Request(u, headers={"User-Agent": "cc-res/1.0"})
    with urllib.request.urlopen(req, timeout=60) as r, open(dest, "wb") as fh:
        fh.write(r.read())

_fetch_locks = {}
_fetch_locks_mu = threading.Lock()
def fetch_cached(u, dest):
    with _fetch_locks_mu:
        lk = _fetch_locks.setdefault(dest, threading.Lock())
    with lk:
        if os.path.exists(dest) and os.path.getsize(dest) > 0:
            return True
        try:
            fetch(u, dest)
            return True
        except Exception as e:
            with LOG_LOCK, open(MISSLOG, "a") as fh:
                fh.write(f"{u} :: {e}\n")
            return False

def strip_ns(root):
    for e in root.iter():
        if "}" in e.tag:
            e.tag = e.tag.split("}", 1)[1]
    return root

pom_cache = {}
RANGE_RE = re.compile(r"^[\[\(]\s*(.+?)\s*[\]\)]$")
def expand_version(v):
    """Maven/Gradle version-range syntax -> concrete candidates, best first."""
    if not v:
        return []
    m = RANGE_RE.match(v.strip())
    if not m:
        return [v.strip()]
    inner = m.group(1)
    nums = [p.strip() for p in inner.split(",") if re.match(r"^\d[\w.\-]*$", p.strip())]
    if not nums:
        return []
    return nums[::-1]

def best_pom(g, a, vstr, depth=15):
    """Resolve a (possibly range) version string to a successfully loaded Pom."""
    for c in expand_version(vstr):
        try:
            return (g, a, c), load_pom((g, a, c), depth)
        except Exception:
            continue
    return None, None

class Pom:
    __slots__ = ("group", "art", "version", "packaging", "props", "deps", "dm")
    def __init__(self):
        self.group = None; self.art = None; self.version = None
        self.packaging = "jar"
        self.props = {}
        self.deps = []    # (g,a,v_or_None,type,scope)
        self.dm = {}      # (g,a) -> version

def load_pom(coord, depth=0):
    g, a, v = coord
    key = f"{g}:{a}:{v}"
    hit = pom_cache.get(key)
    if hit is not None:
        return hit
    d = os.path.join(M2, gpath(g), a, v)
    os.makedirs(d, exist_ok=True)
    pf = os.path.join(d, f"{a}-{v}.pom")
    if not fetch_cached(url(repo_for(g), g, a, v, "pom"), pf):
        raise RuntimeError(f"pom 404 {coord}")
    with open(pf, encoding="utf-8") as fh:
        root = ET.parse(pf).getroot()
    strip_ns(root)
    p = Pom()
    p.group = root.findtext("groupId"); p.art = root.findtext("artifactId")
    p.version = root.findtext("version")
    pk = root.findtext("packaging")
    p.packaging = pk if pk in ("aar", "jar", "pom") else "jar"
    par = root.find("parent")
    parent = None
    if par is not None:
        pg = par.findtext("groupId"); pa = par.findtext("artifactId")
        pv = par.findtext("version")
        if p.group is None: p.group = pg
        if p.version is None: p.version = pv
        if depth < 12:
            parent = load_pom((pg, pa, pv), depth + 1)
    props = {}
    if parent is not None:
        props = dict(parent.props)
    prs = root.find("properties")
    if prs is not None:
        for c in prs:
            t = c.tag
            if t.startswith("{") and "}" in t:
                t = t.split("}", 1)[1]
            props[t] = (c.text or "").strip()
    p.props = props
    if p.version is None:
        p.version = (parent.version if parent else None)
    def subst(val):
        n = 0
        while val and n < 8:
            m = re.match(r"^$\{(.+)}$", val) or None
            val2 = val
            m = re.match(r"^\$\{(.+)\}$", val)
            if not m:
                break
            k = m.group(1)
            val2 = props.get(k)
            if val2 is None:
                val2 = p.version if k in ("project.version", "pom.version", "version") else None
            if val2 is None:
                break
            val = val2
            n += 1
        return val if val else ""
    def dm_new(dag_el, consumer_props):
        out = {}
        for dep in dag_el:
            dg = find_text(dep, "groupId"); da = find_text(dep, "artifactId")
            dv = find_text(dep, "version")
            sc = find_text(dep, "scope") or "compile"
            if sc == "import":
                bp = load_pom((dg, da, subst(dv)), depth + 1)
                out.update(bp.dm)
            else:
                v2 = subst(dv)
                if v2:
                    out[(dg, da)] = v2
        return out

    dm = {}
    if parent is not None:
        dm = dict(parent.dm)
    dmr = root.find("dependencyManagement")
    if dmr is not None:
        dm.update(dm_new(dmr.find("dependencies"), props))
    p.dm = dm

    deps = root.find("dependencies")
    if deps is not None:
        for dep in deps:
            dg = find_text(dep, "groupId"); da = find_text(dep, "artifactId")
            dt = find_text(dep, "type") or "jar"
            dscope = find_text(dep, "scope") or "compile"
            dopt = find_text(dep, "optional") or "false"
            ver = find_text(dep, "version")
            if dscope in ("test", "provided", "system") or dopt == "true":
                continue
            if dt == "pom":
                if ver:
                    vv = ver
                    vv_m = re.match(r"^\$\{(.+)\}$", ver)
                    if vv_m:
                        vv = props.get(vv_m.group(1), "")
                    if vv:
                        try:
                            load_pom((dg, da, vv), depth + 1)
                        except Exception:
                            pass
                continue
            if not ver:
                ver = dm.get((dg, da))
            ver = subst(ver)
            if not ver:
                continue
            p.deps.append((dg, da, ver, dt, dscope))
    pom_cache[key] = p
    return p

def norm_ver(v):
    if not v:
        return (0,)
    nums = re.findall(r"\d+", v)

def version_key(v):
    nums = [int(x) for x in re.findall(r"\d+", v or "0")]
    while len(nums) < 4:
        nums.append(0)
    return tuple(nums[:4])

def skip_artifact(a):
    return (a.endswith("-klib") or a.endswith("-metadata") or a.endswith("-gradle-plugin")
            or a.endswith("-compiler-plugin") or a.endswith("-embeddable")
            or a.endswith("-sources") or a.endswith("-javadoc"))

def resolve_bfs(seeds):
    visited = {}
    frontier = list(seeds)
    result_coords = []
    level = 0
    while frontier and level < 18:
        uniq = {}
        for g, a, v in frontier:
            if not g or not a or not v:
                continue
            k = (g, a)
            if k not in uniq or version_key(v) > version_key(uniq[k]):
                uniq[k] = v
        frontier = [(g, a, v) for (g, a), v in uniq.items()]
        with cf.ThreadPoolExecutor(max_workers=16) as ex:
            futs = {ex.submit(best_pom, c[0], c[1], c[2]): c for c in frontier}
            next_frontier = []
            for f in futs:
                c = futs[f]
                try:
                    _, p = f.result()
                except Exception:
                    with LOG_LOCK, open(MISSLOG, "a") as fh:
                        fh.write(f"RESOLVE FAIL {c}\n")
                    continue
                if p is None:
                    with LOG_LOCK, open(MISSLOG, "a") as fh:
                        fh.write(f"RESOLVE FAIL (no version) {c}\n")
                    continue
                g, a = c[0], c[1]
                v = p.version
                cur = visited.get((g, a))
                if cur is None or version_key(v) > version_key(cur):
                    visited[(g, a)] = v
                for dg, da, dv, dt, scope in p.deps:
                    if dt not in ("jar", "aar", "bundle"):
                        continue
                    if skip_artifact(da) or not dv:
                        continue
                    next_frontier.append((dg, da, dv))
        result_coords.extend(frontier)
        frontier = next_frontier
        level += 1
    return visited

def main():
    os.makedirs(M2, exist_ok=True)
    seeds = []
    for line in open(sys.argv[1]):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split(":")
        if len(parts) != 3:
            continue
        seeds.append(tuple(parts))
    visited = resolve_bfs(seeds)
    def pull(c):
        g, a, v = c
        def skip(msg):
            with LOG_LOCK, open(MISSLOG, "a") as fh:
                fh.write(f"PULL SKIP {g}:{a}:{v} :: {msg}\n")
            return None
        pom = None
        d = os.path.join(M2, gpath(g), a, v)
        pf = os.path.join(d, f"{a}-{v}.pom")
        pack = "jar"
        if os.path.exists(pf):
            try:
                rt = ET.parse(pf).getroot()
                strip_ns(rt)
                pk = rt.findtext("packaging")
                pack = pk if pk in ("aar", "jar", "pom") else "jar"
            except Exception:
                pass
        ext = pack if pack in ("aar", "jar") else "jar"
        p = os.path.join(d, f"{a}-{v}.{ext}")
        if fetch_cached(url(repo_for(g), g, a, v, ext), p) and os.path.getsize(p) > 1500:
            if skip_artifact(a):
                return skip("skip_artifact")
            try:
                from zipfile import ZipFile, BadZipFile
                with ZipFile(p) as z:
                    names = z.namelist()
                if not any(n == "classes.jar" for n in names) and ext == "aar":
                    return skip("aar has no classes.jar (tie/doc stub)")
            except BadZipFile:
                return skip("bad zip")
            return p
        return skip("fetch failed or tiny")
    def pull_item(kv):
        (g, a), v = kv
        return pull((g, a, v))
    with cf.ThreadPoolExecutor(max_workers=12) as ex:
        results = list(ex.map(pull_item, visited.items()))
    paths = sorted({p for p in results if p})
    with open(os.path.join(M2, "classpath.txt"), "w") as fh:
        fh.write("\n".join(paths))
    print(f"{len(paths)} artifacts resolved -> {os.path.join(M2, 'classpath.txt')}")

if __name__ == "__main__":
    main()
