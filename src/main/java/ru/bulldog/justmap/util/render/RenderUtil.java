package ru.bulldog.justmap.util.render;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import ru.bulldog.justmap.map.minimap.skin.MapSkin;
import ru.bulldog.justmap.map.minimap.skin.MapSkin.RenderData;
import ru.bulldog.justmap.util.colors.ColorUtil;

/**
 * Drawing primitives for the map, on top of the 26.2 GUI render-state pipeline.
 *
 * <p>Everything is submitted through {@link GuiGraphicsExtractor}: the GUI is collected into
 * render states and drawn later, so nothing here may touch GL state directly. Shapes vanilla
 * has no state for (triangles, circles, sub-pixel textured quads) go through {@link GuiQuad}
 * and {@link GuiTexturedQuad}.
 */
public class RenderUtil {

	private RenderUtil() {}

	/** Segments used to approximate a circle. */
	private static final int CIRCLE_SIDES = 50;

	private final static Font textRenderer = Minecraft.getInstance().font;

	public static int getWidth(Component text) {
		return textRenderer.width(text);
	}

	public static int getWidth(String string) {
		return textRenderer.width(string);
	}

	public static void drawCenteredString(GuiGraphicsExtractor context, String string, double x, double y, int color) {
		context.text(textRenderer, string, (int) (x - textRenderer.width(string) / 2), (int) y, color);
	}

	public static void drawCenteredText(GuiGraphicsExtractor context, Component text, double x, double y, int color) {
		context.text(textRenderer, text, (int) (x - textRenderer.width(text) / 2), (int) y, color);
	}

	public static void drawBoundedString(GuiGraphicsExtractor context, String string, int x, int y, int leftBound, int rightBound, int color) {
		if (string == null) return;

		int stringWidth = textRenderer.width(string);
		int drawX = x - stringWidth / 2;
		if (drawX < leftBound) {
			drawX = leftBound;
		} else if (drawX + stringWidth > rightBound) {
			drawX = rightBound - stringWidth;
		}

		context.text(textRenderer, string, drawX, y, color);
	}

	public static void drawRightAlignedString(GuiGraphicsExtractor context, String string, int x, int y, int color) {
		context.text(textRenderer, string, x - textRenderer.width(string), y, color);
	}

	// -- solid shapes ------------------------------------------------------------------

	public static void fill(GuiGraphicsExtractor context, double x, double y, double w, double h, int color) {
		quad(context,
			 (float) x, (float) y,
			 (float) x, (float) (y + h),
			 (float) (x + w), (float) (y + h),
			 (float) (x + w), (float) y,
			 color);
	}

	public static void drawTriangle(GuiGraphicsExtractor context, double x1, double y1, double x2, double y2, double x3, double y3, int color) {
		// The GUI batches quads, so a triangle repeats its last corner.
		quad(context, (float) x1, (float) y1, (float) x2, (float) y2, (float) x3, (float) y3, (float) x3, (float) y3, color);
	}

	public static void drawDiamond(GuiGraphicsExtractor context, double x, double y, int width, int height, int color) {
		quad(context,
			 (float) x, (float) (y + height / 2.0),
			 (float) (x + width / 2.0), (float) (y + height),
			 (float) (x + width), (float) (y + height / 2.0),
			 (float) (x + width / 2.0), (float) y,
			 color);
	}

	public static void drawLine(GuiGraphicsExtractor context, double x1, double y1, double x2, double y2, int color) {
		double dx = x2 - x1;
		double dy = y2 - y1;
		double length = Math.sqrt(dx * dx + dy * dy);
		if (length == 0.0) return;

		// half a pixel either side of the ideal line
		double nx = -dy / length * 0.5;
		double ny = dx / length * 0.5;

		quad(context,
			 (float) (x1 + nx), (float) (y1 + ny),
			 (float) (x2 + nx), (float) (y2 + ny),
			 (float) (x2 - nx), (float) (y2 - ny),
			 (float) (x1 - nx), (float) (y1 - ny),
			 color);
	}

