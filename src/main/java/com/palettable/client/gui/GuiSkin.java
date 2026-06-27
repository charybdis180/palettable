package com.palettable.client.gui;

import net.minecraft.util.Mth;

/** Editable GUI texture sets under {@code assets/palettable/textures/gui/}. */
public enum GuiSkin {
    DEFAULT("", "Default", 0xFFFFFFFF, 0xFFDDDDDD, 0xFFFFFFFF, 0xFF9A9A9A, 0x50FFFFFF),
    /** Cool blue-slate panel; text tints match the dark transform palette. */
    DARK("_dark", "Dark Mode", 0xFF252525, 0xFF4A4A4A, 0xFF252525, 0xFF4A5566, 0x50446688),
    WARM("_warm", "Sunny", 0xFFF5D4C4, 0xFFCC9A84, 0xFFFFEDE0, 0xFF8B7355, 0x50FFCC88),
    /** Rust red-brown base (#73362A); parchment tones for explorer-style contrast. */
    RUST("_rust", "Explorer", 0xFFF2E6C8, 0xFFCDB896, 0xFFFFFAE8, 0xFF5A2820, 0x60FFAA88);

    private final String fileSuffix;
    private final String displayName;
    private final int textColor;
    private final int textMutedColor;
    private final int titleColor;
    private final int dividerColor;
    private final int selectionOverlay;

    GuiSkin(String fileSuffix, String displayName, int textColor, int textMutedColor, int titleColor,
            int dividerColor, int selectionOverlay) {
        this.fileSuffix = fileSuffix;
        this.displayName = displayName;
        this.textColor = textColor;
        this.textMutedColor = textMutedColor;
        this.titleColor = titleColor;
        this.dividerColor = dividerColor;
        this.selectionOverlay = selectionOverlay;
    }

    public String fileSuffix() {
        return fileSuffix;
    }

    public String displayName() {
        return displayName;
    }

    public int textColor() {
        return textColor;
    }

    public int textMutedColor() {
        return textMutedColor;
    }

    public int titleColor() {
        return titleColor;
    }

    public int dividerColor() {
        return dividerColor;
    }

    /** Semi-transparent fill drawn over a selected saved-palette row. */
    public int selectionOverlay() {
        return selectionOverlay;
    }

    /**
     * Text on buttons/sliders. Default/Warm keep white on gray buttons;
     * Dark/Explorer use the same color as panel labels.
     */
    public int widgetTextColor() {
        return switch (this) {
            case DEFAULT, WARM -> 0xFFFFFFFF;
            case DARK, RUST -> textColor;
        };
    }

    /** Dark Mode uses flat text without a drop shadow. */
    public boolean textShadow() {
        return this != DARK;
    }

    public GuiSkin next() {
        GuiSkin[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static GuiSkin byIndex(int index) {
        GuiSkin[] values = values();
        return values[Mth.clamp(index, 0, values.length - 1)];
    }
}
