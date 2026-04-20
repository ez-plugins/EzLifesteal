package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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

class ResetAllSubcommandTest {

    @Test
    void permissionDenied_returnsEarlyWithoutResetCall() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        CommandSender sender = mock(CommandSender.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(false);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(plugin.getLifestealManager()).thenReturn(manager);

        new ResetAllSubcommand().execute(sender, null, "lifesteal", new String[]{"resetall"}, context);

        verify(manager, never()).resetAllHeartsAsync();
    }

    @Test
    void success_resetsAndSendsConfirmation() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        MessageService messageService = mock(MessageService.class);
        CommandSender sender = mock(CommandSender.class);
        Player online = mock(Player.class);
        UUID onlineId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(onlineId, 7.0);

        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.getMainThreadExecutorPublic()).thenReturn(Runnable::run);
        when(context.formatPublic(anyDouble())).thenReturn("7");
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(manager.resetAllHeartsAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(manager.getDefaultHearts()).thenReturn(7.0);
        when(online.getUniqueId()).thenReturn(onlineId);
        when(manager.getLoadedProfile(onlineId)).thenReturn(Optional.of(profile));

        try (MockedStatic<Bukkit> mocked = mockStatic(Bukkit.class)) {
            mocked.when(Bukkit::getOnlinePlayers).thenReturn(List.of(online));
            new ResetAllSubcommand().execute(sender, null, "lifesteal", new String[]{"resetall"}, context);
        }

        verify(manager).resetAllHeartsAsync();
        verify(manager).applyHearts(online, profile);
        verify(plugin).requestTopHologramUpdate();
        verify(messageService).sendMessage(eq(sender), eq("reset-all-hearts"), anyMap());
    }
}
