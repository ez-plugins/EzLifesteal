package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GiveheartSubcommandTest {

    @Test
    void giveheartOnlineTarget_sendsGiveheartSuccessAndReceived() {
        PluginAccessor plugin = Mockito.mock(PluginAccessor.class);
        HeartRegistry registry = Mockito.mock(HeartRegistry.class);
        Heart heart = Mockito.mock(Heart.class);
        MessageService msg = Mockito.mock(MessageService.class);
        PlayerLookupService lookup = Mockito.mock(PlayerLookupService.class);
        LifestealManager manager = Mockito.mock(LifestealManager.class);

        when(plugin.getHeartRegistry()).thenReturn(registry);
        when(plugin.getMessageService()).thenReturn(msg);
        when(plugin.getLifestealManager()).thenReturn(manager);

        UUID targetId = UUID.randomUUID();

        Player giveTarget = Mockito.mock(Player.class);
        when(giveTarget.getName()).thenReturn("target");
        when(giveTarget.isOnline()).thenReturn(true);
        when(giveTarget.getInventory()).thenReturn(Mockito.mock(org.bukkit.inventory.PlayerInventory.class));

        OfflinePlayer offline = Mockito.mock(OfflinePlayer.class);
        when(offline.isOnline()).thenReturn(true);
        when(offline.getPlayer()).thenReturn(giveTarget);

        when(lookup.lookupUniqueId(anyString())).thenReturn(CompletableFuture.completedFuture(Optional.of(targetId)));
        when(registry.getById("ruby")).thenReturn(heart);
        when(heart.getId()).thenReturn("ruby");
        when(heart.createItemStack()).thenReturn(Mockito.mock(ItemStack.class));

        LifestealCommand context = Mockito.mock(LifestealCommand.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.getPlayerLookupServicePublic()).thenReturn(lookup);
        when(context.getMainThreadExecutorPublic()).thenReturn(Runnable::run);

        GiveheartSubcommand sub = new GiveheartSubcommand();

        try (MockedStatic<Bukkit> mocked = Mockito.mockStatic(Bukkit.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> Bukkit.getOfflinePlayer(eq(targetId))).thenReturn(offline);

            boolean result = sub.execute(Mockito.mock(org.bukkit.command.CommandSender.class),
                    Mockito
                            .mock(Command.class), "lifesteal", new String[]{"giveheart", "target", "ruby", "2"},
                            context);

            assert result;

            verify(msg).sendMessage(eq(giveTarget), eq("giveheart-received"), anyMap());
            verify(msg).sendMessage(any(), eq("giveheart-success"), anyMap());
        }
    }
}
