package com.skyblockexp.ezlifesteal.hologram;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopHologramManagerBuildLinesTest {

    @Test
    void buildLines_emptyAndSingleProfile() throws Exception {
        EzLifestealPlugin plugin = Mockito.mock(EzLifestealPlugin.class);
        TopHologramManager manager = new TopHologramManager(plugin);

        Class<?> cls = TopHologramManager.class;
        Method buildLines = cls.getDeclaredMethod("buildLines", java.util.List.class);
        buildLines.setAccessible(true);

        // empty profiles -> header + empty message
        @SuppressWarnings("unchecked")
        List<String> emptyLines = (List<String>) buildLines.invoke(manager, List.of());
        assertNotNull(emptyLines);
        assertEquals(2, emptyLines.size());
        assertTrue(emptyLines.get(0).contains("Top Lifesteal"));
        assertTrue(emptyLines.get(1).toLowerCase().contains("no leaderboard"));

        // single profile -> header + one entry
        UUID id = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(id, 3.0);

        OfflinePlayer offline = Mockito.mock(OfflinePlayer.class);
        Mockito.when(offline.getName()).thenReturn("Alice");

        try (MockedStatic<Bukkit> bk = Mockito.mockStatic(Bukkit.class)) {
            bk.when(() -> Bukkit.getPlayer(id)).thenReturn(null);
            bk.when(() -> Bukkit.getOfflinePlayer(id)).thenReturn(offline);

            @SuppressWarnings("unchecked")
            List<String> lines = (List<String>) buildLines.invoke(manager, List.of(profile));
            assertNotNull(lines);
            assertEquals(2, lines.size());
            assertTrue(lines.get(1).contains("Alice"));
            assertTrue(lines.get(1).contains("3"));
        }
    }
}
