#!/usr/bin/env python3
"""
Build real binary GIS + High-Res Terrarium DEM packs for:
  1) /native-assets/assets/terrain_grid/magadi_town_3x5km.bin  (Magic: "MAGT", v1)
  2) /native-assets/assets/terrain_grid/c58_corridor_3km.bin    (Magic: "C58B", v1)

Zero external dependencies beyond Python 3 stdlib (urllib, json, zlib, struct, math, datetime).
Eliminates slow/flaky runtime Overpass live-sync in the Android app by bundling verified
OSM roads (with refs/bridges/roundabouts/oneway), buildings/houses (ways + multipolygon relations),
water bodies, road-symbol/POI nodes, and AWS Terrarium DEM grids directly in APK assets.
"""

from __future__ import annotations

import datetime
import json
import math
import os
import struct
import sys
import time
import urllib.parse
import urllib.request
import zlib
from typing import Dict, List, Optional, Tuple

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ASSETS_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, "..", "assets", "terrain_grid"))
os.makedirs(ASSETS_DIR, exist_ok=True)

MAGADI_BIN_PATH = os.path.join(ASSETS_DIR, "magadi_town_3x5km.bin")
C58_BIN_PATH = os.path.join(ASSETS_DIR, "c58_corridor_3km.bin")

OVERPASS_ENDPOINTS = [
    "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
    "https://overpass-api.de/api/interpreter",
]

TERRARIUM_URL_FMT = "https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png"
USER_AGENT = "Apex3DAssets-BinaryGISBuilder/1.0"
OVERPASS_CACHE_DIR = "/tmp/apex3d_overpass_cache"
os.makedirs(OVERPASS_CACHE_DIR, exist_ok=True)

# UTM Zone 37S (EPSG:32737) constants matching MagadiTownEngine.cpp & C58CorridorEngine.cpp
WGS84_A = 6378137.0
WGS84_F = 1.0 / 298.257223563
WGS84_E2 = WGS84_F * (2.0 - WGS84_F)
WGS84_EP2 = WGS84_E2 / (1.0 - WGS84_E2)
UTM_K0 = 0.9996
UTM_LON0_RAD = math.radians(39.0)

C58_UTM_REF_E = 190000.0
C58_UTM_REF_N = 9780000.0


def latlon_to_utm37s(lat: float, lon: float) -> Tuple[float, float]:
    phi = math.radians(lat)
    lam = math.radians(lon)
    sin_p = math.sin(phi)
    cos_p = math.cos(phi)
    tan_p = math.tan(phi)
    n_val = WGS84_A / math.sqrt(1.0 - WGS84_E2 * sin_p * sin_p)
    t_val = tan_p * tan_p
    c_val = WGS84_EP2 * cos_p * cos_p
    a_val = cos_p * (lam - UTM_LON0_RAD)

    e2 = WGS84_E2
    e4 = e2 * e2
    e6 = e4 * e2
    m_val = WGS84_A * (
        (1.0 - e2 / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0) * phi
        - (3.0 * e2 / 8.0 + 3.0 * e4 / 32.0 + 45.0 * e6 / 1024.0) * math.sin(2.0 * phi)
        + (15.0 * e4 / 256.0 + 45.0 * e6 / 1024.0) * math.sin(4.0 * phi)
        - (35.0 * e6 / 3072.0) * math.sin(6.0 * phi)
    )
    easting = 500000.0 + UTM_K0 * n_val * (
        a_val
        + (1.0 - t_val + c_val) * (a_val ** 3) / 6.0
        + (5.0 - 18.0 * t_val + t_val * t_val + 72.0 * c_val - 58.0 * WGS84_EP2) * (a_val ** 5) / 120.0
    )
    northing_raw = UTM_K0 * (
        m_val
        + n_val
        * tan_p
        * (
            a_val * a_val / 2.0
            + (5.0 - t_val + 9.0 * c_val + 4.0 * c_val * c_val) * (a_val ** 4) / 24.0
            + (61.0 - 58.0 * t_val + t_val * t_val + 600.0 * c_val - 330.0 * WGS84_EP2) * (a_val ** 6) / 720.0
        )
    )
    northing = (10000000.0 + northing_raw) if lat < 0.0 else northing_raw
    return easting, northing


def utm37s_to_latlon(easting: float, northing: float) -> Tuple[float, float]:
    e2 = WGS84_E2
    e4 = e2 * e2
    e6 = e4 * e2
    e1 = (1.0 - math.sqrt(1.0 - e2)) / (1.0 + math.sqrt(1.0 - e2))
    x = easting - 500000.0
    y = northing - 10000000.0
    m_val = y / UTM_K0
    mu = m_val / (WGS84_A * (1.0 - e2 / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0))
    phi1 = (
        mu
        + (3.0 * e1 / 2.0 - 27.0 * (e1 ** 3) / 32.0) * math.sin(2.0 * mu)
        + (21.0 * e1 * e1 / 16.0 - 55.0 * (e1 ** 4) / 32.0) * math.sin(4.0 * mu)
        + (151.0 * (e1 ** 3) / 96.0) * math.sin(6.0 * mu)
        + (1097.0 * (e1 ** 4) / 512.0) * math.sin(8.0 * mu)
    )
    sin_p1 = math.sin(phi1)
    cos_p1 = math.cos(phi1)
    tan_p1 = math.tan(phi1)
    n1 = WGS84_A / math.sqrt(1.0 - e2 * sin_p1 * sin_p1)
    t1 = tan_p1 * tan_p1
    c1 = WGS84_EP2 * cos_p1 * cos_p1
    r1 = WGS84_A * (1.0 - e2) / ((1.0 - e2 * sin_p1 * sin_p1) ** 1.5)
    d = x / (n1 * UTM_K0)
    lat_rad = phi1 - (n1 * tan_p1 / r1) * (
        d * d / 2.0
        - (5.0 + 3.0 * t1 + 10.0 * c1 - 4.0 * c1 * c1 - 9.0 * WGS84_EP2) * (d ** 4) / 24.0
        + (61.0 + 90.0 * t1 + 298.0 * c1 + 45.0 * t1 * t1 - 252.0 * WGS84_EP2 - 3.0 * c1 * c1) * (d ** 6) / 720.0
    )
    lon_rad = UTM_LON0_RAD + (
        d
        - (1.0 + 2.0 * t1 + c1) * (d ** 3) / 6.0
        + (5.0 - 2.0 * c1 + 28.0 * t1 - 3.0 * c1 * c1 + 8.0 * WGS84_EP2 + 24.0 * t1 * t1) * (d ** 5) / 120.0
    ) / cos_p1
    return math.degrees(lat_rad), math.degrees(lon_rad)


# ─── Pure-Python PNG Decoder for AWS Terrarium Tiles ──────────────────────────

def decode_png_rgb(data: bytes) -> Tuple[int, int, int, bytes]:
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("Invalid PNG header")
    pos = 8
    width = height = bit_depth = color_type = 0
    idat_chunks = []
    while pos < len(data):
        length = struct.unpack(">I", data[pos : pos + 4])[0]
        ctype = data[pos + 4 : pos + 8]
        chunk = data[pos + 8 : pos + 8 + length]
        pos += 12 + length
        if ctype == b"IHDR":
            width, height, bit_depth, color_type, comp, flt, interlace = struct.unpack(">IIBBBBB", chunk)
            if bit_depth != 8 or interlace != 0:
                raise ValueError(f"Unsupported PNG format depth={bit_depth} interlace={interlace}")
        elif ctype == b"IDAT":
            idat_chunks.append(chunk)
        elif ctype == b"IEND":
            break

    bpp = {2: 3, 6: 4, 0: 1, 4: 2}.get(color_type, 3)
    raw = zlib.decompress(b"".join(idat_chunks))
    stride = width * bpp
    out = bytearray(height * stride)
    in_pos = 0

    def paeth(a: int, b: int, c: int) -> int:
        p = a + b - c
        pa = abs(p - a)
        pb = abs(p - b)
        pc = abs(p - c)
        if pa <= pb and pa <= pc:
            return a
        return b if pb <= pc else c

    for y in range(height):
        flt = raw[in_pos]
        in_pos += 1
        row_start = y * stride
        prev_start = (y - 1) * stride
        for x in range(stride):
            val = raw[in_pos]
            in_pos += 1
            a = out[row_start + x - bpp] if x >= bpp else 0
            b = out[prev_start + x] if y > 0 else 0
            c = out[prev_start + x - bpp] if (y > 0 and x >= bpp) else 0
            if flt == 0:
                res = val
            elif flt == 1:
                res = (val + a) & 0xFF
            elif flt == 2:
                res = (val + b) & 0xFF
            elif flt == 3:
                res = (val + ((a + b) >> 1)) & 0xFF
            elif flt == 4:
                res = (val + paeth(a, b, c)) & 0xFF
            else:
                raise ValueError(f"Unknown PNG filter {flt}")
            out[row_start + x] = res
    return width, height, bpp, bytes(out)


_TILE_CACHE: Dict[Tuple[int, int, int], Tuple[int, int, int, bytes]] = {}


