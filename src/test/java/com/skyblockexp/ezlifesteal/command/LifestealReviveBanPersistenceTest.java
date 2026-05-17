package com.skyblockexp.ezlifesteal.command;

import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.test.MockBukkitTestHelper;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
import com.skyblockexp.ezlifesteal.util.ban.PlatformBanAdapter;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LifestealReviveBanPersistenceTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkitTestHelper.startServer();
    }

    @AfterEach
    void tearDown() {
        MockBukkitTestHelper.stopServer();
    }

    @Test
    void reviveClearsRepositoryAndBukkitBanState() throws Exception {
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        PlayerLookupService lookup = mock(PlayerLookupService.class);
        MessageService messages = spy(new MessageService(""));
        BanRepository banRepository = mock(BanRepository.class);
        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);
        Command bukkitCommand = mock(Command.class);

        messages.register("revive-success", "ok");
        messages.register("no-permission", "no-permission");
        messages.register("player-not-found", "not-found");
        messages.register("storage-error", "storage-error");

        UUID targetId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(targetId, 0.0);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getPlayerLookupService()).thenReturn(lookup);
        when(plugin.getMessageService()).thenReturn(messages);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getPlugin()).thenReturn(mock(org.bukkit.plugin.java.JavaPlugin.class));
        when(plugin.getBanRepository()).thenReturn(banRepository);
        when(plugin.getBanAdapter()).thenReturn(banAdapter);
        when(manager.getDefaultHearts()).thenReturn(10.0);
        when(manager.loadProfileAsync(targetId)).thenReturn(CompletableFuture.completedFuture(profile));
        when(manager.saveProfileAsync(any(LifestealProfile.class))).thenReturn(CompletableFuture.completedFuture(null));
        when(lookup.lookupUniqueId("target")).thenReturn(CompletableFuture.completedFuture(Optional.of(targetId)));

        LifestealCommand command = new LifestealCommand(plugin);
        MessageCapturingSender sender = new MessageCapturingSender(Set.of("lifesteal.manage.modify"));

        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(offlinePlayer.getName()).thenReturn("TargetName");
        when(offlinePlayer.getUniqueId()).thenReturn(targetId);

        try (MockedStatic<Bukkit> mocked = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> Bukkit.getOfflinePlayer(targetId)).thenReturn(offlinePlayer);

            command.onCommand(sender.getProxy(), bukkitCommand, "lifesteal", new String[]{"revive", "target"});
            server.getScheduler().performTicks(5);
        }

        verify(banAdapter).removeBan(eq(targetId), any(String.class));
        verify(banRepository).removeBan(targetId);
    }
}
