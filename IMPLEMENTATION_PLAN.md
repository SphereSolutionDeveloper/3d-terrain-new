# IMPLEMENTATION PLAN: C++ Procedural Asset Screen (Garage 001 & Test Vehicle)

## 1. Overview & Objective
Add screen navigation buttons to switch between the current screens and a brand-new **"C++ PROCEDURAL"** screen.
The application currently has:
1. `3D SCENE (10×10m)` - Scene Landscape & Grid simulation
2. `GLB / ASSET INSPECTOR` - Single Asset Inspector (Wireframe, GLB Shaded, Erosion, etc.)

We are adding a top-tier navigation button and dedicated screen mode:
3. **`C++ PROCEDURAL`** - Dedicated interactive viewport and inspector for the two mathematical procedural C++ assets specified in the technical reference:
   - **Asset 1: Proving Ground Garage 001 (SE Depot)**:
     - 14m x 6m x 22m main warehouse (`#384252` Steel Slate Gray)
     - 15m x 0.4m x 23m crimson red roof trim (`#D93838`)
     - Dual 5m x 3.6m service bay rollup doors with interactive open/close percentage sliders (`#BFC7D1`)
     - 10m x 1m workshop banner with neon glow toggle (`#00BFE0`)
     - 28m x 24m paved service apron (`#333840` / `#2C3238`)
     - Twin 14m x 0.22m yellow bay guidelines (`#FFD54F`)
     - 24m x 14m connecting driveway (`#262C33`)
     - Real-time procedural box extrusion engine (36 vertices per box, outward normals, vertex colors)
   - **Asset 2: Proving Ground Test Vehicle (Box Lorry)**:
     - 5.8m x 0.4m x 2.2m heavy chassis frame (`#1F242E`)
     - 4 Heavy-duty wheels (0.9m dia, 0.45m width, matte black `#141414`)
     - Driver's cab (1.8m x 1.8m x 2.1m, electric cyan `#00BCD4` / `#00B0FF`)
     - Dark tint windshield & side windows (`#1F2E47`)
     - Freight cargo box (3.8m x 2.4m x 2.3m, clean silver-white `#EBF0F5`)
     - Flanking cyan livery racing stripes
     - Amber front headlights (`#FFE033`) and crimson rear taillights (`#E61A1A`)
     - Interactive headlights & taillights toggle, wheels rotation, steer angle control

Both assets are rendered through:
1. **Interactive Real-time 3D Viewport**: Orbit yaw/pitch, zoom, pan, lighting angle, wireframe overlay, ground grid, and exploded/disassembly view.
2. **Native C++ Engine Source Code & Specs Viewer**: Complete procedural generation source code (`Renderer.h`, `Renderer.cpp`, `addBox()`, vertex structures, shaders, and GLB export tools) side-by-side with telemetry tabs (Specs, Physics & Dimensions, Shader & Materials, C++ Source Code, GLB Binary Exporter).

---

## 2. Directory & File Structure Plan

```
/docs/
  └── proving_ground_garage_and_vehicle_reference.md   <-- [SAVED] Technical reference guide
/cpp/
  ├── CMakeLists.txt                                   <-- Update build target
  ├── README.md
  ├── include/
  │   ├── noise.hpp
  │   ├── rock_renderer.hpp
  │   ├── tree_generator.hpp
  │   ├── procedural_garage.hpp                        <-- C++ procedural garage header & definitions
  │   └── procedural_vehicle.hpp                       <-- C++ procedural vehicle header & definitions
  └── src/
      ├── rock_renderer.cpp
      ├── procedural_garage.cpp                        <-- C++ implementation with addBox logic
      └── procedural_vehicle.cpp                       <-- C++ implementation of box lorry
/app/src/main/java/com/example/
  ├── model/
  │   ├── AssetModels.kt                               <-- Add MainAppView.CPP_PROCEDURAL_VIEW, CppAssetType enum, ProceduralMeshModel
  │   └── ProceduralCppModels.kt                       <-- Data classes for Garage 001 and Test Vehicle specs & telemetry
  ├── engine/
  │   └── Procedural3DBuilder.kt                       <-- Procedural box extrusion generator mirroring C++ addBox algorithm
  ├── viewmodel/
  │   └── AssetStudioViewModel.kt                      <-- State for C++ view, selected C++ asset, door sliders, lights, orbit camera
  ├── ui/
  │   ├── AssetStudioScreen.kt                         <-- Updated MainAppNavBar with C++ PROCEDURAL button, navigation routing
  │   └── components/
  │       ├── CppProceduralScreen.kt                   <-- The new full screen view matching GLB screen layout and aesthetics
  │       ├── CppViewport3DCanvas.kt                   <-- Dedicated 3D Canvas rendering procedural C++ garage & vehicle meshes
  │       └── CppSpecsInspectorView.kt                 <-- Technical specs, dimensions, materials, and C++ source code viewer
/IMPLEMENTATION_PLAN.md                                <-- Root implementation plan
```

---

## 3. Step-by-Step Execution Plan

### Step 1: Model & Enum Extensions
- Update `MainAppView` enum:
  - `SCENE_3D_VIEW("3D Scene (10x10)", "landscape")`
  - `SINGLE_ASSET_INSPECTOR("GLB / Asset Inspector", "view_in_ar")`
  - `CPP_PROCEDURAL_VIEW("C++ Procedural", "code")`
