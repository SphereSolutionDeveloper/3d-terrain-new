# Technical Reference & Complete Implementation Guide: Proving Ground Garage 001 & Test Vehicle

---

## 1. Executive Summary & Architecture Overview

The **Proving Ground** in TransportSim features a high-performance, real-time 3D simulation environment built upon a dual-engine architecture:
1. **Primary Runtime (C++ / OpenGL ES 3.0)**: Located in `native/src/main/cpp/renderer/Renderer.cpp`, responsible for the live 60 FPS vehicle dynamics, terrain rendering, road ribbons, bridge structures, the **SE Garage Depot (Garage 001)**, and the **Test Truck**.
2. **Interactive UI Viewport (Kotlin Jetpack Compose Canvas)**: Located in `app/src/main/java/com/transportsim/app/ui/test/GarageAssetViewport.kt`, providing an interactive 3D orthographic/isometric projection of the exact same depot and parked test vehicle directly inside Compose UI for inspection, door controls, and lighting toggles.
3. **Asset Export Pipeline (Python glTF 2.0 Exporter)**: Located in `dev scripts/export_garage_glb.py` and `dev scripts/generate_vehicle_glbs.py`, translating the mathematical primitives into standard binary `.glb` containers for external 3D engines and viewers.

Neither the Garage Depot nor the Test Vehicle in the live Proving Ground are loaded from external mesh files. They are generated **purely procedurally** via mathematical box extrusions with per-vertex normals and colors, resulting in zero disk I/O, zero file parsing latency, and zero APK bloat.

---

## 2. World Spatial Coordinates & Dimension Specs

### Garage 001 (SE Depot)
* **World Center Anchor**: $(X = 316.0\,\text{m},\, Z = 296.0\,\text{m})$
* **Base Ground Elevation**: Computed dynamically via `getProceduralHeight(316.0f, 296.0f)` (approx. $+1700\,\text{m}$ base datum).
* **Main Warehouse**: $14.0\,\text{m}\,(W) \times 6.0\,\text{m}\,(H) \times 22.0\,\text{m}\,(L)$ in Steel Slate Gray (`#384252`).
* **Roof Trim**: $15.0\,\text{m} \times 0.4\,\text{m} \times 23.0\,\text{m}$ in Crimson Red (`#D93838`).
* **Service Bay Rollup Doors**: Two doors on the West wall ($X = 309.0\,\text{m}$) at $Z = 292.0\,\text{m}$ and $Z = 300.0\,\text{m}$, each $5.0\,\text{m} \times 3.6\,\text{m}$ in Silver Aluminum (`#BFC7D1`).
* **Emissive Workshop Banner**: $0.1\,\text{m} \times 1.0\,\text{m} \times 10.0\,\text{m}$ at $Y = +4.8\,\text{m}$, Cyan Accent (`#00BFE0`).
* **Paved Service Apron**: $28.0\,\text{m} \times 0.12\,\text{m} \times 24.0\,\text{m}$ extending West from $X=281$ to $309\,\text{m}$, Asphalt Dark (`#333840`).
* **Yellow Bay Guidelines**: Two $14.0\,\text{m} \times 0.22\,\text{m}$ safety lines aligned with the service bays.
* **Connecting Driveway**: $24.0\,\text{m} \times 0.12\,\text{m} \times 14.0\,\text{m}$ joining the apron to the $1000\,\text{m}$ superelliptic test loop.

### Proving Ground Test Vehicle (Box Lorry)
* **Default Staging Position**: $(X = 300.0\,\text{m},\, Z = 300.0\,\text{m})$ facing West towards the road ribbon.
* **Total Envelope**: Length $5.8\,\text{m}$, Width $2.2\,\text{m}$, Height $3.3\,\text{m}$.
* **Chassis Frame**: $5.8\,\text{m} \times 0.4\,\text{m} \times 2.2\,\text{m}$ centered at $Y = +0.65\,\text{m}$, Charcoal Slate (`#1F242E`).
* **Wheels**: 4 Heavy-duty wheels, diameter $0.9\,\text{m}$, tread width $0.45\,\text{m}$, at $(\pm 1.8\,\text{m},\, \pm 1.15\,\text{m})$, Matte Black (`#141414`).
* **Driver Cab**: $1.8\,\text{m} \times 1.8\,\text{m} \times 2.1\,\text{m}$ at front ($X = +1.8\,\text{m}$), Electric Cyan (`#00BCD4`).
* **Windshield & Side Windows**: Dark Tint Glass (`#1F2E47`).
* **Freight Cargo Box**: $3.8\,\text{m} \times 2.4\,\text{m} \times 2.3\,\text{m}$ centered at $X = -0.9\,\text{m}$, Clean Silver White (`#EBF0F5`).
* **Livery Stripes**: Cyan accents matching cab running along container flanks.
* **Lighting**: Dual Amber Front Headlights (`#FFE033`) and Dual Crimson Rear Taillights (`#E61A1A`).

---

## 3. Full Implementation: Native C++ / OpenGL ES 3.0 Engine

### 3.1 Vertex Data Structure & Header (`Renderer.h`)

