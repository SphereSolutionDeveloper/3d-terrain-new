#!/usr/bin/env python3
"""
3D Asset Pipeline Script
Generates fully-structured modular 3D asset packages:
- ship/ (textures/, json/, meshes/, glb/)
- drone/ (textures/, json/, meshes/, glb/)
- rover/ (textures/, json/, meshes/, glb/)
- mech_core/ (textures/, json/, meshes/, glb/)
"""

import os
import json
import math
import struct
import shutil

def create_png_image(filename, width, height, r_fn, g_fn, b_fn, a_val=255):
    """Generates an uncompressed or simple valid PNG file without external dependencies."""
    import zlib
    
    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0)  # Filter type 0 (None)
        for x in range(width):
            r = int(max(0, min(255, r_fn(x, y, width, height))))
            g = int(max(0, min(255, g_fn(x, y, width, height))))
            b = int(max(0, min(255, b_fn(x, y, width, height))))
            raw_data.extend([r, g, b, a_val])
            
    compressed = zlib.compress(bytes(raw_data))
    
    with open(filename, 'wb') as f:
        # PNG signature
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

def create_valid_glb(filepath, vertices, normals, uvs, indices):
    """
    Creates a standard binary glTF 2.0 (.glb) file with position, normal, and indices.
    """
    import zlib
    
    # Pack positions (vec3 float32)
    pos_bytes = bytearray()
    min_pos = [float('inf'), float('inf'), float('inf')]
    max_pos = [float('-inf'), float('-inf'), float('-inf')]
    for v in vertices:
        pos_bytes.extend(struct.pack('<fff', v[0], v[1], v[2]))
        for i in range(3):
            min_pos[i] = min(min_pos[i], v[i])
            max_pos[i] = max(max_pos[i], v[i])
            
    # Pack normals (vec3 float32)
    norm_bytes = bytearray()
    for n in normals:
        norm_bytes.extend(struct.pack('<fff', n[0], n[1], n[2]))
        
    # Pack indices (scalar uint16)
    idx_bytes = bytearray()
    min_idx = min(indices)
    max_idx = max(indices)
    for idx in indices:
        idx_bytes.extend(struct.pack('<H', idx))
        
    # Align to 4 bytes
    while len(pos_bytes) % 4 != 0:
        pos_bytes.append(0)
    while len(norm_bytes) % 4 != 0:
        norm_bytes.append(0)
    while len(idx_bytes) % 4 != 0:
        idx_bytes.append(0)
        
    pos_offset = 0
    norm_offset = len(pos_bytes)
    idx_offset = norm_offset + len(norm_bytes)
    total_bin_length = idx_offset + len(idx_bytes)
    
    bin_buffer = pos_bytes + norm_bytes + idx_bytes
    
    gltf_json = {
        "asset": {
            "version": "2.0",
            "generator": "Apex 3D Asset Pipeline"
        },
        "scene": 0,
        "scenes": [
            {
                "name": "DefaultScene",
                "nodes": [0]
            }
        ],
        "nodes": [
            {
                "name": "ModelNode",
                "mesh": 0
            }
        ],
        "meshes": [
            {
                "name": "MeshGeometry",
                "primitives": [
                    {
                        "attributes": {
                            "POSITION": 0,
                            "NORMAL": 1
                        },
                        "indices": 2,
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
                "componentType": 5123,
                "count": len(indices),
                "type": "SCALAR",
                "min": [min_idx],
                "max": [max_idx]
            }
        ]
    }
    
    json_bytes = json.dumps(gltf_json, separators=(',', ':')).encode('utf-8')
    # Pad JSON to 4-byte boundary with space (0x20)
    while len(json_bytes) % 4 != 0:
        json_bytes += b' '
        
    glb_length = 12 + 8 + len(json_bytes) + 8 + len(bin_buffer)
    
    with open(filepath, 'wb') as f:
        # GLB Header
        f.write(struct.pack('<4sII', b'glTF', 2, glb_length))
        
        # Chunk 0: JSON
        f.write(struct.pack('<II', len(json_bytes), 0x4E4F534A))
        f.write(json_bytes)
        
        # Chunk 1: BIN
        f.write(struct.pack('<II', len(bin_buffer), 0x004E4942))
        f.write(bin_buffer)

def write_obj_file(filepath, name, vertices, normals, uvs, faces):
    """Writes standard Wavefront OBJ file."""
    with open(filepath, 'w') as f:
        f.write(f"# Wavefront OBJ File - Apex 3D\n")
        f.write(f"# Object Name: {name}\n")
        f.write(f"o {name}\n\n")
        
        for v in vertices:
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

def get_wireframe_edges(faces):
    """Extracts unique wireframe edge pairs."""
    edges = set()
    for face in faces:
        for i in range(len(face)):
            v1 = face[i]
            v2 = face[(i + 1) % len(face)]
            edge = tuple(sorted([v1, v2]))
            edges.add(edge)
    return [list(e) for e in sorted(edges)]

def generate_ship():
    """Generates vertices, normals, uvs, and faces for a futuristic sci-fi starfighter."""
    v = [
        # Fuselage
        [0.0, 0.4, 2.5],     # 0: nose tip
        [-0.4, 0.2, 1.2],    # 1: cockpit left front
        [0.4, 0.2, 1.2],     # 2: cockpit right front
        [0.0, 0.7, 0.8],     # 3: cockpit canopy top
        [-0.7, 0.0, -0.8],   # 4: body left mid
        [0.7, 0.0, -0.8],    # 5: body right mid
        [0.0, 0.4, -1.8],    # 6: dorsal spine rear
        [0.0, -0.3, 0.5],    # 7: belly forward
        [0.0, -0.2, -1.6],   # 8: belly aft
        
        # Wings Left
        [-1.8, 0.1, -1.0],   # 9: wing left inner bend
        [-3.2, 0.3, -1.6],   # 10: wing left tip
        [-2.8, -0.1, -2.0],  # 11: wing left rear tip
        
        # Wings Right
        [1.8, 0.1, -1.0],    # 12: wing right inner bend
        [3.2, 0.3, -1.6],    # 13: wing right tip
        [2.8, -0.1, -2.0],   # 14: wing right rear tip
        
        # Twin Plasma Thrusters (Aft)
        [-0.5, 0.1, -2.2],   # 15: engine left upper
        [-0.5, -0.2, -2.2],  # 16: engine left lower
        [0.5, 0.1, -2.2],    # 17: engine right upper
        [0.5, -0.2, -2.2],   # 18: engine right lower
        
        # Vertical Stabilizers
        [-0.6, 1.2, -1.8],   # 19: left fin tip
        [0.6, 1.2, -1.8]     # 20: right fin tip
    ]
    
    faces = [
        # Cockpit & Nose
        [0, 3, 1], [0, 2, 3],
        [0, 1, 7], [0, 7, 2],
        [3, 6, 1], [3, 2, 6],
        
        # Main Fuselage
        [1, 6, 4], [2, 5, 6],
        [1, 4, 7], [2, 7, 5],
        [7, 4, 8], [7, 8, 5],
        [4, 6, 8], [5, 8, 6],
        
        # Left Wing
        [4, 9, 6], [9, 10, 6],
        [10, 11, 6], [4, 8, 9],
        [9, 8, 11],
        
        # Right Wing
        [5, 6, 12], [12, 6, 13],
        [13, 6, 14], [5, 12, 8],
        [12, 11 if False else 14, 8],
        
        # Engines
        [6, 15, 8], [6, 8, 17],
        [15, 16, 8], [17, 8, 18],
        
        # Vertical Fins
        [6, 19, 4], [6, 5, 20],
        [19, 15, 6], [20, 6, 17]
    ]
    
    # Calculate normals
    normals = []
    uvs = []
    for pt in v:
        # Pseudo spherical/planar normal and UV
        length = math.sqrt(pt[0]**2 + pt[1]**2 + pt[2]**2) or 1.0
        normals.append([pt[0]/length, pt[1]/length, pt[2]/length])
        u = (pt[0] + 3.5) / 7.0
        v_coord = (pt[2] + 2.5) / 5.0
        uvs.append([u, v_coord])
        
    return v, normals, uvs, faces

def generate_drone():
    """Generates a futuristic quad-recon drone."""
    v = [
        # Central Hub
        [0.0, 0.25, 0.0],    # 0: top dome center
        [-0.5, 0.1, -0.5],   # 1: hub top FL
        [0.5, 0.1, -0.5],    # 2: hub top FR
        [0.5, 0.1, 0.5],     # 3: hub top RR
        [-0.5, 0.1, 0.5],    # 4: hub top RL
        
        [0.0, -0.3, 0.0],    # 5: bottom sensor gimbal
        [-0.4, -0.1, -0.4],  # 6: hub btm FL
        [0.4, -0.1, -0.4],   # 7: hub btm FR
        [0.4, -0.1, 0.4],    # 8: hub btm RR
        [-0.4, -0.1, 0.4],   # 9: hub btm RL
        
        # Rotor Arms & Pods
        [-1.8, 0.15, -1.8],  # 10: rotor FL center
        [1.8, 0.15, -1.8],   # 11: rotor FR center
        [1.8, 0.15, 1.8],    # 12: rotor RR center
        [-1.8, 0.15, 1.8],   # 13: rotor RL center
        
        # Rotor guard outer rings
        [-2.4, 0.15, -1.8],  # 14
        [2.4, 0.15, -1.8],   # 15
        [2.4, 0.15, 1.8],    # 16
        [-2.4, 0.15, 1.8],   # 17
    ]
    
    faces = [
        # Top Dome
        [0, 1, 2], [0, 2, 3], [0, 3, 4], [0, 4, 1],
        # Sides
        [1, 6, 2], [2, 6, 7],
        [2, 7, 3], [3, 7, 8],
        [3, 8, 4], [4, 8, 9],
        [4, 9, 1], [1, 9, 6],
        # Bottom gimbal
        [5, 7, 6], [5, 8, 7], [5, 9, 8], [5, 6, 9],
        # Arms
        [1, 10, 6], [2, 7, 11], [3, 8, 12], [4, 13, 9],
        # Rotor Rings
        [10, 14, 1], [11, 2, 15], [12, 16, 3], [13, 4, 17]
    ]
    
    normals = []
    uvs = []
    for pt in v:
        length = math.sqrt(pt[0]**2 + pt[1]**2 + pt[2]**2) or 1.0
        normals.append([pt[0]/length, pt[1]/length, pt[2]/length])
        uvs.append([(pt[0] + 2.5)/5.0, (pt[2] + 2.5)/5.0])
        
    return v, normals, uvs, faces

def generate_rover():
    """Generates an interplanetary exploration rover."""
    v = [
        # Chassis
        [-0.8, 0.4, 1.2],    # 0: front top left
        [0.8, 0.4, 1.2],     # 1: front top right
        [0.8, 0.4, -1.4],    # 2: rear top right
        [-0.8, 0.4, -1.4],   # 3: rear top left
        
        [-0.7, -0.2, 1.1],   # 4: front btm left
        [0.7, -0.2, 1.1],    # 5: front btm right
        [0.7, -0.2, -1.3],   # 6: rear btm right
        [-0.7, -0.2, -1.3],  # 7: rear btm left
        
        # Mast & Sensor Camera
        [0.0, 1.3, 0.8],     # 8: sensor head top
        [-0.2, 0.9, 0.7],    # 9: mast base left
        [0.2, 0.9, 0.7],     # 10: mast base right
        
        # Wheels (Left 3, Right 3)
        [-1.3, -0.3, 1.0],   # 11: wheel front left
        [-1.3, -0.3, 0.0],   # 12: wheel mid left
        [-1.3, -0.3, -1.0],  # 13: wheel rear left
        
        [1.3, -0.3, 1.0],    # 14: wheel front right
        [1.3, -0.3, 0.0],    # 15: wheel mid right
        [1.3, -0.3, -1.0],   # 16: wheel rear right
        
        # Solar Deck
        [0.0, 0.55, -0.2]    # 17: solar panel peak
    ]
    
    faces = [
        # Body Top
        [0, 1, 17], [1, 2, 17], [2, 3, 17], [3, 0, 17],
        # Body Sides
        [0, 4, 1], [1, 4, 5],
        [1, 5, 2], [2, 5, 6],
        [2, 6, 3], [3, 6, 7],
        [3, 7, 0], [0, 7, 4],
        # Body Bottom
        [4, 6, 5], [4, 7, 6],
        # Mast
        [8, 9, 10], [9, 0, 10], [10, 0, 1],
        # Wheel mount struts
        [4, 11, 7], [7, 12, 6], [7, 13, 6],
        [5, 6, 14], [6, 15, 5], [6, 16, 5]
    ]
    
    normals = []
    uvs = []
    for pt in v:
        length = math.sqrt(pt[0]**2 + pt[1]**2 + pt[2]**2) or 1.0
        normals.append([pt[0]/length, pt[1]/length, pt[2]/length])
        uvs.append([(pt[0] + 1.5)/3.0, (pt[2] + 1.5)/3.0])
        
    return v, normals, uvs, faces

def generate_mech_core():
    """Generates a futuristic fusion power core with magnetic containment pillars."""
    v = [
        # Reactor Core Sphere (simplified icosahedron/octahedron)
        [0.0, 1.2, 0.0],     # 0: core top
        [0.0, -1.2, 0.0],    # 1: core bottom
        [-0.8, 0.0, 0.0],    # 2: core left
        [0.8, 0.0, 0.0],     # 3: core right
        [0.0, 0.0, 0.8],     # 4: core front
        [0.0, 0.0, -0.8],    # 5: core back
        
        # Containment Ring Pillars
        [-1.4, 1.4, -1.4],   # 6: pillar 1 top
        [-1.4, -1.4, -1.4],  # 7: pillar 1 btm
        [1.4, 1.4, -1.4],    # 8: pillar 2 top
        [1.4, -1.4, -1.4],   # 9: pillar 2 btm
        [1.4, 1.4, 1.4],     # 10: pillar 3 top
        [1.4, -1.4, 1.4],    # 11: pillar 3 btm
        [-1.4, 1.4, 1.4],    # 12: pillar 4 top
        [-1.4, -1.4, 1.4],   # 13: pillar 4 btm
        
        # Torus Ring nodes
        [-1.5, 0.0, 0.0],    # 14
        [1.5, 0.0, 0.0],     # 15
        [0.0, 0.0, 1.5],     # 16
        [0.0, 0.0, -1.5]     # 17
    ]
    
    faces = [
        # Core Octahedron
        [0, 2, 4], [0, 4, 3], [0, 3, 5], [0, 5, 2],
        [1, 4, 2], [1, 3, 4], [1, 5, 3], [1, 2, 5],
        # Pillars
        [6, 7, 2], [8, 9, 3], [10, 11, 4], [12, 13, 2],
        # Magnetic Field rings
        [14, 16, 4], [16, 15, 3], [15, 17, 5], [17, 14, 2],
        [6, 8, 0], [8, 10, 0], [10, 12, 0], [12, 6, 0],
        [7, 1, 9], [9, 1, 11], [11, 1, 13], [13, 1, 7]
    ]
    
    normals = []
    uvs = []
    for pt in v:
        length = math.sqrt(pt[0]**2 + pt[1]**2 + pt[2]**2) or 1.0
        normals.append([pt[0]/length, pt[1]/length, pt[2]/length])
        uvs.append([(pt[0] + 1.6)/3.2, (pt[2] + 1.6)/3.2])
        
    return v, normals, uvs, faces

def build_asset(name, gen_func, specs, metadata, root_dirs):
    """Builds a complete modular asset into target root directories."""
    vertices, normals, uvs, faces = gen_func()
    indices = []
    for f in faces:
        indices.extend(f)
        
    wireframe_edges = get_wireframe_edges(faces)
    
    for root_dir in root_dirs:
        asset_base = os.path.join(root_dir, name)
        tex_dir = os.path.join(asset_base, "textures")
        json_dir = os.path.join(asset_base, "json")
        mesh_dir = os.path.join(asset_base, "meshes")
        glb_dir = os.path.join(asset_base, "glb")
        
        for d in [tex_dir, json_dir, mesh_dir, glb_dir]:
            os.makedirs(d, exist_ok=True)
            
        # 1. Meshes
        obj_path = os.path.join(mesh_dir, f"{name}.obj")
        write_obj_file(obj_path, name, vertices, normals, uvs, faces)
        
        geom_path = os.path.join(mesh_dir, "geometry.json")
        with open(geom_path, "w") as f:
            json.dump({
                "name": name,
                "vertex_count": len(vertices),
                "face_count": len(faces),
                "edge_count": len(wireframe_edges),
                "vertices": vertices,
                "normals": normals,
                "uvs": uvs,
                "indices": indices,
                "faces": faces
            }, f, indent=2)
            
        wf_path = os.path.join(mesh_dir, "wireframe_edges.json")
        with open(wf_path, "w") as f:
            json.dump({
                "name": name,
                "edge_count": len(wireframe_edges),
                "edges": wireframe_edges
            }, f, indent=2)
            
        # 2. GLB Binary
        glb_path = os.path.join(glb_dir, f"{name}.glb")
        create_valid_glb(glb_path, vertices, normals, uvs, indices)
        
        # 3. JSON Specifications
        with open(os.path.join(json_dir, "metadata.json"), "w") as f:
            metadata_copy = dict(metadata)
            metadata_copy["vertex_count"] = len(vertices)
            metadata_copy["polygon_count"] = len(faces)
            metadata_copy["edge_count"] = len(wireframe_edges)
            json.dump(metadata_copy, f, indent=2)
            
        with open(os.path.join(json_dir, "specs.json"), "w") as f:
            json.dump(specs, f, indent=2)
            
        with open(os.path.join(json_dir, "physics.json"), "w") as f:
            json.dump({
                "mass_kg": metadata.get("mass_kg", 12500.0),
                "center_of_mass": [0.0, 0.0, 0.0],
                "bounding_box": {
                    "min": [min(v[0] for v in vertices), min(v[1] for v in vertices), min(v[2] for v in vertices)],
                    "max": [max(v[0] for v in vertices), max(v[1] for v in vertices), max(v[2] for v in vertices)]
                },
                "drag_coefficient": 0.24,
                "collider_type": "CONVEX_HULL_HYBRID"
            }, f, indent=2)
            
        with open(os.path.join(json_dir, "materials.json"), "w") as f:
            json.dump({
                "shader_model": "PBR_METALLIC_ROUGHNESS",
                "base_color_factor": [0.85, 0.9, 0.95, 1.0],
                "metallic_factor": 0.88,
                "roughness_factor": 0.32,
                "emissive_factor": [0.0, 0.85, 1.0],
                "emissive_strength": 4.5,
                "normal_scale": 1.2
            }, f, indent=2)
            
        # 4. Textures (Procedural PBR maps: Albedo, Normal, Roughness, Emissive)
        w, h = 128, 128
        
        # Albedo: Dark titanium plating with cyan tech panel circuits
        albedo_path = os.path.join(tex_dir, "albedo.png")
        create_png_image(albedo_path, w, h,
            lambda x, y, W, H: 28 + (15 if (x % 32 == 0 or y % 32 == 0) else (40 if (x+y)%24 == 0 else 0)),
            lambda x, y, W, H: 45 + (35 if (x % 32 == 0 or y % 32 == 0) else (120 if (x*y)%300 == 0 else 0)),
            lambda x, y, W, H: 65 + (80 if (x % 32 == 0 or y % 32 == 0) else (180 if (x*y)%300 == 0 else 0))
        )
        
        # Normal map: Flat #8080FF base with bevel grooves
        normal_path = os.path.join(tex_dir, "normal.png")
        create_png_image(normal_path, w, h,
            lambda x, y, W, H: 128 + (40 if x % 32 < 2 else (-40 if x % 32 > 30 else 0)),
            lambda x, y, W, H: 128 + (40 if y % 32 < 2 else (-40 if y % 32 > 30 else 0)),
            lambda x, y, W, H: 255
        )
        
        # Roughness map: Glossy grooves with matte armor plates
        roughness_path = os.path.join(tex_dir, "roughness.png")
        create_png_image(roughness_path, w, h,
            lambda x, y, W, H: 60 if (x % 32 == 0 or y % 32 == 0) else 180,
            lambda x, y, W, H: 60 if (x % 32 == 0 or y % 32 == 0) else 180,
            lambda x, y, W, H: 60 if (x % 32 == 0 or y % 32 == 0) else 180
        )
        
        # Emissive map: Glowing neon cyan accents and cockpit
        emissive_path = os.path.join(tex_dir, "emissive.png")
        create_png_image(emissive_path, w, h,
            lambda x, y, W, H: 0,
            lambda x, y, W, H: 220 if (x > 50 and x < 78 and y > 50 and y < 78) else (180 if (x % 32 == 0 and y % 8 == 0) else 0),
            lambda x, y, W, H: 255 if (x > 50 and x < 78 and y > 50 and y < 78) else (255 if (x % 32 == 0 and y % 8 == 0) else 0)
        )
        
        with open(os.path.join(tex_dir, "texture_manifest.json"), "w") as f:
            json.dump({
                "asset": name,
                "resolution": "128x128",
                "format": "PNG_RGBA",
                "channels": {
                    "albedo": "albedo.png (Base Color RGB)",
                    "normal": "normal.png (Tangent Space Normal XYZ)",
                    "roughness": "roughness.png (Micro-surface Greyscale)",
                    "emissive": "emissive.png (High-Dynamic Range Glow)"
                }
            }, f, indent=2)

def main():
    # Root targets:
    # 1. assets/ (top-level modular folder)
    # 2. asserts/ (top-level alias for user requirement)
    # 3. app/src/main/assets/ (runtime assets loaded by Android App)
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    targets = [
        os.path.join(base_dir, "assets"),
        os.path.join(base_dir, "asserts"),
        os.path.join(base_dir, "app", "src", "main", "assets")
    ]
    
    print("Generating modular 3D assets...")
    
    # 1. Starfighter Ship
    build_asset(
        name="ship",
        gen_func=generate_ship,
        specs={
            "model_code": "SF-94X VALKYRIE",
            "class": "Interceptor / Tactical Strike",
            "propulsion": "Twin Pulse-Detonation Ion Drives (Thrust: 480 kN)",
            "top_velocity": "Mach 12.8 Sub-Orbital / 0.18c Deep Space",
            "shielding": "Multi-Phase Harmonic Deflector Matrix (Class IV)",
            "power_plant": "Quantum Micro-Singularity Core (9.4 GW)",
            "avionics": "Neural-Linked Fly-By-Light HUD & Holographic Reticle",
            "hardpoints": [
                {"mount": "Wing Tip Alpha", "weapon": "Dual Heavy Plasma Repeaters"},
                {"mount": "Wing Tip Beta", "weapon": "Dual Heavy Plasma Repeaters"},
                {"mount": "Ventral Bay", "weapon": "Proton Torpedo Launcher x4"}
            ]
        },
        metadata={
            "id": "asset_ship_01",
            "title": "Valkyrie Interceptor",
            "category": "Spacecraft",
            "mass_kg": 8500.0,
            "lod": "LOD0 High-Fidelity",
            "author": "Apex 3D Aerospace Lab",
            "version": "2.4.0",
            "tags": ["spacecraft", "starfighter", "sci-fi", "interceptor", "aerospace"]
        },
        root_dirs=targets
    )
    
    # 2. Cyber Drone
    build_asset(
        name="drone",
        gen_func=generate_drone,
        specs={
            "model_code": "QR-77 SENTINEL",
            "class": "Autonomous Recon Quad-UAV",
            "propulsion": "Contra-Rotating Vector Ducted Turbines",
            "top_velocity": "240 km/h with 4.5G maneuver capability",
            "shielding": "Kinetic Energy Absorption Composite Plating",
            "power_plant": "Solid-State Graphene Lithium-Sulfur Array (18 kWh)",
            "avionics": "LiDAR 3D Spatial Mapping & Optical Multispectral Sensor Array",
            "payload": "Electronic Warfare Jammer & Telemetry Relay Pod"
        },
        metadata={
            "id": "asset_drone_02",
            "title": "Sentinel Recon Drone",
            "category": "Robotics / UAV",
            "mass_kg": 42.0,
            "lod": "LOD0 High-Fidelity",
            "author": "Apex Autonomous Labs",
            "version": "1.8.0",
            "tags": ["drone", "robotics", "recon", "autonomous", "quadcopter"]
        },
        root_dirs=targets
    )
    
    # 3. Quantum Rover
    build_asset(
        name="rover",
        gen_func=generate_rover,
        specs={
            "model_code": "EX-303 TITAN",
            "class": "Lunar & Martian Surface Heavy Explorer",
            "propulsion": "Independent 6x6 In-Wheel Brushless Flux Motors",
            "top_velocity": "45 km/h over 40-degree rough regolith terrain",
            "shielding": "Radiation Shielded Aerogel & Titanium Skin",
            "power_plant": "Radioisotope Thermoelectric Generator (RTG) + Solar Deck",
            "avionics": "Autonomous Hazard Avoidance & Ground-Penetrating Radar",
            "payload": "Sample Extraction Robotic Arm & Analytical Mass Spectrometer"
        },
        metadata={
            "id": "asset_rover_03",
            "title": "Titan Surface Rover",
            "category": "Planetary Surface Vehicle",
            "mass_kg": 2400.0,
            "lod": "LOD0 High-Fidelity",
            "author": "Apex Planetary Engineering",
            "version": "3.1.2",
            "tags": ["rover", "planetary", "space", "exploration", "vehicle"]
        },
        root_dirs=targets
    )
    
    # 4. Mech Core
    build_asset(
        name="mech_core",
        gen_func=generate_mech_core,
        specs={
            "model_code": "FC-9000 HYPERION",
            "class": "Compact Toroidal Magnetic Confinement Fusion Cell",
            "output": "18.5 Terawatts Peak Sustained Power",
            "containment": "Quad-Pillar Superconducting Magnetic Solenoids",
            "fuel_type": "Deuterium-Helium 3 Catalyzed Pellet",
            "operating_temp": "150 Million Kelvin Core Plasma",
            "safety_system": "Automatic Cryogenic Quench & Emergency Magnetic Dump"
        },
        metadata={
            "id": "asset_mech_core_04",
            "title": "Hyperion Fusion Core",
            "category": "Energy & Power Systems",
            "mass_kg": 6800.0,
            "lod": "LOD0 High-Fidelity",
            "author": "Apex Advanced Energy Div",
            "version": "4.0.0",
            "tags": ["power", "reactor", "fusion", "sci-fi", "mech"]
        },
        root_dirs=targets
    )
    
    # Generate catalog manifest
    catalog = {
        "catalog_version": "1.0",
        "timestamp": "2026-10-02T21:19:00Z",
        "assets": [
            {
                "id": "ship",
                "name": "Valkyrie Starfighter",
                "folder": "ship",
                "category": "Spacecraft",
                "icon_desc": "Sci-Fi Interceptor",
                "primary_color": "#00F0FF",
                "accent_color": "#7000FF"
            },
            {
                "id": "drone",
                "name": "Sentinel Recon Quad",
                "folder": "drone",
                "category": "Robotics",
                "icon_desc": "Autonomous Drone",
                "primary_color": "#39FF14",
                "accent_color": "#00E5FF"
            },
            {
                "id": "rover",
                "name": "Titan Planetary Rover",
                "folder": "rover",
                "category": "Surface Vehicle",
                "icon_desc": "6x6 Lunar Rover",
                "primary_color": "#FF9900",
                "accent_color": "#FF3366"
            },
            {
                "id": "mech_core",
                "name": "Hyperion Fusion Reactor",
                "folder": "mech_core",
                "category": "Energy Power Cell",
                "icon_desc": "Plasma Torus Core",
                "primary_color": "#BF55EC",
                "accent_color": "#00FFFF"
            }
        ]
    }
    
    for root_dir in targets:
        with open(os.path.join(root_dir, "asset_catalog.json"), "w") as f:
            json.dump(catalog, f, indent=2)
            
    print("Done generating all modular 3D assets!")

if __name__ == "__main__":
    main()
