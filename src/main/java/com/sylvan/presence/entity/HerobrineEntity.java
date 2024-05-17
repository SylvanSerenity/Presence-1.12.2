package com.sylvan.presence.entity;

import com.sylvan.presence.event.Events;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EntityEquipmentSlot;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.decoration.EntityArmorStand;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEntityEquipmentSlot;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerEntityPlayer;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Rotations;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HerobrineEntity {
	private final World world;
	private final EntityArmorStand headEntity;
	private final EntityArmorStand bodyEntity;
	private final EntityArmorStand armsEntity;
	private final EntityArmorStand legsEntity;

	private static final NBTTagCompound headBodyCompound = new NBTTagCompound();
	private static final NBTTagCompound legsCompound = new NBTTagCompound();
	public static final Map<String, Integer> skins = new HashMap<>();

	public static void initEntity() {
		NBTTagList armPoseValues = new NBTTagList();
		armPoseValues.appendTag(new NBTTagFloat(0.0f));
		armPoseValues.appendTag(new NBTTagFloat(0.0f));
		armPoseValues.appendTag(new NBTTagFloat(0.0f));

		NBTTagCompound armLegPoseCompound = new NBTTagCompound();
		armLegPoseCompound.setTag("LeftArm", armPoseValues);
		armLegPoseCompound.setTag("RightArm", armPoseValues);

		headBodyCompound.setBoolean("Invisible", true);
		headBodyCompound.setBoolean("Invulnerable", true);
		headBodyCompound.setBoolean("NoBasePlate", true);
		headBodyCompound.setBoolean("ShowArms", true);
		headBodyCompound.setInteger("DisabledSlots", 2039583);
		headBodyCompound.setTag("Pose", armLegPoseCompound);

		legsCompound.setBoolean("Invisible", true);
		legsCompound.setBoolean("Invulnerable", true);
		legsCompound.setBoolean("NoBasePlate", true);
		legsCompound.setBoolean("ShowArms", true);
		legsCompound.setInteger("DisabledSlots", 2039583);
		legsCompound.setBoolean("Small", true);
		legsCompound.setTag("Pose", armLegPoseCompound);

		skins.put("classic", 100);
		skins.put("smile", 200);
	}

	private static ItemStack newModelItem(final int skinValue) {
		ItemStack itemStack = new ItemStack(Blocks.STONE_BUTTON);
		NBTTagCompound nbt = itemStack.getTagCompound();
		nbt.setInteger("CustomModelData", skinValue);
		itemStack.setTagCompound(nbt);
		return itemStack;
	}

	public HerobrineEntity(final World world, final String skin) {
		this.world = world;

		int skinId = skins.get("classic");
		if (skins.containsKey(skin)) {
			skinId = skins.get(skin);
		}

		ItemStack head = newModelItem(skinId);
		ItemStack body = newModelItem(skinId + 1);
		ItemStack leftArm = newModelItem(skinId + 2);
		ItemStack rightArm = newModelItem(skinId + 3);
		ItemStack leftLeg = newModelItem(skinId + 4);
		ItemStack rightLeg = newModelItem(skinId + 5);

		this.headEntity = new EntityArmorStand(world);
        headEntity.readEntityFromNBT(headBodyCompound);
		headEntity.setItemStackToSlot(EntityEquipmentSlot.HEAD, head);
		headEntity.setNoGravity(true);

		this.bodyEntity = new EntityArmorStand(world);
        bodyEntity.readEntityFromNBT(headBodyCompound);
		bodyEntity.setItemStackToSlot(EntityEquipmentSlot.HEAD, body);
		bodyEntity.setNoGravity(true);

		this.armsEntity = new EntityArmorStand(world);
        armsEntity.readEntityFromNBT(headBodyCompound);
		armsEntity.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, rightArm);
		armsEntity.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, leftArm);
		armsEntity.setNoGravity(true);

		this.legsEntity = new EntityArmorStand(world);
        legsEntity.readEntityFromNBT(legsCompound);
		legsEntity.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, rightLeg);
		legsEntity.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, leftLeg);
		legsEntity.setNoGravity(true);
	}

	public void summon() {
		world.spawnEntity(headEntity);
		world.spawnEntity(bodyEntity);
		world.spawnEntity(armsEntity);
		world.spawnEntity(legsEntity);
	}

	public void scheduleRemoval(final long ms) {
		Events.scheduler.schedule(
				this::remove, ms, TimeUnit.MILLISECONDS
		);
	}

	public void remove() {
		headEntity.setDead();
		bodyEntity.setDead();
		armsEntity.setDead();
		legsEntity.setDead();
	}

	public void setGravity(final boolean gravity) {
		headEntity.setNoGravity(!gravity);
		bodyEntity.setNoGravity(!gravity);
		armsEntity.setNoGravity(!gravity);
		legsEntity.setNoGravity(!gravity);
	}

	public void setHeadRotation(final float pitch, final float yaw, final float roll) {
		final Rotations headRotation = new Rotations(pitch, yaw, roll);
		headEntity.setHeadRotation(headRotation);
	}

	public void setBodyRotation(final float yaw) {
		final Rotations rotation = new Rotations(0.0f, yaw, 0.0f);
		bodyEntity.setBodyRotation(rotation);
		armsEntity.setBodyRotation(rotation);
		legsEntity.setBodyRotation(rotation);
	}

	public void setPosition(final Vec3d pos) {
		headEntity.setPosition(pos);
		bodyEntity.setPosition(pos);
		armsEntity.setPosition(pos);
		legsEntity.setPosition(pos);
	}

	public void lookAt(final Entity entity) {
		final Vec3d direction = Algorithms.getDirectionPosToPos(headEntity.getPositionEyes(1), entity.getPositionEyes(1));
		final EulerAngle rotation = Algorithms.directionToAngles(direction);
		setHeadRotation(rotation.getPitch(), rotation.getYaw(), 0.0f);
		setBodyRotation(rotation.getYaw());
	}

	public boolean isSeenByPlayers(final double dotProductThreshold) {
		final List<EntityPlayerMP> players = headEntity.getServer().getPlayerList().getPlayers();
		final double maxY = headEntity.getPosition().getY() + headEntity.getEyeHeight();
		Vec3d pos = headEntity.getPositionVector();
		while (pos.y < maxY) {
			for (final EntityPlayer player : players) {
				if (Algorithms.isPositionLookedAtByEntity(player, pos, dotProductThreshold)) return true;
				pos = pos.add(new Vec3d(0, 0.25, 0));
			}
		}
		return false;
	}

	public boolean isWithinDistanceOfPlayers(final float distance) {
		final List<EntityPlayerMP> players = headEntity.getServer().getPlayerList().getPlayers();
		for (final EntityPlayer player : players) {
			if (player.getPositionVector().distanceTo(headEntity.getPositionVector()) < distance) return true;
		}
		return false;
	}

	public void move(final Vec3d movementOffset) {
		headEntity.move(MoverType.SELF, movementOffset.x, movementOffset.y, movementOffset.z);
		bodyEntity.move(MoverType.SELF, movementOffset.x, movementOffset.y, movementOffset.z);
		armsEntity.move(MoverType.SELF, movementOffset.x, movementOffset.y, movementOffset.z);
		legsEntity.move(MoverType.SELF, movementOffset.x, movementOffset.y, movementOffset.z);
	}

	public void moveTo(final Vec3d newPos) {
		move(newPos.subtract(getPos()));
	}

	public Vec3d getPos() {
		return headEntity.getPositionVector();
	}

	public Vec3d getEyePos() {
		return headEntity.getPositionEyes(1);
	}

	public float getYaw() {
		return headEntity.getRotationYawHead();
	}

	public Vec3d getRotationVector() {
		return headEntity.getRotationVector();
	}

	public BlockPos getBlockPos() {
		return headEntity.getPosition();
	}
}
