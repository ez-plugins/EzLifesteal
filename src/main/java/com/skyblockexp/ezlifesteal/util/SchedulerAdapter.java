package com.skyblockexp.ezlifesteal.util;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class SchedulerAdapter {

    private static volatile Boolean FOLIA = null;

    private static final boolean HAS_ASYNC_SCHEDULER;

    static {
        boolean hasAsync;
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
            hasAsync = true;
        } catch (ClassNotFoundException ignored) {
            hasAsync = false;
        }
        HAS_ASYNC_SCHEDULER = hasAsync;
    }

    private SchedulerAdapter() {
    }

    public static void runAsync(Plugin plugin, Runnable runnable) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(runnable, "runnable");
        if (!HAS_ASYNC_SCHEDULER) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            return;
        }

        try {
            final Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
            final Method runNow = asyncScheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
            final Consumer<Object> task = ignored -> runnable.run();
            runNow.invoke(asyncScheduler, plugin, task);
        }
        catch (ReflectiveOperationException | RuntimeException ignored) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public static void run(Plugin plugin, Runnable runnable) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(runnable, "runnable");
        if (FOLIA == null) {
            FOLIA = detectFolia();
        }
        if (FOLIA) {
            try {
                final Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                final Method execute = scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
                execute.invoke(scheduler, plugin, runnable);
                return;
            }
            catch (ReflectiveOperationException | RuntimeException ignored) {
                // Fall back to Bukkit scheduler when Folia scheduler APIs are unavailable.
            }
        }

        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static TaskHandle runLater(Plugin plugin, Runnable runnable, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(runnable, "runnable");
        if (FOLIA == null) {
            FOLIA = detectFolia();
        }
        if (FOLIA) {
            try {
                final Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                final Method runDelayed = scheduler.getClass()
                        .getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                final Consumer<Object> taskConsumer = ignored -> runnable.run();
                final Object task = runDelayed.invoke(scheduler, plugin, taskConsumer, delayTicks);
                return new ReflectiveTaskHandle(task);
            }
            catch (ReflectiveOperationException | RuntimeException ignored) {
                // Fall back to Bukkit scheduler when Folia scheduler APIs are unavailable.
            }
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
            try {
                final Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                final Method runAtFixedRate = scheduler.getClass()
                        .getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
                final Consumer<Object> taskConsumer = ignored -> runnable.run();
                final Object task = runAtFixedRate.invoke(scheduler, plugin, taskConsumer, delayTicks, periodTicks);
                return new ReflectiveTaskHandle(task);
            }
            catch (ReflectiveOperationException | RuntimeException ignored) {
                // Fall back to Bukkit scheduler when Folia scheduler APIs are unavailable.
            }
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

    private static final class ReflectiveTaskHandle implements TaskHandle {

        private final Object delegate;

        private final AtomicBoolean cancelled = new AtomicBoolean(false);


        private ReflectiveTaskHandle(Object delegate) {
            this.delegate = delegate;
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                try {
                    delegate.getClass().getMethod("cancel").invoke(delegate);
                }
                catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
        }

        @Override
        public boolean isCancelled() {
            if (cancelled.get()) {
                return true;
            }
            try {
                final Object value = delegate.getClass().getMethod("isCancelled").invoke(delegate);
                if (value instanceof Boolean cancelledValue) {
                    return cancelledValue;
                }
            }
            catch (ReflectiveOperationException | RuntimeException ignored) {
            }
            return false;
        }
    }
}