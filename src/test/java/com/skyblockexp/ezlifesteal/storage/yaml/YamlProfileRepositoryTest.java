package com.skyblockexp.ezlifesteal.storage.yaml;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlProfileRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void initCreatesDirectoryWhenMissing() throws Exception {
        File playersDir = tempDir.resolve("players").toFile();
        assertFalse(playersDir.exists());

        YamlProfileRepository repository = new YamlProfileRepository(playersDir, "players.yml");
        repository.init();

        assertTrue(playersDir.exists());
        assertTrue(playersDir.isDirectory());
    }

    @Test
    void initMigratesLegacyPlayersAndIgnoresInvalidUuids() throws Exception {
        File playersDir = tempDir.resolve("players").toFile();
        assertTrue(playersDir.mkdirs());

        UUID validOne = UUID.randomUUID();
        UUID validTwo = UUID.randomUUID();
        File legacyFile = new File(playersDir, "players.yml");

        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("players." + validOne + ".hearts", 11.5D);
        legacy.set("players." + validTwo + ".hearts", 6.0D);
        legacy.set("players.not-a-uuid.hearts", 42.0D);
        legacy.save(legacyFile);

        YamlProfileRepository repository = new YamlProfileRepository(playersDir, "players.yml");
        repository.init();

        File validOneFile = playersDir.toPath().resolve(validOne + ".yml").toFile();
        File validTwoFile = playersDir.toPath().resolve(validTwo + ".yml").toFile();
        File invalidFile = playersDir.toPath().resolve("not-a-uuid.yml").toFile();

        assertTrue(validOneFile.exists());
        assertTrue(validTwoFile.exists());
        assertFalse(invalidFile.exists());

        assertEquals(11.5D, YamlConfiguration.loadConfiguration(validOneFile).getDouble("hearts"));
        assertEquals(6.0D, YamlConfiguration.loadConfiguration(validTwoFile).getDouble("hearts"));

        assertFalse(legacyFile.exists(), "Legacy file should be moved or deleted after migration");
    }


    @Test
    void initWrapsLegacyMigrationIoFailureInStorageException() throws Exception {
        File playersDir = tempDir.resolve("players").toFile();
        assertTrue(playersDir.mkdirs());

        UUID uniqueId = UUID.randomUUID();
        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("players." + uniqueId + ".hearts", 11.5D);
        legacy.save(new File(playersDir, "players.yml"));

        File targetPath = playersDir.toPath().resolve(uniqueId + ".yml").toFile();
        assertTrue(targetPath.mkdirs(), "Pre-create UUID target path as directory so migration save fails");

        YamlProfileRepository repository = new YamlProfileRepository(playersDir, "players.yml");

        StorageException exception = assertThrows(StorageException.class, repository::init);
        assertTrue(exception.getMessage().contains("Failed to migrate legacy YAML entry"));
        assertNotNull(exception.getCause());
    }

    @Test
    void loadProfileReturnsEmptyForMissingFileOrMissingHearts() throws Exception {
        File playersDir = tempDir.resolve("players").toFile();
        YamlProfileRepository repository = new YamlProfileRepository(playersDir, "players.yml");
        repository.init();

        UUID missing = UUID.randomUUID();
        assertTrue(repository.loadProfile(missing).isEmpty());

        UUID missingHearts = UUID.randomUUID();
        File missingHeartsFile = playersDir.toPath().resolve(missingHearts + ".yml").toFile();
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("notHearts", 3.0D);
        configuration.save(missingHeartsFile);

        assertTrue(repository.loadProfile(missingHearts).isEmpty());
    }

    @Test
    void saveProfilePersistsAndCanBeReloaded() throws Exception {
        File playersDir = tempDir.resolve("players").toFile();
        YamlProfileRepository repository = new YamlProfileRepository(playersDir, "players.yml");
        repository.init();

        UUID uniqueId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(uniqueId, 13.25D);

        repository.saveProfile(profile);

        Optional<LifestealProfile> loaded = repository.loadProfile(uniqueId);
        assertTrue(loaded.isPresent());
        assertEquals(uniqueId, loaded.get().getUniqueId());
        assertEquals(13.25D, loaded.get().getHearts());
    }

    @Test
    void loadProfileSanitizesInvalidHeartsValues() throws Exception {
        File playersDir = tempDir.resolve("players").toFile();
        YamlProfileRepository repository = new YamlProfileRepository(playersDir, "players.yml");
        repository.init();

        UUID nanId = UUID.randomUUID();
        writePlayerFile(playersDir.toPath(), nanId, Double.NaN);
        assertEquals(10.0D, repository.loadProfile(nanId).orElseThrow().getHearts());

        UUID infinityId = UUID.randomUUID();
        writePlayerFile(playersDir.toPath(), infinityId, Double.POSITIVE_INFINITY);
        assertEquals(10.0D, repository.loadProfile(infinityId).orElseThrow().getHearts());

        UUID negativeId = UUID.randomUUID();
        writePlayerFile(playersDir.toPath(), negativeId, -5.0D);
        assertEquals(1.0D, repository.loadProfile(negativeId).orElseThrow().getHearts());

        UUID highId = UUID.randomUUID();
        writePlayerFile(playersDir.toPath(), highId, 999.0D);
        assertEquals(40.0D, repository.loadProfile(highId).orElseThrow().getHearts());
    }

    @Test
    void loadTopProfilesHonorsLimitFilteringAndSortOrder() throws Exception {
        File playersDir = tempDir.resolve("players").toFile();
        YamlProfileRepository repository = new YamlProfileRepository(playersDir, "players.yml");
        repository.init();

        assertTrue(repository.loadTopProfiles(0).isEmpty());
        assertTrue(repository.loadTopProfiles(-5).isEmpty());

        UUID low = UUID.randomUUID();
        UUID high = UUID.randomUUID();
        UUID mid = UUID.randomUUID();

        writePlayerFile(playersDir.toPath(), low, 2.0D);
        writePlayerFile(playersDir.toPath(), high, 20.0D);
        writePlayerFile(playersDir.toPath(), mid, 7.5D);

        Files.writeString(playersDir.toPath().resolve("notes.txt"), "ignore me");
        Files.writeString(playersDir.toPath().resolve("not-a-uuid.yml"), "hearts: 999");

        List<LifestealProfile> topTwo = repository.loadTopProfiles(2);
        assertEquals(2, topTwo.size());
        assertEquals(high, topTwo.get(0).getUniqueId());
        assertEquals(20.0D, topTwo.get(0).getHearts());
        assertEquals(mid, topTwo.get(1).getUniqueId());
        assertEquals(7.5D, topTwo.get(1).getHearts());

        List<LifestealProfile> topThree = repository.loadTopProfiles(10);
        assertEquals(3, topThree.size());
        assertEquals(List.of(high, mid, low), topThree.stream().map(LifestealProfile::getUniqueId).toList());
    }

    @Test
    void loadTopProfilesSanitizesInvalidHeartsValues() throws Exception {
        File playersDir = tempDir.resolve("players").toFile();
        YamlProfileRepository repository = new YamlProfileRepository(playersDir, "players.yml");
        repository.init();

        UUID nanId = UUID.randomUUID();
        UUID infinityId = UUID.randomUUID();
        UUID negativeId = UUID.randomUUID();
        UUID highId = UUID.randomUUID();

        writePlayerFile(playersDir.toPath(), nanId, Double.NaN);
        writePlayerFile(playersDir.toPath(), infinityId, Double.POSITIVE_INFINITY);
        writePlayerFile(playersDir.toPath(), negativeId, -2.0D);
        writePlayerFile(playersDir.toPath(), highId, 999.0D);

        List<LifestealProfile> profiles = repository.loadTopProfiles(10);
        assertEquals(4, profiles.size());
        assertEquals(40.0D, profiles.get(0).getHearts());
        assertEquals(10.0D, profiles.get(1).getHearts());
        assertEquals(10.0D, profiles.get(2).getHearts());
        assertEquals(1.0D, profiles.get(3).getHearts());
    }

    @Test
    void resetAllRewritesAllYamlPlayerFiles() throws Exception {
        File playersDir = tempDir.resolve("players").toFile();
        YamlProfileRepository repository = new YamlProfileRepository(playersDir, "players.yml");
        repository.init();

        UUID one = UUID.randomUUID();
        UUID two = UUID.randomUUID();
        writePlayerFile(playersDir.toPath(), one, 3.0D);
        writePlayerFile(playersDir.toPath(), two, 14.0D);

        repository.resetAll(8.0D);

        assertEquals(8.0D, repository.loadProfile(one).orElseThrow().getHearts());
        assertEquals(8.0D, repository.loadProfile(two).orElseThrow().getHearts());
    }

    @Test
    void saveAndResetWrapIoFailuresInStorageException() throws Exception {
        File playersDir = tempDir.resolve("players").toFile();
        YamlProfileRepository repository = new YamlProfileRepository(playersDir, "players.yml");
        repository.init();

        UUID saveFailureId = UUID.randomUUID();
        File saveFailurePath = playersDir.toPath().resolve(saveFailureId + ".yml").toFile();
        assertTrue(saveFailurePath.mkdirs(), "Pre-create a directory at target file path to force save failure");

        StorageException saveException = assertThrows(StorageException.class,
                () -> repository.saveProfile(new LifestealProfile(saveFailureId, 9.0D)));
        assertNotNull(saveException.getCause());

        File resetFailurePath = playersDir.toPath().resolve(UUID.randomUUID() + ".yml").toFile();
        assertTrue(resetFailurePath.mkdirs(), "Pre-create a .yml directory so resetAll save fails");

        StorageException resetException = assertThrows(StorageException.class, () -> repository.resetAll(10.0D));
        assertNotNull(resetException.getCause());
    }

    @Test
    void saveProfileWriteFailureKeepsOriginalFileUntouched() throws Exception {
        File playersDir = tempDir.resolve("players").toFile();
        UUID uniqueId = UUID.randomUUID();
        writePlayerFile(playersDir.toPath(), uniqueId, 5.0D);

        YamlProfileRepository repository = new YamlProfileRepository(
                playersDir,
                "players.yml",
                1.0D,
                10.0D,
                40.0D,
                (source, target) -> {
                    throw new IOException("simulated move failure");
                }
        );
        repository.init();

        StorageException exception = assertThrows(
                StorageException.class,
                () -> repository.saveProfile(new LifestealProfile(uniqueId, 12.0D))
        );
        assertNotNull(exception.getCause());

        Optional<LifestealProfile> profile = repository.loadProfile(uniqueId);
        assertTrue(profile.isPresent());
        assertEquals(5.0D, profile.orElseThrow().getHearts());
    }

    @Test
    void resetAllWriteFailureKeepsExistingProfileReadableAndUnchanged() throws Exception {
        File playersDir = tempDir.resolve("players").toFile();
        UUID uniqueId = UUID.randomUUID();
        writePlayerFile(playersDir.toPath(), uniqueId, 7.0D);

        YamlProfileRepository repository = new YamlProfileRepository(
                playersDir,
                "players.yml",
                1.0D,
                10.0D,
                40.0D,
                (source, target) -> {
                    throw new IOException("simulated move failure");
                }
        );
        repository.init();

        StorageException exception = assertThrows(StorageException.class, () -> repository.resetAll(10.0D));
        assertNotNull(exception.getCause());

        Optional<LifestealProfile> profile = repository.loadProfile(uniqueId);
        assertTrue(profile.isPresent());
        assertEquals(7.0D, profile.orElseThrow().getHearts());
    }

    @Test
    void migrateLegacyWriteFailureKeepsOriginalTargetFileUntouched() throws Exception {
        File playersDir = tempDir.resolve("players").toFile();
        assertTrue(playersDir.mkdirs());

        UUID uniqueId = UUID.randomUUID();
        writePlayerFile(playersDir.toPath(), uniqueId, 4.0D);

        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("players." + uniqueId + ".hearts", 19.0D);
        legacy.save(new File(playersDir, "players.yml"));

        YamlProfileRepository repository = new YamlProfileRepository(
                playersDir,
                "players.yml",
                1.0D,
                10.0D,
                40.0D,
                (source, target) -> {
                    throw new IOException("simulated move failure");
                }
        );

        StorageException exception = assertThrows(StorageException.class, repository::init);
        assertNotNull(exception.getCause());

        Optional<LifestealProfile> profile = repository.loadProfile(uniqueId);
        assertTrue(profile.isPresent());
        assertEquals(4.0D, profile.orElseThrow().getHearts());
    }

    private static void writePlayerFile(Path playersDir, UUID uniqueId, double hearts) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("hearts", hearts);
        configuration.save(playersDir.resolve(uniqueId + ".yml").toFile());
    }
}
