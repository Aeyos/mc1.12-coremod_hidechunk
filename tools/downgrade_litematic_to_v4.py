#!/usr/bin/env python3
"""
Downgrade .litematic files from schematic format Version 5, 6, or 7 to Version 4.

Why this works (in practice):
  - Newer Litematica bumps the root int tag "Version" when minor format features are
    added; older clients refuse unknown versions (see maruohon/litematica#543).
  - v5+ adds per-region "PendingFluidTicks" (and root "SubVersion") in modern Litematica;
    1.12-era readers only understand up to v4 and never read those tags.

This script does NOT remap block IDs or rewrite palettes — only version metadata and
tags that 1.12 Litematica does not expect. For new blocks in palettes, use a separate
tool (e.g. convert_structure_nbt_to_112.py for vanilla .nbt, or in-game paste/export).

Requires: pip install nbtlib
"""

from __future__ import annotations

import argparse
import os
import shutil
import sys
from pathlib import Path
from zipfile import ZipFile

import nbtlib

# Minecraft 1.12.2 world data version (used by many 1.12 mods for NBT schema hints).
DEFAULT_MC_1122_DATA_VERSION = 1343
DEFAULT_MC_1122_JAR = Path(
    os.environ.get("APPDATA", "")
) / "PrismLauncher/libraries/com/mojang/minecraft/1.12.2/minecraft-1.12.2-client.jar"
COLOR_BY_META_112 = [
    "white",
    "orange",
    "magenta",
    "light_blue",
    "yellow",
    "lime",
    "pink",
    "gray",
    "silver",
    "cyan",
    "purple",
    "blue",
    "brown",
    "green",
    "red",
    "black",
]
ReplacementSpec = tuple[str, dict[str, str] | None]


def is_compound(tag: object) -> bool:
    return hasattr(tag, "get") and hasattr(tag, "__contains__")


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
                    if len(parts) != 4 or parts[2] != "blockstates":
                        continue
                    namespace = parts[1]
                    block_name = parts[3][:-5]
                    if namespace and block_name:
                        supported.add(f"{namespace}:{block_name}")
        except Exception as exc:
            print(f"[warn] failed to read jar {jar}: {exc}", file=sys.stderr)
    return supported


def infer_mods_dir(input_file: Path) -> Path | None:
    """
    Best-effort inference for instance layout like .../minecraft/schematics/<file>.
    """
    p = input_file.resolve()
    if p.parent.name.lower() != "schematics":
        return None
    mc_dir = p.parent.parent
    mods = mc_dir / "mods"
    return mods if mods.exists() and mods.is_dir() else None


def gather_litematic_palette_entries(
    root: nbtlib.Compound,
) -> list[tuple[str, object]]:
    """
    Returns list of (region_name, palette_entry_compound_reference).
    """
    out: list[tuple[str, object]] = []
    regions = root.get("Regions")
    if not is_compound(regions):
        return out
    for region_name in list(regions.keys()):
        region = regions[region_name]
        if not is_compound(region):
            continue
        palette = region.get("BlockStatePalette")
        if palette is None:
            continue
        for entry in palette:
            if is_compound(entry):
                out.append((str(region_name), entry))
    return out


def metadata_to_properties(block_id: str, meta: int) -> dict[str, str] | None:
    if meta < 0 or meta > 15:
        return None

    if block_id in {
        "minecraft:wool",
        "minecraft:stained_hardened_clay",
        "minecraft:stained_glass",
        "minecraft:stained_glass_pane",
        "minecraft:carpet",
        "minecraft:concrete",
        "minecraft:concrete_powder",
    }:
        return {"color": COLOR_BY_META_112[meta]}

    if block_id == "minecraft:stone":
        variants = {
            0: "stone",
            1: "granite",
            2: "smooth_granite",
            3: "diorite",
            4: "smooth_diorite",
            5: "andesite",
            6: "smooth_andesite",
        }
        v = variants.get(meta)
        return {"variant": v} if v else None

    if block_id == "minecraft:stonebrick":
        variants = {0: "default", 1: "mossy", 2: "cracked", 3: "chiseled"}
        v = variants.get(meta)
        return {"variant": v} if v else None

    return None


