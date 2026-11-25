import functools
import hashlib
import http.server
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import threading
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]


@unittest.skipIf(os.name == 'nt', 'POSIX wrapper; Windows wrapper is exercised by CI packaging')
class MavenWrapperTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='SweetCherry wrapper space ')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.project = self.root / 'project'
        (self.project / '.mvn/wrapper').mkdir(parents=True)
        shutil.copy2(ROOT / 'mvnw', self.project / 'mvnw')
        self.archive = self.root / 'apache-maven-3.9.16-bin.zip'
        with zipfile.ZipFile(self.archive, 'w') as archive:
            entry = zipfile.ZipInfo('apache-maven-3.9.16/bin/mvn')
            entry.external_attr = 0o755 << 16
            archive.writestr(entry, '#!/bin/sh\nprintf "MAVEN_ARGUMENT:%s\\n" "$@"\nexit 7\n')
        class Handler(http.server.SimpleHTTPRequestHandler):
            def log_message(self, *args): pass
        self.server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), functools.partial(Handler, directory=self.temp.name))
        threading.Thread(target=self.server.serve_forever, daemon=True).start()
        self.addCleanup(self.server.server_close)
        self.addCleanup(self.server.shutdown)
        self.url = 'http://127.0.0.1:' + str(self.server.server_port) + '/' + self.archive.name
        self.env = dict(os.environ, MAVEN_USER_HOME=str(self.root / 'fresh cache'),
                        MAVEN_SKIP_RC='true', no_proxy='127.0.0.1', NO_PROXY='127.0.0.1')

    def properties(self, checksum, newline='\n'):
        (self.project / '.mvn/wrapper/maven-wrapper.properties').write_bytes(newline.join([
            'wrapperVersion=3.3.4', 'distributionType=only-script',
            'distributionUrl=' + self.url, 'distributionSha256Sum=' + checksum, '']).encode())

    def invoke(self):
        return subprocess.run(['sh', './mvnw', '--batch-mode', 'goal with spaces'],
                              cwd=self.project, env=self.env, capture_output=True, text=True, timeout=20)

    def test_fresh_bootstrap_and_cached_run_need_no_wrapper_jar_and_keep_exit_code(self):
        self.properties(hashlib.sha256(self.archive.read_bytes()).hexdigest())
        result = self.invoke()
        self.assertEqual(result.returncode, 7, result.stderr)
        self.assertIn('MAVEN_ARGUMENT:goal with spaces', result.stdout)
        self.assertFalse((self.project / '.mvn/wrapper/maven-wrapper.jar').exists())
        self.archive.unlink()  # Cached second invocation must not need the download.
        self.assertEqual(self.invoke().returncode, 7)

    def test_crlf_properties_bootstrap_on_linux(self):
        self.properties(hashlib.sha256(self.archive.read_bytes()).hexdigest(), '\r\n')
        self.assertEqual(self.invoke().returncode, 7)

    def test_corrupt_download_is_rejected_before_execution(self):
        self.properties('0' * 64)
        result = self.invoke()
        self.assertNotEqual(result.returncode, 0)
        self.assertNotEqual(result.returncode, 7)
        self.assertNotIn('MAVEN_ARGUMENT:', result.stdout)
        self.assertIn('SHA-256', result.stderr + result.stdout)
