package com.palettable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Palettable.MODID)
public class Palettable {
    public static final String MODID = "palettable";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** The handheld tool that opens the palette creator screen. */
    public static final DeferredItem<PaletteItem> PALETTE = ITEMS.registerItem(
            "palette", PaletteItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PALETTE_TAB =
            CREATIVE_MODE_TABS.register("palettable", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.palettable"))
                    .icon(() -> PALETTE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(PALETTE.get()))
                    .build());

    public Palettable(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }
}
