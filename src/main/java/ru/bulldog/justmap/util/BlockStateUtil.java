package ru.bulldog.justmap.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidFillable;
import net.minecraft.block.PlantBlock;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.property.Properties;

public class BlockStateUtil {
	public static final BlockState AIR = Blocks.AIR.getDefaultState();
	public static final BlockState CAVE_AIR = Blocks.CAVE_AIR.getDefaultState();
	public static final BlockState VOID_AIR = Blocks.VOID_AIR.getDefaultState();

	public static boolean checkState(BlockState state, boolean liquids, boolean plants) {
		return BlockStateUtil.isAir(state) || (!liquids && isLiquid(state, false)) || (!plants && isPlant(state));
	}

	public static boolean isAir(BlockState state) {
		return state.isAir() || state == AIR || state == CAVE_AIR || state == VOID_AIR;
	}

	public static boolean isLiquid(BlockState state, boolean lava) {
		Block material = state.getBlock();
		return state.isLiquid() && (lava || material != Blocks.LAVA);
	}

	public static boolean isWater(BlockState state) {
		return !isSeaweed(state) && state.getFluidState().isIn(FluidTags.WATER);
	}

	public static boolean isPlant(BlockState state) {
		Block material = state.getBlock();
		return material instanceof PlantBlock || isSeaweed(state);
	}

	public static boolean isSeaweed(BlockState state) {
		Block material = state.getBlock();
		return material instanceof FluidFillable;
	}

	public static boolean isWaterlogged(BlockState state) {
		if (state.contains(Properties.WATERLOGGED))
			return state.get(Properties.WATERLOGGED);

		return isSeaweed(state);
	}
}
