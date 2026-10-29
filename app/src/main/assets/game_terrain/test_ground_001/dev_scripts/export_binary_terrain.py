#!/usr/bin/env python3
"""
Exports transport_sim_test_001.json into high-speed binary format (.bin / .tg1)
matching GameEngine::Terrain::BinaryHeader in GameTerrainLoader.cpp.
Allows instant memory-mapped loading in C/C++ mobile game engines without JSON parsing.
"""

import json
import struct
import os

JSON_PATH = "native-assets/src/terrain/game_terrain/test_ground_001/jsons/transport_sim_test_001.json"
BIN_PATH = "native-assets/src/terrain/game_terrain/test_ground_001/jsons/transport_sim_test_001.bin"

SURFACE_MAP = {
    "grass": 0,
    "asphalt": 1,
    "water": 2,
    "mud": 3,
    "gravel": 4,
    "rock": 5
}

def convert():
    if not os.path.exists(JSON_PATH):
        print(f"Error: {JSON_PATH} not found!")
        return

    with open(JSON_PATH, "r") as f:
        data = json.load(f)

    meta = data["meta"]
    terrain = data["terrain"]
    zones = data["surface_zones"]
    assets = data["assets"]

    width = int(terrain["width"])
    depth = int(terrain["depth"])
    res_m = float(meta["heightmap_resolution_m"])
    map_w = float(meta["map_width_m"])
    map_d = float(meta["map_depth_m"])
    base_elev = float(meta["base_elevation_m"])
    asset_count = len(assets)

    # Binary Header:
    # char magic[4] = "TG01"
    # uint32 version = 1
    # uint32 width
    # uint32 depth
    # float resolutionM
    # float mapWidthM
    # float mapDepthM
    # float baseElevationM
    # uint32 assetCount
    header_bytes = struct.pack(
        "<4sIIIf f f f I",
        b"TG01",
        1,
        width,
        depth,
        res_m,
        map_w,
        map_d,
        base_elev,
        asset_count
    )

    heights = terrain["heights"]
    height_bytes = struct.pack(f"<{len(heights)}f", *heights)

    types = [SURFACE_MAP.get(t, 0) for t in zones["types"]]
    zone_bytes = struct.pack(f"<{len(types)}B", *types)

    with open(BIN_PATH, "wb") as f:
        f.write(header_bytes)
        f.write(height_bytes)
        f.write(zone_bytes)

    json_size = os.path.getsize(JSON_PATH)
    bin_size = os.path.getsize(BIN_PATH)
    print(f"Binary export complete: {BIN_PATH}")
    print(f"JSON size: {json_size / 1024:.1f} KB -> Binary size: {bin_size / 1024:.1f} KB ({(1 - bin_size/json_size)*100:.1f}% reduction)")
    print(f"Grid: {width}x{depth} ({len(heights)} floats, {len(types)} uint8 zones)")

if __name__ == "__main__":
    convert()
