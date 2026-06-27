package com.palettable.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Button rendered from a skinned GUI PNG; hover uses a color tint instead of a second texture. */
public class TexturedButton extends Button {
    private final String textureBase;
    private final boolean nineSlice;
    private final int textureWidth;
    private final int textureHeight;
    private final int border;

    private TexturedButton(int x, int y, int width, int height, Component message, OnPress onPress,
            String textureBase, boolean nineSlice, int textureWidth, int textureHeight, int border) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.textureBase = textureBase;
        this.nineSlice = nineSlice;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.border = border;
    }

    public static TexturedButton wide(int x, int y, int width, int height, Component message, OnPress onPress,
            String textureBase) {
        return new TexturedButton(x, y, width, height, message, onPress, textureBase,
                true, GuiTextures.BUTTON_TEX_W, GuiTextures.BUTTON_TEX_H, GuiTextures.BUTTON_BORDER);
    }

    public static TexturedButton square(int x, int y, int size, Component message, OnPress onPress,
            String textureBase) {
        return new TexturedButton(x, y, size, size, message, onPress, textureBase,
                false, size, size, 0);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }
        int tint = this.isHoveredOrFocused() ? GuiTextures.TINT_HOVER : GuiTextures.TINT_NORMAL;
        var texture = GuiTextures.resolve(textureBase);
        if (nineSlice) {
            GuiTextures.blitNineSliced(g, texture, getX(), getY(), width, height,
                    textureWidth, textureHeight, border, tint);
        } else {
            GuiTextures.blit(g, texture, getX(), getY(), width, height,
                    textureWidth, textureHeight, tint);
        }
        int textX = getX() + width / 2;
        int textY = getY() + (height - 8) / 2;
        g.drawString(Minecraft.getInstance().font, getMessage(),
                textX - Minecraft.getInstance().font.width(getMessage()) / 2, textY,
                GuiTextures.getSkin().widgetTextColor(), GuiTextures.getSkin().textShadow());
    }
}
