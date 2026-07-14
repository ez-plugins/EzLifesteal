package com.skyblockexp.ezlifesteal.compat;

import org.bukkit.NamespacedKey;

/**
 * Lazily creates persistent-data keys used by the shop GUI.
 */
public final class ShopPersistentKeys {
    private static final String NAMESPACE = "ezlifesteal";

    private ShopPersistentKeys() {
    }

    public static NamespacedKey shopIdKey() {
        return Lazy.SHOP_ID_KEY;
    }

    public static NamespacedKey shopPriceKey() {
        return Lazy.SHOP_PRICE_KEY;
    }

    public static NamespacedKey shopQuantityKey() {
        return Lazy.SHOP_QTY_KEY;
    }

    public static NamespacedKey shopCommandsKey() {
        return Lazy.SHOP_COMMANDS_KEY;
    }

    private static NamespacedKey create(String key) {
        return new NamespacedKey(NAMESPACE, key);
    }

    private static final class Lazy {
        private static final NamespacedKey SHOP_ID_KEY = create("shop_id");

        private static final NamespacedKey SHOP_PRICE_KEY = create("shop_price");

        private static final NamespacedKey SHOP_QTY_KEY = create("shop_qty");

        private static final NamespacedKey SHOP_COMMANDS_KEY = create("shop_cmds");
    }
}