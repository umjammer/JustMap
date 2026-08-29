package ru.bulldog.justmap.util;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class BlockStateUtil {
	public static final BlockState AIR = Blocks.AIR.defaultBlockState();
	public static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
	public static final BlockState VOID_AIR = Blocks.VOID_AIR.defaultBlockState();

	public static boolean checkState(BlockState state, boolean liquids, boolean plants) {
		return BlockStateUtil.isAir(state) || (!liquids && isLiquid(state, false)) || (!plants && isPlant(state));
	}

	public static boolean isAir(BlockState state) {
		return state.isAir() || state == AIR || state == CAVE_AIR || state == VOID_AIR;
	}

	public static boolean isLiquid(BlockState state, boolean lava) {
		Block material = state.getBlock();
		return state.liquid() && (lava || material != Blocks.LAVA);
	}

	public static boolean isWater(BlockState state) {
		return !isSeaweed(state) && state.getFluidState().is(FluidTags.WATER);
	}

	public static boolean isPlant(BlockState state) {
		Block material = state.getBlock();
		return material instanceof VegetationBlock || isSeaweed(state);
	}

	public static boolean isSeaweed(BlockState state) {
		Block material = state.getBlock();
		return material instanceof LiquidBlockContainer;
	}

	public static boolean isWaterlogged(BlockState state) {
		if (state.hasProperty(BlockStateProperties.WATERLOGGED))
			return state.getValue(BlockStateProperties.WATERLOGGED);

		return isSeaweed(state);
	}
}
