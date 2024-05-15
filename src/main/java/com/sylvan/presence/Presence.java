package com.sylvan.presence;

import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.entity.Entities;
import com.sylvan.presence.event.*;
import com.sylvan.presence.util.Algorithms;
import com.sylvan.presence.util.JsonFile;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = Presence.MOD_ID, name = Presence.NAME, version = Presence.VERSION)
public class Presence {
    public static final String MOD_ID = "presence";
    public static final String NAME = "Presence";
    public static final String VERSION = "1.1.7";
    public static Logger LOGGER;
    public static JsonFile config;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        LOGGER = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        LOGGER.info("Presence loading...");

        initConfig();
        Events.registerEvents();
        Commands.registerCommands();
        Entities.registerEntities();

        LOGGER.info("Presence loaded.");
    }
    public static void initConfig() {
        // Load/create config file
        config = new JsonFile(FabricLoader.getInstance().getConfigDir().toString() + "/" + MOD_ID + ".json");

        // Load config variables
        PlayerData.loadConfig();
        Algorithms.loadConfig();
        AmbientSounds.loadConfig();
        Attack.loadConfig();
        ChatMessage.loadConfig();
        Creep.loadConfig();
        ExtinguishTorches.loadConfig();
        FlickerDoor.loadConfig();
        FlowerGift.loadConfig();
        Footsteps.loadConfig();
        Freeze.loadConfig();
        NearbySounds.loadConfig();
        OpenChest.loadConfig();
        OpenDoor.loadConfig();
        Stalk.loadConfig();
        SubtitleWarning.loadConfig();
        TrampleCrops.loadConfig();
    }
}