```cpp
#pragma once

#include <cstdint>
#include <memory>
#include <vector>
#include <jni.h>
#ifdef __ANDROID__
#include <android/native_window.h>
#else
struct ANativeWindow;
#endif
#include <Eigen/Dense>
#include "../physics/PhysicsEngine.h"

// 36 bytes per vertex with position, surface normal, and RGBA color
struct Vertex3D {
    float x, y, z;       // Position in meters (Offset 0)
    float nx, ny, nz;    // Normal vector for lighting (Offset 12)
    float r, g, b, a;    // Color components in [0.0, 1.0] (Offset 24)
};

class Renderer {
public:
    Renderer();
    ~Renderer();

    bool init(ANativeWindow* window, int width, int height, jobject assetManager);
    bool initGLES(int width, int height);
    void resize(int width, int height);

    bool renderFrame(const VehicleState& state);
    void setCamera(const Eigen::Vector3f& eye, const Eigen::Vector3f& target);
    void rotateCamera(float deltaYawDeg, float deltaPitchDeg);
    void zoomCamera(float deltaDist);
    void panCamera(float deltaX, float deltaZ);
    void setCameraOrbit(float yawDeg, float pitchDeg, float distanceM);
    void resetCamera();

    void shutdown();

private:
    struct Impl;
    std::unique_ptr<Impl> pImpl;

    bool initShaders();
    void buildProceduralMeshes();
    void addBox(std::vector<Vertex3D>& verts, float cx, float cy, float cz,
                float sx, float sy, float sz,
                float r, float g, float b, float a = 1.0f);

    void renderScene();
    void renderVehicle(const VehicleState& state);
    void renderTerrain();
    void renderRoad();
    void renderBridge();
    void renderGarage();

    unsigned int m_prog = 0;
    unsigned int m_vao = 0;

    unsigned int m_garageVbo = 0;
    int m_garageVertCount = 0;

    unsigned int m_truckVbo = 0;
    int m_truckVertCount = 0;

    int m_uMVPLoc = -1;
    int m_uModelLoc = -1;

    bool m_isInitialized = false;
    int m_width = 1080;
    int m_height = 1920;
    Eigen::Matrix4f m_viewMatrix;
    Eigen::Matrix4f m_projMatrix;

    float m_camYaw = 0.0f;
    float m_camPitch = 22.0f;
    float m_camDistance = 9.0f;
    float m_panX = 0.0f;
    float m_panZ = 0.0f;
};
```

---

### 3.2 3D Box Extrusion Algorithm (`Renderer.cpp`)

Every 3D component is generated by `addBox()`. It calculates all 6 faces (Front, Back, Top, Bottom, Right, Left), applies correct outward unit normals, and appends 36 vertices (2 triangles per face) to the target vertex array.

```cpp
void Renderer::addBox(std::vector<Vertex3D>& verts, float cx, float cy, float cz,
                     float sx, float sy, float sz,
                     float r, float g, float b, float a) {
    float hx = sx * 0.5f;
    float hy = sy * 0.5f;
    float hz = sz * 0.5f;

    struct Face {
        float nx, ny, nz;
        float v[4][3];
    };

    Face faces[6] = {
        // Front (+Z)
        {0, 0, 1, {{-hx, -hy, hz}, {hx, -hy, hz}, {hx, hy, hz}, {-hx, hy, hz}}},
        // Back (-Z)
        {0, 0, -1, {{hx, -hy, -hz}, {-hx, -hy, -hz}, {-hx, hy, -hz}, {hx, hy, -hz}}},
        // Top (+Y)
        {0, 1, 0, {{-hx, hy, hz}, {hx, hy, hz}, {hx, hy, -hz}, {-hx, hy, -hz}}},
        // Bottom (-Y)
        {0, -1, 0, {{-hx, -hy, -hz}, {hx, -hy, -hz}, {hx, -hy, hz}, {-hx, -hy, hz}}},
        // Right (+X)
        {1, 0, 0, {{hx, -hy, hz}, {hx, -hy, -hz}, {hx, hy, -hz}, {hx, hy, hz}}},
        // Left (-X)
        {-1, 0, 0, {{-hx, -hy, -hz}, {-hx, -hy, hz}, {-hx, hy, hz}, {-hx, hy, -hz}}}
    };

    for (int i = 0; i < 6; ++i) {
        const auto& f = faces[i];
        // 2 Triangles per quad (indices: 0, 1, 2, 0, 2, 3)
        int idxs[6] = {0, 1, 2, 0, 2, 3};
        for (int k = 0; k < 6; ++k) {
            int vi = idxs[k];
            Vertex3D vert;
            vert.x = cx + f.v[vi][0];
            vert.y = cy + f.v[vi][1];
            vert.z = cz + f.v[vi][2];
            vert.nx = f.nx;
            vert.ny = f.ny;
            vert.nz = f.nz;
            vert.r = r;
            vert.g = g;
            vert.b = b;
            vert.a = a;
            verts.push_back(vert);
        }
    }
}
```

---

### 3.3 Shaders with Ambient and Directional Lighting

```cpp
bool Renderer::initShaders() {
    const char* vsSource = R"(#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec4 aColor;

uniform mat4 uMVP;
uniform mat4 uModel;

out vec3 vNormal;
out vec4 vColor;

void main() {
    vNormal = mat3(uModel) * aNormal;
    vColor = aColor;
    gl_Position = uMVP * vec4(aPos, 1.0);
}
)";

    const char* fsSource = R"(#version 300 es
precision mediump float;

in vec3 vNormal;
in vec4 vColor;

out vec4 FragColor;

void main() {
    // Sunlight vector from high southwest sky
    vec3 lightDir = normalize(vec3(0.45, 0.85, 0.28));
    vec3 norm = normalize(vNormal);
    float diff = max(dot(norm, lightDir), 0.22);
    vec3 ambient = vec3(0.38, 0.42, 0.48);
    vec3 lit = (ambient + diff * vec3(0.85, 0.82, 0.76)) * vColor.rgb;
    FragColor = vec4(lit, vColor.a);
}
)";

    GLuint vs = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vs, 1, &vsSource, nullptr);
    glCompileShader(vs);

    GLuint fs = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fs, 1, &fsSource, nullptr);
    glCompileShader(fs);

    m_prog = glCreateProgram();
    glAttachShader(m_prog, vs);
    glAttachShader(m_prog, fs);
    glLinkProgram(m_prog);

    glDeleteShader(vs);
    glDeleteShader(fs);

    m_uMVPLoc = glGetUniformLocation(m_prog, "uMVP");
    m_uModelLoc = glGetUniformLocation(m_prog, "uModel");
    return m_prog != 0;
}
```

