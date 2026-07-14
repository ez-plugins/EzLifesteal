package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.compat.ShopPersistentKeys;
import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.util.NumberFormatUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ShopMenu extends AbstractSmurfMenu {
    private static final String META_COMMAND_SEPARATOR = ";;";

    public ShopMenu(EzLifestealPlugin plugin, Player viewer) {
        super(
            plugin,
            viewer,
            Math.max(9, Math.min(54, plugin.getShopConfig().getInt("size", 54))),
            plugin.getShopConfig().getString("title", "&cHeart Shop")
        );
        populate();
    }

    private void populate() {
        final YamlConfiguration cfg = plugin.getShopConfig();
        final List<Map<?, ?>> items = cfg.getMapList("items");
        final HeartRegistry registry = plugin.getHeartRegistry();
        for (Map<?, ?> entry : items) {
            try {
                final int slot = (entry.containsKey("slot") ? ((Number) entry.get("slot")).intValue() : -1);
                final String heartId = entry.containsKey("heart") ? entry.get("heart").toString() : null;
                final double price =
                        entry.containsKey("price") ? Double.parseDouble(entry.get("price").toString()) : 0.0;
                final String display = entry.containsKey("display-name") ? entry.get("display-name").toString() : null;
                final List<String> lore = entry.containsKey("lore") ? (List<String>) entry.get("lore") : null;
                final int quantity = entry.containsKey("quantity") ? ((Number) entry.get("quantity")).intValue() : 1;
                final List<String> commands =
                        entry.containsKey("commands") ? (List<String>) entry.get("commands") : null;
                final String icon = entry.containsKey("icon") ? entry.get("icon").toString() : null;
                if (slot < 0 || heartId == null) {
                    continue;
                }
                final Heart heart = registry.getById(heartId);
                if (heart == null) {
                    continue;
                }
                final ItemStack item;
                // create a base item for persistent data and default lore
                final ItemStack base = heart.createItemStack();
                Material mat = null;
                if (icon != null && !icon.isBlank()) {
                    mat = Material.matchMaterial(icon);
                }
                if (mat != null) {
                    item = new ItemStack(mat);
                }
                else {
                    // use the base heart stack which already contains meta and persistent data
                    item = base.clone();
                }
                final ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    // Ensure persistent heart id is present on the displayed item
                    try {
                        final NamespacedKey key = EzLifestealPlugin.HEART_KEY;
                        if (key != null) {
                            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, heart.getId());
                        }
                    }
                    catch (Throwable ignored) {
                    }

                    // Display name: prefer configured display, fall back to heart's display
                    if (display != null && !display.isBlank()) {
                        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', display));
                    }
                    else if (heart.getDisplayName() != null && !heart.getDisplayName().isBlank()) {
                        meta
                                .setDisplayName(org.bukkit.ChatColor
                                        .translateAlternateColorCodes('&', heart.getDisplayName()));
                    }

                    final List<String> effectiveLore = new ArrayList<>();
                    if (lore != null && !lore.isEmpty()) {
                        for (String line : lore) {
                            effectiveLore.add(ChatColor.translateAlternateColorCodes('&', line));
                        }
                    }
                    effectiveLore
                            .add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + NumberFormatUtil.formatCompact(price));

                    meta.getPersistentDataContainer()
                        .set(ShopPersistentKeys.shopIdKey(), PersistentDataType.STRING, heartId);
                    meta.getPersistentDataContainer()
                        .set(ShopPersistentKeys.shopPriceKey(), PersistentDataType.DOUBLE, price);
                    meta.getPersistentDataContainer()
                        .set(ShopPersistentKeys.shopQuantityKey(), PersistentDataType.INTEGER, quantity);
                    if (commands != null && !commands.isEmpty()) {
                        meta.getPersistentDataContainer()
                            .set(ShopPersistentKeys.shopCommandsKey(), PersistentDataType.STRING,
                                    String.join(META_COMMAND_SEPARATOR, commands));
                    }

                    meta.setLore(effectiveLore);
                    item.setItemMeta(meta);
                }
                setItem(slot, item);
            }
            catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void handleClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        // Shop clicks are handled by ShopGuiListener
    }
}
