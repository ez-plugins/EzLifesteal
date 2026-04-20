package com.skyblockexp.ezlifesteal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PluginDescriptorTest {

    @Test
    void pluginYamlIsPackagedForBukkit() {
        assertNotNull(
                com.skyblockexp.ezlifesteal.EzLifestealPlugin.class.getResource("/plugin.yml"),
                "plugin.yml must be bundled so Bukkit-based servers can load the plugin"
        );
    }
}
