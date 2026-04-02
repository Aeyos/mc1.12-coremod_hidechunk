package com.example.hidechunk;

import com.example.hidechunk.command.CommandHideChunk;
import com.example.hidechunk.drawer.DrawerGuiHandler;
import com.example.hidechunk.drawer.TileDrawerCapacitor;
import com.example.hidechunk.drawer.network.HideChunkNetwork;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = HideChunkMod.MODID, name = HideChunkMod.NAME, version = HideChunkMod.VERSION)
public class HideChunkMod {

    public static final String MODID = "hidechunk";
    public static final String NAME = "Hide Chunk";
    public static final String VERSION = "1.0.0";

    public static final int GUI_DRAWER_CAPACITOR = 1;

    @Mod.Instance(HideChunkMod.MODID)
    public static HideChunkMod INSTANCE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerTileEntity(
                TileDrawerCapacitor.class,
                new ResourceLocation(MODID, "drawer_capacitor_wonders"));
        HideChunkNetwork.init();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(INSTANCE, new DrawerGuiHandler());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandHideChunk());
    }
}
