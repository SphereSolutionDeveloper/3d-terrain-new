#!/usr/bin/env python3
"""
===============================================================================
PROCEDURAL 3D ROCK GENERATOR & 10x10 GRID FIELD PIPELINE
Apex 3D Assets - Modular Geological Asset Architecture
===============================================================================
Generates fully modular 3D rock asset packages using procedural noise algorithms:
- 10x10 Grid Field: 100 cells, EACH cell exactly 1.0 by 1.0 meter (10m x 10m total)
- Procedural Noise: Perlin 3D, Simplex 3D, Fractal Brownian Motion (FBM),
                    Worley/Voronoi cellular facets, and sedimentary strata
- 3D Meshes: Deformed geodesic topologies, normals, UVs, faces, wireframe edges,
             and volume shape decomposition
- Textures: Procedural albedo, tangent-space normal maps (Sobel filter),
            micro-roughness, emissive crystal veins, and texture manifests
- Formats: .obj, geometry.json, wireframe_edges.json, glTF 2.0 binary (.glb)
- Target directories: /asserts/, /app/src/main/assets/, /assets/
===============================================================================
"""

import os
import sys
import math
import struct
import zlib
import random
import json

# ---------------------------------------------------------------------------
# 1. PROCEDURAL NOISE LIBRARIES (Pure Python, Zero External Dependencies)
# ---------------------------------------------------------------------------

class PermutationTable:
    def __init__(self, seed=1337):
        rng = random.Random(seed)
        p = list(range(256))
        rng.shuffle(p)
        self.p = p + p

DEFAULT_PERM = PermutationTable(seed=42).p

def fade(t):
    # Ken Perlin's improved smoothstep quintic polynomial: 6t^5 - 15t^4 + 10t^3
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0)

def lerp(a, b, t):
    return a + t * (b - a)

def grad3(hash_val, x, y, z):
    h = hash_val & 15
    u = x if h < 8 else y
    v = y if h < 4 else (x if h in (12, 14) else z)
    return ((u if (h & 1) == 0 else -u) + (v if (h & 2) == 0 else -v))

def perlin3d(x, y, z, perm=DEFAULT_PERM):
    """Ken Perlin 3D Improved Noise in [-1.0, 1.0]"""
    X = int(math.floor(x)) & 255
    Y = int(math.floor(y)) & 255
    Z = int(math.floor(z)) & 255

    x -= math.floor(x)
    y -= math.floor(y)
    z -= math.floor(z)

    u = fade(x)
    v = fade(y)
    w = fade(z)

    A = perm[X] + Y
    AA = perm[A] + Z
    AB = perm[A + 1] + Z
    B = perm[X + 1] + Y
    BA = perm[B] + Z
    BB = perm[B + 1] + Z

    return lerp(
        lerp(
            lerp(grad3(perm[AA], x, y, z), grad3(perm[BA], x - 1, y, z), u),
            lerp(grad3(perm[AB], x, y - 1, z), grad3(perm[BB], x - 1, y - 1, z), u),
            v
        ),
        lerp(
            lerp(grad3(perm[AA + 1], x, y, z - 1), grad3(perm[BA + 1], x - 1, y, z - 1), u),
            lerp(grad3(perm[AB + 1], x, y - 1, z - 1), grad3(perm[BB + 1], x - 1, y - 1, z - 1), u),
            v
        ),
        w
    )

def fbm3d(x, y, z, octaves=4, lacunarity=2.0, persistence=0.5, perm=DEFAULT_PERM):
    """Fractal Brownian Motion (FBM) combining multiple noise octaves."""
    total = 0.0
    frequency = 1.0
    amplitude = 1.0
    max_val = 0.0

    for _ in range(octaves):
        total += perlin3d(x * frequency, y * frequency, z * frequency, perm) * amplitude
        max_val += amplitude
        amplitude *= persistence
        frequency *= lacunarity

    return total / max_val

def worley2d(x, y, cell_scale=3.0):
    """Fast 2D Worley / Voronoi Cellular Noise: checks 9 cells in 2D."""
    x *= cell_scale
    y *= cell_scale

    xi = int(math.floor(x))
    yi = int(math.floor(y))

    min_dist = 999.0
    second_min_dist = 999.0

    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            cx = xi + dx
            cy = yi + dy

            # Deterministic pseudo-random seed point inside cell
            h = (cx * 73856093 ^ cy * 19349663) & 0xFFFFFF
            rx = cx + ((h & 0xFF) / 255.0)
            ry = cy + (((h >> 8) & 0xFF) / 255.0)

            d = math.sqrt((x - rx) ** 2 + (y - ry) ** 2)
            if d < min_dist:
                second_min_dist = min_dist
                min_dist = d
            elif d < second_min_dist:
                second_min_dist = d

    return min_dist, second_min_dist

def strata_noise(y, turbulence=0.0):
    """Sedimentary horizontal rock strata banding."""
    val = math.sin(y * 14.0 + turbulence * 2.5) * 0.5 + 0.5
    return val

def ridged_noise3d(x, y, z, octaves=4, lacunarity=2.0, gain=0.5, perm=DEFAULT_PERM):
    """Ridged Multifractal Noise: 1.0 - abs(noise) creates sharp rock crags & fractures."""
    total = 0.0
    amp = 1.0
    freq = 1.0
    weight = 1.0

    for _ in range(octaves):
        n = perlin3d(x * freq, y * freq, z * freq, perm)
        signal = 1.0 - abs(n)
        signal = signal * signal
        signal *= weight
        weight = max(0.0, min(1.0, signal * 2.0))

        total += signal * amp
        freq *= lacunarity
        amp *= gain

    return total

def domain_warp_fbm3d(x, y, z, warp_scale=0.35, octaves=4, perm=DEFAULT_PERM):
    """Domain Warping: feeds noise back into coordinate space for twisted geologic strata."""
    qx = fbm3d(x + 0.0, y + 0.0, z + 0.0, 3, perm=perm)
    qy = fbm3d(x + 5.2, y + 1.3, z + 2.8, 3, perm=perm)
    qz = fbm3d(x + 1.7, y + 9.2, z + 0.4, 3, perm=perm)

    rx = fbm3d(x + warp_scale * qx + 1.7, y + warp_scale * qy + 9.2, z + warp_scale * qz + 0.1, 3, perm=perm)
    ry = fbm3d(x + warp_scale * qx + 8.3, y + warp_scale * qy + 2.8, z + warp_scale * qz + 4.5, 3, perm=perm)
    rz = fbm3d(x + warp_scale * qx + 3.1, y + warp_scale * qy + 4.7, z + warp_scale * qz + 8.2, 3, perm=perm)

    return fbm3d(x + warp_scale * rx, y + warp_scale * ry, z + warp_scale * rz, octaves, perm=perm)