def fetch_terrarium_tile(z: int, tx: int, ty: int) -> Tuple[int, int, int, bytes]:
    key = (z, tx, ty)
    if key in _TILE_CACHE:
        return _TILE_CACHE[key]
    url = TERRARIUM_URL_FMT.format(z=z, x=tx, y=ty)
    for attempt in range(4):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(req, timeout=25) as resp:
                decoded = decode_png_rgb(resp.read())
                _TILE_CACHE[key] = decoded
                return decoded
        except Exception as e:
            if attempt == 3:
                raise RuntimeError(f"Terrarium tile {z}/{tx}/{ty} failed: {e}") from e
            time.sleep(0.8 * (attempt + 1))
    raise RuntimeError("Unreachable")


def sample_terrarium_elev(z: int, lat: float, lon: float) -> float:
    n = 1 << z
    world_tx = ((lon + 180.0) / 360.0) * n
    rad = math.radians(lat)
    world_ty = ((1.0 - math.log(math.tan(rad) + 1.0 / math.cos(rad)) / math.pi) / 2.0) * n
    tx = int(math.floor(world_tx))
    ty = int(math.floor(world_ty))
    w, h, bpp, raw = fetch_terrarium_tile(z, tx, ty)
    px = max(0, min(w - 1, int((world_tx - tx) * w)))
    py = max(0, min(h - 1, int((world_ty - ty) * h)))
    off = (py * w + px) * bpp
    r = raw[off]
    g = raw[off + 1] if bpp >= 2 else r
    b = raw[off + 2] if bpp >= 3 else 0
    return (r * 256.0) + g + (b / 256.0) - 32768.0


def query_overpass(query: str, timeout_s: int = 38) -> Tuple[dict, str]:
    import hashlib
    qhash = hashlib.sha256(query.encode("utf-8")).hexdigest()[:16]
    cache_path = os.path.join(OVERPASS_CACHE_DIR, f"{qhash}.json")
    if os.path.exists(cache_path) and os.path.getsize(cache_path) > 32:
        try:
            with open(cache_path, "r", encoding="utf-8") as cf:
                cached = json.load(cf)
            if isinstance(cached, dict) and "elements" in cached:
                return cached, cached.get("_endpoint", OVERPASS_ENDPOINTS[0])
        except Exception:
            pass

    payload = urllib.parse.urlencode({"data": query}).encode("utf-8")
    last_err = None
    for ep in OVERPASS_ENDPOINTS:
        for retry in range(2):
            try:
                req = urllib.request.Request(
                    ep,
                    data=payload,
                    method="POST",
                    headers={"User-Agent": USER_AGENT},
                )
                with urllib.request.urlopen(req, timeout=timeout_s) as resp:
                    raw = resp.read().decode("utf-8")
                    obj = json.loads(raw)
                    if "elements" in obj:
                        obj["_endpoint"] = ep
                        with open(cache_path, "w", encoding="utf-8") as cf:
                            json.dump(obj, cf)
                        return obj, ep
            except Exception as e:
                last_err = f"{ep}: {e}"
                print(f"    Overpass mirror retry ({last_err})", flush=True)
                time.sleep(1.5 * (retry + 1))
    raise RuntimeError(f"Overpass query failed: {last_err}")


def pack_str(buf: bytearray, s: str) -> None:
    enc = (s or "").encode("utf-8")
    if len(enc) > 65535:
        enc = enc[:65535]
    buf.extend(struct.pack("<H", len(enc)))
    buf.extend(enc)


# ─── Part 1: Extract & Write Magadi Town 3x5 km @ 1m Binary Pack ("MAGT") ──────

