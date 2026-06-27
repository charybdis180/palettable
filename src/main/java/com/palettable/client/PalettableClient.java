package com.palettable.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import com.palettable.Palettable;
import com.palettable.client.gui.PaletteScreen;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.KeyModifier;

@Mod(value = Palettable.MODID, dist = Dist.CLIENT)
public class PalettableClient {
    public static KeyMapping openPaletteKey;

    public PalettableClient(ModContainer container, IEventBus modEventBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(PalettableClient::registerKeyMappings);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        openPaletteKey = new KeyMapping(
                "key.palettable.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "key.categories.palettable");
        event.register(openPaletteKey);
    }

    public static void openPaletteScreen() {
        Minecraft.getInstance().setScreen(new PaletteScreen());
    }

    /** Apply a new open-key binding and refresh Minecraft's key lookup (matches vanilla Controls behavior). */
    public static void setOpenPaletteKey(InputConstants.Key key) {
        if (openPaletteKey == null || key == null || key == InputConstants.UNKNOWN) {
            return;
        }
        openPaletteKey.setKeyModifierAndCode(KeyModifier.NONE, key);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }
}
