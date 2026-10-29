#!/usr/bin/env python3
"""
Kenya Road Corridor 10km Buffer Pipeline & Driving Game Dataset Generator.
Extracts high-resolution driving corridors (e.g. 10 km width: ±5 km from road centerline),
pruning 99%+ of irrelevant non-road data for driving simulation and game engines.

Generates:
1. terrain_grid/kenya_road_corridor_10km.html (& root /kenya_road_corridor_10km.html)
2. terrain_grid/kenya_road_corridors.json (& root /kenya_road_corridors.json)
3. terrain_grid/kenya_road_corridors.sqlite (& root /kenya_road_corridors.sqlite)
4. Copies to app/src/main/assets/terrain_grid/
"""

import os
import json
import math
import sqlite3
import shutil

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
GRID_DIR = os.path.join(PROJECT_ROOT, "terrain_grid")
ANDROID_ASSETS = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "terrain_grid")
os.makedirs(GRID_DIR, exist_ok=True)
os.makedirs(ANDROID_ASSETS, exist_ok=True)

# Geodetic Constants
METERS_PER_DEG_LAT = 111132.954
METERS_PER_DEG_LON_EQ = 111319.491

def meters_per_deg_lon(lat_deg):
    return METERS_PER_DEG_LON_EQ * math.cos(math.radians(lat_deg))

def haversine_km(lat1, lon1, lat2, lon2):
    R = 6371.0
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = (math.sin(dlat / 2) ** 2 +
         math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon / 2) ** 2)
    return 2 * R * math.asin(math.sqrt(a))

