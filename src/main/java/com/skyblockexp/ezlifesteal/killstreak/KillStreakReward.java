package com.skyblockexp.ezlifesteal.killstreak;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.compat.AdapterSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Represents the configured rewards for reaching a specific kill streak threshold.
 */
public class KillStreakReward {

    private final int streak;

    private final double money;

    private final List<String> commands;

    private final List<String> messages;

    private final String broadcastMessage;

    private final List<ItemStack> items;


    public KillStreakReward(int streak,
                            double money,
                            List<String> commands,
                            List<String> messages,
                            String broadcastMessage,
                            List<ItemStack> items) {
        this.streak = streak;
        this.money = money;
        this.commands = commands == null ? List.of() : List.copyOf(commands);
        this.messages = messages == null ? List.of() : List.copyOf(messages);
        this.broadcastMessage = broadcastMessage == null || broadcastMessage.isBlank() ? null : broadcastMessage;
        if (items == null || items.isEmpty()) {
            this.items = List.of();
        }
        else {
            final List<ItemStack> copy = new ArrayList<>(items.size());
            for (ItemStack item : items) {
                if (item != null) {
                    copy.add(item.clone());
                }
            }
            this.items = copy.isEmpty() ? List.of() : Collections.unmodifiableList(copy);
        }
    }

    public int getStreak() {
        return streak;
    }

    public void apply(EzLifestealPlugin plugin, Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        sendMessages(player);
        applyEconomy(plugin, player);
        giveItems(plugin, player);
        runCommands(player);
        broadcast(player);
    }

    private void sendMessages(Player player) {
        if (messages.isEmpty()) {
            return;
        }
        for (String raw : messages) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            final String formatted = colourise(applyPlaceholders(raw, player));
            player.sendMessage(formatted);
        }
    }

    private void applyEconomy(EzLifestealPlugin plugin, Player player) {
        if (money <= 0.0) {
            return;
        }
        final Economy economy = plugin.getEconomy().orElse(null);
        if (economy == null) {
            plugin.getLogger().warning("Kill streak reward attempted to give " + money +
                    " currency but no Vault economy provider is available.");
            return;
        }
        final EconomyResponse response = economy.depositPlayer(player, money);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("Failed to deposit kill streak reward for " + player.getName() +
                    ": " + response.errorMessage);
        }
    }

    private void giveItems(EzLifestealPlugin plugin, Player player) {
        if (items.isEmpty()) {
            return;
        }
        for (ItemStack item : items) {
            if (item == null) {
                continue;
            }
            final var leftovers = player.getInventory().addItem(item.clone());
            if (!leftovers.isEmpty()) {
                AdapterSupport.dropItemLeftoversAtPlayer(plugin, player, leftovers);
            }
        }
    }

    private void runCommands(Player player) {
        if (commands.isEmpty()) {
            return;
        }
        for (String command : commands) {
            if (command == null || command.isBlank()) {
                continue;
            }
            final String parsed = applyPlaceholders(command, player);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    private void broadcast(Player player) {
        if (broadcastMessage == null || broadcastMessage.isBlank()) {
            return;
        }
        final String formatted = colourise(applyPlaceholders(broadcastMessage, player));
        Bukkit.broadcastMessage(formatted);
    }

    private String applyPlaceholders(String input, Player player) {
        if (input == null) {
            return "";
        }
        String result = input.replace("%player%",
                player.getName() == null ? player.getUniqueId().toString() : player.getName());
        result = result.replace("%streak%", Integer.toString(streak));
        return result;
    }

    private String colourise(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
