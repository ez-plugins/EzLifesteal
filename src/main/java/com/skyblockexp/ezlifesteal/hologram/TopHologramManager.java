package com.skyblockexp.ezlifesteal.hologram;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.compat.AdapterSupport;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter.TaskHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

/**
 * Manages a hologram leaderboard that shows the top lifesteal players.
 */
public class TopHologramManager {

    private static final double LINE_SPACING = 0.30D;

    private final EzLifestealPlugin plugin;

    private final NamespacedKey hologramLineKey;

    private final List<ArmorStand> armorStands = new ArrayList<>();

    private final AtomicBoolean updateScheduled = new AtomicBoolean(false);

    private final AtomicBoolean updateQueued = new AtomicBoolean(false);


    private Location baseLocation;

    private long updateIntervalTicks = 600L;

    private int maxEntries = 10;

    private TaskHandle updateTask;


    public TopHologramManager(EzLifestealPlugin plugin) {
        this(plugin, new NamespacedKey(plugin, "hologram_line"));
    }

    /**
     * Package-private constructor used in unit tests to supply a pre-built (or mocked)
     * {@link NamespacedKey} without requiring a live Bukkit environment.
     */
    TopHologramManager(EzLifestealPlugin plugin, NamespacedKey hologramLineKey) {
        this.plugin = plugin;
        this.hologramLineKey = hologramLineKey;
    }

    public synchronized void reload(ConfigurationSection section) {
        shutdownInternal();
        if (section == null) {
            return;
        }
        final long configuredInterval = section.getLong("update-interval-ticks", 600L);
        this.updateIntervalTicks = Math.max(20L, configuredInterval);
        this.maxEntries = Math.max(1, section.getInt("max-entries", 10));
        final Location configuredLocation = deserializeLocation(section.getConfigurationSection("location"));
        if (configuredLocation != null) {
            activate(configuredLocation);
        }
    }

    public synchronized boolean place(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        final Location clone = cloneBaseLocation(location);
        final ConfigurationSection section = ensureConfigSection();
        serializeLocation(section, clone);
        plugin.saveHologramSettings();
        activate(clone);
        return true;
    }

    public synchronized boolean remove() {
        if (baseLocation == null) {
            return false;
        }
        final ConfigurationSection section = plugin.getHologramSection(false);
        if (section != null) {
            section.set("location", null);
            plugin.saveHologramSettings();
        }
        shutdownInternal();
        return true;
    }

    public synchronized boolean hasHologram() {
        return baseLocation != null;
    }

    public synchronized Location getLocation() {
        return baseLocation == null ? null : baseLocation.clone();
    }

    public void requestUpdate() {
        if (!hasHologram()) {
            return;
        }
        if (updateScheduled.compareAndSet(false, true)) {
            updateLeaderboard();
        }
        else {
            updateQueued.set(true);
        }
    }

    public synchronized void shutdown() {
        shutdownInternal();
    }

    private void activate(Location location) {
        shutdownInternal();
        this.baseLocation = location;
        ensureArmorStandCount(0); // clear any lingering stands just in case
        requestUpdate();
        scheduleTask();
    }

    private void scheduleTask() {
        cancelTask();
        if (!hasHologram()) {
            return;
        }
        updateTask = SchedulerAdapter.runTimer(plugin, this::requestUpdate, updateIntervalTicks, updateIntervalTicks);
    }

    private void cancelTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void shutdownInternal() {
        cancelTask();
        updateScheduled.set(false);
        updateQueued.set(false);
        if (Bukkit.isPrimaryThread()) {
            removeArmorStands();
        }
        else {
            AdapterSupport.runOnMain(plugin, this::removeArmorStands);
        }
        baseLocation = null;
    }

    private void removeArmorStands() {
        for (ArmorStand stand : armorStands) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        armorStands.clear();
    }

