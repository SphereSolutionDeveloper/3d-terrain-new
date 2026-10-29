#!/usr/bin/env python3
"""
Standalone Terrain JSON Generator for Game Engine Integration
Exact mathematical equivalent of generate_terrain_json.py without external dependencies.
Produces transport_sim_test_001.json matching the exact specifications.
"""

import json
import math
import random

CELL_SIZE = 5.0
RESOLUTION = 2.0
TOTAL_M = 320.0
ROAD_CENTER = 160.0
HALF_SIDE = 125.0
ROAD_WIDTH = 5.0

res = int(TOTAL_M / RESOLUTION)  # 160
step = TOTAL_M / float(res - 1)

# 1. Coordinate Grid
x_coords = [i * step for i in range(res)]
y_coords = [i * step for i in range(res)]

# 2. Elevation Matrix (Base 1700m)
raw_dem = [[1700.0 for _ in range(res)] for _ in range(res)]

# West Hill
west_x, west_y = 10.0, 160.0
sig_west = 2.0 * (45.0 ** 2)

# Central Lake
lake_x, lake_y = 160.0, 160.0
sig_lake = 2.0 * (22.0 ** 2)

# Ditches
ditches = [(280.0, 220.0, 1.5), (290.0, 160.0, 1.0), (280.0, 100.0, 0.5)]

for j in range(res):
    y = y_coords[j]
    for i in range(res):
        x = x_coords[i]

        # West Hill
        dw2 = (x - west_x) ** 2 + (y - west_y) ** 2
        raw_dem[j][i] += 40.0 * math.exp(-dw2 / sig_west)

        # Central Lake
        dl2 = (x - lake_x) ** 2 + (y - lake_y) ** 2
        raw_dem[j][i] -= 15.0 * math.exp(-dl2 / sig_lake)

        # River Trench
        if abs(x - 160.0) < 5.0 and (185.0 < y < 320.0):
            raw_dem[j][i] -= 8.0

        # Ditches on East
        for dx, dy, depth in ditches:
            dd2 = (x - dx) ** 2 + (y - dy) ** 2
            raw_dem[j][i] -= depth * math.exp(-dd2 / (2.0 * (8.0 ** 2)))

# 3. 2D Gaussian Filter (sigma = 2, kernel size 9)
kernel_radius = 4
sigma = 2.0
two_sig_sq = 2.0 * sigma * sigma
kernel = []
for ky in range(-kernel_radius, kernel_radius + 1):
    row = []
    for kx in range(-kernel_radius, kernel_radius + 1):
        row.append(math.exp(-(kx * kx + ky * ky) / two_sig_sq))
    kernel.append(row)

# Normalize kernel
k_sum = sum(sum(r) for r in kernel)
for ky in range(len(kernel)):
    for kx in range(len(kernel[0])):
        kernel[ky][kx] /= k_sum

Z_dem = [[0.0 for _ in range(res)] for _ in range(res)]
for j in range(res):
    for i in range(res):
        accum = 0.0
        for ky in range(-kernel_radius, kernel_radius + 1):
            yj = min(max(j + ky, 0), res - 1)
            for kx in range(-kernel_radius, kernel_radius + 1):
                xi = min(max(i + kx, 0), res - 1)
                accum += raw_dem[yj][xi] * kernel[ky + kernel_radius][kx + kernel_radius]
        Z_dem[j][i] = round(accum, 3)

# 4. Surface Zones
surface_map = [["grass" for _ in range(res)] for _ in range(res)]

for j in range(res):
    y = y_coords[j]
    for i in range(res):
        x = x_coords[i]

        # Central Lake
        if math.sqrt((x - lake_x) ** 2 + (y - lake_y) ** 2) < 20.0:
            surface_map[j][i] = "water"

        # River Zone
        if abs(x - 160.0) < 5.0 and (185.0 < y < 320.0):
            surface_map[j][i] = "water"

        # Mud Zone
        if (25.0 < y < 45.0) and (50.0 < x < 270.0):
            surface_map[j][i] = "mud"

        # Gravel Ditches
        for dx, dy in [(280.0, 220.0), (290.0, 160.0), (280.0, 100.0)]:
            if math.sqrt((x - dx) ** 2 + (y - dy) ** 2) < 10.0:
                surface_map[j][i] = "gravel"

# Superellipse Road (Asphalt)
road_points = 500
n = 4.0
road_coords = []
for k in range(road_points):
    th = 2.0 * math.pi * k / float(road_points)
    ct = math.cos(th)
    st = math.sin(th)
    sign_c = 1.0 if ct >= 0 else -1.0
    sign_s = 1.0 if st >= 0 else -1.0
    xr = HALF_SIDE * sign_c * (abs(ct) ** (2.0 / n))
    yr = HALF_SIDE * sign_s * (abs(st) ** (2.0 / n))
    road_coords.append((ROAD_CENTER + xr, ROAD_CENTER + yr))

