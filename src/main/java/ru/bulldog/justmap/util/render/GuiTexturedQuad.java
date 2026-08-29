package ru.bulldog.justmap.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/**
 * A textured quad with four freely positioned corners and per-corner UVs.
 *
 * <p>Vanilla's {@code BlitRenderState} only takes an integer aligned rectangle, which is too
 * coarse for the map: it scrolls by sub-pixel amounts and is drawn as a disc when the minimap
 * is round. Corners are given in the same winding as the vanilla blit state
 * (top-left, bottom-left, bottom-right, top-right).
 */
public record GuiTexturedQuad(
	RenderPipeline pipeline,
	TextureSetup textureSetup,
	Matrix3x2fc pose,
	float x0, float y0, float u0, float v0,
	float x1, float y1, float u1, float v1,
	float x2, float y2, float u2, float v2,
	float x3, float y3, float u3, float v3,
	int color,
	@Nullable ScreenRectangle scissorArea,
	@Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

	public GuiTexturedQuad(
		RenderPipeline pipeline,
		TextureSetup textureSetup,
		Matrix3x2fc pose,
		float x0, float y0, float u0, float v0,
		float x1, float y1, float u1, float v1,
		float x2, float y2, float u2, float v2,
		float x3, float y3, float u3, float v3,
		int color,
		@Nullable ScreenRectangle scissorArea
	) {
		this(pipeline, textureSetup, pose,
			 x0, y0, u0, v0, x1, y1, u1, v1, x2, y2, u2, v2, x3, y3, u3, v3,
			 color, scissorArea, bounds(x0, y0, x1, y1, x2, y2, x3, y3, pose, scissorArea));
	}

	@Override
	public void buildVertices(VertexConsumer vertexConsumer) {
		vertexConsumer.addVertexWith2DPose(this.pose, this.x0, this.y0).setUv(this.u0, this.v0).setColor(this.color);
		vertexConsumer.addVertexWith2DPose(this.pose, this.x1, this.y1).setUv(this.u1, this.v1).setColor(this.color);
		vertexConsumer.addVertexWith2DPose(this.pose, this.x2, this.y2).setUv(this.u2, this.v2).setColor(this.color);
		vertexConsumer.addVertexWith2DPose(this.pose, this.x3, this.y3).setUv(this.u3, this.v3).setColor(this.color);
	}

	private static @Nullable ScreenRectangle bounds(
			float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
			Matrix3x2fc pose, @Nullable ScreenRectangle scissorArea) {

		float minX = Math.min(Math.min(x0, x1), Math.min(x2, x3));
		float maxX = Math.max(Math.max(x0, x1), Math.max(x2, x3));
		float minY = Math.min(Math.min(y0, y1), Math.min(y2, y3));
		float maxY = Math.max(Math.max(y0, y1), Math.max(y2, y3));

		ScreenRectangle bounds = new ScreenRectangle(
				Mth.floor(minX), Mth.floor(minY),
				Mth.ceil(maxX - minX), Mth.ceil(maxY - minY)).transformMaxBounds(pose);

		return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
	}
}
