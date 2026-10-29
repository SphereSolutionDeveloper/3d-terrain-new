package com.example.model

data class AssetItem(
    val id: String,
    val name: String,
    val folder: String,
    val category: String,
    val iconDesc: String,
    val primaryColorHex: String,
    val accentColorHex: String
)

data class AssetCatalog(
    val catalogVersion: String,
    val timestamp: String,
    val assets: List<AssetItem>
)

data class VolumeShape(
    val id: String,
    val name: String,
    val shapeType: String,
    val colorHex: String,
    val faceIndices: List<Int>,
    val vertexIndices: List<Int>,
    val center: List<Float>,
    val dimensions: List<Float>,
    val volumeM3: Double,
    val role: String,
    val callout: String,
    val description: String
)

data class MeshGeometry(
    val name: String,
    val vertexCount: Int,
    val faceCount: Int,
    val edgeCount: Int,
    val vertices: List<List<Float>>,
    val normals: List<List<Float>>,
    val uvs: List<List<Float>>,
    val indices: List<Int>,
    val faces: List<List<Int>>,
    val volumeShapes: List<VolumeShape> = emptyList()
)

data class WireframeEdges(
    val name: String,
    val edgeCount: Int,
    val edges: List<List<Int>>
)

data class BoundingBox(
    val min: List<Float>,
    val max: List<Float>
)

data class PhysicsData(
    val massKg: Double,
    val centerOfMass: List<Float>,
    val boundingBox: BoundingBox,
    val dragCoefficient: Double,
    val colliderType: String
)

data class MaterialData(
    val shaderModel: String,
    val baseColorFactor: List<Float>,
    val metallicFactor: Float,
    val roughnessFactor: Float,
    val emissiveFactor: List<Float>,
    val emissiveStrength: Float,
    val normalScale: Float
)

data class GlbHeaderInfo(
    val magic: String,
    val version: Int,
    val length: Long,
    val jsonLength: Long,
    val binLength: Long,
    val isValid: Boolean,
    val nodeCount: Int,
    val meshCount: Int
)

data class TextureManifest(
    val asset: String,
    val resolution: String,
    val format: String,
    val channels: Map<String, String>
)

data class FullAssetBundle(
    val item: AssetItem,
    val metadataJson: String,
    val specsJson: String,
    val physicsJson: String,
    val materialsJson: String,
    val geometry: MeshGeometry,
    val wireframe: WireframeEdges,
    val glbHeader: GlbHeaderInfo,
    val textureManifest: TextureManifest?,
    val objContent: String
)

enum class MainAppView(
    val label: String,
    val iconTag: String,
    val emoji: String = "📦",
    val subtitle: String = "",
    val category: String = "Simulation"
) {
    KENYA_GRID_VIEW("Kenya Terrain Grid", "map", "🗺️", "16×26 Grid • 416 Cells @ 0.5°", "Native Procedural C++"),
    TERRAIN_VIEW("Procedural Terrain", "terrain", "⛰️", "Heightmaps, Caves, Cliffs, CDLOD", "Native Procedural C++"),
    ROAD_VIEW("Road Network System", "road", "🛣️", "Spline Graph, Lanes & Road Tiles", "Native Procedural C++"),
    AUDIO_VIEW("Native Audio Engine", "audio", "🔊", "3D Spatial Emitters & DSP Mixers", "Native Procedural C++"),
    SCENE_3D_VIEW("3D Scene (10x10)", "landscape", "🌲", "10×10m Scatter, Grass & Rocks", "Runtime 3D Simulation"),
    CPP_PROCEDURAL_VIEW("C++ Power Grid", "code", "⚡", "Modular Posts, Catenary Cables", "Runtime 3D Simulation"),
    SINGLE_ASSET_INSPECTOR("GLB / Asset Inspector", "view_in_ar", "📦", "Wireframe, Textures, Volumes", "Runtime 3D Simulation")
}

data class Scene3DObjectInstance(
    val id: String,
    val sectorId: String,
    val folder: String,
    val title: String,
    val posX: Float,
    val posY: Float,
    val posZ: Float,
    val scaleX: Float,
    val scaleY: Float,
    val scaleZ: Float,
    val rotationYaw: Float,
    val accentColorHex: String,
    val isTree: Boolean,
    val isGrass: Boolean = false,
    val geometry: MeshGeometry? = null,
    val albedoBitmap: android.graphics.Bitmap? = null,
    val grassSpecies: GrassSpecies? = null,
    val grassBlendType: GrassBlendType? = null
)

enum class InspectionMode(val label: String, val iconTag: String) {
    WIREFRAME("Wireframe", "grid"),
    GLB_SHADED("3D Shaded (GLB)", "view_in_ar"),
    EROSION_SIM("Erosion & Noise", "water_drop"),
    GRID_10X10("10x10 Grid (1m²)", "grid_on"),
    VOLUME_DIAGRAM("Volume Diagram", "category"),
    MESH_DATA("Mesh Geometry", "polyline"),
    TEXTURES("Texture Maps", "texture"),
    JSON_SPECS("JSON Specs", "data_object"),
    FOLDER_ARCH("Modular Tree", "folder")
}

data class ErosionSimulationData(
    val thermalAngleDeg: Float = 38.0f,
    val thermalIterations: Int = 8,
    val talusReposeAngle: Float = 35.0f,
    val hydraulicDroplets: Int = 250,
    val sedimentCapacityFactor: Float = 3.5f,
    val dissolveRate: Float = 0.25f,
    val evaporationRate: Float = 0.02f,
    val domainWarpAmplitude: Float = 0.45f,
    val ridgedFractureOctaves: Int = 5,
    val fbmPersistence: Float = 0.5f,
    val fbmLacunarity: Float = 2.0f,
    val activeLodLevel: Int = 0 // 0 = LOD0 (High), 1 = LOD1 (Mid), 2 = LOD2 (Collision)
)

data class GridCellData(
    val sectorId: String,
    val gridRow: Int,
    val gridCol: Int,
    val cellSizeMeters: List<Float>,
    val worldPositionMeters: List<Float>,
    val archetypeId: String,
    val rockTitle: String,
    val dimensionsMeters: List<Float>,
    val volumeM3: Double,
    val massKg: Double,
    val mineralDensityKgM3: Double,
    val accentColor: String,
    val noiseSeed: Int,
    val fbmOctaves: Int,
    val frequency: Float,
    val voronoiFacets: Float,
    val surfaceRoughness: Float,
    val geologicalNotes: String,
    val isTree: Boolean = false,
    val lsystemRule: String = ""
)

data class RockGridCatalog(
    val gridVersion: String,
    val description: String,
    val totalCells: Int,
    val cellSizeMeters: Float,
    val totalFieldSizeMeters: List<Float>,
    val totalAreaM2: Float,
    val cells: List<GridCellData>
)

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0,
    val children: List<FileNode> = emptyList()
)
