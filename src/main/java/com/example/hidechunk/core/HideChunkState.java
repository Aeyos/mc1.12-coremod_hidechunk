package com.example.hidechunk.core;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class HideChunkState {

    private static final Set<Long> HIDDEN = ConcurrentHashMap.newKeySet();

    private HideChunkState() {
    }

    public static void hideChunk(int chunkX, int chunkZ) {
        HIDDEN.add(toKey(chunkX, chunkZ));
    }

    public static void showChunk(int chunkX, int chunkZ) {
        HIDDEN.remove(toKey(chunkX, chunkZ));
    }

    public static boolean isHidden(int chunkX, int chunkZ) {
        return HIDDEN.contains(toKey(chunkX, chunkZ));
    }

    private static long toKey(int x, int z) {
        return ((long) x & 4294967295L) | (((long) z & 4294967295L) << 32);
    }
}
