package com.skyblockexp.ezlifesteal.storage.yaml;

import com.skyblockexp.ezlifesteal.model.TeamBankAccount;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import java.io.File;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlTeamBankRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void initCreatesDirectoryAndCanRoundTripAccount() throws Exception {
        File directory = tempDir.resolve("team-banks").toFile();
        YamlTeamBankRepository repository = new YamlTeamBankRepository(directory);
        repository.init();
        assertTrue(directory.exists());

        UUID teamId = UUID.randomUUID();
        repository.saveAccount(new TeamBankAccount(teamId, 12.5D));
        TeamBankAccount loaded = repository.loadAccount(teamId).orElseThrow();
        assertEquals(teamId, loaded.getTeamId());
        assertEquals(12.5D, loaded.getHearts());
    }

    @Test
    void saveSanitizesInvalidAndNegativeValues() throws Exception {
        File directory = tempDir.resolve("team-banks").toFile();
        YamlTeamBankRepository repository = new YamlTeamBankRepository(directory);
        repository.init();

        UUID teamId = UUID.randomUUID();
        repository.saveAccount(new TeamBankAccount(teamId, -5.0D));
        assertEquals(0.0D, repository.loadAccount(teamId).orElseThrow().getHearts());
    }

    @Test
    void saveThrowsStorageExceptionWhenTargetPathIsDirectory() throws Exception {
        File directory = tempDir.resolve("team-banks").toFile();
        YamlTeamBankRepository repository = new YamlTeamBankRepository(directory);
        repository.init();

        UUID teamId = UUID.randomUUID();
        File target = directory.toPath().resolve(teamId + ".yml").toFile();
        assertTrue(target.mkdirs());

        assertThrows(StorageException.class, () -> repository.saveAccount(new TeamBankAccount(teamId, 10.0D)));
    }
}