---

### 3.4 Garage 001 Construction Code (`Renderer.cpp`)

```cpp
// SE Garage Depot at (316, 296)
std::vector<Vertex3D> garageVerts;
float garageY = getProceduralHeight(316.0f, 296.0f);

// 1. Main Depot Warehouse (Center at 316, Y+3, 296; front wall facing West is at X=309.0m)
addBox(garageVerts, 316.0f, garageY + 3.0f, 296.0f, 14.0f, 6.0f, 22.0f, 0.22f, 0.26f, 0.32f);

// 2. Roof with crimson red trim
addBox(garageVerts, 316.0f, garageY + 6.2f, 296.0f, 15.0f, 0.4f, 23.0f, 0.85f, 0.22f, 0.22f);

// 3. Service Bay Rollup Doors (facing West at X=309.0m towards road)
addBox(garageVerts, 309.0f, garageY + 1.8f, 292.0f, 0.2f, 3.6f, 5.0f, 0.75f, 0.78f, 0.82f);
addBox(garageVerts, 309.0f, garageY + 1.8f, 300.0f, 0.2f, 3.6f, 5.0f, 0.75f, 0.78f, 0.82f);

// 4. Workshop sign banner
addBox(garageVerts, 308.8f, garageY + 4.8f, 296.0f, 0.1f, 1.0f, 10.0f, 0.0f, 0.75f, 0.88f);

// 5. Paved garage apron in front of service bays (from X=281 to 309, Z=284 to 308)
float apronY = getProceduralHeight(295.0f, 296.0f) + 0.06f;
addBox(garageVerts, 295.0f, apronY, 296.0f, 28.0f, 0.12f, 24.0f, 0.20f, 0.22f, 0.25f);

// 6. Painted yellow bay guidelines on the apron
addBox(garageVerts, 295.0f, apronY + 0.08f, 292.0f, 14.0f, 0.02f, 0.22f, 1.0f, 0.85f, 0.2f);
addBox(garageVerts, 295.0f, apronY + 0.08f, 300.0f, 14.0f, 0.02f, 0.22f, 1.0f, 0.85f, 0.2f);

// 7. Connecting driveway leading from apron onto the road loop ribbon (X=258 to 282, Z=273 to 287)
float driveY = getProceduralHeight(270.0f, 280.0f) + 0.08f;
addBox(garageVerts, 270.0f, driveY, 280.0f, 24.0f, 0.12f, 14.0f, 0.18f, 0.20f, 0.24f);

m_garageVertCount = static_cast<int>(garageVerts.size());
glGenBuffers(1, &m_garageVbo);
glBindBuffer(GL_ARRAY_BUFFER, m_garageVbo);
glBufferData(GL_ARRAY_BUFFER, garageVerts.size() * sizeof(Vertex3D), garageVerts.data(), GL_STATIC_DRAW);
```

---

### 3.5 Proving Ground Box Lorry Construction Code (`Renderer.cpp`)

```cpp
// The Test Truck (Positioned at 300, 300 on the road surface near the garage)
std::vector<Vertex3D> truckVerts;

// 1. Heavy Chassis Frame (Y=0.65m)
addBox(truckVerts, 0.0f, 0.65f, 0.0f, 5.8f, 0.4f, 2.2f, 0.12f, 0.14f, 0.18f);

// 2. 4 Heavy-Duty Wheels (resting on ground Y=0, wheel radius 0.45m)
addBox(truckVerts, -1.8f, 0.45f, -1.15f, 0.9f, 0.9f, 0.45f, 0.08f, 0.08f, 0.08f);
addBox(truckVerts, -1.8f, 0.45f,  1.15f, 0.9f, 0.9f, 0.45f, 0.08f, 0.08f, 0.08f);
addBox(truckVerts,  1.8f, 0.45f, -1.15f, 0.9f, 0.9f, 0.45f, 0.08f, 0.08f, 0.08f);
addBox(truckVerts,  1.8f, 0.45f,  1.15f, 0.9f, 0.9f, 0.45f, 0.08f, 0.08f, 0.08f);

// 3. Driver's Cab (Cyan cab body at front)
addBox(truckVerts, 1.8f, 1.75f, 0.0f, 1.8f, 1.8f, 2.1f, 0.0f, 0.74f, 0.83f);

// 4. Dark Windshield & Side Windows
addBox(truckVerts, 2.72f, 1.95f, 0.0f, 0.05f, 0.9f, 1.8f, 0.12f, 0.18f, 0.28f);
addBox(truckVerts, 1.8f, 1.95f,  1.06f, 1.0f, 0.8f, 0.05f, 0.12f, 0.18f, 0.28f);
addBox(truckVerts, 1.8f, 1.95f, -1.06f, 1.0f, 0.8f, 0.05f, 0.12f, 0.18f, 0.28f);

// 5. Large Cargo Box (Freight Container in clean silver-white)
addBox(truckVerts, -0.9f, 2.1f, 0.0f, 3.8f, 2.4f, 2.3f, 0.92f, 0.94f, 0.96f);

// 6. Cargo Stripe Accents (Cyan livery flanking stripes)
addBox(truckVerts, -0.9f, 1.8f,  1.16f, 3.82f, 0.4f, 0.02f, 0.0f, 0.74f, 0.83f);
addBox(truckVerts, -0.9f, 1.8f, -1.16f, 3.82f, 0.4f, 0.02f, 0.0f, 0.74f, 0.83f);

// 7. Dual Headlights (Golden amber)
addBox(truckVerts, 2.72f, 1.1f,  0.75f, 0.05f, 0.3f, 0.4f, 1.0f, 0.88f, 0.2f);
addBox(truckVerts, 2.72f, 1.1f, -0.75f, 0.05f, 0.3f, 0.4f, 1.0f, 0.88f, 0.2f);

// 8. Dual Taillights (Crimson red)
addBox(truckVerts, -2.82f, 1.0f,  0.8f, 0.05f, 0.25f, 0.35f, 0.9f, 0.1f, 0.1f);
addBox(truckVerts, -2.82f, 1.0f, -0.8f, 0.05f, 0.25f, 0.35f, 0.9f, 0.1f, 0.1f);

m_truckVertCount = static_cast<int>(truckVerts.size());
glGenBuffers(1, &m_truckVbo);
glBindBuffer(GL_ARRAY_BUFFER, m_truckVbo);
glBufferData(GL_ARRAY_BUFFER, truckVerts.size() * sizeof(Vertex3D), truckVerts.data(), GL_STATIC_DRAW);
```

