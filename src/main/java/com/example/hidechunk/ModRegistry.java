package com.example.hidechunk;

import com.example.hidechunk.drawer.BlockDrawerCapacitor;
import com.example.hidechunk.drawer.ItemBlockDrawerCapacitor;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = HideChunkMod.MODID)
public final class ModRegistry {

    public static final Item PEARL_CHUNK_OBFUSCATION = new ItemPearlChunkObfuscation()
            .setRegistryName(HideChunkMod.MODID, "pearl_chunk_obfuscation")
            .setTranslationKey(HideChunkMod.MODID + ".pearl_chunk_obfuscation");

    public static final Block DRAWER_CAPACITOR_WONDERS = new BlockDrawerCapacitor()
            .setRegistryName(HideChunkMod.MODID, "drawer_capacitor_wonders");

    public static final Item ITEM_DRAWER_CAPACITOR_WONDERS =
            new ItemBlockDrawerCapacitor(DRAWER_CAPACITOR_WONDERS)
                    .setRegistryName(DRAWER_CAPACITOR_WONDERS.getRegistryName());

    private ModRegistry() {
    }

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(DRAWER_CAPACITOR_WONDERS);
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(PEARL_CHUNK_OBFUSCATION);
        event.getRegistry().register(ITEM_DRAWER_CAPACITOR_WONDERS);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onRegisterModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(
                PEARL_CHUNK_OBFUSCATION,
                0,
                new ModelResourceLocation(PEARL_CHUNK_OBFUSCATION.getRegistryName(), "inventory")
        );
        ModelLoader.setCustomModelResourceLocation(
                ITEM_DRAWER_CAPACITOR_WONDERS,
                0,
                new ModelResourceLocation(ITEM_DRAWER_CAPACITOR_WONDERS.getRegistryName(), "inventory")
        );
    }
}
