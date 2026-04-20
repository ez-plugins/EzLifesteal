package com.skyblockexp.ezlifesteal.storage.yaml;

import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.provider.StorageProvider;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import java.io.File;

public class YamlStorageProvider implements StorageProvider {

    private final YamlProfileRepository profileRepository;

    private final YamlBanRepository banRepository;


    public YamlStorageProvider(File dataRoot, String legacyFileName) {
        this.profileRepository = new YamlProfileRepository(new File(dataRoot, "players"), legacyFileName);
        this.banRepository = new YamlBanRepository(new File(dataRoot, "bans"));
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
        // Nothing to close for YAML.
    }
}
