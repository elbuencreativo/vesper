#!/usr/bin/env python3
"""Prepara ~20 obras de dominio público de la National Gallery of Art para el modo reposo.

Lee en streaming (sin guardar) objects.csv y published_images.csv del repo
NationalGalleryOfArt/opendata, elige obras con marca open access y baja solo esas imágenes
(JPEG a 1920x1080 máx.) desde el servicio IIIF. Uso:

    python3 -I tools/art/prepare_art.py --out /ruta/NUEVA/vacia --assets app/src/main/assets/art
"""
import argparse
import csv
import io
import json
import os
import shutil
import sys
import time
import urllib.request
from collections import Counter

RAW = "https://raw.githubusercontent.com/NationalGalleryOfArt/opendata/main/data/"
UA = "tv-launcher-art-prep/1.0"
MIN_WIDTH = 2000
MIN_RATIO, MAX_RATIO = 1.25, 2.3
MAX_PER_ARTIST = 2

# Paisajes y escenas atmosféricas que se ven bien a pantalla completa; el resto va después.
PREFERRED = [
    "claude monet", "j. m. w. turner", "john constable", "thomas cole", "frederic edwin church",
    "albert bierstadt", "claude lorrain", "vincent van gogh", "camille pissarro", "alfred sisley",
    "jacob van ruisdael", "aelbert cuyp", "canaletto", "francesco guardi", "winslow homer",
    "george inness", "fitz henry lane", "martin johnson heade", "paul cézanne", "édouard manet",
    "pierre-auguste renoir", "johan barthold jongkind", "joaquín sorolla", "gustave courbet",
]


MULTI_PANEL = ("triptych", "polyptych", "diptych", "entire")
SCENIC = (
    "landscape", "harbor", "harbour", "view", "sunset", "sea", "river", "canal", "coast", "cliff",
    "bay", "lake", "mountain", "winter", "evening", "morning", "storm", "port", "venice", "village",
    "bridge", "garden", "field", "snow", "shore", "beach", "city", "square",
)


def _is_multi_panel(title):
    t = (title or "").lower()
    return any(w in t for w in MULTI_PANEL)


def _scenic(title):
    t = (title or "").lower()
    return 0 if any(w in t for w in SCENIC) else 1


def _rank(artist):
    a = (artist or "").lower()
    for i, name in enumerate(PREFERRED):
        if name in a:
            return i
    return len(PREFERRED)


def _usable(r):
    try:
        w, h = int(r["width"]), int(r["height"])
    except (KeyError, TypeError, ValueError):
        return False
    return (
        str(r.get("openaccess")) == "1"
        and r.get("viewtype") == "primary"
        and bool(r.get("iiifurl"))
        and w >= MIN_WIDTH
        and h > 0
        and MIN_RATIO <= w / h <= MAX_RATIO
    )


def select_open_access(rows, n=20):
    """Elige hasta n obras open access: apaisadas, nítidas, máx. 2 por artista, repartidas por siglos."""
    candidates = sorted(
        (r for r in rows if _usable(r) and not _is_multi_panel(r.get("title"))),
        key=lambda r: (_scenic(r.get("title")), _rank(r["artist"]), r["uuid"]),
    )
    per_artist = Counter()
    capped = []
    for r in candidates:
        if per_artist[r["artist"]] < MAX_PER_ARTIST:
            per_artist[r["artist"]] += 1
            capped.append(r)

    # Primero las obras con escena (paisajes, vistas), repartidas por siglos; si faltan, se completa con el resto.
    picked = _spread_by_century([r for r in capped if _scenic(r.get("title")) == 0], n)
    if len(picked) < n:
        picked += _spread_by_century([r for r in capped if _scenic(r.get("title")) != 0], n - len(picked))
    return picked


def _spread_by_century(rows, n):
    buckets = {}
    for r in rows:
        buckets.setdefault(r["year"] // 100, []).append(r)
    picked = []
    while len(picked) < n and any(buckets.values()):
        for century in sorted(buckets):
            if buckets[century] and len(picked) < n:
                picked.append(buckets[century].pop(0))
    return picked


def _stream_csv(name):
    req = urllib.request.Request(RAW + name, headers={"User-Agent": UA})
    resp = urllib.request.urlopen(req, timeout=120)
    return csv.DictReader(io.TextIOWrapper(resp, encoding="utf-8", newline=""))


def load_candidates():
    objects = {}
    for o in _stream_csv("objects.csv"):
        if o.get("classification") != "Painting":
            continue
        try:
            year = int(o["beginyear"])
        except (KeyError, ValueError):
            continue
        objects[o["objectid"]] = {
            "title": o["title"], "artist": o.get("attribution", ""),
            "year": year, "displaydate": o.get("displaydate", ""),
        }
    rows = []
    for img in _stream_csv("published_images.csv"):
        obj = objects.get(img.get("depictstmsobjectid"))
        if obj and img.get("openaccess") == "1" and img.get("viewtype") == "primary":
            rows.append({**obj, "uuid": img["uuid"], "iiifurl": img["iiifurl"],
                         "width": img["width"], "height": img["height"],
                         "openaccess": img["openaccess"], "viewtype": img["viewtype"]})
    return rows


def download(row, dest):
    url = f"{row['iiifurl']}/full/!1920,1080/0/default.jpg"
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=120) as resp, open(dest, "wb") as f:
        shutil.copyfileobj(resp, f)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--out", required=True, help="Directorio NUEVO y vacío para las descargas")
    p.add_argument("--assets", required=True, help="Destino final (assets del app)")
    p.add_argument("--n", type=int, default=20)
    args = p.parse_args()

    if os.path.exists(args.out) and os.listdir(args.out):
        sys.exit("--out debe estar vacío")
    os.makedirs(args.out, exist_ok=True)

    picked = select_open_access(load_candidates(), args.n)
    meta = []
    for i, r in enumerate(picked, 1):
        name = f"art{i:02d}.jpg"
        download(r, os.path.join(args.out, name))
        meta.append({"file": name, "title": r["title"], "artist": r["artist"], "year": r["displaydate"] or str(r["year"])})
        print(f"{name}: {r['artist']} — {r['title']} ({r['displaydate']})")
        time.sleep(0.3)

    os.makedirs(args.assets, exist_ok=True)
    for m in meta:
        shutil.copyfile(os.path.join(args.out, m["file"]), os.path.join(args.assets, m["file"]))
    with open(os.path.join(args.assets, "art.json"), "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)
    print(f"{len(meta)} obras en {args.assets}")


if __name__ == "__main__":
    main()
