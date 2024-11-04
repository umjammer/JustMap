package ru.bulldog.justmap.client.render;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.map.data.MapDataProvider;
import ru.bulldog.justmap.map.waypoint.Waypoint;
import ru.bulldog.justmap.map.waypoint.Waypoint.Icon;
import ru.bulldog.justmap.map.waypoint.WaypointKeeper;
import ru.bulldog.justmap.util.colors.ColorUtil;
import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.math.MathUtil;
import ru.bulldog.justmap.util.render.RenderUtil;

@Environment(EnvType.CLIENT)
public class WaypointRenderer {
	private static final WaypointRenderer renderer = new WaypointRenderer();
	private final static Identifier BEAM_TEX = Identifier.of("textures/entity/beacon_beam.png");
	private final static MinecraftClient minecraft = MinecraftClient.getInstance();

	public static void renderHUD(DrawContext context, float delta, float fov) {
		if (!ClientSettings.showWaypoints || !ClientSettings.waypointsTracking) return;
		if (minecraft.world == null || minecraft.player == null || minecraft.currentScreen != null) {
			return;
		}

		List<Waypoint> wayPoints = WaypointKeeper.getInstance().getWaypoints(MapDataProvider.getMultiworldManager().getCurrentWorldKey(), true);
		for (Waypoint wp : wayPoints) {
			int dist = (int) MathUtil.getDistance(wp.pos, minecraft.player.getBlockPos(), false);
			if (wp.tracking && dist <= wp.showRange) {
				renderer.renderHUD(context, wp, delta, fov, dist);
			}
		}
	}

    public static void startWaypointRender() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
			RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
			RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
			RenderSystem.depthMask(false);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			GL11.glEnable(GL11.GL_LINE_SMOOTH);

			MatrixStack matrixStack = context.matrixStack();

