package com.sylvan.presence.event;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.concurrent.TimeUnit;

public class SubtitleWarning {
	// Config
	public static boolean subtitleWarningEnabled = true;		// Whether the subtitle warning event is active
	private static float subtitleWarningHauntLevelMin = 2.0f;	// The minimum haunt level to play event
	private static int subtitleWarningDelayMin = 60 * 30;		// The minimum delay between subtitle warning events
	private static int subtitleWarningDelayMax = 60 * 60 * 2;	// The maximum delay between subtitle warning events

	public static void loadConfig() {
		try {
			subtitleWarningEnabled = Presence.config.getOrSetValue("subtitleWarningEnabled", subtitleWarningEnabled).getAsBoolean();
			subtitleWarningHauntLevelMin = Presence.config.getOrSetValue("subtitleWarningHauntLevelMin", subtitleWarningHauntLevelMin).getAsFloat();
			subtitleWarningDelayMin = Presence.config.getOrSetValue("subtitleWarningDelayMin", subtitleWarningDelayMin).getAsInt();
			subtitleWarningDelayMax = Presence.config.getOrSetValue("subtitleWarningDelayMax", subtitleWarningDelayMax).getAsInt();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for SubtitleWarning.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void scheduleEvent(final PlayerEntity player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (player.isRemoved()) return;
				subtitleWarning(player, false);
				scheduleEvent(player);
			},
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(subtitleWarningDelayMin, hauntLevel),
				Algorithms.divideByFloat(subtitleWarningDelayMax, hauntLevel)
			), TimeUnit.SECONDS
		);
	}

	public static void subtitleWarning(final PlayerEntity player, final boolean overrideHauntLevel) {
		if (player.isRemoved()) return;
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < subtitleWarningHauntLevelMin) return; // Reset event as if it passed
		}

		player.playSoundToPlayer(SoundEvent.of(new Identifier("presence", "message.warning")), SoundCategory.PLAYERS, 1, 1);
	}
}
