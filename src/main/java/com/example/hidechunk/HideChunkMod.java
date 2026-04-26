package com.example.hidechunk;

import com.example.hidechunk.assembler.AssemblerGuiHandler;
import com.example.hidechunk.assembler.TileUltimateAssembler;
import com.example.hidechunk.assembler.network.HideChunkNetwork;
import com.example.hidechunk.command.CommandHideChunk;
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

    public static final int GUI_ULTIMATE_ASSEMBLER = 1;

    @Mod.Instance(HideChunkMod.MODID)
    public static HideChunkMod INSTANCE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerTileEntity(
                TileUltimateAssembler.class,
                new ResourceLocation(MODID, "ultimate_assembler"));
        HideChunkNetwork.init();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(INSTANCE, new AssemblerGuiHandler());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandHideChunk());
    }
}
