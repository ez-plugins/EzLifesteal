package com.skyblockexp.ezlifesteal.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void keyExistsInSelectedLocale() throws IOException {
        writeLanguageFile("en", "welcome: 'Welcome %player%'");
        writeLanguageFile("nl", "welcome: 'Welkom %player%'");

        String message = LanguageUtil.resolveMessage(
                tempDir.toFile(),
                "nl",
                "welcome",
                Map.of("player", "Alex")
        );

        assertEquals("Welkom Alex", message);
    }

    @Test
    void missingKeyFallsBackToDefaultLocaleFile() throws IOException {
        writeLanguageFile("en", "fallback-key: 'From english default'");
        writeLanguageFile("nl", "other-key: 'Alleen nederlands'");

        String message = LanguageUtil.resolveMessage(
                tempDir.toFile(),
                "nl-NL",
                "fallback-key",
                Map.of()
        );

        assertEquals("From english default", message);
    }

    @Test
    void missingKeyInBothLocalesReturnsDeterministicPlaceholder() throws IOException {
        writeLanguageFile("en", "known: 'Known'");
        writeLanguageFile("nl", "known: 'Bekend'");

        String message = LanguageUtil.resolveMessage(
                tempDir.toFile(),
                "nl",
                "missing.key",
                null
        );

        assertEquals("??missing.key??", message);
    }

    @Test
    void placeholderReplacementSupportsOneAndMultipleTokens() throws IOException {
        writeLanguageFile("en", "single: 'Hi %player%'\nmulti: '%player% has %hearts% hearts in %world%.'");

        String single = LanguageUtil.resolveMessage(
                tempDir.toFile(),
                "en",
                "single",
                Map.of("player", "Riley")
        );

        String multiple = LanguageUtil.resolveMessage(
                tempDir.toFile(),
                "en",
                "multi",
                Map.of("player", "Riley", "hearts", "3", "world", "spawn")
        );

        assertEquals("Hi Riley", single);
        assertEquals("Riley has 3 hearts in spawn.", multiple);
    }

    private void writeLanguageFile(String locale, String yaml) throws IOException {
        Path languagesDir = tempDir.resolve("languages");
        Files.createDirectories(languagesDir);
        Files.writeString(languagesDir.resolve(locale + ".yml"), yaml + System.lineSeparator());
    }
}
