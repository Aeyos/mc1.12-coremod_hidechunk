package com.example.hidechunk.assembler.jei;

import com.example.hidechunk.assembler.client.GuiUltimateAssembler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;

@JEIPlugin
public class HideChunkJeiPlugin implements IModPlugin {

    /**
     * Real category UIDs registered by Extended Crafting (BlakeBr0 / Nomifactory fork). Verified
     * by disassembling {@code BasicTableCategory}, {@code AdvancedTableCategory},
     * {@code EliteTableCategory}, {@code UltimateTableCategory}.
     */
    private static final String[] EC_TABLE_UIDS = {
            "extendedcrafting:table_crafting_3x3",
            "extendedcrafting:table_crafting_5x5",
            "extendedcrafting:table_crafting_7x7",
            "extendedcrafting:table_crafting_9x9",
    };

    @Override
    public void register(IModRegistry registry) {
        AssemblerRecipeTransferHandler handler = new AssemblerRecipeTransferHandler();
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                handler, VanillaRecipeCategoryUid.CRAFTING);
        for (String uid : EC_TABLE_UIDS) {
            registry.getRecipeTransferRegistry().addRecipeTransferHandler(handler, uid);
        }

        registry.addGhostIngredientHandler(
                GuiUltimateAssembler.class, new AssemblerGhostIngredientHandler());
    }
}
