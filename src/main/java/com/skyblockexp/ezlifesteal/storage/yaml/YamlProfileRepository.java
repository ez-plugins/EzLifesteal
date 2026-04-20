package com.skyblockexp.ezlifesteal.storage.yaml;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.service.HeartValueSanitizer;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class YamlProfileRepository implements ProfileRepository {

    private static final Logger LOGGER = Logger.getLogger(YamlProfileRepository.class.getName());

    private static final String BACKEND = "yaml";


    private final File directory;

    private final File legacyFile;

    private final double minHearts;

    private final double defaultHearts;

    private final double maxHearts;

    private final FileMoveStrategy fileMoveStrategy;


    public YamlProfileRepository(File directory, String legacyFileName) {
        this(directory, legacyFileName, 1.0D, 10.0D, 40.0D, new NioFileMoveStrategy());
    }

    public YamlProfileRepository(File directory,
                                 String legacyFileName,
                                 double minHearts,
                                 double defaultHearts,
                                 double maxHearts) {
        this(directory, legacyFileName, minHearts, defaultHearts, maxHearts, new NioFileMoveStrategy());
    }

    YamlProfileRepository(File directory,
                          String legacyFileName,
                          double minHearts,
                          double defaultHearts,
                          double maxHearts,
                          FileMoveStrategy fileMoveStrategy) {
        this.directory = directory;
        this.legacyFile = new File(directory, legacyFileName);
        this.minHearts = minHearts;
        this.defaultHearts = defaultHearts;
        this.maxHearts = maxHearts;
        this.fileMoveStrategy = fileMoveStrategy;
    }

    public void init() throws StorageException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new StorageException("Unable to create players directory at " + directory.getAbsolutePath());
        }
        migrateLegacyFile();
    }

    @Override
    public Optional<LifestealProfile> loadProfile(UUID uniqueId) {
        final File playerFile = playerFile(uniqueId);
        if (!playerFile.exists()) {
            return Optional.empty();
        }
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(playerFile);
        if (!configuration.contains("hearts")) {
            return Optional.empty();
        }
        final double rawHearts = configuration.getDouble("hearts");
        final double sanitizedHearts = sanitizeHearts(uniqueId, rawHearts);
        return Optional.of(new LifestealProfile(uniqueId, sanitizedHearts));
    }

    @Override
    public void saveProfile(LifestealProfile profile) throws StorageException {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("hearts", profile.getHearts());
        writeYamlAtomically(playerFile(profile.getUniqueId()), configuration, "Failed to save player YAML file");
    }

    @Override
    public List<LifestealProfile> loadTopProfiles(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        final File[] files = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) {
            return List.of();
        }
        final List<LifestealProfile> profiles = new ArrayList<>();
        for (File file : files) {
            final String fileName = file.getName();
            final String identifier = fileName.substring(0, fileName.length() - 4);
            try {
                final UUID uniqueId = UUID.fromString(identifier);
                final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                if (!configuration.contains("hearts")) {
                    continue;
                }
                final double rawHearts = configuration.getDouble("hearts");
                final double sanitizedHearts = sanitizeHearts(uniqueId, rawHearts);
                profiles.add(new LifestealProfile(uniqueId, sanitizedHearts));
            }
            catch (IllegalArgumentException ignored) {
                // Ignore files that are not named with a UUID
            }
        }
        profiles.sort(Comparator.comparingDouble(LifestealProfile::getHearts).reversed());
        return profiles.size() > limit ? new ArrayList<>(profiles.subList(0, limit)) : profiles;
    }

    @Override
    public void resetAll(double defaultHearts) throws StorageException {
        final File[] files = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            configuration.set("hearts", defaultHearts);
            writeYamlAtomically(file, configuration, "Failed to reset YAML file for player " + file.getName());
        }
    }

    private File playerFile(UUID uniqueId) {
        return new File(directory, uniqueId + ".yml");
    }

    private double sanitizeHearts(UUID uniqueId, double rawHearts) {
        final double sanitizedHearts = HeartValueSanitizer.sanitize(rawHearts, minHearts, defaultHearts, maxHearts);
        if (!Double.isFinite(rawHearts) || Double.compare(rawHearts, sanitizedHearts) != 0) {
            LOGGER.warning("Invalid hearts value for UUID " + uniqueId + " from backend " + BACKEND
                    + "; raw=" + rawHearts + ", sanitized=" + sanitizedHearts + '.');
        }
        return sanitizedHearts;
    }

    private void migrateLegacyFile() throws StorageException {
        if (!legacyFile.exists() || !legacyFile.isFile()) {
            return;
        }
        final YamlConfiguration legacyConfiguration = YamlConfiguration.loadConfiguration(legacyFile);
        final ConfigurationSection playersSection = legacyConfiguration.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }
        for (String key : playersSection.getKeys(false)) {
            if (key == null || key.isBlank()) {
                continue;
            }
            try {
                final UUID uniqueId = UUID.fromString(key);
                final YamlConfiguration configuration = new YamlConfiguration();
                configuration.set("hearts", playersSection.getDouble(key + ".hearts"));
                writeYamlAtomically(
                        playerFile(uniqueId),
                        configuration,
                        "Failed to migrate legacy YAML entry for " + key
                );
            }
            catch (IllegalArgumentException ignored) {
                // Skip invalid UUID entries
            }
        }
        final File backup = new File(directory.getParentFile(), legacyFile.getName() + ".legacy");
        if (!legacyFile.renameTo(backup)) {
            legacyFile.delete();
        }
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
            fileMoveStrategy.move(temporaryPath, targetPath);
        }
        catch (IOException exception) {
            deleteQuietly(temporaryPath);
            throw new StorageException(errorMessage, exception);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        }
        catch (IOException ignored) {
            // Best-effort cleanup; preserving original file is the priority.
        }
    }

    interface FileMoveStrategy {
        void move(Path source, Path target) throws IOException;
    }

    static final class NioFileMoveStrategy implements FileMoveStrategy {

        @Override
        public void move(Path source, Path target) throws IOException {
            try {
                Files.move(
                        source,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
            catch (AtomicMoveNotSupportedException ignored) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
