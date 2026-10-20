"""Exercise journal recovery without contacting a device."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('tv_refresh', Path(__file__).resolve().parents[1] / 'tools/tv-refresh.py')
tool = importlib.util.module_from_spec(spec)
spec.loader.exec_module(tool)


class JournalChecks(unittest.TestCase):
    def test_recovery_preserves_original_default_state(self):
        with tempfile.TemporaryDirectory() as temp:
            state = Path(temp) / 'state.json'
            tool.save(state, {'serial': 'test', 'changes': {}})
            calls = []

            def fake(serial, *args):
                calls.append(args)
                if 'dumpsys' in args:
                    return 'User 0: installed=true enabled=0'
                if 'disable-user' in args:
                    # Recovery must be recorded before the mutation.
                    saved = json.loads(state.read_text())
                    self.assertEqual(saved['changes']['com.example.optional']['status'], 'pending')
                return 'OK'

            common = ['tool', '--serial', 'test', '--state', str(state)]
            with patch.object(tool, 'adb', side_effect=fake), patch('sys.argv', common + ['disable', 'com.example.optional']):
                tool.main()
            with patch.object(tool, 'adb', side_effect=fake), patch('sys.argv', common + ['restore']):
                tool.main()
            self.assertIn(('shell', 'pm', 'default-state', '--user', '0', 'com.example.optional'), calls)

    def test_protected_request_does_not_touch_device(self):
        with tempfile.TemporaryDirectory() as temp:
            state = Path(temp) / 'state.json'
            tool.save(state, {'serial': 'test', 'changes': {}})
            argv = ['tool', '--serial', 'test', '--state', str(state), 'disable', 'com.android.systemui']
            with patch.object(tool, 'adb') as adb, patch('sys.argv', argv), self.assertRaises(SystemExit):
                tool.main()
            adb.assert_not_called()


if __name__ == '__main__':
    unittest.main()
