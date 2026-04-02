package com.example.hidechunk.drawer.network;

import com.example.hidechunk.drawer.TileDrawerCapacitor;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketDrawerScroll implements IMessage {

    private BlockPos pos;
    private int scrollRows;

    public PacketDrawerScroll() {
    }

    public PacketDrawerScroll(BlockPos pos, int scrollRows) {
        this.pos = pos;
        this.scrollRows = scrollRows;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        scrollRows = buf.readShort();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeShort(scrollRows);
    }

    public static class Handler implements IMessageHandler<PacketDrawerScroll, IMessage> {

        @Override
        public IMessage onMessage(PacketDrawerScroll message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.world.getTileEntity(message.pos);
                if (te instanceof TileDrawerCapacitor) {
                    ((TileDrawerCapacitor) te).setScrollRowOffset(message.scrollRows);
                }
            });
            return null;
        }
    }
}
