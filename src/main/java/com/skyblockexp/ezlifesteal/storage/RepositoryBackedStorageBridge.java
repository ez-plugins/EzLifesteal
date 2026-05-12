package com.skyblockexp.ezlifesteal.storage;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.storage.provider.StorageProvider;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import com.skyblockexp.ezlifesteal.storage.repository.TeamBankRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RepositoryBackedStorageBridge implements Storage {

    private final StorageProvider provider;

    public RepositoryBackedStorageBridge(StorageProvider provider) {
        this.provider = provider;
    }

    @Override
    public void init() throws StorageException {
        provider.init();
    }

    @Override
    public Optional<LifestealProfile> loadProfile(UUID uniqueId) throws StorageException {
        return profiles().loadProfile(uniqueId);
    }

    @Override
    public void saveProfile(LifestealProfile profile) throws StorageException {
        profiles().saveProfile(profile);
    }

    @Override
    public List<LifestealProfile> loadTopProfiles(int limit) throws StorageException {
        return profiles().loadTopProfiles(limit);
    }

    @Override
    public void resetAll(double defaultHearts) throws StorageException {
        profiles().resetAll(defaultHearts);
    }

    @Override
    public void saveBan(BanRecord record) throws StorageException {
        bans().saveBan(record);
    }

    @Override
    public void removeBan(UUID uniqueId) throws StorageException {
        bans().removeBan(uniqueId);
    }

    @Override
    public Optional<BanRecord> loadBan(UUID uniqueId) throws StorageException {
        return bans().loadBan(uniqueId);
    }

    @Override
    public List<BanRecord> loadActiveBans() throws StorageException {
        return bans().loadActiveBans();
    }

    @Override
    public void close() throws StorageException {
        provider.close();
    }

    public ProfileRepository profiles() {
        return provider.profiles();
    }

    public BanRepository bans() {
        return provider.bans();
    }

    public StorageProvider provider() {
        return provider;
    }

    public TeamBankRepository teamBanks() {
        return provider.teamBanks();
    }
}
