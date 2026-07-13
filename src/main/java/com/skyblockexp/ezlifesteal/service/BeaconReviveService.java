package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.config.ReviveAnimationSettings;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.model.SpawnedBeaconStatus;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/**
 * Handles beacon-based revive flow from interaction listeners.
 */
public class BeaconReviveService {

    private static final Map<UUID, String> SELECTED_TARGETS = new ConcurrentHashMap<>();

    private static final Map<UUID, PendingBeaconRevive> PENDING_REVIVES = new ConcurrentHashMap<>();

    private static final Set<String> WHITELISTED_BEACONS = ConcurrentHashMap.newKeySet();

    private static final String WHITELIST_DATA_FILE_NAME = "revive-beacon-whitelist.yml";

    private static final String WHITELIST_DATA_PATH = "revive-beacon-whitelist.whitelisted-beacons";

    private static volatile boolean whitelistLoaded;

    private final PluginAccessor plugin;

    private final ReviveAnimationService reviveAnimationService;

    private String warnedInvalidVoucherId;


    public BeaconReviveService(PluginAccessor plugin) {
        this(plugin, new ReviveAnimationService(plugin));
    }

    public BeaconReviveService(PluginAccessor plugin, ReviveAnimationService reviveAnimationService) {
        this.plugin = plugin;
        this.reviveAnimationService = reviveAnimationService;
    }

    public boolean tryHandleBeaconInteract(Player player, ItemStack heldItem, Block clickedBlock) {
        if (clickedBlock == null || clickedBlock.getType() != Material.BEACON) {
            return false;
        }
        if (!plugin.isReviveBeaconEnabled()) {
            return false;
        }
        if (plugin.isReviveBeaconRequireSneak() && !player.isSneaking()) {
            return false;
        }
        if (plugin.isReviveBeaconWhitelistEnabled() && !isBeaconWhitelisted(clickedBlock.getLocation())) {
            maybeConsumeVoucherOnFailure(heldItem);
            sendFailure(player, "beacon-revive-not-whitelisted", "This beacon is not whitelisted for revive usage.");
            return true;
        }

        // If this is a plugin-spawned beacon, ensure it is in AVAILABLE state before proceeding
        final BeaconSpawnService beaconSpawnService = plugin.getBeaconSpawnService();
        if (beaconSpawnService != null) {
            final var spawned = beaconSpawnService.findByLocation(clickedBlock.getLocation());
            if (spawned.isPresent() && spawned.get().getStatus() != SpawnedBeaconStatus.AVAILABLE) {
                sendFailure(player, "beacon-spawn-not-available", "This beacon is not yet available for use.");
                return true;
            }
        }

        final String configuredVoucherId = resolveVoucherHeartId();
        if (configuredVoucherId == null) {
            maybeConsumeVoucherOnFailure(heldItem);
            return false;
        }

        final String heldHeartId = extractHeartId(heldItem);
        if (heldHeartId == null || configuredVoucherId == null || !configuredVoucherId.equalsIgnoreCase(heldHeartId)) {
            return false;
        }
        if (plugin.isReviveBeaconRequireVoucherInBeacon()) {
            final double holdSeconds = Math.max(0.0D, plugin.getReviveBeaconVoucherHoldSeconds());
            if (holdSeconds > 0.0D) {
                queuePendingRevive(player, clickedBlock, heldItem, configuredVoucherId, holdSeconds);
                return true;
            }
        }

        final ReviveTargetStrategy strategy = resolveConfiguredStrategy();
        final String selectedTarget = SELECTED_TARGETS.get(player.getUniqueId());
        if (strategy.requiresCommandSelection() && (selectedTarget == null || selectedTarget.isBlank())) {
            maybeConsumeVoucherOnFailure(heldItem);
            sendFailure(player, "beacon-revive-no-target-selected", "Select a target first with /revive <player>.");
            sendReviveUsageInstructions(player);
            return true;
        }
        final List<ReviveCandidate> candidates = collectCandidates(
                player,
                clickedBlock.getLocation(),
                plugin.getReviveBeaconMaxDistance(),
                strategy,
                selectedTarget
        );
        if (candidates.isEmpty()) {
            maybeConsumeVoucherOnFailure(heldItem);
            sendFailure(player, "beacon-revive-no-valid-target", "No banned player found near this beacon.");
            return true;
        }

        final LifestealManager manager = plugin.getLifestealManager();
        if (manager == null) {
            sendFailure(player, "beacon-revive-failure", "Revive service is currently unavailable.");
            return true;
        }

        executeReviveFlow(player, heldItem, clickedBlock, candidates, manager);
        return true;
    }

