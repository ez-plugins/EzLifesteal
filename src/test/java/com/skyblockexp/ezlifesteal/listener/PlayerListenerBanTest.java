package com.skyblockexp.ezlifesteal.listener;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.storage.BanRecord;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerListenerBanTest {

    @Test
    @SuppressWarnings("unchecked")
    void handlePlayerDeath_appliesBanAndPersistsRecord_whenHeartsReachZero() throws Exception {
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        BanRepository banRepo = mock(BanRepository.class);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld(any())).thenReturn(true);
        when(plugin.getBanRepository()).thenReturn(banRepo);
        when(plugin.getPluginName()).thenReturn("EzLifesteal");

        Player victim = mock(Player.class);
        UUID id = UUID.randomUUID();
        PlayerProfile playerProfile = mock(PlayerProfile.class);
        when(victim.getUniqueId()).thenReturn(id);
        when(victim.getName()).thenReturn("targetPlayer");
        when(victim.getPlayerProfile()).thenReturn(playerProfile);
        World world = mock(World.class);
        when(victim.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");

        // profile starts with 1 heart, loses 2 -> 0 or less
        LifestealProfile profile = new LifestealProfile(id, 1.0);
        when(manager.getOrCreateProfile(id)).thenReturn(profile);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        when(plugin.getHeartsLostOnDeath(any())).thenReturn(2.0);
        when(plugin.isBanWhenZeroHearts(any())).thenReturn(true);

        // Mock scheduler to run tasks immediately
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTask(any(JavaPlugin.class), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable r = invocation.getArgument(1);
            r.run();
            return mock(BukkitTask.class);
        });

        BanList<PlayerProfile> banList = mock(BanList.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            Server server = mock(Server.class);
            when(server.getName()).thenReturn("Paper");

            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);

            when(plugin.getPlugin()).thenReturn(mock(JavaPlugin.class));

            PlayerListener listener = new PlayerListener(plugin, "&cBanned", "&cKicked", false, 0L);

            Method m = PlayerListener.class.getDeclaredMethod("handlePlayerDeath", Player.class, Player.class);
            m.setAccessible(true);
            m.invoke(listener, victim, null);

            // BanRecord persisted
            verify(banRepo).saveBan(any(BanRecord.class));
            // Bukkit ban added
            bukkit.verify(() -> Bukkit.getBanList(BanList.Type.PROFILE), atLeastOnce());
        }
    }
}
