package com.palletable.client;

import java.util.Comparator;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;

import com.palletable.Config;
import com.palletable.Palletable;
import com.palletable.color.BlockColorEntry;
import com.palletable.color.PaletteEngine;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * Client-side debug commands used to verify the color engine without the GUI:
 * <pre>
 *   /palette dump
 *   /palette gradient &lt;from&gt; &lt;to&gt; [threshold]
 *   /palette reload
 * </pre>
 */
public final class PaletteCommand {
    private PaletteCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("palette")
                .then(Commands.literal("dump")
                        .executes(ctx -> dump(ctx.getSource())))
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            BlockColorAnalyzer.invalidate();
                            ctx.getSource().sendSuccess(() -> Component.literal("Palette color cache cleared."), false);
                            return 1;
                        }))
                .then(Commands.literal("gradient")
                        .then(Commands.argument("from", ResourceLocationArgument.id())
                                .then(Commands.argument("to", ResourceLocationArgument.id())
                                        .executes(ctx -> gradient(ctx, Config.GRADIENT_THRESHOLD.get()))
                                        .then(Commands.argument("threshold", DoubleArgumentType.doubleArg(0))
                                                .executes(ctx -> gradient(ctx, DoubleArgumentType.getDouble(ctx, "threshold"))))))));
    }

    private static int dump(CommandSourceStack source) {
        List<BlockColorEntry> all = BlockColorAnalyzer.getAll();
        source.sendSuccess(() -> Component.literal("Computed colors for " + all.size() + " blocks (full list in the log)."), false);
        Palletable.LOGGER.info("=== Palletable block colors ({}) ===", all.size());
        all.stream()
                .sorted(Comparator.comparing(e -> BuiltInRegistries.BLOCK.getKey(e.block()).toString()))
                .forEach(e -> Palletable.LOGGER.info("  {} -> #{}",
                        BuiltInRegistries.BLOCK.getKey(e.block()), hex(e.rgb())));
        return 1;
    }

    private static int gradient(CommandContext<CommandSourceStack> ctx, double threshold) {
        CommandSourceStack source = ctx.getSource();
        ResourceLocation fromId = ResourceLocationArgument.getId(ctx, "from");
        ResourceLocation toId = ResourceLocationArgument.getId(ctx, "to");

        Block fromBlock = BuiltInRegistries.BLOCK.getOptional(fromId).orElse(null);
        Block toBlock = BuiltInRegistries.BLOCK.getOptional(toId).orElse(null);
        if (fromBlock == null) {
            source.sendFailure(Component.literal("Unknown block: " + fromId));
            return 0;
        }
        if (toBlock == null) {
            source.sendFailure(Component.literal("Unknown block: " + toId));
            return 0;
        }

        BlockColorEntry a = BlockColorAnalyzer.get(fromBlock);
        BlockColorEntry b = BlockColorAnalyzer.get(toBlock);
        if (a == null || b == null) {
            source.sendFailure(Component.literal("No color data for one of those blocks (no usable texture)."));
            return 0;
        }

        List<BlockColorEntry> all = BlockColorAnalyzer.getAll();
        List<BlockColorEntry> hits = PaletteEngine.gradientOrdered(all, a, b, threshold);

        source.sendSuccess(() -> Component.literal(String.format(
                "Gradient %s -> %s: %d blocks (threshold %.1f)",
                fromId.getPath(), toId.getPath(), hits.size(), threshold)), false);

        int shown = 0;
        for (BlockColorEntry entry : hits) {
            if (shown++ >= 40) break;
            int rgb = entry.rgb() & 0xFFFFFF;
            String line = String.format("#%06X  %s", rgb, BuiltInRegistries.BLOCK.getKey(entry.block()));
            source.sendSuccess(() -> Component.literal(line).withStyle(s -> s.withColor(rgb)), false);
        }
        if (hits.size() > shown) {
            int remaining = hits.size() - shown;
            source.sendSuccess(() -> Component.literal("... and " + remaining + " more (see log)."), false);
        }

        Palletable.LOGGER.info("Palette gradient {} -> {} ({} blocks, threshold {}):", fromId, toId, hits.size(), threshold);
        for (int i = 0; i < hits.size(); i++) {
            BlockColorEntry entry = hits.get(i);
            Palletable.LOGGER.info("  [{}] #{} {}", i, hex(entry.rgb()),
                    BuiltInRegistries.BLOCK.getKey(entry.block()));
        }
        return 1;
    }

    private static String hex(int rgb) {
        return String.format("%06X", rgb & 0xFFFFFF);
    }
}
