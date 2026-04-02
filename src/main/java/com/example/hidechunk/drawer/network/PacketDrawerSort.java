package com.example.hidechunk.drawer.network;

import com.example.hidechunk.drawer.TileDrawerCapacitor;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketDrawerSort implements IMessage {

    private BlockPos pos;
    private int sortMode;

    public PacketDrawerSort() {
    }

    public PacketDrawerSort(BlockPos pos, int sortMode) {
        this.pos = pos;
        this.sortMode = sortMode;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        sortMode = buf.readByte() & 0xFF;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeByte(sortMode);
    }

    public static class Handler implements IMessageHandler<PacketDrawerSort, IMessage> {

        @Override
        public IMessage onMessage(PacketDrawerSort message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.world.getTileEntity(message.pos);
                if (te instanceof TileDrawerCapacitor) {
                    ((TileDrawerCapacitor) te).setSortMode(message.sortMode);
                }
            });
            return null;
        }
    }
}
