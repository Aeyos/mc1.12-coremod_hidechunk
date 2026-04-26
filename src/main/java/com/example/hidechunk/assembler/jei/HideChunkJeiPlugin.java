package com.example.hidechunk.assembler.jei;

import com.example.hidechunk.assembler.client.GuiUltimateAssembler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;

@JEIPlugin
public class HideChunkJeiPlugin implements IModPlugin {

    @Override
    public void register(IModRegistry registry) {
        AssemblerRecipeTransferHandler handler = new AssemblerRecipeTransferHandler();
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                handler, VanillaRecipeCategoryUid.CRAFTING);
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                handler, "extendedcrafting:table");
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(
                handler, "extendedcrafting:combination");

        registry.addGhostIngredientHandler(
                GuiUltimateAssembler.class, new AssemblerGhostIngredientHandler());
    }
}
