package ru.bulldog.justmap.mixins.server;

import net.minecraft.block.BlockState;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World {

	protected ServerWorldMixin(MutableWorldProperties properties,
							   RegistryKey<World> registryRef,
							   DynamicRegistryManager registryManager,
							   RegistryEntry<DimensionType> dimensionEntry,
							   boolean isClient,
							   boolean debugWorld,
							   long seed,
							   int maxChainedNeighborUpdates) {
		super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
	}

	@Inject(method = "onBlockChanged", at = @At("HEAD"))
	public void onBlockChanged(BlockPos pos, BlockState oldBlock, BlockState newBlock, CallbackInfo info) {
//		if (!Objects.equals(oldBlock.getBlock(), newBlock.getBlock())) {
//			if (!StateUtil.isAir(newBlock)) {
//				Identifier blockId = Registry.BLOCK.getId(newBlock.getBlock());
//				String modId = blockId.getNamespace();
//				String id = blockId.getPath();
//				String stateFile = String.format("/assets/%s/blockstates/%s.json", modId, id);
//				try {
//					JsonObject stateJson = JsonFactory.getJsonObject(stateFile);
//					if (stateJson.has("variants")) {
//						JsonObject variants = stateJson.getAsJsonObject("variants");
//						variants.entrySet().forEach((entry) -> {
//							System.out.println(entry.getKey());
//							if (entry.getValue().isJsonArray()) {
//								JsonArray entryArray = entry.getValue().getAsJsonArray();
//								entryArray.forEach(elem -> {
//								});
//							} else {
//							}
//						});
//					}
//				} catch (Exception ex) {
//					System.out.println(String.format("Blockstate %s not available.", stateFile));
//				}
//			}
//		}
	}
}
