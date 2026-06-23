package com.palettable.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.blaze3d.platform.NativeImage;

import com.palettable.Config;
import com.palettable.Palettable;
import com.palettable.color.BlockColorEntry;
import com.palettable.color.ColorUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Computes a single representative "average" color for every block in the registry (vanilla + modded)
 * by reading the block's baked model, sampling its texture pixels, and applying biome/foliage tint
 * where the block uses it.
 *
 * <p>Results are cached. Building is lazy (first query) and must happen client-side after resources
 * have loaded, so we only ever call it from in-game commands / the GUI.
 */
public final class BlockColorAnalyzer {
    private static final Map<Block, BlockColorEntry> CACHE = new HashMap<>();
    /** sprite texture id -> average rgb; Integer.MIN_VALUE marks "no usable color". */
    private static final Map<ResourceLocation, Integer> SPRITE_CACHE = new HashMap<>();
    private static volatile boolean built = false;

    private BlockColorAnalyzer() {}

    public static synchronized List<BlockColorEntry> getAll() {
        ensureBuilt();
        return new ArrayList<>(CACHE.values());
    }

    public static synchronized BlockColorEntry get(Block block) {
        ensureBuilt();
        return CACHE.get(block);
    }

    public static synchronized void invalidate() {
        built = false;
        CACHE.clear();
        SPRITE_CACHE.clear();
    }

    private static void ensureBuilt() {
        if (!built) {
            build();
        }
    }

    private static void build() {
        long start = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        BlockColors blockColors = mc.getBlockColors();
        RandomSource random = RandomSource.create(42L);

        boolean requireItem = Config.REQUIRE_ITEM.get();
        int analyzed = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            try {
                BlockState state = block.defaultBlockState();
                if (state.isAir()) continue;
                if (state.getRenderShape() != RenderShape.MODEL) continue;
                if (requireItem && block.asItem() == Items.AIR) continue;

                Integer rgb = computeBlockColor(dispatcher, blockColors, state, random);
                if (rgb == null) continue;

                boolean fullCube;
                try {
                    fullCube = state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                } catch (Exception e) {
                    fullCube = false;
                }

                CACHE.put(block, BlockColorEntry.of(block, rgb, fullCube));
                analyzed++;
            } catch (Exception e) {
                Palettable.LOGGER.debug("Failed to analyze block {}", block, e);
            }
        }
        built = true;
        Palettable.LOGGER.info("Palettable analyzed {} block colors in {} ms",
                analyzed, System.currentTimeMillis() - start);
    }

    private static Integer computeBlockColor(BlockRenderDispatcher dispatcher, BlockColors blockColors,
                                             BlockState state, RandomSource random) {
        BakedModel model = dispatcher.getBlockModel(state);

        List<BakedQuad> quads = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            quads.addAll(model.getQuads(state, dir, random));
        }
        quads.addAll(model.getQuads(state, null, random));

        double sr = 0, sg = 0, sb = 0;
        int n = 0;
        for (BakedQuad quad : quads) {
            Integer spriteRgb = spriteAverage(quad.getSprite());
            if (spriteRgb == null) continue;

            int color = spriteRgb;
            int tintIndex = quad.getTintIndex();
            if (tintIndex != -1) {
                int tint = blockColors.getColor(state, null, null, tintIndex);
                if (tint != -1) {
                    color = ColorUtil.multiply(color, tint);
                }
            }
            sr += ColorUtil.red(color);
            sg += ColorUtil.green(color);
            sb += ColorUtil.blue(color);
            n++;
        }

        if (n == 0) {
            // No quads (e.g. some cross/tinted models): fall back to the particle icon.
            Integer particle = spriteAverage(model.getParticleIcon(ModelData.EMPTY));
            return particle;
        }
        return ColorUtil.rgb((int) Math.round(sr / n), (int) Math.round(sg / n), (int) Math.round(sb / n));
    }

    private static Integer spriteAverage(TextureAtlasSprite sprite) {
        if (sprite == null) return null;
        ResourceLocation name = sprite.contents().name();
        Integer cached = SPRITE_CACHE.get(name);
        if (cached != null) {
            return cached == Integer.MIN_VALUE ? null : cached;
        }
        Integer result = readTextureAverage(name);
        SPRITE_CACHE.put(name, result == null ? Integer.MIN_VALUE : result);
        return result;
    }

    /**
     * Loads the sprite's source PNG straight from the resource manager and averages its opaque pixels,
     * weighting by alpha. This works for modded textures too since they live in the resource manager.
     */
    private static Integer readTextureAverage(ResourceLocation spriteName) {
        if (spriteName.getPath().equals("missingno")) return null;
        ResourceLocation png = ResourceLocation.fromNamespaceAndPath(
                spriteName.getNamespace(), "textures/" + spriteName.getPath() + ".png");

        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        Optional<Resource> resource = rm.getResource(png);
        if (resource.isEmpty()) return null;

        try (InputStream is = resource.get().open(); NativeImage img = NativeImage.read(is)) {
            long sr = 0, sg = 0, sb = 0, weight = 0;
            int w = img.getWidth();
            int h = img.getHeight();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    // NativeImage packs pixels as ABGR (little-endian RGBA bytes).
                    int p = img.getPixelRGBA(x, y);
                    int a = (p >>> 24) & 0xFF;
                    if (a < 16) continue;
                    int r = p & 0xFF;
                    int g = (p >>> 8) & 0xFF;
                    int b = (p >>> 16) & 0xFF;
                    sr += (long) r * a;
                    sg += (long) g * a;
                    sb += (long) b * a;
                    weight += a;
                }
            }
            if (weight == 0) return null;
            return ColorUtil.rgb((int) (sr / weight), (int) (sg / weight), (int) (sb / weight));
        } catch (IOException e) {
            return null;
        }
    }
}
