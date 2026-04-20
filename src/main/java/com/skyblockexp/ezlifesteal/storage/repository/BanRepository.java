package com.skyblockexp.ezlifesteal.storage.repository;

import com.skyblockexp.ezlifesteal.storage.BanRecord;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BanRepository {

    void saveBan(BanRecord record) throws StorageException;

    void removeBan(UUID uniqueId) throws StorageException;

    Optional<BanRecord> loadBan(UUID uniqueId) throws StorageException;

    List<BanRecord> loadActiveBans() throws StorageException;
}
