package com.skyblockexp.ezlifesteal.storage.mysql;

import com.skyblockexp.ezlifesteal.model.TeamBankAccount;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlTeamBankRepositoryTest {

    @Test
    void initCreatesTable() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(support.connection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        MySqlTeamBankRepository repository = new MySqlTeamBankRepository(support, "team_banks");
        repository.init();

        verify(statement).executeUpdate(contains("CREATE TABLE IF NOT EXISTS `team_banks`"));
    }

    @Test
    void loadAndSaveAccount() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        Connection connection = mock(Connection.class);
        PreparedStatement loadStatement = mock(PreparedStatement.class);
        PreparedStatement saveStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(support.connection()).thenReturn(connection, connection);
        when(connection.prepareStatement(contains("WHERE team_id = ?"))).thenReturn(loadStatement);
        when(connection.prepareStatement(startsWith("INSERT INTO"))).thenReturn(saveStatement);
        when(loadStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getDouble("hearts")).thenReturn(18.0D);

        MySqlTeamBankRepository repository = new MySqlTeamBankRepository(support, "team_banks");
        UUID teamId = UUID.randomUUID();

        TeamBankAccount loaded = repository.loadAccount(teamId).orElseThrow();
        assertEquals(18.0D, loaded.getHearts());

        repository.saveAccount(new TeamBankAccount(teamId, 22.0D));
        verify(saveStatement).setString(1, teamId.toString());
        verify(saveStatement).setDouble(2, 22.0D);
    }

    @Test
    void wrapsSqlFailures() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        when(support.connection()).thenThrow(new SQLException("boom"));

        MySqlTeamBankRepository repository = new MySqlTeamBankRepository(support, "team_banks");
        StorageException initException = assertThrows(StorageException.class, repository::init);
        assertTrue(initException.getMessage().contains("team bank"));
    }
}
