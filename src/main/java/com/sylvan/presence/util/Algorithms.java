package com.sylvan.presence.util;

import com.sylvan.presence.Presence;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.List;
import java.util.Map;

public class Algorithms {
	public static final RandomHelper RANDOM = new RandomHelper();

	private static int algorithmsCaveDetectionRays = 30;						// The amount of rays to shoot in random directions to determine whether an entity is in a cave
	private static float algorithmsCaveDetectionMaxNonCaveBlockPercent = 0.0f;	// The percent of blocks a cave detection ray can collide with that are not usually found in a cave before assuming player is in a base

	public static void loadConfig() {
		try {
			algorithmsCaveDetectionRays = Presence.config.getOrSetValue("algorithmsCaveDetectionRays", algorithmsCaveDetectionRays).getAsInt();
			algorithmsCaveDetectionMaxNonCaveBlockPercent = Presence.config.getOrSetValue("algorithmsCaveDetectionMaxNonCaveBlockPercent", algorithmsCaveDetectionMaxNonCaveBlockPercent).getAsFloat();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for Algorithms.java. Wiping and using default.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static float randomBetween(final float min, final float max) {
		return min + RANDOM.nextFloat() * (max - min);
	}

	public static int divideByFloat(final int dividend, final float divisor) {
		return (int) (((float) dividend) / Math.max(0.001f, divisor));
	}

	public static <K> K randomKeyFromWeightMap(final Map<K, Float> keyWeightMap) {
		final float totalWeight = keyWeightMap.values().stream().reduce(0.0f, Float::sum);
		float randomValue = RANDOM.nextFloat() * totalWeight;

		for (final K key : keyWeightMap.keySet()) {
			randomValue -= keyWeightMap.get(key);
			if (randomValue <= 0.0f) {
				return key;
			}
		}

		// This should not happen unless the list is empty or total weight is 0
		return keyWeightMap.keySet().iterator().next();
	}

	public static BlockPos getBlockPosFromVec3d(final Vec3d vec3d) {
		return new BlockPos(
			(int) Math.round(vec3d.x),
			(int) Math.round(vec3d.y),
			(int) Math.round(vec3d.z)
		);
	}
	public static Vec3d getVec3dFromBlockPos(final BlockPos blockPos) {
		return new Vec3d(
				blockPos.getX() + 0.5,
				blockPos.getY() + 0.5,
				blockPos.getZ() + 0.5
		);
	}

	public static Vec3d multiplyVec3d(final Vec3d a, final Vec3d b) {
		return new Vec3d(
				a.x * b.x,
				a.y * b.y,
				a.z * b.z
		);
	}
	public static Vec3d multiplyVec3d(final Vec3d a, final int b) {
		return new Vec3d(
				a.x * b,
				a.y * b,
				a.z * b
		);
	}
	public static Vec3d multiplyVec3d(final Vec3d a, final float b) {
		return new Vec3d(
				a.x * b,
				a.y * b,
				a.z * b
		);
	}

	public static ResourceLocation getIdentifierFromString(final String identifier) {
		String namespace, name;
		String[] parts = identifier.split(":", 2);
		if (parts.length == 2) {
			namespace = parts[0];
			name = parts[1];
		} else if (parts.length == 1) {
			namespace = "minecraft";
			name = parts[0];
		} else {
			Presence.LOGGER.warn("Invalid sound key \"" + identifier + "\".");
			return null;
		}
		return new ResourceLocation(namespace, name);
	}

	public static RayTraceResult castRayFromEye(final Entity entity, final Vec3d pos) {
		return entity.getEntityWorld().rayTraceBlocks(
			entity.getPositionEyes(1),
			pos
		);
	}

	public static boolean couldPosBeSeenByEntity(final Entity entity, final Vec3d pos) {
		// Check max distance before calculating
		if (entity.getPositionVector().distanceTo(pos) > 127.0) return false;

		// Check if behind transparent block
		final RayTraceResult hitResult = castRayFromEye(entity, pos);
        return hitResult.typeOfHit != RayTraceResult.Type.BLOCK ||
                !entity.getEntityWorld().getBlockState(hitResult.getBlockPos()).isOpaqueCube();
    }

	public static boolean couldPosBeSeenByPlayers(final List<? extends EntityPlayer> players, final Vec3d pos) {
		for (final EntityPlayer player : players) {
			if (couldPosBeSeenByEntity(player, pos)) return true;
		}
		return false;
	}

	public static boolean couldBlockBeSeenByPlayers(final List<? extends EntityPlayer> players, final BlockPos pos) {
		Vec3d towardsPlayerDirection;
		BlockPos towardsPlayerPos;
		for (final EntityPlayer player : players) {
			// Move one block towards player to prevent the block itself from blocking raycast
			towardsPlayerDirection = getDirectionPosToPos(getVec3dFromBlockPos(pos), player.getPositionEyes(1));

			towardsPlayerPos = pos.add(
				(int) Math.signum(towardsPlayerDirection.x),
				(int) Math.signum(towardsPlayerDirection.y),
				(int) Math.signum(towardsPlayerDirection.z)
			);

			if (couldPosBeSeenByEntity(player, getVec3dFromBlockPos(towardsPlayerPos))) return true;
		}
		return false;
	}

	public static boolean isPositionLookedAtByEntity(final Entity entity, final Vec3d pos, final double dotProductThreshold) {
		if (!couldPosBeSeenByEntity(entity, pos)) return false;
		final Vec3d lookingDirection = entity.getLookVec(); // Where entity is actually looking
		final Vec3d lookAtDirection = getDirectionPosToPos(entity.getPositionEyes(1), pos); // Where entity should be looking
		final double dotProduct = lookingDirection.dotProduct(lookAtDirection);
		return dotProduct > dotProductThreshold;
	}

	public static boolean couldPlayerStandOnBlock(final World world, final BlockPos blockPos) {
		// Check for opaque blocks
		if (
			!world.getBlockState(blockPos).isOpaqueCube() ||
			world.getBlockState(blockPos.up()).isOpaqueCube() ||
			world.getBlockState(blockPos.up(2)).isOpaqueCube()
		) return false;

		// Cast ray from legs position
		Vec3d blockCenterPos = getVec3dFromBlockPos(blockPos.up());
		Vec3d outsideBlockPos = blockCenterPos.add(new Vec3d(0.45, 0.45, 0.45));
		RayTraceResult hitResult = world.rayTraceBlocks(
			blockCenterPos,
			outsideBlockPos
		);
        assert hitResult != null;
        if (hitResult.typeOfHit != RayTraceResult.Type.MISS) return false;

		// Cast ray from eye position
		blockCenterPos = blockCenterPos.add(new Vec3d(0, 1, 0));
		outsideBlockPos = outsideBlockPos.add(new Vec3d(0, 1, 0));
		hitResult = world.rayTraceBlocks(
			blockCenterPos,
			outsideBlockPos
		);
        assert hitResult != null;
        return hitResult.typeOfHit == RayTraceResult.Type.MISS;
    }

	public static BlockPos getNearestStandableBlockPos(final World world, BlockPos blockPos, final int minY, final int maxY) {
		while (!couldPlayerStandOnBlock(world, blockPos) && (blockPos.getY() >= minY)) {
			blockPos = blockPos.down();
		}
		while (!couldPlayerStandOnBlock(world, blockPos) && (blockPos.getY() <= maxY)) {
			blockPos = blockPos.up();
		}
		return blockPos;
	}

	public static BlockPos getNearestStandableBlockPosTowardsEntity(final Entity entity, BlockPos blockPos, final int minY, final int maxY) {
		final World world = entity.getEntityWorld();
		final BlockPos entityPos = entity.getPosition();
		if (blockPos.getY() > entityPos.getY()) {
			// Above player, try moving down first
			while (!couldPlayerStandOnBlock(world, blockPos) && (blockPos.getY() >= minY)) {
				blockPos = blockPos.down();
			}
			while (!couldPlayerStandOnBlock(world, blockPos) && (blockPos.getY() <= maxY)) {
				blockPos = blockPos.up();
			}
		} else {
			// Below player, try moving up first
			while (!couldPlayerStandOnBlock(world, blockPos) && (blockPos.getY() <= maxY)) {
				blockPos = blockPos.up();
			}
			while (!couldPlayerStandOnBlock(world, blockPos) && (blockPos.getY() >= minY)) {
				blockPos = blockPos.down();
			}
		}
		return blockPos;
	}

	public static Vec3d getRandomDirection(final boolean randomY) {
		return new Vec3d(
			RANDOM.nextDouble() * 2 - 1,
			randomY ? (RANDOM.nextDouble() * 2 - 1) : 0,
			RANDOM.nextDouble() * 2 - 1
		).normalize();
	}

	public static EnumFacing getBlockDirectionFromEntity(final Entity entity, final BlockPos blockPos) {
		final Vec3d entityPos = entity.getPositionVector();
		final Vec3d direction = new Vec3d(
			blockPos.getX() - entityPos.x,
			blockPos.getY() - entityPos.y,
			blockPos.getZ() - entityPos.z
		).normalize();
		return EnumFacing.fromAngle( // TODO From Euler angle
			(int) direction.x,
			(int) direction.y,
			(int) direction.z
		);
	}

	public static Vec3d getRandomPosNearEntity(final Entity entity, final int distanceMin, final int distanceMax, final boolean randomY) {
		final Vec3d randomDirection = getRandomDirection(randomY);
		final int distance = RANDOM.nextBetween(distanceMin, distanceMax);

		// Scale the direction vector by the random distance magnitude
		final Vec3d randomOffset = multiplyVec3d(randomDirection, distance);
		return entity.getPositionVector().add(randomOffset);
	}

	public static BlockPos getRandomStandableBlockNearEntity(final Entity entity, final int distanceMin, final int distanceMax, final int maxAttempts, final boolean randomY) {
		final Vec3d entityPos = entity.getPositionVector();
		final int moveDistance = Math.max(distanceMin, distanceMax - distanceMin);
		final int maxDistanceDown = (int) entityPos.y - moveDistance;
		final int maxDistanceUp = (int) entityPos.x + moveDistance;

		// Start with random block and check maxAttempt times
		BlockPos blockPos = getBlockPosFromVec3d(getRandomPosNearEntity(entity, distanceMin, distanceMax, randomY));
		double distance;
		for (int i = 0; i < maxAttempts; ++i) {
			// Move to nearest standable block
			blockPos = getNearestStandableBlockPosTowardsEntity(entity, blockPos, maxDistanceDown, maxDistanceUp);
			// Return if blockPos is within constraints
			distance = getVec3dFromBlockPos(blockPos).distanceTo(entityPos);
			if (distance >= distanceMin && distance <= distanceMax) return blockPos;
			// Try again
			blockPos = getBlockPosFromVec3d(getRandomPosNearEntity(entity, distanceMin, distanceMax, randomY));
		}

		// If nothing is found in 50 attempts, just select a block in the wall
		return blockPos;
	}

	public static boolean isCaveBlockSound(final SoundType sound) {
		return (
			sound == SoundType.STONE ||
			sound == SoundType.GROUND
		);
	}

	public static boolean isEntityInCave(final Entity entity) {
		final World world = entity.getEntityWorld();
		final BlockPos entityPos = entity.getPosition();
		if (world.getLight(entityPos) > 0) return false;

		// Raycast in cardinal directions
		int nonCaveBlockCount = 0;
		RayTraceResult ray = castRayFromEye(entity, getVec3dFromBlockPos(entityPos.up(128)));
		BlockPos rayPos = ray.getBlockPos();
		IBlockState state = world.getBlockState(rayPos);
		if ((ray.typeOfHit != RayTraceResult.Type.BLOCK)) return false;
		if (!isCaveBlockSound(state.getBlock().getSoundType(state, world, rayPos, null))) ++nonCaveBlockCount;

		ray = castRayFromEye(entity, getVec3dFromBlockPos(entityPos.down(128)));
		rayPos = ray.getBlockPos();
		state = world.getBlockState(rayPos);
		if ((ray.typeOfHit != RayTraceResult.Type.BLOCK)) return false;
		if (!isCaveBlockSound(state.getBlock().getSoundType(state, world, rayPos, null))) ++nonCaveBlockCount;

		ray = castRayFromEye(entity, getVec3dFromBlockPos(entityPos.north(128)));
		rayPos = ray.getBlockPos();
		state = world.getBlockState(rayPos);
		if ((ray.typeOfHit != RayTraceResult.Type.BLOCK)) return false;
		if (!isCaveBlockSound(state.getBlock().getSoundType(state, world, rayPos, null))) ++nonCaveBlockCount;

		ray = castRayFromEye(entity, getVec3dFromBlockPos(entityPos.south(128)));
		rayPos = ray.getBlockPos();
		state = world.getBlockState(rayPos);
		if ((ray.typeOfHit != RayTraceResult.Type.BLOCK)) return false;
		if (!isCaveBlockSound(state.getBlock().getSoundType(state, world, rayPos, null))) ++nonCaveBlockCount;

		ray = castRayFromEye(entity, getVec3dFromBlockPos(entityPos.east(128)));
		rayPos = ray.getBlockPos();
		state = world.getBlockState(rayPos);
		if ((ray.typeOfHit != RayTraceResult.Type.BLOCK)) return false;
		if (!isCaveBlockSound(state.getBlock().getSoundType(state, world, rayPos, null))) ++nonCaveBlockCount;

		ray = castRayFromEye(entity, getVec3dFromBlockPos(entityPos.west(128)));
		rayPos = ray.getBlockPos();
		state = world.getBlockState(rayPos);
		if ((ray.typeOfHit != RayTraceResult.Type.BLOCK)) return false;
		if (!isCaveBlockSound(state.getBlock().getSoundType(state, world, rayPos, null))) ++nonCaveBlockCount;

		// Cast rays in random directions. If they all hit, the sky cannot be seen.
		for (int i = 0; i < algorithmsCaveDetectionRays; ++i) {
			ray = castRayFromEye(entity, getRandomPosNearEntity(entity, 128, 128, true));
			rayPos = ray.getBlockPos();
			state = world.getBlockState(rayPos);
			if (!isCaveBlockSound(state.getBlock().getSoundType(state, world, rayPos, null))) ++nonCaveBlockCount;
		}

		// If over 5% of hit blocks are not normally found in a cave, assume player is in a base
        return !(((float) nonCaveBlockCount / (float) Math.max(1, algorithmsCaveDetectionRays + 6)) > algorithmsCaveDetectionMaxNonCaveBlockPercent);
    }

	public static Vec3d getDirectionPosToPos(final Vec3d pos1, final Vec3d pos2) {
		return pos2.subtract(pos1).normalize();
	}

	public static Vec3d getPosOffsetInDirection(final Vec3d pos, final Vec3d rotation, final float distance) {
		// Scale vector by distance magnitude
		final Vec3d offsetVector = multiplyVec3d(rotation, distance);
		return pos.add(offsetVector);
	}

	public static EulerAngle directionToAngles(final Vec3d direction) {
		final float pitch = (float) -Math.toDegrees(Math.atan2(
			direction.y,
			Math.sqrt((direction.x * direction.x) + (direction.z * direction.z))
		));
		final float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0f;
		return new EulerAngle(pitch, yaw, 0.0f);
	}

	public static boolean isEntityInDarkness(final EntityLivingBase entity, final int maxLightLevel) {
		if (entity.getEntityWorld().getLight(entity.getPosition()) > maxLightLevel) return false;
		final Potion nightVision = ForgeRegistries.POTIONS.getValue(new ResourceLocation("minecraft:night_vision"));
		for (final PotionEffect effect : entity.getActivePotionEffects()) {
			if (effect.getPotion() == nightVision) return false;
		}
		return true;
	}

	public static BlockPos getNearestBlockToEntity(final Entity entity, final Block blockType, final int range) {
		final World world = entity.getEntityWorld();
		final BlockPos entityBlockPos = entity.getPosition();
		final Vec3d entityPos = entity.getPositionVector();
		double closestBlockDistance = 0, checkDistance;
		BlockPos closestBlockPos = null, checkPos;
		for (int x = -range; x < range; ++x) {
			for (int y = -range; y < range; ++y) {
				for (int z = -range; z < range; ++z) {
					checkPos = entityBlockPos.add(x, y, z);
					if (world.getBlockState(checkPos).getBlock() == blockType) {
						// Set first check to closestBlockPos
						if (closestBlockPos == null) {
							closestBlockPos = checkPos;
							closestBlockDistance = entityPos.distanceTo(getVec3dFromBlockPos(closestBlockPos));
							continue;
						}

						// Check if this block is closer than the previous one
						checkDistance = entityPos.distanceTo(getVec3dFromBlockPos(closestBlockPos));
						if (checkDistance < closestBlockDistance) {
							checkDistance = entityPos.distanceTo(getVec3dFromBlockPos(closestBlockPos));
							closestBlockDistance = checkDistance;
							closestBlockPos = checkPos;
						}
					}
				}
			}
		}
		return closestBlockPos;
	}

	public static boolean isBlockOfBlockTypes(final Block block, final List<Block> blockTypes) {
		for (final Block blockType : blockTypes) {
			if (block == blockType) return true;
		}
		return false;
	}

	public static BlockPos getNearestBlockToEntity(final Entity entity, final List<Block> blockTypes, final int range) {
		final World world = entity.getEntityWorld();
		final BlockPos entityBlockPos = entity.getPosition();
		final Vec3d entityPos = entity.getPositionVector();
		double closestBlockDistance = 0, checkDistance;
		BlockPos closestBlockPos = null, checkPos;
		for (int x = -range; x < range; ++x) {
			for (int y = -range; y < range; ++y) {
				for (int z = -range; z < range; ++z) {
					checkPos = entityBlockPos.add(x, y, z);
					if (isBlockOfBlockTypes(world.getBlockState(checkPos).getBlock(), blockTypes)) {
						// Set first check to closestBlockPos
						if (closestBlockPos == null) {
							closestBlockPos = checkPos;
							closestBlockDistance = entityPos.distanceTo(getVec3dFromBlockPos(closestBlockPos));
							continue;
						}

						// Check if this block is closer than the previous one
						checkDistance = entityPos.distanceTo(getVec3dFromBlockPos(checkPos));
						if (checkDistance < closestBlockDistance) {
							checkDistance = entityPos.distanceTo(getVec3dFromBlockPos(closestBlockPos));
							closestBlockDistance = checkDistance;
							closestBlockPos = checkPos;
						}
					}
				}
			}
		}
		return closestBlockPos;
	}
}
