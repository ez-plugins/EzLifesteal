package com.skyblockexp.ezlifesteal.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Cancels all interactions with {@link BeaconInfoMenu} — the beacon info GUI is read-only.
 */
public final class BeaconGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof BeaconInfoMenu menu) {
            menu.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BeaconInfoMenu) {
            event.setCancelled(true);
        }
    }
}
