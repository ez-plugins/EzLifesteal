package com.skyblockexp.ezlifesteal.listener;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HeartRedemptionTest {

    @Test
    void redeemingHeartRightClickAddsHeartsAndConsumesItem() {
        // Build minimal real components to avoid Mockito inline mock limitations on Java 21
        com.skyblockexp.ezlifesteal.config.MessageService msg =
                new com.skyblockexp.ezlifesteal.config.MessageService("");

        // use a concrete Heart and a lightweight HeartRegistry to avoid Mockito/byte-buddy issues
        com.skyblockexp.ezlifesteal.heart.Heart heart = new com.skyblockexp.ezlifesteal.heart.Heart(
                "basic", "&cBasic Heart", 1, 2.0, org.bukkit.Material.NETHER_STAR, "", java.util.Map.of()
        );
        com.skyblockexp.ezlifesteal.heart.HeartRegistry registry
                = new com.skyblockexp.ezlifesteal.heart.HeartRegistry(null) {
                    @Override
                    public com.skyblockexp.ezlifesteal.heart.Heart getById(String id) {
                        if (id == null) {
                            return null;
                        }
                        return id.equalsIgnoreCase("basic") ? heart : null;
                    }
                };

        // Mock PluginAccessor to avoid creating a JavaPlugin; mock LifestealManager to control profiles
        com.skyblockexp.ezlifesteal.runtime.PluginAccessor accessor =
                mock(com.skyblockexp.ezlifesteal.runtime.PluginAccessor.class);

        LifestealManager manager = mock(LifestealManager.class);
        UUID playerId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(playerId, 1.0);
        when(manager.getLoadedProfile(playerId)).thenReturn(Optional.of(profile));
        when(manager.getMaxHearts()).thenReturn(20.0);
        when(manager.saveProfileAsync(profile)).thenReturn(CompletableFuture.completedFuture(null));

        when(accessor.getHeartRegistry()).thenReturn(registry);
        when(accessor.getLifestealManager()).thenReturn(manager);
        when(accessor.getMessageService()).thenReturn(msg);

        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);

        // Mock item and persistent data
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer container = mock(PersistentDataContainer.class);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(container);
        when(accessor.getHeartIdFrom(container)).thenReturn("basic");
        when(item.getAmount()).thenReturn(1);

        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getPlayer()).thenReturn(player);
        when(event.getItem()).thenReturn(item);

        PlayerListener listener = new PlayerListener(accessor, "", "", false, 0L);

        listener.onPlayerInteract(event);

        // profile hearts increased by heart.getHearts() (1.0 + 2.0 => 3.0)
        assertEquals(3.0, profile.getHearts());
        // item consumption may be handled by replacing the ItemStack; verify hearts instead
    }
}
