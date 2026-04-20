package com.skyblockexp.ezlifesteal.killstreak;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KillStreakRewardTest {

    @Test
    void appliesCommandMessageAndEconomyBranchesAndSkipsInvalidEntries() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Logger logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);

        Economy economy = mock(Economy.class);
        EconomyResponse success = new EconomyResponse(100.0, 100.0, EconomyResponse.ResponseType.SUCCESS, null);
        Player player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);
        when(player.getName()).thenReturn("Streaker");
        when(player.getUniqueId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000201"));
        when(economy.depositPlayer(player, 100.0)).thenReturn(success);
        when(plugin.getEconomy()).thenReturn(Optional.of(economy));


        KillStreakReward reward = new KillStreakReward(
                5,
                100.0,
                Arrays.asList("say hello %player% %streak%", "", "   "),
                Arrays.asList("&aReached %streak%", "  ", "    "),
                "&6%player% hit %streak%!",
                List.of()
        );

        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            ConsoleCommandSender console = mock(ConsoleCommandSender.class);
            mockedBukkit.when(Bukkit::getConsoleSender).thenReturn(console);

            reward.apply(plugin, player);

            mockedBukkit.verify(() -> Bukkit.dispatchCommand(console, "say hello Streaker 5"));
            mockedBukkit.verify(() -> Bukkit.broadcastMessage("§6Streaker hit 5!"));
        }

        verify(player).sendMessage("§aReached 5");
        verify(economy).depositPlayer(player, 100.0);
    }

    @Test
    void doesNothingWhenPlayerOfflineOrEconomyMissingOrMoneyNonPositive() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Logger logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);

        Player offline = mock(Player.class);
        when(offline.isOnline()).thenReturn(false);

        KillStreakReward reward = new KillStreakReward(3, 0.0, List.of("cmd"), List.of("msg"), "bcast", List.of());

        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            reward.apply(plugin, offline);
            mockedBukkit.verifyNoInteractions();
        }

        verify(offline, never()).sendMessage(eq("msg"));

        Player online = mock(Player.class);
        when(online.isOnline()).thenReturn(true);
        when(online.getName()).thenReturn("NoVault");
        when(plugin.getEconomy()).thenReturn(Optional.empty());

        KillStreakReward moneyReward = new KillStreakReward(3, 25.0, List.of(), List.of(), null, List.of());
        moneyReward.apply(plugin, online);

        verify(logger).warning(org.mockito.ArgumentMatchers.contains("no Vault economy provider"));
    }
}