def simulate_thermal_erosion(vertices, talus_angle_deg=38.0, erosion_rate=0.15, iterations=4):
    """Simulates thermal erosion: material slides down slopes exceeding talus repose angle."""
    tan_talus = math.tan(math.radians(talus_angle_deg))
    eroded_verts = [list(v) for v in vertices]

    for _ in range(iterations):
        for i in range(len(eroded_verts)):
            v = eroded_verts[i]
            # Height and slope from ground plane
            dist_center = math.sqrt(v[0]*v[0] + v[2]*v[2]) + 0.0001
            slope = abs(v[1]) / dist_center
            if slope > tan_talus:
                # Displace down the slope
                delta = (slope - tan_talus) * 0.5 * erosion_rate
                eroded_verts[i][1] -= delta * 0.4
                # Settle at base
                eroded_verts[i][0] += (v[0] / dist_center) * delta * 0.3
                eroded_verts[i][2] += (v[2] / dist_center) * delta * 0.3

    return [tuple(v) for v in eroded_verts]

def simulate_hydraulic_erosion(vertices, droplet_count=120, sediment_capacity=2.5, dissolve_rate=0.25):
    """Simulates rain droplet hydraulic erosion carving micro-fissures and depositing sediment."""
    eroded = [list(v) for v in vertices]
    rng = random.Random(42)

    for _ in range(droplet_count):
        idx = rng.randint(0, len(eroded) - 1)
        # Droplet lands and runs down
        v = eroded[idx]
        if v[1] > 0.1: # High point: erode
            eroded[idx][1] -= dissolve_rate * 0.05
        else: # Low basin: deposit sediment
            eroded[idx][1] += dissolve_rate * 0.03

    return [tuple(v) for v in eroded]

# ---------------------------------------------------------------------------
# 2. PNG IMAGE GENERATION (Zero External Dependencies)
# ---------------------------------------------------------------------------

def write_png_rgba(filepath, width, height, rgba_data):
    """Writes standard deflate-compressed RGBA PNG image."""
    raw_scanlines = bytearray()
    for y in range(height):
        raw_scanlines.append(0)  # Filter type 0 (None)
        start_idx = y * width * 4
        raw_scanlines.extend(rgba_data[start_idx:start_idx + width * 4])

    compressed = zlib.compress(bytes(raw_scanlines), level=6)

    with open(filepath, 'wb') as f:
        # PNG Signature
        f.write(b'\x89PNG\r\n\x1a\n')
        # IHDR chunk
        ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
        f.write(struct.pack('>I', len(ihdr_data)))
        f.write(b'IHDR')
        f.write(ihdr_data)
        f.write(struct.pack('>I', zlib.crc32(b'IHDR' + ihdr_data)))
        # IDAT chunk
        f.write(struct.pack('>I', len(compressed)))
        f.write(b'IDAT')
        f.write(compressed)
        f.write(struct.pack('>I', zlib.crc32(b'IDAT' + compressed)))
        # IEND chunk
        f.write(struct.pack('>I', 0))
        f.write(b'IEND')
        f.write(struct.pack('>I', zlib.crc32(b'IEND')))

# ---------------------------------------------------------------------------
# 3. GLB (glTF 2.0 BINARY) WRITER
# ---------------------------------------------------------------------------

def write_valid_glb(filepath, vertices, normals, uvs, indices):
    """Creates a standard binary glTF 2.0 (.glb) file with position, normal, UVs, and indices."""
    pos_bytes = bytearray()
    min_pos = [float('inf'), float('inf'), float('inf')]
    max_pos = [float('-inf'), float('-inf'), float('-inf')]
    for v in vertices:
        pos_bytes.extend(struct.pack('<fff', v[0], v[1], v[2]))
        for i in range(3):
            min_pos[i] = min(min_pos[i], v[i])
            max_pos[i] = max(max_pos[i], v[i])

    norm_bytes = bytearray()
    for n in normals:
        norm_bytes.extend(struct.pack('<fff', n[0], n[1], n[2]))

    uv_bytes = bytearray()
    min_uv = [float('inf'), float('inf')]
    max_uv = [float('-inf'), float('-inf')]
    for uv in uvs:
        uv_bytes.extend(struct.pack('<ff', uv[0], uv[1]))
        min_uv[0] = min(min_uv[0], uv[0])
        min_uv[1] = min(min_uv[1], uv[1])
        max_uv[0] = max(max_uv[0], uv[0])
        max_uv[1] = max(max_uv[1], uv[1])

    idx_bytes = bytearray()
    min_idx = min(indices) if indices else 0
    max_idx = max(indices) if indices else 0
    for idx in indices:
        idx_bytes.extend(struct.pack('<H', idx))

    # Align each buffer to 4 bytes
    while len(pos_bytes) % 4 != 0: pos_bytes.append(0)
    while len(norm_bytes) % 4 != 0: norm_bytes.append(0)
    while len(uv_bytes) % 4 != 0: uv_bytes.append(0)
    while len(idx_bytes) % 4 != 0: idx_bytes.append(0)

    pos_offset = 0
    norm_offset = len(pos_bytes)
    uv_offset = norm_offset + len(norm_bytes)
    idx_offset = uv_offset + len(uv_bytes)
    total_bin_length = idx_offset + len(idx_bytes)

    bin_buffer = pos_bytes + norm_bytes + uv_bytes + idx_bytes

    gltf_json = {
        "asset": {
            "version": "2.0",
            "generator": "Apex 3D Procedural Rock Engine"
        },
        "scene": 0,
        "scenes": [
            {
                "name": "GeologicalScene",
                "nodes": [0]
            }
        ],
        "nodes": [
            {
                "name": "RockMeshNode",
                "mesh": 0
            }
        ],
        "meshes": [
            {
                "name": "ProceduralRockMesh",
                "primitives": [
                    {
                        "attributes": {
                            "POSITION": 0,
                            "NORMAL": 1,
                            "TEXCOORD_0": 2
                        },
                        "indices": 3,
                        "mode": 4
                    }
                ]
            }
        ],
        "buffers": [
            {
                "byteLength": total_bin_length
            }
        ],
        "bufferViews": [
            {
                "buffer": 0,
                "byteOffset": pos_offset,
                "byteLength": len(pos_bytes),
                "target": 34962
            },
            {
                "buffer": 0,
                "byteOffset": norm_offset,
                "byteLength": len(norm_bytes),
                "target": 34962
            },
            {
                "buffer": 0,
                "byteOffset": uv_offset,
                "byteLength": len(uv_bytes),
                "target": 34962
            },
            {
                "buffer": 0,
                "byteOffset": idx_offset,
                "byteLength": len(idx_bytes),
                "target": 34963
            }
        ],
        "accessors": [
            {
                "bufferView": 0,
                "byteOffset": 0,
                "componentType": 5126,
                "count": len(vertices),
                "type": "VEC3",
                "min": min_pos,
                "max": max_pos
            },
            {
                "bufferView": 1,
                "byteOffset": 0,
                "componentType": 5126,
                "count": len(normals),
                "type": "VEC3"
            },
            {
                "bufferView": 2,
                "byteOffset": 0,
                "componentType": 5126,
                "count": len(uvs),
                "type": "VEC2",
                "min": min_uv,
                "max": max_uv
            },
            {
                "bufferView": 3,
                "byteOffset": 0,
                "componentType": 5123,
                "count": len(indices),
                "type": "SCALAR",
                "min": [min_idx],
                "max": [max_idx]
            }
        ]
    }

    json_bytes = json.dumps(gltf_json, separators=(',', ':')).encode('utf-8')
    while len(json_bytes) % 4 != 0:
        json_bytes += b' '

    glb_length = 12 + 8 + len(json_bytes) + 8 + len(bin_buffer)

    with open(filepath, 'wb') as f:
        f.write(struct.pack('<4sII', b'glTF', 2, glb_length))
        f.write(struct.pack('<II', len(json_bytes), 0x4E4F534A))
        f.write(json_bytes)
        f.write(struct.pack('<II', len(bin_buffer), 0x004E4942))
        f.write(bin_buffer)

