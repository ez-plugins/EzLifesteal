package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.ConfigLoader;
import com.skyblockexp.ezlifesteal.config.MessageService;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultPluginRuntimeServicesMessagesTest {

    @Test
    void loadLanguageConfigurationWarnsAndFallsBackToEnglishWhenConfiguredLanguageMissing() throws Exception {
        Path dataPath = Path.of("build/tmp/runtime-message-tests/missing-language");
        writeLanguageFile(dataPath, "en", "prefix", "&7[EN] ", "welcome", "&aWelcome");

        Logger logger = mock(Logger.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("language", "fr");

        Registry registry = new Registry();
        DefaultPluginRuntimeServices services = runtimeWithConfigAndDataPath(registry, config, dataPath, logger);

        services.setupMessages();

        assertEquals("en", registry.getConfigState().getActiveLanguage());
        assertEquals("en", getField(services, "activeLanguage"));
        assertEquals("§aWelcome", services.getMessageService().getMessage("welcome"));
        verify(logger).warning(contains("Language 'fr' is not available; falling back to English."));
    }

    @Test
    void setupMessagesFallsBackToEnglishWhenNormalizedLocaleFileIsMissing() throws Exception {
        Path dataPath = Path.of("build/tmp/runtime-message-tests/missing-normalized-locale");
        writeLanguageFile(dataPath, "en", "prefix", "&7[EN] ", "welcome", "&aWelcome");

        YamlConfiguration config = new YamlConfiguration();
        config.set("language", "pt-BR");

        Registry registry = new Registry();
        DefaultPluginRuntimeServices services = runtimeWithConfigAndDataPath(registry, config, dataPath,
                Logger.getLogger("test"));

        services.setupMessages();

        assertEquals("en", registry.getConfigState().getActiveLanguage());
        assertEquals("en", getField(services, "activeLanguage"));
        assertEquals("§aWelcome", services.getMessageService().getMessage("welcome"));
    }

    @Test
    void setupMessagesKeepsEnglishDefaultsWhenConfiguredLanguageOmitsKeys() throws Exception {
        Path dataPath = Path.of("build/tmp/runtime-message-tests/partial-language");
        writeLanguageFile(dataPath, "en", "prefix", "&7[EN] ", "default-only", "&aEnglish default", "shared",
                "&eEnglish shared");
        writeLanguageFile(dataPath, "es", "prefix", "&7[ES] ", "shared", "&bEspanol shared");

        YamlConfiguration config = new YamlConfiguration();
        config.set("language", "es");

        DefaultPluginRuntimeServices services = runtimeWithConfigAndDataPath(new Registry(), config, dataPath);
        services.setupMessages();

        MessageService messageService = services.getMessageService();
        assertNotNull(messageService);
        assertEquals("§aEnglish default", messageService.getMessage("default-only"));
        assertEquals("§bEspanol shared", messageService.getMessage("shared"));
    }

    @Test
    void setupMessagesAppliesLegacyMessagesOverridesAfterLanguageFiles() throws Exception {
        Path dataPath = Path.of("build/tmp/runtime-message-tests/legacy-overrides");
        writeLanguageFile(dataPath, "en", "prefix", "&7[EN] ", "reload-success", "&aLanguage", "unrelated",
                "&eFrom language");

        YamlConfiguration config = new YamlConfiguration();
        config.set("language", "en");
        config.set("messages.reload-success", "&cLegacy override");

        DefaultPluginRuntimeServices services = runtimeWithConfigAndDataPath(new Registry(), config, dataPath);
        services.setupMessages();

        MessageService messageService = services.getMessageService();
        assertEquals("§cLegacy override", messageService.getMessage("reload-success"));
        assertEquals("§eFrom language", messageService.getMessage("unrelated"));
    }

    @Test
    void setupMessagesUsesLegacyPrefixWhenLanguagePrefixIsBlank() throws Exception {
        Path dataPath = Path.of("build/tmp/runtime-message-tests/prefix-fallback");
        writeLanguageFile(dataPath, "en", "prefix", "   ", "notify", "&aMessage body");

        YamlConfiguration config = new YamlConfiguration();
        config.set("language", "en");
        config.set("messages.prefix", "&6[Legacy] ");

        DefaultPluginRuntimeServices services = runtimeWithConfigAndDataPath(new Registry(), config, dataPath);
        services.setupMessages();

        assertEquals("§6[Legacy] ", services.getMessageService().getPrefix());
        assertEquals("§aMessage body", services.getMessageService().getMessage("notify"));
    }

    @Test
    void loadLanguageConfigurationNormalizesBlankAndNullLanguageToEnglish() throws Exception {
        Path dataPath = Path.of("build/tmp/runtime-message-tests/language-normalization");
        writeLanguageFile(dataPath, "en", "prefix", "&7[EN] ", "welcome", "&aWelcome");

        DefaultPluginRuntimeServices services = runtimeWithConfigAndDataPath(new Registry(), new YamlConfiguration(),
                dataPath);
        Method method = DefaultPluginRuntimeServices.class.getDeclaredMethod("loadLanguageConfiguration", String.class);
        method.setAccessible(true);

        method.invoke(services, "   ");
        assertEquals("en", getField(services, "activeLanguage"));

        method.invoke(services, new Object[]{null});
        assertEquals("en", getField(services, "activeLanguage"));
    }

    private DefaultPluginRuntimeServices runtimeWithConfigAndDataPath(Registry registry, YamlConfiguration config,
            Path dataPath) throws Exception {
        return runtimeWithConfigAndDataPath(registry, config, dataPath, Logger.getLogger("test"));
    }

    private DefaultPluginRuntimeServices runtimeWithConfigAndDataPath(Registry registry, YamlConfiguration config,
            Path dataPath, Logger logger) throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDataFolder()).thenReturn(dataPath.toFile());

        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, registry);
        setField(services, "configLoader", mock(ConfigLoader.class));
        return services;
    }

    private static void writeLanguageFile(Path dataPath, String language, String... keyValues) throws IOException {
        Path languagePath = dataPath.resolve("languages").resolve(language + ".yml");
        Files.createDirectories(languagePath.getParent());

        YamlConfiguration configuration = new YamlConfiguration();
        for (int i = 0; i < keyValues.length; i += 2) {
            configuration.set(keyValues[i], keyValues[i + 1]);
        }
        configuration.save(languagePath.toFile());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
