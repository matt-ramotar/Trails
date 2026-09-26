"""Regression checks for snapshot extraction, redaction, and capture selection."""

import datetime
import io
import json
from pathlib import Path
import sqlite3
import tarfile
import tempfile
import unittest
import xml.etree.ElementTree as ET

import android_capture
import database_snapshot


class SnapshotToolsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.addCleanup(self.temporary.cleanup)

    def archive(self, members):
        archive = self.root / 'snapshot.tar'
        with tarfile.open(archive, 'w') as bundle:
            for name, value in members:
                item = tarfile.TarInfo(name)
                item.size = len(value)
                bundle.addfile(item, io.BytesIO(value))
        return archive

    def test_rejects_traversal_and_duplicate_archive_members(self):
        for members in ([('../escape.db', b'x')], [('databases/trails-m1-backend.db', b'x')] * 2):
            with self.subTest(members=members):
                with tempfile.TemporaryDirectory() as output:
                    with self.assertRaises(ValueError):
                        database_snapshot.extract_databases(self.archive(members), Path(output), 1024)

    def test_rejects_oversized_archive_before_extracting(self):
        archive = self.archive([('databases/trails-m1-backend.db', b'1234')])
        with self.assertRaisesRegex(ValueError, 'size bound'):
            database_snapshot.extract_databases(archive, self.root, 3)

    def test_preserves_wal_and_redacts_checkpoint_search(self):
        checkpoint = json.dumps(dict(root='EXPLORE', text='private search', query={'text': 'private search'},
                                     exploreRoutes=[{'name': 'explore'}, {'name': 'trail', 'id': 'half-dome'}]))
        element = ET.Element('map')
        ET.SubElement(element, 'string', name='checkpoint').text = checkpoint
        archive = self.archive([
            ('databases/trails-m1-backend.db', b'database'),
            ('databases/trails-m1-backend.db-wal', b'wal'),
            ('shared_prefs/trails-m1-ui-' + 'private-account'.encode().hex() + '.xml', ET.tostring(element)),
        ])
        members, ignored, checkpoints = database_snapshot.extract_databases(archive, self.root, 4096)
        self.assertEqual(set(members), {'trails-m1-backend.db', 'trails-m1-backend.db-wal'})
        self.assertEqual(ignored, 0)
        self.assertEqual(checkpoints[0]['root'], 'EXPLORE')
        self.assertNotIn('private search', json.dumps(checkpoints))
        self.assertNotIn('private-account', json.dumps(checkpoints))
        self.assertEqual(checkpoints[0]['routes']['exploreRoutes'][1]['id'], 'half-dome')

    def test_activity_totals_use_supplied_month_and_timezone(self):
        # UTC September 1 is still August in New York.
        timestamp = int(datetime.datetime(2026, 9, 1, 1, tzinfo=datetime.timezone.utc).timestamp() * 1000)
        row = dict(namespace='activities', canonical_id='private-account', payload=json.dumps([
            dict(trailId='half-dome', completedAtEpochMillis=timestamp, distanceMeters=22700,
                 durationMinutes=660, elevationMeters=1463, privateField='do not emit')]))
        result = database_snapshot.summarize_feed(row, datetime.date(2026, 8, 31), 'America/New_York')
        self.assertEqual(result['monthSummary']['trails'], 1)
        self.assertEqual(result['monthSummary']['outsideWholeHours'], 11)
        self.assertNotIn('privateField', json.dumps(result))
        self.assertNotIn('private-account', json.dumps(result))
        result = database_snapshot.summarize_feed(row, datetime.date(2026, 9, 1), 'America/New_York')
        self.assertEqual(result['monthSummary']['trails'], 0)
        self.assertNotIn('monthSummary', database_snapshot.summarize_feed(row, None, None))

    def test_rejects_malformed_checkpoint_and_feed_shapes(self):
        with self.assertRaisesRegex(ValueError, 'checkpoint payload'):
            database_snapshot.summarize_checkpoint('trails-m1-ui-61.xml',
                b'<map><string name="checkpoint">[]</string></map>')
        with self.assertRaisesRegex(ValueError, 'checkpoint routes'):
            database_snapshot.summarize_checkpoint('trails-m1-ui-61.xml',
                b'<map><string name="checkpoint">{"exploreRoutes":["invalid"]}</string></map>')
        with self.assertRaisesRegex(ValueError, 'activity payload'):
            database_snapshot.summarize_feed(dict(namespace='activities', canonical_id='private', payload='[1]'), None, None)

    def test_mixed_account_cache_keeps_feed_out_of_memberships(self):
        database = self.root / 'trails-m1-61-values.db'
        with sqlite3.connect(database) as connection:
            connection.executescript('''
                CREATE TABLE cache_row(namespace TEXT, canonical_id TEXT, payload BLOB);
                CREATE TABLE command_acceptance(account TEXT);
                CREATE TABLE backend_meta(id INTEGER);
                CREATE TABLE backend_receipt(account TEXT);
                CREATE TABLE account_identity(id INTEGER);
                CREATE TABLE collection_row(id TEXT, position INTEGER);
            ''')
            connection.execute('INSERT INTO cache_row VALUES(?,?,?)', ('saved-a', 'half-dome',
                json.dumps(dict(trailId='half-dome', collectionIds=['favorites'])).encode()))
            connection.execute('INSERT INTO cache_row VALUES(?,?,?)', ('activities', 'a', b'[]'))
        result = database_snapshot.summarize(database, database_snapshot.db_identity(database.name), 100)
        self.assertTrue(result['complete'])
        self.assertEqual(len(result['confirmedMemberships']), 1)
        self.assertEqual(result['confirmedMemberships'][0]['trail'], 'half-dome')
        self.assertEqual(result['feeds'][0]['activities'], [])

    def test_capture_rejects_existing_output_before_device_access(self):
        output = self.root / 'captures'
        output.mkdir()
        (output / 'before.xml').write_text('original')
        device = android_capture.Device('unused', 'device', 5037, output)
        with self.assertRaisesRegex(ValueError, 'already exists'):
            device.capture('before')
        self.assertEqual((output / 'before.xml').read_text(), 'original')

    def test_capture_names_cannot_escape_output_directory(self):
        device = android_capture.Device('unused', 'device', 5037, self.root)
        with self.assertRaises(ValueError):
            device.targets('../outside')

    def test_slider_selection_resolves_label_to_seekbar(self):
        tree = ET.fromstring('<hierarchy><node class="android.widget.SeekBar" bounds="[0,0][100,48]">'
                             '<node text="Minimum length"/></node></hierarchy>')
        node = android_capture.find_node(tree, 'Minimum length', 0, slider=True)
        self.assertEqual(node.get('class'), 'android.widget.SeekBar')
        with self.assertRaises(ValueError):
            android_capture.find_node(tree, 'Minimum length', -1, slider=True)


if __name__ == '__main__':
    unittest.main()
