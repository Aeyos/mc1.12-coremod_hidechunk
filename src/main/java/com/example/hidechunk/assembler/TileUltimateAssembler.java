package com.example.hidechunk.assembler;

import com.blakebr0.extendedcrafting.tile.TileUltimateCraftingTable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TileUltimateAssembler extends TileEntity implements ITickable {

    public static final int GRID_SIZE = 9;
    public static final int PATTERN_SIZE = GRID_SIZE * GRID_SIZE;

    private final NonNullList<ItemStack> pattern = NonNullList.withSize(PATTERN_SIZE, ItemStack.EMPTY);

    public ItemStack getPatternStack(int idx) {
        if (idx < 0 || idx >= PATTERN_SIZE) {
            return ItemStack.EMPTY;
        }
        return pattern.get(idx);
    }

    public NonNullList<ItemStack> getPattern() {
        return pattern;
    }

    public void setPatternStack(int idx, @Nonnull ItemStack stack) {
        if (idx < 0 || idx >= PATTERN_SIZE) {
            return;
        }
        ItemStack ghost = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (!ghost.isEmpty()) {
            ghost.setCount(1);
        }
        pattern.set(idx, ghost);
        if (world != null && !world.isRemote) {
            markDirty();
            syncToTrackingClients();
        }
    }

    /** Bulk overwrite (used by JEI recipe transfer). */
    public void setPatternBulk(NonNullList<ItemStack> next) {
        for (int i = 0; i < PATTERN_SIZE && i < next.size(); i++) {
            ItemStack s = next.get(i);
            ItemStack ghost = s.isEmpty() ? ItemStack.EMPTY : s.copy();
            if (!ghost.isEmpty()) {
                ghost.setCount(1);
            }
            pattern.set(i, ghost);
        }
        if (world != null && !world.isRemote) {
            markDirty();
            syncToTrackingClients();
        }
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        if (world == null || getWorld().getTileEntity(pos) != this) {
            return false;
        }
        return player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        TileEntity below = world.getTileEntity(pos.down());
        if (!(below instanceof TileUltimateCraftingTable)) {
            return;
        }
        IInventory matrix = (IInventory) below;

        TileEntity above = world.getTileEntity(pos.up());
        if (above == null) {
            return;
        }
        IItemHandler handler = above.getCapability(
                CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.DOWN);
        if (handler == null) {
            return;
        }

        boolean changed = false;
        for (int srcSlot = 0; srcSlot < handler.getSlots(); srcSlot++) {
            ItemStack src = handler.getStackInSlot(srcSlot);
            if (src.isEmpty()) {
                continue;
            }
            if (distributeFromSlot(handler, srcSlot, src, matrix)) {
                changed = true;
            }
        }
        if (changed) {
            below.markDirty();
        }
    }

    /**
     * Round-robin distributes one stack-source across all candidate matrix slots whose ghost pattern
     * matches the source item type. Returns {@literal true} if any item moved.
     */
    private boolean distributeFromSlot(
            IItemHandler handler,
            int srcSlot,
            ItemStack src,
            IInventory matrix) {
        List<int[]> candidates = new ArrayList<>();
        for (int i = 0; i < PATTERN_SIZE; i++) {
            ItemStack ghost = pattern.get(i);
            if (ghost.isEmpty()) {
                continue;
            }
            if (!areItemsEqualIgnoringCount(ghost, src)) {
                continue;
            }
            ItemStack cur = matrix.getStackInSlot(i);
            if (cur.isEmpty()) {
                candidates.add(new int[]{i, 0});
            } else if (areItemsEqualIgnoringCount(cur, src)
                    && cur.getCount() < cur.getMaxStackSize()) {
                candidates.add(new int[]{i, cur.getCount()});
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }
        candidates.sort(Comparator.comparingInt(a -> a[1]));

        boolean moved = false;
        boolean exhausted = false;
        while (!candidates.isEmpty() && !exhausted) {
            for (int idx = 0; idx < candidates.size(); idx++) {
                int slot = candidates.get(idx)[0];
                ItemStack peek = handler.extractItem(srcSlot, 1, true);
                if (peek.isEmpty()) {
                    exhausted = true;
                    break;
                }
                ItemStack cur = matrix.getStackInSlot(slot);
                int max = peek.getMaxStackSize();
                if (!cur.isEmpty()
                        && (!areItemsEqualIgnoringCount(cur, peek)
                                || cur.getCount() >= Math.min(max, matrix.getInventoryStackLimit()))) {
                    continue;
                }
                ItemStack taken = handler.extractItem(srcSlot, 1, false);
                if (taken.isEmpty()) {
                    exhausted = true;
                    break;
                }
                if (cur.isEmpty()) {
                    matrix.setInventorySlotContents(slot, taken);
                } else {
                    cur.grow(1);
                    matrix.setInventorySlotContents(slot, cur);
                }
                moved = true;
            }
            // drop now-full slots
            List<int[]> next = new ArrayList<>();
            for (int[] entry : candidates) {
                ItemStack cur = matrix.getStackInSlot(entry[0]);
                int cap = Math.min(
                        cur.isEmpty() ? src.getMaxStackSize() : cur.getMaxStackSize(),
                        matrix.getInventoryStackLimit());
                if (cur.isEmpty() || cur.getCount() < cap) {
                    next.add(new int[]{entry[0], cur.isEmpty() ? 0 : cur.getCount()});
                }
            }
            candidates = next;
        }
        return moved;
    }

    public static boolean areItemsEqualIgnoringCount(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) {
            return a.isEmpty() && b.isEmpty();
        }
        if (a.getItem() != b.getItem()) {
            return false;
        }
        if (a.getMetadata() != b.getMetadata()) {
            return false;
        }
        return ItemStack.areItemStackTagsEqual(a, b);
    }

    private void syncToTrackingClients() {
        if (world == null || world.isRemote) {
            return;
        }
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        for (int i = 0; i < PATTERN_SIZE; i++) {
            pattern.set(i, ItemStack.EMPTY);
        }
        NBTTagList list = compound.getTagList("Pattern", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int idx = tag.getInteger("Slot");
            if (idx < 0 || idx >= PATTERN_SIZE) {
                continue;
            }
            ItemStack stack = new ItemStack(tag);
            if (!stack.isEmpty()) {
                stack.setCount(1);
            }
            pattern.set(idx, stack);
        }
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < PATTERN_SIZE; i++) {
            ItemStack stack = pattern.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBT(tag);
            tag.setInteger("Slot", i);
            list.appendTag(tag);
        }
        compound.setTag("Pattern", list);
        return compound;
    }
}
