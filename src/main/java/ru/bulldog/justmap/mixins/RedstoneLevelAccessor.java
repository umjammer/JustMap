package ru.bulldog.justmap.mixins;

import net.minecraft.block.RedstoneWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RedstoneWireBlock.class)
public interface RedstoneLevelAccessor {

	@Accessor(value = "COLORS")
	static int[] getPowerColors() {
		return null;
	}
}
