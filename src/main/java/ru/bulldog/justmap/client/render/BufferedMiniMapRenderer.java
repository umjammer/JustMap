package ru.bulldog.justmap.client.render;

import java.util.List;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix4f;
import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.map.ChunkGrid;
import ru.bulldog.justmap.map.data.MapRegion;
import ru.bulldog.justmap.map.icon.MapIcon;
import ru.bulldog.justmap.map.icon.WaypointIcon;
import ru.bulldog.justmap.map.minimap.Minimap;
import ru.bulldog.justmap.util.render.ExtendedFramebuffer;
import ru.bulldog.justmap.util.render.GLC;
import ru.bulldog.justmap.util.render.RenderUtil;

public class BufferedMiniMapRenderer extends AbstractMiniMapRenderer {

	private Framebuffer primaryFramebuffer;
	private Framebuffer secondaryFramebuffer;
	private boolean triedFBO = false;
	private boolean loadedFBO = false;

	public BufferedMiniMapRenderer(Minimap map) {
		super(map);
	}

	public void loadFrameBuffers() {
		if (!ExtendedFramebuffer.canUseFramebuffer()) {
			JustMap.LOGGER.warning("FBO not supported! Using fast minimap render.");
		} else {
			double scale = minecraft.getWindow().getScaleFactor();
			int scaledW = (int) (imgW * scale);
			int scaledH = (int) (imgH * scale);
			this.primaryFramebuffer = new ExtendedFramebuffer(false);
			this.secondaryFramebuffer = new ExtendedFramebuffer(false);
			this.loadedFBO = (this.primaryFramebuffer.fbo != -1 && this.secondaryFramebuffer.fbo != -1);
		}
		this.triedFBO = true;
	}

