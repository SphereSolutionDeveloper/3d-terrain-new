// Procedural Vulkan Asset Suite
// Path: src/buildings/BuildingSystem.cpp

#include "BuildingSystem.h"
#include <algorithm>
#include <cmath>
#include <cstring>

namespace ApexAssets {

namespace {

constexpr float kPi = 3.14159265358979323846f;

class BinCursor {
public:
    BinCursor(const uint8_t* data, size_t size) : m_data(data), m_size(size), m_pos(0) {}

    bool ok() const { return !m_error; }
    size_t remaining() const { return m_pos <= m_size ? (m_size - m_pos) : 0; }

    bool skip(size_t n) {
        if (n > remaining()) { m_error = true; return false; }
        m_pos += n;
        return true;
    }

    template <typename T>
    T read() {
        T val{};
        if (sizeof(T) > remaining()) { m_error = true; return val; }
        std::memcpy(&val, m_data + m_pos, sizeof(T));
        m_pos += sizeof(T);
        return val;
    }

    std::string readStr() {
        uint16_t len = read<uint16_t>();
        if (m_error || len > remaining()) { m_error = true; return ""; }
        if (len == 0) return "";
        std::string s(reinterpret_cast<const char*>(m_data + m_pos), len);
        m_pos += len;
        return s;
    }

    bool readMagic(const char expected[4]) {
        if (4 > remaining()) { m_error = true; return false; }
        bool match = std::memcmp(m_data + m_pos, expected, 4) == 0;
        m_pos += 4;
        if (!match) m_error = true;
        return match;
    }

private:
    const uint8_t* m_data;
    size_t m_size;
    size_t m_pos;
    bool m_error = false;
};

struct RGB {
    float r, g, b;
};

RGB wallBaseColor(WallType w) {
    switch (w) {
        case WallType::CONCRETE_BLOCK_PLASTER:   return {0.82f, 0.80f, 0.74f};
        case WallType::CORRUGATED_IRON_CLADDING: return {0.56f, 0.62f, 0.68f};
        case WallType::DRESSED_MASONRY_STONE:    return {0.66f, 0.61f, 0.54f};
        case WallType::PRECAST_INDUSTRIAL_PANEL: return {0.69f, 0.71f, 0.73f};
        case WallType::TIMBER_PLANK:             return {0.54f, 0.36f, 0.22f};
        case WallType::BRICK_MASONRY:            return {0.68f, 0.34f, 0.24f};
    }
    return {0.78f, 0.76f, 0.70f};
}

RGB roofBaseColor(RoofMaterial m) {
    switch (m) {
        case RoofMaterial::CORRUGATED_IRON_MABATI: return {0.64f, 0.28f, 0.22f};
        case RoofMaterial::STONE_COATED_TILE:      return {0.26f, 0.29f, 0.34f};
        case RoofMaterial::REINFORCED_CONCRETE:    return {0.54f, 0.56f, 0.58f};
        case RoofMaterial::GALVANIZED_TRUSS_SHEET: return {0.66f, 0.72f, 0.76f};
        case RoofMaterial::CLAY_TILE:              return {0.74f, 0.36f, 0.20f};
        case RoofMaterial::THATCH_MAASAI:          return {0.68f, 0.58f, 0.34f};
    }
    return {0.62f, 0.30f, 0.24f};
}

RGB windowGlassColor(WindowType w) {
    switch (w) {
        case WindowType::SLIDING_ALUMINUM:      return {0.22f, 0.48f, 0.68f};
        case WindowType::STEEL_CASEMENT:        return {0.18f, 0.34f, 0.46f};
        case WindowType::INDUSTRIAL_CLERESTORY: return {0.36f, 0.64f, 0.76f};
        case WindowType::LOUVERED_VENT:         return {0.28f, 0.32f, 0.36f};
        case WindowType::CURTAIN_WALL:          return {0.14f, 0.58f, 0.80f};
        case WindowType::NARROW_SLIT:           return {0.12f, 0.18f, 0.22f};
    }
    return {0.22f, 0.46f, 0.66f};
}

RGB fenceColor(FenceType f) {
    switch (f) {
        case FenceType::CHAIN_LINK_POSTS:        return {0.56f, 0.62f, 0.64f};
        case FenceType::MASONRY_BLOCK_WALL:      return {0.72f, 0.68f, 0.60f};
        case FenceType::TIMBER_POST_RAIL:        return {0.48f, 0.33f, 0.20f};
        case FenceType::LIVE_THORN_HEDGE:        return {0.26f, 0.44f, 0.22f};
        case FenceType::PALISADE_SECURITY_STEEL: return {0.22f, 0.26f, 0.30f};
        default:                                 return {0.5f, 0.5f, 0.5f};
    }
}

void pushTri(
    std::vector<ArchVertex>& verts,
    std::vector<uint32_t>& idx,
    const float p0[3], const float p1[3], const float p2[3],
    const RGB& col, float alpha = 1.0f
) {
    float e1[3] = {p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]};
    float e2[3] = {p2[0] - p0[0], p2[1] - p0[1], p2[2] - p0[2]};
    float nx = e1[1] * e2[2] - e1[2] * e2[1];
    float ny = e1[2] * e2[0] - e1[0] * e2[2];
    float nz = e1[0] * e2[1] - e1[1] * e2[0];
    float len = std::sqrt(nx * nx + ny * ny + nz * nz);
    if (len > 1e-6f) { nx /= len; ny /= len; nz /= len; }
    else { nx = 0.0f; ny = 1.0f; nz = 0.0f; }

