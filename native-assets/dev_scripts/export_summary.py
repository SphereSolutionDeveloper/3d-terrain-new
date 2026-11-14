#!/usr/bin/env python3
"""
3D Asset Repository Summary Generator
Prints and exports an overview of all 3D assets in the modular structure.
"""

import os
import json

def main():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    assets_dir = os.path.join(base_dir, "assets")
    
    print("\n=======================================================")
    print("      APEX 3D MODULAR ASSET REPOSITORY SUMMARY         ")
    print("=======================================================\n")
    
    catalog_file = os.path.join(assets_dir, "asset_catalog.json")
    if os.path.exists(catalog_file):
        with open(catalog_file, "r") as f:
            catalog = json.load(f)
            print(f"Catalog Version: {catalog.get('catalog_version')} | Total Assets: {len(catalog.get('assets', []))}\n")
            
    asset_folders = [d for d in os.listdir(assets_dir) if os.path.isdir(os.path.join(assets_dir, d))]
    
    for name in sorted(asset_folders):
        base = os.path.join(assets_dir, name)
        meta_file = os.path.join(base, "json", "metadata.json")
        specs_file = os.path.join(base, "json", "specs.json")
        
        meta = {}
        if os.path.exists(meta_file):
            with open(meta_file, "r") as f:
                meta = json.load(f)
                
        print(f" Asset: [{name.upper()}] - {meta.get('title', name)}")
        print(f"  Category: {meta.get('category')} | LOD: {meta.get('lod')}")
        print(f"  Geometry: {meta.get('vertex_count')} vertices | {meta.get('polygon_count')} polygons | {meta.get('edge_count')} edges")
        print(f"  Folders:")
        print(f"    - textures/  (albedo, normal, roughness, emissive)")
        print(f"    - json/      (metadata, specs, physics, materials)")
        print(f"    - meshes/    ({name}.obj, geometry.json, wireframe_edges.json)")
        print(f"    - glb/       ({name}.glb binary glTF container)")
        print("-------------------------------------------------------")

if __name__ == "__main__":
    main()
