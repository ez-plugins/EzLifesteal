package com.skyblockexp.ezlifesteal.command;

import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.hologram.TopHologramManager;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.test.MockBukkitTestHelper;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
import com.skyblockexp.ezlifesteal.util.ban.BanEntryView;
import com.skyblockexp.ezlifesteal.util.ban.PlatformBanAdapter;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LifestealCommandFeatureTest {

    private ServerMock server;

    private PluginAccessor plugin;

    private LifestealManager manager;

    private PlayerLookupService lookup;

    private TopHologramManager hologramManager;

    private MessageService messages;

    private LifestealCommand command;

    private Command bukkitCommand;


    @BeforeEach

    void setUp() {

        server = MockBukkitTestHelper.startServer();

        plugin = mock(PluginAccessor.class);
        manager = mock(LifestealManager.class);
        lookup = mock(PlayerLookupService.class);
        hologramManager = mock(TopHologramManager.class);
        bukkitCommand = mock(Command.class);

        messages = spy(new MessageService(""));
        for (String key : List.of(
                "no-permission", "player-not-found", "storage-error", "modify-invalid-amount", "set-invalid-amount",
                "add-hearts", "remove-hearts", "set-hearts", "reset-hearts", "revive-success", "reset-all-hearts",
                "top-invalid-page", "top-empty", "top-no-page", "top-header", "top-entry", "top-footer",
                "top-footer-end",
                "banlist-empty", "banlist-header", "banlist-entry", "banlist-footer", "banlist-footer-end",
                "transfer-invalid-amount", "transfer-self", "transfer-insufficient-hearts", "transfer-success",
                "player-not-online", "heart-not-found", "giveheart-success", "giveheart-received",
                "hologram-placed", "hologram-place-failed", "hologram-removed", "hologram-not-found"
        )) {
            messages.register(key, key + " %player% %amount% %total% %remaining% %hearts% %page% %pages%");
        }

        when(plugin.getPlayerLookupService()).thenReturn(lookup);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(messages);
        when(plugin.getTopHologramManager()).thenReturn(hologramManager);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(plugin.getPluginName()).thenReturn("EzLifesteal");
        when(plugin.getPlugin()).thenReturn(mock(org.bukkit.plugin.java.JavaPlugin.class));
        when(plugin.getBanAdapter()).thenReturn(mock(PlatformBanAdapter.class));
        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld(any())).thenReturn(true);
        doNothing().when(plugin).requestTopHologramUpdate();
        doNothing().when(plugin).sendHeartStatus(any(Player.class), any(Double.class));
        doNothing().when(plugin).reloadPlugin(any(CommandSender.class));

        when(manager.getDefaultHearts()).thenReturn(10.0);
        when(manager.getMinHearts()).thenReturn(0.0);
        when(manager.getMaxHearts()).thenReturn(20.0);
        when(manager.resetAllHeartsAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(manager.loadTopProfilesAsync(any(Integer.class))).thenReturn(CompletableFuture.completedFuture(List.of()));
        when(manager.getLoadedProfile(any(UUID.class))).thenReturn(Optional.empty());
        when(manager.saveProfileAsync(any(LifestealProfile.class))).thenReturn(CompletableFuture.completedFuture(null));

        command = new LifestealCommand(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkitTestHelper.stopServer();
    }

    @Test
    void coversHeartsAddRemoveSetResetReviveWithSuccessDeniedUsage() {
        UUID target = UUID.randomUUID();
        when(lookup.lookupUniqueId("target")).thenReturn(CompletableFuture.completedFuture(Optional.of(target)));
        when(manager.loadProfileAsync(target))
                .thenReturn(CompletableFuture.completedFuture(new LifestealProfile(target, 10.0)));

        MessageCapturingSender denied = new MessageCapturingSender(Set.of());
        command.onCommand(denied.getProxy(), bukkitCommand, "lifesteal", new String[]{"hearts", "target"});
        command.onCommand(denied.getProxy(), bukkitCommand, "lifesteal", new String[]{"add", "target", "1"});
        command.onCommand(denied.getProxy(), bukkitCommand, "lifesteal", new String[]{"remove", "target", "1"});
        command.onCommand(denied.getProxy(), bukkitCommand, "lifesteal", new String[]{"set", "target", "1"});
        command.onCommand(denied.getProxy(), bukkitCommand, "lifesteal", new String[]{"reset", "target"});
        command.onCommand(denied.getProxy(), bukkitCommand, "lifesteal", new String[]{"revive", "target"});
        assertContains(denied.getMessages(), "no-permission");

        MessageCapturingSender usage =
                new MessageCapturingSender(Set.of("lifesteal.manage.view", "lifesteal.manage.modify"));
        command.onCommand(usage.getProxy(), bukkitCommand, "lifesteal", new String[]{"hearts"});
        command.onCommand(usage.getProxy(), bukkitCommand, "lifesteal", new String[]{"add", "target", "bad"});
        command.onCommand(usage.getProxy(), bukkitCommand, "lifesteal", new String[]{"remove"});
        command.onCommand(usage.getProxy(), bukkitCommand, "lifesteal", new String[]{"set", "target", "bad"});
        command.onCommand(usage.getProxy(), bukkitCommand, "lifesteal", new String[]{"reset"});
        command.onCommand(usage.getProxy(), bukkitCommand, "lifesteal", new String[]{"revive"});
        assertContains(usage.getMessages(), "Usage: /lifesteal hearts <player>");
        assertContains(usage.getMessages(), "modify-invalid-amount");
        assertContains(usage.getMessages(), "set-invalid-amount");

        MessageCapturingSender success =
                new MessageCapturingSender(Set.of("lifesteal.manage.view", "lifesteal.manage.modify"));
        command.onCommand(success.getProxy(), bukkitCommand, "lifesteal", new String[]{"hearts", "target"});
        command.onCommand(success.getProxy(), bukkitCommand, "lifesteal", new String[]{"add", "target", "1"});
        command.onCommand(success.getProxy(), bukkitCommand, "lifesteal", new String[]{"remove", "target", "1"});
        command.onCommand(success.getProxy(), bukkitCommand, "lifesteal", new String[]{"set", "target", "15"});
        command.onCommand(success.getProxy(), bukkitCommand, "lifesteal", new String[]{"reset", "target"});
        command.onCommand(success.getProxy(), bukkitCommand, "lifesteal", new String[]{"revive", "target"});
        runScheduler();
        verify(manager, times(5)).saveProfileAsync(any(LifestealProfile.class));
        verify(plugin, times(5)).requestTopHologramUpdate();
    }

    @Test
    void coversResetAllTopBanlistWithSuccessDeniedUsage() {
        MessageCapturingSender denied = new MessageCapturingSender(Set.of());
        command.onCommand(denied.getProxy(), bukkitCommand, "lifesteal", new String[]{"resetall"});
        command.onCommand(denied.getProxy(), bukkitCommand, "lifesteal", new String[]{"top"});
        command.onCommand(denied.getProxy(), bukkitCommand, "lifesteal", new String[]{"banlist"});
        assertContains(denied.getMessages(), "no-permission");

        MessageCapturingSender usage = new MessageCapturingSender(
                Set.of("lifesteal.manage.resetall", "lifesteal.top", "lifesteal.admin.banlist"));
        command.onCommand(usage.getProxy(), bukkitCommand, "lifesteal", new String[]{"top", "0"});
        command.onCommand(usage.getProxy(), bukkitCommand, "lifesteal", new String[]{"banlist", "bad"});
        assertContains(usage.getMessages(), "top-invalid-page");

        UUID topUuid = UUID.randomUUID();
        when(manager.loadTopProfilesAsync(eq(10)))
                .thenReturn(CompletableFuture.completedFuture(List.of(new LifestealProfile(topUuid, 20.0))));

        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);
        when(plugin.getBanAdapter()).thenReturn(banAdapter);
        when(banAdapter.getBanEntries()).thenReturn(Set.of(
                new BanEntryView(UUID.randomUUID(), "BannedGuy", "reason", "EzLifesteal", new Date(), null)
        ));

        try (MockedStatic<Bukkit> mocked
                = org.mockito.Mockito.mockStatic(Bukkit.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            MessageCapturingSender success = new MessageCapturingSender(
                    Set.of("lifesteal.manage.resetall", "lifesteal.top", "lifesteal.admin.banlist"));
            command.onCommand(success.getProxy(), bukkitCommand, "lifesteal", new String[]{"resetall"});
            command.onCommand(success.getProxy(), bukkitCommand, "lifesteal", new String[]{"top", "1"});
            command.onCommand(success.getProxy(), bukkitCommand, "lifesteal", new String[]{"banlist", "1"});
            runScheduler();
            assertContains(success.getMessages(), "top-header");
            assertContains(success.getMessages(), "banlist-header");
        }
    }

    @Test
    void coversTransferAndGiveheartWithSuccessDeniedUsage() {
        Player transferSender = mock(Player.class);
        UUID senderId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(transferSender.hasPermission("lifesteal.transfer")).thenReturn(true);
        when(transferSender.hasPermission("lifesteal.admin")).thenReturn(false);
        when(transferSender.getUniqueId()).thenReturn(senderId);
        when(transferSender.isOnline()).thenReturn(true);
        when(transferSender.getName()).thenReturn("sender");

        when(lookup.lookupUniqueId("target")).thenReturn(CompletableFuture.completedFuture(Optional.of(targetId)));
        when(manager.getLoadedProfile(senderId)).thenReturn(Optional.of(new LifestealProfile(senderId, 10.0)));
        when(manager.getLoadedProfile(targetId)).thenReturn(Optional.of(new LifestealProfile(targetId, 5.0)));

        MessageCapturingSender denied = new MessageCapturingSender(Set.of());
        command.onCommand(denied.getProxy(), bukkitCommand, "lifesteal", new String[]{"giveheart", "target", "ruby"});
        assertContains(denied.getMessages(), "no-permission");

        MessageCapturingSender transferNonPlayer = new MessageCapturingSender(Set.of("lifesteal.transfer"));
        command.onCommand(transferNonPlayer.getProxy(), bukkitCommand, "lifesteal", new String[]{"transfer", "target",
            "1"});
        assertContains(transferNonPlayer.getMessages(), "This command can only be used by players.");

        command.onCommand(transferSender, bukkitCommand, "lifesteal", new String[]{"transfer", "target", "bad"});
        verify(plugin.getMessageService()).sendMessage(eq(transferSender), eq("transfer-invalid-amount"));
        command.onCommand(transferSender, bukkitCommand, "lifesteal", new String[]{"transfer", "target", "2"});
        runScheduler();
        verify(manager, times(2)).saveProfileAsync(any(LifestealProfile.class));

        HeartRegistry registry = mock(HeartRegistry.class);
        Heart heart = mock(Heart.class);
        Player giveTarget = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        World world = mock(World.class);
        when(giveTarget.getName()).thenReturn("target");
        when(giveTarget.isOnline()).thenReturn(true);
        when(giveTarget.getInventory()).thenReturn(inventory);
        when(giveTarget.getWorld()).thenReturn(world);
        when(giveTarget.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(inventory.addItem(any(ItemStack.class))).thenReturn(new java.util.HashMap<>());

        when(lookup.lookupUniqueId("target")).thenReturn(CompletableFuture.completedFuture(Optional.of(targetId)));
        when(plugin.getHeartRegistry()).thenReturn(registry);
        when(registry.getById("ruby")).thenReturn(heart);
        when(heart.getId()).thenReturn("ruby");
        when(heart.createItemStack()).thenReturn(new ItemStack(org.bukkit.Material.NETHER_STAR));

        try (MockedStatic<Bukkit> mocked
                = org.mockito.Mockito.mockStatic(Bukkit.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> Bukkit.getPlayer(targetId)).thenReturn(giveTarget);
            MessageCapturingSender usage = new MessageCapturingSender(Set.of("lifesteal.manage.modify"));
            command.onCommand(usage.getProxy(), bukkitCommand, "lifesteal", new String[]{"giveheart", "target"});
            assertContains(usage.getMessages(), "Usage: /lifesteal giveheart <player> <heartId|tier> [amount]");

            MessageCapturingSender success = new MessageCapturingSender(Set.of("lifesteal.manage.modify"));
            command.onCommand(success.getProxy(), bukkitCommand, "lifesteal", new String[]{"giveheart", "target",
                "ruby", "2"});
            runScheduler();
            assertContains(success.getMessages(), "giveheart-success");
        }
    }

    @Test
    void coversShopSmurfReloadTestAndHologramIncludingNestedActions() {
        MessageCapturingSender shopDenied = new MessageCapturingSender();
        command.onCommand(shopDenied.getProxy(), bukkitCommand, "lifesteal", new String[]{"shop"});
        assertContains(shopDenied.getMessages(), "This command can only be used by players.");

        Player player = mock(Player.class);
        when(player.hasPermission("lifesteal.smurf.manage")).thenReturn(true);
        when(player.hasPermission("lifesteal.reload")).thenReturn(true);
        when(player.hasPermission("lifesteal.test")).thenReturn(true);
        when(player.hasPermission("lifesteal.scoreboard.place")).thenReturn(true);
        when(player.hasPermission("lifesteal.scoreboard.remove")).thenReturn(true);
        when(player.hasPermission("lifesteal.admin")).thenReturn(false);
        World playerWorld = mock(World.class);
        when(player.getWorld()).thenReturn(playerWorld);
        when(player.getLocation()).thenReturn(new Location(playerWorld, 10, 64, 10));
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("actor");

        command.onCommand(player, bukkitCommand, "lifesteal", new String[]{"shop"});

        Player smurfDeniedPlayer = mock(Player.class);
        when(smurfDeniedPlayer.hasPermission(anyString())).thenReturn(false);
        command.onCommand(smurfDeniedPlayer, bukkitCommand, "lifesteal", new String[]{"smurf"});
        verify(plugin.getMessageService()).sendMessage(eq(smurfDeniedPlayer), eq("no-permission"));
        command.onCommand(player, bukkitCommand, "lifesteal", new String[]{"smurf"});

        MessageCapturingSender reloadDenied = new MessageCapturingSender(Set.of());
        command.onCommand(reloadDenied.getProxy(), bukkitCommand, "lifesteal", new String[]{"reload"});
        assertContains(reloadDenied.getMessages(), "no-permission");
        command.onCommand(player, bukkitCommand, "lifesteal", new String[]{"reload"});
        runScheduler();
        verify(plugin).reloadPlugin(player);

        MessageCapturingSender testDenied = new MessageCapturingSender(Set.of());
        command.onCommand(testDenied.getProxy(), bukkitCommand, "lifesteal", new String[]{"test", "kill", "a"});
        assertContains(testDenied.getMessages(), "no-permission");

        MessageCapturingSender testUsage = new MessageCapturingSender(Set.of("lifesteal.test"));
        command.onCommand(testUsage.getProxy(), bukkitCommand, "lifesteal", new String[]{"test"});
        command.onCommand(testUsage.getProxy(), bukkitCommand, "lifesteal", new String[]{"test", "kill"});
        command.onCommand(testUsage.getProxy(), bukkitCommand, "lifesteal", new String[]{"test", "death"});
        assertContains(testUsage.getMessages(), "Usage: /lifesteal test <kill|death> ...");

        Player killer = server.addPlayer("killer");
        Player victim = server.addPlayer("victim");
        command.onCommand(testUsage.getProxy(), bukkitCommand, "lifesteal", new String[]{"test", "kill", "killer",
            "victim"});
        command.onCommand(testUsage.getProxy(), bukkitCommand, "lifesteal", new String[]{"test", "death", "victim",
            "killer"});
        verify(plugin, times(2)).simulatePlayerDeath(any(Player.class), any(Player.class));
        verify(plugin, never()).simulatePlayerKill(any(Player.class));

        MessageCapturingSender holoUsage =
                new MessageCapturingSender(Set.of("lifesteal.scoreboard.place", "lifesteal.scoreboard.remove"));
        command.onCommand(holoUsage.getProxy(), bukkitCommand, "lifesteal", new String[]{"hologram"});
        assertContains(holoUsage.getMessages(), "Usage: /lifesteal hologram <place|remove>");

        Player placeDenied = mock(Player.class);
        when(placeDenied.hasPermission(anyString())).thenReturn(false);
        command.onCommand(placeDenied, bukkitCommand, "lifesteal", new String[]{"hologram", "place"});
        verify(plugin.getMessageService(), times(1)).sendMessage(eq(placeDenied), eq("no-permission"));

        when(hologramManager.place(any(Location.class))).thenReturn(true);
        World hologramWorld = mock(World.class);
        when(hologramWorld.getName()).thenReturn("world");
        when(hologramManager.getLocation()).thenReturn(new Location(hologramWorld, 1.5, 66, 1.5));
        command.onCommand(player, bukkitCommand, "lifesteal", new String[]{"hologram", "place"});
        verify(plugin.getMessageService()).sendMessage(eq(player), eq("hologram-placed"), any());

        MessageCapturingSender removeDenied = new MessageCapturingSender(Set.of());
        command.onCommand(removeDenied.getProxy(), bukkitCommand, "lifesteal", new String[]{"hologram", "remove"});
        assertContains(removeDenied.getMessages(), "no-permission");

        when(hologramManager.remove()).thenReturn(true);
        command.onCommand(player, bukkitCommand, "lifesteal", new String[]{"hologram", "remove"});
        verify(plugin.getMessageService()).sendMessage(eq(player), eq("hologram-removed"));

        command.onCommand(holoUsage.getProxy(), bukkitCommand, "lifesteal", new String[]{"hologram", "other"});
        assertContains(holoUsage.getMessages(), "Usage: /lifesteal hologram <place|remove>");
    }

    private void assertContains(List<String> messages, String expectedPart) {
        assertTrue(messages.stream().anyMatch(m -> m.contains(expectedPart)),
                "Expected message containing " + expectedPart + " in " + messages);
    }

    private void runScheduler() {
        server.getScheduler().performTicks(2);
    }
}
