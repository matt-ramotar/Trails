#!/usr/bin/env python3
"""Pull fresh PNG/XML created by the persistent adb shell; never opens adb shell streams."""
import subprocess,sys,hashlib,json,datetime,os,xml.etree.ElementTree as E
from pathlib import Path
name=sys.argv[1]; remote=sys.argv[2] if len(sys.argv)>2 else name
p=Path(__file__).resolve().parents[1]/'android'; adb=str(Path.home()/'Library/Android/sdk/platform-tools/adb')
for ext in ['xml','png']:
 out=p/(name+'.'+ext)
 if out.exists():raise ValueError('Evidence must not be overwritten')
 subprocess.run([adb,'-P',os.environ.get('ROOTS_ADB_PORT','5037'),'-s','emulator-5554','pull','/sdcard/'+remote+'.'+ext,str(out)],check=True,timeout=12)
raw=hashlib.sha256((p/(name+'.png')).read_bytes()).hexdigest()
subprocess.run(['sips','-Z','844',str(p/(name+'.png'))],capture_output=True,check=True)
with (p/'actions.jsonl').open('a') as f:f.write(json.dumps({'time':datetime.datetime.now(datetime.timezone.utc).isoformat(),'action':'persistent-shell-capture','name':name,'remote':remote,'capturedPngSha256':raw,'storedPngSha256':hashlib.sha256((p/(name+'.png')).read_bytes()).hexdigest()})+'\n')
for n in E.parse(p/(name+'.xml')).iter('node'):
 if n.get('text') or n.get('content-desc'):print(n.get('text') or n.get('content-desc'),n.get('bounds'),n.get('checked'))
