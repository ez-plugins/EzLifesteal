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

class SetSubcommandTest {

    @Test
    void permissionDenied_returnsEarly() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        CommandSender sender = mock(CommandSender.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(false);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);

        new SetSubcommand().execute(sender, null, "lifesteal", new String[]{"set", "target", "5"}, context);

        verify(context, never()).getPlayerLookupServicePublic();
    }

    @Test
    void invalidNumber_sendsSetInvalidAmount() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        CommandSender sender = mock(CommandSender.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(plugin.getMessageService()).thenReturn(messageService);

        new SetSubcommand().execute(sender, null, "lifesteal", new String[]{"set", "target", "bad"}, context);

        verify(messageService).sendMessage(sender, "set-invalid-amount");
    }

    @Test
    void asyncSuccess_usesLookupLoadSaveAndMainThreadDispatch() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        PlayerLookupService lookup = mock(PlayerLookupService.class);
        MessageService messageService = mock(MessageService.class);
        CommandSender sender = mock(CommandSender.class);

        UUID targetId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(targetId, 5.0);
        OfflinePlayer offline = mock(OfflinePlayer.class);

        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.getPlayerLookupServicePublic()).thenReturn(lookup);
        when(context.getMainThreadExecutorPublic()).thenReturn(Runnable::run);
        when(context.resolvePlayerNamePublic(any(), anyString())).thenReturn("target");
        when(context.formatPublic(anyDouble())).thenReturn("9");
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(lookup.lookupUniqueId("target")).thenReturn(CompletableFuture.completedFuture(Optional.of(targetId)));
        when(manager.getMinHearts()).thenReturn(1.0);
        when(manager.getMaxHearts()).thenReturn(20.0);
        when(manager.loadProfileAsync(targetId)).thenReturn(CompletableFuture.completedFuture(profile));
        when(offline.isOnline()).thenReturn(false);

        try (MockedStatic<Bukkit> mocked = mockStatic(Bukkit.class)) {
            mocked.when(() -> Bukkit.getOfflinePlayer(targetId)).thenReturn(offline);
            new SetSubcommand().execute(sender, null, "lifesteal", new String[]{"set", "target", "9"}, context);
        }

        verify(lookup).lookupUniqueId("target");
        verify(manager).loadProfileAsync(targetId);
        verify(manager).saveProfileAsync(profile);
        verify(plugin).requestTopHologramUpdate();
        verify(messageService).sendMessage(eq(sender), eq("set-hearts"), anyMap());
    }
}