    uint32_t base = static_cast<uint32_t>(verts.size());
    ArchVertex v0{{p0[0], p0[1], p0[2]}, {nx, ny, nz}, {0.0f, 0.0f}, {col.r, col.g, col.b, alpha}};
    ArchVertex v1{{p1[0], p1[1], p1[2]}, {nx, ny, nz}, {1.0f, 0.0f}, {col.r, col.g, col.b, alpha}};
    ArchVertex v2{{p2[0], p2[1], p2[2]}, {nx, ny, nz}, {0.5f, 1.0f}, {col.r, col.g, col.b, alpha}};
    verts.push_back(v0);
    verts.push_back(v1);
    verts.push_back(v2);
    idx.push_back(base);
    idx.push_back(base + 1);
    idx.push_back(base + 2);
}

void pushQuad(
    std::vector<ArchVertex>& verts,
    std::vector<uint32_t>& idx,
    const float p0[3], const float p1[3], const float p2[3], const float p3[3],
    const RGB& col, float alpha = 1.0f
) {
    float e1[3] = {p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]};
    float e2[3] = {p3[0] - p0[0], p3[1] - p0[1], p3[2] - p0[2]};
    float nx = e1[1] * e2[2] - e1[2] * e2[1];
    float ny = e1[2] * e2[0] - e1[0] * e2[2];
    float nz = e1[0] * e2[1] - e1[1] * e2[0];
    float len = std::sqrt(nx * nx + ny * ny + nz * nz);
    if (len > 1e-6f) { nx /= len; ny /= len; nz /= len; }
    else { nx = 0.0f; ny = 1.0f; nz = 0.0f; }

    uint32_t base = static_cast<uint32_t>(verts.size());
    verts.push_back({{p0[0], p0[1], p0[2]}, {nx, ny, nz}, {0.0f, 0.0f}, {col.r, col.g, col.b, alpha}});
    verts.push_back({{p1[0], p1[1], p1[2]}, {nx, ny, nz}, {1.0f, 0.0f}, {col.r, col.g, col.b, alpha}});
    verts.push_back({{p2[0], p2[1], p2[2]}, {nx, ny, nz}, {1.0f, 1.0f}, {col.r, col.g, col.b, alpha}});
    verts.push_back({{p3[0], p3[1], p3[2]}, {nx, ny, nz}, {0.0f, 1.0f}, {col.r, col.g, col.b, alpha}});
    idx.push_back(base + 0); idx.push_back(base + 1); idx.push_back(base + 2);
    idx.push_back(base + 0); idx.push_back(base + 2); idx.push_back(base + 3);
}

void pushBox(
    std::vector<ArchVertex>& verts,
    std::vector<uint32_t>& idx,
    float cx, float y0, float cz,
    float halfW, float height, float halfD,
    float yawRad,
    const RGB& col
) {
    float c = std::cos(yawRad), s = std::sin(yawRad);
    auto rotPt = [&](float lx, float ly, float lz, float out[3]) {
        out[0] = cx + lx * c - lz * s;
        out[1] = ly;
        out[2] = cz + lx * s + lz * c;
    };
    float y1 = y0 + height;
    float b0[3], b1[3], b2[3], b3[3], t0[3], t1[3], t2[3], t3[3];
    rotPt(-halfW, y0, -halfD, b0);
    rotPt( halfW, y0, -halfD, b1);
    rotPt( halfW, y0,  halfD, b2);
    rotPt(-halfW, y0,  halfD, b3);
    rotPt(-halfW, y1, -halfD, t0);
    rotPt( halfW, y1, -halfD, t1);
    rotPt( halfW, y1,  halfD, t2);
    rotPt(-halfW, y1,  halfD, t3);

    pushQuad(verts, idx, b0, b1, t1, t0, col);
    pushQuad(verts, idx, b1, b2, t2, t1, col);
    pushQuad(verts, idx, b2, b3, t3, t2, col);
    pushQuad(verts, idx, b3, b0, t0, t3, col);
    pushQuad(verts, idx, t0, t1, t2, t3, col);
}

} // namespace

const char* BuildingSystem::ShapeName(BuildingShapeType s) {
    switch (s) {
        case BuildingShapeType::SQUARE: return "SQUARE";
        case BuildingShapeType::RECTANGULAR: return "RECTANGULAR";
        case BuildingShapeType::L_SHAPE: return "L_SHAPE";
        case BuildingShapeType::U_SHAPE: return "U_SHAPE";
        case BuildingShapeType::T_SHAPE: return "T_SHAPE";
        case BuildingShapeType::STAR_CROSS: return "STAR_CROSS";
        case BuildingShapeType::CIRCULAR_SILO: return "CIRCULAR_SILO";
        case BuildingShapeType::POLYGON_IRREGULAR: return "POLYGON_IRREGULAR";
    }
    return "RECTANGULAR";
}

const char* BuildingSystem::RoofName(RoofType r) {
    switch (r) {
        case RoofType::GABLED: return "GABLED";
        case RoofType::HIPPED: return "HIPPED";
        case RoofType::FLAT_PARAPET: return "FLAT_PARAPET";
        case RoofType::SAW_TOOTH_INDUSTRIAL: return "SAW_TOOTH_INDUSTRIAL";
        case RoofType::MONO_PITCH_SHED: return "MONO_PITCH_SHED";
        case RoofType::ARCHED_TRUSS: return "ARCHED_TRUSS";
        case RoofType::CONICAL_DOME: return "CONICAL_DOME";
    }
    return "GABLED";
}

