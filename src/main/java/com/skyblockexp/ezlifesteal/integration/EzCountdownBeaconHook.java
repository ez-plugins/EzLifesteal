package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezcountdown.api.EzCountdownApi;
import com.skyblockexp.ezcountdown.api.exception.DuplicateCountdownException;
import com.skyblockexp.ezcountdown.api.exception.EzCountdownException;
import com.skyblockexp.ezcountdown.api.model.Countdown;
import com.skyblockexp.ezcountdown.api.model.CountdownBuilder;
import com.skyblockexp.ezcountdown.api.model.CountdownType;
import com.skyblockexp.ezcountdown.display.DisplayType;
import com.skyblockexp.ezlifesteal.config.BeaconSpawnSettings;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * EzCountdown-backed implementation of {@link BeaconCountdownProvider}.
 */
public final class EzCountdownBeaconHook implements BeaconCountdownProvider {

    private static final String COUNTDOWN_PREFIX = "ezls-beacon-";
    private static final String DEFAULT_FORMAT_MESSAGE = "&5\u2620 &d&lRevive Beacon &5\u2620 &r&7 {formatted} until active";

    private final Logger logger;

    public EzCountdownBeaconHook(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Optional<String> startCountdown(String beaconShortId, BeaconSpawnSettings.CountdownSettings settings) {
        final EzCountdownApi api = resolveApi();
        if (api == null) {
            return Optional.empty();
        }
        try {
            final String effectivePrefix = (settings.namePrefix() != null && !settings.namePrefix().isBlank())
                    ? settings.namePrefix()
                    : COUNTDOWN_PREFIX;
            final String countdownName = effectivePrefix + beaconShortId;
            final Set<DisplayType> displayTypeSet = parseDisplayTypes(settings.displayTypes());
            final String formatMessage = settings.formatMessage() != null && !settings.formatMessage().isEmpty()
                    ? settings.formatMessage()
                    : DEFAULT_FORMAT_MESSAGE;

            final CountdownBuilder builder = CountdownBuilder.builder(countdownName)
                    .type(CountdownType.DURATION)
                    .displayTypes(displayTypeSet.isEmpty()
                            ? EnumSet.of(DisplayType.ACTION_BAR)
                            : EnumSet.copyOf(displayTypeSet))
                    .duration(Duration.ofSeconds(settings.durationSeconds()))
                    .formatMessage(formatMessage)
                    .bossBarColor(parseBossBarColor(settings.bossBarColor()))
                    .bossBarStyle(parseBossBarStyle(settings.bossBarStyle()))
                    .updateIntervalSeconds(Math.max(1, settings.updateIntervalSeconds()))
                    .ephemeral(settings.ephemeral());

            if (settings.startMessage() != null && !settings.startMessage().isBlank()) {
                builder.startMessage(settings.startMessage());
            }
            if (settings.endMessage() != null && !settings.endMessage().isBlank()) {
                builder.endMessage(settings.endMessage());
            }
            if (settings.endCommands() != null && !settings.endCommands().isEmpty()) {
                builder.endCommands(settings.endCommands());
            }
            if (settings.visibilityPermission() != null && !settings.visibilityPermission().isBlank()) {
                builder.visibilityPermission(settings.visibilityPermission());
            }

            final Countdown countdown = builder.build();
            if (!api.createCountdown(countdown)) {
                logger.warning("EzCountdown: countdown '" + countdownName + "' already exists; skipping create.");
            }
            api.startCountdown(countdownName);
            return Optional.of(countdownName);
        } catch (DuplicateCountdownException exception) {
            logger.warning("EzCountdown: duplicate countdown '" + exception.getCountdownName()
                    + "' for beacon " + beaconShortId + "; attempting to reuse.");
            return Optional.of(COUNTDOWN_PREFIX + beaconShortId);
        } catch (EzCountdownException exception) {
            logger.warning("Failed to start EzCountdown for beacon "
                    + beaconShortId + ": " + exception.getMessage());
            return Optional.empty();
        } catch (Exception exception) {
            logger.warning("Unexpected error starting EzCountdown for beacon "
                    + beaconShortId + ": " + exception.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void cancelCountdown(String countdownId) {
        if (countdownId == null) {
            return;
        }
        final EzCountdownApi api = resolveApi();
        if (api == null) {
            return;
        }
        try {
            api.deleteCountdown(countdownId);
        } catch (Exception exception) {
            logger.warning("Failed to cancel EzCountdown '" + countdownId + "': " + exception.getMessage());
        }
    }

    private EzCountdownApi resolveApi() {
        final RegisteredServiceProvider<EzCountdownApi> provider =
                Bukkit.getServicesManager().getRegistration(EzCountdownApi.class);
        return provider != null ? provider.getProvider() : null;
    }

    private Set<DisplayType> parseDisplayTypes(List<String> names) {
        final Set<DisplayType> result = EnumSet.noneOf(DisplayType.class);
        for (String name : names) {
            try {
                result.add(DisplayType.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                logger.warning("Unknown EzCountdown display type: '" + name + "'. "
                        + "Valid values: ACTION_BAR, BOSS_BAR, CHAT, TITLE, SCOREBOARD");
            }
        }
        return result;
    }

    private BarColor parseBossBarColor(String name) {
        if (name == null) {
            return BarColor.PURPLE;
        }
        try {
            return BarColor.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            logger.warning("Unknown boss bar color: '" + name + "'. Falling back to PURPLE.");
            return BarColor.PURPLE;
        }
    }

    private BarStyle parseBossBarStyle(String name) {
        if (name == null) {
            return BarStyle.SEGMENTED_20;
        }
        try {
            return BarStyle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            logger.warning("Unknown boss bar style: '" + name + "'. Falling back to SEGMENTED_20.");
            return BarStyle.SEGMENTED_20;
        }
    }
}

