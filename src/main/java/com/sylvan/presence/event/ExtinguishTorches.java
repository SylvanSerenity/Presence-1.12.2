package com.sylvan.presence.event;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ExtinguishTorches {
	public static Map<UUID, Map.Entry<DimensionType, Stack<BlockPos>>> torchPlacementMap = new HashMap<>();

	// Config
	public static boolean extinguishTorchesEnabled = true;				// Whether the extinguish torches event is active
	public static float extinguishTorchesHauntLevelMin = 1.5f;			// The minimum haunt level to play event
	private static int extinguishTorchesTrackDelayMin = 60 * 30;			// The minimum delay between beginning to track torches
	private static int extinguishTorchesTrackDelayMax = 60 * 60 * 5;		// The maximum delay between beginning to track torches
	private static int extinguishTorchesExtinguishRetryDelay = 60 * 10;		// The delay between tries to extinguish tracked torches
	public static int extinguishTorchesTrackedMax = 16;				// The maximum number of torches to track
	public static boolean extinguishTorchesMaxDistanceConstraint = true;		// Whether the torch must be within a certain distance of the last torch to be tracked
	public static int extinguishTorchesTorchDistanceMax = 32;			// The maximum distance between the last tracked torch (so that the torches are in the same cave)
	public static boolean extinguishTorchesMaxSkyLightLevelConstraint = true;	// Whether torches are not tracked if they are placed on a block over a certain skylight level
	public static int extinguishTorchesSkyLightLevelMax = 6;			// The maximum light level to determine whether to track the placed torch (so that surface torches are not tracked)
	public static boolean extinguishTorchesSeenConstraint = true;			// Whether torches that are in eyesight of a player are removed

	public static void loadConfig() {
		try {
			extinguishTorchesEnabled = Presence.config.getOrSetValue("extinguishTorchesEnabled", extinguishTorchesEnabled).getAsBoolean();
			extinguishTorchesHauntLevelMin = Presence.config.getOrSetValue("extinguishTorchesHauntLevelMin", extinguishTorchesHauntLevelMin).getAsFloat();
			extinguishTorchesTrackDelayMin = Presence.config.getOrSetValue("extinguishTorchesTrackDelayMin", extinguishTorchesTrackDelayMin).getAsInt();
			extinguishTorchesTrackDelayMax = Presence.config.getOrSetValue("extinguishTorchesTrackDelayMax", extinguishTorchesTrackDelayMax).getAsInt();
			extinguishTorchesExtinguishRetryDelay = Presence.config.getOrSetValue("extinguishTorchesExtinguishRetryDelay", extinguishTorchesExtinguishRetryDelay).getAsInt();
			extinguishTorchesTrackedMax = Presence.config.getOrSetValue("extinguishTorchesTrackedMax", extinguishTorchesTrackedMax).getAsInt();
			extinguishTorchesMaxDistanceConstraint = Presence.config.getOrSetValue("extinguishTorchesMaxDistanceConstraint", extinguishTorchesMaxDistanceConstraint).getAsBoolean();
			extinguishTorchesTorchDistanceMax = Presence.config.getOrSetValue("extinguishTorchesTorchDistanceMax", extinguishTorchesTorchDistanceMax).getAsInt();
			extinguishTorchesMaxSkyLightLevelConstraint = Presence.config.getOrSetValue("extinguishTorchesMaxSkyLightLevelConstraint", extinguishTorchesMaxSkyLightLevelConstraint).getAsBoolean();
			extinguishTorchesSkyLightLevelMax = Presence.config.getOrSetValue("extinguishTorchesSkyLightLevelMax", extinguishTorchesSkyLightLevelMax).getAsInt();
			extinguishTorchesSeenConstraint = Presence.config.getOrSetValue("extinguishTorchesSeenConstraint", extinguishTorchesSeenConstraint).getAsBoolean();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for ExtinguishTorches.java. Wiping and using default.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void scheduleTracking(final PlayerEntity player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (player.isRemoved() || torchPlacementMap.containsKey(player.getUuid())) return;
				if (!startTrackingTorches(player, false)) {
					scheduleTracking(player);
				}
			},
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(extinguishTorchesTrackDelayMin, hauntLevel),
				Algorithms.divideByFloat(extinguishTorchesTrackDelayMax, hauntLevel)
			),
			TimeUnit.SECONDS
		);
	}

	public static boolean startTrackingTorches(final PlayerEntity player, final boolean overrideHauntLevel) {
		if (player.isRemoved() || torchPlacementMap.containsKey(player.getUuid())) return false;

		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < extinguishTorchesHauntLevelMin) return true; // Reset event as if it passed
		}

		torchPlacementMap.put(player.getUuid(), new AbstractMap.SimpleEntry<>(player.getWorld().getDimension(), new Stack<>()));
		scheduleExtinguish(player);
		return true;
	}

	public static void scheduleExtinguish(final PlayerEntity player) {
		Events.scheduler.schedule(() -> {
			if (player.isRemoved()) {
				extinguishTrackedTorches(player);
				return;
			}

			if (torchPlacementMap.containsKey(player.getUuid()) && !extinguishTrackedTorches(player)) {
				// Wait for torches to be placed if first try yielded no results
				scheduleExtinguish(player);
			} else {
				scheduleTracking(player);
			}
		}, extinguishTorchesExtinguishRetryDelay, TimeUnit.SECONDS);
	}

	public static void onUseBlock(final PlayerEntity player, final World world, final BlockHitResult hitResult) {
		if (!extinguishTorchesEnabled) return;

		final BlockPos torchPos = hitResult.getBlockPos().offset(hitResult.getSide()); // Offset by 1 block in the direction of torch placement
		if (
			(player.getMainHandStack().getItem() != Items.TORCH && player.getOffHandStack().getItem() != Items.TORCH) ||	// Player must be holding a torch
			(
				extinguishTorchesMaxSkyLightLevelConstraint &&
				world.getLightLevel(LightType.SKY, torchPos) > extinguishTorchesSkyLightLevelMax	// Torch must be underground
			)
		) return;

		if (torchPlacementMap.containsKey(player.getUuid())) {
			final Map.Entry<DimensionType, Stack<BlockPos>> entry = torchPlacementMap.get(player.getUuid());
			if (entry.getKey() != world.getDimension()) {
				// Restart if not in the same dimension
				extinguishTrackedTorches(player);
				startTrackingTorches(player, false);
				return;
			}

			final Stack<BlockPos> torches = entry.getValue();
			// Torch must be within extinguishTorchesTorchDistanceMax blocks from last torch
			if (
				extinguishTorchesMaxDistanceConstraint &&
				!torches.empty() &&
				!torches.peek().isWithinDistance(torchPos, extinguishTorchesTorchDistanceMax)
			) return;

			if (torches.size() >= extinguishTorchesTrackedMax) {
				// Remove bottom of the stack to make room
				torches.remove(0);
			}
			torches.push(torchPos);
		}
	}

	public static boolean extinguishTrackedTorches(final PlayerEntity player) {
		if (!player.isRemoved()) {
			// Player must be tracked
			if (!torchPlacementMap.containsKey(player.getUuid())) return false;

			final Map.Entry<DimensionType, Stack<BlockPos>> entry = torchPlacementMap.get(player.getUuid());
			final Stack<BlockPos> torchStack = entry.getValue();
			final World world = player.getWorld();
			// Player must be in same dimension
			if (entry.getKey() != world.getDimension()) {
				// Quit tracking if dimensions do not match
				torchStack.clear();
				torchPlacementMap.remove(player.getUuid());
				return false;
			}
			// Player must have tracked torches
			if (torchStack.empty()) return false; // Wait until next attempt

			Block block;
			for (BlockPos torchPos : torchStack) {
				block = world.getBlockState(torchPos).getBlock();
				if (
					!((block == Blocks.TORCH) || (block == Blocks.WALL_TORCH)) ||								// Block must still be a torch
					(extinguishTorchesSeenConstraint && Algorithms.couldPosBeSeenByPlayers(world.getPlayers(), torchPos.toCenterPos()))	// Player cannot see the torch being removed
				) continue;
				// Remove the torch
				world.removeBlock(torchPos, false);
			}

			torchStack.clear();
		}
		torchPlacementMap.remove(player.getUuid());
		return true;
	}
}
