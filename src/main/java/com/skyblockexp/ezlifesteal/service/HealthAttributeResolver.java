package com.skyblockexp.ezlifesteal.service;

import org.bukkit.attribute.Attribute;

/**
 * Resolves the max-health attribute for the running Bukkit server version.
 */
public interface HealthAttributeResolver {

    Attribute resolveMaxHealthAttribute();
}
