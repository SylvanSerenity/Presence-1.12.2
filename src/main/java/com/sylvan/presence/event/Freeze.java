package com.sylvan.presence.event;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
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

	public static void scheduleEvent(final PlayerEntity player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (player.isRemoved()) return;
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

	public static void freeze(final PlayerEntity player, final boolean overrideHauntLevel) {
		if (player.isRemoved()) return;
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < freezeHauntLevelMin) return; // Reset event as if it passed
		}

		// Track player for freeze
		freezeDataList.add(new FreezeData(player));

		// Play freeze sound
		player.playSoundToPlayer(SoundEvent.of(new Identifier("presence", "event.freeze")), SoundCategory.MASTER, 1.0f, 1.0f);
	}

	private static class FreezeData {
		private final PlayerEntity frozenPlayer;
		private int freezeTicks = 0;
		private final float pitch;
		private final float yaw;
		private final Vec3d position;

		public FreezeData(final PlayerEntity player) {
			frozenPlayer = player;
			pitch = player.getPitch();
			yaw = player.getYaw();
			position = player.getPos();
		}

		public void freeze() {
			++freezeTicks;

			// Freeze rotation and position
			frozenPlayer.teleport((ServerWorld) frozenPlayer.getWorld(), position.getX(), position.getY(), position.getZ(), PositionFlag.VALUES, yaw, pitch);
		}

		public int getTicks() {
			return freezeTicks;
		}
	}
}
