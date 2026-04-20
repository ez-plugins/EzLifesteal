package com.skyblockexp.ezlifesteal.command;

import com.skyblockexp.ezlifesteal.service.BeaconReviveService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviveCommandTest {

    @Test
    void onCommandSelectsTargetWhenPlayerAndValidArguments() {
        BeaconReviveService beaconReviveService = mock(BeaconReviveService.class);
        ReviveCommand reviveCommand = new ReviveCommand(beaconReviveService);
        Player player = mock(Player.class);
        Command command = mock(Command.class);

        when(command.getName()).thenReturn("revive");
        reviveCommand.onCommand(player, command, "revive", new String[]{"TargetPlayer"});

        verify(beaconReviveService).selectReviveTarget(player, "TargetPlayer");
    }

    @Test
    void onCommandSendsUsageInstructionsForInvalidArguments() {
        BeaconReviveService beaconReviveService = mock(BeaconReviveService.class);
        ReviveCommand reviveCommand = new ReviveCommand(beaconReviveService);
        Player player = mock(Player.class);
        Command command = mock(Command.class);

        reviveCommand.onCommand(player, command, "revive", new String[0]);

        verify(beaconReviveService).sendReviveUsageInstructions(player);
    }

    @Test
    void onCommandRejectsNonPlayerSender() {
        BeaconReviveService beaconReviveService = mock(BeaconReviveService.class);
        ReviveCommand reviveCommand = new ReviveCommand(beaconReviveService);
        CommandSender sender = mock(CommandSender.class);
        Command command = mock(Command.class);

        reviveCommand.onCommand(sender, command, "revive", new String[]{"Target"});

        verify(sender).sendMessage(org.mockito.ArgumentMatchers.contains("Only players"));
    }
}
