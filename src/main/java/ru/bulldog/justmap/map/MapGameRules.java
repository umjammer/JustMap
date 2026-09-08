package ru.bulldog.justmap.map;

import java.util.HashMap;
import java.util.Map;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.server.JustMapServer;

public class MapGameRules {

	public final static GameRule<Boolean> ALLOW_CAVES_MAP = register("allow_caves_map", false);
	public final static GameRule<Boolean> ALLOW_ENTITY_RADAR = register("allow_entity_radar", false);
	public final static GameRule<Boolean> ALLOW_PLAYER_RADAR = register("allow_player_radar", false);
	public final static GameRule<Boolean> ALLOW_CREATURE_RADAR = register("allow_creature_radar", false);
	public final static GameRule<Boolean> ALLOW_HOSTILE_RADAR = register("allow_hostile_radar", false);
	public final static GameRule<Boolean> ALLOW_SLIME_CHUNKS = register("allow_slime_chunks", false);
	public final static GameRule<Boolean> ALLOW_TELEPORTATION = register("allow_waypoints_jump", false);

	private MapGameRules() {}

	public static void init() {
		JustMap.LOGGER.info("Map gamerules loaded.");
	}

	private static GameRule<Boolean> register(String name, boolean defaultValue) {
		return Registry.register(
			BuiltInRegistries.GAME_RULE,
			Identifier.fromNamespaceAndPath(JustMap.MODID, name),
			new GameRule<>(
				GameRuleCategory.MISC,
				GameRuleType.BOOL,
				BoolArgumentType.bool(),
				GameRuleTypeVisitor::visitBoolean,
				Codec.BOOL,
				value -> value ? 1 : 0,
				defaultValue,
				FeatureFlagSet.of()
			)
		);
	}

	private static final Map<String, GameRule<Boolean>> codes;

	static {
		codes = new HashMap<>();

		codes.put("§a", ALLOW_CAVES_MAP);
		codes.put("§b", ALLOW_ENTITY_RADAR);
		codes.put("§c", ALLOW_PLAYER_RADAR);
		codes.put("§d", ALLOW_CREATURE_RADAR);
		codes.put("§e", ALLOW_HOSTILE_RADAR);
		codes.put("§s", ALLOW_SLIME_CHUNKS);
		codes.put("§t", ALLOW_TELEPORTATION);
	}

	/**
	 * The chat code a rule is broadcast with, or null if the rule is not one of ours.
	 */
	public static String getCode(GameRule<?> rule) {
		for (Map.Entry<String, GameRule<Boolean>> entry : codes.entrySet()) {
			if (entry.getValue() == rule) {
				return entry.getKey();
			}
		}
		return null;
	}

	public static boolean isAllowed(GameRule<Boolean> rule) {
		boolean allow = true;
		if (JustMap.getSide() == EnvType.SERVER) {
			allow = JustMapServer.getServer().getGameRules().get(rule);
		} else {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.hasSingleplayerServer()) {
				allow = minecraft.getSingleplayerServer().getGameRules().get(rule);
			} else if (!minecraft.isLocalServer()) {
				if (minecraft.level == null) return false;
				return false;
			}
		}
		return allow;
	}

	/*
	 *	§0§0: prefix
	 *  §f§f: suffix
	 *
	 *	§a: cave mapping
	 *	§b: entities radar (all)
	 *	§c: entities radar (player)
	 *	§d: entities radar (animal)
	 *	§e: entities radar (hostile)
	 *	§s: slime chunks
	 *	§t: teleportation
	 *
	 *  §1: enable
	 *  §0: disable
	 */
	@Environment(EnvType.CLIENT)
	public static void parseCommand(String command) {
		Minecraft minecraft = Minecraft.getInstance();
		MinecraftServer server = minecraft.getSingleplayerServer();
		GameRules gameRules = server.getGameRules();
		codes.forEach((key, rule) -> {
			if (command.contains(key)) {
				int valPos = command.indexOf(key) + 2;
				boolean value = command.startsWith("§1", valPos);
				gameRules.set(rule, value, server);
				JustMap.LOGGER.info("Map rule {} switched to: {}", rule, value);
			}
		});
	}
}
