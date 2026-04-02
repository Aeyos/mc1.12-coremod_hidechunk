package com.example.hidechunk.drawer;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

/**
 * Reads EnderIO loot capacitor NBT ({@code eiocap} compound) for sorting. No compile-time EnderIO dependency.
 */
public final class CapacitorSortHelper {

    public static final String EIOCAP = "eiocap";

    /** Sort mode 0 = everything (by base {@code level}). Modes 1..N use {@link #SORT_KEYS}[mode-1]. */
    public static final String[] SORT_KEYS = {
            "enderio:block_powered_spawner/speed",
            "enderio:block_sag_mill/speed",
            "enderio:block_alloy_smelter/speed",
            "enderio:block_slice_and_splice/speed",
            "enderio:block_stirling_generator/generate",
            "enderio:block_combustion_generator/generate",
            "enderio:block_vat/speed",
            "enderio:block_soul_binder/speed",
    };

    private CapacitorSortHelper() {
    }

    public static NBTTagCompound getEiocap(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound root = stack.getTagCompound();
        if (root == null || !root.hasKey(EIOCAP, Constants.NBT.TAG_COMPOUND)) {
            return null;
        }
        return root.getCompoundTag(EIOCAP);
    }

    public static float getLevel(ItemStack stack) {
        NBTTagCompound c = getEiocap(stack);
        if (c == null || !c.hasKey("level", Constants.NBT.TAG_FLOAT)) {
            return 0F;
        }
        return c.getFloat("level");
    }

    public static float getKeyBonus(ItemStack stack, String key) {
        NBTTagCompound c = getEiocap(stack);
        if (c == null || !c.hasKey(key, Constants.NBT.TAG_FLOAT)) {
            return 0F;
        }
        return c.getFloat(key);
    }

    public static float sumNonLevelFloatBonuses(ItemStack stack) {
        NBTTagCompound c = getEiocap(stack);
        if (c == null) {
            return 0F;
        }
        float sum = 0F;
        for (String k : c.getKeySet()) {
            if ("level".equals(k)) {
                continue;
            }
            if (c.getTagId(k) == Constants.NBT.TAG_FLOAT) {
                sum += c.getFloat(k);
            }
        }
        return sum;
    }

    /**
     * Compares two stacks for descending sort (better first). {@code sortMode} 0 = overall level; else key index+1.
     */
    public static int compareForSort(int sortMode, ItemStack a, ItemStack b) {
        if (sortMode == 0) {
            int c = Float.compare(getLevel(b), getLevel(a));
            if (c != 0) {
                return c;
            }
            c = Float.compare(sumNonLevelFloatBonuses(b), sumNonLevelFloatBonuses(a));
            if (c != 0) {
                return c;
            }
            return 0;
        }
        int keyIndex = sortMode - 1;
        if (keyIndex < 0 || keyIndex >= SORT_KEYS.length) {
            return 0;
        }
        String key = SORT_KEYS[keyIndex];
        int c = Float.compare(getKeyBonus(b, key), getKeyBonus(a, key));
        if (c != 0) {
            return c;
        }
        c = Float.compare(getLevel(b), getLevel(a));
        if (c != 0) {
            return c;
        }
        return 0;
    }

    public static String sortButtonLabel(int sortMode) {
        if (sortMode == 0) {
            return "All";
        }
        int i = sortMode - 1;
        if (i < 0 || i >= SORT_KEYS.length) {
            return "?";
        }
        String key = SORT_KEYS[i];
        int slash = key.lastIndexOf('/');
        String tail = slash >= 0 ? key.substring(slash + 1) : key;
        if (tail.length() > 3) {
            return tail.substring(0, 3);
        }
        return tail;
    }
}