const char* BuildingSystem::RoofMaterialName(RoofMaterial m) {
    switch (m) {
        case RoofMaterial::CORRUGATED_IRON_MABATI: return "CORRUGATED_IRON_MABATI";
        case RoofMaterial::STONE_COATED_TILE: return "STONE_COATED_TILE";
        case RoofMaterial::REINFORCED_CONCRETE: return "REINFORCED_CONCRETE";
        case RoofMaterial::GALVANIZED_TRUSS_SHEET: return "GALVANIZED_TRUSS_SHEET";
        case RoofMaterial::CLAY_TILE: return "CLAY_TILE";
        case RoofMaterial::THATCH_MAASAI: return "THATCH_MAASAI";
    }
    return "CORRUGATED_IRON_MABATI";
}

const char* BuildingSystem::WallName(WallType w) {
    switch (w) {
        case WallType::CONCRETE_BLOCK_PLASTER: return "CONCRETE_BLOCK_PLASTER";
        case WallType::CORRUGATED_IRON_CLADDING: return "CORRUGATED_IRON_CLADDING";
        case WallType::DRESSED_MASONRY_STONE: return "DRESSED_MASONRY_STONE";
        case WallType::PRECAST_INDUSTRIAL_PANEL: return "PRECAST_INDUSTRIAL_PANEL";
        case WallType::TIMBER_PLANK: return "TIMBER_PLANK";
        case WallType::BRICK_MASONRY: return "BRICK_MASONRY";
    }
    return "CONCRETE_BLOCK_PLASTER";
}

const char* BuildingSystem::WindowName(WindowType w) {
    switch (w) {
        case WindowType::SLIDING_ALUMINUM: return "SLIDING_ALUMINUM";
        case WindowType::STEEL_CASEMENT: return "STEEL_CASEMENT";
        case WindowType::INDUSTRIAL_CLERESTORY: return "INDUSTRIAL_CLERESTORY";
        case WindowType::LOUVERED_VENT: return "LOUVERED_VENT";
        case WindowType::CURTAIN_WALL: return "CURTAIN_WALL";
        case WindowType::NARROW_SLIT: return "NARROW_SLIT";
    }
    return "SLIDING_ALUMINUM";
}

const char* BuildingSystem::FenceName(FenceType f) {
    switch (f) {
        case FenceType::NONE: return "NONE";
        case FenceType::CHAIN_LINK_POSTS: return "CHAIN_LINK_POSTS";
        case FenceType::MASONRY_BLOCK_WALL: return "MASONRY_BLOCK_WALL";
        case FenceType::TIMBER_POST_RAIL: return "TIMBER_POST_RAIL";
        case FenceType::LIVE_THORN_HEDGE: return "LIVE_THORN_HEDGE";
        case FenceType::PALISADE_SECURITY_STEEL: return "PALISADE_SECURITY_STEEL";
    }
    return "NONE";
}

const char* BuildingSystem::GateName(GateType g) {
    switch (g) {
        case GateType::NONE: return "NONE";
        case GateType::SLIDING_STEEL_GATE: return "SLIDING_STEEL_GATE";
        case GateType::DOUBLE_SWING_TIMBER: return "DOUBLE_SWING_TIMBER";
        case GateType::SECURITY_BOOM_BARRIER: return "SECURITY_BOOM_BARRIER";
        case GateType::PEDESTRIAN_WICKET: return "PEDESTRIAN_WICKET";
    }
    return "NONE";
}

