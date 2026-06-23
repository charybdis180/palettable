package com.palettable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Right-click to open the palette creator GUI (client only). */
public class PaletteItem extends Item {
    public PaletteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            // Referenced only on the client; the class is never loaded server-side.
            com.palettable.client.PalettableClient.openPaletteScreen();
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
