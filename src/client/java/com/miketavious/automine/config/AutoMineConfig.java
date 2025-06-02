package com.miketavious.automine.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AutoMineConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("automine.json");
    private static final long SAVE_COOLDOWN = 5000; // 5 seconds

    public AutoMineType autoMineType = AutoMineType.ALL;
    public boolean sendToggleMessages = true;
    public boolean showMessagesInActionBar = true;
    public boolean checkForDurability = true;
    public int stopMinePercentage = 10;

    // Performance optimization - dirty tracking
    private transient boolean isDirty = false;
    private transient long lastSaveTime = 0;

    public static AutoMineConfig load() {
        if (!Files.exists(CONFIG_FILE)) {
            return createAndSaveDefault();
        }

        try {
            var content = Files.readString(CONFIG_FILE);
            var config = GSON.fromJson(content, AutoMineConfig.class);

            return config != null ? resetDirtyFlag(config) : createAndSaveDefault();
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
            return createAndSaveDefault();
        }
    }

    private static AutoMineConfig createAndSaveDefault() {
        var config = new AutoMineConfig();
        config.save();
        return config;
    }

    private static AutoMineConfig resetDirtyFlag(AutoMineConfig config) {
        config.isDirty = false;
        return config;
    }

    public void save() {
        save(false);
    }

    public void save(boolean force) {
        var currentTime = System.currentTimeMillis();

        // Guard clauses for early exit
        if (!force && (!isDirty || currentTime - lastSaveTime < SAVE_COOLDOWN)) {
            return;
        }

        try {
            var json = GSON.toJson(this);
            Files.writeString(CONFIG_FILE, json);

            isDirty = false;
            lastSaveTime = currentTime;
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    // Generic setter method to reduce code duplication
    private <T> void setValue(Supplier<T> getter, Consumer<T> setter, T newValue) {
        if (!getter.get().equals(newValue)) {
            setter.accept(newValue);
            markDirty();
        }
    }

    // Simplified setters using the generic method
    public void setAutoMineType(AutoMineType value) {
        setValue(() -> autoMineType, v -> autoMineType = v, value);
    }

    public void setSendToggleMessages(boolean value) {
        setValue(() -> sendToggleMessages, v -> sendToggleMessages = v, value);
    }

    public void setShowMessagesInActionBar(boolean value) {
        setValue(() -> showMessagesInActionBar, v -> showMessagesInActionBar = v, value);
    }

    public void setCheckForDurability(boolean value) {
        setValue(() -> checkForDurability, v -> checkForDurability = v, value);
    }

    public void setStopMinePercentage(int value) {
        setValue(() -> stopMinePercentage, v -> stopMinePercentage = v, value);
    }

    private void markDirty() {
        isDirty = true;
    }
}