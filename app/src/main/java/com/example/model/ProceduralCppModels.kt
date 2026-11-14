package com.example.model

enum class CppAssetType(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryColorHex: String,
    val accentColorHex: String,
    val category: String,
    val dimensionsLabel: String
) {
    GARAGE_001(
        id = "garage_001",
        title = "Garage House (Modular)",
        subtitle = "Standalone Workshop with Modular Door & Interior",
        primaryColorHex = "#384252",
        accentColorHex = "#D93838",
        category = "Architecture / Workshop",
        dimensionsLabel = "12.4m (W) × 5.8m (H) × 16.4m (L)"
    ),
    TEST_VEHICLE_LORRY(
        id = "test_vehicle_lorry",
        title = "Test Vehicle (Box Lorry)",
        subtitle = "Proving Ground 6-DOF Test Truck",
        primaryColorHex = "#00BCD4",
        accentColorHex = "#EBF0F5",
        category = "Vehicle Dynamics / Hauler",
        dimensionsLabel = "2.2m (W) × 3.3m (H) × 5.8m (L)"
    ),
    MODULAR_POWER_POST(
        id = "modular_power_post",
        title = "Modular Electric Post",
        subtitle = "Single Unit Configuration Deck",
        primaryColorHex = "#FFB300",
        accentColorHex = "#00E5FF",
        category = "Infrastructure / Electric Grid",
        dimensionsLabel = "2.4m (W) × 11.2m (H) × 4.6m (L)"
    ),
    POWER_GRID_NETWORK(
        id = "power_grid_network",
        title = "Power Grid (Corner Route)",
        subtitle = "3–4 Connected Posts with Angle Turn & Sag Cables",
        primaryColorHex = "#00E5FF",
        accentColorHex = "#FFD600",
        category = "Transmission Grid / 3-Phase",
        dimensionsLabel = "38m (W) × 11.2m (H) × 24m (L)"
    ),
    MODULAR_BRIDGE(
        id = "modular_bridge",
        title = "Modular Bridge Architecture",
        subtitle = "12 Architectural Types: Suspension, Foot Bridge, Cable-Stayed, Extradosed, Bascule & More",
        primaryColorHex = "#00E5FF",
        accentColorHex = "#FF9100",
        category = "Civil Infrastructure / Bridges",
        dimensionsLabel = "24m (W) × 65m (H) × 160m (L)"
    ),
    MODULAR_TOLL_PLAZA(
        id = "modular_toll_plaza",
        title = "Modular Toll Plaza System",
        subtitle = "Aerodynamic Spatial Canopy, Multi-Lane Islands & ETC Gantry",
        primaryColorHex = "#FFD600",
        accentColorHex = "#00E676",
        category = "Highway Infrastructure / Tolling",
        dimensionsLabel = "32m (W) × 9.5m (H) × 64m (L)"
    ),
    MODULAR_TUNNEL(
        id = "modular_tunnel",
        title = "Modular Tunnel Engineering",
        subtitle = "NATM Mountain Horseshoe, Circular TBM, Jet Fans & Flared Portal",
        primaryColorHex = "#7C4DFF",
        accentColorHex = "#00E5FF",
        category = "Subterranean / Road Tunnels",
        dimensionsLabel = "14m (W) × 12m (H) × 120m (L)"
    ),
    MODULAR_INTERCHANGE(
        id = "modular_interchange",
        title = "Interchange & Curved Ramp",
        subtitle = "270° Cloverleaf Loop, Directional Flyover & Modern Roundabout",
        primaryColorHex = "#FF5252",
        accentColorHex = "#FFD600",
        category = "Highway Geometry / Ramps",
        dimensionsLabel = "110m (W) × 12m (H) × 110m (L)"
    ),
    MODULAR_RETAINING_WALL(
        id = "modular_retaining_wall",
        title = "Highway Retaining Wall",
        subtitle = "Mechanically Stabilized Earth (MSE) Panels & Stepped Rock Gabions",
        primaryColorHex = "#78909C",
        accentColorHex = "#00E5FF",
        category = "Geotechnical / Earth Retaining",
        dimensionsLabel = "5.5m (W) × 7.5m (H) × 60m (L)"
    ),
    CONNECTED_HIGHWAY_SCENE(
        id = "connected_highway_scene",
        title = "Vulkan Connected Highway Scene",
        subtitle = "Complete Real-World Corridor: Toll Plaza -> Bridge -> Mountain Tunnel",
        primaryColorHex = "#00E676",
        accentColorHex = "#00E5FF",
        category = "Vulkan Scene / Highway System",
        dimensionsLabel = "48m (W) × 85m (H) × 480m (L)"
    ),
    MODULAR_FOOTBRIDGE(
        id = "modular_footbridge",
        title = "Pedestrian Footbridge System",
        subtitle = "Arched Spine, Stress-Ribbon, Cable Promenade, Timber Arch & Covered Skywalk",
        primaryColorHex = "#00E5FF",
        accentColorHex = "#FFB300",
        category = "Civil Infrastructure / Footbridges",
        dimensionsLabel = "5.4m (W) × 18m (H) × 80m (L)"
    ),
    MODULAR_EXPRESSWAY(
        id = "modular_expressway",
        title = "Multi-Lane Expressway Corridor",
        subtitle = "Divided Freeway, Elevated Viaduct, Gore Taper, EV Corridor & Mountain Cut",
        primaryColorHex = "#26A69A",
        accentColorHex = "#FFD600",
        category = "Highway Infrastructure / Expressways",
        dimensionsLabel = "32m (W) × 9m (H) × 120m (L)"
    ),
    MODULAR_SIGNPOST(
        id = "modular_signpost",
        title = "Highway Signposts & Overhead Gantries",
        subtitle = "Cantilever Truss, Full-Span Warren Gantry, LED DMS Matrix, Chevrons & Butterfly",
        primaryColorHex = "#2E7D32",
        accentColorHex = "#00E676",
        category = "Traffic Control / MUTCD Signage",
        dimensionsLabel = "20m (W) × 9.5m (H) × 18m (L)"
    ),
    MODULAR_BARRIER(
        id = "modular_barrier",
        title = "Safety Barriers & Crash Restraints",
        subtitle = "F-Shape Jersey Concrete, W-Beam Guardrail, High-Tension Cable, Noise Wall & Rollers",
        primaryColorHex = "#FFB300",
        accentColorHex = "#ECEFF1",
        category = "MASH 2016 / Crash Safety Barriers",
        dimensionsLabel = "4.2m (W) × 2.5m (H) × 48m (L)"
    ),
    MODULAR_ROAD_BLOCK(
        id = "modular_road_block",
        title = "Road Blocks & Workzone Security",
        subtitle = "Water Jersey Line, K12 Hydraulic Bollards, Spike Chicane, Drum Taper & Dragon Teeth",
        primaryColorHex = "#FF5722",
        accentColorHex = "#FFD600",
        category = "Temporary Control / Anti-Ram Checkpoints",
        dimensionsLabel = "12m (W) × 3.2m (H) × 34m (L)"
    )
}

