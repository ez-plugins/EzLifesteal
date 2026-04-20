package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.config.SmurfConfigAdapter;
import com.skyblockexp.ezlifesteal.detector.AdminDetector;
import com.skyblockexp.ezlifesteal.detector.SmurfDetector;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Builds smurf-detection settings and detector instances.
 */
public class SmurfSettingsFactory {

    public SmurfSettings create(
            SmurfConfigAdapter smurfConfigAdapter,
            MessageService messageService,
            AdminDetector adminDetector,
            boolean restrictSmurfAlertsToAdmins
    ) {
        final boolean smurfEnabled = smurfConfigAdapter.getBoolean("enabled", true);
        final int sameVictimThreshold = smurfConfigAdapter.getInt("same-victim-threshold", 3);
        final Duration window = Duration.ofMinutes(smurfConfigAdapter.getLong("time-window-minutes", 15));
        final String notifyPermission = smurfConfigAdapter.getString("notify-permission", "lifesteal.alert");
        final int alertHistoryLimit = smurfConfigAdapter.getInt("history-limit", 50);
        final int killHistoryLimit = smurfConfigAdapter.getInt("kill-history-limit", 120);

        final List<String> warnings = new ArrayList<>();
        final Set<UUID> exemptPlayers = new HashSet<>();
        for (String entry : smurfConfigAdapter.getStringList("exempted-players")) {
            try {
                exemptPlayers.add(UUID.fromString(entry));
            }
            catch (IllegalArgumentException exception) {
                warnings.add("Invalid smurf exemption UUID: " + entry);
            }
        }

        final SmurfDetector smurfDetector = new SmurfDetector(
                messageService,
                smurfEnabled,
                sameVictimThreshold,
                window,
                notifyPermission,
                adminDetector,
                restrictSmurfAlertsToAdmins,
                exemptPlayers,
                alertHistoryLimit,
                killHistoryLimit
        );
        return new SmurfSettings(smurfDetector, warnings);
    }

    public record SmurfSettings(SmurfDetector smurfDetector, List<String> warnings) {
    }
}
