package com.palletable.palette;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.palletable.Palletable;

import net.neoforged.fml.loading.FMLPaths;

/** Loads/saves the player's palettes to {@code <gamedir>/palletable/palettes.json}. */
public final class PaletteStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PaletteStorage() {
    }

    private static Path file() {
        return FMLPaths.GAMEDIR.get().resolve("palletable").resolve("palettes.json");
    }

    public static List<SavedPalette> load() {
        Path path = file();
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            SavedPalette[] arr = GSON.fromJson(reader, SavedPalette[].class);
            if (arr == null) {
                return new ArrayList<>();
            }
            return new ArrayList<>(Arrays.asList(arr));
        } catch (Exception e) {
            Palletable.LOGGER.warn("Failed to load saved palettes", e);
            return new ArrayList<>();
        }
    }

    public static void save(List<SavedPalette> palettes) {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(palettes, writer);
            }
        } catch (Exception e) {
            Palletable.LOGGER.warn("Failed to save palettes", e);
        }
    }
}
