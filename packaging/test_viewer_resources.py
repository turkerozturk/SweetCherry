"""Release-check regressions using a synthetic Boot archive made from the real source assets.
This checks the verifier, not a Maven/Windows build; the workflow checks the actual products separately.
"""
from pathlib import Path
import importlib.util, io, tempfile, shutil, unittest, zipfile
spec = importlib.util.spec_from_file_location('viewer_check', Path(__file__).with_name('verify-viewer-resources.py'))
check = importlib.util.module_from_spec(spec); spec.loader.exec_module(check)

class ViewerResourcesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.bundle = Path(cls.temp.name) / 'SweetCherry'; cls.bundle.mkdir()
        version, fa = check.version(check.ROOT)
        for name in (*check.DOC_FILES, f'documentation/releases/v{version}.md'):
            source = check.ROOT / (('distribution-notices/' if name.startswith(('licenses/', 'THIRD-PARTY')) else '') + name)
            destination = cls.bundle / name; destination.parent.mkdir(parents=True, exist_ok=True); shutil.copyfile(source, destination)
        for relative, source in check.BUNDLE_SOURCE_FILES.items():
            target = cls.bundle / relative; target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(check.ROOT / source, target)
        for name in ('saved-filters', 'backup'): (cls.bundle / name).mkdir()
        shutil.copytree(check.ROOT / 'external/drawio-images', cls.bundle / 'drawio-images')
        webjar = io.BytesIO()
        with zipfile.ZipFile(webjar, 'w') as nested:
            for name in ('css/all.min.css', 'webfonts/fa-solid-900.woff2', 'webfonts/fa-regular-400.woff2', 'webfonts/fa-brands-400.woff2', 'LICENSE.txt'):
                nested.writestr(f'META-INF/resources/webjars/font-awesome/{fa}/' + name, b'fixture')
        cls.entries = {'BOOT-INF/classes/' + p.relative_to(check.ROOT / 'src/main/resources').as_posix(): p.read_bytes() for p in check.resource_paths(check.ROOT)}
        cls.entries['META-INF/build-info.properties'] = f'build.version={version}\n'.encode()
        cls.entries[f'BOOT-INF/lib/font-awesome-{fa}.jar'] = webjar.getvalue()
        cls.write_jar(cls.entries)
    @classmethod
    def write_jar(cls, entries):
        with zipfile.ZipFile(cls.bundle / 'SweetCherry.jar', 'w') as jar:
            for name, data in entries.items(): jar.writestr(name, data)
    @classmethod
    def tearDownClass(cls): cls.temp.cleanup()
    def tearDown(self): self.write_jar(self.entries)
    def test_complete_offline_resource_tree_and_seed_files(self):
        self.assertEqual(check.verify(self.bundle, clean=True), len(check.resource_paths(check.ROOT)))
    def test_stale_application_version_is_rejected(self):
        entries = dict(self.entries); entries['META-INF/build-info.properties'] = b'build.version=0.0.0\n'; self.write_jar(entries)
        with self.assertRaisesRegex(check.Problem, 'Application JAR version'): check.verify(self.bundle)
    def test_classpath_prefixed_build_information_is_also_accepted(self):
        entries = dict(self.entries)
        entries['BOOT-INF/classes/META-INF/build-info.properties'] = entries.pop('META-INF/build-info.properties')
        self.write_jar(entries); check.verify(self.bundle, clean=True)
    def test_missing_build_information_is_rejected(self):
        entries = dict(self.entries); entries.pop('META-INF/build-info.properties'); self.write_jar(entries)
        with self.assertRaisesRegex(check.Problem, 'Missing/duplicate JAR build information'): check.verify(self.bundle)
    def test_ambiguous_build_information_locations_are_rejected(self):
        entries = dict(self.entries)
        entries['BOOT-INF/classes/META-INF/build-info.properties'] = entries['META-INF/build-info.properties']
        self.write_jar(entries)
        with self.assertRaisesRegex(check.Problem, 'Missing/duplicate JAR build information'): check.verify(self.bundle)
    def test_duplicate_metadata_zip_entries_are_rejected(self):
        import warnings
        with warnings.catch_warnings():
            warnings.simplefilter('ignore', UserWarning)
            with zipfile.ZipFile(self.bundle / 'SweetCherry.jar', 'a') as jar:
                jar.writestr('META-INF/build-info.properties', self.entries['META-INF/build-info.properties'])
        with self.assertRaisesRegex(check.Problem, 'Missing/duplicate JAR build information'): check.verify(self.bundle)
    def test_missing_shape_pack_is_rejected(self):
        entries = dict(self.entries); entries.pop('BOOT-INF/classes/static/js/drawio/stencils.min.js'); self.write_jar(entries)
        with self.assertRaisesRegex(check.Problem, 'Missing/duplicate JAR resource'): check.verify(self.bundle)
    def test_filtered_font_bytes_are_rejected(self):
        entries = dict(self.entries); entries['BOOT-INF/classes/symbols/fa-brands.ttf'] = b'corrupted'; self.write_jar(entries)
        with self.assertRaisesRegex(check.Problem, 'Changed JAR resource bytes: symbols/fa-brands.ttf'): check.verify(self.bundle)
    def test_private_filters_and_backups_are_preserved_locally_but_rejected_for_release(self):
        for name in ('saved-filters', 'backup'):
            private = self.bundle / name / 'private-file'
            try:
                private.write_text('must not ship')
                check.verify(self.bundle)
                with self.assertRaisesRegex(check.Problem, 'Private/linked'):
                    check.verify(self.bundle, clean=True)
            finally:
                private.unlink()

    def test_changed_migration_tool_is_not_accepted_in_distribution(self):
        target = self.bundle / 'tools/migrate-saved-filters.py'
        original = target.read_bytes()
        try:
            target.write_bytes(b'incomplete helper')
            with self.assertRaisesRegex(check.Problem, 'distribution helper'):
                check.verify(self.bundle)
        finally:
            target.write_bytes(original)

    def test_custom_manifest_is_allowed_locally_but_never_in_clean_release(self):
        manifest = self.bundle / 'drawio-images/images.properties'; original = manifest.read_bytes()
        try:
            manifest.write_text('shape.example=private.png', encoding='utf-8'); check.verify(self.bundle)
            with self.assertRaisesRegex(check.Problem, 'Edited Draw.io'): check.verify(self.bundle, clean=True)
        finally: manifest.write_bytes(original)
    def test_private_images_are_rejected_in_clean_release(self):
        image = self.bundle / 'drawio-images/private.png'
        try:
            image.write_bytes(b'private')
            with self.assertRaisesRegex(check.Problem, 'Custom/missing Draw.io'): check.verify(self.bundle, clean=True)
        finally: image.unlink()
    def test_omitted_external_examples_are_rejected(self):
        example = self.bundle / 'documentation/examples/drawio-library-shapes.drawio'; original = example.read_bytes()
        try:
            example.unlink()
            with self.assertRaisesRegex(check.Problem, 'Missing distribution file'): check.verify(self.bundle)
        finally: example.write_bytes(original)

if __name__ == '__main__': unittest.main()
