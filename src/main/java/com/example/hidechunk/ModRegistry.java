package com.example.hidechunk;

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

    private ModRegistry() {
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(PEARL_CHUNK_OBFUSCATION);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onRegisterModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(
                PEARL_CHUNK_OBFUSCATION,
                0,
                new ModelResourceLocation(PEARL_CHUNK_OBFUSCATION.getRegistryName(), "inventory")
        );
    }
}
