#!/usr/bin/env python3
"""Prepare pinned official source dependencies and invoke the tracked Gradle wrapper."""

import argparse
import os
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parent.parent
MODS = ("malilib", "litematica")
PROFILES = ("current", "legacy")


def read_properties(path):
    result = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith(("#", "!")):
            continue
        key, separator, value = line.partition("=")
        if not separator:
            raise ValueError(f"Expected key=value in {path}: {line}")
        result[key.strip()] = value.strip()
    return result


def source_spec(properties, profile, mod):
    if profile not in PROFILES or mod not in MODS:
        raise ValueError("Unknown dependency profile or mod")
    prefix = "legacy_" if profile == "legacy" else ""
    url = properties[f"{mod}_source_url"]
    commit = properties[f"{prefix}{mod}_source_commit"]
    version = properties[f"{prefix}{mod}_version"]
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError(f"{mod} must use a full commit SHA")
    if not url.startswith("https://github.com/") or not url.endswith(".git"):
        raise ValueError(f"{mod} must use an HTTPS GitHub upstream URL")
    return url, commit, version


def git(source, *args):
    command = ["git"]
    if os.name == "nt":
        command += ["-c", "http.sslBackend=openssl"]
    command += ["-c", "gc.auto=0", "-C", str(source), *args]
    return subprocess.check_output(command, text=True).strip()


def prepare_source(root, properties, profile, mod):
    url, commit, version = source_spec(properties, profile, mod)
    source = root / ".dependencies" / profile / mod
    if not source.exists():
        source.mkdir(parents=True)
        git(source, "init", "--quiet")
        git(source, "config", "core.autocrlf", "false")
        git(source, "remote", "add", "origin", url)
    if not (source / ".git").is_dir():
        raise ValueError(f"Not a prepared Git checkout: {source}")
    if git(source, "remote", "get-url", "origin") != url:
        raise ValueError(f"Unexpected upstream in {source}; refusing to overwrite it")
    if git(source, "status", "--porcelain", "--untracked-files=no"):
        raise ValueError(f"Modified upstream source in {source}; refusing to overwrite it")
    has_head = subprocess.run(
        ["git", "-C", str(source), "rev-parse", "--verify", "HEAD"],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    ).returncode == 0
    if has_head and git(source, "rev-parse", "HEAD") != commit:
        raise ValueError(f"Different source pin in {source}; move this cache aside before preparing the new pin")
    if not has_head:
        print(f"Fetching {mod} {version} at {commit}", flush=True)
        git(source, "fetch", "--depth=1", "origin", commit)
        git(source, "checkout", "--quiet", "--detach", commit)
    actual = read_properties(source / "gradle.properties")
    if actual.get("mod_version") != version or actual.get("minecraft_version") != properties["minecraft_version"]:
        raise ValueError(f"Upstream version mismatch in {source}")
    print(f"Verified {mod} {version}: {git(source, 'rev-parse', 'HEAD')}", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", choices=PROFILES, default="current")
    parser.add_argument("--prepare-only", action="store_true")
    args, gradle_args = parser.parse_known_args()
    properties = read_properties(ROOT / "gradle.properties")
    for mod in MODS:
        prepare_source(ROOT, properties, args.profile, mod)
    if args.prepare_only:
        if gradle_args:
            parser.error("--prepare-only does not accept Gradle arguments")
        return 0
    if any(arg.startswith(("-Pdependency_profile", "-Pmalilib_version", "-Plitematica_version")) for arg in gradle_args):
        parser.error("Use --profile to select verified dependency sources and versions")
    wrapper = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    command = [str(wrapper), "--init-script", str(ROOT / "scripts/upstream.init.gradle"),
               f"-Pdependency_profile={args.profile}", *(gradle_args or ["build"])]
    return subprocess.call(command, cwd=ROOT)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (ValueError, subprocess.CalledProcessError, OSError) as error:
        print(f"Dependency preparation failed: {error}", file=sys.stderr)
        sys.exit(1)
