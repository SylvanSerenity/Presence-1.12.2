package com.sylvan.presence.event;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

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

	public static void scheduleEvent(final EntityPlayer player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				subtitleWarning(player, false);
				scheduleEvent(player);
			},
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(subtitleWarningDelayMin, hauntLevel),
				Algorithms.divideByFloat(subtitleWarningDelayMax, hauntLevel)
			), TimeUnit.SECONDS
		);
	}

	public static void subtitleWarning(final EntityPlayer player, final boolean overrideHauntLevel) {
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < subtitleWarningHauntLevelMin) return; // Reset event as if it passed
		}

		final SoundEvent sound = SoundEvent.REGISTRY.getObject(new ResourceLocation("presence", "message.warning"));
		player.getEntityWorld().playSound(player, player.getPosition(), sound, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}
}
