# Procedural Road Generation Subsystem

## Overview
The **Road Subsystem** (`native-assets/src/road/`) provides an end-to-end procedural infrastructure generator for Vulkan-based simulation and rendering. It produces dynamic, mathematically continuous road networks conforming smoothly to terrain heights, streaming through quadtree LOD tiles, and reacting dynamically to 10 distinct environmental surface states.

---

## Architecture & Core Modules

```
native-assets/src/road/
├── CMakeLists.txt              # Builds libroad.a + compiles compute/graphics SPIR-V shaders
├── README.md                   # Subsystem architecture & technical reference
├── RoadNetwork.h / .cpp        # High-level road graph (junction nodes, directed edges, lanes)
├── RoadSplineGraph.h / .cpp    # Hermite/Catmull-Rom spline evaluator with banking & cross-section camber
├── RoadTileGen.h / .cpp        # Streaming-tile quadtree LOD mesh and texture generator
```

### 1. `RoadNetwork`
Manages the topological graph of the road network:
- **Graph Nodes**: Intersections, roundabouts, dead-ends, highway interchanges.
- **Edges**: Directed road segments containing 1–8 lanes, median barriers, curbs, and shoulders.
- **Lane Topology**: Waypoints, connectivity graph for traffic AI, and lane-change corridors.

### 2. `RoadSplineGraph`
Provides $C^2$-continuous mathematical curves along road edges:
- **Spline Curves**: Cubic Hermite & Centripetal Catmull-Rom formulations preventing loops and oscillations.
- **Cross-Section Profile**: Evaluates crown/camber height offset, shoulder slope, and superelevation (banking angle) proportional to curvature radius and target vehicle design speed:
  $$\theta_{\text{bank}} = \arctan\left(\frac{v^2}{g \cdot R}\right)$$
- **Frenet Frame Alignment**: Orthonormal tangent, normal, and binormal frame evaluation for artifact-free vertex extrusion.

### 3. `RoadTileGen`
Streaming chunk generator integrating road geometry with world terrain:
- Generates clipmaps/quadtree tiles conforming road foundations to elevation grids.
- Computes road cuts, hillside embankments, retaining walls, and drainage ditches.
- Emits bounding volumes and occlusion queries for distance LOD culling.

---

## Sub-Component Breakdown

| Sub-Module | Responsibility | Shaders & Assets |
| :--- | :--- | :--- |
| **`asphalt/`** | Micro-surface procedural asphalt generation, bitumen aging, aggregate chip distribution. | `asphalt_albedo.comp`, `asphalt_normal.comp`, `asphalt_roughness.comp` |
| **`markings/`** | SDF-based procedural paint lines (dashed centerlines, solid edge lines, chevron medians, crosswalks, turn arrows). | `road_markings.comp`, SDF distance transforms |
| **`signs/`** | Procedural street signage, regulatory/warning signs, reflective retroreflective sheeting, mounting posts. | SDF glyph fonts, sign border shaders |
| **`barriers/`** | W-beam guardrails, concrete New Jersey barriers, cable barriers with procedural posts and end terminals. | Guardrail extrusion profiles, metal/concrete PBR shaders |
| **`intersections/`**| Procedural junction mesh stitching, curb radius rounding, traffic island islands, pedestrian crosswalk logic. | Polygon boolean clipper, fillet curves |
| **`bridges/`** | Overpasses, concrete beam viaducts, steel truss spans, suspension cables, and support piers down to ground. | Structural beam profiles, pier generators |
| **`tunnels/`** | Terrain subterranean bore cutouts, reinforced concrete liners, arched ceiling portals, and lighting strips. | Bore tunnel extrusion, portal arch profiles |
| **`tiles/`** | Grid-based streaming LOD tiles with seamless stitch seams to adjacent chunks. | Quadtree mesh builders, clipmap blend shaders |

---

## Environmental Road Surface States (`states/`)

The road surface state engine alters physical grip, tire-surface acoustic spectra, and visual PBR material properties across 10 environmental conditions:

| State | Environmental Condition | Key Physical Parameters | Visual & Compute Characteristics |
| :--- | :--- | :--- | :--- |
| **`muddy/`** | Heavy wet clay, mud ruts, saturated soil shoulders | `SlipFactor: 0.35`, `RutDepth: 0.08m`, `MudViscosity: 0.72` | `mud_albedo.comp`, `mud_ruts.comp`, puddles in wheel tracks |
| **`rocky/`** | Unpaved gravel with loose boulders and rock debris | `Roughness: 0.85`, `RollingResistance: 0.45`, `RockDensity: 12/m²` | Procedural Voronoi rock scatter, angular normal perturbations |
| **`bumpy/`** | Washboard corrugations, frost heaves, asphalt buckling | `Amplitude: 0.04m`, `Wavelength: 0.6m`, `VibrationHz: 28` | Harmonic sine/Perlin washboard displacement along spline tangent |
| **`sandy/`** | Loose desert sand drift across tarmac, dunes | `Friction: 0.40`, `DriftDepth: 0.05m`, `GrainRoughness: 0.9` | Wind-aligned ripple displacement, granular micro-specular |
| **`gravel/`** | Crushed stone unpaved road with loose top layer | `TireGrip: 0.55`, `ChippingResistance: 0.6`, `PebbleScale: 0.02m` | Cellular gravel noise, aggregate scattering, dust particulate emitters |
| **`snowy/`** | Fresh snowfall, packed snow base, plow windrows | `Friction: 0.28`, `PowderDepth: 0.12m`, `TrackPack: 0.65` | Subsurface snow scattering, wheel impression channels |
| **`icy/`** | Black ice glaze, hard-packed frozen ice sheets | `Friction: 0.10`, `IceThickness: 0.005m`, `SpecularPower: 250` | Glassy specular reflection, subtle hairline thermal cracks |
| **`wet/`** | Post-rain wet asphalt, saturated sheen, standing puddles | `Friction: 0.65`, `PuddleFill: 0.8`, `MirrorReflection: 0.75` | Planar reflection attenuation, rain ripple normal perturbation |
| **`dry_cracked/`**| Severe drought, sun-baked brittle bitumen, alligator cracking | `Friction: 0.78`, `CrackDepth: 0.015m`, `JointSpacing: 0.4m` | Poisson-disk Voronoi cell fracture network, crumbling edges |
| **`potholed/`** | Severe pavement degradation, asphalt cratering, exposed aggregate | `ImpactSeverity: 0.82`, `RimDepth: 0.09m`, `DamageRadius: 0.45m` | Procedural crater excavation, asphalt spalling, water ponding in craters |

---

## Telemetry & Runtime Pipeline

1. **Spline Evaluation**: Road curves sampled at runtime based on viewer camera velocity and distance.
2. **Compute Dispatch**: Surface state compute shaders update albedo, normal, and ORM maps into bindless GPU descriptor sets.
3. **Terrain Conforming**: Vertex shaders read terrain heightmaps and conform road shoulder margins with smooth Hermite blends.
4. **Physics Feedback**: Vehicle dynamics query `RoadStateParams` at tire contact patches for grip, slip angle, and hydroplaning risks.
5. **Acoustic Profiling**: Surface roughness and slip factor drive synthesized tire roar, road rumble, and splash audio oscillators.
