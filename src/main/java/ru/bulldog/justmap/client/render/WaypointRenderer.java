package ru.bulldog.justmap.client.render;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.map.data.MapDataProvider;
import ru.bulldog.justmap.map.waypoint.Waypoint;
import ru.bulldog.justmap.map.waypoint.Waypoint.Icon;
import ru.bulldog.justmap.map.waypoint.WaypointKeeper;
import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.math.MathUtil;
import ru.bulldog.justmap.util.render.RenderUtil;

/**
 * Draws waypoint beams and markers in the world, and their direction markers on the HUD.
 *
 * <p>26.2 collects world geometry into submit nodes instead of letting a mod push vertices
 * during the render pass, so the beams are handed to the collector Fabric passes to
 * {@code LevelRenderEvents.COLLECT_SUBMITS}, and colours travel as packed ARGB rather than
 * as a shader colour.
 */
@Environment(EnvType.CLIENT)
public class WaypointRenderer {
	private static final WaypointRenderer renderer = new WaypointRenderer();
	private final static Identifier BEAM_TEX = Identifier.parse("textures/entity/beacon_beam.png");
	private final static Minecraft minecraft = Minecraft.getInstance();

	public static void renderHUD(GuiGraphicsExtractor context, float delta, float fov) {
		if (!ClientSettings.showWaypoints || !ClientSettings.waypointsTracking) return;
		if (minecraft.level == null || minecraft.player == null || minecraft.gui.screen() != null) {
			return;
		}

		List<Waypoint> wayPoints = WaypointKeeper.getInstance().getWaypoints(MapDataProvider.getMultiworldManager().getCurrentWorldKey(), true);
		for (Waypoint wp : wayPoints) {
			int dist = (int) MathUtil.getDistance(wp.pos, minecraft.player.blockPosition(), false);
			if (wp.tracking && dist <= wp.showRange) {
				renderer.renderHUD(context, wp, delta, fov, dist);
			}
		}
	}

	public static void startWaypointRender() {
		LevelRenderEvents.COLLECT_SUBMITS.register(WaypointRenderer::collectWaypoints);
	}

	private static void collectWaypoints(LevelRenderContext context) {
		if (minecraft.player == null) return;
		if (!ClientSettings.showWaypoints || !ClientSettings.waypointsWorldRender) return;

		float tickDelta = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		long time = context.levelState().gameTime;
		float tick = (float) Math.floorMod(time, 125L) + tickDelta;

		BlockPos playerPos = minecraft.player.blockPosition();

		List<Waypoint> wayPoints = WaypointKeeper.getInstance().getWaypoints(MapDataProvider.getMultiworldManager().getCurrentWorldKey(), true);
		for (Waypoint wp : wayPoints) {
			int dist = (int) MathUtil.getDistance(wp.pos, playerPos, false);
			if (wp.render && dist >= ClientSettings.minRenderDist && dist <= wp.showRange) {
				renderer.submitWaypoint(context.poseStack(), context.submitNodeCollector(), wp, context.levelState().cameraRenderState, tick, dist);
			}
		}
	}

	private void renderHUD(GuiGraphicsExtractor context, Waypoint waypoint, float delta, float fov, int dist) {
		int wpX = waypoint.pos.getX();
		int wpZ = waypoint.pos.getZ();

		Icon icon = waypoint.getIcon();

		int size = icon != null ? icon.getWidth() : 18;
		int screenWidth = minecraft.getWindow().getGuiScaledWidth();

		double dx = minecraft.player.getX() - wpX;
		double dy = wpZ - minecraft.player.getZ();
		double wfi = correctAngle((float) (Math.atan2(dx, dy) * (180 / Math.PI)));
		double pfi = correctAngle(minecraft.player.getViewYRot(delta) % 360);
		double a0 = pfi - fov / 2;
		double a1 = pfi + fov / 2;
		double ax = correctAngle((float) (2 * pfi - wfi));
		double scale = (MathUtil.clamp(ax, a0, a1) - a0) / fov;

		int x = (int) Math.round(MathUtil.clamp((screenWidth - screenWidth * scale) - size / 2, 0, screenWidth - size));
		int y = ClientSettings.positionOffset;

		if (icon != null) {
			icon.draw(context, x, y);
		} else {
			RenderUtil.drawDiamond(context, x, y, size, size, waypoint.color);
		}
		RenderUtil.drawBoundedString(context, dist + "m", x + size / 2, y + size + 2, 0, screenWidth, Colors.WHITE);
	}

	private void submitWaypoint(PoseStack poseStack, SubmitNodeCollector collector, Waypoint waypoint, CameraRenderState camera, float tick, int dist) {
		int wpX = waypoint.pos.getX();
		int wpY = waypoint.pos.getY();
		int wpZ = waypoint.pos.getZ();

		Vec3 cameraPos = camera.pos;

		float alpha = MathUtil.clamp(0.125F * ((float) dist / 10), 0.11F, 0.275F);
		int beamColor = ARGB.color((int) (alpha * 255), ARGB.red(waypoint.color), ARGB.green(waypoint.color), ARGB.blue(waypoint.color));

		poseStack.pushPose();
		poseStack.translate(wpX - cameraPos.x(), wpY - cameraPos.y(), wpZ - cameraPos.z());
		poseStack.translate(0.5, 0.5, 0.5);
		if (ClientSettings.renderLightBeam) {
			this.submitLightBeam(poseStack, collector, tick, -wpY, 1024 - wpY, beamColor, 0.15F, 0.2F);
		}
		if (ClientSettings.renderMarkers) {
			poseStack.pushPose();
			poseStack.translate(0.0, 1.0, 0.0);
			if (ClientSettings.renderAnimation) {
				double swing = 0.25 * Math.sin((tick * 2.25 - 45.0) / 15.0);
				poseStack.translate(0.0, swing, 0.0);
			}
			poseStack.mulPose(camera.orientation);
			poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));

