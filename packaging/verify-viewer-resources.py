#!/usr/bin/env python3
"""Read-only release check: offline viewer resources in the Boot JAR and optional external seeds.
Run after Maven package; --clean also rejects edited/private Draw.io replacement files.
Python standard library only. Never extracts a JAR, downloads assets or writes user files.
"""
from pathlib import Path
from collections import Counter
import argparse, io, sys, zipfile, xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RESOURCE_DIRS = ('static/js/drawio', 'static/js/symbols', 'static/img/ctbicons', 'symbols')
RESOURCE_FILES = ('static/css/drawioPreview.css', 'static/css/symbols.css', 'templates/common/symbolPicker.html', 'static/js/drawioPreview.js',
                  'templates/attachment/drawioFrame.html')
DOC_FILES = ('documentation/en/drawio-viewer.md', 'documentation/tr/drawio-viewer.md',
             'documentation/en/inline-symbols.md', 'documentation/tr/inline-symbols.md',
             'documentation/en/editor-layout.md', 'documentation/tr/content-zoom-and-editor-controls.md',
             'documentation/examples/drawio-library-shapes.drawio', 'documentation/examples/drawio-viewer-demo.drawio',
             'THIRD-PARTY-NOTICES.md', 'licenses/drawio-Apache-2.0.txt', 'licenses/drawio-icons-TERMS.txt',
             'licenses/drawio-shapes-TERMS.txt', 'licenses/drawio-stencils-TERMS.txt')

class Problem(ValueError): pass

def version(root):
    pom = ET.parse(root / 'pom.xml').getroot()
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    return pom.findtext('m:version', namespaces=ns), pom.findtext('m:properties/m:sweetcherry.font-awesome.version', namespaces=ns)

def resource_paths(root):
    base = root / 'src/main/resources'
    paths = set(base / name for name in RESOURCE_FILES)
    for name in RESOURCE_DIRS:
        folder = base / name
        if not folder.is_dir(): raise Problem('Missing source resource directory: ' + name)
        paths.update(p for p in folder.rglob('*') if p.is_file())
    return sorted(paths)

def verify(bundle, root=ROOT, clean=False):
    bundle, root = Path(bundle), Path(root)
    app_version, fa_version = version(root)
    for relative in (*DOC_FILES, f'documentation/releases/v{app_version}.md', 'drawio-images/README.md', 'drawio-images/images.properties'):
        if not (bundle / relative).is_file(): raise Problem('Missing distribution file: ' + relative)
    seed = root / 'external/drawio-images'
    if clean:
        replacement_root = bundle / 'drawio-images'
        for path in (replacement_root, *replacement_root.rglob('*')):
            if path.is_symlink() or getattr(path, 'is_junction', lambda: False)():
                raise Problem('Linked Draw.io replacement path in clean distribution')
        expected = {p.relative_to(seed).as_posix(): p for p in seed.rglob('*') if p.is_file()}
        actual = {p.relative_to(bundle / 'drawio-images').as_posix(): p for p in (bundle / 'drawio-images').rglob('*') if p.is_file()}
        if actual.keys() != expected.keys(): raise Problem('Custom/missing Draw.io replacement files in clean distribution')
        for name, path in actual.items():
            if path.is_symlink() or path.read_bytes() != expected[name].read_bytes():
                raise Problem('Edited Draw.io replacement seed in clean distribution: ' + name)
    jar_path = bundle / 'SweetCherry.jar'
    if not jar_path.is_file(): raise Problem('Missing application JAR: ' + str(jar_path))
    source_base = root / 'src/main/resources'
    paths = resource_paths(root)
    with zipfile.ZipFile(jar_path) as jar:
        names = Counter(jar.namelist())
        build_info = 'BOOT-INF/classes/META-INF/build-info.properties'
        if names[build_info] != 1: raise Problem('Missing/duplicate JAR build information')
        properties = dict(line.split('=', 1) for line in jar.read(build_info).decode('utf-8').splitlines() if '=' in line and not line.startswith('#'))
        if properties.get('build.version') != app_version: raise Problem('Application JAR version does not match pom.xml')
        for path in paths:
            relative = path.relative_to(source_base).as_posix()
            name = 'BOOT-INF/classes/' + relative
            if names[name] != 1: raise Problem('Missing/duplicate JAR resource: ' + relative)
            # Detect binary filtering damage and incomplete/stale shape/icon packs, including CRLF checkouts.
            if jar.read(name) != path.read_bytes(): raise Problem('Changed JAR resource bytes: ' + relative)
        nested_name = f'BOOT-INF/lib/font-awesome-{fa_version}.jar'
        if names[nested_name] != 1: raise Problem('Missing/duplicate pinned Font Awesome WebJar')
        with zipfile.ZipFile(io.BytesIO(jar.read(nested_name))) as webjar:
            prefix = f'META-INF/resources/webjars/font-awesome/{fa_version}/'
            for name in ('css/all.min.css', 'webfonts/fa-solid-900.woff2', 'webfonts/fa-regular-400.woff2', 'webfonts/fa-brands-400.woff2', 'LICENSE.txt'):
                if prefix + name not in webjar.namelist() or not webjar.read(prefix + name):
                    raise Problem('Missing Font Awesome runtime resource: ' + name)
    return len(paths)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bundle', type=Path, default=ROOT / 'release/SweetCherry')
    parser.add_argument('--clean', action='store_true', help='Require untouched seed files and no custom Draw.io images')
    args = parser.parse_args()
    try:
        count = verify(args.bundle, clean=args.clean)
    except (Problem, OSError, zipfile.BadZipFile, ET.ParseError) as error:
        print('Viewer resource check failed: ' + str(error), file=sys.stderr)
        return 1
    print(f'Verified {count} byte-identical JAR resources, Font Awesome WebJar, guides/examples and Draw.io seeds' + (' (clean)' if args.clean else ''))
    return 0

if __name__ == '__main__':
    raise SystemExit(main())
