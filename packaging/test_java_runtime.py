import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('java_policy', Path(__file__).with_name('verify-java-runtime.py'))
policy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(policy)


class JavaRuntimeTest(unittest.TestCase):
    def test_patched_branches_and_later_updates(self):
        for version in ['17.0.21.1', '17.0.22', '21.0.13.1+2', '25.0.5.1', '27.0.1.1']:
            with self.subTest(version=version):
                self.assertEqual(policy.verify('    java.version = ' + version + '\n'), version)

    def test_old_major_or_patch_and_unverified_previews_are_rejected(self):
        for version in ['17.0.21', '21.0.13', '25.0.4.1', '27.0.1', '11.0.33', '26', '17.0.21.1-ea', 'unknown']:
            with self.subTest(version=version), self.assertRaises(ValueError):
                policy.verify('java.version = ' + version)
        with self.assertRaises(ValueError):
            policy.verify('no runtime properties')
