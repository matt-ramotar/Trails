#!/usr/bin/env python3
"""Measure the recording preview toast on the already installed Navigate screen."""
import datetime, hashlib, json, os, re, subprocess, time, uuid
from pathlib import Path
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[1]
ADB = str(Path.home() / 'Library/Android/sdk/platform-tools/adb')
PORT = os.environ.get('ROOTS_ADB_PORT', '5037')

def adb(*args, binary=False):
    result = subprocess.run([ADB, '-P', PORT, '-s', 'emulator-5554', *args], capture_output=True, check=True, timeout=20, stdin=subprocess.DEVNULL)
    return result.stdout if binary else result.stdout.decode()

source = ROOT / 'android/69-corrected-default-nav.xml'
tree = ET.parse(source)
node = next(n for n in tree.iter('node') if n.get('text') == 'Start recording')
x1,y1,x2,y2 = map(int,re.findall(r'\d+',node.get('bounds')))
report = {'sourceApkSha256':'d94158f5329c3cd3f653fb55bb2f3f3b7c7573ab628d034ed249dd8dd74e2138', 'utc':datetime.datetime.now(datetime.timezone.utc).isoformat(), 'fontScale':1.0, 'adbPort':int(PORT), 'tap':[int((x1+x2)/2),int((y1+y2)/2)], 'captures':[]}
start=time.monotonic()
adb('shell','input','tap',str(report['tap'][0]),str(report['tap'][1]))
report['tapCommandFinishedSeconds']=time.monotonic()-start
for name,target in [('70-recording-toast-timed',0.35),('71-recording-toast-after-five-seconds',5.2)]:
    time.sleep(max(0,target-(time.monotonic()-start)))
    begin=time.monotonic()-start
    shot=adb('exec-out','screencap','-p',binary=True)
    end=time.monotonic()-start
    out=ROOT/'android'/(name+'.png')
    if out.exists(): raise RuntimeError('Evidence path already exists')
    out.write_bytes(shot)
    subprocess.run(['sips','-Z','844',str(out)],capture_output=True,check=True)
    remote='/sdcard/roots-timed-'+uuid.uuid4().hex+'.xml'
    exported=adb('shell','uiautomator','dump',remote)
    if 'dumped to' not in exported: raise RuntimeError(exported)
    local=ROOT/'android'/(name+'.xml')
    adb('pull',remote,str(local))
    labels=[n.get('text') or n.get('content-desc') for n in ET.parse(local).iter('node')]
    report['captures'].append({'name':name,'pngCaptureStartSeconds':begin,'pngCaptureEndSeconds':end,'xmlExportFinishedSeconds':time.monotonic()-start,'toastInTree':'Recording is coming soon' in labels,'capturedPngSha256':hashlib.sha256(shot).hexdigest(),'storedPngSha256':hashlib.sha256(out.read_bytes()).hexdigest()})
    (ROOT/'logs/recording-toast-timing.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
