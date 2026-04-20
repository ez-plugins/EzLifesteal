package com.skyblockexp.ezlifesteal.heart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Heart {
    private final String id;

    private final String displayName;

    private final int tier;

    private final double hearts;

    private final Material material;

    private final String texture;

    private final Map<String, Object> nbt;


    public Heart(String id, String displayName, int tier, double hearts, Material material, String texture, Map<String,
            Object> nbt) {
        this.id = id;
        this.displayName = displayName;
        this.tier = tier;
        this.hearts = hearts;
        this.material = material == null ? Material.NETHER_STAR : material;
        this.texture = texture;
        this.nbt = nbt;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTier() {
        return tier;
    }

    public double getHearts() {
        return hearts;
    }

    public Material getMaterial() {
        return material;
    }

    public String getTexture() {
        return texture;
    }

    public Map<String, Object> getNbt() {
        return nbt;
    }

    public ItemStack createItemStack() {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null && !displayName.isBlank()) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            }
            final List<String> lore = new ArrayList<>();
            lore.add("Tier: " + tier);
            lore.add("Hearts: " + hearts);
            if (texture != null && !texture.isBlank()) {
                lore.add("Texture: " + texture);
            }
            // Do not expose raw NBT values in the lore — use persistent data instead
            meta.setLore(lore);
            item.setItemMeta(meta);
            // Store heart id in persistent data container for identification when used
            try {
                final org.bukkit.NamespacedKey key = com.skyblockexp.ezlifesteal.EzLifestealPlugin.HEART_KEY;
                if (key != null) {
                    final ItemMeta m = item.getItemMeta();
                    if (m != null) {
                        m.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, id);
                        item.setItemMeta(m);
                    }
                }
            }
            catch (NoClassDefFoundError | Exception ignored) {
                // If persistent data API is unavailable, fall back to not storing the id.
            }
        }
        return item;
    }
}
