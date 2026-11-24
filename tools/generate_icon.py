#!/usr/bin/env python3
"""
Generador del icono de Potion Effect.

Dibuja una poción arrojadizable ORIGINAL sobre una cuadrícula lógica de 16×16
píxeles (sin texturas de Mojang ni de otros mods) y la escala a 128×128 con
nearest-neighbor (cada píxel lógico = bloque de 8×8 idéntico), de modo que el
icono tiene el aspecto auténtico de un sprite de Minecraft: bordes duros, sin
suavizado ni degradados. Fondo transparente con margen lógico ≥ 1 px.

Requisitos:
  - frasco de panza redondeada, cuello estrecho, labio y tapón de corcho;
  - líquido rosa con 3 tonos (claro arriba) y vidrio semitransparente;
  - 2-3 píxeles blancos de reflejo arriba-izquierda;
  - contorno de 1 px casi negro azulado; 3 gotitas splash;
  - paleta de ≤ 12 colores opacos.

Uso:  python3 tools/generate_icon.py
Salidas:
  src/main/resources/assets/effecttimer/icon.png   (128×128, RGBA)
  docs/icon-preview.png                             (512×512 para revisión)

Depende solo de Pillow (pip install pillow).
"""
from pathlib import Path

from PIL import Image

SIZE = 16
SCALE_ICON = 8        # 16 * 8 = 128
SCALE_PREVIEW = 32     # 16 * 32 = 512

TRANSPARENT = (0, 0, 0, 0)
PALETTE = {
    "O": (16, 24, 42, 255),        # contorno azul-negruzco
    "G": (215, 228, 238, 235),    # vidrio claro (semitransparente)
    "g": (175, 198, 214, 235),    # vidrio secundario (bordes)
    "W": (255, 255, 255, 255),    # reflejos
    "C": (138, 90, 43, 255),      # corcho
    "D": (94, 58, 28, 255),       # corcho oscuro
    "L": (255, 155, 201, 255),    # líquido claro (superior)
    "M": (255, 102, 178, 255),    # líquido medio  #FF66B2
    "K": (217, 64, 143, 255),     # líquido oscuro
}
# Total opacos distintos: 8 (≤ 12 ✓). Las gotitas reutilizan M/L.

# Silueta del frasco por fila (col_izq, col_der) inclusiva.
BODY_ROWS = {
    6: (5, 10),
    7: (3, 12),
    8: (2, 13),
    9: (2, 13),
    10: (2, 13),
    11: (2, 13),
    12: (2, 13),
    13: (2, 13),
    14: (3, 12),
}
NECK_ROWS = {
    1: (7, 8),   # tapa del corcho
    2: (6, 9),   # corcho
    3: (6, 9),   # corcho (sombra)
    4: (5, 10),  # labio del cuello
    5: (6, 9),   # cuello
}
CORK_ROWS = {1, 2, 3}
LIQUID_TOP = 9   # fila superior del líquido


def build_grid() -> list[list[tuple[int, int, int, int]]]:
    grid = [[TRANSPARENT for _ in range(SIZE)] for _ in range(SIZE)]
    silhouette: dict[int, tuple[int, int]] = {}
    silhouette.update(BODY_ROWS)
    silhouette.update(NECK_ROWS)

    def in_sil(y: int, x: int) -> bool:
        if y not in silhouette:
            return False
        lo, hi = silhouette[y]
        return lo <= x <= hi

    for y, (lo, hi) in silhouette.items():
        for x in range(lo, hi + 1):
            edge = (
                not in_sil(y, x - 1)
                or not in_sil(y, x + 1)
                or not in_sil(y - 1, x)
                or not in_sil(y + 1, x)
            )
            if y in CORK_ROWS:
                # el corcho se pinta entero (su borde superior hace de línea)
                grid[y][x] = PALETTE["D"] if (y == 1 or x == lo or x == hi) else PALETTE["C"]
                if y == 1:
                    grid[y][x] = PALETTE["C"] if 7 <= x <= 8 else PALETTE["D"]
            elif edge:
                grid[y][x] = PALETTE["O"]
            elif y >= LIQUID_TOP:
                inner_left = x == lo + 1
                inner_right = x == hi - 1
                if y == LIQUID_TOP:
                    grid[y][x] = PALETTE["L"]
                elif y in (10, 11):
                    grid[y][x] = PALETTE["K"] if inner_right else PALETTE["M"]
                else:
                    grid[y][x] = PALETTE["M"] if inner_left else PALETTE["K"]
            else:
                # zona de vidrio por encima del líquido
                grid[y][x] = PALETTE["g"] if (x == lo + 1 or x == hi - 1) and y >= 6 else PALETTE["G"]

    # reflejos blancos arriba-izquierda del frasco
    for (ry, rx) in [(7, 4), (8, 4), (7, 5)]:
        grid[ry][rx] = PALETTE["W"]

    # gotas splash (conservan el aire de poción arrojadizable)
    grid[2][12] = PALETTE["M"]
    grid[4][13] = PALETTE["L"]
    grid[10][1] = PALETTE["M"]
    return grid


def upscale(grid: list[list[tuple[int, int, int, int]]], factor: int) -> Image.Image:
    big = Image.new("RGBA", (SIZE * factor, SIZE * factor), TRANSPARENT)
    px = big.load()
    for y in range(SIZE):
        for x in range(SIZE):
            color = grid[y][x]
            if color == TRANSPARENT:
                continue
            for dy in range(factor):
                for dx in range(factor):
                    px[x * factor + dx, y * factor + dy] = color
    return big


def main() -> None:
    root = Path(__file__).resolve().parent.parent
    grid = build_grid()
    icon = upscale(grid, SCALE_ICON)
    icon_path = root / "src/main/resources/assets/effecttimer/icon.png"
    icon.save(icon_path)
    preview = upscale(grid, SCALE_PREVIEW)
    preview_path = root / "docs/icon-preview.png"
    preview_path.parent.mkdir(exist_ok=True)
    preview.save(preview_path)
    opaque = {c for row in grid for c in row if c != TRANSPARENT}
    print(f"icon.png: {icon.size[0]}x{icon.size[1]} RGBA -> {icon_path}")
    print(f"icon-preview.png: {preview.size[0]}x{preview.size[1]} -> {preview_path}")
    print(f"colores opacos: {len(opaque)} (limite 12)")


if __name__ == "__main__":
    main()
