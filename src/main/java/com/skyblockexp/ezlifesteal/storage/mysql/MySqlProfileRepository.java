package com.skyblockexp.ezlifesteal.storage.mysql;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.service.HeartValueSanitizer;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class MySqlProfileRepository implements ProfileRepository {

    private static final Logger LOGGER = Logger.getLogger(MySqlProfileRepository.class.getName());

    private static final String BACKEND = "mysql";


    private final MySqlStorageSupport support;

    private final String tableName;

    private final double minHearts;

    private final double defaultHearts;

    private final double maxHearts;


    public MySqlProfileRepository(MySqlStorageSupport support, String tableName) {
        this(support, tableName, 1.0D, 10.0D, 40.0D);
    }

    public MySqlProfileRepository(MySqlStorageSupport support,
                                  String tableName,
                                  double minHearts,
                                  double defaultHearts,
                                  double maxHearts) {
        this.support = support;
        this.tableName = tableName;
        this.minHearts = minHearts;
        this.defaultHearts = defaultHearts;
        this.maxHearts = maxHearts;
    }

    public void init() throws StorageException {
        try (Connection connection = support.connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                    "`uuid` CHAR(36) NOT NULL PRIMARY KEY, " +
                    "`hearts` DOUBLE NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
        catch (SQLException exception) {
            throw new StorageException("Unable to initialise MySQL profile storage", exception);
        }
    }

    @Override
    public Optional<LifestealProfile> loadProfile(UUID uniqueId) throws StorageException {
        final String query = "SELECT hearts FROM `" + tableName + "` WHERE uuid = ?";
        try (Connection connection = support.connection();
                PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uniqueId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    final double rawHearts = resultSet.getDouble("hearts");
                    final double sanitizedHearts = sanitizeHearts(uniqueId, rawHearts);
                    return Optional.of(new LifestealProfile(uniqueId, sanitizedHearts));
                }
            }
            return Optional.empty();
        }
        catch (SQLException exception) {
            throw new StorageException("Unable to load profile from MySQL", exception);
        }
    }

    @Override
    public void saveProfile(LifestealProfile profile) throws StorageException {
        final String query = "INSERT INTO `" + tableName
                + "` (uuid, hearts) VALUES (?, ?) ON DUPLICATE KEY UPDATE hearts = VALUES(hearts)";
        try (Connection connection = support.connection();
                PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, profile.getUniqueId().toString());
            statement.setDouble(2, profile.getHearts());
            statement.executeUpdate();
        }
        catch (SQLException exception) {
            throw new StorageException("Unable to save profile to MySQL", exception);
        }
    }

    @Override
    public List<LifestealProfile> loadTopProfiles(int limit) throws StorageException {
        if (limit <= 0) {
            return List.of();
        }
        final String query = "SELECT uuid, hearts FROM `" + tableName + "` ORDER BY hearts DESC LIMIT ?";
        try (Connection connection = support.connection();
                PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                final List<LifestealProfile> profiles = new ArrayList<>();
                while (resultSet.next()) {
                    try {
                        final UUID uniqueId = UUID.fromString(resultSet.getString("uuid"));
                        final double rawHearts = resultSet.getDouble("hearts");
                        final double sanitizedHearts = sanitizeHearts(uniqueId, rawHearts);
                        profiles.add(new LifestealProfile(uniqueId, sanitizedHearts));
                    }
                    catch (IllegalArgumentException ignored) {
                        // Skip invalid UUID entries
                    }
                }
                return profiles;
            }
        }
        catch (SQLException exception) {
            throw new StorageException("Unable to load top profiles from MySQL", exception);
        }
    }

    @Override
    public void resetAll(double defaultHearts) throws StorageException {
        final String query = "UPDATE `" + tableName + "` SET hearts = ?";
        try (Connection connection = support.connection();
                PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setDouble(1, defaultHearts);
            statement.executeUpdate();
        }
        catch (SQLException exception) {
            throw new StorageException("Unable to reset profiles in MySQL", exception);
        }
    }

    private double sanitizeHearts(UUID uniqueId, double rawHearts) {
        final double sanitizedHearts = HeartValueSanitizer.sanitize(rawHearts, minHearts, defaultHearts, maxHearts);
        if (!Double.isFinite(rawHearts) || Double.compare(rawHearts, sanitizedHearts) != 0) {
            LOGGER.warning("Invalid hearts value for UUID " + uniqueId + " from backend " + BACKEND
                    + "; raw=" + rawHearts + ", sanitized=" + sanitizedHearts + '.');
        }
        return sanitizedHearts;
    }
}
