#!/usr/bin/env python3
"""Root convenience runner for procedural rocks generator."""
import os
import sys

script_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "dev_scripts", "generate_rocks.py")
if os.path.isfile(script_path):
    with open(script_path, "r") as f:
        code = f.read()
    exec(compile(code, script_path, 'exec'))
else:
    print(f"Error: {script_path} not found!")
    sys.exit(1)
