import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location("creator_build", Path(__file__).parents[1] / "build.py")
build = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(build)


class BuildPreparationTest(unittest.TestCase):
    def setUp(self):
        self.properties = build.read_properties(build.ROOT / "gradle.properties")

    def test_pins_are_full_commits_for_both_profiles(self):
        for profile in build.PROFILES:
            for mod in build.MODS:
                url, commit, version = build.source_spec(self.properties, profile, mod)
                self.assertEqual(url, f"https://github.com/sakura-ryoko/{mod}.git")
                self.assertEqual(len(commit), 40)
                self.assertTrue(version)

    def test_profiles_select_distinct_sources_and_versions(self):
        for mod in build.MODS:
            current = build.source_spec(self.properties, "current", mod)
            legacy = build.source_spec(self.properties, "legacy", mod)
            self.assertNotEqual(current[1:], legacy[1:])

    def test_rejects_floating_revisions(self):
        self.properties["malilib_source_commit"] = "LTS/26.2"
        with self.assertRaisesRegex(ValueError, "full commit"):
            build.source_spec(self.properties, "current", "malilib")

    def test_rejects_unknown_profile_and_mod(self):
        for profile, mod in (("other", "malilib"), ("current", "unknown")):
            with self.assertRaises(ValueError):
                build.source_spec(self.properties, profile, mod)

    def test_rejects_non_https_source(self):
        self.properties["malilib_source_url"] = "file:///some/local/repo.git"
        with self.assertRaisesRegex(ValueError, "HTTPS"):
            build.source_spec(self.properties, "current", "malilib")

    def test_properties_preserve_ranges_and_ignore_comments(self):
        self.assertEqual(self.properties["malilib_version_range"], ">=0.29.2- <0.29.5-")
        self.assertNotIn("# Official", self.properties)

    def test_refuses_existing_non_git_directory(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / ".dependencies/current/malilib").mkdir(parents=True)
            with self.assertRaisesRegex(ValueError, "Not a prepared"):
                build.prepare_source(root, self.properties, "current", "malilib")

    def test_refuses_foreign_or_modified_sources_without_fetching(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / ".dependencies/current/malilib/.git").mkdir(parents=True)
            for responses in (("https://github.com/other/mod.git",),
                              (self.properties["malilib_source_url"], " M build.gradle")):
                with patch.object(build, "git", side_effect=responses) as git:
                    with self.assertRaisesRegex(ValueError, "refusing to overwrite"):
                        build.prepare_source(root, self.properties, "current", "malilib")
                    self.assertFalse(any("fetch" in call.args for call in git.call_args_list))


if __name__ == "__main__":
    unittest.main()
