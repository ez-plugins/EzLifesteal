package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HeartsSubcommandTest {

    @Test
    void permissionDenied_returnsEarly() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        CommandSender sender = mock(CommandSender.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(false);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);

        new HeartsSubcommand().execute(sender, null, "lifesteal", new String[]{"hearts", "target"}, context);

        verify(context, never()).getPlayerLookupServicePublic();
    }

    @Test
    void unknownPlayer_callsPlayerNotFound() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        MessageService messageService = mock(MessageService.class);
        PlayerLookupService lookup = mock(PlayerLookupService.class);
        CommandSender sender = mock(CommandSender.class);

        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.getPlayerLookupServicePublic()).thenReturn(lookup);
        when(context.getMainThreadExecutorPublic()).thenReturn(Runnable::run);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(lookup.lookupUniqueId("ghost")).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        new HeartsSubcommand().execute(sender, null, "lifesteal", new String[]{"hearts", "ghost"}, context);

        verify(context).sendPlayerNotFoundPublic(sender, "ghost");
    }
}
