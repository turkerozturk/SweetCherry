"""Portable Linux launcher checks: python3 src/test/sh/first-start.test.py (no real Java server)."""
import os
import pathlib
import shutil
import subprocess
import tempfile
import time
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[3]


class FirstStartScriptsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="sweetcherry script test ")
        self.addCleanup(self.temporary.cleanup)
        self.root = pathlib.Path(self.temporary.name)
        self.app = self.root / 'App with spaces % and "quotes"'
        self.app.mkdir()
        self.home = self.root / "home"
        (self.home / "Desktop").mkdir(parents=True)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        for name in ("run.sh", "launcher.sh", "install-desktop.sh"):
            shutil.copyfile(ROOT / "external" / name, self.app / name)
        (self.app / "SweetCherry.jar").write_text("test only")
        java = self.bin / "java"
        java.write_text('#!/bin/sh\nprintf "%s\\n" "$PWD" "$@" > "$SC_TEST_OUTPUT"\n')
        java.chmod(0o755)
        self.env = dict(os.environ, HOME=str(self.home), XDG_DATA_HOME=str(self.home / "data"),
                        PATH=str(self.bin) + ":" + os.environ["PATH"], DISPLAY="", WAYLAND_DISPLAY="",
                        SC_TEST_OUTPUT=str(self.root / "arguments"))
        self.env.pop("XDG_CONFIG_HOME", None)

    def run_script(self, name, *args, env=None):
        return subprocess.run(["sh", str(self.app / name), *args], cwd=self.root,
                              env=env or self.env, capture_output=True, text=True, timeout=10, check=True)

    def test_foreground_uses_application_directory_and_preserves_user_options(self):
        self.run_script("run.sh", "--server.http.port=18080", "--myapp.openWebBrowserOnStartup=false")
        text = (self.root / "arguments").read_text()
        self.assertTrue(text.startswith(str(self.app) + "\n"))
        self.assertIn("--server.http.port=18080\n", text)
        self.assertNotIn("--server.port=8443", text)
        self.assertTrue(text.endswith("--myapp.openWebBrowserOnStartup=false\n"))

    def test_headless_launcher_falls_back_to_foreground_without_swing(self):
        result = self.run_script("launcher.sh")
        self.assertIn("No desktop session", result.stdout)
        self.assertNotIn("--myapp.desktop.enabled=true", (self.root / "arguments").read_text())

    def test_desktop_launcher_survives_its_shell_and_uses_private_log(self):
        java = self.bin / "java"
        java.write_text('#!/bin/sh\nprintf "%s\\n" "$@" > "$SC_TEST_OUTPUT"\nsleep 1\nprintf done > "$SC_TEST_OUTPUT.done"\n')
        self.run_script("launcher.sh", env=dict(self.env, DISPLAY=":test"))
        for _ in range(60):
            if (self.root / "arguments.done").exists():
                break
            time.sleep(.05)
        self.assertTrue((self.root / "arguments.done").exists())
        self.assertIn("--myapp.desktop.enabled=true", (self.root / "arguments").read_text())
        self.assertEqual((self.app / "launcher.log").stat().st_mode & 0o077, 0)

    def test_installs_user_shortcuts_and_marks_distribution_scripts_executable(self):
        xdg = self.bin / "xdg-user-dir"
        xdg.write_text('#!/bin/sh\nprintf "%s\\n" "$HOME/Desktop"\n')
        xdg.chmod(0o755)
        self.run_script("install-desktop.sh")
        menu = self.home / "data/applications/sweetcherry.desktop"
        desktop = self.home / "Desktop/sweetcherry.desktop"
        self.assertEqual(menu.read_bytes(), desktop.read_bytes())
        text = menu.read_text()
        self.assertIn("Terminal=false", text)
        self.assertIn("%%", text)
        self.assertIn('\\"quotes\\"', text)
        for name in ("run.sh", "launcher.sh", "install-desktop.sh"):
            self.assertTrue((self.app / name).stat().st_mode & 0o100)


if __name__ == "__main__":
    unittest.main()
