package com.sylvan.presence.event;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Freeze {
	// Config
	public static boolean freezeEnabled = true;		// Whether the freeze event is active
	public static float freezeHauntLevelMin = 2.5f;		// The minimum haunt level to play event
	private static int freezeDelayMin = 60 * 60 * 2;	// The minimum delay between freeze events
	private static int freezeDelayMax = 60 * 60 * 6;	// The maximum delay between freeze events
	private static int freezeTimeTicks = 20 * 10;		// The number of ticks to freeze the player for

	private static final List<FreezeData> freezeDataList = new ArrayList<>();

	public static void loadConfig() {
		try {
			freezeEnabled = Presence.config.getOrSetValue("freezeEnabled", freezeEnabled).getAsBoolean();
			freezeHauntLevelMin = Presence.config.getOrSetValue("freezeHauntLevelMin", freezeHauntLevelMin).getAsFloat();
			freezeDelayMin = Presence.config.getOrSetValue("freezeDelayMin", freezeDelayMin).getAsInt();
			freezeDelayMax = Presence.config.getOrSetValue("freezeDelayMax", freezeDelayMax).getAsInt();
			freezeTimeTicks = Presence.config.getOrSetValue("freezeTimeTicks", freezeTimeTicks).getAsInt();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for Freeze.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void scheduleEvent(final EntityPlayer player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				freeze(player, false);
				scheduleEvent(player);
			},
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(freezeDelayMin, hauntLevel),
				Algorithms.divideByFloat(freezeDelayMax, hauntLevel)
			), TimeUnit.SECONDS
		);
	}

	public static void onWorldTick() {
		final Iterator<FreezeData> it = freezeDataList.iterator();
		FreezeData freezeData;
		while (it.hasNext()) {
			freezeData = it.next();
			freezeData.freeze();
			if (freezeData.getTicks() >= freezeTimeTicks) {
				it.remove();
			}
		}
	}

	public static void freeze(final EntityPlayer player, final boolean overrideHauntLevel) {
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < freezeHauntLevelMin) return; // Reset event as if it passed
		}

		// Track player for freeze
		freezeDataList.add(new FreezeData(player));

		// Play freeze sound
		final SoundEvent sound = SoundEvent.REGISTRY.getObject(new ResourceLocation("presence", "event.freeze"));
		player.getEntityWorld().playSound(player, player.getPosition(), sound, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}

	private static class FreezeData {
		private final EntityPlayer frozenPlayer;
		private int freezeTicks = 0;
		private final float pitch;
		private final float yaw;
		private final Vec3d position;

		public FreezeData(final EntityPlayer player) {
			frozenPlayer = player;
			pitch = player.getPitchYaw().x;
			yaw = player.getPitchYaw().y;
			position = player.getPositionVector();
		}

		public void freeze() {
			++freezeTicks;

			// Freeze rotation and position
			//TODO Check frozenPlayer.attemptTeleport(position.x, position.y, position.z);
			frozenPlayer.setPositionAndRotation(position.x, position.y, position.z, yaw, pitch);
		}

		public int getTicks() {
			return freezeTicks;
		}
	}
}
