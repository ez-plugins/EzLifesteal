package com.skyblockexp.ezlifesteal.placeholder;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class LifestealPlaceholderExpansion extends PlaceholderExpansion implements PlaceholderHook {

    private static final ThreadLocal<DecimalFormat> FORMATTER =
            ThreadLocal.withInitial(() -> new DecimalFormat("0.##"));

    private static final TopCache EMPTY_TOP_CACHE = new TopCache(Collections.emptyList(), 0L);


    private final PluginAccessor plugin;

    private final Duration playerCacheTtl;

    private final Duration topCacheTtl;

    private final Map<UUID, PlayerCache> playerCache = new ConcurrentHashMap<>();

    private final AtomicReference<TopCache> topCache = new AtomicReference<>(EMPTY_TOP_CACHE);

    private final AtomicReference<CompletableFuture<?>> topRefresh = new AtomicReference<>();


    public LifestealPlaceholderExpansion(PluginAccessor plugin) {
        this(plugin, Duration.ofMillis(500), Duration.ofSeconds(5));
    }

    LifestealPlaceholderExpansion(PluginAccessor plugin, Duration playerCacheTtl, Duration topCacheTtl) {
        this.plugin = plugin;
        this.playerCacheTtl = playerCacheTtl;
        this.topCacheTtl = topCacheTtl;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "ezlifesteal";
    }

    @Override
    public String getAuthor() {
        return plugin.getPluginAuthors();
    }

    @Override
    public String getVersion() {
        return plugin.getPluginVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) {
            return "";
        }
        final String raw = params;
        final String normalized = raw.toLowerCase(Locale.ROOT);
        // Support checking if a player is banned:
        // - %ezlifesteal_is_banned% -> checks the provided player
        // - %ezlifesteal_is_banned_<player>% -> checks specified player name or UUID
        if ("is_banned".equals(normalized)) {
            if (player == null) {
                return "";
            }
            final UUID playerId = player.getUniqueId();
            if (playerId == null) {
                return "";
            }
            return plugin.getBanAdapter().isBanned(playerId, player.getName()) ? "true" : "false";
        }
        if (normalized.startsWith("is_banned_")) {
            final String targetRaw = raw.substring("is_banned_".length());
            try {
                final UUID id = UUID.fromString(targetRaw);
                return plugin.getBanAdapter().isBanned(id, null) ? "true" : "false";
            }
            catch (IllegalArgumentException ignored) {
                // target is a name, not a UUID — scan ban entries by name
            }
            final boolean nameBanned = plugin.getBanAdapter().getBanEntries().stream()
                    .anyMatch(entry -> targetRaw.equalsIgnoreCase(entry.getPlayerName()));
            return nameBanned ? "true" : "false";
        }

        return switch (normalized) {
            case "hearts", "current_hearts" -> formatHearts(resolvePlayerHearts(player));
            case "default_hearts" -> formatHearts(getManager().map(LifestealManager::getDefaultHearts));
            case "min_hearts" -> formatHearts(getManager().map(LifestealManager::getMinHearts));
            case "max_hearts" -> formatHearts(getManager().map(LifestealManager::getMaxHearts));
            default -> {
                if (normalized.startsWith("top_")) {
                    yield handleTopPlaceholder(normalized.substring(4));
                }
                yield "";
            }
        };
    }

    @Override
    public void clearCache() {
        playerCache.clear();
        topCache.set(EMPTY_TOP_CACHE);
    }

    @Override
    public boolean register() {
        return super.register();
    }

    @Override
    public boolean unregisterExpansion() {
        clearCache();
        return super.unregister();
    }

    private Optional<LifestealManager> getManager() {
        final LifestealManager manager = plugin.getLifestealManager();
        return Optional.ofNullable(manager);
    }

    private double resolvePlayerHearts(OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) {
            return getManager().map(LifestealManager::getDefaultHearts).orElse(0.0);
        }
        final UUID uniqueId = player.getUniqueId();
        final long now = System.currentTimeMillis();
        final PlayerCache cached = playerCache.compute(uniqueId, (id, existing) -> {
            if (existing != null && now - existing.timestamp <= playerCacheTtl.toMillis()) {
                return existing;
            }
            final double hearts = fetchPlayerHearts(uniqueId);
            return new PlayerCache(hearts, now);
        });
        return cached != null ? cached.hearts : getManager().map(LifestealManager::getDefaultHearts).orElse(0.0);
    }

    private double fetchPlayerHearts(UUID uniqueId) {
        final Optional<LifestealManager> managerOptional = getManager();
        if (managerOptional.isEmpty()) {
            return 0.0;
        }
        final LifestealManager manager = managerOptional.get();
        return manager.getLoadedProfile(uniqueId)
                .map(LifestealProfile::getHearts)
                .orElse(manager.getDefaultHearts());
    }

    private String handleTopPlaceholder(String query) {
        final String[] parts = query.split("_", 2);
        if (parts.length != 2) {
            return "";
        }
        final int position;
        try {
            position = Integer.parseInt(parts[0]);
        }
        catch (NumberFormatException exception) {
            return "";
        }
        if (position <= 0) {
            return "";
        }
        final TopCache cache = requestTopCache(position);
        if (cache.profiles().size() < position) {
            return "";
        }
        final LifestealProfile profile = cache.profiles().get(position - 1);
        return switch (parts[1]) {
            case "hearts", "current_hearts" -> formatHearts(profile.getHearts());
            case "player", "name" -> Optional.ofNullable(Bukkit.getOfflinePlayer(profile.getUniqueId()).getName())
                    .orElse("Unknown");
            case "uuid" -> profile.getUniqueId().toString();
            default -> "";
        };
    }

    private TopCache requestTopCache(int requiredPosition) {
        final LifestealManager manager = plugin.getLifestealManager();
        if (manager == null) {
            return EMPTY_TOP_CACHE;
        }
        final long now = System.currentTimeMillis();
        final TopCache current = topCache.get();
        final boolean expired = now - current.timestamp() > topCacheTtl.toMillis();
        final boolean insufficient = current.profiles().size() < requiredPosition;
        if ((expired || insufficient) && shouldRefreshTopCache()) {
            final int limit = Math.max(requiredPosition, 10);
            final CompletableFuture<List<LifestealProfile>> future = manager.loadTopProfilesAsync(limit);
            future.thenAccept(list -> topCache.set(new TopCache(List.copyOf(list), System.currentTimeMillis())))
                    .exceptionally(throwable -> {
                        plugin.getLogger()
                                .warning("Failed to refresh lifesteal leaderboard cache: " + throwable.getMessage());
                        return null;
                    });
            topRefresh.set(future);
        }
        return topCache.get();
    }

    private boolean shouldRefreshTopCache() {
        final CompletableFuture<?> inFlight = topRefresh.get();
        return inFlight == null || inFlight.isDone();
    }

    private String formatHearts(double value) {
        return FORMATTER.get().format(value);
    }

    private String formatHearts(Optional<Double> value) {
        return value.map(this::formatHearts).orElse("");
    }

    private record PlayerCache(double hearts, long timestamp) {
    }

    private record TopCache(List<LifestealProfile> profiles, long timestamp) {
    }
}
