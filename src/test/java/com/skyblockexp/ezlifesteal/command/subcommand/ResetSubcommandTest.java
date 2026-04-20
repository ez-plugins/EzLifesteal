package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
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

class ResetSubcommandTest {

    @Test
    void permissionDenied_returnsEarly() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        CommandSender sender = mock(CommandSender.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(false);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);

        new ResetSubcommand().execute(sender, null, "lifesteal", new String[]{"reset", "target"}, context);

        verify(context, never()).getPlayerLookupServicePublic();
    }

    @Test
    void missingPlayerArg_sendsUsage() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        CommandSender sender = mock(CommandSender.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);

        new ResetSubcommand().execute(sender, null, "lifesteal", new String[]{"reset"}, context);

        verify(sender).sendMessage("Usage: /lifesteal reset <player>");
    }

    @Test
    void success_delegatesOnce() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        PlayerLookupService lookup = mock(PlayerLookupService.class);
        MessageService messageService = mock(MessageService.class);
        CommandSender sender = mock(CommandSender.class);

        UUID targetId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(targetId, 10.0);
        OfflinePlayer offline = mock(OfflinePlayer.class);

        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.getPlayerLookupServicePublic()).thenReturn(lookup);
        when(context.getMainThreadExecutorPublic()).thenReturn(Runnable::run);
        when(context.resolvePlayerNamePublic(any(), anyString())).thenReturn("target");
        when(context.formatPublic(anyDouble())).thenReturn("8");
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(manager.getDefaultHearts()).thenReturn(8.0);
        when(lookup.lookupUniqueId("target")).thenReturn(CompletableFuture.completedFuture(Optional.of(targetId)));
        when(manager.loadProfileAsync(targetId)).thenReturn(CompletableFuture.completedFuture(profile));
        when(offline.isOnline()).thenReturn(false);

        try (MockedStatic<Bukkit> mocked = mockStatic(Bukkit.class)) {
            mocked.when(() -> Bukkit.getOfflinePlayer(targetId)).thenReturn(offline);
            new ResetSubcommand().execute(sender, null, "lifesteal", new String[]{"reset", "target"}, context);
        }

        verify(manager).loadProfileAsync(targetId);
        verify(manager).saveProfileAsync(profile);
        verify(plugin).requestTopHologramUpdate();
        verify(messageService).sendMessage(eq(sender), eq("reset-hearts"), anyMap());
    }
}
