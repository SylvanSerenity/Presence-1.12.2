package com.sylvan.presence.event;

import com.google.common.base.Predicate;
import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.entity.StalkingEntity;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Stalk {
	// Config
	public static boolean stalkEnabled = true;		// Whether the stalk event is active
	private static float stalkHauntLevelMin = 2.5f;		// The minimum haunt level to play event
	private static int stalkDelayMin = 60 * 45;		// The minimum delay between stalk events
	private static int stalkDelayMax = 60 * 60 * 3;		// The maximum delay between stalk events
	private static int stalkRetryDelay = 1;			// The delay between retrying stalk events in case of failure
	private static int stalkDistanceMin = 64;		// The minimum distance to summon Herobrine
	private static int stalkDistanceMax = 120;		// The maximum distance to summon Herobrine
	private static int stalkClosePlayerDistanceMin = 48;	// The minimum distance from any player before Herobrine vanishes
	public static double stalkLookAtThresholdVanish = 0.2;	// The threshold at which to remove Herobrine after being seen

	public static final List<StalkingEntity> stalkingEntities = new ArrayList<>();

	public static void loadConfig() {
		try {
			stalkEnabled = Presence.config.getOrSetValue("stalkEnabled", stalkEnabled).getAsBoolean();
			stalkHauntLevelMin = Presence.config.getOrSetValue("stalkHauntLevelMin", stalkHauntLevelMin).getAsFloat();
			stalkDelayMin = Presence.config.getOrSetValue("stalkDelayMin", stalkDelayMin).getAsInt();
			stalkDelayMax = Presence.config.getOrSetValue("stalkDelayMax", stalkDelayMax).getAsInt();
			stalkRetryDelay = Presence.config.getOrSetValue("stalkRetryDelay", stalkRetryDelay).getAsInt();
			stalkDistanceMin = Presence.config.getOrSetValue("stalkDistanceMin", stalkDistanceMin).getAsInt();
			stalkDistanceMax = Presence.config.getOrSetValue("stalkDistanceMax", stalkDistanceMax).getAsInt();
			stalkClosePlayerDistanceMin = Presence.config.getOrSetValue("stalkClosePlayerDistanceMin", stalkClosePlayerDistanceMin).getAsInt();
			stalkLookAtThresholdVanish = Presence.config.getOrSetValue("stalkLookAtThresholdVanish", stalkLookAtThresholdVanish).getAsDouble();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for Stalk.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void scheduleEvent(final EntityPlayer player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		scheduleEventWithDelay(
			player,
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(stalkDelayMax, hauntLevel),
				Algorithms.divideByFloat(stalkDelayMax, hauntLevel)
			)
		);
	}

	public static void scheduleEventWithDelay(final EntityPlayer player, final int delay) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (stalk(player, false)) {
					scheduleEventWithDelay(
						player,
						Algorithms.RANDOM.nextBetween(
							Algorithms.divideByFloat(stalkDelayMin, hauntLevel),
							Algorithms.divideByFloat(stalkDelayMax, hauntLevel)
						)
					);
				} else {
					// Retry if it is a bad time
					scheduleEventWithDelay(player, stalkRetryDelay);
				}
			},
			delay, TimeUnit.SECONDS
		);
	}

	public static void onShutdown() {
		for (final StalkingEntity herobrine : stalkingEntities) {
			herobrine.remove();
		}
		stalkingEntities.clear();
	}

	public static void onWorldTick(final WorldServer world) {
		if (stalkingEntities.isEmpty()) return;
		final List<EntityPlayer> players = world.getPlayers(EntityPlayer.class, new Predicate<EntityPlayer>() {
			@Override
			public boolean apply(@Nullable EntityPlayer input) {
				return true;
			}
		});
		if (players.isEmpty()) return;

		Iterator<StalkingEntity> it = stalkingEntities.iterator();
		StalkingEntity herobrine;
		while (it.hasNext()) {
			herobrine = it.next();
			final EntityPlayer player = herobrine.getTrackedPlayer();
			// Remove if player leaves or is in another dimension
			if (
				herobrine.shouldRemove() ||						// Remove if stalk event is finished
				player.getEntityWorld() != world ||		// Remove if player is in another dimension
				herobrine.isWithinDistanceOfPlayers(stalkClosePlayerDistanceMin)	// Remove if players are within a distance
			) {
				herobrine.remove();
				it.remove();
				continue;
			}

			herobrine.tick();
		}
	}

	public static boolean stalk(final EntityPlayer player, final boolean overrideHauntLevel) {
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < stalkHauntLevelMin) return true; // Reset event as if it passed
		}

        final World world = player.getEntityWorld();
		Vec3d spawnPos = Algorithms.getRandomPosNearEntity(player, stalkDistanceMin, stalkDistanceMax, false);
		final BlockPos playerBlockPos = player.getPosition();
		final BlockPos spawnBlockPos = Algorithms.getNearestStandableBlockPos(
			world,
			Algorithms.getBlockPosFromVec3d(spawnPos),
			playerBlockPos.getY() - stalkDistanceMin,
			playerBlockPos.getY() + stalkDistanceMin
		);
		spawnPos = new Vec3d(
			spawnPos.x,
			spawnBlockPos.up().getY(),
			spawnPos.z
		);

		if (!Algorithms.couldPlayerStandOnBlock(world, Algorithms.getBlockPosFromVec3d(spawnPos).down())) return false;

		final StalkingEntity herobrine = new StalkingEntity(world, "classic", player);
		herobrine.setPosition(spawnPos);
		herobrine.lookAt(player);
		herobrine.summon();
		stalkingEntities.add(herobrine);

		return true;
	}
}
