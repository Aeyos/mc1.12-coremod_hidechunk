package com.example.hidechunk;

import com.example.hidechunk.assembler.BlockUltimateAssembler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
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

    public static final Block ULTIMATE_ASSEMBLER = new BlockUltimateAssembler()
            .setRegistryName(HideChunkMod.MODID, "ultimate_assembler");

    public static final Item ITEM_ULTIMATE_ASSEMBLER =
            new ItemBlock(ULTIMATE_ASSEMBLER)
                    .setRegistryName(ULTIMATE_ASSEMBLER.getRegistryName());

    private ModRegistry() {
    }

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(ULTIMATE_ASSEMBLER);
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(PEARL_CHUNK_OBFUSCATION);
        event.getRegistry().register(ITEM_ULTIMATE_ASSEMBLER);
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
                ITEM_ULTIMATE_ASSEMBLER,
                0,
                new ModelResourceLocation(ITEM_ULTIMATE_ASSEMBLER.getRegistryName(), "inventory")
        );
    }
}
