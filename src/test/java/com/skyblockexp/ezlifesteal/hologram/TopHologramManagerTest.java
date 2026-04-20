package com.skyblockexp.ezlifesteal.hologram;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.PluginLogger;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TopHologramManagerTest {

    @Test
    void placeAndRemoveHandleExistingAndMissingHologram() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        LifestealManager lifestealManager = mock(LifestealManager.class);
        ConfigurationSection section = new MemoryConfiguration().createSection("hologram");
        World world = mock(World.class);
        Location location = new Location(world, 10.0, 70.0, -5.0);
        ArmorStand stand = mock(ArmorStand.class);
        SchedulerAdapter.TaskHandle handle = mock(SchedulerAdapter.TaskHandle.class);

        when(world.spawnEntity(any(Location.class), eq(EntityType.ARMOR_STAND))).thenReturn(stand);
        when(plugin.getLifestealManager()).thenReturn(lifestealManager);
        when(plugin.getHologramSection(true)).thenReturn(section);
        when(plugin.getHologramSection(false)).thenReturn(section);
        when(lifestealManager.loadTopProfilesAsync(any(Integer.class)))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            scheduler.when(() -> SchedulerAdapter.run(any(), any())).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return null;
            });
            scheduler.when(() -> SchedulerAdapter.runTimer(any(), any(), any(Long.class), any(Long.class)))
                    .thenReturn(handle);

            TopHologramManager manager = new TopHologramManager(plugin);

            assertFalse(manager.remove());
            assertTrue(manager.place(location));
            assertTrue(manager.hasHologram());
            assertNotNull(manager.getLocation());

            assertTrue(manager.place(location));
            verify(handle, atLeastOnce()).cancel();

            assertTrue(manager.remove());
            assertFalse(manager.hasHologram());
            verify(plugin, atLeastOnce()).saveHologramSettings();
        }
    }

    @Test
    void requestUpdateBuildsEmptyAndPopulatedContent() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        LifestealManager lifestealManager = mock(LifestealManager.class);
        MessageService messageService = mock(MessageService.class);
        World world = mock(World.class);
        Location location = new Location(world, 1.0, 65.0, 1.0);
        ConfigurationSection section = new MemoryConfiguration().createSection("hologram");

        ArmorStand header = mock(ArmorStand.class);
        ArmorStand line2 = mock(ArmorStand.class);
        ArmorStand line3 = mock(ArmorStand.class);
        when(world.spawnEntity(any(Location.class), eq(EntityType.ARMOR_STAND)))
                .thenReturn(header, line2, line3);

        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        OfflinePlayer firstOffline = mock(OfflinePlayer.class);
        OfflinePlayer secondOffline = mock(OfflinePlayer.class);
        when(firstOffline.getName()).thenReturn("Alpha");
        when(secondOffline.getName()).thenReturn("Bravo");

        when(plugin.getLifestealManager()).thenReturn(lifestealManager);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getHologramSection(true)).thenReturn(section);
        when(plugin.getHologramSection(false)).thenReturn(section);

        when(messageService.render(eq("hologram-header"), any())).thenReturn("§cHeader");
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var map = (java.util.Map<String, String>) invocation.getArgument(1);
            return "#" + map.get("rank") + " " + map.get("player") + " " + map.get("hearts");
        }).when(messageService).render(eq("hologram-entry"), any());
        when(messageService.render(eq("hologram-empty"), any())).thenReturn("§7Empty");

        SchedulerAdapter.TaskHandle handle = mock(SchedulerAdapter.TaskHandle.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            bukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(null);
            bukkit.when(() -> Bukkit.getOfflinePlayer(first)).thenReturn(firstOffline);
            bukkit.when(() -> Bukkit.getOfflinePlayer(second)).thenReturn(secondOffline);
            scheduler.when(() -> SchedulerAdapter.run(any(), any())).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return null;
            });
            scheduler.when(() -> SchedulerAdapter.runTimer(any(), any(), any(Long.class), any(Long.class)))
                    .thenReturn(handle);

            TopHologramManager manager = new TopHologramManager(plugin);

            when(lifestealManager.loadTopProfilesAsync(any(Integer.class)))
                    .thenReturn(CompletableFuture.completedFuture(List.of()));
            manager.place(location);
            verify(header, atLeastOnce()).setCustomName("§cHeader");
            verify(line2, atLeastOnce()).setCustomName("§7Empty");

            when(lifestealManager.loadTopProfilesAsync(any(Integer.class)))
                    .thenReturn(CompletableFuture.completedFuture(
                    List.of(new LifestealProfile(first, 15.0), new LifestealProfile(second, 8.5))
            ));
            manager.requestUpdate();

            verify(line2, atLeastOnce()).setCustomName("#1 Alpha 15");
            verify(line3, atLeastOnce()).setCustomName("#2 Bravo 8.5");
        }
    }

    @Test
    void invalidWorldAndEntityReferencesAreSafeNoOps() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PluginLogger logger = mock(PluginLogger.class);
        MemoryConfiguration config = new MemoryConfiguration();
        ConfigurationSection section = config.createSection("hologram");
        ConfigurationSection locationSection = section.createSection("location");
        locationSection.set("world", "missing_world");

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getHologramSection(false)).thenReturn(section);

        TopHologramManager manager = new TopHologramManager(plugin);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class)) {
            bukkit.when(() -> Bukkit.getWorld("missing_world")).thenReturn(null);
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            scheduler.when(() -> SchedulerAdapter.run(any(), any())).thenAnswer(invocation -> null);

            assertFalse(manager.place(null));
            assertFalse(manager.remove());

            manager.reload(section);
            assertFalse(manager.hasHologram());
            verify(logger).warning(any(String.class));

            manager.requestUpdate();
            scheduler.verify(() -> SchedulerAdapter.run(any(), any()), never());
        }

        Method removeArmorStands = TopHologramManager.class.getDeclaredMethod("removeArmorStands");
        removeArmorStands.setAccessible(true);
        removeArmorStands.invoke(manager);
    }
}
