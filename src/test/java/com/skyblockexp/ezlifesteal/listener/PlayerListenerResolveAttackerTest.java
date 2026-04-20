package com.skyblockexp.ezlifesteal.listener;

import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerListenerResolveAttackerTest {

    @Test
    void entityDamageByProjectileTagsBothAttackerAndVictim() throws Exception {
        PluginAccessor plugin = mock(PluginAccessor.class);
        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);

        Player victim = mock(Player.class);
        Player attacker = mock(Player.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(victim.getWorld()).thenReturn(world);
        when(attacker.getWorld()).thenReturn(world);

        UUID victimId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();
        when(victim.getUniqueId()).thenReturn(victimId);
        when(attacker.getUniqueId()).thenReturn(attackerId);
        when(victim.isOnline()).thenReturn(true);
        when(attacker.isOnline()).thenReturn(true);

        Projectile projectile = mock(Projectile.class);
        when(projectile.getShooter()).thenReturn(attacker);

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamager()).thenReturn(projectile);

        when(plugin.isLifestealEnabledInWorld(any())).thenReturn(true);

        PlayerListener listener = new PlayerListener(plugin, "", "", true, 10_000L);
        listener.onEntityDamageByEntity(event);

        // reflectively inspect combatTags map
        Field field = PlayerListener.class.getDeclaredField("combatTags");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Long> tags = (Map<UUID, Long>) field.get(listener);

        assertTrue(tags.containsKey(victimId));
        assertTrue(tags.containsKey(attackerId));
    }
}
