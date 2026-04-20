package com.skyblockexp.ezlifesteal.runtime;


public final class StorageRuntimeService {
    private final DefaultPluginRuntimeServices runtime;

    public StorageRuntimeService(DefaultPluginRuntimeServices runtime) {
        this.runtime = runtime;
    }

    public void start() {
        runtime.initializeStorageExecutor();
        runtime.setupStorage();
    }

    public void reload() {
        runtime.initializeStorageExecutor();
        runtime.setupStorage();
    }

    public void stop() {
        runtime.closeStorage();
        runtime.shutdownStorageExecutor();
    }
}
