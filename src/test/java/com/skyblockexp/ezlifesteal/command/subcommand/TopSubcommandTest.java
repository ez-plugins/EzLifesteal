package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TopSubcommandTest {

    @Test
    void topDisplaysHeaderAndEntries() {
        PluginAccessor plugin = Mockito.mock(PluginAccessor.class);
        LifestealManager manager = Mockito.mock(LifestealManager.class);
        MessageService msg = Mockito.mock(MessageService.class);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(msg);

        LifestealProfile p1 = new LifestealProfile(UUID.randomUUID(), 5.0);
        LifestealProfile p2 = new LifestealProfile(UUID.randomUUID(), 3.0);

        when(manager.loadTopProfilesAsync(anyInt())).thenReturn(CompletableFuture.completedFuture(List.of(p1, p2)));

        LifestealCommand context = Mockito.mock(LifestealCommand.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.getMainThreadExecutorPublic()).thenReturn(Runnable::run);
        when(context.formatPublic(anyDouble())).thenAnswer(inv -> String.format("%.0f", inv.getArgument(0)));

        CommandSender sender = Mockito.mock(CommandSender.class);

        TopSubcommand sub = new TopSubcommand();
        boolean result = sub.execute(sender, null, "lifesteal", new String[]{"top", "1"}, context);

        assert result;

        verify(msg, atLeastOnce()).sendMessage(eq(sender), eq("top-header"), anyMap());
        verify(msg, atLeastOnce()).sendMessage(eq(sender), eq("top-entry"), anyMap());
    }
}
