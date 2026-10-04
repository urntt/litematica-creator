import importlib.util
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).parents[1]))
SPEC = importlib.util.spec_from_file_location("release", Path(__file__).parents[1] / "release.py")
release = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(release)

CHANGELOG = """# Changelog

## [Unreleased]

### Added / 新增

- Upcoming change.

## [0.2.0+26.3] - 2026-11-01

### Fixed / 修复

- Released fix.

## [0.1.0+26.3]

- Undated section.
"""


class ReleaseCheckTest(unittest.TestCase):
    def properties(self, version):
        return {"mod_version": version, "minecraft_version": "26.3"}

    def test_accepts_semver_with_minecraft_build_metadata(self):
        self.assertFalse(release.check_version("0.1.0+26.3", "26.3"))
        self.assertTrue(release.check_version("0.1.0-dev+26.3", "26.3"))
        self.assertTrue(release.check_version("1.2.3-rc.1+26.3", "26.3"))

    def test_rejects_missing_or_wrong_metadata_and_invalid_semver(self):
        for version in ("0.1.0", "0.1.0+26.2", "0.1+26.3", "01.0.0+26.3", "0.1.0-+26.3", "v0.1.0+26.3"):
            with self.assertRaises(ValueError, msg=version):
                release.check_version(version, "26.3")

    def test_tag_prefixes_the_full_version(self):
        self.assertEqual("v0.1.0+26.3", release.tag_for("0.1.0+26.3"))

    def test_extracts_one_section_until_the_next_heading(self):
        notes = release.changelog_section(CHANGELOG, "0.2.0+26.3", dated=True)
        self.assertEqual("### Fixed / 修复\n\n- Released fix.", notes)
        self.assertIn("Upcoming change", release.changelog_section(CHANGELOG, "Unreleased", dated=False))
        self.assertIsNone(release.changelog_section(CHANGELOG, "9.9.9+26.3", dated=True))

    def test_released_sections_need_a_date(self):
        with self.assertRaisesRegex(ValueError, "release date"):
            release.changelog_section(CHANGELOG, "0.1.0+26.3", dated=True)

    def test_prepare_returns_release_metadata(self):
        result = release.prepare(self.properties("0.2.0+26.3"), CHANGELOG, "push", "v0.2.0+26.3", False)
        self.assertEqual("v0.2.0+26.3", result["tag"])
        self.assertFalse(result["prerelease"])
        self.assertIn("Released fix", result["notes"])
        self.assertEqual([], result["warnings"])

    def test_pushed_tag_must_match_the_version(self):
        with self.assertRaisesRegex(ValueError, "does not match"):
            release.prepare(self.properties("0.2.0+26.3"), CHANGELOG, "push", "v0.2.1+26.3", False)

    def test_publishing_requires_a_released_section(self):
        with self.assertRaisesRegex(ValueError, "no released"):
            release.prepare(self.properties("0.3.0+26.3"), CHANGELOG, "workflow_dispatch", "main", False)

    def test_dry_run_previews_unreleased_notes(self):
        result = release.prepare(self.properties("0.3.0-dev+26.3"), CHANGELOG, "workflow_dispatch", "main", True)
        self.assertTrue(result["prerelease"])
        self.assertIn("Upcoming change", result["notes"])
        self.assertEqual(1, len(result["warnings"]))


if __name__ == "__main__":
    unittest.main()
