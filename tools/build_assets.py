#!/usr/bin/env python3
"""
tools/build_assets.py
Generates Enk brand assets, vector drawables, SVGs, and store icon PNG.
"""

import math
import os
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from PIL import Image, ImageDraw

def ensure_dirs():
    os.makedirs("design/src/main/res/drawable", exist_ok=True)
    os.makedirs("app/src/main/res/mipmap-anydpi-v26", exist_ok=True)
    os.makedirs("docs/brand", exist_ok=True)
    os.makedirs("fastlane/metadata/android/en-US/images", exist_ok=True)
    os.makedirs("app/src/main/assets/licenses", exist_ok=True)

def generate_background_drawable():
    content = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:name="background_tile"
        android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="54"
                android:startY="0"
                android:endX="54"
                android:endY="108">
                <item android:offset="0.0" android:color="#FF3A3835" />
                <item android:offset="1.0" android:color="#FF1D1C1B" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
"""
    with open("design/src/main/res/drawable/ic_launcher_background.xml", "w", encoding="utf-8") as f:
        f.write(content)
    print("Wrote ic_launcher_background.xml")

def generate_symbol_xml(width_dp, height_dp, typed_lines_color, typed_lines_alpha, is_monochrome=False):
    if is_monochrome:
        # Monochrome: single color #FFFFFFFF, lines at 0.55 alpha, strip cut out
        return f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{width_dp}dp"
    android:height="{height_dp}dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- Typed lines (at 55% alpha) -->
    <path
        android:name="line1"
        android:fillColor="#FFFFFFFF"
        android:fillAlpha="0.55"
        android:pathData="M37.3,24h25.4a1.3,1.3 0 0,1 1.3,1.3a1.3,1.3 0 0,1 -1.3,1.3h-25.4a1.3,1.3 0 0,1 -1.3,-1.3a1.3,1.3 0 0,1 1.3,-1.3z" />
    <path
        android:name="line2"
        android:fillColor="#FFFFFFFF"
        android:fillAlpha="0.55"
        android:pathData="M37.3,29.5h19.4a1.3,1.3 0 0,1 1.3,1.3a1.3,1.3 0 0,1 -1.3,1.3h-19.4a1.3,1.3 0 0,1 -1.3,-1.3a1.3,1.3 0 0,1 1.3,-1.3z" />
    <path
        android:name="line3"
        android:fillColor="#FFFFFFFF"
        android:fillAlpha="0.55"
        android:pathData="M37.3,35h15.4a1.3,1.3 0 0,1 1.3,1.3a1.3,1.3 0 0,1 -1.3,1.3h-15.4a1.3,1.3 0 0,1 -1.3,-1.3a1.3,1.3 0 0,1 1.3,-1.3z" />

    <!-- Caret bar -->
    <path
        android:name="caret"
        android:fillColor="#FFFFFFFF"
        android:pathData="M56.5,35h9a1,1 0 0,1 1,1v0.1a1,1 0 0,1 -1,1h-9a1,1 0 0,1 -1,-1v-0.1a1,1 0 0,1 1,-1z" />

    <!-- Rail -->
    <path
        android:name="rail"
        android:fillColor="#FFFFFFFF"
        android:pathData="M6,56h96v8.5h-96z" />

    <!-- Carriage (centre 61, 60.25, scale 1.25, with strip cut out) -->
    <group
        android:name="carriage_group"
        android:pivotX="0"
        android:pivotY="0"
        android:scaleX="1.25"
        android:scaleY="1.25"
        android:translateX="61"
        android:translateY="60.25">
        <path
            android:name="slot"
            android:fillColor="#FFFFFFFF"
            android:pathData="M-3.5,-16.4h7a1.7,1.7 0 0,1 1.7,1.7v3.6a1.7,1.7 0 0,1 -1.7,1.7h-7a1.7,1.7 0 0,1 -1.7,-1.7v-3.6a1.7,1.7 0 0,1 1.7,-1.7z" />
        <path
            android:name="carriage_body_with_strip_cutout"
            android:fillColor="#FFFFFFFF"
            android:fillType="evenOdd"
            android:pathData="M-10.5,-10h21a4.5,4.5 0 0,1 4.5,4.5v12a4.5,4.5 0 0,1 -4.5,4.5h-21a4.5,4.5 0 0,1 -4.5,-4.5v-12a4.5,4.5 0 0,1 4.5,-4.5z M-3.35,-4.7h6.7a0.95,0.95 0 0,1 0.95,0.95a0.95,0.95 0 0,1 -0.95,0.95h-6.7a0.95,0.95 0 0,1 -0.95,-0.95a0.95,0.95 0 0,1 0.95,-0.95z" />
    </group>
</vector>
"""

    return f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="{width_dp}dp"
    android:height="{height_dp}dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- Typed lines -->
    <path
        android:name="line1"
        android:fillColor="{typed_lines_color}"
        android:fillAlpha="{typed_lines_alpha}"
        android:pathData="M37.3,24h25.4a1.3,1.3 0 0,1 1.3,1.3a1.3,1.3 0 0,1 -1.3,1.3h-25.4a1.3,1.3 0 0,1 -1.3,-1.3a1.3,1.3 0 0,1 1.3,-1.3z" />
    <path
        android:name="line2"
        android:fillColor="{typed_lines_color}"
        android:fillAlpha="{typed_lines_alpha}"
        android:pathData="M37.3,29.5h19.4a1.3,1.3 0 0,1 1.3,1.3a1.3,1.3 0 0,1 -1.3,1.3h-19.4a1.3,1.3 0 0,1 -1.3,-1.3a1.3,1.3 0 0,1 1.3,-1.3z" />
    <path
        android:name="line3"
        android:fillColor="{typed_lines_color}"
        android:fillAlpha="{typed_lines_alpha}"
        android:pathData="M37.3,35h15.4a1.3,1.3 0 0,1 1.3,1.3a1.3,1.3 0 0,1 -1.3,1.3h-15.4a1.3,1.3 0 0,1 -1.3,-1.3a1.3,1.3 0 0,1 1.3,-1.3z" />

    <!-- Caret -->
    <path
        android:name="caret"
        android:fillColor="#FF5A1F"
        android:pathData="M56.5,35h9a1,1 0 0,1 1,1v0.1a1,1 0 0,1 -1,1h-9a1,1 0 0,1 -1,-1v-0.1a1,1 0 0,1 1,-1z" />

    <!-- Rail shadow -->
    <path
        android:name="rail_shadow"
        android:fillColor="#000000"
        android:fillAlpha="0.42"
        android:pathData="M6,57.5h96v4h-96z" />

    <!-- Rail with steel gradient -->
    <path
        android:name="rail"
        android:pathData="M6,56h96v8.5h-96z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="54"
                android:startY="56"
                android:endX="54"
                android:endY="64.5">
                <item android:offset="0.0" android:color="#FFF4F4F2" />
                <item android:offset="0.4" android:color="#FFA9A9A6" />
                <item android:offset="0.62" android:color="#FF6F6F6C" />
                <item android:offset="1.0" android:color="#FFC9C9C6" />
            </gradient>
        </aapt:attr>
    </path>

    <!-- Carriage (centre 61, 60.25, scale 1.25) -->
    <group
        android:name="carriage_group"
        android:pivotX="0"
        android:pivotY="0"
        android:scaleX="1.25"
        android:scaleY="1.25"
        android:translateX="61"
        android:translateY="60.25">

        <!-- Carriage shadow -->
        <path
            android:name="carriage_shadow"
            android:fillColor="#000000"
            android:fillAlpha="0.55"
            android:pathData="M-14.5,15a14.5,3 0 1,0 29,0a14.5,3 0 1,0 -29,0z" />

        <!-- Slug slot -->
        <path
            android:name="slot"
            android:fillColor="#141312"
            android:pathData="M-3.5,-16.4h7a1.7,1.7 0 0,1 1.7,1.7v3.6a1.7,1.7 0 0,1 -1.7,1.7h-7a1.7,1.7 0 0,1 -1.7,-1.7v-3.6a1.7,1.7 0 0,1 1.7,-1.7z" />

        <!-- Slug -->
        <path
            android:name="slug"
            android:pathData="M-2.5,-14.6h5a1,1 0 0,1 1,1v3a1,1 0 0,1 -1,1h-5a1,1 0 0,1 -1,-1v-3a1,1 0 0,1 1,-1z">
            <aapt:attr name="android:fillColor">
                <gradient
                    android:type="linear"
                    android:startX="-3.5"
                    android:startY="-12.1"
                    android:endX="3.5"
                    android:endY="-12.1">
                    <item android:offset="0.0" android:color="#FF9D9D9A" />
                    <item android:offset="0.5" android:color="#FFEFEFEC" />
                    <item android:offset="1.0" android:color="#FF8A8A87" />
                </gradient>
            </aapt:attr>
        </path>

        <!-- Carriage body clip group -->
        <group>
            <clip-path android:pathData="M-10.5,-10h21a4.5,4.5 0 0,1 4.5,4.5v12a4.5,4.5 0 0,1 -4.5,4.5h-21a4.5,4.5 0 0,1 -4.5,-4.5v-12a4.5,4.5 0 0,1 4.5,-4.5z" />

            <!-- Body fill -->
            <path
                android:name="carriage_body"
                android:pathData="M-15,-10h30v21h-30z">
                <aapt:attr name="android:fillColor">
                    <gradient
                        android:type="linear"
                        android:startX="-15"
                        android:startY="0.5"
                        android:endX="15"
                        android:endY="0.5">
                        <item android:offset="0.0" android:color="#FF2A2927" />
                        <item android:offset="0.2" android:color="#FF3B3A37" />
                        <item android:offset="0.8" android:color="#FF3B3A37" />
                        <item android:offset="1.0" android:color="#FF2A2927" />
                    </gradient>
                </aapt:attr>
            </path>

            <!-- Top collar -->
            <path
                android:name="collar_top"
                android:pathData="M-15,-10h30v3h-30z">
                <aapt:attr name="android:fillColor">
                    <gradient
                        android:type="linear"
                        android:startX="0"
                        android:startY="-10"
                        android:endX="0"
                        android:endY="-7">
                        <item android:offset="0.0" android:color="#FFF0F0ED" />
                        <item android:offset="1.0" android:color="#FFA9A9A6" />
                    </gradient>
                </aapt:attr>
            </path>

            <!-- Bottom collar -->
            <path
                android:name="collar_bottom"
                android:pathData="M-15,8h30v3h-30z">
                <aapt:attr name="android:fillColor">
                    <gradient
                        android:type="linear"
                        android:startX="0"
                        android:startY="8"
                        android:endX="0"
                        android:endY="11">
                        <item android:offset="0.0" android:color="#FF8D8D8A" />
                        <item android:offset="1.0" android:color="#FFD2D2CF" />
                    </gradient>
                </aapt:attr>
            </path>
        </group>

        <!-- Dashed groove line -->
        <path
            android:name="grooves"
            android:strokeColor="#FFFFFF"
            android:strokeAlpha="0.17"
            android:strokeWidth="2.5"
            android:pathData="M-8.4,4.1h16.8" />

        <!-- Orange strip -->
        <path
            android:name="carriage_strip"
            android:fillColor="#FF5A1F"
            android:pathData="M-3.35,-4.7h6.7a0.95,0.95 0 0,1 0.95,0.95a0.95,0.95 0 0,1 -0.95,0.95h-6.7a0.95,0.95 0 0,1 -0.95,-0.95a0.95,0.95 0 0,1 0.95,-0.95z" />
    </group>
