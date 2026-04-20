package com.skyblockexp.ezlifesteal.storage.yaml;

import com.skyblockexp.ezlifesteal.storage.BanRecord;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlBanRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void createSaveLoadAndUnbanRecord() throws Exception {
        YamlBanRepository repository = new YamlBanRepository(tempDir.resolve("bans").toFile());
        repository.init();

        UUID uniqueId = UUID.randomUUID();
        BanRecord record = new BanRecord(
                uniqueId,
                "PlayerOne",
                "Zero hearts",
                "EzLifesteal",
                Instant.parse("2026-01-02T03:04:05Z"),
                null,
                true
        );

        repository.saveBan(record);

        Optional<BanRecord> loaded = repository.loadBan(uniqueId);
        assertTrue(loaded.isPresent());
        assertEquals("PlayerOne", loaded.get().getPlayerName());
        assertEquals("Zero hearts", loaded.get().getReason());
        assertEquals("EzLifesteal", loaded.get().getSource());
        assertEquals(Instant.parse("2026-01-02T03:04:05Z"), loaded.get().getCreatedAt());
        assertNull(loaded.get().getExpiresAt());
        assertTrue(loaded.get().isActive());

        repository.removeBan(uniqueId);
        assertTrue(repository.loadBan(uniqueId).isEmpty());
    }
}
