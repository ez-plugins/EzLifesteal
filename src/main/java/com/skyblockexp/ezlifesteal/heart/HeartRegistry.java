package com.skyblockexp.ezlifesteal.heart;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class HeartRegistry {
    private final Map<String, Heart> byId = new HashMap<>();

    private final Map<Integer, Heart> byTier = new HashMap<>();


    public HeartRegistry(YamlConfiguration config) {
        if (config == null) {
            return;
        }
        final ConfigurationSection section = config.getConfigurationSection("hearts");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            final ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            final String id = key;
            final String display = s.getString("display", "&cHeart");
            final int tier = s.getInt("tier", 1);
            final double hearts = s.getDouble("hearts", 1.0);
            final String materialName = s.getString("material", "NETHER_STAR");
            Material material = Material.NETHER_STAR;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            }
            catch (IllegalArgumentException ignored) {
            }
            final String texture = s.getString("texture", "");
            final Map<String, Object> nbt = s.getValues(false);
            final Heart heart = new Heart(id, display, tier, hearts, material, texture, nbt);
            byId.put(id.toLowerCase(), heart);
            if (!byTier.containsKey(tier)) {
                byTier.put(tier, heart);
            }
        }
    }

    public Heart getById(String id) {
        if (id == null) {
            return null;
        }
        return byId.get(id.toLowerCase());
    }

    public Heart getByTier(int tier) {
        return byTier.get(tier);
    }

    public Map<String, Heart> getAll() {
        return Collections.unmodifiableMap(byId);
    }
}