bool BuildingSystem::ParseMagtBinaryBuffer(
    const uint8_t* data,
    size_t size,
    std::vector<BuildingRecordV2>& outBuildings,
    BuildingPackSummary& outSummary
) {
    outBuildings.clear();
    outSummary = {};
    if (!data || size < 64) return false;

    BinCursor cur(data, size);
    if (!cur.readMagic("MAGT")) return false;
    uint16_t ver = cur.read<uint16_t>();
    cur.read<uint16_t>(); // flags
    outSummary.formatVersion = ver;

    cur.skip(10 * sizeof(double)); // bounds
    for (int i = 0; i < 4; ++i) cur.readStr();
    cur.skip(5 * sizeof(float));

    // DEM Grid
    cur.readStr();
    uint32_t dCols = cur.read<uint32_t>();
    uint32_t dRows = cur.read<uint32_t>();
    cur.skip(4 * sizeof(float));
    cur.skip(static_cast<size_t>(dCols) * static_cast<size_t>(dRows) * sizeof(float));

    // Roads
    uint32_t rCount = cur.read<uint32_t>();
    for (uint32_t i = 0; i < rCount && cur.ok(); ++i) {
        cur.skip(sizeof(int64_t));
        cur.readStr(); cur.readStr(); cur.readStr(); cur.readStr();
        cur.skip(2 * sizeof(uint8_t) + 2 * sizeof(float));
        uint32_t nCnt = cur.read<uint32_t>();
        cur.skip(static_cast<size_t>(nCnt) * 2 * sizeof(float));
    }
    if (!cur.ok()) return false;

    // Buildings
    uint32_t bCount = cur.read<uint32_t>();
    outBuildings.reserve(bCount);
    for (uint32_t i = 0; i < bCount && cur.ok(); ++i) {
        BuildingRecordV2 rec;
        rec.osmId = cur.read<int64_t>();
        rec.name = cur.readStr();
        rec.buildingType = cur.readStr();
        rec.amenity = cur.readStr();
        rec.levels = cur.read<uint8_t>();
        rec.heightM = cur.read<float>();
        rec.floorHeightM = rec.levels > 0 ? (rec.heightM / static_cast<float>(rec.levels)) : rec.heightM;
        rec.elevM = cur.read<float>();
        rec.areaM2 = cur.read<float>();
        rec.perimeterM = cur.read<float>();
        rec.centroidX = cur.read<float>();
        rec.centroidY = cur.read<float>();
        uint32_t pCnt = cur.read<uint32_t>();
        rec.polygon.resize(pCnt);
        for (uint32_t k = 0; k < pCnt && cur.ok(); ++k) {
            rec.polygon[k].x = cur.read<float>();
            rec.polygon[k].y = cur.read<float>();
        }

        if (ver >= 2 && cur.ok()) {
            uint8_t sId = cur.read<uint8_t>();
            uint8_t rId = cur.read<uint8_t>();
            uint8_t rmId = cur.read<uint8_t>();
            uint8_t wId = cur.read<uint8_t>();
            uint8_t winId = cur.read<uint8_t>();
            uint8_t pFlags = cur.read<uint8_t>();
            uint8_t fId = cur.read<uint8_t>();
            uint8_t gId = cur.read<uint8_t>();

            rec.shapeType = static_cast<BuildingShapeType>(std::min<uint8_t>(sId, 7));
            rec.roofType = static_cast<RoofType>(std::min<uint8_t>(rId, 6));
            rec.roofMaterial = static_cast<RoofMaterial>(std::min<uint8_t>(rmId, 5));
            rec.wallType = static_cast<WallType>(std::min<uint8_t>(wId, 5));
            rec.windowType = static_cast<WindowType>(std::min<uint8_t>(winId, 5));
            rec.hasFence = (pFlags & 1) != 0;
            rec.hasGate = (pFlags & 2) != 0;
            rec.fenceType = static_cast<FenceType>(std::min<uint8_t>(fId, 5));
            rec.gateType = static_cast<GateType>(std::min<uint8_t>(gId, 4));

            rec.floorHeightM = cur.read<float>();
            rec.roofPitchDeg = cur.read<float>();
            rec.windowToWallRatio = cur.read<float>();
            rec.parentCompoundId = cur.read<int64_t>();
            rec.parentCompoundName = cur.readStr();
            rec.fenceHeightM = cur.read<float>();
            uint16_t fCnt = cur.read<uint16_t>();
            rec.fencePolygon.resize(fCnt);
            for (uint16_t k = 0; k < fCnt && cur.ok(); ++k) {
                rec.fencePolygon[k].x = cur.read<float>();
                rec.fencePolygon[k].y = cur.read<float>();
            }
            rec.gateWidthM = cur.read<float>();
            rec.gateAngleRad = cur.read<float>();
            rec.gateCoord.x = cur.read<float>();
            rec.gateCoord.y = cur.read<float>();
        }

        if (!cur.ok()) break;
        if (rec.parentCompoundId != 0) outSummary.compoundChildren++;
        if (rec.hasFence) outSummary.fencedBuildings++;
        if (rec.hasGate) outSummary.gatedBuildings++;
        if (rec.levels > 1) outSummary.multiStoreyBuildings++;
        if (rec.heightM > outSummary.maxHeightM) outSummary.maxHeightM = rec.heightM;
        outBuildings.push_back(std::move(rec));
    }

    outSummary.totalBuildings = static_cast<int32_t>(outBuildings.size());
    return cur.ok();
}