    private void updateLeaderboard() {
        final LifestealManager manager = plugin.getLifestealManager();
        final CompletableFuture<List<LifestealProfile>> future;
        if (manager == null) {
            future = CompletableFuture.completedFuture(Collections.emptyList());
        }
        else {
            future = manager.loadTopProfilesAsync(maxEntries);
        }
        future.whenComplete((profiles, throwable) -> {
            updateScheduled.set(false);
            if (updateQueued.compareAndSet(true, false)) {
                requestUpdate();
            }
            if (!hasHologram()) {
                return;
            }
            if (throwable != null) {
                plugin.getLogger().severe("Failed to update hologram leaderboard: " + throwable.getMessage());
                applyLines(Collections.singletonList(colorize("&cLeaderboard unavailable")));
                return;
            }
            applyLines(buildLines(profiles));
        });
    }

    private List<String> buildLines(List<LifestealProfile> profiles) {
        final List<String> lines = new ArrayList<>();
        lines.add(renderMessage("hologram-header", Collections.emptyMap(), "&cTop Lifesteal Players"));
        if (profiles == null || profiles.isEmpty()) {
            lines.add(renderMessage("hologram-empty", Collections.emptyMap(), "&7No leaderboard data available."));
            return lines;
        }
        final int limit = Math.min(maxEntries, profiles.size());
        for (int index = 0; index < limit; index++) {
            final LifestealProfile profile = profiles.get(index);
            final UUID uniqueId = profile.getUniqueId();
            final String name = fetchPlayerName(uniqueId);
            final Map<String, String> placeholders = Map.of(
                    "rank", Integer.toString(index + 1),
                    "player", name,
                    "hearts", format(profile.getHearts())
            );
            lines.add(renderMessage("hologram-entry", placeholders, "&c#%rank% &f%player% &7- &c%hearts% &7hearts"));
        }
        return lines;
    }

    private void applyLines(List<String> lines) {
        if (lines == null) {
            return;
        }
        final Location currentBaseLocation = baseLocation;
        if (currentBaseLocation == null) {
            return;
        }
        AdapterSupport.runAtLocation(plugin, currentBaseLocation, () -> {
            if (!hasHologram()) {
                return;
            }
            ensureArmorStandCount(lines.size());
            for (int i = 0; i < lines.size(); i++) {
                final ArmorStand stand = armorStands.get(i);
                final String line = lines.get(i);
                stand.setCustomName(line);
                stand.setCustomNameVisible(true);
            }
        });
    }

