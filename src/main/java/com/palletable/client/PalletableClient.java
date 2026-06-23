package com.palletable.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import com.palletable.Palletable;
import com.palletable.client.gui.PaletteScreen;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Palletable.MODID, dist = Dist.CLIENT)
public class PalletableClient {
    public static KeyMapping openPaletteKey;

    public PalletableClient(ModContainer container, IEventBus modEventBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(PalletableClient::registerKeyMappings);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        openPaletteKey = new KeyMapping(
                "key.palletable.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "key.categories.palletable");
        event.register(openPaletteKey);
    }

    public static void openPaletteScreen() {
        Minecraft.getInstance().setScreen(new PaletteScreen());
    }
}