def parse_replacement_spec(
    raw_value: str,
    *,
    supported_blocks: set[str],
) -> ReplacementSpec | None:
    value = raw_value.strip()
    if not value:
        return None

    # Convenience typo support requested by user example.
    value = value.replace("minecraft:concrete_power", "minecraft:concrete_powder")

    if "/" not in value:
        return (value, None) if value in supported_blocks else None

    block_id, meta_str = value.rsplit("/", 1)
    if block_id not in supported_blocks:
        return None
    if not meta_str.isdigit():
        return None
    meta = int(meta_str)
    props = metadata_to_properties(block_id, meta)
    if props is None:
        return None
    return block_id, props


def format_replacement_spec(spec: ReplacementSpec) -> str:
    block_id, props = spec
    if not props:
        return block_id
    if "color" in props and block_id in {
        "minecraft:concrete",
        "minecraft:concrete_powder",
        "minecraft:wool",
        "minecraft:stained_hardened_clay",
        "minecraft:stained_glass",
        "minecraft:stained_glass_pane",
        "minecraft:carpet",
    }:
        color = props["color"]
        try:
            idx = COLOR_BY_META_112.index(color)
            return f"{block_id}/{idx}"
        except ValueError:
            pass
    return f"{block_id} {props}"


def prompt_missing_block_mapping(
    missing_blocks: list[str],
    supported: set[str],
    default_replacement: str,
) -> dict[str, ReplacementSpec]:
    mapping: dict[str, ReplacementSpec] = {}
    stdin_is_tty = sys.stdin is not None and sys.stdin.isatty()

    default_spec = parse_replacement_spec(
        default_replacement, supported_blocks=supported
    )
    if default_spec is None:
        # Keep script usable even with odd jars; fall back to first minecraft:* block if possible.
        fallback = next((b for b in sorted(supported) if b.startswith("minecraft:")), None)
        if fallback is None:
            fallback = "minecraft:stone"
        default_spec = (fallback, None)
    default_hint = format_replacement_spec(default_spec)

    for old_block in missing_blocks:
        if not stdin_is_tty:
            mapping[old_block] = default_spec
            print(
                f"[warn] non-interactive stdin: mapping {old_block} -> {default_hint}",
                file=sys.stderr,
            )
            continue

        while True:
            prompt = (
                f"Block '{old_block}' is not available in MC 1.12. "
                f"Replacement block id [{default_hint}] "
                "(supports block/meta like minecraft:concrete_powder/2): "
            )
            user = input(prompt).strip()
            if not user:
                mapping[old_block] = default_spec
                break
            parsed = parse_replacement_spec(user, supported_blocks=supported)
            if parsed is not None:
                mapping[old_block] = parsed
                break
            print(
                f"  '{user}' was not accepted. "
                "Use a valid 1.12 block id (ex: minecraft:stone) "
                "or block/meta (ex: minecraft:concrete_powder/2)."
            )
    return mapping


def remap_missing_palette_blocks(
    root: nbtlib.Compound,
    *,
    supported_blocks: set[str],
    interactive: bool,
    default_replacement: str,
) -> list[str]:
    """
    Rewrites unsupported palette entry Name values. Returns human-readable change lines.
    """
    entries = gather_litematic_palette_entries(root)
    if not entries:
        return ["warn: no region palettes found for block-availability check"]

    missing: set[str] = set()
    for _region, entry in entries:
        name_tag = entry.get("Name")
        if name_tag is None:
            continue
        block_id = str(name_tag)
        if block_id == "minecraft:air":
            continue
        if block_id not in supported_blocks:
            missing.add(block_id)

    if not missing:
        return ["all palette block IDs are available in discovered 1.12 jars"]

    missing_sorted = sorted(missing)
    if interactive:
        mapping = prompt_missing_block_mapping(
            missing_sorted, supported_blocks, default_replacement
        )
    else:
        default_spec = parse_replacement_spec(
            default_replacement, supported_blocks=supported_blocks
        )
        if default_spec is None:
            fallback = next(
                (b for b in sorted(supported_blocks) if b.startswith("minecraft:")),
                "minecraft:stone",
            )
            default_spec = (fallback, None)
        mapping = {old: default_spec for old in missing_sorted}

    replaced_count = 0
    touched_regions: set[str] = set()
    for region_name, entry in entries:
        name_tag = entry.get("Name")
        if name_tag is None:
            continue
        old_block = str(name_tag)
        replacement = mapping.get(old_block)
        if not replacement:
            continue
        new_name, new_props = replacement
        if new_name == old_block and new_props is None:
            continue
        entry["Name"] = nbtlib.String(new_name)
        # Strip stale properties that can be invalid for the replacement block type.
        if "Properties" in entry:
            del entry["Properties"]
        if new_props:
            props_tag = nbtlib.Compound()
            for k, v in new_props.items():
                props_tag[k] = nbtlib.String(v)
            entry["Properties"] = props_tag
        replaced_count += 1
        touched_regions.add(region_name)

    changes = [
        f"palette remap: {len(missing_sorted)} unsupported block ids",
        f"palette remap: {replaced_count} palette entries updated across {len(touched_regions)} regions",
    ]
    for old in missing_sorted:
        changes.append(f"palette remap: {old} -> {format_replacement_spec(mapping[old])}")
    return changes


