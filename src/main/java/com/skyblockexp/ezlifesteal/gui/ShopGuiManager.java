package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import org.bukkit.entity.Player;

public final class ShopGuiManager {

    private ShopGuiManager() { }

    public static void openShop(EzLifestealPlugin plugin, Player player) {
        new ShopMenu(plugin, player).open();
    }
}
