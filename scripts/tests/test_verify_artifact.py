import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).parents[1]))
SPEC = importlib.util.spec_from_file_location("verify_artifact", Path(__file__).parents[1] / "verify_artifact.py")
artifact = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(artifact)


class ArtifactVerificationTest(unittest.TestCase):
    def setUp(self):
        self.properties = artifact.read_properties(artifact.ROOT / "gradle.properties")
        self.license = (artifact.ROOT / "LICENSE").read_text(encoding="utf-8")
        self.entries = {
            "fabric.mod.json": json.dumps({
                "id": self.properties["mod_id"], "version": self.properties["mod_version"],
                "environment": "client", "license": "MIT", "depends": {
                    "minecraft": f"~{self.properties['minecraft_version']}-",
                    "fabricloader": f">={self.properties['fabric_loader_version']}",
                    "java": f">={self.properties['java_version']}",
                    "malilib": self.properties["malilib_version_range"],
                    "litematica": self.properties["litematica_version_range"],
                }}),
            "mixins.litematica_creator.json": json.dumps({
                "compatibilityLevel": f"JAVA_{self.properties['java_version']}"}),
            f"LICENSE_{self.properties['mod_id']}": self.license,
            "assets/litematica-creator/lang/en_us.json": '{"test": "English"}',
            "assets/litematica-creator/lang/zh_cn.json": '{"test": "Chinese"}',
            "io/github/urntt/litematicacreator/LitematicaCreator.class": b"test fixture",
        }

    def verify(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "mod.jar"
            with zipfile.ZipFile(path, "w") as archive:
                for name, content in self.entries.items():
                    archive.writestr(name, content)
            return artifact.verify(path, self.properties, self.license)

    def test_valid_production_jar(self):
        self.assertEqual(len(self.verify()), 64)

    def test_rejects_bundled_classes_nested_jars_and_test_code(self):
        for name in ("fi/dy/masa/litematica/Test.class", "net/fabricmc/fabric/Test.class",
                     "META-INF/jars/dependency.jar", "io/github/urntt/gametest/Test.class"):
            self.entries[name] = "bad"
            with self.assertRaisesRegex(ValueError, "Bundled"):
                self.verify()
            del self.entries[name]

    def test_rejects_wrong_mod_id_version_environment_or_dependencies(self):
        original = self.entries["fabric.mod.json"]
        for key in ("id", "version", "environment", "depends"):
            mod = json.loads(original)
            mod[key] = "incorrect"
            self.entries["fabric.mod.json"] = json.dumps(mod)
            with self.assertRaises(ValueError):
                self.verify()

    def test_rejects_missing_translation_and_changed_license(self):
        self.entries["assets/litematica-creator/lang/zh_cn.json"] = "{}"
        with self.assertRaisesRegex(ValueError, "translation"):
            self.verify()
        self.entries["assets/litematica-creator/lang/zh_cn.json"] = '{"test": "Chinese"}'
        self.entries[f"LICENSE_{self.properties['mod_id']}"] = "other license"
        with self.assertRaisesRegex(ValueError, "license"):
            self.verify()


if __name__ == "__main__":
    unittest.main()
