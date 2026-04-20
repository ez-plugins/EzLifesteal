package com.skyblockexp.ezlifesteal.listener;

import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.storage.BanRecord;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerListenerMoreBranchesTest {

    @Test
    void onEntityDamageByEntity_setsCombatTags_whenEnabledAndConditionsMet() throws Exception {
        PluginAccessor plugin = mock(PluginAccessor.class);
        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld(any())).thenReturn(true);

        LifestealManager manager = mock(LifestealManager.class);
        when(plugin.getLifestealManager()).thenReturn(manager);

        Player attacker = mock(Player.class);
        Player victim = mock(Player.class);
        UUID aId = UUID.randomUUID();
        UUID vId = UUID.randomUUID();
        when(attacker.getUniqueId()).thenReturn(aId);
        when(victim.getUniqueId()).thenReturn(vId);
        when(victim.getWorld()).thenReturn(mock(org.bukkit.World.class));

        PlayerListener listener = new PlayerListener(plugin, "", "", true, 1000L);

        Entity eventDamager = attacker;
        org.bukkit.event.entity.EntityDamageByEntityEvent event =
                mock(org.bukkit.event.entity.EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamager()).thenReturn(eventDamager);

        listener.onEntityDamageByEntity(event);

        // inspect private combatTags map
        Field f = PlayerListener.class.getDeclaredField("combatTags");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Long> tags = (Map<UUID, Long>) f.get(listener);

        assertTrue(tags.containsKey(aId));
        assertTrue(tags.containsKey(vId));
    }

    @Test
    void isMobDeath_returnsTrue_whenLastDamagerIsLivingEntity() throws Exception {
        PluginAccessor plugin = mock(PluginAccessor.class);
        PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);

        UUID victimId = UUID.randomUUID();
        LivingEntity mob = mock(LivingEntity.class);

        Field lastDamagers = PlayerListener.class.getDeclaredField("lastDamagers");
        lastDamagers.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Entity> map = (Map<UUID, Entity>) lastDamagers.get(listener);
        map.put(victimId, mob);

        Method m = PlayerListener.class.getDeclaredMethod("isMobDeath", UUID.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(listener, victimId);

        assertTrue(result);
    }

    @Test
    void handleCombatLogout_appliesBanAndPersistsRecord_whenZeroHearts() throws Exception {
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        BanRepository banRepo = mock(BanRepository.class);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getBanRepository()).thenReturn(banRepo);
        when(plugin.getPluginName()).thenReturn("EzLifesteal");
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        when(plugin.getMessageService()).thenReturn(mock(MessageService.class));

        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(player.getName()).thenReturn("p");
        when(player.getWorld()).thenReturn(mock(org.bukkit.World.class));

        LifestealProfile profile = new LifestealProfile(id, 1.0);
        // set profile to lose hearts to 0
        profile.removeHearts(2.0, 0.0);
        when(player.isOnline()).thenReturn(true);

        when(manager.getLoadedProfile(id)).thenReturn(Optional.of(profile));
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        when(plugin.isBanWhenZeroHearts(any())).thenReturn(true);

        // Mock scheduler and ban list via Bukkit static
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTask(any(JavaPlugin.class), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable r = invocation.getArgument(1);
            r.run();
            return mock(BukkitTask.class);
        });

        BanList banList = mock(BanList.class);

        try (MockedStatic<Bukkit> b = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            Server server = mock(Server.class);
            when(server.getName()).thenReturn("Paper");
            b.when(Bukkit::getServer).thenReturn(server);
            b.when(Bukkit::getScheduler).thenReturn(scheduler);
            b.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);

            when(plugin.getPlugin()).thenReturn(mock(JavaPlugin.class));

            PlayerListener listener = new PlayerListener(plugin, "&cBanned", "&cKicked", true, 1000L);

            Method m = PlayerListener.class.getDeclaredMethod("handleCombatLogout", Player.class);
            m.setAccessible(true);
            m.invoke(listener, player);
        }

        verify(banRepo).saveBan(any(BanRecord.class));
        verify(player).kickPlayer(anyString());
    }
}