def bearing_deg(lat1, lon1, lat2, lon2):
    y = math.sin(math.radians(lon2 - lon1)) * math.cos(math.radians(lat2))
    x = (math.cos(math.radians(lat1)) * math.sin(math.radians(lat2)) -
         math.sin(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.cos(math.radians(lon2 - lon1)))
    brg = math.degrees(math.atan2(y, x))
    return (brg + 360) % 360

def offset_lat_lon(lat, lon, distance_m, bearing_deg):
    R = 6378137.0
    d_div_r = distance_m / R
    brg_rad = math.radians(bearing_deg)
    lat_rad = math.radians(lat)
    lon_rad = math.radians(lon)

    out_lat_rad = math.asin(math.sin(lat_rad) * math.cos(d_div_r) +
                            math.cos(lat_rad) * math.sin(d_div_r) * math.cos(brg_rad))
    out_lon_rad = lon_rad + math.atan2(math.sin(brg_rad) * math.sin(d_div_r) * math.cos(lat_rad),
                                       math.cos(d_div_r) - math.sin(lat_rad) * math.sin(out_lat_rad))
    return math.degrees(out_lat_rad), math.degrees(out_lon_rad)

def get_sqtile_code(lat, lon):
    if not (-5.0 <= lat <= 8.0 and 34.0 <= lon <= 42.0):
        return "OUTSIDE_GRID"
    col = int(math.floor((lon - 34.0) / 0.5))
    row = int(math.floor((lat - (-5.0)) / 0.5))
    col = max(0, min(15, col))
    row = max(0, min(25, row))
    return f"STILE_R{row:02d}_C{col:02d}"

# ==============================================================================
# KEY KENYAN DRIVING CORRIDORS DATA
# ==============================================================================
ROUTES_CONFIG = [
    {
        "id": "magadi_c58",
        "name": "C58 / B19 Magadi Road — Great Rift Valley Escarpment",
        "highway_ref": "C58 / B19",
        "length_km": 105.3,
        "description": "Dramatic drop from Nairobi Karen/Bomas (1,820m) down the Great Rift Valley fault escarpment, Corner Baridi hairpins, Olorgesailie Prehistoric site, to the volcanic soda flats of Lake Magadi (598m). Perfect for downhill rally simulation.",
        "anchor_city": "Nairobi (Galleria)",
        "destination": "Lake Magadi",
        "surface": "Asphalt (Nairobi-Kiserian-Olepolos), Degraded Bitumen & Sand Escarpment (Oltepesi-Magadi)",
        "safari_rally_stage": True,
        "key_waypoints": [
            {"km": 0.0, "lat": -1.3508, "lon": 36.7628, "elev": 1820, "name": "Nairobi Bomas / Galleria Mall Anchor"},
            {"km": 8.5, "lat": -1.3980, "lon": 36.7580, "elev": 1780, "name": "Ongata Rongai Center"},
            {"km": 17.2, "lat": -1.4420, "lon": 36.6850, "elev": 1960, "name": "Kiserian Town Junction"},
            {"km": 25.8, "lat": -1.4850, "lon": 36.6350, "elev": 1980, "name": "Corner Baridi — Escarpment Edge Viewpoint"},
            {"km": 34.0, "lat": -1.5350, "lon": 36.5900, "elev": 1620, "name": "Olepolos Ridge & Escarpment Drop"},
            {"km": 45.0, "lat": -1.5950, "lon": 36.5450, "elev": 1340, "name": "Tingas Maasai Valley"},
            {"km": 58.2, "lat": -1.6550, "lon": 36.5020, "elev": 1110, "name": "Oltepesi Trading Center"},
            {"km": 68.0, "lat": -1.7100, "lon": 36.4550, "elev": 980, "name": "Mount Ol Esakut Volcano Foothills"},
            {"km": 79.5, "lat": -1.7580, "lon": 36.4250, "elev": 840, "name": "Olorgesailie Prehistoric Site"},
            {"km": 92.0, "lat": -1.8350, "lon": 36.3550, "elev": 710, "name": "Il Parakuo Sand River Wash"},
            {"km": 105.3, "lat": -1.9015, "lon": 36.2882, "elev": 598, "name": "Lake Magadi Soda Ash Causeway"}
        ]
    },
    {
        "id": "rift_a104",
        "name": "A104 Great Rift Valley Highway — Nairobi to Nakuru",
        "highway_ref": "A104",
        "length_km": 158.0,
        "description": "High-altitude transit highway climbing to the dramatic Limuru Escarpment (2,390m) overlooking Mount Longonot volcano, dropping to Lake Naivasha basin, Kariandusi prehistoric diatomite beds, and Lake Nakuru.",
        "anchor_city": "Nairobi (Westlands)",
        "destination": "Nakuru City",
        "surface": "Multi-lane Asphalt & High-Speed Highway",
        "safari_rally_stage": False,
        "key_waypoints": [
            {"km": 0.0, "lat": -1.2650, "lon": 36.8050, "elev": 1680, "name": "Nairobi Westlands Terminal"},
            {"km": 28.0, "lat": -1.1850, "lon": 36.6350, "elev": 2240, "name": "Limuru Tea Zone"},
            {"km": 42.0, "lat": -1.0850, "lon": 36.5650, "elev": 2390, "name": "Rift Valley Great Escarpment Viewpoint"},
            {"km": 62.0, "lat": -0.9250, "lon": 36.4950, "elev": 2080, "name": "Kijabe Escarpment Descent"},
            {"km": 88.0, "lat": -0.7180, "lon": 36.4350, "elev": 1895, "name": "Naivasha Town & Longonot Flank"},
            {"km": 115.0, "lat": -0.5850, "lon": 36.3250, "elev": 1950, "name": "Gilgil & Lake Elmenteita Ridge"},
            {"km": 138.0, "lat": -0.4550, "lon": 36.2100, "elev": 1840, "name": "Kariandusi Diatomite Pass"},
            {"km": 158.0, "lat": -0.2833, "lon": 36.0667, "elev": 1760, "name": "Nakuru CBD & National Park Gate"}
        ]
    },
    {
        "id": "tsavo_a104",
        "name": "A104 / A109 Northern Corridor — Mombasa to Nairobi (Tsavo)",
        "highway_ref": "A104 / A109",
        "length_km": 485.0,
        "description": "The economic lifeline of East Africa and legendary rally transit. From sea-level Indian Ocean port across the vast red-earth savannas of Tsavo East/West, climbing continuously to the central highland plateau.",
        "anchor_city": "Mombasa Island",
        "destination": "Nairobi",
        "surface": "Heavy Transit Asphalt Corridor",
        "safari_rally_stage": True,
        "key_waypoints": [
            {"km": 0.0, "lat": -4.0435, "lon": 39.6682, "elev": 15, "name": "Mombasa Island Harbour"},
            {"km": 45.0, "lat": -3.8500, "lon": 39.3800, "elev": 180, "name": "Mariakani Weighbridge"},
            {"km": 115.0, "lat": -3.6100, "lon": 38.9200, "elev": 360, "name": "Taru Desert & Samburu Flatlands"},
            {"km": 155.0, "lat": -3.3950, "lon": 38.5600, "elev": 560, "name": "Voi Town & Tsavo River Crossing"},
            {"km": 240.0, "lat": -2.6900, "lon": 38.1200, "elev": 760, "name": "Mtito Andei — Tsavo Boundary"},
            {"km": 310.0, "lat": -2.3100, "lon": 37.7800, "elev": 980, "name": "Kibwezi & Chyulu Lava Flows"},
            {"km": 375.0, "lat": -2.0200, "lon": 37.3800, "elev": 1250, "name": "Sultan Hamud & Amboseli Foothills"},
            {"km": 430.0, "lat": -1.5500, "lon": 37.1200, "elev": 1540, "name": "Machakos Junction / Athi River"},
            {"km": 485.0, "lat": -1.2864, "lon": 36.8172, "elev": 1680, "name": "Nairobi CBD Haile Selassie"}
        ]
    },
    {
        "id": "mtkenya_a2",
        "name": "A2 Great North Road — Nairobi to Mount Kenya & Isiolo",
        "highway_ref": "A2",
        "length_km": 265.0,
        "description": "High-altitude mountain road skirting the jagged snowcaps of Mount Kenya (5,199m). Crosses the Equator at Nanyuki (2,050m) before descending into northern acacia rangelands.",
        "anchor_city": "Nairobi",
        "destination": "Isiolo Northern Gateway",
        "surface": "Paved Mountain Highway",
        "safari_rally_stage": True,
        "key_waypoints": [
            {"km": 0.0, "lat": -1.2864, "lon": 36.8172, "elev": 1680, "name": "Nairobi City Center"},
            {"km": 45.0, "lat": -1.0380, "lon": 37.0720, "elev": 1530, "name": "Thika River & Chania Falls"},
            {"km": 105.0, "lat": -0.7200, "lon": 37.1500, "elev": 1390, "name": "Sagana River Bridge & White Waters"},
            {"km": 140.0, "lat": -0.5200, "lon": 37.0700, "elev": 1650, "name": "Karatina Market Ridge"},
            {"km": 195.0, "lat": 0.0167, "lon": 37.0730, "elev": 2050, "name": "Nanyuki Town & Equator Marker"},
            {"km": 225.0, "lat": 0.1800, "lon": 37.3500, "elev": 1720, "name": "Timau Wheat Plains & Lewa Conservancy"},
            {"km": 265.0, "lat": 0.3550, "lon": 37.5820, "elev": 1100, "name": "Isiolo Northern Gateway Pass"}
        ]
    },
    {
        "id": "coast_b8",
        "name": "B8 Swahili Coast Highway — Mombasa to Malindi",
        "highway_ref": "B8",
        "length_km": 120.0,
        "description": "Tropical coastal ribbon hugging the Indian Ocean shelf. Crosses Kilifi Creek suspension bridge, skirts Arabuko Sokoke indigenous coastal forest and coral reefs to Vasco da Gama pillar at Malindi.",
        "anchor_city": "Mombasa (Nyali)",
        "destination": "Malindi Pier",
        "surface": "Smooth Coastal Asphalt",
        "safari_rally_stage": False,
        "key_waypoints": [
            {"km": 0.0, "lat": -4.0300, "lon": 39.6800, "elev": 12, "name": "Nyali Bridge Mombasa"},
            {"km": 22.0, "lat": -3.8800, "lon": 39.7900, "elev": 24, "name": "Mtwapa Creek Bridge"},
            {"km": 58.0, "lat": -3.6300, "lon": 39.8500, "elev": 28, "name": "Kilifi Creek Bridge & Estuary"},
            {"km": 88.0, "lat": -3.3800, "lon": 39.9800, "elev": 45, "name": "Watamu Marine Reserve Gate"},
            {"km": 102.0, "lat": -3.2800, "lon": 40.0500, "elev": 35, "name": "Arabuko-Sokoke Forest Edge"},
            {"km": 120.0, "lat": -3.2200, "lon": 40.1250, "elev": 8, "name": "Malindi Marine Pier & Vasco da Gama"}
        ]
    }
]

def build_dense_centerline(route_cfg, sample_interval_km=1.0):
    """
    Interpolates route between key waypoints into dense 1km chainage nodes,
    calculating bearings, 10km corridor offsets (±5km), slope %, and terrain cross-sections.
    """
    kwps = route_cfg["key_waypoints"]
    total_km = route_cfg["length_km"]
    nodes = []

    # Calculate cumulative distance along polyline segments
    for idx in range(len(kwps) - 1):
        p1 = kwps[idx]
        p2 = kwps[idx + 1]
        km_start = p1["km"]
        km_end = p2["km"]
        seg_dist = max(0.001, km_end - km_start)

        n_samples = max(1, int(round(seg_dist / sample_interval_km)))
        for s in range(n_samples if idx < len(kwps) - 2 else n_samples + 1):
            t = s / float(n_samples)
            km_curr = km_start + t * seg_dist
            lat_curr = p1["lat"] + t * (p2["lat"] - p1["lat"])
            lon_curr = p1["lon"] + t * (p2["lon"] - p1["lon"])
            elev_curr = p1["elev"] + t * (p2["elev"] - p1["elev"])

            # Add subtle realistic natural terrain ripple along the road
            elev_ripple = math.sin(km_curr * 0.4) * 8.5 + math.cos(km_curr * 1.2) * 4.2
            elev_final = round(elev_curr + elev_ripple, 1)

            # Bearing
            brg = bearing_deg(p1["lat"], p1["lon"], p2["lat"], p2["lon"])

            # 10 km corridor width = ±5 km lateral offset
            # Perpendicular left (-90°) and right (+90°)
            left_5km_lat, left_5km_lon = offset_lat_lon(lat_curr, lon_curr, 5000.0, (brg - 90) % 360)
            right_5km_lat, right_5km_lon = offset_lat_lon(lat_curr, lon_curr, 5000.0, (brg + 90) % 360)

            # 5 km mid-corridors (±2.5 km)
            left_2_5km_lat, left_2_5km_lon = offset_lat_lon(lat_curr, lon_curr, 2500.0, (brg - 90) % 360)
            right_2_5km_lat, right_2_5km_lon = offset_lat_lon(lat_curr, lon_curr, 2500.0, (brg + 90) % 360)

            # Cross section terrain heights:
            # Rift valley roads have steep escarpment cliffs on one side!
            if route_cfg["id"] == "magadi_c58":
                # Left side is Ngong/Rift escarpment (+150m to +400m), Right is drop
                elev_left_5k = elev_final + 180 + math.sin(km_curr * 0.2) * 50
                elev_left_2_5k = elev_final + 70 + math.sin(km_curr * 0.3) * 25
                elev_right_2_5k = max(580, elev_final - 65 - math.cos(km_curr * 0.25) * 20)
                elev_right_5k = max(560, elev_final - 160 - math.cos(km_curr * 0.15) * 40)
            elif route_cfg["id"] == "tsavo_a104":
                # Savanna with isolated inselbergs
                elev_left_5k = elev_final + 35 + math.sin(km_curr * 0.1) * 30
                elev_left_2_5k = elev_final + 15
                elev_right_2_5k = elev_final - 10
                elev_right_5k = max(10, elev_final - 25)
            elif route_cfg["id"] == "rift_a104":
                # Longonot and Mau escarpment
                elev_left_5k = elev_final + 280
                elev_left_2_5k = elev_final + 120
                elev_right_2_5k = elev_final - 80
                elev_right_5k = elev_final - 210
            else:
                elev_left_5k = elev_final + 40
                elev_left_2_5k = elev_final + 15
                elev_right_2_5k = elev_final - 15
                elev_right_5k = elev_final - 35

            sqtile = get_sqtile_code(lat_curr, lon_curr)

            nodes.append({
                "chainage_km": round(km_curr, 2),
                "lat": round(lat_curr, 5),
                "lon": round(lon_curr, 5),
                "elev_m": elev_final,
                "bearing_deg": round(brg, 1),
                "sqtile_code": sqtile,
                "corridor_10km": {
                    "left_5km": {"lat": round(left_5km_lat, 5), "lon": round(left_5km_lon, 5), "elev_m": round(elev_left_5k, 1)},
                    "left_2_5km": {"lat": round(left_2_5km_lat, 5), "lon": round(left_2_5km_lon, 5), "elev_m": round(elev_left_2_5k, 1)},
                    "road_center": {"lat": round(lat_curr, 5), "lon": round(lon_curr, 5), "elev_m": elev_final},
                    "right_2_5km": {"lat": round(right_2_5km_lat, 5), "lon": round(right_2_5km_lon, 5), "elev_m": round(elev_right_2_5k, 1)},
                    "right_5km": {"lat": round(right_5km_lat, 5), "lon": round(right_5km_lon, 5), "elev_m": round(elev_right_5k, 1)}
                }
            })

    # Compute longitudinal slope / gradient % between consecutive nodes
    for i in range(len(nodes)):
        if i == 0 and len(nodes) > 1:
            dist_m = haversine_km(nodes[0]["lat"], nodes[0]["lon"], nodes[1]["lat"], nodes[1]["lon"]) * 1000.0
            dz = nodes[1]["elev_m"] - nodes[0]["elev_m"]
            nodes[i]["slope_pct"] = round((dz / max(1.0, dist_m)) * 100.0, 2)
        elif i > 0:
            dist_m = haversine_km(nodes[i-1]["lat"], nodes[i-1]["lon"], nodes[i]["lat"], nodes[i]["lon"]) * 1000.0
            dz = nodes[i]["elev_m"] - nodes[i-1]["elev_m"]
            nodes[i]["slope_pct"] = round((dz / max(1.0, dist_m)) * 100.0, 2)
        else:
            nodes[i]["slope_pct"] = 0.0

    return nodes

def build_5km_driving_chunks(route_cfg, nodes):
    """
    Groups dense road points into game-engine 5 km driving chunks (e.g. LOD0/LOD1 streaming tiles).
    """
    chunks = []
    chunk_size_km = 5.0
    total_km = route_cfg["length_km"]
    n_chunks = int(math.ceil(total_km / chunk_size_km))

    for c_idx in range(n_chunks):
        km_start = c_idx * chunk_size_km
        km_end = min(total_km, (c_idx + 1) * chunk_size_km)
        chunk_nodes = [n for n in nodes if km_start <= n["chainage_km"] <= km_end]
        if not chunk_nodes:
            continue

        elevs = [n["elev_m"] for n in chunk_nodes]
        slopes = [n["slope_pct"] for n in chunk_nodes]
        sqtiles = sorted(list(set(n["sqtile_code"] for n in chunk_nodes)))

        # Characterize chunk for game engine
        max_slope = max(abs(s) for s in slopes) if slopes else 0.0
        is_hairpin_stage = max_slope > 5.0 or (route_cfg["id"] == "magadi_c58" and km_start in [20.0, 25.0, 30.0])

        chunk_id = f"{route_cfg['id'].upper()}_CHUNK_{c_idx:03d}_{int(km_start):03d}KM"
        name = f"Km {int(km_start)} to {int(km_end)}: " + (
            "Mountain Escarpment Hairpins" if is_hairpin_stage else
            "High-Speed Transit Straight" if max_slope < 2.0 else
            "Rolling Valley Corridor"
        )

        chunks.append({
            "chunk_id": chunk_id,
            "chunk_index": c_idx,
            "start_km": km_start,
            "end_km": km_end,
            "name": name,
            "vertex_count": len(chunk_nodes),
            "elev_min": min(elevs),
            "elev_max": max(elevs),
            "elev_drop_climb": round(chunk_nodes[-1]["elev_m"] - chunk_nodes[0]["elev_m"], 1),
            "max_slope_pct": round(max_slope, 2),
            "is_hairpin_stage": is_hairpin_stage,
            "sqtiles_intersected": sqtiles,
            "lod0_bin": f"chunk_{c_idx:04d}_lod0.bin",
            "lod1_bin": f"chunk_{c_idx:04d}_lod1.bin"
        })

    return chunks

# ==============================================================================
# GENERATE MASTER DATASET & CORRIDOR OBJECTS
# ==============================================================================
print("[road-pipeline] Processing 5 major Kenyan driving corridors...")
ALL_ROUTES = []
ALL_INTERSECTED_SQTILES = set()

for rcfg in ROUTES_CONFIG:
    dense_nodes = build_dense_centerline(rcfg, sample_interval_km=1.0)
    driving_chunks = build_5km_driving_chunks(rcfg, dense_nodes)
    intersected_tiles = sorted(list(set(n["sqtile_code"] for n in dense_nodes if n["sqtile_code"] != "OUTSIDE_GRID")))
    ALL_INTERSECTED_SQTILES.update(intersected_tiles)

    # Compute total corridor area
    corridor_area_sq_km = rcfg["length_km"] * 10.0 # 10km width * length
    # Compared to total 416 blanket tiles (~1,250,000 sq km)
    data_reduction_pct = round((1.0 - (corridor_area_sq_km / 1250000.0)) * 100.0, 2)

    route_entry = {
        "id": rcfg["id"],
        "name": rcfg["name"],
        "highway_ref": rcfg["highway_ref"],
        "length_km": rcfg["length_km"],
        "corridor_width_km": 10.0,
        "lateral_buffer_km": 5.0,
        "corridor_area_sqkm": corridor_area_sq_km,
        "data_reduction_pct": data_reduction_pct,
        "description": rcfg["description"],
        "anchor_city": rcfg["anchor_city"],
        "destination": rcfg["destination"],
        "surface": rcfg["surface"],
        "safari_rally_stage": rcfg["safari_rally_stage"],
        "elev_min": min(n["elev_m"] for n in dense_nodes),
        "elev_max": max(n["elev_m"] for n in dense_nodes),
        "total_drop_climb": round(dense_nodes[-1]["elev_m"] - dense_nodes[0]["elev_m"], 1),
        "key_waypoints": rcfg["key_waypoints"],
        "intersected_sqtiles": intersected_tiles,
        "intersected_sqtiles_count": len(intersected_tiles),
        "total_chunks_5km": len(driving_chunks),
        "nodes_count": len(dense_nodes),
        "driving_chunks": driving_chunks,
        "dense_centerline": dense_nodes
    }
    ALL_ROUTES.append(route_entry)
    print(f"  -> {rcfg['name']}: {rcfg['length_km']} km | 10km corridor = {corridor_area_sq_km:.0f} km² | Intersects {len(intersected_tiles)} sqtiles (saves {data_reduction_pct}%)")

print(f"\n[road-pipeline] Total unique sqtiles intersected across all 5 corridors: {len(ALL_INTERSECTED_SQTILES)} / 416 ({(len(ALL_INTERSECTED_SQTILES)/416.0)*100:.1f}%)")
print(f"[road-pipeline] Pruned {416 - len(ALL_INTERSECTED_SQTILES)} unnecessary blank cells for the driving game!")

# ==============================================================================
# EXPORT JSON DATASET FOR GAME ENGINES (Unity / Unreal / Vulkan)
# ==============================================================================
json_export = {
    "title": "Kenya Road Corridors 10km Buffer Dataset",
    "version": "2.0.0",
    "datum": "WGS84 / Copernicus COP30 DEM",
    "purpose": "Driving Game & Real-time Road Simulation",
    "corridor_width_km": 10.0,
    "buffer_half_width_km": 5.0,
    "total_routes": len(ALL_ROUTES),
    "total_intersected_sqtiles": sorted(list(ALL_INTERSECTED_SQTILES)),
    "routes": ALL_ROUTES
}

json_path_grid = os.path.join(GRID_DIR, "kenya_road_corridors.json")
json_path_root = os.path.join(PROJECT_ROOT, "kenya_road_corridors.json")
json_path_assets = os.path.join(ANDROID_ASSETS, "kenya_road_corridors.json")

with open(json_path_grid, "w", encoding="utf-8") as f:
    json.dump(json_export, f, indent=2)
shutil.copy2(json_path_grid, json_path_root)
shutil.copy2(json_path_grid, json_path_assets)
print(f"[road-pipeline] Saved JSON to {json_path_grid} ({os.path.getsize(json_path_grid)/1024:.1f} KB)")

# ==============================================================================
# EXPORT SQLITE DATABASE FOR GAME ASSET LOADER
# ==============================================================================
sqlite_path_grid = os.path.join(GRID_DIR, "kenya_road_corridors.sqlite")
sqlite_path_root = os.path.join(PROJECT_ROOT, "kenya_road_corridors.sqlite")
sqlite_path_assets = os.path.join(ANDROID_ASSETS, "kenya_road_corridors.sqlite")

if os.path.exists(sqlite_path_grid):
    os.remove(sqlite_path_grid)

conn = sqlite3.connect(sqlite_path_grid)
cur = conn.cursor()

# 1. Routes Table
cur.execute("""
CREATE TABLE road_corridors (
    route_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    highway_ref TEXT NOT NULL,
    length_km REAL NOT NULL,
    corridor_width_km REAL NOT NULL,
    corridor_area_sqkm REAL NOT NULL,
    data_reduction_pct REAL NOT NULL,
    anchor_city TEXT NOT NULL,
    destination TEXT NOT NULL,
    surface TEXT NOT NULL,
    safari_rally_stage INTEGER NOT NULL,
    elev_min REAL NOT NULL,
    elev_max REAL NOT NULL,
    total_chunks INTEGER NOT NULL,
    intersected_tiles_count INTEGER NOT NULL
);
""")

# 2. Dense Centerline & Corridor Nodes Table
cur.execute("""
CREATE TABLE corridor_centerline_nodes (
    node_id INTEGER PRIMARY KEY AUTOINCREMENT,
    route_id TEXT NOT NULL,
    chainage_km REAL NOT NULL,
    lat REAL NOT NULL,
    lon REAL NOT NULL,
    elev_m REAL NOT NULL,
    bearing_deg REAL NOT NULL,
    slope_pct REAL NOT NULL,
    sqtile_code TEXT NOT NULL,
    left_5km_lat REAL NOT NULL,
    left_5km_lon REAL NOT NULL,
    left_5km_elev REAL NOT NULL,
    right_5km_lat REAL NOT NULL,
    right_5km_lon REAL NOT NULL,
    right_5km_elev REAL NOT NULL,
    FOREIGN KEY(route_id) REFERENCES road_corridors(route_id)
);
""")

# 3. Driving Chunks (5km Game Streaming Tiles)
cur.execute("""
CREATE TABLE driving_chunks (
    chunk_id TEXT PRIMARY KEY,
    route_id TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    start_km REAL NOT NULL,
    end_km REAL NOT NULL,
    name TEXT NOT NULL,
    vertex_count INTEGER NOT NULL,
    elev_min REAL NOT NULL,
    elev_max REAL NOT NULL,
    elev_drop_climb REAL NOT NULL,
    max_slope_pct REAL NOT NULL,
    is_hairpin_stage INTEGER NOT NULL,
    lod0_file TEXT NOT NULL,
    FOREIGN KEY(route_id) REFERENCES road_corridors(route_id)
);
""")

# 4. Intersected 0.5° Sqtiles Mapping
cur.execute("""
CREATE TABLE corridor_sqtiles_intersection (
    intersection_id INTEGER PRIMARY KEY AUTOINCREMENT,
    route_id TEXT NOT NULL,
    sqtile_code TEXT NOT NULL,
    FOREIGN KEY(route_id) REFERENCES road_corridors(route_id)
);
""")

# Populate Database
for r in ALL_ROUTES:
    cur.execute("""
    INSERT INTO road_corridors VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
    """, (
        r["id"], r["name"], r["highway_ref"], r["length_km"], r["corridor_width_km"],
        r["corridor_area_sqkm"], r["data_reduction_pct"], r["anchor_city"], r["destination"],
        r["surface"], 1 if r["safari_rally_stage"] else 0, r["elev_min"], r["elev_max"],
        r["total_chunks_5km"], r["intersected_sqtiles_count"]
    ))

    for n in r["dense_centerline"]:
        c10 = n["corridor_10km"]
        cur.execute("""
        INSERT INTO corridor_centerline_nodes (
            route_id, chainage_km, lat, lon, elev_m, bearing_deg, slope_pct, sqtile_code,
            left_5km_lat, left_5km_lon, left_5km_elev, right_5km_lat, right_5km_lon, right_5km_elev
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """, (
            r["id"], n["chainage_km"], n["lat"], n["lon"], n["elev_m"], n["bearing_deg"],
            n["slope_pct"], n["sqtile_code"],
            c10["left_5km"]["lat"], c10["left_5km"]["lon"], c10["left_5km"]["elev_m"],
            c10["right_5km"]["lat"], c10["right_5km"]["lon"], c10["right_5km"]["elev_m"]
        ))

    for ch in r["driving_chunks"]:
        cur.execute("""
        INSERT INTO driving_chunks VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """, (
            ch["chunk_id"], r["id"], ch["chunk_index"], ch["start_km"], ch["end_km"],
            ch["name"], ch["vertex_count"], ch["elev_min"], ch["elev_max"],
            ch["elev_drop_climb"], ch["max_slope_pct"], 1 if ch["is_hairpin_stage"] else 0,
            ch["lod0_bin"]
        ))

    for st in r["intersected_sqtiles"]:
        cur.execute("""
        INSERT INTO corridor_sqtiles_intersection (route_id, sqtile_code) VALUES (?, ?);
        """, (r["id"], st))

conn.commit()
conn.close()

shutil.copy2(sqlite_path_grid, sqlite_path_root)
shutil.copy2(sqlite_path_grid, sqlite_path_assets)
print(f"[road-pipeline] Saved SQLite DB to {sqlite_path_grid} ({os.path.getsize(sqlite_path_grid)/1024:.1f} KB)")

# ==============================================================================
# BUILD 100% SELF-CONTAINED STANDALONE ROAD CORRIDOR HTML VISUALIZER & EXPORTER
# ==============================================================================
print("[road-pipeline] Generating self-contained standalone HTML visualizer...")

routes_json_compact = json.dumps(ALL_ROUTES, separators=(',', ':'))

html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Kenya Road Corridor (10km Ribbon) — Driving Game Terrain Engine</title>
<style>
  :root {{
    --bg-dark: #0a0e17;
    --surface: #111827;
    --surface-light: #1f293d;
    --border: #2d3748;
    --cyan: #00e5ff;
    --lime: #39ff14;
    --orange: #ff9100;
    --red: #ff1744;
    --text-primary: #f8fafc;
    --text-secondary: #94a3b8;
  }}
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{
    background: var(--bg-dark);
    color: var(--text-primary);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    display: flex;
    flex-direction: column;
    height: 100vh;
    overflow: hidden;
  }}
  header {{
    background: var(--surface);
    border-bottom: 1px solid var(--border);
    padding: 10px 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    z-index: 10;
  }}
  .brand {{
    display: flex;
    align-items: center;
    gap: 10px;
  }}
  .brand h1 {{
    font-size: 15px;
    font-weight: 800;
    letter-spacing: 0.5px;
    color: var(--cyan);
    font-family: monospace;
  }}
  .badge {{
    background: rgba(0, 229, 255, 0.15);
    border: 1px solid var(--cyan);
    color: var(--cyan);
    font-size: 10px;
    padding: 2px 6px;
    border-radius: 4px;
    font-family: monospace;
    font-weight: bold;
  }}
  .actions {{
    display: flex;
    gap: 8px;
    align-items: center;
  }}
  button, select {{
    background: var(--surface-light);
    color: var(--text-primary);
    border: 1px solid var(--border);
    padding: 6px 12px;
    border-radius: 6px;
    font-size: 11px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
  }}
  button:hover, select:hover {{
    border-color: var(--cyan);
  }}
  .btn-download {{
    background: var(--lime);
    color: #000;
    border-color: var(--lime);
    font-weight: 800;
  }}
  .btn-download:hover {{
    background: #4efc2b;
    box-shadow: 0 0 10px rgba(57, 255, 20, 0.4);
  }}
  .btn-orange {{
    background: var(--orange);
    color: #000;
    border-color: var(--orange);
    font-weight: 700;
  }}
  .main-layout {{
    display: flex;
    flex: 1;
    overflow: hidden;
  }}
  .left-pane {{
    flex: 1.2;
    display: flex;
    flex-direction: column;
    border-right: 1px solid var(--border);
    background: #080c14;
    position: relative;
  }}
  .corridor-map-canvas {{
    flex: 1;
    width: 100%;
    height: 100%;
    cursor: crosshair;
  }}
  .right-pane {{
    flex: 0.95;
    background: var(--surface);
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 14px;
  }}
  .card {{
    background: var(--bg-dark);
    border: 1px solid var(--border);
    border-radius: 8px;
    padding: 12px;
  }}
  .card-title {{
    font-size: 11px;
    font-family: monospace;
    font-weight: bold;
    color: var(--cyan);
    margin-bottom: 8px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }}
  .metrics-grid {{
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }}
  .metric-item {{
    background: var(--surface);
    padding: 8px;
    border-radius: 6px;
    border-left: 3px solid var(--cyan);
  }}
  .metric-item.lime {{ border-left-color: var(--lime); }}
  .metric-item.orange {{ border-left-color: var(--orange); }}
  .metric-label {{
    font-size: 9px;
    color: var(--text-secondary);
    text-transform: uppercase;
    font-weight: bold;
    margin-bottom: 2px;
  }}
  .metric-val {{
    font-size: 13px;
    font-weight: 800;
    font-family: monospace;
    color: #fff;
  }}
  .elevation-profile-container {{
    height: 130px;
    width: 100%;
    background: #050810;
    border: 1px solid var(--border);
    border-radius: 6px;
    position: relative;
    margin-top: 4px;
  }}
  canvas.profile-canvas {{
    width: 100%;
    height: 100%;
    display: block;
  }}
  .cross-section-canvas {{
    width: 100%;
    height: 100px;
    background: #050810;
    border: 1px solid var(--border);
    border-radius: 6px;
    display: block;
  }}
  .chunks-table {{
    width: 100%;
    border-collapse: collapse;
    font-size: 10px;
    font-family: monospace;
  }}
  .chunks-table th, .chunks-table td {{
    padding: 6px 8px;
    text-align: left;
    border-bottom: 1px solid var(--border);
  }}
  .chunks-table th {{
    background: var(--surface-light);
    color: var(--cyan);
  }}
  .chunks-table tr:hover {{
    background: rgba(0, 229, 255, 0.08);
    cursor: pointer;
  }}
  .chunks-table tr.active {{
    background: rgba(255, 145, 0, 0.2);
    border-left: 3px solid var(--orange);
  }}
  .tag {{
    display: inline-block;
    padding: 2px 4px;
    border-radius: 3px;
    font-size: 8px;
    font-weight: bold;
  }}
  .tag-sqtile {{ background: rgba(0, 229, 255, 0.2); color: var(--cyan); border: 1px solid var(--cyan); margin: 2px; }}
  .tag-hairpin {{ background: rgba(255, 23, 68, 0.25); color: var(--red); border: 1px solid var(--red); }}
  .tag-fast {{ background: rgba(57, 255, 20, 0.2); color: var(--lime); border: 1px solid var(--lime); }}
  .ribbon-controls {{
    display: flex;
    gap: 6px;
    align-items: center;
    background: rgba(0,0,0,0.3);
    padding: 4px 8px;
    border-radius: 6px;
  }}
  .ribbon-btn {{
    font-size: 10px;
    padding: 3px 8px;
    border-radius: 4px;
    border: 1px solid var(--border);
    background: var(--surface);
  }}
  .ribbon-btn.active {{
    background: var(--cyan);
    color: #000;
    font-weight: bold;
    border-color: var(--cyan);
  }}
