package com.jackson.horizontalminer.client;

import com.jackson.horizontalminer.inventory.MiningMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

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
    private static final int BURN_FILL = 0xFFE08A2E;
    private static final int BURN_X = 124;
    private static final int BURN_Y = 31;
    private static final int BURN_WIDTH = 7;
    private static final int BURN_HEIGHT = 18;
    private static final int PROGRESS_X = 137;
    private static final int PROGRESS_Y = 31;
    private static final int PROGRESS_WIDTH = 29;
    private static final int PROGRESS_HEIGHT = 6;
    private static final int STATUS_X = 124;
    private static final int STATUS_Y = 8;
    private static final int DEPTH_X = 124;
    private static final int DEPTH_Y = 53;

    public MiningMachineScreen(MiningMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.drawString(font, Component.translatable("gui.horizontalminer.fuel"),
                leftPos + 18, topPos + 42, LABEL_COLOR, false);
        drawStatus(graphics);
        drawBurnIndicator(graphics);
        drawMiningProgress(graphics);
        graphics.drawString(font, Component.literal(Integer.toString(menu.getTunnelDepth())),
                leftPos + DEPTH_X, topPos + DEPTH_Y, LABEL_COLOR, false);

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

    private void drawStatus(GuiGraphics graphics) {
        int x = leftPos + STATUS_X;
        int y = topPos + STATUS_Y;
        switch (menu.getMachineStatus()) {
            case MINING -> graphics.drawString(font, Component.translatable("gui.horizontalminer.status.mining"),
                    x, y, STATUS_FILL, false);
            case OUT_OF_FUEL -> {
                graphics.drawString(font, Component.translatable("gui.horizontalminer.status.out_of"),
                        x, y, LABEL_COLOR, false);
                graphics.drawString(font, Component.translatable("gui.horizontalminer.status.fuel"),
                        x, y + 9, LABEL_COLOR, false);
            }
            case OUTPUT_FULL -> {
                graphics.drawString(font, Component.translatable("gui.horizontalminer.status.output"),
                        x, y, LABEL_COLOR, false);
                graphics.drawString(font, Component.translatable("gui.horizontalminer.status.full"),
                        x, y + 9, LABEL_COLOR, false);
            }
            case IDLE -> graphics.drawString(font, Component.translatable("gui.horizontalminer.status.idle"),
                    x, y, LABEL_COLOR, false);
        }
    }

    private void drawBurnIndicator(GuiGraphics graphics) {
        int x = leftPos + BURN_X;
        int y = topPos + BURN_Y;
        int filled = menu.getScaledBurnProgress(BURN_HEIGHT - 2);
        graphics.fill(x, y, x + BURN_WIDTH, y + BURN_HEIGHT, SLOT_SHADOW);
        graphics.fill(x + 1, y + 1, x + BURN_WIDTH - 1, y + BURN_HEIGHT - 1, STATUS_EMPTY);
        graphics.fill(x + 1, y + BURN_HEIGHT - 1 - filled, x + BURN_WIDTH - 1, y + BURN_HEIGHT - 1,
                BURN_FILL);
    }

    private void drawMiningProgress(GuiGraphics graphics) {
        int x = leftPos + PROGRESS_X;
        int y = topPos + PROGRESS_Y;
        int filled = menu.getScaledMiningProgress(PROGRESS_WIDTH - 2);
        graphics.fill(x, y, x + PROGRESS_WIDTH, y + PROGRESS_HEIGHT, SLOT_SHADOW);
        graphics.fill(x + 1, y + 1, x + PROGRESS_WIDTH - 1, y + PROGRESS_HEIGHT - 1, STATUS_EMPTY);
        graphics.fill(x + 1, y + 1, x + 1 + filled, y + PROGRESS_HEIGHT - 1, STATUS_FILL);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderMachineTooltips(graphics, mouseX, mouseY);
    }

    private void renderMachineTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isInside(mouseX, mouseY, BURN_X, BURN_Y, BURN_WIDTH, BURN_HEIGHT)) {
            graphics.renderTooltip(font, List.of(
                    Component.translatable("gui.horizontalminer.tooltip.burn_time").getVisualOrderText(),
                    Component.translatable("gui.horizontalminer.tooltip.ticks_remaining", menu.getRemainingBurnTime())
                            .getVisualOrderText()
            ), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, PROGRESS_X, PROGRESS_Y, PROGRESS_WIDTH, PROGRESS_HEIGHT)) {
            int maximumProgress = menu.getMaximumMiningProgress();
            int percent = maximumProgress == 0 ? 0 : menu.getMiningProgress() * 100 / maximumProgress;
            graphics.renderTooltip(font, List.of(
                    Component.translatable("gui.horizontalminer.tooltip.mining_progress").getVisualOrderText(),
                    Component.translatable("gui.horizontalminer.tooltip.percent", percent).getVisualOrderText()
            ), mouseX, mouseY);
        } else if (isInside(mouseX, mouseY, DEPTH_X, DEPTH_Y, 44, 9)) {
            graphics.renderTooltip(font, Component.translatable("gui.horizontalminer.tooltip.depth"), mouseX, mouseY);
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }
}
