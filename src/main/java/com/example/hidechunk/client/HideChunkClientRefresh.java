package com.example.hidechunk.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Marks a chunk column dirty and reloads chunk renderers so mesh rebuild runs through the obfuscated {@code renderBlock} hook.
 */
@SideOnly(Side.CLIENT)
public final class HideChunkClientRefresh {

    private static final Logger LOG = LogManager.getLogger("hidechunk");

    private HideChunkClientRefresh() {
    }

    public static void scheduleColumnMeshRebuild(World world, int chunkX, int chunkZ) {
        if (world == null || !world.isRemote) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world != world) {
            return;
        }
        mc.addScheduledTask(() -> {
            if (mc.world == null || mc.renderGlobal == null) {
                return;
            }
            mc.world.markBlockRangeForRenderUpdate(
                    chunkX << 4, 0, chunkZ << 4, (chunkX << 4) + 15, 255, (chunkZ << 4) + 15);
            try {
                mc.renderGlobal.loadRenderers();
            } catch (Throwable t) {
                LOG.warn("scheduleColumnMeshRebuild", t);
            }
        });
    }
}
