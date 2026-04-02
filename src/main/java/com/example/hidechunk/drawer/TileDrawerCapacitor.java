package com.example.hidechunk.drawer;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TileDrawerCapacitor extends TileEntity implements ITickable {

    public static final int COLS = 9;
    public static final int VISIBLE_ROWS = 3;
    public static final int VISIBLE_SLOTS = COLS * VISIBLE_ROWS;
    public static final int RF_CAPACITY = 100_000;
    public static final int RF_PER_CAPACITOR = 1_000;
    public static final int MAX_STORAGE = 10_000;

    private static final ResourceLocation CAPACITOR_ITEM =
            new ResourceLocation("enderio", "item_basic_capacitor");

    /**
     * Internal buffer: {@link EnergyStorage} only applies {@link EnergyStorage#extractEnergy} when
     * {@code maxExtract > 0}. We need extraction for catalogue RF cost, but must not leak power to cables,
     * so we expose {@link #energyCap} to the world and use this field only inside the tile.
     */
    private final EnergyStorage energyStorage = new EnergyStorage(RF_CAPACITY, RF_CAPACITY, RF_CAPACITY) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate
                    && received > 0
                    && TileDrawerCapacitor.this.world != null
                    && !TileDrawerCapacitor.this.world.isRemote) {
                TileDrawerCapacitor.this.markDirty();
                TileDrawerCapacitor.this.syncToTrackingClients();
            }
            return received;
        }
    };

    /** Exposed capability: accept RF in, never output (drawer is not a power source). */
    private final IEnergyStorage energyCap = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return energyStorage.receiveEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energyStorage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyStorage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return energyStorage.canReceive();
        }
    };
    private final List<ItemStack> storage = NonNullList.create();
    /** Permutation: display position -> index in {@link #storage}. */
    private final List<Integer> displayOrder = new ArrayList<>();

    private int sortMode;
    private int scrollRowOffset;

    private final ItemStackHandler automationSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            TileDrawerCapacitor.this.markDirtyAndSync();
        }
    };

    public TileDrawerCapacitor() {
        rebuildDisplayOrder();
    }

    public static boolean isLootCapacitor(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = stack.getItem().getRegistryName();
        if (id != null && CAPACITOR_ITEM.equals(id)) {
            return true;
        }
        if (id != null
                && "enderio".equals(id.getNamespace())
                && stack.hasTagCompound()) {
            NBTTagCompound root = stack.getTagCompound();
            if (root != null && root.hasKey(CapacitorSortHelper.EIOCAP, Constants.NBT.TAG_COMPOUND)) {
                return true;
            }
        }
        return false;
    }

    public IEnergyStorage getEnergy() {
        return energyCap;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getSortMode() {
        return sortMode;
    }

    public void setSortMode(int mode) {
        int max = CapacitorSortHelper.SORT_KEYS.length;
        if (mode < 0 || mode > max) {
            mode = 0;
        }
        if (sortMode != mode) {
            sortMode = mode;
            rebuildDisplayOrder();
            markDirtyAndSync();
        }
    }

    public int getScrollRowOffset() {
        return scrollRowOffset;
    }

    public void setScrollRowOffset(int rows) {
        int max = maxScrollRows();
        int clamped = Math.max(0, Math.min(rows, max));
        if (clamped != scrollRowOffset) {
            scrollRowOffset = clamped;
            markDirtyAndSync();
        }
    }

    public int maxScrollRows() {
        int rows = (storage.size() + COLS - 1) / COLS;
        return Math.max(0, rows - VISIBLE_ROWS);
    }

    public int getStorageSize() {
        return storage.size();
    }

    public void rebuildDisplayOrder() {
        displayOrder.clear();
        for (int i = 0; i < storage.size(); i++) {
            displayOrder.add(i);
        }
        displayOrder.sort((ia, ib) -> {
            ItemStack sa = storage.get(ia);
            ItemStack sb = storage.get(ib);
            int c = CapacitorSortHelper.compareForSort(sortMode, sa, sb);
            if (c != 0) {
                return c;
            }
            return Integer.compare(ia, ib);
        });
        int max = maxScrollRows();
        if (scrollRowOffset > max) {
            scrollRowOffset = max;
        }
    }

    /**
     * Global display index (all rows, not windowed). {@code -1} if out of range.
     */
    public int storageIndexForGlobalDisplay(int globalSlot) {
        if (globalSlot < 0 || globalSlot >= displayOrder.size()) {
            return -1;
        }
        return displayOrder.get(globalSlot);
    }

    public boolean isAppendGlobalSlot(int globalSlot) {
        return globalSlot >= storage.size();
    }

    public ItemStack getStackForViewSlot(int viewSlot) {
        int global = scrollRowOffset * COLS + viewSlot;
        int sidx = storageIndexForGlobalDisplay(global);
        if (sidx < 0) {
            return ItemStack.EMPTY;
        }
        return storage.get(sidx).copy();
    }

    /**
     * Removes stack at view slot (player pickup). Server only.
     */
    public ItemStack extractFromViewSlot(int viewSlot, int amount, boolean simulate) {
        if (world == null || world.isRemote) {
            return ItemStack.EMPTY;
        }
        int global = scrollRowOffset * COLS + viewSlot;
        int sidx = storageIndexForGlobalDisplay(global);
        if (sidx < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack here = storage.get(sidx);
        if (here.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int take = Math.min(amount, here.getCount());
        if (simulate) {
            ItemStack copy = here.copy();
            copy.setCount(take);
            return copy;
        }
        ItemStack out = here.splitStack(take);
        if (here.isEmpty()) {
            storage.remove(sidx);
        }
        rebuildDisplayOrder();
        markDirtyAndSync();
        return out;
    }

    /**
     * Inserts into the first empty display position (append). Returns remainder.
     */
    @Nonnull
    public ItemStack insertViaView(int viewSlot, ItemStack stack, boolean simulate) {
        if (world == null || world.isRemote || stack.isEmpty()) {
            return stack;
        }
        if (!isLootCapacitor(stack)) {
            return stack;
        }
        int global = scrollRowOffset * COLS + viewSlot;
        // Only the linear "tail" past stored items may accept inserts (any empty cell in the grid).
        if (global < storage.size()) {
            return stack;
        }
        return appendCapacitors(stack, simulate);
    }

    @Nonnull
    public ItemStack appendCapacitors(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isLootCapacitor(stack)) {
            return stack;
        }
        int space = MAX_STORAGE - storage.size();
        if (space <= 0) {
            return stack;
        }
        int n = Math.min(stack.getCount(), space);
        int cost = RF_PER_CAPACITOR * n;
        if (energyStorage.extractEnergy(cost, true) < cost) {
            int affordable = energyStorage.getEnergyStored() / RF_PER_CAPACITOR;
            if (affordable <= 0) {
                return stack;
            }
            n = Math.min(n, affordable);
            cost = RF_PER_CAPACITOR * n;
        }
        if (simulate) {
            ItemStack rest = stack.copy();
            rest.shrink(n);
            return rest;
        }
        if (energyStorage.extractEnergy(cost, false) < cost) {
            return stack;
        }
        for (int i = 0; i < n; i++) {
            ItemStack one = stack.copy();
            one.setCount(1);
            storage.add(one);
        }
        rebuildDisplayOrder();
        markDirtyAndSync();
        ItemStack rest = stack.copy();
        rest.shrink(n);
        return rest;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        drainAutomationSlot();
    }

    private void drainAutomationSlot() {
        ItemStack s = automationSlot.getStackInSlot(0);
        if (s.isEmpty()) {
            return;
        }
        ItemStack left = appendCapacitors(s, false);
        automationSlot.setStackInSlot(0, left);
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
        energyStorage.extractEnergy(energyStorage.getEnergyStored(), false);
        if (compound.hasKey("Energy", Constants.NBT.TAG_INT)) {
            energyStorage.receiveEnergy(compound.getInteger("Energy"), false);
        }
        sortMode = compound.getInteger("SortMode");
        scrollRowOffset = compound.getInteger("Scroll");
        storage.clear();
        NBTTagList list = compound.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            ItemStack stack = new ItemStack(tag);
            if (!stack.isEmpty()) {
                storage.add(stack);
            }
        }
        if (compound.hasKey("AutomationSlot", Constants.NBT.TAG_COMPOUND)) {
            automationSlot.deserializeNBT(compound.getCompoundTag("AutomationSlot"));
        }
        rebuildDisplayOrder();
    }

    private void markDirtyAndSync() {
        markDirty();
        syncToTrackingClients();
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Energy", energyStorage.getEnergyStored());
        compound.setInteger("SortMode", sortMode);
        compound.setInteger("Scroll", scrollRowOffset);
        NBTTagList list = new NBTTagList();
        for (ItemStack stack : storage) {
            NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBT(tag);
            list.appendTag(tag);
        }
        compound.setTag("Items", list);
        compound.setTag("AutomationSlot", automationSlot.serializeNBT());
        return compound;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return true;
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return facing == EnumFacing.UP || facing == null;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energyCap);
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                && (facing == EnumFacing.UP || facing == null)) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(automationSlot);
        }
        return super.getCapability(capability, facing);
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        if (world == null || getWorld().getTileEntity(pos) != this) {
            return false;
        }
        return player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    /**
     * Direct update for a visible slot (used by {@link SlotItemHandler#putStack}).
     */
    public void setViewSlotContents(int viewSlot, ItemStack stack) {
        if (world == null || world.isRemote) {
            return;
        }
        if (!stack.isEmpty() && !isLootCapacitor(stack)) {
            return;
        }
        int global = scrollRowOffset * COLS + viewSlot;
        int sidx = storageIndexForGlobalDisplay(global);
        if (sidx >= 0) {
            if (stack.isEmpty()) {
                storage.remove(sidx);
            } else {
                storage.set(sidx, stack.copy());
            }
            rebuildDisplayOrder();
            markDirtyAndSync();
            return;
        }
        if (!stack.isEmpty() && global >= storage.size()) {
            ItemStack rest = appendCapacitors(stack, false);
            if (!rest.isEmpty()) {
                spawnAsEntityItem(rest);
            }
        }
    }

    private void spawnAsEntityItem(ItemStack rest) {
        if (world == null || world.isRemote || rest.isEmpty()) {
            return;
        }
        EntityItem entity = new EntityItem(
                world,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                rest);
        entity.setPickupDelay(10);
        world.spawnEntity(entity);
    }

    public void dropAllStoredItems() {
        if (world == null || world.isRemote) {
            return;
        }
        for (ItemStack stack : new ArrayList<>(storage)) {
            if (!stack.isEmpty()) {
                spawnAsEntityItem(stack.copy());
            }
        }
        storage.clear();
        rebuildDisplayOrder();
        markDirtyAndSync();
    }

    public void dropAutomationSlot() {
        if (world == null || world.isRemote) {
            return;
        }
        ItemStack s = automationSlot.getStackInSlot(0);
        if (!s.isEmpty()) {
            spawnAsEntityItem(s.copy());
            automationSlot.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    public DrawerItemHandler createViewHandler() {
        return new DrawerItemHandler(this);
    }

    /**
     * Item handler for the 27 visible slots (windowed by scroll).
     */
    public static final class DrawerItemHandler implements IItemHandlerModifiable {

        private final TileDrawerCapacitor tile;

        DrawerItemHandler(TileDrawerCapacitor tile) {
            this.tile = tile;
        }

        @Override
        public int getSlots() {
            return VISIBLE_SLOTS;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return tile.getStackForViewSlot(slot);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return tile.insertViaView(slot, stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return tile.extractFromViewSlot(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return isLootCapacitor(stack);
        }

        @Override
        public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
            tile.setViewSlotContents(slot, stack);
        }
    }
}
