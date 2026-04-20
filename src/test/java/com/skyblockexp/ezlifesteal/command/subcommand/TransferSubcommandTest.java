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
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferSubcommandTest {

    @Test
    void transferSucceedsAndSendsMessages() {
        PluginAccessor plugin = Mockito.mock(PluginAccessor.class);
        LifestealManager manager = Mockito.mock(LifestealManager.class);
        MessageService msg = Mockito.mock(MessageService.class);
        PlayerLookupService lookup = Mockito.mock(PlayerLookupService.class);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(msg);

        UUID senderId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Player sender = Mockito.mock(Player.class);
        when(sender.getUniqueId()).thenReturn(senderId);

        when(lookup.lookupUniqueId(anyString())).thenReturn(CompletableFuture.completedFuture(Optional.of(targetId)));

        LifestealProfile senderProfile = new LifestealProfile(senderId, 10.0);
        LifestealProfile targetProfile = new LifestealProfile(targetId, 2.0);

        when(manager.getLoadedProfile(senderId)).thenReturn(Optional.of(senderProfile));
        when(manager.getLoadedProfile(targetId)).thenReturn(Optional.of(targetProfile));

        LifestealCommand context = Mockito.mock(LifestealCommand.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.getPlayerLookupServicePublic()).thenReturn(lookup);
        when(context.getMainThreadExecutorPublic()).thenReturn(Runnable::run);
        when(context.formatPublic(anyDouble())).thenAnswer(i -> String.format("%.0f", i.getArgument(0)));
        when(context.resolvePlayerNamePublic(any(), anyString())).thenReturn("TargetName");

        TransferSubcommand sub = new TransferSubcommand();
        boolean result = sub.execute(sender, null, "lifesteal", new String[]{"transfer", "target", "3"}, context);

        assert result;

        verify(msg, atLeastOnce()).sendMessage(eq(sender), eq("transfer-success"), anyMap());
    }
}
