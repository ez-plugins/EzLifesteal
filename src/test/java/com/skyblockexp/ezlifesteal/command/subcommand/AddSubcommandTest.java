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
import java.util.logging.Logger;
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

class AddSubcommandTest {

    @Test
    void permissionDenied_returnsEarlyWithoutSideEffects() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        CommandSender sender = mock(CommandSender.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(false);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);

        boolean result = new AddSubcommand().execute(sender, null, "lifesteal", new String[]{"add", "target", "2"},
                context);

        assert result;
        verify(context, never()).getPlayerLookupServicePublic();
        verify(plugin, never()).getLifestealManager();
    }

    @Test
    void invalidAmount_sendsModifyInvalidAmount() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService messageService = mock(MessageService.class);
        CommandSender sender = mock(CommandSender.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(plugin.getMessageService()).thenReturn(messageService);

        boolean result = new AddSubcommand().execute(sender, null, "lifesteal", new String[]{"add", "target", "NaN!"},
                context);

        assert result;
        verify(messageService).sendMessage(sender, "modify-invalid-amount");
        verify(context, never()).getPlayerLookupServicePublic();
    }

    @Test
    void unknownPlayer_usesPlayerNotFoundBranch() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        PlayerLookupService lookup = mock(PlayerLookupService.class);
        CommandSender sender = mock(CommandSender.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.getPlayerLookupServicePublic()).thenReturn(lookup);
        when(context.getMainThreadExecutorPublic()).thenReturn(Runnable::run);
        when(lookup.lookupUniqueId("ghost")).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        new AddSubcommand().execute(sender, null, "lifesteal", new String[]{"add", "ghost", "2"}, context);

        verify(context).sendPlayerNotFoundPublic(sender, "ghost");
    }

    @Test
    void success_delegatesToProfileLoadSaveAndHologramUpdate() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        PlayerLookupService lookup = mock(PlayerLookupService.class);
        MessageService messageService = mock(MessageService.class);
        Logger logger = mock(Logger.class);
        CommandSender sender = mock(CommandSender.class);

        UUID targetId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(targetId, 10.0);
        OfflinePlayer offline = mock(OfflinePlayer.class);

        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.getPlayerLookupServicePublic()).thenReturn(lookup);
        when(context.getMainThreadExecutorPublic()).thenReturn(Runnable::run);
        when(context.resolvePlayerNamePublic(any(), anyString())).thenReturn("target");
        when(context.formatPublic(anyDouble())).thenReturn("12");
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getLogger()).thenReturn(logger);
        when(lookup.lookupUniqueId("target")).thenReturn(CompletableFuture.completedFuture(Optional.of(targetId)));
        when(manager.getMaxHearts()).thenReturn(20.0);
        when(manager.loadProfileAsync(targetId)).thenReturn(CompletableFuture.completedFuture(profile));
        when(offline.isOnline()).thenReturn(false);

        try (MockedStatic<Bukkit> mocked = mockStatic(Bukkit.class)) {
            mocked.when(() -> Bukkit.getOfflinePlayer(targetId)).thenReturn(offline);
            new AddSubcommand().execute(sender, null, "lifesteal", new String[]{"add", "target", "2"}, context);
        }

        verify(manager).loadProfileAsync(targetId);
        verify(manager).saveProfileAsync(profile);
        verify(plugin).requestTopHologramUpdate();
        verify(messageService).sendMessage(eq(sender), eq("add-hearts"), anyMap());
    }
}
