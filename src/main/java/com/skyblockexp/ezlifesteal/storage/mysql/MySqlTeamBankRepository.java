package com.skyblockexp.ezlifesteal.storage.mysql;

import com.skyblockexp.ezlifesteal.model.TeamBankAccount;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.TeamBankRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

public class MySqlTeamBankRepository implements TeamBankRepository {

    private final MySqlStorageSupport support;

    private final String tableName;

    public MySqlTeamBankRepository(MySqlStorageSupport support, String tableName) {
        this.support = support;
        this.tableName = tableName;
    }

    public void init() throws StorageException {
        try (Connection connection = support.connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS `" + tableName + "` (" +
                    "`team_id` CHAR(36) NOT NULL PRIMARY KEY, " +
                    "`hearts` DOUBLE NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
        catch (SQLException exception) {
            throw new StorageException("Unable to initialise MySQL team bank storage", exception);
        }
    }

    @Override
    public Optional<TeamBankAccount> loadAccount(UUID teamId) throws StorageException {
        final String query = "SELECT hearts FROM `" + tableName + "` WHERE team_id = ?";
        try (Connection connection = support.connection();
                PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, teamId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    final double hearts = sanitize(resultSet.getDouble("hearts"));
                    return Optional.of(new TeamBankAccount(teamId, hearts));
                }
            }
            return Optional.empty();
        }
        catch (SQLException exception) {
            throw new StorageException("Unable to load team bank account from MySQL", exception);
        }
    }

    @Override
    public void saveAccount(TeamBankAccount account) throws StorageException {
        final String query = "INSERT INTO `" + tableName
                + "` (team_id, hearts) VALUES (?, ?) ON DUPLICATE KEY UPDATE hearts = VALUES(hearts)";
        try (Connection connection = support.connection();
                PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, account.getTeamId().toString());
            statement.setDouble(2, sanitize(account.getHearts()));
            statement.executeUpdate();
        }
        catch (SQLException exception) {
            throw new StorageException("Unable to save team bank account to MySQL", exception);
        }
    }

    private double sanitize(double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            return 0.0D;
        }
        return value;
    }
}
