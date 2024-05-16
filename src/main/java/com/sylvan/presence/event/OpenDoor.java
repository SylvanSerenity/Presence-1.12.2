package com.sylvan.presence.event;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OpenDoor {
	// Config
	public static boolean openDoorEnabled = true;			// Whether the open door event is active
	public static float openDoorHauntLevelMin = 1.25f;		// The minimum haunt level to play event
	private static int openDoorDelayMin = 60 * 60;			// The minimum delay between open door events
	private static int openDoorDelayMax = 60 * 60 * 4;		// The maximum delay between open door events
	private static int openDoorRetryDelay = 60;			// The delay between retrying to open a door if the previous attempt failed
	private static int openDoorSearchRadius = 32;			// The search radius of finding doors to open. Higher values have exponential lag during the tick performing the search
	private static boolean openDoorNotSeenConstraint = true;	// Whether the constraint for making the door open only when not seen is active

	public static final ArrayList<Block> doorBlocks = new ArrayList<>();

	public static void loadConfig() {
		try {
			openDoorEnabled = Presence.config.getOrSetValue("openDoorEnabled", openDoorEnabled).getAsBoolean();
			openDoorHauntLevelMin = Presence.config.getOrSetValue("openDoorHauntLevelMin", openDoorHauntLevelMin).getAsFloat();
			openDoorDelayMin = Presence.config.getOrSetValue("openDoorDelayMin", openDoorDelayMin).getAsInt();
			openDoorDelayMax = Presence.config.getOrSetValue("openDoorDelayMax", openDoorDelayMax).getAsInt();
			openDoorRetryDelay = Presence.config.getOrSetValue("openDoorRetryDelay", openDoorRetryDelay).getAsInt();
			openDoorSearchRadius = Presence.config.getOrSetValue("openDoorSearchRadius", openDoorSearchRadius).getAsInt();
			openDoorNotSeenConstraint = Presence.config.getOrSetValue("openDoorNotSeenConstraint", openDoorNotSeenConstraint).getAsBoolean();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for OpenDoor.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void initEvent() {
		doorBlocks.add(Blocks.ACACIA_DOOR);
		doorBlocks.add(Blocks.BIRCH_DOOR);
		doorBlocks.add(Blocks.DARK_OAK_DOOR);
		doorBlocks.add(Blocks.JUNGLE_DOOR);
		doorBlocks.add(Blocks.OAK_DOOR);
		doorBlocks.add(Blocks.SPRUCE_DOOR);
	}

	public static void scheduleEvent(final EntityPlayer player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		scheduleEventWithDelay(
			player,
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(openDoorDelayMin, hauntLevel),
				Algorithms.divideByFloat(openDoorDelayMax, hauntLevel)
			)
		);
	}

	public static void scheduleEventWithDelay(final EntityPlayer player, final int delay) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (openDoor(player, false)) {
					scheduleEventWithDelay(
						player,
						Algorithms.RANDOM.nextBetween(
							Algorithms.divideByFloat(openDoorDelayMin, hauntLevel),
							Algorithms.divideByFloat(openDoorDelayMax, hauntLevel)
						)
					);
				} else {
					// Retry if it is a bad time
					scheduleEventWithDelay(player, openDoorRetryDelay);
				}
			},
			delay, TimeUnit.SECONDS
		);
	}

	public static boolean openDoor(final EntityPlayer player, final boolean overrideHauntLevel) {
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < openDoorHauntLevelMin) return true; // Reset event as if it passed
		}

		// Get nearest door position
		BlockPos nearestDoorPos = Algorithms.getNearestBlockToEntity(player, doorBlocks, openDoorSearchRadius);
		if (nearestDoorPos == null) return false;

		// Players must not see door open
		final World world = player.getEntityWorld();
		final List<? extends EntityPlayer> players = world.getPlayers();
		if (
			openDoorNotSeenConstraint && (
				Algorithms.couldBlockBeSeenByPlayers(players, nearestDoorPos) ||
				Algorithms.couldBlockBeSeenByPlayers(players, nearestDoorPos.up())
			)
		) return false;

		// Open door
		final IBlockState currentBlockState = world.getBlockState(nearestDoorPos);
		final BlockDoor doorBlock = (BlockDoor) currentBlockState.getBlock();
		doorBlock.setOpen(null, world, currentBlockState, nearestDoorPos, true);
		return true;
	}
}
