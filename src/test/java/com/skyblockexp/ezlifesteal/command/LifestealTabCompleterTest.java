package com.skyblockexp.ezlifesteal.command;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LifestealTabCompleterTest {

    @Test
    void returnsEmptyListWhenArgsAreNull() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        LifestealTabCompleter completer = new LifestealTabCompleter(plugin);

        List<String> result = completer.onTabComplete(mock(CommandSender.class), mock(Command.class), "lifesteal",
                null);

        assertEquals(List.of(), result);
    }

    @Test
    void firstArgumentUsesAllowedSubcommandsForEmptyAndSingleArgPaths() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        LifestealTabCompleter completer = new LifestealTabCompleter(plugin);
        CommandSender sender = mock(CommandSender.class);

        try (MockedStatic<LifestealCommand> mocked = org.mockito.Mockito.mockStatic(LifestealCommand.class)) {
            mocked.when(() -> LifestealCommand.listAllowedSubcommands(sender))
                    .thenReturn(List.of("hearts", "help", "shop"));

            List<String> zeroArgsResult = completer.onTabComplete(sender, mock(Command.class), "lifesteal",
                    new String[]{});
            List<String> nullArgPrefixResult = completer.onTabComplete(sender, mock(Command.class), "lifesteal",
                    new String[]{null});
            List<String> filteredResult = completer.onTabComplete(sender, mock(Command.class), "lifesteal",
                    new String[]{"He"});

            assertEquals(List.of("hearts", "help", "shop"), zeroArgsResult);
            assertEquals(List.of("hearts", "help", "shop"), nullArgPrefixResult);
            assertEquals(List.of("hearts", "help"), filteredResult);
        }
    }

    @Test
    void secondArgumentSuggestsOnlinePlayersForPlayerSubcommands() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        LifestealTabCompleter completer = new LifestealTabCompleter(plugin);

        Player alice = mock(Player.class);
        when(alice.getName()).thenReturn("Alice");
        Player bob = mock(Player.class);
        when(bob.getName()).thenReturn("Bob");

        List<String> playerSubcommands = List.of("hearts", "set", "add", "remove", "reset", "revive", "transfer",
                "giveheart");

        try (MockedStatic<Bukkit> mocked = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            mocked.when(Bukkit::getOnlinePlayers).thenReturn(Set.of(alice, bob));

            for (String subcommand : playerSubcommands) {
                List<String> result = completer.onTabComplete(
                        mock(CommandSender.class),
                        mock(Command.class),
                        "lifesteal",
                        new String[]{subcommand, "a"}
                );
                assertEquals(List.of("Alice"), result, "expected player filtering for subcommand " + subcommand);
            }
        }
    }

    @Test
    void secondArgumentHasFixedOptionsForHologramTestAndTopSubcommands() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        LifestealTabCompleter completer = new LifestealTabCompleter(plugin);

        assertEquals(
                List.of("place"),
                completer.onTabComplete(mock(CommandSender.class), mock(Command.class), "lifesteal",
                        new String[]{"hologram", "pl"})
        );
        assertEquals(
                List.of("death"),
                completer.onTabComplete(mock(CommandSender.class), mock(Command.class), "lifesteal",
                        new String[]{"test", "de"})
        );
        assertEquals(
                List.of("1", "2", "3", "4", "5"),
                completer.onTabComplete(mock(CommandSender.class), mock(Command.class), "lifesteal", new String[]{"top",
                    ""})
        );
        assertEquals(
                List.of("add"),
                completer.onTabComplete(mock(CommandSender.class), mock(Command.class), "lifesteal",
                        new String[]{"beacon", "a"})
        );
    }

    @Test
    void secondArgumentDefaultsToEmptyForUnknownSubcommand() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        LifestealTabCompleter completer = new LifestealTabCompleter(plugin);

        List<String> result = completer.onTabComplete(
                mock(CommandSender.class),
                mock(Command.class),
                "lifesteal",
                new String[]{"unknown", "anything"}
        );

        assertEquals(List.of(), result);
    }

    @Test
    void thirdArgumentForGiveheartReturnsEmptyWhenRegistryMissing() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getHeartRegistry()).thenReturn(null);
        LifestealTabCompleter completer = new LifestealTabCompleter(plugin);

        List<String> result = completer.onTabComplete(
                mock(CommandSender.class),
                mock(Command.class),
                "lifesteal",
                new String[]{"giveheart", "Alice", ""}
        );

        assertEquals(List.of(), result);
    }

    @Test
    void thirdArgumentForGiveheartReturnsHeartIdsDistinctTiersAndFiltersByPrefix() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        HeartRegistry registry = mock(HeartRegistry.class);
        when(plugin.getHeartRegistry()).thenReturn(registry);

        Heart t1a = mock(Heart.class);
        when(t1a.getTier()).thenReturn(1);
        Heart t1b = mock(Heart.class);
        when(t1b.getTier()).thenReturn(1);
        Heart t3 = mock(Heart.class);
        when(t3.getTier()).thenReturn(3);

        LinkedHashMap<String, Heart> all = new LinkedHashMap<>();
        all.put("ruby", t1a);
        all.put("onyx", t1b);
        all.put("opal", t3);
        when(registry.getAll()).thenReturn(all);

        LifestealTabCompleter completer = new LifestealTabCompleter(plugin);

        List<String> allSuggestions = completer.onTabComplete(
                mock(CommandSender.class),
                mock(Command.class),
                "lifesteal",
                new String[]{"giveheart", "Alice", ""}
        );
        List<String> filteredSuggestions = completer.onTabComplete(
                mock(CommandSender.class),
                mock(Command.class),
                "lifesteal",
                new String[]{"giveheart", "Alice", "o"}
        );

        assertEquals(List.of("ruby", "onyx", "opal", "1", "3"), allSuggestions);
        assertEquals(List.of("onyx", "opal"), filteredSuggestions);
    }

    @Test
    void unsupportedArgLengthsDefaultToEmptyList() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        LifestealTabCompleter completer = new LifestealTabCompleter(plugin);

        List<String> nonGiveheartThirdArg = completer.onTabComplete(
                mock(CommandSender.class),
                mock(Command.class),
                "lifesteal",
                new String[]{"hearts", "Alice", "extra"}
        );
        List<String> overlongArgs = completer.onTabComplete(
                mock(CommandSender.class),
                mock(Command.class),
                "lifesteal",
                new String[]{"giveheart", "Alice", "tier", "overflow"}
        );

        assertEquals(List.of(), nonGiveheartThirdArg);
        assertEquals(List.of(), overlongArgs);
    }
}
