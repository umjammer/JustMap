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
 * A solid colour quad with four freely positioned corners.
 *
 * <p>Since 26.2 the GUI is drawn from render states rather than by pushing vertices at the
 * driver, and vanilla only ships axis aligned states ({@code ColoredRectangleRenderState},
 * {@code BlitRenderState}). The map draws triangles, circles and rotated shapes, so it
 * contributes its own state. The GUI batches quads, so a triangle is expressed by repeating
 * the last corner.
 */
public record GuiQuad(
	RenderPipeline pipeline,
	TextureSetup textureSetup,
	Matrix3x2fc pose,
	float x0, float y0,
	float x1, float y1,
	float x2, float y2,
	float x3, float y3,
	int color,
	@Nullable ScreenRectangle scissorArea,
	@Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

	public GuiQuad(
		RenderPipeline pipeline,
		TextureSetup textureSetup,
		Matrix3x2fc pose,
		float x0, float y0,
		float x1, float y1,
		float x2, float y2,
		float x3, float y3,
		int color,
		@Nullable ScreenRectangle scissorArea
	) {
		this(pipeline, textureSetup, pose, x0, y0, x1, y1, x2, y2, x3, y3, color, scissorArea,
			 bounds(x0, y0, x1, y1, x2, y2, x3, y3, pose, scissorArea));
	}

	@Override
	public void buildVertices(VertexConsumer vertexConsumer) {
		vertexConsumer.addVertexWith2DPose(this.pose, this.x0, this.y0).setColor(this.color);
		vertexConsumer.addVertexWith2DPose(this.pose, this.x1, this.y1).setColor(this.color);
		vertexConsumer.addVertexWith2DPose(this.pose, this.x2, this.y2).setColor(this.color);
		vertexConsumer.addVertexWith2DPose(this.pose, this.x3, this.y3).setColor(this.color);
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
