package com.sylvan.presence.event;

import com.google.common.base.Predicate;
import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrampleCrops {
	// Config
	public static boolean trampleCropsEnabled = true;		// Whether the trample crops event is active
	public static float trampleCropsHauntLevelMin = 1.75f;		// The minimum haunt level to play event
	private static int trampleCropsDelayMin = 60 * 60 * 2;		// The minimum delay between trample crops events
	private static int trampleCropsDelayMax = 60 * 60 * 4;		// The maximum delay between trample crops events
	private static int trampleCropsRetryDelay = 60;			// The delay between retrying to trample crops if the previous attempt failed
	private static int trampleCropsSearchRadius = 32;		// The search radius of finding crops to trample. Higher values have exponential lag during the tick performing the search
	private static boolean trampleCropsNotSeenConstraint = true;	// Whether the constraint for making the crops trample only when not seen is active

	public static final ArrayList<Block> cropBlocks = new ArrayList<>();

	public static void loadConfig() {
		try {
			trampleCropsEnabled = Presence.config.getOrSetValue("trampleCropsEnabled", trampleCropsEnabled).getAsBoolean();
			trampleCropsHauntLevelMin = Presence.config.getOrSetValue("trampleCropsHauntLevelMin", trampleCropsHauntLevelMin).getAsFloat();
			trampleCropsDelayMin = Presence.config.getOrSetValue("trampleCropsDelayMin", trampleCropsDelayMin).getAsInt();
			trampleCropsDelayMax = Presence.config.getOrSetValue("trampleCropsDelayMax", trampleCropsDelayMax).getAsInt();
			trampleCropsRetryDelay = Presence.config.getOrSetValue("trampleCropsRetryDelay", trampleCropsRetryDelay).getAsInt();
			trampleCropsSearchRadius = Presence.config.getOrSetValue("trampleCropsSearchRadius", trampleCropsSearchRadius).getAsInt();
			trampleCropsNotSeenConstraint = Presence.config.getOrSetValue("trampleCropsNotSeenConstraint", trampleCropsNotSeenConstraint).getAsBoolean();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for TrampleCrops.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void initEvent() {
		cropBlocks.add(Blocks.WHEAT);
		cropBlocks.add(Blocks.CARROTS);
		cropBlocks.add(Blocks.POTATOES);
		cropBlocks.add(Blocks.BEETROOTS);
	}

	public static void scheduleEvent(final EntityPlayer player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		scheduleEventWithDelay(
			player,
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(trampleCropsDelayMin, hauntLevel),
				Algorithms.divideByFloat(trampleCropsDelayMax, hauntLevel)
			)
		);
	}

	public static void scheduleEventWithDelay(final EntityPlayer player, final int delay) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (trampleCrops(player, false)) {
					scheduleEventWithDelay(
						player,
						Algorithms.RANDOM.nextBetween(
							Algorithms.divideByFloat(trampleCropsDelayMin, hauntLevel),
							Algorithms.divideByFloat(trampleCropsDelayMax, hauntLevel)
						)
					);
				} else {
					// Retry if it is a bad time
					scheduleEventWithDelay(player, trampleCropsRetryDelay);
				}
			},
			delay, TimeUnit.SECONDS
		);
	}

	public static boolean trampleCrops(final EntityPlayer player, final boolean overrideHauntLevel) {
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < trampleCropsHauntLevelMin) return true; // Reset event as if it passed
		}

		// Get nearest crop position
		BlockPos nearestCropPos = Algorithms.getNearestBlockToEntity(player, cropBlocks, trampleCropsSearchRadius);
		if (nearestCropPos == null) return false;

		// Players must not see flower get placed
		final World world = player.getEntityWorld();
		final List<? extends EntityPlayer> players = world.getPlayers(EntityPlayer.class, new Predicate<EntityPlayer>() {
			@Override
			public boolean apply(@Nullable EntityPlayer input) {
				return true;
			}
		});
		if (trampleCropsNotSeenConstraint && Algorithms.couldBlockBeSeenByPlayers(players, nearestCropPos)) return false;

		// Plant poppy
		world.setBlockToAir(nearestCropPos);

		return true;
	}
}