</vector>
"""

def generate_drawables():
    # ic_launcher_foreground.xml
    with open("design/src/main/res/drawable/ic_launcher_foreground.xml", "w", encoding="utf-8") as f:
        f.write(generate_symbol_xml(108, 108, "#F3EEE3", "0.34"))
    print("Wrote ic_launcher_foreground.xml")

    # ic_launcher_monochrome.xml
    with open("design/src/main/res/drawable/ic_launcher_monochrome.xml", "w", encoding="utf-8") as f:
        f.write(generate_symbol_xml(108, 108, "#FFFFFFFF", "0.55", is_monochrome=True))
    print("Wrote ic_launcher_monochrome.xml")

    # ic_splash_symbol.xml (dark mode)
    with open("design/src/main/res/drawable/ic_splash_symbol.xml", "w", encoding="utf-8") as f:
        f.write(generate_symbol_xml(288, 288, "#F3EEE3", "0.34"))
    print("Wrote ic_splash_symbol.xml")

    # ic_splash_symbol_light.xml (light mode)
    with open("design/src/main/res/drawable/ic_splash_symbol_light.xml", "w", encoding="utf-8") as f:
        f.write(generate_symbol_xml(288, 288, "#2B2824", "0.34"))
    print("Wrote ic_splash_symbol_light.xml")

def extract_wordmark():
    font = TTFont("tools/Inter-Bold.ttf")
    glyph_set = font.getGlyphSet()
    cmap = font.getBestCmap()
    upm = font['head'].unitsPerEm # 2048

    tracking = int(-0.045 * upm) # -92
    caret_w = int(0.34 * upm)    # 696
    caret_h = int(0.085 * upm)   # 174
    caret_gap = int(0.06 * upm)  # 123
    caret_r = int(0.04 * upm)    # 82

    # Collect glyph pens
    glyph_paths = []
    x_cursor = 0
    for char in "enk":
        pen = SVGPathPen(glyph_set)
        glyph = glyph_set[cmap[ord(char)]]
        glyph.draw(pen)
        d = pen.getCommands()
        glyph_paths.append((char, x_cursor, d))
        x_cursor += glyph.width + tracking

    # k right edge is x_cursor - tracking
    k_right = x_cursor - tracking
    caret_x = k_right + caret_gap
    total_w = caret_x + caret_w

    # In Inter Bold, ascender is ~1490, baseline is 0, caret sits just below baseline (top 0, bottom -caret_h).
    # In SVG, Y grows down. Let top = 0 (corresponding to y_font = 1520), baseline = 1520, caret bottom = 1520 + caret_h = 1694.
    y_top = 1520
    total_h = y_top + caret_h + 30 # ~1724

    # Convert glyph paths to SVG transform
    # Glyph coordinates are (x_font, y_font). We want (x_cursor + x_font, y_top - y_font).
    # SVG allows <g transform="translate(x_cursor, y_top) scale(1, -1)"> <path d="..." /> </g>!
    return glyph_paths, caret_x, total_w, total_h, y_top, caret_w, caret_h, caret_r

def generate_wordmark_and_svgs():
    glyph_paths, caret_x, total_w, total_h, y_top, caret_w, caret_h, caret_r = extract_wordmark()

    # Caret path in SVG coords:
    # x = caret_x, y = y_top, w = caret_w, h = caret_h, rx = caret_r
    cr_top = y_top + 10 # slightly below baseline
    caret_d = f"M{caret_x + caret_r},{cr_top} h{caret_w - 2*caret_r} a{caret_r},{caret_r} 0 0,1 {caret_r},{caret_r} v{caret_h - 2*caret_r} a{caret_r},{caret_r} 0 0,1 -{caret_r},{caret_r} h-{caret_w - 2*caret_r} a{caret_r},{caret_r} 0 0,1 -{caret_r},-{caret_r} v-{caret_h - 2*caret_r} a{caret_r},{caret_r} 0 0,1 {caret_r},-{caret_r} z"

    # Build enk_wordmark.xml
    # VectorDrawable supports <group android:scaleY="-1" android:translateY="...">!
    xml_content = f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="72dp"
    android:height="30dp"
    android:viewportWidth="{total_w}"
    android:viewportHeight="{total_h}">

    <group
        android:name="enk_text"
        android:scaleY="-1"
        android:translateY="{y_top}">
"""
    for char, x_offset, d in glyph_paths:
        xml_content += f"""        <group android:name="glyph_{char}" android:translateX="{x_offset}">
            <path
                android:name="path_{char}"
                android:fillColor="#FFF3EEE3"
                android:pathData="{d}" />
        </group>
"""
    xml_content += f"""    </group>

    <path
        android:name="caret"
        android:fillColor="#FF5A1F"
        android:pathData="{caret_d}" />
</vector>
"""
    with open("design/src/main/res/drawable/enk_wordmark.xml", "w", encoding="utf-8") as f:
        f.write(xml_content)
    print("Wrote enk_wordmark.xml")

    # docs/brand/logo.svg (symbol 108x108)
    logo_svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">
