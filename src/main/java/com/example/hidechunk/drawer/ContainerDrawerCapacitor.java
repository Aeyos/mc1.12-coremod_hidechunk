package com.example.hidechunk.drawer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerDrawerCapacitor extends Container {

    private final TileDrawerCapacitor tile;

    public ContainerDrawerCapacitor(InventoryPlayer playerInv, TileDrawerCapacitor tile) {
        this.tile = tile;
        TileDrawerCapacitor.DrawerItemHandler handler = tile.createViewHandler();

        for (int row = 0; row < TileDrawerCapacitor.VISIBLE_ROWS; row++) {
            for (int col = 0; col < TileDrawerCapacitor.COLS; col++) {
                int slot = col + row * TileDrawerCapacitor.COLS;
                addSlotToContainer(new SlotItemHandler(handler, slot, 8 + col * 18, 24 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(
                        new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 112 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 170));
        }
    }

    public TileDrawerCapacitor getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack remainder = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            remainder = stack.copy();
            int drawerEnd = TileDrawerCapacitor.VISIBLE_SLOTS;
            if (index < drawerEnd) {
                if (!mergeItemStack(stack, drawerEnd, inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!TileDrawerCapacitor.isLootCapacitor(stack)) {
                    return ItemStack.EMPTY;
                }
                if (!mergeItemStack(stack, 0, drawerEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
            if (stack.getCount() == remainder.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return remainder;
    }
}
