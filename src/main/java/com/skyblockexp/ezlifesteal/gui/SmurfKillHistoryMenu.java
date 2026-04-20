package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.detector.SmurfDetector;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SmurfKillHistoryMenu extends AbstractSmurfMenu {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final List<SmurfDetector.KillRecord> records = new ArrayList<>();


    public SmurfKillHistoryMenu(EzLifestealPlugin plugin, Player viewer) {
        super(plugin, viewer, 54, "&bKill Log");
        build();
    }

    private void build() {
        records.clear();
        final SmurfDetector detector = plugin.getSmurfDetector();
        if (detector != null) {
            records.addAll(detector.getKillHistory());
        }
        if (records.isEmpty()) {
            setItem(22, createItem(Material.GRAY_DYE, "&7No tracked kills", List.of(
                    "&7No kills have been",
                    "&7recorded yet"
            )));
        }
        else {
            for (int index = 0; index < records.size() && index < 45; index++) {
                final SmurfDetector.KillRecord record = records.get(index);
                setItem(index,
                        createItem(Material.BOOK, "&b" + record.killerName() + " &7→ &b" + record.victimName(), List.of(
                        "&7When: &f" + FORMATTER.format(record.timestamp())
                )));
            }
        }
        setItem(45, createItem(Material.ARROW, "&7Back", List.of("&7Return to management")));
        setItem(49, createItem(Material.BARRIER, "&cClose", List.of("&7Click to close")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        final int slot = event.getRawSlot();
        if (slot == 45) {
            SmurfGuiManager.openManagement(plugin, viewer);
        }
        else if (slot == 49) {
            viewer.closeInventory();
        }
    }
}
