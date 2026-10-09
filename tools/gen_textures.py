"""Generates the mod's placeholder textures (original procedural art, stdlib only).

Run from the project root:  python tools/gen_textures.py
Outputs into src/main/resources/assets/dirtcheapgolems/textures/.

UV layout of CheapGolemModel (64x64 texture, box UV, see client/CheapGolemModel.java):
  head      6x5x5 at (0, 0)     body      8x6x5 at (0, 16)
  right_arm 3x8x3 at (32, 0)    left_arm  3x8x3 at (32, 16)
  right_leg 3x4x3 at (48, 0)    left_leg  3x4x3 at (48, 16)
"""
import os
import random
import struct
import zlib

ROOT = os.path.join("src", "main", "resources", "assets", "dirtcheapgolems", "textures")

# (u, v, w, h, d) of every box in the model
BOXES = [
    (0, 0, 6, 5, 5),    # head
    (0, 16, 8, 6, 5),   # body
    (32, 0, 3, 8, 3),   # right arm
    (32, 16, 3, 8, 3),  # left arm
    (48, 0, 3, 4, 3),   # right leg
    (48, 16, 3, 4, 3),  # left leg
]
HEAD_FRONT = (5, 5)  # top-left of the head's front face (u + d, v + d)

PALETTES = {
    "dirt": [(134, 96, 67), (121, 85, 58), (150, 108, 74), (108, 76, 52), (94, 66, 45)],
    "sand": [(219, 207, 163), (227, 216, 175), (206, 193, 149), (196, 180, 134), (232, 224, 189)],
    "netherrack": [(111, 54, 52), (97, 38, 38), (130, 66, 63), (84, 31, 31), (140, 76, 72)],
}
EYES = {"dirt": (34, 22, 14), "sand": (92, 74, 40), "netherrack": (255, 170, 40)}


def write_png(path, width, height, pixels):
    """pixels: list of rows, each a list of (r, g, b, a)."""
    raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in pixels)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def blank(w, h):
    return [[(0, 0, 0, 0) for _ in range(w)] for _ in range(h)]


def box_pixels(u, v, w, h, d):
    """All texture pixels used by a box with box UV."""
    for y in range(v, v + d):              # top and bottom faces
        for x in range(u + d, u + d + 2 * w):
            yield x, y
    for y in range(v + d, v + d + h):      # side faces
        for x in range(u, u + 2 * d + 2 * w):
            yield x, y


def used_pixels():
    return {p for box in BOXES for p in box_pixels(*box)}


def golem_skin(material, rng):
    img = blank(64, 64)
    palette = PALETTES[material]
    for x, y in used_pixels():
        img[y][x] = (*rng.choice(palette), 255)
    ex, ey = HEAD_FRONT
    for dx in (1, 4):                       # two eyes on the head front
        img[ey + 2][ex + dx] = (*EYES[material], 255)
    if material == "netherrack":
        for x, y in used_pixels():          # glowing veins
            if rng.random() < 0.05:
                img[y][x] = (190, 70, 30, 255)
    return img


def emissive_layer(rng):
    img = blank(64, 64)
    ex, ey = HEAD_FRONT
    for dx in (1, 4):
        img[ey + 2][ex + dx] = (255, 190, 60, 255)
    for x, y in used_pixels():
        if rng.random() < 0.05:
            img[y][x] = (255, 110, 30, 255)
    return img


def cracks(amount, rng):
    img = blank(64, 64)
    pixels = sorted(used_pixels())
    for _ in range(amount):
        x, y = rng.choice(pixels)
        for _ in range(rng.randint(3, 7)):  # short random-walk crack
            if (x, y) in used_pixels_set:
                img[y][x] = (40, 30, 25, 230)
            x += rng.choice((-1, 0, 1))
            y += rng.choice((0, 1))
    return img


def item_core():
    img = blank(16, 16)
    clay, clay_dark, red, red_light = (160, 166, 179, 255), (126, 131, 145, 255), (170, 20, 20, 255), (255, 60, 50, 255)
    for y in range(16):
        for x in range(16):
            dist = abs(x - 7.5) + abs(y - 7.5)
            if dist <= 7:
                img[y][x] = clay_dark if dist > 5.5 else clay
            if dist <= 3:
                img[y][x] = red
            if dist <= 1.5:
                img[y][x] = red_light
    return img


def spawn_egg(base, spots, rng):
    img = blank(16, 16)
    for y in range(16):
        for x in range(16):
            if ((x - 7.5) / 5.5) ** 2 + ((y - 8.5) / 7.0) ** 2 <= 1.0:
                img[y][x] = (*(spots if rng.random() < 0.25 else base), 255)
    return img


def sand_in_eyes_icon(rng):
    img = blank(18, 18)
    for y in range(18):
        for x in range(18):
            if ((x - 8.5) / 8) ** 2 + ((y - 8.5) / 4.5) ** 2 <= 1.0:
                img[y][x] = (240, 240, 230, 255)
            if (x - 8.5) ** 2 + (y - 8.5) ** 2 <= 9:
                img[y][x] = (60, 40, 20, 255)
    for _ in range(30):
        x, y = rng.randrange(18), rng.randrange(18)
        img[y][x] = (219, 207, 163, 255)
    return img


used_pixels_set = used_pixels()

if __name__ == "__main__":
    rng = random.Random(1234)
    for material in PALETTES:
        write_png(os.path.join(ROOT, "entity", f"{material}_golem.png"), 64, 64, golem_skin(material, rng))
    write_png(os.path.join(ROOT, "entity", "netherrack_golem_glow.png"), 64, 64, emissive_layer(rng))
    write_png(os.path.join(ROOT, "entity", "cracks_low.png"), 64, 64, cracks(12, rng))
    write_png(os.path.join(ROOT, "entity", "cracks_high.png"), 64, 64, cracks(40, rng))
    write_png(os.path.join(ROOT, "item", "golem_core.png"), 16, 16, item_core())
    eggs = {"dirt": ((134, 96, 67), (89, 125, 39)), "sand": ((219, 207, 163), (150, 120, 70)),
            "netherrack": ((111, 54, 52), (255, 150, 40))}
    for material, (base, spots) in eggs.items():
        write_png(os.path.join(ROOT, "item", f"{material}_golem_spawn_egg.png"), 16, 16, spawn_egg(base, spots, rng))
    write_png(os.path.join(ROOT, "mob_effect", "sand_in_eyes.png"), 18, 18, sand_in_eyes_icon(rng))
    print("textures written to", ROOT)