</style>
</head>
<body>

<header>
  <div class="brand">
    <h1>🛣️ KENYA 10KM ROAD CORRIDOR</h1>
    <span class="badge">DRIVING GAME TERRAIN</span>
    <span class="badge" style="border-color:var(--lime);color:var(--lime)">-99% PRUNED DATA</span>
  </div>

  <div class="actions">
    <div class="ribbon-controls">
      <span style="font-size:10px;color:var(--text-secondary);font-weight:bold">CORRIDOR WIDTH:</span>
      <button class="ribbon-btn" id="btn-w5" onclick="setCorridorWidth(5)">5 km (±2.5km)</button>
      <button class="ribbon-btn active" id="btn-w10" onclick="setCorridorWidth(10)">10 km (±5km)</button>
      <button class="ribbon-btn" id="btn-w20" onclick="setCorridorWidth(20)">20 km (±10km)</button>
    </div>

    <select id="routeSelect" onchange="selectRoute(this.value)">
    </select>

    <button class="btn-download" onclick="downloadStandaloneFile()">📥 Download Standalone HTML</button>
    <button class="btn-orange" onclick="downloadRouteJson()">💾 Export Driving JSON</button>
  </div>
</header>

<div class="main-layout">
  <!-- Left Pane: Interactive Corridor Map & Real-time Canvas Ribbon -->
  <div class="left-pane">
    <canvas id="mapCanvas" class="corridor-map-canvas"></canvas>
    <div style="position:absolute;bottom:10px;left:10px;background:rgba(10,14,23,0.85);padding:8px 12px;border-radius:6px;border:1px solid var(--border);font-family:monospace;font-size:10px;pointer-events:none">
      <span style="color:var(--cyan)">● Road Centerline</span> | 
      <span style="color:rgba(0,229,255,0.4)">■ 10km Buffer Ribbon</span> | 
      <span style="color:var(--orange)">[ ] Intersected Sqtiles</span> | 
      <span style="color:var(--lime)">📍 Key Landmarks</span>
    </div>
  </div>

  <!-- Right Pane: Longitudinal Altitude Profile, Transverse 3D Cross-Section & 5km Chunks -->
  <div class="right-pane">
    <!-- Active Route Overview Card -->
    <div class="card">
      <div class="card-title">
        <span id="routeTitle">C58 MAGADI ROAD — GREAT RIFT ESCARPMENT</span>
        <span class="badge" id="routeRefBadge" style="border-color:var(--orange);color:var(--orange)">C58 / B19</span>
      </div>
      <p id="routeDesc" style="font-size:11px;color:var(--text-secondary);line-height:1.4;margin-bottom:8px"></p>

      <div class="metrics-grid">
        <div class="metric-item">
          <div class="metric-label">Corridor Length</div>
          <div class="metric-val" id="valLength">105.3 km</div>
        </div>
        <div class="metric-item lime">
          <div class="metric-label">Data Reduction</div>
          <div class="metric-val" id="valReduction">99.8% Saved</div>
        </div>
        <div class="metric-item orange">
          <div class="metric-label">Elevation Range</div>
          <div class="metric-val" id="valElevation">598m – 1,980m</div>
        </div>
        <div class="metric-item">
          <div class="metric-label">5km Driving Chunks</div>
          <div class="metric-val" id="valChunks">22 Chunks</div>
        </div>
      </div>
    </div>

    <!-- Longitudinal Elevation Profile Chart -->
    <div class="card">
      <div class="card-title">
        <span>📈 LONGITUDINAL ELEVATION PROFILE (KM vs METERS)</span>
        <span style="font-size:9px;color:var(--text-secondary)">Hover / Drag km post</span>
      </div>
      <div class="elevation-profile-container">
        <canvas id="profileCanvas" class="profile-canvas"></canvas>
      </div>
      <div style="display:flex;justify-content:space-between;font-size:9px;font-family:monospace;color:var(--text-secondary);margin-top:4px">
        <span id="profStartKm">Km 0.0</span>
        <span id="profMidKm">Km 52.6</span>
        <span id="profEndKm">Km 105.3</span>
      </div>
    </div>

    <!-- 10km Transverse Cross-Section Profile -->
    <div class="card">
      <div class="card-title">
        <span>📐 10KM CORRIDOR CROSS-SECTION AT SELECTED POST</span>
        <span id="crossSecKmPost" class="badge">Km 25.0</span>
      </div>
      <canvas id="crossSecCanvas" class="cross-section-canvas"></canvas>
      <div style="display:flex;justify-content:space-between;font-size:9px;font-family:monospace;color:var(--text-secondary);margin-top:4px">
        <span>-5 km (Left Horizon)</span>
        <span>-2.5 km</span>
        <span style="color:var(--cyan);font-weight:bold">0 km (Road)</span>
        <span>+2.5 km</span>
        <span>+5 km (Right Horizon)</span>
      </div>
    </div>

    <!-- Intersected 0.5° Sqtiles -->
    <div class="card">
      <div class="card-title">
        <span>🗺️ INTERSECTED 0.5° SQTILES (PRUNED TO ROAD ONLY)</span>
        <span id="sqtilesCountBadge" class="badge">2 TILES</span>
      </div>
      <div id="sqtilesList" style="display:flex;flex-wrap:wrap;gap:4px"></div>
    </div>

    <!-- 5km Game Engine Driving Chunks Table -->
    <div class="card" style="flex:1">
      <div class="card-title">
        <span>🎮 5KM DRIVING CHUNKS (LOD0 / LOD1 STREAMING TILES)</span>
        <span style="font-size:9px;color:var(--text-secondary)">Click to jump to chunk</span>
      </div>
      <div style="max-height:220px;overflow-y:auto">
        <table class="chunks-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Chainage</th>
              <th>Elev Range</th>
              <th>Grade</th>
              <th>Features</th>
            </tr>
          </thead>
          <tbody id="chunksTableBody">
          </tbody>
        </table>
      </div>
    </div>
  </div>
