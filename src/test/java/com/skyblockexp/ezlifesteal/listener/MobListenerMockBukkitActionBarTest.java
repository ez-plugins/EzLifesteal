package com.skyblockexp.ezlifesteal.listener;

import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.model.MobReward;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.util.ActionBarHelper;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class MobListenerMockBukkitActionBarTest {

    @Test
    void actionBarIsSent_whenMessageServiceRendersNonEmpty_andMockBukkitServerPresent() {
        MockBukkit.mock();
        try {
            PluginAccessor plugin = mock(PluginAccessor.class);
            LifestealManager manager = mock(LifestealManager.class);
            MessageService ms = mock(MessageService.class);

            UUID killerId = UUID.randomUUID();
            Player killer = mock(Player.class);
            when(killer.getUniqueId()).thenReturn(killerId);
            when(killer.getName()).thenReturn("Killer");
            when(killer.isOnline()).thenReturn(true);

            EntityDeathEvent event = mock(EntityDeathEvent.class);
            when(event.getEntityType()).thenReturn(EntityType.ZOMBIE);
            org.bukkit.entity.LivingEntity living = mock(org.bukkit.entity.LivingEntity.class);
            org.bukkit.World world = mock(org.bukkit.World.class);
            when(world.getName()).thenReturn("world");
            when(living.getWorld()).thenReturn(world);
            when(event.getEntity()).thenReturn(living);
            when(living.getKiller()).thenReturn(killer);

            when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
            when(plugin.isLifestealEnabledInWorld(any())).thenReturn(true);
            when(plugin.getLifestealManager()).thenReturn(manager);
            when(plugin.getMessageService()).thenReturn(ms);
            org.bukkit.plugin.java.JavaPlugin javaPlugin = mock(org.bukkit.plugin.java.JavaPlugin.class);
            when(plugin.getPlugin()).thenReturn(javaPlugin);

            MobReward reward = new MobReward(EntityType.ZOMBIE, 1.0, Set.of(), Set.of(), null);
            when(plugin.getMobReward(EntityType.ZOMBIE)).thenReturn(reward);

            LifestealProfile profile = new LifestealProfile(killerId, 5.0);
            when(manager.getOrCreateProfile(killerId)).thenReturn(profile);
            when(manager.getMaxHearts()).thenReturn(20.0);
            when(manager.saveProfileAsync(profile)).thenReturn(CompletableFuture.completedFuture(null));

            MobListener listener = new MobListener(plugin);
            // Ensure scheduled tasks run immediately by mocking Bukkit's scheduler
            try (org.mockito.MockedStatic<org.bukkit.Bukkit> bukkit
                    = org.mockito.Mockito.mockStatic(org.bukkit.Bukkit.class)) {
                org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
                org.bukkit.scheduler.BukkitTask task = mock(org.bukkit.scheduler.BukkitTask.class);
                org.bukkit.Server server = mock(org.bukkit.Server.class);
                when(server.getName()).thenReturn("MockBukkit");
                bukkit.when(org.bukkit.Bukkit::getServer).thenReturn(server);
                bukkit.when(org.bukkit.Bukkit::getScheduler).thenReturn(scheduler);
                org.mockito.Mockito
                        .when(scheduler.runTask(
                                org.mockito.ArgumentMatchers.eq(javaPlugin),
                                org.mockito.ArgumentMatchers.any(Runnable.class)))
                        .thenAnswer(invocation -> {
                            Runnable r = invocation.getArgument(1);
                            r.run();
                            return task;
                        });

                try (MockedStatic<ActionBarHelper> actionBar = mockStatic(ActionBarHelper.class)) {
                    when(ms.render(any(), any())).thenReturn("actionbar-text");
                    listener.onEntityDeath(event);
                    actionBar.verify(() -> ActionBarHelper.sendActionBar(killer, "actionbar-text"));
                }
            }
        }
        finally {
            MockBukkit.unmock();
        }
    }
}
