package com.sylvan.presence.entity;

import com.sylvan.presence.event.Creep;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class CreepingEntity extends HerobrineEntity {
	private final PlayerEntity trackedPlayer;

	public CreepingEntity(World world, String skin, PlayerEntity trackedPlayer) {
		super(world, skin);
		this.trackedPlayer = trackedPlayer;
	}

	public void tick() {
		final World world = trackedPlayer.getWorld();

		// Inch forward toward player
		// Pretend player and Herobrine are on the same block to prevent direction from being dependent on Y-axis
		final Vec3d playerXZ = new Vec3d(
			trackedPlayer.getPos().getX(),
			0,
			trackedPlayer.getPos().getZ()
		);
		final Vec3d herobrineXZ = new Vec3d(
			this.getPos().getX(),
			0,
			this.getPos().getZ()
		);
		final double playerDistanceXZ = playerXZ.distanceTo(herobrineXZ);
		final Vec3d towardsPlayer = Algorithms.getDirectionPosToPos(
			herobrineXZ,
			playerXZ
		);

		// Calculate spawn position
		Vec3d spawnPos = Algorithms.getPosOffsetInDirection(
			this.getPos(),
			towardsPlayer,
			(float) Math.max(
				0,
				playerDistanceXZ - Algorithms.RANDOM.nextBetween(Creep.creepDistanceMin, Creep.creepDistanceMax)
			)
		);
		final BlockPos spawnBlockPos = Algorithms.getNearestStandableBlockPos(
			world,
			Algorithms.getBlockPosFromVec3d(spawnPos),
			trackedPlayer.getBlockPos().getY() - Creep.creepVerticalDistanceMax,
			trackedPlayer.getBlockPos().getY() + Creep.creepVerticalDistanceMax
		);
		spawnPos = new Vec3d(
			spawnPos.getX(),
			spawnBlockPos.up().getY(), // Spawn on block while keeping X/Z offset
			spawnPos.getZ()
		);
		if (!Algorithms.couldPlayerStandOnBlock(world, Algorithms.getBlockPosFromVec3d(spawnPos).down())) return;

		// Set position and look at player
		this.moveTo(spawnPos);
		this.lookAt(trackedPlayer);
	}

	public PlayerEntity getTrackedPlayer() {
		return trackedPlayer;
	}
}
