#pragma once

#include <vector>
#include <string>
#include <cstdint>

namespace apex {
namespace procedural {

// 36-byte vertex structure matching the Proving Ground Native Engine
struct Vertex3D {
    float x, y, z;       // Position in meters (Offset 0)
    float nx, ny, nz;    // Surface normal vector (Offset 12)
    float r, g, b, a;    // Color components in [0.0, 1.0] (Offset 24)
};

struct GarageConfig {
    float bay1OpenPct = 0.0f; // 0.0 = fully closed, 1.0 = fully open
    float bay2OpenPct = 0.0f;
    bool neonSignOn = true;
    bool showApron = true;
    bool showGuidelines = true;
    bool showDriveway = true;
};

class ProceduralGarage {
public:
    static void addBox(std::vector<Vertex3D>& verts,
                       float cx, float cy, float cz,
                       float sx, float sy, float sz,
                       float r, float g, float b, float a = 1.0f);

    // Builds the complete Garage 001 SE Depot procedural mesh
    static std::vector<Vertex3D> buildGarage001(const GarageConfig& config = GarageConfig{});

    // Dimensions in meters
    static constexpr float WAREHOUSE_WIDTH = 14.0f;
    static constexpr float WAREHOUSE_HEIGHT = 6.0f;
    static constexpr float WAREHOUSE_LENGTH = 22.0f;

    static constexpr float ROOF_WIDTH = 15.0f;
    static constexpr float ROOF_HEIGHT = 0.4f;
    static constexpr float ROOF_LENGTH = 23.0f;

    static constexpr float DOOR_WIDTH = 5.0f;
    static constexpr float DOOR_HEIGHT = 3.6f;

    static constexpr float APRON_WIDTH = 28.0f;
    static constexpr float APRON_LENGTH = 24.0f;
};

} // namespace procedural
} // namespace apex
