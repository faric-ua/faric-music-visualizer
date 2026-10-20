# Generates PulseDeck icon vector drawables (variant Б + mono Г) from the approved mock.
import os, sys
RES = sys.argv[1]
def f(v): return ('%.3f' % v).rstrip('0').rstrip('.')
def circle(cx, cy, r):
    return f"M{f(cx-r)},{f(cy)}a{f(r)},{f(r)} 0 1,0 {f(2*r)},0a{f(r)},{f(r)} 0 1,0 {f(-2*r)},0Z"
def rrect(x, y, w, h, r):
    return (f"M{f(x+r)},{f(y)}H{f(x+w-r)}A{f(r)},{f(r)} 0 0,1 {f(x+w)},{f(y+r)}V{f(y+h-r)}"
            f"A{f(r)},{f(r)} 0 0,1 {f(x+w-r)},{f(y+h)}H{f(x+r)}A{f(r)},{f(r)} 0 0,1 {f(x)},{f(y+h-r)}"
            f"V{f(y+r)}A{f(r)},{f(r)} 0 0,1 {f(x+r)},{f(y)}Z")
def col(hexrgb, a=1.0):
    return '#%02X%s' % (round(a*255), hexrgb.lstrip('#').upper())
def radial(cx, cy, r, stops):
    items = ''.join(f'<item android:offset="{f(o)}" android:color="{c}"/>' for o, c in stops)
    return (f'<aapt:attr name="android:fillColor"><gradient android:type="radial" android:centerX="{f(cx)}" '
            f'android:centerY="{f(cy)}" android:gradientRadius="{f(r)}">{items}</gradient></aapt:attr>')
def linear(x1, y1, x2, y2, stops, attr='fillColor'):
    items = ''.join(f'<item android:offset="{f(o)}" android:color="{c}"/>' for o, c in stops)
    return (f'<aapt:attr name="android:{attr}"><gradient android:type="linear" android:startX="{f(x1)}" '
            f'android:startY="{f(y1)}" android:endX="{f(x2)}" android:endY="{f(y2)}">{items}</gradient></aapt:attr>')
def path(d, fill=None, grad=None, stroke=None, sw=None, sgrad=None, cap=None, join=None):
    a = f'<path android:pathData="{d}"'
    if fill: a += f' android:fillColor="{fill}"'
    if stroke: a += f' android:strokeColor="{stroke}"'
    if sw: a += f' android:strokeWidth="{f(sw)}"'
    if cap: a += f' android:strokeLineCap="{cap}"'
    if join: a += f' android:strokeLineJoin="{join}"'
    inner = (grad or '') + (sgrad or '')
    return a + (f'>{inner}</path>' if inner else '/>')
def group(children, tx=0, ty=0, sx=1, sy=1, rot=0, clip=None):
    a = '<group'
    if tx: a += f' android:translateX="{f(tx)}"'
    if ty: a += f' android:translateY="{f(ty)}"'
    if sx != 1: a += f' android:scaleX="{f(sx)}"'
    if sy != 1: a += f' android:scaleY="{f(sy)}"'
    if rot: a += f' android:rotation="{f(rot)}"'
    a += '>'
    if clip: a += f'<clip-path android:pathData="{clip}"/>'
    return a + ''.join(children) + '</group>'
def vector(size, viewport, body, tint=None):
    t = f' android:tint="{tint}"' if tint else ''
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- Generated: PulseDeck icon (V86, variant B chosen by faric 20.10 16:44). Source mock: Design canvas «Ярлики PulseDeck». -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android" xmlns:aapt="http://schemas.android.com/aapt"\n'
            f'    android:width="{size}dp" android:height="{size}dp" android:viewportWidth="{viewport}" android:viewportHeight="{viewport}"{t}>\n'
            + body + '\n</vector>\n')

BLUE, DEEP, NEON, ORANGE, PEAK = '#31d7ff', '#0a84ff', '#1aa8ff', '#ff8a3d', '#ffb36b'

def background_parts(full=(-25, 125)):
    lo, hi = full
    return [
        path(f"M{lo},{lo}H{hi}V{hi}H{lo}Z", grad=radial(50, 60, 70, [(0, col('#12293a')), (.55, col('#08111a')), (1, col('#030507'))])),
        group([path(circle(0, 0, 44), grad=radial(0, 0, 44, [(0, col(NEON, .34)), (.6, col(NEON, .08)), (1, col(NEON, 0))]))], tx=50, ty=56, sy=40/44),
        group([path(circle(0, 0, 38), grad=radial(0, 0, 38, [(0, col(ORANGE, .3)), (.5, col(BLUE, .1)), (1, col(BLUE, 0))]))], tx=50, ty=30, sy=20/38),
    ]

