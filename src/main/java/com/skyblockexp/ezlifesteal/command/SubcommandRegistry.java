package com.skyblockexp.ezlifesteal.command;

import com.skyblockexp.ezlifesteal.command.subcommand.Subcommand;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Registry for subcommands and their visibility requirements.
 */
public final class SubcommandRegistry {

    private final Map<String, Subcommand> byName = new LinkedHashMap<>();

    private final Map<String, String> aliasToPrimary = new LinkedHashMap<>();

    private final Map<String, LifestealCommand.SubcommandRequirement> requirements = new LinkedHashMap<>();


    public SubcommandRegistry register(
            String primaryName,
            Subcommand subcommand,
            LifestealCommand.SubcommandRequirement requirement,
            String... aliases
    ) {
        final String normalizedPrimary = normalize(primaryName);
        byName.put(normalizedPrimary, subcommand);
        requirements.put(normalizedPrimary, requirement == null
                ? new LifestealCommand.SubcommandRequirement(false, Collections.emptyList(), Collections.emptyMap())
                : requirement);
        aliasToPrimary.put(normalizedPrimary, normalizedPrimary);
        if (aliases != null) {
            for (String alias : aliases) {
                aliasToPrimary.put(normalize(alias), normalizedPrimary);
            }
        }
        return this;
    }

    public Optional<Subcommand> resolve(String nameOrAlias) {
        final String primary = aliasToPrimary.get(normalize(nameOrAlias));
        return Optional.ofNullable(primary == null ? null : byName.get(primary));
    }

    public List<String> listAllowedSubcommands(CommandSender sender) {
        if (sender == null) {
            return Collections.emptyList();
        }

        final List<String> allowed = new ArrayList<>();
        for (Map.Entry<String, LifestealCommand.SubcommandRequirement> entry : requirements.entrySet()) {
            if (isAllowed(entry.getValue(), sender)) {
                allowed.add(entry.getKey());
            }
        }
        return allowed;
    }

    public Collection<String> getRegisteredSubcommands() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(byName.keySet()));
    }

    private boolean isAllowed(LifestealCommand.SubcommandRequirement requirement, CommandSender sender) {
        if (requirement.isPlayerOnly() && !(sender instanceof Player)) {
            return false;
        }

        final List<String> permissions = requirement.getPermissions();
        if (permissions.isEmpty()) {
            return true;
        }

        for (String permission : permissions) {
            if (sender.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