data class ProceduralBoxPart(
    val id: String,
    val name: String,
    val centerX: Float,
    val centerY: Float,
    val centerZ: Float,
    val sizeX: Float,
    val sizeY: Float,
    val sizeZ: Float,
    val colorHex: String,
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float = 1.0f,
    val category: String = "Structure"
)

data class ProceduralWireCable(
    val id: String,
    val name: String,
    val x1: Float, val y1: Float, val z1: Float,
    val x2: Float, val y2: Float, val z2: Float,
    val sag: Float = 0.55f,
    val colorHex: String = "#37474F",
    val r: Float = 0.22f, val g: Float = 0.25f, val b: Float = 0.30f,
    val isHotPhase: Boolean = true,
    val radiusM: Float = 0.035f,
    val tensionKn: Float = 18.5f,
    val linearMassKgM: Float = 1.25f,
    val catenaryConstantA: Float = 1508.0f,
    val useCylinder: Boolean = true,
    val cylinderSides: Int = 8
)

data class NativeVulkanMesh(
    val assetType: CppAssetType = CppAssetType.MODULAR_BRIDGE,
    val vertices: FloatArray, // 12 floats per vertex: pos[3], normal[3], uv[2], color[4]
    val indices: IntArray,
    val vertexCount: Int = vertices.size / 12,
    val triangleCount: Int = indices.size / 3,
    val boundingBoxMin: List<Float> = listOf(-25f, 0f, -80f),
    val boundingBoxMax: List<Float> = listOf(25f, 65f, 80f),
    val totalVolumeM3: Float = 1420f,
    val structuralSteelTons: Float = 380f,
    val deadLoadKn: Float = 34800f,
    val civilStandard: String = "AASHTO LRFD Bridge Design Specs (9th Edition) / Eurocode EN 1991-2",
    val title: String = "Vulkan Modular Road Component"
)

data class ProceduralModelMesh(
    val assetType: CppAssetType,
    val boxes: List<ProceduralBoxPart> = emptyList(),
    val totalVertices: Int,
    val totalTriangles: Int,
    val boundingBoxMin: List<Float>,
    val boundingBoxMax: List<Float>,
    val estimatedVolumeM3: Float,
    val memoryFootprintBytes: Int,
    val wires: List<ProceduralWireCable> = emptyList(),
    val nativeMesh: NativeVulkanMesh? = null
)

data class CppAssetSpecs(
    val assetType: CppAssetType,
    val anchorCoordinates: String,
    val elevationDatum: String,
    val runtimeEngine: String,
    val memoryUsage: String,
    val componentParts: List<String>,
    val materialPalette: List<Pair<String, String>>,
    val physicsProperties: Map<String, String>
)
