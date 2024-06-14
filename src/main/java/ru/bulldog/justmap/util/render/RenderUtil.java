package ru.bulldog.justmap.util.render;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.map.minimap.skin.MapSkin;
import ru.bulldog.justmap.map.minimap.skin.MapSkin.RenderData;
import ru.bulldog.justmap.util.colors.ColorUtil;

public class RenderUtil {

	private RenderUtil() {}

	private final static VertexFormat VF_POS_TEX_NORMAL = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("UV0", VertexFormatElement.UV_0).add("Normal", VertexFormatElement.NORMAL).build();
	private final static Tessellator tessellator = Tessellator.getInstance();
	private static BufferBuilder vertexBuffer;
	private final static TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

	public static int getWidth(Text text) {
		return textRenderer.getWidth(text);
	}

	public static int getWidth(String string) {
		return textRenderer.getWidth(string);
	}

	public void drawCenteredString(DrawContext context, String string, double x, double y, int color) {
		context.drawTextWithShadow(textRenderer, string, (int) (x - textRenderer.getWidth(string) / 2), (int) y, color);
	}

	public static void drawCenteredText(DrawContext context, Text text, double x, double y, int color) {
		context.drawTextWithShadow(textRenderer, text, (int) (x - textRenderer.getWidth(text) / 2), (int) y, color);
	}

	public static void drawBoundedString(DrawContext context, String string, int x, int y, int leftBound, int rightBound, int color) {
		if (string == null) return;

		int stringWidth = textRenderer.getWidth(string);
		int drawX = x - stringWidth / 2;
		if (drawX < leftBound) {
			drawX = leftBound;
		} else if (drawX + stringWidth > rightBound) {
			drawX = rightBound - stringWidth;
		}

		context.drawTextWithShadow(textRenderer, string, drawX, y, color);
	}

	public static void drawRightAlignedString(DrawContext context, String string, int x, int y, int color) {
		context.drawTextWithShadow(textRenderer, string, x - textRenderer.getWidth(string), y, color);
	}

	public static void drawDiamond(double x, double y, int width, int height, int color) {
		drawTriangle(x, y + height / 2,
				 x + width, y + height / 2,
				 x + width / 2, y,
				 color);
		drawTriangle(x, y + height / 2,
				 x + width / 2, y + height,
				 x + width, y + height / 2,
				 color);
	}

	public static void bindTexture(Identifier id) {
		RenderSystem.setShader(GameRenderer::getPositionTexProgram);
		RenderSystem.setShaderTexture(0, id);
	}

	public static void bindTexture(int id) {
		RenderSystem.setShader(GameRenderer::getPositionTexProgram);
		RenderSystem.setShaderTexture(0, id);
	}

	public static void applyFilter(boolean force) {
		// This is not working properly. Is it even needed?
//		if (force || ClientSettings.textureFilter) {
//			RenderSystem.texParameter(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_MIN_FILTER, GLC.GL_LINEAR_MIPMAP_LINEAR);
//			RenderSystem.texParameter(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_MAG_FILTER, GLC.GL_LINEAR);
//		} else {
//			RenderSystem.texParameter(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_MIN_FILTER, GLC.GL_LINEAR_MIPMAP_NEAREST);
//			RenderSystem.texParameter(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_MAG_FILTER, GLC.GL_NEAREST);
//		}
	}

	public static void enable(int target) {
		GL11.glEnable(target);
	}

	public static void disable(int target) {
		GL11.glDisable(target);
	}

	public static void enableScissor() {
		RenderSystem.assertOnRenderThread();
		enable(GLC.GL_SCISSOR_TEST);
	}

	public static void disableScissor() {
		RenderSystem.assertOnRenderThread();
		disable(GLC.GL_SCISSOR_TEST);
	}

	public static void applyScissor(int x, int y, int width, int height) {
		RenderSystem.assertOnRenderThread();
		GL11.glScissor(x, y, width, height);
	}

	public static void texEnvMode(int mode) {
		// Crashes, and does not seem to be needed?
		// GL11.glTexEnvi(GLC.GL_TEXTURE_ENV, GLC.GL_TEXTURE_ENV_MODE, mode);
	}

	public static void startDraw() {
		startDraw(VertexFormats.POSITION_TEXTURE);
	}

	public static void startDrawNormal() {
		startDraw(VF_POS_TEX_NORMAL);
	}

	public static void startDraw(VertexFormat vertexFormat) {
		startDraw(VertexFormat.DrawMode.QUADS, vertexFormat);
	}