# ---------------------------------------------------------------------------
# 4. PROCEDURAL 3D ROCK MESH GENERATION (Deformed Icosphere)
# ---------------------------------------------------------------------------

def generate_base_icosphere(subdivisions=2):
    """Generates an initial regular icosahedron and subdivides it."""
    phi = (1.0 + math.sqrt(5.0)) / 2.0
    verts = [
        [-1,  phi, 0], [ 1,  phi, 0], [-1, -phi, 0], [ 1, -phi, 0],
        [ 0, -1,  phi], [ 0,  1,  phi], [ 0, -1, -phi], [ 0,  1, -phi],
        [ phi, 0, -1], [ phi, 0,  1], [-phi, 0, -1], [-phi, 0,  1]
    ]

    # Normalize to unit sphere
    normalized_verts = []
    for v in verts:
        length = math.sqrt(v[0]**2 + v[1]**2 + v[2]**2)
        normalized_verts.append([v[0] / length, v[1] / length, v[2] / length])

    faces = [
        [0, 11, 5], [0, 5, 1], [0, 1, 7], [0, 7, 10], [0, 10, 11],
        [1, 5, 9], [5, 11, 4], [11, 10, 2], [10, 7, 6], [7, 1, 8],
        [3, 9, 4], [3, 4, 2], [3, 2, 6], [3, 6, 8], [3, 8, 9],
        [4, 9, 5], [2, 4, 11], [6, 2, 10], [8, 6, 7], [9, 8, 1]
    ]

    midpoint_cache = {}

    def get_midpoint(i1, i2):
        key = tuple(sorted([i1, i2]))
        if key in midpoint_cache:
            return midpoint_cache[key]
        p1 = normalized_verts[i1]
        p2 = normalized_verts[i2]
        mid = [(p1[0] + p2[0]) * 0.5, (p1[1] + p2[1]) * 0.5, (p1[2] + p2[2]) * 0.5]
        length = math.sqrt(mid[0]**2 + mid[1]**2 + mid[2]**2)
        normalized_verts.append([mid[0] / length, mid[1] / length, mid[2] / length])
        idx = len(normalized_verts) - 1
        midpoint_cache[key] = idx
        return idx

    current_faces = faces
    for _ in range(subdivisions):
        new_faces = []
        for tri in current_faces:
            a = get_midpoint(tri[0], tri[1])
            b = get_midpoint(tri[1], tri[2])
            c = get_midpoint(tri[2], tri[0])
            new_faces.append([tri[0], a, c])
            new_faces.append([tri[1], b, a])
            new_faces.append([tri[2], c, b])
            new_faces.append([a, b, c])
        current_faces = new_faces

    return normalized_verts, current_faces

def build_procedural_rock_mesh(
    rock_name,
    scale_xyz=(0.85, 0.70, 0.80), # strictly fits within 1m x 1m cell (max radius < 0.5m)
    noise_seed=42,
    fbm_octaves=4,
    fbm_strength=0.35,
    voronoi_weight=0.25,
    strata_weight=0.10,
    crater_weight=0.05,
    base_radius=0.42 # meters (fits comfortably inside 1m x 1m x 1m sector)
):
    """
    Generates 3D rock geometry using combined procedural noise displacement.
    Returns: vertices, normals, uvs, indices, faces, wireframe_edges, volume_shapes
    """
    perm = PermutationTable(seed=noise_seed).p
    base_verts, faces = generate_base_icosphere(subdivisions=2) # 162 vertices, 320 triangles

    displaced_verts = []
    uvs = []

    for v in base_verts:
        nx, ny, nz = v[0], v[1], v[2]

        # 1. Multi-octave FBM displacement
        noise_val = fbm3d(nx * 2.2, ny * 2.2, nz * 2.2, octaves=fbm_octaves, perm=perm)

        # 2. Worley / Voronoi facets for sharp mineral fractures and cleavage
        d1, d2 = worley2d(nx * 1.8, ny * 1.8, cell_scale=1.5)
        voronoi_disp = (d2 - d1) * voronoi_weight

        # 3. Strata banding displacement
        strata_disp = strata_noise(ny * 3.0, noise_val) * strata_weight

        # 4. Impact cavities / regmaglypts (crater hollows)
        crater_disp = 0.0
        if crater_weight > 0.0:
            c_noise = perlin3d(nx * 4.0, ny * 4.0, nz * 4.0, perm=perm)
            if c_noise < -0.3:
                crater_disp = -(c_noise + 0.3) * crater_weight

        # Total radial displacement
        radius = base_radius * (1.0 + noise_val * fbm_strength - voronoi_disp + strata_disp - crater_disp)

        # Apply aspect ratio scaling (e.g. flat boulder vs tall monolith)
        vx = nx * radius * scale_xyz[0]
        vy = ny * radius * scale_xyz[1]
        vz = nz * radius * scale_xyz[2]

        # Add slight natural basal flat base so rock rests naturally on 1m ground cell
        if vy < -base_radius * 0.4:
            vy = vy * 0.85

        displaced_verts.append([round(vx, 4), round(vy, 4), round(vz, 4)])

        # Spherical UV mapping
        u = 0.5 + (math.atan2(nz, nx) / (2.0 * math.pi))
        v_coord = 0.5 - (math.asin(max(-1.0, min(1.0, ny))) / math.pi)
        uvs.append([round(u, 4), round(v_coord, 4)])

    # Compute area-weighted smooth vertex normals
    normals = [[0.0, 0.0, 0.0] for _ in range(len(displaced_verts))]
    for tri in faces:
        p0 = displaced_verts[tri[0]]
        p1 = displaced_verts[tri[1]]
        p2 = displaced_verts[tri[2]]

        # Cross product (p1 - p0) x (p2 - p0)
        ux = p1[0] - p0[0]; uy = p1[1] - p0[1]; uz = p1[2] - p0[2]
        vx = p2[0] - p0[0]; vy = p2[1] - p0[1]; vz = p2[2] - p0[2]

        fnx = uy * vz - uz * vy
        fny = uz * vx - ux * vz
        fnz = ux * vy - uy * vx

        for idx in tri:
            normals[idx][0] += fnx
            normals[idx][1] += fny
            normals[idx][2] += fnz

    normalized_normals = []
    for n in normals:
        length = math.sqrt(n[0]**2 + n[1]**2 + n[2]**2)
        if length > 1e-6:
            normalized_normals.append([round(n[0]/length, 4), round(n[1]/length, 4), round(n[2]/length, 4)])
        else:
            normalized_normals.append([0.0, 1.0, 0.0])

    # Flatten indices
    indices = []
    for tri in faces:
        indices.extend(tri)

    # Extract wireframe edges
    edges_set = set()
    for tri in faces:
        for i in range(3):
            e = tuple(sorted([tri[i], tri[(i + 1) % 3]]))
            edges_set.add(e)
    wireframe_edges = [list(e) for e in sorted(edges_set)]

    # Compute volume shape decomposition for architectural/volume diagram
    volume_shapes = decompose_rock_volume_shapes(rock_name, displaced_verts, faces)

    return displaced_verts, normalized_normals, uvs, indices, faces, wireframe_edges, volume_shapes

