package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HeartWithdrawServiceTest {

    private final HeartWithdrawService service = new HeartWithdrawService();

    // --- null-guard tests ---

    @Test
    void withdrawReturnsUnavailableWhenPlayerIsNull() {
        HeartWithdrawService.WithdrawResult result =
                service.withdraw(null, mock(LifestealManager.class), mock(HeartRegistry.class));
        assertEquals(HeartWithdrawService.Status.UNAVAILABLE, result.status());
    }

    @Test
    void withdrawReturnsUnavailableWhenManagerIsNull() {
        HeartWithdrawService.WithdrawResult result =
                service.withdraw(mock(Player.class), null, mock(HeartRegistry.class));
        assertEquals(HeartWithdrawService.Status.UNAVAILABLE, result.status());
    }

    @Test
    void withdrawReturnsUnavailableWhenRegistryIsNull() {
        HeartWithdrawService.WithdrawResult result =
                service.withdraw(mock(Player.class), mock(LifestealManager.class), null);
        assertEquals(HeartWithdrawService.Status.UNAVAILABLE, result.status());
    }

    // --- withdraw-heart resolution tests ---

    @Test
    void withdrawReturnsUnavailableWhenNoWithdrawHeartConfigured() {
        HeartRegistry registry = mock(HeartRegistry.class);
        when(registry.getById("basic")).thenReturn(null);
        when(registry.getByTier(1)).thenReturn(null);
        when(registry.getAll()).thenReturn(Collections.emptyMap());

        HeartWithdrawService.WithdrawResult result =
                service.withdraw(mock(Player.class), mock(LifestealManager.class), registry);

        assertEquals(HeartWithdrawService.Status.UNAVAILABLE, result.status());
    }

    @Test
    void withdrawReturnsUnavailableWhenWithdrawHeartHasZeroHearts() {
        HeartRegistry registry = mock(HeartRegistry.class);
        Heart zeroHeart = mock(Heart.class);
        when(zeroHeart.getHearts()).thenReturn(0.0);
        when(registry.getById("basic")).thenReturn(zeroHeart);

        HeartWithdrawService.WithdrawResult result =
                service.withdraw(mock(Player.class), mock(LifestealManager.class), registry);

        assertEquals(HeartWithdrawService.Status.UNAVAILABLE, result.status());
    }

    @Test
    void withdrawResolvesHeartByTierOneWhenBasicIdMissing() {
        HeartRegistry registry = mock(HeartRegistry.class);
        Heart tierOneHeart = basicHeart("tier-one", 1.0);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.addItem(any())).thenReturn(new java.util.HashMap<Integer, ItemStack>());
        when(registry.getById("basic")).thenReturn(null);
        when(registry.getByTier(1)).thenReturn(tierOneHeart);

        HeartWithdrawService.WithdrawResult result =
                service.withdraw(playerWith(inventory, 10.0), managerWith(10.0, 0.0), registry);

        assertEquals(HeartWithdrawService.Status.SUCCESS, result.status());
        assertEquals("tier-one", result.heartId());
    }

    @Test
    void withdrawResolvesHeartByOneHeartValueWhenNeitherBasicNorTierOneExists() {
        HeartRegistry registry = mock(HeartRegistry.class);
        Heart oneHeartItem = basicHeart("premium", 1.0);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.addItem(any())).thenReturn(new java.util.HashMap<Integer, ItemStack>());
        when(registry.getById("basic")).thenReturn(null);
        when(registry.getByTier(1)).thenReturn(null);
        when(registry.getAll()).thenReturn(Map.of("premium", oneHeartItem));

        HeartWithdrawService.WithdrawResult result =
                service.withdraw(playerWith(inventory, 10.0), managerWith(10.0, 0.0), registry);

        assertEquals(HeartWithdrawService.Status.SUCCESS, result.status());
        assertEquals("premium", result.heartId());
    }

    // --- profile state tests ---

    @Test
    void withdrawReturnsProfileMissingWhenProfileNotLoaded() {
        HeartRegistry registry = mock(HeartRegistry.class);
        Heart basic = basicHeart("basic", 1.0);
        when(registry.getById("basic")).thenReturn(basic);

        LifestealManager manager = mock(LifestealManager.class);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(manager.getLoadedProfile(any())).thenReturn(Optional.empty());

        HeartWithdrawService.WithdrawResult result = service.withdraw(player, manager, registry);

        assertEquals(HeartWithdrawService.Status.PROFILE_NOT_LOADED, result.status());
    }

    @Test
    void withdrawReturnsInsufficientWhenHeartsWouldDropBelowMinimum() {
        // Player has 1 heart, minHearts = 1.0 → withdrawing 1 would produce 0.0 which is < 1.0
        HeartRegistry registry = mock(HeartRegistry.class);
        Heart basic = basicHeart("basic", 1.0);
        when(registry.getById("basic")).thenReturn(basic);

        UUID playerId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(playerId, 1.0);

        LifestealManager manager = mock(LifestealManager.class);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(manager.getLoadedProfile(playerId)).thenReturn(Optional.of(profile));
        when(manager.getMinHearts()).thenReturn(1.0);

        HeartWithdrawService.WithdrawResult result = service.withdraw(player, manager, registry);

        assertEquals(HeartWithdrawService.Status.INSUFFICIENT_HEARTS, result.status());
        assertEquals(1.0, profile.getHearts(), "Profile must be unchanged after insufficient withdrawal");
    }

    // --- successful withdrawal tests ---

    @Test
    void withdrawSucceedsAndDecrementsProfileAndGivesVoucher() {
        HeartRegistry registry = mock(HeartRegistry.class);
        Heart heart = basicHeart("basic", 1.0);
        when(registry.getById("basic")).thenReturn(heart);

        UUID playerId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(playerId, 10.0);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.addItem(any(ItemStack.class))).thenReturn(new java.util.HashMap<Integer, ItemStack>());

        Player player = playerWith(inventory, playerId);
        LifestealManager manager = mock(LifestealManager.class);
        when(manager.getLoadedProfile(playerId)).thenReturn(Optional.of(profile));
        when(manager.getMinHearts()).thenReturn(0.0);

        HeartWithdrawService.WithdrawResult result = service.withdraw(player, manager, registry);

        assertEquals(HeartWithdrawService.Status.SUCCESS, result.status());
        assertEquals("basic", result.heartId());
        assertEquals(9.0, result.remainingHearts());
        assertEquals(9.0, profile.getHearts(), "Profile hearts must be decremented by the withdrawal amount");
        verify(manager).saveProfileAsync(profile);
        verify(manager).applyHearts(player, profile);
        verify(inventory).addItem(any(ItemStack.class));
    }

    @Test
    void withdrawDropsVoucherToWorldWhenInventoryFull() {
        HeartRegistry registry = mock(HeartRegistry.class);
        Heart heart = basicHeart("basic", 1.0);
        when(registry.getById("basic")).thenReturn(heart);

        UUID playerId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(playerId, 10.0);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.addItem(any(ItemStack.class))).thenAnswer(inv -> {
            java.util.HashMap<Integer, ItemStack> overflow = new java.util.HashMap<>();
            overflow.put(0, inv.getArgument(0));
            return overflow;
        });

        World world = mock(World.class);
        Location location = mock(Location.class);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);

        LifestealManager manager = mock(LifestealManager.class);
        when(manager.getLoadedProfile(playerId)).thenReturn(Optional.of(profile));
        when(manager.getMinHearts()).thenReturn(0.0);

        HeartWithdrawService.WithdrawResult result = service.withdraw(player, manager, registry);

        assertEquals(HeartWithdrawService.Status.SUCCESS, result.status());
        verify(world).dropItemNaturally(same(location), any(ItemStack.class));
    }

    // --- helpers ---

    private Heart basicHeart(String id, double hearts) {
        Heart heart = mock(Heart.class);
        when(heart.getId()).thenReturn(id);
        when(heart.getHearts()).thenReturn(hearts);
        when(heart.createItemStack()).thenReturn(mock(ItemStack.class));
        return heart;
    }

    private Player playerWith(PlayerInventory inventory, double ignoredHearts) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getInventory()).thenReturn(inventory);
        return player;
    }

    private Player playerWith(PlayerInventory inventory, UUID id) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.getInventory()).thenReturn(inventory);
        return player;
    }

    private LifestealManager managerWith(double profileHearts, double minHearts) {
        LifestealManager manager = mock(LifestealManager.class);
        UUID id = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(id, profileHearts);
        when(manager.getLoadedProfile(any())).thenReturn(Optional.of(profile));
        when(manager.getMinHearts()).thenReturn(minHearts);
        return manager;
    }
}
