package com.sylvan.presence.event;

import com.google.common.base.Predicate;
import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FlowerGift {
	// Config
	public static boolean flowerGiftEnabled = true;			// Whether the flower gift event is active
	public static float flowerGiftHauntLevelMin = 1.5f;		// The minimum haunt level to play event
	private static int flowerGiftDelayMin = 60 * 60;		// The minimum delay between flower gift events
	private static int flowerGiftDelayMax = 60 * 60 * 8;		// The maximum delay between flower gift events
	private static int flowerGiftRetryDelay = 60;			// The delay between retrying to place flower if the previous attempt failed
	private static int flowerGiftSearchRadius = 32;			// The search radius of finding doors to plant a flower at. Higher values have exponential lag during the tick performing the search
	private static boolean flowerGiftNotSeenConstraint = true;	// Whether the constraint for making the flower plant only when not seen is active

	public static final ArrayList<Block> plantableBlocks = new ArrayList<>();

	public static void loadConfig() {
		try {
			flowerGiftEnabled = Presence.config.getOrSetValue("flowerGiftEnabled", flowerGiftEnabled).getAsBoolean();
			flowerGiftHauntLevelMin = Presence.config.getOrSetValue("flowerGiftHauntLevelMin", flowerGiftHauntLevelMin).getAsFloat();
			flowerGiftDelayMin = Presence.config.getOrSetValue("flowerGiftDelayMin", flowerGiftDelayMin).getAsInt();
			flowerGiftDelayMax = Presence.config.getOrSetValue("flowerGiftDelayMax", flowerGiftDelayMax).getAsInt();
			flowerGiftRetryDelay = Presence.config.getOrSetValue("flowerGiftRetryDelay", flowerGiftRetryDelay).getAsInt();
			flowerGiftSearchRadius = Presence.config.getOrSetValue("flowerGiftSearchRadius", flowerGiftSearchRadius).getAsInt();
			flowerGiftNotSeenConstraint = Presence.config.getOrSetValue("flowerGiftNotSeenConstraint", flowerGiftNotSeenConstraint).getAsBoolean();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for FlowerGift.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void initEvent() {
		plantableBlocks.add(Blocks.GRASS);
		plantableBlocks.add(Blocks.DIRT);
		plantableBlocks.add(Blocks.MYCELIUM);
	}

	public static void scheduleEvent(final EntityPlayer player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		scheduleEventWithDelay(
			player,
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(flowerGiftDelayMin, hauntLevel),
				Algorithms.divideByFloat(flowerGiftDelayMax, hauntLevel)
			)
		);
	}

	public static void scheduleEventWithDelay(final EntityPlayer player, final int delay) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (flowerGift(player, false)) {
					scheduleEventWithDelay(
						player,
						Algorithms.RANDOM.nextBetween(
							Algorithms.divideByFloat(flowerGiftDelayMin, hauntLevel),
							Algorithms.divideByFloat(flowerGiftDelayMax, hauntLevel)
						)
					);
				} else {
					// Retry if it is a bad time
					scheduleEventWithDelay(player, flowerGiftRetryDelay);
				}
			},
			delay, TimeUnit.SECONDS
		);
	}

	public static boolean flowerGift(final EntityPlayer player, final boolean overrideHauntLevel) {
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < flowerGiftHauntLevelMin) return true; // Reset event as if it passed
		}

		// Get nearest door position
		BlockPos nearestDoorPos = Algorithms.getNearestBlockToEntity(player, OpenDoor.doorBlocks, flowerGiftSearchRadius);
		if (nearestDoorPos == null) return false;

		// Make sure to select the bottom half of the door
		final World world = player.getEntityWorld();
		final BlockState currentBlockState = world.getBlockState(nearestDoorPos);
		if (currentBlockState.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER) nearestDoorPos = nearestDoorPos.down(2);
		else nearestDoorPos = nearestDoorPos.down();

		// Get grass block 1 distance in any X/Z block away
		BlockPos plantablePos = nearestDoorPos.offset(Direction.NORTH);
		if (!(
			Algorithms.isBlockOfBlockTypes(world.getBlockState(plantablePos).getBlock(), plantableBlocks) &&
			Algorithms.couldPlayerStandOnBlock(world, plantablePos)
		)) {
			plantablePos = nearestDoorPos.offset(Direction.SOUTH);
			if (!(
				Algorithms.isBlockOfBlockTypes(world.getBlockState(plantablePos).getBlock(), plantableBlocks) &&
				Algorithms.couldPlayerStandOnBlock(world, plantablePos)
			)) {
				plantablePos = nearestDoorPos.offset(Direction.EAST);
				if (!(
					Algorithms.isBlockOfBlockTypes(world.getBlockState(plantablePos).getBlock(), plantableBlocks) &&
					Algorithms.couldPlayerStandOnBlock(world, plantablePos)
				)) {
					plantablePos = nearestDoorPos.offset(Direction.WEST);
					if (!(
						Algorithms.isBlockOfBlockTypes(world.getBlockState(plantablePos).getBlock(), plantableBlocks) &&
						Algorithms.couldPlayerStandOnBlock(world, plantablePos)
					)) {
						return false;
					}
				}
			}
		}
		plantablePos = plantablePos.up();

		// Players must not see flower get placed
		final List<? extends EntityPlayer> players = world.getPlayers(EntityPlayer.class, new Predicate<EntityPlayer>() {
			@Override
			public boolean apply(@Nullable EntityPlayer input) {
				return true;
			}
		});
		if (flowerGiftNotSeenConstraint && Algorithms.couldBlockBeSeenByPlayers(players, plantablePos)) return false;

		// Plant poppy
		world.setBlockState(plantablePos, Blocks.RED_FLOWER.getDefaultState());

		return true;
	}
}