	public static void startDraw(VertexFormat.DrawMode mode, VertexFormat vertexFormat) {
		vertexBuffer = tessellator.begin(mode, vertexFormat);
	}

	public static void endDraw() {
		var builtBuffer = vertexBuffer.endNullable();
		if (builtBuffer != null) {
			BufferRenderer.drawWithGlobalProgram(builtBuffer);
		}
	}

	public static void drawQuad(double x, double y, double w, double h) {
		startDraw();
		addQuad(x, y, w, h);
		endDraw();
	}

	public static BufferBuilder getBuffer() {
		return vertexBuffer;
	}

	public static void drawTriangle(double x1, double y1, double x2, double y2, double x3, double y3, int color) {
		float a = (float)(color >> 24 & 255) / 255.0F;
		float r = (float)(color >> 16 & 255) / 255.0F;
		float g = (float)(color >> 8 & 255) / 255.0F;
		float b = (float)(color & 255) / 255.0F;

		RenderSystem.setShaderColor(r, g, b, a);
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		startDraw(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION);
		vertexBuffer.vertex((float) x1, (float) y1, 0);
		vertexBuffer.vertex((float) x2, (float) y2, 0);
		vertexBuffer.vertex((float) x3, (float) y3, 0);
		endDraw();
	}

	public static void drawLine(double x1, double y1, double x2, double y2, int color) {
		float a = (float)(color >> 24 & 255) / 255.0F;
		float r = (float)(color >> 16 & 255) / 255.0F;
		float g = (float)(color >> 8 & 255) / 255.0F;
		float b = (float)(color & 255) / 255.0F;

		RenderSystem.setShaderColor(r, g, b, a);
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		startDraw(VertexFormat.DrawMode.LINES, VertexFormats.POSITION);
		vertexBuffer.vertex((float) x1, (float) y1, 0);
		vertexBuffer.vertex((float) x2, (float) y2, 0);
		endDraw();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	public static void drawOutlineCircle(double x, double y, double radius, double outline, int color) {
		int darken = ColorUtil.colorBrigtness(color, -3);
		RenderUtil.drawCircle(x, y, radius + outline, darken);
		RenderUtil.drawCircle(x, y, radius, color);
	}

	public static void drawCircle(double x, double y, double radius, int color) {
		float a = (float)(color >> 24 & 255) / 255.0F;
		float r = (float)(color >> 16 & 255) / 255.0F;
		float g = (float)(color >> 8 & 255) / 255.0F;
		float b = (float)(color & 255) / 255.0F;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(r, g, b, a);
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		drawCircleVertices(x, y, radius);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	public static void drawCircleVertices(double x, double y, double radius) {
		double pi2 = Math.PI * 2;
		startDraw(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION);
		vertexBuffer.vertex((float) x, (float) y, 0);
		int sides = 50;
		for (int i = 0; i <= sides; i++) {
			double angle = (pi2 * i / sides) + Math.toRadians(180);
			double vx = x + Math.sin(angle) * radius;
			double vy = y + Math.cos(angle) * radius;
			vertexBuffer.vertex((float) vx, (float) vy, 0);
		}
		endDraw();
	}

	public static void fill(double x, double y, double w, double h, int color) {
		fill(AffineTransformation.identity().getMatrix(), x, y, w, h, color);
	}

	public static void fill(MatrixStack matrices, double x, double y, double w, double h, int color) {
		fill(matrices.peek().getPositionMatrix(), x, y, w, h, color);
	}

	public static void fill(Matrix4f matrix4f, double x, double y, double w, double h, int color) {
		float a = (float)(color >> 24 & 255) / 255.0F;
		float r = (float)(color >> 16 & 255) / 255.0F;
		float g = (float)(color >> 8 & 255) / 255.0F;
		float b = (float)(color & 255) / 255.0F;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);
		startDraw(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
		vertexBuffer.vertex(matrix4f, (float) x, (float) (y + h), 0.0F).color(r, g, b, a);
		vertexBuffer.vertex(matrix4f, (float) (x + w), (float) (y + h), 0.0F).color(r, g, b, a);
		vertexBuffer.vertex(matrix4f, (float) (x + w), (float) y, 0.0F).color(r, g, b, a);
		vertexBuffer.vertex(matrix4f, (float) x, (float) y, 0.0F).color(r, g, b, a);
		endDraw();
		RenderSystem.disableBlend();
	}

	public static void draw(DrawContext context, double x, double y, float w, float h) {
		startDrawNormal();
		draw(context, vertexBuffer, x, y, w, h, 0.0F, 0.0F, 1.0F, 1.0F);
		endDraw();
	}

	public static void drawPlayerHead(DrawContext context, double x, double y, int w, int h) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		startDrawNormal();
		draw(context, x, y, w, h, 0.125F, 0.125F, 0.25F, 0.25F);
		draw(context, x, y, w, h, 0.625F, 0.125F, 0.75F, 0.25F);
		endDraw();
	}

	public static void draw(DrawContext context, double x, double y, int w, int h, int ix, int iy, int iw, int ih, int tw, int th) {
		float minU = (float) ix / tw;
		float minV = (float) iy / th;
		float maxU = (float) (ix + iw) / tw;
		float maxV = (float) (iy + ih) / th;

		startDrawNormal();
		draw(context, vertexBuffer, x, y, w, h, minU, minV, maxU, maxV);
		endDraw();
	}

	public static void drawSkin(DrawContext context, MapSkin skin, double x, double y, float w, float h) {
		RenderData renderData = skin.getRenderData();

		if (renderData.scaleChanged || renderData.x != x || renderData.y != y ||
			renderData.width != w || renderData.height != h) {

			renderData.calculate(x, y, w, h);
		}

		float sMinU = 0.0F;
		float sMaxU = 1.0F;
		float sMinV = 0.0F;
		float sMaxV = 1.0F;
		float scaledBrd = renderData.scaledBorder;
		float hSide = renderData.hSide;
		float vSide = renderData.vSide;
		double leftC = renderData.leftC;
		double rightC = renderData.rightC;
		double topC = renderData.topC;
		double bottomC = renderData.bottomC;
		float leftU = renderData.leftU;
		float rightU = renderData.rightU;
		float topV = renderData.topV;
		float bottomV = renderData.bottomV;

		RenderSystem.enableBlend();
		RenderSystem.enableCull();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShader(GameRenderer::getPositionTexProgram);
		skin.bindTexture();
		startDrawNormal();

		draw(context, vertexBuffer, x, y, scaledBrd, scaledBrd, sMinU, sMinV, leftU, topV);
		draw(context, vertexBuffer, rightC, y, scaledBrd, scaledBrd, rightU, sMinV, sMaxU, topV);
		draw(context, vertexBuffer, x, bottomC, scaledBrd, scaledBrd, sMinU, bottomV, leftU, sMaxV);
		draw(context, vertexBuffer, rightC, bottomC, scaledBrd, scaledBrd, rightU, bottomV, sMaxU, sMaxV);

		if (skin.resizable) {
			draw(context, vertexBuffer, rightC, topC, scaledBrd, vSide, rightU, topV, sMaxU, bottomV);
			draw(context, vertexBuffer, x, topC, scaledBrd, vSide, sMinU, topV, leftU, bottomV);
			draw(context, vertexBuffer, leftC, topC, hSide, vSide, leftU, topV, rightU, bottomV);
			if (skin.repeating) {
				float tail = renderData.tail;
				float tailU = renderData.tailU;
				hSide = vSide;

				draw(context, vertexBuffer, leftC + hSide, y, tail, scaledBrd, leftU, sMinV, tailU, topV);
				draw(context, vertexBuffer, leftC + hSide, bottomC, tail, scaledBrd, leftU, bottomV, tailU, sMaxV);
			}

			draw(context, vertexBuffer, leftC, y, hSide, scaledBrd, leftU, sMinV, rightU, topV);
			draw(context, vertexBuffer, leftC, bottomC, hSide, scaledBrd, leftU, bottomV, rightU, sMaxV);
		} else {
			double left = leftC;
			int segments = renderData.hSegments;
			for (int i = 0; i < segments; i++) {
				draw(context, vertexBuffer, left, y, hSide, scaledBrd, leftU, sMinV, rightU, topV);
				draw(context, vertexBuffer, left, bottomC, hSide, scaledBrd, leftU, bottomV, rightU, sMaxV);
				left += hSide;
			}
			double top = topC;
			segments = renderData.vSegments;
			for (int i = 0; i < segments; i++) {
				draw(context, vertexBuffer, x, top, scaledBrd, vSide, sMinU, topV, leftU, bottomV);
				draw(context, vertexBuffer, rightC, top, scaledBrd, vSide, rightU, topV, sMaxU, bottomV);
				top += vSide;
			}

			float hTail = renderData.hTail;
			float vTail = renderData.vTail;
			float hTailU = renderData.hTailU;
			float vTailV = renderData.vTailV;

			draw(context, vertexBuffer, left, y, hTail, scaledBrd, leftU, sMinV, hTailU, topV);
			draw(context, vertexBuffer, left, bottomC, hTail, scaledBrd, leftU, bottomV, hTailU, sMaxV);
			draw(context, vertexBuffer, x, top, scaledBrd, vTail, sMinU, topV, leftU, vTailV);
			draw(context, vertexBuffer, rightC, top, scaledBrd, vTail, rightU, topV, sMaxU, vTailV);
		}

		endDraw();
	}

	public static void drawImage(DrawContext context, Image image, double x, double y, float w, float h) {
		RenderSystem.setShader(GameRenderer::getPositionTexProgram);
		image.bindTexture();
		startDrawNormal();
		draw(context, vertexBuffer, x, y, w, h, 0.0F, 0.0F, 1.0F, 1.0F);
		endDraw();
	}

	private static void draw(DrawContext context, VertexConsumer vertexConsumer, double x, double y, float w, float h, float minU, float minV, float maxU, float maxV) {
		RenderSystem.enableBlend();
		RenderSystem.enableCull();

		MatrixStack matrixStack = context.getMatrices();
		matrixStack.push();
		matrixStack.translate(x, y, 0);

		Matrix4f m4f = matrixStack.peek().getPositionMatrix();
		MatrixStack.Entry mse = matrixStack.peek();

		addVertices(m4f, mse, vertexConsumer, w, h, minU, minV, maxU, maxV);

		matrixStack.pop();
	}

	private static void draw(DrawContext context, double x, double y, float w, float h, float minU, float minV, float maxU, float maxV) {
		draw(context, vertexBuffer, x, y, w, h, minU, minV, maxU, maxV);
	}

	private static void addVertices(Matrix4f m4f, MatrixStack.Entry mse, VertexConsumer vertexConsumer, float w, float h, float minU, float minV, float maxU, float maxV) {
		addVertices(m4f, mse, vertexConsumer, 0, w, 0, h, minU, minV, maxU, maxV);
	}

	private static void addVertices(Matrix4f m4f, MatrixStack.Entry mse, VertexConsumer vertexConsumer, float minX, float maxX, float minY, float maxY, float minU, float minV, float maxU, float maxV) {
		vertex(m4f, mse, vertexConsumer, minX, minY, 1.0F, minU, minV);
		vertex(m4f, mse, vertexConsumer, minX, maxY, 1.0F, minU, maxV);
		vertex(m4f, mse, vertexConsumer, maxX, maxY, 1.0F, maxU, maxV);
		vertex(m4f, mse, vertexConsumer, maxX, minY, 1.0F, maxU, minV);
	}

	public static void addQuad(double x, double y, double w, double h) {
		addQuad(x, y, w, h, 0.0F, 0.0F, 1.0F, 1.0F);
	}

	public static void addQuad(double x, double y, double w, double h, float minU, float minV, float maxU, float maxV) {
		vertex((float) x, (float) (y + h), 0.0f, minU, maxV);
		vertex((float) (x + w), (float) (y + h), 0.0f, maxU, maxV);
		vertex((float) (x + w), (float) y, 0.0f, maxU, minV);
		vertex((float) x, (float) y, 0.0f, minU, minV);
	}

	public static void addQuad(MatrixStack matrices, double x, double y, double w, double h, float minU, float minV, float maxU, float maxV) {
		Matrix4f m4f = matrices.peek().getPositionMatrix();
		MatrixStack.Entry mse = matrices.peek();

		vertex(m4f, mse, vertexBuffer, (float) x, (float) (y + h), 1.0F, minU, maxV);
		vertex(m4f, mse, vertexBuffer, (float) (x + w), (float) (y + h), 1.0F, maxU, maxV);
		vertex(m4f, mse, vertexBuffer, (float) (x + w), (float) y, 1.0F, maxU, minV);
		vertex(m4f, mse, vertexBuffer, (float) x, (float) y, 1.0F, minU, minV);
	}

	private static void vertex(Matrix4f m4f, MatrixStack.Entry mse, VertexConsumer vertexConsumer, float x, float y, float z, float u, float v) {
		vertexConsumer.vertex(m4f, x, y, z).texture(u, v).normal(mse, 0.0F, 1.0F, 0.0F);
	}

	private static void vertex(double x, double y, double z, float u, float v) {
		vertexBuffer.vertex((float) x, (float) y, (float) z).texture(u, v);
	}
}
