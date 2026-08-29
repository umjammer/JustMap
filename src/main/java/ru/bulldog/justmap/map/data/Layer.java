package ru.bulldog.justmap.map.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import ru.bulldog.justmap.util.Dimension;
import ru.bulldog.justmap.util.GameRulesUtil;

public enum Layer {
	SURFACE("surface", 256),
	CAVES("caves", 8),
	NETHER("nether", 16);

	private static final int WORLD_HEIGHT = 256;

	private final String name;
	private final int height;

	Layer(String name, int height) {
		this.name = name;
		this.height = height;
	}

	public String getName() {
		return name;
	}

	public int getHeight() {
		return height;
	}

	public int getLevels() {
		return WORLD_HEIGHT / getHeight();
	}

	public static Layer getLayer(Level world, BlockPos pos) {
		if (Dimension.isNether(world)) {
			return NETHER;
		} else if (GameRulesUtil.allowCaves() && shouldRenderCaves(world, pos)) {
			return CAVES;
		}
		return SURFACE;
	}

	public static int getLevel(Layer layer, int y) {
		if (SURFACE.equals(layer)) return 0;
		return y / layer.height;
	}

	private static boolean shouldRenderCaves(Level world, BlockPos pos) {
		if (Dimension.isEnd(world)) {
			return false;
		}

		DimensionType dimType = world.dimensionType();
		if (dimType.hasCeiling() || !dimType.hasSkyLight()) {
			return true;
		}

		return (!world.canSeeSkyFromBelowWater(pos) && !hasSkyLight(world, pos) ||
				world.dimension().identifier().equals(BuiltinDimensionTypes.OVERWORLD_CAVES.identifier()));
	}

	private static boolean hasSkyLight(Level world, BlockPos pos) {
		// FIXME: this is a bit expensive for repeating use...
		if (world.getBrightness(LightLayer.SKY, pos) > 0) return true;
		if (world.getBrightness(LightLayer.SKY, pos.above()) > 0) return true;
		if (world.getBrightness(LightLayer.SKY, pos.north()) > 0) return true;
		if (world.getBrightness(LightLayer.SKY, pos.east()) > 0) return true;
		if (world.getBrightness(LightLayer.SKY, pos.south()) > 0) return true;
		if (world.getBrightness(LightLayer.SKY, pos.west()) > 0) return true;

		return false;
	}
}
