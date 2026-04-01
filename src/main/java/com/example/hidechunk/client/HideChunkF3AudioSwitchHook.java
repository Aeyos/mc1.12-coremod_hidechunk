package com.example.hidechunk.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class HideChunkF3AudioSwitchHook {

    private static final Logger LOG = LogManager.getLogger("hidechunk");

    private static Field soundHandlerManagerField;
    private static Method reloadSoundSystemMethod;

    private HideChunkF3AudioSwitchHook() {
    }

    /**
     * @return true if the key was handled and vanilla should stop processing this F3 key.
     */
    public static boolean handleF3Key(int key) {
        if (key != Keyboard.KEY_S) {
            return false;
        }
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) {
                return true;
            }
            SoundHandler handler = mc.getSoundHandler();
            if (handler == null) {
                return true;
            }
            reloadSoundSystem(handler);
        } catch (Throwable t) {
            LOG.warn("F3+S audio output switch failed", t);
        }
        return true;
    }

    private static void reloadSoundSystem(SoundHandler handler) throws Exception {
        Object sndManager = readSndManager(handler);
        if (sndManager == null) {
            return;
        }
        Method m = resolveReloadSoundSystem(sndManager.getClass());
        if (m == null) {
            return;
        }
        m.invoke(sndManager);
    }

    private static Object readSndManager(SoundHandler handler) throws Exception {
        if (soundHandlerManagerField == null) {
            // dev: "sndManager", SRG: "field_147694_f"
            for (String name : new String[] { "sndManager", "field_147694_f" }) {
                try {
                    Field f = SoundHandler.class.getDeclaredField(name);
                    f.setAccessible(true);
                    soundHandlerManagerField = f;
                    break;
                } catch (NoSuchFieldException ignored) {
                }
            }
        }
        return soundHandlerManagerField != null ? soundHandlerManagerField.get(handler) : null;
    }

    private static Method resolveReloadSoundSystem(Class<?> soundManagerClass) {
        if (reloadSoundSystemMethod != null) {
            return reloadSoundSystemMethod;
        }
        try {
            // dev: "reloadSoundSystem", SRG: "func_148596_a"
            for (String name : new String[] { "reloadSoundSystem", "func_148596_a" }) {
                try {
                    Method m = soundManagerClass.getDeclaredMethod(name);
                    m.setAccessible(true);
                    reloadSoundSystemMethod = m;
                    return reloadSoundSystemMethod;
                } catch (NoSuchMethodException ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}

