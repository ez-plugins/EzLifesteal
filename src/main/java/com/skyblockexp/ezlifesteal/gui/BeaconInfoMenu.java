package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import com.skyblockexp.ezlifesteal.model.SpawnedBeaconStatus;
import com.skyblockexp.ezlifesteal.service.BeaconReviveService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 54-slot GUI shown when a player right-clicks a plugin-spawned beacon block.
 *
 * <p>The top section (rows 1–3) displays beacon status, location, and info.
 * Rows 4–5 list eliminated (banned) players that can be selected as revive
 * targets by clicking their entry. Row 6 contains prev/next page navigation
 * and a <em>Use Beacon</em> button that closes the GUI and immediately
 * triggers the revive interaction with whatever voucher the player holds.</p>
 *
 * <p>The read-only convenience constructor {@link #BeaconInfoMenu(Player, SpawnedBeacon)}
 * disables interactive actions and is used in tests.</p>
 */
public final class BeaconInfoMenu implements InventoryHolder {

    private static final int SIZE = 54;
    private static final String TITLE = "&5\u2620 &d&lRevive Beacon &5\u2620";

    private static final Material BORDER = Material.PURPLE_STAINED_GLASS_PANE;

    /** Fixed border slots that are always purple glass panes. */
    private static final int[] BORDER_SLOTS = {
            // Row 1 — top border
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            // Row 2 — sides only (11=status, 13=info, 15=location in the middle)
            9, 17,
            // Row 3 — full separator
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            // Rows 4–5 — sides of player list
            27, 35, 36, 44,
            // Row 6 — sides and gaps around nav buttons
            45, 47, 48, 50, 51, 53
    };

    // Row 2 content slots
    private static final int SLOT_STATUS    = 11;
    private static final int SLOT_INFO      = 13;
    private static final int SLOT_LOCATION  = 15;

    // Rows 4–5 player list (7 per row = 14 per page)
    private static final int[] PLAYER_SLOTS = {28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final int PLAYERS_PER_PAGE = PLAYER_SLOTS.length;

    // Row 6 action slots
    private static final int SLOT_PREV       = 46;
    private static final int SLOT_USE_BEACON = 49;
    private static final int SLOT_NEXT       = 52;

    private final Inventory inventory;
    private final Player viewer;
    private final SpawnedBeacon beacon;
    private final BeaconReviveService reviveService; // null in read-only mode
    private final List<String> bannedPlayers;
    private int page = 0;

    /**
     * Full constructor used at runtime.
     *
     * @param viewer        the player opening the menu
     * @param beacon        the spawned beacon this menu represents
     * @param reviveService service used to handle revive actions; {@code null} for read-only mode
     * @param bannedPlayers list of banned / eliminated player names to display
     */
    public BeaconInfoMenu(Player viewer, SpawnedBeacon beacon,
                          BeaconReviveService reviveService, List<String> bannedPlayers) {
        this.viewer = viewer;
        this.beacon = beacon;
        this.reviveService = reviveService;
        this.bannedPlayers = bannedPlayers == null ? List.of() : List.copyOf(bannedPlayers);
        this.inventory = Bukkit.createInventory(this, SIZE,
                ChatColor.translateAlternateColorCodes('&', TITLE));
        populate();
    }

    /**
     * Convenience / read-only constructor — interactive actions are disabled.
     * Retained for backward compatibility with tests.
     */
    public BeaconInfoMenu(Player viewer, SpawnedBeacon beacon) {
        this(viewer, beacon, null, List.of());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    /**
     * Called by {@link BeaconGuiListener} on every click.
     * Always cancels item movement; routes action slots to their handlers.
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        // Ignore clicks in the player's own bottom inventory rows
        if (event.getClickedInventory() == null || event.getClickedInventory() != inventory) {
            return;
        }
        final int slot = event.getSlot();
        if (slot == SLOT_USE_BEACON) {
            handleUseBeacon();
            return;
        }
        if (slot == SLOT_PREV) {
            navigatePage(-1);
            return;
        }
        if (slot == SLOT_NEXT) {
            navigatePage(1);
            return;
        }
        for (int i = 0; i < PLAYER_SLOTS.length; i++) {
            if (slot == PLAYER_SLOTS[i]) {
                handlePlayerSelect(i);
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    /**
     * Closes this GUI and immediately triggers the beacon revive interaction
     * using the item currently held in the player's main hand.
     */
    private void handleUseBeacon() {
        if (reviveService == null) {
            return;
        }
        final Block block = beacon.getLocation().getBlock();
        final ItemStack mainHand = viewer.getInventory().getItemInMainHand();
        viewer.closeInventory();
        reviveService.tryHandleBeaconInteract(viewer, mainHand, block);
    }

    /**
     * Selects the eliminated player at {@code indexInPage} as the revive target
     * and closes the GUI so the player can proceed with the voucher interaction.
     */
    private void handlePlayerSelect(int indexInPage) {
        if (reviveService == null) {
            return;
        }
        final int absoluteIndex = page * PLAYERS_PER_PAGE + indexInPage;
        if (absoluteIndex >= bannedPlayers.size()) {
            return;
        }
        final String playerName = bannedPlayers.get(absoluteIndex);
        viewer.closeInventory();
        reviveService.selectReviveTarget(viewer, playerName);
    }

    private void navigatePage(int delta) {
        final int maxPage = Math.max(0, (bannedPlayers.size() - 1) / PLAYERS_PER_PAGE);
        page = Math.max(0, Math.min(page + delta, maxPage));
        refreshPlayerList();
        refreshNavigation();
    }

    // -------------------------------------------------------------------------
    // Layout
    // -------------------------------------------------------------------------

    private void populate() {
        final ItemStack border = createItem(BORDER, "&8 ", null);
        for (int slot : BORDER_SLOTS) {
            inventory.setItem(slot, border);
        }
        inventory.setItem(SLOT_STATUS, buildStatusItem());
        inventory.setItem(SLOT_INFO, buildCenterItem());
        inventory.setItem(SLOT_LOCATION, buildLocationItem());
        inventory.setItem(SLOT_USE_BEACON, buildUseBeaconItem());
        refreshPlayerList();
        refreshNavigation();
    }

    private void refreshPlayerList() {
        for (int slot : PLAYER_SLOTS) {
            inventory.setItem(slot, null);
        }
        if (bannedPlayers.isEmpty()) {
            inventory.setItem(PLAYER_SLOTS[3],
                    createItem(Material.BARRIER, "&c&lNo Eliminated Players",
                            List.of("&7All players still have hearts.")));
            return;
        }
        final int startIndex = page * PLAYERS_PER_PAGE;
        for (int i = 0; i < PLAYER_SLOTS.length; i++) {
            final int dataIndex = startIndex + i;
            if (dataIndex >= bannedPlayers.size()) {
                break;
            }
            inventory.setItem(PLAYER_SLOTS[i], buildPlayerItem(bannedPlayers.get(dataIndex)));
        }
    }

    private void refreshNavigation() {
        final int maxPage = Math.max(0, bannedPlayers.isEmpty() ? 0
                : (bannedPlayers.size() - 1) / PLAYERS_PER_PAGE);
        final ItemStack border = createItem(BORDER, "&8 ", null);

        if (page > 0) {
            inventory.setItem(SLOT_PREV, createItem(Material.ARROW, "&7\u25c4 &ePrevious",
                    List.of("&7Page " + page + " / " + (maxPage + 1))));
        } else {
            inventory.setItem(SLOT_PREV, border);
        }
        if (page < maxPage) {
            inventory.setItem(SLOT_NEXT, createItem(Material.ARROW, "&eNext &7\u25ba",
                    List.of("&7Page " + (page + 2) + " / " + (maxPage + 1))));
        } else {
            inventory.setItem(SLOT_NEXT, border);
        }
    }

    // -------------------------------------------------------------------------
    // Item builders
    // -------------------------------------------------------------------------

    private ItemStack buildStatusItem() {
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

    private ItemStack buildCenterItem() {
        final boolean available = beacon.getStatus() == SpawnedBeaconStatus.AVAILABLE;
        final Material mat = available ? Material.BEACON : Material.GRAY_STAINED_GLASS_PANE;
        final String name = available ? "&d&lRevive Beacon" : "&8Beacon (warming up)";

        final List<String> lore = new ArrayList<>();
        if (available) {
            lore.add("&7Hold a &dRevive Voucher&7 and");
            lore.add("&7right-click the beacon to revive");
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

    private ItemStack buildLocationItem() {
        final Location loc = beacon.getLocation();
        final String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";

        final List<String> lore = new ArrayList<>();
        lore.add("&7World: &f" + worldName);
        lore.add("&7X: &f" + loc.getBlockX());
        lore.add("&7Y: &f" + loc.getBlockY());
        lore.add("&7Z: &f" + loc.getBlockZ());
        lore.add("");
        lore.add("&7Spawned: &f" + formatRelativeTime(beacon.getSpawnedAtMillis()));
        return createItem(Material.COMPASS, "&b&lLocation", lore);
    }

    private ItemStack buildUseBeaconItem() {
        final boolean available = beacon.getStatus() == SpawnedBeaconStatus.AVAILABLE;
        if (!available || reviveService == null) {
            return createItem(BORDER, "&8 ", null);
        }
        final List<String> lore = new ArrayList<>();
        lore.add("&7Uses the revive voucher in your hand");
        lore.add("&7on this beacon immediately.");
        lore.add("");
        lore.add("&eClick to use!");
        return createItem(Material.NETHER_STAR, "&d&lUse Beacon", lore);
    }

    private ItemStack buildPlayerItem(String playerName) {
        final List<String> lore = new ArrayList<>();
        lore.add("&7Click to select this player as");
        lore.add("&7your revive target.");
        lore.add("");
        lore.add("&eClick to select!");
        return createItem(Material.SKELETON_SKULL, "&c" + playerName, lore);
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
