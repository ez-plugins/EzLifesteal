package com.skyblockexp.ezlifesteal.listener;

import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerListenerMockBukkitJoinTest {

    @Test
    void joinAppliesHeartsWhenProfileLoads_withMockBukkitServer() {
        MockBukkit.mock();
        try {
            PluginAccessor plugin = mock(PluginAccessor.class);
            MessageService messageService = mock(MessageService.class);
            LifestealManager manager = mock(LifestealManager.class);
            org.bukkit.plugin.java.JavaPlugin javaPlugin = mock(org.bukkit.plugin.java.JavaPlugin.class);
            when(plugin.getPlugin()).thenReturn(javaPlugin);

            UUID playerId = UUID.randomUUID();
            LifestealProfile profile = new LifestealProfile(playerId, 8.0);

            Player player = mock(Player.class);
            World world = mock(World.class);
            when(player.getUniqueId()).thenReturn(playerId);
            when(player.getWorld()).thenReturn(world);
            when(player.getName()).thenReturn("joiner");
            when(player.isOnline()).thenReturn(true);

            PlayerJoinEvent event = mock(PlayerJoinEvent.class);
            when(event.getPlayer()).thenReturn(player);

            when(plugin.getMessageService()).thenReturn(messageService);
            when(plugin.getLifestealManager()).thenReturn(manager);
            when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
            when(plugin.isLifestealEnabledInWorld(any())).thenReturn(true);
            when(manager.loadProfileAsync(playerId)).thenReturn(CompletableFuture.completedFuture(profile));

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

                PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
                listener.onPlayerJoin(event);

                verify(plugin).sendHeartStatus(player, profile.getHearts());
            }
        }
        finally {
            MockBukkit.unmock();
        }
    }
}
