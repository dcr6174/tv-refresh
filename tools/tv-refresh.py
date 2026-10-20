#!/usr/bin/env python3
"""ADB inventory and reversible package disabling. Python 3; no third-party modules."""
import argparse
import datetime
import json
import re
import subprocess
from pathlib import Path

PROTECTED = ('android', 'com.android.', 'com.google.android.gms', 'com.google.android.gsf',
             'com.google.android.tvlauncher', 'com.google.android.leanbacklauncher',
             'com.chand.minimalhome', 'com.tcl.tv', 'com.tcl.tvinput', 'com.tcl.suspension')
SETTINGS = [('global', 'private_dns_mode'), ('global', 'private_dns_specifier'),
            ('secure', 'screensaver_components'), ('secure', 'screensaver_enabled'),
            ('secure', 'screensaver_activate_on_sleep'), ('secure', 'sleep_timeout'),
            ('system', 'screen_off_timeout')]


def adb(serial, *args):
    result = subprocess.run(['adb', '-s', serial, *args], capture_output=True, text=True, timeout=90)
    if result.returncode or re.search(r'(?m)^(Error|Failure|Exception)', result.stdout):
        raise RuntimeError((result.stderr + result.stdout).strip())
    return result.stdout.strip()


def save(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_suffix('.tmp')
    temp.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
    temp.replace(path)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True, help='ADB serial or TV_IP:PORT')
    parser.add_argument('--state', type=Path, default=Path('backups/tv-state.json'))
    sub = parser.add_subparsers(dest='action', required=True)
    sub.add_parser('snapshot', help='Read-only inventory; refuses to overwrite an existing backup')
    disable = sub.add_parser('disable', help='Disable only explicitly named, reviewed packages')
    disable.add_argument('packages', nargs='+')
    sub.add_parser('restore', help='Restore package states changed by this tool')
    args = parser.parse_args()
    if args.action == 'snapshot':
        if args.state.exists():
            parser.error('Backup exists. Use a new --state path; retain the original.')
        data = {'serial': args.serial, 'created': datetime.datetime.now(datetime.timezone.utc).isoformat(),
                'model': adb(args.serial, 'shell', 'getprop', 'ro.product.model'),
                'packages': adb(args.serial, 'shell', 'pm', 'list', 'packages', '-e'),
                'home': adb(args.serial, 'shell', 'cmd', 'package', 'resolve-activity', '--brief',
                            '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME'),
                'settings': {}, 'changes': {}}
        for scope, key in SETTINGS:
            data['settings'][scope + '/' + key] = adb(args.serial, 'shell', 'settings', 'get', scope, key)
        save(args.state, data)
        print('Snapshot saved. It contains device details; keep it private:', args.state)
        return
    data = json.loads(args.state.read_text(encoding='utf-8'))
    if data['serial'] != args.serial:
        parser.error('Backup belongs to a different ADB serial. Review it before proceeding.')
    if args.action == 'disable':
        # Validate the entire request before making any changes.
        for pkg in args.packages:
            if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+', pkg):
                parser.error('Invalid package name: ' + pkg)
            if any(pkg == p or pkg.startswith(p) for p in PROTECTED):
                parser.error('Protected system/input/Home package: ' + pkg)
        for pkg in args.packages:
            if pkg in data['changes']:
                print('Already recorded:', pkg)
                continue
            detail = adb(args.serial, 'shell', 'dumpsys', 'package', pkg)
            state = re.search(r'User 0:.*?installed=true.*?enabled=(\d+)', detail)
            if not state:
                raise RuntimeError('Package not installed for user 0: ' + pkg)
            original = int(state.group(1))
            if original not in (0, 1):
                print('Already disabled; unchanged:', pkg)
                continue
            data['changes'][pkg] = {'enabled': original, 'status': 'pending'}
            save(args.state, data)  # Record recovery information before mutating the TV.
            print(adb(args.serial, 'shell', 'pm', 'disable-user', '--user', '0', pkg))
            data['changes'][pkg]['status'] = 'disabled'
            save(args.state, data)
    else:
        for pkg, change in data['changes'].items():
            if not re.fullmatch(r'[A-Za-z][A-Za-z0-9_.]+', pkg):
                raise RuntimeError('Invalid package in backup')
            command = 'default-state' if change['enabled'] == 0 else 'enable'
            print(adb(args.serial, 'shell', 'pm', command, '--user', '0', pkg))
            change['status'] = 'restored'
            save(args.state, data)
        print('Package states restored. For Home/DNS/power, follow docs/restore.md using the saved values.')


if __name__ == '__main__':
    main()
