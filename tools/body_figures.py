# Body-map figures: hand-authored half outlines mirrored around x = 100. Writes figures.json (path data
# copied into ui/components/BodyFigures.kt) and preview.html/svg in the current directory. Run: python3 tools/body_figures.py
import json, sys
W = 200.0

def mirror_pt(p): return (W - p[0], p[1])

def full_outline(start, segs):
    """start + list of cubic segs (c1,c2,end) on the viewer-left half, from top centre down to bottom centre.
    Returns SVG path d: left half then mirrored right half back up."""
    d = [f"M{start[0]:.1f},{start[1]:.1f}"]
    for c1, c2, e in segs:
        d.append(f"C{c1[0]:.1f},{c1[1]:.1f} {c2[0]:.1f},{c2[1]:.1f} {e[0]:.1f},{e[1]:.1f}")
    for i in range(len(segs) - 1, -1, -1):
        c1, c2, _ = segs[i]
        s = start if i == 0 else segs[i - 1][2]
        a, b, e = mirror_pt(c2), mirror_pt(c1), mirror_pt(s)
        d.append(f"C{a[0]:.1f},{a[1]:.1f} {b[0]:.1f},{b[1]:.1f} {e[0]:.1f},{e[1]:.1f}")
    d.append("Z")
    return " ".join(d)

def closed(start, segs):
    d = [f"M{start[0]:.1f},{start[1]:.1f}"]
    for c1, c2, e in segs:
        d.append(f"C{c1[0]:.1f},{c1[1]:.1f} {c2[0]:.1f},{c2[1]:.1f} {e[0]:.1f},{e[1]:.1f}")
    d.append("Z")
    return " ".join(d)

def mirror_closed(start, segs):
    return closed(mirror_pt(start), [(mirror_pt(a), mirror_pt(b), mirror_pt(c)) for a, b, c in segs])

def lines_mirrored(paths):
    """paths: list of (start, segs) open strokes on the left; returns list of d for both sides."""
    out = []
    for start, segs in paths:
        out.append(" ".join([f"M{start[0]:.1f},{start[1]:.1f}"] + [f"C{a[0]:.1f},{a[1]:.1f} {b[0]:.1f},{b[1]:.1f} {c[0]:.1f},{c[1]:.1f}" for a, b, c in segs]))
        ms = mirror_pt(start)
        out.append(" ".join([f"M{ms[0]:.1f},{ms[1]:.1f}"] + [f"C{mirror_pt(a)[0]:.1f},{mirror_pt(a)[1]:.1f} {mirror_pt(b)[0]:.1f},{mirror_pt(b)[1]:.1f} {mirror_pt(c)[0]:.1f},{mirror_pt(c)[1]:.1f}" for a, b, c in segs]))
    return out