bool BuildingSystem::ParseC58BinaryBuffer(
    const uint8_t* data,
    size_t size,
    std::vector<BuildingRecordV2>& outBuildings,
    BuildingPackSummary& outSummary
) {
    outBuildings.clear();
    outSummary = {};
    if (!data || size < 64) return false;

    BinCursor cur(data, size);
    if (!cur.readMagic("C58B")) return false;
    uint16_t ver = cur.read<uint16_t>();
    cur.read<uint16_t>(); // isLiveCl
    outSummary.formatVersion = ver;

    cur.skip(2 * sizeof(double)); // refE, refN
    for (int i = 0; i < 4; ++i) cur.readStr();
    cur.skip(5 * sizeof(float));

    uint8_t secCount = cur.read<uint8_t>();
    cur.skip(secCount);

    uint32_t clCount = cur.read<uint32_t>();
    cur.skip(static_cast<size_t>(clCount) * 2 * sizeof(double));

    // DEM Grid
    cur.readStr();
    uint32_t uCols = cur.read<uint32_t>();
    uint32_t vRows = cur.read<uint32_t>();
    cur.skip(4 * sizeof(float));
    cur.skip(static_cast<size_t>(uCols) * static_cast<size_t>(vRows) * sizeof(float));

    // Roads
    uint32_t rCount = cur.read<uint32_t>();
    for (uint32_t i = 0; i < rCount && cur.ok(); ++i) {
        cur.skip(sizeof(int64_t));
        cur.readStr(); cur.readStr(); cur.readStr(); cur.readStr();
        cur.skip(3 * sizeof(uint8_t) + 2 * sizeof(float));
        cur.skip(4 * sizeof(float)); // mid C58Pt
        uint16_t nCnt = cur.read<uint16_t>();
        cur.skip(static_cast<size_t>(nCnt) * 4 * sizeof(float));
    }
    if (!cur.ok()) return false;

    // Houses / Buildings
    uint32_t hCount = cur.read<uint32_t>();
    outBuildings.reserve(hCount);
    for (uint32_t i = 0; i < hCount && cur.ok(); ++i) {
        BuildingRecordV2 rec;
        rec.osmId = cur.read<int64_t>();
        rec.name = cur.readStr();
        rec.buildingType = cur.readStr();
        rec.amenity = cur.readStr();
        rec.levels = cur.read<uint8_t>();
        rec.sectorIndex = cur.read<uint8_t>();
        rec.heightM = cur.read<float>();
        rec.floorHeightM = rec.levels > 0 ? (rec.heightM / static_cast<float>(rec.levels)) : rec.heightM;
        rec.elevM = cur.read<float>();
        rec.areaM2 = cur.read<float>();
        rec.perimeterM = cur.read<float>();
        cur.read<float>(); // reserved lateral offset

        // centroid C58Pt: dE, dN, ch, off
        rec.centroidX = cur.read<float>();
        rec.centroidY = cur.read<float>();
        cur.skip(2 * sizeof(float));

        uint16_t pCnt = cur.read<uint16_t>();
        rec.polygon.resize(pCnt);
        for (uint16_t k = 0; k < pCnt && cur.ok(); ++k) {
            rec.polygon[k].x = cur.read<float>();
            rec.polygon[k].y = cur.read<float>();
            cur.skip(2 * sizeof(float));
        }

        if (ver >= 2 && cur.ok()) {
            uint8_t sId = cur.read<uint8_t>();
            uint8_t rId = cur.read<uint8_t>();
            uint8_t rmId = cur.read<uint8_t>();
            uint8_t wId = cur.read<uint8_t>();
            uint8_t winId = cur.read<uint8_t>();
            uint8_t pFlags = cur.read<uint8_t>();
            uint8_t fId = cur.read<uint8_t>();
            uint8_t gId = cur.read<uint8_t>();

            rec.shapeType = static_cast<BuildingShapeType>(std::min<uint8_t>(sId, 7));
            rec.roofType = static_cast<RoofType>(std::min<uint8_t>(rId, 6));
            rec.roofMaterial = static_cast<RoofMaterial>(std::min<uint8_t>(rmId, 5));
            rec.wallType = static_cast<WallType>(std::min<uint8_t>(wId, 5));
            rec.windowType = static_cast<WindowType>(std::min<uint8_t>(winId, 5));
            rec.hasFence = (pFlags & 1) != 0;
            rec.hasGate = (pFlags & 2) != 0;
            rec.fenceType = static_cast<FenceType>(std::min<uint8_t>(fId, 5));
            rec.gateType = static_cast<GateType>(std::min<uint8_t>(gId, 4));

            rec.floorHeightM = cur.read<float>();
            rec.roofPitchDeg = cur.read<float>();
            rec.windowToWallRatio = cur.read<float>();
            rec.parentCompoundId = cur.read<int64_t>();
            rec.parentCompoundName = cur.readStr();
            rec.fenceHeightM = cur.read<float>();
            uint16_t fCnt = cur.read<uint16_t>();
            rec.fencePolygon.resize(fCnt);
            for (uint16_t k = 0; k < fCnt && cur.ok(); ++k) {
                rec.fencePolygon[k].x = cur.read<float>();
                rec.fencePolygon[k].y = cur.read<float>();
                cur.skip(2 * sizeof(float));
            }
            rec.gateWidthM = cur.read<float>();
            rec.gateAngleRad = cur.read<float>();
            rec.gateCoord.x = cur.read<float>();
            rec.gateCoord.y = cur.read<float>();
            cur.skip(2 * sizeof(float));
        }

        if (!cur.ok()) break;
        if (rec.parentCompoundId != 0) outSummary.compoundChildren++;
        if (rec.hasFence) outSummary.fencedBuildings++;
        if (rec.hasGate) outSummary.gatedBuildings++;
        if (rec.levels > 1) outSummary.multiStoreyBuildings++;
        if (rec.heightM > outSummary.maxHeightM) outSummary.maxHeightM = rec.heightM;
        outBuildings.push_back(std::move(rec));
    }

    outSummary.totalBuildings = static_cast<int32_t>(outBuildings.size());
    return cur.ok();
}

void BuildingSystem::LoadFromMagtBuffer(const uint8_t* data, size_t size) {
    ParseMagtBinaryBuffer(data, size, m_buildings, m_lastSummary);
}

void BuildingSystem::LoadFromC58Buffer(const uint8_t* data, size_t size) {
    ParseC58BinaryBuffer(data, size, m_buildings, m_lastSummary);
}

const BuildingRecordV2* BuildingSystem::FindByOsmId(int64_t osmId) const {
    for (const auto& b : m_buildings) {
        if (b.osmId == osmId) return &b;
    }
    return nullptr;
}