			matrixStack.push();
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

			Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
			float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false); // TODO 1.21

			renderWaypoints(matrixStack, camera, tickDelta);

			buffer.endNullable();
			matrixStack.pop();
			RenderSystem.disableBlend();
		});
    }

    private void renderHUD(DrawContext context, Waypoint waypoint, float delta, float fov, int dist) {
		int wpX = waypoint.pos.getX();
		int wpZ = waypoint.pos.getZ();

		Icon icon = waypoint.getIcon();

		int size = icon != null ? icon.getWidth() : 18;
		int screenWidth = minecraft.getWindow().getScaledWidth();

		double dx = minecraft.player.getX() - wpX;
		double dy = wpZ - minecraft.player.getZ();
		double wfi = correctAngle((float) (Math.atan2(dx, dy) * (180 / Math.PI)));
		double pfi = correctAngle(minecraft.player.getYaw(delta) % 360);
		double a0 = pfi - fov / 2;
		double a1 = pfi + fov / 2;
		double ax = correctAngle((float) (2 * pfi - wfi));
		double scale = (MathUtil.clamp(ax, a0, a1) - a0) / fov;

		int x = (int) Math.round(MathUtil.clamp((screenWidth - screenWidth * scale) - size / 2, 0, screenWidth - size));
		int y = ClientSettings.positionOffset;

		if (icon != null) {
			icon.draw(context, x, y);
		} else {
			RenderUtil.drawDiamond(x, y, size, size, waypoint.color);
		}
		RenderUtil.drawBoundedString(context, dist + "m", x + size / 2, y + size + 2, 0, screenWidth, Colors.WHITE);
	}

	public static void renderWaypoints(MatrixStack matrixStack, Camera camera, float tickDelta) {
		if (minecraft == null) return;
		if (!ClientSettings.showWaypoints || !ClientSettings.waypointsWorldRender) return;

		long time = minecraft.player.getWorld().getTime();
		float tick = (float) Math.floorMod(time, 125L) + tickDelta;

		BlockPos playerPos = minecraft.player.getBlockPos();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false);

		VertexConsumerProvider.Immediate consumerProvider = minecraft.getBufferBuilders().getEntityVertexConsumers();
		List<Waypoint> wayPoints = WaypointKeeper.getInstance().getWaypoints(MapDataProvider.getMultiworldManager().getCurrentWorldKey(), true);
		for (Waypoint wp : wayPoints) {
			int dist = (int) MathUtil.getDistance(wp.pos, playerPos, false);
			if (wp.render && dist >= ClientSettings.minRenderDist && dist <= wp.showRange) {
				renderer.renderWaypoint(matrixStack, consumerProvider, wp, camera, tick, dist);
			}
		}
		consumerProvider.draw();

		RenderSystem.depthMask(true);
	}

	private void renderWaypoint(MatrixStack matrixStack, VertexConsumerProvider consumerProvider, Waypoint waypoint, Camera camera, float tick, int dist) {
		int wpX = waypoint.pos.getX();
		int wpY = waypoint.pos.getY();
		int wpZ = waypoint.pos.getZ();

		Vec3d vec3d = camera.getPos();

		double camX = vec3d.getX();
		double camY = vec3d.getY();
		double camZ = vec3d.getZ();

		float[] colors = ColorUtil.toFloatArray(waypoint.color);
		float alpha = MathUtil.clamp(0.125F * ((float) dist / 10), 0.11F, 0.275F);

		matrixStack.push();
		matrixStack.translate((double) wpX - camX, (double) wpY - camY, (double) wpZ - camZ);
		matrixStack.translate(0.5, 0.5, 0.5);
		if (ClientSettings.renderLightBeam) {
			VertexConsumer vertexConsumer = consumerProvider.getBuffer(RenderLayer.getBeaconBeam(BEAM_TEX, true));
			this.renderLightBeam(matrixStack, vertexConsumer, tick, -wpY, 1024 - wpY, colors, alpha, 0.15F, 0.2F);
		}
		if (ClientSettings.renderMarkers) {
			matrixStack.push();
			matrixStack.translate(0.0, 1.0, 0.0);
			if (ClientSettings.renderAnimation) {
				double swing = 0.25 * Math.sin((tick * 2.25 - 45.0) / 15.0);
				matrixStack.translate(0.0, swing, 0.0);
			}
			matrixStack.multiply(camera.getRotation());
			matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
			matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F));

			alpha = MathUtil.clamp(alpha * 3, 0.0F, 1.0F);

			Identifier texture = waypoint.getIcon().getTexture();
			VertexConsumer vertexConsumer = consumerProvider.getBuffer(RenderLayer.getBeaconBeam(texture, true));
			this.renderIcon(matrixStack, vertexConsumer, colors, alpha);
			matrixStack.pop();
		}
		matrixStack.pop();
	}

	private void renderIcon(MatrixStack matrixStack, VertexConsumer vertexConsumer, float[] colors, float alpha) {
		MatrixStack.Entry entry = matrixStack.peek();
		Matrix4f matrix4f = entry.getPositionMatrix();

		this.addVertex(matrix4f, entry, vertexConsumer, colors[0], colors[1], colors[2], alpha, -0.5F, -0.5F, 0.0F, 0.0F, 0.0F);
		this.addVertex(matrix4f, entry, vertexConsumer, colors[0], colors[1], colors[2], alpha, -0.5F, 0.5F, 0.0F, 0.0F, 1.0F);
		this.addVertex(matrix4f, entry, vertexConsumer, colors[0], colors[1], colors[2], alpha, 0.5F, 0.5F, 0.0F, 1.0F, 1.0F);
		this.addVertex(matrix4f, entry, vertexConsumer, colors[0], colors[1], colors[2], alpha, 0.5F, -0.5F, 0.0F, 1.0F, 0.0F);
	}

	private void renderLightBeam(MatrixStack matrixStack, VertexConsumer vertexConsumer, float tick, int i, int j, float[] colors, float alpha, float h, float k) {
		int m = i + j;

		float o = j < 0 ? tick : -tick;
		float p = MathHelper.fractionalPart(o * 0.2F - (float) MathHelper.floor(o * 0.1F));
		float red = colors[0];
		float green = colors[1];
		float blue = colors[2];

		matrixStack.push();
		matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(tick * 2.25F - 45.0F));
		float af;
		float ai;
		float aj = -h;
		float aa = -h;
		float ap = -1.0F + p;
		float aq = (float) j * (0.5F / h) + ap;

		this.renderBeam(matrixStack, vertexConsumer, red, green, blue, alpha, i, m, 0.0F, h, h, 0.0F, aj, 0.0F, 0.0F, aa, 0.0F, 1.0F, aq, ap);
		matrixStack.pop();

		af = -k;
		float ag = -k;
		ai = -k;
		aj = -k;
		ap = -1.0F + p;
		aq = (float) j + ap;
		this.renderBeam(matrixStack, vertexConsumer, red, green, blue, alpha, i, m, af, ag, k, ai, aj, k, k, k, 0.0F, 1.0F, aq, ap);
	}

	private void renderBeam(MatrixStack matrixStack, VertexConsumer vertexConsumer, float red, float green, float blue, float alpha, int j, int k, float l, float m, float n, float o, float p, float q, float r, float s, float t, float u, float v, float w) {
		MatrixStack.Entry entry = matrixStack.peek();
		Matrix4f matrix4f = entry.getPositionMatrix();
		this.renderBeam(matrix4f, entry, vertexConsumer, red, green, blue, alpha, j, k, l, m, n, o, t, u, v, w);
		this.renderBeam(matrix4f, entry, vertexConsumer, red, green, blue, alpha, j, k, r, s, p, q, t, u, v, w);
		this.renderBeam(matrix4f, entry, vertexConsumer, red, green, blue, alpha, j, k, n, o, r, s, t, u, v, w);
		this.renderBeam(matrix4f, entry, vertexConsumer, red, green, blue, alpha, j, k, p, q, l, m, t, u, v, w);
	}

	private void renderBeam(Matrix4f matrix4f, MatrixStack.Entry matrixSE, VertexConsumer vertexConsumer, float red, float green, float blue, float alpha, int j, int k, float l, float m, float n, float o, float p, float q, float r, float s) {
		this.addVertex(matrix4f, matrixSE, vertexConsumer, red, green, blue, alpha, k, l, m, q, r);
		this.addVertex(matrix4f, matrixSE, vertexConsumer, red, green, blue, alpha, j, l, m, q, s);
		this.addVertex(matrix4f, matrixSE, vertexConsumer, red, green, blue, alpha, j, n, o, p, s);
		this.addVertex(matrix4f, matrixSE, vertexConsumer, red, green, blue, alpha, k, n, o, p, r);
	}

	private void addVertex(Matrix4f matrix4f, MatrixStack.Entry matrixSE, VertexConsumer vertexConsumer, float red, float green, float blue, float alpha, float y, float x, float l, float m, float n) {
		vertexConsumer.vertex(matrix4f, x, y, l).color(red, green, blue, alpha).texture(m, n).overlay(OverlayTexture.DEFAULT_UV).light(Colors.LIGHT).normal(matrixSE, 0.0F, 1.0F, 0.0F);
	}

	private double correctAngle(float angle) {
		return angle < 0 ? angle + 360.0D : angle >= 360.0D ? angle - 360.0D : angle;
	}
}