road_thresh = (ROAD_WIDTH / 2.0) + 1.0
for j in range(res):
    y = y_coords[j]
    for i in range(res):
        x = x_coords[i]
        # Quick bounding box check around road center loop
        if abs(x - ROAD_CENTER) > (HALF_SIDE + 8.0) or abs(y - ROAD_CENTER) > (HALF_SIDE + 8.0):
            continue
        # Check distance to nearest road centerline sample
        for rx, ry in road_coords:
            if math.hypot(x - rx, y - ry) < road_thresh:
                surface_map[j][i] = "asphalt"
                break

# 5. Asset Placements
assets = [
    {
        "id": "garage_001",
        "type": "garage",
        "glb": "assets/structures/garage_hub.glb",
        "position": [300.0, 300.0, 1700.0],
        "rotation": [0.0, 0.0, 0.0],
        "scale": [1.0, 1.0, 1.0]
    },
    {
        "id": "bridge_001",
        "type": "bridge",
        "glb": "assets/structures/concrete_bridge.glb",
        "position": [160.0, 285.0, 1700.0],
        "rotation": [0.0, 0.0, 0.0],
        "scale": [1.0, 1.0, 1.0]
    }
]

# Trees (Dense Forest on East)
rng = random.Random(42)
for i in range(30):
    tx = rng.uniform(250.0, 315.0)
    ty = rng.uniform(30.0, 290.0)
    ix = min(max(int(tx / RESOLUTION), 0), res - 1)
    iy = min(max(int(ty / RESOLUTION), 0), res - 1)
    tz = Z_dem[iy][ix]
    assets.append({
        "id": f"tree_00{i:02d}",
        "type": "tree",
        "glb": "assets/nature/palm_tree.glb",
        "position": [round(tx, 2), round(ty, 2), round(tz, 2)],
        "rotation": [0.0, round(rng.uniform(0.0, 360.0), 1), 0.0],
        "scale": [1.0, 1.0, 1.0]
    })

# Rocks (West Leg)
for i in range(10):
    rx = rng.uniform(15.0, 30.0)
    ry = rng.uniform(120.0, 180.0)
    ix = min(max(int(rx / RESOLUTION), 0), res - 1)
    iy = min(max(int(ry / RESOLUTION), 0), res - 1)
    rz = Z_dem[iy][ix]
    assets.append({
        "id": f"rock_00{i:02d}",
        "type": "rock",
        "glb": "assets/nature/boulder_1.5m.glb",
        "position": [round(rx, 2), round(ry, 2), round(rz, 2)],
        "rotation": [0.0, round(rng.uniform(0.0, 360.0), 1), 0.0],
        "scale": [1.0, 1.0, 1.0]
    })

# Start Line
assets.append({
    "id": "start_line",
    "type": "spawn",
    "glb": "assets/markers/start_grid.glb",
    "position": [300.0, 285.0, 1700.0],
    "rotation": [0.0, -90.0, 0.0],
    "scale": [1.0, 1.0, 1.0]
})

# Flattened arrays (Row-major order)
flat_heights = [Z_dem[j][i] for j in range(res) for i in range(res)]
flat_types = [surface_map[j][i] for j in range(res) for i in range(res)]

terrain_data = {
    "meta": {
        "map_name": "TEST-001",
        "version": "1.0.0",
        "cell_size_m": CELL_SIZE,
        "heightmap_resolution_m": RESOLUTION,
        "map_width_m": TOTAL_M,
        "map_depth_m": TOTAL_M,
        "base_elevation_m": 1700.0,
        "min_elevation_m": min(flat_heights),
        "max_elevation_m": max(flat_heights),
        "physics": {
            "gravity": -9.81,
            "default_friction": 0.85,
            "mud_friction": 0.22,
            "asphalt_friction": 0.85,
            "gravel_friction": 0.65,
            "water_friction": 0.05
        }
    },
    "terrain": {
        "width": res,
        "depth": res,
        "resolution_m": RESOLUTION,
        "heights": flat_heights
    },
    "surface_zones": {
        "width": res,
        "depth": res,
        "types": flat_types
    },
    "assets": assets
}

output_path = "native-assets/src/terrain/game_terrain/test_ground_001/jsons/transport_sim_test_001.json"
with open(output_path, "w") as f:
    json.dump(terrain_data, f, indent=2)

print(f"Map exported successfully to {output_path}")
print(f"Grid: {res}x{res} ({len(flat_heights)} elevation points)")
print(f"Elevation range: {min(flat_heights):.2f}m .. {max(flat_heights):.2f}m")
print(f"Assets placed: {len(assets)}")
