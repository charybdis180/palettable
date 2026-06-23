package com.palettable;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side configuration for the palette tool. */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue GRADIENT_THRESHOLD = BUILDER
            .comment("How far (CIELAB deltaE) a block may sit off the A->B line and still be included in the gradient.",
                    "Larger = more blocks, looser match. ~14 is a good starting point.")
            .defineInRange("gradientThreshold", 14.0, 0.0, 100.0);

    public static final ModConfigSpec.BooleanValue REQUIRE_ITEM = BUILDER
            .comment("Only include blocks that have an obtainable item form (recommended for a building palette).")
            .define("requireItem", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