---

### 3.6 Dynamic Vehicle & Garage Rendering Passes (`Renderer.cpp`)

```cpp
void Renderer::renderVehicle(const VehicleState& state) {
    if (m_truckVbo == 0 || m_truckVertCount == 0) return;

    // Position the truck at state.pos on road/paved surface
    Eigen::Vector3f truckPos = state.pos;
    truckPos.y() = getProceduralHeight(truckPos.x(), truckPos.z()) + 0.06f; // Sit firmly on surface

    Eigen::Matrix4f model = Eigen::Matrix4f::Identity();
    // Translation to road position
    model.block<3, 1>(0, 3) = truckPos;

    // Orientation from vehicle dynamics (6-DOF quaternion to 3x3 rotation matrix)
    Eigen::Matrix3f rot = state.orientation.toRotationMatrix();
    model.block<3, 3>(0, 0) = rot;

    Eigen::Matrix4f mvp = m_projMatrix * m_viewMatrix * model;
    glUniformMatrix4fv(m_uMVPLoc, 1, GL_FALSE, mvp.data());
    glUniformMatrix4fv(m_uModelLoc, 1, GL_FALSE, model.data());

    glBindBuffer(GL_ARRAY_BUFFER, m_truckVbo);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, sizeof(Vertex3D), (void*)0);
    glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, sizeof(Vertex3D), (void*)12);
    glVertexAttribPointer(2, 4, GL_FLOAT, GL_FALSE, sizeof(Vertex3D), (void*)24);

    glDrawArrays(GL_TRIANGLES, 0, m_truckVertCount);
}

void Renderer::renderGarage() {
    if (m_garageVbo == 0) return;
    Eigen::Matrix4f model = Eigen::Matrix4f::Identity();
    Eigen::Matrix4f mvp = m_projMatrix * m_viewMatrix * model;
    glUniformMatrix4fv(m_uMVPLoc, 1, GL_FALSE, mvp.data());
    glUniformMatrix4fv(m_uModelLoc, 1, GL_FALSE, model.data());

    glBindBuffer(GL_ARRAY_BUFFER, m_garageVbo);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, sizeof(Vertex3D), (void*)0);
    glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, sizeof(Vertex3D), (void*)12);
    glVertexAttribPointer(2, 4, GL_FLOAT, GL_FALSE, sizeof(Vertex3D), (void*)24);

    glDrawArrays(GL_TRIANGLES, 0, m_garageVertCount);
}
```

---

### 3.7 Frame Render Loop & Camera Collision Guard (`Renderer.cpp`)

