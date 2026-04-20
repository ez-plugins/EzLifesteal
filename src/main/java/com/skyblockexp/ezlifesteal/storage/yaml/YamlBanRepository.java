package com.skyblockexp.ezlifesteal.storage.yaml;

import com.skyblockexp.ezlifesteal.storage.BanRecord;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

public class YamlBanRepository implements BanRepository {

    private final File directory;

    public YamlBanRepository(File directory) {
        this.directory = directory;
    }

    public void init() throws StorageException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new StorageException("Unable to create bans directory at " + directory.getAbsolutePath());
        }
    }

    @Override
    public void saveBan(BanRecord record) throws StorageException {
        final YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("name", record.getPlayerName());
        configuration.set("reason", record.getReason());
        configuration.set("source", record.getSource());
        configuration.set("created-at", record.getCreatedAt().toString());
        configuration.set("expires-at", record.getExpiresAt() == null ? null : record.getExpiresAt().toString());
        configuration.set("active", record.isActive());
        try {
            configuration.save(banFile(record.getUniqueId()));
        }
        catch (IOException exception) {
            throw new StorageException("Failed to save ban YAML file", exception);
        }
    }

    @Override
    public void removeBan(UUID uniqueId) throws StorageException {
        final File file = banFile(uniqueId);
        if (file.exists() && !file.delete()) {
            throw new StorageException("Failed to remove ban YAML file for " + uniqueId);
        }
    }

    @Override
    public Optional<BanRecord> loadBan(UUID uniqueId) {
        final File file = banFile(uniqueId);
        if (!file.exists()) {
            return Optional.empty();
        }
        return parseBan(file);
    }

    @Override
    public List<BanRecord> loadActiveBans() {
        final File[] files = directory.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) {
            return List.of();
        }
        final List<BanRecord> bans = new ArrayList<>();
        final Instant now = Instant.now();
        for (File file : files) {
            final Optional<BanRecord> parsed = parseBan(file);
            if (parsed.isEmpty()) {
                continue;
            }
            final BanRecord record = parsed.get();
            if (record.isActive() && (record.getExpiresAt() == null || record.getExpiresAt().isAfter(now))) {
                bans.add(record);
            }
        }
        return bans;
    }

    private File banFile(UUID uniqueId) {
        return new File(directory, uniqueId + ".yml");
    }

    private Optional<BanRecord> parseBan(File file) {
        final String fileName = file.getName();
        final String identifier = fileName.substring(0, fileName.length() - 4);
        try {
            final UUID uniqueId = UUID.fromString(identifier);
            final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            final Instant createdAt = parseInstant(configuration.getString("created-at"))
                    .or(() -> parseInstant(configuration.getString("createdAt")))
                    .orElse(Instant.EPOCH);
            final Instant expiresAt = parseInstant(configuration.getString("expires-at"))
                    .or(() -> parseInstant(configuration.getString("expiresAt")))
                    .orElse(null);
            final boolean active = configuration.getBoolean("active", true);
            return Optional.of(new BanRecord(
                    uniqueId,
                    firstPresent(configuration.getString("name"), configuration.getString("playerName")),
                    configuration.getString("reason"),
                    configuration.getString("source"),
                    createdAt,
                    expiresAt,
                    active
            ));
        }
        catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private Optional<Instant> parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(raw));
        }
        catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String firstPresent(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
