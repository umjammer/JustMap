package ru.bulldog.justmap.map.icon;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Monster;
import org.joml.Matrix3x2fStack;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.client.render.EntityModelRenderer;
import ru.bulldog.justmap.util.CurrentWorldPos;
import ru.bulldog.justmap.util.GameRulesUtil;
import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.math.MathUtil;
import ru.bulldog.justmap.util.render.RenderUtil;

public class EntityIcon extends MapIcon<EntityIcon> {

	private final Entity entity;
	final boolean hostile;

	public EntityIcon(Entity entity) {
		this.hostile = entity instanceof Monster;
		this.entity = entity;
	}

	@Override
	public void draw(GuiGraphicsExtractor context, int mapX, int mapY, int mapW, int mapH, float rotation) {
		if (!GameRulesUtil.allowCreatureRadar() && !hostile) { return; }
		if (!GameRulesUtil.allowHostileRadar() && hostile) { return; }

		int color;
		if (entity instanceof TamableAnimal) {
			TamableAnimal tameable = (TamableAnimal) entity;
			color = tameable.isTame() ? Colors.GREEN : Colors.YELLOW;
		} else {
			color = (hostile) ? Colors.DARK_RED : Colors.YELLOW;
		}
		int size = ClientSettings.entityIconSize;
		this.updatePos(mapX, mapY, mapW, mapH, size);
		if (!allowRender) return;
		if (ClientSettings.renderEntityModel) {
			EntityModelRenderer.renderModel(context, entity, iconPos.x, iconPos.y);
		} else if (ClientSettings.showEntityHeads) {
			EntityHeadIconImage icon = EntityHeadIconImage.getIcon(entity);
			if (icon != null) {
				float moveX = (float) (iconPos.x + size / 2);
				float moveY = (float) (iconPos.y + size / 2);
				float scale = MathUtil.clamp(1.0F / ClientSettings.mapScale, 0.5F, 1.5F);
				Matrix3x2fStack matrices = context.pose();
				matrices.pushMatrix();
				if (ClientSettings.rotateMap) {
					matrices.rotateAbout((float) Math.toRadians(rotation + 180.0F), moveX, moveY);
				}
				matrices.scaleAround(scale, scale, moveX, moveY);
				icon.draw(context, iconPos.x, iconPos.y, size, size, shadingTint(height));
				matrices.popMatrix();
			} else {
				RenderUtil.drawOutlineCircle(context, iconPos.x, iconPos.y, size / 3, 0.6, color);
			}
		} else {
			RenderUtil.drawOutlineCircle(context, iconPos.x, iconPos.y, size / 3, 0.6, color);
		}
	}

	/**
	 * Icons dim with the height difference to the player. The old renderer set a global
	 * shader colour for this; in the 26.2 GUI the shade travels with the element as a
	 * vertex tint instead.
	 */
	static int shadingTint(int height) {
		if (!ClientSettings.entityIconsShading) return -1;

		int hdiff = CurrentWorldPos.coordY() - height;
		float hmod = hdiff < 0
				? MathUtil.clamp(Math.abs(hdiff) / 24F, 0.0F, 0.5F)
				: MathUtil.clamp((24 - Math.abs(hdiff)) / 24F, 0.25F, 1.0F);

		int shade = (int) (hmod * 255);
		return ARGB.color(255, shade, shade, shade);
	}
}
