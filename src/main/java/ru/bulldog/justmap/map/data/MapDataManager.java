package ru.bulldog.justmap.map.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public interface MapDataManager {

	// World mapper management

	WorldMapper getWorldMapper();

	// Event callbacks

	void onChunkLoad(Level world, LevelChunk worldChunk);

	void onSetBlockState(BlockPos pos, BlockState state, Level world);

	void onTick(boolean isServer);

	void onWorldStop();
}
