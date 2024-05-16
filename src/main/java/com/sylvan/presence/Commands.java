package com.sylvan.presence;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sylvan.presence.data.PlayerData;
import com.sylvan.presence.entity.HerobrineEntity;
import com.sylvan.presence.event.*;
import com.sylvan.presence.util.Algorithms;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import static net.minecraft.server.command.CommandManager.*;

public class Commands {
	private static HerobrineEntity herobrineEntity;

	public static void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			literal("presence")
			.requires(source -> source.hasPermissionLevel(2))
			.then(
				literal("event")
				.then(
					literal("ambientSounds")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							if (AmbientSounds.playAmbientSound(context.getSource().getPlayer(), true)) {
								context.getSource().sendFeedback(() -> Text.literal("Successfully executed ambient sounds event.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Conditions to execute ambient sounds not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute ambient sounds event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final EntityPlayer player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else {
								if (AmbientSounds.playAmbientSound(player, true)) {
									context.getSource().sendFeedback(() -> Text.literal("Successfully executed ambient sounds event.").withColor(Formatting.BLUE.getColorValue()), false);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Conditions to execute ambient sounds not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
								}
							}
							return 1;
						})
					)
				)
				.then(
					literal("attack")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							context.getSource().sendFeedback(() -> Text.literal("Executing attack event.").withColor(Formatting.BLUE.getColorValue()), false);
							Attack.attack(context.getSource().getPlayer(), Algorithms.randomBetween(Attack.attackDamageMin, Attack.attackDamageMax), true);
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute attack event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("damage", FloatArgumentType.floatArg())
						.executes(context -> {
							if (context.getSource().isExecutedByPlayer()) {
								final float damage = FloatArgumentType.getFloat(context, "damage");
								if (Attack.attack(context.getSource().getPlayer(), damage, true)) {
									context.getSource().sendFeedback(() -> Text.literal("Successfully executed attack event.").withColor(Formatting.BLUE.getColorValue()), false);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Conditions to execute attack not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
								}
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Cannot execute attack event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
							return 1;
						})
						.then(
							argument("player", StringArgumentType.word())
							.suggests((context, builder) -> {
								final Iterable<String> playerNames = context.getSource().getPlayerNames();
								for (final String playerName : playerNames) {
									builder.suggest(playerName);
								}
								return builder.buildFuture();
							})
							.executes(context -> {
								final String playerName = StringArgumentType.getString(context, "player");
								final EntityPlayer player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
								if (player == null) {
									context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
								} else {
									final float damage = FloatArgumentType.getFloat(context, "damage");
									if (Attack.attack(player, damage, true)) {
										context.getSource().sendFeedback(() -> Text.literal("Successfully executed attack event for player " + player.getName().toString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
									} else {
										context.getSource().sendFeedback(() -> Text.literal("Conditions to execute attack not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
									}
								}
								return 1;
							})
						)
					)
				)
				.then(
					literal("chatMessage")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							ChatMessage.chatMessage(context.getSource().getPlayer(), true, true);
							context.getSource().sendFeedback(() -> Text.literal("Successfully executed chat message event.").withColor(Formatting.BLUE.getColorValue()), false);
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute chat message event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final EntityPlayer player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else {
								ChatMessage.chatMessage(player, true, true);
								context.getSource().sendFeedback(() -> Text.literal("Successfully executed chat message event.").withColor(Formatting.BLUE.getColorValue()), false);
							}
							return 1;
						})
					)
				)
				.then(
					literal("creep")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							if (Creep.creep(context.getSource().getPlayer(), true)) {
								context.getSource().sendFeedback(() -> Text.literal("Successfully executed creep event.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Conditions to execute creep not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute creep event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final EntityPlayer player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else {
								if (Creep.creep(player, true)) {
									context.getSource().sendFeedback(() -> Text.literal("Successfully executed creep event for player " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Conditions to execute creep event not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
								}
							}
							return 1;
						})
					)
				)
				.then(
					literal("extinguishTorches")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							final EntityPlayer player = context.getSource().getPlayer();
							if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
								context.getSource().sendFeedback(() -> Text.literal(
									"Extinguished tracked torches for " + player.getName().getString() + "."
								).withColor(Formatting.BLUE.getColorValue()), false);
								ExtinguishTorches.extinguishTrackedTorches(player);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Player is not being tracked for torch placements.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot track server placement. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						literal("extinguish")
						.executes(context -> {
							if (context.getSource().isExecutedByPlayer()) {
								final PlayerEntity player = context.getSource().getPlayer();
								if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
									context.getSource().sendFeedback(() -> Text.literal(
										"Extinguished tracked torches for " + player.getName().getString() + "."
									).withColor(Formatting.BLUE.getColorValue()), false);
									ExtinguishTorches.extinguishTrackedTorches(player);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Player is not being tracked for torch placements.").withColor(Formatting.DARK_RED.getColorValue()), false);
								}
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Cannot track server placement. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
							return 1;
						})
						.then(
							argument("player", StringArgumentType.word())
							.suggests((context, builder) -> {
								final Iterable<String> playerNames = context.getSource().getPlayerNames();
								for (final String playerName : playerNames) {
									builder.suggest(playerName);
								}
								return builder.buildFuture();
							})
							.executes(context -> {
								final String playerName = StringArgumentType.getString(context, "player");
								final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
								if (player == null) {
									context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
								} else if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
									context.getSource().sendFeedback(() -> Text.literal("Extinguished torches for player " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
									ExtinguishTorches.extinguishTrackedTorches(player);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Player is not being tracked for torch placements.").withColor(Formatting.DARK_RED.getColorValue()), false);
								}
								return 1;
							})
						)
					)
					.then(
						literal("track")
						.executes(context -> {
							if (context.getSource().isExecutedByPlayer()) {
								final PlayerEntity player = context.getSource().getPlayer();
								if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
									context.getSource().sendFeedback(() -> Text.literal("Player is already being tracked for torch placements.").withColor(Formatting.DARK_RED.getColorValue()), false);
								} else {
									context.getSource().sendFeedback(() -> Text.literal(
										"Started tracking torches for " + player.getName().getString() + "."
									).withColor(Formatting.BLUE.getColorValue()), false);
									ExtinguishTorches.startTrackingTorches(player, true);
								}
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Cannot track server placement. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
							return 1;
						})
						.then(
							argument("player", StringArgumentType.word())
							.suggests((context, builder) -> {
								final Iterable<String> playerNames = context.getSource().getPlayerNames();
								for (final String playerName : playerNames) {
									builder.suggest(playerName);
								}
								return builder.buildFuture();
							})
							.executes(context -> {
								final String playerName = StringArgumentType.getString(context, "player");
								final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
								if (player == null) {
									context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
								} else if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
									context.getSource().sendFeedback(() -> Text.literal("Player is already being tracked for torch placements.").withColor(Formatting.DARK_RED.getColorValue()), false);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Started tracking torches for player " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
									ExtinguishTorches.startTrackingTorches(player, true);
								}
								return 1;
							})
						)
					)
				)
				.then(
					literal("flickerDoor")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							FlickerDoor.trackPlayer(context.getSource().getPlayer(), true);
							context.getSource().sendFeedback(() -> Text.literal("Tracking player for flicker door event.").withColor(Formatting.BLUE.getColorValue()), false);
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute flicker door event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else {
								FlickerDoor.trackPlayer(player, true);
								context.getSource().sendFeedback(() -> Text.literal("Tracking player " + player.getName().getString() + "for flicker door event.").withColor(Formatting.BLUE.getColorValue()), false);
							}
							return 1;
						})
					)
				)
				.then(
					literal("flowerGift")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							if (FlowerGift.flowerGift(context.getSource().getPlayer(), true)) {
								context.getSource().sendFeedback(() -> Text.literal("Successfully executed flower gift event.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Conditions to execute flower gift event not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute flower gift event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else {
								if (FlowerGift.flowerGift(player, true)) {
									context.getSource().sendFeedback(() -> Text.literal("Successfully executed flower gift event for " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Conditions to execute flower gift event not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
								}
							}
							return 1;
						})
					)
				)
				.then(
					literal("footsteps")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							context.getSource().sendFeedback(() -> Text.literal("Executing footsteps event.").withColor(Formatting.BLUE.getColorValue()), false);
							Footsteps.generateFootsteps(context.getSource().getPlayer(), Algorithms.RANDOM.nextBetween(Footsteps.footstepsStepsMin, Footsteps.footstepsStepsMax), true);
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute footsteps event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("footstepCount", IntegerArgumentType.integer(1))
						.executes(context -> {
							if (context.getSource().isExecutedByPlayer()) {
								final int footstepCount = IntegerArgumentType.getInteger(context, "footstepCount");
								context.getSource().sendFeedback(() -> Text.literal("Executing footsteps event.").withColor(Formatting.BLUE.getColorValue()), false);
								Footsteps.generateFootsteps(context.getSource().getPlayer(), footstepCount, true);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Cannot execute footsteps event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
							return 1;
						})
						.then(
							argument("player", StringArgumentType.word())
							.suggests((context, builder) -> {
								final Iterable<String> playerNames = context.getSource().getPlayerNames();
								for (final String playerName : playerNames) {
									builder.suggest(playerName);
								}
								return builder.buildFuture();
							})
							.executes(context -> {
								final String playerName = StringArgumentType.getString(context, "player");
								final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
								if (player == null) {
									context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
								} else {
									final int footstepCount = IntegerArgumentType.getInteger(context, "footstepCount");
									context.getSource().sendFeedback(() -> Text.literal("Executing footsteps event for player " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
									Footsteps.generateFootsteps(player, footstepCount, true);
								}
								return 1;
							})
						)
					)
				)
				.then(
					literal("freeze")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							Freeze.freeze(context.getSource().getPlayer(), true);
							context.getSource().sendFeedback(() -> Text.literal("Executing freeze event.").withColor(Formatting.BLUE.getColorValue()), false);
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute freeze event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else {
								Freeze.freeze(player, true);
								context.getSource().sendFeedback(() -> Text.literal("Executing freeze event for player " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
							}
							return 1;
						})
					)
				)
				.then(
					literal("nearbySounds")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							context.getSource().sendFeedback(() -> Text.literal("Executing nearby sounds event.").withColor(Formatting.BLUE.getColorValue()), false);
							NearbySounds.playNearbySound(context.getSource().getPlayer(), true);
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute nearby sounds event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Executing nearby sounds event for " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
								NearbySounds.playNearbySound(player, true);
							}
							return 1;
						})
					)
				)
				.then(
					literal("openChest")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							if (OpenChest.openChest(context.getSource().getPlayer(), true)) {
								context.getSource().sendFeedback(() -> Text.literal("Successfully executed open chest event.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Conditions to execute open chest event not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute open chest event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else {
								if (OpenChest.openChest(player, true)) {
									context.getSource().sendFeedback(() -> Text.literal("Successfully executed open chest event for " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Conditions to execute open chest event not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
								}
							}
							return 1;
						})
					)
				)
				.then(
					literal("openDoor")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							if (OpenDoor.openDoor(context.getSource().getPlayer(), true)) {
								context.getSource().sendFeedback(() -> Text.literal("Successfully executed open door event.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Conditions to execute open door event not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute open door event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else {
								if (OpenDoor.openDoor(player, true)) {
									context.getSource().sendFeedback(() -> Text.literal("Successfully executed open door event for " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Conditions to execute open door event not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
								}
							}
							return 1;
						})
					)
				)
				.then(
					literal("stalk")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							if (Stalk.stalk(context.getSource().getPlayer(), true)) {
								context.getSource().sendFeedback(() -> Text.literal("Successfully executed stalk event.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Conditions to execute stalk event not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute stalk event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else {
								if (Stalk.stalk(player, true)) {
									context.getSource().sendFeedback(() -> Text.literal("Successfully executed stalk event for " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Conditions to execute stalk event not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
								}
							}
							return 1;
						})
					)
				)
				.then(
					literal("subtitleWarning")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							context.getSource().sendFeedback(() -> Text.literal("Executing subtitle warning event.").withColor(Formatting.BLUE.getColorValue()), false);
							SubtitleWarning.subtitleWarning(context.getSource().getPlayer(), true);
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute subtitle warning event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Executing subtitle warning event for " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
								SubtitleWarning.subtitleWarning(player, true);
							}
							return 1;
						})
					)
				)
				.then(
					literal("trampleCrops")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							if (TrampleCrops.trampleCrops(context.getSource().getPlayer(), true)) {
								context.getSource().sendFeedback(() -> Text.literal("Successfully executed trample crops event.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Conditions to execute trample crops event not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot execute trample crops event on server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else {
								if (TrampleCrops.trampleCrops(player, true)) {
									context.getSource().sendFeedback(() -> Text.literal("Successfully executed trample crops event for " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Conditions to execute trample crops event not met.").withColor(Formatting.DARK_RED.getColorValue()), false);
								}
							}
							return 1;
						})
					)
				)
			)
			.then(
				literal("fakeHerobrine")
				.then(
					literal("destroy")
					.executes(context -> {
						context.getSource().sendFeedback(() -> Text.literal("Destroyed fake Herobrine.").withColor(Formatting.BLUE.getColorValue()), false);
						destroyFakeHerobrine();
						return 1;
					})
				)
				.then(
					literal("summon")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							final PlayerEntity player = context.getSource().getPlayer();
							context.getSource().sendFeedback(() -> Text.literal("Summoned fake Herobrine.").withColor(Formatting.BLUE.getColorValue()), false);
							summonFakeHerobrine(player, "classic");
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot summon fake Herobrine by server.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						literal("skin")
						.then(
							argument("skinName", StringArgumentType.word())
							.suggests((context, builder) -> {
								for (final String skin : HerobrineEntity.skins.keySet()) {
									builder.suggest(skin);
								}
								return builder.buildFuture();
							})
							.executes(context -> {
								if (context.getSource().isExecutedByPlayer()) {
									final String skin = StringArgumentType.getString(context, "skinName");
									if (HerobrineEntity.skins.containsKey(skin)) {
										final PlayerEntity player = context.getSource().getPlayer();
										context.getSource().sendFeedback(() -> Text.literal("Summoned fake Herobrine.").withColor(Formatting.BLUE.getColorValue()), false);
										summonFakeHerobrine(player, skin);
									} else {
										context.getSource().sendFeedback(() -> Text.literal("Skin not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
									}
								} else {
									context.getSource().sendFeedback(() -> Text.literal("Cannot summon fake Herobrine by server.").withColor(Formatting.DARK_RED.getColorValue()), false);
								}
								return 1;
							})
						)
					)
				)
			)
			.then(
				literal("haunt")
				.executes(context -> {
					if (context.getSource().isExecutedByPlayer()) {
						final PlayerEntity player = context.getSource().getPlayer();
						final PlayerData playerData = PlayerData.getPlayerData(player);
						if (playerData.isHaunted()) {
							context.getSource().sendFeedback(() -> Text.literal("Freed yourself.").withColor(Formatting.BLUE.getColorValue()), false);
							playerData.setHaunted(false);
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Haunted yourself.").withColor(Formatting.BLUE.getColorValue()), false);
							playerData.setHaunted(true);
						}
					} else {
						context.getSource().sendFeedback(() -> Text.literal("Cannot haunt server. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
					}
					return 1;
				})
				.then(
					argument("player", StringArgumentType.word())
					.suggests((context, builder) -> {
						final Iterable<String> playerNames = context.getSource().getPlayerNames();
						for (final String playerName : playerNames) {
							builder.suggest(playerName);
						}
						return builder.buildFuture();
					})
					.executes(context -> {
						final String playerName = StringArgumentType.getString(context, "player");
						final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
						final PlayerData playerData = PlayerData.getPlayerData(player);
						if (player == null) {
							context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
						} else if (playerData.isHaunted()) {
							context.getSource().sendFeedback(() -> Text.literal("Freed player " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
							playerData.setHaunted(false);
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Haunted player " + player.getName().getString() + ".").withColor(Formatting.BLUE.getColorValue()), false);
							playerData.setHaunted(true);
						}
						return 1;
					})
				)
			)
			.then(
				literal("query")
				.then(
					literal("haunted")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							final PlayerEntity player = context.getSource().getPlayer();
							final PlayerData playerData = PlayerData.getPlayerData(player);
							if (playerData.isHaunted()) {
								context.getSource().sendFeedback(() -> Text.literal("You are haunted.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("You are not haunted.").withColor(Formatting.RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot query server haunt status. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							final PlayerData playerData = PlayerData.getPlayerData(player);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else if (playerData.isHaunted()) {
								context.getSource().sendFeedback(() -> Text.literal("Player " + player.getName().getString() + " is haunted.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Player " + player.getName().getString() + " is not haunted.").withColor(Formatting.RED.getColorValue()), false);
							}
							return 1;
						})
					)
				)
				.then(
					literal("isInCave")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							final PlayerEntity player = context.getSource().getPlayer();
							if (Algorithms.isEntityInCave(player)) {
								context.getSource().sendFeedback(() -> Text.literal("You are in a cave.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("You are not in a cave.").withColor(Formatting.RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot query server cave status. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else if (Algorithms.isEntityInCave(player)) {
								context.getSource().sendFeedback(() -> Text.literal("Player " + player.getName().getString() + " is in a cave.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Player " + player.getName().getString() + " is not in a cave.").withColor(Formatting.RED.getColorValue()), false);
							}
							return 1;
						})
					)
				)
				.then(
					literal("trackingFlickerDoor")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							final PlayerEntity player = context.getSource().getPlayer();
							if (FlickerDoor.trackedPlayers.contains(player.getUuid())) {
								context.getSource().sendFeedback(() -> Text.literal("You are being tracked for flicker door event.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("You are not being tracked for flicker door event.").withColor(Formatting.RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot query server torch placement. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else if (FlickerDoor.trackedPlayers.contains(player.getUuid())) {
								context.getSource().sendFeedback(() -> Text.literal("Player " + player.getName().getString() + " is being tracked for flicker door event.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Player " + player.getName().getString() + " is not being tracked for flicker door event.").withColor(Formatting.RED.getColorValue()), false);
							}
							return 1;
						})
					)
				)
				.then(
					literal("trackingTorches")
					.executes(context -> {
						if (context.getSource().isExecutedByPlayer()) {
							final PlayerEntity player = context.getSource().getPlayer();
							if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
								context.getSource().sendFeedback(() -> Text.literal("You are being tracked for torch placements.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("You are not being tracked for torch placements.").withColor(Formatting.RED.getColorValue()), false);
							}
						} else {
							context.getSource().sendFeedback(() -> Text.literal("Cannot query server torch placement. Please specify a player.").withColor(Formatting.DARK_RED.getColorValue()), false);
						}
						return 1;
					})
					.then(
						argument("player", StringArgumentType.word())
						.suggests((context, builder) -> {
							final Iterable<String> playerNames = context.getSource().getPlayerNames();
							for (final String playerName : playerNames) {
								builder.suggest(playerName);
							}
							return builder.buildFuture();
						})
						.executes(context -> {
							final String playerName = StringArgumentType.getString(context, "player");
							final PlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
							if (player == null) {
								context.getSource().sendFeedback(() -> Text.literal("Player not found.").withColor(Formatting.DARK_RED.getColorValue()), false);
							} else if (ExtinguishTorches.torchPlacementMap.containsKey(player.getUuid())) {
								context.getSource().sendFeedback(() -> Text.literal("Player " + player.getName().getString() + " is being tracked for torch placements.").withColor(Formatting.BLUE.getColorValue()), false);
							} else {
								context.getSource().sendFeedback(() -> Text.literal("Player " + player.getName().getString() + " is not being tracked for torch placements.").withColor(Formatting.RED.getColorValue()), false);
							}
							return 1;
						})
					)
				)
			)
		));
	}

	private static void summonFakeHerobrine(final PlayerEntity player, final String skin) {
		destroyFakeHerobrine();
		final World world = player.getWorld();
		herobrineEntity = new HerobrineEntity(world, skin);
		herobrineEntity.setPosition(player.getPos());
		herobrineEntity.setBodyRotation(player.getYaw());
		herobrineEntity.setHeadRotation(player.getPitch(), player.getYaw(), Algorithms.randomBetween(-15.0f, 15.0f));
		herobrineEntity.summon();
	}

	private static void destroyFakeHerobrine() {
		if (herobrineEntity != null) herobrineEntity.remove();
	}
}
