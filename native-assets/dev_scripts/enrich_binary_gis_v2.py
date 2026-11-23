#!/usr/bin/env python3
"""
Upgrades MAGT and C58B offline binary GIS packs from v1 to v2 in-place (preserving 100%
of existing OSM/DEM geometry and enriching every building with runtime-ready architectural,
compound containment, roof/wall/window, and road-collision-free perimeter fence & gate metadata).

Run from workspace root:
    python3 native-assets/dev_scripts/enrich_binary_gis_v2.py
"""

from __future__ import annotations

import math
import os
import random
import struct
from typing import Dict, List, Optional, Tuple

MAGADI_BIN_PATH = os.path.join("native-assets", "assets", "terrain_grid", "magadi_town_3x5km.bin")
C58_BIN_PATH = os.path.join("native-assets", "assets", "terrain_grid", "c58_corridor_3km.bin")

# ── Canonical v2 Enum Catalogs (Keep in exact 1:1 sync with C++ & Kotlin) ───────

SHAPE_TYPES = (
    "SQUARE",             # 0
    "RECTANGULAR",        # 1
    "L_SHAPE",            # 2
    "U_SHAPE",            # 3
    "T_SHAPE",            # 4
    "STAR_CROSS",         # 5
    "CIRCULAR_SILO",      # 6
    "POLYGON_IRREGULAR",  # 7
)

ROOF_TYPES = (
    "GABLED",               # 0
    "HIPPED",               # 1
    "FLAT_PARAPET",         # 2
    "SAW_TOOTH_INDUSTRIAL", # 3
    "MONO_PITCH_SHED",      # 4
    "ARCHED_TRUSS",         # 5
    "CONICAL_DOME",         # 6
)

ROOF_MATERIALS = (
    "CORRUGATED_IRON_MABATI",  # 0
    "STONE_COATED_TILE",       # 1
    "REINFORCED_CONCRETE",     # 2
    "GALVANIZED_TRUSS_SHEET",  # 3
    "CLAY_TILE",               # 4
    "THATCH_MAASAI",           # 5
)

WALL_TYPES = (
    "CONCRETE_BLOCK_PLASTER",   # 0
    "CORRUGATED_IRON_CLADDING", # 1
    "DRESSED_MASONRY_STONE",    # 2
    "PRECAST_INDUSTRIAL_PANEL", # 3
    "TIMBER_PLANK",             # 4
    "BRICK_MASONRY",            # 5
)

WINDOW_TYPES = (
    "SLIDING_ALUMINUM",      # 0
    "STEEL_CASEMENT",        # 1
    "INDUSTRIAL_CLERESTORY", # 2
    "LOUVERED_VENT",         # 3
    "CURTAIN_WALL",          # 4
    "NARROW_SLIT",           # 5
)

FENCE_TYPES = (
    "NONE",                    # 0
    "CHAIN_LINK_POSTS",        # 1
    "MASONRY_BLOCK_WALL",      # 2
    "TIMBER_POST_RAIL",        # 3
    "LIVE_THORN_HEDGE",        # 4
    "PALISADE_SECURITY_STEEL", # 5
)

GATE_TYPES = (
    "NONE",                  # 0
    "SLIDING_STEEL_GATE",    # 1
    "DOUBLE_SWING_TIMBER",   # 2
    "SECURITY_BOOM_BARRIER", # 3
    "PEDESTRIAN_WICKET",     # 4
)

# ── 2D Geometry Utilities ────────────────────────────────────────────────────────

Pt = Tuple[float, float]


def clean_ring(pts: List[Pt]) -> List[Pt]:
    if len(pts) >= 4 and math.hypot(pts[0][0] - pts[-1][0], pts[0][1] - pts[-1][1]) < 0.05:
        pts = pts[:-1]
    out: List[Pt] = []
    for p in pts:
        if not out or math.hypot(p[0] - out[-1][0], p[1] - out[-1][1]) >= 0.15:
            out.append(p)
    if len(out) >= 3 and math.hypot(out[0][0] - out[-1][0], out[0][1] - out[-1][1]) < 0.15:
        out.pop()
    return out


def signed_area(pts: List[Pt]) -> float:
    n = len(pts)
    if n < 3:
        return 0.0
    s = 0.0
    for i in range(n):
        x0, y0 = pts[i]
        x1, y1 = pts[(i + 1) % n]
        s += x0 * y1 - x1 * y0
    return 0.5 * s


def ensure_ccw(pts: List[Pt]) -> List[Pt]:
    return list(reversed(pts)) if signed_area(pts) < 0.0 else list(pts)


def poly_perimeter(pts: List[Pt]) -> float:
    n = len(pts)
    if n < 2:
        return 0.0
    return sum(math.hypot(pts[(i + 1) % n][0] - pts[i][0], pts[(i + 1) % n][1] - pts[i][1]) for i in range(n))


def point_in_poly(x: float, y: float, poly: List[Pt]) -> bool:
    inside = False
    n = len(poly)
    j = n - 1
    for i in range(n):
        xi, yi = poly[i]
        xj, yj = poly[j]
        if ((yi > y) != (yj > y)) and (x < (xj - xi) * (y - yi) / ((yj - yi) if abs(yj - yi) > 1e-9 else 1e-9) + xi):
            inside = not inside
        j = i
    return inside


def point_seg_dist_sq(px: float, py: float, ax: float, ay: float, bx: float, by: float) -> float:
    dx = bx - ax
    dy = by - ay
    lsq = dx * dx + dy * dy
    if lsq <= 1e-10:
        return (px - ax) ** 2 + (py - ay) ** 2
    t = ((px - ax) * dx + (py - ay) * dy) / lsq
    if t < 0.0:
        t = 0.0
    elif t > 1.0:
        t = 1.0
    rx = px - (ax + t * dx)
    ry = py - (ay + t * dy)
    return rx * rx + ry * ry


def _ccw(ax: float, ay: float, bx: float, by: float, cx: float, cy: float) -> float:
    return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)


def segments_intersect(ax: float, ay: float, bx: float, by: float, cx: float, cy: float, dx: float, dy: float) -> bool:
    if max(ax, bx) < min(cx, dx) or max(cx, dx) < min(ax, bx):
        return False
    if max(ay, by) < min(cy, dy) or max(cy, dy) < min(ay, by):
        return False
    d1 = _ccw(cx, cy, dx, dy, ax, ay)
    d2 = _ccw(cx, cy, dx, dy, bx, by)
    d3 = _ccw(ax, ay, bx, by, cx, cy)
    d4 = _ccw(ax, ay, bx, by, dx, dy)
    if ((d1 > 0 and d2 < 0) or (d1 < 0 and d2 > 0)) and ((d3 > 0 and d4 < 0) or (d3 < 0 and d4 > 0)):
        return True
    eps = 1e-7
    if abs(d1) <= eps and min(cx, dx) - eps <= ax <= max(cx, dx) + eps and min(cy, dy) - eps <= ay <= max(cy, dy) + eps:
        return True
    if abs(d2) <= eps and min(cx, dx) - eps <= bx <= max(cx, dx) + eps and min(cy, dy) - eps <= by <= max(cy, dy) + eps:
        return True
    if abs(d3) <= eps and min(ax, bx) - eps <= cx <= max(ax, bx) + eps and min(ay, by) - eps <= cy <= max(ay, by) + eps:
        return True
    if abs(d4) <= eps and min(ax, bx) - eps <= dx <= max(ax, bx) + eps and min(ay, by) - eps <= dy <= max(ay, by) + eps:
        return True
    return False


def seg_seg_dist_sq(ax: float, ay: float, bx: float, by: float, cx: float, cy: float, dx: float, dy: float) -> float:
    if segments_intersect(ax, ay, bx, by, cx, cy, dx, dy):
        return 0.0
    return min(
        point_seg_dist_sq(ax, ay, cx, cy, dx, dy),
        point_seg_dist_sq(bx, by, cx, cy, dx, dy),
        point_seg_dist_sq(cx, cy, ax, ay, bx, by),
        point_seg_dist_sq(dx, dy, ax, ay, bx, by),
    )


def simplify_corners(pts: List[Pt], cos_thresh: float = 0.985) -> List[Pt]:
    pts = clean_ring(pts)
    n = len(pts)
    if n <= 4:
        return pts
    kept: List[Pt] = []
    for i in range(n):
        p0 = pts[(i - 1) % n]
        p1 = pts[i]
        p2 = pts[(i + 1) % n]
        v1x, v1y = p1[0] - p0[0], p1[1] - p0[1]
        v2x, v2y = p2[0] - p1[0], p2[1] - p1[1]
        l1 = math.hypot(v1x, v1y)
        l2 = math.hypot(v2x, v2y)
        if l1 < 1e-4 or l2 < 1e-4:
            continue
        dot = (v1x * v2x + v1y * v2y) / (l1 * l2)
        if dot < cos_thresh:
            kept.append(p1)
    return kept if len(kept) >= 3 else pts


