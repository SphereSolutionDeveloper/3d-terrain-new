#pragma once

#include <vector>
#include <string>
#include "procedural_garage.hpp"

namespace apex {
namespace procedural {

struct VehicleConfig {
    bool headlightsOn = true;
    bool taillightsOn = true;
    float steerAngleDeg = 0.0f; // Front wheels steering angle
};

class ProceduralVehicle {
public:
    // Builds the Proving Ground Box Lorry procedural mesh
    static std::vector<Vertex3D> buildTestLorry(const VehicleConfig& config = VehicleConfig{});

    // Key dimensions in meters
    static constexpr float TOTAL_LENGTH = 5.8f;
    static constexpr float TOTAL_WIDTH = 2.2f;
    static constexpr float TOTAL_HEIGHT = 3.3f;

    static constexpr float CHASSIS_LENGTH = 5.8f;
    static constexpr float CHASSIS_WIDTH = 2.2f;
    static constexpr float CHASSIS_HEIGHT = 0.4f;

    static constexpr float CAB_LENGTH = 1.8f;
    static constexpr float CAB_WIDTH = 2.1f;
    static constexpr float CAB_HEIGHT = 1.8f;

    static constexpr float CARGO_LENGTH = 3.8f;
    static constexpr float CARGO_WIDTH = 2.3f;
    static constexpr float CARGO_HEIGHT = 2.4f;

    static constexpr float WHEEL_DIAMETER = 0.9f;
    static constexpr float WHEEL_WIDTH = 0.45f;
};

} // namespace procedural
} // namespace apex
