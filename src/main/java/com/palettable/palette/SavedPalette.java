package com.palettable.palette;

import java.util.ArrayList;
import java.util.List;

/**
 * A user-saved palette. Stored as plain fields (with a no-arg constructor) so Gson serialization is
 * robust across Gson versions. Block references are kept as registry-id strings.
 */
public class SavedPalette {
    public String name = "";
    public String from = "";
    public String to = "";
    public double threshold = 14.0;
    public int max = 64;
    public boolean fullBlocksOnly = false;
    public List<String> blocks = new ArrayList<>();

    public SavedPalette() {
    }

    public SavedPalette(String name, String from, String to, double threshold, int max,
                        boolean fullBlocksOnly, List<String> blocks) {
        this.name = name;
        this.from = from;
        this.to = to;
        this.threshold = threshold;
        this.max = max;
        this.fullBlocksOnly = fullBlocksOnly;
        this.blocks = new ArrayList<>(blocks);
    }
}
