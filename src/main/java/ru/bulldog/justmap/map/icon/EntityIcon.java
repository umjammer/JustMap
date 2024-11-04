package ru.bulldog.justmap.map.icon;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.util.math.RotationAxis;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.client.render.EntityModelRenderer;
import ru.bulldog.justmap.util.CurrentWorldPos;
import ru.bulldog.justmap.util.GameRulesUtil;
import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.math.MathUtil;
import ru.bulldog.justmap.util.render.GLC;
import ru.bulldog.justmap.util.render.RenderUtil;

public class EntityIcon extends MapIcon<EntityIcon> {

	private final Entity entity;
	final boolean hostile;

	public EntityIcon(Entity entity) {
		this.hostile = entity instanceof HostileEntity;
		this.entity = entity;
	}

	@Override
	public void draw(DrawContext context, VertexConsumerProvider consumerProvider, int mapX, int mapY, int mapW, int mapH, float rotation) {
		if (!GameRulesUtil.allowCreatureRadar() && !hostile) { return; }
		if (!GameRulesUtil.allowHostileRadar() && hostile) { return; }

		int color;
		if (entity instanceof TameableEntity) {
			TameableEntity tameable = (TameableEntity) entity;
			color = tameable.isTamed() ? Colors.GREEN : Colors.YELLOW;
		} else {
			color = (hostile) ? Colors.DARK_RED : Colors.YELLOW;
		}
		int size = ClientSettings.entityIconSize;
		this.updatePos(mapX, mapY, mapW, mapH, size);
		if (!allowRender) return;
		if (ClientSettings.renderEntityModel) {
			EntityModelRenderer.renderModel(context.getMatrices(), consumerProvider, entity, iconPos.x, iconPos.y);
		} else if (ClientSettings.showEntityHeads) {
			EntityHeadIconImage icon = EntityHeadIconImage.getIcon(entity);
			if (icon != null) {
				if (ClientSettings.entityIconsShading) {
					int posY = CurrentWorldPos.coordY();
					int hdiff = posY - height;
					float hmod;
					if (hdiff < 0) {
						hmod = MathUtil.clamp(Math.abs(hdiff) / 24F, 0.0F, 0.5F);
						RenderUtil.texEnvMode(GLC.GL_ADD);
					} else {
						hmod = MathUtil.clamp((24 - Math.abs(hdiff)) / 24F, 0.25F, 1.0F);
						RenderUtil.texEnvMode(GLC.GL_MODULATE);
					}
					RenderSystem.setShaderColor(hmod, hmod, hmod, 1.0F);
				}
				double moveX = iconPos.x + size / 2;
				double moveY = iconPos.y + size / 2;
				float scale = MathUtil.clamp(1.0F / ClientSettings.mapScale, 0.5F, 1.5F);
				MatrixStack matrices = context.getMatrices();
				matrices.push();
				matrices.translate(moveX, moveY, 0.0);
				if (ClientSettings.rotateMap) {
					matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation + 180.0F));
				}
				matrices.scale(scale, scale, 1.0F);
				matrices.translate(-moveX, -moveY, 0.0);
				icon.draw(context, iconPos.x, iconPos.y, size);
				matrices.pop();
				RenderUtil.texEnvMode(GLC.GL_MODULATE);
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			} else {
				RenderUtil.drawOutlineCircle(iconPos.x, iconPos.y, size / 3, 0.6, color);
			}
		} else {
			RenderSystem.setShader(ShaderProgramKeys.POSITION);
			RenderUtil.drawOutlineCircle(iconPos.x, iconPos.y, size / 3, 0.6, color);
		}
	}
}