- Define `CppAssetType` (`GARAGE_001`, `TEST_VEHICLE_LORRY`) with metadata, dimensions, bounding boxes, and architectural specs.
- Define `ProceduralVertex` and `ProceduralBox` data models matching C++ `Vertex3D`.

### Step 2: Procedural Math Engine (`Procedural3DBuilder.kt`)
- Implement the exact mathematical `addBox()` extrusion logic from Section 3.2 of the technical reference.
- Implement `buildGarage001(bay1OpenPct, bay2OpenPct, neonSign)`:
  - Warehouse body, crimson roof trim, rollup doors, workshop banner, paved apron, yellow bay lines, driveway.
- Implement `buildTestVehicle(headlightsOn, wheelsSteerAngle)`:
  - Chassis frame, 4 wheels, cyan cab, dark windows, silver cargo box, livery stripes, headlights, taillights.
- Calculate outward surface normals, vertex colors, face quads, and indexed triangles.

### Step 3: C++ Header and Implementation Files in `/cpp/`
- Add `/cpp/include/procedural_garage.hpp` and `/cpp/include/procedural_vehicle.hpp`.
- Add `/cpp/src/procedural_garage.cpp` and `/cpp/src/procedural_vehicle.cpp` containing the native C++ implementations.
- Update `/cpp/CMakeLists.txt` to include the new procedural assets.

### Step 4: ViewModel Enhancements (`AssetStudioViewModel.kt`)
- Add state holders:
  - `selectedCppAsset`: StateFlow of `CppAssetType` (Garage vs Test Vehicle)
  - `cppBay1OpenPct`: Float (0.0f to 1.0f)
  - `cppBay2OpenPct`: Float (0.0f to 1.0f)
  - `cppNeonSign`: Boolean
  - `cppHeadlights`: Boolean
  - `cppSteerAngle`: Float
  - `cppShadingMode`: String ("Lit Shaded", "Wireframe Over Solid", "Pure Wireframe", "Part Disassembly")
  - `cppExplodeFactor`: Float (0.0f to 1.0f)
  - Camera orbit controls for C++ view (yaw, pitch, zoom, panX, panY, auto-rotate)
  - Active telemetry tabs ("Specs & Blueprint", "C++ Source Code", "Materials & Lighting", "GLB Exporter Script", "Dimensions & Physics")

### Step 5: Compose UI Components
- **`CppViewport3DCanvas.kt`**: High performance 3D projection canvas with directional lighting, specular highlights, box projection, wireframe overlays, exploded component view, ground grid, and HUD diagnostics.
- **`CppSpecsInspectorView.kt`**: Tabbed technical inspector with code viewer, syntax highlighting styling, door/light interactive control deck, copy/inspect buttons, and physical properties.
- **`CppProceduralScreen.kt`**: The full screen assembling the asset selector (Garage vs Vehicle), 3D viewport with camera controls, and lower inspection tabs, maintaining exact visual parity with the GLB screen.
- **`AssetStudioScreen.kt`**: Add navigation button to `MainAppNavBar`, add back-navigation handler, and route between `SCENE_3D_VIEW`, `SINGLE_ASSET_INSPECTOR`, and `CPP_PROCEDURAL_VIEW`.

### Step 6: Compilation & Verification
- Run `compile_applet` to verify zero compile or type errors.
- Test camera interactions, switching between assets, and interactive controls (bay doors, vehicle headlights).

---

## 4. View Angle & Perspective Foreshortening Fix (Bugfix Summary)

- **Issue Reported**: The view angle in the C++ scene rendered assets small at the front viewpoint and enlarged at the end (the reverse of natural perspective foreshortening).
- **Root Cause**:
  1. The projection engine in `CppViewport3DCanvas.kt` was previously using a purely orthographic projection that rotated around raw world coordinates without model centering or depth distance scaling.
  2. In the absence of perspective foreshortening, physical dimensional differences (such as the 2.4m cargo box behind the 1.8m cab on the test vehicle, or the 22m warehouse behind the flat entrance apron on Garage 001) caused background elements to appear disproportionately enlarged and foreground elements to look undersized.
  3. The camera presets ("Front", "Side") were reversed relative to the asset orientation vectors (+X cab for vehicle, -X service bays for garage).
- **Resolution Implemented**:
  1. **Perspective Projection Engine**: Upgraded `project()` in `CppViewport3DCanvas.kt` to calculate true perspective foreshortening:
     $$\text{perspectiveFactor} = \frac{\text{cameraDistance}}{\text{cameraDistance} - z_{\text{view}}} \cdot \frac{1}{\tan(\text{FOV} / 2)}$$
     Vertices in the foreground ($z_{\text{view}} > 0$, closer to camera) receive a factor $> 1.0$ (enlarged at front viewpoint), while background vertices ($z_{\text{view}} < 0$, further from camera) receive a factor $< 1.0$ (tapering naturally into distance).
  2. **Geometric Centering**: Transformed all vertices relative to the model's bounding box center `(centerModelX, centerModelY, centerModelZ)` before rotation so that both the 65m garage complex and the 6.5m test vehicle pivot naturally.
  3. **Asset-Aware Camera Presets**: Updated `resetCppCamera` in `AssetStudioViewModel.kt` so that "Front" looks directly at the front of each asset (the cab and headlights at $+X$ for the vehicle; the service rollup doors and neon sign at $-X$ for the garage).
  4. **Dynamic HUD & Controls**: Added a `PERSP` / `ORTHO` toggle in the viewport HUD and a View Angle / Field of View (FOV) slider (30° to 90°) in the Interactive Controls tab.
