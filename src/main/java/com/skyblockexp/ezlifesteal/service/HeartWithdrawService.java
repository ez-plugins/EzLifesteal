package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.compat.AdapterSupport;
import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import java.util.Map;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Handles player-driven heart withdrawals into voucher items.
 */
public class HeartWithdrawService {

    private static final String DEFAULT_WITHDRAW_HEART_ID = "basic";

    public WithdrawResult withdraw(Player player, LifestealManager manager, HeartRegistry heartRegistry) {
        if (player == null || manager == null || heartRegistry == null) {
            return WithdrawResult.unavailable();
        }

        final Heart withdrawHeart = resolveWithdrawHeart(heartRegistry);
        if (withdrawHeart == null || withdrawHeart.getHearts() <= 0) {
            return WithdrawResult.unavailable();
        }

        final Optional<LifestealProfile> profileOptional = manager.getLoadedProfile(player.getUniqueId());
        if (profileOptional.isEmpty()) {
            return WithdrawResult.profileMissing();
        }

        final LifestealProfile profile = profileOptional.get();
        final double withdrawAmount = withdrawHeart.getHearts();
        final double minHearts = manager.getMinHearts();
        if ((profile.getHearts() - withdrawAmount) < minHearts) {
            return WithdrawResult.insufficient();
        }

        profile.removeHearts(withdrawAmount, minHearts);
        manager.saveProfileAsync(profile);
        manager.applyHearts(player, profile);

        final ItemStack voucher = withdrawHeart.createItemStack();
        final Map<Integer, ItemStack> leftovers = player.getInventory().addItem(voucher);
        if (!leftovers.isEmpty()) {
            AdapterSupport.dropItemLeftoversAtPlayer(manager.getPlugin(), player, leftovers);
        }

        return WithdrawResult.success(withdrawHeart.getId(), profile.getHearts());
    }

    private Heart resolveWithdrawHeart(HeartRegistry heartRegistry) {
        final Heart byId = heartRegistry.getById(DEFAULT_WITHDRAW_HEART_ID);
        if (byId != null) {
            return byId;
        }
        final Heart byTier = heartRegistry.getByTier(1);
        if (byTier != null) {
            return byTier;
        }
        return heartRegistry.getAll().values().stream()
                .filter(heart -> Math.abs(heart.getHearts() - 1.0D) < 0.0000001D)
                .findFirst()
                .orElse(null);
    }

    public record WithdrawResult(Status status, String heartId, double remainingHearts) {
        public static WithdrawResult success(String heartId, double remainingHearts) {
            return new WithdrawResult(Status.SUCCESS, heartId, remainingHearts);
        }

        public static WithdrawResult insufficient() {
            return new WithdrawResult(Status.INSUFFICIENT_HEARTS, null, 0.0D);
        }

        public static WithdrawResult unavailable() {
            return new WithdrawResult(Status.UNAVAILABLE, null, 0.0D);
        }

        public static WithdrawResult profileMissing() {
            return new WithdrawResult(Status.PROFILE_NOT_LOADED, null, 0.0D);
        }
    }

    public enum Status {
        SUCCESS,
        INSUFFICIENT_HEARTS,
        UNAVAILABLE,
        PROFILE_NOT_LOADED
    }
}