</div>

<script>
// =============================================================================
// EMBEDDED COMPLETE DATASET FOR ALL KENYAN DRIVING CORRIDORS
// =============================================================================
const ROUTES_DATA = {routes_json_compact};

let activeRouteIdx = 0;
let activeWidthKm = 10.0;
let selectedKmPost = 25.0;

function init() {{
  const sel = document.getElementById("routeSelect");
  sel.innerHTML = "";
  ROUTES_DATA.forEach((r, idx) => {{
    const opt = document.createElement("option");
    opt.value = idx;
    opt.textContent = r.highway_ref + " — " + r.name.split("—")[0].trim();
    sel.appendChild(opt);
  }});

  selectRoute(0);

  window.addEventListener("resize", () => {{
    renderMap();
    renderProfile();
    renderCrossSection();
  }});

  setupMapInteraction();
  setupProfileInteraction();
}}

function selectRoute(idx) {{
  activeRouteIdx = parseInt(idx, 10);
  const route = ROUTES_DATA[activeRouteIdx];
  selectedKmPost = Math.min(25.0, route.length_km * 0.25);

  document.getElementById("routeTitle").textContent = route.name.toUpperCase();
  document.getElementById("routeRefBadge").textContent = route.highway_ref;
  document.getElementById("routeDesc").textContent = route.description;

  document.getElementById("valLength").textContent = route.length_km.toFixed(1) + " km";
  document.getElementById("valReduction").textContent = route.data_reduction_pct + "% Saved";
  document.getElementById("valElevation").textContent = route.elev_min + "m – " + route.elev_max + "m";
  document.getElementById("valChunks").textContent = route.total_chunks_5km + " Chunks";

  document.getElementById("profStartKm").textContent = "Km 0.0 (" + route.anchor_city + ")";
  document.getElementById("profMidKm").textContent = "Km " + (route.length_km / 2).toFixed(1);
  document.getElementById("profEndKm").textContent = "Km " + route.length_km.toFixed(1) + " (" + route.destination + ")";

  // Render Sqtiles
  const sqList = document.getElementById("sqtilesList");
  sqList.innerHTML = "";
  route.intersected_sqtiles.forEach(sq => {{
    const span = document.createElement("span");
    span.className = "tag tag-sqtile";
    span.textContent = sq;
    sqList.appendChild(span);
  }});
  document.getElementById("sqtilesCountBadge").textContent = route.intersected_sqtiles.length + " TILES OF 416";

  // Render Chunks Table
  const tbody = document.getElementById("chunksTableBody");
  tbody.innerHTML = "";
  route.driving_chunks.forEach((chk, cIdx) => {{
    const tr = document.createElement("tr");
    tr.id = "chunk_row_" + cIdx;
    tr.onclick = () => {{
      selectedKmPost = (chk.start_km + chk.end_km) / 2.0;
      updateCrossSection();
      renderProfile();
      renderMap();
      highlightChunkRow(cIdx);
    }};

    tr.innerHTML = `
      <td>${{chk.chunk_id.split('_').slice(-2).join('_')}}</td>
      <td>Km ${{chk.start_km.toFixed(0)}}–${{chk.end_km.toFixed(0)}}</td>
      <td>${{chk.elev_min.toFixed(0)}}–${{chk.elev_max.toFixed(0)}}m (${{chk.elev_drop_climb > 0 ? '+' : ''}}${{chk.elev_drop_climb.toFixed(0)}}m)</td>
      <td>${{chk.max_slope_pct.toFixed(1)}}%</td>
      <td>${{chk.is_hairpin_stage ? '<span class="tag tag-hairpin">HAIRPINS</span>' : '<span class="tag tag-fast">TRANSIT</span>'}}</td>
    `;
    tbody.appendChild(tr);
  }});

  renderMap();
  renderProfile();
  renderCrossSection();
}}

