package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import org.bukkit.entity.Player;

public final class SmurfGuiManager {

    private SmurfGuiManager() {
    }

    public static void openManagement(EzLifestealPlugin plugin, Player player) {
        new SmurfManagementMenu(plugin, player).open();
    }

    public static void openTrusted(EzLifestealPlugin plugin, Player player) {
        new SmurfTrustedMenu(plugin, player).open();
    }

    public static void openAddTrusted(EzLifestealPlugin plugin, Player player) {
        new SmurfAddTrustedMenu(plugin, player).open();
    }

    public static void openAlertHistory(EzLifestealPlugin plugin, Player player) {
        new SmurfAlertHistoryMenu(plugin, player).open();
    }

    public static void openKillHistory(EzLifestealPlugin plugin, Player player) {
        new SmurfKillHistoryMenu(plugin, player).open();
    }
}
