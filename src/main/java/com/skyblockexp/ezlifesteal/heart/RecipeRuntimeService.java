package com.skyblockexp.ezlifesteal.heart;

import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;

public final class RecipeRuntimeService {
    private final DefaultPluginRuntimeServices runtime;

    private boolean started;

    public RecipeRuntimeService(DefaultPluginRuntimeServices runtime) {
        this.runtime = runtime;
    }

    public void start() {
        if (started || !runtime.isHeartRecipesEnabled()) {
            return;
        }
        runtime.registerHeartRecipes();
        started = true;
    }

    public void reload() {
        stop();
        start();
    }

    public void stop() {
        if (!started) {
            return;
        }
        runtime.clearHeartRecipesSafely();
        started = false;
    }
}