def decompose_rock_volume_shapes(rock_name, verts, faces):
    """Volumetric structural breakdown into geological components."""
    # Find bounding dimensions
    min_y = min(v[1] for v in verts)
    max_y = max(v[1] for v in verts)
    y_range = max_y - min_y

    threshold_low = min_y + y_range * 0.33
    threshold_mid = min_y + y_range * 0.66

    base_face_indices = []
    mid_face_indices = []
    crest_face_indices = []

    for i, face in enumerate(faces):
        avg_y = sum(verts[idx][1] for idx in face) / 3.0
        if avg_y <= threshold_low:
            base_face_indices.append(i)
        elif avg_y <= threshold_mid:
            mid_face_indices.append(i)
        else:
            crest_face_indices.append(i)

    return [
        {
            "id": f"{rock_name}_vol_base",
            "name": "Basal Bedrock Footing",
            "shape_type": "Anchored Convex Mass",
            "color_hex": "#708090",
            "face_indices": base_face_indices,
            "vertex_indices": list(set([idx for f in [faces[i] for i in base_face_indices] for idx in f])),
            "center": [0.0, round(min_y + y_range * 0.16, 3), 0.0],
            "dimensions": [0.75, round(y_range * 0.33, 3), 0.70],
            "volume_m3": round(0.12 * (y_range / 0.7), 4),
            "role": "Ground Contact Anchor (1m Cell)",
            "callout": "BASAL BEDROCK ZONE",
            "description": "Dense load-bearing base with fractured bedding plane contacting the 1m x 1m terrain cell."
        },
        {
            "id": f"{rock_name}_vol_core",
            "name": "Primary Boulder Lithosphere",
            "shape_type": "Volumetric Core Mass",
            "color_hex": "#00E5FF",
            "face_indices": mid_face_indices,
            "vertex_indices": list(set([idx for f in [faces[i] for i in mid_face_indices] for idx in f])),
            "center": [0.0, round(min_y + y_range * 0.50, 3), 0.0],
            "dimensions": [0.82, round(y_range * 0.34, 3), 0.78],
            "volume_m3": round(0.24 * (y_range / 0.7), 4),
            "role": "Lithic Mass Core",
            "callout": "PRIMARY LITHOSPHERE",
            "description": "High-density crystalline rock core resisting weathering with procedural fracture networks."
        },
        {
            "id": f"{rock_name}_vol_crest",
            "name": "Weathered Crag Crest",
            "shape_type": "Eroded Apical Spires",
            "color_hex": "#FF9900",
            "face_indices": crest_face_indices,
            "vertex_indices": list(set([idx for f in [faces[i] for i in crest_face_indices] for idx in f])),
            "center": [0.0, round(min_y + y_range * 0.83, 3), 0.0],
            "dimensions": [0.60, round(y_range * 0.33, 3), 0.55],
            "volume_m3": round(0.09 * (y_range / 0.7), 4),
            "role": "Aerodynamic / Weathering Summit",
            "callout": "APICAL CRAG SUMMIT",
            "description": "Exposed upper facet displaying multi-octave thermal spallation and wind-sculpted ridges."
        }
    ]

# ---------------------------------------------------------------------------
# 5. PROCEDURAL TEXTURE GENERATION USING NOISE LIBRARIES
# ---------------------------------------------------------------------------