# ---------------- MALE ----------------
male_body = full_outline((100, 54), [
    ((93, 54), (91, 64), (88, 70)),          # neck left side
    ((82, 75), (70, 77), (60, 80)),          # trapezius slope
    ((52, 83), (47, 90), (48, 100)),         # top of shoulder (hidden by the arm)
    ((49, 110), (58, 118), (63, 124)),       # armpit
    ((64, 140), (66, 158), (69, 176)),       # flank to waist
    ((71, 190), (68, 204), (66, 216)),       # oblique flare to hip
    ((64, 228), (64, 238), (65, 248)),       # hip to thigh
    ((66, 270), (69, 298), (73, 322)),       # outer thigh to knee
    ((74, 330), (73, 338), (72, 344)),       # knee outer
    ((70, 358), (71, 372), (75, 388)),       # calf bulge
    ((77, 398), (79, 406), (81, 414)),       # to ankle
    ((80, 421), (74, 426), (74, 431)),       # foot outer
    ((74, 437), (86, 438), (93, 436)),       # toes
    ((97, 435), (97, 430), (96, 425)),       # foot inner
    ((95, 419), (95, 414), (95, 408)),       # ankle inner
    ((96, 392), (98, 372), (97, 356)),       # inner calf
    ((96, 347), (95, 340), (95, 334)),       # inner knee
    ((95, 314), (98, 288), (99, 264)),       # inner thigh
    ((99.5, 256), (100, 252), (100, 250)),   # crotch
])
male_arm_start = (60, 80)
male_arm = [
    ((49, 81), (41, 90), (40, 103)),         # deltoid
    ((39, 118), (39, 136), (39, 152)),       # upper arm outer
    ((39, 162), (38, 170), (38, 176)),       # elbow
    ((37, 193), (35, 210), (35, 226)),       # forearm outer
    ((35, 233), (33, 238), (32, 245)),       # wrist -> hand
    ((30, 255), (31, 263), (35, 267)),       # hand outer
    ((39, 271), (45, 269), (46, 262)),       # fingertips
    ((47, 254), (48, 247), (48, 241)),       # hand inner
    ((49, 235), (50, 230), (51, 225)),       # wrist inner
    ((53, 208), (55, 190), (56, 177)),       # forearm inner
    ((57, 166), (58, 156), (59, 146)),       # elbow inner / biceps
    ((61, 134), (63, 125), (64, 117)),       # biceps to armpit
    ((65, 100), (66, 88), (60, 80)),         # back into the shoulder (hidden)
]
male_head = "M100,6 C114,6 122,17 122,32 C122,43 119,50 114,55 C110,59 105,61 100,61 C95,61 90,59 86,55 C81,50 78,43 78,32 C78,17 86,6 100,6 Z"
male_hair = "M78,30 C77,15 87,4 100,4 C113,4 123,15 122,30 C119,22 113,17 100,17 C88,17 81,22 78,30 Z"
male_lines = lines_mirrored([
    ((99, 86), [((92, 88), (84, 87), (74, 84))]),                 # clavicle
    ((99, 122), [((92, 128), (78, 130), (66, 120))]),             # lower pec
    ((89, 146), [((93, 148), (97, 148), (99, 147))]),             # abs row 1
    ((88, 162), [((92, 164), (97, 164), (99, 163))]),             # abs row 2
    ((89, 178), [((93, 180), (97, 180), (99, 179))]),             # abs row 3
    ((74, 202), [((82, 216), (90, 230), (97, 244))]),             # inguinal V
    ((79, 330), [((83, 336), (89, 336), (92, 331))]),             # knee cap
    ((57, 108), [((53, 116), (50, 124), (49, 130))]),             # deltoid separation
]) + ["M100,128 C100,150 100,172 100,192"]                         # linea alba

# ---------------- FEMALE ----------------
female_body = full_outline((100, 56), [
    ((94, 56), (92, 64), (90, 70)),          # neck (slimmer)
    ((84, 75), (74, 78), (64, 82)),          # trapezius slope (narrower)
    ((56, 85), (52, 92), (53, 101)),         # shoulder (hidden)
    ((54, 110), (61, 117), (65, 123)),       # armpit
    ((67, 136), (73, 156), (75, 172)),       # ribcage to narrow waist
    ((76, 186), (68, 202), (63, 216)),       # waist to hip flare
    ((60, 228), (59, 240), (60, 252)),       # wide hip
    ((61, 274), (66, 300), (72, 324)),       # fuller outer thigh to knee
    ((73, 332), (73, 340), (73, 346)),       # knee outer
    ((71, 360), (72, 374), (76, 390)),       # calf
    ((78, 400), (80, 407), (82, 414)),       # ankle
    ((81, 421), (76, 425), (76, 430)),       # foot outer
    ((76, 436), (86, 437), (92, 435)),       # toes
    ((95, 434), (96, 429), (95, 424)),       # foot inner
    ((94, 418), (94, 413), (94, 408)),       # ankle inner
    ((95, 392), (97, 374), (96, 358)),       # inner calf
    ((95, 349), (94, 342), (94, 336)),       # inner knee
    ((94, 316), (97, 290), (99, 266)),       # inner thigh
    ((99.5, 258), (100, 254), (100, 252)),   # crotch
])
female_arm_start = (64, 82)
female_arm = [
    ((55, 83), (48, 91), (47, 102)),         # shoulder
    ((46, 118), (46, 136), (46, 152)),       # upper arm outer
    ((46, 161), (45, 168), (45, 174)),       # elbow
    ((44, 191), (42, 208), (42, 224)),       # forearm outer
    ((42, 231), (40, 236), (39, 243)),       # wrist -> hand
    ((37, 252), (38, 260), (41, 264)),       # hand outer
    ((45, 268), (50, 266), (51, 259)),       # fingertips
    ((52, 251), (53, 244), (53, 239)),       # hand inner
    ((54, 233), (55, 228), (56, 223)),       # wrist inner
    ((58, 207), (59, 190), (60, 176)),       # forearm inner
    ((61, 165), (62, 156), (63, 146)),       # elbow inner
    ((64, 134), (65, 126), (66, 118)),       # to armpit
    ((67, 102), (69, 90), (64, 82)),         # back into the shoulder
]
female_head = "M100,9 C113,9 121,19 121,33 C121,44 118,51 113,56 C109,60 104,62 100,62 C96,62 91,60 87,56 C82,51 79,44 79,33 C79,19 87,9 100,9 Z"
female_hair = "M100,3 C117,3 128,15 127,33 C126,46 129,58 133,67 C126,71 118,67 116,58 C119,46 119,33 113,24 C107,31 95,35 83,35 C82,45 81,55 84,58 C82,67 74,71 67,67 C71,58 74,46 73,33 C72,15 83,3 100,3 Z"
female_lines = lines_mirrored([
    ((99, 86), [((92, 88), (85, 87), (77, 85))]),                 # clavicle
    ((70, 120), [((70, 138), (84, 148), (98, 138))]),             # under-bust
    ((76, 200), [((84, 216), (91, 232), (97, 246))]),             # hip line
    ((80, 332), [((84, 338), (89, 338), (92, 333))]),             # knee
])

