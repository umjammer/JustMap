package ru.bulldog.justmap.mixins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.bulldog.justmap.map.data.MapDataProvider;
import ru.bulldog.justmap.util.CurrentWorldPos;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

	@Inject(method = "setLevel", at = @At("TAIL"))
	public void onJoinWorld(ClientLevel world, CallbackInfo ci) {
		MapDataProvider.getMultiworldManager().onWorldChanged(world);
		CurrentWorldPos.updateWorld(world);
	}
}
