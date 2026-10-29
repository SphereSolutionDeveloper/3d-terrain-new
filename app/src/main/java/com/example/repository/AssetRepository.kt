package com.example.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.engine.GlbParser
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream

class AssetRepository(private val context: Context) {

    suspend fun loadCatalog(): List<AssetItem> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("asset_catalog.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val jsonArray = root.getJSONArray("assets")
            val list = mutableListOf<AssetItem>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                list.add(
                    AssetItem(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        folder = item.getString("folder"),
                        category = item.getString("category"),
                        iconDesc = item.getString("icon_desc"),
                        primaryColorHex = item.getString("primary_color"),
                        accentColorHex = item.getString("accent_color")
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback default rock list
            listOf(
                AssetItem("rocks", "10x10 Procedural Rock Field", "rocks", "Field Terrain (1m² Grid)", "Procedural Field Rock", "#00E5FF", "#7000FF"),
                AssetItem("rock_granite", "Granite Boulder", "rock_granite", "Plutonic Granite", "Quartz-Feldspar Boulder", "#39FF14", "#00E5FF"),
                AssetItem("rock_basalt", "Volcanic Basalt Column", "rock_basalt", "Extrusive Basalt", "Prismatic Basalt Column", "#FF8800", "#FF3366"),
                AssetItem("rock_meteorite", "Chondrite Meteorite", "rock_meteorite", "Extra-Terrestrial Regolith", "Pitted Impact Asteroid", "#00F0FF", "#BF55EC"),
                AssetItem("rock_crystal", "Quartz Geode Cluster", "rock_crystal", "Hydrothermal Silicate", "Amethyst Quartz Spires", "#BF55EC", "#00FFFF"),
                AssetItem("rock_strata", "Sedimentary Sandstone", "rock_strata", "Stratified Sandstone", "Layered Terraced Slab", "#FFCC00", "#FF9900")
            )
        }
    }

    suspend fun loadRockGridCatalog(): RockGridCatalog? = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("rocks_grid_10x10.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val dimObj = root.optJSONObject("grid_dimensions")
            val totalCells = dimObj?.optInt("total_cells", 100) ?: 100
            val cellSizeM = dimObj?.optDouble("cell_size_meters", 1.0)?.toFloat() ?: 1.0f
            val totalArea = dimObj?.optDouble("total_area_m2", 100.0)?.toFloat() ?: 100.0f
            val fieldSizeArr = dimObj?.optJSONArray("total_field_size_meters")
            val fieldSize = if (fieldSizeArr != null) {
                listOf(fieldSizeArr.getDouble(0).toFloat(), fieldSizeArr.getDouble(1).toFloat())
            } else {
                listOf(10.0f, 10.0f)
            }

            val cellsArr = root.getJSONArray("cells")
            val cellsList = mutableListOf<GridCellData>()
            for (i in 0 until cellsArr.length()) {
                val c = cellsArr.getJSONObject(i)
                val posArr = c.getJSONArray("world_position_meters")
                val dimArr = c.getJSONArray("dimensions_meters")
                val sizeArr = c.getJSONArray("cell_size_meters")
                val noiseObj = c.optJSONObject("noise_parameters")

                cellsList.add(
                    GridCellData(
                        sectorId = c.getString("sector_id"),
                        gridRow = c.getInt("grid_row"),
                        gridCol = c.getInt("grid_col"),
                        cellSizeMeters = listOf(sizeArr.getDouble(0).toFloat(), sizeArr.getDouble(1).toFloat()),
                        worldPositionMeters = listOf(posArr.getDouble(0).toFloat(), posArr.getDouble(1).toFloat(), posArr.getDouble(2).toFloat()),
                        archetypeId = c.getString("archetype_id"),
                        rockTitle = c.getString("rock_title"),
                        dimensionsMeters = listOf(dimArr.getDouble(0).toFloat(), dimArr.getDouble(1).toFloat(), dimArr.getDouble(2).toFloat()),
                        volumeM3 = c.getDouble("volume_m3"),
                        massKg = c.getDouble("mass_kg"),
                        mineralDensityKgM3 = c.optDouble("mineral_density_kg_m3", 2700.0),
                        accentColor = c.optString("accent_color", "#00E5FF"),
                        noiseSeed = noiseObj?.optInt("seed", 42) ?: 42,
                        fbmOctaves = noiseObj?.optInt("fbm_octaves", 4) ?: 4,
                        frequency = noiseObj?.optDouble("frequency", 2.0)?.toFloat() ?: 2.0f,
                        voronoiFacets = noiseObj?.optDouble("voronoi_facets", 0.3)?.toFloat() ?: 0.3f,
                        surfaceRoughness = noiseObj?.optDouble("surface_roughness", 0.7)?.toFloat() ?: 0.7f,
                        geologicalNotes = c.optString("geological_notes", "")
                    )
                )
            }

            RockGridCatalog(
                gridVersion = root.optString("grid_version", "2.0"),
                description = root.optString("description", "10x10 Rock Grid"),
                totalCells = totalCells,
                cellSizeMeters = cellSizeM,
                totalFieldSizeMeters = fieldSize,
                totalAreaM2 = totalArea,
                cells = cellsList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun loadTreeGridCatalog(): RockGridCatalog? = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("trees_grid_10x10.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val dimObj = root.optJSONObject("grid_dimensions")
            val totalCells = dimObj?.optInt("total_cells", 100) ?: 100
            val cellSizeM = dimObj?.optDouble("cell_size_meters", 1.0)?.toFloat() ?: 1.0f
            val totalArea = dimObj?.optDouble("total_area_m2", 100.0)?.toFloat() ?: 100.0f
            val fieldSizeArr = dimObj?.optJSONArray("total_field_size_meters")
            val fieldSize = if (fieldSizeArr != null) {
                listOf(fieldSizeArr.getDouble(0).toFloat(), fieldSizeArr.getDouble(1).toFloat())
            } else {
                listOf(10.0f, 10.0f)
            }

            val cellsArr = root.getJSONArray("cells")
            val cellsList = mutableListOf<GridCellData>()
            for (i in 0 until cellsArr.length()) {
                val c = cellsArr.getJSONObject(i)
                val posArr = c.getJSONArray("world_position_meters")
                val dimArr = c.getJSONArray("dimensions_meters")
                val sizeArr = c.getJSONArray("cell_size_meters")
                val botObj = c.optJSONObject("botanical_parameters")

                cellsList.add(
                    GridCellData(
                        sectorId = c.getString("sector_id"),
                        gridRow = c.getInt("grid_row"),
                        gridCol = c.getInt("grid_col"),
                        cellSizeMeters = listOf(sizeArr.getDouble(0).toFloat(), sizeArr.getDouble(1).toFloat()),
                        worldPositionMeters = listOf(posArr.getDouble(0).toFloat(), posArr.getDouble(1).toFloat(), posArr.getDouble(2).toFloat()),
                        archetypeId = c.getString("archetype_id"),
                        rockTitle = c.getString("tree_title"),
                        dimensionsMeters = listOf(dimArr.getDouble(0).toFloat(), dimArr.getDouble(1).toFloat(), dimArr.getDouble(2).toFloat()),
                        volumeM3 = c.getDouble("volume_m3"),
                        massKg = c.getDouble("mass_kg"),
                        mineralDensityKgM3 = c.optDouble("timber_density_kg_m3", 650.0),
                        accentColor = c.optString("accent_color", "#39FF14"),
                        noiseSeed = botObj?.optInt("seed", 42) ?: 42,
                        fbmOctaves = botObj?.optInt("lsystem_iterations", 3) ?: 3,
                        frequency = botObj?.optDouble("branching_angle_deg", 30.0)?.toFloat() ?: 30.0f,
                        voronoiFacets = botObj?.optDouble("leonardo_ratio", 0.707)?.toFloat() ?: 0.707f,
                        surfaceRoughness = botObj?.optDouble("leaf_coverage", 0.8)?.toFloat() ?: 0.8f,
                        geologicalNotes = c.optString("geological_notes", ""),
                        isTree = true,
                        lsystemRule = "Leonardo w_r = 0.707, Angle: ${botObj?.optDouble("branching_angle_deg", 30.0)?.toInt() ?: 30}°"
                    )
                )
            }

            RockGridCatalog(
                gridVersion = root.optString("grid_version", "2.0"),
                description = root.optString("description", "10x10 Tree Forest Grid"),
                totalCells = totalCells,
                cellSizeMeters = cellSizeM,
                totalFieldSizeMeters = fieldSize,
                totalAreaM2 = totalArea,
                cells = cellsList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun loadGrassGridCatalog(): RockGridCatalog? = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("grass_grid_10x10.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val dimObj = root.optJSONObject("grid_dimensions")
            val totalCells = dimObj?.optInt("total_cells", 100) ?: 100
            val cellSizeM = dimObj?.optDouble("cell_size_meters", 1.0)?.toFloat() ?: 1.0f
            val totalArea = dimObj?.optDouble("total_area_m2", 100.0)?.toFloat() ?: 100.0f
            val fieldSizeArr = dimObj?.optJSONArray("total_field_size_meters")
            val fieldSize = if (fieldSizeArr != null) {
                listOf(fieldSizeArr.getDouble(0).toFloat(), fieldSizeArr.getDouble(1).toFloat())
            } else {
                listOf(10.0f, 10.0f)
            }

            val cellsArr = root.getJSONArray("cells")
            val cellsList = mutableListOf<GridCellData>()
            for (i in 0 until cellsArr.length()) {
                val c = cellsArr.getJSONObject(i)
                val posArr = c.getJSONArray("world_position_meters")
                val dimArr = c.getJSONArray("dimensions_meters")
                val sizeArr = c.getJSONArray("cell_size_meters")
                val grassObj = c.optJSONObject("grass_parameters")

                cellsList.add(
                    GridCellData(
                        sectorId = c.getString("sector_id"),
                        gridRow = c.getInt("grid_row"),
                        gridCol = c.getInt("grid_col"),
                        cellSizeMeters = listOf(sizeArr.getDouble(0).toFloat(), sizeArr.getDouble(1).toFloat()),
                        worldPositionMeters = listOf(posArr.getDouble(0).toFloat(), posArr.getDouble(1).toFloat(), posArr.getDouble(2).toFloat()),
                        archetypeId = c.getString("archetype_id"),
                        rockTitle = c.getString("grass_title"),
                        dimensionsMeters = listOf(dimArr.getDouble(0).toFloat(), dimArr.getDouble(1).toFloat(), dimArr.getDouble(2).toFloat()),
                        volumeM3 = c.getDouble("volume_m3"),
                        massKg = c.getDouble("mass_kg"),
                        mineralDensityKgM3 = c.optDouble("biomass_density_kg_m3", 220.0),
                        accentColor = c.optString("accent_color", "#39FF14"),
                        noiseSeed = grassObj?.optInt("seed", 42) ?: 42,
                        fbmOctaves = grassObj?.optInt("blade_count", 16) ?: 16,
                        frequency = grassObj?.optDouble("wind_recovery_hz", 2.2)?.toFloat() ?: 2.2f,
                        voronoiFacets = grassObj?.optDouble("stiffness_coefficient", 0.65)?.toFloat() ?: 0.65f,
                        surfaceRoughness = grassObj?.optDouble("bezier_bend_ratio", 0.25)?.toFloat() ?: 0.25f,
                        geologicalNotes = c.optString("geological_notes", ""),
                        isTree = false,
                        lsystemRule = "Bézier Blade s=${grassObj?.optDouble("stiffness_coefficient", 0.65) ?: 0.65}"
                    )
                )
            }

            RockGridCatalog(
                gridVersion = root.optString("grid_version", "2.0"),
                description = root.optString("description", "10x10 Responsive Bézier Grass Grid"),
                totalCells = totalCells,
                cellSizeMeters = cellSizeM,
                totalFieldSizeMeters = fieldSize,
                totalAreaM2 = totalArea,
                cells = cellsList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun loadFullAssetBundle(assetItem: AssetItem): FullAssetBundle = withContext(Dispatchers.IO) {
        val folder = assetItem.folder

        // 1. Load JSONs
        val metaJson = readAssetText("$folder/json/metadata.json")
        val specsJson = readAssetText("$folder/json/specs.json")
        val physicsJson = readAssetText("$folder/json/physics.json")
        val materialsJson = readAssetText("$folder/json/materials.json")

        // 2. Load Geometry
        val geomJsonString = readAssetText("$folder/meshes/geometry.json")
        val geometry = parseGeometry(geomJsonString, folder)

        // 3. Load Wireframe Edges
        val wireframeJsonString = readAssetText("$folder/meshes/wireframe_edges.json")
        val wireframe = parseWireframeEdges(wireframeJsonString, folder)

        // 4. Load OBJ text
        val objContent = readAssetText("$folder/meshes/$folder.obj")

        // 5. Parse GLB header
        val glbHeader = try {
            context.assets.open("$folder/glb/$folder.glb").use {
                GlbParser.parse(it).first
            }
        } catch (e: Exception) {
            GlbHeaderInfo("glTF", 2, 0L, 0L, 0L, isValid = false, nodeCount = 0, meshCount = 0)
        }

        // 6. Texture Manifest
        val textureManifestString = readAssetText("$folder/textures/texture_manifest.json")
        val textureManifest = parseTextureManifest(textureManifestString)

        FullAssetBundle(
            item = assetItem,
            metadataJson = metaJson,
            specsJson = specsJson,
            physicsJson = physicsJson,
            materialsJson = materialsJson,
            geometry = geometry,
            wireframe = wireframe,
            glbHeader = glbHeader,
            textureManifest = textureManifest,
            objContent = objContent
        )
    }

    private val geometryCache = mutableMapOf<String, MeshGeometry>()
    private val bitmapCache = mutableMapOf<String, Bitmap>()

    suspend fun getMeshGeometry(folder: String): MeshGeometry? = withContext(Dispatchers.IO) {
        if (geometryCache.containsKey(folder)) {
            return@withContext geometryCache[folder]
        }
        val path = if (folder == "grass_fine") {
            "grass/meshes/geometry_fine.json"
        } else {
            "$folder/meshes/geometry.json"
        }
        val text = readAssetText(path)
        if (text.isNotEmpty() && !text.startsWith("// File not found")) {
            val geom = parseGeometry(text, folder)
            geometryCache[folder] = geom
            geom
        } else null
    }

    suspend fun loadTextureBitmap(folder: String, textureName: String): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "$folder/$textureName"
        if (bitmapCache.containsKey(cacheKey)) {
            return@withContext bitmapCache[cacheKey]
        }
        try {
            val bmp = context.assets.open("$folder/textures/$textureName").use {
                BitmapFactory.decodeStream(it)
            }
            if (bmp != null) {
                bitmapCache[cacheKey] = bmp
            }
            bmp
        } catch (e: Exception) {
            null
        }
    }

    suspend fun buildFolderTree(): FileNode = withContext(Dispatchers.IO) {
        // Build the clean modular structure of assets
        // assets/
        //   ship/
        //     textures/
        //     json/
        //     meshes/
        //     glb/
        val catalog = loadCatalog()
        val assetNodes = catalog.map { asset ->
            val folder = asset.folder
            val texturesChildren = listOf(
                FileNode("albedo.png", "$folder/textures/albedo.png", false, getAssetSize("$folder/textures/albedo.png")),
                FileNode("normal.png", "$folder/textures/normal.png", false, getAssetSize("$folder/textures/normal.png")),
                FileNode("roughness.png", "$folder/textures/roughness.png", false, getAssetSize("$folder/textures/roughness.png")),
                FileNode("emissive.png", "$folder/textures/emissive.png", false, getAssetSize("$folder/textures/emissive.png")),
                FileNode("volume_diagram.png", "$folder/textures/volume_diagram.png", false, getAssetSize("$folder/textures/volume_diagram.png")),
                FileNode("texture_manifest.json", "$folder/textures/texture_manifest.json", false, getAssetSize("$folder/textures/texture_manifest.json"))
            )

            val jsonChildren = listOf(
                FileNode("metadata.json", "$folder/json/metadata.json", false, getAssetSize("$folder/json/metadata.json")),
                FileNode("specs.json", "$folder/json/specs.json", false, getAssetSize("$folder/json/specs.json")),
                FileNode("physics.json", "$folder/json/physics.json", false, getAssetSize("$folder/json/physics.json")),
                FileNode("materials.json", "$folder/json/materials.json", false, getAssetSize("$folder/json/materials.json"))
            )

            val meshesChildren = listOf(
                FileNode("$folder.obj", "$folder/meshes/$folder.obj", false, getAssetSize("$folder/meshes/$folder.obj")),
                FileNode("geometry.json", "$folder/meshes/geometry.json", false, getAssetSize("$folder/meshes/geometry.json")),
                FileNode("wireframe_edges.json", "$folder/meshes/wireframe_edges.json", false, getAssetSize("$folder/meshes/wireframe_edges.json"))
            )

            val glbChildren = listOf(
                FileNode("$folder.glb", "$folder/glb/$folder.glb", false, getAssetSize("$folder/glb/$folder.glb"))
            )

            val subDirs = listOf(
                FileNode("textures", "$folder/textures", true, children = texturesChildren),
                FileNode("json", "$folder/json", true, children = jsonChildren),
                FileNode("meshes", "$folder/meshes", true, children = meshesChildren),
                FileNode("glb", "$folder/glb", true, children = glbChildren)
            )

            FileNode(
                name = folder,
                path = folder,
                isDirectory = true,
                children = subDirs
            )
        }

        FileNode(
            name = "assets (modular architecture)",
            path = "assets",
            isDirectory = true,
            children = assetNodes
        )
    }

    private fun getAssetSize(path: String): Long {
        return try {
            context.assets.open(path).use { it.available().toLong() }
        } catch (e: Exception) {
            0L
        }
    }

    private fun readAssetText(path: String): String {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "// File not found or empty: $path"
        }
    }

    private fun parseGeometry(jsonString: String, fallbackName: String): MeshGeometry {
        return try {
            val obj = JSONObject(jsonString)
            val name = obj.optString("name", fallbackName)
            val vertexCount = obj.optInt("vertex_count", 0)
            val faceCount = obj.optInt("face_count", 0)
            val edgeCount = obj.optInt("edge_count", 0)

            val vertices = parse2DFloatArray(obj.optJSONArray("vertices"))
            val normals = parse2DFloatArray(obj.optJSONArray("normals"))
            val uvs = parse2DFloatArray(obj.optJSONArray("uvs"))

            val indicesJson = obj.optJSONArray("indices")
            val indices = mutableListOf<Int>()
            if (indicesJson != null) {
                for (i in 0 until indicesJson.length()) {
                    indices.add(indicesJson.getInt(i))
                }
            }

            val facesJson = obj.optJSONArray("faces")
            val faces = mutableListOf<List<Int>>()
            if (facesJson != null) {
                for (i in 0 until facesJson.length()) {
                    val f = facesJson.getJSONArray(i)
                    faces.add(listOf(f.getInt(0), f.getInt(1), f.getInt(2)))
                }
            }

            val volumeShapes = mutableListOf<com.example.model.VolumeShape>()
            val volumeShapesJson = obj.optJSONArray("volume_shapes")
            if (volumeShapesJson != null && volumeShapesJson.length() > 0) {
                for (i in 0 until volumeShapesJson.length()) {
                    val vs = volumeShapesJson.getJSONObject(i)
                    val id = vs.optString("id", "vol_$i")
                    val vsName = vs.optString("name", "Volume Shape $i")
                    val shapeType = vs.optString("shape_type", "Solid Polyhedron")
                    val colorHex = vs.optString("color_hex", "#00E5FF")
                    val role = vs.optString("role", "Component")
                    val callout = vs.optString("callout", "")
                    val description = vs.optString("description", "")
                    val volumeM3 = vs.optDouble("volume_m3", 1.0)

                    val faceIdxList = mutableListOf<Int>()
                    val fArr = vs.optJSONArray("face_indices")
                    if (fArr != null) {
                        for (j in 0 until fArr.length()) faceIdxList.add(fArr.getInt(j))
                    }

                    val vertIdxList = mutableListOf<Int>()
                    val vArr = vs.optJSONArray("vertex_indices")
                    if (vArr != null) {
                        for (j in 0 until vArr.length()) vertIdxList.add(vArr.getInt(j))
                    }

                    val centerList = mutableListOf<Float>()
                    val cArr = vs.optJSONArray("center")
                    if (cArr != null) {
                        for (j in 0 until cArr.length()) centerList.add(cArr.getDouble(j).toFloat())
                    }

                    val dimList = mutableListOf<Float>()
                    val dArr = vs.optJSONArray("dimensions")
                    if (dArr != null) {
                        for (j in 0 until dArr.length()) dimList.add(dArr.getDouble(j).toFloat())
                    }

                    volumeShapes.add(
                        com.example.model.VolumeShape(
                            id = id,
                            name = vsName,
                            shapeType = shapeType,
                            colorHex = colorHex,
                            faceIndices = faceIdxList,
                            vertexIndices = vertIdxList,
                            center = centerList,
                            dimensions = dimList,
                            volumeM3 = volumeM3,
                            role = role,
                            callout = callout,
                            description = description
                        )
                    )
                }
            } else if (faces.isNotEmpty()) {
                // Fallback synthesized volume groups for legacy models
                val chunkSize = maxOf(1, faces.size / 3)
                val f1 = (0 until minOf(chunkSize, faces.size)).toList()
                val f2 = (chunkSize until minOf(chunkSize * 2, faces.size)).toList()
                val f3 = (chunkSize * 2 until faces.size).toList()
                volumeShapes.add(
                    com.example.model.VolumeShape(
                        id = "vol_core",
                        name = "Central Structural Core",
                        shapeType = "Polyhedron Chassis",
                        colorHex = "#00E5FF",
                        faceIndices = f1,
                        vertexIndices = f1.flatMap { faces[it] }.distinct(),
                        center = listOf(0f, 0f, 0f),
                        dimensions = listOf(2f, 1f, 3f),
                        volumeM3 = 12.0,
                        role = "Chassis & Core Assembly",
                        callout = "Primary Structural Framework",
                        description = "Core structural volumetric framework."
                    )
                )
                if (f2.isNotEmpty()) {
                    volumeShapes.add(
                        com.example.model.VolumeShape(
                            id = "vol_aux1",
                            name = "Secondary Volume Module",
                            shapeType = "Cylindrical Actuators",
                            colorHex = "#FF7700",
                            faceIndices = f2,
                            vertexIndices = f2.flatMap { faces[it] }.distinct(),
                            center = listOf(1f, 0f, -1f),
                            dimensions = listOf(1.5f, 0.8f, 2f),
                            volumeM3 = 6.5,
                            role = "Subsystem Module",
                            callout = "Auxiliary Propulsion / Actuators",
                            description = "Subsystem module volumes."
                        )
                    )
                }
                if (f3.isNotEmpty()) {
                    volumeShapes.add(
                        com.example.model.VolumeShape(
                            id = "vol_aux2",
                            name = "Aerodynamic Outer Array",
                            shapeType = "Faceted Outriggers",
                            colorHex = "#00E676",
                            faceIndices = f3,
                            vertexIndices = f3.flatMap { faces[it] }.distinct(),
                            center = listOf(-1f, 0f, -1f),
                            dimensions = listOf(2.5f, 0.5f, 2f),
                            volumeM3 = 8.0,
                            role = "Stabilizers & Wings",
                            callout = "Outer Control Surfaces",
                            description = "Outer control surfaces and aerodynamic arrays."
                        )
                    )
                }
            }

            MeshGeometry(name, vertexCount, faceCount, edgeCount, vertices, normals, uvs, indices, faces, volumeShapes)
        } catch (e: Exception) {
            MeshGeometry(fallbackName, 0, 0, 0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        }
    }

    private fun parseWireframeEdges(jsonString: String, fallbackName: String): WireframeEdges {
        return try {
            val obj = JSONObject(jsonString)
            val name = obj.optString("name", fallbackName)
            val edgeCount = obj.optInt("edge_count", 0)
            val edgesJson = obj.optJSONArray("edges")
            val edges = mutableListOf<List<Int>>()
            if (edgesJson != null) {
                for (i in 0 until edgesJson.length()) {
                    val pair = edgesJson.getJSONArray(i)
                    edges.add(listOf(pair.getInt(0), pair.getInt(1)))
                }
            }
            WireframeEdges(name, edgeCount, edges)
        } catch (e: Exception) {
            WireframeEdges(fallbackName, 0, emptyList())
        }
    }

    private fun parseTextureManifest(jsonString: String): TextureManifest? {
        return try {
            val obj = JSONObject(jsonString)
            val asset = obj.optString("asset", "")
            val resolution = obj.optString("resolution", "128x128")
            val format = obj.optString("format", "PNG")
            val channelsObj = obj.optJSONObject("channels")
            val channelsMap = mutableMapOf<String, String>()
            if (channelsObj != null) {
                for (key in channelsObj.keys()) {
                    channelsMap[key] = channelsObj.getString(key)
                }
            }
            TextureManifest(asset, resolution, format, channelsMap)
        } catch (e: Exception) {
            null
        }
    }

    private fun parse2DFloatArray(jsonArray: JSONArray?): List<List<Float>> {
        if (jsonArray == null) return emptyList()
        val result = mutableListOf<List<Float>>()
        for (i in 0 until jsonArray.length()) {
            val inner = jsonArray.getJSONArray(i)
            val row = mutableListOf<Float>()
            for (j in 0 until inner.length()) {
                row.add(inner.getDouble(j).toFloat())
            }
            result.add(row)
        }
        return result
    }
}
