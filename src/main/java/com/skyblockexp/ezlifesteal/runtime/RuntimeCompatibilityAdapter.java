package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.placeholder.PlaceholderHook;

/**
 * Centralizes legacy field synchronization needed by reflective tests while runtime state
 * progressively moves to registry-backed services.
 */
public final class RuntimeCompatibilityAdapter {

    private final DefaultPluginRuntimeServices runtimeServices;

    private final Registry registry;

    public RuntimeCompatibilityAdapter(DefaultPluginRuntimeServices runtimeServices, Registry registry) {
        this.runtimeServices = runtimeServices;
        this.registry = registry;
    }

    public void syncManagerFields() {
        final Registry.ManagerState managerState = registry.getManagerState();
        runtimeServices.setLegacyLifestealManager(managerState.getLifestealManager());
        runtimeServices.setLegacyHeartOverlayManager(managerState.getHeartOverlayManager());
        runtimeServices.setLegacyTopHologramManager(managerState.getTopHologramManager());
        runtimeServices.setLegacyKillStreakManager(managerState.getKillStreakManager());
        runtimeServices.setLegacyPlaceholderExpansion(managerState.getPlaceholderExpansion());
    }

    public void clearPlaceholderField() {
        runtimeServices.setLegacyPlaceholderExpansion(null);
    }

    public void setPlaceholderField(PlaceholderHook placeholderHook) {
        runtimeServices.setLegacyPlaceholderExpansion(placeholderHook);
    }
}
