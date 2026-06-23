package com.palettable.color;

import net.minecraft.world.level.block.Block;

/**
 * A block paired with its computed representative color.
 *
 * @param block    the block
 * @param rgb      packed {@code 0xFFRRGGBB} average color of the block's texture(s)
 * @param lab      the same color in CIELAB space, precomputed for fast distance queries
 * @param fullCube whether the block's default state is a full solid cube (for the "full blocks only" filter)
 */
public record BlockColorEntry(Block block, int rgb, float[] lab, boolean fullCube) {
    public static BlockColorEntry of(Block block, int rgb, boolean fullCube) {
        return new BlockColorEntry(block, rgb, ColorUtil.rgbToLab(rgb), fullCube);
    }
}
