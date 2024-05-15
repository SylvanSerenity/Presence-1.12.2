package com.sylvan.presence.event;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.concurrent.TimeUnit;

public class ChatMessage {
	// Config
	public static boolean chatMessageEnabled = true;		// Whether the chat message event is active
	private static float chatMessageHauntLevelMin = 2.0f;		// The minimum haunt level to play event
	private static int chatMessageDelayMin = 60 * 60;		// The minimum delay between chat message events
	private static int chatMessageDelayMax = 60 * 60 * 4;		// The maximum delay between chat message events
	private static boolean chatMessageAloneConstraint = true;	// Whether there must be only one player one the server to send a message

	public static void loadConfig() {
		try {
			chatMessageEnabled = Presence.config.getOrSetValue("chatMessageEnabled", chatMessageEnabled).getAsBoolean();
			chatMessageHauntLevelMin = Presence.config.getOrSetValue("chatMessageHauntLevelMin", chatMessageHauntLevelMin).getAsFloat();
			chatMessageDelayMin = Presence.config.getOrSetValue("chatMessageDelayMin", chatMessageDelayMin).getAsInt();
			chatMessageDelayMax = Presence.config.getOrSetValue("chatMessageDelayMax", chatMessageDelayMax).getAsInt();
			chatMessageAloneConstraint = Presence.config.getOrSetValue("chatMessageAloneConstraint", chatMessageAloneConstraint).getAsBoolean();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for ChatMessage.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void scheduleEvent(final PlayerEntity player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (player.isRemoved()) return;
				chatMessage(player, false, false);
				scheduleEvent(player);
			},
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(chatMessageDelayMin, hauntLevel),
				Algorithms.divideByFloat(chatMessageDelayMax, hauntLevel)
			), TimeUnit.SECONDS
		);
	}

	public static void chatMessage(final PlayerEntity player, final boolean overrideHauntLevel, final boolean overrideAloneConstraint) {
		if (player.isRemoved()) return;
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < chatMessageHauntLevelMin) return; // Reset event as if it passed
		}

		// Player must be alone
		if (chatMessageAloneConstraint && !overrideAloneConstraint && player.getServer().getPlayerManager().getPlayerList().size() != 1) return;

		player.sendMessage(Text.literal("."));
	}
}
