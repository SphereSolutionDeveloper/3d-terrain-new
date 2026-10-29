# Procedural Terrain Engine (Vulkan & Pure C++)

A high-performance procedural terrain generation subsystem implemented in modern C++20 and Vulkan GLSL compute shaders. Completely procedural geometry and texture synthesis: **no external 3D meshes or .glb assets are required**.

## Architectural Features
- **Heightmap Generation**: Multi-octave Fractal Brownian Motion (FBM), Simplex/Perlin gradient noise, domain warping, ridged multi-fractal mountains, and hydraulic/thermal particle erosion.
- **Procedural Splatmaps**: Dynamic texture layer synthesis blending 4 distinct biomes (Grass/Soil, Dirt/Gravel, Steep Rock Cliff, Alpine Snow) computed on-the-fly based on surface normals, elevation thresholds, and turbulence noise.
- **Cliff Synthesis**: Procedural strata layering, overhang extrusion, voronoi joint cracking, and stepped talus slopes.
- **Road Embankments**: Procedural cut-and-fill profile carving along spline curves with retaining walls and shoulder transitions.
- **Volumetric Caves**: 3D cellular noise density fields extracted into watertight triangular meshes via the **Marching Cubes** algorithm.
- **LOD & Streaming Tile Hierarchy**: Continuous Distance-dependent Level of Detail (CDLOD) with seamless edge morphing to eliminate T-junction cracking and mesh popping.

## Directory Structure
- `heightmaps/`: FBM noise, ridged multifractal, domain warping, particle-based hydraulic erosion, compute shaders.
- `splatmaps/`: Height and slope splatting rules, compute shaders, texture specs.
- `cliffs/`: Rock strata generation, overhang extrusion, cliff profiles.
- `roads_cut/`: Embankment slicing, cut-and-fill cross-sections, road shoulder mesh specs.
- `caves/`: Volumetric isosurface extraction via Marching Cubes, 3D voxel density shaders.
- `meta/`: Quadtree tile spatial partitioning, CDLOD distance calculation, frustum culling.
- `shaders/`: Vulkan compute shaders (`.comp`) and shared GLSL include headers.
- `textures/`: Procedural noise seed tables and texture format specifications.
- `meshes/`: Geometry buffer format specifications and LOD tier tables.