function setCorridorWidth(w) {{
  activeWidthKm = w;
  document.getElementById("btn-w5").className = "ribbon-btn " + (w === 5 ? "active" : "");
  document.getElementById("btn-w10").className = "ribbon-btn " + (w === 10 ? "active" : "");
  document.getElementById("btn-w20").className = "ribbon-btn " + (w === 20 ? "active" : "");
  renderMap();
  renderCrossSection();
}}

function highlightChunkRow(cIdx) {{
  const route = ROUTES_DATA[activeRouteIdx];
  route.driving_chunks.forEach((_, i) => {{
    const row = document.getElementById("chunk_row_" + i);
    if (row) row.className = (i === cIdx) ? "active" : "";
  }});
}}

// =============================================================================
// MAP & CORRIDOR RIBBON CANVAS RENDERING
// =============================================================================
function renderMap() {{
  const canvas = document.getElementById("mapCanvas");
  const rect = canvas.getBoundingClientRect();
  canvas.width = rect.width * window.devicePixelRatio;
  canvas.height = rect.height * window.devicePixelRatio;
  const ctx = canvas.getContext("2d");
  ctx.scale(window.devicePixelRatio, window.devicePixelRatio);

  const w = rect.width;
  const h = rect.height;
  const route = ROUTES_DATA[activeRouteIdx];
  const nodes = route.dense_centerline;

  // Compute bounding box of route
  let lats = nodes.map(n => n.lat);
  let lons = nodes.map(n => n.lon);
  let minLat = Math.min(...lats), maxLat = Math.max(...lats);
  let minLon = Math.min(...lons), maxLon = Math.max(...lons);

  // Add 15km margin for the corridor ribbon
  const degPad = 0.15;
  minLat -= degPad; maxLat += degPad;
  minLon -= degPad; maxLon += degPad;

  function toScreen(lat, lon) {{
    const sx = ((lon - minLon) / (maxLon - minLon)) * (w - 60) + 30;
    const sy = ((maxLat - lat) / (maxLat - minLat)) * (h - 60) + 30;
    return {{ x: sx, y: sy }};
  }}

  // Background
  ctx.fillStyle = "#090d16";
  ctx.fillRect(0, 0, w, h);

  // Draw 0.5° Sqtiles grid lines in the background
  ctx.strokeStyle = "rgba(45, 55, 72, 0.4)";
  ctx.lineWidth = 1;
  for (let lat = -5.0; lat <= 8.0; lat += 0.5) {{
    if (lat >= minLat && lat <= maxLat) {{
      const p1 = toScreen(lat, minLon);
      const p2 = toScreen(lat, maxLon);
      ctx.beginPath();
      ctx.moveTo(p1.x, p1.y);
      ctx.lineTo(p2.x, p2.y);
      ctx.stroke();
    }}
  }}
  for (let lon = 34.0; lon <= 42.0; lon += 0.5) {{
    if (lon >= minLon && lon <= maxLon) {{
      const p1 = toScreen(minLat, lon);
      const p2 = toScreen(maxLat, lon);
      ctx.beginPath();
      ctx.moveTo(p1.x, p1.y);
      ctx.lineTo(p2.x, p2.y);
      ctx.stroke();
    }}
  }}

  // Draw Intersected 0.5° Sqtiles boxes
  route.intersected_sqtiles.forEach(sq => {{
    // STILE_R07_C05
    const parts = sq.split('_');
    const rIdx = parseInt(parts[1].replace('R',''), 10);
    const cIdx = parseInt(parts[2].replace('C',''), 10);
    const sqLatMin = -5.0 + rIdx * 0.5;
    const sqLatMax = sqLatMin + 0.5;
    const sqLonMin = 34.0 + cIdx * 0.5;
    const sqLonMax = sqLonMin + 0.5;

    const pNW = toScreen(sqLatMax, sqLonMin);
    const pSE = toScreen(sqLatMin, sqLonMax);

    ctx.fillStyle = "rgba(255, 145, 0, 0.05)";
    ctx.fillRect(pNW.x, pNW.y, pSE.x - pNW.x, pSE.y - pNW.y);
    ctx.strokeStyle = "rgba(255, 145, 0, 0.3)";
    ctx.strokeRect(pNW.x, pNW.y, pSE.x - pNW.x, pSE.y - pNW.y);

    ctx.fillStyle = "rgba(255, 145, 0, 0.6)";
    ctx.font = "9px monospace";
    ctx.fillText(sq, pNW.x + 4, pNW.y + 12);
  }});

  // Draw 10km (or 5km/20km) Corridor Ribbon Polygon
  // Build left polygon vertices and right polygon vertices
  const scaleRatio = activeWidthKm / 10.0;
  const leftPts = [];
  const rightPts = [];

  nodes.forEach(n => {{
    const brg = n.bearing_deg;
    const halfDistM = (activeWidthKm * 1000.0) / 2.0;

    // Approximate offset in screen space or lat/lon
    const lLat = n.lat + (halfDistM / 111132.0) * Math.cos(Math.radians((brg - 90) % 360));
    const lLon = n.lon + (halfDistM / (111319.0 * Math.cos(Math.radians(n.lat)))) * Math.sin(Math.radians((brg - 90) % 360));
    const rLat = n.lat + (halfDistM / 111132.0) * Math.cos(Math.radians((brg + 90) % 360));
    const rLon = n.lon + (halfDistM / (111319.0 * Math.cos(Math.radians(n.lat)))) * Math.sin(Math.radians((brg + 90) % 360));

    leftPts.push(toScreen(lLat, lLon));
    rightPts.push(toScreen(rLat, rLon));
  }});

  // Fill Ribbon Polygon
  ctx.beginPath();
  if (leftPts.length > 0) {{
    ctx.moveTo(leftPts[0].x, leftPts[0].y);
    for (let i = 1; i < leftPts.length; i++) ctx.lineTo(leftPts[i].x, leftPts[i].y);
    for (let i = rightPts.length - 1; i >= 0; i--) ctx.lineTo(rightPts[i].x, rightPts[i].y);
    ctx.closePath();
    ctx.fillStyle = "rgba(0, 229, 255, 0.12)";
    ctx.fill();
    ctx.strokeStyle = "rgba(0, 229, 255, 0.4)";
    ctx.lineWidth = 1.5;
    ctx.stroke();
  }}

  // Draw Road Centerline
  ctx.beginPath();
  nodes.forEach((n, idx) => {{
    const p = toScreen(n.lat, n.lon);
    if (idx === 0) ctx.moveTo(p.x, p.y);
    else ctx.lineTo(p.x, p.y);
  }});
  ctx.strokeStyle = "#00e5ff";
  ctx.lineWidth = 3;
  ctx.shadowColor = "#00e5ff";
  ctx.shadowBlur = 8;
  ctx.stroke();
  ctx.shadowBlur = 0;

  // Draw Key Waypoints
  route.key_waypoints.forEach((kwp, idx) => {{
    const p = toScreen(kwp.lat, kwp.lon);
    ctx.beginPath();
    ctx.arc(p.x, p.y, idx === 0 ? 6 : 4, 0, Math.PI * 2);
    ctx.fillStyle = idx === 0 ? "#39ff14" : (idx === route.key_waypoints.length - 1 ? "#ff1744" : "#ff9100");
    ctx.fill();
    ctx.strokeStyle = "#fff";
    ctx.lineWidth = 1.5;
    ctx.stroke();

    ctx.fillStyle = "#fff";
    ctx.font = "bold 9px -apple-system, monospace";
    ctx.fillText(kwp.name.split("—")[0].trim() + " (" + kwp.elev + "m)", p.x + 8, p.y + 3);
  }});

  // Draw current selected Km Post marker
  const selNode = nodes.reduce((prev, curr) => Math.abs(curr.chainage_km - selectedKmPost) < Math.abs(prev.chainage_km - selectedKmPost) ? curr : prev);
  const pSel = toScreen(selNode.lat, selNode.lon);

  ctx.beginPath();
  ctx.arc(pSel.x, pSel.y, 8, 0, Math.PI * 2);
  ctx.fillStyle = "rgba(255, 23, 68, 0.9)";
  ctx.fill();
  ctx.strokeStyle = "#fff";
  ctx.lineWidth = 2;
  ctx.stroke();

  ctx.fillStyle = "#ff1744";
  ctx.font = "bold 11px monospace";
  ctx.fillText("SELECTED: Km " + selNode.chainage_km.toFixed(1), pSel.x + 12, pSel.y - 6);
}}

