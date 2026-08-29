package ru.bulldog.justmap.mixins.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.bulldog.justmap.map.data.MapDataProvider;

@Mixin(ClientLevel.class)
public abstract class ClientWorldMixin extends Level {

	protected ClientWorldMixin(WritableLevelData properties,
							   ResourceKey<Level> registryRef,
							   RegistryAccess registryManager,
							   Holder<DimensionType> dimensionEntry,
							   boolean isClient,
							   boolean debugWorld,
							   long seed,
							   int maxChainedNeighborUpdates) {
		super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
	}

	@Inject(method = "setBlock", at = @At("TAIL"))
	public void onSetBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
		MapDataProvider.getManager().onSetBlockState(pos, state, this);
	}
}
