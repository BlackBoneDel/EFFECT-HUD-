#!/usr/bin/env python3
"""Ilustración de galería para Modrinth — Potion Effect (by Denvdrite).

NO es una captura de pantalla: ninguna persona puede ejecutar Minecraft en
este entorno. Es una ILUSTRACIÓN dibujada píxel a píxel con Pillow (todo el
arte es original de este script: sin texturas de Mojang ni de otros mods).

Reproduce el estilo "Tarjeta limpia" del mod: panel oscuro con borde, barra de
acento por efecto con su color vívido automático, temporizador con colores por
tiempo restante (rojo <=10 s, amarillo <=30 s, blanco >30 s, cian infinito) y
sombra de texto 1px (opción `textShadow`, default ON en v1.2).

Salidas (rama modrinth-assets, carpeta docs/modrinth/):
  - docs/modrinth/gallery-main.png         1280x720
  - docs/modrinth/gallery-main-preview.png  640x360 (nearest)

Uso:  pip3 install --break-system-packages pillow   (una vez)
      python3 tools/generate_gallery_image.py
"""
from PIL import Image, ImageDraw, ImageFont
import pathlib
import random

W, H = 1280, 720
BLOCK = 64
random.seed(1207)  # determinista

# ----------------------------------------------------------------------------- paleta
SKY_TOP = (16, 20, 48)
SKY_MID = (44, 40, 92)
SKY_HORIZON = (176, 84, 44)
SUN = (255, 214, 90)
CLOUD = (208, 214, 234)
GRASS = (62, 124, 49)
GRASS_DARK = (44, 96, 36)
DIRT = (122, 82, 48)
DIRT_DARK = (98, 64, 36)
STONE = (110, 110, 110)
STONE_DARK = (86, 86, 88)
ORE_GOLD = (234, 184, 30)
ORE_IRON = (196, 150, 116)

ACCENT = {
    "regen": (72, 255, 38),     # #48FF26  (docs/vivid-colors.md)
    "speed": (32, 255, 219),    # #20FFDB
    "haste": (242, 184, 30),   # #F2B81E
    "nv": (77, 157, 255),      # #4D9DFF
    "weak": (214, 235, 235),    # #D6EBEB
}
TIME_RED = (255, 59, 59)        # #FF3B3B  <=10 s
TIME_YELLOW = (255, 230, 0)     # #FFE600  <=30 s
TIME_NEUTRAL = (255, 255, 255)  # #FFFFFF  >30 s
TIME_INFINITY = (0, 229, 255)   # #00E5FF  permanente


def lerp(c1, c2, t):
    return tuple(round(a + (b - a) * t) for a, b in zip(c1, c2))


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c)


# ----------------------------------------------------------------------------- fondo
img = Image.new("RGB", (W, H), SKY_TOP)
dr = ImageDraw.Draw(img, "RGBA")

horizon = int(H * 0.66)
for by in range(0, horizon, BLOCK):
    t = by / horizon
    col = lerp(SKY_TOP, SKY_MID, min(1.0, t * 1.6)) if t < 0.62 else lerp(SKY_MID, SKY_HORIZON, (t - 0.62) / 0.38)
    for bx in range(0, W, BLOCK):
        noise = random.randint(-6, 6)
        c = tuple(max(0, min(255, v + noise)) for v in col)
        dr.rectangle([bx, by, bx + BLOCK, by + BLOCK], fill=c)

# sol bajo (2x2 bloques calientes) y estrellas cuadradas
dr.rectangle([150, horizon - 3 * BLOCK, 150 + 2 * BLOCK, horizon - BLOCK], fill=SUN)
dr.rectangle([150 - 12, horizon - 3 * BLOCK - 12, 150 + 2 * BLOCK + 12, horizon - BLOCK + 12], fill=shade(SUN, 0.55))
for _ in range(90):
    x, y = random.randrange(0, W), random.randrange(0, int(H * 0.42))
    s = random.choice([2, 2, 3])
    dr.rectangle([x, y, x + s, y + s], fill=(235, 238, 250, random.randint(120, 235)))

