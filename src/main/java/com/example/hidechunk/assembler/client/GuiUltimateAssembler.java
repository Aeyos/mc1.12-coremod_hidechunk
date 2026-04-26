package com.example.hidechunk.assembler.client;

import com.example.hidechunk.assembler.ContainerUltimateAssembler;
import com.example.hidechunk.assembler.SlotGhost;
import com.example.hidechunk.assembler.TileUltimateAssembler;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiUltimateAssembler extends GuiContainer {

    /** Vanilla double-chest GUI texture, reused so the assembler matches the chest look. */
    private static final ResourceLocation BG_TEX =
            new ResourceLocation("textures/gui/container/generic_54.png");

    /** Source rect of one slot row in the chest texture (y=17, height=18). */
    private static final int SLOT_ROW_SRC_Y = 17;
    private static final int SLOT_ROW_SRC_H = 18;
    /** Source rect of the player-inventory section (label gap + 3 rows + hotbar + border). */
    private static final int PLAYER_SRC_Y = 125;
    private static final int PLAYER_SRC_H = 97;

    private static final int X_SIZE = 176;
    private static final int Y_SIZE = 17 + TileUltimateAssembler.GRID_SIZE * 18 + PLAYER_SRC_H;

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
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        this.mc.getTextureManager().bindTexture(BG_TEX);
        int left = guiLeft;
        int top = guiTop;

        // Top border + title strip (matches vanilla chest)
        drawTexturedModalRect(left, top, 0, 0, xSize, SLOT_ROW_SRC_Y);
        // Repeat one slot row of the chest texture for each of our 9 grid rows
        for (int row = 0; row < TileUltimateAssembler.GRID_SIZE; row++) {
            int dy = top + SLOT_ROW_SRC_Y + row * SLOT_ROW_SRC_H;
            drawTexturedModalRect(left, dy, 0, SLOT_ROW_SRC_Y, xSize, SLOT_ROW_SRC_H);
        }
        // Player-inventory section drawn straight from the bottom of the chest texture
        int playerY = top + SLOT_ROW_SRC_Y + TileUltimateAssembler.GRID_SIZE * SLOT_ROW_SRC_H;
        drawTexturedModalRect(left, playerY, 0, PLAYER_SRC_Y, xSize, PLAYER_SRC_H);
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
