package com.sylvan.presence.event;

import com.sylvan.presence.Presence;
import com.sylvan.presence.data.PlayerData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Events {
	public static ScheduledExecutorService scheduler;

	public static void initEvents() {
		AmbientSounds.initEvent();
		FlowerGift.initEvent();
		NearbySounds.initEvent();
		OpenChest.initEvent();
		OpenDoor.initEvent();
		TrampleCrops.initEvent();
	}

	public static void registerEvents() {
		initEvents();

		// Start/stop scheduler with server
		ServerLifecycleEvents.SERVER_STARTING.register((serverStarting) -> {
			scheduler = Executors.newScheduledThreadPool(8);
			PlayerData.setInstance(serverStarting);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register((serverStopping) -> {
			scheduler.shutdown();

			Creep.onShutdown();
			Stalk.onShutdown();

			if (!Presence.config.exists()) {
				Presence.config.wipe();
				Presence.initConfig();
			}

			try {
				if (!scheduler.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
					Presence.LOGGER.error("Failed to await termination of event scheduler.");
				}
			} catch (InterruptedException e) {
				Presence.LOGGER.error("Failed to await termination of event scheduler.", e);
			}
		});

		// Schedule player join events
		ServerPlayConnectionEvents.JOIN.register((serverPlayNetworkHandler, packetSender, server)
				-> PlayerData.addPlayerData(serverPlayNetworkHandler.getPlayer()).startEvents());

		// Attempt to remove torches when player disconnects
		ServerPlayConnectionEvents.DISCONNECT.register((serverPlayNetworkHandler, server) -> {
			final PlayerEntity player = serverPlayNetworkHandler.getPlayer();
			PlayerData.getPlayerData(player).remove();
		});

		// Add block use/place tracker for extinguish torches event
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!world.isClient()) {
				ExtinguishTorches.onUseBlock(player, world, hitResult);
				FlickerDoor.onUseBlock(player, world, hitResult);
			}
			return ActionResult.PASS;
		});

		// Add server tick events for Herobrine
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (!world.isClient()) {
				Creep.onWorldTick(world);
				Freeze.onWorldTick();
				Stalk.onWorldTick(world);
			}
		});
	}
}
