#!/usr/bin/env python3
"""Check a release candidate's version, tag and changelog section, and write its release notes."""

import argparse
import os
from pathlib import Path
import re
import sys

from build import ROOT, read_properties

# SemVer 2.0.0; build metadata must name the target Minecraft version.
SEMVER = re.compile(
    r"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)"
    r"(?:-(?P<prerelease>(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?"
    r"(?:\+(?P<build>[0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?")
HEADING = re.compile(r"^## \[(?P<name>[^\]]+)\](?: - (?P<date>\d{4}-\d{2}-\d{2}))?\s*$", re.MULTILINE)
UNRELEASED = "Unreleased"


def check_version(version, minecraft_version):
    """Validate the full mod version and return whether it is a prerelease."""
    match = SEMVER.fullmatch(version)
    if not match:
        raise ValueError(f"mod_version is not a SemVer version: {version}")
    if match.group("build") != minecraft_version:
        raise ValueError(f"mod_version must end with the Minecraft build metadata +{minecraft_version}: {version}")
    return match.group("prerelease") is not None


def tag_for(version):
    return f"v{version}"


def changelog_section(text, name, dated):
    """Return the body of the '## [name]' section, requiring a date for released versions."""
    headings = list(HEADING.finditer(text))
    for index, heading in enumerate(headings):
        if heading.group("name") != name:
            continue
        if dated and not heading.group("date"):
            raise ValueError(f"Changelog section [{name}] needs a ' - YYYY-MM-DD' release date")
        end = headings[index + 1].start() if index + 1 < len(headings) else len(text)
        return text[heading.end():end].strip()
    return None


def prepare(properties, changelog, event, ref_name, dry_run):
    """Return the release metadata and notes, or raise when the candidate must not be published."""
    version = properties["mod_version"]
    prerelease = check_version(version, properties["minecraft_version"])
    tag = tag_for(version)
    if event == "push" and ref_name != tag:
        raise ValueError(f"Tag {ref_name} does not match mod_version; expected {tag}")

    warnings = []
    notes = changelog_section(changelog, version, dated=True)
    if not notes:
        if not dry_run:
            raise ValueError(f"CHANGELOG.md has no released [{version}] section with notes")
        warnings.append(f"CHANGELOG.md has no [{version}] section yet; previewing the Unreleased notes")
        notes = changelog_section(changelog, UNRELEASED, dated=False) or "(no changelog entries)"
    return {"version": version, "tag": tag, "prerelease": prerelease, "notes": notes, "warnings": warnings}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("prepare",))
    parser.add_argument("--event", required=True, help="GitHub event name, such as push or workflow_dispatch")
    parser.add_argument("--ref-name", default="", help="Pushed tag name for push events")
    parser.add_argument("--dry-run", action="store_true", help="Preview without requiring a released section")
    parser.add_argument("--notes", type=Path, required=True, help="Where to write the release notes")
    args = parser.parse_args()

    release = prepare(
        read_properties(ROOT / "gradle.properties"),
        (ROOT / "CHANGELOG.md").read_text(encoding="utf-8"),
        args.event,
        args.ref_name,
        args.dry_run,
    )
    for warning in release["warnings"]:
        print(f"::warning::{warning}")
    args.notes.parent.mkdir(parents=True, exist_ok=True)
    args.notes.write_text(release["notes"] + "\n", encoding="utf-8")

    outputs = (f"version={release['version']}\n"
               f"tag={release['tag']}\n"
               f"prerelease={str(release['prerelease']).lower()}\n")
    output_path = os.environ.get("GITHUB_OUTPUT")
    if output_path:
        with open(output_path, "a", encoding="utf-8") as output:
            output.write(outputs)
    print(outputs, end="")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, KeyError) as error:
        print(f"Release check failed: {error}", file=sys.stderr)
        sys.exit(1)