    private void executeReviveFlow(Player player, ItemStack heldItem, Block clickedBlock,
            List<ReviveCandidate> candidates,
                                   LifestealManager manager) {
        resolveTargetCandidateAsync(candidates)
                .thenCompose(optionalCandidate -> {
                    if (optionalCandidate.isEmpty()) {
                        return CompletableFuture.completedFuture(BeaconReviveResult.noTarget());
                    }
                    final ReviveCandidate candidate = optionalCandidate.get();
                    return manager.loadProfileAsync(candidate.uniqueId())
                            .thenCompose(profile -> persistRevive(manager, profile, candidate));
                })
                .whenComplete((result, throwable) -> SchedulerAdapter.run(plugin.getPlugin(), () -> {
                    if (throwable != null) {
                        plugin.getLogger().warning("Beacon revive failed: " + throwable.getMessage());
                        sendStorageError(player);
                        return;
                    }
                    if (result == null || result.status() == BeaconReviveStatus.NO_TARGET) {
                        maybeConsumeVoucherOnFailure(heldItem);
                        sendFailure(player, "beacon-revive-no-valid-target",
                                "No banned player found near this beacon.");
                        return;
                    }
                    if (result.status() == BeaconReviveStatus.FAILURE) {
                        maybeConsumeVoucherOnFailure(heldItem);
                        sendStorageError(player);
                        return;
                    }
                    SELECTED_TARGETS.remove(player.getUniqueId());
                    consumeSingleVoucher(heldItem);
                    // If this was a plugin-spawned beacon, mark it as used
                    final BeaconSpawnService spawnSvc = plugin.getBeaconSpawnService();
                    if (spawnSvc != null) {
                        spawnSvc.markUsedByLocation(clickedBlock.getLocation(), plugin);
                    }
                    applyOnlineEffects(manager, result);
                    final ReviveAnimationSettings settings = plugin.getReviveAnimationSettings();
                    reviveAnimationService.playReviveAnimation(clickedBlock.getLocation(), player, settings);
                    sendSuccess(player, result);
                    plugin.requestTopHologramUpdate();
                }));
    }

