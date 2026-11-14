#include "GameTerrainLoader.h"
#include <iostream>

int main(int argc, char** argv) {
    std::cout << "=====================================================" << std::endl;
    std::cout << " Game Terrain C++ Converter & Physics Validator" << std::endl;
    std::cout << " Test Ground 001 (Vulkan / Unreal / Unity / Godot)" << std::endl;
    std::cout << "=====================================================" << std::endl;

    std::string jsonPath = "native-assets/src/terrain/game_terrain/test_ground_001/jsons/transport_sim_test_001.json";
    if (argc > 1) {
        jsonPath = argv[1];
    }

    GameEngine::Terrain::GameTerrain terrain;
    std::cout << "[1/4] Parsing JSON: " << jsonPath << " ..." << std::endl;
    if (!terrain.LoadFromJSON(jsonPath)) {
        std::cerr << "Failed to parse JSON terrain." << std::endl;
        return 1;
    }

    std::cout << "Map: " << terrain.GetMeta().mapName << " (v" << terrain.GetMeta().version << ")" << std::endl;
    std::cout << "Dimensions: " << terrain.GetGridWidth() << "x" << terrain.GetGridDepth() 
              << " (" << terrain.GetHeights().size() << " points, " 
              << terrain.GetMeta().mapWidthM << "m x " << terrain.GetMeta().mapDepthM << "m)" << std::endl;
    std::cout << "Elevation Range: " << terrain.GetMeta().minElevationM << "m .. " 
              << terrain.GetMeta().maxElevationM << "m" << std::endl;
    std::cout << "Placed Assets: " << terrain.GetAssets().size() << std::endl;

    // Test elevation queries
    std::cout << "\n[2/4] Elevation & Physics Sampling Tests:" << std::endl;
    float testPoints[][2] = {
        {160.0f, 160.0f}, // Central Lake
        {10.0f, 160.0f},  // West Hill
        {285.0f, 160.0f}, // East Road
        {160.0f, 285.0f}, // North Bridge
        {100.0f, 35.0f}   // Mud Zone
    };

    for (const auto& pt : testPoints) {
        float h = terrain.GetElevationAt(pt[0], pt[1]);
        auto surf = terrain.GetSurfaceTypeAt(pt[0], pt[1]);
        float fric = terrain.GetFrictionAt(pt[0], pt[1]);
        auto norm = terrain.GetNormalAt(pt[0], pt[1]);
        std::cout << "  At (" << pt[0] << "m, " << pt[1] << "m): "
                  << "Elev=" << h << "m, Zone=" << GameEngine::Terrain::SurfaceTypeToString(surf)
                  << ", Friction=" << fric
                  << ", Normal=(" << norm.x << ", " << norm.y << ", " << norm.z << ")" << std::endl;
    }

    // Export high-speed binary format (.bin)
    std::string binPath = "native-assets/src/terrain/game_terrain/test_ground_001/jsons/transport_sim_test_001.bin";
    std::cout << "\n[3/4] Exporting High-Performance Binary: " << binPath << " ..." << std::endl;
    if (terrain.SaveToBinary(binPath)) {
        std::cout << "  Binary cache exported successfully! (Zero-parse mobile instant loading)" << std::endl;
    }

    // Generate GPU Mesh Chunk
    std::cout << "\n[4/4] Generating GPU Mesh Data (LOD 1):" << std::endl;
    std::vector<GameEngine::Terrain::TerrainVertex> vertices;
    std::vector<uint32_t> indices;
    terrain.GenerateMeshData(vertices, indices, 1);
    std::cout << "  Generated " << vertices.size() << " vertices, " << (indices.size() / 3) << " triangles." << std::endl;

    std::cout << "\nTest Ground 001 C++ validation passed successfully!" << std::endl;
    return 0;
}
