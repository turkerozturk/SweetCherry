#!/usr/bin/env python3
"""Refresh/check the dependency notice inventory against the actual packaged JAR.

Maintainer use after Maven package: python packaging/update-jar-notices.py
Verification (no downloads or writes): python packaging/update-jar-notices.py --check
Only embedded notices and available upstream source notices are copied; review
licence obligations separately. Existing historical notice files are retained.
"""
import argparse
import concurrent.futures
import functools
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import re
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

REPO = Path(__file__).resolve().parents[1]
CENTRAL = "https://repo.maven.apache.org/maven2/"


def notice(name):
    leaf = PurePosixPath(name).name.lower()
    return bool(re.match(r"^(licen[cs]e|notice|copying|copyright)([._-]|$)", leaf))


def save_notice(base, name, data):
    parts = PurePosixPath(name).parts
    if not parts or PurePosixPath(name).is_absolute() or ".." in parts or "\\" in name or any(":" in part for part in parts):
        raise ValueError("Unsafe archive member: " + name)
    path = base.joinpath(*parts)
    path.parent.mkdir(parents=True, exist_ok=True)
    # Normalise whitespace for Git without changing the licence wording.
    text = data.decode("utf-8")
    path.write_text("\n".join(line.rstrip() for line in text.splitlines()).rstrip() + "\n", encoding="utf-8")


def download(url):
    with urllib.request.urlopen(url, timeout=40) as response:
        data = response.read(60_000_001)
    if len(data) > 60_000_000:
        raise ValueError("Source/metadata download too large")
    return data


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--archive", type=Path, default=REPO / "target/SweetCherry.jar")
    parser.add_argument("--notices", type=Path, default=REPO / "distribution-notices/licenses")
    parser.add_argument("--maven-cache", type=Path, default=Path.home() / ".m2/repository")
    args = parser.parse_args()
    inventory = args.notices / "JAR-INVENTORY.json"
    previous = {row["jar"]: row for row in json.loads(inventory.read_text(encoding="utf-8"))} if inventory.exists() else {}
    with zipfile.ZipFile(args.archive) as archive:
        jars = {name.rsplit("/", 1)[-1]: archive.read(name) for name in archive.namelist()
                if name.startswith("BOOT-INF/lib/") and name.endswith(".jar")}
    hashes = {name: hashlib.sha256(data).hexdigest() for name, data in jars.items()}
    if args.check:
        if set(jars) != set(previous) or any(previous[name].get("sha256") != sha for name, sha in hashes.items()):
            raise SystemExit("Dependency notice inventory differs from packaged JAR; regenerate and review it.")
        print(f"Verified {len(jars)} dependency JARs against notice inventory")
        return

    cached = {}
    for path in args.maven_cache.rglob("*.jar"):
        cached.setdefault(path.name, []).append(path)

    @functools.lru_cache(maxsize=None)
    def pom(coords):
        group, artifact, version = coords
        relative = f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}.pom"
        path = args.maven_cache / relative
        return ET.fromstring(path.read_bytes() if path.exists() else download(CENTRAL + relative)), CENTRAL + relative

    def metadata(coords, depth=0):
        if depth > 12:
            raise ValueError("POM parent depth exceeded")
        root, url = pom(coords)
        licences = [{child.tag.split('}')[-1]: (child.text or '').strip()
                     for child in licence} for licence in root.findall("{*}licenses/{*}license")]
        scm = root.findtext("{*}scm/{*}url")
        if licences:
            return licences, url, scm
        parent = root.find("{*}parent")
        if parent is None:
            return [], url, scm
        parent_coords = tuple(parent.findtext("{*}" + field) for field in ("groupId", "artifactId", "version"))
        inherited, origin, parent_scm = metadata(parent_coords, depth + 1)
        return inherited, origin, scm or parent_scm

    def process(item):
        name, data = item
        if previous.get(name, {}).get("sha256") == hashes[name]:
            return previous[name]
        candidates = [p for p in cached.get(name, []) if hashlib.sha256(p.read_bytes()).hexdigest() == hashes[name]]
        if len(candidates) == 1:
            relative = candidates[0].relative_to(args.maven_cache).parts
            coords = (".".join(relative[:-3]), relative[-3], relative[-2])
        elif re.fullmatch(r"spring-boot-jarmode-tools-[0-9][A-Za-z0-9.-]*\.jar", name):
            # The Boot plugin embeds this published artifact instead of resolving it into the local cache.
            version = name.removeprefix("spring-boot-jarmode-tools-").removesuffix(".jar")
            coords = ("org.springframework.boot", "spring-boot-jarmode-tools", version)
            published = download(CENTRAL + f"org/springframework/boot/spring-boot-jarmode-tools/{version}/{name}")
            if hashlib.sha256(published).hexdigest() != hashes[name]:
                raise ValueError("Boot embedded tools differ from the published artifact")
        else:
            raise ValueError("Cannot identify Maven coordinates for " + name)
        group, artifact, version = coords
        base = f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}"
        row = {"jar": name, "coordinates": list(coords), "sha256": hashes[name],
               "maven_pom_url": CENTRAL + base + ".pom", "source_archive_url": CENTRAL + base + "-sources.jar",
               "embedded_notices": [], "source_notice_files": [], "source_archive_verified": False}
        row["resolved_licenses"], row["license_metadata_source"], row["scm_url"] = metadata(coords)
        with zipfile.ZipFile(io.BytesIO(data)) as jar:
            for member in jar.infolist():
                if not member.is_dir() and notice(member.filename) and member.file_size <= 250_000:
                    save_notice(args.notices / "embedded-notices" / name[:-4], member.filename, jar.read(member))
                    row["embedded_notices"].append(member.filename)
        if not row["embedded_notices"]:
            try:
                path = args.maven_cache / (base + "-sources.jar")
                if not path.exists():
                    source_data = download(row["source_archive_url"])
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_bytes(source_data)
                else:
                    source_data = path.read_bytes()
                with zipfile.ZipFile(io.BytesIO(source_data)) as source:
                    header_count = 0
                    for member in source.infolist():
                        if member.is_dir() or member.file_size > 250_000:
                            continue
                        if notice(member.filename):
                            save_notice(args.notices / "source-notices" / name[:-4], member.filename, source.read(member))
                            row["source_notice_files"].append(member.filename)
                        elif header_count < 3 and member.filename.endswith((".java", ".kt")):
                            text = source.read(member).decode("utf-8", errors="replace")
                            end = text.find("*/")
                            if 0 <= end < 12_000 and "copyright" in text[:end].lower():
                                save_notice(args.notices / "source-notices" / name[:-4],
                                            f"copyright-header-{header_count + 1}.txt",
                                            (member.filename + "\n" + text[:end + 2] + "\n").encode())
                                header_count += 1
                    row["source_copyright_headers"] = header_count
                row["source_archive_verified"] = True
            except (OSError, ValueError, zipfile.BadZipFile) as error:
                row["source_verification_note"] = type(error).__name__ + ": source archive not verified; review upstream licence references."
        return row

    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        rows = list(pool.map(process, sorted(jars.items())))
    args.notices.mkdir(parents=True, exist_ok=True)
    inventory.write_text(json.dumps(rows, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    missing = [row["jar"] for row in rows if not row.get("resolved_licenses")]
    print(f"Inventoried {len(rows)} dependency JARs; {sum(bool(r.get('embedded_notices')) for r in rows)} contain embedded notices")
    if missing:
        print("Review missing POM licence metadata:", ", ".join(missing))


if __name__ == "__main__":
    main()
