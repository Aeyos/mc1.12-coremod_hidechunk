package com.example.hidechunk.assembler.network;

import com.example.hidechunk.HideChunkMod;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class HideChunkNetwork {

    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(HideChunkMod.MODID + ":assembler");

    private HideChunkNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(
                PacketSetGhost.Handler.class,
                PacketSetGhost.class,
                0,
                Side.SERVER);
        CHANNEL.registerMessage(
                PacketSetPattern.Handler.class,
                PacketSetPattern.class,
                1,
                Side.SERVER);
    }
}