void BuildingSystem::GenerateBuildingAndPerimeterMesh(
    const BuildingRecordV2& record,
    std::vector<ArchVertex>& outVerts,
    std::vector<uint32_t>& outIndices
) {
    outVerts.clear();
    outIndices.clear();
    if (record.polygon.size() < 3) return;

    const float cx = record.centroidX;
    const float cz = record.centroidY;
    const int n = static_cast<int>(record.polygon.size());
    const float h = std::max(2.2f, record.heightM);
    const int floors = std::max(1, static_cast<int>(record.levels));
    const float floorH = h / static_cast<float>(floors);

    RGB wCol = wallBaseColor(record.wallType);
    RGB rCol = roofBaseColor(record.roofMaterial);
    RGB gCol = windowGlassColor(record.windowType);

    // Local footprint vertices centered on (0, 0) in XZ
    std::vector<Pt2D> localPoly(n);
    float minX = 1e9f, maxX = -1e9f, minZ = 1e9f, maxZ = -1e9f;
    for (int i = 0; i < n; ++i) {
        float lx = record.polygon[i].x - cx;
        float lz = -(record.polygon[i].y - cz);
        localPoly[i] = {lx, lz};
        minX = std::min(minX, lx);
        maxX = std::max(maxX, lx);
        minZ = std::min(minZ, lz);
        maxZ = std::max(maxZ, lz);
    }

    // 1. Extruded facade walls + multi-tier windows per floor
    for (int i = 0; i < n; ++i) {
        Pt2D a = localPoly[i];
        Pt2D b = localPoly[(i + 1) % n];
        float dx = b.x - a.x;
        float dz = b.y - a.y;
        float edgeLen = std::sqrt(dx * dx + dz * dz);
        if (edgeLen < 0.25f) continue;

        float nx = dz / edgeLen;
        float nz = -dx / edgeLen;
        float shade = 0.85f + 0.15f * std::max(-1.0f, std::min(1.0f, nx * 0.6f + nz * 0.8f));
        RGB shadedWall{wCol.r * shade, wCol.g * shade, wCol.b * shade};

        float b0[3] = {a.x, 0.0f, a.y};
        float b1[3] = {b.x, 0.0f, b.y};
        float t1[3] = {b.x, h, b.y};
        float t0[3] = {a.x, h, a.y};
        pushQuad(outVerts, outIndices, b0, b1, t1, t0, shadedWall);

        // Floor slab molding band + recessed window bays
        if (edgeLen >= 2.6f && record.windowToWallRatio > 0.04f) {
            int bays = std::max(1, std::min(8, static_cast<int>(edgeLen / 3.2f)));
            float winFrac = std::max(0.28f, std::min(0.75f, record.windowToWallRatio * 2.2f));
            for (int f = 0; f < floors; ++f) {
                float yBot = f * floorH + floorH * 0.28f;
                float yTop = f * floorH + floorH * 0.78f;
                for (int bay = 0; bay < bays; ++bay) {
                    float tCenter = (static_cast<float>(bay) + 0.5f) / static_cast<float>(bays);
                    float tHalf = (0.5f / static_cast<float>(bays)) * winFrac;
                    float tA = std::max(0.05f, tCenter - tHalf);
                    float tB = std::min(0.95f, tCenter + tHalf);
                    float off = 0.06f;
                    float w0[3] = {a.x + dx * tA + nx * off, yBot, a.y + dz * tA + nz * off};
                    float w1[3] = {a.x + dx * tB + nx * off, yBot, a.y + dz * tB + nz * off};
                    float w2[3] = {a.x + dx * tB + nx * off, yTop, a.y + dz * tB + nz * off};
                    float w3[3] = {a.x + dx * tA + nx * off, yTop, a.y + dz * tA + nz * off};
                    pushQuad(outVerts, outIndices, w0, w1, w2, w3, gCol, 0.94f);
                }
            }
        }
    }

    // 2. Architectural Roof Geometry
    float spanX = std::max(2.0f, maxX - minX);
    float spanZ = std::max(2.0f, maxZ - minZ);
    float pitchRad = std::max(2.0f, std::min(42.0f, record.roofPitchDeg)) * (kPi / 180.0f);
    float rise = std::max(0.35f, std::min(spanX, spanZ) * 0.45f * std::tan(pitchRad));

    if (record.roofType == RoofType::FLAT_PARAPET) {
        RGB capCol{rCol.r * 0.92f, rCol.g * 0.92f, rCol.b * 0.92f};
        for (int i = 0; i < n; ++i) {
            Pt2D a = localPoly[i];
            Pt2D b = localPoly[(i + 1) % n];
            float p0[3] = {a.x, h, a.y};
            float p1[3] = {b.x, h, b.y};
            float p2[3] = {b.x, h + 0.45f, b.y};
            float p3[3] = {a.x, h + 0.45f, a.y};
            pushQuad(outVerts, outIndices, p0, p1, p2, p3, wCol);
            float c0[3] = {0.0f, h + 0.35f, 0.0f};
            pushTri(outVerts, outIndices, c0, p0, p1, capCol);
        }
    } else if (record.roofType == RoofType::CONICAL_DOME) {
        float apex[3] = {0.0f, h + rise * 1.15f, 0.0f};
        for (int i = 0; i < n; ++i) {
            Pt2D a = localPoly[i];
            Pt2D b = localPoly[(i + 1) % n];
            float p0[3] = {a.x * 1.04f, h, a.y * 1.04f};
            float p1[3] = {b.x * 1.04f, h, b.y * 1.04f};
            pushTri(outVerts, outIndices, p0, p1, apex, rCol);
        }
    } else if (record.roofType == RoofType::HIPPED || record.roofType == RoofType::GABLED) {
        bool xLonger = (spanX >= spanZ);
        float rx0 = xLonger ? minX * 0.55f : 0.0f;
        float rx1 = xLonger ? maxX * 0.55f : 0.0f;
        float rz0 = xLonger ? 0.0f : minZ * 0.55f;
        float rz1 = xLonger ? 0.0f : maxZ * 0.55f;
        if (record.roofType == RoofType::GABLED) {
            rx0 = xLonger ? minX : 0.0f;
            rx1 = xLonger ? maxX : 0.0f;
            rz0 = xLonger ? 0.0f : minZ;
            rz1 = xLonger ? 0.0f : maxZ;
        }
        for (int i = 0; i < n; ++i) {
            Pt2D a = localPoly[i];
            Pt2D b = localPoly[(i + 1) % n];
            float mx = 0.5f * (a.x + b.x);
            float mz = 0.5f * (a.y + b.y);
            float tProj = xLonger ? ((mx - minX) / spanX) : ((mz - minZ) / spanZ);
            float rx = rx0 + (rx1 - rx0) * std::max(0.0f, std::min(1.0f, tProj));
            float rz = rz0 + (rz1 - rz0) * std::max(0.0f, std::min(1.0f, tProj));
            float p0[3] = {a.x, h, a.y};
            float p1[3] = {b.x, h, b.y};
            float pApex[3] = {rx, h + rise, rz};
            pushTri(outVerts, outIndices, p0, p1, pApex, rCol);
        }
    } else if (record.roofType == RoofType::SAW_TOOTH_INDUSTRIAL) {
        int bays = 3;
        float bayW = spanX / static_cast<float>(bays);
        for (int b = 0; b < bays; ++b) {
            float x0 = minX + b * bayW;
            float x1 = x0 + bayW;
            float s0[3] = {x0, h, minZ};
            float s1[3] = {x0 + bayW * 0.18f, h + rise, minZ};
            float s2[3] = {x0 + bayW * 0.18f, h + rise, maxZ};
            float s3[3] = {x0, h, maxZ};
            pushQuad(outVerts, outIndices, s0, s1, s2, s3, gCol, 0.92f); // Clerestory glazing
            float e0[3] = {x0 + bayW * 0.18f, h + rise, minZ};
            float e1[3] = {x1, h, minZ};
            float e2[3] = {x1, h, maxZ};
            float e3[3] = {x0 + bayW * 0.18f, h + rise, maxZ};
            pushQuad(outVerts, outIndices, e0, e1, e2, e3, rCol);
        }
    } else if (record.roofType == RoofType::ARCHED_TRUSS) {
        int segs = 6;
        for (int s = 0; s < segs; ++s) {
            float t0 = static_cast<float>(s) / segs;
            float t1 = static_cast<float>(s + 1) / segs;
            float x0 = minX + spanX * t0;
            float x1 = minX + spanX * t1;
            float y0 = h + rise * std::sin(t0 * kPi);
            float y1 = h + rise * std::sin(t1 * kPi);
            float q0[3] = {x0, y0, minZ};
            float q1[3] = {x1, y1, minZ};
            float q2[3] = {x1, y1, maxZ};
            float q3[3] = {x0, y0, maxZ};
            pushQuad(outVerts, outIndices, q0, q1, q2, q3, rCol);
        }
    } else {
        // MONO_PITCH_SHED
        for (int i = 0; i < n; ++i) {
            Pt2D a = localPoly[i];
            Pt2D b = localPoly[(i + 1) % n];
            float ya = h + rise * ((a.x - minX) / spanX);
            float yb = h + rise * ((b.x - minX) / spanX);
            float center[3] = {0.0f, h + rise * 0.5f, 0.0f};
            float p0[3] = {a.x, ya, a.y};
            float p1[3] = {b.x, yb, b.y};
            pushTri(outVerts, outIndices, p0, p1, center, rCol);
        }
    }

    // 3. Road-Safe Perimeter Fence Geometry (leaving gate opening clear)
    if (record.hasFence && record.fencePolygon.size() >= 3) {
        float fh = std::max(1.1f, record.fenceHeightM);
        RGB fCol = fenceColor(record.fenceType);
        float gx = record.gateCoord.x - cx;
        float gz = -(record.gateCoord.y - cz);
        float gateHalfClearSq = std::pow(std::max(1.0f, record.gateWidthM * 0.5f), 2.0f);

        const int nf = static_cast<int>(record.fencePolygon.size());
        for (int i = 0; i < nf; ++i) {
            Pt2D fa{record.fencePolygon[i].x - cx, -(record.fencePolygon[i].y - cz)};
            Pt2D fb{record.fencePolygon[(i + 1) % nf].x - cx, -(record.fencePolygon[(i + 1) % nf].y - cz)};
            float mx = 0.5f * (fa.x + fb.x);
            float mz = 0.5f * (fa.y + fb.y);
            // Skip drawing solid wall directly across the gate leaf opening
            if (record.hasGate && ((mx - gx) * (mx - gx) + (mz - gz) * (mz - gz)) < gateHalfClearSq) {
                continue;
            }
            float segLen = std::hypot(fb.x - fa.x, fb.y - fa.y);
            if (segLen < 0.25f) continue;
            float yaw = std::atan2(fb.y - fa.y, fb.x - fa.x);
            pushBox(outVerts, outIndices, mx, 0.0f, mz, segLen * 0.5f, fh, 0.11f, yaw, fCol);
            // Vertical fence post
            pushBox(outVerts, outIndices, fa.x, 0.0f, fa.y, 0.14f, fh + 0.12f, 0.14f, 0.0f, {0.34f, 0.36f, 0.40f});
        }
    }

    // 4. Road-Facing Gate Leaf & Pillars at Gate Coordinates
    if (record.hasGate) {
        float gx = record.gateCoord.x - cx;
        float gz = -(record.gateCoord.y - cz);
        float gw = std::max(1.2f, record.gateWidthM);
        float gh = std::max(1.5f, record.fenceHeightM);
        float yaw = -record.gateAngleRad;
        float cosA = std::cos(yaw), sinA = std::sin(yaw);
        RGB pillarCol{0.82f, 0.78f, 0.72f};
        RGB gateLeafCol = (record.gateType == GateType::SECURITY_BOOM_BARRIER)
            ? RGB{0.96f, 0.62f, 0.12f}
            : (record.gateType == GateType::DOUBLE_SWING_TIMBER ? RGB{0.52f, 0.34f, 0.18f} : RGB{0.18f, 0.68f, 0.84f});

        float p1x = gx - cosA * (gw * 0.5f), p1z = gz - sinA * (gw * 0.5f);
        float p2x = gx + cosA * (gw * 0.5f), p2z = gz + sinA * (gw * 0.5f);
        pushBox(outVerts, outIndices, p1x, 0.0f, p1z, 0.24f, gh + 0.35f, 0.24f, yaw, pillarCol);
        pushBox(outVerts, outIndices, p2x, 0.0f, p2z, 0.24f, gh + 0.35f, 0.24f, yaw, pillarCol);

        if (record.gateType == GateType::SECURITY_BOOM_BARRIER) {
            pushBox(outVerts, outIndices, gx, 0.95f, gz, gw * 0.46f, 0.14f, 0.08f, yaw, gateLeafCol);
        } else {
            pushBox(outVerts, outIndices, gx, 0.08f, gz, gw * 0.47f, gh * 0.88f, 0.07f, yaw, gateLeafCol);
        }
    }
}

