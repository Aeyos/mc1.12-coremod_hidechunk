#!/usr/bin/env python3
"""
Convert a structure-style .nbt schematic for Minecraft 1.12 compatibility.

It scans supported block IDs from blockstate JSON files found in:
  - the vanilla 1.12.2 client jar (required)
  - optional mod jars (for modded block IDs)

Any unsupported palette block is replaced with minecraft:stone (except air).
This is useful for opening newer structure NBTs in old Litematica builds.
"""

from __future__ import annotations

import argparse
import os
from collections import Counter
from pathlib import Path
from zipfile import ZipFile

import nbtlib


def collect_supported_blocks(jars: list[Path]) -> set[str]:
    supported: set[str] = set()
    for jar in jars:
        if not jar.exists() or not jar.is_file():
            continue
        try:
            with ZipFile(jar) as zf:
                for name in zf.namelist():
                    # assets/<namespace>/blockstates/<block>.json
                    if not name.startswith("assets/") or not name.endswith(".json"):
                        continue
                    parts = name.split("/")
                    if len(parts) != 4:
                        continue
                    if parts[2] != "blockstates":
                        continue
                    namespace = parts[1]
                    block_name = parts[3][:-5]
                    if namespace and block_name:
                        supported.add(f"{namespace}:{block_name}")
        except Exception as exc:
            print(f"[warn] failed to read jar {jar}: {exc}")
    return supported


def gather_jars(mc_jar: Path, mods_dir: Path | None) -> list[Path]:
    jars: list[Path] = [mc_jar]
    if mods_dir and mods_dir.exists() and mods_dir.is_dir():
        for name in sorted(os.listdir(mods_dir)):
            if name.lower().endswith(".jar"):
                jars.append(mods_dir / name)
    return jars


def convert_palette(
    nbt_file: nbtlib.File,
    supported_blocks: set[str],
    fallback_block: str,
) -> tuple[int, Counter[str]]:
    palette = nbt_file.get("palette") or nbt_file.get("Palette")
    if palette is None:
        raise ValueError("No 'palette'/'Palette' tag found. Not a structure-style NBT.")

    replaced_counter: Counter[str] = Counter()
    replacements = 0

    for entry in palette:
        if not hasattr(entry, "get"):
            continue
        name_tag = entry.get("Name")
        if name_tag is None:
            continue
        block_id = str(name_tag)

        if block_id == "minecraft:air":
            continue

        if block_id not in supported_blocks:
            entry["Name"] = nbtlib.String(fallback_block)
            if "Properties" in entry:
                del entry["Properties"]
            replacements += 1
            replaced_counter[block_id] += 1

    return replacements, replaced_counter


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Replace unsupported palette blocks in structure NBT with minecraft:stone."
    )
    parser.add_argument("--input", required=True, type=Path, help="Input .nbt structure file")
    parser.add_argument(
        "--minecraft-jar",
        required=True,
        type=Path,
        help="Path to vanilla minecraft-1.12.2-client.jar",
    )
    parser.add_argument(
        "--mods-dir",
        type=Path,
        default=None,
        help="Optional mods directory; include these jars as supported namespaces",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="Output file path (default: <input_stem>_mc112.nbt)",
    )
    parser.add_argument(
        "--fallback-block",
        default="minecraft:stone",
        help="Replacement block id for unsupported palette entries",
    )
    parser.add_argument(
        "--in-place",
        action="store_true",
        help="Overwrite input file instead of writing a new one",
    )
    args = parser.parse_args()

    if not args.input.exists():
        raise FileNotFoundError(f"Input file not found: {args.input}")
    if not args.minecraft_jar.exists():
        raise FileNotFoundError(f"Minecraft jar not found: {args.minecraft_jar}")

    jars = gather_jars(args.minecraft_jar, args.mods_dir)
    supported = collect_supported_blocks(jars)
    if not supported:
        raise RuntimeError("No supported block IDs discovered from provided jars.")

    nbt_file = nbtlib.load(args.input)
    replacements, replaced_counter = convert_palette(
        nbt_file=nbt_file,
        supported_blocks=supported,
        fallback_block=args.fallback_block,
    )

    if args.in_place:
        out_path = args.input
    else:
        out_path = args.output or args.input.with_name(f"{args.input.stem}_mc112.nbt")

    gzipped = getattr(nbt_file, "gzipped", True)
    byteorder = getattr(nbt_file, "byteorder", "big")
    nbt_file.save(out_path, gzipped=gzipped, byteorder=byteorder)

    print(f"Input:        {args.input}")
    print(f"Output:       {out_path}")
    print(f"Supported IDs discovered: {len(supported)}")
    print(f"Palette entries replaced: {replacements}")
    if replaced_counter:
        print("Replaced block IDs:")
        for block_id, count in replaced_counter.most_common():
            print(f"  - {block_id}: {count}")
    else:
        print("No unsupported palette entries found.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

