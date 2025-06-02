package com.miketavious.automine.config;

import com.miketavious.automine.AutoMineClient;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import static com.miketavious.automine.config.NotificationType.*;
import static com.miketavious.automine.config.AutoMineType.*;

public class AutoMineConfigScreen {

    // Pre-allocated Text objects for consistent optimization pattern
    private static final Text ACTION_BAR_TEXT = Text.literal("Action Bar");
    private static final Text CHAT_TEXT = Text.literal("Chat");
    private static final Text OFF_TEXT = Text.literal("Off");

    private static final Text PICKAXE_TEXT = Text.literal("Pickaxe");
    private static final Text AXE_TEXT = Text.literal("Axe");
    private static final Text SHOVEL_TEXT = Text.literal("Shovel");
    private static final Text TOOLS_TEXT = Text.literal("Tools");
    private static final Text ALL_TEXT = Text.literal("All");

    public static Screen create(Screen parent) {
        var builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Auto Mine Configuration"))
                .setSavingRunnable(() -> AutoMineClient.CONFIG.save(true));

        var general = builder.getOrCreateCategory(Text.literal("General"));
        var entryBuilder = builder.entryBuilder();

        // Auto Mine Type setting with formatted display names
        general.addEntry(entryBuilder.startEnumSelector(
                        Text.literal("Auto Mine Type"),
                        AutoMineType.class,
                        AutoMineClient.CONFIG.autoMineType)
                .setDefaultValue(ALL)
                .setTooltip(Text.literal("What tools should trigger auto mining"))
                .setEnumNameProvider(autoMineType -> switch (autoMineType) {
                    case PICKAXE -> PICKAXE_TEXT;
                    case AXE -> AXE_TEXT;
                    case SHOVEL -> SHOVEL_TEXT;
                    case TOOLS -> TOOLS_TEXT;
                    case ALL -> ALL_TEXT;
                    default -> throw new IllegalArgumentException("Unknown mine type: " + autoMineType);
                })
                .setSaveConsumer(AutoMineClient.CONFIG::setAutoMineType)
                .build());

        // Combined Notification setting (cleaner UX with pre-allocated display names)
        general.addEntry(entryBuilder.startEnumSelector(
                        Text.literal("Notification"),
                        NotificationType.class,
                        getCurrentNotificationType())
                .setDefaultValue(ACTION_BAR)
                .setTooltip(Text.literal("How to display toggle messages"))
                .setEnumNameProvider(notificationType -> switch (notificationType) {
                    case ACTION_BAR -> ACTION_BAR_TEXT;
                    case CHAT -> CHAT_TEXT;
                    case OFF -> OFF_TEXT;
                    default -> throw new IllegalArgumentException("Unknown notification type: " + notificationType);
                })
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
            return OFF;
        } else if (config.showMessagesInActionBar) {
            return ACTION_BAR;
        } else {
            return CHAT;
        }
    }

    // Setting notification type with exhaustive switch
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
            default -> throw new IllegalArgumentException("Unknown notification type: " + type);
        }
    }
}