package com.skyblockexp.ezlifesteal.storage;

import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;

public interface Storage extends ProfileRepository, BanRepository, AutoCloseable {

    void init() throws StorageException;

    @Override
    void close() throws StorageException;
}
