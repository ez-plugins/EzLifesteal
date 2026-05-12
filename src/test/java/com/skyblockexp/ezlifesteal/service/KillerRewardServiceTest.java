package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KillerRewardServiceTest {

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    /**
     * Regression test: when killer's inventory is full, the overflowing heart item was dropped at
     * {@code victim.getLocation()} inside a scheduled task. Because the task runs on the next
     * server tick, the victim may have already respawned — making the item appear at the respawn
     * point rather than the kill location.
     *
     * <p>Fix: use {@code killer.getLocation()} for the drop so the item lands where the killer
     * stands, which is also the kill site on the tick following death.</p>
     */
    @Test
    void applyKillerReward_heartItemMode_fullInventory_dropsAtKillerLocation() {
        ServerMock server = MockBukkit.mock();
        JavaPlugin javaPlugin = MockBukkit.createMockPlugin();

        World world = mock(World.class);
        Location killerLocation = new Location(world, 100.0, 64.0, 100.0);
        Location victimRespawnLocation = new Location(world, 300.0, 64.0, 300.0);

        // Force addItem to always return a leftover (simulates full inventory)
        PlayerInventory inventory = mock(PlayerInventory.class);
        HashMap<Integer, ItemStack> leftover = new HashMap<>();
        leftover.put(0, new ItemStack(Material.NETHER_STAR));
        when(inventory.addItem(any())).thenReturn(leftover);

        Player killer = mock(Player.class);
        when(killer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(killer.getInventory()).thenReturn(inventory);
        when(killer.getLocation()).thenReturn(killerLocation);
        when(killer.getWorld()).thenReturn(world);

        // victim.getLocation() returns respawn point — different from kill site
        Player victim = mock(Player.class);
        when(victim.getUniqueId()).thenReturn(UUID.randomUUID());
        when(victim.getLocation()).thenReturn(victimRespawnLocation);

        Heart heart = mock(Heart.class);
        when(heart.getHearts()).thenReturn(1.0);
        when(heart.createItemStack()).thenReturn(new ItemStack(Material.NETHER_STAR));

        HeartRegistry registry = mock(HeartRegistry.class);
        when(registry.getById("basic")).thenReturn(heart);

        PluginAccessor pluginAccessor = mock(PluginAccessor.class);
        when(pluginAccessor.getPlugin()).thenReturn(javaPlugin);
        when(pluginAccessor.getDropHeartId()).thenReturn("basic");
        when(pluginAccessor.getDropHeartAmount()).thenReturn(1);
        when(pluginAccessor.getHeartRegistry()).thenReturn(registry);

        LifestealManager manager = mock(LifestealManager.class);
        when(manager.getMaxHearts()).thenReturn(40.0);

        LifestealProfile killerProfile = new LifestealProfile(killer.getUniqueId(), 10.0);
        KillerOutcome killerOutcome = new KillerOutcome(true, KillerRewardMode.HEART_ITEM, 1.0);

        KillerRewardService service = new KillerRewardService(pluginAccessor);
        service.applyKillerReward(victim, killer, killerProfile, manager, killerOutcome);

        // Execute the scheduled give-item task
        server.getScheduler().performTicks(1);

        ArgumentCaptor<Location> locationCaptor = ArgumentCaptor.forClass(Location.class);
        verify(world).dropItemNaturally(locationCaptor.capture(), any());

        Location dropLocation = locationCaptor.getValue();
        assertEquals(killerLocation, dropLocation,
                "Heart item must drop at killer's location, not victim's respawn location");
    }
}

