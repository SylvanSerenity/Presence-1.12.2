package com.sylvan.presence.entity;

import com.sylvan.presence.event.Events;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.decoration.EntityArmorStand;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EulerAngle;
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
		itemStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(skinValue));
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

		this.headEntity = EntityType.ARMOR_STAND.create(world);
        assert headEntity != null;
        headEntity.readCustomDataFromNbt(headBodyCompound);
		headEntity.equipStack(EquipmentSlot.HEAD, head);
		headEntity.setNoGravity(true);

		this.bodyEntity = EntityType.ARMOR_STAND.create(world);
        assert bodyEntity != null;
        bodyEntity.readCustomDataFromNbt(headBodyCompound);
		bodyEntity.equipStack(EquipmentSlot.HEAD, body);
		bodyEntity.setNoGravity(true);

		this.armsEntity = EntityType.ARMOR_STAND.create(world);
        assert armsEntity != null;
        armsEntity.readCustomDataFromNbt(headBodyCompound);
		armsEntity.equipStack(EquipmentSlot.MAINHAND, rightArm);
		armsEntity.equipStack(EquipmentSlot.OFFHAND, leftArm);
		armsEntity.setNoGravity(true);

		this.legsEntity = EntityType.ARMOR_STAND.create(world);
        assert legsEntity != null;
        legsEntity.readCustomDataFromNbt(legsCompound);
		legsEntity.equipStack(EquipmentSlot.MAINHAND, rightLeg);
		legsEntity.equipStack(EquipmentSlot.OFFHAND, leftLeg);
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
			() -> {
				if (!headEntity.isRemoved()) remove();
			}, ms, TimeUnit.MILLISECONDS
		);
	}

	public void remove() {
		headEntity.remove(RemovalReason.DISCARDED);
		bodyEntity.remove(RemovalReason.DISCARDED);
		armsEntity.remove(RemovalReason.DISCARDED);
		legsEntity.remove(RemovalReason.DISCARDED);
	}

	public void setGravity(final boolean gravity) {
		headEntity.setNoGravity(!gravity);
		bodyEntity.setNoGravity(!gravity);
		armsEntity.setNoGravity(!gravity);
		legsEntity.setNoGravity(!gravity);
	}

	public void setHeadRotation(final float pitch, final float yaw, final float roll) {
		EulerAngle headRotation = new EulerAngle(pitch, yaw, roll);
		headEntity.setHeadRotation(headRotation);
	}

	public void setBodyRotation(final float yaw) {
		bodyEntity.setYaw(yaw);
		armsEntity.setYaw(yaw);
		legsEntity.setYaw(yaw);
	}

	public void setPosition(final Vec3d pos) {
		headEntity.setPosition(pos);
		bodyEntity.setPosition(pos);
		armsEntity.setPosition(pos);
		legsEntity.setPosition(pos);
	}

	public void lookAt(final Entity entity) {
		final Vec3d direction = Algorithms.getDirectionPosToPos(headEntity.getEyePos(), entity.getEyePos());
		final EulerAngle rotation = Algorithms.directionToAngles(direction);
		setHeadRotation(rotation.getPitch(), rotation.getYaw(), 0.0f);
		setBodyRotation(rotation.getYaw());
	}

	public boolean isSeenByPlayers(final double dotProductThreshold) {
		final List<ServerPlayerEntity> players = headEntity.getServer().getPlayerManager().getPlayerList();
		final double maxY = headEntity.getY() + headEntity.getHeight();
		Vec3d pos = headEntity.getPos();
		while (pos.getY() < maxY) {
			for (final PlayerEntity player : players) {
				if (Algorithms.isPositionLookedAtByEntity(player, pos, dotProductThreshold)) return true;
				pos = pos.add(0, 0.25, 0);
			}
		}
		return false;
	}

	public boolean isWithinDistanceOfPlayers(final float distance) {
		final List<ServerPlayerEntity> players = headEntity.getServer().getPlayerManager().getPlayerList();
		for (final PlayerEntity player : players) {
			if (player.getPos().distanceTo(headEntity.getPos()) < distance) return true;
		}
		return false;
	}

	public void move(final Vec3d movementOffset) {
		headEntity.move(MovementType.SELF, movementOffset);
		bodyEntity.move(MovementType.SELF, movementOffset);
		armsEntity.move(MovementType.SELF, movementOffset);
		legsEntity.move(MovementType.SELF, movementOffset);
	}

	public void moveTo(final Vec3d newPos) {
		move(newPos.subtract(getPos()));
	}

	public Vec3d getPos() {
		return headEntity.getPos();
	}

	public Vec3d getEyePos() {
		return headEntity.getEyePos();
	}

	public float getYaw() {
		return headEntity.getYaw();
	}

	public Vec3d getRotationVector() {
		return headEntity.getRotationVector();
	}

	public BlockPos getBlockPos() {
		return headEntity.getBlockPos();
	}
}
