package com.miketavious.automine.config;

import com.miketavious.automine.AutoMineClient;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import static com.miketavious.automine.config.NotificationType.*;
import static com.miketavious.automine.config.AutoMineType.*;

public class AutoMineConfigScreen {

    // Use translation keys for better internationalization support
    private static final Text ACTION_BAR_TEXT = Text.translatable("automine.config.notification.action_bar");
    private static final Text CHAT_TEXT = Text.translatable("automine.config.notification.chat");
    private static final Text OFF_TEXT = Text.translatable("automine.config.notification.off");

    private static final Text PICKAXE_TEXT = Text.translatable("automine.config.type.pickaxe");
    private static final Text AXE_TEXT = Text.translatable("automine.config.type.axe");
    private static final Text SHOVEL_TEXT = Text.translatable("automine.config.type.shovel");
    private static final Text TOOLS_TEXT = Text.translatable("automine.config.type.tools");
    private static final Text ALL_TEXT = Text.translatable("automine.config.type.all");

    public static Screen create(Screen parent) {
        var builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("automine.config.title"))
                .setSavingRunnable(() -> AutoMineClient.CONFIG.save(true));

        var general = builder.getOrCreateCategory(Text.translatable("automine.config.category.general"));
        var entryBuilder = builder.entryBuilder();

        // Auto Mine Type setting with internationalization
        general.addEntry(entryBuilder.startEnumSelector(
                        Text.translatable("automine.config.type"),
                        AutoMineType.class,
                        AutoMineClient.CONFIG.autoMineType)
                .setDefaultValue(ALL)
                .setTooltip(Text.translatable("automine.config.type.tooltip"))
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

        // Notification setting with better UX
        general.addEntry(entryBuilder.startEnumSelector(
                        Text.translatable("automine.config.notification"),
                        NotificationType.class,
                        getCurrentNotificationType())
                .setDefaultValue(ACTION_BAR)
                .setTooltip(Text.translatable("automine.config.notification.tooltip"))
                .setEnumNameProvider(notificationType -> switch (notificationType) {
                    case ACTION_BAR -> ACTION_BAR_TEXT;
                    case CHAT -> CHAT_TEXT;
                    case OFF -> OFF_TEXT;
                    default -> throw new IllegalArgumentException("Unknown notification type: " + notificationType);
                })
                .setSaveConsumer(AutoMineConfigScreen::setNotificationType)
                .build());

        // Durability check with improved tooltip
        general.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("automine.config.durability.check"),
                        AutoMineClient.CONFIG.checkForDurability)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("automine.config.durability.check.tooltip"))
                .setSaveConsumer(AutoMineClient.CONFIG::setCheckForDurability)
                .build());

        // Percentage slider with better formatting
        general.addEntry(entryBuilder.startIntSlider(
                        Text.translatable("automine.config.durability.percentage"),
                        AutoMineClient.CONFIG.stopMinePercentage,
                        1, 100)
                .setDefaultValue(10)
                .setTooltip(Text.translatable("automine.config.durability.percentage.tooltip"))
                .setTextGetter(value -> Text.translatable("automine.config.durability.percentage.display", value))
                .setSaveConsumer(AutoMineClient.CONFIG::setStopMinePercentage)
                .build());

        return builder.build();
    }

    private static NotificationType getCurrentNotificationType() {
        var config = AutoMineClient.CONFIG;
        return !config.sendToggleMessages ? OFF :
                config.showMessagesInActionBar ? ACTION_BAR : CHAT;
    }

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