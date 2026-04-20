package com.skyblockexp.ezlifesteal.service;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BanMessageFormatterTest {

    @Test
    void formatMessagesAppliesFallbackWhenTemplatesAreBlank() {
        BanMessageFormatter formatter = new BanMessageFormatter();
        Player victim = mock(Player.class);
        when(victim.getUniqueId()).thenReturn(UUID.randomUUID());
        when(victim.getName()).thenReturn("victim");

        BanMessageFormatter.BanMessages messages = formatter.formatMessages("", "", victim, null, 0.0);

        assertEquals("You have run out of hearts.", messages.banMessage());
        assertEquals("You have run out of hearts.", messages.kickMessage());
    }

    @Test
    void formatTemplateReplacesAllPlaceholders() {
        BanMessageFormatter formatter = new BanMessageFormatter();
        Player victim = mock(Player.class);
        Player killer = mock(Player.class);
        when(victim.getUniqueId()).thenReturn(UUID.randomUUID());
        when(victim.getName()).thenReturn("victim");
        when(killer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(killer.getName()).thenReturn("killer");

        String formatted = formatter.formatTemplate("%victim% %player% %killer% %remaining_hearts% %hearts%",
                victim,
                killer,
                0.5);

        assertEquals("victim victim killer 0.5 0.5", formatted);
    }
}
