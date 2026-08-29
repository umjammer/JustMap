package ru.bulldog.justmap.mixins.client;

import java.util.Collection;

import com.google.common.collect.Ordering;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
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

/**
 * Moves the status effect icons out from under the minimap.
 *
 * <p>26.2 split the in-game overlay out of {@code Gui} into {@code Hud}, and the icons are
 * drawn from named sprites through the GUI render states rather than by binding the
 * inventory texture, so the copy of vanilla's layout below follows the new drawing calls.
 */
@Mixin(Hud.class)
abstract class HudMixin {

	@Unique
	private static final Identifier EFFECT_BACKGROUND_AMBIENT_SPRITE =
			Identifier.withDefaultNamespace("hud/effect_background_ambient");
	@Unique
	private static final Identifier EFFECT_BACKGROUND_SPRITE =
			Identifier.withDefaultNamespace("hud/effect_background");

	@Final
	@Shadow
	private Minecraft minecraft;

	@Inject(at = @At("HEAD"), method = "extractEffects", cancellable = true)
	protected void renderStatusEffects(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
		if (ClientSettings.moveEffects) {
			int posX = context.guiWidth();
			int posY = ClientSettings.positionOffset;
			if (ClientSettings.mapPosition == ScreenPosition.TOP_RIGHT) {
				posX = JustMapClient.getMiniMap().getSkinX();
			}

			this.drawMovedEffects(context, posX, posY);
			ci.cancel();
		}
	}

	@Unique
	private void drawMovedEffects(GuiGraphicsExtractor context, int screenX, int screenY) {
		Collection<MobEffectInstance> statusEffects = this.minecraft.player.getActiveEffects();
		if (statusEffects.isEmpty()) return;

		int size = 24;
		int hOffset = 6;
		int vOffset = 10;

		if (!ClientSettings.showEffectTimers) {
			hOffset = 1;
			vOffset = 2;
		}

		int beneficialCount = 0, harmfulCount = 0;
		for (MobEffectInstance statusEffectInstance : Ordering.natural().reverse().sortedCopy(statusEffects)) {
			Holder<MobEffect> statusEffect = statusEffectInstance.getEffect();
			if (!statusEffectInstance.showIcon()) continue;

			int x = screenX;
			int y = screenY;
			if (this.minecraft.isDemo()) {
				y += 15;
			}

			if (statusEffect.value().isBeneficial()) {
				++beneficialCount;
				x -= (size + hOffset) * beneficialCount;
			} else {
				++harmfulCount;
				x -= (size + hOffset) * harmfulCount;
				y += size + vOffset;
			}

			int effectDuration = statusEffectInstance.getDuration();
			float alpha = 1.0F;
			if (statusEffectInstance.isAmbient()) {
				context.blitSprite(RenderPipelines.GUI_TEXTURED, EFFECT_BACKGROUND_AMBIENT_SPRITE, x, y, size, size);
			} else {
				context.blitSprite(RenderPipelines.GUI_TEXTURED, EFFECT_BACKGROUND_SPRITE, x, y, size, size);
				if (effectDuration <= 200) {
					int m = 10 - effectDuration / 20;
					alpha = Mth.clamp(effectDuration / 10F / 5F * 0.5F, 0F, 0.5F)
							+ Mth.cos((float) (effectDuration * Math.PI) / 5F) * Mth.clamp(m / 10F * 0.25F, 0.0F, 0.25F);
					alpha = Mth.clamp(alpha, 0.0F, 1.0F);
				}
			}

			context.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(statusEffect),
					x + 3, y + 3, 18, 18, ARGB.white(alpha));

			if (ClientSettings.showEffectTimers) {
				context.centeredText(this.minecraft.font, convertDuration(effectDuration),
						x + size / 2, y + (size + 1), Colors.WHITE);
			}
		}
	}

	@Unique
	private static String convertDuration(int time) {
		int mils = time * 50;
		int s = (mils / 1000) % 60;
		int m = (mils / (1000 * 60)) % 60;

		return String.format("%02d:%02d", m, s);
	}
}
