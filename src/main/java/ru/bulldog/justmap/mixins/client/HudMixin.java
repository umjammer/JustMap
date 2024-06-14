package ru.bulldog.justmap.mixins.client;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.ibm.icu.text.ListFormatter.Width;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.bulldog.justmap.client.JustMapClient;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.enums.ScreenPosition;
import ru.bulldog.justmap.util.colors.Colors;

@Mixin(InGameHud.class)
abstract class HudMixin {

	@Final
	@Shadow
	private MinecraftClient client;

	@Inject(at = @At("HEAD"), method = "renderStatusEffectOverlay", cancellable = true)
	protected void renderStatusEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (ClientSettings.moveEffects) {
			int posX = context.getScaledWindowWidth();
			int posY = ClientSettings.positionOffset;
			if (ClientSettings.mapPosition == ScreenPosition.TOP_RIGHT) {
				posX = JustMapClient.getMiniMap().getSkinX();
			}

			this.drawMovedEffects(context, posX, posY);
			ci.cancel();
		}
	}

	@Unique
	private void drawMovedEffects(DrawContext context, int screenX, int screenY) {
		Collection<StatusEffectInstance> statusEffects = this.client.player.getStatusEffects();
		if (statusEffects.isEmpty()) return;

		RenderSystem.enableBlend();

		int size = 24;
		int hOffset = 6;
		int vOffset = 10;

		if (!ClientSettings.showEffectTimers) {
			hOffset = 1;
			vOffset = 2;
		}

		StatusEffectSpriteManager statusEffectSpriteManager = this.client.getStatusEffectSpriteManager();
		List<Runnable> icons = Lists.newArrayListWithExpectedSize(statusEffects.size());
		List<Runnable> timers = Lists.newArrayListWithExpectedSize(statusEffects.size());
		RenderSystem.setShaderTexture(0, HandledScreen.BACKGROUND_TEXTURE);
		Iterator<StatusEffectInstance> effectsIterator = Ordering.natural().reverse().sortedCopy(statusEffects).iterator();

	 	int i = 0, j = 0;
		while (effectsIterator.hasNext()) {
	 		StatusEffectInstance statusEffectInstance = effectsIterator.next();
			RegistryEntry<StatusEffect> statusEffect = statusEffectInstance.getEffectType();
			if (statusEffectInstance.shouldShowIcon()) {
				int x = screenX;
			   	int y = screenY;
			   	if (this.client.isDemo()) {
				   y += 15;
			   	}

			   	if (statusEffect.value().isBeneficial()) {
			   		++i;
				  	x -= (size + hOffset) * i;
			   	} else {
			   		++j;
				  	x -= (size + hOffset) * j;
				  	y += size + vOffset;
			   	}

		   		int effectDuration = statusEffectInstance.getDuration();
		   		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		   		float alpha = 1.0F;
		   		if (statusEffectInstance.isAmbient()) {
		   			context.drawTexture(HandledScreen.BACKGROUND_TEXTURE ,x, y, 165, 166, size, size);
		   		} else {
			   		context.drawTexture(HandledScreen.BACKGROUND_TEXTURE ,x, y, 141, 166, size, size);
			  		if (effectDuration <= 200) {
				  		int m = 10 - effectDuration / 20;
				 		alpha = MathHelper.clamp(effectDuration / 10F / 5F * 0.5F, 0F, 0.5F) + MathHelper.cos((float) (effectDuration * Math.PI) / 5F) * MathHelper.clamp(m / 10F * 0.25F, 0.0F, 0.25F);
			  		}
		   		}

		   		Sprite sprite = statusEffectSpriteManager.getSprite(statusEffect);
		   		final int fx = x, fy = y;
		   		final float fa = alpha;
		   		icons.add(() -> {
					RenderSystem.setShaderTexture(0, sprite.getContents().getId());
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fa);
					context.drawSprite(fx + 3, fy + 3, 0, 18, 18, sprite);
		   		});
		   		if (ClientSettings.showEffectTimers) {
			   		timers.add(() ->
							context.drawCenteredTextWithShadow(client.textRenderer, convertDuration(effectDuration), fx + size / 2, fy + (size + 1), Colors.WHITE));
		   		}
			}
	 	}

	 	icons.forEach(Runnable::run);
	 	timers.forEach(Runnable::run);
	}

	@Unique
	private static String convertDuration(int time) {
		int mils = time * 50;
		int s = (mils / 1000) % 60;
		int m = (mils / (1000 * 60)) % 60;

		return String.format("%02d:%02d", m, s);
	}
}
