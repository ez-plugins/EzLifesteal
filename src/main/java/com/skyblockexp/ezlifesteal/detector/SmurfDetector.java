package com.skyblockexp.ezlifesteal.detector;

import com.skyblockexp.ezlifesteal.config.MessageService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class SmurfDetector {

    private final MessageService messages;

    private final boolean enabled;

    private final int sameVictimThreshold;

    private final Duration window;

    private final String notifyPermission;

    private final AdminDetector adminDetector;

    private final boolean restrictAlertsToAdmins;


    private final Map<UUID, Map<UUID, Deque<Instant>>> recentKills = new HashMap<>();

    private final Map<UUID, Deque<KillEvent>> recentVictimKills = new HashMap<>();

    private final Set<UUID> exemptPlayers;

    private final Deque<SmurfAlert> alertHistory = new ArrayDeque<>();

    private final Deque<KillRecord> killHistory = new ArrayDeque<>();

    private final int alertHistoryLimit;

    private final int killHistoryLimit;


    public SmurfDetector(MessageService messages,
                         boolean enabled,
                         int sameVictimThreshold,
                         Duration window,
                         String notifyPermission,
                         AdminDetector adminDetector,
                         boolean restrictAlertsToAdmins,
                         Set<UUID> exemptPlayers,
                         int alertHistoryLimit,
                         int killHistoryLimit) {
        this.messages = messages;
        this.enabled = enabled;
        this.sameVictimThreshold = sameVictimThreshold;
        this.window = window;
        this.notifyPermission = notifyPermission;
        this.adminDetector = adminDetector;
        this.restrictAlertsToAdmins = restrictAlertsToAdmins;
        this.exemptPlayers = exemptPlayers == null ? new HashSet<>() : exemptPlayers;
        this.alertHistoryLimit = Math.max(1, alertHistoryLimit);
        this.killHistoryLimit = Math.max(1, killHistoryLimit);
    }

    public void recordKill(Player killer, Player victim) {
        if (!enabled || killer == null || victim == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        final Instant now = Instant.now();
        final UUID killerId = killer.getUniqueId();
        final UUID victimId = victim.getUniqueId();

        if (isExempt(killerId) || isExempt(victimId)) {
            return;
        }

        recordKillHistory(killer, victim, now);

        final Map<UUID, Deque<Instant>> killerKills = recentKills.computeIfAbsent(killerId, uuid -> new HashMap<>());
        final Deque<Instant> timestamps = killerKills.computeIfAbsent(victimId, uuid -> new ArrayDeque<>());
        timestamps.addLast(now);

        pruneOld(timestamps, now);

        if (timestamps.isEmpty()) {
            killerKills.remove(victimId);
            if (killerKills.isEmpty()) {
                recentKills.remove(killerId);
            }
        }
        else if (timestamps.size() >= sameVictimThreshold) {
            alertStaff(killer.getName(), victim.getName(), timestamps.size(), 1);
        }

        final Deque<KillEvent> victimEvents = recentVictimKills.computeIfAbsent(victimId, uuid -> new ArrayDeque<>());
        victimEvents.addLast(new KillEvent(killerId, now));
        pruneOldEvents(victimEvents, now);

        if (victimEvents.isEmpty()) {
            recentVictimKills.remove(victimId);
        }
        else {
            final int totalKills = victimEvents.size();
            final int distinctKillers = countDistinctKillers(victimEvents);

            if (totalKills >= sameVictimThreshold && distinctKillers > 1) {
                alertStaff(killer.getName(), victim.getName(), totalKills, distinctKillers);
            }
        }
    }

    private void pruneOld(Deque<Instant> timestamps, Instant now) {
        final Instant threshold = now.minus(window);
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(threshold)) {
            timestamps.removeFirst();
        }
    }

    private void pruneOldEvents(Deque<KillEvent> events, Instant now) {
        final Instant threshold = now.minus(window);
        while (!events.isEmpty() && events.peekFirst().timestamp().isBefore(threshold)) {
            events.removeFirst();
        }
    }

    private int countDistinctKillers(Deque<KillEvent> events) {
        final Set<UUID> distinct = new HashSet<>();
        for (KillEvent event : events) {
            distinct.add(event.killerId());
        }
        return distinct.size();
    }

    private void alertStaff(String killerName, String victimName, int totalKills, int distinctKillers) {
        recordAlert(killerName, victimName, totalKills, distinctKillers);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (restrictAlertsToAdmins && (adminDetector == null || !adminDetector.isAdmin(online))) {
                continue;
            }
            if (online.hasPermission(notifyPermission)) {
                final Map<String, String> placeholders = new HashMap<>();
                placeholders.put("killer", killerName);
                placeholders.put("victim", victimName);
                placeholders.put("count", Integer.toString(totalKills));
                placeholders.put("total_kills", Integer.toString(totalKills));
                placeholders.put("distinct_killers", Integer.toString(distinctKillers));
                placeholders.put("minutes", Long.toString(window.toMinutes()));
                online.sendMessage(messages.format("smurf-alert", placeholders));
            }
        }
    }

    private void recordAlert(String killerName, String victimName, int totalKills, int distinctKillers) {
        alertHistory.addFirst(new SmurfAlert(killerName, victimName, totalKills, distinctKillers, Instant.now()));
        while (alertHistory.size() > alertHistoryLimit) {
            alertHistory.removeLast();
        }
    }

    private void recordKillHistory(Player killer, Player victim, Instant timestamp) {
        killHistory.addFirst(new KillRecord(
                killer.getUniqueId(),
                killer.getName(),
                victim.getUniqueId(),
                victim.getName(),
                timestamp
        ));
        while (killHistory.size() > killHistoryLimit) {
            killHistory.removeLast();
        }
    }

    public boolean isExempt(UUID uniqueId) {
        return uniqueId != null && exemptPlayers.contains(uniqueId);
    }

    public boolean addExemptPlayer(UUID uniqueId) {
        if (uniqueId == null) {
            return false;
        }
        return exemptPlayers.add(uniqueId);
    }

    public boolean removeExemptPlayer(UUID uniqueId) {
        if (uniqueId == null) {
            return false;
        }
        return exemptPlayers.remove(uniqueId);
    }

    public Set<UUID> getExemptPlayers() {
        return Collections.unmodifiableSet(exemptPlayers);
    }

    public List<SmurfAlert> getAlertHistory() {
        return List.copyOf(alertHistory);
    }

    public List<KillRecord> getKillHistory() {
        return List.copyOf(killHistory);
    }

    public void clear(UUID uniqueId) {
        recentKills.remove(uniqueId);
        recentKills.values().forEach(map -> map.remove(uniqueId));
        recentVictimKills.remove(uniqueId);
        recentVictimKills.values().forEach(events -> events.removeIf(event -> event.killerId().equals(uniqueId)));
    }

    private record KillEvent(UUID killerId, Instant timestamp) {
    }

    public record SmurfAlert(String killerName, String victimName, int totalKills, int distinctKillers,
            Instant timestamp) {
    }

    public record KillRecord(UUID killerId, String killerName, UUID victimId, String victimName, Instant timestamp) {
    }
}