```cpp
bool Renderer::renderFrame(const VehicleState& state) {
    if (!m_isInitialized) {
        initGLES(m_width, m_height);
    }
    if (!m_isInitialized || m_prog == 0) return false;

    glViewport(0, 0, m_width, m_height);

    // Atmospheric sky background (pleasant sunny daylight)
    glClearColor(0.48f, 0.70f, 0.90f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LEQUAL);
    glDepthMask(GL_TRUE);
    glDisable(GL_CULL_FACE);

    if (m_vao != 0) {
        glBindVertexArray(m_vao);
    }

    glUseProgram(m_prog);
    glEnableVertexAttribArray(0);
    glEnableVertexAttribArray(1);
    glEnableVertexAttribArray(2);

    // Vehicle position on ground surface
    Eigen::Vector3f truckPos = state.pos;
    truckPos.y() = getProceduralHeight(truckPos.x(), truckPos.z()) + 0.06f;

    // Vehicle forward vector
    Eigen::Vector3f forward = state.orientation * Eigen::Vector3f(1.0f, 0.0f, 0.0f);
    if (forward.squaredNorm() < 1e-4f) {
        forward = Eigen::Vector3f(1.0f, 0.0f, 0.0f);
    }
    forward.normalize();

    // Base heading angle of vehicle in XZ plane
    float baseHeading = atan2f(forward.z(), forward.x());

    // Spherical orbit calculation
    float totalYaw = baseHeading + (float)M_PI + (m_camYaw * (float)M_PI / 180.0f);
    float pitchRad = m_camPitch * (float)M_PI / 180.0f;

    float horizDist = m_camDistance * cosf(pitchRad);
    float vertHeight = m_camDistance * sinf(pitchRad);

    // Camera target is center of vehicle plus pan
    Eigen::Vector3f target = truckPos;
    target.y() += 1.2f;
    target.x() += m_panX;
    target.z() += m_panZ;

    // Eye position calculated from target + spherical coordinates
    Eigen::Vector3f eye;
    eye.x() = target.x() + horizDist * cosf(totalYaw);
    eye.z() = target.z() + horizDist * sinf(totalYaw);
    eye.y() = target.y() + vertHeight;

    // Ground clearance guard: keep eye comfortably above terrain
    float groundAtEye = getProceduralHeight(eye.x(), eye.z());
    if (eye.y() < groundAtEye + 0.8f) {
        eye.y() = groundAtEye + 0.8f;
    }

    // Garage warehouse building clearance guard:
    // Front wall is at X = 309. If camera eye approaches garage, clamp X to prevent clipping inside
    if (eye.x() > 306.5f && eye.z() >= 282.0f && eye.z() <= 310.0f) {
        eye.x() = 306.5f;
    }

    setCamera(eye, target);

    // Render 3D Environment (Terrain chunks, Road loop, Bridge, SE Garage)
    renderScene();

    // Render dynamic vehicle
    renderVehicle(state);

    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glDisableVertexAttribArray(2);

    if (m_vao != 0) {
        glBindVertexArray(0);
    }

    if (pImpl->display != EGL_NO_DISPLAY && pImpl->surface != EGL_NO_SURFACE) {
        eglSwapBuffers(pImpl->display, pImpl->surface);
    }

    return true;
}
```

---

## 4. Full Implementation: Compose Canvas Viewport (`GarageAssetViewport.kt`)

The Jetpack Compose viewport projects the exact same garage dimensions into interactive 2D canvas coordinates using 3D rotation mathematics.

```kotlin
// ─── Native Renderer Box Projection Helpers ──────────────────────────────────

private fun proj(
    x: Float, y: Float, z: Float,
    yaw: Float, pitch: Float,
    scale: Float, cx: Float, cy: Float
): Offset {
    // Center world coordinates around depot center (X=308, Y=0, Z=296)
    val relX = x - 308f
    val relY = y
    val relZ = z - 296f

    val cosY = cos(yaw)
    val sinY = sin(yaw)
    val x1 = relX * cosY - relZ * sinY
    val z1 = relX * sinY + relZ * cosY

    val cosP = cos(pitch)
    val sinP = sin(pitch)
    val y2 = relY * cosP - z1 * sinP

    return Offset(
        x = cx + x1 * scale,
        y = cy - y2 * scale
    )
}

private fun DrawScope.drawSolidBox(
    cx: Float, cy: Float, scale: Float,
    yaw: Float, pitch: Float,
    centerX: Float, centerY: Float, centerZ: Float,
    sizeX: Float, sizeY: Float, sizeZ: Float,
    baseColor: Color
) {
    val hx = sizeX * 0.5f
    val hy = sizeY * 0.5f
    val hz = sizeZ * 0.5f

    // 8 Box Vertices
    val c000 = proj(centerX - hx, centerY - hy, centerZ - hz, yaw, pitch, scale, cx, cy)
    val c100 = proj(centerX + hx, centerY - hy, centerZ - hz, yaw, pitch, scale, cx, cy)
    val c110 = proj(centerX + hx, centerY + hy, centerZ - hz, yaw, pitch, scale, cx, cy)
    val c010 = proj(centerX - hx, centerY + hy, centerZ - hz, yaw, pitch, scale, cx, cy)

    val c001 = proj(centerX - hx, centerY - hy, centerZ + hz, yaw, pitch, scale, cx, cy)
    val c101 = proj(centerX + hx, centerY - hy, centerZ + hz, yaw, pitch, scale, cx, cy)
    val c111 = proj(centerX + hx, centerY + hy, centerZ + hz, yaw, pitch, scale, cx, cy)
    val c011 = proj(centerX - hx, centerY + hy, centerZ + hz, yaw, pitch, scale, cx, cy)

    // Top Face (+Y)
    val pathTop = Path().apply {
        moveTo(c010.x, c010.y)
        lineTo(c110.x, c110.y)
        lineTo(c111.x, c111.y)
        lineTo(c011.x, c011.y)
        close()
    }
    drawPath(pathTop, color = baseColor.copy(alpha = 1.0f))

    // Front Face (Facing West / Apron, -X)
    val pathFront = Path().apply {
        moveTo(c000.x, c000.y)
        lineTo(c001.x, c001.y)
        lineTo(c011.x, c011.y)
        lineTo(c010.x, c010.y)
        close()
    }
    drawPath(pathFront, color = baseColor.copy(red = baseColor.red * 0.85f, green = baseColor.green * 0.85f, blue = baseColor.blue * 0.85f))

    // Side Face (Facing South, -Z)
    val pathSide = Path().apply {
        moveTo(c000.x, c000.y)
        lineTo(c100.x, c100.y)
        lineTo(c110.x, c110.y)
        lineTo(c010.x, c010.y)
        close()
    }
    drawPath(pathSide, color = baseColor.copy(red = baseColor.red * 0.70f, green = baseColor.green * 0.70f, blue = baseColor.blue * 0.70f))
}
```