    private CompletableFuture<BeaconReviveResult> persistRevive(LifestealManager manager,
                                                                 LifestealProfile profile,
                                                                 ReviveCandidate candidate) {
        final double defaultHearts = manager.getDefaultHearts();
        profile.setHearts(defaultHearts);

        final CompletableFuture<Void> saveFuture = manager.saveProfileAsync(profile);
        final CompletableFuture<Void> unbanFuture = clearPersistedBanAsync(candidate.uniqueId());

        return CompletableFuture.allOf(saveFuture, unbanFuture)
                .thenApply(unused -> BeaconReviveResult.success(candidate, profile.getHearts()))
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Failed beacon revive persistence for "
                            + candidate.uniqueId() + ": " + throwable.getMessage());
                    return BeaconReviveResult.failure(candidate);
                });
    }

    private void applyOnlineEffects(LifestealManager manager, BeaconReviveResult result) {
        final OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(result.uniqueId());
        if (offlineTarget.isOnline()) {
            final Player onlineTarget = offlineTarget.getPlayer();
            if (onlineTarget != null) {
                final LifestealProfile profile = manager.getOrCreateProfile(result.uniqueId());
                profile.setHearts(result.hearts());
                manager.applyHearts(onlineTarget, profile);
                plugin.sendHeartStatus(onlineTarget, result.hearts());
            }
        }

        plugin.getBanAdapter().removeBan(result.uniqueId(), result.playerName());
    }

    private CompletableFuture<Void> clearPersistedBanAsync(UUID uniqueId) {
        final BanRepository banRepository = plugin.getBanRepository();
        if (banRepository == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try {
                banRepository.removeBan(uniqueId);
            }
            catch (StorageException exception) {
                throw new CompletionException(exception);
            }
        }, plugin.getLifestealManager().getPlugin().getStorageExecutor()).exceptionally(throwable -> {
            plugin.getLogger().warning("Failed to remove persisted ban record for "
                    + uniqueId + ": " + throwable.getMessage());
            return null;
        });
    }

    private CompletableFuture<Optional<ReviveCandidate>> resolveTargetCandidateAsync(List<ReviveCandidate> candidates) {
        return CompletableFuture.supplyAsync(() -> {
            final BanRepository banRepository = plugin.getBanRepository();
            for (ReviveCandidate candidate : candidates) {
                if (candidate.bukkitBanned()) {
                    return Optional.of(candidate);
                }
                if (banRepository != null) {
                    try {
                        if (banRepository.loadBan(candidate.uniqueId()).isPresent()) {
                            return Optional.of(candidate);
                        }
                    }
                    catch (StorageException exception) {
                        plugin.getLogger().warning("Failed loading ban state for "
                                + candidate.uniqueId() + ": " + exception.getMessage());
                    }
                }
            }
            return Optional.empty();
        }, plugin.getLifestealManager().getPlugin().getStorageExecutor());
    }

    private List<ReviveCandidate> collectCandidates(Player interactor,
                                                    Location beaconLocation,
                                                    double radius,
                                                    ReviveTargetStrategy strategy,
                                                    String selectedTargetName) {
        final List<ReviveCandidate> candidates = new ArrayList<>();

        for (Player onlinePlayer : interactor.getWorld().getPlayers()) {
            if (onlinePlayer.getUniqueId().equals(interactor.getUniqueId())) {
                continue;
            }
            if (strategy.requiresCommandSelection()
                    && (selectedTargetName == null || !onlinePlayer.getName().equalsIgnoreCase(selectedTargetName))) {
                continue;
            }
            if (beaconLocation.distanceSquared(onlinePlayer.getLocation()) > radius * radius) {
                continue;
            }
            final String playerName = onlinePlayer.getName();
            final boolean bannedByProfile = plugin.getBanAdapter()
                    .isBanned(onlinePlayer.getUniqueId(), playerName);

            candidates.add(new ReviveCandidate(
                    onlinePlayer.getUniqueId(),
                    playerName,
                    beaconLocation.distanceSquared(onlinePlayer.getLocation()),
                    bannedByProfile
            ));
        }

        candidates.sort(Comparator
                .comparingDouble(ReviveCandidate::distanceSquared)
                .thenComparing(candidate -> candidate.uniqueId().toString()));
        if (strategy == ReviveTargetStrategy.NEAREST_BANNED && !candidates.isEmpty()) {
            return List.of(candidates.get(0));
        }
        return candidates;
    }

    public void selectReviveTarget(Player player, String targetName) {
        SELECTED_TARGETS.put(player.getUniqueId(), targetName);
        final MessageService messageService = plugin.getMessageService();
        if (isMessageConfigured(messageService, "beacon-revive-target-selected")) {
            messageService.sendMessage(player, "beacon-revive-target-selected", Map.of("player", targetName));
            sendReviveUsageInstructions(player);
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Revive target selected: " + ChatColor.WHITE + targetName);
        sendReviveUsageInstructions(player);
    }

    public void sendReviveUsageInstructions(Player player) {
        final MessageService messageService = plugin.getMessageService();
        if (isMessageConfigured(messageService, "beacon-revive-instructions")) {
            messageService.sendMessage(player, "beacon-revive-instructions");
            return;
        }
        player
                .sendMessage(ChatColor.YELLOW + "Use /revive <player> to select a target, then right-click a beacon"
                        + " with a revive voucher.");
    }

    public boolean whitelistBeacon(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        ensureWhitelistLoaded();
        return WHITELISTED_BEACONS.add(serializeLocation(location));
    }

    public boolean removeWhitelistedBeacon(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        ensureWhitelistLoaded();
        return WHITELISTED_BEACONS.remove(serializeLocation(location));
    }

    public List<String> listWhitelistedBeacons() {
        ensureWhitelistLoaded();
        return WHITELISTED_BEACONS.stream().sorted(String::compareTo).toList();
    }

    public void clearWhitelistedBeacons() {
        ensureWhitelistLoaded();
        WHITELISTED_BEACONS.clear();
    }

    public void saveWhitelist() {
        if (!(plugin.getPlugin() instanceof com.skyblockexp.ezlifesteal.EzLifestealPlugin ezLifestealPlugin)) {
            return;
        }
        final java.io.File lifestealFile = new java.io.File(ezLifestealPlugin.getDataFolder(),
                WHITELIST_DATA_FILE_NAME);
        final org.bukkit.configuration.file.YamlConfiguration configuration =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(lifestealFile);
        configuration.set(WHITELIST_DATA_PATH, listWhitelistedBeacons());
        try {
            configuration.save(lifestealFile);
        }
        catch (java.io.IOException exception) {
            plugin.getLogger().warning("Failed saving revive beacon whitelist: " + exception.getMessage());
        }
    }

    public boolean isBeaconWhitelisted(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        ensureWhitelistLoaded();
        return WHITELISTED_BEACONS.contains(serializeLocation(location));
    }

    private void ensureWhitelistLoaded() {
        if (whitelistLoaded) {
            return;
        }
        synchronized (WHITELISTED_BEACONS) {
            if (whitelistLoaded) {
                return;
            }
            WHITELISTED_BEACONS.clear();
            List<String> configured = loadWhitelistData();
            if (configured.isEmpty()) {
                configured = plugin.getReviveBeaconWhitelistedBeacons();
            }
            if (configured == null) {
                configured = List.of();
            }
            for (String value : configured) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                WHITELISTED_BEACONS.add(value.toLowerCase(Locale.ROOT));
            }
            whitelistLoaded = true;
        }
    }

    private List<String> loadWhitelistData() {
        if (!(plugin.getPlugin() instanceof com.skyblockexp.ezlifesteal.EzLifestealPlugin ezLifestealPlugin)) {
            return List.of();
        }
        final java.io.File whitelistDataFile = new java.io.File(ezLifestealPlugin.getDataFolder(),
                WHITELIST_DATA_FILE_NAME);
        if (!whitelistDataFile.exists()) {
            return List.of();
        }
        final org.bukkit.configuration.file.YamlConfiguration configuration =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(whitelistDataFile);
        return configuration.getStringList(WHITELIST_DATA_PATH);
    }

    private String serializeLocation(Location location) {
        return location.getWorld().getName().toLowerCase(Locale.ROOT) + ";"
                + location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
    }

    private void queuePendingRevive(Player player, Block clickedBlock, ItemStack heldItem, String configuredVoucherId,
                                    double holdSeconds) {
        final PendingBeaconRevive previous = PENDING_REVIVES.remove(player.getUniqueId());
        if (previous != null && previous.taskHandle() != null) {
            previous.taskHandle().cancel();
        }
        final long holdTicks = Math.max(1L, Math.round(holdSeconds * 20.0D));
        final SchedulerAdapter.TaskHandle taskHandle = SchedulerAdapter.runLater(plugin.getPlugin(), () -> {
            final PendingBeaconRevive pending = PENDING_REVIVES.remove(player.getUniqueId());
            if (pending == null) {
                return;
            }
            final ItemStack currentHand = player.getInventory().getItemInMainHand();
            final String currentHeartId = extractHeartId(currentHand);
            if (currentHeartId == null || !configuredVoucherId.equalsIgnoreCase(currentHeartId)) {
                sendFailure(player, "beacon-revive-hold-failed", "Revive cancelled because the voucher was removed.");
                return;
            }
            final String selectedTarget = SELECTED_TARGETS.get(player.getUniqueId());
            final ReviveTargetStrategy strategy = resolveConfiguredStrategy();
            if (strategy.requiresCommandSelection() && (selectedTarget == null || selectedTarget.isBlank())) {
                sendFailure(player, "beacon-revive-no-target-selected", "Select a target first with /revive <player>.");
                return;
            }
            final List<ReviveCandidate> candidates = collectCandidates(
                    player,
                    clickedBlock.getLocation(),
                    plugin.getReviveBeaconMaxDistance(),
                    strategy,
                    selectedTarget
            );
            if (candidates.isEmpty()) {
                sendFailure(player, "beacon-revive-no-valid-target", "No banned player found near this beacon.");
                return;
            }
            final LifestealManager manager = plugin.getLifestealManager();
            if (manager == null) {
                sendFailure(player, "beacon-revive-failure", "Revive service is currently unavailable.");
                return;
            }
            executeReviveFlow(player, heldItem, clickedBlock, candidates, manager);
        }, holdTicks);
        if (taskHandle != null) {
            PENDING_REVIVES.put(player.getUniqueId(),
                    new PendingBeaconRevive(serializeLocation(clickedBlock.getLocation()), taskHandle));
        }
        sendFailure(player, "beacon-revive-hold-started",
                "Keep the voucher in the beacon for " + String.format(Locale.US, "%.1f", holdSeconds) + " seconds.");
        maybeBroadcastHoldStart(player, clickedBlock.getLocation(), holdSeconds);
    }

    ReviveTargetStrategy resolveStrategy(String rawStrategy) {
        if (rawStrategy == null || rawStrategy.isBlank()) {
            return ReviveTargetStrategy.COMMAND_SELECTION;
        }
        try {
            return ReviveTargetStrategy.valueOf(rawStrategy.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex) {
            return ReviveTargetStrategy.COMMAND_SELECTION;
        }
    }

    private ReviveTargetStrategy resolveConfiguredStrategy() {
        if (plugin.getLifestealConfigAdapter() == null) {
            return ReviveTargetStrategy.COMMAND_SELECTION;
        }
        String rawStrategy = plugin.getLifestealConfigAdapter()
                .getString("revive-beacon.target-strategy", null);
        if (rawStrategy == null || rawStrategy.isBlank()) {
            rawStrategy = plugin.getLifestealConfigAdapter()
                    .getString("beacon-revive.target-strategy", ReviveTargetStrategy.COMMAND_SELECTION.name());
        }
        final ReviveTargetStrategy strategy = resolveStrategy(rawStrategy);
        if (!strategy.name().equalsIgnoreCase(rawStrategy == null ? "" : rawStrategy.trim())) {
            plugin.getLogger().warning("Unsupported beacon-revive target strategy '" + rawStrategy
                    + "'. Falling back to COMMAND_SELECTION.");
        }
        return strategy;
    }

    private void consumeSingleVoucher(ItemStack heldItem) {
        heldItem.setAmount(Math.max(heldItem.getAmount() - 1, 0));
    }

    private String extractHeartId(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) {
            return null;
        }
        return plugin.getHeartIdFrom(itemStack.getItemMeta().getPersistentDataContainer());
    }

    private String resolveVoucherHeartId() {
        final String configuredVoucherId = plugin.getReviveBeaconVoucherHeartId();
        if (configuredVoucherId == null || configuredVoucherId.isBlank()) {
            warnVoucherUnavailable(configuredVoucherId);
            return null;
        }
        if (plugin.getHeartRegistry() == null || plugin.getHeartRegistry().getById(configuredVoucherId) != null) {
            warnedInvalidVoucherId = null;
            return configuredVoucherId;
        }
        warnVoucherUnavailable(configuredVoucherId);
        return null;
    }

    private void warnVoucherUnavailable(String configuredVoucherId) {
        if (configuredVoucherId != null && configuredVoucherId.equalsIgnoreCase(warnedInvalidVoucherId)) {
            return;
        }
        warnedInvalidVoucherId = configuredVoucherId;
        plugin.getLogger().warning("Revive beacon is enabled but voucher heart id '" + configuredVoucherId
                + "' is not registered in HeartRegistry. Disabling beacon revive interaction path.");
    }

    private void maybeConsumeVoucherOnFailure(ItemStack heldItem) {
        if (!plugin.isReviveBeaconConsumeOnFail()) {
            return;
        }
        consumeSingleVoucher(heldItem);
    }

    private void sendStorageError(Player player) {
        final MessageService messageService = plugin.getMessageService();
        if (isMessageConfigured(messageService, "beacon-revive-failure")) {
            messageService.sendMessage(player, "beacon-revive-failure");
            return;
        }
        player.sendMessage(ChatColor.RED + "Failed to contact storage. See console for details.");
    }

    private void sendSuccess(Player player, BeaconReviveResult result) {
        final MessageService messageService = plugin.getMessageService();
        if (isMessageConfigured(messageService, "beacon-revive-success")) {
            messageService.sendMessage(player, "beacon-revive-success", Map.of(
                    "player", result.playerName(),
                    "hearts", Double.toString(result.hearts())
            ));
            maybeBroadcastReviveCompletion(player, result);
            return;
        }
        player.sendMessage(ChatColor.GRAY + "Revived " + ChatColor.WHITE + result.playerName() + ChatColor.GRAY
                + " with " + ChatColor.RED + result.hearts() + ChatColor.GRAY + " hearts.");
        maybeBroadcastReviveCompletion(player, result);
    }

    private void sendFailure(Player player, String messageKey, String fallbackMessage) {
        final MessageService messageService = plugin.getMessageService();
        if (isMessageConfigured(messageService, messageKey)) {
            messageService.sendMessage(player, messageKey);
            return;
        }
        player.sendMessage(ChatColor.RED + fallbackMessage);
    }

    private boolean isMessageConfigured(MessageService messageService, String messageKey) {
        if (messageService == null) {
            return false;
        }
        final String configuredMessage = messageService.getMessage(messageKey);
        return configuredMessage != null && !configuredMessage.isBlank();
    }

    private void maybeBroadcastHoldStart(Player reviver, Location beaconLocation, double holdSeconds) {
        if (!plugin.isReviveBeaconRequireVoucherInBeacon() || !plugin.isReviveBeaconBroadcastEnabled()) {
            return;
        }
        broadcastConfiguredMessage(
                plugin.getReviveBeaconBroadcastHoldStartMessageKey(),
                Map.of(
                        "player", reviver.getName(),
                        "seconds", String.format(Locale.US, "%.1f", holdSeconds),
                        "world", beaconLocation.getWorld() == null ? "unknown" : beaconLocation.getWorld().getName(),
                        "x", Integer.toString(beaconLocation.getBlockX()),
                        "y", Integer.toString(beaconLocation.getBlockY()),
                        "z", Integer.toString(beaconLocation.getBlockZ())
                )
        );
    }

    private void maybeBroadcastReviveCompletion(Player reviver, BeaconReviveResult result) {
        if (!plugin.isReviveBeaconRequireVoucherInBeacon() || !plugin.isReviveBeaconBroadcastEnabled()) {
            return;
        }
        broadcastConfiguredMessage(
                plugin.getReviveBeaconBroadcastCompleteMessageKey(),
                Map.of(
                        "player", reviver.getName(),
                        "target", result.playerName(),
                        "hearts", Double.toString(result.hearts())
                )
        );
    }

    private void broadcastConfiguredMessage(String messageKey, Map<String, String> placeholders) {
        if (messageKey == null || messageKey.isBlank()) {
            return;
        }
        final MessageService messageService = plugin.getMessageService();
        if (!isMessageConfigured(messageService, messageKey)) {
            return;
        }
        Bukkit.broadcastMessage(messageService.format(messageKey, placeholders));
    }

    enum ReviveTargetStrategy {
        NEAREST_BANNED(false),
        COMMAND_SELECTION(true);

        private final boolean requiresCommandSelection;

        ReviveTargetStrategy(boolean requiresCommandSelection) {
            this.requiresCommandSelection = requiresCommandSelection;
        }

        boolean requiresCommandSelection() {
            return requiresCommandSelection;
        }
    }

    private record ReviveCandidate(UUID uniqueId, String playerName, double distanceSquared, boolean bukkitBanned) {
    }

    private record PendingBeaconRevive(String beaconKey, SchedulerAdapter.TaskHandle taskHandle) {
    }

    private enum BeaconReviveStatus {
        SUCCESS,
        NO_TARGET,
        FAILURE
    }

    private record BeaconReviveResult(BeaconReviveStatus status, UUID uniqueId, String playerName, double hearts) {
        static BeaconReviveResult success(ReviveCandidate candidate, double hearts) {
            return new BeaconReviveResult(BeaconReviveStatus.SUCCESS, candidate.uniqueId(), candidate.playerName(),
                    hearts);
        }

        static BeaconReviveResult noTarget() {
            return new BeaconReviveResult(BeaconReviveStatus.NO_TARGET, null, null, 0.0D);
        }

        static BeaconReviveResult failure(ReviveCandidate candidate) {
            return new BeaconReviveResult(BeaconReviveStatus.FAILURE, candidate.uniqueId(), candidate.playerName(),
                    0.0D);
        }
    }
}
