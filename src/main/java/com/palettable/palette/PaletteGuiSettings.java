package com.palettable.palette;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.palettable.Config;
import com.palettable.Palettable;

import net.neoforged.fml.loading.FMLPaths;

/** Last-used creator-tab options, persisted between menu open/close. */
public final class PaletteGuiSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean fullBlocksOnly = false;
    public boolean sortByColor = false;
    public double threshold = 14.0;
    /** Empty string means no cap (show all gradient matches). */
    public String maxList = "";
    public int gradientRows = 7;
    /** @deprecated migrated to {@link #gradientRows} */
    public int gradientCellSize = 18;

    public PaletteGuiSettings() {
        threshold = Config.GRADIENT_THRESHOLD.get();
    }

    private static Path file() {
        return FMLPaths.GAMEDIR.get().resolve("palettable").resolve("gui_settings.json");
    }

    public static PaletteGuiSettings load() {
        Path path = file();
        if (!Files.exists(path)) {
            return new PaletteGuiSettings();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            PaletteGuiSettings loaded = GSON.fromJson(reader, PaletteGuiSettings.class);
            return loaded != null ? loaded : new PaletteGuiSettings();
        } catch (Exception e) {
            Palettable.LOGGER.warn("Failed to load palette GUI settings", e);
            return new PaletteGuiSettings();
        }
    }

    public static void save(PaletteGuiSettings settings) {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(settings, writer);
            }
        } catch (Exception e) {
            Palettable.LOGGER.warn("Failed to save palette GUI settings", e);
        }
    }
}