	@Override
	protected void render(DrawContext context, double scale) {
		VertexConsumerProvider.Immediate consumerProvider = minecraft.getBufferBuilders().getEntityVertexConsumers();

		int scaledW = (int) (imgW * scale);
		int scaledH = (int) (imgH * scale);
		if (paramsUpdated) {
			this.resize(scaledW, scaledH);
			if (ClientSettings.showGrid) {
				if (chunkGrid == null) {
					this.chunkGrid = new ChunkGrid(lastX, lastZ, 0, 0, imgW, imgH, mapScale);
				} else {
					this.chunkGrid.updateRange(0, 0, imgW, imgH, mapScale);
					this.chunkGrid.updateGrid();
				}
			}
			this.paramsUpdated = false;
		}

		MatrixStack matrices = context.getMatrices();
		matrices.push();
		this.primaryFramebuffer.beginWrite(true);
		RenderSystem.clear(GLC.GL_COLOR_OR_DEPTH_BUFFER_BIT);
		RenderSystem.backupProjectionMatrix();
		Matrix4f orthographic = projectionMatrix(0.0F, scaledW, 0.0F, scaledH, 1000.0F, 3000.0F);
		RenderSystem.setProjectionMatrix(orthographic, ProjectionType.PERSPECTIVE);
		matrices.loadIdentity();
		matrices.translate(0.0F, 0.0F, -2000.0F);
		matrices.scale((float) scale, (float) scale, 1.0F);
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		this.drawMap(context);
		if (ClientSettings.showGrid) {
			this.drawGrid();
		}
		if (!mapRotation) {
			this.drawEntities(context, consumerProvider);
		}
		this.primaryFramebuffer.endWrite();
		matrices.pop();

		this.secondaryFramebuffer.beginWrite(false);
		RenderSystem.clear(GLC.GL_COLOR_OR_DEPTH_BUFFER_BIT);
		matrices.push();
		RenderSystem.enableCull();
		if (mapRotation) {
			float shiftX = scaledW / 2.0F;
			float shiftY = scaledH / 2.0F;
			matrices.translate(shiftX, shiftY, 0.0);
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F - rotation));
			matrices.translate(-shiftX, -shiftY, 0.0);
		}
		matrices.translate(-offX * scale, -offY * scale, 0.0);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
		this.primaryFramebuffer.beginRead();
		RenderUtil.startDraw();
		BufferBuilder buffer = RenderUtil.getBuffer();
		buffer.vertex(0.0f, scaledH, 0.0f).texture(0.0F, 0.0F);
		buffer.vertex(scaledW, scaledH, 0.0f).texture(1.0F, 0.0F);
		buffer.vertex(scaledW, 0.0f, 0.0f).texture(1.0F, 1.0F);
		buffer.vertex(0.0f, 0.0f, 0.0f).texture(0.0F, 1.0F);
		RenderUtil.endDraw();
		if (mapRotation) {
			matrices.push();
			matrices.scale((float) scale, (float) scale, 1.0F);
			this.drawEntities(context, consumerProvider);
			matrices.pop();
		}
		matrices.pop();
		this.secondaryFramebuffer.endWrite();
		RenderSystem.restoreProjectionMatrix();
		matrices.pop();

		Framebuffer minecraftFramebuffer = minecraft.getFramebuffer();
		int fbuffW = minecraftFramebuffer.viewportWidth;
		int fbuffH = minecraftFramebuffer.viewportHeight;
		int scissX = (int) (mapX * scale);
		int scissY = (int) (fbuffH - (mapY + mapHeight) * scale);
		int scissW = (int) (mapWidth * scale);
		int scissH = (int) (mapHeight * scale);
		RenderUtil.enableScissor();
		RenderUtil.applyScissor(scissX, scissY, scissW, scissH);
		minecraftFramebuffer.beginWrite(false);
		RenderSystem.viewport(0, 0, fbuffW, fbuffH);
		if (Minimap.isRound()) {
			RenderSystem.enableBlend();
			RenderSystem.colorMask(false, false, false, true);
			RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
			RenderSystem.clear(GLC.GL_COLOR_BUFFER_BIT);
			RenderSystem.colorMask(true, true, true, true);
			RenderUtil.bindTexture(roundMask);
			RenderUtil.drawQuad(mapX, mapY, mapWidth, mapHeight);
			RenderSystem.blendFunc(GLC.GL_DST_ALPHA, GLC.GL_ONE_MINUS_DST_ALPHA);
		}
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
		RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
		this.secondaryFramebuffer.beginRead();
		RenderUtil.startDraw();
		buffer = RenderUtil.getBuffer();
		buffer.vertex(imgX, imgY + imgH, 0.0f).texture(0.0F, 0.0F);
		buffer.vertex(imgX + imgW, imgY + imgH, 0.0f).texture(1.0F, 0.0F);
		buffer.vertex(imgX + imgW, imgY, 0.0f).texture(1.0F, 1.0F);
		buffer.vertex(imgX, imgY, 0.0f).texture(0.0F, 1.0F);
		RenderUtil.endDraw();
		matrices.pop();
		List<WaypointIcon> drawableWaypoints = minimap.getWaypoints(playerPos, centerX, centerY);
		for (WaypointIcon icon : drawableWaypoints) {
			icon.draw(context, consumerProvider, mapX, mapY, mapWidth, mapHeight, offX, offY, rotation);
		}
		consumerProvider.draw();
		RenderUtil.disableScissor();
	}

	// TODO aka net.minecraft.util.math.Matrix4f#projectionMatrix idk where is this in 1.19.3
	public static Matrix4f projectionMatrix(float left, float right, float bottom, float top, float nearPlane, float farPlane) {
		Matrix4f matrix4f = new Matrix4f();
		float f = right - left;
		float g = bottom - top;
		float h = farPlane - nearPlane;
		matrix4f.m00(2.0F / f);
		matrix4f.m11(2.0F / g);
		matrix4f.m22(-2.0F / h);
		matrix4f.m03(-(right + left) / f);
		matrix4f.m13(-(bottom + top) / g);
		matrix4f.m23(-(farPlane + nearPlane) / h);
		matrix4f.m33(1.0F);
		return matrix4f;
	}

	private void drawMap(DrawContext context) {
		int cornerX = lastX - scaledW / 2;
		int cornerZ = lastZ - scaledH / 2;

		int picX = 0;
		while (picX < scaledW) {
			int texW = 512;
			if (picX + texW > scaledW) texW = scaledW - picX;

			int picY = 0;
			int cX = cornerX + picX;
			while (picY < scaledH) {
				int texH = 512;
				if (picY + texH > scaledH) texH = scaledH - picY;

				int cZ = cornerZ + picY;
				MapRegion region = worldMapper.getMapRegion(minimap, cX, cZ);

				int texX = cX - (region.getPos().x << 9);
				int texY = cZ - (region.getPos().z << 9);
				if (texX + texW >= 512) texW = 512 - texX;
				if (texY + texH >= 512) texH = 512 - texY;

				double scX = (double) picX / mapScale;
				double scY = (double) picY / mapScale;
				double scW = (double) texW / mapScale;
				double scH = (double) texH / mapScale;

				region.drawLayer(context, minimap.getLayer(), minimap.getLevel(), scX, scY, scW, scH, texX, texY, texW, texH);

				picY += texH > 0 ? texH : 512;
			}

			picX += texW > 0 ? texW : 512;
		}
	}

	private void drawGrid() {
		if (playerMoved) {
			this.chunkGrid.updateCenter(lastX, lastZ);
			this.chunkGrid.updateGrid();
			this.playerMoved = false;
		}
		this.chunkGrid.draw();
	}

	private void drawEntities(DrawContext context, VertexConsumerProvider.Immediate consumerProvider) {
		float halfW = imgW / 2.0F;
		float halfH = imgH / 2.0F;
		int iconX = imgW - mapWidth;
		int iconY = imgH - mapHeight;
		List<MapIcon<?>> drawableEntities = minimap.getDrawableIcons(lastX, lastZ, halfW, halfH, delta);
		for (MapIcon<?> icon : drawableEntities) {
			icon.draw(context, consumerProvider, iconX, iconY, mapWidth, mapHeight, rotation);
			consumerProvider.draw();
		}
	}

	public void resize(int width, int height) {
		this.primaryFramebuffer.resize(width, height);
		this.secondaryFramebuffer.resize(width, height);
	}

	public void deleteFramebuffers() {
		this.primaryFramebuffer.delete();
		this.secondaryFramebuffer.delete();
		this.setLoadedFBO(false);
		this.triedFBO = false;
	}

	public boolean isFBOLoaded() {
		return this.loadedFBO;
	}

	public void setLoadedFBO(boolean loadedFBO) {
		this.loadedFBO = loadedFBO;
	}

	public boolean isFBOTried() {
		return this.triedFBO;
	}
}
