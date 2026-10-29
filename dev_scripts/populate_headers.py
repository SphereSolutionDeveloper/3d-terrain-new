import os

count = 0
for root, dirs, files in os.walk('native-assets'):
    for f in files:
        p = os.path.join(root, f)
        ext = os.path.splitext(f)[1].lower()
        rel = os.path.relpath(p, 'native-assets')

        if ext in ['.h', '.hpp', '.c', '.cpp', '.comp', '.vert', '.frag', '.glsl']:
            content = f"// Procedural Vulkan Asset Suite\n// Path: {rel}\n\n#pragma once\n" if ext in ['.h', '.hpp'] else f"// Procedural Vulkan Asset Suite\n// Path: {rel}\n"
        elif ext in ['.md']:
            content = f"# {f}\n\nProcedural asset module specification for `{rel}`.\n"
        elif ext in ['.bin', '.r8', '.sdf', '.lsys', '.txt']:
            content = f"# Binary/Data Asset Specification\n# Path: {rel}\n"
        elif f == 'CMakeLists.txt' or ext in ['.cmake']:
            content = f"# CMake build script for {rel}\n"
        elif f in ['.clang-format', '.gitignore']:
            content = f"# Configuration file: {rel}\n"
        else:
            content = f"// Procedural Asset: {rel}\n"

        with open(p, 'w') as out:
            out.write(content)
        count += 1

print(f"Successfully populated {count} files in native-assets with descriptive comments.")