def downgrade_one(
    root: nbtlib.Compound,
    *,
    strip_fluid_ticks: bool,
    remove_subversion: bool,
    clamp_data_version: bool,
    target_data_version: int,
) -> tuple[int, list[str]]:
    """
    Mutates `root` in place. Returns (old_version, list of human-readable changes).
    """
    changes: list[str] = []

    if "Version" not in root:
        raise ValueError("Missing root tag 'Version' — not a litematic?")

    old_ver = int(root["Version"])
    if old_ver <= 4:
        return old_ver, ["skip: Version already <= 4"]

    if old_ver not in (5, 6, 7):
        raise ValueError(
            f"Unsupported schematic Version {old_ver} "
            f"(this script only handles 5, 6, 7 → 4)."
        )

    root["Version"] = nbtlib.Int(4)
    changes.append(f"Version: {old_ver} → 4")

    if remove_subversion and "SubVersion" in root:
        del root["SubVersion"]
        changes.append("removed root tag 'SubVersion'")

    if clamp_data_version:
        old_dv = root.get("MinecraftDataVersion")
        root["MinecraftDataVersion"] = nbtlib.Int(target_data_version)
        changes.append(
            f"MinecraftDataVersion: {old_dv} → {target_data_version} "
            f"(clamp for 1.12.2-era readers)"
        )

    if "Regions" not in root or not is_compound(root["Regions"]):
        changes.append("warn: no 'Regions' compound at root")
        return old_ver, changes

    regions = root["Regions"]
    for region_name in list(regions.keys()):
        region = regions[region_name]
        if not is_compound(region):
            continue
        if strip_fluid_ticks and "PendingFluidTicks" in region:
            del region["PendingFluidTicks"]
            changes.append(f"region '{region_name}': removed 'PendingFluidTicks'")

    return old_ver, changes


