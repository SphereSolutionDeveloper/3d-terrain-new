# Full Procedural Asset Tree — With Descriptions, Road States, Energy/Utility Props

Every folder carries an inline # description comment. Added: road states (muddy/rocky/bumpy/sandy/gravel/snowy/icy/wet/dry-cracked/potholed) and new props (electric post, wind turbine, solar panels, transformer, etc.). No truncation.

```
native-assets/                                           # Root of the procedural generator workspace
├── CMakeLists.txt                                       # Top-level build file: adds base/, src/, shaders/, tests/
├── main.cpp                                             # Entry point: creates Application, registers generators, runs main loop
├── README.md                                            # Project overview, build instructions, architecture summary
├── .clang-format                                        # C++ formatting rules for the whole tree
├── .gitignore                                           # Ignores build/, *.spv, *.o, cache artifacts
│
├── base/                                                # Shared Vulkan infrastructure ported from SaschaWillems/Vulkan
│   ├── CMakeLists.txt                                   # Builds libbase.a
│   ├── README.md                                        # Explains base vs src/vulkan split
│   ├── Entrypoints.h                                    # Vulkan function pointer loader (vkGetInstanceProcAddr wrappers)
│   ├── VulkanBuffer.hpp                                 # GPU buffer creation helpers (device-local, host-visible, staging)
│   ├── VulkanBuffer.cpp                                 # VulkanBuffer implementation
│   ├── VulkanDevice.hpp                                 # Device/queue/physical device wrapper
│   ├── VulkanDevice.cpp                                 # VulkanDevice implementation
│   ├── VulkanTexture.hpp                                # VkImage creation, layout transitions, mipmap generation
│   ├── VulkanTexture.cpp                                # VulkanTexture implementation
│   ├── VulkanTools.hpp                                  # Debug markers, error strings, helper utilities
│   ├── VulkanTools.cpp                                  # VulkanTools implementation
│   ├── VulkanInitializers.hpp                           # Struct-initializer helpers (SaschaWillems style)
│   ├── VulkanFrameBuffer.hpp                            # Framebuffer creation helpers
│   ├── VulkanSwapChain.hpp                              # Swapchain wrapper
│   ├── VulkanSwapChain.cpp                              # Swapchain implementation
│   ├── VulkanDebug.hpp                                  # Validation layer messenger + debug utils
│   └── VulkanDebug.cpp                                  # VulkanDebug implementation
│
├── src/                                                 # All engine + generator code lives here
│   │
│   ├── core/                                            # Core abstractions (kentril0 src/core/ style)
│   │   ├── CMakeLists.txt                               # Builds libcore.a
│   │   ├── Application.h                                # Base app: window, main loop, event pump
│   │   ├── Application.cpp                              # Application implementation
│   │   ├── Assert.h                                     # ASSERT / VERIFY macros
│   │   ├── Base.h                                       # Common typedefs, platform macros
│   │   ├── Log.h                                        # Logging levels + macros
│   │   ├── Log.cpp                                      # Log implementation
│   │   ├── Profile.h                                    # CPU/GPU scope profiler macros
│   │   ├── Profile.cpp                                  # Profiler implementation
│   │   ├── Timer.h                                      # High-resolution timer utility
│   │   └── Timestep.h                                   # Delta-time wrapper
│   │
│   ├── vulkan/                                          # Vulkan wrappers (kentril0 src/vulkan/ style)
│   │   ├── CMakeLists.txt                               # Builds libvulkan_wrap.a
│   │   ├── Buffer.h                                     # RAII VkBuffer + VkDeviceMemory wrapper
│   │   ├── Buffer.cpp                                   # Buffer implementation
│   │   ├── CommandBuffer.h                              # Command buffer recording helpers
│   │   ├── CommandBuffer.cpp                            # CommandBuffer implementation
│   │   ├── CommandPool.h                                # Pool per queue family
│   │   ├── CommandPool.cpp                              # CommandPool implementation
│   │   ├── Descriptors.h                                # Descriptor set layout + pool helpers
│   │   ├── Descriptors.cpp                              # Descriptors implementation
│   │   ├── Device.h                                     # Logical device + queues
│   │   ├── Device.cpp                                   # Device implementation
│   │   ├── Pipeline.h                                   # Graphics/compute pipeline wrapper
│   │   ├── Pipeline.cpp                                 # Pipeline implementation
│   │   ├── RenderPass.h                                 # Render pass + subpass helpers
│   │   ├── RenderPass.cpp                               # RenderPass implementation
│   │   ├── Shader.h                                     # SPIR-V module loader
│   │   ├── Shader.cpp                                   # Shader implementation
│   │   ├── SwapChain.h                                  # Present-mode, image-view, acquire/present
│   │   ├── SwapChain.cpp                                # SwapChain implementation
│   │   ├── Texture.h                                    # VkImage + view + sampler
│   │   ├── Texture.cpp                                  # Texture implementation
│   │   ├── BindlessHeap.h                               # Bindless descriptor heap (VK_EXT_descriptor_indexing)
│   │   ├── BindlessHeap.cpp                             # BindlessHeap implementation
│   │   ├── DescriptorIndexing.cpp                       # Runtime feature detection for descriptor indexing
│   │   ├── PipelineCache.h                              # VkPipelineCache persistence
│   │   ├── PipelineCache.cpp                            # PipelineCache implementation
│   │   ├── PipelineBuilder.h                            # Fluent pipeline construction
│   │   ├── PipelineBuilder.cpp                          # PipelineBuilder implementation
│   │   ├── FrameGraph.h                                 # Render-graph for pass scheduling
│   │   └── FrameGraph.cpp                               # FrameGraph implementation
│   │
│   ├── scene/                                           # Scene / world objects (kentril0 src/scene/ style)
│   │   ├── CMakeLists.txt                               # Builds libscene.a
│   │   ├── Camera.h                                     # Perspective/ortho camera
│   │   ├── Camera.cpp                                   # Camera implementation
│   │   ├── CameraController.h                           # Fly/walk/orbit controllers
│   │   ├── CameraController.cpp                         # Controller implementation
│   │   ├── Mesh.h                                       # GPU vertex/index buffer pair + bounds
│   │   ├── Mesh.cpp                                     # Mesh implementation
│   │   ├── Meshlet.h                                    # Meshlet cluster (VK_EXT_mesh_shader path)
│   │   ├── Meshlet.cpp                                  # Meshlet implementation
│   │   ├── Material.h                                   # Material descriptor + PBR constants
│   │   ├── Material.cpp                                 # Material implementation
│   │   ├── MaterialRegistry.h                           # Global material table + bindless handles
│   │   ├── MaterialRegistry.cpp                         # MaterialRegistry implementation
│   │   ├── Scene.h                                      # Scene graph root
│   │   ├── Scene.cpp                                    # Scene implementation
│   │   ├── SceneNode.h                                  # Transform + children + component slots
│   │   ├── SceneNode.cpp                                # SceneNode implementation
│   │   ├── SkyModel.h                                   # Sky/atmosphere model hook (kentril0 SkyModel.h)
│   │   ├── SkyModel.cpp                                 # SkyModel implementation
│   │   ├── Light.h                                      # Directional/point/spot light
│   │   ├── Light.cpp                                    # Light implementation
│   │   ├── Transform.h                                  # TRS + matrix cache
│   │   ├── Transform.cpp                                # Transform implementation
│   │   ├── AABB.h                                       # Axis-aligned bounding box
│   │   ├── AABB.cpp                                     # AABB implementation
│   │   └── WorldState.h                                 # Global time, weather, seed state
│   │
│   ├── road/                                            # Road system: splines, network, tiles
│   │   ├── CMakeLists.txt                               # Builds libroad.a + compiles road shaders
│   │   ├── RoadNetwork.h                                # Road graph (nodes, edges, lanes)
│   │   ├── RoadNetwork.cpp                              # RoadNetwork implementation
│   │   ├── RoadSplineGraph.h                            # Spline-based road centerline representation
│   │   ├── RoadSplineGraph.cpp                          # RoadSplineGraph implementation
│   │   ├── RoadTileGen.h                                # Streaming-tile road chunk generator
│   │   ├── RoadTileGen.cpp                              # RoadTileGen implementation
│   │   ├── README.md                                    # Road subsystem overview
│   │   │
│   │   ├── shaders/                                     # Road-category shared shaders
│   │   │   ├── include/
│   │   │   │   ├── road_common.glsl                     # Shared road constants, bindings layout
│   │   │   │   └── road_types.glsl                      # Shared structs (RoadVertex, Lane, Segment)
│   │   │   ├── road_network.comp                        # Network graph → segment geometry
│   │   │   ├── road_spline.comp                         # Spline evaluation → vertex ribbons
│   │   │   └── road_tile.comp                           # Tile chunk assembly + LOD
│   │   │
│   │   ├── textures/                                    # Road-category shared texture inputs
│   │   │   ├── seeds/
│   │   │   │   └── road_seed.bin                        # Road noise seeds shared across states
│   │   │   └── spec/
│   │   │       └── road_common.h                        # Shared texture format spec
│   │   │
│   │   ├── meshes/                                      # Road-category shared mesh inputs
│   │   │   ├── profiles/
│   │   │   │   └── road_camber.h                        # Road cross-section camber profile
│   │   │   └── graphs/
│   │   │       └── road_graph.bin                       # Serialized road graphs (shared)
│   │   │
│   │   ├── asphalt/                                     # Dry asphalt generation
│   │   │   ├── ProceduralAsphalt.h                      # Asphalt PBR set generator
│   │   │   ├── ProceduralAsphalt.cpp                    # Asphalt PBR set implementation
│   │   │   ├── AsphaltParams.h                          # constexpr asphalt parameters
│   │   │   ├── README.md                                # Asphalt module docs
│   │   │   ├── shaders/
│   │   │   │   ├── asphalt_albedo.comp                  # Albedo texture generation
│   │   │   │   ├── asphalt_normal.comp                  # Normal map generation
│   │   │   │   ├── asphalt_orm.comp                     # ORM (occlusion/rough/metal) generation
│   │   │   │   └── asphalt_height.comp                  # Height/displacement generation
│   │   │   ├── textures/
│   │   │   │   ├── seeds/
│   │   │   │   │   └── asphalt_seed.bin                 # Random seed for asphalt noise
│   │   │   │   └── spec/
│   │   │   │       └── asphalt_spec.h                   # Format spec: BC7/BC5/BC4 choices
│   │   │   └── meshes/
│   │   │       └── spec/
│   │   │           └── asphalt_lod.h                    # LOD tier counts for asphalt patches
│   │   │
│   │   ├── markings/                                    # Road markings: centerlines, crosswalks, arrows
│   │   │   ├── RoadMarkingGenerator.h                   # Markings generator
│   │   │   ├── RoadMarkingGenerator.cpp                 # Markings implementation
│   │   │   ├── MarkingParams.h                          # constexpr marking parameters
│   │   │   ├── README.md                                # Markings module docs
│   │   │   ├── shaders/
│   │   │   │   ├── centerline.comp                      # Continuous/dashed centerlines
│   │   │   │   ├── crosswalk.comp                       # Crosswalk stripes
│   │   │   │   └── marking_stencil.comp                 # Stencil mask applicator
│   │   │   ├── textures/
│   │   │   │   ├── stencils/
│   │   │   │   │   ├── crosswalk.r8                     # Crosswalk stencil mask
│   │   │   │   │   └── arrow_straight.r8                # Straight-arrow stencil
│   │   │   │   └── spec/
│   │   │   │       └── stencil_spec.h                   # Stencil format spec
│   │   │   └── meshes/
│   │   │       └── profiles/
│   │   │           └── marking_widths.h                 # Marking width table
│   │   │
│   │   ├── signs/                                       # Road signs: SDF-driven signs + poles
│   │   │   ├── ProceduralSign.h                         # Sign generator
│   │   │   ├── ProceduralSign.cpp                       # Sign implementation
│   │   │   ├── SignParams.h                             # constexpr sign parameters
│   │   │   ├── README.md                                # Signs module docs
│   │   │   ├── shaders/
│   │   │   │   ├── sign_sdf.comp                        # SDF evaluation for sign face
│   │   │   │   ├── sign_mesh.comp                       # Sign body mesh generation
│   │   │   │   └── sign_atlas.comp                      # Sign face atlas packing
│   │   │   ├── textures/
│   │   │   │   ├── sdf/
│   │   │   │   │   ├── stop.sdf                         # Stop sign SDF
│   │   │   │   │   ├── yield.sdf                        # Yield sign SDF
│   │   │   │   │   └── speed_60.sdf                     # Speed limit SDF
│   │   │   │   └── spec/
│   │   │   │       └── sign_spec.h                      # Sign format spec
│   │   │   └── meshes/
│   │   │       └── profiles/
│   │   │           └── sign_pole.h                      # Sign pole cross-section
│   │   │
│   │   ├── barriers/                                    # Guardrails, jersey barriers, bollards
│   │   │   ├── BarrierKit.h                             # Modular barrier kit generator
│   │   │   ├── BarrierKit.cpp                           # Barrier kit implementation
│   │   │   ├── BarrierParams.h                          # constexpr barrier parameters
│   │   │   ├── README.md                                # Barriers module docs
│   │   │   ├── shaders/
│   │   │   │   ├── guardrail.comp                       # Guardrail extrusion along road
│   │   │   │   ├── jersey_barrier.comp                  # Concrete jersey barrier extrusion
│   │   │   │   └── bollard.comp                         # Bollard placement + mesh
│   │   │   ├── textures/
│   │   │   │   └── spec/
│   │   │   │       └── barrier_spec.h                   # Barrier format spec
│   │   │   └── meshes/
│   │   │       └── profiles/
│   │   │           ├── guardrail_profile.h              # Guardrail W-beam cross-section
│   │   │           └── jersey_profile.h                 # Jersey barrier cross-section
│   │   │
│   │   ├── intersections/                               # 4-way, roundabout, T-junction builders
│   │   │   ├── IntersectionBuilder.h                    # Intersection generator
│   │   │   ├── IntersectionBuilder.cpp                  # Intersection implementation
│   │   │   ├── IntersectionParams.h                     # constexpr intersection parameters
│   │   │   ├── README.md                                # Intersections module docs
│   │   │   ├── shaders/
│   │   │   │   ├── cross_4way.comp                      # 4-way intersection geometry
│   │   │   │   ├── roundabout.comp                      # Roundabout geometry
│   │   │   │   └── t_junction.comp                      # T-junction geometry
│   │   │   ├── textures/
│   │   │   └── spec/
│   │   │       └── intersection_spec.h                  # Intersection format spec
│   │   │   └── meshes/
│   │   │       └── graphs/
│   │   │           ├── cross_4way.bin                   # Serialized 4-way graph
│   │   │           └── roundabout.bin                   # Serialized roundabout graph
│   │   │
│   │   ├── bridges/                                     # Overpasses and footbridges
│   │   │   ├── BridgeGenerator.h                        # Bridge generator
│   │   │   ├── BridgeGenerator.cpp                      # Bridge implementation
│   │   │   ├── BridgeParams.h                           # constexpr bridge parameters
│   │   │   ├── README.md                                # Bridges module docs
│   │   │   ├── shaders/
│   │   │   │   ├── overpass.comp                        # Overpass deck + supports
│   │   │   │   └── footbridge.comp                      # Footbridge geometry
│   │   │   ├── textures/
│   │   │   │   └── spec/
│   │   │   │       └── bridge_spec.h                    # Bridge format spec
│   │   │   └── meshes/
│   │   │       └── profiles/
│   │   │           └── bridge_span.h                    # Bridge span cross-section
│   │   │
│   │   ├── tunnels/                                     # Tunnel segments + interior lighting
│   │   │   ├── TunnelGenerator.h                        # Tunnel generator
│   │   │   ├── TunnelGenerator.cpp                      # Tunnel implementation
│   │   │   ├── TunnelParams.h                           # constexpr tunnel parameters
│   │   │   ├── README.md                                # Tunnels module docs
│   │   │   ├── shaders/
│   │   │   │   ├── tunnel_segment.comp                  # Tunnel tube segment extrusion
│   │   │   │   └── tunnel_lighting.comp                 # Interior light placement
│   │   │   ├── textures/
│   │   │   │   └── spec/
│   │   │   │       └── tunnel_spec.h                    # Tunnel format spec
│   │   │   └── meshes/
│   │   │       └── profiles/
│   │   │           └── tunnel_ring.h                    # Tunnel cross-section ring
│   │   │
│   │   ├── states/                                      # Road surface states (condition variants)
│   │   │   ├── README.md                                # Road-state overview: what each means
│   │   │   ├── muddy/                                   # Muddy road state
│   │   │   │   ├── MuddyState.h                         # Muddy road surface generator
│   │   │   │   ├── MuddyState.cpp                       # Implementation
│   │   │   │   ├── MuddyParams.h                        # Slip factor, wheel-rut depth, wetness
│   │   │   │   ├── README.md                            # Docs: material + physics interaction
│   │   │   │   ├── shaders/
│   │   │   │   │   ├── mud_albedo.comp                  # Mud albedo + puddle blend
│   │   │   │   │   ├── mud_normal.comp                  # Mud normal + ruts
│   │   │   │   │   └── mud_wetness.comp                 # Dynamic wetness mask
│   │   │   │   ├── textures/
│   │   │   │   │   ├── seeds/muddy_seed.bin             # Muddy noise seed
│   │   │   │   │   └── spec/muddy_spec.h                # Format spec
│   │   │   │   └── meshes/
│   │   │   │       └── profiles/muddy_rut.h             # Wheel-rut profile
│   │   │   ├── rocky/                                   # Rocky / unpaved rubble road
│   │   │   │   ├── RockyState.h
│   │   │   │   ├── RockyState.cpp
│   │   │   │   ├── RockyParams.h                        # Rock size distribution, density
│   │   │   │   ├── README.md
│   │   │   │   ├── shaders/
│   │   │   │   │   ├── rock_albedo.comp
│   │   │   │   │   ├── rock_normal.comp
│   │   │   │   │   └── rock_scatter.comp
│   │   │   │   ├── textures/
│   │   │   │   │   ├── seeds/rocky_seed.bin
│   │   │   │   │   └── spec/rocky_spec.h
│   │   │   │   └── meshes/
│   │   │   │       └── profiles/rocky_bed.h
│   │   │   ├── bumpy/                                   # Bumpy / washboard surface
│   │   │   │   ├── BumpyState.h
│   │   │   │   ├── BumpyState.cpp
│   │   │   │   ├── BumpyParams.h                        # Amplitude, frequency, seed
│   │   │   │   ├── README.md
│   │   │   │   ├── shaders/
│   │   │   │   │   ├── bumpy_height.comp
│   │   │   │   │   └── bumpy_normal.comp
│   │   │   │   ├── textures/
│   │   │   │   │   ├── seeds/bumpy_seed.bin
│   │   │   │   └── spec/bumpy_spec.h
│   │   │   │   └── meshes/
│   │   │   │       └── profiles/bumpy_profile.h
│   │   │   ├── sandy/                                   # Sandy / loose-sand surface
│   │   │   │   ├── SandyState.h
│   │   │   │   ├── SandyState.cpp
│   │   │   │   ├── SandyParams.h                        # Grain size, dune scale
│   │   │   │   ├── README.md
│   │   │   │   ├── shaders/
│   │   │   │   │   ├── sand_albedo.comp
│   │   │   │   │   ├── sand_normal.comp
│   │   │   │   │   └── sand_ripple.comp
│   │   │   │   ├── textures/
│   │   │   │   │   ├── seeds/sandy_seed.bin
│   │   │   │   │   └── spec/sandy_spec.h
│   │   │   │   └── meshes/
│   │   │   │       └── profiles/sand_ripple.h
│   │   │   ├── gravel/                                  # Gravel / crushed-stone road
│   │   │   │   ├── GravelState.h
│   │   │   │   ├── GravelState.cpp
│   │   │   │   ├── GravelParams.h
│   │   │   │   ├── README.md
│   │   │   │   ├── shaders/
│   │   │   │   │   ├── gravel_albedo.comp
│   │   │   │   │   ├── gravel_normal.comp
│   │   │   │   └── gravel_scatter.comp
│   │   │   │   ├── textures/
│   │   │   │   │   ├── seeds/gravel_seed.bin
│   │   │   │   │   └── spec/gravel_spec.h
│   │   │   │   └── meshes/
│   │   │   │       └── profiles/gravel_bed.h
│   │   │   ├── snowy/                                   # Snow-covered road
│   │   │   │   ├── SnowyState.h
│   │   │   │   ├── SnowyState.cpp
│   │   │   │   ├── SnowyParams.h                        # Snow depth, tire tracks, salt
│   │   │   │   ├── README.md
│   │   │   │   ├── shaders/
│   │   │   │   │   ├── snow_albedo.comp
│   │   │   │   │   ├── snow_normal.comp
│   │   │   │   │   └── snow_tracks.comp
│   │   │   │   ├── textures/
│   │   │   │   │   ├── seeds/snowy_seed.bin
│   │   │   │   │   └── spec/snowy_spec.h
│   │   │   │   └── meshes/
│   │   │   │       └── profiles/snow_track.h
│   │   │   ├── icy/                                     # Icy / frozen road
│   │   │   │   ├── IcyState.h
│   │   │   │   ├── IcyState.cpp
│   │   │   │   ├── IcyParams.h                          # Ice thickness, crack density
│   │   │   │   ├── README.md
│   │   │   │   ├── shaders/
│   │   │   │   │   ├── ice_albedo.comp
│   │   │   │   │   ├── ice_normal.comp
│   │   │   │   │   └── ice_cracks.comp
│   │   │   │   ├── textures/
│   │   │   │   │   ├── seeds/icy_seed.bin
│   │   │   │   │   └── spec/icy_spec.h
│   │   │   │   └── meshes/
│   │   │   │       └── profiles/ice_surface.h
│   │   │   ├── wet/                                     # Wet / post-rain road
│   │   │   │   ├── WetState.h
│   │   │   │   ├── WetState.cpp
│   │   │   │   ├── WetParams.h                          # Film thickness, reflection blur
│   │   │   │   ├── README.md
│   │   │   │   ├── shaders/
│   │   │   │   │   ├── wet_albedo.comp
│   │   │   │   │   ├── wet_normal.comp
│   │   │   │   │   └── wet_reflection.comp
│   │   │   │   ├── textures/
│   │   │   │   │   ├── seeds/wet_seed.bin
│   │   │   │   │   └── spec/wet_spec.h
│   │   │   │   └── meshes/
│   │   │   │       └── profiles/wet_film.h
│   │   │   ├── dry_cracked/                             # Dry cracked road (drought)
│   │   │   │   ├── DryCrackedState.h
│   │   │   │   ├── DryCrackedState.cpp
│   │   │   │   ├── DryCrackedParams.h
│   │   │   │   ├── README.md
│   │   │   │   ├── shaders/
│   │   │   │   │   ├── crack_pattern.comp
│   │   │   │   │   └── crack_normal.comp
│   │   │   │   ├── textures/
│   │   │   │   │   ├── seeds/dry_seed.bin
│   │   │   │   │   └── spec/dry_spec.h
│   │   │   │   └── meshes/
│   │   │   │       └── profiles/crack_network.h
│   │   │   └── potholed/                                # Potholed road (degraded)
│   │   │       ├── PotholedState.h
│   │   │       ├── PotholedState.cpp
│   │   │       ├── PotholedParams.h
│   │   │       ├── README.md
│   │   │       ├── shaders/
│   │   │       │   ├── pothole_mask.comp
│   │   │       │   └── pothole_normal.comp
│   │   │       ├── textures/
│   │   │       │   ├── seeds/pothole_seed.bin
│   │   │       │   └── spec/pothole_spec.h
│   │   │       └── meshes/
│   │   │           └── profiles/pothole_profile.h
│   │   │
│   │   └── tiles/                                       # Road streaming tiles
│   │       ├── RoadTileStreaming.h                      # Tile load/unload manager
│   │       ├── RoadTileStreaming.cpp                    # Implementation
│   │       ├── README.md                                # Tiles module docs
│   │       ├── shaders/
│   │       │   └── tile_stream.comp                     # Tile stream-in compute
│   │       ├── textures/
│   │       │   └── spec/
│   │       │       └── tile_spec.h                      # Tile format spec
│   │       └── meshes/
│   │           └── spec/
│   │               └── tile_lod.h                       # Tile LOD tier config
│   │
│   ├── vegetation/                                      # All plant life: trees, bushes, grass, crops
│   │   ├── CMakeLists.txt                               # Builds libvegetation.a
│   │   ├── VegetationSystem.h                           # Orchestrates all vegetation subsystems
│   │   ├── VegetationSystem.cpp                         # Implementation
│   │   ├── README.md                                    # Vegetation subsystem docs
│   │   │
│   │   ├── shaders/                                     # Vegetation-category shared shaders
│   │   │   ├── include/
│   │   │   │   ├── vegetation_common.glsl               # Shared bindings + constants
│   │   │   │   └── vegetation_types.glsl                # Shared structs
│   │   │   ├── vegetation_scatter.comp                  # Global vegetation scatter
│   │   │   └── vegetation_lod.comp                      # Cross-species LOD
│   │   │
│   │   ├── textures/                                    # Vegetation-category shared texture inputs
│   │   │   ├── seeds/
│   │   │   │   └── vegetation_seed.bin                  # Global vegetation seed
│   │   │   └── spec/
│   │   │       └── vegetation_common.h                  # Shared spec
│   │   │
│   │   ├── meshes/                                      # Vegetation-category shared mesh inputs
│   │   │   ├── profiles/
│   │   │   │   └── vegetation_lod_tiers.h               # LOD tier table
│   │   │   └── spec/
│   │   │       └── vegetation_mesh_spec.h               # Shared mesh spec
│   │   │
│   │   ├── trees/                                       # L-system driven trees
│   │   │   ├── LSystemTree.h                            # L-system tree generator
│   │   │   ├── LSystemTree.cpp                          # Implementation
│   │   │   ├── TreeGrammar.h                            # Grammar interpreter
│   │   │   ├── TreeGrammar.cpp                          # Implementation
│   │   │   ├── TreeLOD.h                                # Tree LOD generation
│   │   │   ├── TreeLOD.cpp                              # Implementation
│   │   │   ├── TreeImpostor.h                           # Impostor billboard baker
│   │   │   ├── TreeImpostor.cpp                         # Implementation
│   │   │   ├── TreeSpecies.h                            # constexpr species table
│   │   │   ├── README.md                                # Trees module docs
│   │   │   ├── shaders/
│   │   │   │   ├── tree_growth.comp                     # L-system expansion
│   │   │   │   ├── tree_mesh.comp                       # Trunk/branch mesh from grammar
│   │   │   │   ├── tree_lod.comp                        # LOD mesh reduction
│   │   │   │   └── tree_impostor.comp                   # Billboard bake
│   │   │   ├── textures/
│   │   │   │   ├── bark/
│   │   │   │   │   ├── bark_oak.comp                    # Oak bark procedural texture
│   │   │   │   │   └── bark_pine.comp                   # Pine bark procedural texture
│   │   │   ├── leaves/
│   │   │   │   │   └── leaf_atlas.comp                  # Leaf card atlas
│   │   │   │   └── spec/
│   │   │   │       └── tree_spec.h                      # Tree texture spec
│   │   │   └── meshes/
│   │   │       ├── grammars/
│   │   │       │   ├── oak.lsys                         # Oak L-system
│   │   │       │   ├── pine.lsys                        # Pine L-system
│   │   │       │   ├── palm.lsys                        # palm L-system
│   │   │       │   └── birch.lsys                       # Birch L-system
│   │   │       └── spec/
│   │   │           └── tree_mesh_spec.h                 # Tree mesh spec
│   │   │
│   │   ├── bushes/                                      # Shrub generation
│   │   │   ├── BushGenerator.h
│   │   │   ├── BushGenerator.cpp
│   │   │   ├── BushParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── bush_shape.comp                      # Bush volume shape
│   │   │   │   └── bush_scatter.comp                    # Bush placement
│   │   │   ├── textures/
│   │   │   │   ├── leaf_atlas_bush.comp
│   │   │   │   └── spec/bush_spec.h
│   │   │   └── meshes/
│   │   │       └── spec/bush_mesh_spec.h
│   │   │
│   │   ├── grass/                                       # Grass blades + simulation
│   │   │   ├── GrassBladeGen.h                          # Blade geometry generator
│   │   │   ├── GrassBladeGen.cpp
│   │   │   ├── GrassSim.h                               # Wind + trample simulation
│   │   │   ├── GrassSim.cpp
│   │   │   ├── GrassScatter.h                           # Density-based scatter
│   │   │   ├── GrassScatter.cpp
│   │   │   ├── GrassTypes.h                             # constexpr grass type table
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── grass_blade.comp                     # Blade geometry generation
│   │   │   │   ├── grass_sim.comp                       # Per-blade physics sim
│   │   │   │   ├── grass_scatter.comp                   # Scatter placement
│   │   │   │   └── grass_wind.comp                      # Wind response
│   │   │   ├── textures/
│   │   │   │   ├── grass_atlas.comp
│   │   │   │   ├── clump_masks/
│   │   │   │   │   └── clump_mask.r8                       # Clump density masks
│   │   │   │   └── spec/grass_spec.h
│   │   │   └── meshes/
│   │   │       └── profiles/blade_curve.h               # Blade Bezier curve
│   │   │
│   │   ├── flowers/                                     # Flower cards merged into grass
│   │   │   ├── FlowerGen.h
│   │   │   ├── FlowerGen.cpp
│   │   │   ├── FlowerParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/flower_merge.comp
│   │   │   ├── textures/
│   │   │   │   ├── flower_atlas.comp
│   │   │   │   └── spec/flower_spec.h
│   │   │   └── meshes/spec/flower_mesh_spec.h
│   │   │
│   │   ├── crops/                                       # Wheat, corn, vineyard rows
│   │   │   ├── CropRowGen.h
│   │   │   ├── CropRowGen.cpp
│   │   │   ├── CropParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── crop_stalk.comp
│   │   │   │   └── crop_field.comp
│   │   │   ├── textures/spec/crop_spec.h
│   │   │   └── meshes/profiles/crop_row.h
│   │   │
│   │   ├── rocks/                                       # Boulders, pebbles, erosion
│   │   │   ├── RockGenerator.h
│   │   │   ├── RockGenerator.cpp
│   │   │   ├── RockErosion.h
│   │   │   ├── RockErosion.cpp
│   │   │   ├── RockParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── rock_noise.comp
│   │   │   │   ├── erosion.comp
│   │   │   │   └── rock_scatter.comp
│   │   │   ├── textures/
│   │   │   │   ├── rock_detail.comp
│   │   │   │   └── spec/rock_spec.h
│   │   │   └── meshes/spec/rock_mesh_spec.h
│   │   │
│   │   ├── debris/                                      # Logs, branches, leaf piles
│   │   │   ├── DebrisGen.h
│   │   │   ├── DebrisGen.cpp
│   │   │   ├── DebrisParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/debris_scatter.comp
│   │   │   ├── textures/spec/debris_spec.h
│   │   │   └── meshes/spec/debris_mesh_spec.h
│   │   │
│   │   ├── biomes/                                      # Biome rule tables
│   │   │   ├── BiomeRules.h
│   │   │   ├── BiomeRules.cpp
│   │   │   ├── BiomeTemperate.cpp
│   │   │   ├── BiomeDesert.cpp
│   │   │   ├── BiomeForest.cpp
│   │   │   ├── BiomeTundra.cpp
│   │   │   ├── README.md
│   │   │   ├── shaders/biome_blend.comp
│   │   │   ├── textures/spec/biome_spec.h
│   │   │   └── meshes/spec/biome_mesh_spec.h
│   │   │
│   │   └── wind/                                        # Vegetation wind response
│   │       ├── WindField.h
│   │       ├── WindField.cpp
│   │       ├── WindParams.h
│   │       ├── README.md
│   │       ├── shaders/wind_zone.comp
│   │       ├── textures/spec/wind_spec.h
│   │       └── meshes/spec/wind_mesh_spec.h
│   │
│   ├── surfaces/                                        # PBR surface generators
│   │   ├── CMakeLists.txt
│   │   ├── SurfaceLibrary.h
│   │   ├── SurfaceLibrary.cpp
│   │   ├── README.md
│   │   ├── shaders/
│   │   │   ├── include/
│   │   │   │   ├── surface_common.glsl
│   │   │   │   └── surface_types.glsl
│   │   │   └── surface_blend.comp
│   │   ├── textures/
│   │   │   ├── seeds/surface_seed.bin
│   │   │   └── spec/surface_common.h
│   │   ├── meshes/spec/surface_mesh_spec.h
│   │   │
│   │   ├── ground/                                      # Soil, mud, sand, snow ground
│   │   │   ├── GroundMaterial.h
│   │   │   ├── GroundMaterial.cpp
│   │   │   ├── SoilNoise.h
│   │   │   ├── SoilNoise.cpp
│   │   │   ├── Mud.cpp
│   │   │   ├── Sand.cpp
│   │   │   ├── Snow.cpp
│   │   │   ├── GroundParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── soil_noise.comp
│   │   │   │   ├── mud.comp
│   │   │   │   ├── sand.comp
│   │   │   │   └── snow.comp
│   │   │   ├── textures/
│   │   │   │   ├── seeds/ground_seed.bin
│   │   │   │   └── spec/ground_spec.h
│   │   │   └── meshes/spec/ground_mesh_spec.h
│   │   │
│   │   ├── concrete/                                    # Concrete surfaces
│   │   │   ├── ConcreteGen.h
│   │   │   ├── ConcreteGen.cpp
│   │   │   ├── ConcreteParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── concrete_voronoi.comp
│   │   │   │   ├── concrete_grain.comp
│   │   │   │   └── concrete_cracks.comp
│   │   │   ├── textures/spec/concrete_spec.h
│   │   │   └── meshes/spec/concrete_mesh_spec.h
│   │   │
│   │   ├── metal/                                       # Metal surfaces
│   │   │   ├── MetalGen.h
│   │   │   ├── MetalGen.cpp
│   │   │   ├── MetalParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── metal_scratch.comp
│   │   │   │   ├── metal_rust.comp
│   │   │   │   └── metal_brush.comp
│   │   │   ├── textures/spec/metal_spec.h
│   │   │   └── meshes/spec/metal_mesh_spec.h
│   │   │
│   │   ├── wood/                                        # Wood surfaces
│   │   │   ├── WoodGen.h
│   │   │   ├── WoodGen.cpp
│   │   │   ├── BarkGen.h
│   │   │   ├── BarkGen.cpp
│   │   │   ├── WoodParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── wood_rings.comp
│   │   │   │   ├── wood_grain.comp
│   │   │   │   └── bark.comp
│   │   │   ├── textures/spec/wood_spec.h
│   │   │   └── meshes/spec/wood_mesh_spec.h
│   │   │
│   │   ├── tiles/                                       # Ceramic, marble
│   │   │   ├── TileGen.h
│   │   │   ├── TileGen.cpp
│   │   │   ├── Ceramic.cpp
│   │   │   ├── Marble.cpp
│   │   │   ├── TileParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── ceramic.comp
│   │   │   │   └── marble.comp
│   │   │   ├── textures/spec/tile_spec.h
│   │   │   └── meshes/spec/tile_mesh_spec.h
│   │   │
│   │   ├── fabric/                                      # Weave, carpet
│   │   │   ├── FabricGen.h
│   │   │   ├── FabricGen.cpp
│   │   │   ├── Weave.cpp
│   │   │   ├── Carpet.cpp
│   │   │   ├── FabricParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── weave.comp
│   │   │   │   └── carpet.comp
│   │   │   ├── textures/spec/fabric_spec.h
│   │   │   └── meshes/spec/fabric_mesh_spec.h
│   │   │
│   │   ├── glass/                                       # Glass IOR + refraction
│   │   │   ├── GlassMaterial.h
│   │   │   ├── GlassMaterial.cpp
│   │   │   ├── GlassIOR.h
│   │   │   ├── README.md
│   │   │   ├── shaders/glass_refract.comp
│   │   │   ├── textures/spec/glass_spec.h
│   │   │   └── meshes/spec/glass_mesh_spec.h
│   │   │
│   │   ├── decals/                                      # Oil, cracks, moss decals
│   │   │   ├── DecalProjector.h
│   │   │   ├── DecalProjector.cpp
│   │   │   ├── OilStain.cpp
│   │   │   ├── CrackDecal.cpp
│   │   │   ├── MossDecal.cpp
│   │   │   ├── DecalParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── oil_stain.comp
│   │   │   │   └── crack_decal.comp
│   │   │   │   └── moss_decal.comp
│   │   │   ├── textures/spec/decal_spec.h
│   │   │   └── meshes/spec/decal_mesh_spec.h
│   │   │
│   │   ├── splat/                                       # Terrain splatmap blending
│   │   │   ├── SplatGenerator.h
│   │   │   ├── SplatGenerator.cpp
│   │   │   ├── SplatParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/splat_blend.comp
│   │   │   ├── textures/spec/splat_spec.h
│   │   │   └── meshes/spec/splat_mesh_spec.h
│   │   │
│   │   └── detail/                                      # High-frequency detail layers
│   │       ├── DetailNoise.h
│   │       ├── DetailNoise.cpp
│   │       ├── DetailParams.h
│   │       ├── README.md
│   │       ├── shaders/
│   │       │   ├── detail_rock.comp
│   │       │   └── detail_grass.comp
│   │       ├── textures/spec/detail_spec.h
│   │       └── meshes/spec/detail_mesh_spec.h
│   │
│   ├── buildings/                                       # Building generators
│   │   ├── CMakeLists.txt
│   │   ├── BuildingSystem.h
│   │   ├── BuildingSystem.cpp
│   │   ├── README.md
│   │   ├── shaders/
│   │   │   ├── include/
│   │   │   │   ├── building_common.glsl
│   │   │   │   └── building_types.glsl
│   │   │   ├── facade.comp
│   │   │   └── socket_attach.comp
│   │   ├── textures/
│   │   │   ├── seeds/building_seed.bin
│   │   │   └── spec/building_common.h
│   │   ├── meshes/
│   │   │   ├── profiles/building_lod_tiers.h
│   │   │   └── spec/building_mesh_spec.h
│   │   │
│   │   ├── residential/                                 # Houses, apartments, villas
│   │   │   ├── HouseGen.h
│   │   │   ├── HouseGen.cpp
│   │   │   ├── HouseSuburban.cpp
│   │   │   ├── ApartmentBlock.cpp
│   │   │   ├── Villa.cpp
│   │   │   ├── ResidentialParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/residential_facade.comp
│   │   │   ├── textures/spec/residential_spec.h
│   │   │   └── meshes/spec/residential_mesh_spec.h
│   │   │
│   │   ├── commercial/                                  # Towers, malls, shops
│   │   │   ├── TowerGen.h
│   │   │   ├── TowerGen.cpp
│   │   │   ├── OfficeTower.cpp
│   │   │   ├── Mall.cpp
│   │   │   ├── ShopSmall.cpp
│   │   │   ├── CommercialParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/window_grid.comp
│   │   │   ├── textures/spec/commercial_spec.h
│   │   │   └── meshes/spec/commercial_mesh_spec.h
│   │   │
│   │   ├── industrial/                                  # Warehouses, factories, silos
│   │   │   ├── WarehouseGen.h
│   │   │   ├── WarehouseGen.cpp
│   │   │   ├── Factory.cpp
│   │   │   ├── Silo.cpp
│   │   │   ├── IndustrialParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/roll_door.comp
│   │   │   ├── textures/spec/industrial_spec.h
│   │   │   └── meshes/spec/industrial_mesh_spec.h
│   │   │
│   │   ├── civic/                                       # Schools, hospitals, townhalls
│   │   │   ├── CivicGen.h
│   │   │   ├── CivicGen.cpp
│   │   │   ├── School.cpp
│   │   │   ├── Hospital.cpp
│   │   │   ├── Townhall.cpp
│   │   │   ├── CivicParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/civic_facade.comp
│   │   │   ├── textures/spec/civic_spec.h
│   │   │   └── meshes/spec/civic_mesh_spec.h
│   │   │
│   │   ├── parts/                                       # Wall/roof/door/window kits
│   │   │   ├── WallSegment.h
│   │   │   ├── WallSegment.cpp
│   │   │   ├── RoofGen.h
│   │   │   ├── RoofGen.cpp
│   │   │   ├── DoorGen.h
│   │   │   ├── DoorGen.cpp
│   │   │   ├── WindowGen.h
│   │   │   ├── WindowGen.cpp
│   │   │   ├── Socket.h
│   │   │   ├── PartsParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── wall.comp
│   │   │   │   ├── roof.comp
│   │   │   │   ├── door.comp
│   │   │   │   └── window.comp
│   │   │   ├── textures/spec/parts_spec.h
│   │   │   └── meshes/profiles/
│   │   │       ├── wall_profile.h
│   │   │       ├── roof_profile.h
│   │   │       └── window_frame.h
│   │   │
│   │   ├── interiors/                                   # Room floorplans
│   │   │   ├── RoomGen.h
│   │   │   ├── RoomGen.cpp
│   │   │   ├── RoomLiving.cpp
│   │   │   ├── RoomOffice.cpp
│   │   │   ├── Floorplan.cpp
│   │   │   ├── InteriorParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/floorplan.comp
│   │   │   ├── textures/spec/interior_spec.h
│   │   │   └── meshes/spec/interior_mesh_spec.h
│   │   │
│   │   ├── props/                                       # Building-attached props
│   │   │   ├── PropAttach.h
│   │   │   ├── PropAttach.cpp
│   │   │   ├── ACUnit.cpp
│   │   │   ├── Chimney.cpp
│   │   │   ├── Antenna.cpp
│   │   │   ├── BuildingPropParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/prop_attach.comp
│   │   │   ├── textures/spec/building_prop_spec.h
│   │   │   └── meshes/spec/building_prop_mesh_spec.h
│   │   │
│   │   ├── signage/                                     # Neon signs, billboards
│   │   │   ├── NeonSignGen.h
│   │   │   ├── NeonSignGen.cpp
│   │   │   ├── Billboard.cpp
│   │   │   ├── SignageParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/sign_glow.comp
│   │   │   ├── textures/spec/signage_spec.h
│   │   │   └── meshes/spec/signage_mesh_spec.h
│   │   │
│   │   ├── damage/                                      # Cracks, rubble
│   │   │   ├── DamageGen.h
│   │   │   ├── DamageGen.cpp
│   │   │   ├── WallCracked.cpp
│   │   │   ├── RubblePile.cpp
│   │   │   ├── DamageParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/crack_sim.comp
│   │   │   ├── textures/spec/damage_spec.h
│   │   │   └── meshes/spec/damage_mesh_spec.h
│   │   │
│   │   └── lod/                                         # Impostors + bakes
│   │       ├── ImpostorGen.h
│   │       ├── ImpostorGen.cpp
│   │       ├── CityBlockImpostor.cpp
│   │       ├── LodParams.h
│   │       ├── README.md
│   │       ├── shaders/
│   │       │   ├── city_block_impostor.comp
│   │       │   └── impostor_bake.comp
│   │       ├── textures/spec/impostor_spec.h
│   │       └── meshes/spec/impostor_mesh_spec.h
│   │
│   ├── weather/                                         # Weather systems
│   │   ├── CMakeLists.txt
│   │   ├── WeatherSystem.h
│   │   ├── WeatherSystem.cpp
│   │   ├── WeatherState.h
│   │   ├── README.md
│   │   ├── shaders/
│   │   │   ├── include/
│   │   │   │   ├── weather_common.glsl
│   │   │   │   └── weather_types.glsl
│   │   │   └── weather_blend.comp
│   │   ├── textures/
│   │   │   ├── seeds/weather_seed.bin
│   │   │   └── spec/weather_common.h
│   │   ├── meshes/spec/weather_mesh_spec.h
│   │   │
│   │   ├── rain/                                        # Rain + splash + streak
│   │   │   ├── RainSystem.h
│   │   │   ├── RainSystem.cpp
│   │   │   ├── RainSplash.h
│   │   │   ├── RainSplash.cpp
│   │   │   ├── RainParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── rain_particle.comp
│   │   │   │   ├── rain_splash.comp
│   │   │   │   └── rain_streak.comp
│   │   │   ├── textures/
│   │   │   │   ├── rain_streak_atlas.comp
│   │   │   │   └── spec/rain_spec.h
│   │   │   └── meshes/spec/rain_mesh_spec.h
│   │   │
│   │   ├── snow/                                        # Snow + blizzard
│   │   │   ├── SnowSystem.h
│   │   │   ├── SnowSystem.cpp
│   │   │   ├── Blizzard.cpp
│   │   │   ├── SnowParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── snow_flurry.comp
│   │   │   │   └── blizzard.comp
│   │   │   ├── textures/
│   │   │   │   ├── snowflake_atlas.comp
│   │   │   │   └── spec/snow_spec.h
│   │   │   └── meshes/spec/snow_mesh_spec.h
│   │   │
│   │   ├── fog/                                         # Froxel + ground fog
│   │   │   ├── FogVolume.h
│   │   │   ├── FogVolume.cpp
│   │   │   ├── FogParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── fog_froxel.comp
│   │   │   │   └── fog_ground.comp
│   │   │   ├── textures/
│   │   │   │   ├── fog_noise.comp
│   │   │   │   └── spec/fog_spec.h
│   │   │   └── meshes/spec/fog_mesh_spec.h
│   │   │
│   │   ├── clouds/                                      # Volumetric clouds
│   │   │   ├── CloudGen.h
│   │   │   ├── CloudGen.cpp
│   │   │   ├── CloudRaymarch.h
│   │   │   ├── CloudRaymarch.cpp
│   │   │   ├── CloudParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── cloud_noise.comp
│   │   │   │   ├── cloud_raymarch.comp
│   │   │   │   └── cloud_shape.comp
│   │   │   ├── textures/
│   │   │   │   ├── cloud_shape_3d.comp
│   │   │   │   └── spec/cloud_spec.h
│   │   │   └── meshes/spec/cloud_mesh_spec.h
│   │   │
│   │   ├── storms/                                      # Thunderstorm + hail
│   │   │   ├── StormPreset.h
│   │   │   ├── StormPreset.cpp
│   │   │   ├── Thunderstorm.cpp
│   │   │   ├── Hail.cpp
│   │   │   ├── StormParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/storm_blend.comp
│   │   │   ├── textures/spec/storm_spec.h
│   │   │   └── meshes/spec/storm_mesh_spec.h
│   │   │
│   │   ├── wind/                                        # Global wind field
│   │   │   ├── GlobalWind.h
│   │   │   ├── GlobalWind.cpp
│   │   │   ├── WindParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/wind_global.comp
│   │   │   ├── textures/spec/wind_spec.h
│   │   │   └── meshes/spec/wind_mesh_spec.h
│   │   │
│   │   ├── lightning/                                   # Bolt path + flash curve
│   │   │   ├── LightningGen.h
│   │   │   ├── LightningGen.cpp
│   │   │   ├── FlashCurve.h
│   │   │   ├── LightningParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/bolt_path.comp
│   │   │   ├── textures/spec/lightning_spec.h
│   │   │   └── meshes/spec/lightning_mesh_spec.h
│   │   │
│   │   ├── puddles/                                     # Wetness + puddle mask
│   │   │   ├── WetnessMap.h
│   │   │   ├── WetnessMap.cpp
│   │   │   ├── PuddleMask.cpp
│   │   │   ├── PuddleParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── wetness.comp
│   │   │   │   └── puddle_mask.comp
│   │   │   ├── textures/spec/puddle_spec.h
│   │   │   └── meshes/spec/puddle_mesh_spec.h
│   │   │
│   │   ├── sky/                                         # Weather-driven sky
│   │   │   ├── SkyGenerator.h
│   │   │   ├── SkyGenerator.cpp
│   │   │   ├── WeatherSkyParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── sky_lut.comp
│   │   │   │   └── sky_cloud_blend.comp
│   │   │   ├── textures/
│   │   │   │   ├── sky_lut_spec.bin
│   │   │   │   └── spec/weather_sky_spec.h
│   │   │   └── meshes/spec/weather_sky_mesh_spec.h
│   │   │
│   │   └── presets/                                     # Clear/rain/snow/storm presets
│   │       ├── WeatherPreset.h
│   │       ├── WeatherPreset.cpp
│   │       ├── WeatherClear.cpp
│   │       ├── WeatherRain.cpp
│   │       ├── WeatherSnow.cpp
│   │       ├── WeatherStorm.cpp
│   │       ├── PresetDefaults.h
│   │       ├── README.md
│   │       ├── shaders/preset_blend.comp
│   │       ├── textures/spec/preset_spec.h
│   │       └── meshes/spec/preset_mesh_spec.h
│   │
│   ├── terrain/                                         # Terrain: heightmaps, splatmaps, cliffs, caves
│   │   ├── CMakeLists.txt
│   │   ├── TerrainSystem.h
│   │   ├── TerrainSystem.cpp
│   │   ├── README.md
│   │   ├── shaders/
│   │   │   ├── include/
│   │   │   │   ├── terrain_common.glsl
│   │   │   │   └── terrain_types.glsl
│   │   │   └── terrain_blend.comp
│   │   ├── textures/
│   │   │   ├── seeds/terrain_seed.bin
│   │   │   └── spec/terrain_common.h
│   │   ├── meshes/
│   │   │   ├── profiles/terrain_lod.h
│   │   │   └── spec/terrain_mesh_spec.h
│   │   │
│   │   ├── heightmaps/                                  # FBM, erosion, ridged, warp
│   │   │   ├── HeightmapGen.h
│   │   │   ├── HeightmapGen.cpp
│   │   │   ├── FbmNoise.h
│   │   │   ├── FbmNoise.cpp
│   │   │   ├── Erosion.h
│   │   │   ├── Erosion.cpp
│   │   │   ├── Ridged.cpp
│   │   │   ├── Warp.cpp
│   │   │   ├── TerrainParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── fbm.comp
│   │   │   │   ├── erosion.comp
│   │   │   │   ├── ridged.comp
│   │   │   │   └── warp.comp
│   │   │   ├── textures/
│   │   │   │   ├── heightmap_seeds.bin
│   │   │   │   └── spec/heightmap_spec.h
│   │   │   └── meshes/spec/heightmap_mesh_spec.h
│   │   │
│   │   ├── splatmaps/                                   # Height/slope splat
│   │   │   ├── SplatGenerator.h
│   │   │   ├── SplatGenerator.cpp
│   │   │   ├── SplatParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── splat_height.comp
│   │   │   │   └── splat_slope.comp
│   │   │   ├── textures/spec/splatmap_spec.h
│   │   │   └── meshes/spec/splatmap_mesh_spec.h
│   │   │
│   │   ├── cliffs/                                      # Eroded + modular cliffs
│   │   │   ├── CliffGen.h
│   │   │   ├── CliffGen.cpp
│   │   │   ├── CliffErosion.cpp
│   │   │   ├── CliffModular.cpp
│   │   │   ├── CliffParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/cliff_erosion.comp
│   │   │   ├── textures/spec/cliff_spec.h
│   │   │   └── meshes/profiles/cliff_profile.h
│   │   │
│   │   ├── roads_cut/                                   # Road-cut embankments
│   │   │   ├── RoadCut.h
│   │   │   ├── RoadCut.cpp
│   │   │   ├── RoadCutParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/road_embankment.comp
│   │   │   ├── textures/spec/road_cut_spec.h
│   │   │   └── meshes/spec/road_cut_mesh_spec.h
│   │   │
│   │   ├── caves/                                       # Marching cubes + voxel
│   │   │   ├── CaveGen.h
│   │   │   ├── CaveGen.cpp
│   │   │   ├── MarchingCubes.h
│   │   │   ├── MarchingCubes.cpp
│   │   │   ├── CaveVoxel.cpp
│   │   │   ├── CaveParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── marching_cubes.comp
│   │   │   │   └── cave_voxel.comp
│   │   │   ├── textures/spec/cave_spec.h
│   │   │   └── meshes/spec/cave_mesh_spec.h
│   │   │
│   │   └── meta/                                        # Tile index + LOD
│   │       ├── TileIndex.h
│   │       ├── TileIndex.cpp
│   │       ├── TileLOD.h
│   │       ├── TileLOD.cpp
│   │       ├── README.md
│   │       ├── shaders/tile_select.comp
│   │       ├── textures/spec/tile_meta_spec.h
│   │       └── meshes/spec/tile_meta_mesh_spec.h
│   │
│   ├── water/                                           # FFT ocean, lakes, rivers, waterfalls
│   │   ├── CMakeLists.txt
│   │   ├── WaterSystem.h
│   │   ├── WaterSystem.cpp
│   │   ├── WaterSurface.h                               # kentril0-style water surface
│   │   ├── WaterSurface.cpp
│   │   ├── WaterParams.h                                # Hardcoded wave/FFT/wind constants
│   │   ├── README.md
│   │   ├── shaders/
│   │   │   ├── include/
│   │   │   │   ├── water_common.glsl
│   │   │   │   └── water_types.glsl
│   │   │   ├── water_surface.vert
│   │   │   └── water_surface.frag
│   │   ├── textures/
│   │   │   ├── seeds/water_seed.bin
│   │   │   └── spec/water_common.h
│   │   ├── meshes/
│   │   │   ├── profiles/water_lod.h
│   │   │   └── spec/water_mesh_spec.h
│   │   │
│   │   ├── ocean/                                       # FFT ocean (Tessendorf)
│   │   │   ├── OceanFFT.h
│   │   │   ├── OceanFFT.cpp
│   │   │   ├── FFTSpectrum.h
│   │   │   ├── FFTSpectrum.cpp
│   │   │   ├── ChoppyWave.h
│   │   │   ├── ChoppyWave.cpp
│   │   │   ├── OceanParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── fft_spectrum.comp
│   │   │   │   ├── ifft.comp
│   │   │   │   ├── ocean_choppy.comp
│   │   │   │   └── ocean_normal.comp
│   │   │   ├── textures/
│   │   │   │   ├── ocean_spectrum_spec.bin
│   │   │   │   └── spec/ocean_spec.h
│   │   │   └── meshes/spec/ocean_mesh_spec.h
│   │   │
│   │   ├── lakes/                                       # Still water + ripple
│   │   │   ├── LakeMesh.h
│   │   │   ├── LakeMesh.cpp
│   │   │   ├── LakeRipple.cpp
│   │   │   ├── LakeParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── lake_shore_blend.comp
│   │   │   │   └── lake_ripple.comp
│   │   │   ├── textures/spec/lake_spec.h
│   │   │   └── meshes/spec/lake_mesh_spec.h
│   │   │
│   │   ├── rivers/                                      # Spline rivers + flow
│   │   │   ├── RiverSpline.h
│   │   │   ├── RiverSpline.cpp
│   │   │   ├── RiverFlow.cpp
│   │   │   ├── RiverParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── river_flow.comp
│   │   │   │   └── river_mesh.comp
│   │   │   ├── textures/
│   │   │   │   ├── river_flow_spec.bin
│   │   │   │   └── spec/river_spec.h
│   │   │   └── meshes/
│   │   │       ├── graphs/river_spline.bin
│   │   │       └── spec/river_mesh_spec.h
│   │   │
│   │   ├── waterfalls/                                  # Falls + mist
│   │   │   ├── WaterfallGen.h
│   │   │   ├── WaterfallGen.cpp
│   │   │   ├── WaterfallMist.cpp
│   │   │   ├── WaterfallParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── waterfall_foam.comp
│   │   │   │   └── waterfall_mist.comp
│   │   │   ├── textures/spec/waterfall_spec.h
│   │   │   └── meshes/spec/waterfall_mesh_spec.h
│   │   │
│   │   ├── shore/                                       # Shore foam + wet sand
│   │   │   ├── ShoreFoam.h
│   │   │   ├── ShoreFoam.cpp
│   │   │   ├── WetSand.cpp
│   │   │   ├── ShoreParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── shore_foam.comp
│   │   │   │   └── wet_sand.comp
│   │   │   ├── textures/spec/shore_spec.h
│   │   │   └── meshes/spec/shore_mesh_spec.h
│   │   │
│   │   └── underwater/                                  # Caustics + godrays
│   │       ├── CausticsGen.h
│   │       ├── CausticsGen.cpp
│   │       ├── Godray.cpp
│   │       ├── UnderwaterParams.h
│   │       ├── README.md
│   │       ├── shaders/
│   │       │   ├── caustics.comp
│   │       │   └── godray.comp
│   │       ├── textures/spec/underwater_spec.h
│   │       └── meshes/spec/underwater_mesh_spec.h
│   │
│   ├── vehicles/                                        # Vehicle generators
│   │   ├── CMakeLists.txt
│   │   ├── VehicleSystem.h
│   │   ├── VehicleSystem.cpp
│   │   ├── README.md
│   │   ├── shaders/
│   │   │   ├── include/
│   │   │   │   ├── vehicle_common.glsl
│   │   │   │   └── vehicle_types.glsl
│   │   │   └── vehicle_assembly.comp
│   │   ├── textures/
│   │   │   ├── seeds/vehicle_seed.bin
│   │   │   └── spec/vehicle_common.h
│   │   ├── meshes/
│   │   │   ├── profiles/vehicle_lod.h
│   │   │   └── spec/vehicle_mesh_spec.h
│   │   │
│   │   ├── cars/                                        # Sedans, SUVs, hatchbacks
│   │   │   ├── CarGen.h
│   │   │   ├── CarGen.cpp
│   │   │   ├── Sedan.cpp
│   │   │   ├── SUV.cpp
│   │   │   ├── Hatch.cpp
│   │   │   ├── CarParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/car_body.comp
│   │   │   ├── textures/spec/car_spec.h
│   │   │   └── meshes/profiles/
│   │   │       ├── sedan_silhouette.h
│   │   │       ├── suv_silhouette.h
│   │   │       └── hatch_silhouette.h
│   │   │
│   │   ├── trucks/                                      # Box trucks, semis
│   │   │   ├── TruckGen.h
│   │   │   ├── TruckGen.cpp
│   │   │   ├── BoxTruck.cpp
│   │   │   ├── Semi.cpp
│   │   │   ├── TruckParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/truck_chassis.comp
│   │   │   ├── textures/spec/truck_spec.h
│   │   │   └── meshes/profiles/truck_chassis_profile.h
│   │   │
│   │   ├── bikes/                                       # Bicycles, motorbikes
│   │   │   ├── BikeGen.h
│   │   │   ├── BikeGen.cpp
│   │   │   ├── Bicycle.cpp
│   │   │   ├── Motorbike.cpp
│   │   │   ├── BikeParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/spoke.comp
│   │   │   ├── textures/spec/bike_spec.h
│   │   │   └── meshes/profiles/bike_frame_profile.h
│   │   │
│   │   ├── emergency/                                   # Ambulance, firetruck, police
│   │   │   ├── EmergencyGen.h
│   │   │   ├── EmergencyGen.cpp
│   │   │   ├── Ambulance.cpp
│   │   │   ├── Firetruck.cpp
│   │   │   ├── Police.cpp
│   │   │   ├── EmergencyParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/lightbar.comp
│   │   │   ├── textures/spec/emergency_spec.h
│   │   │   └── meshes/profiles/emergency_body_profile.h
│   │   │
│   │   ├── parts/                                       # Wheels, doors, lights
│   │   │   ├── WheelGen.h
│   │   │   ├── WheelGen.cpp
│   │   │   ├── DoorGen.h
│   │   │   ├── DoorGen.cpp
│   │   │   ├── LightGen.h
│   │   │   ├── LightGen.cpp
│   │   │   ├── VehiclePartParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── wheel.comp
│   │   │   │   ├── wheel_tread.comp
│   │   │   │   └── light_lens.comp
│   │   │   ├── textures/spec/vehicle_part_spec.h
│   │   │   └── meshes/profiles/
│   │   │       ├── wheel_profile.h
│   │   │       └── lens_profile.h
│   │   │
│   │   └── lod/                                         # Car impostors
│   │       ├── CarImpostor.h
│   │       ├── CarImpostor.cpp
│   │       ├── CarLodParams.h
│   │       ├── README.md
│   │       ├── shaders/car_impostor.comp
│   │       ├── textures/spec/car_impostor_spec.h
│   │       └── meshes/spec/car_impostor_mesh_spec.h
│   │
│   ├── props/                                           # World props (street, industrial, urban, rural, interior, energy, utility)
│   │   ├── CMakeLists.txt
│   │   ├── PropSystem.h
│   │   ├── PropSystem.cpp
│   │   ├── README.md
│   │   ├── shaders/
│   │   │   ├── include/
│   │   │   │   ├── prop_common.glsl
│   │   │   │   └── prop_types.glsl
│   │   │   └── prop_placement.comp
│   │   ├── textures/
│   │   │   ├── seeds/prop_seed.bin
│   │   │   └── spec/prop_common.h
│   │   ├── meshes/spec/prop_mesh_spec.h
│   │   │
│   │   ├── street/                                      # Street furniture
│   │   │   ├── LampPostGen.h
│   │   │   ├── LampPostGen.cpp
│   │   │   ├── TrafficLightGen.h
│   │   │   ├── TrafficLightGen.cpp
│   │   │   ├── BenchGen.h
│   │   │   ├── BenchGen.cpp
│   │   │   ├── TrashBinGen.h
│   │   │   ├── TrashBinGen.cpp
│   │   │   ├── StreetPropParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── lamp_post.comp
│   │   │   │   ├── traffic_light.comp
│   │   │   │   └── bench.comp
│   │   │   ├── textures/spec/street_prop_spec.h
│   │   │   └── meshes/profiles/
│   │   │       ├── lamp_post_profile.h
│   │   │       ├── bench_profile.h
│   │   │       └── traffic_light_profile.h
│   │   │
│   │   ├── industrial/                                  # Industrial props
│   │   │   ├── CrateGen.h
│   │   │   ├── CrateGen.cpp
│   │   │   ├── BarrelGen.h
│   │   │   ├── BarrelGen.cpp
│   │   │   ├── PalletGen.h
│   │   │   ├── PalletGen.cpp
│   │   │   ├── PipeGen.h
│   │   │   ├── PipeGen.cpp
│   │   │   ├── IndustrialPropParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── crate.comp
│   │   │   │   ├── barrel.comp
│   │   │   │   └── pipe.comp
│   │   │   ├── textures/spec/industrial_prop_spec.h
│   │   │   └── meshes/profiles/
│   │   │       ├── crate_profile.h
│   │   │       ├── barrel_profile.h
│   │   │       └── pipe_profile.h
│   │   │
│   │   ├── urban/                                       # Urban props
│   │   │   ├── HydrantGen.h
│   │   │   ├── HydrantGen.cpp
│   │   │   ├── MailboxGen.h
│   │   │   ├── MailboxGen.cpp
│   │   │   ├── BusStopGen.h
│   │   │   ├── BusStopGen.cpp
│   │   │   ├── UrbanPropParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── hydrant.comp
│   │   │   │   └── bus_stop.comp
│   │   │   ├── textures/spec/urban_prop_spec.h
│   │   │   └── meshes/profiles/
│   │   │       ├── hydrant_profile.h
│   │   │       └── bus_stop_profile.h
│   │   │
│   │   ├── rural/                                       # Rural props
│   │   │   ├── FenceGen.h
│   │   │   ├── FenceGen.cpp
│   │   │   ├── HayBaleGen.h
│   │   │   ├── HayBaleGen.cpp
│   │   │   ├── WaterTroughGen.h
│   │   │   ├── WaterTroughGen.cpp
│   │   │   ├── RuralPropParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── fence_wood.comp
│   │   │   │   └── hay_bale.comp
│   │   │   ├── textures/spec/rural_prop_spec.h
│   │   │   └── meshes/profiles/
│   │   │       ├── fence_wood_profile.h
│   │   │       └── hay_bale_profile.h
│   │   │
│   │   ├── interior/                                    # Interior furniture
│   │   │   ├── DeskGen.h
│   │   │   ├── DeskGen.cpp
│   │   │   ├── ChairGen.h
│   │   │   ├── ChairGen.cpp
│   │   │   ├── ShelfGen.h
│   │   │   ├── ShelfGen.cpp
│   │   │   ├── InteriorPropParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── desk.comp
│   │   │   │   └── chair.comp
│   │   │   ├── textures/spec/interior_prop_spec.h
│   │   │   └── meshes/profiles/
│   │   │       ├── desk_profile.h
│   │   │       └── chair_profile.h
│   │   │
│   │   ├── utility/                                     # Utility infrastructure (electric grid, water, telecom)
│   │   │   ├── ElectricPostGen.h                        # Wooden/concrete electric posts
│   │   │   ├── ElectricPostGen.cpp
│   │   │   ├── TransformerGen.h                         # Pole-top + pad-mounted transformers
│   │   │   ├── TransformerGen.cpp
│   │   │   ├── PowerMeterGen.h                          # Residential/commercial meters
│   │   │   ├── PowerMeterGen.cpp
│   │   │   ├── StreetLightGen.h                         # Cobra-head street lights
│   │   │   ├── StreetLightGen.cpp
│   │   │   ├── WaterTowerGen.h                          # Elevated water towers
│   │   │   ├── WaterTowerGen.cpp
│   │   │   ├── TelecomTowerGen.h                        # Cell / radio towers
│   │   │   ├── TelecomTowerGen.cpp
│   │   │   ├── UtilityParams.h                          # Pole spacing, wire sag, heights
│   │   │   ├── README.md                                # Utility module docs
│   │   │   ├── shaders/
│   │   │   │   ├── electric_post.comp                   # Post body extrusion
│   │   │   │   ├── wire_catenary.comp                   # Catenary wire generation
│   │   │   │   ├── transformer.comp                     # Transformer body + cooling fins
│   │   │   │   ├── street_light.comp                    # Lamp head + arm
│   │   │   │   ├── water_tower.comp                     # Tank + legs
│   │   │   │   └── telecom_tower.comp                   # Lattice tower
│   │   │   ├── textures/
│   │   │   │   ├── seeds/
│   │   │   │   │   └── utility_seed.bin                 # Utility prop noise seed
│   │   │   │   └── spec/
│   │   │   │       └── utility_spec.h                   # Utility texture spec
│   │   │   └── meshes/
│   │   │       └── profiles/
│   │   │           ├── electric_post_profile.h          # Post cross-section
│   │   │           ├── transformer_profile.h            # Transformer can profile
│   │   │           ├── street_light_arm.h               # Lamp arm curve
│   │   │           └── tower_lattice.h                  # Lattice member profile
│   │   │
│   │   ├── energy/                                      # Renewable + storage energy props
│   │   │   ├── WindTurbineGen.h                         # Horizontal-axis wind turbine
│   │   │   ├── WindTurbineGen.cpp
│   │   │   ├── WindTurbineTower.h                       # Tower + nacelle + hub
│   │   │   ├── WindTurbineTower.cpp
│   │   │   ├── WindTurbineBlades.h                      # Blade airfoil + rotation
│   │   │   ├── WindTurbineBlades.cpp
│   │   │   ├── SolarPanelGen.h                          # Single PV panel
│   │   │   ├── SolarPanelGen.cpp
│   │   │   ├── SolarFarmGen.h                           # Row-arranged PV farm
│   │   │   ├── SolarFarmGen.cpp
│   │   │   ├── SolarTracker.h                           # Sun-tracking mount
│   │   │   ├── SolarTracker.cpp
│   │   │   ├── BatteryBankGen.h                         # Grid-scale battery containers
│   │   │   ├── BatteryBankGen.cpp
│   │   │   ├── SubstationGen.h                          # Step-up/down substation
│   │   │   ├── SubstationGen.cpp
│   │   │   ├── EnergyParams.h                           # Hub height, rotor diameter, panel tilt
│   │   │   ├── README.md                                # Energy module docs
│   │   │   ├── shaders/
│   │   │   │   ├── wind_turbine_tower.comp              # Tubular steel tower taper
│   │   │   │   ├── wind_turbine_blade.comp              # Blade airfoil lofting
│   │   │   │   ├── wind_turbine_rotation.comp           # Runtime blade rotation (compute)
│   │   │   │   ├── solar_panel.comp                     # Frame + cell grid
│   │   │   │   ├── solar_farm_layout.comp               # Row layout + tilt
│   │   │   │   ├── solar_tracker.comp                   # Gimbal + actuator
│   │   │   │   ├── battery_container.comp               # Container + vents
│   │   │   │   └── substation.comp                      # Transformer yard + busbars
│   │   │   ├── textures/
│   │   │   │   ├── seeds/
│   │   │   │   │   └── energy_seed.bin                  # Energy prop noise seed
│   │   │   │   ├── solar_cell_atlas.comp                # PV cell texture atlas
│   │   │   │   └── spec/
│   │   │   │       └── energy_spec.h                    # Energy texture spec
│   │   │   └── meshes/
│   │   │       └── profiles/
│   │   │           ├── wind_blade_airfoil.h             # NACA-style blade profile
│   │   │           ├── turbine_tower_profile.h          # Tower taper profile
│   │   │           ├── solar_panel_frame.h              # Panel frame profile
│   │   │           └── substation_busbar.h              # Busbar cross-section
│   │   │
│   │   ├── communication/                               # Comms + signals (optional)
│   │   │   ├── RadioMastGen.h
│   │   │   ├── RadioMastGen.cpp
│   │   │   ├── SatelliteDishGen.h
│   │   │   ├── SatelliteDishGen.cpp
│   │   │   ├── CommunicationParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── radio_mast.comp
│   │   │   │   └── satellite_dish.comp
│   │   │   ├── textures/spec/communication_spec.h
│   │   │   └── meshes/profiles/
│   │   │       ├── radio_mast_profile.h
│   │   │       └── satellite_dish_profile.h
│   │   │
│   │   └── signage_street/                              # Roadside signage + info boards
│   │       ├── InfoBoardGen.h
│   │       ├── InfoBoardGen.cpp
│   │       ├── KilometreMarkerGen.h
│   │       ├── KilometreMarkerGen.cpp
│   │       ├── SignageStreetParams.h
│   │       ├── README.md
│   │       ├── shaders/
│   │       │   ├── info_board.comp
│   │       │   └── km_marker.comp
│   │       ├── textures/spec/signage_street_spec.h
│   │       └── meshes/profiles/
│   │           ├── info_board_profile.h
│   │           └── km_marker_profile.h
│   │
│   ├── sky/                                             # Sky: atmosphere, stars, sun, moon, LUT
│   │   ├── CMakeLists.txt
│   │   ├── SkySystem.h
│   │   ├── SkySystem.cpp
│   │   ├── README.md
│   │   ├── shaders/
│   │   │   ├── include/
│   │   │   │   └── sky_common.glsl
│   │   │   ├── sky.vert
│   │   │   └── sky.frag
│   │   ├── textures/
│   │   │   ├── seeds/sky_seed.bin
│   │   │   └── spec/sky_common.h
│   │   ├── meshes/spec/sky_dome_spec.h
│   │   │
│   │   ├── atmosphere/                                  # Preetham + Hosek-Wilkie
│   │   │   ├── PreethamModel.h
│   │   │   ├── PreethamModel.cpp
│   │   │   ├── HosekWilkie.h
│   │   │   ├── HosekWilkie.cpp
│   │   │   ├── AtmosphereScatter.h
│   │   │   ├── AtmosphereScatter.cpp
│   │   │   ├── AtmosphereParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── sky_lut.comp
│   │   │   │   └── atmosphere_scatter.comp
│   │   │   ├── textures/
│   │   │   │   ├── sky_lut_spec.bin
│   │   │   │   └── spec/atmosphere_spec.h
│   │   │   └── meshes/spec/atmosphere_mesh_spec.h
│   │   │
│   │   ├── stars/                                       # Starfield + twinkle
│   │   │   ├── StarfieldGen.h
│   │   │   ├── StarfieldGen.cpp
│   │   │   ├── StarTwinkle.cpp
│   │   │   ├── StarParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── star_hash.comp
│   │   │   │   └── star_twinkle.comp
│   │   │   ├── textures/
│   │   │   │   ├── star_catalog.bin
│   │   │   │   └── spec/star_spec.h
│   │   │   └── meshes/spec/star_mesh_spec.h
│   │   │
│   │   ├── sun/                                         # Sun disk + glow
│   │   │   ├── SunDisk.h
│   │   │   ├── SunDisk.cpp
│   │   │   ├── SunGlow.cpp
│   │   │   ├── SunParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/sun_glow.comp
│   │   │   ├── textures/
│   │   │   │   ├── sun_disk_spec.bin
│   │   │   │   └── spec/sun_spec.h
│   │   │   └── meshes/spec/sun_mesh_spec.h
│   │   │
│   │   ├── moon/                                        # Moon phase + disk
│   │   │   ├── MoonPhaseGen.h
│   │   │   ├── MoonPhaseGen.cpp
│   │   │   ├── MoonDisk.cpp
│   │   │   ├── MoonParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/moon_disk.comp
│   │   │   ├── textures/
│   │   │   │   ├── moon_phase_spec.bin
│   │   │   │   └── spec/moon_spec.h
│   │   │   └── meshes/spec/moon_mesh_spec.h
│   │   │
│   │   └── lut/                                         # Tonemap + color grade
│   │       ├── TonemapLUT.h
│   │       ├── TonemapLUT.cpp
│   │       ├── ColorGrade.cpp
│   │       ├── TonemapParams.h
│   │       ├── README.md
│   │       ├── shaders/
│   │       │   ├── tonemap.comp
│   │       │   └── color_grade.comp
│   │       ├── textures/
│   │       │   ├── lut_specs/
│   │       │   │   ├── aces.bin
│   │       │   │   └── reinhard.bin
│   │       │   └── spec/tonemap_spec.h
│   │       └── meshes/spec/lut_mesh_spec.h
│   │
│   ├── particles/                                       # GPU particle systems
│   │   ├── CMakeLists.txt
│   │   ├── ParticleSystem.h
│   │   ├── ParticleSystem.cpp
│   │   ├── README.md
│   │   ├── shaders/
│   │   │   ├── include/
│   │   │   │   └── particle_common.glsl
│   │   │   └── particle_sort.comp
│   │   ├── textures/
│   │   │   ├── seeds/particle_seed.bin
│   │   │   └── spec/particle_common.h
│   │   ├── meshes/spec/particle_mesh_spec.h
│   │   │
│   │   ├── fire/                                        # Fire sim + flame sheet
│   │   │   ├── FireParticle.h
│   │   │   ├── FireParticle.cpp
│   │   │   ├── FlameSheet.cpp
│   │   │   ├── FireParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── fire_sim.comp
│   │   │   │   └── flame_sheet.comp
│   │   │   ├── textures/
│   │   │   │   ├── flame_atlas.comp
│   │   │   │   └── spec/fire_spec.h
│   │   │   └── meshes/spec/fire_mesh_spec.h
│   │   │
│   │   ├── smoke/                                       # Smoke curl + puff
│   │   │   ├── SmokeSystem.h
│   │   │   ├── SmokeSystem.cpp
│   │   │   ├── SmokeCurl.cpp
│   │   │   ├── SmokePuff.cpp
│   │   │   ├── SmokeParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── smoke_curl.comp
│   │   │   │   └── smoke_puff.comp
│   │   │   ├── textures/
│   │   │   │   ├── smoke_atlas.comp
│   │   │   │   └── spec/smoke_spec.h
│   │   │   └── meshes/spec/smoke_mesh_spec.h
│   │   │
│   │   ├── dust/                                        # Dust kickup
│   │   │   ├── DustKickup.h
│   │   │   ├── DustKickup.cpp
│   │   │   ├── DustParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/dust_sim.comp
│   │   │   ├── textures/spec/dust_spec.h
│   │   │   └── meshes/spec/dust_mesh_spec.h
│   │   │
│   │   ├── sparks/                                      # Sparks
│   │   │   ├── SparkSystem.h
│   │   │   ├── SparkSystem.cpp
│   │   │   ├── SparkParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/spark_sim.comp
│   │   │   ├── textures/spec/spark_spec.h
│   │   │   └── meshes/spec/spark_mesh_spec.h
│   │   │
│   │   ├── explosions/                                  # Shockwave + explosion sim
│   │   │   ├── ExplosionGen.h
│   │   │   ├── ExplosionGen.cpp
│   │   │   ├── Shockwave.cpp
│   │   │   ├── ExplosionParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── shockwave.comp
│   │   │   │   └── explosion_sim.comp
│   │   │   ├── textures/spec/explosion_spec.h
│   │   │   └── meshes/spec/explosion_mesh_spec.h
│   │   │
│   │   └── vfx_graphs/                                  # Node-graph VFX
│   │       ├── VfxGraph.h
│   │       ├── VfxGraph.cpp
│   │       ├── FxRainSplash.cpp
│   │       ├── FxCampfire.cpp
│   │       ├── VfxParams.h
│   │       ├── README.md
│   │       ├── shaders/vfx_evaluate.comp
│   │       ├── textures/spec/vfx_spec.h
│   │       └── meshes/spec/vfx_mesh_spec.h
│   │
│   ├── audio/                                           # Procedural audio synthesis
│   │   ├── CMakeLists.txt
│   │   ├── AudioSystem.h
│   │   ├── AudioSystem.cpp
│   │   ├── README.md
│   │   ├── shaders/
│   │   │   ├── include/
│   │   │   │   └── audio_common.glsl
│   │   │   └── audio_mix.comp
│   │   ├── textures/spec/audio_common.h
│   │   ├── meshes/spec/audio_mesh_spec.h
│   │   │
│   │   ├── ambient/                                     # Forest/city/rain ambience
│   │   │   ├── ForestAmbience.h
│   │   │   ├── ForestAmbience.cpp
│   │   │   ├── CityAmbience.h
│   │   │   ├── CityAmbience.cpp
│   │   │   ├── RainLoop.cpp
│   │   │   ├── AmbientParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/ambient_synth.comp
│   │   │   ├── textures/spec/ambient_spec.h
│   │   │   └── meshes/spec/ambient_mesh_spec.h
│   │   │
│   │   ├── footsteps/                                   # Footstep synth per surface
│   │   │   ├── FootstepSynthesizer.h
│   │   │   ├── FootstepSynthesizer.cpp
│   │   │   ├── StepGrass.cpp
│   │   │   ├── StepConcrete.cpp
│   │   │   ├── StepWood.cpp
│   │   │   ├── FootstepParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/footstep_synth.comp
│   │   │   ├── textures/spec/footstep_spec.h
│   │   │   └── meshes/spec/footstep_mesh_spec.h
│   │   │
│   │   ├── vehicles/                                    # Engine oscillator synth
│   │   │   ├── EngineSynth.h
│   │   │   ├── EngineSynth.cpp
│   │   │   ├── EngineParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/engine_osc.comp
│   │   │   ├── textures/spec/engine_spec.h
│   │   │   └── meshes/spec/engine_mesh_spec.h
│   │   │
│   │   ├── ui/                                          # UI clicks/hovers
│   │   │   ├── UIClickGen.h
│   │   │   ├── UIClickGen.cpp
│   │   │   ├── UIHover.cpp
│   │   │   ├── UIAudioParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/ui_click_synth.comp
│   │   │   ├── textures/spec/ui_audio_spec.h
│   │   │   └── meshes/spec/ui_audio_mesh_spec.h
│   │   │
│   │   └── music/                                       # Procedural music themes
│   │       ├── MusicTheme.h
│   │       ├── MusicTheme.cpp
│   │       ├── ThemeCalm.cpp
│   │       ├── MusicParams.h
│   │       ├── README.md
│   │       ├── shaders/theme_synth.comp
│   │       ├── textures/spec/music_spec.h
│   │       └── meshes/spec/music_mesh_spec.h
│   │
│   ├── ui/                                              # UI: fonts, icons, atlases, layouts
│   │   ├── CMakeLists.txt
│   │   ├── UISystem.h
│   │   ├── UISystem.cpp
│   │   ├── Gui.h                                        # kentril0-style immediate-mode GUI
│   │   ├── Gui.cpp
│   │   ├── README.md
│   │   ├── shaders/
│   │   │   ├── include/ui_common.glsl
│   │   │   ├── ui_quad.vert
│   │   │   ├── ui_quad.frag
│   │   │   ├── ui_text.vert
│   │   │   ├── ui_text.frag
│   │   │   └── ui_blur.comp
│   │   ├── textures/
│   │   │   ├── seeds/ui_seed.bin
│   │   │   └── spec/ui_common.h
│   │   ├── meshes/spec/ui_quad_spec.h
│   │   │
│   │   ├── fonts/                                       # SDF font atlases
│   │   │   ├── FontAtlasGen.h
│   │   │   ├── FontAtlasGen.cpp
│   │   │   ├── FontSDF.cpp
│   │   │   ├── FontParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── font_sdf.comp
│   │   │   │   └── font_rasterize.comp
│   │   │   ├── textures/
│   │   │   │   ├── fonts/
│   │   │   │   │   ├── inter.ttf                        # Source TTF (rasterized at runtime)
│   │   │   │   │   └── mono.ttf
│   │   │   │   └── spec/font_spec.h
│   │   │   └── meshes/spec/font_mesh_spec.h
│   │   │
│   │   ├── icons/                                       # SDF icon atlases
│   │   │   ├── IconGenerator.h
│   │   │   ├── IconGenerator.cpp
│   │   │   ├── IconSDF.cpp
│   │   │   ├── IconParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/
│   │   │   │   ├── icon_sdf.comp
│   │   │   │   └── icon_atlas.comp
│   │   │   ├── textures/
│   │   │   │   ├── icons/
│   │   │   │   │   ├── settings.svg
│   │   │   │   │   └── pause.svg
│   │   │   │   └── spec/icon_spec.h
│   │   │   └── meshes/spec/icon_mesh_spec.h
│   │   │
│   │   ├── atlases/                                     # UI atlas packer
│   │   │   ├── UIAtlas.h
│   │   │   ├── UIAtlas.cpp
│   │   │   ├── AtlasPack.cpp
│   │   │   ├── AtlasParams.h
│   │   │   ├── README.md
│   │   │   ├── shaders/atlas_pack.comp
│   │   │   ├── textures/spec/atlas_spec.h
│   │   │   └── meshes/spec/atlas_mesh_spec.h
│   │   │
│   │   └── layouts/                                     # HUD + menu layouts
│   │       ├── HudLayout.h
│   │       ├── HudLayout.cpp
│   │       ├── PauseMenu.cpp
│   │       ├── LayoutParser.cpp
│   │       ├── LayoutDefaults.h
│   │       ├── README.md
│   │       ├── shaders/layout_quad.comp
│   │       ├── textures/spec/layout_spec.h
│   │       └── meshes/spec/layout_mesh_spec.h
│   │
│   └── shared/                                          # Cross-category helpers
│       ├── CMakeLists.txt
│       ├── IGenerator.h                                 # Generator interface (Generate/Name)
│       ├── GeneratorRegistry.h                          # Registry of all generators
│       ├── GeneratorRegistry.cpp
│       ├── GeneratorContext.h                           # VkDevice, VkCommandBuffer, allocator
│       ├── GeneratorContext.cpp
│       ├── GpuTypes.h                                   # Shared GPU structs
│       ├── GpuTypes.cpp
│       ├── Seed.h                                       # Deterministic seed mixer
│       ├── Seed.cpp
│       ├── README.md
│       ├── shaders/
│       │   ├── ShaderCache.h
│       │   ├── ShaderCache.cpp
│       │   └── README.md
│       ├── pipelines/
│       │   ├── PipelineCache.h
│       │   ├── PipelineCache.cpp
│       │   ├── PipelineBuilder.h
│       │   ├── PipelineBuilder.cpp
│       │   └── README.md
│       ├── materials/
│       │   ├── MaterialDefs.h
│       │   ├── MaterialDefs.cpp
│       │   ├── MaterialRegistry.h
│       │   ├── MaterialRegistry.cpp
│       │   ├── MaterialLibrary.cpp
│       │   └── README.md
│       ├── noise/
│       │   ├── NoiseLib.h
│       │   ├── NoiseLib.cpp
│       │   ├── NoiseBlue.h
│       │   ├── NoiseBlue.cpp
│       │   ├── NoiseWorley.cpp
│       │   ├── NoiseSimplex.cpp
│       │   ├── NoiseCurl.cpp
│       │   └── README.md
│       ├── math/
│       │   ├── SplineLib.h
│       │   ├── SplineLib.cpp
│       │   ├── FrustumCull.h
│       │   ├── FrustumCull.cpp
│       │   ├── HiZ.h
│       │   ├── HiZ.cpp
│       │   ├── HizBuild.cpp
│       │   ├── HizCull.cpp
│       │   └── README.md
│       ├── physics/
│       │   ├── CollisionGroups.h
│       │   ├── CollisionGroups.cpp
│       │   ├── PhysicsWorld.h
│       │   ├── PhysicsWorld.cpp
│       │   ├── PhysicsDebug.cpp
│       │   └── README.md
│       ├── skeletons/
│       │   ├── SkeletonGen.h
│       │   ├── SkeletonGen.cpp
│       │   ├── RigShared.cpp
│       │   └── README.md
│       ├── memory/
│       │   ├── StreamingPool.h
│       │   ├── StreamingPool.cpp
│       │   ├── LruCache.h
│       │   ├── DeviceMemoryAllocator.h
│       │   ├── DeviceMemoryAllocator.cpp
│       │   └── README.md
│       ├── bindless/
│       │   ├── BindlessHeap.h
│       │   ├── BindlessHeap.cpp
│       │   ├── DescriptorIndexing.h
│       │   ├── DescriptorIndexing.cpp
│       │   └── README.md
│       └── debug/
│           ├── DebugDraw.h
│           ├── DebugDraw.cpp
│           ├── DebugText.h
│           ├── DebugText.cpp
│           └── README.md
│
├── shaders/                                             # ROOT = shared across 2+ categories ONLY
│   ├── CMakeLists.txt                                   # Compiles root shaders to SPIR-V
│   ├── README.md                                        # Explains sharing rule
│   └── glsl/
│       ├── include/
│       │   ├── bindless.glsl                            # Bindless descriptor indexing helpers
│       │   ├── pbr_common.glsl                          # Shared PBR BRDF + IBL
│       │   ├── noise_common.glsl                        # Shared noise functions
│       │   ├── math_common.glsl                         # Shared math helpers
│       │   ├── atmosphere_common.glsl                   # Shared atmosphere scattering
│       │   └── vulkan_compat.glsl                       # Vulkan 1.3 compat macros
│       ├── pbr.vert                                     # Global PBR vertex shader
│       ├── pbr.frag                                     # Global PBR fragment shader
│       ├── shadow.vert                                  # Shadow map vertex shader
│       ├── shadow.frag                                  # Shadow map fragment shader
│       ├── sky.vert                                     # Sky dome vertex shader
│       ├── sky.comp                                     # Sky LUT compute
│       ├── cull.comp                                    # Global frustum/HiZ cull
│       ├── meshlet.comp                                 # Global meshlet cluster cull
│       └── debug/
│           ├── debug_line.vert                          # Debug line vertex shader
│           ├── debug_line.frag                          # Debug line fragment shader
│           └── debug_text.frag                          # Debug text fragment shader
│
├── textures/                                            # ROOT = shared texture inputs ONLY
│   ├── README.md                                        # Explains sharing rule
│   ├── seeds/
│   │   └── shared_seed.bin                              # Cross-category seed
│   ├── sdf/
│   │   └── shared_sdf.sdf                               # Cross-category SDF
│   └── spec/
│       └── shared_spec.h                                # Cross-category spec
│
├── meshes/                                              # ROOT = shared mesh inputs ONLY
│   ├── README.md                                        # Explains sharing rule
│   ├── profiles/
│   │   └── shared_profile.h                             # Cross-category profile
│   ├── grammars/
│   │   └── shared.lsys                                  # Cross-category grammar
│   ├── graphs/
│   │   └── shared_graph.bin                             # Cross-category graph
│   └── spec/
│       └── shared_mesh_spec.h                           # Cross-category mesh spec
│
└── tests/                                               # All unit + determinism tests
    ├── CMakeLists.txt                                   # Builds all test executables
    ├── README.md                                        # Test tree overview
    ├── test_determinism.cpp                             # Seed + params → byte-identical output
    ├── test_registry.cpp                                # Generator registry integrity
    ├── road/
    │   ├── test_road_network.cpp
    │   ├── test_road_spline_graph.cpp
    │   ├── test_asphalt.cpp
    │   ├── test_markings.cpp
    │   ├── test_signs.cpp
    │   ├── test_barriers.cpp
    │   ├── test_intersections.cpp
    │   ├── test_bridges.cpp
    │   ├── test_tunnels.cpp
    │   ├── test_road_tiles.cpp
    │   └── states/
    │       ├── test_muddy.cpp
    │       ├── test_rocky.cpp
    │       ├── test_bumpy.cpp
    │       ├── test_sandy.cpp
    │       ├── test_gravel.cpp
    │       ├── test_snowy.cpp
    │       ├── test_icy.cpp
    │       ├── test_wet.cpp
    │       ├── test_dry_cracked.cpp
    │       └── test_potholed.cpp
    ├── vegetation/
    │   ├── test_trees.cpp
    │   ├── test_tree_grammar.cpp
    │   ├── test_bushes.cpp
    │   ├── test_grass.cpp
    │   ├── test_grass_sim.cpp
    │   ├── test_flowers.cpp
    │   ├── test_crops.cpp
    │   ├── test_rocks.cpp
    │   ├── test_rock_erosion.cpp
    │   ├── test_debris.cpp
    │   ├── test_biomes.cpp
    │   └── test_wind_field.cpp
    ├── surfaces/
    │   ├── test_ground.cpp
    │   ├── test_concrete.cpp
    │   ├── test_metal.cpp
    │   ├── test_wood.cpp
    │   ├── test_tiles.cpp
    │   ├── test_fabric.cpp
    │   ├── test_glass.cpp
    │   ├── test_decals.cpp
    │   ├── test_splat.cpp
    │   └── test_detail.cpp
    ├── buildings/
    │   ├── test_residential.cpp
    │   ├── test_commercial.cpp
    │   ├── test_industrial.cpp
    │   ├── test_civic.cpp
    │   ├── test_parts.cpp
    │   ├── test_interiors.cpp
    │   ├── test_building_props.cpp
    │   ├── test_signage.cpp
    │   ├── test_damage.cpp
    │   └── test_building_lod.cpp
    ├── weather/
    │   ├── test_rain.cpp
    │   ├── test_snow.cpp
    │   ├── test_fog.cpp
    │   ├── test_clouds.cpp
    │   ├── test_storms.cpp
    │   ├── test_global_wind.cpp
    │   ├── test_lightning.cpp
    │   ├── test_puddles.cpp
    │   ├── test_weather_sky.cpp
    │   └── test_presets.cpp
    ├── terrain/
    │   ├── test_heightmap.cpp
    │   ├── test_fbm.cpp
    │   ├── test_erosion.cpp
    │   ├── test_splatmaps.cpp
    │   ├── test_cliffs.cpp
    │   ├── test_road_cut.cpp
    │   ├── test_caves.cpp
    │   └── test_tile_index.cpp
    ├── water/
    │   ├── test_ocean_fft.cpp
    │   ├── test_choppy.cpp
    │   ├── test_lakes.cpp
    │   ├── test_rivers.cpp
    │   ├── test_waterfalls.cpp
    │   ├── test_shore.cpp
    │   └── test_underwater.cpp
    ├── vehicles/
    │   ├── test_cars.cpp
    │   ├── test_trucks.cpp
    │   ├── test_bikes.cpp
    │   ├── test_emergency.cpp
    │   ├── test_vehicle_parts.cpp
    │   └── test_car_lod.cpp
    ├── props/
    │   ├── test_street_props.cpp
    │   ├── test_industrial_props.cpp
    │   ├── test_urban_props.cpp
    │   ├── test_rural_props.cpp
    │   ├── test_interior_props.cpp
    │   ├── test_utility_props.cpp                       # electric post, transformer, etc.
    │   ├── test_energy_props.cpp                        # wind turbine, solar panels, etc.
    │   ├── test_communication_props.cpp                 # radio mast, satellite dish
    │   └── test_signage_street_props.cpp                # info board, km marker
    ├── sky/
    │   ├── test_atmosphere.cpp
    │   ├── test_stars.cpp
    │   ├── test_sun.cpp
    │   ├── test_moon.cpp
    │   └── test_tonemap.cpp
    ├── particles/
    │   ├── test_fire.cpp
    │   ├── test_smoke.cpp
    │   ├── test_dust.cpp
    │   ├── test_sparks.cpp
    │   ├── test_explosions.cpp
    │   └── test_vfx_graphs.cpp
    ├── audio/
    │   ├── test_ambient.cpp
    │   ├── test_footsteps.cpp
    │   ├── test_engine.cpp
    │   ├── test_ui_audio.cpp
    │   └── test_music.cpp
    └── ui/
        ├── test_fonts.cpp
        ├── test_icons.cpp
        ├── test_atlas.cpp
        └── test_layouts.cpp
```

