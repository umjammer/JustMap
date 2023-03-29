package ru.bulldog.justmap.mixins.client;

import java.util.function.Supplier;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.bulldog.justmap.map.data.MapDataProvider;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin extends World {

	protected ClientWorldMixin(MutableWorldProperties properties,
							   RegistryKey<World> registryRef,
							   DynamicRegistryManager registryManager,
							   RegistryEntry<DimensionType> dimension,
							   Supplier<Profiler> profiler,
							   boolean isClient,
							   boolean debugWorld,
							   long seed,
							   int maxChainedNeighborUpdates) {
		super(properties, registryRef, registryManager, dimension, profiler, isClient, debugWorld, seed, maxChainedNeighborUpdates);
	}

	@Inject(method = "setBlockState", at = @At("TAIL"))
	public void onSetBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
		MapDataProvider.getManager().onSetBlockState(pos, state, this);
	}
}
