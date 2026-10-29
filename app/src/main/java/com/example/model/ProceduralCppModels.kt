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

data class ProceduralModelMesh(
    val assetType: CppAssetType,
    val boxes: List<ProceduralBoxPart>,
    val totalVertices: Int,
    val totalTriangles: Int,
    val boundingBoxMin: List<Float>,
    val boundingBoxMax: List<Float>,
    val estimatedVolumeM3: Float,
    val memoryFootprintBytes: Int,
    val wires: List<ProceduralWireCable> = emptyList()
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
