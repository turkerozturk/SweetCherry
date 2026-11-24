#!/usr/bin/env python3
"""One-time, explicit import of a legacy hash-named saved-filter document. No overwrite or deletion."""
import argparse
import hashlib
import json
import os
from pathlib import Path


def portable_source(installation, jdbc_url):
    prefix = 'jdbc:sqlite:'
    if not jdbc_url.startswith(prefix):
        return jdbc_url
    value = jdbc_url[len(prefix):]
    if not value or value.startswith('file:') or value == ':memory:' or '?' in value:
        return jdbc_url
    root = Path(os.path.abspath(installation))
    path = Path(os.path.abspath(root / value))
    try:
        relative = path.relative_to(root)
    except ValueError:
        return prefix + str(path)
    return prefix + 'portable:' + relative.as_posix()


def scope(user, tenant, installation, jdbc_url):
    identity = 'saved-filters-v2\0' + portable_source(installation, jdbc_url)
    digest = hashlib.sha256()
    for value in (user, tenant, identity):
        data = value.encode('utf-8')
        digest.update(len(data).to_bytes(4, 'big'))
        digest.update(data)
    return digest.hexdigest()


def migrate(source, installation, user, tenant, jdbc_url):
    source = Path(source)
    if source.stat().st_size > 1_048_576:
        raise ValueError('Saved filters exceed 1 MiB')
    with source.open('rb') as stream:
        data = stream.read(1_048_577)
    if len(data) > 1_048_576:
        raise ValueError('Saved filters exceed 1 MiB')
    document = json.loads(data)
    if not isinstance(document, dict) or document.get('version') != 1 or not isinstance(document.get('presets'), list) or len(document['presets']) > 50:
        raise ValueError('Unsupported saved-filter document')
    # Never enumerate other users' preference files or guess the originating database.
    directory = Path(installation) / 'saved-filters'
    directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    if directory.is_symlink():
        raise ValueError('Linked preference directory is unsupported')
    target = directory / (scope(user, tenant, installation, jdbc_url) + '.json')
    try:
        descriptor = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    except FileExistsError:
        if not target.is_symlink() and target.read_bytes() == data:
            return target
        raise ValueError('Destination already exists; no files were changed')
    with os.fdopen(descriptor, 'wb') as stream:
        stream.write(data)
    return target


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path, help='The specific JSON file to import')
    parser.add_argument('--installation', type=Path, required=True, help='Destination SweetCherry folder')
    parser.add_argument('--user', required=True, help='SweetCherry login name')
    parser.add_argument('--tenant', required=True, help='Exact configured tenant name')
    parser.add_argument('--jdbc-url', required=True, help='Destination tenant JDBC URL')
    args = parser.parse_args()
    try:
        target = migrate(args.source, args.installation, args.user, args.tenant, args.jdbc_url)
    except (OSError, ValueError) as error:
        parser.exit(1, f'Import failed: {error}\n')
    print(f'Imported without changing the original: {target}')


if __name__ == '__main__':
    main()
