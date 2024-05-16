package com.sylvan.presence.event;

import com.google.common.base.Predicate;
import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.util.Algorithms;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OpenChest {
	// Config
	public static boolean openChestEnabled = true;			// Whether the open chest event is active
	public static float openChestHauntLevelMin = 1.25f;		// The minimum haunt level to play event
	private static int openChestDelayMin = 60 * 60;			// The minimum delay between open chest events
	private static int openChestDelayMax = 60 * 60 * 4;		// The maximum delay between open chest events
	private static int openChestRetryDelay = 60;			// The delay between retrying to open a chest if the previous attempt failed
	private static int openChestSearchRadius = 32;			// The search radius of finding chests to open. Higher values have exponential lag during the tick performing the search
	private static int openChestCloseSoundMsMin = 1000;		// The minimum amount of time in milliseconds to close the chest after opening it
	private static int openChestCloseSoundMsMax = 3000;		// The maximum amount of time in milliseconds to close the chest after opening it
	private static boolean openChestNotSeenConstraint = true;	// Whether the constraint for making the chest open only when not seen is active
	private static boolean openChestSwapItems = true;		// Whether to swap two random items in the chest
	private static boolean openChestPlaySound = true;		// Whether to play the chest open sound

	public static final ArrayList<Block> chestBlocks = new ArrayList<>();

	public static void loadConfig() {
		try {
			openChestEnabled = Presence.config.getOrSetValue("openChestEnabled", openChestEnabled).getAsBoolean();
			openChestHauntLevelMin = Presence.config.getOrSetValue("openChestHauntLevelMin", openChestHauntLevelMin).getAsFloat();
			openChestDelayMin = Presence.config.getOrSetValue("openChestDelayMin", openChestDelayMin).getAsInt();
			openChestDelayMax = Presence.config.getOrSetValue("openChestDelayMax", openChestDelayMax).getAsInt();
			openChestRetryDelay = Presence.config.getOrSetValue("openChestRetryDelay", openChestRetryDelay).getAsInt();
			openChestSearchRadius = Presence.config.getOrSetValue("openChestSearchRadius", openChestSearchRadius).getAsInt();
			openChestCloseSoundMsMin = Presence.config.getOrSetValue("openChestCloseSoundMsMin", openChestCloseSoundMsMin).getAsInt();
			openChestCloseSoundMsMax = Presence.config.getOrSetValue("openChestCloseSoundMsMax", openChestCloseSoundMsMax).getAsInt();
			openChestNotSeenConstraint = Presence.config.getOrSetValue("openChestNotSeenConstraint", openChestNotSeenConstraint).getAsBoolean();
			openChestSwapItems = Presence.config.getOrSetValue("openChestSwapItems", openChestSwapItems).getAsBoolean();
			openChestPlaySound = Presence.config.getOrSetValue("openChestPlaySound", openChestPlaySound).getAsBoolean();
		} catch (UnsupportedOperationException e) {
			Presence.LOGGER.error("Configuration issue for OpenChest.java. Wiping and using default values.", e);
			Presence.config.wipe();
			Presence.initConfig();
		}
	}

	public static void initEvent() {
		chestBlocks.add(Blocks.CHEST);
		chestBlocks.add(Blocks.TRAPPED_CHEST);
	}

	public static void scheduleEvent(final EntityPlayer player) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		scheduleEventWithDelay(
			player,
			Algorithms.RANDOM.nextBetween(
				Algorithms.divideByFloat(openChestDelayMin, hauntLevel),
				Algorithms.divideByFloat(openChestDelayMax, hauntLevel)
			)
		);
	}

	public static void scheduleEventWithDelay(final EntityPlayer player, final int delay) {
		final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
		Events.scheduler.schedule(
			() -> {
				if (openChest(player, false)) {
					scheduleEventWithDelay(
						player,
						Algorithms.RANDOM.nextBetween(
							Algorithms.divideByFloat(openChestDelayMin, hauntLevel),
							Algorithms.divideByFloat(openChestDelayMax, hauntLevel)
						)
					);
				} else {
					// Retry if it is a bad time
					scheduleEventWithDelay(player, openChestRetryDelay);
				}
			},
			delay, TimeUnit.SECONDS
		);
	}

	public static boolean openChest(final EntityPlayer player, final boolean overrideHauntLevel) {
		if (!overrideHauntLevel) {
			final float hauntLevel = PlayerData.getPlayerData(player).getHauntLevel();
			if (hauntLevel < openChestHauntLevelMin) return true; // Reset event as if it passed
		}

		// Get nearest chest position
		BlockPos nearestChestPos = Algorithms.getNearestBlockToEntity(player, chestBlocks, openChestSearchRadius);
		if (nearestChestPos == null) return false;

		// Players must not see chest open
		final World world = player.getEntityWorld();
		final List<? extends EntityPlayer> players = world.getPlayers(EntityPlayer.class, new Predicate<EntityPlayer>() {
			@Override
			public boolean apply(@Nullable EntityPlayer input) {
				return true;
			}
		});
		if (openChestNotSeenConstraint && (Algorithms.couldBlockBeSeenByPlayers(players, nearestChestPos))) return false;
		final IBlockState chestBlockState = world.getBlockState(nearestChestPos);

		// Swap two random items
		if (openChestSwapItems) {
			// Get chest block inventory
			final Inventory chestInventory = ChestBlock.getInventory((ChestBlock) chestBlockState.getBlock(), chestBlockState, world, nearestChestPos, true);

			// Get two random slots
            assert chestInventory != null;
            final int slot1 = Algorithms.RANDOM.nextBetween(0, chestInventory.size() - 1);
			final ItemStack stack1 = chestInventory.getStack(slot1);
			int slot2 = Algorithms.RANDOM.nextBetween(0, chestInventory.size() - 1);
			ItemStack stack2;
	
			// Make sure slots are different
			if (slot1 == slot2) {
				if (slot2 < chestInventory.size() - 1) ++slot2;
				else --slot2;
			}
	
			// Make sure to have at least one item
			final int startSlot = slot2;
			while ((stack2 = chestInventory.getStack(slot2)) == ItemStack.EMPTY) {
				// Increase slot number or reset to first
				if (slot2 < chestInventory.size() - 1) ++slot2;
				else slot2 = 0;
	
				// Continue if the slots are the same
				if (slot2 == slot1) continue;
	
				// Exit loop if we checked every item
				if (slot2 == startSlot) break;
			}
	
			// Swap slots
			chestInventory.setStack(slot1, stack2);
			chestInventory.setStack(slot2, stack1);
			chestInventory.markDirty();
		}

		// Play open chest sound
		if (openChestPlaySound) {
			world.playSound(null, nearestChestPos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 16.0f, 1.0f);
			Events.scheduler.schedule(
				() -> world.playSound(null, nearestChestPos, SoundEvents.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 16.0f, 1.0f),
					Algorithms.RANDOM.nextBetween(openChestCloseSoundMsMin, openChestCloseSoundMsMax),
					TimeUnit.MILLISECONDS
			);
		}

		return true;
	}
}
