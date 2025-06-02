package com.miketavious.automine.integration;

import com.miketavious.automine.config.AutoMineConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class AutoMineModMenuApiImpl implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AutoMineConfigScreen::create;
    }
}