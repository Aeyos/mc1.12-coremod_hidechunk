package com.example.hidechunk.client;

import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;
import com.example.hidechunk.core.HideChunkState;

public final class HideChunkRenderHook {

    private HideChunkRenderHook() {
    }

    public static boolean shouldSkipRebuild(RenderChunk renderChunk, ChunkCompileTaskGenerator generator) {
        BlockPos pos = getRenderChunkPosition(renderChunk);
        if (pos == null) {
            return false;
        }

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        if (!HideChunkState.isHidden(chunkX, chunkZ)) {
            return false;
        }

        CompiledChunk empty = new CompiledChunk();
        generator.setCompiledChunk(empty);

        return true;
    }

    private static BlockPos getRenderChunkPosition(RenderChunk renderChunk) {
        try {
            return renderChunk.getPosition();
        } catch (Throwable ignored) {
        }

        try {
            java.lang.reflect.Field f;
            try {
                f = RenderChunk.class.getDeclaredField("position");
            } catch (NoSuchFieldException ex) {
                f = RenderChunk.class.getDeclaredField("field_178586_f");
            }
            f.setAccessible(true);
            return (BlockPos) f.get(renderChunk);
        } catch (Throwable ignored) {
        }

        return null;
    }
}
