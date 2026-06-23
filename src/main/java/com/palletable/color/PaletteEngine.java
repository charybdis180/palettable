package com.palletable.color;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.world.level.block.Block;

/**
 * Algorithms that operate on a collection of {@link BlockColorEntry}.
 *
 * <p>The core feature -- "find all blocks whose colors fall between two selected blocks" -- is
 * modeled as: take the line segment connecting color A and color B in CIELAB space, then keep every
 * block whose color projects onto that segment within {@code maxDistance} of it. Results are ordered
 * by how far along the segment they sit (0 = at A, 1 = at B), which produces a perceptual gradient.
 */
public final class PaletteEngine {
    private PaletteEngine() {}

    public record GradientHit(BlockColorEntry entry, double t, double distance) {}

    /**
     * @param all         all candidate blocks
     * @param a           gradient start
     * @param b           gradient end
     * @param maxDistance how far (in Lab deltaE) a block may sit off the A-B line and still be included
     * @return blocks lying near the A-B segment, ordered from A to B
     */
    public static List<GradientHit> gradient(List<BlockColorEntry> all, BlockColorEntry a, BlockColorEntry b, double maxDistance) {
        float[] la = a.lab();
        float[] lb = b.lab();
        double abx = lb[0] - la[0];
        double aby = lb[1] - la[1];
        double abz = lb[2] - la[2];
        double abLen2 = abx * abx + aby * aby + abz * abz;

        List<GradientHit> hits = new ArrayList<>();
        for (BlockColorEntry e : all) {
            float[] c = e.lab();
            double t;
            if (abLen2 < 1e-6) {
                // A and B are basically the same color: fall back to plain distance from A.
                t = 0.0;
            } else {
                t = ((c[0] - la[0]) * abx + (c[1] - la[1]) * aby + (c[2] - la[2]) * abz) / abLen2;
            }
            double tc = Math.max(0.0, Math.min(1.0, t));
            double px = la[0] + tc * abx;
            double py = la[1] + tc * aby;
            double pz = la[2] + tc * abz;
            double dx = c[0] - px;
            double dy = c[1] - py;
            double dz = c[2] - pz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist <= maxDistance) {
                hits.add(new GradientHit(e, tc, dist));
            }
        }
        hits.sort(Comparator.comparingDouble(GradientHit::t).thenComparingDouble(GradientHit::distance));
        return hits;
    }

    /**
     * Like {@link #gradient}, but always starts with {@code a} and ends with {@code b}. Middle blocks
     * stay ordered by position along the A→B segment.
     */
    public static List<BlockColorEntry> gradientOrdered(List<BlockColorEntry> all, BlockColorEntry a,
                                                        BlockColorEntry b, double maxDistance) {
        List<GradientHit> hits = gradient(all, a, b, maxDistance);
        List<BlockColorEntry> middle = new ArrayList<>();
        for (GradientHit hit : hits) {
            Block block = hit.entry().block();
            if (block == a.block() || block == b.block()) {
                continue;
            }
            middle.add(hit.entry());
        }
        List<BlockColorEntry> ordered = new ArrayList<>(middle.size() + 2);
        ordered.add(a);
        ordered.addAll(middle);
        ordered.add(b);
        return ordered;
    }

    /**
     * Reduce a list to at most {@code max} elements, sampled evenly across the list.
     * When {@code max >= 2} and the list has at least two entries, the first and last items are kept
     * (the gradient endpoints) and only the middle is subsampled.
     */
    public static <T> List<T> sampleEvenly(List<T> list, int max) {
        if (max <= 0 || list.size() <= max) {
            return list;
        }
        if (max == 1) {
            return List.of(list.get(0));
        }
        if (list.size() >= 2) {
            T first = list.get(0);
            T last = list.get(list.size() - 1);
            if (max == 2) {
                return List.of(first, last);
            }
            List<T> middle = list.subList(1, list.size() - 1);
            if (middle.isEmpty()) {
                return List.of(first, last);
            }
            List<T> sampledMiddle = sampleEvenlyRange(middle, max - 2);
            List<T> out = new ArrayList<>(max);
            out.add(first);
            out.addAll(sampledMiddle);
            out.add(last);
            return out;
        }
        return sampleEvenlyRange(list, max);
    }

    private static <T> List<T> sampleEvenlyRange(List<T> list, int max) {
        if (max <= 0 || list.isEmpty()) {
            return List.of();
        }
        if (list.size() <= max) {
            return new ArrayList<>(list);
        }
        if (max == 1) {
            return List.of(list.get(0));
        }
        List<T> out = new ArrayList<>(max);
        double step = (list.size() - 1) / (double) (max - 1);
        for (int i = 0; i < max; i++) {
            int idx = (int) Math.round(i * step);
            out.add(list.get(Math.min(idx, list.size() - 1)));
        }
        return out;
    }

    /** Find the N blocks closest to a target color (perceptually). Handy for "what block matches this?". */
    public static List<BlockColorEntry> nearest(List<BlockColorEntry> all, float[] targetLab, int count) {
        List<BlockColorEntry> sorted = new ArrayList<>(all);
        sorted.sort(Comparator.comparingDouble(e -> ColorUtil.deltaE(e.lab(), targetLab)));
        return sorted.subList(0, Math.min(count, sorted.size()));
    }
}
