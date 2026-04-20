package com.skyblockexp.ezlifesteal.storage.mysql;

import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.provider.StorageProvider;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;

public class MySqlStorageProvider implements StorageProvider {

    private final MySqlProfileRepository profileRepository;

    private final MySqlBanRepository banRepository;

    public MySqlStorageProvider(String host,
                                int port,
                                String database,
                                String username,
                                String password,
                                boolean useSsl,
                                String tableName) {
        final MySqlStorageSupport support = new MySqlStorageSupport(host, port, database, username, password, useSsl);
        this.profileRepository = new MySqlProfileRepository(support, tableName);
        this.banRepository = new MySqlBanRepository(support, tableName + "_bans");
    }

    @Override
    public void init() throws StorageException {
        profileRepository.init();
        banRepository.init();
    }

    @Override
    public ProfileRepository profiles() {
        return profileRepository;
    }

    @Override
    public BanRepository bans() {
        return banRepository;
    }

    @Override
    public void close() {
        // No persistent resources to close.
    }
}
