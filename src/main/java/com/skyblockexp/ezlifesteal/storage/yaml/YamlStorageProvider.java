package com.skyblockexp.ezlifesteal.storage.yaml;

import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.provider.StorageProvider;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import com.skyblockexp.ezlifesteal.storage.repository.TeamBankRepository;
import java.io.File;

public class YamlStorageProvider implements StorageProvider {

    private final YamlProfileRepository profileRepository;

    private final YamlBanRepository banRepository;

    private final YamlTeamBankRepository teamBankRepository;


    public YamlStorageProvider(File dataRoot, String legacyFileName) {
        this.profileRepository = new YamlProfileRepository(new File(dataRoot, "players"), legacyFileName);
        this.banRepository = new YamlBanRepository(new File(dataRoot, "bans"));
        this.teamBankRepository = new YamlTeamBankRepository(new File(dataRoot, "team-banks"));
    }

    @Override
    public void init() throws StorageException {
        profileRepository.init();
        banRepository.init();
        teamBankRepository.init();
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
    public TeamBankRepository teamBanks() {
        return teamBankRepository;
    }

    @Override
    public void close() {
        // Nothing to close for YAML.
    }
}
