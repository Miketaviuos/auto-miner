package com.miketavious.automine.config;

import com.miketavious.automine.AutoMineClient;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class AutoMineConfigScreen {

    public static Screen create(Screen parent) {
        var builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Auto Mine Configuration"))
                .setSavingRunnable(() -> AutoMineClient.CONFIG.save(true));

        var general = builder.getOrCreateCategory(Text.literal("General"));
        var entryBuilder = builder.entryBuilder();

        // Auto Mine Type setting
        general.addEntry(entryBuilder.startEnumSelector(
                        Text.literal("Auto Mine Type"),
                        AutoMineType.class,
                        AutoMineClient.CONFIG.autoMineType)
                .setDefaultValue(AutoMineType.ALL)
                .setTooltip(Text.literal("What tools should trigger auto mining"))
                .setSaveConsumer(AutoMineClient.CONFIG::setAutoMineType)
                .build());

        // Combined Notification setting (cleaner UX)
        general.addEntry(entryBuilder.startEnumSelector(
                        Text.literal("Notification"),
                        NotificationType.class,
                        getCurrentNotificationType())
                .setDefaultValue(NotificationType.ACTION_BAR)
                .setTooltip(Text.literal("How to display toggle messages"))
                .setSaveConsumer(AutoMineConfigScreen::setNotificationType)
                .build());

        // Check for Durability setting
        general.addEntry(entryBuilder.startBooleanToggle(
                        Text.literal("Check for Durability"),
                        AutoMineClient.CONFIG.checkForDurability)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Stop auto mining when tool durability gets low"))
                .setSaveConsumer(AutoMineClient.CONFIG::setCheckForDurability)
                .build());

        // Stop Mine Percentage setting
        general.addEntry(entryBuilder.startIntSlider(
                        Text.literal("Stop Mine Percentage"),
                        AutoMineClient.CONFIG.stopMinePercentage,
                        1, 100)
                .setDefaultValue(10)
                .setTooltip(Text.literal("Stop mining when tool durability drops below this percentage"))
                .setSaveConsumer(AutoMineClient.CONFIG::setStopMinePercentage)
                .build());

        return builder.build();
    }

    // Java 21 compatible - using if-else instead of boolean switch
    private static NotificationType getCurrentNotificationType() {
        var config = AutoMineClient.CONFIG;

        if (!config.sendToggleMessages) {
            return NotificationType.OFF;
        } else if (config.showMessagesInActionBar) {
            return NotificationType.ACTION_BAR;
        } else {
            return NotificationType.CHAT;
        }
    }

    // Setting notification type with traditional switch
    private static void setNotificationType(NotificationType type) {
        var config = AutoMineClient.CONFIG;

        switch (type) {
            case OFF -> {
                config.setSendToggleMessages(false);
                config.setShowMessagesInActionBar(false);
            }
            case ACTION_BAR -> {
                config.setSendToggleMessages(true);
                config.setShowMessagesInActionBar(true);
            }
            case CHAT -> {
                config.setSendToggleMessages(true);
                config.setShowMessagesInActionBar(false);
            }
        }
    }
}