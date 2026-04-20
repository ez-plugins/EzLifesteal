package com.skyblockexp.ezlifesteal.util;

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Utility that abstracts scheduling tasks between Paper and Folia.
 */
public final class SchedulerAdapter {

    private static volatile Boolean FOLIA = null;

    private SchedulerAdapter() {
    }

    public static void run(Plugin plugin, Runnable runnable) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(runnable, "runnable");
        if (FOLIA == null) {
            FOLIA = detectFolia();
        }
        if (FOLIA) {
            final GlobalRegionScheduler scheduler = Bukkit.getGlobalRegionScheduler();
            scheduler.execute(plugin, runnable);
        }
        else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static TaskHandle runLater(Plugin plugin, Runnable runnable, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(runnable, "runnable");
        if (FOLIA == null) {
            FOLIA = detectFolia();
        }
        if (FOLIA) {
            final GlobalRegionScheduler scheduler = Bukkit.getGlobalRegionScheduler();
            final ScheduledTask task = scheduler.runDelayed(plugin, scheduledTask -> runnable.run(), delayTicks);
            return new FoliaTaskHandle(task);
        }
        final BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        return new BukkitTaskHandle(task);
    }

    public static TaskHandle runTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(runnable, "runnable");
        if (FOLIA == null) {
            FOLIA = detectFolia();
        }
        if (FOLIA) {
            final GlobalRegionScheduler scheduler = Bukkit.getGlobalRegionScheduler();
            final ScheduledTask task = scheduler.runAtFixedRate(plugin, scheduledTask -> runnable.run(), delayTicks,
                    periodTicks);
            return new FoliaTaskHandle(task);
        }
        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        return new BukkitTaskHandle(task);
    }

    public interface TaskHandle {
        void cancel();

        boolean isCancelled();
    }

    public static boolean detectFolia() {
        final String serverName = Bukkit.getServer().getName();
        return "Folia".equalsIgnoreCase(serverName);
    }

    private static final class BukkitTaskHandle implements TaskHandle {

        private final BukkitTask delegate;

        private BukkitTaskHandle(BukkitTask delegate) {
            this.delegate = delegate;
        }

        @Override
        public void cancel() {
            delegate.cancel();
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }
    }

    private static final class FoliaTaskHandle implements TaskHandle {

        private final ScheduledTask delegate;

        private final AtomicBoolean cancelled = new AtomicBoolean(false);


        private FoliaTaskHandle(ScheduledTask delegate) {
            this.delegate = delegate;
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                delegate.cancel();
            }
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get() || delegate.isCancelled();
        }
    }
}
