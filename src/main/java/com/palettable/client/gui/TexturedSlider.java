package com.palettable.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Slider with editable track/handle PNGs; hover tints the textures. */
public abstract class TexturedSlider extends AbstractSliderButton {
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    protected TexturedSlider(int x, int y, int width, int height, Component message, double value) {
        super(x, y, width, height, message, value);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        int tint = this.isHoveredOrFocused() ? GuiTextures.TINT_HOVER : GuiTextures.TINT_NORMAL;
        GuiTextures.blitNineSliced(g, GuiTextures.SLIDER_TRACK, getX(), getY(), width, height,
                GuiTextures.SLIDER_TEX_W, GuiTextures.SLIDER_TEX_H, GuiTextures.SLIDER_BORDER, tint);

        int handleW = GuiTextures.SLIDER_HANDLE_W;
        int handleH = GuiTextures.SLIDER_HANDLE_H;
        int handleX = getX() + (int) (this.value * (width - handleW));
        int handleY = getY() + (height - handleH) / 2;
        GuiTextures.blit(g, GuiTextures.SLIDER_HANDLE, handleX, handleY, handleW, handleH,
                GuiTextures.SLIDER_HANDLE_W, GuiTextures.SLIDER_HANDLE_H, tint);

        String label = getMessage().getString();
        int textX = getX() + width / 2 - Minecraft.getInstance().font.width(label) / 2;
        int textY = getY() + (height - 8) / 2;
        g.drawString(Minecraft.getInstance().font, label, textX, textY, TEXT_COLOR, true);
    }

    protected static double clampValue(double value) {
        return Mth.clamp(value, 0.0, 1.0);
    }
}
