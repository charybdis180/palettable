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
}
