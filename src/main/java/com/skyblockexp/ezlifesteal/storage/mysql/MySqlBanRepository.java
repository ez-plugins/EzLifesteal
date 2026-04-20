package com.skyblockexp.ezlifesteal.storage.mysql;

import com.skyblockexp.ezlifesteal.storage.BanRecord;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MySqlBanRepository implements BanRepository {

    private final MySqlStorageSupport support;

    private final String tableName;


    public MySqlBanRepository(MySqlStorageSupport support, String tableName) {
        this.support = support;
        this.tableName = tableName;
    }

    public void init() throws StorageException {
        try (Connection connection = support.connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                    "`uuid` CHAR(36) NOT NULL PRIMARY KEY, " +
                    "`player_name` VARCHAR(16) NULL, " +
                    "`reason` TEXT NULL, " +
                    "`source` VARCHAR(64) NULL, " +
                    "`created_at` TIMESTAMP NOT NULL, " +
                    "`expires_at` TIMESTAMP NULL, " +
                    "`active` BOOLEAN NOT NULL DEFAULT TRUE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS `" + tableName + "_active_created_idx` " +
                    "ON `" + tableName + "` (`active`, `created_at`)");
        }
        catch (SQLException exception) {
            throw new StorageException("Unable to initialise MySQL ban storage", exception);
        }
    }

    @Override
    public void saveBan(BanRecord record) throws StorageException {
        final String query = "INSERT INTO `" + tableName + "` " +
                "(uuid, player_name, reason, source, created_at, expires_at, active) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), reason = VALUES(reason), source ="
                        + " VALUES(source), "
                        +
                "created_at = VALUES(created_at), expires_at = VALUES(expires_at), active = VALUES(active)";
        try (Connection connection = support.connection();
                PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, record.getUniqueId().toString());
            statement.setString(2, record.getPlayerName());
            statement.setString(3, record.getReason());
            statement.setString(4, record.getSource());
            statement.setTimestamp(5, Timestamp.from(record.getCreatedAt()));
            statement.setTimestamp(6, record.getExpiresAt() == null ? null : Timestamp.from(record.getExpiresAt()));
            statement.setBoolean(7, record.isActive());
            statement.executeUpdate();
        }
        catch (SQLException exception) {
            throw new StorageException("Unable to save ban to MySQL", exception);
        }
    }

    @Override
    public void removeBan(UUID uniqueId) throws StorageException {
        final String query = "DELETE FROM `" + tableName + "` WHERE uuid = ?";
        try (Connection connection = support.connection();
                PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uniqueId.toString());
            statement.executeUpdate();
        }
        catch (SQLException exception) {
            throw new StorageException("Unable to remove ban from MySQL", exception);
        }
    }

    @Override
    public Optional<BanRecord> loadBan(UUID uniqueId) throws StorageException {
        final String query = "SELECT uuid, player_name, reason, source, created_at, expires_at, active FROM `"
                + tableName + "` WHERE uuid = ?";
        try (Connection connection = support.connection();
                PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uniqueId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapBan(resultSet));
                }
            }
            return Optional.empty();
        }
        catch (SQLException exception) {
            throw new StorageException("Unable to load ban from MySQL", exception);
        }
    }

    @Override
    public List<BanRecord> loadActiveBans() throws StorageException {
        final String query = "SELECT uuid, player_name, reason, source, created_at, expires_at, active FROM `"
                + tableName + "` " +
                "WHERE active = TRUE AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)";
        try (Connection connection = support.connection();
                PreparedStatement statement = connection.prepareStatement(query);
                ResultSet resultSet = statement.executeQuery()) {
            final List<BanRecord> records = new ArrayList<>();
            while (resultSet.next()) {
                records.add(mapBan(resultSet));
            }
            return records;
        }
        catch (SQLException exception) {
            throw new StorageException("Unable to load active bans from MySQL", exception);
        }
    }

    private BanRecord mapBan(ResultSet resultSet) throws SQLException {
        final UUID uniqueId = UUID.fromString(resultSet.getString("uuid"));
        final Timestamp createdAtRaw = resultSet.getTimestamp("created_at");
        final Instant createdAt = createdAtRaw == null ? Instant.EPOCH : createdAtRaw.toInstant();
        final Timestamp expiresAtRaw = resultSet.getTimestamp("expires_at");
        final Instant expiresAt = expiresAtRaw == null ? null : expiresAtRaw.toInstant();
        return new BanRecord(uniqueId, resultSet.getString("player_name"), resultSet.getString("reason"),
                resultSet.getString("source"), createdAt, expiresAt, resultSet.getBoolean("active"));
    }
}