void BuildingSystem::GenerateCatalogShowcaseMesh(
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
) {
    BuildingRecordV2 rec;
    rec.shapeType = shapeType;
    rec.roofType = roofType;
    rec.wallType = wallType;
    rec.windowType = windowType;
    rec.fenceType = fenceType;
    rec.gateType = gateType;
    rec.levels = static_cast<uint8_t>(std::max(1, levels));
    rec.floorHeightM = std::max(2.6f, floorHeightM);
    rec.heightM = rec.levels * rec.floorHeightM;
    rec.roofPitchDeg = (roofType == RoofType::FLAT_PARAPET) ? 2.0f : 22.0f;
    rec.windowToWallRatio = 0.28f;

    float hw = std::max(2.0f, footprintWidthM * 0.5f);
    float hd = std::max(2.0f, footprintDepthM * 0.5f);
    if (shapeType == BuildingShapeType::CIRCULAR_SILO) {
        int segs = 12;
        float r = std::min(hw, hd);
        for (int i = 0; i < segs; ++i) {
            float ang = (2.0f * kPi * i) / segs;
            rec.polygon.push_back({r * std::cos(ang), r * std::sin(ang)});
        }
    } else if (shapeType == BuildingShapeType::L_SHAPE) {
        rec.polygon = {{-hw, -hd}, {hw, -hd}, {hw, 0.0f}, {0.0f, 0.0f}, {0.0f, hd}, {-hw, hd}};
    } else if (shapeType == BuildingShapeType::U_SHAPE) {
        rec.polygon = {{-hw, -hd}, {hw, -hd}, {hw, hd}, {hw * 0.45f, hd}, {hw * 0.45f, -hd * 0.1f}, {-hw * 0.45f, -hd * 0.1f}, {-hw * 0.45f, hd}, {-hw, hd}};
    } else if (shapeType == BuildingShapeType::T_SHAPE) {
        rec.polygon = {{-hw, -hd}, {hw, -hd}, {hw, -hd * 0.1f}, {hw * 0.35f, -hd * 0.1f}, {hw * 0.35f, hd}, {-hw * 0.35f, hd}, {-hw * 0.35f, -hd * 0.1f}, {-hw, -hd * 0.1f}};
    } else {
        rec.polygon = {{-hw, -hd}, {hw, -hd}, {hw, hd}, {-hw, hd}};
    }

    if (fenceType != FenceType::NONE) {
        rec.hasFence = true;
        rec.fenceHeightM = 1.9f;
        float fw = hw + 3.2f, fd = hd + 3.2f;
        rec.fencePolygon = {{-fw, -fd}, {fw, -fd}, {fw, fd}, {-fw, fd}};
        rec.hasGate = (gateType != GateType::NONE);
        rec.gateWidthM = 3.8f;
        rec.gateAngleRad = 0.0f;
        rec.gateCoord = {0.0f, -fd};
    }

    GenerateBuildingAndPerimeterMesh(rec, outVerts, outIndices);
}

} // namespace ApexAssets
