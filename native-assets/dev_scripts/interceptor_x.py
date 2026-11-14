#!/usr/bin/env python3
"""
Convenience launcher for interceptor_x.py from dev_scripts/
"""
import os
import sys

parent_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
root_script = os.path.join(parent_dir, "interceptor_x.py")

if os.path.isfile(root_script):
    with open(root_script, "r") as f:
        code = f.read()
    exec(compile(code, root_script, 'exec'))
else:
    print(f"Error: {root_script} not found!")
    sys.exit(1)
