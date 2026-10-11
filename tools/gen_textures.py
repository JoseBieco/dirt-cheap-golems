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
# end_stone is generated last with its own RNG so adding it never changes the other textures.
END_STONE_PALETTE = [(219, 222, 158), (207, 210, 144), (228, 231, 172), (196, 199, 131), (233, 235, 187)]
# gravel, like end_stone, uses its own RNG so the existing textures stay byte-identical.
GRAVEL_PALETTE = [(131, 127, 126), (118, 114, 114), (145, 141, 140), (100, 96, 96), (156, 152, 150)]
# basalt/deepslate: one dark grey-blue palette for both source blocks, own RNG as well.
BASALT_PALETTE = [(78, 77, 82), (68, 67, 72), (90, 89, 95), (58, 57, 62), (99, 98, 105)]
EYES = {"basalt": (154, 160, 168), "gravel": (61, 58, 58), "dirt": (34, 22, 14), "sand": (92, 74, 40), "netherrack": (255, 170, 40), "end_stone": (155, 95, 192)}


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
    palette = PALETTES.get(material) or {"gravel": GRAVEL_PALETTE, "basalt": BASALT_PALETTE}.get(material, END_STONE_PALETTE)
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
    # Spawn eggs: see tools/gen_spawn_egg.py (one tinted template shared by all golems).
    write_png(os.path.join(ROOT, "mob_effect", "sand_in_eyes.png"), 18, 18, sand_in_eyes_icon(rng))
    write_png(os.path.join(ROOT, "entity", "end_stone_golem.png"), 64, 64, golem_skin("end_stone", random.Random(5678)))
    write_png(os.path.join(ROOT, "entity", "gravel_golem.png"), 64, 64, golem_skin("gravel", random.Random(9012)))
    write_png(os.path.join(ROOT, "entity", "basalt_golem.png"), 64, 64, golem_skin("basalt", random.Random(3456)))
    print("textures written to", ROOT)
