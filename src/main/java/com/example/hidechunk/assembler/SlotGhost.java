package com.example.hidechunk.assembler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * Read-only ghost slot — mutations go through {@link ContainerUltimateAssembler#slotClick}
 * and {@link com.example.hidechunk.assembler.network.PacketSetGhost}, never via vanilla
 * pickup/place. The backing {@link IInventory} is a stub; reads come from the tile's pattern.
 */
public class SlotGhost extends Slot {

    private final TileUltimateAssembler tile;
    private final int patternIndex;

    public SlotGhost(TileUltimateAssembler tile, IInventory stub, int patternIndex, int xPos, int yPos) {
        super(stub, patternIndex, xPos, yPos);
        this.tile = tile;
        this.patternIndex = patternIndex;
    }

    @Override
    @Nonnull
    public ItemStack getStack() {
        return tile.getPatternStack(patternIndex);
    }

    @Override
    public void putStack(@Nonnull ItemStack stack) {
        // No-op: mutations are routed through Container.slotClick and the network packet.
    }

    @Override
    public boolean getHasStack() {
        return !getStack().isEmpty();
    }

    @Override
    public boolean isItemValid(@Nonnull ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeStack(EntityPlayer playerIn) {
        return false;
    }

    @Override
    public void onSlotChanged() {
        // No-op: pattern dirties itself when set on the tile.
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }

    @Override
    public int getItemStackLimit(@Nonnull ItemStack stack) {
        return 1;
    }

    public int getPatternIndex() {
        return patternIndex;
    }
}
