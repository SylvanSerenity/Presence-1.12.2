package com.sylvan.presence.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AmbientSounds {
	private static final Map<SoundEvent, Float> ambientSounds = new HashMap<>();

	// Config
	public static boolean ambientSoundsEnabled = true;			// Whether the ambient sounds event is active
	private static float ambientSoundsHauntLevelMin = 1.0f;			// The minimum haunt level to play event
	private static int ambientSoundsDelayMin = 60 * 30;			// The minimum delay between ambient sounds
	private static int ambientSoundsDelayMax = 60 * 60 * 2;			// The maximum delay between ambient sounds
	private static int ambientSoundsRetryDelay = 60;			// The delay between tries to play ambient sound
	private static boolean ambientSoundsCaveConstraint = true;		// Whether the player must be in a cave for the sound to play
	private static boolean ambientSoundsDarknessConstraint = true;		// Whether the player must be in darkness for the sound to play
	private static int ambientSoundsLightLevelMax = 7;			// The maximum light level to play ambient sound (so that it is dark)
	private static float ambientSoundsPitchMin = 0.5f;			// The minimum sound pitch (so that it is slow and darker sounding)
	private static float ambientSoundsPitchMax = 0.5f;			// The maximum sound pitch
	private static JsonObject ambientSoundsSoundWeights = new JsonObject();	// A set of sound ID keys with weight values to play during the event

	public static void loadConfig() {
		ambientSoundsSoundWeights.addProperty(SoundEvents.AMBIENT_CAVE.value().getId().toString(), 35.0f);
		ambientSoundsSoundWeights.addProperty(SoundEvents.AMBIENT_CRIMSON_FOREST_ADDITIONS.value().getId().toString(), 15.0f);
		ambientSoundsSoundWeights.addProperty(SoundEvents.AMBIENT_NETHER_WASTES_ADDITIONS.value().getId().toString(), 15.0f);
		ambientSoundsSoundWeights.addProperty(SoundEvents.AMBIENT_BASALT_DELTAS_ADDITIONS.value().getId().toString(), 10.0f);
		ambientSoundsSoundWeights.addProperty(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS.value().getId().toString(), 10.0f);
		ambientSoundsSoundWeights.addProperty(SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE.getId().toString(), 7.0f);
		ambientSoundsSoundWeights.addProperty(SoundEvents.AMBIENT_WARPED_FOREST_ADDITIONS.value().getId().toString(), 4.0f);
		ambientSoundsSoundWeights.addProperty(SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE.getId().toString(), 2.5f);
		ambientSoundsSoundWeights.addProperty(SoundEvents.ENTITY_ENDERMAN_STARE.getId().toString(), 0.8f);
		ambientSoundsSoundWeights.addProperty(SoundEvents.ENTITY_WARDEN_AMBIENT.getId().toString(), 0.4f);
		ambientSoundsSoundWeights.addProperty(SoundEvents.BLOCK_SCULK_SHRIEKER_SHRIEK.getId().toString(), 0.2f);
		ambientSoundsSoundWeights.addProperty(SoundEvents.ENTITY_WARDEN_ROAR.getId().toString(), 0.1f);

		try {
			ambientSoundsEnabled = Presence.config.getOrSetValue("ambientSoundsEnabled", ambientSoundsEnabled).getAsBoolean();
			ambientSoundsHauntLevelMin = Presence.config.getOrSetValue("ambientSoundsHauntLevelMin", ambientSoundsHauntLevelMin).getAsFloat();
			ambientSoundsDelayMin = Presence.config.getOrSetValue("ambientSoundsDelayMin", ambientSoundsDelayMin).getAsInt();
			ambientSoundsDelayMax = Presence.config.getOrSetValue("ambientSoundsDelayMax", ambientSoundsDelayMax).getAsInt();
			ambientSoundsRetryDelay = Presence.config.getOrSetValue("ambientSoundsRetryDelay", ambientSoundsRetryDelay).getAsInt();
			ambientSoundsCaveConstraint = Presence.config.getOrSetValue("ambientSoundsCaveConstraint", ambientSoundsCaveConstraint).getAsBoolean();
			ambientSoundsDarknessConstraint = Presence.config.getOrSetValue("ambientSoundsDarknessConstraint", ambientSoundsDarknessConstraint).getAsBoolean();
			ambientSoundsLightLevelMax = Presence.config.getOrSetValue("ambientSoundsLightLevelMax", ambientSoundsLightLevelMax).getAsInt();
			ambientSoundsPitchMin = Presence.config.getOrSetValue("ambientSoundsPitchMin", ambientSoundsPitchMin).getAsFloat();
			ambientSoundsPitchMax = Presence.config.getOrSetValue("ambientSoundsPitchMax", ambientSoundsPitchMax).getAsFloat();
			ambientSoundsSoundWeights = Presence.config.getOrSetValue("ambientSoundsSoundWeights", ambientSoundsSoundWeights).getAsJsonObject();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for AmbientSounds.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void initEvent() {
		try {
			String key;
			for (Map.Entry<String, JsonElement> entry : ambientSoundsSoundWeights.entrySet()) {
				key = entry.getKey();
				final Identifier soundId = Algorithms.getIdentifierFromString(key);
				final SoundEvent sound = Registries.SOUND_EVENT.get(soundId);
				if (sound == null) {
					Presence.LOGGER.warn("Could not find sound \"" + key + "\" in AmbientSounds.java.");
					continue;
				}
				ambientSounds.put(sound, entry.getValue().getAsFloat());
			}
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for AmbientSounds.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
			Events.initEvents();
		}
	}

	public static void scheduleEvent(final PlayerEntity player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		scheduleEventWithDelay(
			player,
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(ambientSoundsDelayMin, hauntLevel),
				Algorithms.divideByFloat(ambientSoundsDelayMax, hauntLevel)
			)
		);
	}

	public static void scheduleEventWithDelay(final PlayerEntity player, final int delay) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (player.isRemoved()) return;
				if (playAmbientSound(player, false)) {
					scheduleEventWithDelay(
						player,
						Algorithms.RANDOM.nextBetween(
							Algorithms.divideByFloat(ambientSoundsDelayMin, hauntLevel),
							Algorithms.divideByFloat(ambientSoundsDelayMax, hauntLevel)
						)
					);
				} else {
					// Retry if it is a bad time
					scheduleEventWithDelay(player, ambientSoundsRetryDelay);
				}
			},
			delay, TimeUnit.SECONDS
		);
	}

	public static boolean playAmbientSound(final PlayerEntity player, final boolean overrideHauntLevel) {
		if (player.isRemoved()) return false;
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < ambientSoundsHauntLevelMin) return true; // Reset event as if it passed
		}

		if (
			(ambientSoundsCaveConstraint && !Algorithms.isEntityInCave(player)) ||					// Player must be in a cave
			(ambientSoundsDarknessConstraint && !Algorithms.isEntityInDarkness(player, ambientSoundsLightLevelMax))	// Player must be in darkness
		) return false;

		final float pitch = Algorithms.randomBetween(ambientSoundsPitchMin, ambientSoundsPitchMax);
		final SoundEvent sound = Algorithms.randomKeyFromWeightMap(ambientSounds);
		player.playSoundToPlayer(sound, SoundCategory.AMBIENT, 256.0f, pitch);
		return true;
	}
}