def generate_procedural_rock_textures(tex_dir, archetype, size=128, seed=42):
    """
    Generates 4 procedural PBR texture maps using multi-octave noise:
    - albedo.png (color strata, granite grains, basalt cavities, crystal streaks)
    - normal.png (Sobel heightfield filter tangent-space normal map)
    - roughness.png (mineral micro-roughness variations)
    - emissive.png (crystallite vein radiance)
    """
    os.makedirs(tex_dir, exist_ok=True)
    perm = PermutationTable(seed=seed).p

    # Precompute heightfield for Sobel normal map
    heightfield = [[0.0 for _ in range(size)] for _ in range(size)]
    for y in range(size):
        ny = (y / size) * 4.0
        for x in range(size):
            nx = (x / size) * 4.0
            h = fbm3d(nx, ny, 1.5, octaves=3, perm=perm)
            d1, _ = worley2d(nx, ny, cell_scale=2.5)
            heightfield[y][x] = h * 0.7 + (1.0 - d1) * 0.3

    # Generate Maps
    albedo_data = bytearray()
    normal_data = bytearray()
    roughness_data = bytearray()
    emissive_data = bytearray()

    for y in range(size):
        ny = (y / size) * 4.0
        v_coord = y / size

        for x in range(size):
            nx = (x / size) * 4.0
            u_coord = x / size

            h_val = heightfield[y][x]
            noise_detail = perlin3d(nx * 8.0, ny * 8.0, 3.2, perm=perm)

            # 1. Albedo based on archetype
            if archetype == "rock_granite":
                # Speckled feldspar (pinkish/buff), quartz (grey/white), biotite mica (black)
                speckle = perlin3d(nx * 14.0, ny * 14.0, 7.1, perm=perm)
                if speckle > 0.35:
                    r, g, b = (45, 45, 48)  # Biotite flake
                elif speckle > -0.1:
                    r, g, b = (175, 145, 135) # Potassium feldspar
                else:
                    r, g, b = (205, 205, 215) # Quartz grain
            elif archetype == "rock_basalt":
                # Dark charcoal volcanic stone with weathered iron patina
                base_c = int(50 + h_val * 35 + noise_detail * 15)
                r = max(25, min(110, base_c + 8))
                g = max(25, min(110, base_c + 5))
                b = max(28, min(115, base_c + 12))
            elif archetype == "rock_meteorite":
                # Dark nickel-iron fusion crust with metallic sheen and olivine chondrules
                chondrule = worley2d(nx * 3.0, ny * 3.0, 2.0)[0]
                if chondrule < 0.2:
                    r, g, b = (130, 110, 45) # Olivine crystal
                else:
                    base_c = int(35 + h_val * 25)
                    r, g, b = (base_c + 5, base_c, base_c + 10)
            elif archetype == "rock_crystal":
                # Amethyst / quartz geode with glowing cyan/violet crystalline fissures
                vein = math.sin(ny * 6.0 + fbm3d(nx, ny, 2.0, octaves=3, perm=perm) * 4.0)
                if abs(vein) < 0.2:
                    r, g, b = (70, 230, 255) # Electric cyan crystal vein
                elif abs(vein) < 0.45:
                    r, g, b = (180, 80, 240) # Amethyst quartz
                else:
                    r, g, b = (80, 75, 85)   # Host rock matrix
            elif archetype == "rock_strata":
                # Sedimentary sandstone with warm ochre and terracotta strata bands
                band = math.sin(v_coord * 18.0 + noise_detail * 1.5) * 0.5 + 0.5
                r = int(180 + band * 50)
                g = int(120 + band * 35)
                b = int(80 + band * 25)
            else: # "rocks" / Master Archetype
                # Mountain boulder with lichen patches and geological strata
                lichen = perlin3d(nx * 2.5, ny * 2.5, 9.0, perm=perm)
                if lichen > 0.4:
                    r, g, b = (95, 135, 65)  # Lichen moss green
                else:
                    base_c = int(115 + h_val * 50 + noise_detail * 20)
                    r, g, b = (base_c, base_c - 5, base_c - 10)

            albedo_data.extend([r, g, b, 255])

            # 2. Normal Map via 3x3 Sobel filter
            x_prev = (x - 1) % size; x_next = (x + 1) % size
            y_prev = (y - 1) % size; y_next = (y + 1) % size
            dx = (heightfield[y][x_next] - heightfield[y][x_prev]) * 3.5
            dy = (heightfield[y_next][x] - heightfield[y_prev][x]) * 3.5

            norm_len = math.sqrt(dx * dx + dy * dy + 1.0)
            nx_val = -dx / norm_len
            ny_val = -dy / norm_len
            nz_val = 1.0 / norm_len

            norm_r = int((nx_val * 0.5 + 0.5) * 255)
            norm_g = int((ny_val * 0.5 + 0.5) * 255)
            norm_b = int((nz_val * 0.5 + 0.5) * 255)
            normal_data.extend([norm_r, norm_g, norm_b, 255])

            # 3. Roughness Map (crystalline veins smooth 0.2, rock matrix 0.85)
            if archetype == "rock_crystal":
                vein_dist = abs(math.sin(ny * 6.0 + fbm3d(nx, ny, 2.0, perm=perm) * 4.0))
                rough_val = int(50 + vein_dist * 180)
            elif archetype == "rock_meteorite":
                rough_val = int(80 + h_val * 80) # metallic polished surface
            else:
                rough_val = int(180 + h_val * 60 + noise_detail * 15)
            rough_val = max(20, min(255, rough_val))
            roughness_data.extend([rough_val, rough_val, rough_val, 255])

            # 4. Emissive Map
            if archetype == "rock_crystal":
                vein_dist = abs(math.sin(ny * 6.0 + fbm3d(nx, ny, 2.0, perm=perm) * 4.0))
                if vein_dist < 0.15:
                    em_r, em_g, em_b = (0, 240, 255) # Glowing cyan quartz fissure
                elif vein_dist < 0.35:
                    em_r, em_g, em_b = (120, 20, 200) # Soft purple resonance
                else:
                    em_r, em_g, em_b = (0, 0, 0)
            else:
                em_r, em_g, em_b = (0, 0, 0)
            emissive_data.extend([em_r, em_g, em_b, 255])

    # Write PNG files
    write_png_rgba(os.path.join(tex_dir, "albedo.png"), size, size, albedo_data)
    write_png_rgba(os.path.join(tex_dir, "normal.png"), size, size, normal_data)
    write_png_rgba(os.path.join(tex_dir, "roughness.png"), size, size, roughness_data)
    write_png_rgba(os.path.join(tex_dir, "emissive.png"), size, size, emissive_data)

    manifest = {
        "asset": archetype,
        "resolution": f"{size}x{size}",
        "format": "PNG-RGBA8",
        "channels": {
            "albedo.png": "Diffuse Color (Sediment strata, mineral composition, weathering)",
            "normal.png": "Tangent-Space Normal Map (Sobel gradient bump detail)",
            "roughness.png": "Micro-roughness (Crystalline smooth vs porous weathered stone)",
            "emissive.png": "Radiant mineral vein luminescence"
        },
        "geological_shader_model": "PBR Metallic-Roughness (Burley Diffuse + GGX Specular)"
    }
    with open(os.path.join(tex_dir, "texture_manifest.json"), "w") as f:
        json.dump(manifest, f, indent=2)

# ---------------------------------------------------------------------------
# 6. WAVEFRONT OBJ WRITER
# ---------------------------------------------------------------------------