---

## What Each Top-Level Folder Does

| Folder | Purpose |
| :--- | :--- |
| `native-assets/` | Root of the procedural generator workspace |
| `base/` | SaschaWillems-style Vulkan primitives (buffers, textures, device, swapchain) shared by every subsystem |
| `src/core/` | Application lifecycle: window, main loop, logging, profiling, timers |
| `src/vulkan/` | kentril0-style thin Vulkan wrappers around every object used at runtime |
| `src/scene/` | Scene graph, camera, transform hierarchy, mesh/material descriptors, lights |
| `src/road/` | Road network, splines, asphalt, markings, signs, barriers, bridges, tunnels, surface states |
| `src/vegetation/` | Trees, bushes, grass, flowers, crops, rocks, debris, biomes, wind |
| `src/surfaces/` | PBR generators: ground, concrete, metal, wood, tiles, fabric, glass, decals, splat, detail |
| `src/buildings/` | Residential, commercial, industrial, civic buildings, parts kit, interiors, signage, damage, LOD |
| `src/weather/` | Rain, snow, fog, clouds, storms, wind, lightning, puddles, sky presets |
| `src/terrain/` | Heightmaps, splatmaps, cliffs, road cuts, caves, tile metadata |
| `src/water/` | FFT ocean, lakes, rivers, waterfalls, shore, underwater caustics |
| `src/vehicles/` | Cars, trucks, bikes, emergency vehicles, parts, LODs |
| `src/props/` | Street, industrial, urban, rural, interior, utility, energy, communication, signage |
| `src/sky/` | Atmosphere, stars, sun, moon, tonemap LUT |
| `src/particles/` | Fire, smoke, dust, sparks, explosions, VFX graphs |
| `src/audio/` | Ambient, footsteps, engine, UI, music synthesis |
| `src/ui/` | Fonts, icons, atlases, HUD/menu layouts |
| `src/shared/` | IGenerator interface, registry, bindless heap, noise, math, physics, memory, debug |
| `shaders/glsl/` | Only cross-category shared shaders (pbr, shadow, sky, cull, meshlet) |
| `textures/` | Only cross-category shared texture inputs |
| `meshes/` | Only cross-category shared mesh inputs |
| `tests/` | GoogleTest suites mirroring src/ structure per category, plus test_determinism.cpp and test_registry.cpp |

