package com.palettable.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/** Slider with editable track/handle PNGs from the active GUI skin. */
public abstract class TexturedSlider extends AbstractSliderButton {
    private final String trackTexture;
    private final String handleTexture;

    protected TexturedSlider(int x, int y, int width, int height, Component message, String trackTexture,
            String handleTexture, double value) {
        super(x, y, width, height, message, value);
        this.trackTexture = trackTexture;
        this.handleTexture = handleTexture;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int tint = this.isHoveredOrFocused() ? GuiTextures.TINT_HOVER : GuiTextures.TINT_NORMAL;
        GuiTextures.blitNineSliced(g, GuiTextures.resolve(trackTexture), getX(), getY(), width, height,
                GuiTextures.SLIDER_TEX_W, GuiTextures.SLIDER_TEX_H, GuiTextures.SLIDER_BORDER, tint);

        int handleW = GuiTextures.SLIDER_HANDLE_W;
        int handleH = GuiTextures.SLIDER_HANDLE_H;
        int handleX = getX() + (int) (this.value * (width - handleW));
        int handleY = getY() + (height - handleH) / 2;
        GuiTextures.blit(g, GuiTextures.resolve(handleTexture), handleX, handleY, handleW, handleH,
                GuiTextures.SLIDER_HANDLE_W, GuiTextures.SLIDER_HANDLE_H, tint);

        var font = Minecraft.getInstance().font;
        Component msg = getMessage();
        int textW = font.width(msg);
        int textX = getX() + (width - textW) / 2;
        int textY = getY() + (height - 8) / 2;
        g.drawString(font, msg, textX, textY, GuiTextures.getSkin().widgetTextColor(), GuiTextures.getSkin().textShadow());
    }
}