### 4.1 Garage Structural Components & Parked Truck

```kotlin
private fun DrawScope.drawDepotApron(cx: Float, cy: Float, scale: Float, yaw: Float, pitch: Float) {
    // Paved apron: 28m (X) x 24m (Z) centered at X=295, Z=296
    drawSolidBox(
        cx, cy, scale, yaw, pitch,
        centerX = 295.0f, centerY = 0.06f, centerZ = 296.0f,
        sizeX = 28.0f, sizeY = 0.12f, sizeZ = 24.0f,
        baseColor = Color(0xFF2C3238)
    )
}

private fun DrawScope.drawBayGuidelines(cx: Float, cy: Float, scale: Float, yaw: Float, pitch: Float) {
    // Twin yellow guidelines: 14m x 0.22m at Z=292 and Z=300
    for (z in listOf(292.0f, 300.0f)) {
        drawSolidBox(
            cx, cy, scale, yaw, pitch,
            centerX = 295.0f, centerY = 0.14f, centerZ = z,
            sizeX = 14.0f, sizeY = 0.04f, sizeZ = 0.22f,
            baseColor = Color(0xFFFFD54F)
        )
    }
}

private fun DrawScope.drawConnectingDriveway(cx: Float, cy: Float, scale: Float, yaw: Float, pitch: Float) {
    // Driveway: 24m x 14m centered at X=270, Z=280
    drawSolidBox(
        cx, cy, scale, yaw, pitch,
        centerX = 270.0f, centerY = 0.08f, centerZ = 280.0f,
        sizeX = 24.0f, sizeY = 0.12f, sizeZ = 14.0f,
        baseColor = Color(0xFF262C33)
    )
}

private fun DrawScope.drawMainDepotBuilding(cx: Float, cy: Float, scale: Float, yaw: Float, pitch: Float) {
    // Main Depot Warehouse: 14m x 6m x 22m centered at X=316, Y=3.0, Z=296
    drawSolidBox(
        cx, cy, scale, yaw, pitch,
        centerX = 316.0f, centerY = 3.0f, centerZ = 296.0f,
        sizeX = 14.0f, sizeY = 6.0f, sizeZ = 22.0f,
        baseColor = Color(0xFF384252)
    )
}

private fun DrawScope.drawRedRoofTrim(cx: Float, cy: Float, scale: Float, yaw: Float, pitch: Float) {
    // Roof Trim: 15m x 0.4m x 23m centered at X=316, Y=6.2, Z=296
    drawSolidBox(
        cx, cy, scale, yaw, pitch,
        centerX = 316.0f, centerY = 6.2f, centerZ = 296.0f,
        sizeX = 15.0f, sizeY = 0.4f, sizeZ = 23.0f,
        baseColor = Color(0xFFD93838)
    )
}

private fun DrawScope.drawDepotRollupDoors(
    cx: Float, cy: Float, scale: Float,
    yaw: Float, pitch: Float,
    bay1OpenPct: Float, bay2OpenPct: Float
) {
    // Rollup doors on West wall (X=309): 5m (Z) x 3.6m (Y)
    val maxH = 3.6f
    // Bay 1 (Z=292)
    val h1 = maxH * (1.0f - bay1OpenPct)
    val y1 = 3.6f - (h1 * 0.5f)
    if (h1 > 0.05f) {
        drawSolidBox(
            cx, cy, scale, yaw, pitch,
            centerX = 309.0f, centerY = y1, centerZ = 292.0f,
            sizeX = 0.2f, sizeY = h1, sizeZ = 5.0f,
            baseColor = Color(0xFFBFC7D1)
        )
    }

    // Bay 2 (Z=300)
    val h2 = maxH * (1.0f - bay2OpenPct)
    val y2 = 3.6f - (h2 * 0.5f)
    if (h2 > 0.05f) {
        drawSolidBox(
            cx, cy, scale, yaw, pitch,
            centerX = 309.0f, centerY = y2, centerZ = 300.0f,
            sizeX = 0.2f, sizeY = h2, sizeZ = 5.0f,
            baseColor = Color(0xFFBFC7D1)
        )
    }
}

private fun DrawScope.drawWorkshopBanner(
    cx: Float, cy: Float, scale: Float,
    yaw: Float, pitch: Float,
    neonSign: Boolean
) {
    val bannerColor = if (neonSign) Color(0xFF00E5FF) else Color(0xFF00607A)
    drawSolidBox(
        cx, cy, scale, yaw, pitch,
        centerX = 308.8f, centerY = 4.8f, centerZ = 296.0f,
        sizeX = 0.15f, sizeY = 1.0f, sizeZ = 10.0f,
        baseColor = bannerColor
    )
}

private fun DrawScope.drawTestTruckOnApron(
    cx: Float, cy: Float, scale: Float,
    yaw: Float, pitch: Float
) {
    // Heavy truck parked on the apron facing road: X=288, Z=296
    // Chassis Frame
    drawSolidBox(
        cx, cy, scale, yaw, pitch,
        centerX = 288.0f, centerY = 0.65f, centerZ = 296.0f,
        sizeX = 5.8f, sizeY = 0.4f, sizeZ = 2.2f,
        baseColor = Color(0xFF212121)
    )
    // Cyan Cab
    drawSolidBox(
        cx, cy, scale, yaw, pitch,
        centerX = 286.2f, centerY = 1.75f, centerZ = 296.0f,
        sizeX = 1.8f, sizeY = 1.8f, sizeZ = 2.1f,
        baseColor = Color(0xFF00B0FF)
    )
}
```

