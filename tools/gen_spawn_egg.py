"""Generates the shared, tintable spawn egg template (original pixel art, stdlib only).

Run from the project root:  python tools/gen_spawn_egg.py
Outputs into src/main/resources/assets/dirtcheapgolems/textures/item/:
  golem_spawn_egg_base.png     layer0 - egg shell in light grays (tinted with the shell color)
  golem_spawn_egg_overlay.png  layer1 - pebble-shaped spots in light grays (tinted with the spot color)

Both layers are grayscale on purpose: item tints MULTIPLY the texture color, so dark
pixels would crush every tint. Shading lives in the gray values.
"""
import os
import struct
import zlib

OUT = os.path.join("src", "main", "resources", "assets", "dirtcheapgolems", "textures", "item")

# Egg silhouette: row -> (first_x, last_x), inclusive. Narrower at the top, like an egg.
EGG_ROWS = {
    1: (6, 9), 2: (5, 10), 3: (4, 11), 4: (4, 11),
    5: (3, 12), 6: (3, 12), 7: (3, 12), 8: (3, 12), 9: (3, 12), 10: (3, 12), 11: (3, 12), 12: (3, 12),
    13: (4, 11), 14: (6, 9),
}

# Shell shades (gray levels): light from the upper left.
OUTLINE = 118
SHADES = [255, 236, 214, 190]  # highlight, light, mid, shadow

# Pebble-shaped spots: list of (x, y). The last pixel of each pebble is its shaded edge.
PEBBLES = [
    [(8, 3), (9, 3), (8, 4), (9, 4)],
    [(4, 7), (5, 7), (4, 8), (5, 8)],
    [(9, 6), (10, 6), (9, 7), (10, 7), (11, 7)],
    [(6, 10), (7, 10), (6, 11), (7, 11)],
    [(10, 11), (11, 11), (10, 12)],
]
SPOT_LIGHT, SPOT_SHADE = 245, 200


def in_egg(x, y):
    r = EGG_ROWS.get(y)
    return r is not None and r[0] <= x <= r[1]


def is_outline(x, y):
    return in_egg(x, y) and any(not in_egg(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))


def shell_shade(x, y):
    # Diagonal gradient: 0 at upper left, 1 at lower right of the egg box (x 3..12, y 1..14).
    t = ((x - 3) / 9 + (y - 1) / 13) / 2
    if (x, y) in ((5, 3), (5, 4), (6, 3), (4, 5)):
        return SHADES[0]
    if t < 0.38:
        return SHADES[1]
    if t < 0.68:
        return SHADES[2]
    return SHADES[3]


def write_png(path, pixels, w=16, h=16):
    raw = b"".join(b"\x00" + b"".join(struct.pack("4B", *pixels[y * w + x]) for x in range(w)) for y in range(h))

    def chunk(tag, data):
        c = tag + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def main():
    os.makedirs(OUT, exist_ok=True)
    clear = (0, 0, 0, 0)

    base = []
    for y in range(16):
        for x in range(16):
            if not in_egg(x, y):
                base.append(clear)
            else:
                g = OUTLINE if is_outline(x, y) else shell_shade(x, y)
                base.append((g, g, g, 255))
    write_png(os.path.join(OUT, "golem_spawn_egg_base.png"), base)

    overlay = [clear] * 256
    for pebble in PEBBLES:
        for i, (x, y) in enumerate(pebble):
            assert in_egg(x, y) and not is_outline(x, y), (x, y)
            g = SPOT_SHADE if i == len(pebble) - 1 else SPOT_LIGHT
            overlay[y * 16 + x] = (g, g, g, 255)
    write_png(os.path.join(OUT, "golem_spawn_egg_overlay.png"), overlay)
    print("wrote golem_spawn_egg_base.png and golem_spawn_egg_overlay.png to", OUT)


if __name__ == "__main__":
    main()
