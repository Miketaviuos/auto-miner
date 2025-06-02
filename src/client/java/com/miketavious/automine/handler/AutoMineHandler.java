package com.miketavious.automine.handler;

import com.miketavious.automine.AutoMineClient;
import com.miketavious.automine.config.AutoMineType;
import com.miketavious.automine.util.AutoMineItemsList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Objects;
import java.util.Optional;

public class AutoMineHandler {
    public static boolean autoMine = false;
    private static final String MESSAGE_DURABILITY_STOP = "Auto Mine stopped: Low durability!";

    // Track when auto mine was just enabled to force immediate durability check
    private static boolean wasJustEnabled = false;

    // Pre-allocated objects to reduce GC pressure
    private static final Text DURABILITY_MESSAGE = Text.literal(MESSAGE_DURABILITY_STOP).formatted(Formatting.YELLOW);

    // Message queue - simplified with record
    private record QueuedMessage(Text text, boolean actionBar) {}
    private static QueuedMessage queuedMessage;

    // Mining state - simplified with record
    private record MiningState(BlockPos pos, Direction face) {}
    private static MiningState currentMining;

    // Performance cache - using records for immutable caching
    private record CacheState(
            AutoMineType autoMineType,
            boolean checkDurability,
            int stopPercentage,
            boolean showMessages,
            int tick
    ) {}

    private record PlayerState(ClientPlayerEntity player, Item heldItem, int tick) {}

    private static CacheState configCache;
    private static PlayerState playerCache;
    private static HitResult raycastCache;
    private static int raycastCacheTick;

    // Cache intervals
    private static final int CONFIG_CACHE_INTERVAL = 60;
    private static final int PLAYER_CACHE_INTERVAL = 3;
    private static final int RAYCAST_CACHE_INTERVAL = 2;

    // Tool validation cache - using nullable Boolean instead of Optional for field
    private static Boolean toolValidationCache = null;
    private static Item lastValidatedItem;
    private static AutoMineType lastValidatedType;

    private static int tickCounter = 0;

    public static void initialize() {
        refreshConfigCache();
    }

    public static void onClientTick() {
        tickCounter++;

        var client = MinecraftClient.getInstance();

        // Guard clauses for early exit
        if (client.player == null || client.interactionManager == null) return;

        handleQueuedMessages(client.player);

        if (!autoMine) {
            stopMiningIfActive(client);
            return;
        }

        refreshCaches(client);

        // Early exit if invalid tool (using cached validation)
        if (!isValidToolCached()) {
            stopMiningIfActive(client);
            return;
        }

        // Check durability immediately when auto mine is enabled, then periodically
        if ((wasJustEnabled || shouldCheckDurability()) && isDurabilityTooLow()) {
            autoMine = false;
            wasJustEnabled = false; // Reset flag
            stopMiningIfActive(client);
            queueMessage(DURABILITY_MESSAGE, getConfigCache().showMessages);
            return;
        }

        // Reset the flag after the first successful tick
        if (wasJustEnabled) {
            wasJustEnabled = false;
        }

        handleBlockMining(client);
    }

    // Simplified message handling
    private static void handleQueuedMessages(ClientPlayerEntity player) {
        Optional.ofNullable(queuedMessage)
                .ifPresent(msg -> {
                    player.sendMessage(msg.text, msg.actionBar);
                    queuedMessage = null;
                });
    }

    // Guard clause for mining stop
    private static void stopMiningIfActive(MinecraftClient client) {
        if (currentMining != null) {
            stopMining(client);
        }
    }

    // Simplified cache management with lazy evaluation
    private static void refreshCaches(MinecraftClient client) {
        if (shouldRefreshConfigCache()) refreshConfigCache();
        if (shouldRefreshPlayerCache(client)) refreshPlayerCache(client);
    }

    private static boolean shouldRefreshConfigCache() {
        return configCache == null || tickCounter - configCache.tick > CONFIG_CACHE_INTERVAL;
    }

    private static boolean shouldRefreshPlayerCache(MinecraftClient client) {
        return playerCache == null ||
                tickCounter - playerCache.tick > PLAYER_CACHE_INTERVAL ||
                !Objects.equals(client.player, playerCache.player);
    }

    private static void refreshConfigCache() {
        var config = AutoMineClient.CONFIG;
        configCache = new CacheState(
                config != null ? config.autoMineType : AutoMineType.ALL,
                config != null && config.checkForDurability,
                config != null ? config.stopMinePercentage : 10,
                config != null && config.showMessagesInActionBar,
                tickCounter
        );

        // Clear tool validation cache when config changes
        toolValidationCache = null;
    }

    private static void refreshPlayerCache(MinecraftClient client) {
        var player = client.player;
        var heldItem = Optional.ofNullable(player)
                .map(ClientPlayerEntity::getMainHandStack)
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::getItem)
                .orElse(null);

        // Clear tool validation cache if item changed
        if (!Objects.equals(heldItem, lastValidatedItem)) {
            toolValidationCache = null;
        }

