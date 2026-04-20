package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
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

public class SmurfAddTrustedMenu extends AbstractSmurfMenu {

    private final List<UUID> candidates = new ArrayList<>();

    public SmurfAddTrustedMenu(EzLifestealPlugin plugin, Player viewer) {
        super(plugin, viewer, 54, "&aAdd Trusted Player");
        build();
    }

    private void build() {
        candidates.clear();
        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (plugin.getSmurfDetector() != null && plugin.getSmurfDetector().isExempt(online.getUniqueId())) {
                continue;
            }
            if (slot >= 45) {
                break;
            }
            candidates.add(online.getUniqueId());
            final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            final SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a" + online.getName());
                meta.setOwningPlayer(online);
                meta.setLore(List.of("§7Click to trust this player."));
                head.setItemMeta(meta);
            }
            setItem(slot++, head);
        }
        if (candidates.isEmpty()) {
            setItem(22, createItem(Material.GRAY_DYE, "&7No eligible players", List.of(
                    "&7No online players can",
                    "&7be added right now"
            )));
        }
        setItem(45, createItem(Material.ARROW, "&7Back", List.of("&7Return to trusted list")));
        setItem(49, createItem(Material.BARRIER, "&cClose", List.of("&7Click to close")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        final int slot = event.getRawSlot();
        if (slot == 45) {
            SmurfGuiManager.openTrusted(plugin, viewer);
            return;
        }
        if (slot == 49) {
            viewer.closeInventory();
            return;
        }
        if (slot < candidates.size()) {
            final UUID uuid = candidates.get(slot);
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            final String name = offlinePlayer.getName() == null ? uuid.toString() : offlinePlayer.getName();
            if (plugin.addSmurfExemption(uuid)) {
                plugin.getMessageService().sendMessage(viewer, "smurf-gui-added", Map.of(
                        "player", name
                ));
            }
            SmurfGuiManager.openTrusted(plugin, viewer);
        }
    }
}
