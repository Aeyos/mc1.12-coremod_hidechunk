package com.example.hidechunk.core;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class HideChunkState {

    private static final class ChunkKey {

        final int dimension;
        final int chunkX;
        final int chunkZ;

        ChunkKey(int dimension, int chunkX, int chunkZ) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ChunkKey)) {
                return false;
            }
            ChunkKey chunkKey = (ChunkKey) o;
            return dimension == chunkKey.dimension
                    && chunkX == chunkKey.chunkX
                    && chunkZ == chunkKey.chunkZ;
        }

        @Override
        public int hashCode() {
            int result = dimension;
            result = 31 * result + chunkX;
            result = 31 * result + chunkZ;
            return result;
        }
    }

    private static final Set<ChunkKey> HIDDEN = ConcurrentHashMap.newKeySet();

    private HideChunkState() {
    }

    public static void hideChunk(int dimension, int chunkX, int chunkZ) {
        HIDDEN.add(new ChunkKey(dimension, chunkX, chunkZ));
    }

    public static void showChunk(int dimension, int chunkX, int chunkZ) {
        HIDDEN.remove(new ChunkKey(dimension, chunkX, chunkZ));
    }

    public static boolean isHidden(int dimension, int chunkX, int chunkZ) {
        return HIDDEN.contains(new ChunkKey(dimension, chunkX, chunkZ));
    }

    /** Clear all hidden columns (e.g. before client world teardown). */
    public static void clearAll() {
        HIDDEN.clear();
    }
}
