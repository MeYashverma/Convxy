#!/usr/bin/env python3
"""Build the Convxy launcher set from app_icons/Convxy.png.

Cleans JPEG-style noise in the near-black field, squares the mark, then
writes every density Android expects plus the website / Play Store assets.
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageFilter, ImageOps

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "app_icons" / "Convxy.png"
RES = ROOT / "app" / "src" / "main" / "res"
DOCS = ROOT / "docs"

# Adaptive-icon foreground is 108dp; the safe zone is the inner 72dp.
# We keep ~18% padding so the C is never cropped by the launcher mask.
DENSITIES = {
    "mdpi": 1,
    "hdpi": 1.5,
    "xhdpi": 2,
    "xxhdpi": 3,
    "xxxhdpi": 4,
}


def clean_and_square(src: Path) -> Image.Image:
    img = Image.open(src).convert("RGBA")
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            luma = 0.2126 * r + 0.7152 * g + 0.0722 * b
            chroma = max(r, g, b) - min(r, g, b)
            # Compression speckles live in the near-black field.
            if luma < 26 or (luma < 46 and chroma < 42):
                px[x, y] = (0, 0, 0, 255)

    # Square on a true-black canvas, then add a little breathing room so the
    # C sits comfortably inside Android's adaptive-icon safe zone.
    side = max(w, h)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 255))
    canvas.paste(img, ((side - w) // 2, (side - h) // 2), img)

    padded = int(side * 1.08)
    square = Image.new("RGBA", (padded, padded), (0, 0, 0, 255))
    square.paste(canvas, ((padded - side) // 2, (padded - side) // 2), canvas)
    return square


def resize(img: Image.Image, size: int) -> Image.Image:
    return img.resize((size, size), Image.Resampling.LANCZOS)


def monochrome(img: Image.Image, size: int) -> Image.Image:
    """White silhouette of the C on a transparent field (themed icons)."""
    gray = ImageOps.grayscale(img)
    # Anything brighter than the black field becomes white; keep alpha.
    mask = gray.point(lambda p: 255 if p > 18 else 0)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    white = Image.new("RGBA", img.size, (255, 255, 255, 255))
    out.paste(white, mask=mask)
    return resize(out, size)


def solid(color: tuple[int, int, int, int], size: int) -> Image.Image:
    return Image.new("RGBA", (size, size), color)


def save(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, "PNG", optimize=True)
    print(f"  wrote {path.relative_to(ROOT)} ({img.size[0]}x{img.size[1]})")


def main() -> None:
    if not SRC.exists():
        raise SystemExit(f"missing source icon: {SRC}")
    print(f"source: {SRC}")
    mark = clean_and_square(SRC)

    # Master copies
    save(resize(mark, 1024), ROOT / "app_icons" / "Convxy-1024.png")
    save(resize(mark, 512), ROOT / "app" / "src" / "main" / "ic_launcher-playstore.png")
    save(resize(mark, 512), RES / "drawable" / "icon.png")
    save(resize(mark, 432), RES / "drawable" / "ic_launcher_foreground.png")
    save(monochrome(mark, 432), RES / "drawable" / "ic_launcher_monochrome.png")
    save(resize(mark, 256), DOCS / "icon.png")

    for name, scale in DENSITIES.items():
        folder = RES / f"mipmap-{name}"
        launcher = int(48 * scale)
        fg = int(108 * scale)
        save(resize(mark, launcher), folder / "ic_launcher.png")
        save(resize(mark, launcher), folder / "ic_launcher_round.png")
        save(resize(mark, fg), folder / "ic_launcher_foreground.png")
        save(solid((0, 0, 0, 255), fg), folder / "ic_launcher_background.png")
        save(monochrome(mark, fg), folder / "ic_launcher_monochrome.png")


if __name__ == "__main__":
    main()
