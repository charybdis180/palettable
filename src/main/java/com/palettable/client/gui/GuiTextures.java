package com.palettable.client.gui;

import com.palettable.Palettable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Editable GUI PNGs under {@code assets/palettable/textures/gui/}. */
public final class GuiTextures {
    public static final int PANEL_TEX_SIZE = 24;
    public static final int PANEL_BORDER = 6;

    public static final int INSET_TEX_SIZE = 18;
    public static final int INSET_BORDER = 4;

    public static final int TAB_TEX_W = 28;
    public static final int TAB_TEX_H = 28;
    /** Tab icon PNG size ({@code tab_icon_gear.png}). */
    public static final int TAB_ICON_SIZE = 16;

    public static final int BUTTON_TEX_W = 20;
    public static final int BUTTON_TEX_H = 16;
    public static final int BUTTON_BORDER = 4;

    public static final int SLIDER_TEX_W = 20;
    public static final int SLIDER_TEX_H = 16;
    public static final int SLIDER_BORDER = 4;
    public static final int SLIDER_HANDLE_W = 8;
    public static final int SLIDER_HANDLE_H = 16;

    /** Vanilla hotbar slot size and screen offsets ({@code width/2 - 91}, {@code height - 22}). */
    public static final int HOTBAR_SLOT_SIZE = 20;
    public static final int HOTBAR_LEFT_OFFSET = 91;
    public static final int HOTBAR_BOTTOM_OFFSET = 22;
    public static final int HOTBAR_ITEM_INSET = 3;

    /** Confirm/delete icon buttons — 13x13 each. */
    public static final int ACTION_BTN_SIZE = 13;

    /** Full-strength texture; channels multiply the PNG pixel colors. */
    public static final int TINT_NORMAL = 0xFFFFFFFF;
    /** Warm highlight applied on hover (no second PNG needed). */
    public static final int TINT_HOVER = 0xFFFFF0CC;

    private static GuiSkin skin = GuiSkin.DEFAULT;

    private GuiTextures() {}

    public static GuiSkin getSkin() {
        return skin;
    }

    public static void setSkin(GuiSkin newSkin) {
        skin = newSkin != null ? newSkin : GuiSkin.DEFAULT;
    }

    /** Base PNG name without suffix or extension, e.g. {@code "panel"} → {@code panel_dark.png}. */
    public static ResourceLocation resolve(String baseName) {
        return ResourceLocation.fromNamespaceAndPath(Palettable.MODID,
                "textures/gui/" + baseName + skin.fileSuffix() + ".png");
    }

    public static void blitPanel(GuiGraphics g, int x, int y, int width, int height) {
        blitNineSliced(g, resolve("panel"), x, y, width, height, PANEL_TEX_SIZE, PANEL_TEX_SIZE, PANEL_BORDER);
    }

    public static void blitTextField(GuiGraphics g, int x, int y, int width, int height) {
        blitNineSliced(g, resolve("text_field"), x, y, width, height, INSET_TEX_SIZE, INSET_TEX_SIZE, INSET_BORDER);
    }

    public static void blitSlot(GuiGraphics g, int x, int y, int width, int height) {
        blitNineSliced(g, resolve("slot"), x, y, width, height, INSET_TEX_SIZE, INSET_TEX_SIZE, INSET_BORDER);
    }

    public static void blitHotbarSlot(GuiGraphics g, int x, int y) {
        blit(g, resolve("hotbar_slot"), x, y, HOTBAR_SLOT_SIZE, HOTBAR_SLOT_SIZE,
                HOTBAR_SLOT_SIZE, HOTBAR_SLOT_SIZE, TINT_NORMAL);
    }

    public static void blitActionButton(GuiGraphics g, String baseName, int x, int y, int tint) {
        blit(g, resolve(baseName), x, y, ACTION_BTN_SIZE, ACTION_BTN_SIZE,
                ACTION_BTN_SIZE, ACTION_BTN_SIZE, tint);
    }

    public static void blitTab(GuiGraphics g, boolean active, int x, int y) {
        blit(g, resolve(active ? "tab_active" : "tab_inactive"), x, y, TAB_TEX_W, TAB_TEX_H,
                TAB_TEX_W, TAB_TEX_H, TINT_NORMAL);
    }

