package com.skyblockexp.ezlifesteal.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class WorldNameUtil {
    private WorldNameUtil() { }

    public static String normalizeWorldName(String worldName) {
        return worldName == null ? "" : worldName.toLowerCase(Locale.ROOT);
    }

    public static Set<String> parseWorldList(List<String> worldNames) {
        if (worldNames == null || worldNames.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<String> parsed = new HashSet<>();
        for (String name : worldNames) {
            if (name != null && !name.isBlank()) {
                parsed.add(normalizeWorldName(name));
            }
        }
        return Collections.unmodifiableSet(parsed);
    }
}
