#!/usr/bin/env python3
"""Reject known vulnerable Java releases in CI; this is not a vulnerability scanner."""
import re
import subprocess
import sys

# November 2026 CSPU, CVE-2026-114938; see SECURITY.md.
MINIMUM = {17: (17, 0, 21, 1), 21: (21, 0, 13, 1),
           25: (25, 0, 5, 1), 27: (27, 0, 1, 1)}


def verify(output):
    match = re.search(r'^\s*java\.version\s*=\s*(\S+)', output, re.MULTILINE)
    if not match or not re.fullmatch(r'\d+(?:\.\d+){0,3}(?:\+[\w.-]+)?', match[1]):
        raise ValueError('Cannot verify a stable java.version from the runtime.')
    value = match[1]
    parts = tuple(map(int, value.split('+')[0].split('.')))
    version = parts + (0,) * (4 - len(parts))
    minimum = MINIMUM.get(version[0])
    if minimum is None or version < minimum:
        raise ValueError('Java ' + value + ' is outside the November 2026 security baseline; '
                         'install a patched vendor runtime. See SECURITY.md.')
    return value


if __name__ == '__main__':
    try:
        result = subprocess.run(['java', '-XshowSettings:properties', '-version'],
                                capture_output=True, text=True, check=True)
        print('Verified Java runtime security baseline: ' + verify(result.stdout + result.stderr))
    except (OSError, subprocess.SubprocessError, ValueError) as error:
        sys.exit(str(error))
