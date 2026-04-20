package com.skyblockexp.ezlifesteal.detector;

import java.util.Set;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDetectorTest {

    @Test
    void returnsFalseWhenDetectorDisabledOrPlayerNull() {
        AdminDetector disabledDetector = new AdminDetector(
                false,
                true,
                "ezlifesteal.admin",
                Set.of(),
                Set.of()
        );

        OfflinePlayer player = mock(OfflinePlayer.class);
        assertFalse(disabledDetector.isAdmin(player));

        AdminDetector enabledDetector = new AdminDetector(
                true,
                true,
                "ezlifesteal.admin",
                Set.of(),
                Set.of()
        );
        assertFalse(enabledDetector.isAdmin(null));
    }

    @Test
    void returnsTrueForAllowedUuid() {
        UUID allowedId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        OfflinePlayer player = mock(OfflinePlayer.class);
        when(player.getUniqueId()).thenReturn(allowedId);

        AdminDetector detector = new AdminDetector(
                true,
                false,
                "",
                Set.of(allowedId),
                Set.of()
        );

        assertTrue(detector.isAdmin(player));
    }

    @Test
    void returnsTrueForAllowedNameUsingLowercaseNormalization() {
        OfflinePlayer player = mock(OfflinePlayer.class);
        when(player.getName()).thenReturn("AdMiNUser");

        AdminDetector detector = new AdminDetector(
                true,
                false,
                "",
                Set.of(),
                Set.of("adminuser")
        );

        assertTrue(detector.isAdmin(player));
    }

    @Test
    void returnsTrueWhenTreatOpsAsAdminAndPlayerIsOp() {
        OfflinePlayer player = mock(OfflinePlayer.class);
        when(player.isOp()).thenReturn(true);

        AdminDetector detector = new AdminDetector(
                true,
                true,
                "",
                Set.of(),
                Set.of()
        );

        assertTrue(detector.isAdmin(player));
    }

    @Test
    void returnsTrueWhenPermissionNodeSetAndOnlinePlayerHasPermission() {
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        Player onlinePlayer = mock(Player.class);
        when(offlinePlayer.getPlayer()).thenReturn(onlinePlayer);
        when(onlinePlayer.hasPermission("ezlifesteal.admin")).thenReturn(true);

        AdminDetector detector = new AdminDetector(
                true,
                false,
                "ezlifesteal.admin",
                Set.of(),
                Set.of()
        );

        assertTrue(detector.isAdmin(offlinePlayer));
    }

    @Test
    void returnsFalseWhenPermissionNodeSetButOnlinePlayerIsNull() {
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(offlinePlayer.getPlayer()).thenReturn(null);

        AdminDetector detector = new AdminDetector(
                true,
                false,
                "ezlifesteal.admin",
                Set.of(),
                Set.of()
        );

        assertFalse(detector.isAdmin(offlinePlayer));
    }

    @Test
    void returnsFalseWhenNoRuleMatches() {
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        Player onlinePlayer = mock(Player.class);

        UUID notAllowedId = UUID.fromString("00000000-0000-0000-0000-000000000202");
        when(offlinePlayer.getUniqueId()).thenReturn(notAllowedId);
        when(offlinePlayer.getName()).thenReturn("RegularPlayer");
        when(offlinePlayer.isOp()).thenReturn(false);
        when(offlinePlayer.getPlayer()).thenReturn(onlinePlayer);
        when(onlinePlayer.hasPermission("ezlifesteal.admin")).thenReturn(false);

        AdminDetector detector = new AdminDetector(
                true,
                false,
                "ezlifesteal.admin",
                Set.of(UUID.fromString("00000000-0000-0000-0000-000000000999")),
                Set.of("someotheradmin")
        );

        assertFalse(detector.isAdmin(offlinePlayer));
        verify(onlinePlayer).hasPermission("ezlifesteal.admin");
    }

    @Test
    void doesNotQueryOnlinePlayerWhenOperatorRuleAlreadyMatches() {
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        Player onlinePlayer = mock(Player.class);
        when(offlinePlayer.isOp()).thenReturn(true);
        when(offlinePlayer.getPlayer()).thenReturn(onlinePlayer);

        AdminDetector detector = new AdminDetector(
                true,
                true,
                "ezlifesteal.admin",
                Set.of(),
                Set.of()
        );

        assertTrue(detector.isAdmin(offlinePlayer));
        verify(onlinePlayer, never()).hasPermission("ezlifesteal.admin");
    }
}
