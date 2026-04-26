package com.example.hidechunk.assembler.client;

import com.example.hidechunk.assembler.ContainerUltimateAssembler;
import com.example.hidechunk.assembler.SlotGhost;
import com.example.hidechunk.assembler.TileUltimateAssembler;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiUltimateAssembler extends GuiContainer {

    private static final int X_SIZE = 178;
    private static final int Y_SIZE = 270;

    private final TileUltimateAssembler tile;

    public GuiUltimateAssembler(InventoryPlayer playerInv, TileUltimateAssembler tile) {
        super(new ContainerUltimateAssembler(playerInv, tile));
        this.tile = tile;
        this.xSize = X_SIZE;
        this.ySize = Y_SIZE;
    }

    public TileUltimateAssembler getTile() {
        return tile;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format("gui.hidechunk.ultimate_assembler");
        fontRenderer.drawString(title, 8, 6, 0x404040);
        int invY = ContainerUltimateAssembler.GUI_TOP_PADDING
                + TileUltimateAssembler.GRID_SIZE * ContainerUltimateAssembler.SLOT_SIZE
                + 2;
        fontRenderer.drawString(I18n.format("container.inventory"), 8, invY + 2, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        int left = guiLeft;
        int top = guiTop;

        drawRect(left, top, left + xSize, top + ySize, 0xFFC6C6C6);
        drawRect(left + 4, top + 4, left + xSize - 4, top + 16, 0xFF8B8B8B);

        int gridLeft = left + ContainerUltimateAssembler.GUI_LEFT_PADDING - 1;
        int gridTop = top + ContainerUltimateAssembler.GUI_TOP_PADDING - 1;
        int gridSize = TileUltimateAssembler.GRID_SIZE * ContainerUltimateAssembler.SLOT_SIZE + 2;
        drawRect(gridLeft, gridTop, gridLeft + gridSize, gridTop + gridSize, 0xFF555555);
        for (int row = 0; row < TileUltimateAssembler.GRID_SIZE; row++) {
            for (int col = 0; col < TileUltimateAssembler.GRID_SIZE; col++) {
                int sx = left + ContainerUltimateAssembler.GUI_LEFT_PADDING + col * ContainerUltimateAssembler.SLOT_SIZE;
                int sy = top + ContainerUltimateAssembler.GUI_TOP_PADDING + row * ContainerUltimateAssembler.SLOT_SIZE;
                drawRect(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
                drawRect(sx + 1, sy + 1, sx + 15, sy + 15, 0xFFF0F0F0);
            }
        }

        int invY = top + ContainerUltimateAssembler.GUI_TOP_PADDING
                + TileUltimateAssembler.GRID_SIZE * ContainerUltimateAssembler.SLOT_SIZE + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = left + ContainerUltimateAssembler.GUI_LEFT_PADDING + col * ContainerUltimateAssembler.SLOT_SIZE;
                int sy = invY + row * ContainerUltimateAssembler.SLOT_SIZE;
                drawRect(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
                drawRect(sx + 1, sy + 1, sx + 15, sy + 15, 0xFFF0F0F0);
            }
        }
        int hotbarY = invY + 3 * ContainerUltimateAssembler.SLOT_SIZE + 4;
        for (int col = 0; col < 9; col++) {
            int sx = left + ContainerUltimateAssembler.GUI_LEFT_PADDING + col * ContainerUltimateAssembler.SLOT_SIZE;
            drawRect(sx, hotbarY, sx + 16, hotbarY + 16, 0xFF8B8B8B);
            drawRect(sx + 1, hotbarY + 1, sx + 15, hotbarY + 15, 0xFFF0F0F0);
        }
    }

    /** Public accessor for JEI ghost-handler hit-testing. */
    public Slot getSlotAtScreenPos(int mouseX, int mouseY) {
        return getSlotUnderMouse0(mouseX, mouseY);
    }

    private Slot getSlotUnderMouse0(int mouseX, int mouseY) {
        for (int i = 0; i < this.inventorySlots.inventorySlots.size(); ++i) {
            Slot slot = this.inventorySlots.inventorySlots.get(i);
            if (!(slot instanceof SlotGhost)) {
                continue;
            }
            int sx = guiLeft + slot.xPos;
            int sy = guiTop + slot.yPos;
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                return slot;
            }
        }
        return null;
    }

    public int getGuiLeftPublic() {
        return guiLeft;
    }

    public int getGuiTopPublic() {
        return guiTop;
    }
}
