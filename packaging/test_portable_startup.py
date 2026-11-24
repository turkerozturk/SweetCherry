import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('migrate', ROOT / 'packaging/migrate-saved-filters.py')
migrate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(migrate)


class PortableStartupTest(unittest.TestCase):
    def test_namespace_follows_relative_database_path_and_separates_user_tenant(self):
        with tempfile.TemporaryDirectory() as temp:
            old, new = Path(temp) / 'old', Path(temp) / 'new'
            first = migrate.scope('admin', 'Notes', old, 'jdbc:sqlite:CTBDATA/notes.ctb')
            self.assertEqual(first, '30980165516f655f581361331888ebeb999026de33a026960c348830c8429b81')
            self.assertEqual(first, migrate.scope('admin', 'Notes', new, 'jdbc:sqlite:' + str(new / 'CTBDATA/notes.ctb')))
            self.assertNotEqual(first, migrate.scope('reader', 'Notes', new, 'jdbc:sqlite:CTBDATA/notes.ctb'))
            self.assertNotEqual(first, migrate.scope('admin', 'Other', new, 'jdbc:sqlite:CTBDATA/notes.ctb'))
            self.assertNotEqual(first, migrate.scope('admin', 'Notes', new, 'jdbc:sqlite:CTBDATA/other.ctb'))

    def test_explicit_import_preserves_original_and_refuses_overwrite(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            source = root / 'old.json'
            data = b'{"version":1,"presets":[]}'
            source.write_bytes(data)
            target = migrate.migrate(source, root / 'install', 'admin', 'Notes', 'jdbc:sqlite:CTBDATA/notes.ctb')
            self.assertEqual(source.read_bytes(), data)
            self.assertEqual(target.read_bytes(), data)
            self.assertEqual(migrate.migrate(source, root / 'install', 'admin', 'Notes', 'jdbc:sqlite:CTBDATA/notes.ctb'), target)
            source.write_text('{"version":1,"presets":[{}]}')
            with self.assertRaises(ValueError):
                migrate.migrate(source, root / 'install', 'admin', 'Notes', 'jdbc:sqlite:CTBDATA/notes.ctb')
            self.assertEqual(target.read_bytes(), data)

    def test_bad_import_cannot_create_destination(self):
        with tempfile.TemporaryDirectory() as temp:
            source = Path(temp) / 'bad.json'
            for data in ('invalid', '{"version":9,"presets":[]}', 'x' * 1_048_577):
                source.write_text(data)
                with self.assertRaises(ValueError):
                    migrate.migrate(source, Path(temp) / 'out', 'admin', 'Notes', 'jdbc:sqlite:db.ctb')
            self.assertFalse((Path(temp) / 'out').exists())

    def test_launcher_uses_java17_fallback_without_port_overrides(self):
        config = ET.parse(ROOT / 'packaging/windows/launch4j.xml').getroot()
        self.assertEqual(config.findtext('jre/path'), 'runtime;%JAVA_HOME%;%PATH%')
        self.assertEqual(config.findtext('jre/minVersion'), '17')
        self.assertEqual(config.findtext('jre/requires64Bit'), 'true')
        self.assertFalse(any('server.port=' in opt.text or 'server.http.port=' in opt.text for opt in config.findall('jre/opt')))

    @unittest.skipUnless(shutil.which('sh') and os.name != 'nt', 'POSIX script test')
    def test_run_script_uses_adjacent_java_and_forwards_arguments_without_ports(self):
        with tempfile.TemporaryDirectory(prefix='Sweet Cherry ') as temp:
            root = Path(temp)
            shutil.copyfile(ROOT / 'external/run.sh', root / 'run.sh')
            (root / 'SweetCherry.jar').touch()
            java = root / 'runtime/bin/java'
            java.parent.mkdir(parents=True)
            java.write_text('#!/bin/sh\nprintf "%s\\n" "$@" > arguments.txt\npwd > working-directory.txt\n')
            java.chmod(0o755)
            subprocess.run(['sh', str(root / 'run.sh'), '--custom=value with spaces'], check=True, cwd='/tmp', stdout=subprocess.PIPE)
            args = (root / 'arguments.txt').read_text().splitlines()
            self.assertEqual(args, ['-jar', 'SweetCherry.jar', '--myapp.openWebBrowserOnStartup=true', '--custom=value with spaces'])
            self.assertEqual((root / 'working-directory.txt').read_text().strip(), str(root))

    def test_package_creates_personal_directories_without_copying_private_data(self):
        ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
        pom = ET.parse(ROOT / 'pom.xml')
        directories = {item.get('dir') for item in pom.findall('.//m:mkdir', ns)}
        for name in ('saved-filters', 'backup'):
            self.assertIn('${project.basedir}/release/SweetCherry/' + name, directories)
        bundle = (ROOT / 'packaging/windows/build-bundle.ps1').read_text()
        self.assertIn("@('saved-filters','backup')", bundle)
        self.assertIn('Do not package private data', bundle)


if __name__ == '__main__':
    unittest.main()
