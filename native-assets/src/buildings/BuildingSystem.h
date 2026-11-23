// Procedural Vulkan Asset Suite
// Path: src/buildings/BuildingSystem.h
// Runtime C++ Binary v2 Parser (MAGT & C58B) & Procedural 3D Architectural + Fence/Gate Builder

#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace ApexAssets {

enum class BuildingShapeType : uint8_t {
    SQUARE = 0,
    RECTANGULAR = 1,
    L_SHAPE = 2,
    U_SHAPE = 3,
    T_SHAPE = 4,
    STAR_CROSS = 5,
    CIRCULAR_SILO = 6,
    POLYGON_IRREGULAR = 7
};

enum class RoofType : uint8_t {
    GABLED = 0,
    HIPPED = 1,
    FLAT_PARAPET = 2,
    SAW_TOOTH_INDUSTRIAL = 3,
    MONO_PITCH_SHED = 4,
    ARCHED_TRUSS = 5,
    CONICAL_DOME = 6
};

enum class RoofMaterial : uint8_t {
    CORRUGATED_IRON_MABATI = 0,
    STONE_COATED_TILE = 1,
    REINFORCED_CONCRETE = 2,
    GALVANIZED_TRUSS_SHEET = 3,
    CLAY_TILE = 4,
    THATCH_MAASAI = 5
};

enum class WallType : uint8_t {
    CONCRETE_BLOCK_PLASTER = 0,
    CORRUGATED_IRON_CLADDING = 1,
    DRESSED_MASONRY_STONE = 2,
    PRECAST_INDUSTRIAL_PANEL = 3,
    TIMBER_PLANK = 4,
    BRICK_MASONRY = 5
};

enum class WindowType : uint8_t {
    SLIDING_ALUMINUM = 0,
    STEEL_CASEMENT = 1,
    INDUSTRIAL_CLERESTORY = 2,
    LOUVERED_VENT = 3,
    CURTAIN_WALL = 4,
    NARROW_SLIT = 5
};

enum class FenceType : uint8_t {
    NONE = 0,
    CHAIN_LINK_POSTS = 1,
    MASONRY_BLOCK_WALL = 2,
    TIMBER_POST_RAIL = 3,
    LIVE_THORN_HEDGE = 4,
    PALISADE_SECURITY_STEEL = 5
};

enum class GateType : uint8_t {
    NONE = 0,
    SLIDING_STEEL_GATE = 1,
    DOUBLE_SWING_TIMBER = 2,
    SECURITY_BOOM_BARRIER = 3,
    PEDESTRIAN_WICKET = 4
};

struct Pt2D {
    float x = 0.0f;
    float y = 0.0f;
};

struct BuildingRecordV2 {
    int64_t osmId = 0;
    std::string name;
    std::string buildingType;
    std::string amenity;
    uint8_t levels = 1;
    uint8_t sectorIndex = 0;
    float heightM = 3.2f;
    float floorHeightM = 3.2f;
    float elevM = 600.0f;
    float areaM2 = 50.0f;
    float perimeterM = 30.0f;
    float centroidX = 0.0f;  // local xM in MAGT, UTM dE (relative to refE) in C58B
    float centroidY = 0.0f;  // local yM in MAGT, UTM dN (relative to refN) in C58B
    std::vector<Pt2D> polygon;

    BuildingShapeType shapeType = BuildingShapeType::RECTANGULAR;
    RoofType roofType = RoofType::GABLED;
    RoofMaterial roofMaterial = RoofMaterial::CORRUGATED_IRON_MABATI;
    WallType wallType = WallType::CONCRETE_BLOCK_PLASTER;
    WindowType windowType = WindowType::SLIDING_ALUMINUM;
    float roofPitchDeg = 20.0f;
    float windowToWallRatio = 0.20f;

    int64_t parentCompoundId = 0;
    std::string parentCompoundName;

    bool hasFence = false;
    FenceType fenceType = FenceType::NONE;
    float fenceHeightM = 0.0f;
    std::vector<Pt2D> fencePolygon;

    bool hasGate = false;
    GateType gateType = GateType::NONE;
    float gateWidthM = 0.0f;
    float gateAngleRad = 0.0f;
    Pt2D gateCoord{0.0f, 0.0f};
};

struct BuildingPackSummary {
    uint16_t formatVersion = 0;
    int32_t totalBuildings = 0;
    int32_t compoundChildren = 0;
    int32_t fencedBuildings = 0;
    int32_t gatedBuildings = 0;
    int32_t multiStoreyBuildings = 0;
    float maxHeightM = 0.0f;
};

struct ArchVertex {
    float pos[3];
    float normal[3];
    float uv[2];
    float color[4];
};

class BuildingSystem {
public:
    BuildingSystem() = default;
    ~BuildingSystem() = default;

    // Direct C++ runtime binary parsers for MAGT (v1/v2) and C58B (v1/v2)
    static bool ParseMagtBinaryBuffer(
        const uint8_t* data,
        size_t size,
        std::vector<BuildingRecordV2>& outBuildings,
        BuildingPackSummary& outSummary
    );

    static bool ParseC58BinaryBuffer(
        const uint8_t* data,
        size_t size,
        std::vector<BuildingRecordV2>& outBuildings,
        BuildingPackSummary& outSummary
    );

    // Procedural 3D architectural + perimeter fence + gate mesh generator (meters around building centroid)
    static void GenerateBuildingAndPerimeterMesh(
        const BuildingRecordV2& record,
        std::vector<ArchVertex>& outVerts,
        std::vector<uint32_t>& outIndices
    );

    static void GenerateCatalogShowcaseMesh(
        BuildingShapeType shapeType,
        RoofType roofType,
        WallType wallType,
        WindowType windowType,
        FenceType fenceType,
        GateType gateType,
        int levels,
        float floorHeightM,
        float footprintWidthM,
        float footprintDepthM,
        std::vector<ArchVertex>& outVerts,
        std::vector<uint32_t>& outIndices
    );

    // Cache most recently loaded runtime records for fast JNI procedural inspection
    void LoadFromMagtBuffer(const uint8_t* data, size_t size);
    void LoadFromC58Buffer(const uint8_t* data, size_t size);
    const BuildingPackSummary& GetLastSummary() const { return m_lastSummary; }
    const std::vector<BuildingRecordV2>& GetCachedBuildings() const { return m_buildings; }
    const BuildingRecordV2* FindByOsmId(int64_t osmId) const;

    static const char* ShapeName(BuildingShapeType s);
    static const char* RoofName(RoofType r);
    static const char* RoofMaterialName(RoofMaterial m);
    static const char* WallName(WallType w);
    static const char* WindowName(WindowType w);
    static const char* FenceName(FenceType f);
    static const char* GateName(GateType g);

private:
    std::vector<BuildingRecordV2> m_buildings;
    BuildingPackSummary m_lastSummary{};
};

} // namespace ApexAssets
