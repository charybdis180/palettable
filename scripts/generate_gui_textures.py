"""Generate default Palettable GUI textures (editable PNGs in assets)."""
import math
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    raise SystemExit("Install Pillow first: pip install pillow")

OUT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "assets" / "palettable" / "textures" / "gui"
OUT.mkdir(parents=True, exist_ok=True)

PANEL = (198, 198, 198)
HIGHLIGHT = (255, 255, 255)
SHADOW = (85, 85, 85)
INSET_BG = (139, 139, 139)
INSET_DARK = (55, 55, 55)
TAB_INACTIVE = (160, 160, 160)
BUTTON_BG = (172, 172, 172)


def save(name: str, image: Image.Image) -> None:
    path = OUT / name
    image.save(path)
    print(f"Wrote {path} ({image.size[0]}x{image.size[1]})")


def fill_rect(img, x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), color + (255,))


def make_panel(size=24):
    img = Image.new("RGBA", (size, size), PANEL + (255,))
    fill_rect(img, 0, 0, size, 1, HIGHLIGHT)
    fill_rect(img, 0, 0, 1, size, HIGHLIGHT)
    fill_rect(img, 0, size - 1, size, size, SHADOW)
    fill_rect(img, size - 1, 0, size, size, SHADOW)
    return img


def make_inset(size=18):
    img = Image.new("RGBA", (size, size), INSET_BG + (255,))
    fill_rect(img, 0, 0, size, 1, INSET_DARK)
    fill_rect(img, 0, 0, 1, size, INSET_DARK)
    fill_rect(img, 0, size - 1, size, size, HIGHLIGHT)
    fill_rect(img, size - 1, 0, size, size, HIGHLIGHT)
    return img


def make_inset_sized(width, height):
    img = Image.new("RGBA", (width, height), INSET_BG + (255,))
    fill_rect(img, 0, 0, width, 1, INSET_DARK)
    fill_rect(img, 0, 0, 1, height, INSET_DARK)
    fill_rect(img, 0, height - 1, width, height, HIGHLIGHT)
    fill_rect(img, width - 1, 0, width, height, HIGHLIGHT)
    return img


def make_tab(active: bool, size=28):
    bg = PANEL if active else TAB_INACTIVE
    img = Image.new("RGBA", (size, size), bg + (255,))
    fill_rect(img, 0, 0, size, 1, HIGHLIGHT)
    fill_rect(img, 0, 0, 1, size, HIGHLIGHT)
    fill_rect(img, 0, size - 1, size, size, SHADOW)
    if active:
        fill_rect(img, size - 1, 1, size, size - 1, PANEL)
    else:
        fill_rect(img, size - 1, 0, size, size, SHADOW)
    return img


def make_button(size_w, size_h, symbol=None):
    img = Image.new("RGBA", (size_w, size_h), BUTTON_BG + (255,))
    fill_rect(img, 0, 0, size_w, 1, HIGHLIGHT)
    fill_rect(img, 0, 0, 1, size_h, HIGHLIGHT)
    fill_rect(img, 0, size_h - 1, size_w, size_h, SHADOW)
    fill_rect(img, size_w - 1, 0, size_w, size_h, SHADOW)
    if symbol == "+":
        cx, cy = size_w // 2, size_h // 2
        fill_rect(img, cx - 3, cy, cx + 4, cy + 1, INSET_DARK)
        fill_rect(img, cx, cy - 3, cx + 1, cy + 4, INSET_DARK)
    elif symbol == "-":
        cx, cy = size_w // 2, size_h // 2
        fill_rect(img, cx - 3, cy, cx + 4, cy + 1, INSET_DARK)
    return img


def make_hotbar_slot(size=20):
    """Vanilla-sized hotbar cell (20x20); darker than inventory slots."""
    bg = (55, 55, 55)
    dark = (25, 25, 25)
    img = Image.new("RGBA", (size, size), bg + (255,))
    fill_rect(img, 0, 0, size, 1, dark)
    fill_rect(img, 0, 0, 1, size, dark)
    fill_rect(img, 0, size - 1, size, size, (85, 85, 85))
    fill_rect(img, size - 1, 0, size, size, (85, 85, 85))
    return img


def put_pixel(img, x, y, color):
    if 0 <= x < img.size[0] and 0 <= y < img.size[1]:
        img.putpixel((x, y), color + (255,))


def make_action_button(bg, icon):
    """13x13 confirm/delete buttons used in the saved-palette list and delete popup."""
    size = 13
    img = Image.new("RGBA", (size, size), bg + (255,))
    white = (255, 255, 255)
    if icon == "cross":
        ox, oy = 4, 4
        for px, py in (
            (0, 0), (4, 0),
            (1, 1), (3, 1),
            (2, 2),
            (1, 3), (3, 3),
            (0, 4), (4, 4),
        ):
            put_pixel(img, ox + px, oy + py, white)
    elif icon == "check":
        ox, oy = 3, 4
        for px, py in (
            (4, 0), (5, 0),
            (3, 1), (4, 1),
            (2, 2), (3, 2),
            (1, 3), (2, 3),
            (0, 4), (1, 4), (2, 4), (3, 4), (4, 4), (5, 4), (6, 4),
        ):
            put_pixel(img, ox + px, oy + py, white)
    return img


def make_tab_icon_gear(size=16):
    """16x16 gear for the Settings tab (dark pixels on transparent)."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    dark = (55, 55, 55)
    mid = (85, 85, 85)
    cx, cy = size // 2, size // 2
    for y in range(size):
        for x in range(size):
            dx, dy = x - cx, y - cy
            dist = (dx * dx + dy * dy) ** 0.5
            angle = math.atan2(dy, dx)
            # eight teeth + hub
            tooth = abs(math.cos(4 * angle))
            outer = 6.2 + 1.8 * tooth
            inner = 2.8
            if inner <= dist <= outer:
                img.putpixel((x, y), (mid if dist > 5.0 else dark) + (255,))
            elif dist < inner:
                img.putpixel((x, y), dark + (255,))
    return img


if __name__ == "__main__":
    save("panel.png", make_panel())
    save("text_field.png", make_inset())
    save("slot.png", make_inset())
    save("tab_active.png", make_tab(True))
    save("tab_inactive.png", make_tab(False))
    save("button.png", make_button(20, 16))
    save("button_zoom_in.png", make_button(16, 16, symbol="+"))
    save("button_zoom_out.png", make_button(16, 16, symbol="-"))
    save("slider_track.png", make_inset_sized(20, 16))
    save("slider_handle.png", make_button(8, 16))
    save("hotbar_slot.png", make_hotbar_slot(20))
    save("action_confirm.png", make_action_button((48, 139, 48), "check"))
    save("action_delete.png", make_action_button((139, 48, 48), "cross"))
    save("tab_icon_gear.png", make_tab_icon_gear())
