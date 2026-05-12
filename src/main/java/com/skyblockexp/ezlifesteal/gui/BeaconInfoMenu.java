package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import com.skyblockexp.ezlifesteal.model.SpawnedBeaconStatus;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Read-only 27-slot GUI shown when a player right-clicks a plugin-spawned beacon block.
 *
 * <p>The GUI displays the beacon status, location, and usage instructions in a
 * purple-themed inventory. No click actions are processed — all clicks are cancelled
 * by {@link BeaconGuiListener}.</p>
 */
public final class BeaconInfoMenu implements InventoryHolder {

    private static final int SIZE = 27;
    private static final String TITLE = "&5\u2620 &d&lRevive Beacon &5\u2620";

    private static final Material BORDER = Material.PURPLE_STAINED_GLASS_PANE;
    private static final int[] BORDER_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 18, 19, 20, 21, 22, 23, 24, 25, 26,
            9, 17};

    private final Inventory inventory;
    private final Player viewer;

    public BeaconInfoMenu(Player viewer, SpawnedBeacon beacon) {
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, SIZE,
                ChatColor.translateAlternateColorCodes('&', TITLE));
        populate(beacon);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    /** Called by {@link BeaconGuiListener} — no-op since this is a read-only GUI. */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // Layout
    // -------------------------------------------------------------------------

    private void populate(SpawnedBeacon beacon) {
        final ItemStack border = createItem(BORDER, "&8 ", null);
        for (int slot : BORDER_SLOTS) {
            inventory.setItem(slot, border);
        }

        // Slot 11 — status indicator
        inventory.setItem(11, buildStatusItem(beacon));

        // Slot 13 — center item
        inventory.setItem(13, buildCenterItem(beacon));

        // Slot 15 — location info
        inventory.setItem(15, buildLocationItem(beacon));
    }

    private ItemStack buildStatusItem(SpawnedBeacon beacon) {
        final boolean available = beacon.getStatus() == SpawnedBeaconStatus.AVAILABLE;
        final Material mat = available ? Material.EMERALD : Material.CLOCK;
        final String name = available ? "&a&lAVAILABLE" : "&e&lCOUNTDOWN";

        final List<String> lore = new ArrayList<>();
        lore.add("&7Status: &f" + beacon.getStatus().name());
        if (!available) {
            lore.add("&7The beacon is warming up.");
            lore.add("&7Wait for the countdown to finish.");
        } else {
            lore.add("&7The beacon is ready to use!");
            if (beacon.getExpiresAtMillis() > 0) {
                final long remaining = beacon.getExpiresAtMillis() - System.currentTimeMillis();
                if (remaining > 0) {
                    lore.add("&7Expires in: &c" + formatDuration(remaining));
                }
            }
        }
        return createItem(mat, name, lore);
    }

    private ItemStack buildCenterItem(SpawnedBeacon beacon) {
        final boolean available = beacon.getStatus() == SpawnedBeaconStatus.AVAILABLE;
        final Material mat = available ? Material.BEACON : Material.GRAY_STAINED_GLASS_PANE;
        final String name = available ? "&d&lRevive Beacon" : "&8Beacon (warming up)";

        final List<String> lore = new ArrayList<>();
        if (available) {
            lore.add("&7Hold a &dRevive Voucher&7 and");
            lore.add("&7right-click this beacon to revive");
            lore.add("&7an eliminated player.");
            lore.add("");
            lore.add("&5ID: &f" + beacon.shortId());
        } else {
            lore.add("&7This beacon is in its countdown phase.");
            lore.add("&7Come back once it becomes available.");
            lore.add("");
            lore.add("&5ID: &f" + beacon.shortId());
        }
        return createItem(mat, name, lore);
    }

    private ItemStack buildLocationItem(SpawnedBeacon beacon) {
        final Location loc = beacon.getLocation();
        final String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
        final String name = "&b&lLocation";

        final List<String> lore = new ArrayList<>();
        lore.add("&7World: &f" + worldName);
        lore.add("&7X: &f" + loc.getBlockX());
        lore.add("&7Y: &f" + loc.getBlockY());
        lore.add("&7Z: &f" + loc.getBlockZ());
        lore.add("");
        lore.add("&7Spawned: &f" + formatRelativeTime(beacon.getSpawnedAtMillis()));
        return createItem(Material.COMPASS, name, lore);
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    private ItemStack createItem(Material material, String displayName, List<String> lore) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            }
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore.stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatDuration(long millis) {
        final Duration d = Duration.ofMillis(millis);
        final long hours = d.toHours();
        final long minutes = d.toMinutesPart();
        final long seconds = d.toSecondsPart();
        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        } else if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }

    private String formatRelativeTime(long epochMillis) {
        final long ago = System.currentTimeMillis() - epochMillis;
        if (ago < 60_000) {
            return "just now";
        }
        return formatDuration(ago) + " ago";
    }
}