def write_wavefront_obj(filepath, name, verts, normals, uvs, faces):
    with open(filepath, 'w') as f:
        f.write(f"# Wavefront OBJ File - Apex Procedural Rock Generator\n")
        f.write(f"# Object Name: {name}\n")
        f.write(f"# Grid Cell Bound: 1.0m x 1.0m\n")
        f.write(f"o {name}\n\n")

        for v in verts:
            f.write(f"v {v[0]:.4f} {v[1]:.4f} {v[2]:.4f}\n")
        f.write("\n")

        for vt in uvs:
            f.write(f"vt {vt[0]:.4f} {vt[1]:.4f}\n")
        f.write("\n")

        for vn in normals:
            f.write(f"vn {vn[0]:.4f} {vn[1]:.4f} {vn[2]:.4f}\n")
        f.write("\n")

        f.write("s 1\n")
        for face in faces:
            # 1-indexed in OBJ
            f.write(f"f {face[0]+1}/{face[0]+1}/{face[0]+1} {face[1]+1}/{face[1]+1}/{face[1]+1} {face[2]+1}/{face[2]+1}/{face[2]+1}\n")

# ---------------------------------------------------------------------------
# 7. 10x10 GRID FIELD SIMULATION (Each Cell Exactly 1.0m x 1.0m)
# ---------------------------------------------------------------------------

def generate_10x10_rock_grid_catalog(seed=777):
    """
    Generates a full 10x10 spatial field (100 cells total, each 1.0m x 1.0m).
    Total field footprint: 10m x 10m.
    Coordinates centered from X: -4.5m to +4.5m, Z: -4.5m to +4.5m.
    """
    perm = PermutationTable(seed=seed).p
    archetypes = [
        ("rock_granite", "Granite Boulder", 2680.0, "#00E5FF", (0.80, 0.65, 0.75)),
        ("rock_basalt", "Volcanic Basalt Column", 2950.0, "#FF8800", (0.50, 0.90, 0.50)),
        ("rock_meteorite", "Chondrite Meteorite", 3400.0, "#39FF14", (0.65, 0.55, 0.60)),
        ("rock_crystal", "Quartz Geode Cluster", 2650.0, "#BF55EC", (0.55, 0.75, 0.55)),
        ("rock_strata", "Sedimentary Sandstone", 2350.0, "#FFCC00", (0.85, 0.45, 0.70))
    ]

    cells = []

    for row in range(10): # Z index (0 to 9)
        world_z = round((row - 4.5) * 1.0, 2) # -4.5m to +4.5m

        for col in range(10): # X index (0 to 9)
            world_x = round((col - 4.5) * 1.0, 2) # -4.5m to +4.5m

            # Macro terrain elevation via 2D Perlin noise in [-0.2m, +0.35m]
            terrain_elevation = round(perlin3d(col * 0.4, row * 0.4, 1.2, perm=perm) * 0.25 + 0.10, 3)

            # Cell specific noise seed
            cell_seed = (row * 31 + col * 17 + seed) & 0xFFFF

            # Deterministic archetype distribution across the 10x10 grid
            arch_idx = (row * 3 + col * 7) % len(archetypes)
            arch_id, arch_title, density_kg_m3, hex_color, default_scale = archetypes[arch_idx]

            # Procedural scale variation per cell (always strictly < 1.0m so rock fits in 1m x 1m cell)
            scale_var = 0.85 + perlin3d(col * 0.8, row * 0.8, 5.5, perm=perm) * 0.15
            dim_x = round(default_scale[0] * scale_var, 3)
            dim_y = round(default_scale[1] * scale_var, 3)
            dim_z = round(default_scale[2] * scale_var, 3)

            # Volume calculation (approx ellipsoid V = 4/3 * pi * a * b * c)
            volume_m3 = round((4.0 / 3.0) * math.pi * (dim_x / 2.0) * (dim_y / 2.0) * (dim_z / 2.0) * 0.82, 4)
            mass_kg = round(volume_m3 * density_kg_m3, 1)

            cell_data = {
                "sector_id": f"CELL_{row:02d}_{col:02d}",
                "grid_row": row,
                "grid_col": col,
                "cell_size_meters": [1.0, 1.0],
                "world_position_meters": [world_x, terrain_elevation, world_z],
                "archetype_id": arch_id,
                "rock_title": f"{arch_title} [{row},{col}]",
                "dimensions_meters": [dim_x, dim_y, dim_z],
                "volume_m3": volume_m3,
                "mass_kg": mass_kg,
                "mineral_density_kg_m3": density_kg_m3,
                "accent_color": hex_color,
                "noise_parameters": {
                    "seed": cell_seed,
                    "fbm_octaves": 4,
                    "frequency": round(2.0 + (col % 3) * 0.4, 2),
                    "voronoi_facets": round(0.20 + (row % 4) * 0.08, 2),
                    "surface_roughness": round(0.65 + perlin3d(col, row, 3.0, perm=perm) * 0.2, 2)
                },
                "geological_notes": f"1m² grid sector {row},{col}. {arch_title} resting on local terrain bedrock."
            }
            cells.append(cell_data)

    grid_catalog = {
        "grid_version": "2.0",
        "description": "Procedural 10x10 Rock Field Terrain Matrix",
        "grid_dimensions": {
            "columns": 10,
            "rows": 10,
            "total_cells": 100,
            "cell_size_meters": 1.0, # Exactly 1m x 1m cell size
            "total_field_size_meters": [10.0, 10.0],
            "total_area_m2": 100.0,
            "coordinate_bounds": {
                "min_x_meters": -4.5,
                "max_x_meters": 4.5,
                "min_z_meters": -4.5,
                "max_z_meters": 4.5
            }
        },
        "cells": cells
    }
    return grid_catalog

# ---------------------------------------------------------------------------
# 8. MASTER ASSET BUILD PIPELINE (Modular Structure)
# ---------------------------------------------------------------------------

