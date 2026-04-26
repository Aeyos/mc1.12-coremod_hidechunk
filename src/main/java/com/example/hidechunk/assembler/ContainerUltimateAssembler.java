package com.example.hidechunk.assembler;

import com.example.hidechunk.assembler.network.HideChunkNetwork;
import com.example.hidechunk.assembler.network.PacketSetGhost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerUltimateAssembler extends Container {

    public static final int GUI_LEFT_PADDING = 8;
    public static final int GUI_TOP_PADDING = 18;
    public static final int SLOT_SIZE = 18;

    private final TileUltimateAssembler tile;
    private final InventoryBasic ghostStub;

    public ContainerUltimateAssembler(InventoryPlayer playerInv, TileUltimateAssembler tile) {
        this.tile = tile;
        this.ghostStub = new InventoryBasic("ghost", false, TileUltimateAssembler.PATTERN_SIZE);

        for (int row = 0; row < TileUltimateAssembler.GRID_SIZE; row++) {
            for (int col = 0; col < TileUltimateAssembler.GRID_SIZE; col++) {
                int idx = col + row * TileUltimateAssembler.GRID_SIZE;
                int x = GUI_LEFT_PADDING + col * SLOT_SIZE;
                int y = GUI_TOP_PADDING + row * SLOT_SIZE;
                addSlotToContainer(new SlotGhost(tile, ghostStub, idx, x, y));
            }
        }

        // Match the vanilla chest texture: 13px gap above main inventory (label area).
        int playerInvTop = GUI_TOP_PADDING + TileUltimateAssembler.GRID_SIZE * SLOT_SIZE + 13;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(
                        playerInv,
                        col + row * 9 + 9,
                        GUI_LEFT_PADDING + col * SLOT_SIZE,
                        playerInvTop + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(
                    playerInv,
                    col,
                    GUI_LEFT_PADDING + col * SLOT_SIZE,
                    playerInvTop + 3 * SLOT_SIZE + 4));
        }
    }

    public TileUltimateAssembler getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotId >= 0 && slotId < TileUltimateAssembler.PATTERN_SIZE) {
            return handleGhostClick(slotId, dragType, clickType, player);
        }
        return super.slotClick(slotId, dragType, clickType, player);
    }

    private ItemStack handleGhostClick(
            int slotId,
            int dragType,
            ClickType clickType,
            EntityPlayer player) {
        ItemStack cursor = player.inventory.getItemStack();
        ItemStack newGhost;

        switch (clickType) {
            case PICKUP:
            case QUICK_MOVE: {
                if (dragType == 1 || cursor.isEmpty()) {
                    newGhost = ItemStack.EMPTY;
                } else {
                    newGhost = cursor.copy();
                    newGhost.setCount(1);
                }
                break;
            }
            case CLONE: {
                if (cursor.isEmpty()) {
                    newGhost = ItemStack.EMPTY;
                } else {
                    newGhost = cursor.copy();
                    newGhost.setCount(1);
                }
                break;
            }
            case THROW: {
                newGhost = ItemStack.EMPTY;
                break;
            }
            default:
                return ItemStack.EMPTY;
        }

        tile.setPatternStack(slotId, newGhost);
        if (player.world.isRemote) {
            HideChunkNetwork.CHANNEL.sendToServer(
                    new PacketSetGhost(tile.getPos(), slotId, newGhost));
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < TileUltimateAssembler.PATTERN_SIZE) {
            return ItemStack.EMPTY;
        }
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack remainder = stack.copy();

        int playerStart = TileUltimateAssembler.PATTERN_SIZE;
        int mainEnd = playerStart + 27;
        int hotbarEnd = mainEnd + 9;

        if (index >= playerStart && index < mainEnd) {
            if (!mergeItemStack(stack, mainEnd, hotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= mainEnd && index < hotbarEnd) {
            if (!mergeItemStack(stack, playerStart, mainEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
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
        return remainder;
    }
}