---

## 5. Standalone Python Exporter Implementations

### 5.1 Standalone Garage 001 GLB Exporter (`export_garage_glb.py`)

This script converts all the boxes of Garage 001 into a self-contained, valid binary `glTF 2.0` (`.glb`) file.

```python
#!/usr/bin/env python3
"""
export_garage_glb.py
====================
Standalone developer tool to export the exact 3D Garage 001 architectural asset (.glb)
matching the Proving Ground SE Garage Depot from native Renderer.cpp.
"""

import os
import struct
import json

def build_box(min_x, min_y, min_z, max_x, max_y, max_z):
    faces = [
        # +Z (North / Back)
        ([(min_x, min_y, max_z), (max_x, min_y, max_z), (max_x, max_y, max_z), (min_x, max_y, max_z)], (0, 0, 1)),
        # -Z (South / Front)
        ([(max_x, min_y, min_z), (min_x, min_y, min_z), (min_x, max_y, min_z), (max_x, max_y, min_z)], (0, 0, -1)),
        # +Y (Top)
        ([(min_x, max_y, max_z), (max_x, max_y, max_z), (max_x, max_y, min_z), (min_x, max_y, min_z)], (0, 1, 0)),
        # -Y (Bottom)
        ([(min_x, min_y, min_z), (max_x, min_y, min_z), (max_x, min_y, max_z), (min_x, min_y, max_z)], (0, -1, 0)),
        # +X (East / Rear)
        ([(max_x, min_y, max_z), (max_x, min_y, min_z), (max_x, max_y, min_z), (max_x, max_y, max_z)], (1, 0, 0)),
        # -X (West / Front Apron)
        ([(min_x, min_y, min_z), (min_x, min_y, max_z), (min_x, max_y, max_z), (min_x, max_y, min_z)], (-1, 0, 0)),
    ]
    
    verts = []
    norms = []
    indices = []
    for quad, norm in faces:
        base_idx = len(verts)
        for pt in quad:
            verts.append(pt)
            norms.append(norm)
        indices.extend([base_idx, base_idx + 1, base_idx + 2, base_idx, base_idx + 2, base_idx + 3])
    return verts, norms, indices

def create_glb(boxes, color_rgb=(0.22, 0.26, 0.32), roughness=0.6, metallic=0.3):
    all_verts = []
    all_norms = []
    all_indices = []
    
    for (b_min, b_max) in boxes:
        v, n, idx = build_box(b_min[0], b_min[1], b_min[2], b_max[0], b_max[1], b_max[2])
        offset = len(all_verts)
        all_verts.extend(v)
        all_norms.extend(n)
        for i in idx:
            all_indices.append(i + offset)
            
    min_pos = [round(min(v[0] for v in all_verts), 3), round(min(v[1] for v in all_verts), 3), round(min(v[2] for v in all_verts), 3)]
    max_pos = [round(max(v[0] for v in all_verts), 3), round(max(v[1] for v in all_verts), 3), round(max(v[2] for v in all_verts), 3)]
    
    pos_bytes = bytearray()
    for x, y, z in all_verts:
        pos_bytes.extend(struct.pack('<fff', x, y, z))
        
    norm_bytes = bytearray()
    for nx, ny, nz in all_norms:
        norm_bytes.extend(struct.pack('<fff', nx, ny, nz))
        
    idx_bytes = bytearray()
    for i in all_indices:
        idx_bytes.extend(struct.pack('<H', i))
        
    while len(pos_bytes) % 4 != 0: pos_bytes.append(0)
    while len(norm_bytes) % 4 != 0: norm_bytes.append(0)
    while len(idx_bytes) % 4 != 0: idx_bytes.append(0)
    
    pos_offset = 0
    pos_len = len(pos_bytes)
    norm_offset = pos_len
    norm_len = len(norm_bytes)
    idx_offset = norm_offset + norm_len
    idx_len = len(idx_bytes)
    
    total_bin_len = idx_offset + idx_len
    bin_buffer = pos_bytes + norm_bytes + idx_bytes
    
    gltf = {
        "asset": { "version": "2.0", "generator": "TransportSim Proving Ground Garage 001 3D Exporter" },
        "scene": 0,
        "scenes": [{ "nodes": [0] }],
        "nodes": [{ "mesh": 0, "name": "ProvingGround_Garage001" }],
        "materials": [
            {
                "name": "DepotMaterial",
                "pbrMetallicRoughness": {
                    "baseColorFactor": [color_rgb[0], color_rgb[1], color_rgb[2], 1.0],
                    "metallicFactor": metallic,
                    "roughnessFactor": roughness
                },
                "doubleSided": True
            }
        ],
        "meshes": [
            {
                "name": "Garage001Mesh",
                "primitives": [
                    {
                        "attributes": {
                            "POSITION": 0,
                            "NORMAL": 1
                        },
                        "indices": 2,
                        "material": 0,
                        "mode": 4
                    }
                ]
            }
        ],
        "accessors": [
            {
                "bufferView": 0,
                "byteOffset": 0,
                "componentType": 5126,
                "count": len(all_verts),
                "type": "VEC3",
                "min": min_pos,
                "max": max_pos
            },
            {
                "bufferView": 1,
                "byteOffset": 0,
                "componentType": 5126,
                "count": len(all_norms),
                "type": "VEC3",
                "min": [-1.0, -1.0, -1.0],
                "max": [1.0, 1.0, 1.0]
            },
            {
                "bufferView": 2,
                "byteOffset": 0,
                "componentType": 5123,
                "count": len(all_indices),
                "type": "SCALAR",
                "min": [0],
                "max": [len(all_verts) - 1]
            }
        ],
        "bufferViews": [
            { "buffer": 0, "byteOffset": pos_offset, "byteLength": pos_len, "target": 34962 },
            { "buffer": 0, "byteOffset": norm_offset, "byteLength": norm_len, "target": 34962 },
            { "buffer": 0, "byteOffset": idx_offset, "byteLength": idx_len, "target": 34963 }
        ],
        "buffers": [{ "byteLength": total_bin_len }]
    }
    
    json_str = json.dumps(gltf, separators=(',', ':'))
    json_bytes = json_str.encode('utf-8')
    while len(json_bytes) % 4 != 0:
        json_bytes += b' '
        
    glb_header = struct.pack('<4sII', b'glTF', 2, 12 + 8 + len(json_bytes) + 8 + total_bin_len)
    json_chunk_header = struct.pack('<II', len(json_bytes), 0x4E4F534A) # JSON
    bin_chunk_header = struct.pack('<II', total_bin_len, 0x004E4942)   # BIN
    
    return glb_header + json_chunk_header + json_bytes + bin_chunk_header + bin_buffer

def export_garage_001():
    # Specifications matching Renderer.cpp
    # Local origin centered at depot center: X=0, Y=0, Z=0
    boxes = [
        # 1. Main Depot Warehouse (14m wide X, 6m high Y, 22m long Z)
        ((-7.0, 0.0, -11.0), (7.0, 6.0, 11.0)),
        # 2. Signature Red Roof Trim (15m x 0.4m x 23m)
        ((-7.5, 6.0, -11.5), (7.5, 6.4, 11.5)),
        # 3. Dual Service Bay Rollup Doors on West wall (X = -7.0m)
        # Bay 1 (Z = -4.0m, width 5.0m, height 3.6m)
        ((-7.1, 0.0, -6.5), (-6.9, 3.6, -1.5)),
        # Bay 2 (Z = +4.0m, width 5.0m, height 3.6m)
        ((-7.1, 0.0, 1.5), (-6.9, 3.6, 6.5)),
        # 4. Cyan Workshop Signboard (10m long, 1m high, X = -7.1m)
        ((-7.15, 4.3, -5.0), (-6.95, 5.3, 5.0)),
        # 5. Paved Apron (28m x 24m) extending from front wall
        ((-35.0, -0.05, -12.0), (-7.0, 0.05, 12.0)),
    ]
    return create_glb(boxes, color_rgb=(0.28, 0.32, 0.38), roughness=0.55, metallic=0.25)
```

