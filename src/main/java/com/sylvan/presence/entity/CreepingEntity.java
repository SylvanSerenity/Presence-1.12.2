package com.sylvan.presence.entity;

import com.sylvan.presence.event.Creep;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class CreepingEntity extends HerobrineEntity {
	private final EntityPlayer trackedPlayer;

	public CreepingEntity(World world, String skin, EntityPlayer trackedPlayer) {
		super(world, skin);
		this.trackedPlayer = trackedPlayer;
	}

	public void tick() {
		final World world = trackedPlayer.getEntityWorld();

		// Inch forward toward player
		// Pretend player and Herobrine are on the same block to prevent direction from being dependent on Y-axis
		final Vec3d playerXZ = new Vec3d(
			trackedPlayer.getPositionVector().x,
			0,
			trackedPlayer.getPositionVector().z
		);
		final Vec3d herobrineXZ = new Vec3d(
			this.getPos().x,
			0,
			this.getPos().z
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
			trackedPlayer.getPosition().getY() - Creep.creepVerticalDistanceMax,
			trackedPlayer.getPosition().getY() + Creep.creepVerticalDistanceMax
		);
		spawnPos = new Vec3d(
			spawnPos.x,
			spawnBlockPos.up().getY(), // Spawn on block while keeping X/Z offset
			spawnPos.z
		);
		if (!Algorithms.couldPlayerStandOnBlock(world, Algorithms.getBlockPosFromVec3d(spawnPos).down())) return;

		// Set position and look at player
		this.moveTo(spawnPos);
		this.lookAt(trackedPlayer);
	}

	public EntityPlayer getTrackedPlayer() {
		return trackedPlayer;
	}
}
