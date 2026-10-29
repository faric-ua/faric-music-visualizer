#!/usr/bin/env python3
"""Знімки екранів PulseDeck на емуляторі для відео-показу (faric 29.10).
Читає tour/plan.txt і виконує команди через adb:
  shot NAME | tap X Y | swipe X1 Y1 X2 Y2 [MS] | key CODE | sleep S | tapt ТЕКСТ | launch | dump NAME | sh КОМАНДА
Знімки й дампи — у out/."""
import subprocess, sys, time, re, os, html
OUT = 'out'; os.makedirs(OUT, exist_ok=True)
PKG = 'com.saney.musicvisualizer'
log = open(f'{OUT}/drive-log.txt', 'a')
def adb(*a, timeout=60, binary=False):
    try:
        r = subprocess.run(['adb', *a], capture_output=True, timeout=timeout)
        return r.stdout if binary else r.stdout.decode('utf-8', 'replace') + r.stderr.decode('utf-8', 'replace')
    except subprocess.TimeoutExpired:
        return b'' if binary else 'TIMEOUT'
last_xml = ''
def dump(name):
    global last_xml
    o = adb('shell', 'uiautomator', 'dump', '/sdcard/ui.xml', timeout=25)
    x = adb('shell', 'cat', '/sdcard/ui.xml', timeout=20)
    if '<hierarchy' in x:
        last_xml = x; open(f'{OUT}/{name}.xml', 'w').write(x)
    log.write(f'dump {name}: {o.strip()[:120]}\n')
def shot(name):
    png = adb('exec-out', 'screencap', '-p', binary=True, timeout=30)
    open(f'{OUT}/{name}.png', 'wb').write(png)
    top = adb('shell', 'dumpsys', 'activity', 'activities', timeout=20)
    m = re.search(r'topResumedActivity=.*?\{[^ ]+ [^ ]+ ([^ }]+)', top)
    log.write(f'shot {name}: {len(png)} bytes, top={m.group(1) if m else "?"}\n')
def tapt(text):
    dump('_tapt')
    for m in re.finditer(r'<node [^>]*>', last_xml):
        n = m.group(0)
        t = html.unescape(re.search(r'text="([^"]*)"', n).group(1)); d = html.unescape(re.search(r'content-desc="([^"]*)"', n).group(1))
        if text in t or text in d:
            b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
            x = (int(b.group(1)) + int(b.group(3))) // 2; y = (int(b.group(2)) + int(b.group(4))) // 2
            adb('shell', 'input', 'tap', str(x), str(y)); log.write(f'tapt {text} -> {x},{y}\n'); return
    log.write(f'tapt {text}: NOT FOUND\n')
for line in open(sys.argv[1], encoding='utf-8'):
    line = line.strip()
    if not line or line.startswith('#'): continue
    cmd, _, rest = line.partition(' ')
    log.write(f'> {line}\n'); log.flush()
    if cmd == 'shot': shot(rest)
    elif cmd == 'dump': dump(rest)
    elif cmd == 'tap': adb('shell', 'input', 'tap', *rest.split())
    elif cmd == 'swipe':
        p = rest.split(); p += ['350'] if len(p) == 4 else []; adb('shell', 'input', 'swipe', *p)
    elif cmd == 'key': adb('shell', 'input', 'keyevent', rest)
    elif cmd == 'sleep': time.sleep(float(rest))
    elif cmd == 'tapt': tapt(rest)
    elif cmd == 'launch': log.write(adb('shell', 'monkey', '-p', PKG, '-c', 'android.intent.category.LAUNCHER', '1') + '\n')
    elif cmd == 'sh': log.write(adb('shell', rest) + '\n')
    log.flush()
log.close()