			float markerAlpha = MathUtil.clamp(alpha * 3, 0.0F, 1.0F);
			int markerColor = ARGB.color((int) (markerAlpha * 255), ARGB.red(waypoint.color), ARGB.green(waypoint.color), ARGB.blue(waypoint.color));

			Identifier texture = waypoint.getIcon().getTexture();
			collector.submitCustomGeometry(poseStack, RenderTypes.beaconBeam(texture, true),
					(pose, buffer) -> this.renderIcon(pose, buffer, markerColor));
			poseStack.popPose();
		}
		poseStack.popPose();
	}

	private void renderIcon(PoseStack.Pose pose, VertexConsumer vertexConsumer, int color) {
		this.addVertex(pose, vertexConsumer, color, -0.5F, -0.5F, 0.0F, 0.0F, 0.0F);
		this.addVertex(pose, vertexConsumer, color, -0.5F, 0.5F, 0.0F, 0.0F, 1.0F);
		this.addVertex(pose, vertexConsumer, color, 0.5F, 0.5F, 0.0F, 1.0F, 1.0F);
		this.addVertex(pose, vertexConsumer, color, 0.5F, -0.5F, 0.0F, 1.0F, 0.0F);
	}

	private void submitLightBeam(PoseStack poseStack, SubmitNodeCollector collector, float tick, int i, int j, int color, float h, float k) {
		int m = i + j;

		float o = j < 0 ? tick : -tick;
		float p = Mth.frac(o * 0.2F - (float) Mth.floor(o * 0.1F));

		poseStack.pushPose();
		poseStack.mulPose(Axis.YP.rotationDegrees(tick * 2.25F - 45.0F));
		float aj = -h;
		float aa = -h;
		float ap = -1.0F + p;
		float aq = (float) j * (0.5F / h) + ap;

		final int beamStart = i, beamEnd = m;
		final float f0 = h, f1 = aj, f2 = aa, v1 = aq, v2 = ap;
		collector.submitCustomGeometry(poseStack, RenderTypes.beaconBeam(BEAM_TEX, false),
				(pose, buffer) -> this.renderPart(pose, buffer, color, beamStart, beamEnd,
						0.0F, f0, f0, 0.0F, f1, 0.0F, 0.0F, f2, 0.0F, 1.0F, v1, v2));
		poseStack.popPose();

		float af = -k;
		float ag = -k;
		float ai = -k;
		float ak = -k;
		ap = -1.0F + p;
		aq = (float) j + ap;

		final float g0 = af, g1 = ag, g2 = k, g3 = ai, g4 = ak, gv1 = aq, gv2 = ap;
		collector.submitCustomGeometry(poseStack, RenderTypes.beaconBeam(BEAM_TEX, true),
				(pose, buffer) -> this.renderPart(pose, buffer, color, beamStart, beamEnd,
						g0, g1, g2, g3, g4, g2, g2, g2, 0.0F, 1.0F, gv1, gv2));
	}

	private void renderPart(PoseStack.Pose pose, VertexConsumer vertexConsumer, int color, int j, int k,
							float l, float m, float n, float o, float p, float q, float r, float s,
							float t, float u, float v, float w) {
		this.renderQuad(pose, vertexConsumer, color, j, k, l, m, n, o, t, u, v, w);
		this.renderQuad(pose, vertexConsumer, color, j, k, r, s, p, q, t, u, v, w);
		this.renderQuad(pose, vertexConsumer, color, j, k, n, o, r, s, t, u, v, w);
		this.renderQuad(pose, vertexConsumer, color, j, k, p, q, l, m, t, u, v, w);
	}

	private void renderQuad(PoseStack.Pose pose, VertexConsumer vertexConsumer, int color, int j, int k,
							float l, float m, float n, float o, float p, float q, float r, float s) {
		this.addVertex(pose, vertexConsumer, color, l, k, m, q, r);
		this.addVertex(pose, vertexConsumer, color, l, j, m, q, s);
		this.addVertex(pose, vertexConsumer, color, n, j, o, p, s);
		this.addVertex(pose, vertexConsumer, color, n, k, o, p, r);
	}

	private void addVertex(PoseStack.Pose pose, VertexConsumer vertexConsumer, int color,
						   float x, float y, float z, float u, float v) {
		vertexConsumer.addVertex(pose, x, y, z)
				.setColor(color)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(Colors.LIGHT)
				.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}

	private double correctAngle(float angle) {
		return angle < 0 ? angle + 360.0D : angle >= 360.0D ? angle - 360.0D : angle;
	}
}
