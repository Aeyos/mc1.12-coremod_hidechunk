package com.example.hidechunk.assembler.network;

import com.example.hidechunk.assembler.TileUltimateAssembler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;

public class PacketSetGhost implements IMessage {

    private BlockPos pos;
    private int slot;
    private ItemStack stack;

    public PacketSetGhost() {
    }

    public PacketSetGhost(BlockPos pos, int slot, ItemStack stack) {
        this.pos = pos;
        this.slot = slot;
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        if (!this.stack.isEmpty()) {
            this.stack.setCount(1);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        long packed = buf.readLong();
        pos = BlockPos.fromLong(packed);
        slot = buf.readInt();
        boolean present = buf.readBoolean();
        if (!present) {
            stack = ItemStack.EMPTY;
            return;
        }
        try {
            NBTTagCompound tag = new PacketBuffer(buf).readCompoundTag();
            stack = tag == null ? ItemStack.EMPTY : new ItemStack(tag);
        } catch (IOException e) {
            stack = ItemStack.EMPTY;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeInt(slot);
        if (stack == null || stack.isEmpty()) {
            buf.writeBoolean(false);
            return;
        }
        buf.writeBoolean(true);
        NBTTagCompound tag = new NBTTagCompound();
        stack.writeToNBT(tag);
        new PacketBuffer(buf).writeCompoundTag(tag);
    }

    public static class Handler implements IMessageHandler<PacketSetGhost, IMessage> {
        @Override
        public IMessage onMessage(PacketSetGhost msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> apply(msg, player));
            return null;
        }

        private void apply(PacketSetGhost msg, EntityPlayerMP player) {
            if (!player.world.isBlockLoaded(msg.pos)) {
                return;
            }
            TileEntity te = player.world.getTileEntity(msg.pos);
            if (!(te instanceof TileUltimateAssembler)) {
                return;
            }
            TileUltimateAssembler tile = (TileUltimateAssembler) te;
            if (!tile.isUsableByPlayer(player)) {
                return;
            }
            if (msg.slot < 0 || msg.slot >= TileUltimateAssembler.PATTERN_SIZE) {
                return;
            }
            tile.setPatternStack(msg.slot, msg.stack);
        }
    }
}