figures = {
    "male": dict(body=male_body, arm=closed(male_arm_start, male_arm), arm2=mirror_closed(male_arm_start, male_arm),
                 head=male_head, hair=male_hair, lines=male_lines, navel=(100, 196)),
    "female": dict(body=female_body, arm=closed(female_arm_start, female_arm), arm2=mirror_closed(female_arm_start, female_arm),
                   head=female_head, hair=female_hair, lines=female_lines, navel=(100, 192)),
}
# zones (viewer-left = person's right)
zones = {
    "male": {"ABDOMEN_RIGHT": (84, 192), "THIGH_RIGHT": (82, 290), "ARM_RIGHT": (44, 128)},
    "female": {"ABDOMEN_RIGHT": (85, 190), "THIGH_RIGHT": (80, 292), "ARM_RIGHT": (50, 128)},
}
json.dump({"figures": figures, "zones": zones}, open("figures.json", "w"), indent=1)

def svg(name, f, z, x0):
    base, light, dark = "#C9D6D1", "#EEF4F1", "#8FA39C"
    g = f'''
<g transform="translate({x0},0)">
<defs>
 <linearGradient id="b{name}" x1="55" x2="145" gradientUnits="userSpaceOnUse">
  <stop offset="0" stop-color="{dark}"/><stop offset="0.28" stop-color="{base}"/><stop offset="0.5" stop-color="{light}"/><stop offset="0.72" stop-color="{base}"/><stop offset="1" stop-color="{dark}"/></linearGradient>
 <linearGradient id="a{name}" x1="30" x2="68" gradientUnits="userSpaceOnUse">
  <stop offset="0" stop-color="{dark}"/><stop offset="0.45" stop-color="{light}"/><stop offset="1" stop-color="{base}"/></linearGradient>
 <linearGradient id="a2{name}" x1="132" x2="170" gradientUnits="userSpaceOnUse">
  <stop offset="0" stop-color="{base}"/><stop offset="0.55" stop-color="{light}"/><stop offset="1" stop-color="{dark}"/></linearGradient>
 <radialGradient id="h{name}" cx="96" cy="26" r="34" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="{light}"/><stop offset="1" stop-color="{dark}"/></radialGradient>
</defs>
<ellipse cx="100" cy="436" rx="40" ry="5" fill="#00000018"/>
<g stroke="#7E918A" stroke-width="2.2" stroke-linejoin="round">
<path d="{f['arm']}" fill="url(#a{name})"/><path d="{f['arm2']}" fill="url(#a2{name})"/>
<path d="{f['body']}" fill="url(#b{name})"/>
</g>
<path d="{f['arm']}" fill="url(#a{name})"/><path d="{f['arm2']}" fill="url(#a2{name})"/>
<path d="{f['head']}" fill="url(#h{name})" stroke="#7E918A" stroke-width="2.2"/>
<path d="{f['hair']}" fill="#55665F"/>
<g fill="none" stroke="#7E918A" stroke-opacity="0.55" stroke-width="1.3" stroke-linecap="round">
{''.join(f'<path d="{d}"/>' for d in f['lines'])}
</g>
<ellipse cx="{f['navel'][0]}" cy="{f['navel'][1]}" rx="1.6" ry="2.2" fill="#7E918A" fill-opacity="0.6"/>
'''
    for site, (x, y) in z.items():
        for xx in (x, 200 - x):
            g += f'<circle cx="{xx}" cy="{y}" r="9" fill="#00897B33" stroke="#00897B" stroke-width="2"/>'
    return g + "</g>"

doc = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 420 445" width="840" height="890" style="background:#fff">
{svg("m", figures["male"], zones["male"], 0)}{svg("f", figures["female"], zones["female"], 210)}
</svg>'''
open("preview.svg", "w").write(doc)
open("preview.html", "w").write(f"<html><body style='margin:0'>{doc}</body></html>")