<defs>
  <linearGradient id="gRail" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#f4f4f2"/><stop offset=".4" stop-color="#a9a9a6"/><stop offset=".62" stop-color="#6f6f6c"/><stop offset="1" stop-color="#c9c9c6"/></linearGradient>
  <linearGradient id="gColT" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#f0f0ed"/><stop offset="1" stop-color="#a9a9a6"/></linearGradient>
  <linearGradient id="gColB" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#8d8d8a"/><stop offset="1" stop-color="#d2d2cf"/></linearGradient>
  <linearGradient id="gBody" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#2a2927"/><stop offset=".2" stop-color="#3b3a37"/><stop offset=".8" stop-color="#3b3a37"/><stop offset="1" stop-color="#2a2927"/></linearGradient>
  <linearGradient id="gSlug" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#9d9d9a"/><stop offset=".5" stop-color="#efefec"/><stop offset="1" stop-color="#8a8a87"/></linearGradient>
  <filter id="fB" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="1.7"/></filter>
  <clipPath id="cpBody"><rect x="-15" y="-10" width="30" height="21" rx="4.5"/></clipPath>
  <g id="car">
    <ellipse cx="0" cy="15" rx="14.5" ry="3" fill="#000" opacity=".55" filter="url(#fB)"/>
    <rect x="-5.2" y="-16.4" width="10.4" height="7" rx="1.7" fill="#141312"/>
    <rect x="-3.5" y="-14.6" width="7" height="5" rx="1" fill="url(#gSlug)"/>
    <g clip-path="url(#cpBody)"><rect x="-15" y="-10" width="30" height="21" fill="url(#gBody)"/><rect x="-15" y="-10" width="30" height="3" fill="url(#gColT)"/><rect x="-15" y="8" width="30" height="3" fill="url(#gColB)"/></g>
    <path d="M-8.4 4.1h16.8" stroke="#fff" stroke-opacity=".17" stroke-width="2.5" stroke-dasharray=".9 1.7"/>
    <rect x="-4.3" y="-4.7" width="8.6" height="1.9" rx=".95" fill="#ff5a1f"/>
  </g>
