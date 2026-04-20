package com.skyblockexp.ezlifesteal.detector;

import com.skyblockexp.ezlifesteal.config.MessageService;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmurfDetectorTest {

    @Test
    void sameKillerRepeatedKills_triggersAlert_and_notifiesPermissionedPlayers() {
        MessageService messages = mock(MessageService.class);
        when(messages.format(eq("smurf-alert"), anyMap())).thenReturn("ALERT");

        Player notifier = mock(Player.class);
        when(notifier.hasPermission("smurf.notify")).thenReturn(true);

        try (MockedStatic<Bukkit> b = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            b.when(Bukkit::getOnlinePlayers).thenReturn(Set.of(notifier));

            SmurfDetector detector = new SmurfDetector(
                    messages,
                    true,
                    2,
                    Duration.ofMinutes(10),
                    "smurf.notify",
                    null,
                    false,
                    null,
                    10,
                    10
            );

            Player killer = mock(Player.class);
            Player victim = mock(Player.class);
            UUID kId = UUID.randomUUID();
            UUID vId = UUID.randomUUID();
            when(killer.getUniqueId()).thenReturn(kId);
            when(killer.getName()).thenReturn("killer");
            when(victim.getUniqueId()).thenReturn(vId);
            when(victim.getName()).thenReturn("victim");

            detector.recordKill(killer, victim);
            detector.recordKill(killer, victim);

            assertFalse(detector.getAlertHistory().isEmpty(), "Alert history should contain an alert");
            assertEquals(2, detector.getKillHistory().size(), "Kill history should record both kills");
            verify(notifier).sendMessage("ALERT");
        }
    }

    @Test
    void multipleDistinctKillers_triggersAlert_withDistinctCount() {
        MessageService messages = mock(MessageService.class);
        when(messages.format(eq("smurf-alert"), anyMap())).thenReturn("ALERT2");

        Player notifier = mock(Player.class);
        when(notifier.hasPermission("smurf.notify")).thenReturn(true);

        try (MockedStatic<Bukkit> b = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            b.when(Bukkit::getOnlinePlayers).thenReturn(Set.of(notifier));

            SmurfDetector detector = new SmurfDetector(
                    messages,
                    true,
                    2,
                    Duration.ofMinutes(10),
                    "smurf.notify",
                    null,
                    false,
                    null,
                    10,
                    10
            );

            Player killer1 = mock(Player.class);
            Player killer2 = mock(Player.class);
            Player victim = mock(Player.class);
            when(killer1.getUniqueId()).thenReturn(UUID.randomUUID());
            when(killer1.getName()).thenReturn("k1");
            when(killer2.getUniqueId()).thenReturn(UUID.randomUUID());
            when(killer2.getName()).thenReturn("k2");
            UUID vId = UUID.randomUUID();
            when(victim.getUniqueId()).thenReturn(vId);
            when(victim.getName()).thenReturn("victim2");

            detector.recordKill(killer1, victim);
            detector.recordKill(killer2, victim);

            assertFalse(detector.getAlertHistory().isEmpty(), "Alert should be created for multiple distinct killers");
            assertEquals(2, detector.getKillHistory().size());
            verify(notifier).sendMessage("ALERT2");
        }
    }

    @Test
    void exemptPlayers_areIgnored_and_notRecorded() {
        MessageService messages = mock(MessageService.class);
        SmurfDetector detector = new SmurfDetector(
            messages,
            true,
            2,
            Duration.ofMinutes(10),
            "smurf.notify",
            null,
            false,
            null,
            10,
            10
        );

        Player killer = mock(Player.class);
        Player victim = mock(Player.class);
        UUID killerId = UUID.randomUUID();
        when(killer.getUniqueId()).thenReturn(killerId);
        when(killer.getName()).thenReturn("ignored");
        UUID victimId = UUID.randomUUID();
        when(victim.getUniqueId()).thenReturn(victimId);
        when(victim.getName()).thenReturn("v");

        // ensure not exempt initially
        assertFalse(detector.isExempt(killerId));

        // add exempt and ensure recordKill does nothing
        detector.addExemptPlayer(killerId);
        assertTrue(detector.isExempt(killerId));

        detector.recordKill(killer, victim);
        assertTrue(detector.getKillHistory().isEmpty(), "No kills recorded for exempt killer");
    }

    @Test
    void addAndRemoveExemptPlayer_behavesCorrectly() {
        MessageService messages = mock(MessageService.class);
        SmurfDetector detector = new SmurfDetector(
                messages,
                true,
                2,
                Duration.ofMinutes(10),
                "smurf.notify",
                null,
                false,
                null,
                10,
                10
        );

        UUID id = UUID.randomUUID();
        assertTrue(detector.addExemptPlayer(id));
        assertFalse(detector.addExemptPlayer(id));
        assertTrue(detector.removeExemptPlayer(id));
        assertFalse(detector.removeExemptPlayer(id));
    }
}
