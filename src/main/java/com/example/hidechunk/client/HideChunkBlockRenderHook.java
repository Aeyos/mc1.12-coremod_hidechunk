package com.example.hidechunk.client;

import java.lang.reflect.Field;

import com.example.hidechunk.core.HideChunkState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Called from bytecode patched into {@link net.minecraft.client.renderer.BlockRendererDispatcher#renderBlock}
 * so hidden chunk columns rebuild with no block meshes (air-only), without nulling frustum slots or forcing dummy compiled chunks.
 */
@SideOnly(Side.CLIENT)
public final class HideChunkBlockRenderHook {

    private static Field chunkCacheWorldField;

    private HideChunkBlockRenderHook() {
    }

    public static boolean shouldSkipBlockMesh(IBlockAccess worldIn, BlockPos pos) {
        if (worldIn == null || pos == null) {
            return false;
        }
        World world = resolveWorld(worldIn);
        if (world == null) {
            return false;
        }
        int dim = world.provider.getDimension();
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        return HideChunkState.isHidden(dim, cx, cz);
    }

    private static World resolveWorld(IBlockAccess access) {
        if (access instanceof World) {
            return (World) access;
        }
        if (access instanceof ChunkCache) {
            return readChunkCacheWorld((ChunkCache) access);
        }
        return null;
    }

    private static World readChunkCacheWorld(ChunkCache cache) {
        try {
            if (chunkCacheWorldField == null) {
                for (String name : new String[] { "world", "field_72815_e" }) {
                    try {
                        Field f = ChunkCache.class.getDeclaredField(name);
                        f.setAccessible(true);
                        chunkCacheWorldField = f;
                        break;
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            }
            if (chunkCacheWorldField != null) {
                Object w = chunkCacheWorldField.get(cache);
                if (w instanceof World) {
                    return (World) w;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
