package ru.bulldog.justmap.util;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;


public class Dimension {
	public static int getId(Level world) {
		if (isNether(world)) return -1;
		if (isOverworld(world)) return 0;
		if (isEnd(world)) return 1;

		return Integer.MIN_VALUE;
	}

	public static Identifier fromId(int id) {
		switch(id) {
			case -1: return BuiltinDimensionTypes.NETHER.identifier();
			case 0: return BuiltinDimensionTypes.OVERWORLD.identifier();
			case 1: return BuiltinDimensionTypes.END.identifier();
		}

		return Identifier.parse("unknown");
	}

	public static boolean isEnd(Level world) {
		return isEnd(world.dimension().identifier());
	}

	public static boolean isNether(Level world) {
		return isNether(world.dimension().identifier());
	}

	public static boolean isOverworld(Level world) {
		return isOverworld(world.dimension().identifier());
	}

	public static boolean isEnd(Identifier dimId) {
		return dimId.equals(Level.END.identifier());
	}

	public static boolean isNether(Identifier dimId) {
		return dimId.equals(Level.NETHER.identifier());
	}

	public static boolean isOverworld(Identifier dimId) {
		return dimId.equals(Level.OVERWORLD.identifier());
	}
}
