import json
import numpy as np
from scipy.ndimage import gaussian_filter

# =========================================================
# 1. TERRAIN GENERATION (SAME MATH AS PREVIOUS MAPS)
# =========================================================
CELL_SIZE = 5.0        # Physics cell
RESOLUTION = 2.0       # High-definition 2m grid for mobile mesh
TOTAL_M = 320.0
ROAD_CENTER = 160.0
HALF_SIDE = 125.0
ROAD_WIDTH = 5.0

# Generate High-Def Grid
res = int(TOTAL_M / RESOLUTION)  # 160
x_dem = np.linspace(0, TOTAL_M, res)
y_dem = np.linspace(0, TOTAL_M, res)
X_dem, Y_dem = np.meshgrid(x_dem, y_dem)
Z_dem = np.full_like(X_dem, 1700.0)

# West Hill
west_x, west_y = 10, 160
dist_west = np.sqrt((X_dem - west_x)**2 + (Y_dem - west_y)**2)
Z_dem += 40 * np.exp(-(dist_west**2) / (2 * 45**2))

# Central Lake
lake_x, lake_y = 160, 160
dist_lake = np.sqrt((X_dem - lake_x)**2 + (Y_dem - lake_y)**2)
Z_dem -= 15 * np.exp(-(dist_lake**2) / (2 * 22**2))

# River Trench
river_mask = np.abs(X_dem - 160) < 5
river_mask &= (Y_dem > 185) & (Y_dem < 320)
Z_dem[river_mask] -= 8.0

# Ditches on East
for dx, dy, depth in [(280, 220, 1.5), (290, 160, 1.0), (280, 100, 0.5)]:
    dist_ditch = np.sqrt((X_dem - dx)**2 + (Y_dem - dy)**2)
    Z_dem -= depth * np.exp(-(dist_ditch**2) / (2 * 8**2))

# Smooth terrain
Z_dem = gaussian_filter(Z_dem, sigma=2)

# =========================================================
# 2. GENERATE SURFACE ZONES (For Texture/Material Mapping)
# =========================================================
# Initialize as Grass
surface_map = np.full((res, res), "grass", dtype=object)

# Lake Zone
lake_radius = 20
lake_dist = np.sqrt((X_dem - lake_x)**2 + (Y_dem - lake_y)**2)
surface_map[lake_dist < lake_radius] = "water"

# River Zone
surface_map[river_mask] = "water"

# South Mud Zone (roughly Y < 45, outside road)
mud_mask = (Y_dem > 25) & (Y_dem < 45) & (X_dem > 50) & (X_dem < 270)
surface_map[mud_mask] = "mud"

# Gravel Ditches (East)
for dx, dy in [(280, 220), (290, 160), (280, 100)]:
    dist_ditch = np.sqrt((X_dem - dx)**2 + (Y_dem - dy)**2)
    surface_map[dist_ditch < 10] = "gravel"

# Road Zone (Asphalt) - using Superellipse math
theta = np.linspace(0, 2 * np.pi, 500)
n = 4
x_rel = HALF_SIDE * np.sign(np.cos(theta)) * np.abs(np.cos(theta))**(2/n)
y_rel = HALF_SIDE * np.sign(np.sin(theta)) * np.abs(np.sin(theta))**(2/n)
road_cx = ROAD_CENTER + x_rel
road_cy = ROAD_CENTER + y_rel

# Mark cells close to the road centerline as asphalt
for i in range(len(road_cx)):
    dist_road = np.sqrt((X_dem - road_cx[i])**2 + (Y_dem - road_cy[i])**2)
    surface_map[dist_road < (ROAD_WIDTH / 2) + 1] = "asphalt"

# =========================================================
# 3. DEFINE 3D GLB ASSET PLACEMENTS
# =========================================================
assets = []
# 1. Garage (SE)
assets.append({
    "id": "garage_001",
    "type": "garage",
    "glb": "assets/structures/garage_hub.glb",
    "position": [300, 300, 1700],
    "rotation": [0, 0, 0],
    "scale": [1.0, 1.0, 1.0]
})

# 2. Bridge (North Leg at river crossing)
assets.append({
    "id": "bridge_001",
    "type": "bridge",
    "glb": "assets/structures/concrete_bridge.glb",
    "position": [160, 285, 1700],
    "rotation": [0, 0, 0],
    "scale": [1.0, 1.0, 1.0]
})

# 3. Trees (Dense Forest on East)
np.random.seed(42) # Deterministic for testing
for i in range(30):
    tx = np.random.uniform(250, 315)
    ty = np.random.uniform(30, 290)
    idx_x = int(tx / RESOLUTION)
    idx_y = int(ty / RESOLUTION)
    tz = Z_dem[idx_y, idx_x]
    assets.append({
        "id": f"tree_00{i}",
        "type": "tree",
        "glb": "assets/nature/palm_tree.glb",
        "position": [tx, ty, tz],
        "rotation": [0, np.random.uniform(0, 360), 0],
        "scale": [1.0, 1.0, 1.0]
    })

# 4. Rocks / Boulders (West Leg)
for i in range(10):
    rx = np.random.uniform(15, 30)
    ry = np.random.uniform(120, 180)
    idx_x = int(rx / RESOLUTION)
    idx_y = int(ry / RESOLUTION)
    rz = Z_dem[idx_y, idx_x]
    assets.append({
        "id": f"rock_00{i}",
        "type": "rock",
        "glb": "assets/nature/boulder_1.5m.glb",
        "position": [rx, ry, rz],
        "rotation": [0, np.random.uniform(0, 360), 0],
        "scale": [1.0, 1.0, 1.0]
    })

# 5. Start Line
assets.append({
    "id": "start_line",
    "type": "spawn",
    "glb": "assets/markers/start_grid.glb",
    "position": [300, 285, 1700],
    "rotation": [0, -90, 0],
    "scale": [1.0, 1.0, 1.0]
})

# =========================================================
# 4. BUILD THE FINAL JSON STRUCTURE
# =========================================================
terrain_data = {
    "meta": {
        "map_name": "TEST-001",
        "version": "1.0.0",
        "cell_size_m": CELL_SIZE,
        "heightmap_resolution_m": RESOLUTION,
        "map_width_m": TOTAL_M,
        "map_depth_m": TOTAL_M,
        "base_elevation_m": 1700.0,
        "physics": {
            "gravity": -9.81,
            "default_friction": 0.85,
            "mud_friction": 0.22,
            "asphalt_friction": 0.85
        }
    },
    "terrain": {
        "width": res,
        "depth": res,
        # Flattened list of heights (Row-major order: for each Y, list X)
        "heights": Z_dem.flatten().tolist()
    },
    "surface_zones": {
        "width": res,
        "depth": res,
        # Flattened list of surface types (Grass, Asphalt, Water, Mud, Gravel)
        "types": surface_map.flatten().tolist()
    },
    "assets": assets
}

# Write to file
filename = "native-assets/src/terrain/game_terrain/test_ground_001/jsons/transport_sim_test_001.json"
with open(filename, 'w') as f:
    json.dump(terrain_data, f, indent=2)

print(f"Map exported successfully to {filename}")
print(f"High-Def Grid: {res}x{res} points (Resolution: {RESOLUTION}m)")
print(f"Assets placed: {len(assets)}")
