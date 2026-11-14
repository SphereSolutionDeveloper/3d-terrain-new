#!/usr/bin/env python3
"""
build_1m_segment_grid.py
Generates the 1-Meter Spatial Grid System per segment for Magadi Road (B19 / C58).
105,274 surveyed points at 1m resolution (0m to 105,274m) divided across 10 segments.
Outputs:
  - /terrain_grid/magadi_1m_grid_segments.bin (3.37 MB high-speed binary, 0 SQLite)
  - /terrain_grid/magadi_1m_grid_metadata.json (10 segment origins, 1m grid transforms, starter assets)
  - Copies to /app/src/main/assets/terrain_grid/
"""
import os, sys, csv, math, struct, json, shutil

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))
CSV_PATH = os.path.join(PROJECT_ROOT, "native-assets/src/terrain/game_terrain/kenya_magadi_50km/output/elevation_raw_1m.csv")
OUT_DIR = os.path.join(PROJECT_ROOT, "terrain_grid")
ANDROID_ASSETS_DIR = os.path.join(PROJECT_ROOT, "app/src/main/assets/terrain_grid")

os.makedirs(OUT_DIR, exist_ok=True)
os.makedirs(ANDROID_ASSETS_DIR, exist_ok=True)

SEGMENT_NAMES = [
    "Nairobi Galleria / Bomas to Ongata Rongai",
    "Ongata Rongai to Kiserian Town Junction",
    "Kiserian to Corner Baridi Escarpment Summit",
    "Corner Baridi Hairpins to Olepolos Ridge",
    "Olepolos Ridge to Kisamis Rift Basin",
    "Kisamis Basin to Tinga Maasai Center",
    "Tinga Center to Singiraine Plains",
    "Singiraine Plains to Olorgesailie Site",
    "Olorgesailie Basin to Il Kamukuru Crossing",
    "Kamukuru to Lake Magadi Soda Causeway"
]