def build_single_modular_rock_asset(
    asset_id,
    display_title,
    category,
    accent_hex,
    scale_xyz,
    seed,
    fbm_strength,
    voronoi_weight,
    strata_weight,
    crater_weight,
    density_kg_m3,
    target_roots
):
    """
    Builds a complete modular asset package:
      asset_id/
        glb/       -> asset_id.glb
        textures/  -> albedo.png, normal.png, roughness.png, emissive.png, texture_manifest.json
        meshes/    -> asset_id.obj, geometry.json, wireframe_edges.json
        json/      -> metadata.json, specs.json, physics.json, materials.json
    """
    print(f"\n--> Generating Modular Asset: {asset_id} ({display_title})...")

    # 1. Generate Mesh Geometry
    verts, normals, uvs, indices, faces, wireframe_edges, volume_shapes = build_procedural_rock_mesh(
        rock_name=asset_id,
        scale_xyz=scale_xyz,
        noise_seed=seed,
        fbm_octaves=4,
        fbm_strength=fbm_strength,
        voronoi_weight=voronoi_weight,
        strata_weight=strata_weight,
        crater_weight=crater_weight,
        base_radius=0.42 # Fits in 1m x 1m sector
    )

    # Calculate actual bounding box
    min_x = min(v[0] for v in verts); max_x = max(v[0] for v in verts)
    min_y = min(v[1] for v in verts); max_y = max(v[1] for v in verts)
    min_z = min(v[2] for v in verts); max_z = max(v[2] for v in verts)

    dim_x = round(max_x - min_x, 3)
    dim_y = round(max_y - min_y, 3)
    dim_z = round(max_z - min_z, 3)

    volume_m3 = round(dim_x * dim_y * dim_z * 0.52, 4)
    mass_kg = round(volume_m3 * density_kg_m3, 1)

    geometry_dict = {
        "name": asset_id,
        "vertex_count": len(verts),
        "face_count": len(faces),
        "edge_count": len(wireframe_edges),
        "vertices": verts,
        "normals": normals,
        "uvs": uvs,
        "indices": indices,
        "faces": faces,
        "volume_shapes": volume_shapes
    }

    wireframe_dict = {
        "name": asset_id,
        "edge_count": len(wireframe_edges),
        "edges": wireframe_edges
    }

    metadata_dict = {
        "id": f"asset_{asset_id}",
        "title": display_title,
        "category": category,
        "grid_footprint": "1.0m x 1.0m Cell",
        "mass_kg": mass_kg,
        "volume_m3": volume_m3,
        "density_kg_m3": density_kg_m3,
        "lod": "LOD0 High-Fidelity Geodesic",
        "author": "Apex Geological Procedural Engine",
        "version": "2.0.0",
        "tags": ["rock", "geology", "1m_grid", "procedural", "noise", "pbr"]
    }

    specs_dict = {
        "model_code": f"GEO-1M-{asset_id.upper()}",
        "grid_cell_size": "1.0m x 1.0m",
        "dimensions_meters": f"{dim_x}m (W) x {dim_y}m (H) x {dim_z}m (D)",
        "mineral_classification": category,
        "noise_generator": "Perlin + FBM + Worley Voronoi Facets + Strata",
        "fracture_cleavage": "Basal and Prismatic Planar Fracture Networks",
        "geological_origin": "Igneous / Metamorphic / Planetary Impact",
        "weathering_index": "Sub-aerial Thermal Spallation Grade IV",
        "volume_diagram_breakdown": {
            "total_volume_m3": volume_m3,
            "components": [v["name"] for v in volume_shapes]
        }
    }

    physics_dict = {
        "mass_kg": mass_kg,
        "center_of_mass": [0.0, round(min_y + dim_y * 0.42, 3), 0.0],
        "bounding_box": {
            "min": [min_x, min_y, min_z],
            "max": [max_x, max_y, max_z]
        },
        "friction_coefficient": 0.78,
        "restitution": 0.12,
        "collider_type": "Convex Polyhedron Hull (1m Boundary)"
    }

    materials_dict = {
        "shader_model": "PBR Metallic-Roughness (Burley Specular GGX)",
        "base_color_factor": [1.0, 1.0, 1.0, 1.0],
        "metallic_factor": 0.15 if asset_id == "rock_meteorite" else 0.04,
        "roughness_factor": 0.45 if asset_id == "rock_crystal" else 0.82,
        "emissive_factor": [0.0, 0.85, 1.0] if asset_id == "rock_crystal" else [0.0, 0.0, 0.0],
        "emissive_strength": 2.5 if asset_id == "rock_crystal" else 0.0,
        "normal_scale": 1.25
    }

    # Generate once into first target root, then copy
    first_root = target_roots[0]
    first_asset_folder = os.path.join(first_root, asset_id)
    first_tex_dir = os.path.join(first_asset_folder, "textures")
    generate_procedural_rock_textures(first_tex_dir, archetype=asset_id, size=128, seed=seed)

    for root_dir in target_roots:
        asset_folder = os.path.join(root_dir, asset_id)

        # Create modular subdirectories: glb, textures, meshes, json
        glb_dir = os.path.join(asset_folder, "glb")
        tex_dir = os.path.join(asset_folder, "textures")
        mesh_dir = os.path.join(asset_folder, "meshes")
        json_dir = os.path.join(asset_folder, "json")

        for d in (glb_dir, tex_dir, mesh_dir, json_dir):
            os.makedirs(d, exist_ok=True)

        # 1. glb/
        glb_path = os.path.join(glb_dir, f"{asset_id}.glb")
        write_valid_glb(glb_path, verts, normals, uvs, indices)

        # 2. meshes/
        obj_path = os.path.join(mesh_dir, f"{asset_id}.obj")
        write_wavefront_obj(obj_path, asset_id, verts, normals, uvs, faces)

        with open(os.path.join(mesh_dir, "geometry.json"), "w") as f:
            json.dump(geometry_dict, f, indent=2)

        with open(os.path.join(mesh_dir, "wireframe_edges.json"), "w") as f:
            json.dump(wireframe_dict, f, indent=2)

        # 3. textures/ (copy from first generated if not first)
        if root_dir != first_root:
            for tf in ("albedo.png", "normal.png", "roughness.png", "emissive.png", "texture_manifest.json"):
                src_tf = os.path.join(first_tex_dir, tf)
                dst_tf = os.path.join(tex_dir, tf)
                if os.path.isfile(src_tf):
                    with open(src_tf, "rb") as sf, open(dst_tf, "wb") as df:
                        df.write(sf.read())

        # 4. json/
        with open(os.path.join(json_dir, "metadata.json"), "w") as f:
            json.dump(metadata_dict, f, indent=2)

        with open(os.path.join(json_dir, "specs.json"), "w") as f:
            json.dump(specs_dict, f, indent=2)

        with open(os.path.join(json_dir, "physics.json"), "w") as f:
            json.dump(physics_dict, f, indent=2)

        with open(os.path.join(json_dir, "materials.json"), "w") as f:
            json.dump(materials_dict, f, indent=2)

    print(f"  [OK] Successfully built {asset_id} across all target directories.")

