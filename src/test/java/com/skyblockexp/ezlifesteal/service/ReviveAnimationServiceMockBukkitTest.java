package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.ReviveAnimationSettings;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviveAnimationServiceMockBukkitTest {

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @Test
    void playReviveAnimationSchedulesAndCompletesTask() {
        ServerMock server = MockBukkit.mock();
        JavaPlugin javaPlugin = MockBukkit.createMockPlugin();
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer("reviver");
        player.teleport(new Location(world, 0.0D, 64.0D, 0.0D));

        PluginAccessor pluginAccessor = mock(PluginAccessor.class);
        when(pluginAccessor.getPlugin()).thenReturn(javaPlugin);

        ReviveAnimationSettings settings = new ReviveAnimationSettings(
                true,
                4,
                4,
                2,
                0.05D,
                ReviveAnimationSettings.defaults().spiralParticle(),
                ReviveAnimationSettings.defaults().ringParticle(),
                ReviveAnimationSettings.defaults().impactParticle(),
                ReviveAnimationSettings.defaults().loopSound(),
                ReviveAnimationSettings.defaults().impactSound()
        );

        ReviveAnimationService service = new ReviveAnimationService(pluginAccessor);
        service.playReviveAnimation(new Location(world, 0.0D, 64.0D, 0.0D), player, settings);

        assertFalse(server.getScheduler().getPendingTasks().isEmpty());
        server.getScheduler().performTicks(8);
        assertFalse(server.getScheduler().getPendingTasks().stream()
                .anyMatch(task -> javaPlugin.equals(task.getOwner())));
    }
}