def build_magadi_town_bin() -> None:
    print("\n=== Building Magadi Town 3x5km Binary Pack ===")
    center_lat = -1.9012
    center_lon = 36.2870
    width_m = 3000.0
    height_m = 5000.0
    m_per_lat = 110574.0
    m_per_lon = 111320.0 * math.cos(math.radians(center_lat))

    half_lat = (height_m * 0.5) / m_per_lat
    half_lon = (width_m * 0.5) / m_per_lon
    min_lat = center_lat - half_lat
    max_lat = center_lat + half_lat
    min_lon = center_lon - half_lon
    max_lon = center_lon + half_lon
    origin_utm_e, origin_utm_n = latlon_to_utm37s(min_lat, min_lon)

    def to_local_xy(lat: float, lon: float) -> Tuple[float, float]:
        return (lon - min_lon) * m_per_lon, (lat - min_lat) * m_per_lat

    bbox_str = f"{min_lat:.6f},{min_lon:.6f},{max_lat:.6f},{max_lon:.6f}"
    osm_query = f"""
    [out:json][timeout:45];
    (
      way["highway"]({bbox_str});
      way["building"]({bbox_str});
      relation["building"]({bbox_str});
      way["amenity"]({bbox_str});
      relation["amenity"]({bbox_str});
      way["man_made"]({bbox_str});
      relation["man_made"]({bbox_str});
      way["industrial"]({bbox_str});
      relation["industrial"]({bbox_str});
      way["landuse"~"industrial|commercial|education|institutional|retail|quarry|military|religious"]({bbox_str});
      relation["landuse"~"industrial|commercial|education|institutional|retail|quarry|military|religious"]({bbox_str});
      way["office"]({bbox_str});
      way["shop"]({bbox_str});
      way["tourism"]({bbox_str});
      way["aeroway"]({bbox_str});
      way["natural"~"water|wetland|mud"]({bbox_str});
      relation["natural"~"water|wetland"]({bbox_str});
      way["landuse"~"salt_pond|reservoir"]({bbox_str});
      relation["landuse"~"salt_pond|reservoir"]({bbox_str});
      node["place"]({bbox_str});
      node["amenity"]({bbox_str});
      node["man_made"]({bbox_str});
      node["industrial"]({bbox_str});
      node["office"]({bbox_str});
      node["craft"]({bbox_str});
      node["shop"]({bbox_str});
      node["aeroway"]({bbox_str});
      node["tourism"]({bbox_str});
      node["highway"~"crossing|bus_stop|turning_circle|mini_roundabout|traffic_signals"]({bbox_str});
      node["barrier"]({bbox_str});
    );
    (._;>;);
    out body;
    """.strip()

    print("Querying Overpass for Magadi Town 3x5km bbox...")
    osm_json, osm_ep = query_overpass(osm_query, timeout_s=50)
    elements = osm_json.get("elements", [])
    print(f"Fetched {len(elements)} raw OSM elements from {osm_ep}")

    print("Fetching Terrarium Zoom-14 (~9.5m/px) DEM grid (120x200)...")
    dem_cols, dem_rows = 120, 200
    dem_vals = []
    for r in range(dem_rows):
        lat = min_lat + (r / (dem_rows - 1)) * (max_lat - min_lat)
        for c in range(dem_cols):
            lon = min_lon + (c / (dem_cols - 1)) * (max_lon - min_lon)
            dem_vals.append(sample_terrarium_elev(14, lat, lon))

    dem_min = min(dem_vals)
    dem_max = max(dem_vals)
    dem_mean = sum(dem_vals) / len(dem_vals)
    print(f"Terrarium Z14 DEM range: min={dem_min:.2f}m max={dem_max:.2f}m mean={dem_mean:.2f}m")

    def sample_dem_xy(x_m: float, y_m: float) -> float:
        fx = max(0.0, min(1.0, x_m / width_m)) * (dem_cols - 1)
        fy = max(0.0, min(1.0, y_m / height_m)) * (dem_rows - 1)
        c0 = int(math.floor(fx))
        r0 = int(math.floor(fy))
        c1 = min(dem_cols - 1, c0 + 1)
        r1 = min(dem_rows - 1, r0 + 1)
        tx = fx - c0
        ty = fy - r0
        v00 = dem_vals[r0 * dem_cols + c0]
        v10 = dem_vals[r0 * dem_cols + c1]
        v01 = dem_vals[r1 * dem_cols + c0]
        v11 = dem_vals[r1 * dem_cols + c1]
        return (v00 * (1 - tx) + v10 * tx) * (1 - ty) + (v01 * (1 - tx) + v11 * tx) * ty

    node_ll: Dict[int, Tuple[float, float]] = {}
    way_nodes: Dict[int, List[Tuple[float, float, float, float]]] = {}  # (lat, lon, xM, yM)
    pois = []
    poi_ids = set()
    roads = []
    buildings = []
    waters = []
    seen_bldg_ids = set()

    LANDMARK_LANDUSES = {
        "industrial", "commercial", "education", "institutional",
        "retail", "quarry", "military", "religious",
    }

    def resolve_poi_category(tags: dict) -> str:
        name_l = (tags.get("name") or "").lower()
        mm = tags.get("man_made") or ""
        ind = tags.get("industrial") or ""
        lu = tags.get("landuse") or ""
        if mm in ("works", "factory", "plant") or "factory" in name_l or (ind and lu == "industrial"):
            return "factory"
        if ind or lu == "industrial":
            return f"industrial:{ind}" if ind else "industrial"
        return (
            tags.get("barrier")
            or tags.get("highway")
            or tags.get("place")
            or tags.get("amenity")
            or tags.get("office")
            or tags.get("craft")
            or mm
            or tags.get("aeroway")
            or tags.get("tourism")
            or tags.get("shop")
            or lu
            or "landmark"
        )

    def resolve_landmark_props(tags: dict) -> Tuple[str, str]:
        raw_bldg = tags.get("building") or ""
        ind = tags.get("industrial") or ""
        mm = tags.get("man_made") or ""
        lu = tags.get("landuse") or ""
        am = tags.get("amenity") or ""
        off = tags.get("office") or ""
        craft = tags.get("craft") or ""
        shop = tags.get("shop") or ""
        tour = tags.get("tourism") or ""
        aero = tags.get("aeroway") or ""
        name_l = (tags.get("name") or "").lower()

        if raw_bldg and raw_bldg != "yes":
            b_type = raw_bldg
        elif mm in ("works", "factory", "plant") or "factory" in name_l:
            b_type = "factory"
        elif ind or lu == "industrial":
            b_type = "industrial"
        elif am:
            b_type = am
        elif mm:
            b_type = mm
        elif off:
            b_type = f"office:{off}" if off != "yes" else "office"
        elif craft:
            b_type = f"craft:{craft}" if craft != "yes" else "craft"
        elif tour:
            b_type = tour
        elif aero:
            b_type = aero
        elif shop:
            b_type = "retail"
        elif lu:
            b_type = lu
        else:
            b_type = raw_bldg or "yes"

        base_am = am or (f"industrial:{ind}" if ind else "") or mm or lu or off or craft or shop or tour or aero
        extra_bits = []
        if base_am and b_type == "factory" and "factory" not in base_am:
            extra_bits.append("factory")
        for tk in ("resource", "product", "healthcare"):
            tv = (tags.get(tk) or "").strip()
            if tv and tv not in base_am:
                extra_bits.append(tv)
        amenity_val = " • ".join([p for p in ([base_am] + extra_bits) if p])
        return b_type, amenity_val

    def is_landmark_area(tags: dict) -> bool:
        if any(k in tags for k in ("amenity", "industrial", "man_made", "office", "craft", "aeroway")):
            return True
        if tags.get("tourism") or tags.get("shop"):
            return True
        lu = tags.get("landuse") or ""
        if lu in LANDMARK_LANDUSES and ("name" in tags or "resource" in tags or "operator" in tags):
            return True
        return False

    for el in elements:
        if el.get("type") == "node":
            nid = el["id"]
            lat = el.get("lat")
            lon = el.get("lon")
            if lat is not None and lon is not None:
                node_ll[nid] = (lat, lon)
                tags = el.get("tags") or {}
                if any(
                    k in tags
                    for k in ("place", "amenity", "man_made", "industrial", "office", "craft", "aeroway", "tourism", "shop", "name", "barrier")
                ) or tags.get("highway") in ("crossing", "bus_stop", "turning_circle", "mini_roundabout", "traffic_signals"):
                    x_m, y_m = to_local_xy(lat, lon)
                    cat = resolve_poi_category(tags)
                    name = tags.get("name") or cat.replace("_", " ").replace(":", " ").title()
                    elev = sample_dem_xy(x_m, y_m)
                    pois.append((nid, name, cat, lat, lon, x_m, y_m, elev))
                    poi_ids.add(nid)

    def has_nearby_poi(name: str, x_m: float, y_m: float, radius_m: float = 220.0) -> bool:
        nl = name.strip().lower()
        for _pid, pname, _cat, _la, _lo, px, py, _el in pois:
            if pname.strip().lower() == nl and math.hypot(px - x_m, py - y_m) <= radius_m:
                return True
        return False

    def process_building(bid: int, pts: List[Tuple[float, float, float, float]], tags: dict, is_compound_only: bool = False) -> None:
        if bid in seen_bldg_ids or len(pts) < 3:
            return
        poly = pts[:-1] if (len(pts) > 3 and pts[0][2] == pts[-1][2] and pts[0][3] == pts[-1][3]) else pts
        area_sum = 0.0
        cx = 0.0
        cy = 0.0
        perim = 0.0
        for k in range(len(poly)):
            p0 = poly[k]
            p1 = poly[(k + 1) % len(poly)]
            cross = p0[2] * p1[3] - p1[2] * p0[3]
            area_sum += cross
            cx += (p0[2] + p1[2]) * cross
            cy += (p0[3] + p1[3]) * cross
            perim += math.hypot(p1[2] - p0[2], p1[3] - p0[3])
        area = max(4.0, abs(area_sum * 0.5))
        if is_compound_only and area > 3_500_000.0:
            return
        seen_bldg_ids.add(bid)
        if abs(area_sum) > 1e-3:
            mx = cx / (3.0 * area_sum)
            my = cy / (3.0 * area_sum)
            c_lon = min_lon + mx / m_per_lon
            c_lat = min_lat + my / m_per_lat
        else:
            mx = sum(p[2] for p in poly) / len(poly)
            my = sum(p[3] for p in poly) / len(poly)
            c_lat = sum(p[0] for p in poly) / len(poly)
            c_lon = sum(p[1] for p in poly) / len(poly)

        b_type, amenity = resolve_landmark_props(tags)
        b_name = tags.get("name") or (
            amenity.split(" • ")[0].replace("_", " ").replace(":", " ").title()
            if amenity
            else (f"{b_type.replace('_', ' ').title()} #{bid}" if b_type != "yes" else f"Building #{bid}")
        )
        try:
            levels = max(1, int(tags.get("building:levels", "1")))
        except ValueError:
            levels = 1
        try:
            h_raw = tags.get("height", "").replace("m", "").strip()
            height_m = float(h_raw) if h_raw else (levels * 3.4 if not is_compound_only else 6.0)
        except ValueError:
            height_m = levels * 3.4
        elev = sample_dem_xy(mx, my)
        buildings.append((bid, b_name, b_type, amenity, levels, height_m, elev, area, perim, (c_lat, c_lon, mx, my), pts))

        if (is_compound_only or is_landmark_area(tags)) and tags.get("name"):
            synth_id = 100_000_000_000 + abs(bid)
            if synth_id not in poi_ids and not has_nearby_poi(b_name, mx, my):
                cat = resolve_poi_category(tags)
                pois.append((synth_id, b_name, cat, c_lat, c_lon, mx, my, elev))
                poi_ids.add(synth_id)

    for el in elements:
        if el.get("type") != "way":
            continue
        wid = el["id"]
        nds = el.get("nodes") or []
        pts = []
        for nid in nds:
            ll = node_ll.get(nid)
            if ll is not None:
                x_m, y_m = to_local_xy(ll[0], ll[1])
                pts.append((ll[0], ll[1], x_m, y_m))
        if len(pts) < 2:
            continue
        way_nodes[wid] = pts
        tags = el.get("tags") or {}
        if "highway" in tags:
            htype = tags.get("highway") or "unclassified"
            ref = tags.get("ref") or ""
            rname = tags.get("name") or ref or f"Magadi {htype.replace('_', ' ').title()} #{wid}"
            surface = tags.get("surface") or (
                "asphalt"
                if htype in ("trunk", "primary", "secondary", "tertiary")
                else ("compacted / paved" if htype in ("residential", "service") else "unpaved / murram")
            )
            try:
                lanes = max(1, int(tags.get("lanes", "2" if htype in ("trunk", "primary", "secondary") else "1")))
            except ValueError:
                lanes = 1
            one_way = tags.get("oneway") == "yes"
            is_bridge = tags.get("bridge") in ("yes", "viaduct", "boardwalk")
            is_roundabout = tags.get("junction") in ("roundabout", "circular")
            width_road = (
                8.0
                if htype in ("trunk", "primary")
                else (7.0 if htype in ("secondary", "tertiary") else (5.5 if htype in ("residential", "unclassified") else 3.5))
            )
            length_m = sum(math.hypot(pts[k + 1][2] - pts[k][2], pts[k + 1][3] - pts[k][3]) for k in range(len(pts) - 1))
            flags = (1 if one_way else 0) | (2 if is_bridge else 0) | (4 if is_roundabout else 0)
            roads.append((wid, rname, ref, htype, surface, lanes, flags, width_road, length_m, pts))
        elif "building" in tags and len(pts) >= 3:
            process_building(wid, pts, tags, is_compound_only=False)
        elif (tags.get("natural") in ("water", "wetland", "mud") or tags.get("landuse") in ("salt_pond", "reservoir")) and len(pts) >= 3:
            wtype = tags.get("water") or tags.get("landuse") or tags.get("natural") or "water"
            wname = tags.get("name") or (f"Magadi Salt Pan #{wid}" if wtype == "salt_pond" else f"Lake Magadi Basin #{wid}")
            mean_e = sum(sample_dem_xy(p[2], p[3]) for p in pts) / len(pts)
            waters.append((wid, wname, wtype, mean_e, pts))
        elif is_landmark_area(tags) and len(pts) >= 3:
            process_building(wid, pts, tags, is_compound_only=True)

    for el in elements:
        if el.get("type") != "relation":
            continue
        rid = el["id"]
        tags = el.get("tags") or {}
        if "building" in tags or is_landmark_area(tags):
            outer_pts = []
            for m in el.get("members") or []:
                if m.get("type") == "way" and m.get("role") != "inner":
                    wpts = way_nodes.get(m["ref"])
                    if wpts and len(wpts) >= 3:
                        outer_pts = wpts
                        break
            if outer_pts:
                process_building(rid + 9_000_000_000, outer_pts, tags, is_compound_only=("building" not in tags))
        elif tags.get("natural") in ("water", "wetland") or tags.get("landuse") in ("salt_pond", "reservoir"):
            wname = tags.get("name") or f"Lake Magadi #{rid}"
            wtype = tags.get("water") or tags.get("landuse") or tags.get("natural") or "lake"
            for idx_m, m in enumerate(el.get("members") or []):
                if m.get("type") == "way" and m.get("role") != "inner":
                    wpts = way_nodes.get(m["ref"])
                    if wpts and len(wpts) >= 3:
                        mean_e = sum(sample_dem_xy(p[2], p[3]) for p in wpts) / len(wpts)
                        waters.append((rid * 1000 + idx_m, wname, wtype, mean_e, wpts))

    roads.sort(key=lambda r: r[8], reverse=True)
    buildings.sort(key=lambda b: b[7], reverse=True)

    sorted_dem = sorted(dem_vals)
    p14 = sorted_dem[min(len(sorted_dem) - 1, int(len(sorted_dem) * 0.14))]
    if waters:
        shore_avg = sum(w[3] for w in waters) / len(waters)
        lake_level = max(dem_min + 0.5, min(dem_max - 4.0, shore_avg + 0.8))
    else:
        lake_level = max(dem_min + 0.8, min(dem_max - 4.0, p14 + 1.2))

    iso_now = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")

    out = bytearray()
    out.extend(b"MAGT")
    out.extend(struct.pack("<HH", 1, 0))
    out.extend(
        struct.pack(
            "<dddddddddd",
            center_lat,
            center_lon,
            width_m,
            height_m,
            min_lat,
            max_lat,
            min_lon,
            max_lon,
            origin_utm_e,
            origin_utm_n,
        )
    )
    pack_str(out, "Magadi Town, Kajiado County, Kenya")
    pack_str(out, iso_now)
    pack_str(out, osm_ep)
    pack_str(out, TERRARIUM_URL_FMT)
    out.extend(struct.pack("<fffff", 9.5, lake_level, dem_min, dem_max, dem_mean))

    # DEM Grid
    pack_str(out, "Terrarium ~9.5m/px DEM (120×200 Binary -> 1m C++ Bicubic)")
    out.extend(struct.pack("<IIffff", dem_cols, dem_rows, 9.5, dem_min, dem_max, dem_mean))
    out.extend(struct.pack(f"<{len(dem_vals)}f", *dem_vals))

    # Roads
    out.extend(struct.pack("<I", len(roads)))
    for wid, rname, ref, htype, surface, lanes, flags, width_r, length_m, pts in roads:
        out.extend(struct.pack("<q", wid))
        pack_str(out, rname)
        pack_str(out, ref)
        pack_str(out, htype)
        pack_str(out, surface)
        out.extend(struct.pack("<BBffI", min(255, lanes), flags, width_r, length_m, len(pts)))
        for _lat, _lon, x_m, y_m in pts:
            out.extend(struct.pack("<ff", x_m, y_m))

    # Buildings
    out.extend(struct.pack("<I", len(buildings)))
    for bid, bname, btype, amenity, levels, height_m, elev_m, area_m2, perim_m, cent, pts in buildings:
        out.extend(struct.pack("<q", bid))
        pack_str(out, bname)
        pack_str(out, btype)
        pack_str(out, amenity)
        out.extend(struct.pack("<BffffffI", min(255, levels), height_m, elev_m, area_m2, perim_m, cent[2], cent[3], len(pts)))
        for _lat, _lon, x_m, y_m in pts:
            out.extend(struct.pack("<ff", x_m, y_m))

    # Water polygons
    out.extend(struct.pack("<I", len(waters)))
    for wid, wname, wtype, mean_e, pts in waters:
        out.extend(struct.pack("<q", wid))
        pack_str(out, wname)
        pack_str(out, wtype)
        out.extend(struct.pack("<fI", mean_e, len(pts)))
        for _lat, _lon, x_m, y_m in pts:
            out.extend(struct.pack("<ff", x_m, y_m))

    # POIs & road symbols
    out.extend(struct.pack("<I", len(pois)))
    for nid, name, cat, _lat, _lon, x_m, y_m, elev in pois:
        out.extend(struct.pack("<q", nid))
        pack_str(out, name)
        pack_str(out, cat)
        out.extend(struct.pack("<fff", x_m, y_m, elev))

    with open(MAGADI_BIN_PATH, "wb") as f:
        f.write(out)

    print(
        f"Wrote {MAGADI_BIN_PATH} ({len(out):,} bytes): "
        f"roads={len(roads)}, buildings={len(buildings)}, water={len(waters)}, pois={len(pois)}"
    )


