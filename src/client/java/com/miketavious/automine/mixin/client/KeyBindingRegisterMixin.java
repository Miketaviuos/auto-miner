package com.miketavious.automine.mixin.client;

import com.miketavious.automine.handler.KeybindHandler;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(KeyBinding.class)
public class KeyBindingRegisterMixin {

    @Shadow @Final private static Map<String, KeyBinding> KEYS_BY_ID;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void registerAutoMineKeybind(CallbackInfo ci) {
        KeybindHandler.register();
        if (KeybindHandler.autoMineToggleKey != null) {
            KEYS_BY_ID.put(KeybindHandler.autoMineToggleKey.getTranslationKey(), KeybindHandler.autoMineToggleKey);
        }
    }
}