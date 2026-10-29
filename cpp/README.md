# Apex 3D C++ Procedural Rock Renderer

## Overview
High-performance C++20 rendering module for procedural rock generation and 10x10 grid instancing (each cell 1.0m x 1.0m).

### Features
1. **Procedural Noise Engine (`include/noise.hpp`)**:
   - Ken Perlin Improved 3D Noise with quintic smoothstep fade curve
   - Multi-octave Fractal Brownian Motion (FBM)
   - Worley / Voronoi Cellular Noise for planar cleavage facets & crystals
2. **10x10 Grid Simulation (`include/rock_renderer.hpp`)**:
   - 100 sectors spanning 10.0m x 10.0m (each cell strictly 1.0m x 1.0m)
   - Procedural heightfield and geological rock archetype assignment
   - Physical volume and mass calculation using mineral density
3. **PBR Cook-Torrance Microfacet Shading**:
   - Specular GGX and Burley diffuse evaluation
   - Emissive crystal vein radiance

### Build
```bash
cmake -B build
cmake --build build
```
