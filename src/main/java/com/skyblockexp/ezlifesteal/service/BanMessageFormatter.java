package com.skyblockexp.ezlifesteal.service;

import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class BanMessageFormatter {

    public BanMessages formatMessages(String banMessageTemplate,
                                      String kickMessageTemplate,
                                      Player victim,
                                      Player killer,
                                      double remainingHearts) {
        String formattedBanMessage = formatTemplate(banMessageTemplate, victim, killer, remainingHearts);
        String formattedKickMessage = formatTemplate(kickMessageTemplate, victim, killer, remainingHearts);
        if (formattedBanMessage.isEmpty() && formattedKickMessage.isEmpty()) {
            formattedBanMessage = ChatColor.translateAlternateColorCodes('&', "You have run out of hearts.");
            formattedKickMessage = formattedBanMessage;
        }
        else if (formattedBanMessage.isEmpty()) {
            formattedBanMessage = formattedKickMessage;
        }
        else if (formattedKickMessage.isEmpty()) {
            formattedKickMessage = formattedBanMessage;
        }
        return new BanMessages(formattedBanMessage, formattedKickMessage);
    }

    public String formatTemplate(String template, Player victim, Player killer, double remainingHearts) {
        if (template == null || template.isBlank()) {
            return "";
        }
        String result = template;
        final String victimName = victim.getName() == null ? victim.getUniqueId().toString() : victim.getName();
        result = result.replace("%victim%", victimName);
        result = result.replace("%player%", victimName);
        String killerName = "";
        if (killer != null) {
            killerName = killer.getName() == null ? killer.getUniqueId().toString() : killer.getName();
        }
        result = result.replace("%killer%", killerName);
        final String formattedHearts = formatHearts(remainingHearts);
        result = result.replace("%remaining_hearts%", formattedHearts);
        result = result.replace("%hearts%", formattedHearts);
        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private String formatHearts(double value) {
        return value % 1 == 0 ? Integer.toString((int) value) : String.format(Locale.US, "%.1f", value);
    }

    public record BanMessages(String banMessage, String kickMessage) {
    }
}
