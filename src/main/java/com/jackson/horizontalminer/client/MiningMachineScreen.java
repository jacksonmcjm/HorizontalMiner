package com.jackson.horizontalminer.client;

import com.jackson.horizontalminer.inventory.MiningMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Client presentation for the mining machine's fuel and output inventory. */
public class MiningMachineScreen extends AbstractContainerScreen<MiningMachineMenu> {

    private static final int PANEL_DARK = 0xFF2B2B2B;
    private static final int PANEL_LIGHT = 0xFFF0F0F0;
    private static final int PANEL_MID = 0xFFC6C6C6;
    private static final int SLOT_SHADOW = 0xFF373737;
    private static final int SLOT_HIGHLIGHT = 0xFFFFFFFF;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int LABEL_COLOR = 0xFF404040;
    private static final int STATUS_EMPTY = 0xFF555555;
    private static final int STATUS_FILL = 0xFF5C8E3E;

    public MiningMachineScreen(MiningMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.drawString(font, "Fuel", leftPos + 18, topPos + 42, LABEL_COLOR, false);
        graphics.drawString(font, "Output", leftPos + 124, topPos + 8, LABEL_COLOR, false);
        drawIdleStatus(graphics, leftPos + 124, topPos + 24);

        drawSlot(graphics, leftPos + 26, topPos + 18);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                drawSlot(graphics, leftPos + 62 + column * 18, topPos + 18 + row * 18);
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, leftPos + 8 + column * 18, topPos + 84 + row * 18);
            }
        }

        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, leftPos + 8 + column * 18, topPos + 142);
        }
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_DARK);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_LIGHT);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, PANEL_MID);
        graphics.fill(x + 6, y + 6, x + width - 6, y + height - 6, PANEL_LIGHT);
        graphics.fill(x + 8, y + 12, x + width - 8, y + 76, PANEL_MID);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_SHADOW);
        graphics.fill(x, y, x + 16, y + 16, SLOT_HIGHLIGHT);
        graphics.fill(x + 1, y + 1, x + 16, y + 16, SLOT_FILL);
    }

    private void drawIdleStatus(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, "Status", x, y, LABEL_COLOR, false);
        graphics.fill(x, y + 10, x + 36, y + 16, SLOT_SHADOW);
        graphics.fill(x + 1, y + 11, x + 35, y + 15, STATUS_EMPTY);
        graphics.fill(x + 1, y + 11, x + 1, y + 15, STATUS_FILL);
        graphics.drawString(font, "Idle", x + 4, y + 20, LABEL_COLOR, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
