package com.skyblockexp.ezlifesteal.command;

import com.skyblockexp.ezlifesteal.command.subcommand.BeaconSubcommand;
import com.skyblockexp.ezlifesteal.config.BeaconSpawnSettings;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import com.skyblockexp.ezlifesteal.model.SpawnedBeaconStatus;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.BeaconSpawnService;
import com.skyblockexp.ezlifesteal.storage.SpawnedBeaconRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BeaconSubcommandTest {

    private ServerMock server;
    private WorldMock world;
    private PluginAccessor plugin;
    private LifestealCommand context;
    private MessageService messageService;
    private Command command;
    private BeaconSubcommand subcommand;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        plugin = mock(PluginAccessor.class);
        messageService = mock(MessageService.class);
        command = mock(Command.class);
        subcommand = new BeaconSubcommand();

        when(plugin.getMessageService()).thenReturn(messageService);
        when(messageService.getMessage(anyString())).thenReturn(null);

        // Set up default disabled beacon spawn settings
        when(plugin.getBeaconSpawnSettings()).thenReturn(BeaconSpawnSettings.disabled());
        when(plugin.getBeaconSpawnService()).thenReturn(null);

        // Whitelist methods
        when(plugin.isReviveBeaconWhitelistEnabled()).thenReturn(false);
        when(plugin.isReviveBeaconRequireVoucherInBeacon()).thenReturn(false);
        when(plugin.getReviveBeaconVoucherHoldSeconds()).thenReturn(0.0D);

        when(plugin.getPlugin()).thenReturn(MockBukkit.createMockPlugin());
        when(plugin.getLogger()).thenReturn(mock(Logger.class));

        context = new LifestealCommand(plugin);
    }

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    private Player mockPlayerWithPermission() {
        Player player = mock(Player.class);
        when(player.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(player.hasPermission("lifesteal.admin")).thenReturn(true);
        when(player.isOnline()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }

    @Test
    void execute_noPermission_returnsTrue() {
        Player player = mock(Player.class);
        when(player.hasPermission(anyString())).thenReturn(false);

        boolean result = subcommand.execute(player, command, "lifesteal",
                new String[]{"beacon"}, context);

        // Should return true (handled) even with no permission
        assert result;
    }

    @Test
    void execute_tooFewArgs_sendsUsage() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(sender.hasPermission("lifesteal.admin")).thenReturn(true);

        subcommand.execute(sender, command, "lifesteal", new String[]{"beacon"}, context);

        verify(sender).sendMessage(anyString());
    }

    @Test
    void execute_unknownAction_sendsUnknownMessage() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(sender.hasPermission("lifesteal.admin")).thenReturn(true);

        subcommand.execute(sender, command, "lifesteal", new String[]{"beacon", "unknown"}, context);

        verify(sender).sendMessage(anyString());
    }

    @Test
    void execute_spawn_featureDisabled_sendsDisabledMessage() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(sender.hasPermission("lifesteal.admin")).thenReturn(true);

        subcommand.execute(sender, command, "lifesteal", new String[]{"beacon", "spawn"}, context);

        verify(sender).sendMessage(ChatColor.RED + "The beacon spawn feature is not enabled.");
    }

    @Test
    void execute_despawn_serviceNull_sendsNotActiveMessage() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(sender.hasPermission("lifesteal.admin")).thenReturn(true);

        subcommand.execute(sender, command, "lifesteal", new String[]{"beacon", "despawn"}, context);

        verify(sender).sendMessage(ChatColor.RED + "The beacon spawn feature is not active.");
    }

    @Test
    void execute_despawn_missingId_sendsUsage() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(sender.hasPermission("lifesteal.admin")).thenReturn(true);

        BeaconSpawnService spawnService = mock(BeaconSpawnService.class);
        when(plugin.getBeaconSpawnService()).thenReturn(spawnService);

        subcommand.execute(sender, command, "lifesteal", new String[]{"beacon", "despawn"}, context);

        verify(sender).sendMessage(ChatColor.RED + "Usage: beacon despawn <id|all>");
    }

    @Test
    void execute_despawn_all_callsDespawnAll() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(sender.hasPermission("lifesteal.admin")).thenReturn(true);

        BeaconSpawnService spawnService = mock(BeaconSpawnService.class);
        when(plugin.getBeaconSpawnService()).thenReturn(spawnService);

        subcommand.execute(sender, command, "lifesteal", new String[]{"beacon", "despawn", "all"}, context);

        verify(spawnService).despawnAll(plugin);
        verify(sender).sendMessage(ChatColor.GREEN + "Despawned all active beacons.");
    }

    @Test
    void execute_despawn_unknownId_sendsNotFound() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(sender.hasPermission("lifesteal.admin")).thenReturn(true);

        BeaconSpawnService spawnService = mock(BeaconSpawnService.class);
        when(plugin.getBeaconSpawnService()).thenReturn(spawnService);
        when(spawnService.getActiveBeacons()).thenReturn(Collections.emptyList());

        subcommand.execute(sender, command, "lifesteal", new String[]{"beacon", "despawn", "abc12345"}, context);

        verify(sender).sendMessage(ChatColor.RED + "No active beacon found with id: abc12345");
    }

    @Test
    void execute_despawn_byShortId_callsDespawnBeacon() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(sender.hasPermission("lifesteal.admin")).thenReturn(true);

        BeaconSpawnService spawnService = mock(BeaconSpawnService.class);
        when(plugin.getBeaconSpawnService()).thenReturn(spawnService);

        UUID beaconId = UUID.randomUUID();
        String shortId = beaconId.toString().substring(0, 8);
        SpawnedBeacon beacon = mock(SpawnedBeacon.class);
        when(beacon.getId()).thenReturn(beaconId);
        when(beacon.shortId()).thenReturn(shortId);
        when(spawnService.getActiveBeacons()).thenReturn(List.of(beacon));

        subcommand.execute(sender, command, "lifesteal", new String[]{"beacon", "despawn", shortId}, context);

        verify(spawnService).despawnBeacon(beaconId, plugin);
    }

    @Test
    void execute_spawns_serviceNull_sendsNotActiveMessage() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(sender.hasPermission("lifesteal.admin")).thenReturn(true);

        subcommand.execute(sender, command, "lifesteal", new String[]{"beacon", "spawns"}, context);

        verify(sender).sendMessage(ChatColor.RED + "The beacon spawn feature is not active.");
    }

    @Test
    void execute_spawns_noActiveBeacons_sendsNoBeaconsMessage() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(sender.hasPermission("lifesteal.admin")).thenReturn(true);

        BeaconSpawnService spawnService = mock(BeaconSpawnService.class);
        when(plugin.getBeaconSpawnService()).thenReturn(spawnService);
        when(spawnService.getActiveBeacons()).thenReturn(Collections.emptyList());

        subcommand.execute(sender, command, "lifesteal", new String[]{"beacon", "spawns"}, context);

        verify(sender).sendMessage(ChatColor.GRAY + "No active spawned beacons.");
    }

    @Test
    void execute_spawns_withActiveBeacons_listsThem() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(sender.hasPermission("lifesteal.admin")).thenReturn(true);

        BeaconSpawnService spawnService = mock(BeaconSpawnService.class);
        when(plugin.getBeaconSpawnService()).thenReturn(spawnService);

        UUID beaconId = UUID.randomUUID();
        SpawnedBeacon beacon = mock(SpawnedBeacon.class);
        when(beacon.shortId()).thenReturn("abcd1234");
        when(beacon.getStatus()).thenReturn(SpawnedBeaconStatus.AVAILABLE);
        Location loc = new Location(world, 10, 64, 10);
        when(beacon.getLocation()).thenReturn(loc);
        when(spawnService.getActiveBeacons()).thenReturn(List.of(beacon));

        subcommand.execute(sender, command, "lifesteal", new String[]{"beacon", "spawns"}, context);

        verify(sender).sendMessage(ChatColor.GOLD + "Active spawned beacons (1):");
    }

    @Test
    void execute_add_consoleSender_sendsLookAtBeaconMessage() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(sender.hasPermission("lifesteal.admin")).thenReturn(true);

        // Console sender → resolveBeaconLocation returns null
        subcommand.execute(sender, command, "lifesteal", new String[]{"beacon", "add"}, context);

        verify(sender).sendMessage(ChatColor.RED + "Look at a beacon block to add it to the whitelist.");
    }

    @Test
    void execute_remove_consoleSender_sendsLookAtBeaconMessage() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(sender.hasPermission("lifesteal.admin")).thenReturn(true);

        subcommand.execute(sender, command, "lifesteal", new String[]{"beacon", "remove"}, context);

        verify(sender).sendMessage(ChatColor.RED + "Look at a beacon block to remove it from the whitelist.");
    }

    @Test
    void execute_spawn_invalidWorldName_sendsWorldNotFound() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(sender.hasPermission("lifesteal.admin")).thenReturn(true);

        BeaconSpawnSettings enabled = new BeaconSpawnSettings(
                true, 10,
                BeaconSpawnSettings.WorldGuardSettings.defaults(),
                BeaconSpawnSettings.CountdownSettings.defaults(),
                BeaconSpawnSettings.RandomSpawnSettings.defaults(),
                java.util.List.of(),
                0,
                BeaconSpawnSettings.ScheduleSettings.defaults(),
                BeaconSpawnSettings.ExpirySettings.defaults(),
                BeaconSpawnSettings.AvailabilityEventSettings.defaults()
        );
        when(plugin.getBeaconSpawnSettings()).thenReturn(enabled);
        BeaconSpawnService spawnService = mock(BeaconSpawnService.class);
        when(plugin.getBeaconSpawnService()).thenReturn(spawnService);

        subcommand.execute(sender, command, "lifesteal",
                new String[]{"beacon", "spawn", "nonexistentworld", "0", "64", "0"}, context);

        verify(sender).sendMessage(ChatColor.RED + "World not found: nonexistentworld");
    }

    @Test
    void execute_spawn_invalidCoordinates_sendsInvalidCoordsMessage() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("lifesteal.manage.modify")).thenReturn(true);
        when(sender.hasPermission("lifesteal.admin")).thenReturn(true);

        BeaconSpawnSettings enabled = new BeaconSpawnSettings(
                true, 10,
                BeaconSpawnSettings.WorldGuardSettings.defaults(),
                BeaconSpawnSettings.CountdownSettings.defaults(),
                BeaconSpawnSettings.RandomSpawnSettings.defaults(),
                java.util.List.of(),
                0,
                BeaconSpawnSettings.ScheduleSettings.defaults(),
                BeaconSpawnSettings.ExpirySettings.defaults(),
                BeaconSpawnSettings.AvailabilityEventSettings.defaults()
        );
        when(plugin.getBeaconSpawnSettings()).thenReturn(enabled);
        BeaconSpawnService spawnService = mock(BeaconSpawnService.class);
        when(plugin.getBeaconSpawnService()).thenReturn(spawnService);

        subcommand.execute(sender, command, "lifesteal",
                new String[]{"beacon", "spawn", "world", "notanumber", "64", "0"}, context);

        verify(sender).sendMessage(ChatColor.RED + "Invalid coordinates. Usage: beacon spawn <world> <x> <y> <z>");
    }
}
