package com.skyblockexp.ezlifesteal.util.ban;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SpigotBanAdapter}.
 *
 * <p>The Bukkit {@link BanList} interface has both {@code isBanned(T)} and {@code isBanned(String)}
 * overloads.  When {@code T = String} (name-based ban list), the two overloads have the same
 * erasure and the Java compiler reports an ambiguous reference.  To avoid this, all mock
 * instances are declared as the raw type {@code BanList} and the relevant suppress-warnings
 * annotation is applied per test class.</p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class SpigotBanAdapterTest {

    private final SpigotBanAdapter adapter = new SpigotBanAdapter();

    @Test
    void addBanInsertsNameWhenNotAlreadyBanned() {
        UUID playerId = UUID.randomUUID();
        BanList banList = mock(BanList.class);
        when(banList.isBanned("Alice")).thenReturn(false);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);

            adapter.addBan(playerId, "Alice", "grief", "console", null);

            verify(banList).addBan(eq("Alice"), eq("grief"), (Date) eq(null), eq("console"));
        }
    }

    @Test
    void addBanSkipsWhenAlreadyBanned() {
        UUID playerId = UUID.randomUUID();
        BanList banList = mock(BanList.class);
        when(banList.isBanned("Bob")).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);

            adapter.addBan(playerId, "Bob", "grief", "console", null);

            verify(banList, never()).addBan(any(), any(), (Date) any(), any());
        }
    }

    @Test
    void addBanIgnoresNullOrBlankName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            BanList banList = mock(BanList.class);
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);

            adapter.addBan(UUID.randomUUID(), null, "reason", "console", null);
            adapter.addBan(UUID.randomUUID(), "  ", "reason", "console", null);

            verify(banList, never()).addBan(any(), any(), (Date) any(), any());
        }
    }

    @Test
    void removeBanPardonsName() {
        BanList banList = mock(BanList.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);

            adapter.removeBan(UUID.randomUUID(), "Carol");

            verify(banList).pardon(eq("Carol"));
        }
    }

    @Test
    void removeBanIgnoresBlankName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            BanList banList = mock(BanList.class);
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);

            adapter.removeBan(UUID.randomUUID(), "  ");

            verify(banList, never()).pardon(any());
        }
    }

    @Test
    void isBannedReturnsTrueWhenNameBanned() {
        BanList banList = mock(BanList.class);
        when(banList.isBanned("Dave")).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);

            assertTrue(adapter.isBanned(UUID.randomUUID(), "Dave"));
        }
    }

    @Test
    void isBannedReturnsFalseForNullOrBlankName() {
        assertFalse(adapter.isBanned(UUID.randomUUID(), null));
        assertFalse(adapter.isBanned(UUID.randomUUID(), "  "));
    }

    @Test
    void getBanEntriesMapsNameBanEntriesToViewsWithNullPlayerId() {
        Date created = new Date();
        BanEntry entry = mock(BanEntry.class);
        when(entry.getBanTarget()).thenReturn("Eve");
        when(entry.getReason()).thenReturn("spam");
        when(entry.getSource()).thenReturn("admin");
        when(entry.getCreated()).thenReturn(created);
        when(entry.getExpiration()).thenReturn(null);

        BanList banList = mock(BanList.class);
        Set<BanEntry> entries = new LinkedHashSet<>();
        entries.add(entry);
        when(banList.getBanEntries()).thenReturn(entries);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);

            Set<BanEntryView> views = adapter.getBanEntries();

            assertEquals(1, views.size());
            BanEntryView view = views.iterator().next();
            assertNull(view.getPlayerId(), "Spigot adapter must return null playerId");
            assertEquals("Eve", view.getPlayerName());
            assertEquals("spam", view.getReason());
            assertEquals("admin", view.getSource());
            assertEquals(created, view.getCreated());
            assertNull(view.getExpiration());
        }
    }

    @Test
    void getBanEntriesSkipsBlankNames() {
        BanEntry blank = mock(BanEntry.class);
        when(blank.getBanTarget()).thenReturn("  ");

        BanList banList = mock(BanList.class);
        Set<BanEntry> entries = new LinkedHashSet<>();
        entries.add(blank);
        when(banList.getBanEntries()).thenReturn(entries);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);

            Set<BanEntryView> views = adapter.getBanEntries();

            assertTrue(views.isEmpty());
        }
    }

    @Test
    void getBanEntriesReturnsEmptySetWhenBanListEmpty() {
        BanList banList = mock(BanList.class);
        when(banList.getBanEntries()).thenReturn(new LinkedHashSet<>());

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);

            Set<BanEntryView> views = adapter.getBanEntries();

            assertNotNull(views);
            assertTrue(views.isEmpty());
        }
    }
}

