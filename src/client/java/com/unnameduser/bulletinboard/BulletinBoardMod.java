package com.unnameduser.bulletinboard;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulletinBoardMod implements ModInitializer {
    public static final String MOD_ID = "bulletin-board";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Bulletin Board Mod loaded!");
    }
}