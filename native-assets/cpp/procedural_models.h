// ─── app/src/main/cpp/procedural_models.h ─────────────────────────────────────
#pragma once
#include <cstdint>
#include <string>
#include <vector>

namespace apex {

struct BoxPrimitive {
    std::string id;
    std::string name;
    float centerX, centerY, centerZ;
    float sizeX, sizeY, sizeZ;
    float r, g, b, a;
    std::string category;
};

struct ModelMesh {
    std::string assetType;
    std::vector<BoxPrimitive> boxes;
    uint32_t totalVertices;
    uint32_t totalTriangles;
};

class ProceduralModelGenerator {
public:
    static ModelMesh buildGarage001(float bay1OpenPct, float bay2OpenPct, bool neonSignOn);
    static ModelMesh buildTestVehicle(bool headlightsOn, bool taillightsOn);
    static ModelMesh buildModularPowerPost(const std::string& postType, bool hasTransformer, bool hasStreetlight, bool streetlightOn, bool hasGuyWire);
    static ModelMesh buildPowerGridNetwork(int numPosts, float spanDistance, float cornerAngleDeg, float wireSag, bool energized, bool useCylinder, float cableRadiusM, float tensionKn, float linearMassKgM, int cylinderSides);
};

} // namespace apex
