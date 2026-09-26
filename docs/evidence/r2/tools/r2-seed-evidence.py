#!/usr/bin/env python3
"""R2 catalog-reseed reader. Read-only over a copied databases tar; reports the integer seed
version, catalog order and difficulty grades that m1-database-evidence.py booleanizes or redacts.
Accounts are hashed; only public catalog identifiers and collection names are emitted."""
import argparse, hashlib, json, re, sqlite3, tarfile, tempfile
from pathlib import Path, PurePosixPath

DB = re.compile(r'^trails-m1-(?:(backend|catalog)|([0-9a-f]+)-(values|journal))\.db$')

def digest(v):
    if v is None: return None
    if not isinstance(v, bytes): v = str(v).encode()
    return hashlib.sha256(v).hexdigest()

def main():
    p = argparse.ArgumentParser()
    p.add_argument('archive', type=Path)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--capture-state', default='force-stopped')
    a = p.parse_args()
    if a.output.exists(): p.error('output must be a new path; evidence is never overwritten')
    report = {'reader': 'r2-seed-evidence', 'archiveSha256': hashlib.sha256(a.archive.read_bytes()).hexdigest(),
              'captureStateDeclaredByCaller': a.capture_state, 'databases': {}}
    with tempfile.TemporaryDirectory(prefix='trails-r2-seed-') as tmp:
        directory = Path(tmp)
        with tarfile.open(a.archive, 'r|*') as bundle:
            for entry in bundle:
                path = PurePosixPath(entry.name)
                if path.is_absolute() or '..' in path.parts or not entry.isfile(): continue
                name = path.name
                base = re.sub(r'-(wal|shm|journal)$', '', name)
                if path.parts[:-1] != ('databases',) or not DB.fullmatch(base): continue
                stream = bundle.extractfile(entry)
                (directory / name).write_bytes(stream.read())
        for db_path in sorted(directory.iterdir()):
            m = DB.fullmatch(db_path.name)
            if not m: continue
            public, account, role = m.groups()
            role = public or role
            c = sqlite3.connect(db_path.as_uri() + '?mode=ro', uri=True)
            c.row_factory = sqlite3.Row
            c.execute('PRAGMA query_only = ON')
            tables = {r[0] for r in c.execute("SELECT name FROM sqlite_master WHERE type='table'")}
            out = {'role': role}
            if account: out['accountSha256'] = digest(bytes.fromhex(account).decode())
            if role == 'backend' and 'backend_meta' in tables:
                meta = c.execute('SELECT * FROM backend_meta WHERE id=1').fetchone()
                out['seedVersion'] = meta['seeded']
                out['schemaVersion'] = meta['version']
                rows = c.execute('SELECT id,payload,position FROM backend_trail ORDER BY position').fetchall()
                out['trailCount'] = len(rows)
                grades, activities, treks = {}, {}, []
                order = []
                for r in rows:
                    t = json.loads(bytes(r['payload']).decode())
                    order.append(r['id'])
                    grades[t.get('difficulty')] = grades.get(t.get('difficulty'), 0) + 1
                    for act in t.get('activities', []):
                        activities[act] = activities.get(act, 0) + 1
                    if t.get('difficulty') == 'STRENUOUS':
                        treks.append({'id': r['id'], 'name': t.get('name'), 'position': r['position'],
                                      'activities': sorted(t.get('activities', [])),
                                      'durationMinutes': t.get('durationMinutes')})
                out['firstTrail'] = order[0] if order else None
                out['firstFiveTrails'] = order[:5]
                out['difficultyCounts'] = grades
                out['activityCounts'] = activities
                out['strenuousTrails'] = treks
                out['savedMemberships'] = [
                    {'accountSha256': digest(r['account']), 'trailId': r['trail_id'],
                     'collectionIds': sorted(json.loads(bytes(r['payload']).decode()).get('collectionIds', []))}
                    for r in c.execute('SELECT account,trail_id,payload FROM backend_saved ORDER BY account,trail_id')]
                out['receiptCount'] = c.execute('SELECT COUNT(*) FROM backend_receipt').fetchone()[0]
            elif role == 'catalog':
                out['cacheCounts'] = {r[0]: r[1] for r in c.execute('SELECT namespace,COUNT(*) FROM cache_row GROUP BY namespace')}
            elif role == 'values':
                out['collections'] = [{'id': r['id'], 'name': r['name'], 'position': r['position']}
                                      for r in c.execute('SELECT * FROM collection_row ORDER BY position')]
                out['confirmedMemberships'] = [
                    {'trailId': r['canonical_id'],
                     'collectionIds': sorted(json.loads(bytes(r['payload']).decode()).get('collectionIds', []))}
                    for r in c.execute("SELECT canonical_id,payload FROM cache_row ORDER BY canonical_id")]
            elif role == 'journal':
                out['acceptanceCount'] = c.execute('SELECT COUNT(*) FROM command_acceptance').fetchone()[0]
            c.close()
            report['databases'][db_path.name] = out
    a.output.write_text(json.dumps(report, indent=2, sort_keys=True) + '\n')
    print(json.dumps({k: v for k, v in report['databases'].items()}, indent=2)[:3000])

main()
