package com.miketavious.automine;

import com.miketavious.automine.config.AutoMineConfig;
import com.miketavious.automine.handler.AutoMineHandler;
import com.miketavious.automine.handler.KeybindHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class AutoMineClient implements ClientModInitializer {
	public static AutoMineConfig CONFIG;

	@Override
	public void onInitializeClient() {
		initializeConfig();
		registerHandlers();
		registerEvents();
	}

	private void initializeConfig() {
		CONFIG = AutoMineConfig.load();
	}

	private void registerHandlers() {
		KeybindHandler.register();
		AutoMineHandler.initialize();
	}

	private void registerEvents() {
		// Use method reference and cleaner predicate for client state validation
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (isValidClientState(client)) {
				AutoMineHandler.onClientTick();
				KeybindHandler.onKeyInput();
			}
		});
	}

	// Extract validation logic into a predicate for better readability
	private static boolean isValidClientState(net.minecraft.client.MinecraftClient client) {
		return client.player != null &&
				client.world != null &&
				!client.isPaused();
	}
}