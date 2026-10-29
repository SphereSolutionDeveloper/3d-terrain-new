#!/usr/bin/env python3
"""
===============================================================================
PROCEDURAL 3D TREE GENERATOR USING STOCHASTIC L-SYSTEMS & PERLIN NOISE
Based on the mathematical framework by Nemanja Stojanovic (2016):
"A method for generating stochastic 3D tree models with Python in Autodesk Maya"
===============================================================================
Features:
- 3D Turtle Orientation: Heading (H), Left (L), Up (U) orthonormal vectors
- Leonardo da Vinci Branch Radius Scaling: w_r = 0.707
- Stochastic Branching Rules with Probabilistic Productions & Angles (25 - 40 deg)
- Truncated Cone Branch Cylinders with Smooth Normals & UVs
- Foliage Leaf Quad Placement on Terminal Nodes
- Procedural Bark & Foliage Textures via Multi-Octave Perlin Noise & Sobel Normals
- 10x10 Forest Grid (100 cells, EACH strictly 1.0m x 1.0m, total 10m x 10m plot)
- Clean Modular Formats: .glb, .obj, geometry.json, wireframe_edges.json,
  metadata.json, specs.json, physics.json, materials.json, texture_manifest.json
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
# 1. 3D VECTOR & MATRIX MATH (Pure Python)
# ---------------------------------------------------------------------------

def vec3_add(a, b):
    return [a[0] + b[0], a[1] + b[1], a[2] + b[2]]

def vec3_sub(a, b):
    return [a[0] - b[0], a[1] - b[1], a[2] - b[2]]

def vec3_scale(v, s):
    return [v[0] * s, v[1] * s, v[2] * s]

def vec3_length(v):
    return math.sqrt(v[0]**2 + v[1]**2 + v[2]**2)

def vec3_normalize(v):
    l = vec3_length(v)
    if l > 1e-7:
        return [v[0] / l, v[1] / l, v[2] / l]
    return [0.0, 1.0, 0.0]

def vec3_cross(a, b):
    return [
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0]
    ]

def vec3_dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

def rotate_vector_axis(v, axis, angle_rad):
    """Rodrigues rotation formula."""
    axis = vec3_normalize(axis)
    cos_a = math.cos(angle_rad)
    sin_a = math.sin(angle_rad)
    dot = vec3_dot(axis, v)
    cross = vec3_cross(axis, v)
    return [
        v[0] * cos_a + cross[0] * sin_a + axis[0] * dot * (1.0 - cos_a),
        v[1] * cos_a + cross[1] * sin_a + axis[1] * dot * (1.0 - cos_a),
        v[2] * cos_a + cross[2] * sin_a + axis[2] * dot * (1.0 - cos_a)
    ]

# ---------------------------------------------------------------------------
# 2. NOISE LIBRARY FOR PROCEDURAL BARK & NORMAL MAPS
# ---------------------------------------------------------------------------

class FastNoise:
    def __init__(self, seed=1337):
        rng = random.Random(seed)
        p = list(range(256))
        rng.shuffle(p)
        self.p = p + p

    def fade(self, t):
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0)

    def lerp(self, a, b, t):
        return a + t * (b - a)

    def grad2d(self, hash_val, x, y):
        h = hash_val & 7
        u = x if h < 4 else y
        v = y if h < 4 else x
        return (u if (h & 1) == 0 else -u) + (v if (h & 2) == 0 else -v)

    def noise2d(self, x, y):
        X = int(math.floor(x)) & 255
        Y = int(math.floor(y)) & 255
        x -= math.floor(x)
        y -= math.floor(y)
        u = self.fade(x)
        v = self.fade(y)
        A = self.p[X] + Y
        B = self.p[X + 1] + Y
        return self.lerp(
            self.lerp(self.grad2d(self.p[A], x, y), self.grad2d(self.p[B], x - 1, y), u),
            self.lerp(self.grad2d(self.p[A + 1], x, y - 1), self.grad2d(self.p[B + 1], x - 1, y - 1), u),
            v
        )

    def fbm2d(self, x, y, octaves=3):
        total = 0.0
        freq = 1.0
        amp = 1.0
        max_amp = 0.0
        for _ in range(octaves):
            total += self.noise2d(x * freq, y * freq) * amp
            max_amp += amp
            amp *= 0.5
            freq *= 2.0
        return total / max_amp

# ---------------------------------------------------------------------------
# 3. STOCHASTIC L-SYSTEM TURTLE GRAPHICS (3D)
# ---------------------------------------------------------------------------

class TurtleState:
    def __init__(self, pos, H, L, U, radius, length, depth):
        self.pos = list(pos)
        self.H = list(H) # Heading vector
        self.L = list(L) # Left vector
        self.U = list(U) # Up vector
        self.radius = radius
        self.length = length
        self.depth = depth

def generate_stochastic_lsystem_string(iterations, seed=42):
    """
    Evaluates stochastic L-system string production rules with probabilities.
    Symbols:
      F: Move forward & create branch
      +: Yaw right, -: Yaw left
      &: Pitch down, ^: Pitch up
      /: Roll right, \: Roll left
      [: Push branch state
      ]: Pop branch state
    """
    rng = random.Random(seed)
    current = "FA"

    for _ in range(iterations):
        next_str = []
        for ch in current:
            if ch == 'A':
                # Stochastic distribution rules from paper
                p = rng.random()
                a1 = rng.randint(25, 40)
                a2 = rng.randint(25, 40)
                roll = rng.randint(45, 120)

                if p < 0.35:
                    # Bilateral bifurcated branching
                    next_str.append(f"[&({a1})/({roll})FA][^({a2})\\({roll})FA]")
                elif p < 0.65:
                    # Trilateral branching with main leader shoot
                    next_str.append(f"F[&({a1})FA][+({a2})\\({roll})FA][^({a1})/({roll})FA]")
                elif p < 0.85:
                    # Asymmetric lateral branch
                    next_str.append(f"[+({a1})/({roll})FA][-({a2})\\({roll})FA]")
                else:
                    # Vertical shoot extension with lateral fork
                    next_str.append(f"F[&({a1})FA]")
            else:
                next_str.append(ch)
        current = "".join(next_str)

    return current

# ---------------------------------------------------------------------------
# 4. 3D TREE MESH GENERATION (Truncated Cones + Leonardo da Vinci Rule)
# ---------------------------------------------------------------------------

def build_tree_mesh(
    tree_type="tree_oak",
    base_height=0.92, # strictly fits in 1m x 1m cell
    base_radius=0.045, # trunk radius in meters
    leonardo_factor=0.707, # Leonardo da Vinci factor: r_new = r_prev * 0.707
    iterations=3,
    radial_segments=6,
    seed=101
):
    rng = random.Random(seed)
    lsystem_str = generate_stochastic_lsystem_string(iterations=iterations, seed=seed)

    # Initial 3D Turtle orientation (pointing vertically along +Y)
    pos = [0.0, 0.0, 0.0]
    H = [0.0, 1.0, 0.0]  # Heading up
    L = [-1.0, 0.0, 0.0] # Left
    U = [0.0, 0.0, 1.0]  # Up / Forward Z

    initial_step = base_height * 0.28
    turtle = TurtleState(pos, H, L, U, base_radius, initial_step, depth=0)
    stack = []

    vertices = []
    normals = []
    uvs = []
    faces = []
    leaves_faces = []

    def add_truncated_cone(p_start, p_end, r_start, r_end, dir_vec):
        """Creates a truncated cone segment between p_start and p_end."""
        # Find orthogonal vectors to dir_vec for cylinder circle vertices
        ref = [0.0, 0.0, 1.0] if abs(dir_vec[2]) < 0.9 else [1.0, 0.0, 0.0]
        side1 = vec3_normalize(vec3_cross(dir_vec, ref))
        side2 = vec3_normalize(vec3_cross(dir_vec, side1))

        base_idx = len(vertices)

        # Bottom ring
        for i in range(radial_segments):
            angle = (2.0 * math.pi * i) / radial_segments
            cos_a = math.cos(angle)
            sin_a = math.sin(angle)
            norm = [side1[0] * cos_a + side2[0] * sin_a,
                    side1[1] * cos_a + side2[1] * sin_a,
                    side1[2] * cos_a + side2[2] * sin_a]
            pt = [p_start[0] + norm[0] * r_start,
                  p_start[1] + norm[1] * r_start,
                  p_start[2] + norm[2] * r_start]
            vertices.append([round(pt[0], 4), round(pt[1], 4), round(pt[2], 4)])
            normals.append([round(norm[0], 4), round(norm[1], 4), round(norm[2], 4)])
            uvs.append([round(i / radial_segments, 4), 0.0])

        # Top ring
        for i in range(radial_segments):
            angle = (2.0 * math.pi * i) / radial_segments
            cos_a = math.cos(angle)
            sin_a = math.sin(angle)
            norm = [side1[0] * cos_a + side2[0] * sin_a,
                    side1[1] * cos_a + side2[1] * sin_a,
                    side1[2] * cos_a + side2[2] * sin_a]
            pt = [p_end[0] + norm[0] * r_end,
                  p_end[1] + norm[1] * r_end,
                  p_end[2] + norm[2] * r_end]
            vertices.append([round(pt[0], 4), round(pt[1], 4), round(pt[2], 4)])
            normals.append([round(norm[0], 4), round(norm[1], 4), round(norm[2], 4)])
            uvs.append([round(i / radial_segments, 4), 1.0])

        # Triangulate side walls
        for i in range(radial_segments):
            next_i = (i + 1) % radial_segments
            b1 = base_idx + i
            b2 = base_idx + next_i
            t1 = base_idx + radial_segments + i
            t2 = base_idx + radial_segments + next_i

            faces.append([b1, b2, t1])
            faces.append([b2, t2, t1])

    def add_leaf_cluster(p_end, dir_vec, leaf_size=0.085):
        """Creates foliage leaf quads facing outward at terminal branch tip."""
        base_idx = len(vertices)
        ref = [0.0, 1.0, 0.0] if abs(dir_vec[1]) < 0.9 else [1.0, 0.0, 0.0]
        s1 = vec3_normalize(vec3_cross(dir_vec, ref))
        s2 = vec3_normalize(vec3_cross(dir_vec, s1))

        # Diamond leaf quad 1
        p1 = vec3_add(p_end, vec3_scale(s1, -leaf_size))
        p2 = vec3_add(p_end, vec3_scale(dir_vec, leaf_size * 1.5))
        p3 = vec3_add(p_end, vec3_scale(s1, leaf_size))
        p4 = vec3_add(p_end, vec3_scale(dir_vec, -leaf_size * 0.5))

        for pt in (p1, p2, p3, p4):
            vertices.append([round(pt[0], 4), round(pt[1], 4), round(pt[2], 4)])
            normals.append([0.0, 1.0, 0.0])
            uvs.append([0.5, 0.5])

        faces.append([base_idx, base_idx + 1, base_idx + 2])
        faces.append([base_idx, base_idx + 2, base_idx + 3])
        leaves_faces.extend([len(faces) - 2, len(faces) - 1])

        # Cross leaf quad 2
        p5 = vec3_add(p_end, vec3_scale(s2, -leaf_size))
        p6 = vec3_add(p_end, vec3_scale(dir_vec, leaf_size * 1.5))
        p7 = vec3_add(p_end, vec3_scale(s2, leaf_size))
        p8 = vec3_add(p_end, vec3_scale(dir_vec, -leaf_size * 0.5))

        for pt in (p5, p6, p7, p8):
            vertices.append([round(pt[0], 4), round(pt[1], 4), round(pt[2], 4)])
            normals.append([0.0, 1.0, 0.0])
            uvs.append([0.5, 0.5])

        faces.append([base_idx + 4, base_idx + 5, base_idx + 6])
        faces.append([base_idx + 4, base_idx + 6, base_idx + 7])
        leaves_faces.extend([len(faces) - 2, len(faces) - 1])

    # Parse L-system commands
    i = 0
    max_branch_depth = 0

    while i < len(lsystem_str):
        c = lsystem_str[i]

        # Extract parameter in parentheses if present e.g. &(30)
        param = 30.0
        if i + 1 < len(lsystem_str) and lsystem_str[i + 1] == '(':
            end_p = lsystem_str.find(')', i + 1)
            if end_p != -1:
                try:
                    param = float(lsystem_str[i + 2:end_p])
                except:
                    param = 30.0
                i = end_p

        rad = math.radians(param)

        if c == 'F':
            # Create truncated cone branch
            start_pos = list(turtle.pos)
            step_len = turtle.length * (0.85 + rng.random() * 0.3)
            end_pos = vec3_add(start_pos, vec3_scale(turtle.H, step_len))
            r_start = turtle.radius
            r_end = turtle.radius * leonardo_factor # Da Vinci rule

            add_truncated_cone(start_pos, end_pos, r_start, r_end, turtle.H)

            turtle.pos = end_pos
            turtle.radius = r_end
            turtle.length *= 0.88 # Natural branch length attenuation

        elif c == '+':
            # Yaw right around U
            turtle.H = rotate_vector_axis(turtle.H, turtle.U, rad)
            turtle.L = rotate_vector_axis(turtle.L, turtle.U, rad)
        elif c == '-':
            # Yaw left around U
            turtle.H = rotate_vector_axis(turtle.H, turtle.U, -rad)
            turtle.L = rotate_vector_axis(turtle.L, turtle.U, -rad)
        elif c == '&':
            # Pitch down around L
            turtle.H = rotate_vector_axis(turtle.H, turtle.L, rad)
            turtle.U = rotate_vector_axis(turtle.U, turtle.L, rad)
        elif c == '^':
            # Pitch up around L
            turtle.H = rotate_vector_axis(turtle.H, turtle.L, -rad)
            turtle.U = rotate_vector_axis(turtle.U, turtle.L, -rad)
        elif c == '/':
            # Roll right around H
            turtle.L = rotate_vector_axis(turtle.L, turtle.H, rad)
            turtle.U = rotate_vector_axis(turtle.U, turtle.H, rad)
        elif c == '\\':
            # Roll left around H
            turtle.L = rotate_vector_axis(turtle.L, turtle.H, -rad)
            turtle.U = rotate_vector_axis(turtle.U, turtle.H, -rad)
        elif c == '[':
            # Push state
            stack.append(TurtleState(turtle.pos, turtle.H, turtle.L, turtle.U,
                                     turtle.radius, turtle.length, turtle.depth + 1))
            turtle.depth += 1
            if turtle.depth > max_branch_depth:
                max_branch_depth = turtle.depth
        elif c == ']':
            # Terminal tip leaf generation
            if turtle.depth >= 2:
                add_leaf_cluster(turtle.pos, turtle.H, leaf_size=0.065)

            # Pop state
            if stack:
                prev = stack.pop()
                turtle = TurtleState(prev.pos, prev.H, prev.L, prev.U,
                                     prev.radius, prev.length, prev.depth)

        i += 1

    # Flatten indices
    indices = []
    for f in faces:
        indices.extend(f)

    # Wireframe edges
    edges_set = set()
    for f in faces:
        for j in range(3):
            e = tuple(sorted([f[j], f[(j + 1) % 3]]))
            edges_set.add(e)
    wireframe_edges = [list(e) for e in sorted(edges_set)]

    # Volumetric breakdown
    volume_shapes = decompose_tree_volumes(tree_type, vertices, faces, leaves_faces)

    return vertices, normals, uvs, indices, faces, wireframe_edges, volume_shapes

def decompose_tree_volumes(tree_type, verts, faces, leaves_faces):
    """Volumetric structural breakdown into botanical tree components."""
    min_y = min(v[1] for v in verts) if verts else 0.0
    max_y = max(v[1] for v in verts) if verts else 1.0
    height = max_y - min_y

    trunk_faces = []
    bough_faces = []
    canopy_faces = []

    leaves_set = set(leaves_faces)

    for i, face in enumerate(faces):
        if i in leaves_set:
            canopy_faces.append(i)
            continue
        avg_y = sum(verts[idx][1] for idx in face) / 3.0
        if avg_y <= min_y + height * 0.35:
            trunk_faces.append(i)
        elif avg_y <= min_y + height * 0.70:
            bough_faces.append(i)
        else:
            canopy_faces.append(i)

    return [
        {
            "id": f"{tree_type}_vol_trunk",
            "name": "Bole Trunk Base (1m Cell)",
            "shape_type": "Truncated Cone Cylinder",
            "color_hex": "#8B5A2B",
            "face_indices": trunk_faces,
            "vertex_indices": list(set([idx for f in [faces[i] for i in trunk_faces] for idx in f])),
            "center": [0.0, round(min_y + height * 0.17, 3), 0.0],
            "dimensions": [0.18, round(height * 0.35, 3), 0.18],
            "volume_m3": round(0.015 * height, 4),
            "role": "Load-Bearing Main Bole",
            "callout": "BASAL TRUNK COLUMN",
            "description": "Heartwood structural trunk satisfying Da Vinci cross-sectional area conservation."
        },
        {
            "id": f"{tree_type}_vol_boughs",
            "name": "Primary Scaffold Boughs",
            "shape_type": "Stochastic Forked Limbs",
            "color_hex": "#CD853F",
            "face_indices": bough_faces,
            "vertex_indices": list(set([idx for f in [faces[i] for i in bough_faces] for idx in f])),
            "center": [0.0, round(min_y + height * 0.52, 3), 0.0],
            "dimensions": [0.45, round(height * 0.35, 3), 0.45],
            "volume_m3": round(0.018 * height, 4),
            "role": "Scaffold Limb Network",
            "callout": "SCAFFOLD BOUGH FORKS",
            "description": "Stochastic L-system branching forks diverged at 25-40 degree angles."
        },
        {
            "id": f"{tree_type}_vol_canopy",
            "name": "Foliage Crown & Terminal Leaves",
            "shape_type": "Photosynthetic Leaf Clusters",
            "color_hex": "#39FF14",
            "face_indices": canopy_faces,
            "vertex_indices": list(set([idx for f in [faces[i] for i in canopy_faces] for idx in f])),
            "center": [0.0, round(min_y + height * 0.85, 3), 0.0],
            "dimensions": [0.65, round(height * 0.30, 3), 0.65],
            "volume_m3": round(0.022 * height, 4),
            "role": "Photosynthetic Crown",
            "callout": "FOLIAGE CANOPY CROWN",
            "description": "Terminal diamond leaf quads attached to branch tips with sunlight interception."
        }
    ]

# ---------------------------------------------------------------------------
# 5. PROCEDURAL BARK & LEAF TEXTURE MAPS (Perlin Noise & Sobel)
# ---------------------------------------------------------------------------

def write_png_rgba(filepath, width, height, rgba_data):
    """Writes standard deflate-compressed RGBA PNG image."""
    raw_scanlines = bytearray()
    for y in range(height):
        raw_scanlines.append(0)
        start_idx = y * width * 4
        raw_scanlines.extend(rgba_data[start_idx:start_idx + width * 4])

    compressed = zlib.compress(bytes(raw_scanlines), level=6)

    with open(filepath, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n')
        ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
        f.write(struct.pack('>I', len(ihdr_data)))
        f.write(b'IHDR')
        f.write(ihdr_data)
        f.write(struct.pack('>I', zlib.crc32(b'IHDR' + ihdr_data)))
        f.write(struct.pack('>I', len(compressed)))
        f.write(b'IDAT')
        f.write(compressed)
        f.write(struct.pack('>I', zlib.crc32(b'IDAT' + compressed)))
        f.write(struct.pack('>I', 0))
        f.write(b'IEND')
        f.write(struct.pack('>I', zlib.crc32(b'IEND')))

def generate_procedural_tree_textures(tex_dir, tree_type, size=128, seed=42):
    """Generates procedural bark albedo, Sobel normal map, roughness, and emissive."""
    os.makedirs(tex_dir, exist_ok=True)
    noise = FastNoise(seed=seed)

    # Precompute bark heightfield with vertical furrow elongation
    heightfield = [[0.0 for _ in range(size)] for _ in range(size)]
    for y in range(size):
        ny = (y / size) * 16.0 # Stretched vertically for tree bark ridges
        for x in range(size):
            nx = (x / size) * 4.0
            n_val = noise.fbm2d(nx, ny, octaves=3)
            # Bark furrow ridges formula: sin(x * freq + noise * turbulence)
            furrow = math.sin(nx * 12.0 + n_val * 4.0) * 0.5 + 0.5
            heightfield[y][x] = furrow * 0.7 + n_val * 0.3

    albedo_data = bytearray()
    normal_data = bytearray()
    roughness_data = bytearray()
    emissive_data = bytearray()

    for y in range(size):
        for x in range(size):
            h_val = heightfield[y][x]
            n_detail = noise.noise2d((x / size) * 10.0, (y / size) * 10.0)

            # 1. Albedo based on tree type
            if tree_type == "tree_birch":
                # Silver Birch: chalk white bark with black horizontal lenticels
                if h_val < 0.28:
                    r, g, b = (35, 35, 40) # Black birch knot
                else:
                    c = int(220 + h_val * 30 + n_detail * 10)
                    r = g = b = max(180, min(255, c))
            elif tree_type == "tree_pine":
                # Scotch Pine: reddish-brown flaky resinous bark
                base_c = int(120 + h_val * 50)
                r = min(220, base_c + 35)
                g = max(40, base_c - 30)
                b = max(20, base_c - 50)
            elif tree_type == "tree_willow":
                # Weeping Willow: deep olive grey bark
                r = int(75 + h_val * 40)
                g = int(85 + h_val * 45)
                b = int(60 + h_val * 30)
            elif tree_type == "tree_acacia":
                # Umbrella Acacia: rough charcoal brown with amber resin streaks
                r = int(95 + h_val * 55)
                g = int(70 + h_val * 40)
                b = int(50 + h_val * 30)
            else: # "trees" / "tree_oak"
                # English Oak: warm rugged furrowed timber brown with moss flecks
                if n_detail > 0.4:
                    r, g, b = (65, 115, 45) # Moss green patch
                else:
                    base_c = int(90 + h_val * 60)
                    r = min(210, base_c + 15)
                    g = min(170, base_c)
                    b = max(30, base_c - 25)

            albedo_data.extend([r, g, b, 255])

            # 2. Tangent-Space Normal Map via 3x3 Sobel filter
            x_prev = (x - 1) % size; x_next = (x + 1) % size
            y_prev = (y - 1) % size; y_next = (y + 1) % size
            dx = (heightfield[y][x_next] - heightfield[y][x_prev]) * 3.8
            dy = (heightfield[y_next][x] - heightfield[y_prev][x]) * 2.2

            norm_len = math.sqrt(dx * dx + dy * dy + 1.0)
            nx_val = -dx / norm_len
            ny_val = -dy / norm_len
            nz_val = 1.0 / norm_len

            norm_r = int((nx_val * 0.5 + 0.5) * 255)
            norm_g = int((ny_val * 0.5 + 0.5) * 255)
            norm_b = int((nz_val * 0.5 + 0.5) * 255)
            normal_data.extend([norm_r, norm_g, norm_b, 255])

            # 3. Roughness Map (Bark is naturally matte rough ~0.82 - 0.95)
            rough_val = int(190 + h_val * 50)
            rough_val = max(120, min(255, rough_val))
            roughness_data.extend([rough_val, rough_val, rough_val, 255])

            # 4. Emissive Map
            emissive_data.extend([0, 0, 0, 255])

    write_png_rgba(os.path.join(tex_dir, "albedo.png"), size, size, albedo_data)
    write_png_rgba(os.path.join(tex_dir, "normal.png"), size, size, normal_data)
    write_png_rgba(os.path.join(tex_dir, "roughness.png"), size, size, roughness_data)
    write_png_rgba(os.path.join(tex_dir, "emissive.png"), size, size, emissive_data)

    manifest = {
        "asset": tree_type,
        "resolution": f"{size}x{size}",
        "format": "PNG-RGBA8",
        "channels": {
            "albedo.png": "Bark Diffuse Timber Grain & Moss Flecks",
            "normal.png": "Tangent-Space Normal Map (Sobel bark furrow crevices)",
            "roughness.png": "Micro-roughness (Weathered outer bark vs cambium)",
            "emissive.png": "Non-emissive timber"
        },
        "botanical_shader_model": "PBR Metallic-Roughness (Burley Bark + Translucent Leaf BSDF)"
    }
    with open(os.path.join(tex_dir, "texture_manifest.json"), "w") as f:
        json.dump(manifest, f, indent=2)

# ---------------------------------------------------------------------------
# 6. GLB & OBJ WRITERS
# ---------------------------------------------------------------------------

def write_valid_glb(filepath, vertices, normals, uvs, indices):
    """Creates a standard binary glTF 2.0 (.glb) container."""
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
            "generator": "Apex 3D Stochastic L-System Tree Pipeline"
        },
        "scene": 0,
        "scenes": [{"name": "ForestScene", "nodes": [0]}],
        "nodes": [{"name": "TreeMeshNode", "mesh": 0}],
        "meshes": [
            {
                "name": "BotanicalTreeMesh",
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
        "buffers": [{"byteLength": total_bin_length}],
        "bufferViews": [
            {"buffer": 0, "byteOffset": pos_offset, "byteLength": len(pos_bytes), "target": 34962},
            {"buffer": 0, "byteOffset": norm_offset, "byteLength": len(norm_bytes), "target": 34962},
            {"buffer": 0, "byteOffset": uv_offset, "byteLength": len(uv_bytes), "target": 34962},
            {"buffer": 0, "byteOffset": idx_offset, "byteLength": len(idx_bytes), "target": 34963}
        ],
        "accessors": [
            {"bufferView": 0, "byteOffset": 0, "componentType": 5126, "count": len(vertices), "type": "VEC3", "min": min_pos, "max": max_pos},
            {"bufferView": 1, "byteOffset": 0, "componentType": 5126, "count": len(normals), "type": "VEC3"},
            {"bufferView": 2, "byteOffset": 0, "componentType": 5126, "count": len(uvs), "type": "VEC2", "min": min_uv, "max": max_uv},
            {"bufferView": 3, "byteOffset": 0, "componentType": 5123, "count": len(indices), "type": "SCALAR", "min": [min_idx], "max": [max_idx]}
        ]
    }

    json_bytes = json.dumps(gltf_json, separators=(',', ':')).encode('utf-8')
    while len(json_bytes) % 4 != 0: json_bytes += b' '

    glb_length = 12 + 8 + len(json_bytes) + 8 + len(bin_buffer)

    with open(filepath, 'wb') as f:
        f.write(struct.pack('<4sII', b'glTF', 2, glb_length))
        f.write(struct.pack('<II', len(json_bytes), 0x4E4F534A))
        f.write(json_bytes)
        f.write(struct.pack('<II', len(bin_buffer), 0x004E4942))
        f.write(bin_buffer)

def write_wavefront_obj(filepath, name, verts, normals, uvs, faces):
    with open(filepath, 'w') as f:
        f.write(f"# Wavefront OBJ File - Apex Stochastic L-System Tree Engine\n")
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
            f.write(f"f {face[0]+1}/{face[0]+1}/{face[0]+1} {face[1]+1}/{face[1]+1}/{face[1]+1} {face[2]+1}/{face[2]+1}/{face[2]+1}\n")

# ---------------------------------------------------------------------------
# 7. 10x10 FOREST TREE GRID (100 Cells, Each 1.0m x 1.0m)
# ---------------------------------------------------------------------------

def generate_10x10_tree_grid_catalog(seed=888):
    """
    Generates a full 10x10 spatial forest matrix (100 sectors, each 1.0m x 1.0m).
    Total forest grove footprint: 10m x 10m.
    Coordinates centered from X: -4.5m to +4.5m, Z: -4.5m to +4.5m.
    """
    noise = FastNoise(seed=seed)
    archetypes = [
        ("tree_oak", "English Oak", 720.0, "#39FF14", (0.85, 0.95, 0.85)),
        ("tree_pine", "Scotch Pine", 560.0, "#00E5FF", (0.65, 0.98, 0.65)),
        ("tree_birch", "Silver Birch", 650.0, "#FFCC00", (0.55, 0.92, 0.55)),
        ("tree_willow", "Weeping Willow", 580.0, "#00F0FF", (0.90, 0.85, 0.90)),
        ("tree_acacia", "Umbrella Acacia", 780.0, "#FF9900", (0.95, 0.78, 0.95))
    ]

    cells = []

    for row in range(10): # Z index (0 to 9)
        world_z = round((row - 4.5) * 1.0, 2)

        for col in range(10): # X index (0 to 9)
            world_x = round((col - 4.5) * 1.0, 2)

            terrain_elevation = round(noise.noise2d(col * 0.4, row * 0.4) * 0.20 + 0.08, 3)
            cell_seed = (row * 43 + col * 19 + seed) & 0xFFFF

            arch_idx = (row * 4 + col * 3) % len(archetypes)
            arch_id, arch_title, wood_density, hex_color, default_bounds = archetypes[arch_idx]

            scale_var = 0.85 + noise.noise2d(col * 0.7, row * 0.7) * 0.15
            dim_x = round(default_bounds[0] * scale_var, 3)
            dim_y = round(default_bounds[1] * scale_var, 3)
            dim_z = round(default_bounds[2] * scale_var, 3)

            trunk_diameter_m = round(0.08 + (col % 4) * 0.025, 3)
            volume_m3 = round(math.pi * ((trunk_diameter_m / 2.0) ** 2) * (dim_y * 0.45) + (dim_x * dim_z * 0.05), 4)
            wood_mass_kg = round(volume_m3 * wood_density, 1)

            stochastic_angle = 25 + ((row * 7 + col * 11) % 16) # 25 to 40 degrees

            cell_data = {
                "sector_id": f"FOREST_{row:02d}_{col:02d}",
                "grid_row": row,
                "grid_col": col,
                "cell_size_meters": [1.0, 1.0],
                "world_position_meters": [world_x, terrain_elevation, world_z],
                "archetype_id": arch_id,
                "tree_title": f"{arch_title} [{row},{col}]",
                "dimensions_meters": [dim_x, dim_y, dim_z],
                "volume_m3": volume_m3,
                "mass_kg": wood_mass_kg,
                "timber_density_kg_m3": wood_density,
                "accent_color": hex_color,
                "botanical_parameters": {
                    "seed": cell_seed,
                    "lsystem_iterations": 3,
                    "branching_angle_deg": stochastic_angle,
                    "leonardo_ratio": 0.707,
                    "leaf_coverage": round(0.70 + (col % 3) * 0.10, 2)
                },
                "geological_notes": f"1m² woodland sector {row},{col}. {arch_title} rooted in soil bedrock."
            }
            cells.append(cell_data)

    return {
        "grid_version": "2.0",
        "description": "Procedural 10x10 Stochastic L-System Tree Forest Matrix",
        "grid_dimensions": {
            "columns": 10,
            "rows": 10,
            "total_cells": 100,
            "cell_size_meters": 1.0,
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

# ---------------------------------------------------------------------------
# 8. MASTER ASSET BUILD PIPELINE (Modular Structure)
# ---------------------------------------------------------------------------

def build_single_modular_tree_asset(
    asset_id,
    display_title,
    category,
    accent_hex,
    base_height,
    base_radius,
    iterations,
    density_kg_m3,
    seed,
    target_roots
):
    print(f"\n--> Generating Modular Tree Asset: {asset_id} ({display_title})...")

    # 1. Generate 3D Tree Mesh using Stochastic L-System & Leonardo rule
    verts, normals, uvs, indices, faces, wireframe_edges, volume_shapes = build_tree_mesh(
        tree_type=asset_id,
        base_height=base_height,
        base_radius=base_radius,
        leonardo_factor=0.707,
        iterations=iterations,
        radial_segments=6,
        seed=seed
    )

    min_x = min(v[0] for v in verts) if verts else -0.3; max_x = max(v[0] for v in verts) if verts else 0.3
    min_y = min(v[1] for v in verts) if verts else 0.0;  max_y = max(v[1] for v in verts) if verts else base_height
    min_z = min(v[2] for v in verts) if verts else -0.3; max_z = max(v[2] for v in verts) if verts else 0.3

    dim_x = round(max_x - min_x, 3)
    dim_y = round(max_y - min_y, 3)
    dim_z = round(max_z - min_z, 3)

    volume_m3 = round(math.pi * ((base_radius) ** 2) * (dim_y * 0.45) + (dim_x * dim_z * 0.05), 4)
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
        "lod": "LOD0 High-Fidelity Stochastic L-System",
        "author": "Apex Botanical Procedural Engine",
        "version": "2.0.0",
        "tags": ["tree", "lsystem", "stochastic", "leonardo_0.707", "perlin_bark", "1m_grid"]
    }

    specs_dict = {
        "model_code": f"BOT-1M-{asset_id.upper()}",
        "grid_cell_size": "1.0m x 1.0m",
        "dimensions_meters": f"{dim_x}m (W) x {dim_y}m (H) x {dim_z}m (D)",
        "botanical_classification": category,
        "generator_algorithm": "3D Stochastic L-System + Turtle Graphics (Stojanovic 2016)",
        "branch_conservation_rule": "Leonardo da Vinci Postulate (w_r = 0.707)",
        "branching_angle_range": "Stochastic 25 deg - 40 deg with randomized roll",
        "foliage_distribution": "Terminal Node Diametral Leaf Quads",
        "volume_diagram_breakdown": {
            "total_volume_m3": volume_m3,
            "components": [v["name"] for v in volume_shapes]
        }
    }

    physics_dict = {
        "mass_kg": mass_kg,
        "center_of_mass": [0.0, round(min_y + dim_y * 0.45, 3), 0.0],
        "bounding_box": {
            "min": [min_x, min_y, min_z],
            "max": [max_x, max_y, max_z]
        },
        "friction_coefficient": 0.65,
        "restitution": 0.08,
        "collider_type": "Botanical Trunk & Canopy Convex Hull (1m Boundary)"
    }

    materials_dict = {
        "shader_model": "PBR Metallic-Roughness (Burley Bark + Leaf Subsurface)",
        "base_color_factor": [1.0, 1.0, 1.0, 1.0],
        "metallic_factor": 0.02,
        "roughness_factor": 0.88,
        "emissive_factor": [0.0, 0.0, 0.0],
        "emissive_strength": 0.0,
        "normal_scale": 1.45
    }

    # Generate textures into first root, then copy
    first_root = target_roots[0]
    first_asset_folder = os.path.join(first_root, asset_id)
    first_tex_dir = os.path.join(first_asset_folder, "textures")
    generate_procedural_tree_textures(first_tex_dir, tree_type=asset_id, size=128, seed=seed)

    for root_dir in target_roots:
        asset_folder = os.path.join(root_dir, asset_id)

        glb_dir = os.path.join(asset_folder, "glb")
        tex_dir = os.path.join(asset_folder, "textures")
        mesh_dir = os.path.join(asset_folder, "meshes")
        json_dir = os.path.join(asset_folder, "json")

        for d in (glb_dir, tex_dir, mesh_dir, json_dir):
            os.makedirs(d, exist_ok=True)

        # 1. glb/
        write_valid_glb(os.path.join(glb_dir, f"{asset_id}.glb"), verts, normals, uvs, indices)

        # 2. meshes/
        write_wavefront_obj(os.path.join(mesh_dir, f"{asset_id}.obj"), asset_id, verts, normals, uvs, faces)
        with open(os.path.join(mesh_dir, "geometry.json"), "w") as f:
            json.dump(geometry_dict, f, indent=2)
        with open(os.path.join(mesh_dir, "wireframe_edges.json"), "w") as f:
            json.dump(wireframe_dict, f, indent=2)

        # 3. textures/
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

    print(f"  [OK] Successfully built tree {asset_id} across all target directories.")

def main():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    target_roots = [
        os.path.join(base_dir, "asserts"),
        os.path.join(base_dir, "app", "src", "main", "assets"),
        os.path.join(base_dir, "assets")
    ]

    print("=================================================================")
    print("Apex Procedural Tree Engine - Stochastic L-Systems (1m x 1m Cells)")
    print("=================================================================")
    for r in target_roots:
        os.makedirs(r, exist_ok=True)
        print(f"Target Root: {r}")

    tree_configs = [
        {
            "asset_id": "trees",
            "display_title": "Procedural Forest Tree (1m² Sector)",
            "category": "Broadleaf Deciduous Tree",
            "accent_hex": "#39FF14",
            "base_height": 0.92,
            "base_radius": 0.045,
            "iterations": 3,
            "density_kg_m3": 700.0,
            "seed": 111
        },
        {
            "asset_id": "tree_oak",
            "display_title": "English Oak (1m² Cell)",
            "category": "Quercus Robur (Deciduous Oak)",
            "accent_hex": "#2ECC71",
            "base_height": 0.95,
            "base_radius": 0.052,
            "iterations": 3,
            "density_kg_m3": 720.0,
            "seed": 222
        },
        {
            "asset_id": "tree_pine",
            "display_title": "Scotch Pine (1m² Cell)",
            "category": "Pinus Sylvestris (Coniferous Pine)",
            "accent_hex": "#00E5FF",
            "base_height": 0.98,
            "base_radius": 0.038,
            "iterations": 3,
            "density_kg_m3": 560.0,
            "seed": 333
        },
        {
            "asset_id": "tree_birch",
            "display_title": "Silver Birch (1m² Cell)",
            "category": "Betula Pendula (Silver Birch)",
            "accent_hex": "#FFCC00",
            "base_height": 0.92,
            "base_radius": 0.032,
            "iterations": 3,
            "density_kg_m3": 650.0,
            "seed": 444
        },
        {
            "asset_id": "tree_willow",
            "display_title": "Weeping Willow (1m² Cell)",
            "category": "Salix Babylonica (Pendant Willow)",
            "accent_hex": "#00F0FF",
            "base_height": 0.88,
            "base_radius": 0.048,
            "iterations": 3,
            "density_kg_m3": 580.0,
            "seed": 555
        },
        {
            "asset_id": "tree_acacia",
            "display_title": "Umbrella Acacia (1m² Cell)",
            "category": "Vachellia Tortilis (Savanna Acacia)",
            "accent_hex": "#FF9900",
            "base_height": 0.82,
            "base_radius": 0.042,
            "iterations": 3,
            "density_kg_m3": 780.0,
            "seed": 666
        }
    ]

    for cfg in tree_configs:
        build_single_modular_tree_asset(
            asset_id=cfg["asset_id"],
            display_title=cfg["display_title"],
            category=cfg["category"],
            accent_hex=cfg["accent_hex"],
            base_height=cfg["base_height"],
            base_radius=cfg["base_radius"],
            iterations=cfg["iterations"],
            density_kg_m3=cfg["density_kg_m3"],
            seed=cfg["seed"],
            target_roots=target_roots
        )

    # Generate 10x10 Forest Grid Spatial Catalog
    print("\n--> Generating 10x10 Forest Grid Catalog (100 cells, 1m x 1m each)...")
    tree_grid_catalog = generate_10x10_tree_grid_catalog(seed=888)

    for root_dir in target_roots:
        with open(os.path.join(root_dir, "trees_grid_10x10.json"), "w") as f:
            json.dump(tree_grid_catalog, f, indent=2)

    # Append trees to asset_catalog.json
    for root_dir in target_roots:
        cat_file = os.path.join(root_dir, "asset_catalog.json")
        try:
            with open(cat_file, "r") as f:
                cat = json.load(f)
        except:
            cat = {"catalog_version": "2.0", "assets": []}

        existing_ids = {a["id"] for a in cat.get("assets", [])}

        tree_entries = [
            {
                "id": "trees",
                "name": "10x10 Forest Tree Field",
                "folder": "trees",
                "category": "Forest Plot (1m² Grid)",
                "icon_desc": "Stochastic L-System Forest",
                "primary_color": "#39FF14",
                "accent_color": "#00E5FF"
            },
            {
                "id": "tree_oak",
                "name": "English Oak Tree",
                "folder": "tree_oak",
                "category": "Deciduous Broadleaf",
                "icon_desc": "Stochastic English Oak",
                "primary_color": "#2ECC71",
                "accent_color": "#39FF14"
            },
            {
                "id": "tree_pine",
                "name": "Scotch Pine Tree",
                "folder": "tree_pine",
                "category": "Coniferous Evergreen",
                "icon_desc": "Needle-Branch Scotch Pine",
                "primary_color": "#00E5FF",
                "accent_color": "#7000FF"
            },
            {
                "id": "tree_birch",
                "name": "Silver Birch Tree",
                "folder": "tree_birch",
                "category": "Deciduous Birch",
                "icon_desc": "Chalk-Bark Silver Birch",
                "primary_color": "#FFCC00",
                "accent_color": "#FF9900"
            },
            {
                "id": "tree_willow",
                "name": "Weeping Willow Tree",
                "folder": "tree_willow",
                "category": "Pendant Salicaceae",
                "icon_desc": "Drooping Weeping Willow",
                "primary_color": "#00F0FF",
                "accent_color": "#BF55EC"
            },
            {
                "id": "tree_acacia",
                "name": "Umbrella Acacia Tree",
                "folder": "tree_acacia",
                "category": "Savanna Fabaceae",
                "icon_desc": "Flat-Top Umbrella Acacia",
                "primary_color": "#FF9900",
                "accent_color": "#FF3366"
            }
        ]

        for entry in tree_entries:
            if entry["id"] not in existing_ids:
                cat["assets"].append(entry)
                existing_ids.add(entry["id"])

        with open(cat_file, "w") as f:
            json.dump(cat, f, indent=2)

    print("\n[COMPLETE] Successfully generated all stochastic L-system tree assets and 10x10 forest grid!")

if __name__ == "__main__":
    main()
