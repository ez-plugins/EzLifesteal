package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ShopGuiListenerCommandsTest {

    @Test
    void successfulPurchaseExecutesConfiguredCommands() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        var messageService = mock(com.skyblockexp.ezlifesteal.config.MessageService.class);
        when(messageService.getPrefix()).thenReturn("[EZ] ");
        when(plugin.getMessageService()).thenReturn(messageService);

        var heart = mock(Heart.class);
        when(heart.createItemStack()).thenReturn(mock(ItemStack.class));
        HeartRegistry registry = mock(HeartRegistry.class);
        when(registry.getById("basic")).thenReturn(heart);
        when(plugin.getHeartRegistry()).thenReturn(registry);

        var economy = mock(Economy.class);
        when(economy.has(any(org.bukkit.OfflinePlayer.class), anyDouble())).thenReturn(true);
        when(economy.withdrawPlayer(any(org.bukkit.OfflinePlayer.class), anyDouble()))
                .thenReturn(new EconomyResponse(1.0, 0.0, EconomyResponse.ResponseType.SUCCESS, null));
        when(plugin.getEconomy()).thenReturn(java.util.Optional.of(economy));

        var player = mock(org.bukkit.entity.Player.class);
        var world = mock(org.bukkit.World.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("Tester");

        org.bukkit.inventory.PlayerInventory inventory = mock(org.bukkit.inventory.PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new java.util.HashMap<>());

        YamlConfiguration shopCfg = mock(YamlConfiguration.class);
        when(shopCfg.getInt(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(0);
        when(shopCfg.getString(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn("&6Heart Shop");
        when(plugin.getShopConfig()).thenReturn(shopCfg);

        var event = mock(org.bukkit.event.inventory.InventoryClickEvent.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            Server server = mock(Server.class);
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(Bukkit::getConsoleSender).thenReturn(mock(org.bukkit.command.ConsoleCommandSender.class));
            bukkit
                    .when(() -> Bukkit.createInventory(
                            org.mockito.ArgumentMatchers.any(),
                            org.mockito.ArgumentMatchers.anyInt(),
                            org.mockito.ArgumentMatchers.anyString()))
                        .thenAnswer(invocation -> {
                            org.bukkit.inventory.Inventory inv = mock(org.bukkit.inventory.Inventory.class);
                            Object holder = invocation.getArgument(0);
                            when(inv.getHolder()).thenReturn((org.bukkit.inventory.InventoryHolder) holder);
                            return inv;
                        });
            bukkit
                    .when(() -> Bukkit.dispatchCommand(
                            org.mockito.ArgumentMatchers.any(),
                            org.mockito.ArgumentMatchers.anyString())).thenReturn(true);

            ShopMenu menu = new ShopMenu(plugin, player);
            when(event.getInventory()).thenReturn(menu.getInventory());

            ItemStack item = mock(ItemStack.class);
            ItemMeta meta = mock(ItemMeta.class);
            when(meta.hasLore()).thenReturn(true);
            when(meta.getLore())
                    .thenReturn(java.util.List.of(
                            "\u00A77shop-id:basic", "\u00A77shop-price:5.0",
                            "\u00A77shop-qty:1", "\u00A77shop-cmds:say hi {player};;say there"));
            when(item.getItemMeta()).thenReturn(meta);

            when(event.getCurrentItem()).thenReturn(item);

            ShopGuiListener listener = new ShopGuiListener();
            listener.onInventoryClick(event);

            bukkit.verify(() -> Bukkit.dispatchCommand(any(), any(String.class)), org.mockito.Mockito.times(2));
        }
    }
}