---

## What Road States Add

Each folder under `src/road/states/` describes a surface condition consumed by:

- **Physics** (`SlipFactor`, `Roughness` constants in `*Params.h`)
- **Renderer** (albedo/normal/ORM overrides)
- **Weather system** (wetness ↔ `WetState`, snow ↔ `SnowyState`)
- **Vehicle dynamics** (tire grip sampling)
- **Audio** (`FootstepSynthesizer`, `EngineSynth` surface filters)

| State | Meaning | Key Parameters |
| :--- | :--- | :--- |
| `muddy/` | Wet clay/dirt surface with ruts | slip factor, rut depth, puddle blend |
| `rocky/` | Unpaved, scattered rock surface | rock density, size dist, dust factor |
| `bumpy/` | Washboard / heavy undulation | amplitude, frequency, seed |
| `sandy/` | Loose dry or damp sand | grain size, dune scale, sink rate |
| `gravel/` | Loose gravel road | pebble density, angularity, dust |
| `snowy/` | Snow-covered (packed or powder) | depth, track pattern, salt coverage |
| `icy/` | Frozen/glazed road | ice thickness, crack density, gloss |
| `wet/` | Post-rain wet film | film thickness, mirror blur, splash rate |
| `dry_cracked/` | Drought cracked surface | crack density, width, joint pattern |
| `potholed/` | Advanced degradation | pothole density, size, edge roughness |

---

## What Energy / Utility Props Add

| Subfolder | Props | Use |
| :--- | :--- | :--- |
| `props/utility/` | Electric post, transformer, power meter, street light, water tower, telecom tower | Residential/commercial power grids, water distribution |
| `props/energy/` | Wind turbine (tower/nacelle/blades), solar panel, solar farm, solar tracker, battery bank, substation | Renewable generation + grid storage |
| `props/communication/` | Radio mast, satellite dish | Broadcast + telecom |
| `props/signage_street/` | Info board, kilometre marker | Roadside information |

Each is a fully procedural generator (compute shader driven), so wind turbines animate via `wind_turbine_rotation.comp`, solar trackers orient via `solar_tracker.comp` from the sun vector in `WorldState`, and electric post wires use `wire_catenary.comp` tied to the pole sockets.

---

## Next Implementation Steps

1. A concrete `WindTurbineBlades.cpp` + `wind_turbine_blade.comp` implementing the airfoil loft.
2. A concrete `MuddyState.cpp` + `mud_albedo.comp` implementing the road state.
3. The `CMakeLists.txt` per category that compiles shaders with correct include paths scoped to their co-located module.
