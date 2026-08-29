package ru.bulldog.justmap.mixins;

import net.minecraft.world.level.block.RedStoneWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RedStoneWireBlock.class)
public interface RedstoneLevelAccessor {

	@Accessor(value = "COLORS")
	static int[] getPowerColors() {
		return null;
	}
}
