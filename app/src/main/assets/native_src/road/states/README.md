# Road Surface States Subsystem

## Overview
The `states/` directory defines 10 procedural surface conditions for roads. Each state modifies vehicle traction physics, acoustic tire emissions, and PBR rendering textures via compute shaders.

```
native-assets/src/road/states/
├── README.md               # States specification and parameter guide
├── muddy/                  # Wet clay/soil with wheel ruts and puddle accumulations
├── rocky/                  # Unpaved bedrock and scattered stone debris
├── bumpy/                  # Washboard ridges and asphalt frost heaves
├── sandy/                  # Loose sand drifts and desert encroachment
├── gravel/                 # Loose crushed gravel with dust emission
├── snowy/                  # Fresh snow cover and packed tire tracks
├── icy/                    # Glazed black ice and low-friction sheen
├── wet/                    # Rain sheen, standing water puddles, and reflection
├── dry_cracked/            # Severe drought and polygonal asphalt fracture networks
└── potholed/               # Structural asphalt failure, excavated craters, broken rims
```

---

## State Architecture

Every state subfolder adheres to a standardized module interface:
- **`*State.h / .cpp`**: State lifecycle class implementing `IRoadState` (updates params, binds descriptors, evaluates blend weights).
- **`*Params.h`**: Physical constants consumed by vehicle simulation, tire dynamics, and audio synthesis:
  - `frictionCoefficient`: Base Coulomb friction ($0.08$ on ice to $0.85$ on dry asphalt)
  - `slipFactor`: Lateral drift multiplier
  - `rollingResistance`: Energy loss due to surface deformation
  - `acousticRumbleHz`: Dominant vibration frequency for vehicle cabin audio
  - `splashEmissionRate`: Particle spawn rate per tire per second
- **`shaders/`**: High-performance compute shaders that generate or blend albedo, normal perturbation, and ORM maps.
- **`textures/seeds/`**: Seed binary files storing deterministic noise offsets.
- **`meshes/profiles/`**: Cross-section deformation curves for vertex displacement along road splines.

---

## State Matrix & Parameter Guide

| Directory | Surface Name | Friction ($\mu$) | Rut / Displacement | Primary Shaders |
| :--- | :--- | :--- | :--- | :--- |
| `muddy/` | Muddy Rutted Road | 0.32 | 0.08 m | `mud_albedo.comp`, `mud_ruts.comp` |
| `rocky/` | Rocky Unpaved Road | 0.58 | 0.06 m | `rock_scatter.comp`, `rock_normal.comp` |
| `bumpy/` | Washboard Road | 0.65 | 0.04 m | `washboard_disp.comp` |
| `sandy/` | Sandy Road | 0.38 | 0.05 m | `sand_drift.comp`, `sand_ripple.comp` |
| `gravel/`| Gravel Road | 0.52 | 0.03 m | `gravel_scatter.comp`, `gravel_normal.comp` |
| `snowy/` | Snowy Road | 0.25 | 0.12 m | `snow_subsurface.comp`, `snow_tracks.comp` |
| `icy/`   | Glazed Icy Road | 0.10 | 0.005 m | `ice_glaze.comp`, `ice_specular.comp` |
| `wet/`   | Wet Rain Road | 0.62 | 0.002 m | `wet_film.comp`, `puddle_accum.comp` |
| `dry_cracked/` | Dry Cracked Road | 0.75 | 0.015 m | `crack_voronoi.comp`, `crack_depth.comp` |
| `potholed/` | Potholed Asphalt | 0.60 | 0.09 m | `pothole_carve.comp`, `crater_edge.comp` |

---

## Integration with Vehicle Dynamics
When a vehicle's tire contacts a road segment, `RoadNetwork` queries the active `RoadState` at the tire's UV coordinates:
1. Multi-state blending applies transitions (e.g. wet transition to muddy or icy).
2. Friction coefficients are passed directly to Pacejka 'Magic Formula' tire models.
3. Normal and height displacement offsets are sampled for chassis suspension excitation.
