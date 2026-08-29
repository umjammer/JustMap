package ru.bulldog.justmap.client.render;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.map.ChunkGrid;
import ru.bulldog.justmap.map.data.MapRegion;
import ru.bulldog.justmap.map.icon.MapIcon;
import ru.bulldog.justmap.map.icon.WaypointIcon;
import ru.bulldog.justmap.map.minimap.Minimap;

public class FastMiniMapRenderer extends AbstractMiniMapRenderer {

	/**
	 * How many horizontal bands approximate the disc of a round minimap.
	 *
	 * <p>Since 26.2 the GUI is drawn from render states and the alpha-mask trick the old
	 * renderer used (clear the alpha channel, stamp a round mask, then blend against it) has
	 * no equivalent: there is no immediate GL state to set. Clipping is a stack of rectangles,
	 * so the disc is built from bands. The skin frame drawn on top hides the outermost step.
	 */
	private static final int ROUND_BANDS = 64;

	public FastMiniMapRenderer(Minimap map) {
		super(map);
	}

	protected void render(GuiGraphicsExtractor context) {
		if (Minimap.isRound()) {
			this.renderRound(context);
		} else {
			context.enableScissor(mapX, mapY, mapX + mapWidth, mapY + mapHeight);
			this.renderContent(context);
			context.disableScissor();
		}
	}

	private void renderRound(GuiGraphicsExtractor context) {
		double centerX = mapX + mapWidth / 2.0;
		double centerY = mapY + mapHeight / 2.0;
		double radiusX = mapWidth / 2.0;
		double radiusY = mapHeight / 2.0;

		int bands = Math.min(mapHeight, ROUND_BANDS);
		double bandHeight = (double) mapHeight / bands;

		for (int i = 0; i < bands; i++) {
			double top = mapY + i * bandHeight;
			double bottom = top + bandHeight;

			// widest point of the band: the edge nearest the centre line
			double dy;
			if ((top - centerY) * (bottom - centerY) <= 0.0) {
				dy = 0.0;
			} else {
				dy = Math.min(Math.abs(top - centerY), Math.abs(bottom - centerY));
			}

			double t = 1.0 - (dy * dy) / (radiusY * radiusY);
			if (t <= 0.0) continue;

			double halfWidth = radiusX * Math.sqrt(t);
			context.enableScissor(
					(int) Math.floor(centerX - halfWidth), (int) Math.floor(top),
					(int) Math.ceil(centerX + halfWidth), (int) Math.ceil(bottom));
			this.renderContent(context);
			context.disableScissor();
		}
	}

	private void renderContent(GuiGraphicsExtractor context) {
		Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		if (mapRotation) {
			float moveX = mapX + mapWidth / 2.0F;
			float moveY = mapY + mapHeight / 2.0F;
			matrices.rotateAbout((float) Math.toRadians(-rotation + 180), moveX, moveY);
		}
		matrices.translate(-offX, -offY);

		this.drawMap(context);
		if (ClientSettings.showGrid) {
			this.drawGrid(context);
		}

		List<MapIcon<?>> drawableEntities = minimap.getDrawableIcons(lastX, lastZ, centerX, centerY, delta);
		for (MapIcon<?> icon : drawableEntities) {
			icon.draw(context, mapX, mapY, mapWidth, mapHeight, rotation);
		}

		matrices.popMatrix();

		List<WaypointIcon> drawableWaypoints = minimap.getWaypoints(playerPos, centerX, centerY);
		for (WaypointIcon icon : drawableWaypoints) {
			icon.draw(context, mapX, mapY, mapWidth, mapHeight, offX, offY, rotation);
		}
	}

	private void drawMap(GuiGraphicsExtractor context) {
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

				region.drawLayer(context, minimap.getLayer(), minimap.getLevel(), imgX + scX, imgY + scY, scW, scH, texX, texY, texW, texH);

				picY += texH > 0 ? texH : 512;
			}

			picX += texW > 0 ? texW : 512;
		}
	}

	private void drawGrid(GuiGraphicsExtractor context) {
		if (paramsUpdated) {
			if (chunkGrid == null) {
				this.chunkGrid = new ChunkGrid(lastX, lastZ, imgX, imgY, imgW, imgH, mapScale);
			} else {
				this.chunkGrid.updateRange(imgX, imgY, imgW, imgH, mapScale);
				this.chunkGrid.updateGrid();
			}
			this.paramsUpdated = false;
		}
		if (playerMoved) {
			this.chunkGrid.updateCenter(lastX, lastZ);
			this.chunkGrid.updateGrid();
			this.playerMoved = false;
		}
		this.chunkGrid.draw(context);
	}
}
