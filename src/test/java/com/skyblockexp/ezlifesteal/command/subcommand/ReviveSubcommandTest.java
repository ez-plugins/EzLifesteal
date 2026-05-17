package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
import com.skyblockexp.ezlifesteal.util.ban.PlatformBanAdapter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviveSubcommandTest {

    @Test
    void permissionDenied_returnsEarly() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        CommandSender sender = mock(CommandSender.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(false);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);

        new ReviveSubcommand().execute(sender, null, "lifesteal", new String[]{"revive", "target"}, context);

        verify(context, never()).getPlayerLookupServicePublic();
    }

    @Test
    void unknownPlayer_callsPlayerNotFound() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        PlayerLookupService lookup = mock(PlayerLookupService.class);
        CommandSender sender = mock(CommandSender.class);

        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.getPlayerLookupServicePublic()).thenReturn(lookup);
        when(context.getMainThreadExecutorPublic()).thenReturn(Runnable::run);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(lookup.lookupUniqueId("ghost")).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        new ReviveSubcommand().execute(sender, null, "lifesteal", new String[]{"revive", "ghost"}, context);

        verify(context).sendPlayerNotFoundPublic(sender, "ghost");
    }

    @Test
    void success_loadsSavesAndSendsReviveMessage() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        PlayerLookupService lookup = mock(PlayerLookupService.class);
        MessageService messageService = mock(MessageService.class);
        CommandSender sender = mock(CommandSender.class);

        UUID targetId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(targetId, 1.0);
        OfflinePlayer offline = mock(OfflinePlayer.class);
        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);

        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.getPlayerLookupServicePublic()).thenReturn(lookup);
        when(context.getMainThreadExecutorPublic()).thenReturn(Runnable::run);
        when(context.resolvePlayerNamePublic(any(), anyString())).thenReturn("target");
        when(context.formatPublic(anyDouble())).thenReturn("6");
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getBanAdapter()).thenReturn(banAdapter);
        when(manager.getDefaultHearts()).thenReturn(6.0);
        when(lookup.lookupUniqueId("target")).thenReturn(CompletableFuture.completedFuture(Optional.of(targetId)));
        when(manager.loadProfileAsync(targetId)).thenReturn(CompletableFuture.completedFuture(profile));
        when(offline.isOnline()).thenReturn(false);
        when(offline.getUniqueId()).thenReturn(targetId);

        try (MockedStatic<Bukkit> mocked = mockStatic(Bukkit.class)) {
            mocked.when(() -> Bukkit.getOfflinePlayer(targetId)).thenReturn(offline);
            new ReviveSubcommand().execute(sender, null, "lifesteal", new String[]{"revive", "target"}, context);
        }

        verify(manager).loadProfileAsync(targetId);
        verify(manager).saveProfileAsync(profile);
        verify(banAdapter).removeBan(targetId, "target");
        verify(plugin).requestTopHologramUpdate();
        verify(messageService).sendMessage(eq(sender), eq("revive-success"), anyMap());
    }
}
