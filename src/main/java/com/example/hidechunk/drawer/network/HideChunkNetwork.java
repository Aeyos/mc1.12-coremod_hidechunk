package com.example.hidechunk.drawer.network;

import com.example.hidechunk.HideChunkMod;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class HideChunkNetwork {

    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(HideChunkMod.MODID + ":drawer");

    private HideChunkNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(
                PacketDrawerSort.Handler.class,
                PacketDrawerSort.class,
                0,
                Side.SERVER);
        CHANNEL.registerMessage(
                PacketDrawerScroll.Handler.class,
                PacketDrawerScroll.class,
                1,
                Side.SERVER);
    }
}
