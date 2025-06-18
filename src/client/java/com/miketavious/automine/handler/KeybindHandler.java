package com.miketavious.automine.handler;

import com.miketavious.automine.AutoMineClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler {
    private static final String KEY_CATEGORY = "Auto Mine";
    private static final String KEY_MINE_TOGGLE = "key.automine.toggle";

    public static KeyBinding autoMineToggleKey;
    private static boolean wasPressed = false;

    // Modern translation-based messages
    private static Text getToggleOnMessage() {
        return Text.translatable("automine.toggle.on").formatted(Formatting.GREEN);
    }

    private static Text getToggleOffMessage() {
        return Text.translatable("automine.toggle.off").formatted(Formatting.RED);
    }

    public static void register() {
        autoMineToggleKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(KEY_MINE_TOGGLE, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, KEY_CATEGORY)
        );
    }

    public static void onKeyInput() {
        // Guard clause for early exit
        if (autoMineToggleKey == null) return;

        var isPressed = autoMineToggleKey.isPressed();

        // Only process on key press (not release)
        if (isPressed && !wasPressed) {
            toggleAutoMine();
        }

        wasPressed = isPressed;
    }

    private static void toggleAutoMine() {
        AutoMineHandler.autoMine = !AutoMineHandler.autoMine;

        if (AutoMineHandler.autoMine) {
            // Force immediate durability check when enabling auto mine
            AutoMineHandler.onAutoMineEnabled();

            // Smart messaging: only show "ON" if we can actually mine
            if (shouldSendToggleMessage() && !isDurabilityTooLowForToggle()) {
                // Safe to show "ON" message - tool has sufficient durability
                var showInActionBar = AutoMineClient.CONFIG.showMessagesInActionBar;
                AutoMineHandler.queueMessage(getToggleOnMessage(), showInActionBar);
            }
            // If durability is too low, skip "ON" message - let durability check show warning instead
        } else {
            // Just clear caches when disabling
            AutoMineHandler.clearCaches();

            // Always show "OFF" message when manually disabling
            if (shouldSendToggleMessage()) {
                var showInActionBar = AutoMineClient.CONFIG.showMessagesInActionBar;
                AutoMineHandler.queueMessage(getToggleOffMessage(), showInActionBar);
            }
        }
    }

    // Quick durability check for toggle logic (simplified version of main check)
    private static boolean isDurabilityTooLowForToggle() {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player == null) return false;

        var config = AutoMineClient.CONFIG;
        if (config == null || !config.checkForDurability) return false;

        var itemStack = client.player.getMainHandStack();
        if (!itemStack.isDamageable()) return false;

        var currentDamage = itemStack.getDamage();
        var maxDamage = itemStack.getMaxDamage();
        var currentRemaining = maxDamage - currentDamage;
        var thresholdRemaining = Math.max(1, (maxDamage * config.stopMinePercentage) / 100);

        return currentRemaining - 1 < thresholdRemaining;
    }

    // Extract condition for better readability
    private static boolean shouldSendToggleMessage() {
        return AutoMineClient.CONFIG != null && AutoMineClient.CONFIG.sendToggleMessages;
    }
}