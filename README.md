# Apex 3D Studio — Vulkan 1.3 Procedural Engine & Asset Suite

Apex 3D Studio is a high-performance procedural 3D modeler and architectural visualization engine powered by modern **C++20** and **Vulkan 1.3** graphics pipelines (following Sascha Willems' established architecture patterns).

---

## 1. Vulkan 1.3 Graphics Engine Architecture

The native rendering engine (`vulkan_engine.cpp`, `vulkan_renderer.cpp`, `vulkan_asset_models.cpp`) targets Vulkan 1.3 specifications:

- **Dynamic Rendering (`VK_KHR_dynamic_rendering`)**:
  - Replaces legacy `VkRenderPass` and `VkFramebuffer` constructs with streamlined direct command buffer recording via `vkCmdBeginRendering` and `vkCmdEndRendering`.
  - Simplifies PSO configuration and enables single-pass rendering to color and depth attachments directly.
- **SPIR-V 1.5 Bytecode Pipeline**:
  - Vertex shader (`shaders/grid.vert`) and fragment shader (`shaders/grid.frag`) compiled using Khronos `glslc`.
  - Unified vertex format (`custom_vulkan::EngineVertex`):
    - Position `pos[3]` (float32)
    - Face normal `normal[3]` (unit normal for Lambertian diffuse + specular rim)
    - Texture coordinates `uv[2]`
    - Color `color[4]` (RGBA float32)
- **Vulkan Memory Allocator (VMA) Integration**:
  - Vertex and index buffers allocated in dedicated device-local memory (`VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT`).
  - Staging buffers transfer procedurally synthesized geometry through high-bandwidth DMA copies.
- **Push Constants & Uniform Buffers**:
  - Fast 128-byte push constants dispatch per-draw interactive states (e.g., catenary wire sag factors, door opening percentages, sun azimuth angle, camera projection matrices).
  - Binding 0 Uniform Buffer Object (UBO) encapsulates global camera matrices (View, Projection, Model) and multi-point lighting parameters.
- **Rasterization & Antialiasing (MSAA)**:
  - Support for `VK_POLYGON_MODE_FILL` (solid geometry) and `VK_POLYGON_MODE_LINE` (wireframe overlay).
  - Subpixel Multi-Sample Anti-Aliasing (`VK_SAMPLE_COUNT_1_BIT`, `VK_SAMPLE_COUNT_4_BIT`, `VK_SAMPLE_COUNT_8_BIT`).
- **Validation Layers**:
  - Comprehensive debugging via Khronos Validation Layers (`VK_LAYER_KHRONOS_validation`).

---

## 2. Procedural Assets Specification

### A. Garage 001 (Modular Depot with Interior)
A standalone, fully enclosed architectural garage building featuring:
- **Structural Envelope**:
  - 14.0m width × 6.0m height × 22.0m depth.
  - Steel slate gray ribbed siding panels and signature crimson roof fascia trim (`#D32F2F`).
  - Heavy-duty reinforced concrete foundation slab floor (`#37474F`).
- **Modular Rollup Service Doors**:
  - Bay 1 (West wall, North portal) and Bay 2 (West wall, South portal).
  - Interactive opening ratio (0% closed to 100% fully opened).
  - Multi-slat segmented panels with overhead rollup drum assembly and perimeter guide tracks.
- **Detailed Workshop Interior**:
  - Fully modeled interior space visible through opened service bay doors.
  - Reinforced concrete floor slab with yellow/black hazard perimeter striping.
  - Ceiling steel open-web trusses and structural rafters supporting overhead lighting troffers.
  - Workshop workbench station, heavy steel tool cabinet chest with drawer pulls, wall-mounted tiered tire storage racks, and vehicle hydraulic service lift posts.
- **Exterior Features**:
  - Paved asphalt apron (28.0m × 24.0m) with concrete curb boundaries.
  - Cyan emissive neon workshop banner sign (`#00BFE0`) mounted on the exterior facade.

### B. Modular Electric Utility Post
Engineering-grade utility pole for medium-voltage distribution:
- **Mast & Foundation**:
  - Hexagonal concrete foundation pier with anchor bolts.
  - Pressure-treated timber pole mast (11.0m total height) with tapered sectional girth.
- **High-Voltage 3-Phase Crossarm**:
  - Structural galvanized steel crossarm at 9.8m elevation.
  - Glazed brown porcelain pin insulators supporting 3-phase conductors.
  - Top pole-pin insulator carrying the center primary phase conductor.
- **Transformer & Equipment**:
  - 50 kVA cylindrical oil-filled step-down transformer canister with heat-dissipating cooling fins, primary HV bushings, and secondary LV terminal spools.
  - Curved cantilever galvanized pipe arm holding a cobra-head LED streetlight luminaire.
  - High-visibility yellow safety guard sleeve (`#FFEB3B`) and steel ground anchor guy wire stay.

### C. Power Grid Network & Catenary Cable Physics
Multi-span electrical transmission corridor:
- **Corridor Configuration**:
  - Configurable 3 or 4 connected utility posts with an authentic strain corner post turning at 45°–120°.
  - Automated guy wire counterbalancing along the corner post's interior angle bisector.
- **Catenary Sag Physics**:
  - Exact catenary hyperbolic curve:
    $$y(x) = a \cdot \left(\cosh\left(\frac{x}{a}\right) - 1\right)$$
    where parameter $a = \frac{T_0}{\mu \cdot g}$ is determined by conductor horizontal tension $T_0$, linear mass density $\mu$, and gravitational acceleration $g$.
- **Continuous 3D Cylindrical Swept Tubes**:
  - Conductors modeled as continuous 3D swept cylindrical tube meshes with configurable radial facets (6, 8, 12 sides) and conductor radius (10mm–60mm).
  - Metallic finish: Warm amber/copper electrical conductors (`#D99B52`, `#E8AB66`) with metallic sheen, and galvanized steel guy wires (`#90A4AE`, `#78909C`).

### D. Test Vehicle (Heavy Box Lorry)
Standard vehicle reference model:
- Heavy-duty chassis frame with front and rear dual-wheel axle assemblies.
- Aerodynamic cab with panoramic windshield.
- Enclosed cargo box container.
- Working front amber headlights (`#FFE033`) and rear crimson taillights (`#E61A1A`).

---

## 3. GLB 2.0 Exporter Pipeline
Includes Python script generators (`export_garage_glb.py`, `export_modular_post_glb.py`, `export_power_grid_glb.py`, `generate_vehicle_glbs.py`) that synthesize the exact procedural box and cylinder primitives into standard Khronos `.glb` (glTF 2.0 Binary) files for import into Blender, Maya, Unreal Engine, or Godot.