    public static void blitTabIcon(GuiGraphics g, int x, int y, int tint) {
        blit(g, resolve("tab_icon_gear"), x, y, TAB_ICON_SIZE, TAB_ICON_SIZE,
                TAB_ICON_SIZE, TAB_ICON_SIZE, tint);
    }

    public static void blit(GuiGraphics g, ResourceLocation texture, int x, int y, int width, int height,
            int textureWidth, int textureHeight, int tint) {
        applyTint(g, tint);
        g.blit(texture, x, y, width, height, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
        resetTint(g);
    }

    public static void blitNineSliced(GuiGraphics g, ResourceLocation texture, int x, int y, int width, int height,
            int textureWidth, int textureHeight, int border) {
        blitNineSliced(g, texture, x, y, width, height, textureWidth, textureHeight, border, TINT_NORMAL);
    }

    public static void blitNineSliced(GuiGraphics g, ResourceLocation texture, int x, int y, int width, int height,
            int textureWidth, int textureHeight, int border, int tint) {
        blitNineSliced(g, texture, x, y, width, height, textureWidth, textureHeight,
                border, border, border, border, tint);
    }

    public static void blitNineSliced(GuiGraphics g, ResourceLocation texture, int x, int y, int width, int height,
            int textureWidth, int textureHeight, int borderLeft, int borderRight, int borderTop, int borderBottom) {
        blitNineSliced(g, texture, x, y, width, height, textureWidth, textureHeight,
                borderLeft, borderRight, borderTop, borderBottom, TINT_NORMAL);
    }

    /** Nine-slice stretch using a single PNG with fixed corner/edge sizes. */
    public static void blitNineSliced(GuiGraphics g, ResourceLocation texture, int x, int y, int width, int height,
            int textureWidth, int textureHeight, int borderLeft, int borderRight, int borderTop, int borderBottom,
            int tint) {
        applyTint(g, tint);
        int left = Math.min(borderLeft, width / 2);
        int right = Math.min(borderRight, width / 2);
        int top = Math.min(borderTop, height / 2);
        int bottom = Math.min(borderBottom, height / 2);
        int centerW = Math.max(0, width - left - right);
        int centerH = Math.max(0, height - top - bottom);

        int texCenterW = textureWidth - borderLeft - borderRight;
        int texCenterH = textureHeight - borderTop - borderBottom;

        blitRegion(g, texture, x, y, left, top, 0, 0, borderLeft, borderTop, textureWidth, textureHeight);
        blitRegion(g, texture, x + width - right, y, right, top,
                textureWidth - borderRight, 0, borderRight, borderTop, textureWidth, textureHeight);
        blitRegion(g, texture, x, y + height - bottom, left, bottom,
                0, textureHeight - borderBottom, borderLeft, borderBottom, textureWidth, textureHeight);
        blitRegion(g, texture, x + width - right, y + height - bottom, right, bottom,
                textureWidth - borderRight, textureHeight - borderBottom, borderRight, borderBottom, textureWidth, textureHeight);

        blitRegion(g, texture, x + left, y, centerW, top,
                borderLeft, 0, texCenterW, borderTop, textureWidth, textureHeight);
        blitRegion(g, texture, x + left, y + height - bottom, centerW, bottom,
                borderLeft, textureHeight - borderBottom, texCenterW, borderBottom, textureWidth, textureHeight);
        blitRegion(g, texture, x, y + top, left, centerH,
                0, borderTop, borderLeft, texCenterH, textureWidth, textureHeight);
        blitRegion(g, texture, x + width - right, y + top, right, centerH,
                textureWidth - borderRight, borderTop, borderRight, texCenterH, textureWidth, textureHeight);

        blitRegion(g, texture, x + left, y + top, centerW, centerH,
                borderLeft, borderTop, texCenterW, texCenterH, textureWidth, textureHeight);
        resetTint(g);
    }

    private static void applyTint(GuiGraphics g, int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float green = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        g.setColor(r, green, b, a);
    }

    private static void resetTint(GuiGraphics g) {
        g.setColor(1f, 1f, 1f, 1f);
    }

    private static void blitRegion(GuiGraphics g, ResourceLocation texture, int x, int y, int width, int height,
            int u, int v, int regionW, int regionH, int textureWidth, int textureHeight) {
        if (width <= 0 || height <= 0 || regionW <= 0 || regionH <= 0) {
            return;
        }
        g.blit(texture, x, y, width, height, u, v, regionW, regionH, textureWidth, textureHeight);
    }
}
