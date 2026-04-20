package com.skyblockexp.ezlifesteal.command;

import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LifestealCommandBranchesTest {

    @Test
    void topCommand_withInvalidNumber_sendsInvalidPageMessage() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService ms = mock(MessageService.class);
        when(plugin.getMessageService()).thenReturn(ms);

        LifestealCommand cmd = new LifestealCommand(plugin);

        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.top")).thenReturn(true);

        cmd.onCommand(sender, mock(Command.class), "lifesteal", new String[]{"top", "not-a-number"});

        verify(ms).sendMessage(sender, "top-invalid-page");
    }

    @Test
    void topCommand_withZeroPage_sendsInvalidPageMessage() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService ms = mock(MessageService.class);
        when(plugin.getMessageService()).thenReturn(ms);

        LifestealCommand cmd = new LifestealCommand(plugin);

        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.top")).thenReturn(true);

        cmd.onCommand(sender, mock(Command.class), "lifesteal", new String[]{"top", "0"});

        verify(ms).sendMessage(sender, "top-invalid-page");
    }

    @Test
    void transfer_withInsufficientHearts_sendsInsufficientMessage() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        MessageService ms = mock(MessageService.class);
        PlayerLookupService pls = mock(PlayerLookupService.class);
        LifestealManager manager = mock(LifestealManager.class);
        org.bukkit.plugin.java.JavaPlugin javaPlugin = mock(org.bukkit.plugin.java.JavaPlugin.class);

        when(plugin.getMessageService()).thenReturn(ms);
        when(plugin.getPlayerLookupService()).thenReturn(pls);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getPlugin()).thenReturn(javaPlugin);

        Player sender = mock(Player.class);
        UUID senderUuid = UUID.randomUUID();
        when(sender.getUniqueId()).thenReturn(senderUuid);
        when(sender.hasPermission("lifesteal.transfer")).thenReturn(true);
        when(sender.isOnline()).thenReturn(true);

        UUID targetUuid = UUID.randomUUID();
        when(pls.lookupUniqueId("targetName")).thenReturn(CompletableFuture.completedFuture(Optional.of(targetUuid)));

        LifestealProfile senderProfile = new LifestealProfile(senderUuid, 1.0);
        LifestealProfile targetProfile = new LifestealProfile(targetUuid, 0.0);
        when(manager.getLoadedProfile(senderUuid)).thenReturn(Optional.of(senderProfile));
        when(manager.getLoadedProfile(targetUuid)).thenReturn(Optional.of(targetProfile));
        when(manager.getMinHearts()).thenReturn(0.0);

        // Mock Bukkit scheduler to run tasks immediately
        try (org.mockito.MockedStatic<org.bukkit.Bukkit> bukkit
                = org.mockito.Mockito.mockStatic(org.bukkit.Bukkit.class)) {
            org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
            org.bukkit.scheduler.BukkitTask task = mock(org.bukkit.scheduler.BukkitTask.class);
            org.bukkit.Server server = mock(org.bukkit.Server.class);
            when(server.getName()).thenReturn("MockBukkit");
            bukkit.when(org.bukkit.Bukkit::getServer).thenReturn(server);
            bukkit.when(org.bukkit.Bukkit::getScheduler).thenReturn(scheduler);
            org.mockito.Mockito
                    .when(scheduler.runTask(eq(javaPlugin), org.mockito.ArgumentMatchers.any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable r = invocation.getArgument(1);
                        r.run();
                        return task;
                    });

            LifestealCommand cmd = new LifestealCommand(plugin);

            cmd.onCommand(sender, mock(Command.class), "lifesteal", new String[]{"transfer", "targetName", "2.0"});

            verify(ms).sendMessage(eq(sender), eq("transfer-insufficient-hearts"), anyMap());
        }
    }
}
