import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import lab
import profiles
import server_list
import webapp


class ProfilesTests(unittest.TestCase):
    def test_one_exact_version_and_no_unsupported_grim_substitution(self):
        rows = profiles.mapping()
        self.assertEqual({r['version'] for r in lab.VANILLA_VERSIONS}, set(rows))
        self.assertEqual('26.2-grim', rows['26.2']['version'])
        self.assertEqual('26.3', rows['26.3']['version'])
        self.assertNotIn('grim_version', rows['26.3'])
        self.assertIn('26.2', rows['26.3']['unavailable_reason'])
        self.assertNotIn('grim_version', rows['1.13'])
        self.assertEqual(len(rows), len({r['version'] for r in rows.values()}))

    def test_cli_routes_public_ids_and_preserves_explicit_reference_access(self):
        self.assertEqual('26.2-grim', profiles.select('26.2')[0]['version'])
        self.assertEqual('26.2', profiles.select('instance:26.2')[0]['version'])
        self.assertEqual(len(lab.VANILLA_VERSIONS), len(profiles.select('all')))
        self.assertEqual('1.12.2-grim', profiles.select('regression')[0]['version'])

    def test_public_status_and_logs_use_chosen_world_not_old_vanilla_report(self):
        row = profiles.mapping()['26.2']
        with tempfile.TemporaryDirectory() as tmp, patch.object(lab, 'ROOT', Path(tmp)), patch.object(lab, 'status', return_value=None):
            old = Path(tmp) / '26.2'; new = Path(tmp) / '26.2-grim'
            lab.save(old / 'verification.json', dict(success=True, passed=9999, time=100))
            lab.save(new / 'world-validation.json', dict(success=False, instance='26.2-grim', error='Missing sign', time=101))
            actual = webapp.inspect_server(row)
            self.assertEqual('26.2', actual['version'])
            self.assertIsNone(actual['verified'])
            self.assertFalse(actual['world_verified'])
            self.assertEqual('Missing sign', actual['world_check_error'])
            lab.save(new / 'world-validation.json', dict(success=True, instance='26.2', time=102))
            self.assertIsNone(webapp.inspect_server(row)['world_verified'])

    def test_exported_server_uses_real_protocol_name_without_grim_suffix(self):
        data = server_list.entries(profiles.select('26.2'))[0]
        self.assertIn(server_list.string_tag('viaForge$version', '26.2'), data)
        self.assertIn(server_list.string_tag('name', 'ViaForge Labor 26.2'), data)
        self.assertNotIn(b'-grim', data)

    def test_restored_world_cannot_inherit_displaced_worlds_pass(self):
        row = profiles.mapping()['26.2']
        with tempfile.TemporaryDirectory() as tmp, patch.object(lab, 'ROOT', Path(tmp)), patch.object(lab, 'status', return_value=None):
            folder = Path(tmp) / row['version']
            lab.save(folder / 'verification.json', dict(success=True, time=100, passed=99))
            lab.save(folder / 'world-validation.json', dict(success=True, instance=row['version'], time=100))
            lab.save(folder / 'world-audit.json', dict(time=100, missing=[], changed_type=[]))
            lab.save(folder / 'snapshot-restore.json', dict(time=101, success=True))
            status = webapp.inspect_server(row)
            self.assertIsNone(status['verified'])
            self.assertIsNone(status['world_verified'])
            self.assertIsNone(status['exhibit_issues'])
            lab.save(folder / 'world-validation.json', dict(success=True, instance=row['version'], time=102))
            self.assertTrue(webapp.inspect_server(row)['world_verified'])
            self.assertIsNone(webapp.inspect_server(row)['verified'])

    def test_paper_starting_protocol_is_not_a_port_conflict_when_owned(self):
        row = profiles.mapping()['26.2']
        with tempfile.TemporaryDirectory() as tmp, patch.object(lab, 'ROOT', Path(tmp)), \
                patch.object(lab, 'status', return_value={'version': {'protocol': -1}}), \
                patch.object(lab, 'process_alive', return_value=True):
            folder = Path(tmp) / row['version']
            lab.save(folder / 'process.json', dict(worker_pid=1, server_pid=2))
            self.assertEqual('conflict', webapp.inspect_server(row)['state'])
            (folder / 'worker.lock').touch()
            self.assertEqual('starting', webapp.inspect_server(row)['state'])

    def test_existing_client_list_moves_managed_address_without_losing_custom_tags(self):
        original = server_list.entries(lab.select('26.2'))[0]
        extra = server_list.string_tag('custom:keep', 'untouched')
        original = original[:-1] + extra + b'\0'
        actual = server_list.retarget_managed(original, profiles.select('26.2'))
        self.assertIn(extra, actual)
        self.assertEqual('127.0.0.1:27084', server_list.address(actual))
        self.assertIn(server_list.string_tag('viaForge$version', '26.2'), actual)
        self.assertEqual(actual, server_list.retarget_managed(actual, profiles.select('26.2')))
        custom = original.replace(server_list.string_tag('name', 'ViaForge Labor 26.2'),
                                  server_list.string_tag('name', 'My saved reference'))
        self.assertEqual(custom, server_list.retarget_managed(custom, profiles.select('26.2')))
        foreign = original.replace(server_list.string_tag('ip', '127.0.0.1:25637'),
                                   server_list.string_tag('ip', 'example.invalid:25565'))
        self.assertEqual(foreign, server_list.retarget_managed(foreign, profiles.select('26.2')))


if __name__ == '__main__': unittest.main()