def main():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    target_roots = [
        os.path.join(base_dir, "asserts"),
        os.path.join(base_dir, "app", "src", "main", "assets"),
        os.path.join(base_dir, "assets")
    ]

    print("=================================================================")
    print("Apex Procedural Rock Engine - 10x10 Grid (1m x 1m Cells)")
    print("=================================================================")
    for r in target_roots:
        os.makedirs(r, exist_ok=True)
        print(f"Target Root: {r}")

    # Build primary "rocks" asset and specialized modular rock archetypes
    rock_configs = [
        {
            "asset_id": "rocks",
            "display_title": "Procedural Field Rock (1m² Sector)",
            "category": "Composite Geological Field Stone",
            "accent_hex": "#00E5FF",
            "scale_xyz": (0.85, 0.72, 0.80),
            "seed": 101,
            "fbm_strength": 0.35,
            "voronoi_weight": 0.28,
            "strata_weight": 0.12,
            "crater_weight": 0.08,
            "density_kg_m3": 2720.0
        },
        {
            "asset_id": "rock_granite",
            "display_title": "Granite Boulder (1m² Cell)",
            "category": "Plutonic Quartz-Feldspar Granite",
            "accent_hex": "#39FF14",
            "scale_xyz": (0.80, 0.65, 0.75),
            "seed": 202,
            "fbm_strength": 0.28,
            "voronoi_weight": 0.22,
            "strata_weight": 0.06,
            "crater_weight": 0.04,
            "density_kg_m3": 2680.0
        },
        {
            "asset_id": "rock_basalt",
            "display_title": "Volcanic Basalt Column (1m² Cell)",
            "category": "Extrusive Mafic Volcanic Basalt",
            "accent_hex": "#FF8800",
            "scale_xyz": (0.50, 0.92, 0.50), # Columnar monolith
            "seed": 303,
            "fbm_strength": 0.32,
            "voronoi_weight": 0.45, # High prismatic facets
            "strata_weight": 0.08,
            "crater_weight": 0.15, # Vesicular cavities
            "density_kg_m3": 2950.0
        },
        {
            "asset_id": "rock_meteorite",
            "display_title": "Chondrite Meteorite (1m² Cell)",
            "category": "Extra-Terrestrial Nickel-Iron Regolith",
            "accent_hex": "#00F0FF",
            "scale_xyz": (0.65, 0.55, 0.60),
            "seed": 404,
            "fbm_strength": 0.40,
            "voronoi_weight": 0.20,
            "strata_weight": 0.02,
            "crater_weight": 0.28, # Impact craters / regmaglypts
            "density_kg_m3": 3400.0
        },
        {
            "asset_id": "rock_crystal",
            "display_title": "Quartz Geode Cluster (1m² Cell)",
            "category": "Hydrothermal Crystalline Silicate",
            "accent_hex": "#BF55EC",
            "scale_xyz": (0.55, 0.78, 0.55),
            "seed": 505,
            "fbm_strength": 0.38,
            "voronoi_weight": 0.55, # Sharp crystal facets
            "strata_weight": 0.04,
            "crater_weight": 0.02,
            "density_kg_m3": 2650.0
        },
        {
            "asset_id": "rock_strata",
            "display_title": "Sedimentary Sandstone (1m² Cell)",
            "category": "Fluvial Stratified Sandstone",
            "accent_hex": "#FFCC00",
            "scale_xyz": (0.85, 0.42, 0.70), # Flat stratified slab
            "seed": 606,
            "fbm_strength": 0.24,
            "voronoi_weight": 0.18,
            "strata_weight": 0.35, # High sedimentary stratification
            "crater_weight": 0.03,
            "density_kg_m3": 2350.0
        }
    ]

    for cfg in rock_configs:
        build_single_modular_rock_asset(
            asset_id=cfg["asset_id"],
            display_title=cfg["display_title"],
            category=cfg["category"],
            accent_hex=cfg["accent_hex"],
            scale_xyz=cfg["scale_xyz"],
            seed=cfg["seed"],
            fbm_strength=cfg["fbm_strength"],
            voronoi_weight=cfg["voronoi_weight"],
            strata_weight=cfg["strata_weight"],
            crater_weight=cfg["crater_weight"],
            density_kg_m3=cfg["density_kg_m3"],
            target_roots=target_roots
        )

    # Generate 10x10 Grid Spatial Catalog (Each Cell 1m x 1m)
    print("\n--> Generating 10x10 Grid Field Catalog (100 cells, 1m x 1m each)...")
    grid_catalog = generate_10x10_rock_grid_catalog(seed=777)

    # Master Asset Catalog
    asset_catalog = {
        "catalog_version": "2.0",
        "description": "Procedural Rock Assets & 10x10 Grid Field Architecture",
        "timestamp": "2026-10-06T13:00:00Z",
        "grid_meta": {
            "columns": 10,
            "rows": 10,
            "cell_size_m": 1.0,
            "total_area_m2": 100.0
        },
        "assets": [
            {
                "id": "rocks",
                "name": "10x10 Procedural Rock Field",
                "folder": "rocks",
                "category": "Field Terrain (1m² Grid)",
                "icon_desc": "Procedural Field Rock",
                "primary_color": "#00E5FF",
                "accent_color": "#7000FF"
            },
            {
                "id": "rock_granite",
                "name": "Granite Boulder",
                "folder": "rock_granite",
                "category": "Plutonic Granite",
                "icon_desc": "Quartz-Feldspar Boulder",
                "primary_color": "#39FF14",
                "accent_color": "#00E5FF"
            },
            {
                "id": "rock_basalt",
                "name": "Volcanic Basalt Column",
                "folder": "rock_basalt",
                "category": "Extrusive Basalt",
                "icon_desc": "Prismatic Basalt Column",
                "primary_color": "#FF8800",
                "accent_color": "#FF3366"
            },
            {
                "id": "rock_meteorite",
                "name": "Chondrite Meteorite",
                "folder": "rock_meteorite",
                "category": "Extra-Terrestrial Regolith",
                "icon_desc": "Pitted Impact Asteroid",
                "primary_color": "#00F0FF",
                "accent_color": "#BF55EC"
            },
            {
                "id": "rock_crystal",
                "name": "Quartz Geode Cluster",
                "folder": "rock_crystal",
                "category": "Hydrothermal Silicate",
                "icon_desc": "Amethyst Quartz Spires",
                "primary_color": "#BF55EC",
                "accent_color": "#00FFFF"
            },
            {
                "id": "rock_strata",
                "name": "Sedimentary Sandstone",
                "folder": "rock_strata",
                "category": "Stratified Sandstone",
                "icon_desc": "Layered Terraced Slab",
                "primary_color": "#FFCC00",
                "accent_color": "#FF9900"
            }
        ]
    }

    for root_dir in target_roots:
        with open(os.path.join(root_dir, "asset_catalog.json"), "w") as f:
            json.dump(asset_catalog, f, indent=2)

        with open(os.path.join(root_dir, "rocks_grid_10x10.json"), "w") as f:
            json.dump(grid_catalog, f, indent=2)

    print("\n[COMPLETE] Successfully generated all procedural rock assets and 10x10 (1m x 1m) grid!")

if __name__ == "__main__":
    main()