def speaker():
    p = []
    p.append(path(circle(0, 0, 26), grad=linear(-26, -26, 26, 26, [(0, col('#5d6b78')), (.35, col('#1b232b')), (.7, col('#0b1015')), (1, col('#3a4652'))])))
    p.append(path(circle(0, 0, 25.7), stroke=col(BLUE, .45), sw=.6))
    p.append(path("M-20.8,-12A24,24 0 0,1 -8.2,-22.6", stroke=col('#ffffff', .55), sw=.9, cap='round'))
    for sx in (-1, 1):
        for sy in (-1, 1):
            p.append(path(circle(17.25*sx, 17.25*sy, 1), fill=col('#9aa6b2')))
    p.append(path(circle(0, 0, 22), sw=1.5, sgrad=linear(-22, -22, 22, 22, [(0, col(BLUE)), (.6, col(NEON)), (1, col(ORANGE))], attr='strokeColor')))
    p.append(path(circle(0, 0, 21.3), grad=radial(0, 0, 21.3, [(0, col('#20272e')), (.8, col('#12171c')), (.92, col('#2a333c')), (1, col('#07090b'))])))
    p.append(path(circle(0, 0, 17.6), grad=radial(-2.8, -4.2, 24.6, [(0, col('#3a434c')), (.45, col('#1a2026')), (1, col('#06080a'))])))
    p.append(path(circle(0, 0, 17.6), stroke=col('#000000', .6), sw=.5))
    p.append(path(circle(0, 0, 14), stroke=col('#ffffff', .06), sw=.5))
    p.append(path(circle(0, 0, 10.5), stroke=col('#ffffff', .05), sw=.5))
    p.append(group([path(circle(0, 0, 4), grad=radial(0, 0, 4, [(0, col('#ffffff', .22)), (1, col('#ffffff', 0))]))], tx=-6.5, ty=-7, sx=2, rot=-40))
    p.append(path(circle(0, 0, 11), grad=radial(0, 0, 11, [(0, col(ORANGE, .9)), (.5, col('#ff6a00', .35)), (1, col('#ff6a00', 0))])))
    p.append(path(circle(0, 0, 6.4), grad=radial(-1.54, -1.92, 9.6, [(0, col('#ffe0bd')), (.35, col('#ff9a4d')), (.8, col('#d24a12')), (1, col('#7a2508'))])))
    p.append(group([path(circle(0, 0, 1.5), fill=col('#ffffff', .55))], tx=-2.2, ty=-2.4, sx=2.6/1.5, rot=-30))
    return p

BARS = [(-75, 5), (-60, 8), (-45, 12), (-30, 10), (-15, 15), (0, 19), (15, 14), (30, 17), (45, 11), (60, 7), (75, 4)]
WAVE = "M27,88H38L41,84L44,92L47,81.5L50,95L53,83L56,90L59,86L62,88H73"

def foreground_parts():
    p = []
    bar_grad = [(0, col(DEEP)), (.45, col(BLUE)), (1, col(ORANGE))]
    for ang, L in BARS:
        p.append(group([
            path(rrect(-1.6, -(26+L), 3.2, L, 1), grad=linear(0, -26, 0, -45, bar_grad)),
            path(circle(0, -(26+L+2.4), .9), fill=col(PEAK)),
        ], tx=50, ty=58, rot=ang))
    p.append(group(speaker(), tx=50, ty=58, sx=22/26, sy=22/26))
    p.append(path(WAVE, stroke=col(BLUE, .22), sw=4, cap='round', join='round'))
    p.append(path(WAVE, sw=1.4, cap='round', join='round', sgrad=linear(27, 0, 73, 0, [(0, col(DEEP)), (.5, col(ORANGE)), (1, col(DEEP))], attr='strokeColor')))
    return p

MONO_BARS = [(26.5, 29, 9), (36.5, 22, 16), (46.5, 16, 22), (56.5, 23, 15), (66.5, 30, 8)]
def mono_parts(color='#FFFFFFFF'):
    p = [path(rrect(x, y, 7, h, 2), fill=color) for x, y, h in MONO_BARS]
    p.append(path(circle(50, 66, 20), stroke=color, sw=6))
    p.append(path(circle(50, 66, 7.5), fill=color))
    return p

def w(name, text):
    os.makedirs(os.path.dirname(os.path.join(RES, name)), exist_ok=True)
    open(os.path.join(RES, name), 'w').write(text)

# Adaptive icon layers: 108dp, the 100-unit mock maps onto the 72dp visible area.
w('drawable/ic_launcher_background.xml', vector(108, 108, group(background_parts(), tx=18, ty=18, sx=.72, sy=.72)))
w('drawable/ic_launcher_foreground.xml', vector(108, 108, group(foreground_parts(), tx=18, ty=18, sx=.72, sy=.72)))
w('drawable/ic_launcher_monochrome.xml', vector(108, 108, group(mono_parts(), tx=21, ty=21, sx=.66, sy=.66)))
adaptive = ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@drawable/ic_launcher_background" />\n'
            '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
            '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
            '</adaptive-icon>\n')
w('mipmap-anydpi-v26/ic_launcher.xml', adaptive)
w('mipmap-anydpi-v26/ic_launcher_round.xml', adaptive)
# Status bar / notification shade (variant Г, white on transparent). Same name as
# Media3's own drawable, so the library notification uses ours.
w('drawable/media3_notification_small_icon.xml', vector(24, 24, group(mono_parts(), tx=-3, ty=-3.75, sx=.3, sy=.3)))
# Full-colour logo for headers (library title, centre HUD title).
w('drawable/ic_pulsedeck_logo.xml', vector(48, 100, group(background_parts((0, 100)) + foreground_parts(), clip=rrect(0, 0, 100, 100, 22))))
print('ok')
