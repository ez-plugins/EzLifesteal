package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

class SmurfGuiManagerTest {

    @Test
    void openManagementConstructsMenuAndOpensIt() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Player player = mock(Player.class);

        try (MockedConstruction<SmurfManagementMenu> construction = mockConstruction(SmurfManagementMenu.class)) {
            SmurfGuiManager.openManagement(plugin, player);

            assertEquals(1, construction.constructed().size());
            verify(construction.constructed().get(0)).open();
        }
    }

    @Test
    void openTrustedConstructsMenuAndOpensIt() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Player player = mock(Player.class);

        try (MockedConstruction<SmurfTrustedMenu> construction = mockConstruction(SmurfTrustedMenu.class)) {
            SmurfGuiManager.openTrusted(plugin, player);

            assertEquals(1, construction.constructed().size());
            verify(construction.constructed().get(0)).open();
        }
    }

    @Test
    void openAddTrustedConstructsMenuAndOpensIt() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Player player = mock(Player.class);

        try (MockedConstruction<SmurfAddTrustedMenu> construction = mockConstruction(SmurfAddTrustedMenu.class)) {
            SmurfGuiManager.openAddTrusted(plugin, player);

            assertEquals(1, construction.constructed().size());
            verify(construction.constructed().get(0)).open();
        }
    }

    @Test
    void openAlertHistoryConstructsMenuAndOpensIt() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Player player = mock(Player.class);

        try (MockedConstruction<SmurfAlertHistoryMenu> construction = mockConstruction(SmurfAlertHistoryMenu.class)) {
            SmurfGuiManager.openAlertHistory(plugin, player);

            assertEquals(1, construction.constructed().size());
            verify(construction.constructed().get(0)).open();
        }
    }

    @Test
    void openKillHistoryConstructsMenuAndOpensIt() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Player player = mock(Player.class);

        try (MockedConstruction<SmurfKillHistoryMenu> construction = mockConstruction(SmurfKillHistoryMenu.class)) {
            SmurfGuiManager.openKillHistory(plugin, player);

            assertEquals(1, construction.constructed().size());
            verify(construction.constructed().get(0)).open();
        }
    }
}
