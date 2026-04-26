package com.example.hidechunk.assembler.network;

import com.example.hidechunk.assembler.TileUltimateAssembler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;

/**
 * Bulk pattern overwrite — used by JEI recipe transfer to push an entire 9x9 layout in one packet.
 */
public class PacketSetPattern implements IMessage {

    private BlockPos pos;
    private NonNullList<ItemStack> entries;

    public PacketSetPattern() {
    }

    public PacketSetPattern(BlockPos pos, NonNullList<ItemStack> entries) {
        this.pos = pos;
        this.entries = entries;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        entries = NonNullList.withSize(TileUltimateAssembler.PATTERN_SIZE, ItemStack.EMPTY);
        int count = buf.readInt();
        PacketBuffer pb = new PacketBuffer(buf);
        for (int i = 0; i < count; i++) {
            int idx = buf.readInt();
            boolean present = buf.readBoolean();
            if (!present) {
                continue;
            }
            try {
                NBTTagCompound tag = pb.readCompoundTag();
                ItemStack stack = tag == null ? ItemStack.EMPTY : new ItemStack(tag);
                if (idx >= 0 && idx < TileUltimateAssembler.PATTERN_SIZE) {
                    entries.set(idx, stack);
                }
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        int count = 0;
        for (ItemStack s : entries) {
            if (!s.isEmpty()) {
                count++;
            }
        }
        buf.writeInt(count);
        PacketBuffer pb = new PacketBuffer(buf);
        for (int i = 0; i < entries.size(); i++) {
            ItemStack s = entries.get(i);
            if (s.isEmpty()) {
                continue;
            }
            buf.writeInt(i);
            buf.writeBoolean(true);
            NBTTagCompound tag = new NBTTagCompound();
            s.writeToNBT(tag);
            pb.writeCompoundTag(tag);
        }
    }

    public static class Handler implements IMessageHandler<PacketSetPattern, IMessage> {
        @Override
        public IMessage onMessage(PacketSetPattern msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> apply(msg, player));
            return null;
        }

        private void apply(PacketSetPattern msg, EntityPlayerMP player) {
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
            tile.setPatternBulk(msg.entries);
        }
    }
}
