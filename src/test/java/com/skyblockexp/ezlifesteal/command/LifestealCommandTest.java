package com.skyblockexp.ezlifesteal.command;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LifestealCommandTest {

    @Test
    void consoleSeesSubcommandsButNotPlayerOnly() {
        LifestealCommand cmd = new LifestealCommand();

        CommandSender console = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(), new Class[]{CommandSender.class}, (proxy, method, args) -> {
                    if ("hasPermission".equals(method.getName())) {
                        return true;
                    }
                    if (method.getReturnType().equals(void.class)) {
                        return null;
                    }
                    if (method.getReturnType().isPrimitive()) {
                        return 0;
                    }
                    return null;
                });

        List<String> completions = LifestealCommand.listAllowedSubcommands(console);
        assertNotNull(completions);
        // common subcommand should be present
        assertTrue(completions.contains("hearts"), "should include 'hearts'");
        // player-only subcommands (shop, transfer, smurf) should NOT be present for non-player senders
        assertFalse(completions.contains("shop"), "console should not see 'shop'");
        assertFalse(completions.contains("transfer"), "console should not see 'transfer'");
        assertFalse(completions.contains("smurf"), "console should not see 'smurf'");

        // no global state to restore when using the test constructor
    }

    @Test
    void playerSeesAllSubcommands() {
        LifestealCommand cmd2 = new LifestealCommand();

        Player player = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(), new Class[]{Player.class}, (proxy, method, args) -> {
                    if ("hasPermission".equals(method.getName())) {
                        return true;
                    }
                    if (method.getReturnType().equals(void.class)) {
                        return null;
                    }
                    if (method.getReturnType().isPrimitive()) {
                        return 0;
                    }
                    return null;
                });

        List<String> completions = LifestealCommand.listAllowedSubcommands(player);
        assertNotNull(completions);
        assertTrue(completions.contains("hearts"), "should include 'hearts'");
        assertTrue(completions.contains("shop"), "should include 'shop' for players");
        // player-only subcommands should be present for player senders
        assertTrue(completions.contains("transfer"), "player should see 'transfer'");
        assertTrue(completions.contains("smurf"), "player should see 'smurf'");

        // no global state to restore when using the test constructor
    }

    @Test
    void banlistCommandSendsLocalizedEntries() {
        com.skyblockexp.ezlifesteal.runtime.PluginAccessor plugin =
                mock(com.skyblockexp.ezlifesteal.runtime.PluginAccessor.class);
        LifestealCommand cmd = new LifestealCommand(plugin);

        // Setup MessageService with simple messages (no prefix) so we can capture raw sends
        com.skyblockexp.ezlifesteal.config.MessageService messages =
                new com.skyblockexp.ezlifesteal.config.MessageService("");
        messages.register("banlist-header", "%count% (Page %page%/%pages%)");
        messages.register("banlist-entry", "%target% | %reason% | created=%created% | expires=%expires%");
        messages.register("banlist-footer-end", "end");
        when(plugin.getMessageService()).thenReturn(messages);
        when(plugin.getPluginName()).thenReturn("EzLifesteal");

        // Mock Ban entries
        com.destroystokyo.paper.profile.PlayerProfile profile1 =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        when(profile1.getName()).thenReturn("PlayerOne");
        BanEntry e1 = mock(BanEntry.class);
        when(e1.getSource()).thenReturn("EzLifesteal");
        when(e1.getBanTarget()).thenReturn(profile1);
        when(e1.getReason()).thenReturn("grief");
        when(e1.getCreated()).thenReturn(new Date());
        when(e1.getExpiration()).thenReturn(null);

        com.destroystokyo.paper.profile.PlayerProfile profile2 =
                mock(com.destroystokyo.paper.profile.PlayerProfile.class);
        when(profile2.getName()).thenReturn("PlayerTwo");
        BanEntry e2 = mock(BanEntry.class);
        when(e2.getSource()).thenReturn("EzLifesteal");
        when(e2.getBanTarget()).thenReturn(profile2);
        when(e2.getReason()).thenReturn("abuse");
        when(e2.getCreated()).thenReturn(new Date());
        when(e2.getExpiration()).thenReturn(null);

        BanList banList = mock(BanList.class);
        when(banList.getEntries()).thenReturn(Set.of(e1, e2));

        try (MockedStatic<Bukkit> mocked = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            mocked.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);

            MessageCapturingSender capturing = new MessageCapturingSender();
            CommandSender sender = capturing.getProxy();

            // Execute command without page (defaults to page 1)
            cmd.onCommand(sender, null, "lifesteal", new String[]{"banlist"});

            // Expect header + 2 entries + footer-end -> at least 3 sendMessage calls
            List<String> sent = capturing.getMessages();
            assertNotNull(sent);
            assertTrue(sent.size() >= 3, "expected at least header, 2 entries and footer messages");
            // ensure entries for both mocked targets are present
            assertTrue(sent.stream().anyMatch(s -> s.contains("PlayerOne")), "should contain PlayerOne entry");
            assertTrue(sent.stream().anyMatch(s -> s.contains("PlayerTwo")), "should contain PlayerTwo entry");
        }
    }

    @Test
    void shopCommandRejectsNonPlayerSenders() {
        LifestealCommand cmd = new LifestealCommand();
        MessageCapturingSender sender = new MessageCapturingSender();

        boolean handled = cmd.onCommand(sender.getProxy(), mock(Command.class), "lifesteal", new String[]{"shop"});

        assertTrue(handled, "shop command should be handled");
        assertTrue(sender.getMessages().contains("This command can only be used by players."));
    }

    @Test
    void noArgsDelegatesToHelpSubcommand() {
        SubcommandRegistry registry = new SubcommandRegistry();
        com.skyblockexp.ezlifesteal.command.subcommand.Subcommand helpSubcommand = mock(
                com.skyblockexp.ezlifesteal.command.subcommand.Subcommand.class
        );
        registry.register(
                "help",
                helpSubcommand,
                new LifestealCommand.SubcommandRequirement(false, List.of(), java.util.Collections.emptyMap())
        );
        LifestealCommand cmd = new LifestealCommand(null, registry);

        CommandSender sender = mock(CommandSender.class);
        Command command = mock(Command.class);
        when(helpSubcommand.execute(
                eq(sender),
                eq(command),
                eq("lifesteal"),
                argThat(arguments -> Arrays.equals(arguments, new String[]{"help"})),
                eq(cmd)
        ))
                .thenReturn(true);

        boolean handled = cmd.onCommand(sender, command, "lifesteal", new String[0]);

        assertTrue(handled, "base /lifesteal should be handled by help subcommand");
        verify(helpSubcommand).execute(
                eq(sender),
                eq(command),
                eq("lifesteal"),
                argThat(arguments -> Arrays.equals(arguments, new String[]{"help"})),
                eq(cmd)
        );
    }
}
