package com.palettable.client;

import com.palettable.Palettable;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/** Game-bus (runtime) client events: keybind polling and client command registration. */
@EventBusSubscriber(modid = Palettable.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private ClientGameEvents() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (PalettableClient.openPaletteKey == null) return;
        Minecraft mc = Minecraft.getInstance();
        while (PalettableClient.openPaletteKey.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                PalettableClient.openPaletteScreen();
            }
        }
    }

    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        PaletteCommand.register(event.getDispatcher());
    }
}
