package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CombatTagServiceTest {

    @Test
    void handleCombatLogoutAppliesPenaltyAndZeroHeartCommandsWhenNotBanned() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        BanEnforcementService banService = mock(BanEnforcementService.class);
        LifestealManager manager = mock(LifestealManager.class);
        MessageService messageService = mock(MessageService.class);
        Player player = mock(Player.class);
        World world = mock(World.class);
        UUID playerId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(playerId, 2.0);

        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(player.getName()).thenReturn("combat");
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(2.0);
        when(plugin.isBanWhenZeroHearts("world")).thenReturn(false);
        when(manager.getLoadedProfile(playerId)).thenReturn(Optional.of(profile));
        when(manager.getMinHearts()).thenReturn(0.0);
        when(manager.saveProfileAsync(profile)).thenReturn(CompletableFuture.completedFuture(null));

        CombatTagService service = new CombatTagService(plugin, banService, "", "", true, 60_000L);
        service.handleCombatLogout(player);

        verify(messageService).sendMessage(eq(player), eq("combat-logout-penalty"), any());
        verify(plugin).executeZeroHeartCommands(player, null, 0.0);
        verify(banService, never()).applyBanWithStorage(any(), any(), any());
    }
}
