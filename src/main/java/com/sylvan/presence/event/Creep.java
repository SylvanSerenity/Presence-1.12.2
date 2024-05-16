package com.sylvan.presence.event;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.entity.CreepingEntity;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.network.ServerEntityPlayer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Creep {
	// Config
	public static boolean creepEnabled = true;		// Whether the creep event is active
	private static float creepHauntLevelMin = 2.25f;		// The minimum haunt level to play event
	private static int creepDelayMin = 60 * 45;		// The minimum delay between creep events
	private static int creepDelayMax = 60 * 60 * 3;		// The maximum delay between creep events
	private static int creepRetryDelay = 1;			// The delay between retrying creep events in case of failure
	public static int creepDistanceMin = 1;		// The minimum distance behind the player to summon Herobrine
	public static int creepDistanceMax = 1;		// The maximum distance behind the player to summon Herobrine
	public static int creepVerticalDistanceMax = 3;	// The maximum distance Herobrine can be above/below the player
	private static int creepReflexMs = 0;			// The time in milliseconds before Herobrine vanishes
	private static double creepLookAtThreshold = 0.25;	// The threshold at which the player will be considered looking at Herobrine. -1.0 is directly oppsotie, 1.0 is directly towards

	public static final List<CreepingEntity> creepingEntities = new ArrayList<>();

	public static void loadConfig() {
		try {
			creepEnabled = Presence.config.getOrSetValue("creepEnabled", creepEnabled).getAsBoolean();
			creepHauntLevelMin = Presence.config.getOrSetValue("creepHauntLevelMin", creepHauntLevelMin).getAsFloat();
			creepDelayMin = Presence.config.getOrSetValue("creepDelayMin", creepDelayMin).getAsInt();
			creepDelayMax = Presence.config.getOrSetValue("creepDelayMax", creepDelayMax).getAsInt();
			creepRetryDelay = Presence.config.getOrSetValue("creepRetryDelay", creepRetryDelay).getAsInt();
			creepDistanceMin = Presence.config.getOrSetValue("creepDistanceMin", creepDistanceMin).getAsInt();
			creepDistanceMax = Presence.config.getOrSetValue("creepDistanceMax", creepDistanceMax).getAsInt();
			creepVerticalDistanceMax = Presence.config.getOrSetValue("creepVerticalDistanceMax", creepVerticalDistanceMax).getAsInt();
			creepReflexMs = Presence.config.getOrSetValue("creepReflexMs", creepReflexMs).getAsInt();
			creepLookAtThreshold = Presence.config.getOrSetValue("creepLookAtThreshold", creepLookAtThreshold).getAsDouble();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for Creep.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void scheduleEvent(final EntityPlayer player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		scheduleEventWithDelay(
			player,
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(creepDelayMax, hauntLevel),
				Algorithms.divideByFloat(creepDelayMax, hauntLevel)
			)
		);
	}

	public static void scheduleEventWithDelay(final EntityPlayer player, final int delay) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (creep(player, false)) {
					scheduleEventWithDelay(
						player,
						Algorithms.RANDOM.nextBetween(
							Algorithms.divideByFloat(creepDelayMin, hauntLevel),
							Algorithms.divideByFloat(creepDelayMax, hauntLevel)
						)
					);
				} else {
					// Retry if it is a bad time
					scheduleEventWithDelay(player, creepRetryDelay);
				}
			},
			delay, TimeUnit.SECONDS
		);
	}

	public static void onShutdown() {
		for (final CreepingEntity herobrine : creepingEntities) {
			herobrine.remove();
		}
		creepingEntities.clear();
	}

	public static void onWorldTick(final ServerWorld world) {
		if (creepingEntities.isEmpty()) return;
		final List<ServerEntityPlayer> players = world.getPlayers();
		if (players.isEmpty()) return;

		Iterator<CreepingEntity> it = creepingEntities.iterator();
		CreepingEntity herobrine;
		while (it.hasNext()) {
			herobrine = it.next();
			final EntityPlayer player = herobrine.getTrackedPlayer();
			// Remove if player leaves or is in another dimension
			if (player.isRemoved() || player.getWorld().getDimension() != world.getDimension()) {
				herobrine.remove();
				it.remove();
				continue;
			}

			// Remove if seen
			if (herobrine.isSeenByPlayers(creepLookAtThreshold)) {
				if (creepReflexMs > 0) herobrine.scheduleRemoval(creepReflexMs);
				else herobrine.remove();
				it.remove();
				continue;
			}

			herobrine.tick();
		}
	}

	public static boolean creep(final EntityPlayer player, final boolean overrideHauntLevel) {
		if (player.isRemoved()) return false;
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < creepHauntLevelMin) return true; // Reset event as if it passed
		}

		final World world = player.getWorld();
		// Get the block behind the player
		final BlockPos playerBlockPos = player.getBlockPos();
		Vec3d spawnPos = Algorithms.getPosOffsetInDirection(
			player.getPos(),
			player.getRotationVector().negate(),
			Algorithms.RANDOM.nextBetween(creepDistanceMin, creepDistanceMax)
		);
		final BlockPos spawnBlockPos = Algorithms.getNearestStandableBlockPos(
			player.getWorld(),
			Algorithms.getBlockPosFromVec3d(spawnPos),
			playerBlockPos.getY() - creepVerticalDistanceMax,
			playerBlockPos.getY() + creepVerticalDistanceMax
		);
		spawnPos = new Vec3d(
			spawnPos.getX(),
			spawnBlockPos.up().getY(), // Keep X/Z offset
			spawnPos.getZ()
		);
		if (!Algorithms.couldPlayerStandOnBlock(world, Algorithms.getBlockPosFromVec3d(spawnPos).down())) return false;

		final CreepingEntity herobrine = new CreepingEntity(world, "smile", player);
		herobrine.setPosition(spawnPos);
		herobrine.lookAt(player);
		herobrine.summon();
		creepingEntities.add(herobrine);

		return true;
	}
}
