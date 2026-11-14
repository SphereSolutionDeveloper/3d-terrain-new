// ─── app/src/main/cpp/continuous_cable_cylinder.cpp ──────────────────────────
#include "continuous_cable_cylinder.h"
#include <iostream>

namespace apex {

// Factory helper to generate complete 3-phase overhead continuous cylinder transmission lines
void generateContinuous3PhaseTransmissionLine(
    const Vec3D& poleA,
    const Vec3D& poleB,
    float tensionKn,
    float linearMassKgM,
    float conductorRadiusM,
    uint32_t radialSides,
    uint32_t segmentsPerSpan,
    std::vector<ContinuousCylinderMesh>& outCylinders
) {
    outCylinders.clear();
    outCylinders.reserve(3);

    // Crossarm width offsets (Phase A Left = -1.0m, Phase B Center Ridge = 0.0m + 1.2m higher, Phase C Right = +1.0m)
    Vec3D spanDir = (poleB - poleA).normalized();
    Vec3D lateral(spanDir.z, 0.0f, -spanDir.x); // Perpendicular to transmission corridor

    // Phase A (Left Conductor)
    Vec3D pA_left = poleA + lateral * 1.0f + Vec3D(0, 10.05f, 0);
    Vec3D pB_left = poleB + lateral * 1.0f + Vec3D(0, 10.05f, 0);
    ContinuousCableBendingFunction curveA(pA_left, pB_left, tensionKn, linearMassKgM);
    ContinuousCylinderMesh meshA(conductorRadiusM, radialSides, segmentsPerSpan);
    meshA.generate(curveA, 0.95f, 0.65f, 0.18f); // Energized amber
    outCylinders.push_back(meshA);

    // Phase B (Center Ridge Conductor - Higher elevation)
    Vec3D pA_center = poleA + Vec3D(0, 11.25f, 0);
    Vec3D pB_center = poleB + Vec3D(0, 11.25f, 0);
    ContinuousCableBendingFunction curveB(pA_center, pB_center, tensionKn, linearMassKgM);
    ContinuousCylinderMesh meshB(conductorRadiusM, radialSides, segmentsPerSpan);
    meshB.generate(curveB, 0.95f, 0.65f, 0.18f);
    outCylinders.push_back(meshB);

    // Phase C (Right Conductor)
    Vec3D pA_right = poleA - lateral * 1.0f + Vec3D(0, 10.05f, 0);
    Vec3D pB_right = poleB - lateral * 1.0f + Vec3D(0, 10.05f, 0);
    ContinuousCableBendingFunction curveC(pA_right, pB_right, tensionKn, linearMassKgM);
    ContinuousCylinderMesh meshC(conductorRadiusM, radialSides, segmentsPerSpan);
    meshC.generate(curveC, 0.95f, 0.65f, 0.18f);
    outCylinders.push_back(meshC);
}

} // namespace apex