</defs>
<g fill="#f3eee3" opacity=".34"><rect x="36" y="24" width="28" height="2.6" rx="1.3"/><rect x="36" y="29.5" width="22" height="2.6" rx="1.3"/><rect x="36" y="35" width="18" height="2.6" rx="1.3"/></g>
<rect x="55.5" y="35" width="11" height="2.1" rx="1" fill="#ff5a1f"/>
<rect x="6" y="57.5" width="96" height="4" fill="#000" opacity=".42" filter="url(#fB)"/>
<rect x="6" y="56" width="96" height="8.5" fill="url(#gRail)"/>
<use href="#car" transform="translate(61 60.25) scale(1.25)"/>
</svg>
"""
    with open("docs/brand/logo.svg", "w", encoding="utf-8") as f:
        f.write(logo_svg)
    print("Wrote docs/brand/logo.svg")

    def make_lockup(dark=True):
        bg_col = "#1a1918" if dark else "#f2ecdf"
        text_col = "#f3eee3" if dark else "#2b2824"
        wordmark_scale = 84.0 / total_h
        wm_w = total_w * wordmark_scale
        wm_h = total_h * wordmark_scale
        w = 120 + wm_w + 32
        h = 124

        svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w}" height="{h}">
<defs>
  <linearGradient id="tileG" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#3a3835"/><stop offset="1" stop-color="#1d1c1b"/></linearGradient>
  <linearGradient id="gRail" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#f4f4f2"/><stop offset=".4" stop-color="#a9a9a6"/><stop offset=".62" stop-color="#6f6f6c"/><stop offset="1" stop-color="#c9c9c6"/></linearGradient>
  <linearGradient id="gColT" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#f0f0ed"/><stop offset="1" stop-color="#a9a9a6"/></linearGradient>
  <linearGradient id="gColB" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#8d8d8a"/><stop offset="1" stop-color="#d2d2cf"/></linearGradient>
  <linearGradient id="gBody" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#2a2927"/><stop offset=".2" stop-color="#3b3a37"/><stop offset=".8" stop-color="#3b3a37"/><stop offset="1" stop-color="#2a2927"/></linearGradient>
  <linearGradient id="gSlug" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#9d9d9a"/><stop offset=".5" stop-color="#efefec"/><stop offset="1" stop-color="#8a8a87"/></linearGradient>
  <filter id="fB" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="1.7"/></filter>
  <clipPath id="cpBody"><rect x="-15" y="-10" width="30" height="21" rx="4.5"/></clipPath>
  <g id="car">
    <ellipse cx="0" cy="15" rx="14.5" ry="3" fill="#000" opacity=".55" filter="url(#fB)"/>
    <rect x="-5.2" y="-16.4" width="10.4" height="7" rx="1.7" fill="#141312"/>
    <rect x="-3.5" y="-14.6" width="7" height="5" rx="1" fill="url(#gSlug)"/>
    <g clip-path="url(#cpBody)"><rect x="-15" y="-10" width="30" height="21" fill="url(#gBody)"/><rect x="-15" y="-10" width="30" height="3" fill="url(#gColT)"/><rect x="-15" y="8" width="30" height="3" fill="url(#gColB)"/></g>
    <path d="M-8.4 4.1h16.8" stroke="#fff" stroke-opacity=".17" stroke-width="2.5" stroke-dasharray=".9 1.7"/>
    <rect x="-4.3" y="-4.7" width="8.6" height="1.9" rx=".95" fill="#ff5a1f"/>
  </g>
  <g id="sym">
    <g fill="#f3eee3" opacity=".34"><rect x="36" y="24" width="28" height="2.6" rx="1.3"/><rect x="36" y="29.5" width="22" height="2.6" rx="1.3"/><rect x="36" y="35" width="18" height="2.6" rx="1.3"/></g>
    <rect x="55.5" y="35" width="11" height="2.1" rx="1" fill="#ff5a1f"/>
    <rect x="6" y="57.5" width="96" height="4" fill="#000" opacity=".42" filter="url(#fB)"/>
    <rect x="6" y="56" width="96" height="8.5" fill="url(#gRail)"/>
    <use href="#car" transform="translate(61 60.25) scale(1.25)"/>
  </g>
</defs>
<rect width="100%" height="100%" rx="18" fill="{bg_col}"/>
<g transform="translate(20, 20)">
  <rect width="84" height="84" rx="19" fill="url(#tileG)"/>
  <rect width="84" height="84" rx="19" fill="none" stroke="#fff" stroke-opacity=".18" stroke-width="1"/>
  <g transform="scale(0.7777)"><use href="#sym"/></g>
</g>
<g transform="translate(120, 20) scale({wordmark_scale})">
  <g transform="translate(0, {y_top}) scale(1, -1)" fill="{text_col}">
"""
        for char, x_offset, d in glyph_paths:
            svg += f'    <path transform="translate({x_offset}, 0)" d="{d}"/>\n'
        svg += f"""  </g>
  <path d="{caret_d}" fill="#ff5a1f"/>
</g>
</svg>
"""
        return svg

    with open("docs/brand/lockup-dark.svg", "w", encoding="utf-8") as f:
        f.write(make_lockup(dark=True))
    print("Wrote docs/brand/lockup-dark.svg")

    with open("docs/brand/lockup-light.svg", "w", encoding="utf-8") as f:
        f.write(make_lockup(dark=False))
    print("Wrote docs/brand/lockup-light.svg")

