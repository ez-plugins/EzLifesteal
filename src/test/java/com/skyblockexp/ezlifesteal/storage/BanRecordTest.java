package com.skyblockexp.ezlifesteal.storage;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BanRecordTest {

    @Test
    void constructorRejectsNullUniqueId() {
        Instant createdAt = Instant.parse("2026-01-02T03:04:05Z");

        assertThrows(NullPointerException.class,
                () -> new BanRecord(null, "PlayerOne", "Reason", "EzLifesteal", createdAt, null, true));
    }

    @Test
    void constructorRejectsNullCreatedAt() {
        UUID uniqueId = UUID.randomUUID();

        assertThrows(NullPointerException.class,
                () -> new BanRecord(uniqueId, "PlayerOne", "Reason", "EzLifesteal", null, null, true));
    }

    @Test
    void gettersReturnConstructorValues() {
        UUID uniqueId = UUID.randomUUID();
        String playerName = "PlayerOne";
        String reason = "Reached minimum hearts";
        String source = "EzLifesteal";
        Instant createdAt = Instant.parse("2026-01-02T03:04:05Z");
        Instant expiresAt = Instant.parse("2026-01-09T03:04:05Z");
        boolean active = true;

        BanRecord record = new BanRecord(uniqueId, playerName, reason, source, createdAt, expiresAt, active);

        assertEquals(uniqueId, record.getUniqueId());
        assertEquals(playerName, record.getPlayerName());
        assertEquals(reason, record.getReason());
        assertEquals(source, record.getSource());
        assertEquals(createdAt, record.getCreatedAt());
        assertEquals(expiresAt, record.getExpiresAt());
        assertTrue(record.isActive());
    }

    @Test
    void nullableFieldsCanBeNull() {
        UUID uniqueId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-02T03:04:05Z");

        BanRecord record = new BanRecord(uniqueId, null, null, null, createdAt, null, false);

        assertEquals(uniqueId, record.getUniqueId());
        assertNull(record.getPlayerName());
        assertNull(record.getReason());
        assertNull(record.getSource());
        assertEquals(createdAt, record.getCreatedAt());
        assertNull(record.getExpiresAt());
        assertFalse(record.isActive());
    }
}