def compute_obb_stats(pts: List[Pt]) -> Tuple[float, float, float]:
    """Returns (obb_w, obb_h, obb_area)."""
    n = len(pts)
    if n < 3:
        return 1.0, 1.0, 1.0
    best_area = 1e30
    best_w = 1.0
    best_h = 1.0
    sample_stride = 1 if n <= 24 else max(1, n // 16)
    for i in range(0, n, sample_stride):
        p0 = pts[i]
        p1 = pts[(i + 1) % n]
        dx = p1[0] - p0[0]
        dy = p1[1] - p0[1]
        L = math.hypot(dx, dy)
        if L < 1e-5:
            continue
        ux, uy = dx / L, dy / L
        vx, vy = -uy, ux
        min_u = min_v = 1e18
        max_u = max_v = -1e18
        for px, py in pts:
            u = px * ux + py * uy
            v = px * vx + py * vy
            if u < min_u:
                min_u = u
            if u > max_u:
                max_u = u
            if v < min_v:
                min_v = v
            if v > max_v:
                max_v = v
        w = max(0.5, max_u - min_u)
        h = max(0.5, max_v - min_v)
        ar = w * h
        if ar < best_area:
            best_area = ar
            best_w = w
            best_h = h
    return best_w, best_h, best_area


def classify_shape(pts_raw: List[Pt], area: float, perim: float, btype: str, name: str) -> int:
    pts = simplify_corners(pts_raw)
    n = len(pts)
    nl = name.lower()
    bt = btype.lower()
    isop = (4.0 * math.pi * area) / max(1.0, perim * perim)
    if bt in ("storage_tank", "silo", "water_tower", "hut") or "silo" in nl or "tank" in nl:
        if isop >= 0.72 or n >= 7:
            return 6  # CIRCULAR_SILO
    if isop >= 0.86 and n >= 7:
        return 6  # CIRCULAR_SILO

    w, h, obb_area = compute_obb_stats(pts)
    aspect = max(w, h) / max(0.5, min(w, h))
    fill = area / max(0.5, obb_area)

    if n <= 4:
        return 0 if aspect <= 1.28 else 1
    if n == 5 and fill >= 0.76:
        return 0 if aspect <= 1.28 else 1
    if n == 6:
        if fill < 0.82:
            return 2  # L_SHAPE
        return 0 if aspect <= 1.28 else 1
    if n in (7, 8):
        if fill < 0.74:
            # Distinguish L / T / U via longest vs 2nd longest edge symmetry
            lens = sorted(
                (math.hypot(pts[(i + 1) % n][0] - pts[i][0], pts[(i + 1) % n][1] - pts[i][1]) for i in range(n)),
                reverse=True,
            )
            if len(lens) >= 3 and lens[1] > 0.72 * lens[0] and aspect > 1.25:
                return 3  # U_SHAPE
            return 4 if fill > 0.56 else 2  # T_SHAPE or L_SHAPE
        return 1 if aspect > 1.28 else 0
    if n >= 9 and fill < 0.68:
        return 5  # STAR_CROSS
    if fill >= 0.80:
        return 0 if aspect <= 1.28 else 1
    return 7  # POLYGON_IRREGULAR


# ── Road Spatial Grid Index ──────────────────────────────────────────────────────

class RoadSpatialIndex:
    """Exact spatial hash storing (ax, ay, bx, by, clearance_m) for all road segments."""

    CELL = 25.0

    def __init__(self, road_segments: List[Tuple[float, float, float, float, float]]):
        # seg = (ax, ay, bx, by, clearance_m, minx, maxx, miny, maxy)
        self.segs: List[Tuple[float, float, float, float, float, float, float, float, float]] = []
        self.grid: Dict[Tuple[int, int], List[int]] = {}
        cell = self.CELL
        for idx, (ax, ay, bx, by, clr) in enumerate(road_segments):
            minx = min(ax, bx) - clr - 4.0
            maxx = max(ax, bx) + clr + 4.0
            miny = min(ay, by) - clr - 4.0
            maxy = max(ay, by) + clr + 4.0
            self.segs.append((ax, ay, bx, by, clr, minx, maxx, miny, maxy))
            gx0 = int(math.floor(minx / cell))
            gx1 = int(math.floor(maxx / cell))
            gy0 = int(math.floor(miny / cell))
            gy1 = int(math.floor(maxy / cell))
            for gx in range(gx0, gx1 + 1):
                for gy in range(gy0, gy1 + 1):
                    self.grid.setdefault((gx, gy), []).append(idx)

    def query_box(self, minx: float, miny: float, maxx: float, maxy: float) -> List[int]:
        cell = self.CELL
        gx0 = int(math.floor(minx / cell))
        gx1 = int(math.floor(maxx / cell))
        gy0 = int(math.floor(miny / cell))
        gy1 = int(math.floor(maxy / cell))
        seen = set()
        out: List[int] = []
        for gx in range(gx0, gx1 + 1):
            for gy in range(gy0, gy1 + 1):
                bucket = self.grid.get((gx, gy))
                if bucket:
                    for s_i in bucket:
                        if s_i not in seen:
                            seen.add(s_i)
                            out.append(s_i)
        return out

    def min_clearance_margin_for_point(self, px: float, py: float, candidates: Optional[List[int]] = None) -> Tuple[float, float, int]:
        """Returns (signed_margin = dist - clr, raw_dist, seg_idx). Negative margin = inside road clearance."""
        if candidates is None:
            candidates = self.query_box(px - 18.0, py - 18.0, px + 18.0, py + 18.0)
        best_margin = 1e9
        best_dist = 1e9
        best_idx = -1
        for s_i in candidates:
            ax, ay, bx, by, clr, minx, maxx, miny, maxy = self.segs[s_i]
            if px < minx or px > maxx or py < miny or py > maxy:
                continue
            d = math.sqrt(point_seg_dist_sq(px, py, ax, ay, bx, by))
            m = d - clr
            if m < best_margin:
                best_margin = m
                best_dist = d
                best_idx = s_i
        return best_margin, best_dist, best_idx

    def nearest_road_edge_for_point(self, px: float, py: float, radius_m: float = 180.0) -> Tuple[float, int]:
        cands = self.query_box(px - radius_m, py - radius_m, px + radius_m, py + radius_m)
        best_dsq = 1e18
        best_idx = -1
        for s_i in cands:
            ax, ay, bx, by, _clr, *_ = self.segs[s_i]
            dsq = point_seg_dist_sq(px, py, ax, ay, bx, by)
            if dsq < best_dsq:
                best_dsq = dsq
                best_idx = s_i
        return (math.sqrt(best_dsq) if best_idx >= 0 else 1e9), best_idx

    def poly_clears_roads(
        self,
        poly: List[Pt],
        road_edge_margin_m: float = 4.8,
        min_centerline_m: float = 6.0,
    ) -> bool:
        n = len(poly)
        if n < 3:
            return False
        minx = min(p[0] for p in poly) - 18.0
        maxx = max(p[0] for p in poly) + 18.0
        miny = min(p[1] for p in poly) - 18.0
        maxy = max(p[1] for p in poly) + 18.0
        cands = self.query_box(minx, miny, maxx, maxy)
        if not cands:
            return True
        for i in range(n):
            p0 = poly[i]
            p1 = poly[(i + 1) % n]
            ex0, ex1 = min(p0[0], p1[0]), max(p0[0], p1[0])
            ey0, ey1 = min(p0[1], p1[1]), max(p0[1], p1[1])
            for s_i in cands:
                ax, ay, bx, by, hw, smx0, smx1, smy0, smy1 = self.segs[s_i]
                clr = max(min_centerline_m, hw + road_edge_margin_m)
                if ex1 < min(ax, bx) - clr or ex0 > max(ax, bx) + clr or ey1 < min(ay, by) - clr or ey0 > max(ay, by) + clr:
                    continue
                dsq = seg_seg_dist_sq(p0[0], p0[1], p1[0], p1[1], ax, ay, bx, by)
                if dsq < clr * clr:
                    return False
        return True

    def fence_clears_all_roads(self, fence: List[Pt]) -> bool:
        return self.poly_clears_roads(fence, road_edge_margin_m=1.0, min_centerline_m=3.2)

    def poly_edge_to_road_distances(self, poly: List[Pt]) -> Tuple[float, float, float, float, float]:
        """
        Returns (min_dist_to_road_edge, min_dist_to_road_ctr, dir_ux, dir_uy, road_hw)
        across all segments of poly. `segs[i][4]` must store road half-width `hw`.
        """
        if not poly:
            return 1e9, 1e9, 1.0, 0.0, 3.0
        minx = min(p[0] for p in poly) - 65.0
        maxx = max(p[0] for p in poly) + 65.0
        miny = min(p[1] for p in poly) - 65.0
        maxy = max(p[1] for p in poly) + 65.0
        cands = self.query_box(minx, miny, maxx, maxy)
        if not cands:
            return 1e9, 1e9, 1.0, 0.0, 3.0
        best_edge = 1e9
        best_ctr = 1e9
        best_ux, best_uy = 1.0, 0.0
        best_hw = 3.0
        n = len(poly)
        for i in range(n):
            p0 = poly[i]
            p1 = poly[(i + 1) % n]
            for s_i in cands:
                ax, ay, bx, by, hw, *_ = self.segs[s_i]
                d = math.sqrt(seg_seg_dist_sq(p0[0], p0[1], p1[0], p1[1], ax, ay, bx, by))
                d_edge = d - hw
                if d_edge < best_edge:
                    best_edge = d_edge
                    best_ctr = d
                    best_hw = hw
                    rl = math.hypot(bx - ax, by - ay)
                    if rl > 1e-4:
                        best_ux, best_uy = (bx - ax) / rl, (by - ay) / rl
        return best_edge, best_ctr, best_ux, best_uy, best_hw


# ── Outward Miter Offset & Road-Safe Fence Builder ───────────────────────────────

def offset_polygon_ccw(pts_ccw: List[Pt], margin_m: float) -> List[Pt]:
    n = len(pts_ccw)
    if n < 3:
        return []
    out: List[Pt] = []
    max_miter = margin_m * 2.1
    for i in range(n):
        p_prev = pts_ccw[(i - 1) % n]
        p_curr = pts_ccw[i]
        p_next = pts_ccw[(i + 1) % n]

        e1x, e1y = p_curr[0] - p_prev[0], p_curr[1] - p_prev[1]
        e2x, e2y = p_next[0] - p_curr[0], p_next[1] - p_curr[1]
        l1 = math.hypot(e1x, e1y)
        l2 = math.hypot(e2x, e2y)
        if l1 < 1e-4 or l2 < 1e-4:
            continue
        # Outward normals for CCW ring point RIGHT of directed edge: (dy, -dx)
        n1x, n1y = e1y / l1, -e1x / l1
        n2x, n2y = e2y / l2, -e2x / l2
        bx, by = n1x + n2x, n1y + n2y
        bl = math.hypot(bx, by)
        if bl < 1e-4:
            out.append((p_curr[0] + n1x * margin_m, p_curr[1] + n1y * margin_m))
        else:
            bx /= bl
            by /= bl
            denom = max(0.35, bx * n1x + by * n1y)
            scale = min(max_miter, margin_m / denom)
            out.append((p_curr[0] + bx * scale, p_curr[1] + by * scale))
    return out


def densify_ring(pts: List[Pt], max_seg_m: float = 7.0, max_pts: int = 38) -> List[Pt]:
    n = len(pts)
    if n < 3:
        return pts
    out: List[Pt] = []
    budget = max_pts - n
    for i in range(n):
        a = pts[i]
        b = pts[(i + 1) % n]
        out.append(a)
        L = math.hypot(b[0] - a[0], b[1] - a[1])
        if L > max_seg_m and budget > 0:
            subs = min(budget + 1, int(math.ceil(L / max_seg_m)))
            for k in range(1, subs):
                t = k / float(subs)
                out.append((a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t))
                budget -= 1
    return out


class BuildingSpatialIndex:
    """Spatial grid for neighbor-safe fence sizing and <=2m proximity compound clustering."""

    CELL = 24.0

    def __init__(self, polys: List[List[Pt]], areas: List[float]):
        self.polys = polys
        self.areas = areas
        self.bboxes: List[Tuple[float, float, float, float]] = []
        self.grid: Dict[Tuple[int, int], List[int]] = {}
        cell = self.CELL
        for idx, pts in enumerate(polys):
            if not pts:
                self.bboxes.append((0.0, 0.0, 0.0, 0.0))
                continue
            xs = [p[0] for p in pts]
            ys = [p[1] for p in pts]
            minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
            self.bboxes.append((minx, maxx, miny, maxy))
            if areas[idx] > 15_000.0:
                continue
            for gx in range(int(math.floor(minx / cell)), int(math.floor(maxx / cell)) + 1):
                for gy in range(int(math.floor(miny / cell)), int(math.floor(maxy / cell)) + 1):
                    self.grid.setdefault((gx, gy), []).append(idx)

    def query_box(self, minx: float, miny: float, maxx: float, maxy: float) -> List[int]:
        cell = self.CELL
        seen = set()
        out: List[int] = []
        for gx in range(int(math.floor(minx / cell)), int(math.floor(maxx / cell)) + 1):
            for gy in range(int(math.floor(miny / cell)), int(math.floor(maxy / cell)) + 1):
                for b_i in self.grid.get((gx, gy), ()):
                    if b_i not in seen:
                        seen.add(b_i)
                        out.append(b_i)
        return out

    def overlaps_any_external(self, poly: List[Pt], excluded_indices: set) -> bool:
        if len(poly) < 3:
            return True
        xs = [p[0] for p in poly]
        ys = [p[1] for p in poly]
        minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
        cands = self.query_box(minx - 1.5, miny - 1.5, maxx + 1.5, maxy + 1.5)
        np = len(poly)
        for o_i in cands:
            if o_i in excluded_indices:
                continue
            ox0, ox1, oy0, oy1 = self.bboxes[o_i]
            if ox1 < minx or ox0 > maxx or oy1 < miny or oy0 > maxy:
                continue
            opts = self.polys[o_i]
            if not opts:
                continue
            # Check vertex inside fence OR fence corner inside neighbor OR segment crossing
            if any(point_in_poly(px, py, poly) for px, py in opts):
                return True
            if any(point_in_poly(fx, fy, opts) for fx, fy in poly):
                return True
            no = len(opts)
            for i in range(np):
                ax, ay = poly[i]
                bx, by = poly[(i + 1) % np]
                for j in range(no):
                    cx, cy = opts[j]
                    dx, dy = opts[(j + 1) % no]
                    if seg_seg_dist_sq(ax, ay, bx, by, cx, cy, dx, dy) < 0.15 * 0.15:
                        return True
        return False


def convex_hull_ccw(pts: List[Pt]) -> List[Pt]:
    uniq = sorted(set((round(x, 3), round(y, 3)) for x, y in pts))
    if len(uniq) <= 3:
        return ensure_ccw(uniq)
    lower: List[Pt] = []
    for p in uniq:
        while len(lower) >= 2 and _ccw(lower[-2][0], lower[-2][1], lower[-1][0], lower[-1][1], p[0], p[1]) <= 0.0:
            lower.pop()
        lower.append(p)
    upper: List[Pt] = []
    for p in reversed(uniq):
        while len(upper) >= 2 and _ccw(upper[-2][0], upper[-2][1], upper[-1][0], upper[-1][1], p[0], p[1]) <= 0.0:
            upper.pop()
        upper.append(p)
    return lower[:-1] + upper[:-1]


def poly_min_dist_sq(p1: List[Pt], p2: List[Pt]) -> float:
    md = 1e18
    n1 = len(p1)
    n2 = len(p2)
    for i in range(n1):
        ax, ay = p1[i]
        bx, by = p1[(i + 1) % n1]
        for j in range(n2):
            cx, cy = p2[j]
            dx, dy = p2[(j + 1) % n2]
            d2 = seg_seg_dist_sq(ax, ay, bx, by, cx, cy, dx, dy)
            if d2 < md:
                md = d2
                if md <= 1e-6:
                    return 0.0
    return md


def cluster_close_buildings(
    polys: List[List[Pt]],
    areas: List[float],
    centroids: List[Pt],
    eligible_mask: List[bool],
    bldg_index: BuildingSpatialIndex,
    road_index: RoadSpatialIndex,
    max_gap_m: float = 2.25,
    max_members: int = 12,
    max_span_m: float = 68.0,
) -> Dict[int, List[int]]:
    """
    Groups fence-eligible houses within <= 2.2m edge-to-edge (excluding pairs separated by a road)
    into shared homestead/plot compounds capped at natural plot span/count.
    Returns map: cluster_root_or_rep_idx -> list of member indices (len >= 1).
    """
    n = len(polys)
    parent = list(range(n))
    comp_members: Dict[int, List[int]] = {i: [i] for i in range(n) if eligible_mask[i]}
    comp_bbox: Dict[int, Tuple[float, float, float, float]] = {
        i: bldg_index.bboxes[i] for i in range(n) if eligible_mask[i]
    }

    def find(x: int) -> int:
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    # Gather close eligible pairs sorted by shortest gap first
    gap_sq_lim = max_gap_m * max_gap_m
    cand_pairs: List[Tuple[float, int, int]] = []
    seen = set()
    for i in range(n):
        if not eligible_mask[i] or len(polys[i]) < 3:
            continue
        ax0, ax1, ay0, ay1 = bldg_index.bboxes[i]
        for j in bldg_index.query_box(ax0 - max_gap_m - 0.5, ay0 - max_gap_m - 0.5, ax1 + max_gap_m + 0.5, ay1 + max_gap_m + 0.5):
            if j <= i or not eligible_mask[j] or len(polys[j]) < 3:
                continue
            pair = (i, j)
            if pair in seen:
                continue
            seen.add(pair)
            bx0, bx1, by0, by1 = bldg_index.bboxes[j]
            if ax1 + max_gap_m < bx0 or bx1 + max_gap_m < ax0 or ay1 + max_gap_m < by0 or by1 + max_gap_m < ay0:
                continue
            d2 = poly_min_dist_sq(polys[i], polys[j])
            if d2 <= gap_sq_lim:
                # Reject if a road passes between their centroids
                c1x, c1y = centroids[i]
                c2x, c2y = centroids[j]
                rcands = road_index.query_box(min(c1x, c2x) - 6.0, min(c1y, c2y) - 6.0, max(c1x, c2x) + 6.0, max(c1y, c2y) + 6.0)
                road_splits = False
                for s_i in rcands:
                    rax, ray, rbx, rby, *_ = road_index.segs[s_i]
                    if segments_intersect(c1x, c1y, c2x, c2y, rax, ray, rbx, rby):
                        road_splits = True
                        break
                if not road_splits:
                    cand_pairs.append((d2, i, j))

    cand_pairs.sort(key=lambda t: t[0])
    for _d2, i, j in cand_pairs:
        ri, rj = find(i), find(j)
        if ri == rj:
            continue
        mi, mj = comp_members[ri], comp_members[rj]
        if len(mi) + len(mj) > max_members:
            continue
        bi0, bi1, bi2, bi3 = comp_bbox[ri]
        bj0, bj1, bj2, bj3 = comp_bbox[rj]
        nx0, nx1, ny0, ny1 = min(bi0, bj0), max(bi1, bj1), min(bi2, bj2), max(bi3, bj3)
        if (nx1 - nx0) > max_span_m or (ny1 - ny0) > max_span_m:
            continue
        if len(mi) < len(mj):
            ri, rj = rj, ri
            mi, mj = mj, mi
        parent[rj] = ri
        mi.extend(mj)
        comp_bbox[ri] = (nx0, nx1, ny0, ny1)
        del comp_members[rj]
        del comp_bbox[rj]

    return comp_members


def choose_plot_axes(cluster_pts: List[Pt], cx: float, cy: float, road_index: RoadSpatialIndex) -> Tuple[float, float]:
    """Orients compound fence frame along nearest road tangent (if within 55m), else OBB principal axis."""
    _d_edge, d_ctr, rux, ruy, _hw = road_index.poly_edge_to_road_distances([(cx, cy)])
    if d_ctr < 55.0:
        return rux, ruy
    n = len(cluster_pts)
    if n < 3:
        return 1.0, 0.0
    best_area = 1e30
    best_u = (1.0, 0.0)
    stride = 1 if n <= 20 else max(1, n // 16)
    for i in range(0, n, stride):
        p0 = cluster_pts[i]
        p1 = cluster_pts[(i + 1) % n]
        dx, dy = p1[0] - p0[0], p1[1] - p0[1]
        L = math.hypot(dx, dy)
        if L < 1e-4:
            continue
        ux, uy = dx / L, dy / L
        vx, vy = -uy, ux
        us = [px * ux + py * uy for px, py in cluster_pts]
        vs = [px * vx + py * vy for px, py in cluster_pts]
        ar = max(0.5, max(us) - min(us)) * max(0.5, max(vs) - min(vs))
        if ar < best_area:
            best_area = ar
            best_u = (ux, uy)
    return best_u


def build_natural_compound_fence_and_gate(
    member_polys: List[List[Pt]],
    member_indices_set: set,
    cluster_cent: Pt,
    total_bldg_area: float,
    btype: str,
    is_named_compound: bool,
    parent_compound_id: int,
    road_index: RoadSpatialIndex,
    bldg_index: BuildingSpatialIndex,
    rng: random.Random,
) -> Tuple[bool, int, float, List[Pt], bool, int, float, float, Optional[Pt]]:
    """
    Constructs a natural plot/compound fence (oriented rectilinear compound polygon, NOT house-hugging)
    with a ~6m road setback, zero fences for standalone buildings sitting within 6.2m of a road edge,
    and guaranteed road-facing gate placement.
    """
    bt = btype.lower()
    NON_FENCED = {
        "parking", "kiosk", "commercial_kiosk", "petrol_canopy", "market_stall",
        "shop_row", "retail", "ruins", "quarry", "bridge", "weighbridge_kiosk",
    }
    if bt in NON_FENCED or total_bldg_area < 24.0 or total_bldg_area > 550_000.0:
        return False, 0, 0.0, [], False, 0, 0.0, 0.0, None
    # Inner children of a named compound or secondary co-members share the primary compound fence
    if parent_compound_id != 0:
        return False, 0, 0.0, [], False, 0, 0.0, 0.0, None
    if is_named_compound and total_bldg_area < 450.0:
        return False, 0, 0.0, [], False, 0, 0.0, 0.0, None

    # 1. Near-road check: standalone buildings fronting < 6.2m from road edge have NO fence
    if not is_named_compound:
        for mp in member_polys:
            d_edge, _d_ctr, _ux, _uy, _hw = road_index.poly_edge_to_road_distances(mp)
            if d_edge < 6.2:
                return False, 0, 0.0, [], False, 0, 0.0, 0.0, None

    # 2. Pick coherent fence & gate styles
    if is_named_compound or bt in (
        "factory", "industrial", "industrial_plant", "warehouse", "power_substation",
        "police", "police_post", "barracks", "hospital", "school", "college", "university",
    ):
        fence_id = rng.choice([5, 2, 1])  # PALISADE_SECURITY_STEEL / MASONRY_BLOCK_WALL / CHAIN_LINK_POSTS
        fence_h = round(rng.uniform(2.1, 2.7), 2)
        desired_gate_w = rng.uniform(5.2, 7.6)
        gate_id = 3 if fence_id == 5 else 1
    elif len(member_polys) > 1 or bt in ("villa", "apartments", "apartment_block", "townhouse_court", "swahili_bungalow"):
        fence_id = rng.choice([2, 2, 5, 1])  # MASONRY_BLOCK_WALL dominant for shared/villa compounds
        fence_h = round(rng.uniform(1.85, 2.4), 2)
        desired_gate_w = rng.uniform(3.6, 4.8)
        gate_id = 1  # SLIDING_STEEL_GATE
    elif bt in ("maasai_boma_house", "hut", "homestead_cottage"):
        fence_id = rng.choice([4, 3, 1])  # LIVE_THORN_HEDGE / TIMBER_POST_RAIL / CHAIN_LINK_POSTS
        fence_h = round(rng.uniform(1.25, 1.65), 2)
        desired_gate_w = rng.uniform(2.4, 3.4)
        gate_id = rng.choice([2, 4])
    else:
        fence_id = rng.choice([1, 2, 3, 4])
        fence_h = round(rng.uniform(1.5, 2.15), 2)
        desired_gate_w = rng.uniform(3.2, 4.4)
        gate_id = 1 if fence_id in (1, 2, 5) else 2

    chosen_fence: Optional[List[Pt]] = None

    if is_named_compound:
        # Large named OSM compounds keep their simplified compound perimeter polygon (outset slightly)
        pts_ccw = ensure_ccw(simplify_corners(member_polys[0], cos_thresh=0.992))
        if len(pts_ccw) < 3:
            return False, 0, 0.0, [], False, 0, 0.0, 0.0, None
        for margin in (1.2, 0.5):
            raw_out = offset_polygon_ccw(pts_ccw, margin)
            if len(raw_out) < 3:
                continue
            minx = min(p[0] for p in raw_out) - 12.0
            maxx = max(p[0] for p in raw_out) + 12.0
            miny = min(p[1] for p in raw_out) - 12.0
            maxy = max(p[1] for p in raw_out) + 12.0
            near_roads = road_index.query_box(minx, miny, maxx, maxy)
            adjusted: List[Pt] = []
            ok = True
            for vx, vy in raw_out:
                m_clr = 1e9
                for s_i in near_roads:
                    ax, ay, bx, by, hw, *_ = road_index.segs[s_i]
                    d = math.sqrt(point_seg_dist_sq(vx, vy, ax, ay, bx, by))
                    m = d - (hw + 2.0)
                    if m < m_clr:
                        m_clr = m
                if m_clr >= 0.0:
                    adjusted.append((vx, vy))
                else:
                    dx, dy = cluster_cent[0] - vx, cluster_cent[1] - vy
                    dc = math.hypot(dx, dy)
                    if dc < 1.0:
                        ok = False
                        break
                    shift = min(dc * 0.45, -m_clr + 0.5)
                    adjusted.append((vx + (dx / dc) * shift, vy + (dy / dc) * shift))
            if ok and len(adjusted) >= 3 and road_index.poly_clears_roads(adjusted, road_edge_margin_m=1.0, min_centerline_m=3.2):
                chosen_fence = adjusted
                break
    else:
        # Natural oriented rectilinear plot boundary around 1..8 clustered houses (never hugs L/T/circle corners!)
        cluster_pts = [pt for mp in member_polys for pt in mp]
        if len(cluster_pts) < 3:
            return False, 0, 0.0, [], False, 0, 0.0, 0.0, None

        ux, uy = choose_plot_axes(cluster_pts, cluster_cent[0], cluster_cent[1], road_index)
        vx, vy = -uy, ux

        us = [px * ux + py * uy for px, py in cluster_pts]
        vs = [px * vx + py * vy for px, py in cluster_pts]
        u0, u1 = min(us), max(us)
        v0, v1 = min(vs), max(vs)

        # Generous natural homestead/plot margins (not close to house)
        pad_u0 = rng.uniform(4.2, 6.8)
        pad_u1 = rng.uniform(4.2, 6.8)
        pad_v0 = rng.uniform(4.8, 7.6)
        pad_v1 = rng.uniform(4.8, 7.6)

        # If a road faces a side within ~20m, snap that side cleanly near the ~6m road setback line
        near_ext = bldg_index.query_box(
            min(p[0] for p in cluster_pts) - 18.0,
            min(p[1] for p in cluster_pts) - 18.0,
            max(p[0] for p in cluster_pts) + 18.0,
            max(p[1] for p in cluster_pts) + 18.0,
        )
        for o_i in near_ext:
            if o_i in member_indices_set or bldg_index.areas[o_i] > 15_000.0:
                continue
            opts = bldg_index.polys[o_i]
            if not opts:
                continue
            ous = [px * ux + py * uy for px, py in opts]
            ovs = [px * vx + py * vy for px, py in opts]
            eu0, eu1, ev0, ev1 = min(ous), max(ous), min(ovs), max(ovs)
            # Check primary separation axis in plot (u, v) frame
            ov_u = min(u1, eu1) - max(u0, eu0)
            ov_v = min(v1, ev1) - max(v0, ev0)
            if ov_v > -3.5:
                if eu0 >= u1 - 0.5:
                    pad_u1 = min(pad_u1, max(0.50, (eu0 - u1) * 0.44))
                elif eu1 <= u0 + 0.5:
                    pad_u0 = min(pad_u0, max(0.50, (u0 - eu1) * 0.44))
            if ov_u > -3.5:
                if ev0 >= v1 - 0.5:
                    pad_v1 = min(pad_v1, max(0.50, (ev0 - v1) * 0.44))
                elif ev1 <= v0 + 0.5:
                    pad_v0 = min(pad_v0, max(0.50, (v0 - ev1) * 0.44))

        def build_uv_box(pu0: float, pu1: float, pv0: float, pv1: float) -> List[Pt]:
            cu0, cu1, cv0, cv1 = u0 - pu0, u1 + pu1, v0 - pv0, v1 + pv1
            return ensure_ccw([
                (cu0 * ux + cv0 * vx, cu0 * uy + cv0 * vy),
                (cu1 * ux + cv0 * vx, cu1 * uy + cv0 * vy),
                (cu1 * ux + cv1 * vx, cu1 * uy + cv1 * vy),
                (cu0 * ux + cv1 * vx, cu0 * uy + cv1 * vy),
            ])

        # Helper to check whether side s is clear of roads at target ~6m road setback
        def side_clears_roads(p_a: Pt, p_b: Pt) -> bool:
            cands = road_index.query_box(
                min(p_a[0], p_b[0]) - 14.0, min(p_a[1], p_b[1]) - 14.0,
                max(p_a[0], p_b[0]) + 14.0, max(p_a[1], p_b[1]) + 14.0,
            )
            for s_i in cands:
                ax, ay, bx, by, hw, *_ = road_index.segs[s_i]
                clr = max(6.0, hw + 4.8)
                if seg_seg_dist_sq(p_a[0], p_a[1], p_b[0], p_b[1], ax, ay, bx, by) < clr * clr:
                    return False
            return True

        # Binary search each side inward if needed so roadside sits right on the 6m road setback line
        for _pass in range(2):
            box_now = [pad_u0, pad_u1, pad_v0, pad_v1]
            sides = [
                (0, lambda v: (u0 - v, u0 - v, v0 - box_now[2], v1 + box_now[3])),
                (1, lambda v: (u1 + v, u1 + v, v0 - box_now[2], v1 + box_now[3])),
                (2, lambda v: (u0 - box_now[0], u1 + box_now[1], v0 - v, v0 - v)),
                (3, lambda v: (u0 - box_now[0], u1 + box_now[1], v1 + v, v1 + v)),
            ]
            for s_idx, edge_fn in sides:
                cu_a, cu_b, cv_a, cv_b = edge_fn(box_now[s_idx])
                pa = (cu_a * ux + cv_a * vx, cu_a * uy + cv_a * vy)
                pb = (cu_b * ux + cv_b * vx, cu_b * uy + cv_b * vy)
                if not side_clears_roads(pa, pb):
                    lo, hi = 0.85, box_now[s_idx]
                    for _ in range(10):
                        mid = 0.5 * (lo + hi)
                        cu_a, cu_b, cv_a, cv_b = edge_fn(mid)
                        pa = (cu_a * ux + cv_a * vx, cu_a * uy + cv_a * vy)
                        pb = (cu_b * ux + cv_b * vx, cu_b * uy + cv_b * vy)
                        if side_clears_roads(pa, pb):
                            lo = mid
                        else:
                            hi = mid
                    box_now[s_idx] = lo
            pad_u0, pad_u1, pad_v0, pad_v1 = box_now

        # Try oriented rectilinear compound box first (with gentle uniform shrinkage if diagonal corners clash)
        for sc in (1.0, 0.80, 0.60, 0.45):
            cand = build_uv_box(
                max(0.45, pad_u0 * sc),
                max(0.45, pad_u1 * sc),
                max(0.45, pad_v0 * sc),
                max(0.45, pad_v1 * sc),
            )
            if (
                road_index.poly_clears_roads(cand, road_edge_margin_m=4.8, min_centerline_m=6.0)
                and not bldg_index.overlaps_any_external(cand, member_indices_set)
            ):
                chosen_fence = cand
                break

        # Convex hull compound fallback if concave cluster L-notch trapped an external neighbor
        if chosen_fence is None:
            hull = simplify_corners(convex_hull_ccw(cluster_pts), cos_thresh=0.992)
            if len(hull) >= 3:
                cx_h = sum(p[0] for p in hull) / len(hull)
                cy_h = sum(p[1] for p in hull) / len(hull)
                for margin in (3.8, 2.2, 1.1, 0.6):
                    raw_out = offset_polygon_ccw(hull, margin)
                    if len(raw_out) < 3:
                        continue
                    retracted: List[Pt] = []
                    near_r = road_index.query_box(
                        min(p[0] for p in raw_out) - 16.0, min(p[1] for p in raw_out) - 16.0,
                        max(p[0] for p in raw_out) + 16.0, max(p[1] for p in raw_out) + 16.0,
                    )
                    near_b = bldg_index.query_box(
                        min(p[0] for p in raw_out) - 6.0, min(p[1] for p in raw_out) - 6.0,
                        max(p[0] for p in raw_out) + 6.0, max(p[1] for p in raw_out) + 6.0,
                    )
                    ok = True
                    for px, py in raw_out:
                        dx, dy = cx_h - px, cy_h - py
                        dc = math.hypot(dx, dy)
                        if dc < 0.5:
                            ok = False
                            break
                        ux_c, uy_c = dx / dc, dy / dc
                        # Check needed pullback for roads & external buildings
                        max_pull = 0.0
                        for s_i in near_r:
                            ax, ay, bx, by, hw, *_ = road_index.segs[s_i]
                            d = math.sqrt(point_seg_dist_sq(px, py, ax, ay, bx, by))
                            clr = max(6.0, hw + 4.8)
                            if d < clr:
                                max_pull = max(max_pull, clr - d + 0.3)
                        for o_i in near_b:
                            if o_i in member_indices_set or bldg_index.areas[o_i] > 15_000.0:
                                continue
                            opts = bldg_index.polys[o_i]
                            for k in range(len(opts)):
                                ax, ay = opts[k]
                                bx, by = opts[(k + 1) % len(opts)]
                                d = math.sqrt(point_seg_dist_sq(px, py, ax, ay, bx, by))
                                if point_in_poly(px, py, opts) or d < 0.45:
                                    max_pull = max(max_pull, min(margin - 0.35, 0.55 + d))
                        if max_pull > 0.0:
                            if max_pull >= dc * 0.65:
                                ok = False
                                break
                            nx, ny = px + ux_c * max_pull, py + uy_c * max_pull
                            if not point_in_poly(nx, ny, hull):
                                retracted.append((nx, ny))
                            else:
                                ok = False
                                break
                        else:
                            retracted.append((px, py))
                    if ok and len(retracted) >= 3:
                        if (
                            road_index.poly_clears_roads(retracted, road_edge_margin_m=4.8, min_centerline_m=6.0)
                            and not bldg_index.overlaps_any_external(retracted, member_indices_set)
                        ):
                            chosen_fence = retracted
                            break

    if chosen_fence is None:
        return False, 0, 0.0, [], False, 0, 0.0, 0.0, None

    # Guaranteed single gate placement on road-facing fence edge
    nf = len(chosen_fence)
    best_edge_score = 1e18
    best_mid: Pt = chosen_fence[0]
    best_angle = 0.0
    best_edge_len = 4.0

    for i in range(nf):
        p0 = chosen_fence[i]
        p1 = chosen_fence[(i + 1) % nf]
        elen = math.hypot(p1[0] - p0[0], p1[1] - p0[1])
        if elen < 1.2:
            continue
        mx = 0.5 * (p0[0] + p1[0])
        my = 0.5 * (p0[1] + p1[1])
        _de, rdist, *_ = road_index.poly_edge_to_road_distances([(mx, my)])
        len_penalty = 0.0 if elen >= 2.8 else 5.0
        score = rdist + len_penalty
        if score < best_edge_score:
            best_edge_score = score
            best_mid = (mx, my)
            best_angle = math.atan2(p1[1] - p0[1], p1[0] - p0[0])
            best_edge_len = elen

    gate_w = round(max(1.4, min(desired_gate_w, best_edge_len * 0.72)), 2)
    return True, fence_id, fence_h, chosen_fence, True, gate_id, gate_w, best_angle, best_mid


# ── Semantic & Envelope Synthesizer ──────────────────────────────────────────────

COMPOUND_KEYWORDS = (
    "factory", "soda", "works", "industrial", "school", "academy", "college",
    "university", "hospital", "clinic", "police", "campus", "seminary", "quarry",
    "shopping", "marketplace", "water works", "safari", "kbc", "cathedral", "church",
)


def classify_compound_domain(name: str, btype: str, amenity: str) -> str:
    txt = f"{name} {btype} {amenity}".lower()
    if any(k in txt for k in ("police", "gsu", "military", "prison", "security")):
        return "police"
    if any(k in txt for k in ("factory", "soda", "works", "industrial", "quarry", "water_works", "water works", "transmission", "radio")):
        return "factory"
    if any(k in txt for k in ("hospital", "clinic", "health", "dispensary", "medical")):
        return "hospital"
    if any(k in txt for k in ("university", "college", "school", "academy", "seminary", "polytechnic", "institute")):
        return "school"
    if any(k in txt for k in ("shopping", "mall", "market", "retail", "commercial")):
        return "commercial"
    if any(k in txt for k in ("church", "cathedral", "mosque", "chapel", "place_of_worship")):
        return "religious"
    return "institutional"


def synthesize_building_semantics_and_envelope(
    bid: int,
    orig_name: str,
    orig_btype: str,
    orig_amenity: str,
    orig_levels: int,
    orig_height_m: float,
    area_m2: float,
    shape_id: int,
    parent_id: int,
    parent_name: str,
    parent_domain: str,
    dist_to_road_m: float,
    rng: random.Random,
) -> Tuple[str, str, int, float, float, int, float, int, int, int, float]:
    """
    Returns:
      name, btype, levels, height_m, floor_h,
      roof_id, roof_pitch_deg, roof_mat_id, wall_id, window_id, wwr
    """
    name = orig_name
    btype = orig_btype
    is_generic_name = (
        not name
        or name.startswith("House #")
        or name.startswith("Building #")
        or name.startswith("Yes #")
    )
    is_generic_type = btype.lower() in ("yes", "building", "house", "")

    # 1. Assign realistic semantic sub-type & label
    if parent_id != 0 and is_generic_type:
        if parent_domain == "factory":
            if shape_id == 6:
                btype, lbl = "storage_silo", "Soda Ash / Brine Silo"
            elif area_m2 >= 900.0:
                btype, lbl = rng.choice([
                    ("industrial_plant", "Calciner & Evaporator Hall"),
                    ("industrial_plant", "Soda Processing Plant"),
                    ("warehouse", "Bulk Soda Warehouse"),
                ])
            elif area_m2 >= 220.0:
                btype, lbl = rng.choice([
                    ("workshop", "Mechanical Maintenance Bay"),
                    ("boiler_house", "Steam & Boiler House"),
                    ("warehouse", "Packaging & Dispatch Store"),
                ])
            elif area_m2 >= 70.0:
                btype, lbl = rng.choice([
                    ("control_room", "Process Control Room"),
                    ("power_substation", "Factory Substation"),
                    ("pump_house", "Brine Booster Pump House"),
                ])
            else:
                btype, lbl = rng.choice([
                    ("weighbridge_kiosk", "Weighbridge Check Post"),
                    ("security_kiosk", "Factory Gatehouse"),
                    ("pump_house", "Utility Valve Shed"),
                ])
        elif parent_domain == "police":
            if area_m2 >= 260.0:
                btype, lbl = rng.choice([
                    ("barracks", "Police Training Barracks"),
                    ("assembly_hall", "Parade & Lecture Hall"),
                ])
            elif area_m2 >= 90.0:
                btype, lbl = rng.choice([
                    ("police_post", "Command & Ops Block"),
                    ("staff_quarters", "Officers Quarters"),
                    ("mess_hall", "Cadet Mess Hall"),
                ])
            else:
                btype, lbl = rng.choice([
                    ("guard_house", "Quartermaster & Armory Store"),
                    ("security_kiosk", "Security Sentry Post"),
                ])
        elif parent_domain == "school":
            if area_m2 >= 260.0:
                btype, lbl = rng.choice([
                    ("tuition_block", "Classroom & Tuition Block"),
                    ("dormitory", "Student Dormitory Wing"),
                    ("laboratory", "Science & Computer Lab"),
                ])
            elif area_m2 >= 90.0:
                btype, lbl = rng.choice([
                    ("admin_office", "Principal & Admin Block"),
                    ("library", "Library & Resource Hall"),
                    ("dining_hall", "Dining & Kitchen Hall"),
                ])
            else:
                btype, lbl = rng.choice([
                    ("staff_quarters", "Staff House"),
                    ("security_kiosk", "Campus Gatehouse"),
                    ("ablution_block", "Campus Utility Block"),
                ])
        elif parent_domain == "hospital":
            if area_m2 >= 220.0:
                btype, lbl = rng.choice([
                    ("ward_block", "Inpatient Ward Wing"),
                    ("outpatient_wing", "Outpatient & Triage Block"),
                ])
            elif area_m2 >= 70.0:
                btype, lbl = rng.choice([
                    ("laboratory", "Diagnostic Lab & Pharmacy"),
                    ("admin_office", "Hospital Admin Block"),
                ])
            else:
                btype, lbl = ("generator_house", "Backup Power & Oxygen Shed")
        elif parent_domain == "commercial":
            btype, lbl = ("shop_row", "Commercial Shop Arcade") if area_m2 >= 120.0 else ("commercial_kiosk", "Market Stall Block")
        elif parent_domain == "religious":
            btype, lbl = ("chapel", "Sanctuary & Prayer Hall") if area_m2 >= 200.0 else ("parish_hall", "Parish Wing")
        else:
            btype, lbl = ("institutional_block", "Campus Facility Block")
        if is_generic_name:
            short_p = parent_name.split(",")[0][:24]
            name = f"{short_p} • {lbl} #{abs(bid) % 1000}"
    elif is_generic_type:
        # Standalone structure diversification based on footprint, shape, & road adjacency
        if shape_id == 6 and area_m2 < 45.0:
            btype = "maasai_boma_house"
        elif dist_to_road_m < 28.0 and area_m2 < 110.0 and shape_id == 1 and rng.random() < 0.32:
            btype = "shop_row" if area_m2 >= 45.0 else "commercial_kiosk"
        elif area_m2 >= 420.0:
            btype = rng.choice(["apartment_block", "commercial_block", "warehouse"])
        elif area_m2 >= 210.0:
            btype = rng.choice(["apartment_block", "villa", "commercial_block"])
        elif area_m2 >= 115.0:
            btype = "villa" if shape_id in (2, 3, 4) else rng.choice(["residential_house", "villa", "townhouse_court"])
        elif area_m2 >= 50.0:
            btype = rng.choice(["residential_house", "swahili_bungalow", "detached_house"])
        else:
            btype = rng.choice(["homestead_cottage", "residential_house", "detached_house"])

        if is_generic_name:
            name = f"{btype.replace('_', ' ').title()} #{bid}"

    bt = btype.lower()

    # 2. Floors & Heights
    if orig_levels > 1:
        levels = orig_levels
        floor_h = round(max(2.8, orig_height_m / levels), 2)
        height_m = round(max(orig_height_m, levels * floor_h), 2)
    else:
        if bt in ("apartment_block", "apartments"):
            levels = rng.choices([2, 3, 4, 5], weights=[30, 38, 22, 10])[0]
            floor_h = round(rng.uniform(2.95, 3.15), 2)
        elif bt == "commercial_block":
            levels = rng.choices([2, 3, 4], weights=[40, 42, 18])[0]
            floor_h = round(rng.uniform(3.3, 3.8), 2)
        elif bt in ("tuition_block", "dormitory", "ward_block", "barracks", "hotel", "college", "university"):
            levels = rng.choices([1, 2, 3], weights=[35, 45, 20])[0]
            floor_h = round(rng.uniform(3.2, 3.6), 2)
        elif bt in ("villa", "townhouse_court"):
            levels = rng.choices([1, 2], weights=[45, 55])[0]
            floor_h = round(rng.uniform(3.0, 3.3), 2)
        elif bt in ("industrial_plant", "factory", "industrial", "warehouse", "workshop", "boiler_house"):
            levels = 1 if area_m2 < 1200.0 else rng.choice([1, 2])
            floor_h = round(rng.uniform(5.2, 8.8), 2)
        elif bt == "storage_silo":
            levels = 1
            floor_h = round(rng.uniform(11.0, 21.0), 2)
        elif bt in ("shop_row", "retail", "commercial_kiosk", "kiosk", "weighbridge_kiosk", "security_kiosk", "pump_house"):
            levels = 1
            floor_h = round(rng.uniform(2.8, 3.6), 2)
        elif bt in ("maasai_boma_house", "hut", "homestead_cottage"):
            levels = 1
            floor_h = round(rng.uniform(2.5, 3.1), 2)
        else:
            levels = 1 if area_m2 < 150.0 else rng.choices([1, 2], weights=[72, 28])[0]
            floor_h = round(rng.uniform(2.9, 3.3), 2)
        height_m = round(levels * floor_h, 2)

    # 3. Architectural Envelope: Roof, RoofMaterial, Wall, Window, WWR
    if shape_id == 6 or bt in ("storage_silo", "maasai_boma_house", "hut"):
        roof_id = 6  # CONICAL_DOME
        roof_pitch = round(rng.uniform(22.0, 32.0), 1)
        if bt == "storage_silo":
            roof_mat = 3  # GALVANIZED_TRUSS_SHEET
            wall_id = 3   # PRECAST_INDUSTRIAL_PANEL
            window_id = 5 # NARROW_SLIT
            wwr = 0.06
        else:
            roof_mat = rng.choice([5, 0])  # THATCH_MAASAI / CORRUGATED_IRON_MABATI
            wall_id = rng.choice([4, 0])   # TIMBER_PLANK / CONCRETE_BLOCK_PLASTER
            window_id = 3                  # LOUVERED_VENT
            wwr = round(rng.uniform(0.08, 0.14), 2)
    elif bt in ("industrial_plant", "factory", "industrial", "warehouse", "workshop", "boiler_house", "power_substation"):
        roof_id = rng.choice([3, 5, 4])  # SAW_TOOTH_INDUSTRIAL / ARCHED_TRUSS / MONO_PITCH_SHED
        roof_pitch = round(rng.uniform(10.0, 20.0), 1)
        roof_mat = 3                     # GALVANIZED_TRUSS_SHEET
        wall_id = rng.choice([1, 3])     # CORRUGATED_IRON_CLADDING / PRECAST_INDUSTRIAL_PANEL
        window_id = 2                    # INDUSTRIAL_CLERESTORY
        wwr = round(rng.uniform(0.14, 0.24), 2)
    elif bt in ("apartment_block", "apartments", "commercial_block"):
        roof_id = rng.choice([2, 2, 1])  # FLAT_PARAPET / HIPPED
        roof_pitch = 2.0 if roof_id == 2 else round(rng.uniform(18.0, 25.0), 1)
        roof_mat = 2 if roof_id == 2 else 1  # REINFORCED_CONCRETE / STONE_COATED_TILE
        wall_id = rng.choice([0, 2, 5])  # CONCRETE_BLOCK_PLASTER / DRESSED_MASONRY_STONE / BRICK_MASONRY
        window_id = 4 if bt == "commercial_block" and rng.random() < 0.4 else 0  # CURTAIN_WALL / SLIDING_ALUMINUM
        wwr = round(rng.uniform(0.28, 0.48), 2)
    elif bt in ("tuition_block", "dormitory", "ward_block", "outpatient_wing", "barracks", "police_post", "admin_office", "library", "hospital", "school", "college", "university"):
        roof_id = rng.choice([0, 1, 2])
        roof_pitch = 2.0 if roof_id == 2 else round(rng.uniform(18.0, 28.0), 1)
        roof_mat = 2 if roof_id == 2 else rng.choice([0, 1, 4])
        wall_id = rng.choice([2, 0, 5])
        window_id = rng.choice([1, 0, 3])  # STEEL_CASEMENT / SLIDING_ALUMINUM / LOUVERED_VENT
        wwr = round(rng.uniform(0.24, 0.36), 2)
    elif bt in ("shop_row", "commercial_kiosk", "kiosk", "weighbridge_kiosk", "security_kiosk", "pump_house", "generator_house"):
        roof_id = rng.choice([4, 2, 0])  # MONO_PITCH_SHED / FLAT_PARAPET / GABLED
        roof_pitch = 2.0 if roof_id == 2 else round(rng.uniform(8.0, 16.0), 1)
        roof_mat = 0 if roof_id != 2 else 2
        wall_id = rng.choice([0, 1, 5])
        window_id = rng.choice([1, 3, 5])
        wwr = round(rng.uniform(0.12, 0.25), 2)
    elif bt in ("villa", "townhouse_court", "swahili_bungalow"):
        roof_id = rng.choice([1, 0])     # HIPPED / GABLED
        roof_pitch = round(rng.uniform(22.0, 32.0), 1)
        roof_mat = rng.choice([1, 4, 0]) # STONE_COATED_TILE / CLAY_TILE / MABATI
        wall_id = rng.choice([2, 0, 5])  # DRESSED_MASONRY_STONE / CONCRETE_BLOCK_PLASTER / BRICK
        window_id = rng.choice([0, 1])   # SLIDING_ALUMINUM / STEEL_CASEMENT
        wwr = round(rng.uniform(0.22, 0.34), 2)
    else:
        # residential_house / detached_house / homestead_cottage
        roof_id = rng.choice([0, 1, 4])  # GABLED / HIPPED / MONO_PITCH_SHED
        roof_pitch = round(rng.uniform(16.0, 27.0), 1)
        roof_mat = rng.choice([0, 0, 1, 4])
        wall_id = rng.choice([0, 2, 5, 4])
        window_id = rng.choice([1, 0, 3])
        wwr = round(rng.uniform(0.16, 0.28), 2)

    return name, btype, levels, height_m, floor_h, roof_id, roof_pitch, roof_mat, wall_id, window_id, wwr


def build_compound_spatial_index(items) -> Tuple[List[int], Dict[Tuple[int, int], List[int]]]:
    """items = list of (bid, bname, btype, amenity, area_m2, cent_x, cent_y, pts_xy)"""
    comp_indices: List[int] = []
    grid: Dict[Tuple[int, int], List[int]] = {}
    cell = 50.0
    for idx, (bid, bname, btype, amenity, area_m2, _cx, _cy, pts) in enumerate(items):
        if len(pts) < 3 or area_m2 < 900.0 or area_m2 > 2_500_000.0:
            continue
        txt = f"{bname} {btype} {amenity}".lower()
        if "parking" in txt:
            continue
        is_cand = any(k in txt for k in COMPOUND_KEYWORDS)
        if not is_cand:
            continue
        comp_indices.append(idx)
        minx = min(p[0] for p in pts)
        maxx = max(p[0] for p in pts)
        miny = min(p[1] for p in pts)
        maxy = max(p[1] for p in pts)
        for gx in range(int(math.floor(minx / cell)), int(math.floor(maxx / cell)) + 1):
            for gy in range(int(math.floor(miny / cell)), int(math.floor(maxy / cell)) + 1):
                grid.setdefault((gx, gy), []).append(idx)
    return comp_indices, grid


def find_enclosing_compound(
    idx: int,
    cent_x: float,
    cent_y: float,
    area_m2: float,
    items,
    comp_grid: Dict[Tuple[int, int], List[int]],
) -> Tuple[int, str, str, float]:
    """Returns (parent_id, parent_name, domain, parent_area). (0, '', '', 0.0) if none."""
    cell = 50.0
    cands = comp_grid.get((int(math.floor(cent_x / cell)), int(math.floor(cent_y / cell))))
    if not cands:
        return 0, "", "", 0.0
    best_idx = -1
    best_area = 1e18
    for c_i in cands:
        if c_i == idx:
            continue
        c_bid, c_name, c_type, c_am, c_area, _cx, _cy, c_pts = items[c_i]
        if area_m2 >= c_area * 0.72:
            continue
        if c_area < best_area and point_in_poly(cent_x, cent_y, c_pts):
            best_area = c_area
            best_idx = c_i
    if best_idx < 0:
        return 0, "", "", 0.0
    c_bid, c_name, c_type, c_am, c_area, *_ = items[best_idx]
    return c_bid, c_name, classify_compound_domain(c_name, c_type, c_am), c_area


# ── Upgrade MAGT -> v2 ───────────────────────────────────────────────────────────

def upgrade_magt_to_v2(path: str) -> None:
    data = open(path, "rb").read()
    pos = 0

    def rd(fmt: str):
        nonlocal pos
        sz = struct.calcsize(fmt)
        res = struct.unpack_from(fmt, data, pos)
        pos += sz
        return res

    def rstr() -> str:
        nonlocal pos
        n = rd("<H")[0]
        s = data[pos : pos + n].decode("utf-8") if n else ""
        pos += n
        return s

    magic = data[pos : pos + 4]
    pos += 4
    assert magic == b"MAGT", f"Expected MAGT, got {magic}"
    ver, flags = rd("<HH")
    bounds_vals = rd("<10d")
    meta_strs = [rstr() for _ in range(4)]
    meta_floats = rd("<5f")
    dem_src = rstr()
    dem_cols, dem_rows, dem_res, dem_min, dem_max, dem_mean = rd("<IIffff")
    dem_vals = rd(f"<{dem_cols * dem_rows}f")

    road_count = rd("<I")[0]
    roads = []
    road_segs: List[Tuple[float, float, float, float, float]] = []
    for _ in range(road_count):
        wid = rd("<q")[0]
        rname, ref, htype, surface = rstr(), rstr(), rstr(), rstr()
        lanes, rflags, width_r, length_m, ncnt = rd("<BBffI")
        pts = [rd("<ff") for _ in range(ncnt)]
        roads.append((wid, rname, ref, htype, surface, lanes, rflags, width_r, length_m, pts))
        hw = max(width_r * 0.5, 2.0)
        for k in range(len(pts) - 1):
            road_segs.append((pts[k][0], pts[k][1], pts[k + 1][0], pts[k + 1][1], hw))

    bldg_count = rd("<I")[0]
    bldgs_raw = []
    for _ in range(bldg_count):
        bid = rd("<q")[0]
        bname, btype, amenity = rstr(), rstr(), rstr()
        levels, height_m, elev_m, area_m2, perim_m, cx, cy, pcnt = rd("<BffffffI")
        pts = [rd("<ff") for _ in range(pcnt)]
        bldgs_raw.append((bid, bname, btype, amenity, levels, height_m, elev_m, area_m2, perim_m, cx, cy, pts, ver))
        if ver >= 2:
            # Skip existing v2 tail when re-running on already upgraded v2 pack
            rd("<8B3fq")
            rstr()
            _fh, fcnt = rd("<fH")
            pos += fcnt * 8 + 16

    water_count = rd("<I")[0]
    waters = []
    for _ in range(water_count):
        wid = rd("<q")[0]
        wname, wtype = rstr(), rstr()
        mean_e, pcnt = rd("<fI")
        pts = [rd("<ff") for _ in range(pcnt)]
        waters.append((wid, wname, wtype, mean_e, pts))

    poi_count = rd("<I")[0]
    pois = []
    for _ in range(poi_count):
        nid = rd("<q")[0]
        pname, cat = rstr(), rstr()
        xm, ym, elev = rd("<fff")
        pois.append((nid, pname, cat, xm, ym, elev))

    road_index = RoadSpatialIndex(road_segs)
    comp_items = [(b[0], b[1], b[2], b[3], b[7], b[9], b[10], b[11]) for b in bldgs_raw]
    comp_set, comp_grid = build_compound_spatial_index(comp_items)
    comp_idx_set = set(comp_set)

    NON_FENCED_SET = {
        "parking", "kiosk", "commercial_kiosk", "petrol_canopy", "market_stall",
        "shop_row", "retail", "ruins", "quarry", "bridge", "weighbridge_kiosk",
    }

    # Pass 1: Shape, named-compound containment, semantics, and road-proximity eligibility
    pass1 = []
    all_pts = []
    all_areas = []
    all_cents = []
    eligible_cluster_mask = []
    for idx, (bid, bname, btype, amenity, levels, height_m, elev_m, area_m2, perim_m, cx, cy, pts, _) in enumerate(bldgs_raw):
        rng = random.Random((bid * 6364136223846793005 + 1442695040888963407) & 0xFFFFFFFFFFFFFFFF)
        shape_id = classify_shape(pts, area_m2, perim_m, btype, bname)
        p_id, p_name, p_dom, p_area = find_enclosing_compound(idx, cx, cy, area_m2, comp_items, comp_grid)
        d_edge, rdist_ctr, *_ = road_index.poly_edge_to_road_distances(pts)
        new_name, new_type, new_levels, new_h, floor_h, roof_id, roof_pitch, roof_mat, wall_id, win_id, wwr = (
            synthesize_building_semantics_and_envelope(
                bid, bname, btype, amenity, levels, height_m, area_m2, shape_id,
                p_id, p_name, p_dom, rdist_ctr, rng
            )
        )
        all_pts.append(pts)
        all_areas.append(area_m2)
        all_cents.append((cx, cy))
        is_named_comp = idx in comp_idx_set
        elig = (
            p_id == 0
            and not is_named_comp
            and new_type.lower() not in NON_FENCED_SET
            and 24.0 <= area_m2 <= 15_000.0
            and d_edge >= 6.2
        )
        eligible_cluster_mask.append(elig)
        pass1.append([
            bid, new_name, new_type, amenity, new_levels, new_h, elev_m, area_m2, perim_m, cx, cy, pts,
            shape_id, roof_id, roof_mat, wall_id, win_id, floor_h, roof_pitch, wwr,
            p_id, p_name, is_named_comp, rng
        ])

    # Pass 2: Group buildings <= 2.2m apart into shared homestead compounds (1 shared fence + gate)
    bldg_index = BuildingSpatialIndex(all_pts, all_areas)
    clusters = cluster_close_buildings(
        all_pts, all_areas, all_cents, eligible_cluster_mask, bldg_index, road_index
    )
    cluster_of_rep: Dict[int, List[int]] = {}
    for _root, members in clusters.items():
        if len(members) > 1:
            rep = max(members, key=lambda i: (1 if "house" in pass1[i][2] or "villa" in pass1[i][2] else 0, all_areas[i]))
            cluster_of_rep[rep] = members
            shared_name = f"Shared Compound #{abs(pass1[rep][0]) % 10000}"
            for m_i in members:
                if m_i != rep:
                    pass1[m_i][20] = pass1[rep][0]
                    pass1[m_i][21] = shared_name

    # Pass 3: Generate natural compound fences + road-facing gates
    fence_results = [None] * len(pass1)
    for idx, rec in enumerate(pass1):
        (
            bid, new_name, new_type, amenity, new_levels, new_h, elev_m, area_m2, perim_m, cx, cy, pts,
            shape_id, roof_id, roof_mat, wall_id, win_id, floor_h, roof_pitch, wwr,
            p_id, p_name, is_named_comp, rng
        ) = rec
        if idx in cluster_of_rep:
            members = cluster_of_rep[idx]
            member_polys = [all_pts[m] for m in members]
            member_set = set(members)
            tot_area = sum(all_areas[m] for m in members)
            mcx = sum(all_cents[m][0] for m in members) / len(members)
            mcy = sum(all_cents[m][1] for m in members) / len(members)
            res = build_natural_compound_fence_and_gate(
                member_polys, member_set, (mcx, mcy), tot_area, new_type,
                False, 0, road_index, bldg_index, rng
            )
            if not res[0]:
                for m_i in members:
                    if m_i != idx:
                        pass1[m_i][20] = 0
                        pass1[m_i][21] = ""
        else:
            res = build_natural_compound_fence_and_gate(
                [pts], {idx}, (cx, cy), area_m2, new_type,
                is_named_comp, p_id, road_index, bldg_index, rng
            )
        fence_results[idx] = res

    enriched = []
    stats = {"fenced": 0, "gated": 0, "compound_children": 0, "shared_clusters": len(cluster_of_rep), "shapes": {}, "types": {}}
    for idx, rec in enumerate(pass1):
        (
            bid, new_name, new_type, amenity, new_levels, new_h, elev_m, area_m2, perim_m, cx, cy, pts,
            shape_id, roof_id, roof_mat, wall_id, win_id, floor_h, roof_pitch, wwr,
            p_id, p_name, is_named_comp, rng
        ) = rec
        has_fence, fence_id, fence_h, fence_poly, has_gate, gate_id, gate_w, gate_ang, gate_pt = fence_results[idx]
        if p_id != 0:
            stats["compound_children"] += 1
        if has_fence:
            assert has_gate and gate_pt is not None
            assert road_index.fence_clears_all_roads(fence_poly)
            stats["fenced"] += 1
            stats["gated"] += 1
        stats["shapes"][SHAPE_TYPES[shape_id]] = stats["shapes"].get(SHAPE_TYPES[shape_id], 0) + 1
        stats["types"][new_type] = stats["types"].get(new_type, 0) + 1
        enriched.append((
            bid, new_name, new_type, amenity, new_levels, new_h, elev_m, area_m2, perim_m, cx, cy, pts,
            shape_id, roof_id, roof_mat, wall_id, win_id,
            (1 if has_fence else 0) | (2 if has_gate else 0),
            fence_id, gate_id, floor_h, roof_pitch, wwr,
            p_id, p_name, fence_h, fence_poly, gate_w, gate_ang, gate_pt
        ))

    def pack_str(buf: bytearray, s: str) -> None:
        raw = s.encode("utf-8")[:65535]
        buf.extend(struct.pack("<H", len(raw)))
        buf.extend(raw)

    out = bytearray()
    out.extend(b"MAGT")
    out.extend(struct.pack("<HH", 2, flags))
    out.extend(struct.pack("<10d", *bounds_vals))
    for s in meta_strs:
        pack_str(out, s)
    out.extend(struct.pack("<5f", *meta_floats))
    pack_str(out, dem_src)
    out.extend(struct.pack("<IIffff", dem_cols, dem_rows, dem_res, dem_min, dem_max, dem_mean))
    out.extend(struct.pack(f"<{len(dem_vals)}f", *dem_vals))

    out.extend(struct.pack("<I", len(roads)))
    for wid, rname, ref, htype, surface, lanes, rflags, width_r, length_m, pts in roads:
        out.extend(struct.pack("<q", wid))
        pack_str(out, rname); pack_str(out, ref); pack_str(out, htype); pack_str(out, surface)
        out.extend(struct.pack("<BBffI", lanes, rflags, width_r, length_m, len(pts)))
        for px, py in pts:
            out.extend(struct.pack("<ff", px, py))

    out.extend(struct.pack("<I", len(enriched)))
    for item in enriched:
        (
            bid, bname, btype, amenity, levels, h_m, elev_m, area_m2, perim_m, cx, cy, pts,
            shape_id, roof_id, roof_mat, wall_id, win_id, pflags, fence_id, gate_id,
            floor_h, roof_pitch, wwr, p_id, p_name, fence_h, fence_poly, gate_w, gate_ang, gate_pt
        ) = item
        out.extend(struct.pack("<q", bid))
        pack_str(out, bname); pack_str(out, btype); pack_str(out, amenity)
        out.extend(struct.pack("<BffffffI", min(255, levels), h_m, elev_m, area_m2, perim_m, cx, cy, len(pts)))
        for px, py in pts:
            out.extend(struct.pack("<ff", px, py))
        # v2 tail
        out.extend(struct.pack("<8B3fq", shape_id, roof_id, roof_mat, wall_id, win_id, pflags, fence_id, gate_id, floor_h, roof_pitch, wwr, p_id))
        pack_str(out, p_name)
        out.extend(struct.pack("<fH", fence_h, len(fence_poly)))
        for fx, fy in fence_poly:
            out.extend(struct.pack("<ff", fx, fy))
        gx, gy = gate_pt if gate_pt is not None else (0.0, 0.0)
        out.extend(struct.pack("<ffff", gate_w, gate_ang, gx, gy))

    out.extend(struct.pack("<I", len(waters)))
    for wid, wname, wtype, mean_e, pts in waters:
        out.extend(struct.pack("<q", wid))
        pack_str(out, wname); pack_str(out, wtype)
        out.extend(struct.pack("<fI", mean_e, len(pts)))
        for px, py in pts:
            out.extend(struct.pack("<ff", px, py))

    out.extend(struct.pack("<I", len(pois)))
    for nid, pname, cat, xm, ym, elev in pois:
        out.extend(struct.pack("<q", nid))
        pack_str(out, pname); pack_str(out, cat)
        out.extend(struct.pack("<fff", xm, ym, elev))

    with open(path, "wb") as f:
        f.write(out)

    print(
        f"[MAGT v2] Upgraded {path}: {len(out):,} bytes, buildings={len(enriched)}, "
        f"compound_children={stats['compound_children']}, fenced+gated={stats['fenced']}, "
        f"shapes={stats['shapes']}"
    )


# ── Upgrade C58B -> v2 ───────────────────────────────────────────────────────────

def upgrade_c58_to_v2(path: str) -> None:
    data = open(path, "rb").read()
    pos = 0

    def rd(fmt: str):
        nonlocal pos
        sz = struct.calcsize(fmt)
        res = struct.unpack_from(fmt, data, pos)
        pos += sz
        return res

    def rstr() -> str:
        nonlocal pos
        n = rd("<H")[0]
        s = data[pos : pos + n].decode("utf-8") if n else ""
        pos += n
        return s

    magic = data[pos : pos + 4]
    pos += 4
    assert magic == b"C58B", f"Expected C58B, got {magic}"
    ver, is_live_cl = rd("<HH")
    ref_e, ref_n = rd("<dd")
    meta_strs = [rstr() for _ in range(4)]
    meta_floats = rd("<5f")
    sec_n = rd("<B")[0]
    secs = rd(f"<{sec_n}B")
    cl_n = rd("<I")[0]
    centerline = [rd("<dd") for _ in range(cl_n)]
    dem_src = rstr()
    u_cols, v_rows, dem_res, dem_min, dem_max, dem_mean = rd("<IIffff")
    dem_vals = rd(f"<{u_cols * v_rows}f")

    def rpt4():
        return rd("<ffff")  # dE, dN, ch, off

    r_cnt = rd("<I")[0]
    roads = []
    road_segs: List[Tuple[float, float, float, float, float]] = []
    for _ in range(r_cnt):
        wid = rd("<q")[0]
        rname, ref, htype, surface = rstr(), rstr(), rstr(), rstr()
        lanes, rflags, sec_i, width_r, length_m = rd("<BBBff")
        mid = rpt4()
        nc = rd("<H")[0]
        pts = [rpt4() for _ in range(nc)]
        roads.append((wid, rname, ref, htype, surface, lanes, rflags, sec_i, width_r, length_m, mid, pts))
        hw = max(width_r * 0.5, 2.0)
        for k in range(len(pts) - 1):
            road_segs.append((pts[k][0], pts[k][1], pts[k + 1][0], pts[k + 1][1], hw))

    # Build fast centerline projection table from ribbon for C58Pt (chainage, lateral_offset) of new fence/gate points
    ribbon_segs = []
    if centerline:
        pass

    h_cnt = rd("<I")[0]
    houses_raw = []
    for _ in range(h_cnt):
        hid = rd("<q")[0]
        hname, btype, amenity = rstr(), rstr(), rstr()
        levels, sec_i, height_m, elev_m, area_m2, perim_m, lat_off = rd("<BBfffff")
        cent = rpt4()
        pc = rd("<H")[0]
        pts = [rpt4() for _ in range(pc)]
        houses_raw.append((hid, hname, btype, amenity, levels, sec_i, height_m, elev_m, area_m2, perim_m, lat_off, cent, pts))
        if ver >= 2:
            rd("<8B3fq")
            rstr()
            _fh, fcnt = rd("<fH")
            pos += fcnt * 16 + 24

    w_cnt = rd("<I")[0]
    waters = []
    for _ in range(w_cnt):
        wid = rd("<q")[0]
        wname, wtype = rstr(), rstr()
        mean_e, pc = rd("<fH")
        pts = [rpt4() for _ in range(pc)]
        waters.append((wid, wname, wtype, mean_e, pts))

    p_cnt = rd("<I")[0]
    pois = []
    for _ in range(p_cnt):
        nid = rd("<q")[0]
        pname, cat = rstr(), rstr()
        sec_i, elev = rd("<Bf")
        pt = rpt4()
        pois.append((nid, pname, cat, sec_i, elev, pt))

    print(f"[C58B] Indexing {len(road_segs)} road segments and {len(houses_raw)} houses...", flush=True)
    road_index = RoadSpatialIndex(road_segs)
    comp_items = [
        (h[0], h[1], h[2], h[3], h[8], h[11][0], h[11][1], [(p[0], p[1]) for p in h[12]])
        for h in houses_raw
    ]
    comp_set, comp_grid = build_compound_spatial_index(comp_items)
    comp_idx_set = set(comp_set)
    print(f"[C58B] Identified {len(comp_set)} compounds for inner-structure detection.", flush=True)

    NON_FENCED_SET = {
        "parking", "kiosk", "commercial_kiosk", "petrol_canopy", "market_stall",
        "shop_row", "retail", "ruins", "quarry", "bridge", "weighbridge_kiosk",
    }

    # Pass 1: Shape, named-compound containment, semantics, and road-proximity eligibility
    pass1 = []
    all_pts = []
    all_areas = []
    all_cents = []
    eligible_cluster_mask = []
    for idx, (hid, hname, btype, amenity, levels, sec_i, height_m, elev_m, area_m2, perim_m, lat_off, cent, pts) in enumerate(houses_raw):
        pts_xy = [(p[0], p[1]) for p in pts]
        rng = random.Random((hid * 6364136223846793005 + 1442695040888963407) & 0xFFFFFFFFFFFFFFFF)
        shape_id = classify_shape(pts_xy, area_m2, perim_m, btype, hname)
        p_id, p_name, p_dom, p_area = find_enclosing_compound(idx, cent[0], cent[1], area_m2, comp_items, comp_grid)
        d_edge, rdist_ctr, *_ = road_index.poly_edge_to_road_distances(pts_xy)
        new_name, new_type, new_levels, new_h, floor_h, roof_id, roof_pitch, roof_mat, wall_id, win_id, wwr = (
            synthesize_building_semantics_and_envelope(
                hid, hname, btype, amenity, levels, height_m, area_m2, shape_id,
                p_id, p_name, p_dom, rdist_ctr, rng
            )
        )
        all_pts.append(pts_xy)
        all_areas.append(area_m2)
        all_cents.append((cent[0], cent[1]))
        is_named_comp = idx in comp_idx_set
        elig = (
            p_id == 0
            and not is_named_comp
            and new_type.lower() not in NON_FENCED_SET
            and 24.0 <= area_m2 <= 15_000.0
            and d_edge >= 6.2
        )
        eligible_cluster_mask.append(elig)
        pass1.append([
            hid, new_name, new_type, amenity, new_levels, sec_i, new_h, elev_m, area_m2, perim_m, lat_off, cent, pts, pts_xy,
            shape_id, roof_id, roof_mat, wall_id, win_id, floor_h, roof_pitch, wwr,
            p_id, p_name, is_named_comp, rng
        ])

    # Pass 2: Group buildings <= 2.2m apart into shared homestead compounds (1 shared fence + gate)
    print("[C58B] Clustering <=2.2m buildings into shared compounds...", flush=True)
    bldg_index = BuildingSpatialIndex(all_pts, all_areas)
    clusters = cluster_close_buildings(
        all_pts, all_areas, all_cents, eligible_cluster_mask, bldg_index, road_index
    )
    cluster_of_rep: Dict[int, List[int]] = {}
    for _root, members in clusters.items():
        if len(members) > 1:
            rep = max(members, key=lambda i: (1 if "house" in pass1[i][2] or "villa" in pass1[i][2] else 0, all_areas[i]))
            cluster_of_rep[rep] = members
            shared_name = f"Shared Compound #{abs(pass1[rep][0]) % 10000}"
            for m_i in members:
                if m_i != rep:
                    pass1[m_i][22] = pass1[rep][0]
                    pass1[m_i][23] = shared_name
    print(f"[C58B] Formed {len(cluster_of_rep)} multi-building shared homestead compounds.", flush=True)

    # Pass 3: Generate natural compound fences + road-facing gates
    fence_results = [None] * len(pass1)
    for idx, rec in enumerate(pass1):
        (
            hid, new_name, new_type, amenity, new_levels, sec_i, new_h, elev_m, area_m2, perim_m, lat_off, cent, pts, pts_xy,
            shape_id, roof_id, roof_mat, wall_id, win_id, floor_h, roof_pitch, wwr,
            p_id, p_name, is_named_comp, rng
        ) = rec
        if idx in cluster_of_rep:
            members = cluster_of_rep[idx]
            member_polys = [all_pts[m] for m in members]
            member_set = set(members)
            tot_area = sum(all_areas[m] for m in members)
            mcx = sum(all_cents[m][0] for m in members) / len(members)
            mcy = sum(all_cents[m][1] for m in members) / len(members)
            res = build_natural_compound_fence_and_gate(
                member_polys, member_set, (mcx, mcy), tot_area, new_type,
                False, 0, road_index, bldg_index, rng
            )
            if not res[0]:
                for m_i in members:
                    if m_i != idx:
                        pass1[m_i][22] = 0
                        pass1[m_i][23] = ""
        else:
            res = build_natural_compound_fence_and_gate(
                [pts_xy], {idx}, (cent[0], cent[1]), area_m2, new_type,
                is_named_comp, p_id, road_index, bldg_index, rng
            )
        fence_results[idx] = res

    enriched = []
    stats = {"fenced": 0, "gated": 0, "compound_children": 0, "shapes": {}}
    for idx, rec in enumerate(pass1):
        (
            hid, new_name, new_type, amenity, new_levels, sec_i, new_h, elev_m, area_m2, perim_m, lat_off, cent, pts, pts_xy,
            shape_id, roof_id, roof_mat, wall_id, win_id, floor_h, roof_pitch, wwr,
            p_id, p_name, is_named_comp, rng
        ) = rec
        has_fence, fence_id, fence_h, fence_poly, has_gate, gate_id, gate_w, gate_ang, gate_pt = fence_results[idx]
        if p_id != 0:
            stats["compound_children"] += 1
        if has_fence:
            assert has_gate and gate_pt is not None
            assert road_index.fence_clears_all_roads(fence_poly)
            stats["fenced"] += 1
            stats["gated"] += 1
        st = SHAPE_TYPES[shape_id]
        stats["shapes"][st] = stats["shapes"].get(st, 0) + 1
        enriched.append((
            hid, new_name, new_type, amenity, new_levels, sec_i, new_h, elev_m, area_m2, perim_m, lat_off, cent, pts,
            shape_id, roof_id, roof_mat, wall_id, win_id,
            (1 if has_fence else 0) | (2 if has_gate else 0),
            fence_id, gate_id, floor_h, roof_pitch, wwr,
            p_id, p_name, fence_h, fence_poly, gate_w, gate_ang, gate_pt
        ))

    def pack_str(buf: bytearray, s: str) -> None:
        raw = s.encode("utf-8")[:65535]
        buf.extend(struct.pack("<H", len(raw)))
        buf.extend(raw)

    out = bytearray()
    out.extend(b"C58B")
    out.extend(struct.pack("<HH", 2, is_live_cl))
    out.extend(struct.pack("<dd", ref_e, ref_n))
    for s in meta_strs:
        pack_str(out, s)
    out.extend(struct.pack("<5f", *meta_floats))
    out.extend(struct.pack("<B", sec_n))
    out.extend(struct.pack(f"<{sec_n}B", *secs))
    out.extend(struct.pack("<I", len(centerline)))
    for lat, lon in centerline:
        out.extend(struct.pack("<dd", lat, lon))
    pack_str(out, dem_src)
    out.extend(struct.pack("<IIffff", u_cols, v_rows, dem_res, dem_min, dem_max, dem_mean))
    out.extend(struct.pack(f"<{len(dem_vals)}f", *dem_vals))

    out.extend(struct.pack("<I", len(roads)))
    for wid, rname, ref, htype, surface, lanes, rflags, sec_i, width_r, length_m, mid, pts in roads:
        out.extend(struct.pack("<q", wid))
        pack_str(out, rname); pack_str(out, ref); pack_str(out, htype); pack_str(out, surface)
        out.extend(struct.pack("<BBBff", lanes, rflags, sec_i, width_r, length_m))
        out.extend(struct.pack("<4f", *mid))
        out.extend(struct.pack("<H", len(pts)))
        for pt in pts:
            out.extend(struct.pack("<4f", *pt))

    out.extend(struct.pack("<I", len(enriched)))
    for item in enriched:
        (
            hid, hname, btype, amenity, levels, sec_i, h_m, elev_m, area_m2, perim_m, lat_off, cent, pts,
            shape_id, roof_id, roof_mat, wall_id, win_id, pflags, fence_id, gate_id,
            floor_h, roof_pitch, wwr, p_id, p_name, fence_h, fence_poly, gate_w, gate_ang, gate_pt
        ) = item
        out.extend(struct.pack("<q", hid))
        pack_str(out, hname); pack_str(out, btype); pack_str(out, amenity)
        out.extend(struct.pack("<BBfffff", min(255, levels), sec_i, h_m, elev_m, area_m2, perim_m, lat_off))
        out.extend(struct.pack("<4f", *cent))
        out.extend(struct.pack("<H", len(pts)))
        for pt in pts:
            out.extend(struct.pack("<4f", *pt))
        # v2 tail: preserve local chainage/offset approx relative to centroid for fence/gate C58Pts
        out.extend(struct.pack("<8B3fq", shape_id, roof_id, roof_mat, wall_id, win_id, pflags, fence_id, gate_id, floor_h, roof_pitch, wwr, p_id))
        pack_str(out, p_name)
        out.extend(struct.pack("<fH", fence_h, len(fence_poly)))
        c_de, c_dn, c_ch, c_off = cent
        for fx, fy in fence_poly:
            out.extend(struct.pack("<4f", fx, fy, c_ch, c_off + (fx - c_de) * 0.5))
        gx, gy = gate_pt if gate_pt is not None else (0.0, 0.0)
        g_off = (c_off + (gx - c_de) * 0.5) if gate_pt is not None else 0.0
        g_ch = c_ch if gate_pt is not None else 0.0
        out.extend(struct.pack("<6f", gate_w, gate_ang, gx, gy, g_ch, g_off))

    out.extend(struct.pack("<I", len(waters)))
    for wid, wname, wtype, mean_e, pts in waters:
        out.extend(struct.pack("<q", wid))
        pack_str(out, wname); pack_str(out, wtype)
        out.extend(struct.pack("<fH", mean_e, len(pts)))
        for pt in pts:
            out.extend(struct.pack("<4f", *pt))

    out.extend(struct.pack("<I", len(pois)))
    for nid, pname, cat, sec_i, elev, pt in pois:
        out.extend(struct.pack("<q", nid))
        pack_str(out, pname); pack_str(out, cat)
        out.extend(struct.pack("<Bf4f", sec_i, elev, *pt))

    with open(path, "wb") as f:
        f.write(out)

    print(
        f"[C58B v2] Upgraded {path}: {len(out):,} bytes, houses={len(enriched)}, "
        f"compound_children={stats['compound_children']}, fenced+gated={stats['fenced']}, "
        f"shapes={stats['shapes']}"
    )


if __name__ == "__main__":
    if os.path.exists(MAGADI_BIN_PATH):
        upgrade_magt_to_v2(MAGADI_BIN_PATH)
    if os.path.exists(C58_BIN_PATH):
        upgrade_c58_to_v2(C58_BIN_PATH)
    print("v2 GIS Binary Pack enrichment complete.")
