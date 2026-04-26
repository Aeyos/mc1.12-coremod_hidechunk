package com.example.hidechunk.assembler.jei;

import com.example.hidechunk.assembler.ContainerUltimateAssembler;
import com.example.hidechunk.assembler.TileUltimateAssembler;
import com.example.hidechunk.assembler.network.HideChunkNetwork;
import com.example.hidechunk.assembler.network.PacketSetPattern;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps a JEI crafting/EC table recipe layout into the top-left of the 9x9 ghost pattern, then
 * pushes a single {@link PacketSetPattern} to the server. Slot 0 is the output (skipped); slots
 * 1..N*N are the input grid in row-major order.
 */
public class AssemblerRecipeTransferHandler
        implements IRecipeTransferHandler<ContainerUltimateAssembler> {

    @Override
    public Class<ContainerUltimateAssembler> getContainerClass() {
        return ContainerUltimateAssembler.class;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(
            ContainerUltimateAssembler container,
            IRecipeLayout recipeLayout,
            EntityPlayer player,
            boolean maxTransfer,
            boolean doTransfer) {
        Map<Integer, ? extends IGuiIngredient<ItemStack>> all =
                recipeLayout.getItemStacks().getGuiIngredients();

        Map<Integer, ItemStack> inputs = new HashMap<>();
        int maxIdx = 0;
        for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> e : all.entrySet()) {
            int idx = e.getKey();
            IGuiIngredient<ItemStack> ing = e.getValue();
            if (!ing.isInput()) {
                continue;
            }
            ItemStack displayed = ing.getDisplayedIngredient();
            if (displayed == null) {
                continue;
            }
            if (idx <= 0) {
                continue;
            }
            inputs.put(idx, displayed);
            if (idx > maxIdx) {
                maxIdx = idx;
            }
        }
        if (inputs.isEmpty()) {
            return null;
        }
        int n = (int) Math.round(Math.sqrt(maxIdx));
        if (n <= 0 || n > TileUltimateAssembler.GRID_SIZE || n * n < maxIdx) {
            return null;
        }

        if (!doTransfer) {
            return null;
        }

        NonNullList<ItemStack> next = NonNullList.withSize(
                TileUltimateAssembler.PATTERN_SIZE, ItemStack.EMPTY);
        for (Map.Entry<Integer, ItemStack> entry : inputs.entrySet()) {
            int s = entry.getKey() - 1;
            int row = s / n;
            int col = s % n;
            int dest = row * TileUltimateAssembler.GRID_SIZE + col;
            if (dest < 0 || dest >= TileUltimateAssembler.PATTERN_SIZE) {
                continue;
            }
            ItemStack ghost = entry.getValue().copy();
            if (!ghost.isEmpty()) {
                ghost.setCount(1);
            }
            next.set(dest, ghost);
        }

        TileUltimateAssembler tile = container.getTile();
        tile.setPatternBulk(next);
        HideChunkNetwork.CHANNEL.sendToServer(new PacketSetPattern(tile.getPos(), next));
        return null;
    }
}
