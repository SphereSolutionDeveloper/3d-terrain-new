#!/usr/bin/env python3
"""
3D Asset Validator Script
Validates the modular folder architecture under assets/ (or asserts/):
- Verifies subdirectories: textures/, json/, meshes/, glb/
- Checks JSON schemas and files (metadata.json, specs.json, physics.json, materials.json)
- Checks meshes (.obj, geometry.json, wireframe_edges.json)
- Checks GLB binary magic and length
- Checks texture maps (.png)
"""

import os
import sys
import json
import struct

def validate_dir(asset_path):
    print(f"\n--- Checking Asset: {os.path.basename(asset_path)} ---")
    required_subdirs = ["textures", "json", "meshes", "glb"]
    errors = []
    
    for sub in required_subdirs:
        sub_path = os.path.join(asset_path, sub)
        if not os.path.isdir(sub_path):
            errors.append(f"Missing required subdirectory: {sub}/")
        else:
            print(f"  [OK] Subdirectory exists: {sub}/")
            
    # Check JSON
    json_dir = os.path.join(asset_path, "json")
    if os.path.isdir(json_dir):
        for f in ["metadata.json", "specs.json", "physics.json", "materials.json"]:
            fp = os.path.join(json_dir, f)
            if not os.path.isfile(fp):
                errors.append(f"Missing JSON specification: {f}")
            else:
                try:
                    with open(fp, "r") as jf:
                        json.load(jf)
                    print(f"  [OK] Valid JSON: {f}")
                except Exception as e:
                    errors.append(f"Corrupt JSON in {f}: {e}")
                    
    # Check Meshes
    mesh_dir = os.path.join(asset_path, "meshes")
    if os.path.isdir(mesh_dir):
        obj_files = [f for f in os.listdir(mesh_dir) if f.endswith(".obj")]
        if not obj_files:
            errors.append("No .obj file found in meshes/")
        else:
            print(f"  [OK] Wavefront OBJ found: {obj_files[0]}")
            
        for f in ["geometry.json", "wireframe_edges.json"]:
            fp = os.path.join(mesh_dir, f)
            if not os.path.isfile(fp):
                errors.append(f"Missing mesh data: {f}")
            else:
                print(f"  [OK] Mesh data: {f}")
                
    # Check GLB
    glb_dir = os.path.join(asset_path, "glb")
    if os.path.isdir(glb_dir):
        glb_files = [f for f in os.listdir(glb_dir) if f.endswith(".glb")]
        if not glb_files:
            errors.append("No .glb file found in glb/")
        else:
            glb_path = os.path.join(glb_dir, glb_files[0])
            with open(glb_path, "rb") as gf:
                header = gf.read(12)
                if len(header) >= 12:
                    magic, version, length = struct.unpack("<4sII", header)
                    if magic == b'glTF' and version == 2:
                        print(f"  [OK] Valid binary GLTF 2.0 (.glb): {glb_files[0]} ({length} bytes)")
                    else:
                        errors.append(f"Invalid GLB header magic: {magic}")
                else:
                    errors.append("GLB file too short")
                    
    # Check Textures
    tex_dir = os.path.join(asset_path, "textures")
    if os.path.isdir(tex_dir):
        for f in ["albedo.png", "normal.png", "roughness.png", "emissive.png"]:
            fp = os.path.join(tex_dir, f)
            if os.path.isfile(fp):
                print(f"  [OK] Texture map: {f} ({os.path.getsize(fp)} bytes)")
            else:
                errors.append(f"Missing texture map: {f}")
                
    return errors

def main():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    root = sys.argv[1] if len(sys.argv) > 1 else os.path.join(base_dir, "assets")
    print(f"Validating modular 3D asset directory: {root}")
    if not os.path.isdir(root):
        print(f"Error: directory {root} does not exist!")
        sys.exit(1)
        
    all_errors = []
    assets = [os.path.join(root, d) for d in os.listdir(root) if os.path.isdir(os.path.join(root, d))]
    for asset in sorted(assets):
        errs = validate_dir(asset)
        all_errors.extend(errs)
        
    print("\n===============================")
    if not all_errors:
        print("ALL 3D ASSETS VALIDATED SUCCESSFULLY!")
    else:
        print(f"Validation found {len(all_errors)} issues:")
        for e in all_errors:
            print(f" - {e}")
            
if __name__ == "__main__":
    main()