def process_file(
    path: Path,
    *,
    out: Path | None,
    in_place: bool,
    strip_fluid_ticks: bool,
    remove_subversion: bool,
    clamp_data_version: bool,
    target_data_version: int,
    backup: bool,
    supported_blocks: set[str] | None,
    interactive_remap_missing: bool,
    default_missing_block: str,
) -> int:
    nbt_file = nbtlib.load(path)
    if not isinstance(nbt_file, nbtlib.File):
        raise TypeError(f"Expected nbtlib.File, got {type(nbt_file)}")

    old_ver, changes = downgrade_one(
        nbt_file,
        strip_fluid_ticks=strip_fluid_ticks,
        remove_subversion=remove_subversion,
        clamp_data_version=clamp_data_version,
        target_data_version=target_data_version,
    )

    if changes and changes[0].startswith("skip:"):
        print(f"{path}: {changes[0]}")
        return 0

    if supported_blocks is not None and supported_blocks:
        changes.extend(
            remap_missing_palette_blocks(
                nbt_file,
                supported_blocks=supported_blocks,
                interactive=interactive_remap_missing,
                default_replacement=default_missing_block,
            )
        )
    else:
        changes.append("warn: skipped palette availability check (no block-id registry loaded)")

    dest = path if in_place else (out or path.with_name(f"{path.stem}_v4{path.suffix}"))
    if backup and in_place:
        bak = path.with_suffix(path.suffix + ".bak")
        shutil.copy2(path, bak)
        print(f"{path}: backup -> {bak}")

    gzipped = getattr(nbt_file, "gzipped", True)
    byteorder = getattr(nbt_file, "byteorder", "big")
    nbt_file.save(dest, gzipped=gzipped, byteorder=byteorder)

    print(f"{path} -> {dest} (was Version {old_ver})")
    for line in changes:
        print(f"  {line}")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser(
        description="Downgrade .litematic schematic format Version 5/6/7 to Version 4."
    )
    ap.add_argument(
        "inputs",
        nargs="+",
        type=Path,
        help="One or more .litematic files",
    )
    ap.add_argument(
        "--output",
        type=Path,
        default=None,
        help="Output path when processing a single input (ignored with multiple inputs)",
    )
    ap.add_argument(
        "--in-place",
        action="store_true",
        help="Overwrite each input file",
    )
    ap.add_argument(
        "--backup",
        action="store_true",
        help="With --in-place, write a .litematic.bak copy first",
    )
    ap.add_argument(
        "--no-strip-fluid-ticks",
        action="store_true",
        help="Keep per-region 'PendingFluidTicks' (not recommended for 1.12)",
    )
    ap.add_argument(
        "--no-remove-subversion",
        action="store_true",
        help="Keep root 'SubVersion' tag",
    )
    ap.add_argument(
        "--no-clamp-data-version",
        action="store_true",
        help="Leave MinecraftDataVersion unchanged",
    )
    ap.add_argument(
        "--target-data-version",
        type=int,
        default=DEFAULT_MC_1122_DATA_VERSION,
        help=f"MinecraftDataVersion to write when clamping (default: {DEFAULT_MC_1122_DATA_VERSION})",
    )
    ap.add_argument(
        "--minecraft-jar",
        type=Path,
        default=None,
        help=(
            "Path to minecraft-1.12.2-client.jar for palette block availability checks. "
            f"Default auto-detect: {DEFAULT_MC_1122_JAR}"
        ),
    )
    ap.add_argument(
        "--mods-dir",
        type=Path,
        default=None,
        help=(
            "Optional mods folder; jar blockstates here are also treated as available "
            "(lets you keep modded blocks instead of remapping them)."
        ),
    )
    ap.add_argument(
        "--no-interactive-remap-missing",
        action="store_true",
        help="Do not ask per missing block; map all unsupported blocks to --default-missing-block.",
    )
    ap.add_argument(
        "--default-missing-block",
        default="minecraft:stone",
        help="Default replacement block used for non-interactive remap (default: minecraft:stone).",
    )

    args = ap.parse_args()
    strip_fluid_ticks = not args.no_strip_fluid_ticks
    remove_subversion = not args.no_remove_subversion
    clamp_data_version = not args.no_clamp_data_version
    interactive_remap_missing = not args.no_interactive_remap_missing

    if len(args.inputs) > 1 and args.output:
        print("--output is only valid with a single input file", file=sys.stderr)
        return 2

    mc_jar = args.minecraft_jar or DEFAULT_MC_1122_JAR
    mods_dir = args.mods_dir
    if mods_dir is None and len(args.inputs) == 1:
        mods_dir = infer_mods_dir(args.inputs[0])

    jars_for_registry: list[Path] = []
    if mc_jar and mc_jar.exists():
        jars_for_registry.append(mc_jar)
    else:
        print(
            f"[warn] MC 1.12 jar not found for palette check: {mc_jar}",
            file=sys.stderr,
        )

    if mods_dir and mods_dir.exists() and mods_dir.is_dir():
        for candidate in sorted(mods_dir.iterdir()):
            if candidate.is_file() and candidate.suffix.lower() == ".jar":
                jars_for_registry.append(candidate)

    supported_blocks = collect_supported_blocks(jars_for_registry)
    if supported_blocks:
        print(
            f"[info] Loaded {len(supported_blocks)} supported block IDs from "
            f"{len(jars_for_registry)} jar(s)."
        )

    rc = 0
    for p in args.inputs:
        if not p.exists():
            print(f"missing: {p}", file=sys.stderr)
            rc = 1
            continue
        if p.suffix.lower() != ".litematic":
            print(f"warn: expected .litematic extension: {p}", file=sys.stderr)
        try:
            process_file(
                p,
                out=args.output,
                in_place=args.in_place,
                strip_fluid_ticks=strip_fluid_ticks,
                remove_subversion=remove_subversion,
                clamp_data_version=clamp_data_version,
                target_data_version=args.target_data_version,
                backup=args.backup,
                supported_blocks=supported_blocks,
                interactive_remap_missing=interactive_remap_missing,
                default_missing_block=args.default_missing_block,
            )
        except Exception as exc:
            print(f"{p}: ERROR: {exc}", file=sys.stderr)
            rc = 1
    return rc


if __name__ == "__main__":
    raise SystemExit(main())
