#include "procedural_power_grid.hpp"

namespace apex {
namespace procedural {

void ProceduralPowerGrid::addCatenaryWire(
    std::vector<Vertex3D>& verts,
    float x1, float y1, float z1,
    float x2, float y2, float z2,
    float sag,
    int numSegments,
    float thickness,
    float r, float g, float b, float a
) {
    if (numSegments < 2) numSegments = 8;
    float prevX = x1;
    float prevY = y1;
    float prevZ = z1;

    for (int i = 1; i <= numSegments; ++i) {
        float t = static_cast<float>(i) / static_cast<float>(numSegments);
        float curX = x1 + t * (x2 - x1);
        float curZ = z1 + t * (z2 - z1);
        // Parabolic catenary sag formula: max sag at t = 0.5
        float sagOffset = 4.0f * sag * t * (1.0f - t);
        float curY = y1 + t * (y2 - y1) - sagOffset;

        float midX = (prevX + curX) * 0.5f;
        float midY = (prevY + curY) * 0.5f;
        float midZ = (prevZ + curZ) * 0.5f;

        float dx = curX - prevX;
        float dy = curY - prevY;
        float dz = curZ - prevZ;
        float segLen = std::sqrt(dx * dx + dy * dy + dz * dz);
        if (segLen < 0.001f) segLen = 0.001f;

        // Add 3D box segment aligned with wire path
        float sx = (std::abs(dx) > 0.02f) ? std::abs(dx) : thickness;
        float sy = (std::abs(dy) > 0.02f) ? std::abs(dy) : thickness;
        float sz = (std::abs(dz) > 0.02f) ? std::abs(dz) : thickness;

        ProceduralGarage::addBox(verts, midX, midY, midZ, sx, sy, sz, r, g, b, a);

        prevX = curX;
        prevY = curY;
        prevZ = curZ;
    }
}

std::vector<Vertex3D> ProceduralPowerGrid::buildSinglePost(
    const PostConfig& config,
    float originX,
    float originY,
    float originZ,
    float orientationYawRad
) {
    std::vector<Vertex3D> verts;

    float cosA = std::cos(orientationYawRad);
    float sinA = std::sin(orientationYawRad);
    auto rotatePoint = [&](float lx, float lz, float& outX, float& outZ) {
        outX = originX + (lx * cosA - lz * sinA);
        outZ = originZ + (lx * sinA + lz * cosA);
    };

    // 1. Concrete Ground Foundation Pedestal (0.65m x 0.45m x 0.65m, Slate Concrete #8A929A)
    ProceduralGarage::addBox(verts, originX, originY + 0.22f, originZ, 0.65f, 0.45f, 0.65f, 0.54f, 0.57f, 0.60f);

    // 2. Main Vertical Pole Mast (10.5m tall, tapered weathered treated timber/concrete)
    // Lower section (Ground to 4.0m) - thicker base
    ProceduralGarage::addBox(verts, originX, originY + 2.2f, originZ, 0.36f, 4.0f, 0.36f, 0.44f, 0.35f, 0.28f);
    // Middle section (4.0m to 7.5m)
    ProceduralGarage::addBox(verts, originX, originY + 5.75f, originZ, 0.30f, 3.5f, 0.30f, 0.48f, 0.38f, 0.30f);
    // Upper section (7.5m to 10.5m)
    ProceduralGarage::addBox(verts, originX, originY + 9.0f, originZ, 0.24f, 3.0f, 0.24f, 0.52f, 0.42f, 0.34f);

    // Steel Mast Cap
    ProceduralGarage::addBox(verts, originX, originY + 10.55f, originZ, 0.28f, 0.10f, 0.28f, 0.30f, 0.32f, 0.35f);

    // Grounding Cable (Galvanized ground wire running down the mast side)
    float gndX, gndZ;
    rotatePoint(0.14f, 0.0f, gndX, gndZ);
    ProceduralGarage::addBox(verts, gndX, originY + 5.25f, gndZ, 0.02f, 10.2f, 0.02f, 0.70f, 0.72f, 0.75f);

    // Climbing Steps (Metal foot rungs alternating up the mast from 2.5m to 9.0m)
    for (int step = 0; step < 7; ++step) {
        float stepY = originY + 2.8f + step * 0.9f;
        float side = (step % 2 == 0) ? 0.18f : -0.18f;
        float rx, rz;
        rotatePoint(side, 0.0f, rx, rz);
        ProceduralGarage::addBox(verts, rx, stepY, rz, 0.14f, 0.03f, 0.03f, 0.75f, 0.75f, 0.78f);
    }

    // 3. Primary High-Voltage Crossarm (Galvanized Steel / Treated Wood)
    float armY = originY + 9.8f;
    float crossarmWidth = config.crossarmWidth;

    bool isCornerOrDeadEnd = (config.type == PowerPostType::CORNER_ANGLE || config.type == PowerPostType::DEAD_END_TERMINAL);

    // Main Crossarm Beam (aligned with local X)
    float armX, armZ;
    rotatePoint(0.0f, 0.0f, armX, armZ);
    float armRotW = (std::abs(cosA) > 0.01f) ? crossarmWidth * std::abs(cosA) : 0.12f;
    float armRotD = (std::abs(sinA) > 0.01f) ? crossarmWidth * std::abs(sinA) : 0.12f;
    ProceduralGarage::addBox(verts, armX, armY, armZ, std::max(armRotW, 0.12f), 0.12f, std::max(armRotD, 0.12f), 0.38f, 0.40f, 0.44f);

    // Diagonal Angle Braces (Under crossarm supporting load)
    ProceduralGarage::addBox(verts, armX, armY - 0.45f, armZ, std::max(armRotW * 0.55f, 0.08f), 0.05f, std::max(armRotD * 0.55f, 0.08f), 0.50f, 0.52f, 0.55f);

    // Secondary Crossarm if Corner or Dead-End (Double crossarm for cable tension)
    if (isCornerOrDeadEnd) {
        float doubleArmY = armY - 0.35f;
        ProceduralGarage::addBox(verts, armX, doubleArmY, armZ, std::max(armRotW, 0.12f), 0.12f, std::max(armRotD, 0.12f), 0.34f, 0.36f, 0.40f);
    }

    // Top Ridge Pin Bracket (Carries Phase B / Center Conductor at top of pole)
    ProceduralGarage::addBox(verts, originX, originY + 10.75f, originZ, 0.08f, 0.35f, 0.08f, 0.40f, 0.42f, 0.45f);

    // 4. Porcelain / Glass Pin Insulators (3 High-Voltage Distribution Phases)
    // Insulator 1: Left Phase A (X = -crossarmWidth/2 * 0.85)
    float insOff = (crossarmWidth * 0.5f) - 0.2f;
    float ins1X, ins1Z;
    rotatePoint(-insOff, 0.0f, ins1X, ins1Z);
    // Pin stalk & bell skirt
    ProceduralGarage::addBox(verts, ins1X, armY + 0.12f, ins1Z, 0.04f, 0.15f, 0.04f, 0.65f, 0.65f, 0.68f);
    ProceduralGarage::addBox(verts, ins1X, armY + 0.26f, ins1Z, 0.16f, 0.18f, 0.16f, 0.58f, 0.22f, 0.18f); // Red terracotta insulator
    ProceduralGarage::addBox(verts, ins1X, armY + 0.38f, ins1Z, 0.10f, 0.08f, 0.10f, 0.70f, 0.28f, 0.24f);

    // Insulator 2: Center Phase B (Atop top ridge pin at Y = 10.95m)
    ProceduralGarage::addBox(verts, originX, originY + 11.0f, originZ, 0.16f, 0.20f, 0.16f, 0.58f, 0.22f, 0.18f);
    ProceduralGarage::addBox(verts, originX, originY + 11.12f, originZ, 0.10f, 0.08f, 0.10f, 0.70f, 0.28f, 0.24f);

    // Insulator 3: Right Phase C (X = +crossarmWidth/2 * 0.85)
    float ins3X, ins3Z;
    rotatePoint(insOff, 0.0f, ins3X, ins3Z);
    ProceduralGarage::addBox(verts, ins3X, armY + 0.12f, ins3Z, 0.04f, 0.15f, 0.04f, 0.65f, 0.65f, 0.68f);
    ProceduralGarage::addBox(verts, ins3X, armY + 0.26f, ins3Z, 0.16f, 0.18f, 0.16f, 0.58f, 0.22f, 0.18f);
    ProceduralGarage::addBox(verts, ins3X, armY + 0.38f, ins3Z, 0.10f, 0.08f, 0.10f, 0.70f, 0.28f, 0.24f);

    // 5. Mounted Distribution Transformer (If TRANSFORMER_DISTRIBUTION or config.hasTransformer)
    if (config.type == PowerPostType::TRANSFORMER_DISTRIBUTION || config.hasTransformer) {
        float transOffX, transOffZ;
        rotatePoint(0.0f, 0.52f, transOffX, transOffZ);

        // Heavy steel mounting bracket hanger
        ProceduralGarage::addBox(verts, transOffX, originY + 7.2f, transOffZ, 0.40f, 0.08f, 0.20f, 0.25f, 0.28f, 0.32f);

        // Cylindrical 50kVA Step-down Transformer Tank (Dark battleship gray #37474F)
        ProceduralGarage::addBox(verts, transOffX, originY + 6.6f, transOffZ, 0.65f, 1.15f, 0.65f, 0.22f, 0.28f, 0.31f);
        // Radiator cooling fins on sides
        ProceduralGarage::addBox(verts, transOffX, originY + 6.6f, transOffZ, 0.74f, 0.85f, 0.55f, 0.18f, 0.22f, 0.25f);
        // Top lid & primary HV bushing terminals
        ProceduralGarage::addBox(verts, transOffX, originY + 7.25f, transOffZ, 0.62f, 0.08f, 0.62f, 0.30f, 0.35f, 0.38f);
        ProceduralGarage::addBox(verts, transOffX - 0.14f, originY + 7.42f, transOffZ, 0.08f, 0.25f, 0.08f, 0.85f, 0.55f, 0.20f);
        ProceduralGarage::addBox(verts, transOffX + 0.14f, originY + 7.42f, transOffZ, 0.08f, 0.25f, 0.08f, 0.85f, 0.55f, 0.20f);

        // Secondary Low-Voltage Spool Rack (Mounted at Y = 5.2m with 3 neutral/hot lugs)
        float rackX, rackZ;
        rotatePoint(0.0f, -0.22f, rackX, rackZ);
        ProceduralGarage::addBox(verts, rackX, originY + 5.2f, rackZ, 0.12f, 0.75f, 0.12f, 0.35f, 0.38f, 0.42f);
        // 3 Low voltage spool ceramic insulators (Brown/White)
        for (int lv = 0; lv < 3; ++lv) {
            ProceduralGarage::addBox(verts, rackX, originY + 4.95f + lv * 0.25f, rackZ, 0.16f, 0.12f, 0.16f, 0.90f, 0.90f, 0.92f);
        }
    }

    // 6. Guy Wire Stay & Counterbalance Anchor (Critical for Corner Angle & Dead-End Posts)
    if (config.type == PowerPostType::CORNER_ANGLE || config.type == PowerPostType::DEAD_END_TERMINAL || config.hasGuyWire) {
        // Guy wire attachment clamp near top of mast
        float clampY = originY + 9.5f;
        ProceduralGarage::addBox(verts, originX, clampY, originZ, 0.32f, 0.12f, 0.32f, 0.25f, 0.27f, 0.30f);

        // Guy wire anchor point on ground (counterbalancing the pull angle)
        float anchorDist = 4.2f;
        float anchorAngle = orientationYawRad + 3.14159f; // Backwards counterbalance
        float anchorX = originX + anchorDist * std::cos(anchorAngle);
        float anchorZ = originZ + anchorDist * std::sin(anchorAngle);

        // Ground anchor rod stake
        ProceduralGarage::addBox(verts, anchorX, originY + 0.35f, anchorZ, 0.12f, 0.70f, 0.12f, 0.30f, 0.32f, 0.35f);
        // High-visibility yellow safety guard sleeve (#FFEB3B) on the lower 1.8m
        float sleeveMidX = (anchorX * 0.75f + originX * 0.25f);
        float sleeveMidZ = (anchorZ * 0.75f + originZ * 0.25f);
        ProceduralGarage::addBox(verts, sleeveMidX, originY + 0.95f, sleeveMidZ, 0.10f, 1.80f, 0.10f, 1.0f, 0.88f, 0.15f);

        // Steel stranded stay cable running from clamp down to anchor
        addCatenaryWire(verts, originX, clampY, originZ, anchorX, originY + 0.4f, anchorZ, 0.05f, 6, 0.03f, 0.78f, 0.80f, 0.82f);

        // Secondary ground stay for dead-end terminal
        if (config.type == PowerPostType::DEAD_END_TERMINAL) {
            float anchor2Angle = anchorAngle + 0.45f;
            float anchor2X = originX + (anchorDist * 0.9f) * std::cos(anchor2Angle);
            float anchor2Z = originZ + (anchorDist * 0.9f) * std::sin(anchor2Angle);
            ProceduralGarage::addBox(verts, anchor2X, originY + 0.35f, anchor2Z, 0.12f, 0.70f, 0.12f, 0.30f, 0.32f, 0.35f);
            addCatenaryWire(verts, originX, clampY - 0.5f, originZ, anchor2X, originY + 0.4f, anchor2Z, 0.05f, 6, 0.03f, 0.78f, 0.80f, 0.82f);
        }
    }

    // 7. Cantilever Streetlight Fixture (Optional modular amenity)
    if (config.hasStreetlight) {
        float armBaseY = originY + 8.2f;
        float armExtent = 1.8f;
        float lampEndX, lampEndZ;
        rotatePoint(armExtent, 0.0f, lampEndX, lampEndZ);

        // Cantilever horizontal curved mounting arm (Galvanized steel pipe)
        float armMidX = (originX + lampEndX) * 0.5f;
        float armMidZ = (originZ + lampEndZ) * 0.5f;
        ProceduralGarage::addBox(verts, armMidX, armBaseY + 0.2f, armMidZ, std::abs(lampEndX - originX) + 0.08f, 0.07f, std::abs(lampEndZ - originZ) + 0.08f, 0.60f, 0.63f, 0.67f);

        // Luminaire Cobra-head fixture housing
        ProceduralGarage::addBox(verts, lampEndX, armBaseY + 0.35f, lampEndZ, 0.35f, 0.12f, 0.65f, 0.75f, 0.78f, 0.82f);

        // Glowing LED lens / High-Pressure Sodium Lamp
        if (config.isStreetlightOn) {
            // Emissive warm bright yellow glow (#FFF176 / #FFEE58)
            ProceduralGarage::addBox(verts, lampEndX, armBaseY + 0.26f, lampEndZ, 0.28f, 0.06f, 0.52f, 1.0f, 0.94f, 0.35f);
        } else {
            // Unlit dark glass
            ProceduralGarage::addBox(verts, lampEndX, armBaseY + 0.26f, lampEndZ, 0.28f, 0.06f, 0.52f, 0.40f, 0.40f, 0.35f);
        }
    }

    return verts;
}

std::vector<Vertex3D> ProceduralPowerGrid::buildPowerGridNetwork(const GridNetworkConfig& config) {
    std::vector<Vertex3D> verts;

    int postCount = (config.numPosts >= 3) ? config.numPosts : 4;
    float span = config.spanDistanceM;
    float cornerAngleRad = config.cornerAngleDeg * 3.14159265f / 180.0f;

    // Grid Node Layout:
    // Pole 1: Tangent Straight Run Post at start
    // Pole 2: Transformer Distribution Post (in-line)
    // Pole 3: CORNER ANGLE POST (turns line by cornerAngleRad toward +Z)
    // Pole 4: Terminal / Dead-End Post (at end of new direction)

    struct PoleNode {
        float x, y, z;
        PostConfig cfg;
        float yawRad;
    };

    std::vector<PoleNode> nodes;

    // Node 0: Tangent Post at (-span * 1.5, 0, 0)
    PostConfig cfg0;
    cfg0.type = PowerPostType::TANGENT_STRAIGHT;
    cfg0.hasTransformer = false;
    cfg0.hasStreetlight = config.hasStreetlights;
    cfg0.isStreetlightOn = config.streetlightGlow;
    cfg0.hasGuyWire = false;
    nodes.push_back({-span * 1.35f, 0.0f, 0.0f, cfg0, 0.0f});

    // Node 1: Transformer Post at (0, 0, 0)
    PostConfig cfg1;
    cfg1.type = PowerPostType::TRANSFORMER_DISTRIBUTION;
    cfg1.hasTransformer = true;
    cfg1.hasStreetlight = false;
    cfg1.hasGuyWire = false;
    nodes.push_back({0.0f, 0.0f, 0.0f, cfg1, 0.0f});

    // Node 2: Corner Angle Post at (span, 0, 0)
    PostConfig cfg2;
    cfg2.type = PowerPostType::CORNER_ANGLE;
    cfg2.hasTransformer = false;
    cfg2.hasStreetlight = config.hasStreetlights;
    cfg2.isStreetlightOn = config.streetlightGlow;
    cfg2.hasGuyWire = true;
    cfg2.cornerAngleDeg = config.cornerAngleDeg;
    // Bisector orientation for the corner crossarms
    float cornerBisector = cornerAngleRad * 0.5f;
    nodes.push_back({span * 1.15f, 0.0f, 0.0f, cfg2, cornerBisector});

    // Node 3: Terminal Dead-End Post along the turned direction
    if (postCount >= 4) {
        float dirX = std::cos(cornerAngleRad);
        float dirZ = std::sin(cornerAngleRad);
        float p3X = (span * 1.15f) + dirX * (span * 1.05f);
        float p3Z = 0.0f + dirZ * (span * 1.05f);

        PostConfig cfg3;
        cfg3.type = PowerPostType::DEAD_END_TERMINAL;
        cfg3.hasTransformer = false;
        cfg3.hasStreetlight = config.hasStreetlights;
        cfg3.isStreetlightOn = config.streetlightGlow;
        cfg3.hasGuyWire = true;
        nodes.push_back({p3X, 0.0f, p3Z, cfg3, cornerAngleRad});
    }

    // 1. Build and instantiate all poles
    for (const auto& node : nodes) {
        auto poleVerts = buildSinglePost(node.cfg, node.x, node.y, node.z, node.yawRad);
        verts.insert(verts.end(), poleVerts.begin(), poleVerts.end());
    }

    // 2. Connect Continuous Overhead Wires between each adjacent pole span
    for (size_t s = 0; s + 1 < nodes.size(); ++s) {
        const auto& pA = nodes[s];
        const auto& pB = nodes[s + 1];

        // Wire attachment offsets on crossarm:
        // Left Phase A: local X = -0.9m
        // Center Phase B: atop top ridge pin at Y = 11.1m
        // Right Phase C: local X = +0.9m
        // Neutral Static Ground: center Y = 11.4m

        float cosA = std::cos(pA.yawRad); float sinA = std::sin(pA.yawRad);
        float cosB = std::cos(pB.yawRad); float sinB = std::sin(pB.yawRad);

        float armYA = pA.y + 10.15f;
        float armYB = pB.y + 10.15f;
        float offW = 1.0f;

        // Phase A attachment points
        float pA1_X = pA.x + (-offW * cosA);
        float pA1_Z = pA.z + (-offW * sinA);
        float pB1_X = pB.x + (-offW * cosB);
        float pB1_Z = pB.z + (-offW * sinB);

        // Phase B (Center Ridge) attachment points
        float pA2_X = pA.x; float pA2_Z = pA.z; float pA2_Y = pA.y + 11.15f;
        float pB2_X = pB.x; float pB2_Z = pB.z; float pB2_Y = pB.y + 11.15f;

        // Phase C attachment points
        float pA3_X = pA.x + (offW * cosA);
        float pA3_Z = pA.z + (offW * sinA);
        float pB3_X = pB.x + (offW * cosB);
        float pB3_Z = pB.z + (offW * sinB);

        // Ground/Neutral Messenger Wire (at Y = 8.8m)
        float pAgnd_Y = pA.y + 8.8f;
        float pBgnd_Y = pB.y + 8.8f;

        float wireR = config.energized ? 0.22f : 0.35f;
        float wireG = config.energized ? 0.25f : 0.38f;
        float wireB = config.energized ? 0.30f : 0.42f;

        // Add 3 high-voltage phase conductors with catenary sag
        addCatenaryWire(verts, pA1_X, armYA, pA1_Z, pB1_X, armYB, pB1_Z, config.wireSagM, 10, config.wireThickness, wireR, wireG, wireB);
        addCatenaryWire(verts, pA2_X, pA2_Y, pA2_Z, pB2_X, pB2_Y, pB2_Z, config.wireSagM * 0.9f, 10, config.wireThickness, wireR, wireG, wireB);
        addCatenaryWire(verts, pA3_X, armYA, pA3_Z, pB3_X, armYB, pB3_Z, config.wireSagM, 10, config.wireThickness, wireR, wireG, wireB);

        // Add neutral messenger wire with tighter sag
        addCatenaryWire(verts, pA.x, pAgnd_Y, pA.z, pB.x, pBgnd_Y, pB.z, config.wireSagM * 0.6f, 8, config.wireThickness * 0.85f, 0.45f, 0.48f, 0.52f);
    }

    return verts;
}

} // namespace procedural
} // namespace apex
