package com.skyblockexp.ezlifesteal.overlay;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.util.ActionBarHelper;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter.TaskHandle;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class HeartOverlayManager {

    private final EzLifestealPlugin plugin;

    private final Map<UUID, Double> trackedHearts = new ConcurrentHashMap<>();

    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();


    private boolean enabled;

    private DisplayMode mode = DisplayMode.ACTION_BAR;

    private long updateIntervalTicks = 40L;

    private String messageTemplate = "&c❤ %hearts% &7hearts";

    private Set<String> enabledWorlds = Collections.emptySet();

    private Set<String> disabledWorlds = Collections.emptySet();

    private BarColor bossBarColor = BarColor.RED;

    private BarStyle bossBarStyle = BarStyle.SOLID;

    private double maxHearts = 40.0D;

    private TaskHandle task;


    public HeartOverlayManager(EzLifestealPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void reload(ConfigurationSection section, double maxHearts) {
        shutdown();
        this.maxHearts = maxHearts <= 0 ? 20.0D : maxHearts;
        if (section == null) {
            this.enabled = false;
            return;
        }
        this.enabled = section.getBoolean("enabled", false);
        final String modeName = section.getString("mode", "ACTION_BAR");
        this.mode = DisplayMode.fromString(modeName);
        final long configuredInterval = section.getLong("update-interval-ticks", 40L);
        this.updateIntervalTicks = Math.max(1L, configuredInterval);
        final String template = section.getString("message", "&c❤ %hearts% &7hearts");
        this.messageTemplate = template == null ? "&c❤ %hearts% &7hearts" : template;
        this.enabledWorlds = parseWorldList(section.getStringList("enabled-worlds"));
        this.disabledWorlds = parseWorldList(section.getStringList("disabled-worlds"));

        final ConfigurationSection bossBarSection = section.getConfigurationSection("boss-bar");
        if (bossBarSection != null) {
            this.bossBarColor = parseBossBarColor(bossBarSection.getString("color"));
            this.bossBarStyle = parseBossBarStyle(bossBarSection.getString("overlay"));
        }
        else {
            this.bossBarColor = BarColor.RED;
            this.bossBarStyle = BarStyle.SOLID;
        }

        if (enabled) {
            ensureTask();
        }
    }

    public synchronized void shutdown() {
        trackedHearts.clear();
        cancelTask();
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();
    }

    public void sendHeartStatus(Player player, double hearts) {
        if (!enabled) {
            clear(player.getUniqueId());
            return;
        }
        trackedHearts.put(player.getUniqueId(), hearts);
        displayOnce(player, hearts);
        ensureTask();
    }

    public void clear(UUID uniqueId) {
        trackedHearts.remove(uniqueId);
        final BossBar bossBar = bossBars.remove(uniqueId);
        if (bossBar != null) {
            bossBar.removeAll();
        }
        if (trackedHearts.isEmpty()) {
            cancelTask();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void tick() {
        if (!enabled) {
            shutdown();
            return;
        }
        for (Map.Entry<UUID, Double> entry : trackedHearts.entrySet()) {
            final UUID uniqueId = entry.getKey();
            final Player player = Bukkit.getPlayer(uniqueId);
            if (player == null || !player.isOnline()) {
                clear(uniqueId);
                continue;
            }
            if (!isWorldAllowed(player.getWorld().getName())) {
                hideBossBar(uniqueId, player);
                continue;
            }
            displayOverlay(player, entry.getValue());
        }
    }

    private void displayOnce(Player player, double hearts) {
        if (!isWorldAllowed(player.getWorld().getName())) {
            hideBossBar(player.getUniqueId(), player);
            return;
        }
        displayOverlay(player, hearts);
    }

    private void displayOverlay(Player player, double hearts) {
        final String message = formatMessage(player, hearts);
        switch (mode) {
            case ACTION_BAR -> ActionBarHelper.sendActionBar(player, message);
            case BOSS_BAR -> updateBossBar(player, message, hearts);
        }
    }

    private String formatMessage(Player player, double hearts) {
        final String formatted = messageTemplate
                .replace("%player%", player.getName())
                .replace("%world%", player.getWorld().getName())
                .replace("%hearts%", formatHearts(hearts))
                .replace("%max_hearts%", formatHearts(maxHearts));
        return colorize(formatted);
    }

    private String formatHearts(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        return value % 1 == 0 ? Integer.toString((int) value) : String.format(Locale.US, "%.1f", value);
    }

    private void updateBossBar(Player player, String message, double hearts) {
        final UUID uniqueId = player.getUniqueId();
        final BossBar bossBar = bossBars.computeIfAbsent(uniqueId,
                id -> Bukkit.createBossBar(message, bossBarColor, bossBarStyle));
        bossBar.setTitle(message);
        final double denominator = maxHearts <= 0 ? 20.0D : maxHearts;
        final double progress = Math.max(0.0D, Math.min(1.0D, hearts / denominator));
        bossBar.setProgress(progress);
        bossBar.setColor(bossBarColor);
        bossBar.setStyle(bossBarStyle);
        if (!bossBar.getPlayers().contains(player)) {
            bossBar.addPlayer(player);
        }
    }

    private void hideBossBar(UUID uniqueId, Player player) {
        final BossBar bossBar = bossBars.get(uniqueId);
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    private void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void ensureTask() {
        if (!enabled || trackedHearts.isEmpty()) {
            return;
        }
        if (task != null && !task.isCancelled()) {
            return;
        }
        task = SchedulerAdapter.runTimer(plugin, this::tick, updateIntervalTicks, updateIntervalTicks);
    }

    private Set<String> parseWorldList(Iterable<String> worlds) {
        if (worlds == null) {
            return Collections.emptySet();
        }
        final Set<String> parsed = new HashSet<>();
        for (String world : worlds) {
            if (world == null) {
                continue;
            }
            final String trimmed = world.trim();
            if (!trimmed.isEmpty()) {
                parsed.add(trimmed.toLowerCase(Locale.ROOT));
            }
        }
        return parsed.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(parsed);
    }

    private boolean isWorldAllowed(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return enabledWorlds.isEmpty();
        }
        final String normalized = worldName.toLowerCase(Locale.ROOT);
        if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(normalized)) {
            return false;
        }
        return !disabledWorlds.contains(normalized);
    }

    private BarColor parseBossBarColor(String colorName) {
        if (colorName == null || colorName.isBlank()) {
            return BarColor.RED;
        }
        try {
            return BarColor.valueOf(colorName.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Unknown boss bar color '" + colorName + "'. Defaulting to RED.");
            return BarColor.RED;
        }
    }

    private BarStyle parseBossBarStyle(String overlayName) {
        if (overlayName == null || overlayName.isBlank()) {
            return BarStyle.SOLID;
        }
        final String normalized = overlayName.trim().toUpperCase(Locale.ROOT);
        try {
            return BarStyle.valueOf(normalized);
        }
        catch (IllegalArgumentException ignored) {
            return mapLegacyOverlay(normalized);
        }
    }

    private BarStyle mapLegacyOverlay(String overlayName) {
        return switch (overlayName) {
            case "PROGRESS" -> BarStyle.SOLID;
            case "NOTCHED_6" -> BarStyle.SEGMENTED_6;
            case "NOTCHED_10" -> BarStyle.SEGMENTED_10;
            case "NOTCHED_12" -> BarStyle.SEGMENTED_12;
            case "NOTCHED_20" -> BarStyle.SEGMENTED_20;
            default -> {
                plugin.getLogger().warning("Unknown boss bar overlay '" + overlayName + "'. Defaulting to SOLID.");
                yield BarStyle.SOLID;
            }
        };
    }

    private String colorize(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private enum DisplayMode {
        ACTION_BAR,
        BOSS_BAR;

        static DisplayMode fromString(String value) {
            if (value == null) {
                return ACTION_BAR;
            }
            try {
                return DisplayMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException exception) {
                return ACTION_BAR;
            }
        }
    }
}