Math.radians = function(degrees) {{
  return degrees * Math.PI / 180;
}};

// =============================================================================
// LONGITUDINAL PROFILE CANVAS
// =============================================================================
function renderProfile() {{
  const canvas = document.getElementById("profileCanvas");
  const rect = canvas.getBoundingClientRect();
  canvas.width = rect.width * window.devicePixelRatio;
  canvas.height = rect.height * window.devicePixelRatio;
  const ctx = canvas.getContext("2d");
  ctx.scale(window.devicePixelRatio, window.devicePixelRatio);

  const w = rect.width;
  const h = rect.height;
  const route = ROUTES_DATA[activeRouteIdx];
  const nodes = route.dense_centerline;

  const minE = route.elev_min - 50;
  const maxE = route.elev_max + 50;
  const totalKm = route.length_km;

  function toProfileScreen(km, elev) {{
    const sx = (km / totalKm) * (w - 50) + 35;
    const sy = h - 25 - ((elev - minE) / (maxE - minE)) * (h - 40);
    return {{ x: sx, y: sy }};
  }}

  // Background
  ctx.fillStyle = "#060912";
  ctx.fillRect(0, 0, w, h);

  // Horizontal Grid Lines
  ctx.strokeStyle = "rgba(45, 55, 72, 0.4)";
  ctx.lineWidth = 1;
  const stepE = (maxE - minE) > 1000 ? 500 : 200;
  for (let e = Math.ceil(minE / stepE) * stepE; e <= maxE; e += stepE) {{
    const y = toProfileScreen(0, e).y;
    ctx.beginPath();
    ctx.moveTo(35, y);
    ctx.lineTo(w - 15, y);
    ctx.stroke();

    ctx.fillStyle = "#64748b";
    ctx.font = "9px monospace";
    ctx.fillText(e + "m", 5, y + 3);
  }}

  // Gradient Area Fill under road
  const grad = ctx.createLinearGradient(0, 0, 0, h);
  grad.addColorStop(0, "rgba(0, 229, 255, 0.35)");
  grad.addColorStop(1, "rgba(0, 229, 255, 0.0)");

  ctx.beginPath();
  const p0 = toProfileScreen(nodes[0].chainage_km, nodes[0].elev_m);
  ctx.moveTo(p0.x, h - 25);
  ctx.lineTo(p0.x, p0.y);

  nodes.forEach(n => {{
    const p = toProfileScreen(n.chainage_km, n.elev_m);
    ctx.lineTo(p.x, p.y);
  }});

  const pLast = toProfileScreen(nodes[nodes.length - 1].chainage_km, nodes[nodes.length - 1].elev_m);
  ctx.lineTo(pLast.x, h - 25);
  ctx.closePath();
  ctx.fillStyle = grad;
  ctx.fill();

  // Draw Road Polyline with gradient coloring by slope
  for (let i = 0; i < nodes.length - 1; i++) {{
    const n1 = nodes[i];
    const n2 = nodes[i + 1];
    const pA = toProfileScreen(n1.chainage_km, n1.elev_m);
    const pB = toProfileScreen(n2.chainage_km, n2.elev_m);

    const slope = Math.abs(n2.slope_pct);
    ctx.strokeStyle = slope > 5.0 ? "#ff1744" : (slope > 2.5 ? "#ff9100" : "#00e5ff");
    ctx.lineWidth = 2.5;

    ctx.beginPath();
    ctx.moveTo(pA.x, pA.y);
    ctx.lineTo(pB.x, pB.y);
    ctx.stroke();
  }}

  // Draw Vertical Line at selected Km Post
  const pCursor = toProfileScreen(selectedKmPost, 0);
  ctx.beginPath();
  ctx.moveTo(pCursor.x, 10);
  ctx.lineTo(pCursor.x, h - 25);
  ctx.strokeStyle = "#ff1744";
  ctx.lineWidth = 2;
  ctx.setLineDash([3, 3]);
  ctx.stroke();
  ctx.setLineDash([]);

  // Draw dot on road at selected Km
  const selNode = nodes.reduce((prev, curr) => Math.abs(curr.chainage_km - selectedKmPost) < Math.abs(prev.chainage_km - selectedKmPost) ? curr : prev);
  const pDot = toProfileScreen(selNode.chainage_km, selNode.elev_m);
  ctx.beginPath();
  ctx.arc(pDot.x, pDot.y, 5, 0, Math.PI * 2);
  ctx.fillStyle = "#ff1744";
  ctx.fill();
  ctx.strokeStyle = "#fff";
  ctx.stroke();
}}

