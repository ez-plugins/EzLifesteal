package com.skyblockexp.ezlifesteal.storage.mysql;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MySqlProfileRepositoryTest {

    @Test
    void initCreatesTableSuccessfully() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(support.connection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        MySqlProfileRepository repository = new MySqlProfileRepository(support, "lifesteal_profiles");
        repository.init();

        verify(statement).executeUpdate(contains("CREATE TABLE IF NOT EXISTS `lifesteal_profiles`"));
    }

    @Test
    void initThrowsStorageExceptionOnSqlFailure() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        when(support.connection()).thenThrow(new SQLException("boom"));

        MySqlProfileRepository repository = new MySqlProfileRepository(support, "lifesteal_profiles");

        StorageException exception = assertThrows(StorageException.class, repository::init);
        assertTrue(exception.getMessage().contains("Unable to initialise MySQL profile storage"));
    }

    @Test
    void loadProfileReturnsEmptyWhenMissingAndProfileWhenPresent() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(support.connection()).thenReturn(connection, connection);
        when(connection.prepareStatement(contains("WHERE uuid = ?"))).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);

        UUID uniqueId = UUID.randomUUID();
        MySqlProfileRepository repository = new MySqlProfileRepository(support, "lifesteal_profiles");

        when(resultSet.next()).thenReturn(false);
        Optional<LifestealProfile> missing = repository.loadProfile(uniqueId);
        assertTrue(missing.isEmpty());

        when(resultSet.next()).thenReturn(true);
        when(resultSet.getDouble("hearts")).thenReturn(17.5);
        Optional<LifestealProfile> loaded = repository.loadProfile(uniqueId);

        assertTrue(loaded.isPresent());
        assertEquals(uniqueId, loaded.get().getUniqueId());
        assertEquals(17.5, loaded.get().getHearts());
        verify(statement, times(2)).setString(1, uniqueId.toString());
    }

    @Test
    void loadProfileSanitizesInvalidHeartsValues() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(support.connection()).thenReturn(connection, connection, connection, connection);
        when(connection.prepareStatement(contains("WHERE uuid = ?"))).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, true, true);
        when(resultSet.getDouble("hearts")).thenReturn(Double.NaN, Double.NEGATIVE_INFINITY, -4.0D, 999.0D);

        MySqlProfileRepository repository = new MySqlProfileRepository(support, "lifesteal_profiles");

        assertEquals(10.0D, repository.loadProfile(UUID.randomUUID()).orElseThrow().getHearts());
        assertEquals(10.0D, repository.loadProfile(UUID.randomUUID()).orElseThrow().getHearts());
        assertEquals(1.0D, repository.loadProfile(UUID.randomUUID()).orElseThrow().getHearts());
        assertEquals(40.0D, repository.loadProfile(UUID.randomUUID()).orElseThrow().getHearts());
    }

    @Test
    void loadProfileThrowsStorageExceptionOnSqlFailure() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        when(support.connection()).thenThrow(new SQLException("boom"));

        MySqlProfileRepository repository = new MySqlProfileRepository(support, "lifesteal_profiles");

        StorageException exception = assertThrows(StorageException.class,
                () -> repository.loadProfile(UUID.randomUUID()));
        assertTrue(exception.getMessage().contains("Unable to load profile from MySQL"));
    }

    @Test
    void saveProfileUsesUpsertAndWritesUuidAndHearts() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(support.connection()).thenReturn(connection);
        when(connection.prepareStatement(startsWith("INSERT INTO"))).thenReturn(statement);

        MySqlProfileRepository repository = new MySqlProfileRepository(support, "lifesteal_profiles");
        UUID uniqueId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(uniqueId, 14.0);

        repository.saveProfile(profile);

        verify(connection).prepareStatement(contains("ON DUPLICATE KEY UPDATE hearts = VALUES(hearts)"));
        verify(statement).setString(1, uniqueId.toString());
        verify(statement).setDouble(2, 14.0);
        verify(statement).executeUpdate();
    }

    @Test
    void saveProfileThrowsStorageExceptionOnSqlFailure() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        when(support.connection()).thenThrow(new SQLException("boom"));

        MySqlProfileRepository repository = new MySqlProfileRepository(support, "lifesteal_profiles");

        StorageException exception = assertThrows(StorageException.class,
                () -> repository.saveProfile(new LifestealProfile(UUID.randomUUID(), 12.0)));
        assertTrue(exception.getMessage().contains("Unable to save profile to MySQL"));
    }

    @Test
    void loadTopProfilesHandlesLimitSortOrderAndInvalidUuids() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(support.connection()).thenReturn(connection);
        when(connection.prepareStatement(contains("ORDER BY hearts DESC LIMIT ?"))).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);

        MySqlProfileRepository repository = new MySqlProfileRepository(support, "lifesteal_profiles");

        assertTrue(repository.loadTopProfiles(0).isEmpty());
        assertTrue(repository.loadTopProfiles(-4).isEmpty());
        verifyNoInteractions(support);

        when(resultSet.next()).thenReturn(true, true, true, false);
        UUID firstUuid = UUID.randomUUID();
        UUID thirdUuid = UUID.randomUUID();
        when(resultSet.getString("uuid")).thenReturn(firstUuid.toString(), "not-a-uuid", thirdUuid.toString());
        when(resultSet.getDouble("hearts")).thenReturn(20.0, 18.0);

        List<LifestealProfile> profiles = repository.loadTopProfiles(3);

        verify(statement).setInt(1, 3);
        assertEquals(2, profiles.size());
        assertEquals(firstUuid, profiles.get(0).getUniqueId());
        assertEquals(20.0, profiles.get(0).getHearts());
        assertEquals(thirdUuid, profiles.get(1).getUniqueId());
        assertEquals(18.0, profiles.get(1).getHearts());
    }

    @Test
    void loadTopProfilesSanitizesInvalidHeartsValues() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(support.connection()).thenReturn(connection);
        when(connection.prepareStatement(contains("ORDER BY hearts DESC LIMIT ?"))).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, true, true, false);
        when(resultSet.getString("uuid")).thenReturn(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
        when(resultSet.getDouble("hearts")).thenReturn(Double.NaN, Double.POSITIVE_INFINITY, -3.0D, 999.0D);

        MySqlProfileRepository repository = new MySqlProfileRepository(support, "lifesteal_profiles");
        List<LifestealProfile> profiles = repository.loadTopProfiles(10);

        assertEquals(4, profiles.size());
        assertEquals(10.0D, profiles.get(0).getHearts());
        assertEquals(10.0D, profiles.get(1).getHearts());
        assertEquals(1.0D, profiles.get(2).getHearts());
        assertEquals(40.0D, profiles.get(3).getHearts());
    }


    @Test
    void loadTopProfilesReturnsEmptyWhenQueryHasNoRows() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(support.connection()).thenReturn(connection);
        when(connection.prepareStatement(contains("ORDER BY hearts DESC LIMIT ?"))).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        MySqlProfileRepository repository = new MySqlProfileRepository(support, "lifesteal_profiles");

        List<LifestealProfile> profiles = repository.loadTopProfiles(5);

        assertTrue(profiles.isEmpty());
        verify(statement).setInt(1, 5);
    }

    @Test
    void loadTopProfilesThrowsStorageExceptionOnSqlFailure() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        when(support.connection()).thenThrow(new SQLException("boom"));

        MySqlProfileRepository repository = new MySqlProfileRepository(support, "lifesteal_profiles");

        StorageException exception = assertThrows(StorageException.class,
                () -> repository.loadTopProfiles(5));
        assertTrue(exception.getMessage().contains("Unable to load top profiles from MySQL"));
    }

    @Test
    void resetAllUpdatesAllRows() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(support.connection()).thenReturn(connection);
        when(connection.prepareStatement(startsWith("UPDATE"))).thenReturn(statement);

        MySqlProfileRepository repository = new MySqlProfileRepository(support, "lifesteal_profiles");
        repository.resetAll(10.5);

        verify(connection).prepareStatement(contains("UPDATE `lifesteal_profiles` SET hearts = ?"));
        verify(statement).setDouble(1, 10.5);
        verify(statement).executeUpdate();
    }

    @Test
    void resetAllThrowsStorageExceptionOnSqlFailure() throws Exception {
        MySqlStorageSupport support = mock(MySqlStorageSupport.class);
        when(support.connection()).thenThrow(new SQLException("boom"));

        MySqlProfileRepository repository = new MySqlProfileRepository(support, "lifesteal_profiles");

        StorageException exception = assertThrows(StorageException.class,
                () -> repository.resetAll(12.0));
        assertTrue(exception.getMessage().contains("Unable to reset profiles in MySQL"));
    }
}