        playerCache = new PlayerState(player, heldItem, tickCounter);
    }

    // Cached tool validation with cleaner logic
    private static boolean isValidToolCached() {
        var cache = getConfigCache();
        var player = getPlayerCache();

        if (toolValidationCache == null ||
                !Objects.equals(player.heldItem, lastValidatedItem) ||
                !Objects.equals(cache.autoMineType, lastValidatedType)) {

            lastValidatedItem = player.heldItem;
            lastValidatedType = cache.autoMineType;
            toolValidationCache = isValidTool(player.heldItem, cache.autoMineType);
        }

        return toolValidationCache;
    }

    // Modern switch expression with guard clause - ALL mode allows anything including empty hand
    private static boolean isValidTool(Item item, AutoMineType type) {
        if (type == null) return false;

        return switch (type) {
            case PICKAXE -> item != null && AutoMineItemsList.pickaxeSet.contains(item);
            case AXE -> item != null && AutoMineItemsList.axeSet.contains(item);
            case SHOVEL -> item != null && AutoMineItemsList.shovelSet.contains(item);
            case TOOLS -> item != null && AutoMineItemsList.allToolsSet.contains(item);
            case ALL -> true; // Allow anything - empty hand, any item, etc.
        };
    }

    // Simplified periodic check - we now check before every mining action
    private static boolean shouldCheckDurability() {
        // Since we check before every mining action, this is just a backup safety net
        return tickCounter % 60 == 0 && getConfigCache().checkDurability; // Every 3 seconds as backup
    }

    // Simple predictive durability check - stops exactly at user's percentage
    private static boolean isDurabilityTooLow() {
        return Optional.ofNullable(getPlayerCache().player)
                .map(ClientPlayerEntity::getMainHandStack)
                .filter(ItemStack::isDamageable)
                .map(stack -> {
                    var currentDamage = stack.getDamage();
                    var maxDamage = stack.getMaxDamage();
                    var currentRemaining = maxDamage - currentDamage;

                    // Calculate user's desired threshold (minimum 1 for rounding protection)
                    var thresholdRemaining = Math.max(1, (maxDamage * getConfigCache().stopPercentage) / 100);

                    // Stop if the next mining action would bring us below the threshold
                    return currentRemaining - 1 < thresholdRemaining;
                })
                .orElse(false);
    }

    // Simplified block mining with cleaner flow
    private static void handleBlockMining(MinecraftClient client) {
        var hitResult = getCachedRaycast();

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            stopMiningIfActive(client);
            return;
        }

        var blockHit = (BlockHitResult) hitResult;
        var newMining = new MiningState(blockHit.getBlockPos(), blockHit.getSide());

        // Handle mining state changes
        if (!Objects.equals(currentMining, newMining)) {
            // Either we're mining a different block, or we weren't mining anything
            stopMiningIfActive(client);
            startMining(client, newMining);
        }
        // If currentMining equals newMining, we're already mining the right block

        // Continue mining the current block
        continueMining(client);
    }

    // Cached raycast with simplified logic
    private static HitResult getCachedRaycast() {
        if (raycastCache == null || tickCounter - raycastCacheTick > RAYCAST_CACHE_INTERVAL) {
            raycastCache = getPlayerCache().player.raycast(4.5d, 0.0f, false);
            raycastCacheTick = tickCounter;
        }
        return raycastCache;
    }

    private static void startMining(MinecraftClient client, MiningState mining) {
        // Check durability before starting any new block - perfect protection with minimal overhead
        if (getConfigCache().checkDurability && isDurabilityTooLow()) {
            autoMine = false;
            queueMessage(DURABILITY_MESSAGE, getConfigCache().showMessages);
            return;
        }

        currentMining = mining;
        // Add null safety for interactionManager
        if (client.interactionManager != null) {
            client.interactionManager.attackBlock(mining.pos, mining.face);
        }
    }

    private static void continueMining(MinecraftClient client) {
        // Check durability before each mining action - prevents tool breaking during continuous mining
        if (getConfigCache().checkDurability && isDurabilityTooLow()) {
            autoMine = false;
            queueMessage(DURABILITY_MESSAGE, getConfigCache().showMessages);
            stopMiningIfActive(client);
            return;
        }

        // Add null safety for currentMining, interactionManager, and player
        if (currentMining != null && client.interactionManager != null) {
            client.interactionManager.updateBlockBreakingProgress(currentMining.pos, currentMining.face);
            var player = getPlayerCache().player;
            if (player != null) {
                player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private static void stopMining(MinecraftClient client) {
        // Java 21 compatible - add null safety for interactionManager
        if (currentMining != null && client.interactionManager != null) {
            client.interactionManager.cancelBlockBreaking();
        }
        currentMining = null;
    }

    // Simplified cache getters with defensive programming
    private static CacheState getConfigCache() {
        return Objects.requireNonNullElseGet(configCache, () -> {
            refreshConfigCache();
            return configCache;
        });
    }

    private static PlayerState getPlayerCache() {
        return Objects.requireNonNullElse(playerCache,
                new PlayerState(null, null, 0));
    }

    // Public API methods
    public static void queueMessage(Text message, boolean actionBar) {
        queuedMessage = new QueuedMessage(message, actionBar);
    }

    public static void clearCaches() {
        configCache = null;
        playerCache = null;
        raycastCache = null;
        toolValidationCache = null;
        refreshConfigCache();
    }

    // Called when auto mine is toggled on to force immediate durability check
    public static void onAutoMineEnabled() {
        wasJustEnabled = true;
        clearCaches();
    }
}