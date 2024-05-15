package com.sylvan.presence.event;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.concurrent.TimeUnit;

public class Footsteps {
	// Config
	public static boolean footstepsEnabled = true;		// Whether the footsteps event is active
	public static float footstepsHauntLevelMin = 1.5f;	// The minimum haunt level to play event
	private static int footstepsDelayMin = 60 * 45;		// The minimum delay between footstep events
	private static int footstepsDelayMax = 60 * 60 * 4;	// The maximum delay between footstep events
	private static int footstepsReflexMs = 500;		// The maximum time the footstep event can take (so that the player turns around right after they stop)
	private static int footstepsMaxReflexVariance = 150;	// The maximum addition to the reflex time (to add randomness to the footstep speed)
	public static int footstepsStepsMin = 1;		// The minimum number of footsteps to play
	public static int footstepsStepsMax = 5;		// The maximum number of footsteps to play
	private static int footstepsMsPerStepMax = 300;		// The maximum amount of time of each step in milliseconds (so that it doesn't sound like walking with fewer footsteps)
	private static int footstepsStepVarianceMax = 25;	// The maximum amount of time to add between each footstep (so the step cadence has randomness)

	public static void loadConfig() {
		try {
			footstepsEnabled = Presence.config.getOrSetValue("footstepsEnabled", footstepsEnabled).getAsBoolean();
			footstepsHauntLevelMin = Presence.config.getOrSetValue("footstepsHauntLevelMin", footstepsHauntLevelMin).getAsFloat();
			footstepsDelayMin = Presence.config.getOrSetValue("footstepsDelayMin", footstepsDelayMin).getAsInt();
			footstepsDelayMax = Presence.config.getOrSetValue("footstepsDelayMax", footstepsDelayMax).getAsInt();
			footstepsReflexMs = Presence.config.getOrSetValue("footstepsReflexMs", footstepsReflexMs).getAsInt();
			footstepsMaxReflexVariance = Presence.config.getOrSetValue("footstepsMaxReflexVariance", footstepsMaxReflexVariance).getAsInt();
			footstepsStepsMin = Presence.config.getOrSetValue("footstepsStepsMin", footstepsStepsMin).getAsInt();
			footstepsStepsMax = Presence.config.getOrSetValue("footstepsStepsMax", footstepsStepsMax).getAsInt();
			footstepsMsPerStepMax = Presence.config.getOrSetValue("footstepsMsPerStepMax", footstepsMsPerStepMax).getAsInt();
			footstepsStepVarianceMax = Presence.config.getOrSetValue("footstepsStepVarianceMax", footstepsStepVarianceMax).getAsInt();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for Footsteps.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void scheduleEvent(final PlayerEntity player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (player.isRemoved()) return;
				generateFootsteps(player, Math.max(1, Algorithms.RANDOM.nextBetween(footstepsStepsMin, footstepsStepsMax)), false);
				scheduleEvent(player);
			},
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(footstepsDelayMin, hauntLevel),
				Algorithms.divideByFloat(footstepsDelayMax, hauntLevel)
			), TimeUnit.SECONDS
		);
	}

	public static void generateFootsteps(final PlayerEntity player, final int footstepCount, final boolean overrideHauntLevel) {
		if (player.isRemoved() || footstepCount < 1) return;

		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < footstepsHauntLevelMin) return; // Reset event as if it passed
		}

		final int msPerStep = (
			(footstepCount > 2) ?
			Math.min(
				(footstepsReflexMs + Algorithms.RANDOM.nextBetween(0, footstepsMaxReflexVariance)) / footstepCount,
				footstepsMsPerStepMax
			) : (footstepsReflexMs + Algorithms.RANDOM.nextBetween(0, footstepsMaxReflexVariance)) / footstepCount
		);

		final BlockPos blockPos = player.getBlockPos().down();
		final Direction behindPlayer = player.getHorizontalFacing().getOpposite();
		int delay;
		// Play footstep on each block approaching the player
		for (int distance = footstepCount; distance > 0; --distance) {
			delay = (footstepCount - distance) * msPerStep + Algorithms.RANDOM.nextBetween(0, footstepsStepVarianceMax);
			final int blockDistance = distance;
			Events.scheduler.schedule(() -> playFootstep(player, blockPos.offset(behindPlayer, blockDistance)), delay, TimeUnit.MILLISECONDS);
		}
	}

	public static void playFootstep(final PlayerEntity player, BlockPos soundPos) {
		if (player.isRemoved()) return;

		final World world = player.getWorld();
		final BlockPos playerPos = player.getBlockPos();

		// Player must be able to stand on source block
		soundPos = Algorithms.getNearestStandableBlockPosTowardsEntity(
			player,
			soundPos,
			playerPos.getY() - footstepsStepsMax,
			playerPos.getY() + footstepsStepsMax
		);
		if (!Algorithms.couldPlayerStandOnBlock(world, soundPos)) return;

		// Play the sound of the block the footsteps on
		final SoundEvent stepSound = world.getBlockState(soundPos).getSoundGroup().getStepSound();
		world.playSound(null, soundPos, stepSound, SoundCategory.BLOCKS);
	}
}
