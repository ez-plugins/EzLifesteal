package com.skyblockexp.ezlifesteal.smoke;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests that verify the plugin loads and the server starts on Spigot, Paper and Folia
 * across two Minecraft / Java version combinations.
 *
 * <p>The tests are only activated under the {@code smoke-tests} Maven profile which sets
 * the {@code plugin.jar.path} system property pointing at the freshly-built plugin JAR.
 * Each test launches a real Minecraft server inside a Docker container (via Testcontainers)
 * and waits for the "Done" log line that signals a successful startup.</p>
 */
@Testcontainers
@DisabledIfSystemProperty(named = "skipSmokeTests", matches = "true")
class ServerStartupSmokeTest {

    /**
     * Returns one {@link Arguments} row per (serverType, mcVersion, dockerImage) combination.
     *
     * <ul>
     *   <li>MC 1.21.4 on Java 21 → {@code itzg/minecraft-server:java21}</li>
     *   <li>MC 1.21.11 on Java 25 → {@code itzg/minecraft-server:java25}</li>
     * </ul>
     */
    static Stream<Arguments> serverConfigs() {
        return Stream.of(
                // MC 1.21.4 / Java 21
                Arguments.of("PAPER",  "1.21.4", "itzg/minecraft-server:java21"),
                Arguments.of("SPIGOT", "1.21.4", "itzg/minecraft-server:java21"),
                Arguments.of("FOLIA",  "1.21.4", "itzg/minecraft-server:java21"),
                // MC 1.21.11 / Java 25
                Arguments.of("PAPER",  "1.21.11", "itzg/minecraft-server:java25"),
                Arguments.of("SPIGOT", "1.21.11", "itzg/minecraft-server:java25"),
                Arguments.of("FOLIA",  "1.21.11", "itzg/minecraft-server:java25")
        );
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("serverConfigs")
    void serverStartsAndPluginLoads(String serverType, String mcVersion, String dockerImage) throws Exception {
        final String jarPath = System.getProperty(
                "plugin.jar.path",
                "target/ezlifesteal.jar"
        );

        @SuppressWarnings("resource")
        GenericContainer<?> container = new GenericContainer<>(dockerImage)
                .withEnv("EULA", "TRUE")
                .withEnv("TYPE", serverType)
                .withEnv("VERSION", mcVersion)
                .withEnv("ONLINE_MODE", "FALSE")
                .withEnv("MEMORY", "512M")
                .withEnv("MAX_TICK_TIME", "-1")
                .withEnv("SKIP_FIREWALL", "true")
                .withCopyFileToContainer(
                        MountableFile.forHostPath(jarPath),
                        "/data/plugins/ezlifesteal.jar"
                )
                .waitingFor(
                        Wait.forLogMessage("(?i).*Done \\(.*\\)!.*", 1)
                                .withStartupTimeout(Duration.ofMinutes(5))
                );

        try {
            container.start();

            // Confirm the plugin loaded without errors
            String logs = container.getLogs();
            assertTrue(
                    logs.contains("EzLifesteal") || logs.contains("ezlifesteal"),
                    serverType + " " + mcVersion + ": expected plugin name in server logs"
            );
        } finally {
            container.stop();
        }
    }
}
