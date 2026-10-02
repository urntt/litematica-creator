#!/usr/bin/env python3
"""Reject malformed, bundled, or incorrectly versioned production artifacts."""

import argparse
import hashlib
import json
from pathlib import Path
import sys
import zipfile

from build import ROOT, read_properties

MAX_JAR_BYTES = 5 * 1024 * 1024
FORBIDDEN_PREFIXES = ("fi/dy/masa/", "net/fabricmc/fabric/", "org/junit/")


def verify(path, properties, license_text):
    if not 0 < path.stat().st_size <= MAX_JAR_BYTES:
        raise ValueError("Production JAR size is outside the expected bounds")
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)):
            raise ValueError("Duplicate archive entries")
        forbidden = [name for name in names if name.startswith(FORBIDDEN_PREFIXES)
                     or name.endswith(".jar") or "/gametest/" in name]
        if forbidden:
            raise ValueError(f"Bundled dependency or test content: {forbidden[0]}")
        mod = json.loads(archive.read("fabric.mod.json"))
        expected = {"id": properties["mod_id"], "version": properties["mod_version"],
                    "environment": "client", "license": "MIT"}
        for key, value in expected.items():
            if mod.get(key) != value:
                raise ValueError(f"Incorrect mod metadata: {key}")
        dependencies = {
            "minecraft": f"~{properties['minecraft_version']}-",
            "fabricloader": f">={properties['fabric_loader_version']}",
            "java": f">={properties['java_version']}",
            "malilib": properties["malilib_version_range"],
            "litematica": properties["litematica_version_range"],
        }
        if mod.get("depends") != dependencies or mod.get("jars"):
            raise ValueError("Incorrect dependency ranges or embedded mods")
        mixins = json.loads(archive.read("mixins.litematica_creator.json"))
        if mixins.get("compatibilityLevel") != f"JAVA_{properties['java_version']}":
            raise ValueError("Incorrect Mixin Java compatibility")
        for resource in ("fabric.mod.json", "mixins.litematica_creator.json"):
            if b"${" in archive.read(resource):
                raise ValueError("Unexpanded resource placeholder")
        packaged_license = archive.read(f"LICENSE_{properties['mod_id']}").decode("utf-8")
        if packaged_license.replace("\r\n", "\n").strip() != license_text.replace("\r\n", "\n").strip():
            raise ValueError("Packaged license differs from LICENSE")
        prefix = "assets/litematica-creator/lang/"
        english = json.loads(archive.read(prefix + "en_us.json"))
        chinese = json.loads(archive.read(prefix + "zh_cn.json"))
        if english.keys() != chinese.keys():
            raise ValueError("English and Chinese translation keys differ")
        if "io/github/urntt/litematicacreator/LitematicaCreator.class" not in names:
            raise ValueError("Creator entrypoint is missing")
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    properties = read_properties(ROOT / "gradle.properties")
    default = ROOT / "build/libs" / (
        f"{properties['mod_file_name']}-{properties['minecraft_version']}-{properties['mod_version']}.jar")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path, nargs="?", default=default)
    args = parser.parse_args()
    digest = verify(args.jar, properties, (ROOT / "LICENSE").read_text(encoding="utf-8"))
    print(f"Verified {args.jar.name}: {args.jar.stat().st_size} bytes, SHA-256 {digest}")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, KeyError, zipfile.BadZipFile) as error:
        print(f"Artifact verification failed: {error}", file=sys.stderr)
        sys.exit(1)