    private void ensureArmorStandCount(int required) {
        if (!Bukkit.isPrimaryThread()) {
            final Location currentBaseLocation = baseLocation;
            if (currentBaseLocation == null) {
                return;
            }
            AdapterSupport.runAtLocation(plugin, currentBaseLocation, () -> ensureArmorStandCount(required));
            return;
        }
        if (baseLocation == null) {
            return;
        }
        while (armorStands.size() > required) {
            final ArmorStand stand = armorStands.remove(armorStands.size() - 1);
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        while (armorStands.size() < required) {
            final int index = armorStands.size();
            final Location spawnLocation = baseLocation.clone().subtract(0.0D, index * LINE_SPACING, 0.0D);
            spawnLocation.setPitch(0.0F);
            final ArmorStand stand = spawnArmorStand(spawnLocation);
            armorStands.add(stand);
        }
    }

    private ArmorStand spawnArmorStand(Location location) {
        final World world = location.getWorld();
        if (world == null) {
            throw new IllegalStateException("Unable to spawn hologram in null world");
        }
        final ArmorStand stand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setMarker(true);
        stand.setInvisible(true);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setPersistent(true);
        stand.setSilent(true);
        stand.setCanPickupItems(false);
        stand.setRemoveWhenFarAway(false);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setCustomNameVisible(true);
        stand.setCustomName(colorize(" "));
        final org.bukkit.persistence.PersistentDataContainer pdc = stand.getPersistentDataContainer();
        if (pdc != null && hologramLineKey != null) {
            pdc.set(hologramLineKey, PersistentDataType.BYTE, (byte) 1);
        }
        return stand;
    }

    /**
     * Scans for armor stands near {@code center} within {@code radius} blocks that look like
     * EzLifesteal hologram lines but are not currently tracked by this manager (orphans).
     * Orphan detection uses a PersistentDataContainer tag first, then falls back to a
     * heuristic for pre-existing untagged stands.
     *
     * <p>Must be called on the main thread.
     *
     * @param center the scan origin
     * @param radius the search half-width in blocks (clamped to [1, 64])
     * @return the number of orphaned armor stands removed
     */
    public synchronized int removeNearbyOrphans(Location center, double radius) {
        if (center == null) {
            return 0;
        }
        final World world = center.getWorld();
        if (world == null) {
            return 0;
        }
        final double clampedRadius = Math.max(1.0, Math.min(64.0, radius));
        final Collection<Entity> nearby = world.getNearbyEntities(center, clampedRadius, clampedRadius, clampedRadius);
        int removed = 0;
        for (Entity entity : nearby) {
            if (!(entity instanceof ArmorStand stand)) {
                continue;
            }
            if (isOwnedByThisManager(stand)) {
                continue;
            }
            if (isHologramStand(stand)) {
                stand.remove();
                removed++;
            }
        }
        return removed;
    }

    private boolean isOwnedByThisManager(ArmorStand stand) {
        return armorStands.contains(stand);
    }

    private boolean isHologramStand(ArmorStand stand) {
        if (hologramLineKey != null && stand.getPersistentDataContainer().has(hologramLineKey, PersistentDataType.BYTE)) {
            return true;
        }
        return stand.isMarker()
                && stand.isInvisible()
                && !stand.hasGravity()
                && stand.isSmall()
                && stand.isCustomNameVisible()
                && !stand.hasBasePlate()
                && !stand.hasArms();
    }

    private String renderMessage(String key, Map<String, String> placeholders, String fallback) {
        final MessageService service = plugin.getMessageService();
        final String rendered = service != null ? service.render(key, placeholders) : "";
        if (rendered == null || rendered.isBlank()) {
            return colorize(applyPlaceholders(fallback, placeholders));
        }
        return rendered;
    }

    private String applyPlaceholders(String template, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty() || template == null) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    private String fetchPlayerName(UUID uniqueId) {
        if (uniqueId == null) {
            return "Unknown";
        }
        final Player online = Bukkit.getPlayer(uniqueId);
        if (online != null) {
            return online.getName();
        }
        return Objects.requireNonNullElse(Bukkit.getOfflinePlayer(uniqueId).getName(), uniqueId.toString());
    }

    private String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        return value % 1 == 0 ? Integer.toString((int) value) : String.format(Locale.US, "%.1f", value);
    }

    private ConfigurationSection ensureConfigSection() {
        return plugin.getHologramSection(true);
    }

    private void serializeLocation(ConfigurationSection section, Location location) {
        if (section == null || location == null || location.getWorld() == null) {
            return;
        }
        ConfigurationSection locationSection = section.getConfigurationSection("location");
        if (locationSection == null) {
            locationSection = section.createSection("location");
        }
        locationSection.set("world", location.getWorld().getName());
        locationSection.set("x", location.getX());
        locationSection.set("y", location.getY());
        locationSection.set("z", location.getZ());
        locationSection.set("yaw", location.getYaw());
        locationSection.set("pitch", location.getPitch());
    }

    private Location deserializeLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        final String worldName = section.getString("world");
        if (worldName == null || worldName.isBlank()) {
            return null;
        }
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Hologram world '" + worldName + "' is not loaded; skipping hologram spawn.");
            return null;
        }
        final double x = section.getDouble("x");
        final double y = section.getDouble("y");
        final double z = section.getDouble("z");
        final float yaw = (float) section.getDouble("yaw", 0.0);
        final float pitch = (float) section.getDouble("pitch", 0.0);
        final Location location = new Location(world, x, y, z, yaw, pitch);
        location.setPitch(0.0F);
        return location;
    }

    private Location cloneBaseLocation(Location source) {
        final Location clone = source.clone();
        clone.setYaw(source.getYaw());
        clone.setPitch(0.0F);
        return clone;
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}