def bearing_deg(lat1, lon1, lat2, lon2):
    dlon = math.radians(lon2 - lon1)
    y = math.sin(dlon) * math.cos(math.radians(lat2))
    x = math.cos(math.radians(lat1)) * math.sin(math.radians(lat2)) - \
        math.sin(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.cos(dlon)
    b = math.degrees(math.atan2(y, x))
    return (b + 360.0) % 360.0

def float_to_half(f):
    """Pack float to 16-bit half precision."""
    return struct.unpack('<H', struct.pack('<e', f))[0]

def main():
    print("[1/4] Reading 105,274 surveyed points at 1-meter resolution...")
    with open(CSV_PATH, "r") as f:
        reader = csv.DictReader(f)
        raw_pts = list(reader)

    total_pts = len(raw_pts)
    print(f"  Loaded {total_pts} points (0m to {float(raw_pts[-1]['distance_m']):.1f}m)")

    # 10 Segments partitioning
    segments = []
    seg_step = 10000.0

    print("[2/4] Partitioning 10 segments and calculating 1m local grid origins...")
    all_stations_data = []

    for seg_idx in range(10):
        start_d = seg_idx * seg_step
        end_d = (seg_idx + 1) * seg_step if seg_idx < 9 else float(raw_pts[-1]["distance_m"]) + 1.0
        
        seg_pts = [p for p in raw_pts if start_d <= float(p["distance_m"]) < end_d]
        if seg_idx == 9:
            # Include the very last point
            seg_pts = [p for p in raw_pts if start_d <= float(p["distance_m"])]

        p0 = seg_pts[0]
        origin_utm_x = float(p0["x_m"])
        origin_utm_y = float(p0["y_m"])
        origin_lon = float(p0["lon"])
        origin_lat = float(p0["lat"])
        origin_elev = float(p0["elevation_raw_m"])

        utm_xs = [float(p["x_m"]) for p in seg_pts]
        utm_ys = [float(p["y_m"]) for p in seg_pts]
        elevs = [float(p["elevation_raw_m"]) for p in seg_pts]

        min_utm_x = min(utm_xs) - 5000.0  # 10km corridor envelope (±5km)
        max_utm_x = max(utm_xs) + 5000.0
        min_utm_y = min(utm_ys) - 5000.0
        max_utm_y = max(utm_ys) + 5000.0

        grid_dim_x_m = int(math.ceil(max_utm_x - min_utm_x))
        grid_dim_y_m = int(math.ceil(max_utm_y - min_utm_y))

        seg_meta = {
            "segment_id": seg_idx + 1,
            "segment_code": f"MAGADI_SEG_{seg_idx+1:02d}",
            "name": SEGMENT_NAMES[seg_idx],
            "station_start_idx": len(all_stations_data),
            "station_count": len(seg_pts),
            "chainage_start_m": round(float(seg_pts[0]["distance_m"]), 1),
            "chainage_end_m": round(float(seg_pts[-1]["distance_m"]), 1),
            "length_m": round(float(seg_pts[-1]["distance_m"]) - float(seg_pts[0]["distance_m"]), 1),
            "origin": {
                "utm_x_m": round(origin_utm_x, 2),
                "utm_y_m": round(origin_utm_y, 2),
                "lon": round(origin_lon, 7),
                "lat": round(origin_lat, 7),
                "elev_m": round(origin_elev, 2)
            },
            "corridor_1m_grid": {
                "resolution_m": 1.0,
                "lateral_width_m": 10000.0,
                "lateral_half_width_m": 5000.0,
                "bbox_utm_m": {
                    "min_x": round(min_utm_x, 1),
                    "max_x": round(max_utm_x, 1),
                    "min_y": round(min_utm_y, 1),
                    "max_y": round(max_utm_y, 1)
                },
                "grid_cells_x": grid_dim_x_m,
                "grid_cells_y": grid_dim_y_m,
                "total_1m_cells": grid_dim_x_m * grid_dim_y_m
            },
            "elev_min_m": round(min(elevs), 2),
            "elev_max_m": round(max(elevs), 2)
        }
        segments.append(seg_meta)

        # Process individual 1m station records
        for i, p in enumerate(seg_pts):
            dist_m = float(p["distance_m"])
            utm_x = float(p["x_m"])
            utm_y = float(p["y_m"])
            lon = float(p["lon"])
            lat = float(p["lat"])
            road_elev = float(p["elevation_raw_m"])
            terrain_elev = road_elev  # COP30 survey anchor

            # Compute grade and bearing
            if i < len(seg_pts) - 1:
                p_next = seg_pts[i + 1]
                brg = bearing_deg(lat, lon, float(p_next["lat"]), float(p_next["lon"]))
                d_dist = max(0.5, float(p_next["distance_m"]) - dist_m)
                grade = ((float(p_next["elevation_raw_m"]) - road_elev) / d_dist) * 100.0
            elif i > 0:
                p_prev = seg_pts[i - 1]
                brg = bearing_deg(float(p_prev["lat"]), float(p_prev["lon"]), lat, lon)
                d_dist = max(0.5, dist_m - float(p_prev["distance_m"]))
                grade = ((road_elev - float(p_prev["elevation_raw_m"])) / d_dist) * 100.0
            else:
                brg = 0.0
                grade = 0.0

            # Special flags (bridges, junctions, steep grades)
            flags = 0
            if abs(grade) > 4.5: flags |= 1  # STEEP_GRADE
            if seg_idx == 0 and 6100 <= dist_m <= 6300: flags |= 2  # RONGAI_RIVER_BRIDGE
            if seg_idx == 2 and 29500 <= dist_m <= 29700: flags |= 4  # BARIDI_CREST_VIEWPOINT
            if seg_idx == 3 and 34000 <= dist_m <= 36000: flags |= 8  # HAIRPIN_CURVES
            if seg_idx == 7 and 79100 <= dist_m <= 79300: flags |= 16  # OLORGESAILIE_MUSEUM_JUNCTION
            if seg_idx == 9 and 105100 <= dist_m: flags |= 32  # MAGADI_CAUSEWAY

            all_stations_data.append({
                "dist_m": dist_m,
                "utm_x": utm_x,
                "utm_y": utm_y,
                "lon": lon,
                "lat": lat,
                "road_elev": road_elev,
                "terrain_elev": terrain_elev,
                "grade": grade,
                "bearing": brg,
                "flags": flags
            })

    print(f"  Processed {len(all_stations_data)} station points across 10 segments.")

    # Starter asset placements along the 1m grid
    sample_assets = [
        {"asset_id": "ASSET_001", "name": "Solar Street Light - Galleria Departure", "category": "LIGHTING", "segment_id": 1, "station_m": 150.0, "lateral_offset_m": 4.5, "height_m": 8.0, "notes": "Dual carriageway median lighting mast"},
        {"asset_id": "ASSET_002", "name": "Speed Limit 60 km/h Sign - Multimedia Univ", "category": "SIGNAGE", "segment_id": 1, "station_m": 2400.0, "lateral_offset_m": -5.0, "height_m": 2.5, "notes": "Reflective regulatory sign, left shoulder"},
        {"asset_id": "ASSET_003", "name": "Rongai River Reinforced Concrete Culvert", "category": "DRAINAGE", "segment_id": 1, "station_m": 6180.0, "lateral_offset_m": 0.0, "height_m": -2.8, "notes": "Twin cell box culvert 3.0m x 2.5m under road"},
        {"asset_id": "ASSET_004", "name": "Kiserian Town Pedestrian Crossing Gantry", "category": "STRUCTURE", "segment_id": 2, "station_m": 19820.0, "lateral_offset_m": 0.0, "height_m": 5.2, "notes": "Steel truss overhead pedestrian bridge"},
        {"asset_id": "ASSET_005", "name": "Corner Baridi Safari Rally Time Control Gate", "category": "RALLY_INFRA", "segment_id": 3, "station_m": 29600.0, "lateral_offset_m": 0.0, "height_m": 4.5, "notes": "FIA / KCB Safari Rally official timing gate"},
        {"asset_id": "ASSET_006", "name": "Armco W-Beam Steel Crash Barrier (200m)", "category": "SAFETY", "segment_id": 4, "station_m": 34500.0, "lateral_offset_m": 4.0, "height_m": 0.9, "notes": "Double corrugated steel barrier along fault cliff"},
        {"asset_id": "ASSET_007", "name": "Olepolos Ridge Solar Weather Telemetry Mast", "category": "TELEMETRY", "segment_id": 4, "station_m": 38200.0, "lateral_offset_m": -18.0, "height_m": 12.0, "notes": "Wind speed, ambient temp, solar irradiance sensor"},
        {"asset_id": "ASSET_008", "name": "Kisamis Rift Valley Cellular Relay Tower", "category": "COMMUNICATIONS", "segment_id": 5, "station_m": 45100.0, "lateral_offset_m": 35.0, "height_m": 36.0, "notes": "Lattice steel 4G/5G mast with solar battery compound"},
        {"asset_id": "ASSET_009", "name": "Tinga Pastoral Cattle Drift Cattlegrid", "category": "DRAINAGE", "segment_id": 6, "station_m": 54800.0, "lateral_offset_m": 0.0, "height_m": 0.0, "notes": "Cast iron heavy vehicle cattle grid over storm ditch"},
        {"asset_id": "ASSET_010", "name": "Olorgesailie Prehistoric Museum Entrance Pylon", "category": "STRUCTURE", "segment_id": 8, "station_m": 79250.0, "lateral_offset_m": 12.5, "height_m": 6.0, "notes": "National Museums of Kenya stone monumental archway"},
        {"asset_id": "ASSET_011", "name": "Kamukuru Railway Level Crossing Automated Boom", "category": "SAFETY", "segment_id": 9, "station_m": 88100.0, "lateral_offset_m": -4.2, "height_m": 2.2, "notes": "Magadi Soda freight rail level crossing automated gate"},
        {"asset_id": "ASSET_012", "name": "Lake Magadi Industrial Weighbridge Platform", "category": "STRUCTURE", "segment_id": 10, "station_m": 104800.0, "lateral_offset_m": 8.0, "height_m": 0.4, "notes": "80-ton multi-axle freight scale for trona soda ash trucks"}
    ]

    # Compute exact spatial locations for starter assets on the 1m grid
    for a in sample_assets:
        target_d = a["station_m"]
        closest = min(all_stations_data, key=lambda s: abs(s["dist_m"] - target_d))
        brg_rad = math.radians(closest["bearing"])
        # Normal vector pointing 90 deg right (offset > 0 = right, offset < 0 = left)
        # Tangent vector: (sin(brg), cos(brg)). Right normal: (cos(brg), -sin(brg))
        nx = math.cos(brg_rad)
        ny = -math.sin(brg_rad)
        off_m = a["lateral_offset_m"]
        asset_utm_x = closest["utm_x"] + off_m * nx
        asset_utm_y = closest["utm_y"] + off_m * ny

        # WGS84 approx offset
        asset_lon = closest["lon"] + (off_m * nx) / (111000.0 * math.cos(math.radians(closest["lat"])))
        asset_lat = closest["lat"] + (off_m * ny) / 111000.0

        seg_info = next(s for s in segments if s["segment_id"] == a["segment_id"])
        local_x_m = asset_utm_x - seg_info["origin"]["utm_x_m"]
        local_y_m = asset_utm_y - seg_info["origin"]["utm_y_m"]

        a["spatial_1m_grid"] = {
            "utm_x": round(asset_utm_x, 2),
            "utm_y": round(asset_utm_y, 2),
            "local_grid_x_m": round(local_x_m, 2),
            "local_grid_y_m": round(local_y_m, 2),
            "lon": round(asset_lon, 7),
            "lat": round(asset_lat, 7),
            "ground_elev_m": round(closest["terrain_elev"], 2),
            "road_elev_m": round(closest["road_elev"], 2),
            "vertical_clearance_m": round(a["height_m"], 2),
            "cell_col": int(math.floor(asset_utm_x - seg_info["corridor_1m_grid"]["bbox_utm_m"]["min_x"])),
            "cell_row": int(math.floor(asset_utm_y - seg_info["corridor_1m_grid"]["bbox_utm_m"]["min_y"]))
        }

    # [3/4] Export Binary: magadi_1m_grid_segments.bin
    print("[3/4] Exporting 1-Meter Grid Binary (magadi_1m_grid_segments.bin)...")
    bin_path = os.path.join(OUT_DIR, "magadi_1m_grid_segments.bin")
    
    with open(bin_path, "wb") as bf:
        # File Header (64 bytes)
        # Magic: 'GRID1M\x00\x01' (8 bytes)
        # uint32 version (1)
        # uint32 segment_count (10)
        # uint32 total_stations (105274)
        # float32 grid_resolution_m (1.0)
        # reserved 40 bytes
        bf.write(b"GRID1M\x00\x01")
        bf.write(struct.pack("<IIIf", 1, len(segments), len(all_stations_data), 1.0))
        bf.write(b"\x00" * 44)

        # Segment Index Table (10 segments * 128 bytes = 1280 bytes)
        for s in segments:
            name_bytes = s["name"].encode("utf-8")[:32].ljust(32, b"\x00")
            bf.write(struct.pack("<H", s["segment_id"]))
            bf.write(name_bytes)
            bbox = s["corridor_1m_grid"]["bbox_utm_m"]
            orig = s["origin"]
            bf.write(struct.pack(
                "<IIffddff ffff II",
                s["station_start_idx"],
                s["station_count"],
                s["chainage_start_m"],
                s["chainage_end_m"],
                orig["utm_x_m"],
                orig["utm_y_m"],
                float(orig["lon"]),
                float(orig["lat"]),
                bbox["min_x"],
                bbox["max_x"],
                bbox["min_y"],
                bbox["max_y"],
                s["corridor_1m_grid"]["grid_cells_x"],
                s["corridor_1m_grid"]["grid_cells_y"]
            ))
            # Pad to 128 bytes
            written = 2 + 32 + (4*2 + 4*2 + 8*2 + 4*2 + 4*4 + 4*2)  # 2 + 32 + (8+8+16+8+16+8) = 34 + 64 = 98
            bf.write(b"\x00" * (128 - written))

        # Station records: 32 bytes each
        # dist_m (f4), utm_x (f4), utm_y (f4), lon (f4), lat (f4), road_elev (f4), terrain_elev (f4), grade (h2), bearing (h2)
        for st in all_stations_data:
            grade_h = float_to_half(st["grade"])
            bearing_h = float_to_half(st["bearing"])
            bf.write(struct.pack(
                "<ffffff f H H",
                float(st["dist_m"]),
                float(st["utm_x"]),
                float(st["utm_y"]),
                float(st["lon"]),
                float(st["lat"]),
                float(st["road_elev"]),
                float(st["terrain_elev"]),
                grade_h,
                bearing_h
            ))

    bin_size_kb = os.path.getsize(bin_path) / 1024.0
    print(f"  Saved Binary: {bin_path} ({bin_size_kb:.1f} KB / {bin_size_kb/1024.0:.2f} MB)")
    shutil.copy2(bin_path, os.path.join(ANDROID_ASSETS_DIR, "magadi_1m_grid_segments.bin"))

    # [4/4] Export JSON Metadata: magadi_1m_grid_metadata.json
    print("[4/4] Exporting 1-Meter Grid Metadata & Asset Catalog JSON...")
    meta_path = os.path.join(OUT_DIR, "magadi_1m_grid_metadata.json")
    meta_data = {
        "title": "Kenya Magadi Road (B19/C58) 1-Meter Spatial Grid System",
        "format": "Direct Memory-Mapped Binary (ZERO DATABASE) + JSON Specification",
        "road_name": "Magadi Road (B19 / C58: Galleria - Kiserian - Magadi)",
        "total_distance_m": float(raw_pts[-1]["distance_m"]),
        "total_stations_1m": len(all_stations_data),
        "grid_resolution_m": 1.0,
        "lateral_corridor_width_m": 10000.0,
        "segment_count": len(segments),
        "binary_file": "magadi_1m_grid_segments.bin",
        "binary_size_bytes": os.path.getsize(bin_path),
        "segments": segments,
        "placed_assets_sample": sample_assets,
        "asset_categories": [
            {"code": "LIGHTING", "name": "Solar & Grid Lighting Masts", "default_offset_m": 4.5, "default_height_m": 8.0},
            {"code": "SAFETY", "name": "Armco Barriers & Impact Attenuators", "default_offset_m": 4.0, "default_height_m": 0.9},
            {"code": "SIGNAGE", "name": "Milestone & Directional Gantry Signs", "default_offset_m": -5.0, "default_height_m": 2.5},
            {"code": "DRAINAGE", "name": "Culverts, Sump Wells & Cattlegrids", "default_offset_m": 0.0, "default_height_m": -2.0},
            {"code": "RALLY_INFRA", "name": "Safari Rally Time Control & Spectator Chicanes", "default_offset_m": 0.0, "default_height_m": 4.5},
            {"code": "STRUCTURE", "name": "Overhead Bridges, Archways & Weighbridges", "default_offset_m": 0.0, "default_height_m": 5.5},
            {"code": "COMMUNICATIONS", "name": "Cellular Radio Masts & Towers", "default_offset_m": 35.0, "default_height_m": 36.0},
            {"code": "TELEMETRY", "name": "Weather Stations & Surface Road Sensors", "default_offset_m": -15.0, "default_height_m": 3.0}
        ]
    }

    with open(meta_path, "w") as f:
        json.dump(meta_data, f, indent=2)

    print(f"  Saved Metadata JSON: {meta_path} ({os.path.getsize(meta_path)/1024.0:.1f} KB)")
    shutil.copy2(meta_path, os.path.join(ANDROID_ASSETS_DIR, "magadi_1m_grid_metadata.json"))

    print("Done! 1m grid system per segment created successfully (ZERO SQLITE).")

if __name__ == "__main__":
    main()
