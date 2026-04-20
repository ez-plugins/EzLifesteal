package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.AdminConfigAdapter;
import com.skyblockexp.ezlifesteal.detector.AdminDetector;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Builds admin detection settings from admin configuration.
 */
public class AdminSettingsFactory {

    public AdminSettings create(AdminConfigAdapter adminConfigAdapter) {
        final boolean adminEnabled = adminConfigAdapter.getBoolean("enabled", true);
        final boolean treatOpsAsAdmin = adminConfigAdapter.getBoolean("treat-ops-as-admin", true);
        final String permissionNode = adminConfigAdapter.getString("permission-node", "lifesteal.admin");

        final Set<UUID> allowedUuids = new HashSet<>();
        final List<String> warnings = new ArrayList<>();
        for (String uuidString : adminConfigAdapter.getStringList("allowed-uuids")) {
            try {
                allowedUuids.add(UUID.fromString(uuidString));
            }
            catch (IllegalArgumentException exception) {
                warnings.add("Invalid admin UUID in configuration: " + uuidString);
            }
        }

        final Set<String> allowedNames = new HashSet<>();
        for (String name : adminConfigAdapter.getStringList("allowed-names")) {
            if (name != null && !name.isBlank()) {
                allowedNames.add(name.toLowerCase(Locale.ROOT));
            }
        }

        final AdminDetector adminDetector = new AdminDetector(
                adminEnabled,
                treatOpsAsAdmin,
                permissionNode,
                allowedUuids,
                allowedNames
        );

        return new AdminSettings(
                adminDetector,
                adminConfigAdapter.getBoolean("bypass-heart-loss", true),
                adminConfigAdapter.getBoolean("bypass-heart-gain", true),
                adminConfigAdapter.getBoolean("restrict-smurf-alerts-to-admins", false),
                warnings
        );
    }

    public record AdminSettings(
            AdminDetector adminDetector,
            boolean adminBypassHeartLoss,
            boolean adminBypassHeartGain,
            boolean restrictSmurfAlertsToAdmins,
            List<String> warnings
    ) {
    }
}
