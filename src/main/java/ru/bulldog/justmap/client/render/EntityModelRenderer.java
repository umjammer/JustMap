package ru.bulldog.justmap.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Ghast;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.bulldog.justmap.client.JustMapClient;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.util.math.MathUtil;

/**
 * Draws a live entity model as a map icon.
 *
 * <p>Since 26.2 GUI code cannot drive the entity renderer itself; it hands the GUI an
 * {@link EntityRenderState} and the picture-in-picture pass renders it. The facing that used
 * to be forced onto the entity is now written into the render state instead, so the entity
 * itself is left untouched.
 */
public class EntityModelRenderer {

	private static final Minecraft minecraft = Minecraft.getInstance();

	public static void renderModel(GuiGraphicsExtractor context, Entity entity, double x, double y) {
		if (!(entity instanceof LivingEntity livingEntity)) return;

		EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
		EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(livingEntity);
		EntityRenderState renderState = renderer.createRenderState(livingEntity, 1.0F);
		renderState.shadowPieces.clear();

		if (renderState instanceof LivingEntityRenderState livingState) {
			float yaw = facingYaw(livingEntity);
			livingState.bodyRot = yaw;
			livingState.yRot = 0.0F;
			livingState.xRot = 0.0F;
			livingState.boundingBoxWidth = livingState.boundingBoxWidth / livingState.scale;
			livingState.boundingBoxHeight = livingState.boundingBoxHeight / livingState.scale;
			livingState.scale = 1.0F;
		}

		int modelSize = ClientSettings.entityModelSize;
		float scale = (float) getScale(livingEntity);

		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
		if (ClientSettings.rotateMap) {
			rotation.rotateZ((float) Math.toRadians(MathUtil.correctAngle(minecraft.player.yHeadRot)));
		}

		Vector3f translation = new Vector3f(0.0F, renderState.boundingBoxHeight / 2.0F, 0.0F);

		int x0 = (int) x;
		int y0 = (int) y;
		context.entity(renderState, scale, translation, rotation, null, x0, y0, x0 + modelSize, y0 + modelSize);
	}

	private static double getScale(LivingEntity livingEntity) {
		int modelSize = ClientSettings.entityModelSize;
		double mapScale = JustMapClient.getMiniMap().getScale();

		modelSize = (int) Math.min(modelSize, modelSize / mapScale);

		double scaleX = modelSize / Math.max(livingEntity.getBbWidth(), 1.0F);
		double scaleY = modelSize / Math.max(livingEntity.getBbHeight(), 1.0F);

		double scale = Math.max(Math.min(scaleX, scaleY), modelSize);

		if (livingEntity instanceof Ghast || livingEntity instanceof EnderDragon) {
			scale = modelSize / 3.0F;
		}
		if (livingEntity instanceof WaterAnimal) {
			scale = modelSize / 1.35F;
		}
		if (livingEntity.isSleeping()) {
			scale = modelSize;
		}

		return scale;
	}

	/** The body rotation that shows the entity facing the way it is moving on the map. */
	private static float facingYaw(LivingEntity livingEntity) {
		return switch (livingEntity.getMotionDirection()) {
			case NORTH -> 0.0F;
			case WEST -> 135.0F;
			case EAST -> 225.0F;
			default -> 180.0F;
		};
	}
}
