package com.sylvan.presence.data;

import com.google.gson.JsonObject;
import com.sylvan.presence.Presence;
import com.sylvan.presence.event.*;
import com.sylvan.presence.util.Algorithms;
import com.sylvan.presence.util.JsonFile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlayerData {
	private static final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
	private static MinecraftServer server;
	private static String playerDataDirectory;

	// Config
	public static float defaultHauntChance = 0.1f;			// Default chance of being haunted when joining the server. Range: [0.0, 1.0]
	private static float defaultHauntLevel = 1.0f;			// The default haunt level of each player
	private static float hauntChanceMaxBeforeReset = 0.8f;		// The maximum haunt chance before haunted players get their haunt chance reset
	private static int minutesToFullyReduceHauntChance = 60 * 24;	// The playtime in minutes that would reduce haunt chance by 100%
	private static int minutesToIncreaseHauntLevelBy1 = 60 * 2;	// The playtime of minutes that would increase haunt level by 1.0f
	private static int hauntLevelCalculationDelay = 60;		// The time between haunt level calculations based on playtime

	public static PlayerData addPlayerData(final EntityPlayer player) {
		final PlayerData playerData = new PlayerData(player);
		playerDataMap.put(player.getUuid(), playerData);
		return playerData;
	}

	public static PlayerData getPlayerData(final EntityPlayer player) {
		if (playerDataMap.containsKey(player.getUuid())) return playerDataMap.get(player.getUuid());
		else return new PlayerData(player);
	}

	public static void loadConfig() {
		try {
			defaultHauntChance = Presence.config.getOrSetValue("defaultHauntChance", defaultHauntChance).getAsFloat();
			defaultHauntLevel = Presence.config.getOrSetValue("defaultHauntLevel", defaultHauntLevel).getAsFloat();
			hauntChanceMaxBeforeReset = Presence.config.getOrSetValue("hauntChanceMaxBeforeReset", hauntChanceMaxBeforeReset).getAsFloat();
			minutesToFullyReduceHauntChance = Presence.config.getOrSetValue("minutesToFullyReduceHauntChance", minutesToFullyReduceHauntChance).getAsInt();
			minutesToIncreaseHauntLevelBy1 = Presence.config.getOrSetValue("minutesToIncreaseHauntLevelBy1", minutesToIncreaseHauntLevelBy1).getAsInt();
			hauntLevelCalculationDelay = Presence.config.getOrSetValue("hauntLevelCalculationDelay", hauntLevelCalculationDelay).getAsInt();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for PlayerData.java. Wiping and using default.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void setInstance(final MinecraftServer minecraftServer) {
		server = minecraftServer;
		playerDataDirectory = server.getSavePath(WorldSavePath.ROOT).toString() + "/presence/playerdata/";
	}

	public static boolean hasInstance() {
		return server != null && server.isRunning();
	}

	// Instance
	private final UUID uuid;
	private EntityPlayer player;
	private final String playerDataPath;
	private final LocalDateTime joinTime;
	private boolean isHaunted = false;
	private float hauntLevel = defaultHauntLevel;	// Limits events and divides delay minima and maxima by hauntLevel, such that events happen more often as time goes on. 1.0 has no effect, and larger numbers increase events

	// Persistent
	private float hauntChance = defaultHauntChance;	// Chance of being haunted when joining the server
	private long playTime = 0;			// Time in minutes that the player has played

	private PlayerData(final EntityPlayer playerEntity) {
		this.player = playerEntity;
		this.uuid = playerEntity.getUuid();
		this.playerDataPath = playerDataDirectory + uuid.toString();
		this.joinTime = LocalDateTime.now();
		load();
		rollHauntChance();
	}

	public float getHauntChance() {
		return hauntChance;
	}

	public float getHauntLevel() {
		return hauntLevel;
	}

	public EntityPlayer getPlayer() {
		if (player == null) player = server.getPlayerManager().getPlayer(uuid);
		return player;
	}

	public boolean isHaunted() {
		return isHaunted;
	}

	public void setHaunted(boolean haunted) {
		this.isHaunted = haunted;
	}

	public boolean rollHauntChance() {
		this.isHaunted = Algorithms.RANDOM.nextFloat() <= hauntChance;
		return isHaunted;
	}

	public float calculateHauntChance() {
		if (isHaunted && hauntChance > hauntChanceMaxBeforeReset) {
			// Reset haunt chance
			hauntChance = defaultHauntChance;
		} else {
			// Increase haunt chance based on instance playtime
			hauntChance = Math.max(
				defaultHauntChance,
				Math.min(
					1.0f,
					hauntChance + (
						((float) (Duration.between(joinTime, LocalDateTime.now()).toMinutes())) / Math.max(1, minutesToFullyReduceHauntChance)
					)
				)
			);
		}
		return hauntChance;
	}

	public float calculateHauntLevel() {
		if (isHaunted) {
			// Increase haunt level
			hauntLevel = Math.max(
				0.001f,
				(defaultHauntLevel + (
					((float) Duration.between(joinTime, LocalDateTime.now()).toMinutes()) / Math.max(1, minutesToIncreaseHauntLevelBy1)
				))
			);
		}
		return hauntLevel;
	}

	public void startEvents() {
		if (isHaunted) {
			scheduleEvents();
		}
	}

	public void scheduleEvents() {
		final PlayerEntity player = getPlayer();
		scheduleHauntLevelCalculation();
		if (AmbientSounds.ambientSoundsEnabled) AmbientSounds.scheduleEvent(player);
		if (Attack.attackEnabled) Attack.scheduleEvent(player);
		if (ChatMessage.chatMessageEnabled) ChatMessage.scheduleEvent(player);
		if (Creep.creepEnabled) Creep.scheduleEvent(player);
		if (ExtinguishTorches.extinguishTorchesEnabled) ExtinguishTorches.scheduleTracking(player);
		if (FlickerDoor.flickerDoorEnabled) FlickerDoor.scheduleTracking(player);
		if (FlowerGift.flowerGiftEnabled) FlowerGift.scheduleEvent(player);
		if (Footsteps.footstepsEnabled) Footsteps.scheduleEvent(player);
		if (Freeze.freezeEnabled) Freeze.scheduleEvent(player);
		if (NearbySounds.nearbySoundsEnabled) NearbySounds.scheduleEvent(player);
		if (OpenChest.openChestEnabled) OpenChest.scheduleEvent(player);
		if (OpenDoor.openDoorEnabled) OpenDoor.scheduleEvent(player);
		if (Stalk.stalkEnabled) Stalk.scheduleEvent(player);
		if (SubtitleWarning.subtitleWarningEnabled) SubtitleWarning.scheduleEvent(player);
		if (TrampleCrops.trampleCropsEnabled) TrampleCrops.scheduleEvent(player);
	}

	private void scheduleHauntLevelCalculation() {
		Events.scheduler.schedule(
			() -> {
				calculateHauntLevel();
				if (!player.isRemoved()) scheduleHauntLevelCalculation();
			},
			hauntLevelCalculationDelay, TimeUnit.SECONDS
		);
	}

	private void load() {
		final JsonFile dataFile = new JsonFile(playerDataPath);
		try {
			hauntChance = dataFile.getOrSetValue("hauntChance", hauntChance).getAsFloat();
			playTime = dataFile.getOrSetValue("playTime", playTime).getAsLong();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Corrupted player data for " + uuid.toString() + ". Wiping and using default values.", e);
			dataFile.wipe();
		}
		dataFile.writeJson(dataFile.getJsonObject(), false);
	}

	public void remove() {
		calculateHauntChance();
		save();
		playerDataMap.remove(uuid);
	}

	public void save() {
		final JsonFile dataFile = new JsonFile(playerDataPath);
		final JsonObject data = dataFile.getJsonObject();
		dataFile.setValue(data, "hauntChance", hauntChance);
		dataFile.setValue(data, "playTime", playTime + Duration.between(joinTime, LocalDateTime.now()).toMinutes());
		dataFile.writeJson(data, false);
	}
}