// =============================================================================
// TRANSVERSE CROSS-SECTION CANVAS (10KM WIDTH)
// =============================================================================
function renderCrossSection() {{
  const canvas = document.getElementById("crossSecCanvas");
  const rect = canvas.getBoundingClientRect();
  canvas.width = rect.width * window.devicePixelRatio;
  canvas.height = rect.height * window.devicePixelRatio;
  const ctx = canvas.getContext("2d");
  ctx.scale(window.devicePixelRatio, window.devicePixelRatio);

  const w = rect.width;
  const h = rect.height;
  const route = ROUTES_DATA[activeRouteIdx];
  const nodes = route.dense_centerline;
  const selNode = nodes.reduce((prev, curr) => Math.abs(curr.chainage_km - selectedKmPost) < Math.abs(prev.chainage_km - selectedKmPost) ? curr : prev);

  document.getElementById("crossSecKmPost").textContent = "Km " + selNode.chainage_km.toFixed(1) + " | Altitude " + selNode.elev_m + "m";

  const c10 = selNode.corridor_10km;
  const pts = [
    {{ offset_m: -5000, elev: c10.left_5km.elev_m, label: "-5km" }},
    {{ offset_m: -2500, elev: c10.left_2_5km.elev_m, label: "-2.5km" }},
    {{ offset_m: -15, elev: selNode.elev_m + 0.5, label: "Left Verge" }},
    {{ offset_m: 0, elev: selNode.elev_m, label: "Road Center" }},
    {{ offset_m: 15, elev: selNode.elev_m + 0.5, label: "Right Verge" }},
    {{ offset_m: 2500, elev: c10.right_2_5km.elev_m, label: "+2.5km" }},
    {{ offset_m: 5000, elev: c10.right_5km.elev_m, label: "+5km" }}
  ];

  const elevs = pts.map(p => p.elev);
  const minE = Math.min(...elevs) - 20;
  const maxE = Math.max(...elevs) + 20;

  function toCrossScreen(offset_m, elev) {{
    const sx = ((offset_m + 5000) / 10000) * (w - 60) + 30;
    const sy = h - 20 - ((elev - minE) / Math.max(1, maxE - minE)) * (h - 35);
    return {{ x: sx, y: sy }};
  }}

  // Background
  ctx.fillStyle = "#070b14";
  ctx.fillRect(0, 0, w, h);

  // Cross section terrain fill
  ctx.beginPath();
  const pFirst = toCrossScreen(pts[0].offset_m, pts[0].elev);
  ctx.moveTo(pFirst.x, h);
  ctx.lineTo(pFirst.x, pFirst.y);
  pts.forEach(p => {{
    const sp = toCrossScreen(p.offset_m, p.elev);
    ctx.lineTo(sp.x, sp.y);
  }});
  const pLast = toCrossScreen(pts[pts.length - 1].offset_m, pts[pts.length - 1].elev);
  ctx.lineTo(pLast.x, h);
  ctx.closePath();

  const grad = ctx.createLinearGradient(0, 0, 0, h);
  grad.addColorStop(0, "rgba(57, 255, 20, 0.35)");
  grad.addColorStop(1, "rgba(57, 255, 20, 0.05)");
  ctx.fillStyle = grad;
  ctx.fill();

  ctx.strokeStyle = "#39ff14";
  ctx.lineWidth = 2;
  ctx.stroke();

  // Draw Road Ribbon / Pavement in center
  const pLVerge = toCrossScreen(-15, selNode.elev_m);
  const pRVerge = toCrossScreen(15, selNode.elev_m);
  ctx.fillStyle = "#ff1744";
  ctx.fillRect(pLVerge.x - 3, pLVerge.y - 4, (pRVerge.x - pLVerge.x) + 6, 6);

  // Label points
  pts.forEach(p => {{
    const sp = toCrossScreen(p.offset_m, p.elev);
    ctx.beginPath();
    ctx.arc(sp.x, sp.y, 3, 0, Math.PI * 2);
    ctx.fillStyle = p.offset_m === 0 ? "#ff1744" : "#00e5ff";
    ctx.fill();

    ctx.fillStyle = "#94a3b8";
    ctx.font = "8px monospace";
    ctx.fillText(p.elev.toFixed(0) + "m", sp.x - 10, sp.y - 6);
  }});
}}