def render_store_icon():
    # 512 x 512 PNG, full bleed tile gradient (#3A3835 top to #1D1C1B bottom)
    # Highlight: 1 unit inset at top edge (1 unit in 108 grid = 512 / 108 = 4.74 px), white 18%
    img = Image.new("RGBA", (512, 512))
    draw = ImageDraw.Draw(img)

    # Gradient background
    c_top = (0x3A, 0x38, 0x35, 0xFF)
    c_bot = (0x1D, 0x1C, 0x1B, 0xFF)
    for y in range(512):
        t = y / 511.0
        r = int(c_top[0] * (1 - t) + c_bot[0] * t)
        g = int(c_top[1] * (1 - t) + c_bot[1] * t)
        b = int(c_top[2] * (1 - t) + c_bot[2] * t)
        draw.line([(0, y), (511, y)], fill=(r, g, b, 255))

    # Top highlight line
    draw.line([(0, 1), (511, 1)], fill=(255, 255, 255, int(255 * 0.18)), width=2)

    # Scale symbol: 512 / 108 = 4.74074
    scale = 512.0 / 108.0

    def s(x, y=None):
        if y is None:
            return x * scale
        return (x * scale, y * scale)

    # Typed lines
    lines = [
        (36, 24, 28, 2.6),
        (36, 29.5, 22, 2.6),
        (36, 35, 18, 2.6)
    ]
    line_col = (0xF3, 0xEE, 0xE3, int(255 * 0.34))
    for lx, ly, lw, lh in lines:
        rx0, ry0 = s(lx, ly)
        rx1, ry1 = s(lx + lw, ly + lh)
        draw.rounded_rectangle([rx0, ry0, rx1, ry1], radius=s(1.3), fill=line_col)

    # Caret
    cx0, cy0 = s(55.5, 35)
    cx1, cy1 = s(55.5 + 11, 35 + 2.1)
    draw.rounded_rectangle([cx0, cy0, cx1, cy1], radius=s(1.0), fill=(0xFF, 0x5A, 0x1F, 255))

    # Rail shadow
    rsx0, rsy0 = s(6, 57.5)
    rsx1, rsy1 = s(6 + 96, 57.5 + 4)
    draw.rectangle([rsx0, rsy0, rsx1, rsy1], fill=(0, 0, 0, int(255 * 0.42)))

    # Rail steel gradient
    rx0, ry0 = s(6, 56)
    rx1, ry1 = s(6 + 96, 56 + 8.5)
    # Stops: 0% #F4F4F2, 40% #A9A9A6, 62% #6F6F6C, 100% #C9C9C6
    def rail_color(t):
        if t <= 0.4:
            u = t / 0.4
            c1, c2 = (0xF4, 0xF4, 0xF2), (0xA9, 0xA9, 0xA6)
        elif t <= 0.62:
            u = (t - 0.4) / 0.22
            c1, c2 = (0xA9, 0xA9, 0xA6), (0x6F, 0x6F, 0x6C)
        else:
            u = (t - 0.62) / 0.38
            c1, c2 = (0x6F, 0x6F, 0x6C), (0xC9, 0xC9, 0xC6)
        return (
            int(c1[0] * (1 - u) + c2[0] * u),
            int(c1[1] * (1 - u) + c2[1] * u),
            int(c1[2] * (1 - u) + c2[2] * u),
            255
        )

    for y in range(int(ry0), int(ry1) + 1):
        t = (y - ry0) / (ry1 - ry0) if (ry1 - ry0) > 0 else 0
        t = min(max(t, 0.0), 1.0)
        draw.line([(rx0, y), (rx1, y)], fill=rail_color(t))

    # Carriage at center (61, 60.25), scale 1.25 of symbol units
    c_scale = scale * 1.25
    ox, oy = s(61, 60.25)

    def cs(cx, cy=None):
        if cy is None:
            return cx * c_scale
        return (ox + cx * c_scale, oy + cy * c_scale)

    # Carriage shadow (ellipse cx=0, cy=15, rx=14.5, ry=3)
    sh_x0, sh_y0 = cs(-14.5, 15 - 3)
    sh_x1, sh_y1 = cs(14.5, 15 + 3)
    draw.ellipse([sh_x0, sh_y0, sh_x1, sh_y1], fill=(0, 0, 0, int(255 * 0.55)))

    # Slot (-5.2, -16.4, 10.4, 7) rx=1.7
    sl_x0, sl_y0 = cs(-5.2, -16.4)
    sl_x1, sl_y1 = cs(-5.2 + 10.4, -16.4 + 7)
    draw.rounded_rectangle([sl_x0, sl_y0, sl_x1, sl_y1], radius=cs(1.7), fill=(0x14, 0x13, 0x12, 255))

    # Slug (-3.5, -14.6, 7, 5) rx=1
    sg_x0, sg_y0 = cs(-3.5, -14.6)
    sg_x1, sg_y1 = cs(-3.5 + 7, -14.6 + 5)
    # horizontal gradient #9D9D9A to #EFEFEC to #8A8A87
    draw.rounded_rectangle([sg_x0, sg_y0, sg_x1, sg_y1], radius=cs(1.0), fill=(0xDF, 0xDF, 0xDC, 255))

    # Carriage body (-15, -10, 30, 21, rx=4.5)
    b_x0, b_y0 = cs(-15, -10)
    b_x1, b_y1 = cs(15, 11)
    draw.rounded_rectangle([b_x0, b_y0, b_x1, b_y1], radius=cs(4.5), fill=(0x3B, 0x3A, 0x37, 255))

    # Collars
    tc_x0, tc_y0 = cs(-15, -10)
    tc_x1, tc_y1 = cs(15, -7)
    draw.rectangle([tc_x0, tc_y0, tc_x1, tc_y1], fill=(0xCC, 0xCC, 0xC9, 255))

    bc_x0, bc_y0 = cs(-15, 8)
    bc_x1, bc_y1 = cs(15, 11)
    draw.rectangle([bc_x0, bc_y0, bc_x1, bc_y1], fill=(0xB0, 0xB0, 0xAD, 255))

    # Dashed groove line: M-8.4, 4.1 h16.8
    # draw dashed segments
    gx = -8.4
    while gx < 8.4:
        gx0, gy = cs(gx, 4.1)
        gx1, _ = cs(min(gx + 0.9, 8.4), 4.1)
        draw.line([(gx0, gy), (gx1, gy)], fill=(255, 255, 255, int(255 * 0.17)), width=int(cs(2.5)))
        gx += 0.9 + 1.7

    # Orange strip (-4.3, -4.7, 8.6, 1.9, rx=0.95)
    st_x0, st_y0 = cs(-4.3, -4.7)
    st_x1, st_y1 = cs(4.3, -2.8)
    draw.rounded_rectangle([st_x0, st_y0, st_x1, st_y1], radius=cs(0.95), fill=(0xFF, 0x5A, 0x1F, 255))

    img.save("docs/brand/store-icon-512.png", "PNG")
    img.save("fastlane/metadata/android/en-US/images/icon.png", "PNG")
    print("Wrote docs/brand/store-icon-512.png and fastlane icon.png")

if __name__ == "__main__":
    ensure_dirs()
    generate_background_drawable()
    generate_drawables()
    generate_wordmark_and_svgs()
    render_store_icon()
    print("All assets generated successfully!")
