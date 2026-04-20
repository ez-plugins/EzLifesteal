package com.skyblockexp.ezlifesteal.util;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PlayerLookupService {

    private final EzLifestealPlugin plugin;

    public PlayerLookupService(EzLifestealPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Optional<UUID>> lookupUniqueId(String identifier) {
        if (identifier == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        final String trimmed = identifier.trim();
        if (trimmed.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        try {
            final UUID uniqueId = UUID.fromString(trimmed);
            return CompletableFuture.completedFuture(Optional.of(uniqueId));
        }
        catch (IllegalArgumentException ignored) {
            // Not a UUID, continue.
        }

        Player onlinePlayer = null;
        try {
            onlinePlayer = Bukkit.getPlayerExact(trimmed);
        }
        catch (RuntimeException ignored) {
            // Fall through to other lookup paths.
        }
        if (onlinePlayer != null) {
            return CompletableFuture.completedFuture(Optional.of(onlinePlayer.getUniqueId()));
        }

        OfflinePlayer cachedOffline = null;
        try {
            cachedOffline = Bukkit.getOfflinePlayerIfCached(trimmed);
        }
        catch (RuntimeException ignored) {
            // Fall through to async lookup.
        }
        if (cachedOffline != null && cachedOffline.getUniqueId() != null) {
            return CompletableFuture.completedFuture(Optional.of(cachedOffline.getUniqueId()));
        }

        final CompletableFuture<Optional<UUID>> future = new CompletableFuture<>();
        final Object profile;
        try {
            profile = Bukkit.class.getMethod("createProfile", String.class).invoke(null, trimmed);
        }
        catch (Throwable throwable) {
            final Logger logger = plugin.getLogger();
            logger.warning("Failed to create lookup profile for '" + trimmed + "': " + throwable.getMessage());
            future.complete(Optional.empty());
            return future;
        }

        final Object finalProfile = profile;
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try {
                final UUID resolved = resolveProfileId(finalProfile);
                future.complete(Optional.ofNullable(resolved));
            }
            catch (Throwable throwable) {
                final Logger logger = plugin.getLogger();
                logger.warning("Failed to resolve player identifier '" + trimmed + "': " + throwable.getMessage());
                future.complete(Optional.empty());
            }
        });

        return future;
    }

    private UUID resolveProfileId(Object profile) throws Exception {
        final Class<?> profileClass = profile.getClass();
        try {
            profileClass.getMethod("complete", boolean.class).invoke(profile, true);
        }
        catch (NoSuchMethodException ignored) {
            profileClass.getMethod("complete").invoke(profile);
        }

        return (UUID) profileClass.getMethod("getId").invoke(profile);
    }
}
