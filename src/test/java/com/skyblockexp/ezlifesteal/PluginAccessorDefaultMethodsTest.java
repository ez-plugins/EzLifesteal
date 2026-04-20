package com.skyblockexp.ezlifesteal;

import com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.detector.AdminDetector;
import com.skyblockexp.ezlifesteal.detector.SmurfDetector;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.hologram.TopHologramManager;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakManager;
import com.skyblockexp.ezlifesteal.model.MobReward;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.storage.Storage;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginAccessorDefaultMethodsTest {

    @AfterEach
    void resetHeartKey() {
        EzLifestealPlugin.HEART_KEY = null;
    }

    @Test
    void getProfileAndBanRepositoriesDelegateToStorage() {
        Storage storage = mock(Storage.class);
        PluginAccessor accessor = new FakePluginAccessor(storage);

        ProfileRepository profileRepository = accessor.getProfileRepository();
        BanRepository banRepository = accessor.getBanRepository();

        assertSame(storage, profileRepository);
        assertSame(storage, banRepository);
    }

    @Test
    void getHeartIdFromReturnsNullForNullContainer() {
        PluginAccessor accessor = new FakePluginAccessor(mock(Storage.class));

        assertNull(accessor.getHeartIdFrom(null));
    }

    @Test
    void getHeartIdFromReturnsNullWhenHeartKeyIsNull() {
        PluginAccessor accessor = new FakePluginAccessor(mock(Storage.class));
        PersistentDataContainer container = mock(PersistentDataContainer.class);
        EzLifestealPlugin.HEART_KEY = null;

        assertNull(accessor.getHeartIdFrom(container));
    }

    @Test
    void getHeartIdFromReturnsNullWhenContainerThrows() {
        PluginAccessor accessor = new FakePluginAccessor(mock(Storage.class));
        PersistentDataContainer container = mock(PersistentDataContainer.class);
        NamespacedKey key = mock(NamespacedKey.class);
        EzLifestealPlugin.HEART_KEY = key;
        doThrow(new RuntimeException("boom")).when(container).get(eq(key), eq(PersistentDataType.STRING));

        assertNull(accessor.getHeartIdFrom(container));
    }

    @Test
    void getHeartIdFromReturnsHeartIdWhenPresent() {
        PluginAccessor accessor = new FakePluginAccessor(mock(Storage.class));
        PersistentDataContainer container = mock(PersistentDataContainer.class);
        NamespacedKey key = mock(NamespacedKey.class);
        EzLifestealPlugin.HEART_KEY = key;
        when(container.get(eq(key), eq(PersistentDataType.STRING))).thenReturn("starter_heart");

        assertSame("starter_heart", accessor.getHeartIdFrom(container));
    }

    private static final class FakePluginAccessor implements PluginAccessor {
        private final Storage storage;

        private FakePluginAccessor(Storage storage) {
            this.storage = storage;
        }

        @Override
        public Storage getStorage() {
            return storage;
        }

        @Override public JavaPlugin getPlugin() {
            return null;
        }

        @Override public Logger getLogger() {
            return null;
        }

        @Override public String getPluginName() {
            return null;
        }

        @Override public String getPluginVersion() {
            return null;
        }

        @Override public String getPluginAuthors() {
            return null;
        }

        @Override public HeartRegistry getHeartRegistry() {
            return null;
        }

        @Override public LifestealManager getLifestealManager() {
            return null;
        }

        @Override public MessageService getMessageService() {
            return null;
        }

        @Override public PlayerLookupService getPlayerLookupService() {
            return null;
        }

        @Override public TopHologramManager getTopHologramManager() {
            return null;
        }

        @Override public LifestealConfigAdapter getLifestealConfigAdapter() {
            return null;
        }

        @Override public AdminDetector getAdminDetector() {
            return null;
        }

        @Override public SmurfDetector getSmurfDetector() {
            return null;
        }

        @Override public KillStreakManager getKillStreakManager() {
            return null;
        }

        @Override public MobReward getMobReward(EntityType entityType) {
            return null;
        }

        @Override public boolean isGlobalLifestealEnabled() {
            return false;
        }

        @Override public boolean isLifestealEnabledInWorld(String worldName) {
            return false;
        }

        @Override public boolean isAdminBypassHeartLoss() {
            return false;
        }

        @Override public boolean isAdminBypassHeartGain() {
            return false;
        }

        @Override public boolean isDontRemoveHeartsFromMobs() {
            return false;
        }

        @Override public double getMobRemoveHeartsGreaterThan() {
            return 0;
        }

        @Override public double getHeartsLostOnDeath(String worldName) {
            return 0;
        }

        @Override public boolean isBanWhenZeroHearts(String worldName) {
            return false;
        }

        @Override public boolean isDropHeartOnDeath() {
            return false;
        }

        @Override public boolean isDropHeartOnlyWhenKilledByPlayer() {
            return false;
        }

        @Override public String getDropHeartId() {
            return null;
        }

        @Override public int getDropHeartAmount() {
            return 0;
        }

        @Override public boolean isReviveBeaconEnabled() {
            return false;
        }

        @Override public String getReviveBeaconVoucherHeartId() {
            return null;
        }

        @Override public boolean isReviveBeaconRequireSneak() {
            return false;
        }

        @Override public double getReviveBeaconMaxDistance() {
            return 0;
        }

        @Override public boolean isReviveBeaconConsumeOnFail() {
            return false;
        }

        @Override public double getHeartsPerKill(String worldName) {
            return 0;
        }

        @Override public void clearHeartStatus(UUID playerId) { }

        @Override public void sendHeartStatus(Player player, double hearts) { }

        @Override public void executeZeroHeartCommands(Player victim, Player killer, double remainingHearts) { }

        @Override public void requestTopHologramUpdate() { }

        @Override public void reloadPlugin(CommandSender initiator) { }

        @Override public void simulatePlayerDeath(Player victim, Player killer) { }

        @Override public void simulatePlayerKill(Player killer) { }

    }

}
