package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.compat.AdapterSupport;
import com.skyblockexp.ezlifesteal.compat.ShopPersistentKeys;
import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.util.NumberFormatUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ShopGuiListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopMenu menu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() == null) {
            return;
        }
        final ItemStack item = event.getCurrentItem();
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        String heartId = null;
        double price = 0.0;
        boolean invalidPrice = false;
        int quantity = 1;
        List<String> commands = null;

        final var persistentDataContainer = meta.getPersistentDataContainer();
        Double storedPrice = null;
        Integer storedQuantity = null;
        String storedCommands = null;
        if (persistentDataContainer != null) {
            heartId = persistentDataContainer.get(ShopPersistentKeys.shopIdKey(), PersistentDataType.STRING);
            storedPrice = persistentDataContainer.get(ShopPersistentKeys.shopPriceKey(), PersistentDataType.DOUBLE);
            storedQuantity =
                persistentDataContainer.get(ShopPersistentKeys.shopQuantityKey(), PersistentDataType.INTEGER);
            storedCommands =
                persistentDataContainer.get(ShopPersistentKeys.shopCommandsKey(), PersistentDataType.STRING);
        }

        if (storedPrice != null) {
            price = storedPrice;
        }
        if (storedQuantity != null) {
            quantity = storedQuantity;
        }
        if (storedCommands != null && !storedCommands.isBlank()) {
            commands = java.util.Arrays.asList(storedCommands.split(";;"));
        }

        if (heartId == null && meta.hasLore()) {
            for (String line : meta.getLore()) {
                final String plain = ChatColor.stripColor(line);
                if (plain == null) {
                    continue;
                }
                if (plain.startsWith("shop-id:")) {
                    heartId = plain.substring(plain.indexOf(':') + 1);
                }
                else if (plain.startsWith("shop-price:")) {
                    try {
                        price = Double.parseDouble(plain.substring(plain.indexOf(':') + 1));
                    }
                    catch (Throwable ignored) {
                        invalidPrice = true;
                    }
                }
                else if (plain.startsWith("shop-qty:")) {
                    try {
                        quantity = Integer.parseInt(plain.substring(plain.indexOf(':') + 1));
                    }
                    catch (Throwable ignored) {
                    }
                }
                else if (plain.startsWith("shop-cmds:")) {
                    final String raw = plain.substring(plain.indexOf(':') + 1);
                    if (raw != null && !raw.isBlank()) {
                        commands = java.util.Arrays.asList(raw.split(";;"));
                    }
                }
            }
        }
        if (heartId == null) {
            return;
        }
        final var plugin = menu.plugin;
        final var player = menu.viewer;
        if (invalidPrice || !Double.isFinite(price) || price < 0.0) {
            player
                    .sendMessage(plugin.getMessageService().getPrefix() + "This item has an invalid price and cannot be"
                            + " purchased.");
            return;
        }
        final double chargeAmount = roundCurrency(price);
        final Economy economy = plugin.getEconomy().orElse(null);
        if (economy == null) {
            player
                    .sendMessage(plugin.getMessageService().getPrefix() + "Currency support is unavailable on this"
                            + " server.");
            return;
        }
        try {
            if (!economy.has(player, chargeAmount)) {
                player.sendMessage(plugin.getMessageService().getPrefix()
                        + "You do not have enough funds to purchase this item. Price: "
                        + NumberFormatUtil.formatCompact(chargeAmount) + ".");
                return;
            }
        }
        catch (Throwable throwable) {
            player
                    .sendMessage(plugin.getMessageService().getPrefix() + "Currency support is unavailable on this"
                            + " server.");
            return;
        }
        final var r = economy.withdrawPlayer(player, chargeAmount);
        if (r != null && r.transactionSuccess()) {
            final Heart heart = plugin.getHeartRegistry().getById(heartId);
            if (heart != null) {
                final ItemStack toGive = heart.createItemStack();
                toGive.setAmount(Math.max(1, Math.min(toGive.getMaxStackSize(), quantity)));
                final var leftover = player.getInventory().addItem(toGive);
                if (!leftover.isEmpty()) {
                    AdapterSupport.dropItemLeftoversAtPlayer(plugin, player, leftover);
                }
                player.sendMessage(plugin.getMessageService().getPrefix()
                        + "Purchase successful for " + NumberFormatUtil.formatCompact(chargeAmount) + ".");
                if (commands != null && !commands.isEmpty()) {
                    {
                    }
                    for (String cmd : commands) {
                        {
                        }
                        if (cmd == null || cmd.isBlank()) {
                            continue;
                        }
                        final String resolved = cmd.replace("{player}", player.getName());
                        org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), resolved);
                    }
                }
            }
            else {
                player.sendMessage(plugin.getMessageService().getPrefix() + "This item is no longer available.");
            }
        }
        else {
            player.sendMessage(plugin.getMessageService().getPrefix()
                    + "Failed to withdraw funds for purchase of " + NumberFormatUtil.formatCompact(chargeAmount) + ".");
        }
    }

    private double roundCurrency(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