# ─── Part 2: Extract & Write Route C58 ±1.5km Binary Pack ("C58B") ─────────────

C58_ANCHORS = [
    (-1.9012, 36.2870),  # Magadi Town
    (-1.8620, 36.3060),
    (-1.7890, 36.3420),
    (-1.7180, 36.3780),
    (-1.6480, 36.4125),
    (-1.5785, 36.4472),  # Olorgesailie
    (-1.5250, 36.4950),  # Tinga
    (-1.4820, 36.5480),  # Kamukuru
    (-1.4480, 36.6050),  # Corner Baridi
    (-1.4295, 36.6820),  # Kiserian
    (-1.4110, 36.7160),  # Ongata Rongai
    (-1.3945, 36.7438),
    (-1.3640, 36.7565),
    (-1.3340, 36.7660),  # Bomas of Kenya
]


def stitch_c58_centerline() -> Tuple[List[Tuple[float, float]], bool, str]:
    # Compact out-geom query across full B19/C58 corridor South -> North
    q = """
    [out:json][timeout:35];
    (
      way["ref"~"C58|C 58|B19|B 19"]["highway"~"trunk|primary|secondary|tertiary"](-1.95,36.24,-1.30,36.82);
      way["name"="Magadi Road"]["highway"~"trunk|primary|secondary|tertiary"](-1.95,36.24,-1.30,36.82);
    );
    out geom;
    """.strip()
    way_segments: Dict[int, List[Tuple[float, float]]] = {}
    used_ep = "Anchor fallback"
    try:
        obj, ep = query_overpass(q, timeout_s=40)
        used_ep = ep
        els = obj.get("elements", [])
        for el in els:
            if el.get("type") == "way" and el.get("id") not in way_segments:
                tags = el.get("tags") or {}
                if "highway" not in tags:
                    continue
                pts = [(g["lat"], g["lon"]) for g in (el.get("geometry") or []) if "lat" in g and "lon" in g]
                if len(pts) >= 2:
                    if pts[-1][0] < pts[0][0]:
                        pts = list(reversed(pts))
                    way_segments[el["id"]] = pts
    except Exception as e:
        print(f"Centerline query warning: {e}", flush=True)

    ways = list(way_segments.values())
    if len(ways) >= 8:
        ways.sort(key=lambda w: w[0][0])
        stitched = list(ways.pop(0))
        while ways:
            tail = stitched[-1]
            best_i = -1
            best_score = 1e9
            best_d = 1e9
            rev = False
            for i, w in enumerate(ways):
                # Prefer ways that connect closely and advance monotonically North
                d0 = math.hypot(tail[0] - w[0][0], tail[1] - w[0][1])
                lat_gain0 = w[-1][0] - tail[0]
                score0 = d0 + (0.04 if lat_gain0 < -0.002 else 0.0)
                if score0 < best_score:
                    best_score, best_d, best_i, rev = score0, d0, i, False

                d1 = math.hypot(tail[0] - w[-1][0], tail[1] - w[-1][1])
                lat_gain1 = w[0][0] - tail[0]
                score1 = d1 + (0.04 if lat_gain1 < -0.002 else 0.0)
                if score1 < best_score:
                    best_score, best_d, best_i, rev = score1, d1, i, True

            if best_i < 0:
                break
            chosen = ways.pop(best_i)
            if rev:
                chosen = list(reversed(chosen))
            # Skip redundant parallel dual-carriageway segments that don't extend northward
            if chosen[-1][0] <= stitched[-1][0] - 0.001:
                continue
            if best_d < 0.08:
                for pt in chosen:
                    if pt[0] >= stitched[-1][0] - 0.002 and math.hypot(stitched[-1][0] - pt[0], stitched[-1][1] - pt[1]) > 0.00018:
                        stitched.append(pt)

        # Ensure Magadi Town / Causeway (-1.9012, 36.2870) and Bomas (-1.3340, 36.7660) termini are anchored
        if stitched[0][0] > -1.8995:
            stitched.insert(0, C58_ANCHORS[0])
        if stitched[-1][0] < -1.3400:
            stitched.append(C58_ANCHORS[-1])

        if len(stitched) >= 40 and stitched[0][0] < -1.85 and stitched[-1][0] > -1.36:
            # Downsample cleanly while preserving ~200 evenly distributed vertices along chainage
            if len(stitched) > 210:
                stride = max(1, len(stitched) // 190)
                sampled = [stitched[i] for i in range(0, len(stitched), stride)]
                if sampled[-1] != stitched[-1]:
                    sampled.append(stitched[-1])
                return sampled, True, used_ep
            return stitched, True, used_ep

    # Smooth fallback anchors (66 vertices)
    out = []
    for i in range(len(C58_ANCHORS) - 1):
        a = C58_ANCHORS[i]
        b = C58_ANCHORS[i + 1]
        for s in range(5):
            t = s / 5.0
            out.append((a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t))
    out.append(C58_ANCHORS[-1])
    return out, False, used_ep


def read_existing_c58_utm_features(bin_path: str):
    """Read existing verified OSM UTM geometry from C58B pack to merge when extending south."""
    with open(bin_path, "rb") as f:
        data = f.read()
    pos = 0

    def rd(fmt: str):
        nonlocal pos
        sz = struct.calcsize(fmt)
        vals = struct.unpack_from(fmt, data, pos)
        pos += sz
        return vals

    def rstr() -> str:
        nonlocal pos
        n = rd("<H")[0]
        s = data[pos : pos + n].decode("utf-8") if n else ""
        pos += n
        return s

    magic = data[pos : pos + 4]
    pos += 4
    if magic != b"C58B":
        raise ValueError("Existing pack is not C58B")
    _ver, _live = rd("<HH")
    ref_e, ref_n = rd("<dd")

    def rpt():
        de, dn, _ch, _off = rd("<ffff")
        return ref_e + de, ref_n + dn

    for _ in range(4):
        rstr()
    rd("<fffff")
    sec_n = rd("<B")[0]
    pos += sec_n
    cl_n = rd("<I")[0]
    pos += cl_n * 16
    rstr()
    uc, vr, *_ = rd("<IIffff")
    pos += uc * vr * 4

    roads_raw = []
    r_cnt = rd("<I")[0]
    for _ in range(r_cnt):
        wid = rd("<q")[0]
        rname, ref, htype, surface = rstr(), rstr(), rstr(), rstr()
        lanes, flags, _sec, width_r, length_m = rd("<BBBff")
        _mid = rpt()
        nc = rd("<H")[0]
        pts = [rpt() for _ in range(nc)]
        roads_raw.append((wid, rname, ref, htype, surface, lanes, flags, width_r, length_m, pts))

    houses_raw = []
    h_cnt = rd("<I")[0]
    for _ in range(h_cnt):
        hid = rd("<q")[0]
        hname, btype, amenity = rstr(), rstr(), rstr()
        levels, _sec, height_m, _elev_m, area_m2, perim_m, _res_off = rd("<BBfffff")
        cent_ue, cent_un = rpt()
        pc = rd("<H")[0]
        pts = [rpt() for _ in range(pc)]
        houses_raw.append((hid, hname, btype, amenity, levels, height_m, area_m2, perim_m, (cent_ue, cent_un), pts))

    waters_raw = []
    w_cnt = rd("<I")[0]
    for _ in range(w_cnt):
        wid = rd("<q")[0]
        wname, wtype = rstr(), rstr()
        _me, pc = rd("<fH")
        pts = [rpt() for _ in range(pc)]
        waters_raw.append((wid, wname, wtype, pts))

    pois_raw = []
    p_cnt = rd("<I")[0]
    for _ in range(p_cnt):
        nid = rd("<q")[0]
        pname, cat = rstr(), rstr()
        _sec, _elev = rd("<Bf")
        ue, un = rpt()
        pois_raw.append((nid, pname, cat, ue, un))

    return roads_raw, houses_raw, waters_raw, pois_raw


def build_c58_corridor_bin(extend_south: bool = False) -> None:
    print("\n=== Building Route C58 ±1.5km Non-Box Corridor Binary Pack ===", flush=True)
    centerline, is_live_cl, cl_ep = stitch_c58_centerline()
    print(f"Centerline ready: {len(centerline)} vertices (liveOsm={is_live_cl}, src={cl_ep}, S={centerline[0]}, N={centerline[-1]})", flush=True)

    # Build UTM ribbon matching C58CorridorEngine & C58CorridorRepository
    utm_cl = [latlon_to_utm37s(lat, lon) for lat, lon in centerline]
    ribbon = []  # (ce, cn, le, ln, re, rn, cumS)
    cum_s = 0.0
    for i in range(len(utm_cl)):
        if i > 0:
            cum_s += math.hypot(utm_cl[i][0] - utm_cl[i - 1][0], utm_cl[i][1] - utm_cl[i - 1][1])
        prev_pt = utm_cl[max(0, i - 1)]
        next_pt = utm_cl[min(len(utm_cl) - 1, i + 1)]
        te = next_pt[0] - prev_pt[0]
        tn = next_pt[1] - prev_pt[1]
        tl = max(1e-6, math.hypot(te, tn))
        te /= tl
        tn /= tl
        re, rn = tn, -te
        ce, cn = utm_cl[i]
        le, ln = ce - re * 1500.0, cn - rn * 1500.0
        rre, rrn = ce + re * 1500.0, cn + rn * 1500.0
        ribbon.append((ce, cn, le, ln, rre, rrn, cum_s))

    total_len_m = cum_s
    print(f"Total corridor length: {total_len_m / 1000.0:.2f} km", flush=True)

    seg_data = []
    for i in range(len(ribbon) - 1):
        a = ribbon[i]
        b = ribbon[i + 1]
        dx = b[0] - a[0]
        dy = b[1] - a[1]
        seg_lsq = dx * dx + dy * dy
        seg_len = math.sqrt(seg_lsq)
        if seg_len < 1e-6:
            continue
        min_e = min(a[0], b[0])
        max_e = max(a[0], b[0])
        min_n = min(a[1], b[1])
        max_n = max(a[1], b[1])
        seg_data.append((i, a[0], a[1], dx, dy, seg_lsq, seg_len, a[6], min_e, max_e, min_n, max_n))
    last_seg_i = len(ribbon) - 2

    def project_utm(utm_e: float, utm_n: float) -> Tuple[float, float, bool]:
        best_dsq = 1e30
        best_chain = 0.0
        best_off = 9999.0
        inside_ends = False
        for i, ae, an, dx, dy, seg_lsq, seg_len, a_ch, min_e, max_e, min_n, max_n in seg_data:
            de_box = (min_e - utm_e) if utm_e < min_e else ((utm_e - max_e) if utm_e > max_e else 0.0)
            dn_box = (min_n - utm_n) if utm_n < min_n else ((utm_n - max_n) if utm_n > max_n else 0.0)
            if de_box * de_box + dn_box * dn_box >= best_dsq:
                continue
            t_raw = ((utm_e - ae) * dx + (utm_n - an) * dy) / seg_lsq
            t_c = 0.0 if t_raw < 0.0 else (1.0 if t_raw > 1.0 else t_raw)
            ee = utm_e - (ae + t_c * dx)
            en = utm_n - (an + t_c * dy)
            dsq = ee * ee + en * en
            if dsq < best_dsq:
                best_dsq = dsq
                best_chain = a_ch + t_c * seg_len
                r_e = dy / seg_len
                r_n = -dx / seg_len
                sign = 1.0 if (ee * r_e + en * r_n >= 0.0) else -1.0
                best_off = sign * math.sqrt(dsq)
                before_start = (i == 0 and t_raw < -1500.0 / seg_len)
                after_end = (i == last_seg_i and t_raw > 1.0 + 1500.0 / seg_len)
                inside_ends = not (before_start or after_end)
        inside = inside_ends and abs(best_off) <= 1500.0
        return best_chain, best_off, inside

    def interp_ribbon(s_m: float):
        if s_m <= ribbon[0][6]:
            v = ribbon[0]
            return v[0], v[1], v[2], v[3], v[4], v[5]
        for i in range(len(ribbon) - 1):
            a = ribbon[i]
            b = ribbon[i + 1]
            if s_m <= b[6] or i == len(ribbon) - 2:
                span = max(1e-3, b[6] - a[6])
                t = max(0.0, min(1.0, (s_m - a[6]) / span))
                return (
                    a[0] + (b[0] - a[0]) * t,
                    a[1] + (b[1] - a[1]) * t,
                    a[2] + (b[2] - a[2]) * t,
                    a[3] + (b[3] - a[3]) * t,
                    a[4] + (b[4] - a[4]) * t,
                    a[5] + (b[5] - a[5]) * t,
                )
        v = ribbon[-1]
        return v[0], v[1], v[2], v[3], v[4], v[5]

    def sector_from_chainage(ch_m: float) -> int:
        frac = max(0.0, min(0.999999, ch_m / max(10000.0, total_len_m)))
        return min(10, max(1, int(frac * 10.0) + 1))

    # Fetch Curvilinear Terrarium Zoom-13 (31 x 220) covering ONLY the ±1.5 km swath
    print("Fetching Terrarium Zoom-13 (~19m/px) curvilinear DEM along C58 ribbon (31x220)...", flush=True)
    u_cols = 31
    v_rows = 220
    dem_vals: List[float] = []
    for iv in range(v_rows):
        s_m = (iv / (v_rows - 1)) * total_len_m
        _ce, _cn, le, ln, re, rn = interp_ribbon(s_m)
        for iu in range(u_cols):
            u_frac = iu / (u_cols - 1)
            ue = le + (re - le) * u_frac
            un = ln + (rn - ln) * u_frac
            lat, lon = utm37s_to_latlon(ue, un)
            dem_vals.append(sample_terrarium_elev(13, lat, lon))

    dem_min = min(dem_vals)
    dem_max = max(dem_vals)
    dem_mean = sum(dem_vals) / len(dem_vals)
    print(f"Terrarium Z13 C58 DEM: min={dem_min:.2f}m max={dem_max:.2f}m mean={dem_mean:.2f}m", flush=True)

    def sample_dem_at_chain(ch_m: float, lat_off_m: float) -> float:
        fv = max(0.0, min(1.0, ch_m / max(1.0, total_len_m))) * (v_rows - 1)
        fu = max(0.0, min(1.0, (lat_off_m + 1500.0) / 3000.0)) * (u_cols - 1)
        r0 = int(math.floor(fv))
        c0 = int(math.floor(fu))
        r1 = min(v_rows - 1, r0 + 1)
        c1 = min(u_cols - 1, c0 + 1)
        tv = fv - r0
        tu = fu - c0
        v00 = dem_vals[r0 * u_cols + c0]
        v10 = dem_vals[r0 * u_cols + c1]
        v01 = dem_vals[r1 * u_cols + c0]
        v11 = dem_vals[r1 * u_cols + c1]
        return (v00 * (1 - tu) + v10 * tu) * (1 - tv) + (v01 * (1 - tu) + v11 * tu) * tv

    def read_existing_magadi_town_features(mag_path: str):
        with open(mag_path, "rb") as f:
            data = f.read()
        pos = 0
        def rd(fmt: str):
            nonlocal pos
            sz = struct.calcsize(fmt)
            vals = struct.unpack_from(fmt, data, pos)
            pos += sz
            return vals
        def rstr() -> str:
            nonlocal pos
            n = rd("<H")[0]
            s = data[pos : pos + n].decode("utf-8") if n else ""
            pos += n
            return s
        if data[0:4] != b"MAGT":
            return [], [], [], []
        pos = 8
        _clat, _clon, _wm, _hm, _minlat, _maxlat, _minlon, _maxlon, oue, oun = rd("<dddddddddd")
        for _ in range(4):
            rstr()
        rd("<fffff")
        rstr()
        dc, dr, *_ = rd("<IIffff")
        pos += dc * dr * 4
        m_roads, m_bldgs, m_waters, m_pois = [], [], [], []
        rc = rd("<I")[0]
        for _ in range(rc):
            wid = rd("<q")[0]
            rname, ref, htype, surface = rstr(), rstr(), rstr(), rstr()
            lanes, flags, wr, lm, np = rd("<BBffI")
            pts = []
            for _ in range(np):
                xm, ym = rd("<ff")
                pts.append((oue + xm, oun + ym))
            m_roads.append((wid, rname, ref, htype, surface, lanes, flags, wr, lm, pts))
        bc = rd("<I")[0]
        for _ in range(bc):
            bid = rd("<q")[0]
            bname, btype, amenity = rstr(), rstr(), rstr()
            lv, hm2, _em, am, pm, cxm, cym, np = rd("<BffffffI")
            pts = []
            for _ in range(np):
                xm, ym = rd("<ff")
                pts.append((oue + xm, oun + ym))
            m_bldgs.append((bid, bname, btype, amenity, lv, hm2, am, pm, (oue + cxm, oun + cym), pts))
        wc = rd("<I")[0]
        for _ in range(wc):
            wid = rd("<q")[0]
            wname, wtype = rstr(), rstr()
            _me, np = rd("<fI")
            pts = []
            for _ in range(np):
                xm, ym = rd("<ff")
                pts.append((oue + xm, oun + ym))
            m_waters.append((wid, wname, wtype, pts))
        pc = rd("<I")[0]
        for _ in range(pc):
            nid = rd("<q")[0]
            pname, cat = rstr(), rstr()
            xm, ym, _el = rd("<fff")
            m_pois.append((nid, pname, cat, oue + xm, oun + ym))
        return m_roads, m_bldgs, m_waters, m_pois

    road_map = {}
    house_map = {}
    poi_map = {}
    water_map = {}
    last_ep = cl_ep

    def ingest_utm_features(eroads, ehouses, ewaters, epois):
        for wid, rname, ref, htype, surface, lanes, flags, width_r, _len_m, pts_en in eroads:
            pts = []
            for ue, un in pts_en:
                ch, off, inside = project_utm(ue, un)
                if inside and abs(off) <= 1505.0:
                    pts.append((0.0, 0.0, ue, un, ch, off, True))
            if len(pts) >= 2:
                length_m = sum(math.hypot(pts[k + 1][2] - pts[k][2], pts[k + 1][3] - pts[k][3]) for k in range(len(pts) - 1))
                mid = pts[len(pts) // 2]
                road_map[wid] = (wid, rname, ref, htype, surface, lanes, flags, width_r, length_m, sector_from_chainage(mid[4]), mid, pts)
        for hid, hname, btype, amenity, levels, height_m, area_m2, perim_m, (cue, cun), pts_en in ehouses:
            cch, coff, cin = project_utm(cue, cun)
            if not cin or abs(coff) > 1500.0:
                continue
            pts = []
            for ue, un in pts_en:
                ch, off, _ = project_utm(ue, un)
                pts.append((0.0, 0.0, ue, un, ch, off, True))
            if len(pts) >= 3:
                elev = sample_dem_at_chain(cch, coff)
                house_map[hid] = (hid, hname, btype, amenity, levels, height_m, elev, area_m2, perim_m, sector_from_chainage(cch), (0.0, 0.0, cue, cun, cch, coff), pts)
        for wid, wname, wtype, pts_en in ewaters:
            pts = []
            for ue, un in pts_en:
                ch, off, ins = project_utm(ue, un)
                pts.append((0.0, 0.0, ue, un, ch, off, ins))
            if any(p[6] and abs(p[5]) <= 1500.0 for p in pts):
                me = sum(sample_dem_at_chain(p[4], p[5]) for p in pts) / len(pts)
                water_map[wid] = (wid, wname, wtype, me, pts)
        for nid, pname, cat, ue, un in epois:
            ch, off, ins = project_utm(ue, un)
            if ins and abs(off) <= 1500.0:
                elev = sample_dem_at_chain(ch, off)
                poi_map[nid] = (nid, pname, cat, 0.0, 0.0, ue, un, ch, off, elev, sector_from_chainage(ch))

    bbox_jobs = []
    if os.path.exists(MAGADI_BIN_PATH):
        print(f"Seeding Magadi Town features (waters/POIs/roads/buildings) from {MAGADI_BIN_PATH}...", flush=True)
        mr, mh, mw, mp = read_existing_magadi_town_features(MAGADI_BIN_PATH)
        ingest_utm_features(mr, mh, mw, mp)

    if extend_south and os.path.exists(C58_BIN_PATH):
        print(f"Re-projecting existing features from {C58_BIN_PATH} onto corridor ribbon...", flush=True)
        eroads, ehouses, ewaters, epois = read_existing_c58_utm_features(C58_BIN_PATH)
        ingest_utm_features(eroads, ehouses, ewaters, epois)
        print(f"Seeded binary features: roads={len(road_map)} houses={len(house_map)} water={len(water_map)} pois={len(poi_map)}", flush=True)
        bands = [
            ("S1..S3 (Magadi -> Kwenia)", 0.0, 0.32),
            ("S3..S6 (Kwenia -> Tinga)", 0.30, 0.64),
        ]
    else:
        print(f"Seeded Magadi Town features: roads={len(road_map)} houses={len(house_map)} water={len(water_map)} pois={len(poi_map)}", flush=True)
        bands = [
            ("S1..S3 (Magadi -> Kwenia)", 0.0, 0.32),
            ("S3..S6 (Kwenia -> Tinga)", 0.30, 0.62),
            ("S6..S8 (Tinga -> Corner Baridi)", 0.60, 0.80),
            ("S8..S9 (Corner Baridi -> Kiserian)", 0.78, 0.90),
            ("S9..S10a (Kiserian -> Ongata Rongai)", 0.88, 0.95),
            ("S10b1 (Ongata Rongai -> Hardy)", 0.94, 0.97),
            ("S10b2 (Hardy -> Bomas)", 0.968, 1.00),
        ]

    for label, frac_a, frac_b in bands:
        s_a = frac_a * total_len_m
        s_b = min(total_len_m, frac_b * total_len_m + 350.0)
        lats, lons = [], []
        for k in range(16):
            sm = s_a + (k / 15.0) * (s_b - s_a)
            ce, cn, le, ln, re, rn = interp_ribbon(sm)
            for ue, un in ((ce, cn), (le, ln), (re, rn)):
                la, lo = utm37s_to_latlon(ue, un)
                lats.append(la)
                lons.append(lo)
        bbox_jobs.append((label, (min(lats) - 0.0028, min(lons) - 0.0028, max(lats) + 0.0028, max(lons) + 0.0028)))

    C58_LANDMARK_LANDUSES = {
        "industrial", "commercial", "education", "institutional",
        "retail", "quarry", "military", "religious",
    }

    def resolve_c58_poi_cat(tags: dict) -> str:
        name_l = (tags.get("name") or "").lower()
        mm = tags.get("man_made") or ""
        ind = tags.get("industrial") or ""
        lu = tags.get("landuse") or ""
        if mm in ("works", "factory", "plant") or "factory" in name_l or (ind and lu == "industrial"):
            return "factory"
        if ind or lu == "industrial":
            return f"industrial:{ind}" if ind else "industrial"
        return (
            tags.get("barrier")
            or tags.get("highway")
            or tags.get("place")
            or tags.get("amenity")
            or tags.get("office")
            or tags.get("craft")
            or mm
            or tags.get("aeroway")
            or tags.get("tourism")
            or tags.get("shop")
            or lu
            or "landmark"
        )

    def resolve_c58_house_props(tags: dict) -> Tuple[str, str]:
        raw_bldg = tags.get("building") or ""
        ind = tags.get("industrial") or ""
        mm = tags.get("man_made") or ""
        lu = tags.get("landuse") or ""
        am = tags.get("amenity") or ""
        off = tags.get("office") or ""
        craft = tags.get("craft") or ""
        shop = tags.get("shop") or ""
        tour = tags.get("tourism") or ""
        aero = tags.get("aeroway") or ""
        name_l = (tags.get("name") or "").lower()

        if raw_bldg and raw_bldg != "yes":
            b_type = raw_bldg
        elif mm in ("works", "factory", "plant") or "factory" in name_l:
            b_type = "factory"
        elif ind or lu == "industrial":
            b_type = "industrial"
        elif am:
            b_type = am
        elif mm:
            b_type = mm
        elif off:
            b_type = f"office:{off}" if off != "yes" else "office"
        elif craft:
            b_type = f"craft:{craft}" if craft != "yes" else "craft"
        elif tour:
            b_type = tour
        elif aero:
            b_type = aero
        elif shop:
            b_type = "retail"
        elif lu:
            b_type = lu
        else:
            b_type = raw_bldg or "house"

        base_am = am or (f"industrial:{ind}" if ind else "") or mm or lu or off or craft or shop or tour or aero
        extra_bits = []
        if base_am and b_type == "factory" and "factory" not in base_am:
            extra_bits.append("factory")
        for tk in ("resource", "product", "healthcare"):
            tv = (tags.get(tk) or "").strip()
            if tv and tv not in base_am:
                extra_bits.append(tv)
        amenity_val = " • ".join([p for p in ([base_am] + extra_bits) if p])
        return b_type, amenity_val

    def is_c58_landmark_area(tags: dict) -> bool:
        if any(k in tags for k in ("amenity", "industrial", "man_made", "office", "craft", "aeroway")):
            return True
        if tags.get("tourism") or tags.get("shop"):
            return True
        lu = tags.get("landuse") or ""
        if lu in C58_LANDMARK_LANDUSES and ("name" in tags or "resource" in tags or "operator" in tags):
            return True
        return False

    def c58_has_nearby_poi(name: str, ue: float, un: float, radius_m: float = 220.0) -> bool:
        nl = name.strip().lower()
        for existing in poi_map.values():
            if existing[1].strip().lower() == nl and math.hypot(existing[5] - ue, existing[6] - un) <= radius_m:
                return True
        return False

    def add_c58_house(hid: int, coords_raw, tags: dict, is_compound_only: bool = False) -> None:
        if hid in house_map or len(coords_raw) < 3:
            return
        poly = coords_raw[:-1] if (
            len(coords_raw) > 3
            and abs(coords_raw[0][2] - coords_raw[-1][2]) < 0.01
            and abs(coords_raw[0][3] - coords_raw[-1][3]) < 0.01
        ) else coords_raw
        ref_e, ref_n = poly[0][2], poly[0][3]
        area_sum = ce_sum = cn_sum = perim = 0.0
        for k in range(len(poly)):
            p0 = poly[k]
            p1 = poly[(k + 1) % len(poly)]
            x0, y0 = p0[2] - ref_e, p0[3] - ref_n
            x1, y1 = p1[2] - ref_e, p1[3] - ref_n
            cross = x0 * y1 - x1 * y0
            area_sum += cross
            ce_sum += (x0 + x1) * cross
            cn_sum += (y0 + y1) * cross
            perim += math.hypot(x1 - x0, y1 - y0)
        area = max(4.0, abs(area_sum * 0.5))
        if is_compound_only and area > 3_500_000.0:
            return
        if abs(area_sum) > 1e-3:
            cent_e = ref_e + ce_sum / (3.0 * area_sum)
            cent_n = ref_n + cn_sum / (3.0 * area_sum)
        else:
            cent_e = sum(p[2] for p in poly) / len(poly)
            cent_n = sum(p[3] for p in poly) / len(poly)
        cent_ch, cent_off, cent_in = project_utm(cent_e, cent_n)
        if not cent_in or abs(cent_off) > 1500.0:
            return
        cent_lat, cent_lon = utm37s_to_latlon(cent_e, cent_n)
        b_type, amenity = resolve_c58_house_props(tags)
        h_name = tags.get("name") or (
            amenity.split(" • ")[0].replace("_", " ").replace(":", " ").title()
            if amenity
            else (f"{b_type.replace('_', ' ').title()} #{hid}" if b_type != "yes" else f"House #{hid}")
        )
        try:
            levels = max(1, int(tags.get("building:levels", "1")))
        except ValueError:
            levels = 1
        try:
            hr = tags.get("height", "").replace("m", "").strip()
            height_m = float(hr) if hr else (levels * 3.2 if not is_compound_only else 5.5)
        except ValueError:
            height_m = levels * 3.2
        elev = sample_dem_at_chain(cent_ch, cent_off)
        sec_i = sector_from_chainage(cent_ch)
        house_map[hid] = (
            hid,
            h_name,
            b_type,
            amenity,
            levels,
            height_m,
            elev,
            area,
            perim,
            sec_i,
            (cent_lat, cent_lon, cent_e, cent_n, cent_ch, cent_off),
            coords_raw,
        )
        if (is_compound_only or is_c58_landmark_area(tags)) and tags.get("name"):
            synth_id = 100_000_000_000 + abs(hid)
            if synth_id not in poi_map and not c58_has_nearby_poi(h_name, cent_e, cent_n):
                cat = resolve_c58_poi_cat(tags)
                poi_map[synth_id] = (synth_id, h_name, cat, cent_lat, cent_lon, cent_e, cent_n, cent_ch, cent_off, elev, sec_i)

    for job_label, bbox_tuple in bbox_jobs:
        min_la, min_lo, max_la, max_lo = bbox_tuple
        bbox_s = f"{min_la:.5f},{min_lo:.5f},{max_la:.5f},{max_lo:.5f}"
        print(f"Querying Overpass {job_label} ({bbox_s})...", flush=True)
        q = f"""
        [out:json][timeout:40][maxsize:134217728];
        (
          way["highway"]({bbox_s});
          way["building"]({bbox_s});
          relation["building"]({bbox_s});
          way["amenity"]({bbox_s});
          relation["amenity"]({bbox_s});
          way["man_made"]({bbox_s});
          relation["man_made"]({bbox_s});
          way["industrial"]({bbox_s});
          relation["industrial"]({bbox_s});
          way["landuse"~"industrial|commercial|education|institutional|retail|quarry|military|religious"]({bbox_s});
          relation["landuse"~"industrial|commercial|education|institutional|retail|quarry|military|religious"]({bbox_s});
          way["office"]({bbox_s});
          way["tourism"]({bbox_s});
          way["shop"]({bbox_s});
          way["aeroway"]({bbox_s});
          way["natural"~"water|wetland|mud"]({bbox_s});
          way["landuse"~"salt_pond|reservoir"]({bbox_s});
          node["place"]({bbox_s});
          node["amenity"]({bbox_s});
          node["man_made"]({bbox_s});
          node["industrial"]({bbox_s});
          node["office"]({bbox_s});
          node["craft"]({bbox_s});
          node["tourism"]({bbox_s});
          node["shop"]({bbox_s});
          node["aeroway"]({bbox_s});
          node["barrier"]({bbox_s});
          node["highway"~"crossing|bus_stop|turning_circle|mini_roundabout|traffic_signals"]({bbox_s});
        );
        out geom;
        """.strip()
        try:
            obj, ep = query_overpass(q, timeout_s=45)
            els = obj.get("elements", [])
            last_ep = ep
            print(f"  Fetched {len(els)} elements from {ep}", flush=True)
        except Exception as exc:
            print(f"  Bbox warning for {job_label} (continuing): {exc}", flush=True)
            els = []

        for el in els:
            etype = el.get("type")
            if etype == "node":
                nid = el["id"]
                lat = el.get("lat")
                lon = el.get("lon")
                if lat is None or lon is None:
                    continue
                tags = el.get("tags") or {}
                if (
                    any(k in tags for k in ("place", "amenity", "man_made", "industrial", "office", "craft", "aeroway", "tourism", "shop", "name", "barrier"))
                    or tags.get("highway") in ("crossing", "bus_stop", "turning_circle", "mini_roundabout", "traffic_signals")
                ):
                    ue, un = latlon_to_utm37s(lat, lon)
                    ch, off, inside = project_utm(ue, un)
                    if inside and abs(off) <= 1500.0:
                        cat = resolve_c58_poi_cat(tags)
                        pname = tags.get("name") or cat.replace("_", " ").replace(":", " ").title()
                        elev = sample_dem_at_chain(ch, off)
                        poi_map[nid] = (nid, pname, cat, lat, lon, ue, un, ch, off, elev, sector_from_chainage(ch))
            elif etype == "way":
                wid = el["id"]
                geom = el.get("geometry") or []
                pts = []
                for g in geom:
                    lat = g.get("lat")
                    lon = g.get("lon")
                    if lat is None or lon is None:
                        continue
                    ue, un = latlon_to_utm37s(lat, lon)
                    ch, off, inside = project_utm(ue, un)
                    pts.append((lat, lon, ue, un, ch, off, inside))
                if len(pts) < 2:
                    continue
                tags = el.get("tags") or {}
                if "highway" in tags:
                    in_pts = [p for p in pts if p[6] and abs(p[5]) <= 1500.0]
                    if len(in_pts) < 2:
                        continue
                    htype = tags.get("highway") or "unclassified"
                    ref = tags.get("ref") or ""
                    rname = tags.get("name") or ref or f"Unnamed {htype.replace('_', ' ').title()} #{wid}"
                    surface = tags.get("surface") or (
                        "asphalt"
                        if htype in ("trunk", "primary", "secondary", "tertiary")
                        else ("compacted / paved" if htype in ("residential", "service") else "unpaved / murram")
                    )
                    try:
                        lanes = max(1, int(tags.get("lanes", "2" if htype in ("trunk", "primary", "secondary") else "1")))
                    except ValueError:
                        lanes = 1
                    one_way = tags.get("oneway") == "yes"
                    is_bridge = tags.get("bridge") in ("yes", "viaduct", "boardwalk")
                    is_roundabout = tags.get("junction") in ("roundabout", "circular")
                    flags = (1 if one_way else 0) | (2 if is_bridge else 0) | (4 if is_roundabout else 0)
                    width_r = (
                        8.5
                        if htype in ("trunk", "primary")
                        else (7.0 if htype in ("secondary", "tertiary") else (5.5 if htype in ("residential", "unclassified") else 3.5))
                    )
                    length_m = sum(math.hypot(in_pts[k + 1][2] - in_pts[k][2], in_pts[k + 1][3] - in_pts[k][3]) for k in range(len(in_pts) - 1))
                    mid = in_pts[len(in_pts) // 2]
                    sec_i = sector_from_chainage(mid[4])
                    road_map[wid] = (wid, rname, ref, htype, surface, lanes, flags, width_r, length_m, sec_i, mid, in_pts)
                elif "building" in tags and len(pts) >= 3:
                    add_c58_house(wid, pts, tags, is_compound_only=False)
                elif (tags.get("natural") in ("water", "wetland", "mud") or tags.get("landuse") in ("salt_pond", "reservoir")) and len(pts) >= 3:
                    if wid not in water_map and any(p[6] and abs(p[5]) <= 1500.0 for p in pts):
                        wtype = tags.get("water") or tags.get("landuse") or tags.get("natural") or "water"
                        wname = tags.get("name") or f"Water #{wid}"
                        mean_e = sum(sample_dem_at_chain(p[4], p[5]) for p in pts) / len(pts)
                        water_map[wid] = (wid, wname, wtype, mean_e, pts)
                elif is_c58_landmark_area(tags) and len(pts) >= 3:
                    add_c58_house(wid, pts, tags, is_compound_only=True)
            elif etype == "relation":
                rid = el["id"]
                tags = el.get("tags") or {}
                if "building" in tags or is_c58_landmark_area(tags):
                    for idx_m, m in enumerate(el.get("members") or []):
                        if m.get("type") == "way" and m.get("role") != "inner":
                            geom = m.get("geometry") or []
                            pts = []
                            for g in geom:
                                lat = g.get("lat")
                                lon = g.get("lon")
                                if lat is None or lon is None:
                                    continue
                                ue, un = latlon_to_utm37s(lat, lon)
                                ch, off, inside = project_utm(ue, un)
                                pts.append((lat, lon, ue, un, ch, off, inside))
                            if len(pts) >= 3:
                                add_c58_house(rid + 9_000_000_000 + idx_m, pts, tags, is_compound_only=("building" not in tags))
                                break

    all_roads = sorted(road_map.values(), key=lambda r: r[10][4])
    all_houses = sorted(house_map.values(), key=lambda h: h[10][4])
    all_pois = sorted(poi_map.values(), key=lambda p: p[7])
    all_waters = list(water_map.values())

    # Sector histogram debug print
    sec_h_counts = {s: 0 for s in range(1, 11)}
    for h in all_houses:
        sec_h_counts[h[9]] += 1
    print(f"Houses per sector S1..S10: {sec_h_counts}")

    if all_waters:
        lake_level = max(594.0, min(620.0, (sum(w[3] for w in all_waters) / len(all_waters)) + 0.6))
    else:
        lake_level = 602.5

    iso_now = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")

    out = bytearray()
    out.extend(b"C58B")
    out.extend(struct.pack("<HH", 1, 1 if is_live_cl else 0))
    out.extend(struct.pack("<dd", C58_UTM_REF_E, C58_UTM_REF_N))
    pack_str(out, "Route C58 (Magadi Road • ±1.5 km Non-Box Corridor)")
    pack_str(out, iso_now)
    pack_str(out, last_ep)
    pack_str(out, f"https://s3.amazonaws.com/elevation-tiles-prod/terrarium/13/{{x}}/{{y}}.png")
    out.extend(struct.pack("<fffff", 19.0, lake_level, dem_min, dem_max, dem_mean))

    # Extracted sectors bitmask (all 10)
    out.extend(struct.pack("<B", 10))
    for s_i in range(1, 11):
        out.extend(struct.pack("<B", s_i))

    # Centerline (lat, lon as float64)
    out.extend(struct.pack("<I", len(centerline)))
    for lat, lon in centerline:
        out.extend(struct.pack("<dd", lat, lon))

    # Curvilinear DEM Matrix (31 x 220)
    pack_str(out, "Terrarium ~19m/px Corridor DEM (31×220 Binary -> 1m C++ Bicubic)")
    out.extend(struct.pack("<IIffff", u_cols, v_rows, 19.0, dem_min, dem_max, dem_mean))
    out.extend(struct.pack(f"<{len(dem_vals)}f", *dem_vals))

    # Helper: compact C58Coord (dE, dN float32 rel to C58_UTM_REF + chainM, latOffM float32)
    def pack_c58_pt(ue: float, un: float, ch: float, off: float) -> None:
        out.extend(struct.pack("<ffff", ue - C58_UTM_REF_E, un - C58_UTM_REF_N, ch, off))

    # Roads
    out.extend(struct.pack("<I", len(all_roads)))
    for wid, rname, ref, htype, surface, lanes, flags, width_r, length_m, sec_i, mid, pts in all_roads:
        out.extend(struct.pack("<q", wid))
        pack_str(out, rname)
        pack_str(out, ref)
        pack_str(out, htype)
        pack_str(out, surface)
        out.extend(struct.pack("<BBBff", min(255, lanes), flags, sec_i, width_r, length_m))
        pack_c58_pt(mid[2], mid[3], mid[4], mid[5])
        out.extend(struct.pack("<H", min(65535, len(pts))))
        for pt in pts[:65535]:
            pack_c58_pt(pt[2], pt[3], pt[4], pt[5])

    # Houses / Buildings
    out.extend(struct.pack("<I", len(all_houses)))
    for hid, hname, btype, amenity, levels, height_m, elev_m, area_m2, perim_m, sec_i, cent, pts in all_houses:
        out.extend(struct.pack("<q", hid))
        pack_str(out, hname)
        pack_str(out, btype)
        pack_str(out, amenity)
        out.extend(struct.pack("<BBfffff", min(255, levels), sec_i, height_m, elev_m, area_m2, perim_m, cent[5]))
        pack_c58_pt(cent[2], cent[3], cent[4], cent[5])
        out.extend(struct.pack("<H", min(65535, len(pts))))
        for pt in pts[:65535]:
            pack_c58_pt(pt[2], pt[3], pt[4], pt[5])

    # Water Polygons
    out.extend(struct.pack("<I", len(all_waters)))
    for wid, wname, wtype, mean_e, pts in all_waters:
        out.extend(struct.pack("<q", wid))
        pack_str(out, wname)
        pack_str(out, wtype)
        out.extend(struct.pack("<fH", mean_e, min(65535, len(pts))))
        for pt in pts[:65535]:
            pack_c58_pt(pt[2], pt[3], pt[4], pt[5])

    # POIs & Road Symbol Nodes
    out.extend(struct.pack("<I", len(all_pois)))
    for nid, name, cat, _lat, _lon, ue, un, ch, off, elev, sec_i in all_pois:
        out.extend(struct.pack("<q", nid))
        pack_str(out, name)
        pack_str(out, cat)
        out.extend(struct.pack("<Bf", sec_i, elev))
        pack_c58_pt(ue, un, ch, off)

    with open(C58_BIN_PATH, "wb") as f:
        f.write(out)

    print(
        f"Wrote {C58_BIN_PATH} ({len(out):,} bytes): "
        f"roads={len(all_roads)}, houses={len(all_houses)}, water={len(all_waters)}, pois={len(all_pois)}"
    )


if __name__ == "__main__":
    c58_only = "--c58-only" in sys.argv
    extend_south = "--extend-south" in sys.argv
    import enrich_binary_gis_v2
    if not c58_only:
        build_magadi_town_bin()
        enrich_binary_gis_v2.upgrade_magt_to_v2(MAGADI_BIN_PATH)
    build_c58_corridor_bin(extend_south=extend_south)
    enrich_binary_gis_v2.upgrade_c58_to_v2(C58_BIN_PATH)
    print("\nBinary GIS asset pack generation (v2 enriched) complete.", flush=True)
