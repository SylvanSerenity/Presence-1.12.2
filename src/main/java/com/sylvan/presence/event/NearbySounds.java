package com.sylvan.presence.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NearbySounds {
	private static final Map<SoundEvent, Float> nearbySounds = new HashMap<>();

	// Config
	public static boolean nearbySoundsEnabled = true;			// Whether the nearby sounds event is active
	public static float nearbySoundsHauntLevelMin = 1.25f;			// The minimum haunt level to play event
	private static int nearbySoundsDelayMin = 60 * 5;			// The minimum delay between nearby sounds events
	private static int nearbySoundsDelayMax = 60 * 60 * 2;			// The maximum delay between nearby sounds events
	private static int nearbySoundsDistanceMin = 12;			// The minimum distance of the sound from the player. 12 gives a distant-feeling sound to where it is often barely noticeable
	private static int nearbySoundsDistanceMax = 16;			// The maximum distance of the sound from the player. 16 is maximum distance to hear sounds
	private static JsonObject nearbySoundsSoundWeights = new JsonObject();	// A set of sound ID keys with weight values to play during the event

	public static void loadConfig() {
		nearbySoundsSoundWeights.addProperty(SoundEvents.ENTITY_PLAYER_SMALL_FALL.getRegistryName().getResourcePath(), 40.0f);
		nearbySoundsSoundWeights.addProperty(SoundEvents.ENTITY_ITEM_PICKUP.getRegistryName().getResourcePath(), 30.0f);
		nearbySoundsSoundWeights.addProperty(SoundEvents.ENTITY_PLAYER_BIG_FALL.getRegistryName().getResourcePath(), 20.0f);
		nearbySoundsSoundWeights.addProperty(SoundEvents.ENTITY_GENERIC_EAT.getRegistryName().getResourcePath(), 8.5f);
		nearbySoundsSoundWeights.addProperty(SoundEvents.ENTITY_PLAYER_HURT.getRegistryName().getResourcePath(), 1.0f);
		nearbySoundsSoundWeights.addProperty(SoundEvents.ENTITY_PLAYER_BREATH.getRegistryName().getResourcePath(), 0.5f);

		try {
			nearbySoundsEnabled = Presence.config.getOrSetValue("nearbySoundsEnabled", nearbySoundsEnabled).getAsBoolean();
			nearbySoundsHauntLevelMin = Presence.config.getOrSetValue("nearbySoundsHauntLevelMin", nearbySoundsHauntLevelMin).getAsFloat();
			nearbySoundsDelayMin = Presence.config.getOrSetValue("nearbySoundsDelayMin", nearbySoundsDelayMin).getAsInt();
			nearbySoundsDelayMax = Presence.config.getOrSetValue("nearbySoundsDelayMax", nearbySoundsDelayMax).getAsInt();
			nearbySoundsDistanceMin = Presence.config.getOrSetValue("nearbySoundsDistanceMin", nearbySoundsDistanceMin).getAsInt();
			nearbySoundsDistanceMax = Presence.config.getOrSetValue("nearbySoundsDistanceMax", nearbySoundsDistanceMax).getAsInt();
			nearbySoundsSoundWeights = Presence.config.getOrSetValue("nearbySoundsSoundWeights", nearbySoundsSoundWeights).getAsJsonObject();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for NearbySounds.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void initEvent() {
		try {
			String key;
			for (Map.Entry<String, JsonElement> entry : nearbySoundsSoundWeights.entrySet()) {
				key = entry.getKey();
				final ResourceLocation soundId = Algorithms.getIdentifierFromString(key);
				final SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
				if (sound == null) {
					Presence.LOGGER.warn("Could not find sound \"" + key + "\" in NearbySounds.java.");
					continue;
				}
				nearbySounds.put(sound, entry.getValue().getAsFloat());
			}
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for AmbientSounds.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
			Events.initEvents();
		}
	}

	public static void scheduleEvent(final EntityPlayer player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (player.isRemoved()) return;
				playNearbySound(player, false);
				scheduleEvent(player);
			},
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(nearbySoundsDelayMin, hauntLevel),
				Algorithms.divideByFloat(nearbySoundsDelayMax, hauntLevel)
			), TimeUnit.SECONDS
		);
	}

	public static void playNearbySound(final EntityPlayer player, final boolean overrideHauntLevel) {
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < nearbySoundsHauntLevelMin) return; // Reset event as if it passed
		}

		final World world = player.getEntityWorld();
		final BlockPos soundPos = Algorithms.getRandomStandableBlockNearEntity(player, nearbySoundsDistanceMin, nearbySoundsDistanceMax, 20, true);
		final SoundEvent sound = Algorithms.randomKeyFromWeightMap(nearbySounds);
		world.playSound(null, soundPos, sound, SoundCategory.PLAYERS, 16.0f, 1.0f);
	}
}