	public static void drawOutlineCircle(GuiGraphicsExtractor context, double x, double y, double radius, double outline, int color) {
		int darken = ColorUtil.colorBrigtness(color, -3);
		drawCircle(context, x, y, radius + outline, darken);
		drawCircle(context, x, y, radius, color);
	}

	public static void drawCircle(GuiGraphicsExtractor context, double x, double y, double radius, int color) {
		double step = Math.PI * 2 / CIRCLE_SIDES;
		double angle = Math.toRadians(180);
		double px = x + Math.sin(angle) * radius;
		double py = y + Math.cos(angle) * radius;

		for (int i = 1; i <= CIRCLE_SIDES; i++) {
			double next = angle + step * i;
			double nx = x + Math.sin(next) * radius;
			double ny = y + Math.cos(next) * radius;

			// fan segment as a degenerate quad
			quad(context, (float) x, (float) y, (float) px, (float) py, (float) nx, (float) ny, (float) nx, (float) ny, color);

			px = nx;
			py = ny;
		}
	}

	/** Submits one solid quad in the current pose. Corners must wind consistently. */
	public static void quad(GuiGraphicsExtractor context,
							float x0, float y0, float x1, float y1,
							float x2, float y2, float x3, float y3, int color) {

		context.guiRenderState.addGuiElement(new GuiQuad(
				RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(context.pose()),
				x0, y0, x1, y1, x2, y2, x3, y3, color, context.scissorStack.peek()));
	}

	// -- textures ----------------------------------------------------------------------

	private static AbstractTexture texture(Identifier id) {
		return Minecraft.getInstance().getTextureManager().getTexture(id);
	}

	/** Submits one textured quad in the current pose, with sub-pixel accurate corners. */
	public static void texturedQuad(GuiGraphicsExtractor context, GpuTextureView view, GpuSampler sampler,
									double x, double y, double w, double h,
									float minU, float minV, float maxU, float maxV) {

		texturedQuad(context, view, sampler, x, y, w, h, minU, minV, maxU, maxV, -1);
	}

	/** As above, multiplying the sampled texture by {@code tint} (an ARGB colour). */
	public static void texturedQuad(GuiGraphicsExtractor context, GpuTextureView view, GpuSampler sampler,
									double x, double y, double w, double h,
									float minU, float minV, float maxU, float maxV, int tint) {

		float x0 = (float) x;
		float y0 = (float) y;
		float x1 = (float) (x + w);
		float y1 = (float) (y + h);

		context.guiRenderState.addGuiElement(new GuiTexturedQuad(
				RenderPipelines.GUI_TEXTURED, TextureSetup.singleTexture(view, sampler), new Matrix3x2f(context.pose()),
				x0, y0, minU, minV,
				x0, y1, minU, maxV,
				x1, y1, maxU, maxV,
				x1, y0, maxU, minV,
				tint, context.scissorStack.peek()));
	}

	public static void drawTexture(GuiGraphicsExtractor context, Identifier id,
								   double x, double y, double w, double h,
								   float minU, float minV, float maxU, float maxV) {

		drawTexture(context, id, x, y, w, h, minU, minV, maxU, maxV, -1);
	}

	public static void drawTexture(GuiGraphicsExtractor context, Identifier id,
								   double x, double y, double w, double h,
								   float minU, float minV, float maxU, float maxV, int tint) {

		AbstractTexture texture = texture(id);
		if (texture == null) return;
		texturedQuad(context, texture.getTextureView(), texture.getSampler(), x, y, w, h, minU, minV, maxU, maxV, tint);
	}

	public static void drawTexture(GuiGraphicsExtractor context, Identifier id, double x, double y, double w, double h) {
		drawTexture(context, id, x, y, w, h, 0.0F, 0.0F, 1.0F, 1.0F);
	}

	public static void drawTexture(GuiGraphicsExtractor context, Identifier id, double x, double y, double w, double h, int tint) {
		drawTexture(context, id, x, y, w, h, 0.0F, 0.0F, 1.0F, 1.0F, tint);
	}

	public static void drawImage(GuiGraphicsExtractor context, Image image, double x, double y, float w, float h) {
		drawTexture(context, image.getId(), x, y, w, h);
	}

