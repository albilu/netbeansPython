##Little helper for Netbeans to handle toml files

import sys
from pathlib import Path

import tomlkit

input_path = sys.argv[1]
project_name = sys.argv[2]
project_version = sys.argv[3]
project_description = sys.argv[4]
project_requires_python = sys.argv[5]
type = sys.argv[6]

file = Path(input_path)

try:
    pyproject = tomlkit.loads(file.read_text())
    with file.open("w") as f:
        if type != "poetry":
            pyproject["project"]["name"] = project_name
            pyproject["project"]["version"] = project_version
            pyproject["project"]["description"] = project_description
            pyproject["project"]["requires-python"] = project_requires_python
        else:
            #pyproject["tool"]["poetry"]["name"] = project_name
            pyproject["tool"]["poetry"]["version"] = project_version
            pyproject["tool"]["poetry"]["description"] = project_description
            pyproject["tool"]["poetry"]["dependencies"][
                "python"
            ] = project_requires_python

        f.write(tomlkit.dumps(pyproject))
        sys.exit()
except Exception as e:
    print("Error updating pyproject.toml: %s" % e)
