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

public class SmurfAlertHistoryMenu extends AbstractSmurfMenu {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final List<SmurfDetector.SmurfAlert> alerts = new ArrayList<>();


    public SmurfAlertHistoryMenu(EzLifestealPlugin plugin, Player viewer) {
        super(plugin, viewer, 54, "&6Smurf Alerts");
        build();
    }

    private void build() {
        alerts.clear();
        final SmurfDetector detector = plugin.getSmurfDetector();
        if (detector != null) {
            alerts.addAll(detector.getAlertHistory());
        }
        if (alerts.isEmpty()) {
            setItem(22, createItem(Material.GRAY_DYE, "&7No alerts", List.of(
                    "&7No smurf alerts have",
                    "&7been recorded yet"
            )));
        }
        else {
            for (int index = 0; index < alerts.size() && index < 45; index++) {
                final SmurfDetector.SmurfAlert alert = alerts.get(index);
                setItem(index, createItem(Material.PAPER, "&6" + alert.victimName(), List.of(
                        "&7Last killer: &f" + alert.killerName(),
                        "&7Kills: &c" + alert.totalKills(),
                        "&7Distinct killers: &c" + alert.distinctKillers(),
                        "&7When: &f" + FORMATTER.format(alert.timestamp())
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
