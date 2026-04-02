package com.example.hidechunk.drawer.client;

import com.example.hidechunk.drawer.CapacitorSortHelper;
import com.example.hidechunk.drawer.ContainerDrawerCapacitor;
import com.example.hidechunk.drawer.TileDrawerCapacitor;
import com.example.hidechunk.drawer.network.HideChunkNetwork;
import com.example.hidechunk.drawer.network.PacketDrawerScroll;
import com.example.hidechunk.drawer.network.PacketDrawerSort;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiDrawerCapacitor extends GuiContainer {

    private static final int BTN_START_X = 8;
    private static final int BTN_START_Y = 6;
    private static final int BTN_W = 16;
    private static final int BTN_H = 12;
    private static final int BTN_GAP = 1;

    private static final int SCROLL_X = 170;
    private static final int SCROLL_Y = 24;
    private static final int SCROLL_W = 8;
    private static final int SCROLL_H = 54;

    private final TileDrawerCapacitor tile;
    private int clientEnergy;
    private int clientScroll;
    private boolean draggingScroll;

    public GuiDrawerCapacitor(InventoryPlayer playerInv, TileDrawerCapacitor tile) {
        super(new ContainerDrawerCapacitor(playerInv, tile));
        this.tile = tile;
        this.clientEnergy = tile.getEnergyStored();
        this.clientScroll = tile.getScrollRowOffset();
        this.xSize = 184;
        this.ySize = 194;
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
        drawButtonTooltips(mouseX, mouseY);
    }

    private void drawButtonTooltips(int mouseX, int mouseY) {
        int gx = mouseX - guiLeft;
        int gy = mouseY - guiTop;
        int numButtons = 1 + CapacitorSortHelper.SORT_KEYS.length;
        for (int i = 0; i < numButtons; i++) {
            if (inSortButton(i, gx, gy)) {
                List<String> lines = new ArrayList<>();
                if (i == 0) {
                    lines.add(TextFormatting.GOLD + I18n.format("gui.hidechunk.drawer.sort_all"));
                } else {
                    lines.add(TextFormatting.AQUA + CapacitorSortHelper.SORT_KEYS[i - 1]);
                    lines.add(TextFormatting.GRAY + I18n.format("gui.hidechunk.drawer.sort_by_key"));
                }
                drawHoveringText(lines, mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String energy = I18n.format(
                "gui.hidechunk.drawer.energy",
                clientEnergy,
                TileDrawerCapacitor.RF_CAPACITY);
        fontRenderer.drawString(energy, 8, 88, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        int left = guiLeft;
        int top = guiTop;
        drawRect(left, top, left + xSize, top + ySize, 0xFFC6C6C6);
        drawRect(left + 6, top + 4, left + xSize - 6, top + 4 + BTN_H + 2, 0xFF8B8B8B);

        int numButtons = 1 + CapacitorSortHelper.SORT_KEYS.length;
        for (int i = 0; i < numButtons; i++) {
            int bx = left + BTN_START_X + i * (BTN_W + BTN_GAP);
            int by = top + BTN_START_Y;
            boolean active = tile.getSortMode() == i;
            int bg = active ? 0xFF6BA3FF : 0xFFAAAAAA;
            drawRect(bx, by, bx + BTN_W, by + BTN_H, bg);
            drawRect(bx, by, bx + BTN_W, by + 1, 0xFFFFFFFF);
            drawRect(bx, by + BTN_H - 1, bx + BTN_W, by + BTN_H, 0xFF555555);
            String label = i == 0 ? "All" : CapacitorSortHelper.sortButtonLabel(i);
            fontRenderer.drawString(
                    label,
                    bx + (BTN_W - fontRenderer.getStringWidth(label)) / 2,
                    by + 2,
                    0x202020);
        }

        for (int row = 0; row < TileDrawerCapacitor.VISIBLE_ROWS; row++) {
            for (int col = 0; col < TileDrawerCapacitor.COLS; col++) {
                int sx = left + 7 + col * 18;
                int sy = top + 23 + row * 18;
                drawRect(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
                drawRect(sx + 1, sy + 1, sx + 15, sy + 15, 0xFFF0F0F0);
            }
        }

        int stLeft = left + SCROLL_X;
        int stTop = top + SCROLL_Y;
        drawRect(stLeft, stTop, stLeft + SCROLL_W, stTop + SCROLL_H, 0xFF555555);
        int maxScroll = maxScrollRowsClient();
        if (maxScroll > 0) {
            float frac = clientScroll / (float) maxScroll;
            int thumbH = Math.max(6, SCROLL_H * TileDrawerCapacitor.VISIBLE_ROWS
                    / Math.max(TileDrawerCapacitor.VISIBLE_ROWS, rowsForItemCount()));
            int thumbY = stTop + (int) ((SCROLL_H - thumbH) * frac);
            drawRect(stLeft + 1, thumbY, stLeft + SCROLL_W - 1, thumbY + thumbH, 0xFFCCCCCC);
        }
    }

    private int rowsForItemCount() {
        int n = tile.getStorageSize();
        return Math.max(TileDrawerCapacitor.VISIBLE_ROWS, (n + TileDrawerCapacitor.COLS - 1) / TileDrawerCapacitor.COLS);
    }

    private int maxScrollRowsClient() {
        int rows = (tile.getStorageSize() + TileDrawerCapacitor.COLS - 1) / TileDrawerCapacitor.COLS;
        return Math.max(0, rows - TileDrawerCapacitor.VISIBLE_ROWS);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) {
            return;
        }
        int gx = mouseX - guiLeft;
        int gy = mouseY - guiTop;
        int numButtons = 1 + CapacitorSortHelper.SORT_KEYS.length;
        for (int i = 0; i < numButtons; i++) {
            if (inSortButton(i, gx, gy)) {
                HideChunkNetwork.CHANNEL.sendToServer(new PacketDrawerSort(tile.getPos(), i));
                return;
            }
        }
        if (gx >= SCROLL_X && gx < SCROLL_X + SCROLL_W && gy >= SCROLL_Y && gy < SCROLL_Y + SCROLL_H) {
            draggingScroll = true;
            updateScrollFromMouseY(gy);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingScroll = false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (draggingScroll && clickedMouseButton == 0) {
            updateScrollFromMouseY(mouseY - guiTop);
        }
    }

    private void updateScrollFromMouseY(int gy) {
        int rel = gy - SCROLL_Y;
        int max = maxScrollRowsClient();
        if (max <= 0) {
            return;
        }
        rel = MathHelper.clamp(rel, 0, SCROLL_H);
        int row = (int) Math.round(rel / (double) SCROLL_H * max);
        row = MathHelper.clamp(row, 0, max);
        HideChunkNetwork.CHANNEL.sendToServer(new PacketDrawerScroll(tile.getPos(), row));
    }

    private boolean inSortButton(int id, int gx, int gy) {
        int numButtons = 1 + CapacitorSortHelper.SORT_KEYS.length;
        if (id < 0 || id >= numButtons) {
            return false;
        }
        int bx = BTN_START_X + id * (BTN_W + BTN_GAP);
        int by = BTN_START_Y;
        return gx >= bx && gx < bx + BTN_W && gy >= by && gy < by + BTN_H;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) {
            int max = maxScrollRowsClient();
            if (max > 0) {
                int dir = d > 0 ? -1 : 1;
                int next = MathHelper.clamp(clientScroll + dir, 0, max);
                HideChunkNetwork.CHANNEL.sendToServer(new PacketDrawerScroll(tile.getPos(), next));
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        clientEnergy = tile.getEnergyStored();
        clientScroll = tile.getScrollRowOffset();
    }
}
