package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import java.util.HashMap;
import java.util.Optional;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopGuiListenerTest {

    private ServerMock server;

    private final ShopGuiListener listener = new ShopGuiListener();


    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void clickingPurchasableItemChecksBalanceAndCancelsEvent() {
        EzLifestealPlugin plugin = basePluginWithShopConfig();
        Player player = basePlayer();
        Economy economy = mock(Economy.class);
        when(plugin.getEconomy()).thenReturn(Optional.of(economy));
        when(economy.has(player, 50.0)).thenReturn(false);

        InventoryClickEvent event = mockShopClick(plugin, player, purchaseItem("basic", 50.0, 1));

        listener.onInventoryClick(event);

        verify(event).setCancelled(true);
        verify(economy).has(player, 50.0);
        verify(economy, never()).withdrawPlayer(any(Player.class), anyDouble());
    }

    @Test
    void zeroBalanceExactBalanceAndInsufficientBalanceChecksAreApplied() {
        EzLifestealPlugin plugin = basePluginWithShopConfig();
        Player player = basePlayer();
        Economy economy = mock(Economy.class);
        when(plugin.getEconomy()).thenReturn(Optional.of(economy));

        InventoryClickEvent zeroBalanceEvent = mockShopClick(plugin, player, purchaseItem("basic", 50.0, 1));
        when(economy.has(player, 50.0)).thenReturn(false);
        listener.onInventoryClick(zeroBalanceEvent);
        verify(economy).has(player, 50.0);
        verify(economy, never()).withdrawPlayer(player, 50.0);

        reset(player, economy);
        when(plugin.getEconomy()).thenReturn(Optional.of(economy));
        when(economy.has(player, 50.0)).thenReturn(true);
        when(economy.withdrawPlayer(player, 50.0)).thenReturn(
                new EconomyResponse(50.0, 0.0, EconomyResponse.ResponseType.SUCCESS, null)
        );
        Heart exactHeart = mock(Heart.class);
        when(exactHeart.createItemStack()).thenReturn(new ItemStack(Material.NETHER_STAR));
        HeartRegistry registry = mock(HeartRegistry.class);
        when(registry.getById("basic")).thenReturn(exactHeart);
        when(plugin.getHeartRegistry()).thenReturn(registry);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());

        InventoryClickEvent exactBalanceEvent = mockShopClick(plugin, player, purchaseItem("basic", 50.0, 1));
        listener.onInventoryClick(exactBalanceEvent);
        verify(economy).has(player, 50.0);
        verify(economy).withdrawPlayer(player, 50.0);

        reset(player, economy);
        when(plugin.getEconomy()).thenReturn(Optional.of(economy));
        when(economy.has(player, 50.0)).thenReturn(false);
        InventoryClickEvent insufficientBalanceEvent = mockShopClick(plugin, player, purchaseItem("basic", 50.0, 1));
        listener.onInventoryClick(insufficientBalanceEvent);
        verify(player).sendMessage("[EZ] You do not have enough funds to purchase this item. Price: 50.");
        verify(economy, never()).withdrawPlayer(player, 50.0);
    }

    @Test
    void successPathGrantsItemAndSendsConfirmationMessage() {
        EzLifestealPlugin plugin = basePluginWithShopConfig();
        Player player = basePlayer();
        Economy economy = mock(Economy.class);
        when(plugin.getEconomy()).thenReturn(Optional.of(economy));

        when(economy.has(player, 75.0)).thenReturn(true);
        when(economy.withdrawPlayer(player, 75.0)).thenReturn(
                new EconomyResponse(75.0, 25.0, EconomyResponse.ResponseType.SUCCESS, null)
        );

        Heart heart = mock(Heart.class);
        when(heart.createItemStack()).thenReturn(new ItemStack(Material.NETHER_STAR));
        HeartRegistry registry = mock(HeartRegistry.class);
        when(registry.getById("basic")).thenReturn(heart);
        when(plugin.getHeartRegistry()).thenReturn(registry);

        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());

        InventoryClickEvent event = mockShopClick(plugin, player, purchaseItem("basic", 75.0, 3));

        listener.onInventoryClick(event);

        verify(economy).has(player, 75.0);
        verify(economy).withdrawPlayer(player, 75.0);
        org.mockito.ArgumentCaptor<ItemStack[]> addedItems = org.mockito.ArgumentCaptor.forClass(ItemStack[].class);
        verify(inventory).addItem(addedItems.capture());
        org.bukkit.inventory.ItemStack granted = addedItems.getValue()[0];
        org.junit.jupiter.api.Assertions.assertEquals(Material.NETHER_STAR, granted.getType());
        org.junit.jupiter.api.Assertions.assertEquals(3, granted.getAmount());
        verify(player).sendMessage("[EZ] Purchase successful for 75.");
    }

    @Test
    void failurePathForInsufficientFundsAndInvalidItemSendsMessageAndMakesNoChanges() {
        EzLifestealPlugin plugin = basePluginWithShopConfig();
        Player player = basePlayer();
        Economy economy = mock(Economy.class);
        when(plugin.getEconomy()).thenReturn(Optional.of(economy));

        InventoryClickEvent noFundsEvent = mockShopClick(plugin, player, purchaseItem("basic", 200.0, 1));
        when(economy.has(player, 200.0)).thenReturn(false);

        listener.onInventoryClick(noFundsEvent);

        verify(player).sendMessage("[EZ] You do not have enough funds to purchase this item. Price: 200.");
        verify(economy, never()).withdrawPlayer(player, 200.0);

        reset(player, economy);
        when(plugin.getEconomy()).thenReturn(Optional.of(economy));
        when(economy.has(player, 125.0)).thenReturn(true);
        when(economy.withdrawPlayer(player, 125.0)).thenReturn(
                new EconomyResponse(125.0, 0.0, EconomyResponse.ResponseType.SUCCESS, null)
        );

        HeartRegistry registry = mock(HeartRegistry.class);
        when(registry.getById("missing-heart")).thenReturn(null);
        when(plugin.getHeartRegistry()).thenReturn(registry);

        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());

        InventoryClickEvent invalidItemEvent = mockShopClick(plugin, player, purchaseItem("missing-heart", 125.0, 1));

        listener.onInventoryClick(invalidItemEvent);

        verify(player).sendMessage("[EZ] This item is no longer available.");
        verify(inventory, never()).addItem(any(ItemStack.class));
    }

    @Test
    void negativeAndInvalidPricesAreGuardedBeforeEconomyCalls() {
        EzLifestealPlugin plugin = basePluginWithShopConfig();
        Player player = basePlayer();
        Economy economy = mock(Economy.class);
        when(plugin.getEconomy()).thenReturn(Optional.of(economy));

        InventoryClickEvent negativePriceEvent = mockShopClick(plugin, player, purchaseItem("basic", -1.0, 1));
        listener.onInventoryClick(negativePriceEvent);
        verify(player).sendMessage("[EZ] This item has an invalid price and cannot be purchased.");
        verify(economy, never()).has(any(Player.class), anyDouble());
        verify(economy, never()).withdrawPlayer(any(Player.class), anyDouble());

        reset(player, economy);
        when(plugin.getEconomy()).thenReturn(Optional.of(economy));
        InventoryClickEvent invalidPriceEvent = mockShopClick(plugin, player,
                purchaseItemWithRawPrice("basic", "NaN??", 1));
        listener.onInventoryClick(invalidPriceEvent);
        verify(player).sendMessage("[EZ] This item has an invalid price and cannot be purchased.");
        verify(economy, never()).has(any(Player.class), anyDouble());
        verify(economy, never()).withdrawPlayer(any(Player.class), anyDouble());
    }

    @Test
    void providerUnavailableMidOperationShowsCurrencyUnavailableMessage() {
        EzLifestealPlugin plugin = basePluginWithShopConfig();
        Player player = basePlayer();
        Economy economy = mock(Economy.class);
        when(plugin.getEconomy()).thenReturn(Optional.of(economy));
        when(economy.has(player, 30.0)).thenThrow(new IllegalStateException("provider unloaded"));

        InventoryClickEvent event = mockShopClick(plugin, player, purchaseItem("basic", 30.0, 1));
        listener.onInventoryClick(event);

        verify(player).sendMessage("[EZ] Currency support is unavailable on this server.");
        verify(economy, never()).withdrawPlayer(any(Player.class), anyDouble());
    }

    @Test
    void malformedLoreLinesAreHandledSafelyWithoutExecutingCommands() {
        EzLifestealPlugin plugin = basePluginWithShopConfig();
        Player player = basePlayer();
        Economy economy = mock(Economy.class);
        when(plugin.getEconomy()).thenReturn(Optional.of(economy));

        try (org.mockito.MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            mockedBukkit.when(Bukkit::getConsoleSender).thenReturn(server.getConsoleSender());

            InventoryClickEvent invalidPriceEvent = mockShopClick(
                    plugin,
                    player,
                    purchaseItemWithLore("§7shop-id:basic", "§7shop-price:not-a-number", "§7shop-qty:1",
                            "§7shop-cmds:say bad")
            );
            listener.onInventoryClick(invalidPriceEvent);
            verify(player).sendMessage("[EZ] This item has an invalid price and cannot be purchased.");
            verify(economy, never()).has(any(Player.class), anyDouble());
            verify(economy, never()).withdrawPlayer(any(Player.class), anyDouble());
            mockedBukkit.verify(() -> Bukkit.dispatchCommand(any(), anyString()), never());

            reset(player, economy);
            when(plugin.getEconomy()).thenReturn(Optional.of(economy));
            when(economy.has(player, 10.0)).thenReturn(true);
            when(economy.withdrawPlayer(player, 10.0)).thenReturn(
                    new EconomyResponse(10.0, 0.0, EconomyResponse.ResponseType.SUCCESS, null)
            );
            Heart heart = mock(Heart.class);
            when(heart.createItemStack()).thenReturn(new ItemStack(Material.NETHER_STAR));
            HeartRegistry registry = mock(HeartRegistry.class);
            when(registry.getById("basic")).thenReturn(heart);
            when(plugin.getHeartRegistry()).thenReturn(registry);
            PlayerInventory inventory = mock(PlayerInventory.class);
            when(player.getInventory()).thenReturn(inventory);
            when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());

            InventoryClickEvent invalidQuantityAndBlankCommandEvent = mockShopClick(
                    plugin,
                    player,
                    purchaseItemWithLore("§7shop-id:basic", "§7shop-price:10.0", "§7shop-qty:not-an-int",
                            "§7shop-cmds:  ")
            );
            listener.onInventoryClick(invalidQuantityAndBlankCommandEvent);
            verify(economy).has(player, 10.0);
            verify(economy).withdrawPlayer(player, 10.0);
            verify(player).sendMessage("[EZ] Purchase successful for 10.");
            mockedBukkit.verify(() -> Bukkit.dispatchCommand(any(), anyString()), never());

            reset(player, economy, inventory, registry, heart);
            when(plugin.getEconomy()).thenReturn(Optional.of(economy));

            InventoryClickEvent missingMetadataLinesEvent = mockShopClick(
                    plugin,
                    player,
                    purchaseItemWithLore("§7display:basic", "§7cost:10", "§7amount:1", "§7commands:say hi")
            );
            listener.onInventoryClick(missingMetadataLinesEvent);
            verify(economy, never()).has(any(Player.class), anyDouble());
            verify(economy, never()).withdrawPlayer(any(Player.class), anyDouble());
            mockedBukkit.verify(() -> Bukkit.dispatchCommand(any(), anyString()), never());
        }
    }

    @Test
    void purchasePriceIsRoundedToTwoDecimalsBeforeEconomyChecks() {
        EzLifestealPlugin plugin = basePluginWithShopConfig();
        Player player = basePlayer();
        Economy economy = mock(Economy.class);
        when(plugin.getEconomy()).thenReturn(Optional.of(economy));

        when(economy.has(player, 12.35)).thenReturn(true);
        when(economy.withdrawPlayer(player, 12.35)).thenReturn(
                new EconomyResponse(12.35, 0.0, EconomyResponse.ResponseType.FAILURE, "simulated")
        );

        InventoryClickEvent event = mockShopClick(plugin, player, purchaseItem("basic", 12.345, 1));
        listener.onInventoryClick(event);

        verify(economy).has(player, 12.35);
        verify(economy).withdrawPlayer(player, 12.35);
        verify(player).sendMessage("[EZ] Failed to withdraw funds for purchase of 12.35.");
    }

    @Test
    void nonShopInventoryClicksAreIgnored() {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        Inventory inventory = mock(Inventory.class);
        when(event.getInventory()).thenReturn(inventory);
        when(inventory.getHolder()).thenReturn(null);

        listener.onInventoryClick(event);

        verify(event, never()).setCancelled(anyBoolean());
    }

    private EzLifestealPlugin basePluginWithShopConfig() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("size", 9);
        config.set("title", "&6Shop");
        config.set("items", java.util.List.of());
        when(plugin.getShopConfig()).thenReturn(config);

        MessageService messageService = mock(MessageService.class);
        when(messageService.getPrefix()).thenReturn("[EZ] ");
        when(plugin.getMessageService()).thenReturn(messageService);

        HeartRegistry registry = mock(HeartRegistry.class);
        when(plugin.getHeartRegistry()).thenReturn(registry);
        return plugin;
    }

    private Player basePlayer() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("Tester");
        when(player.getLocation()).thenReturn(new org.bukkit.Location(world, 0, 64, 0));
        return player;
    }

    private InventoryClickEvent mockShopClick(EzLifestealPlugin plugin, Player player, ItemStack clickedItem) {
        ShopMenu menu = new ShopMenu(plugin, player);
        Inventory inventory = menu.getInventory();

        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getCurrentItem()).thenReturn(clickedItem);
        return event;
    }

    private ItemStack purchaseItem(String heartId, double price, int quantity) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(java.util.List.of(
                "§7shop-id:" + heartId,
                "§7shop-price:" + price,
                "§7shop-qty:" + quantity
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack purchaseItemWithRawPrice(String heartId, String rawPrice, int quantity) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(java.util.List.of(
                "§7shop-id:" + heartId,
                "§7shop-price:" + rawPrice,
                "§7shop-qty:" + quantity
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack purchaseItemWithLore(String... lore) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(java.util.List.of(lore));
        item.setItemMeta(meta);
        return item;
    }
}
