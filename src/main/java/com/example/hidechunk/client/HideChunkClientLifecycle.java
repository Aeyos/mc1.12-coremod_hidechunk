package com.example.hidechunk.client;

import com.example.hidechunk.HideChunkMod;
import com.example.hidechunk.core.HideChunkState;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Clears hidden-column state when the active client world unloads so hidden masks are not carried across
 * quit / dimension teardown. Reloads renderers so the next world starts with untouched meshes.
 */
@Mod.EventBusSubscriber(modid = HideChunkMod.MODID, value = Side.CLIENT)
public final class HideChunkClientLifecycle {

    private HideChunkClientLifecycle() {
    }

    @SubscribeEvent
    public static void onClientWorldUnload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        // Only clear when this unload is for the world the client is rendering (not another dimension).
        if (mc.world != event.getWorld()) {
            return;
        }
        resetClientRenderersAfterTamper(mc);
    }

    @SubscribeEvent
    public static void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            resetClientRenderersAfterTamper(mc);
        }
    }

    private static void resetClientRenderersAfterTamper(Minecraft mc) {
        HideChunkState.clearAll();
        mc.addScheduledTask(() -> {
            if (mc.renderGlobal == null || mc.world == null) {
                return;
            }
            try {
                mc.renderGlobal.loadRenderers();
            } catch (Throwable ignored) {
            }
        });
    }
}
