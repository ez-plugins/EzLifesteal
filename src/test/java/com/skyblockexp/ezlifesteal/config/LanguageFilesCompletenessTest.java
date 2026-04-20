package com.skyblockexp.ezlifesteal.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageFilesCompletenessTest {

    private static final Path LANGUAGES_DIR = Path.of("src/main/resources/languages");

    private static final String BASE_LANGUAGE_FILE = "en.yml";


    @Test
    void everyLanguageFileContainsAllKeysFromEnglish() throws Exception {
        assertTrue(Files.exists(LANGUAGES_DIR), "Missing languages directory: " + LANGUAGES_DIR);

        File baseFile = LANGUAGES_DIR.resolve(BASE_LANGUAGE_FILE).toFile();
        assertTrue(baseFile.isFile(), "Missing base language file: " + baseFile.getPath());
        Set<String> baseKeys = extractMessageKeys(baseFile);

        List<Path> languageFiles;
        try (Stream<Path> stream = Files.list(LANGUAGES_DIR)) {
            languageFiles = stream
                    .filter(path -> path.toString().endsWith(".yml"))
                    .filter(path -> !BASE_LANGUAGE_FILE.equals(path.getFileName().toString()))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .collect(Collectors.toList());
        }

        List<String> errors = new ArrayList<>();
        for (Path languageFile : languageFiles) {
            Set<String> currentKeys = extractMessageKeys(languageFile.toFile());
            Set<String> missingKeys = new TreeSet<>(baseKeys);
            missingKeys.removeAll(currentKeys);
            if (!missingKeys.isEmpty()) {
                errors.add(languageFile.getFileName() + " missing keys: " + String.join(", ", missingKeys));
            }
        }

        assertTrue(errors.isEmpty(), String.join(System.lineSeparator(), errors));
    }

    private Set<String> extractMessageKeys(File file) {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        return configuration.getKeys(true).stream()
                .filter(path -> !configuration.isConfigurationSection(path))
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
