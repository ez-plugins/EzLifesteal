package com.skyblockexp.ezlifesteal.util;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

public final class LanguageUtil {
    private LanguageUtil() { }

    public static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        final int separatorIndex = normalized.indexOf('_');
        if (separatorIndex > 0) {
            normalized = normalized.substring(0, separatorIndex);
        }
        return normalized;
    }

    public static String resolveMessage(File dataFolder, String locale, String key, Map<String, String> placeholders) {
        if (key == null || key.isBlank()) {
            return missingKeyPlaceholder(key);
        }

        final YamlConfiguration selectedLocale = loadLocale(dataFolder, normalizeLanguage(locale));
        final YamlConfiguration defaultLocale = loadLocale(dataFolder, "en");

        String message = selectedLocale == null ? null : selectedLocale.getString(key);
        if (message == null) {
            message = defaultLocale == null ? null : defaultLocale.getString(key);
        }
        if (message == null) {
            return missingKeyPlaceholder(key);
        }

        return applyPlaceholders(message, placeholders);
    }

    private static YamlConfiguration loadLocale(File dataFolder, String locale) {
        if (dataFolder == null || locale == null || locale.isBlank()) {
            return null;
        }

        final File languageFile = new File(dataFolder, "languages/" + locale + ".yml");
        if (!languageFile.exists()) {
            return null;
        }

        return YamlConfiguration.loadConfiguration(languageFile);
    }

    private static String applyPlaceholders(String input, Map<String, String> placeholders) {
        String message = input;
        if (placeholders == null || placeholders.isEmpty()) {
            return message;
        }

        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            message = message.replace("%" + placeholder.getKey() + "%", placeholder.getValue());
        }
        return message;
    }

    private static String missingKeyPlaceholder(String key) {
        final String safeKey = key == null ? "null" : key;
        return "??" + safeKey + "??";
    }
}
