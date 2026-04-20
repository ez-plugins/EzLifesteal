package com.skyblockexp.ezlifesteal.storage.provider;

import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;

public interface StorageProvider extends AutoCloseable {

    void init() throws StorageException;

    ProfileRepository profiles();

    BanRepository bans();

    @Override
    void close() throws StorageException;
}
