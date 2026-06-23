package com.palletable.client;

import com.palletable.Palletable;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/** Game-bus (runtime) client events: keybind polling and client command registration. */
@EventBusSubscriber(modid = Palletable.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private ClientGameEvents() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (PalletableClient.openPaletteKey == null) return;
        Minecraft mc = Minecraft.getInstance();
        while (PalletableClient.openPaletteKey.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                PalletableClient.openPaletteScreen();
            }
        }
    }

    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        PaletteCommand.register(event.getDispatcher());
    }
}
