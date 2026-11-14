#include "procedural_vehicle.hpp"

namespace apex {
namespace procedural {

std::vector<Vertex3D> ProceduralVehicle::buildTestLorry(const VehicleConfig& config) {
    std::vector<Vertex3D> verts;

    // 1. Heavy Chassis Frame (5.8m x 0.4m x 2.2m centered at Y = 0.65m, Charcoal Slate #1F242E)
    ProceduralGarage::addBox(verts, 0.0f, 0.65f, 0.0f, 5.8f, 0.4f, 2.2f, 0.12f, 0.14f, 0.18f);

    // 2. 4 Heavy-Duty Wheels (rest on ground Y = 0, wheel center Y = 0.45m, Matte Black #141414)
    // Rear Wheels (X = -1.8m)
    ProceduralGarage::addBox(verts, -1.8f, 0.45f, -1.15f, 0.9f, 0.9f, 0.45f, 0.08f, 0.08f, 0.08f);
    ProceduralGarage::addBox(verts, -1.8f, 0.45f,  1.15f, 0.9f, 0.9f, 0.45f, 0.08f, 0.08f, 0.08f);

    // Front Wheels (X = +1.8m)
    ProceduralGarage::addBox(verts,  1.8f, 0.45f, -1.15f, 0.9f, 0.9f, 0.45f, 0.08f, 0.08f, 0.08f);
    ProceduralGarage::addBox(verts,  1.8f, 0.45f,  1.15f, 0.9f, 0.9f, 0.45f, 0.08f, 0.08f, 0.08f);

    // 3. Driver's Cab (1.8m x 1.8m x 2.1m at front X = +1.8m, Electric Cyan #00BCD4)
    ProceduralGarage::addBox(verts, 1.8f, 1.75f, 0.0f, 1.8f, 1.8f, 2.1f, 0.0f, 0.74f, 0.83f);

    // 4. Dark Windshield & Side Windows (Tint Glass #1F2E47)
    // Front windshield
    ProceduralGarage::addBox(verts, 2.72f, 1.95f, 0.0f, 0.05f, 0.9f, 1.8f, 0.12f, 0.18f, 0.28f);
    // Side windows
    ProceduralGarage::addBox(verts, 1.8f, 1.95f,  1.06f, 1.0f, 0.8f, 0.05f, 0.12f, 0.18f, 0.28f);
    ProceduralGarage::addBox(verts, 1.8f, 1.95f, -1.06f, 1.0f, 0.8f, 0.05f, 0.12f, 0.18f, 0.28f);

    // 5. Large Freight Cargo Box (3.8m x 2.4m x 2.3m at X = -0.9m, Clean Silver-White #EBF0F5)
    ProceduralGarage::addBox(verts, -0.9f, 2.1f, 0.0f, 3.8f, 2.4f, 2.3f, 0.92f, 0.94f, 0.96f);

    // 6. Cargo Stripe Accents (Cyan livery flanking stripes matching cab)
    ProceduralGarage::addBox(verts, -0.9f, 1.8f,  1.16f, 3.82f, 0.4f, 0.02f, 0.0f, 0.74f, 0.83f);
    ProceduralGarage::addBox(verts, -0.9f, 1.8f, -1.16f, 3.82f, 0.4f, 0.02f, 0.0f, 0.74f, 0.83f);

    // 7. Dual Headlights (Front bumper at X = 2.72m)
    if (config.headlightsOn) {
        ProceduralGarage::addBox(verts, 2.72f, 1.1f,  0.75f, 0.05f, 0.3f, 0.4f, 1.0f, 0.88f, 0.2f);
        ProceduralGarage::addBox(verts, 2.72f, 1.1f, -0.75f, 0.05f, 0.3f, 0.4f, 1.0f, 0.88f, 0.2f);
    } else {
        ProceduralGarage::addBox(verts, 2.72f, 1.1f,  0.75f, 0.05f, 0.3f, 0.4f, 0.4f, 0.35f, 0.1f);
        ProceduralGarage::addBox(verts, 2.72f, 1.1f, -0.75f, 0.05f, 0.3f, 0.4f, 0.4f, 0.35f, 0.1f);
    }

    // 8. Dual Taillights (Rear bumper at X = -2.82m)
    if (config.taillightsOn) {
        ProceduralGarage::addBox(verts, -2.82f, 1.0f,  0.8f, 0.05f, 0.25f, 0.35f, 0.9f, 0.1f, 0.1f);
        ProceduralGarage::addBox(verts, -2.82f, 1.0f, -0.8f, 0.05f, 0.25f, 0.35f, 0.9f, 0.1f, 0.1f);
    } else {
        ProceduralGarage::addBox(verts, -2.82f, 1.0f,  0.8f, 0.05f, 0.25f, 0.35f, 0.3f, 0.05f, 0.05f);
        ProceduralGarage::addBox(verts, -2.82f, 1.0f, -0.8f, 0.05f, 0.25f, 0.35f, 0.3f, 0.05f, 0.05f);
    }

    return verts;
}

} // namespace procedural
} // namespace apex
