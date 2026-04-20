package com.skyblockexp.ezlifesteal.detector;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class AdminDetector {

    private final boolean enabled;

    private final boolean treatOpsAsAdmin;

    private final String permissionNode;

    private final Set<UUID> allowedUuids;

    private final Set<String> allowedNames;


    public AdminDetector(boolean enabled,
                         boolean treatOpsAsAdmin,
                         String permissionNode,
                         Set<UUID> allowedUuids,
                         Set<String> allowedNames) {
        this.enabled = enabled;
        this.treatOpsAsAdmin = treatOpsAsAdmin;
        this.permissionNode = permissionNode == null ? "" : permissionNode.trim();
        this.allowedUuids = allowedUuids;
        this.allowedNames = allowedNames;
    }

    public boolean isAdmin(OfflinePlayer player) {
        if (!enabled || player == null) {
            return false;
        }

        final UUID uniqueId = player.getUniqueId();
        if (uniqueId != null && allowedUuids.contains(uniqueId)) {
            return true;
        }

        final String name = player.getName();
        if (name != null && allowedNames.contains(name.toLowerCase(Locale.ROOT))) {
            return true;
        }

        if (treatOpsAsAdmin && player.isOp()) {
            return true;
        }

        if (!permissionNode.isEmpty()) {
            final Player online = player.getPlayer();
            if (online != null && online.hasPermission(permissionNode)) {
                return true;
            }
        }

        return false;
    }
}
