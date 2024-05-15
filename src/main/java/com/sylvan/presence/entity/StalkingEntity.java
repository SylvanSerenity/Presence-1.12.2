package com.sylvan.presence.entity;

import com.sylvan.presence.event.Stalk;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class StalkingEntity extends HerobrineEntity {
	private final PlayerEntity trackedPlayer;
	private boolean shouldRemove = false;
	private boolean hasBeenSeen = false;

	public StalkingEntity(World world, String skin, final PlayerEntity trackedPlayer) {
		super(world, skin);
		this.trackedPlayer = trackedPlayer;
		this.setGravity(true);
	}

	public void tick() {
		// Continue looking at player
		this.lookAt(trackedPlayer);

		if (hasBeenSeen) {
			// Vanish when unseen
			if (!this.isSeenByPlayers(Stalk.stalkLookAtThresholdVanish)) {
				shouldRemove = true;
			}
		} else {
			if (this.isSeenByPlayers(Stalk.stalkLookAtThresholdVanish)) {
				hasBeenSeen = true;
			}
		}
	}

	public PlayerEntity getTrackedPlayer() {
		return trackedPlayer;
	}

	public boolean shouldRemove() {
		return shouldRemove;
	}
}
