package com.example.hidechunk.drawer;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;

/**
 * Ensures the drawer appears in creative / JEI search and places like a normal block.
 * Sets the item creative tab explicitly and falls back if the block tab were ever null.
 */
public class ItemBlockDrawerCapacitor extends ItemBlock {

    public ItemBlockDrawerCapacitor(Block block) {
        super(block);
        setMaxDamage(0);
        setHasSubtypes(false);
        setCreativeTab(CreativeTabs.REDSTONE);
    }

    @Override
    public CreativeTabs getCreativeTab() {
        CreativeTabs fromBlock = block.getCreativeTab();
        return fromBlock != null ? fromBlock : CreativeTabs.REDSTONE;
    }
}