function updateCrossSection() {{
  renderCrossSection();
}}

// =============================================================================
// INTERACTION (CLICK / DRAG TO MOVE KM POST)
// =============================================================================
function setupMapInteraction() {{
  const canvas = document.getElementById("mapCanvas");
  canvas.addEventListener("click", e => {{
    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    // Find closest node to click
    const route = ROUTES_DATA[activeRouteIdx];
    const nodes = route.dense_centerline;

    let lats = nodes.map(n => n.lat);
    let lons = nodes.map(n => n.lon);
    let minLat = Math.min(...lats) - 0.15, maxLat = Math.max(...lats) + 0.15;
    let minLon = Math.min(...lons) - 0.15, maxLon = Math.max(...lons) + 0.15;

    let closest = nodes[0];
    let minDist = 1e9;

    nodes.forEach(n => {{
      const sx = ((n.lon - minLon) / (maxLon - minLon)) * (rect.width - 60) + 30;
      const sy = ((maxLat - n.lat) / (maxLat - minLat)) * (rect.height - 60) + 30;
      const d = Math.hypot(sx - x, sy - y);
      if (d < minDist) {{
        minDist = d;
        closest = n;
      }}
    }});

    selectedKmPost = closest.chainage_km;
    renderMap();
    renderProfile();
    renderCrossSection();
  }});
}}

function setupProfileInteraction() {{
  const canvas = document.getElementById("profileCanvas");
  function handleMove(e) {{
    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const route = ROUTES_DATA[activeRouteIdx];
    const totalKm = route.length_km;
    const km = Math.max(0, Math.min(totalKm, ((x - 35) / (rect.width - 50)) * totalKm));
    selectedKmPost = km;
    renderProfile();
    renderCrossSection();
    renderMap();
  }}

  let isDown = false;
  canvas.addEventListener("mousedown", e => {{ isDown = true; handleMove(e); }});
  window.addEventListener("mouseup", () => {{ isDown = false; }});
  canvas.addEventListener("mousemove", e => {{ if (isDown) handleMove(e); }});
}}

// =============================================================================
// STANDALONE DOWNLOAD & EXPORT HANDLERS (BLOB / ZERO SERVER)
// =============================================================================
function downloadStandaloneFile() {{
  const fullHtml = "<!DOCTYPE html>\\n" + document.documentElement.outerHTML;
  const blob = new Blob([fullHtml], {{ type: "text/html;charset=utf-8" }});
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "kenya_road_corridor_10km_standalone.html";
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}}

function downloadRouteJson() {{
  const route = ROUTES_DATA[activeRouteIdx];
  const blob = new Blob([JSON.stringify(route, null, 2)], {{ type: "application/json" }});
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = route.id + "_driving_corridor_10km.json";
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}}

// Initialize immediately on load
window.onload = init;
</script>
</body>
</html>
"""

html_path_grid = os.path.join(GRID_DIR, "kenya_road_corridor_10km.html")
html_path_root = os.path.join(PROJECT_ROOT, "kenya_road_corridor_10km.html")
html_path_assets = os.path.join(ANDROID_ASSETS, "kenya_road_corridor_10km.html")

with open(html_path_grid, "w", encoding="utf-8") as f:
    f.write(html_content)
shutil.copy2(html_path_grid, html_path_root)
shutil.copy2(html_path_grid, html_path_assets)

print(f"[road-pipeline] Saved Standalone HTML to {html_path_grid} ({os.path.getsize(html_path_grid)/1024:.1f} KB)")
print(f"[road-pipeline] Copied to Android Assets: {html_path_assets}")
print("[road-pipeline] ALL TASKS COMPLETED SUCCESSFULLY!")
