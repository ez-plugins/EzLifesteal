package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.detector.SmurfDetector;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class SmurfTrustedMenu extends AbstractSmurfMenu {

    private final List<UUID> entries = new ArrayList<>();

    public SmurfTrustedMenu(EzLifestealPlugin plugin, Player viewer) {
        super(plugin, viewer, 54, "&aTrusted Players");
        build();
    }

    private void build() {
        entries.clear();
        final SmurfDetector detector = plugin.getSmurfDetector();
        if (detector != null) {
            int slot = 0;
            for (UUID uuid : detector.getExemptPlayers()) {
                if (slot >= 45) {
                    break;
                }
                entries.add(uuid);
                final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                final SkullMeta meta = (SkullMeta) head.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§b" + getDisplayName(offlinePlayer, uuid));
                    meta.setOwningPlayer(offlinePlayer);
                    meta.setLore(List.of("§7Click to remove from the trusted list."));
                    head.setItemMeta(meta);
                }
                setItem(slot++, head);
            }
            if (entries.isEmpty()) {
                setItem(13, createItem(Material.GRAY_DYE, "&7No trusted players", List.of(
                        "&7Use the add button below"
                )));
            }
        }
        setItem(45, createItem(Material.ARROW, "&7Back", List.of("&7Return to management")));
        setItem(49, createItem(Material.BARRIER, "&cClose", List.of("&7Click to close")));
        setItem(53, createItem(Material.LIME_DYE, "&aAdd Player", List.of(
                "&7Select an online player",
                "&7to mark them as trusted"
        )));
    }

    private String getDisplayName(OfflinePlayer offlinePlayer, UUID uuid) {
        final String name = offlinePlayer.getName();
        return (name == null || name.isBlank()) ? uuid.toString() : name;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        final int slot = event.getRawSlot();
        if (slot == 45) {
            SmurfGuiManager.openManagement(plugin, viewer);
            return;
        }
        if (slot == 49) {
            viewer.closeInventory();
            return;
        }
        if (slot == 53) {
            SmurfGuiManager.openAddTrusted(plugin, viewer);
            return;
        }
        if (slot < entries.size()) {
            final UUID uuid = entries.get(slot);
            if (plugin.removeSmurfExemption(uuid)) {
                final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                plugin.getMessageService().sendMessage(viewer, "smurf-gui-removed", Map.of(
                        "player", getDisplayName(offlinePlayer, uuid)
                ));
            }
            SmurfGuiManager.openTrusted(plugin, viewer);
        }
    }
}
