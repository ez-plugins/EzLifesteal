package com.skyblockexp.ezlifesteal.placeholder;

/**
 * Simple abstraction around optional placeholder integrations so the main plugin
 * can interact with them without depending on external APIs at class-load time.
 */
public interface PlaceholderHook {

    /**
     * Register the placeholder provider.
     *
     * @return {@code true} when the registration succeeded
     */
    boolean register();

    /**
     * Unregister the placeholder provider and release any resources associated
     * with the expansion.
     *
     * @return {@code true} when the unregister operation succeeded
     */
    boolean unregisterExpansion();

    /**
     * Clear any cached placeholder data.
     */
    void clearCache();
}