# nubes: racimos de rectángulos
for cx, cy, wdt in [(420, 120, 210), (900, 180, 260), (1140, 90, 150)]:
    for i in range(5):
        x = cx + i * (wdt // 5)
        y = cy + random.choice([0, 10, -8, 4])
        dr.rectangle([x, y, x + wdt // 4, y + 26], fill=shade(CLOUD, 0.85))
        dr.rectangle([x, y - 10, x + wdt // 6, y + 8], fill=CLOUD)

# terreno: hierba, tierra, piedra con menas, todo en bloques
for bx in range(0, W, BLOCK):
    top = horizon + (0 if (bx // BLOCK) % 3 else BLOCK // 2)  # colinas de media losa
    dr.rectangle([bx, top, bx + BLOCK, top + BLOCK], fill=GRASS)
    dr.rectangle([bx, top, bx + BLOCK, top + 14], fill=shade(GRASS, 1.18))
    for gy in range(1, 3):
        dr.rectangle([bx, top + BLOCK * gy, bx + BLOCK, top + BLOCK * (gy + 1)], fill=DIRT if gy == 1 else shade(DIRT, 0.9))
    dr.rectangle([bx, top + BLOCK * 3, bx + BLOCK, H], fill=STONE)
    # textura simple de bloques (líneas oscuras + motas)
    dr.rectangle([bx, top + BLOCK, bx + BLOCK, top + BLOCK + 2], fill=shade(DIRT, 0.75))
    dr.rectangle([bx, top + BLOCK * 2, bx + BLOCK, top + BLOCK * 2 + 2], fill=shade(DIRT_DARK, 0.8))
    dr.rectangle([bx, top + BLOCK * 3, bx + BLOCK, top + BLOCK * 3 + 2], fill=STONE_DARK)
    for _ in range(3):
        rx, ry = bx + random.randrange(6, BLOCK - 6), top + BLOCK * 3 + random.randrange(10, BLOCK - 10)
        dr.rectangle([rx, ry, rx + 6, ry + 6], fill=shade(STONE_DARK, 0.85))
    if random.random() < 0.28:
        ox, oy = bx + random.randrange(8, BLOCK - 16), top + BLOCK * 3 + random.randrange(14, BLOCK - 14)
        dr.rectangle([ox, oy, ox + 10, oy + 8], fill=ORE_GOLD if random.random() < 0.5 else ORE_IRON)

# ----------------------------------------------------------------------------- HUD
# filas: (nombre, nivel, tiempo, color_acento, tiempo_color, glifo)
rows = [
    ("Regeneration", "", "2:15", ACCENT["regen"], TIME_NEUTRAL, "plus"),
    ("Speed", "II", "0:42", ACCENT["speed"], TIME_NEUTRAL, "up2"),
    ("Haste", "", "\u221e", ACCENT["haste"], TIME_INFINITY, "pick"),
    ("Night Vision", "", "5:03", ACCENT["nv"], TIME_NEUTRAL, "eye"),
    ("Weakness", "", "0:08", ACCENT["weak"], TIME_RED, "down"),
]

def glyph(d, kind, x, y, s, col):
    """Glifos dibujados a mano (2 px de trazo), dentro de una celda s x s."""
    u = s / 16.0
    t = max(2, round(2 * u))
    cx, cy = x + s / 2, y + s / 2
    if kind == "plus":
        d.rectangle([cx - u, y + 3 * u, cx + u, y + 13 * u], fill=col)
        d.rectangle([x + 3 * u, cy - u, x + 13 * u, cy + u], fill=col)
    elif kind == "up2":
        for i, oy in enumerate((10 * u, 4 * u)):
            d.polygon([(x + 3 * u, y + oy), (cx, y + oy - 4 * u), (x + 13 * u, y + oy),
                       (x + 11 * u, y + oy), (cx, y + oy - 2 * u), (x + 5 * u, y + oy)], fill=col)
    elif kind == "pick":
        d.line([x + 3 * u, y + 12 * u, x + 12 * u, y + 4 * u], fill=col, width=t)
        d.arc([x + 2 * u, y + 1 * u, x + 14 * u, y + 9 * u], start=200, end=340, fill=col)
    elif kind == "eye":
        d.ellipse([x + 2 * u, cy - 4 * u, x + 14 * u, cy + 4 * u], outline=col, width=t)
        d.ellipse([cx - 2 * u, cy - 2 * u, cx + 2 * u, cy + 2 * u], fill=col)
    elif kind == "down":
        d.polygon([(x + 3 * u, y + 5 * u), (cx, y + 12 * u), (x + 13 * u, y + 5 * u),
                   (x + 11 * u, y + 5 * u), (cx, y + 9.5 * u), (x + 5 * u, y + 5 * u)], fill=col)


def text(d, x, y, s, font, fill):
    """Texto con sombra de 1 px (como textShadow del mod)."""
    d.text((x + 1, y + 1), s, font=font, fill=(0, 0, 0))
    d.text((x, y), s, font=font, fill=fill)


FB = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FM = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"
f_name = ImageFont.truetype(FB, 21)
f_time = ImageFont.truetype(FM, 22)
f_cap = ImageFont.truetype(FM, 13)

ICON = 30
ROW_H = 44
PAD = 14
panel_w, panel_h = 430, len(rows) * ROW_H + PAD * 2 - 8
px = W - panel_w - 26
py = (H - panel_h) // 2

dr.rounded_rectangle([px, py, px + panel_w, py + panel_h], radius=5,
                     fill=(9, 13, 18, 178), outline=(255, 255, 255, 90), width=2)
for i, (name, lvl, timer, accent, tcol, g) in enumerate(rows):
    ry = py + PAD + i * ROW_H
    textx = px + PAD + ICON + 12
    dr.rectangle([px + 6, ry - 3, px + 10, ry + ROW_H - 12], fill=accent)          # acento CLEAN_CARD
    dr.rounded_rectangle([textx, ry + 24, px + panel_w - PAD, ry + 25], fill=(255, 255, 255, 34))  # separador
    # icono: los sprites de efecto del juego NO se tiñen (regla del mod); aquí un
    # cuadro con glifo propio en el color automático vívido del efecto
    dr.rectangle([px + PAD, ry, px + PAD + ICON, ry + ICON], fill=(12, 16, 22, 255))
    dr.rectangle([px + PAD - 1, ry - 1, px + PAD + ICON + 1, ry + ICON + 1], outline=shade(accent, 0.55), width=2)
    glyph(dr, g, px + PAD + 1, ry + 1, ICON - 2, accent)
    label = f"{name} {lvl}".rstrip()
    text(dr, textx, ry + 3, label, f_name, accent)
    tw = dr.textlength(timer, font=f_time)
    text(dr, px + panel_w - PAD - tw, ry + 2, timer, f_time, tcol)

cap = "Illustration — Potion Effect by Denvdrite"
cw = ImageDraw.Draw(img).textlength(cap, font=f_cap)
dr.rectangle([W - cw - 16, H - 26, W, H], fill=(0, 0, 0, 150))
dr.text((W - cw - 8, H - 22), cap, font=f_cap, fill=(210, 216, 228))

out = pathlib.Path("docs/modrinth")
out.mkdir(parents=True, exist_ok=True)
img.save(out / "gallery-main.png", optimize=True)
img.resize((640, 360), Image.Resampling.NEAREST).save(out / "gallery-main-preview.png", optimize=True)
print("OK: docs/modrinth/gallery-main.png", img.size, "+ preview 640x360")
