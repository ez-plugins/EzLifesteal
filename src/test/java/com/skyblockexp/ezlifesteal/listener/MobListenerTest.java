package com.skyblockexp.ezlifesteal.listener;

import com.skyblockexp.ezlifesteal.detector.AdminDetector;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.model.MobReward;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MobListenerTest {

    @Test
    void returnsEarlyForUnsupportedEntityWorldAndKillerCases() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);

        MobListener listener = new MobListener(plugin);
        EntityDeathEvent event = mock(EntityDeathEvent.class);
        LivingEntity entity = mock(LivingEntity.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(entity.getWorld()).thenReturn(world);
        when(event.getEntity()).thenReturn(entity);

        // Player deaths are ignored.
        when(event.getEntityType()).thenReturn(EntityType.PLAYER);
        listener.onEntityDeath(event);

        // Non-player with null killer is ignored.
        when(event.getEntityType()).thenReturn(EntityType.ZOMBIE);
        when(entity.getKiller()).thenReturn(null);
        listener.onEntityDeath(event);

        // Reward lookup miss is ignored.
        Player killer = mock(Player.class);
        when(entity.getKiller()).thenReturn(killer);
        when(plugin.getMobReward(EntityType.ZOMBIE)).thenReturn(null);
        listener.onEntityDeath(event);

        LifestealManager manager = mock(LifestealManager.class);
        when(plugin.getLifestealManager()).thenReturn(manager);
        verify(manager, never()).getOrCreateProfile(any(UUID.class));
    }

    @Test
    void filtersByWorldAdminBypassAndZeroReward() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);

        EntityDeathEvent event = mock(EntityDeathEvent.class);
        LivingEntity entity = mock(LivingEntity.class);
        Player killer = mock(Player.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("disabled_world");

        when(event.getEntityType()).thenReturn(EntityType.SKELETON);
        when(event.getEntity()).thenReturn(entity);
        when(entity.getKiller()).thenReturn(killer);
        when(entity.getWorld()).thenReturn(world);

        MobReward positiveReward = new MobReward(EntityType.SKELETON, 1.0, Set.of(), Set.of(), null);
        when(plugin.getMobReward(EntityType.SKELETON)).thenReturn(positiveReward);

        // Global world filter blocks reward.
        when(plugin.isLifestealEnabledInWorld("disabled_world")).thenReturn(false);
        new MobListener(plugin).onEntityDeath(event);

        // Per-reward world filter blocks reward.
        when(plugin.isLifestealEnabledInWorld("disabled_world")).thenReturn(true);
        MobReward restrictedReward = new MobReward(EntityType.SKELETON, 1.0, Set.of("allowed_world"), Set.of(), null);
        when(plugin.getMobReward(EntityType.SKELETON)).thenReturn(restrictedReward);
        new MobListener(plugin).onEntityDeath(event);

        // Admin bypass blocks reward.
        AdminDetector adminDetector = mock(AdminDetector.class);
        when(adminDetector.isAdmin(killer)).thenReturn(true);
        when(plugin.getAdminDetector()).thenReturn(adminDetector);
        when(plugin.isAdminBypassHeartGain()).thenReturn(true);
        when(plugin.getMobReward(EntityType.SKELETON)).thenReturn(positiveReward);
        new MobListener(plugin).onEntityDeath(event);

        // Zero hearts delta blocks reward.
        when(plugin.getAdminDetector()).thenReturn(null);
        MobReward zeroReward = new MobReward(EntityType.SKELETON, 0.0, Set.of(), Set.of(), null);
        when(plugin.getMobReward(EntityType.SKELETON)).thenReturn(zeroReward);
        new MobListener(plugin).onEntityDeath(event);

        LifestealManager manager = mock(LifestealManager.class);
        when(plugin.getLifestealManager()).thenReturn(manager);
        verify(manager, never()).getOrCreateProfile(any(UUID.class));
    }

    @Test
    void appliesRewardWhenEligibleAndSchedulesHeartsUpdate() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MobListener listener = new MobListener(plugin);

        UUID killerId = UUID.fromString("00000000-0000-0000-0000-000000000101");

        EntityDeathEvent event = mock(EntityDeathEvent.class);
        LivingEntity entity = mock(LivingEntity.class);
        Player killer = mock(Player.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        when(event.getEntityType()).thenReturn(EntityType.ZOMBIE);
        when(event.getEntity()).thenReturn(entity);
        when(entity.getKiller()).thenReturn(killer);
        when(entity.getWorld()).thenReturn(world);

        when(killer.getUniqueId()).thenReturn(killerId);
        when(killer.isOnline()).thenReturn(true);
        when(killer.getName()).thenReturn("Killer");

        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("world")).thenReturn(true);
        when(plugin.getAdminDetector()).thenReturn(null);

        MobReward reward = new MobReward(EntityType.ZOMBIE, 1.5, Set.of(), Set.of(), "mob.reward.perm");
        when(plugin.getMobReward(EntityType.ZOMBIE)).thenReturn(reward);
        when(killer.hasPermission("mob.reward.perm")).thenReturn(true);

        LifestealManager manager = mock(LifestealManager.class);
        LifestealProfile profile = new LifestealProfile(killerId, 10.0);
        when(manager.getOrCreateProfile(killerId)).thenReturn(profile);
        when(manager.getMaxHearts()).thenReturn(20.0);
        when(manager.saveProfileAsync(profile)).thenReturn(CompletableFuture.completedFuture(null));
        when(plugin.getLifestealManager()).thenReturn(manager);

        JavaPlugin javaPlugin = mock(JavaPlugin.class);
        when(plugin.getPlugin()).thenReturn(javaPlugin);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);

        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            Server server = mock(Server.class);
            when(server.getName()).thenReturn("Paper");
            mockedBukkit.when(Bukkit::getServer).thenReturn(server);
            mockedBukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTask(eq(javaPlugin), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return task;
            });

            listener.onEntityDeath(event);
        }

        verify(manager).getOrCreateProfile(killerId);
        verify(manager).saveProfileAsync(profile);
        verify(manager).applyHearts(killer, profile);
        verify(plugin).requestTopHologramUpdate();
    }
}
