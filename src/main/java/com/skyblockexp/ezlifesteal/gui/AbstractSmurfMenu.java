package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class AbstractSmurfMenu implements InventoryHolder {

    protected final EzLifestealPlugin plugin;

    protected final Player viewer;

    private final Inventory inventory;


    protected AbstractSmurfMenu(EzLifestealPlugin plugin, Player viewer, int size, String title) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, size, ChatColor.translateAlternateColorCodes('&', title));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    protected void setItem(int slot, ItemStack itemStack) {
        inventory.setItem(slot, itemStack);
    }

    protected ItemStack createItem(Material material, String displayName, List<String> lore) {
        final ItemStack itemStack = new ItemStack(material);
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            }
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).toList());
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    public abstract void handleClick(org.bukkit.event.inventory.InventoryClickEvent event);
}
