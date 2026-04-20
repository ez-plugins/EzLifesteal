package com.skyblockexp.ezlifesteal.storage.mysql;

import com.skyblockexp.ezlifesteal.storage.BanRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlBanRepositoryTest {

    @Test
    void createSaveLoadAndUnbanRecord() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        PreparedStatement insertStatement = mock(PreparedStatement.class);
        PreparedStatement selectStatement = mock(PreparedStatement.class);
        PreparedStatement deleteStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(support.connection()).thenReturn(connection, connection, connection, connection);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.prepareStatement(startsWith("INSERT INTO"))).thenReturn(insertStatement);
        when(connection.prepareStatement(contains("WHERE uuid = ?"))).thenReturn(selectStatement, deleteStatement);
        when(selectStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        UUID uniqueId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-02T03:04:05Z");
        when(resultSet.getString("uuid")).thenReturn(uniqueId.toString());
        when(resultSet.getString("player_name")).thenReturn("PlayerOne");
        when(resultSet.getString("reason")).thenReturn("Zero hearts");
        when(resultSet.getString("source")).thenReturn("EzLifesteal");
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(createdAt));
        when(resultSet.getTimestamp("expires_at")).thenReturn(null);
        when(resultSet.getBoolean("active")).thenReturn(true);

        MySqlBanRepository repository = new MySqlBanRepository(support, "lifesteal_bans");
        repository.init();

        verify(statement).executeUpdate(contains("CREATE TABLE IF NOT EXISTS `lifesteal_bans`"));
        verify(statement).executeUpdate(contains("(`active`, `created_at`)"));

        BanRecord record = new BanRecord(uniqueId, "PlayerOne", "Zero hearts", "EzLifesteal", createdAt, null, true);
        repository.saveBan(record);
        Optional<BanRecord> loaded = repository.loadBan(uniqueId);
        repository.removeBan(uniqueId);

        assertTrue(loaded.isPresent());
        assertEquals("PlayerOne", loaded.get().getPlayerName());
        assertEquals("Zero hearts", loaded.get().getReason());

        verify(insertStatement).setString(1, uniqueId.toString());
        verify(deleteStatement).setString(1, uniqueId.toString());
    }
}