	/** Draws the head and hat layers of a player skin as a square icon. */
	public static void drawPlayerHead(GuiGraphicsExtractor context, Identifier skin, double x, double y, int w, int h, int tint) {
		drawTexture(context, skin, x, y, w, h, 0.125F, 0.125F, 0.25F, 0.25F, tint);
		drawTexture(context, skin, x, y, w, h, 0.625F, 0.125F, 0.75F, 0.25F, tint);
	}

	public static void drawSkin(GuiGraphicsExtractor context, MapSkin skin, double x, double y, float w, float h) {
		RenderData renderData = skin.getRenderData();

		if (renderData.scaleChanged || renderData.x != x || renderData.y != y ||
			renderData.width != w || renderData.height != h) {

			renderData.calculate(x, y, w, h);
		}

		AbstractTexture texture = texture(skin.getId());
		if (texture == null) return;

		GpuTextureView view = texture.getTextureView();
		GpuSampler sampler = texture.getSampler();

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

		texturedQuad(context, view, sampler, x, y, scaledBrd, scaledBrd, sMinU, sMinV, leftU, topV);
		texturedQuad(context, view, sampler, rightC, y, scaledBrd, scaledBrd, rightU, sMinV, sMaxU, topV);
		texturedQuad(context, view, sampler, x, bottomC, scaledBrd, scaledBrd, sMinU, bottomV, leftU, sMaxV);
		texturedQuad(context, view, sampler, rightC, bottomC, scaledBrd, scaledBrd, rightU, bottomV, sMaxU, sMaxV);

		if (skin.resizable) {
			texturedQuad(context, view, sampler, rightC, topC, scaledBrd, vSide, rightU, topV, sMaxU, bottomV);
			texturedQuad(context, view, sampler, x, topC, scaledBrd, vSide, sMinU, topV, leftU, bottomV);
			texturedQuad(context, view, sampler, leftC, topC, hSide, vSide, leftU, topV, rightU, bottomV);
			if (skin.repeating) {
				float tail = renderData.tail;
				float tailU = renderData.tailU;
				hSide = vSide;

				texturedQuad(context, view, sampler, leftC + hSide, y, tail, scaledBrd, leftU, sMinV, tailU, topV);
				texturedQuad(context, view, sampler, leftC + hSide, bottomC, tail, scaledBrd, leftU, bottomV, tailU, sMaxV);
			}

			texturedQuad(context, view, sampler, leftC, y, hSide, scaledBrd, leftU, sMinV, rightU, topV);
			texturedQuad(context, view, sampler, leftC, bottomC, hSide, scaledBrd, leftU, bottomV, rightU, sMaxV);
		} else {
			double left = leftC;
			int segments = renderData.hSegments;
			for (int i = 0; i < segments; i++) {
				texturedQuad(context, view, sampler, left, y, hSide, scaledBrd, leftU, sMinV, rightU, topV);
				texturedQuad(context, view, sampler, left, bottomC, hSide, scaledBrd, leftU, bottomV, rightU, sMaxV);
				left += hSide;
			}
			double top = topC;
			segments = renderData.vSegments;
			for (int i = 0; i < segments; i++) {
				texturedQuad(context, view, sampler, x, top, scaledBrd, vSide, sMinU, topV, leftU, bottomV);
				texturedQuad(context, view, sampler, rightC, top, scaledBrd, vSide, rightU, topV, sMaxU, bottomV);
				top += vSide;
			}

			float hTail = renderData.hTail;
			float vTail = renderData.vTail;
			float hTailU = renderData.hTailU;
			float vTailV = renderData.vTailV;

			texturedQuad(context, view, sampler, left, y, hTail, scaledBrd, leftU, sMinV, hTailU, topV);
			texturedQuad(context, view, sampler, left, bottomC, hTail, scaledBrd, leftU, bottomV, hTailU, sMaxV);
			texturedQuad(context, view, sampler, x, top, scaledBrd, vTail, sMinU, topV, leftU, vTailV);
			texturedQuad(context, view, sampler, rightC, top, scaledBrd, vTail, rightU, topV, sMaxU, vTailV);
		}
	}
}
