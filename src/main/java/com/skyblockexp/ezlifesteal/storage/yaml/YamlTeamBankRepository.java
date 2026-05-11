package com.skyblockexp.ezlifesteal.storage.yaml;

import com.skyblockexp.ezlifesteal.model.TeamBankAccount;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.TeamBankRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

public class YamlTeamBankRepository implements TeamBankRepository {

    private final File directory;

    public YamlTeamBankRepository(File directory) {
        this.directory = directory;
    }

    public void init() throws StorageException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new StorageException("Unable to create team bank directory at " + directory.getAbsolutePath());
        }
    }

    @Override
    public Optional<TeamBankAccount> loadAccount(UUID teamId) {
        final File accountFile = accountFile(teamId);
        if (!accountFile.exists()) {
            return Optional.empty();
        }
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(accountFile);
        if (!configuration.contains("hearts")) {
            return Optional.empty();
        }
        final double hearts = sanitize(configuration.getDouble("hearts"));
        return Optional.of(new TeamBankAccount(teamId, hearts));
    }

    @Override
    public void saveAccount(TeamBankAccount account) throws StorageException {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("hearts", sanitize(account.getHearts()));
        writeYamlAtomically(accountFile(account.getTeamId()), configuration,
                "Failed to save team bank YAML file");
    }

    private double sanitize(double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            return 0.0D;
        }
        return value;
    }

    private File accountFile(UUID teamId) {
        return new File(directory, teamId + ".yml");
    }

    private void writeYamlAtomically(File targetFile, YamlConfiguration configuration, String errorMessage)
            throws StorageException {
        final Path targetPath = targetFile.toPath();
        final Path temporaryPath = targetPath.resolveSibling(targetFile.getName() + ".tmp");
        try {
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(temporaryPath.toFile()), StandardCharsets.UTF_8)) {
                writer.write(configuration.saveToString());
                writer.flush();
            }
            move(temporaryPath, targetPath);
        }
        catch (IOException exception) {
            deleteQuietly(temporaryPath);
            throw new StorageException(errorMessage, exception);
        }
    }

    private void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        }
        catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }
}
