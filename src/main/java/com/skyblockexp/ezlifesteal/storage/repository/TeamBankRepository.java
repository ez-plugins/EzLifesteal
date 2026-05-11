package com.skyblockexp.ezlifesteal.storage.repository;

import com.skyblockexp.ezlifesteal.model.TeamBankAccount;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import java.util.Optional;
import java.util.UUID;

public interface TeamBankRepository {

    Optional<TeamBankAccount> loadAccount(UUID teamId) throws StorageException;

    void saveAccount(TeamBankAccount account) throws StorageException;
}
