package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SmurfManagementMenu extends AbstractSmurfMenu {

    public SmurfManagementMenu(EzLifestealPlugin plugin, Player viewer) {
        super(plugin, viewer, 27, "&cSmurf Management");
        build();
    }

    private void build() {
        setItem(11, createItem(Material.EMERALD, "&aTrusted Players", List.of(
                "&7Add or remove trusted players",
                "&7who bypass smurf detection"
        )));
        setItem(13, createItem(Material.BOOK, "&6Alert History", List.of(
                "&7Review recent smurf alerts"
        )));
        setItem(15, createItem(Material.PAPER, "&bKill Log", List.of(
                "&7Browse recent kill events",
                "&7tracked by the detector"
        )));
        setItem(22, createItem(Material.BARRIER, "&cClose", List.of("&7Click to close")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        final int slot = event.getRawSlot();
        switch (slot) {
            case 11 -> SmurfGuiManager.openTrusted(plugin, viewer);
            case 13 -> SmurfGuiManager.openAlertHistory(plugin, viewer);
            case 15 -> SmurfGuiManager.openKillHistory(plugin, viewer);
            case 22 -> viewer.closeInventory();
            default -> {
            }
        }
    }
}
