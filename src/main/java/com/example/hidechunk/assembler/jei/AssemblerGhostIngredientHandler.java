package com.example.hidechunk.assembler.jei;

import com.example.hidechunk.assembler.SlotGhost;
import com.example.hidechunk.assembler.TileUltimateAssembler;
import com.example.hidechunk.assembler.client.GuiUltimateAssembler;
import com.example.hidechunk.assembler.network.HideChunkNetwork;
import com.example.hidechunk.assembler.network.PacketSetGhost;
import mezz.jei.api.gui.IGhostIngredientHandler;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class AssemblerGhostIngredientHandler implements IGhostIngredientHandler<GuiUltimateAssembler> {

    @Override
    public <I> List<Target<I>> getTargets(GuiUltimateAssembler gui, I ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        if (!(ingredient instanceof ItemStack)) {
            return targets;
        }
        TileUltimateAssembler tile = gui.getTile();
        BlockPos pos = tile.getPos();
        int left = gui.getGuiLeftPublic();
        int top = gui.getGuiTopPublic();
        for (Slot slot : gui.inventorySlots.inventorySlots) {
            if (!(slot instanceof SlotGhost)) {
                continue;
            }
            int sx = left + slot.xPos;
            int sy = top + slot.yPos;
            int patternIdx = ((SlotGhost) slot).getPatternIndex();
            Rectangle rect = new Rectangle(sx, sy, 16, 16);
            targets.add(new GhostTarget<>(rect, pos, patternIdx, tile));
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }

    private static final class GhostTarget<I> implements Target<I> {

        private final Rectangle rect;
        private final BlockPos pos;
        private final int patternIdx;
        private final TileUltimateAssembler tile;

        GhostTarget(Rectangle rect, BlockPos pos, int patternIdx, TileUltimateAssembler tile) {
            this.rect = rect;
            this.pos = pos;
            this.patternIdx = patternIdx;
            this.tile = tile;
        }

        @Override
        public Rectangle getArea() {
            return rect;
        }

        @Override
        public void accept(I ingredient) {
            if (!(ingredient instanceof ItemStack)) {
                return;
            }
            ItemStack stack = ((ItemStack) ingredient).copy();
            if (!stack.isEmpty()) {
                stack.setCount(1);
            }
            tile.setPatternStack(patternIdx, stack);
            HideChunkNetwork.CHANNEL.sendToServer(new PacketSetGhost(pos, patternIdx, stack));
        }
    }
}
