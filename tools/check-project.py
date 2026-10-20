#!/usr/bin/env python3
"""Check publication files without connecting to a TV."""
from pathlib import Path
import re
import hashlib

root = Path(__file__).resolve().parents[1]
errors = []
for path in root.rglob('*'):
    if not path.is_file() or '.git' in path.parts or 'work-tests' in path.parts:
        continue
    if path.suffix in ('.keystore', '.jks', '.pk8', '.der') or path.name.startswith('adbkey'):
        errors.append('Credential file: ' + str(path.relative_to(root)))
    if path.suffix == '.md':
        text = path.read_text(encoding='utf-8')
        targets = re.findall(r'\]\(([^\s)]+)\)', text) + re.findall(r'src="([^"]+)"', text)
        for target in targets:
            if '://' in target or target.startswith('#') or target.startswith('mailto:'):
                continue
            target = target.split('#', 1)[0]
            if target and not (path.parent / target).exists():
                errors.append(f'Broken link in {path.relative_to(root)}: {target}')
    if path.suffix in ('.java', '.py', '.md', '.json', '.xml', '.ps1', '.txt'):
        content = path.read_text(encoding='utf-8', errors='replace')
        if re.search(r'-----BEGIN (?:RSA |OPENSSH )?PRIVATE KEY-----', content):
            errors.append('Private key material in ' + str(path.relative_to(root)))
        if re.search(r'gh[pousr]_[A-Za-z0-9]{30,}', content):
            errors.append('Possible GitHub token in ' + str(path.relative_to(root)))
checksum = root / 'dist/SHA256SUMS.txt'
if checksum.exists():
    for line in checksum.read_text().splitlines():
        digest, name = line.split(maxsplit=1)
        path = checksum.parent / name.strip()
        if not path.exists() or hashlib.sha256(path.read_bytes()).hexdigest() != digest:
            errors.append('Checksum mismatch: ' + name)
if errors:
    raise SystemExit('\n'.join(errors))
print('PASS: local links, APK checksum and credential-file checks')
