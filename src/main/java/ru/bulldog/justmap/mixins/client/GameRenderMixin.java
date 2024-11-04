package ru.bulldog.justmap.mixins.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.bulldog.justmap.client.render.WaypointRenderer;

@Mixin(GameRenderer.class)
public abstract class GameRenderMixin {

	@Final
	@Shadow
	private Camera camera;

	@Final
	@Shadow
	MinecraftClient client;

	@Final
	@Shadow
	private BufferBuilderStorage buffers;

	@Shadow
	private float lastFovMultiplier;

	@Inject(method = "render", at = @At("RETURN"))
	public void renderHUD(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
		DrawContext context = new DrawContext(this.client, this.buffers.getEntityVertexConsumers());
		float tickDelta = tickCounter.getTickDelta(false);
		WaypointRenderer.renderHUD(context, tickDelta, (float) this.lastFovMultiplier);
	}
}