---

### 5.2 Lorry Generation Script (`generate_vehicle_glbs.py`)

```python
def generate_lorry(name, color, is_flatbed=False, is_tanker=False):
    boxes = []
    # Heavy Dual-Rail Chassis Frame
    boxes.append(((-0.85, 0.25, -3.2), (0.85, 0.55, 3.0)))
    # Front and rear heavy wheels
    boxes.extend(get_wheels(1.9, 5.8, y=0.42, r=0.40))
    # Wheel guards
    for x_sign in [-1, 1]:
        boxes.append(((x_sign * 1.0 - 0.15, 0.02, -1.2), (x_sign * 1.0 + 0.15, 0.82, -0.4)))
    # Heavy Cab Over Front
    boxes.append(((-0.95, 0.45, 1.5), (0.95, 2.2, 3.1)))
    
    if is_tanker:
        # Tanker cylinder approximation
        boxes.append(((-0.92, 0.65, -3.0), (0.92, 2.05, 1.3)))
    elif is_flatbed:
        # Heavy Flatbed with headboard
        boxes.append(((-1.05, 0.65, -3.2), (1.05, 0.85, 1.4)))
        boxes.append(((-1.0, 0.85, 1.25), (1.0, 2.15, 1.4)))
    else:
        # Large Cargo Box
        boxes.append(((-1.02, 0.55, -3.2), (1.02, 2.45, 1.35)))
        
    return create_glb(boxes, color_rgb=color)
```

---

## 6. Synthesis: Why the Proving Ground Models Look and Perform Better

1. **Multi-Part Geometry & Distinct Material Tones**:
   * The Proving Ground models are not single monolithic blobs. The C++ truck splits geometry into distinct parts with distinct colors: chassis (`#1F242E`), wheels (`#141414`), cyan cab (`#00BCD4`), silver cargo box (`#EBF0F5`), cyan racing stripes, golden amber headlights (`#FFE033`), and crimson taillights (`#E61A1A`).
   * The GLB model in the dashboard catalog uses a single global PBR material factor across all vertices, losing the contrast of multi-material subcomponents.
2. **Dedicated Directional Lighting**:
   * The shader computes dynamic Lambertian diffuse (`diff * vec3(0.85, 0.82, 0.76)`) plus ambient sky fill (`vec3(0.38, 0.42, 0.48)`), which makes edges, roof trims, and wheel wells cast distinct high-contrast shadows.
3. **Hardware Coupling & Performance**:
   * The procedural vertices are written directly into GPU VBO memory at app launch (`GL_STATIC_DRAW`), with 0 ms disk access, bypassing JSON serialization, base64 decoders, or texture staging memory.
