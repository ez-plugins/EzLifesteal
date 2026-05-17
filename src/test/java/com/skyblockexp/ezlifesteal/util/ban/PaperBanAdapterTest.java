package com.skyblockexp.ezlifesteal.util.ban;

import com.destroystokyo.paper.profile.PlayerProfile;
import java.time.Instant;
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

@SuppressWarnings({"rawtypes", "unchecked"})
class PaperBanAdapterTest {

    private final PaperBanAdapter adapter = new PaperBanAdapter();

    @Test
    void addBanInsertsEntryWhenNotAlreadyBanned() {
        UUID playerId = UUID.randomUUID();
        PlayerProfile profile = mock(PlayerProfile.class);
        BanList<PlayerProfile> banList = mock(BanList.class);
        when(banList.isBanned(profile)).thenReturn(false);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.createProfile(eq(playerId), eq("Alice"))).thenReturn(profile);
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);

            adapter.addBan(playerId, "Alice", "grief", "console", null);

            verify(banList).addBan(eq(profile), eq("grief"), (Instant) eq(null), eq("console"));
        }
    }

    @Test
    void addBanSkipsWhenAlreadyBanned() {
        UUID playerId = UUID.randomUUID();
        PlayerProfile profile = mock(PlayerProfile.class);
        BanList<PlayerProfile> banList = mock(BanList.class);
        when(banList.isBanned(profile)).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.createProfile(eq(playerId), any(String.class))).thenReturn(profile);
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);

            adapter.addBan(playerId, "Alice", "grief", "console", null);

            verify(banList, never()).addBan(any(), any(), (Instant) any(), any());
        }
    }

    @Test
    void removeBanPardonsProfile() {
        UUID playerId = UUID.randomUUID();
        PlayerProfile profile = mock(PlayerProfile.class);
        BanList<PlayerProfile> banList = mock(BanList.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.createProfile(eq(playerId), eq("Bob"))).thenReturn(profile);
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);

            adapter.removeBan(playerId, "Bob");

            verify(banList).pardon(profile);
        }
    }

    @Test
    void isBannedReturnsTrueWhenProfileBanned() {
        UUID playerId = UUID.randomUUID();
        PlayerProfile profile = mock(PlayerProfile.class);
        BanList<PlayerProfile> banList = mock(BanList.class);
        when(banList.isBanned(profile)).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.createProfile(eq(playerId), eq("Carol"))).thenReturn(profile);
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);

            assertTrue(adapter.isBanned(playerId, "Carol"));
        }
    }

    @Test
    void isBannedReturnsFalseWhenProfileNotBanned() {
        UUID playerId = UUID.randomUUID();
        PlayerProfile profile = mock(PlayerProfile.class);
        BanList<PlayerProfile> banList = mock(BanList.class);
        when(banList.isBanned(profile)).thenReturn(false);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.createProfile(eq(playerId), eq("Dave"))).thenReturn(profile);
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);

            assertFalse(adapter.isBanned(playerId, "Dave"));
        }
    }

    @Test
    void getBanEntriesMapsProfileBanEntriesToViews() {
        UUID playerId = UUID.randomUUID();
        PlayerProfile profile = mock(PlayerProfile.class);
        when(profile.getId()).thenReturn(playerId);
        when(profile.getName()).thenReturn("Eve");

        Date created = new Date();
        BanEntry<PlayerProfile> entry = mock(BanEntry.class);
        when(entry.getBanTarget()).thenReturn(profile);
        when(entry.getReason()).thenReturn("spam");
        when(entry.getSource()).thenReturn("admin");
        when(entry.getCreated()).thenReturn(created);
        when(entry.getExpiration()).thenReturn(null);

        BanList<PlayerProfile> banList = mock(BanList.class);
        Set<BanEntry<PlayerProfile>> entries = new LinkedHashSet<>();
        entries.add(entry);
        when(banList.getBanEntries()).thenReturn((Set) entries);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);

            Set<BanEntryView> views = adapter.getBanEntries();

            assertEquals(1, views.size());
            BanEntryView view = views.iterator().next();
            assertEquals(playerId, view.getPlayerId());
            assertEquals("Eve", view.getPlayerName());
            assertEquals("spam", view.getReason());
            assertEquals("admin", view.getSource());
            assertEquals(created, view.getCreated());
            assertNull(view.getExpiration());
        }
    }

    @Test
    void getBanEntriesSkipsNullProfiles() {
        BanEntry<PlayerProfile> entry = mock(BanEntry.class);
        when(entry.getBanTarget()).thenReturn(null);

        BanList<PlayerProfile> banList = mock(BanList.class);
        Set<BanEntry<PlayerProfile>> entries = new LinkedHashSet<>();
        entries.add(entry);
        when(banList.getBanEntries()).thenReturn((Set) entries);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);

            Set<BanEntryView> views = adapter.getBanEntries();

            assertTrue(views.isEmpty());
        }
    }

    @Test
    void getBanEntriesReturnsEmptySetWhenBanListEmpty() {
        BanList<PlayerProfile> banList = mock(BanList.class);
        when(banList.getBanEntries()).thenReturn(new LinkedHashSet<>());

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);

            Set<BanEntryView> views = adapter.getBanEntries();

            assertNotNull(views);
            assertTrue(views.isEmpty());
        }
    }
}
