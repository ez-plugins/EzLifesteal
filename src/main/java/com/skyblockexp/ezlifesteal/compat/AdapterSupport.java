package com.skyblockexp.ezlifesteal.compat;

import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Common compatibility support for runtime adapters and scheduler-sensitive player/world operations.
 *
 * <p>These helpers intentionally execute inline when already on a safe thread to preserve immediate side effects
 * expected by command and service flows, while still delegating to the platform scheduler when needed.</p>
 */
public final class AdapterSupport {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)");

    private AdapterSupport() {
    }

    public static boolean isFoliaServer() {
        return SchedulerAdapter.detectFolia();
    }

    public static String resolveRuntimeAdapterId() {
        final String apiLine = resolveApiLine();
        if (isSpigotServer()) {
            return "spigot." + apiLine;
        }
        if (isFoliaServer()) {
            return "folia." + apiLine;
        }
        return "paper." + apiLine;
    }

    private static boolean isSpigotServer() {
        try {
            final String serverName = Bukkit.getServer().getName();
            return "Spigot".equalsIgnoreCase(serverName);
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    public static void runOnMain(Plugin plugin, Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (plugin == null) {
            runnable.run();
            return;
        }
        if (isPrimaryThreadSafe()) {
            runnable.run();
        } else {
            SchedulerAdapter.run(plugin, runnable);
        }
    }

    public static void runAsync(Plugin plugin, Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (plugin == null) {
            runnable.run();
            return;
        }
        SchedulerAdapter.runAsync(plugin, runnable);
    }

    public static void runForPlayer(Plugin plugin, Player player, Runnable runnable) {
        if (player == null || runnable == null) {
            return;
        }
        if (!isPlayerUsable(player)) {
            return;
        }
        if (plugin == null) {
            runPlayerAction(player, runnable);
            return;
        }
        if (isPrimaryThreadSafe()) {
            runPlayerAction(player, runnable);
            return;
        }
        if (!isFoliaServer()) {
            SchedulerAdapter.run(plugin, () -> runPlayerAction(player, runnable));
            return;
        }

        try {
            final Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
            final Method runMethod = scheduler.getClass().getMethod("run", Plugin.class, Consumer.class,
                    Runnable.class);
            final Consumer<Object> action = task -> runPlayerAction(player, runnable);
            runMethod.invoke(scheduler, plugin, action, null);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            if (isPrimaryThreadSafe()) {
                runPlayerAction(player, runnable);
            } else {
                SchedulerAdapter.run(plugin, () -> runPlayerAction(player, runnable));
            }
        }
    }

    public static void runAtLocation(Plugin plugin, Location location, Runnable runnable) {
        if (location == null || location.getWorld() == null || runnable == null) {
            return;
        }
        if (plugin == null) {
            runnable.run();
            return;
        }
        if (isPrimaryThreadSafe()) {
            runnable.run();
            return;
        }
        if (!isFoliaServer()) {
            SchedulerAdapter.run(plugin, runnable);
            return;
        }

        try {
            final Object regionScheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
            final Method runMethod = regionScheduler.getClass().getMethod("run", Plugin.class, Location.class,
                    Consumer.class);
            final Consumer<Object> action = task -> runnable.run();
            runMethod.invoke(regionScheduler, plugin, location, action);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            if (isPrimaryThreadSafe()) {
                runnable.run();
            } else {
                SchedulerAdapter.run(plugin, runnable);
            }
        }
    }

    public static void dropItemLeftoversAtPlayer(Plugin plugin, Player player, Map<Integer, ItemStack> leftovers) {
        if (leftovers == null || leftovers.isEmpty()) {
            return;
        }
        runForPlayer(plugin, player, () -> {
            if (player.getWorld() == null) {
                return;
            }
            for (ItemStack itemStack : leftovers.values()) {
                if (itemStack == null) {
                    continue;
                }
                player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
            }
        });
    }

    public static void dropItemAtPlayer(Plugin plugin, Player player, ItemStack item) {
        if (item == null) {
            return;
        }
        runForPlayer(plugin, player, () -> {
            if (player.getWorld() != null) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        });
    }

    public static boolean isPlayerUsable(Player player) {
        return player != null;
    }

    private static String resolveApiLine() {
        final String bukkitVersion = Bukkit.getBukkitVersion();
        final Matcher matcher = VERSION_PATTERN.matcher(bukkitVersion == null ? "" : bukkitVersion);
        if (!matcher.find()) {
            return "unknown";
        }

        final int major = parseIntSafe(matcher.group(1));
        final int minor = parseIntSafe(matcher.group(2));

        // New server version lines report as 1.26.x; normalize to 26.x family.
        if (major == 1 && minor >= 26) {
            return "26.2.x";
        }
        if (major == 1 && minor <= 21) {
            return "1.21.x";
        }
        if (major == 26) {
            if (minor <= 1) {
                return "26.1.x";
            }
            return "26.2.x";
        }
        if (major > 26) {
            return "26.2.x";
        }
        return String.format(Locale.ROOT, "%d.%d.x", major, minor);
    }

    private static int parseIntSafe(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean isPrimaryThreadSafe() {
        try {
            return Bukkit.isPrimaryThread();
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static void runPlayerAction(Player player, Runnable action) {
        if (isPlayerUsable(player)) {
            action.run();
        }
    }
}
