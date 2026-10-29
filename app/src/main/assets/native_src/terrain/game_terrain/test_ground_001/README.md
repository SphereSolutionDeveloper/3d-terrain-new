# Test Ground 001 - Game Terrain System

High-Definition Procedural Game Terrain with Road Contours, Physical Zones, and 3D Asset Placements.

## Directory Structure

```
native-assets/src/terrain/game_terrain/test_ground_001/
├── dev_scripts/
│   ├── generate_terrain_json.py             # User generator script (NumPy/SciPy)
│   ├── generate_terrain_json_standalone.py  # Pure Python generator (no dependencies)
│   └── export_binary_terrain.py             # High-speed binary format exporter (.bin / .tg1)
├── jsons/
│   ├── transport_sim_test_001.json          # 160x160 HD terrain (25,600 points, zones, assets)
│   └── transport_sim_test_001.bin           # Zero-copy binary cache (125 KB, 84% compression)
├── cpp/
│   ├── GameTerrainLoader.h                  # C++17/20 Game Engine Terrain Parser & Raycaster
│   ├── GameTerrainLoader.cpp                # JSON & Binary loaders, Mesh generator, Bilinear physics
│   ├── GameTerrainC_API.h                   # C API Header for Unity C#, Unreal, Godot
│   ├── GameTerrainC_API.cpp                 # C ABI implementation
│   ├── main_converter.cpp                   # Standalone CLI validation and converter
│   └── CMakeLists.txt                       # Build targets (Shared & Static libs)
└── README.md                                # Integration guide & documentation
```

## Terrain Specifications

| Property | Value | Description |
| :--- | :--- | :--- |
| **Map Name** | `TEST-001` | Transport simulation test ground 001 |
| **Grid Resolution** | `2.0m` | High-definition mobile mesh spacing |
| **Grid Dimensions** | `160 × 160` | 25,600 elevation points & surface zones |
| **World Dimensions** | `320.0m × 320.0m` | Total terrain footprint |
| **Base Elevation** | `1700.0m` | Baseline datum |
| **Elevation Range** | `1685.60m .. 1739.72m` | Central lake depth to West hill peak |
| **Physics Cell Size**| `5.0m` | Physics collision evaluation cell |

## Surface Zones & Friction

- **Asphalt** (`0.85` friction): Superellipse road corridor with 5.0m width.
- **Mud** (`0.22` friction): South wet mud hazard zone (`25m < Y < 45m`).
- **Gravel** (`0.65` friction): East ditches (`dx=280..290m`).
- **Water** (`0.05` friction): Central lake (`R=20m`) and river trench (`Y > 185m`).
- **Grass** (`0.85` friction): Surrounding natural terrain.

## 3D Asset Placements (43 Assets)

- `garage_001` (`garage`): `[300m, 300m, 1700m]` -> `assets/structures/garage_hub.glb`
- `bridge_001` (`bridge`): `[160m, 285m, 1700m]` -> `assets/structures/concrete_bridge.glb`
- `start_line` (`spawn`): `[300m, 285m, 1700m]` -> `assets/markers/start_grid.glb`
- 30 `tree` assets placed in dense East forest (`X: 250..315m`, `Y: 30..290m`)
- 10 `rock` boulders placed along West hill (`X: 15..30m`, `Y: 120..180m`)

## C++ Integration Example

```cpp
#include "GameTerrainLoader.h"

GameEngine::Terrain::GameTerrain terrain;

// Load JSON or binary cache:
terrain.LoadFromJSON("transport_sim_test_001.json");
// Or 84% smaller binary cache:
// terrain.LoadFromBinary("transport_sim_test_001.bin");

// Query continuous elevation & friction for tire physics:
float z = terrain.GetElevationAt(160.0f, 160.0f);
float friction = terrain.GetFrictionAt(160.0f, 160.0f);
Vec3 normal = terrain.GetNormalAt(160.0f, 160.0f);

// Raycast tire contact:
Vec3 hitPt, hitNorm;
bool hit = terrain.Raycast(Vec3(300, 1750, 300), Vec3(0, -1, 0), 100.0f, hitPt, hitNorm);

// Generate GPU mesh buffers:
std::vector<TerrainVertex> vertices;
std::vector<uint32_t> indices;
terrain.GenerateMeshData(vertices, indices, /*stepLOD=*/1);
```

## Unity C# P/Invoke Bindings

```csharp
[DllImport("GameTerrainShared")]
public static extern IntPtr GameTerrain_Create();

[DllImport("GameTerrainShared")]
public static extern bool GameTerrain_LoadJSON(IntPtr handle, string filepath);

[DllImport("GameTerrainShared")]
public static extern float GameTerrain_GetElevation(IntPtr handle, float x, float z);

[DllImport("GameTerrainShared")]
public static extern float GameTerrain_GetFriction(IntPtr handle, float x, float z);
```
