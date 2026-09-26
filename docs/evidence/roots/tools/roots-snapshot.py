#!/usr/bin/env python3
"""Read checkpoints and public feed fixtures from a force-stopped local archive.
Never changes the device or databases. Hashes account identifiers in the emitted report.
"""
import argparse, datetime, hashlib, json, sqlite3, tarfile, tempfile, xml.etree.ElementTree as ET
from pathlib import Path, PurePosixPath
from zoneinfo import ZoneInfo
p=argparse.ArgumentParser(); p.add_argument('archive',type=Path); p.add_argument('--output',required=True,type=Path); a=p.parse_args()
if a.output.exists(): p.error('Evidence output must be new')
def digest(b): return hashlib.sha256(b if isinstance(b,bytes) else b.encode()).hexdigest()
r={'archiveSha256':digest(a.archive.read_bytes()),'captureStateDeclaredByCaller':'force-stopped','checkpoints':[],'feeds':[]}
with tempfile.TemporaryDirectory(prefix='roots-snapshot-') as tmp:
 with tarfile.open(a.archive) as t:
  for m in t.getmembers():
   path=PurePosixPath(m.name)
   if not m.isfile() or path.is_absolute() or '..' in path.parts: continue
   if path.parts[0]=='databases': (Path(tmp)/path.name).write_bytes(t.extractfile(m).read())
   if path.parts[0]=='shared_prefs' and path.name.startswith('trails-m1-ui-'):
    raw=t.extractfile(m).read(); cp=ET.fromstring(raw).find('string').text
    account=bytes.fromhex(path.stem.removeprefix('trails-m1-ui-')).decode()
    r['checkpoints'].append(dict(accountSha256=digest(account),preferencesSha256=digest(raw),checkpointSha256=digest(cp),checkpoint=json.loads(cp)))
 for db in Path(tmp).glob('trails-m1-*.db'):
  c=sqlite3.connect(db.as_uri()+'?mode=ro',uri=True); c.row_factory=sqlite3.Row;c.execute('PRAGMA query_only=ON')
  for row in c.execute("SELECT namespace,canonical_id,payload FROM cache_row WHERE namespace IN ('backend-activities','backend-foryou','activities','foryou') ORDER BY namespace,canonical_id"):
   payload=json.loads(bytes(row['payload'])); f={'databaseRole':'backend' if db.name=='trails-m1-backend.db' else 'values','namespace':row['namespace'],'accountSha256':digest(row['canonical_id']),'payload':payload}
   if row['namespace']=='backend-activities':
    now=datetime.datetime.now(ZoneInfo('Europe/Madrid')); selected=[x for x in payload if datetime.datetime.fromtimestamp(x['completedAtEpochMillis']/1000,now.tzinfo).strftime('%Y-%m')==now.strftime('%Y-%m')]
    f['monthSummaryFromRows']={'month':now.strftime('%Y-%m'),'timezone':'Europe/Madrid','distanceMeters':sum(x['distanceMeters'] for x in selected),'trails':len(selected),'durationMinutes':sum(x['durationMinutes'] for x in selected),'outsideWholeHours':sum(x['durationMinutes'] for x in selected)//60}
   r['feeds'].append(f)
  c.close()
a.output.write_text(json.dumps(r,indent=2)+'\n');print(json.dumps({'checkpoints':len(r['checkpoints']),'feeds':len(r['feeds']),'output':str(a.output)}))
