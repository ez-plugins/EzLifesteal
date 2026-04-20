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
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class MobListenerActionBarTest {

    @Test
    void nonEmptyActionBarIsSentViaActionBarHelper() {
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
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(living.getWorld()).thenReturn(world);
        when(living.getKiller()).thenReturn(killer);
        when(event.getEntity()).thenReturn(living);

        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld(any())).thenReturn(true);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(ms);

        MobReward reward = new MobReward(EntityType.ZOMBIE, 1.0, Set.of(), Set.of(), null);
        when(plugin.getMobReward(EntityType.ZOMBIE)).thenReturn(reward);

        LifestealProfile profile = new LifestealProfile(killerId, 5.0);
        when(manager.getOrCreateProfile(killerId)).thenReturn(profile);
        when(manager.getMaxHearts()).thenReturn(20.0);
        when(manager.saveProfileAsync(profile)).thenReturn(CompletableFuture.completedFuture(null));

        JavaPlugin javaPlugin = mock(JavaPlugin.class);
        when(plugin.getPlugin()).thenReturn(javaPlugin);

        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            Server server = mock(Server.class);
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            org.mockito.Mockito.when(scheduler.runTask(
                    org.mockito.ArgumentMatchers.eq(javaPlugin),
                    org.mockito.ArgumentMatchers.any(Runnable.class))).thenAnswer(invocation -> {
                        Runnable r = invocation.getArgument(1);
                        r.run();
                        return mock(org.bukkit.scheduler.BukkitTask.class);
                    });

            when(ms.render(any(), any())).thenReturn("actionbar-text");

            MobListener listener = new MobListener(plugin);
            try (MockedStatic<ActionBarHelper> actionBar = mockStatic(ActionBarHelper.class)) {
                listener.onEntityDeath(event);
                actionBar.verify(() -> ActionBarHelper.sendActionBar(killer, "actionbar-text"));
            }
        }
    }
}
