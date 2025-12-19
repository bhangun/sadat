
import os

root_dir = "src/main/java"

def get_package(path):
    rel_path = os.path.relpath(os.path.dirname(path), root_dir)
    return rel_path.replace(os.sep, ".")

def fix_file(path):
    pkg_name = get_package(path)
    with open(path, 'r') as f:
        content = f.read()

    lines = content.splitlines()
    new_lines = []
    
    # Check if package exists
    has_package = any(line.strip().startswith("package ") for line in lines)
    
    if not has_package:
        new_lines.append(f"package {pkg_name};")
        new_lines.append("")
        new_lines.append("import java.util.Map;")
        new_lines.append("import java.util.List;")
        new_lines.append("import java.util.Optional;")
        new_lines.append("import java.util.UUID;")
        new_lines.append("")

    for line in lines:
        # Fix imports
        if line.strip().startswith("import com.agentic.platform.sdk"):
            line = line.replace("com.agentic.platform.sdk", "tech.kayys.wayang.sdk")
        
        new_lines.append(line)

    with open(path, 'w') as f:
        f.write("\n".join(new_lines) + "\n")
    print(f"Fixed {path}")

for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(".java"):
            fix_file(os.path.join(root, file))
