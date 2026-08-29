package ru.bulldog.justmap.client.screen;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.opengl.GlStateManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;
import ru.bulldog.justmap.client.JustMapClient;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.client.config.ConfigFactory;
import ru.bulldog.justmap.client.widget.DropDownListWidget;
import ru.bulldog.justmap.client.widget.ListElementWidget;
import ru.bulldog.justmap.map.ChunkGrid;
import ru.bulldog.justmap.map.IMap;
import ru.bulldog.justmap.map.MapPlayerManager;
import ru.bulldog.justmap.map.data.Layer;
import ru.bulldog.justmap.map.data.MapDataProvider;
import ru.bulldog.justmap.map.data.MapRegion;
import ru.bulldog.justmap.map.data.WorldMapper;
import ru.bulldog.justmap.map.icon.PlayerIcon;
import ru.bulldog.justmap.map.icon.WaypointIcon;
import ru.bulldog.justmap.map.multiworld.WorldKey;
import ru.bulldog.justmap.map.waypoint.Waypoint;
import ru.bulldog.justmap.map.waypoint.WaypointKeeper;
import ru.bulldog.justmap.util.CurrentWorldPos;
import ru.bulldog.justmap.util.Dimension;
import ru.bulldog.justmap.util.GameRulesUtil;
import ru.bulldog.justmap.util.LangUtil;
import ru.bulldog.justmap.util.PosUtil;
import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.math.MathUtil;
import ru.bulldog.justmap.util.render.RenderUtil;


public class WorldmapScreen extends AbstractJustMapScreen implements IMap {

	private final static Component TITLE = Component.literal("Worldmap");

	private static WorldmapScreen worldmap;

	public static WorldmapScreen getScreen() {
		if (worldmap == null) {
			worldmap = new WorldmapScreen();
		}
		return worldmap;
	}

	private int scaledWidth;
	private int scaledHeight;
	private double centerX;
	private double centerY;
	private float imageScale = 1.0F;
	private boolean playerTracking = true;
	private int mapLevel = 0;
	private DropDownListWidget mapMenu;
	private WorldMapper worldMapper;
	private WorldKey world;
	private BlockPos centerPos;
	private String cursorCoords;
	private Layer mapLayer;
	private ChunkGrid chunkGrid;

	private final List<WaypointIcon> waypoints = new ArrayList<>();
	private final List<PlayerIcon> players = new ArrayList<>();

	private WorldmapScreen() {
		super(TITLE);
	}

	@Override
	public void init() {
		super.init();

		this.paddingTop = 8;
		this.paddingBottom = 8;
		this.centerX = width / 2.0;
		this.centerY = height / 2.0;

		this.worldMapper = MapDataProvider.getManager().getWorldMapper();
		WorldKey worldKey = MapDataProvider.getMultiworldManager().getCurrentWorldKey();
		if (centerPos == null || !worldKey.equals(world)) {
			this.centerPos = CurrentWorldPos.currentPos();
			this.world = worldKey;
		} else if (playerTracking) {
			this.centerPos = CurrentWorldPos.currentPos();
		}
		this.cursorCoords = PosUtil.posToString(centerPos);
		this.chunkGrid = new ChunkGrid(centerPos.getX(), centerPos.getZ(), x, y, width, height, imageScale);

		this.updateScale();

		if (Dimension.isNether(world.getDimension())) {
			this.mapLayer = Layer.NETHER;
			this.mapLevel = CurrentWorldPos.coordY() / mapLayer.getHeight();
		} else {
			this.mapLayer = Layer.SURFACE;
			this.mapLevel = 0;
		}

		this.waypoints.clear();
		List<Waypoint> wps = WaypointKeeper.getInstance().getWaypoints(world, true);
		if (wps != null) {
			Stream<Waypoint> stream = wps.stream().filter(wp -> MathUtil.getDistance(centerPos, wp.pos) <= wp.showRange);
			for (Waypoint wp : stream.toArray(Waypoint[]::new)) {
				WaypointIcon waypoint = new WaypointIcon(this, wp);
				this.waypoints.add(waypoint);
			}
		}
		this.players.clear();
		if (GameRulesUtil.allowPlayerRadar()) {
			List<AbstractClientPlayer> players = this.minecraft.level.players();
			for (Player player : players) {
				if (player == minecraft.player) continue;
				this.players.add(new PlayerIcon(player));
			}
		}

		this.addMapMenu();
		this.addMapButtons();
	}

	private void addMapMenu() {
		LangUtil langUtil = new LangUtil("gui.worldmap");
		this.mapMenu = this.addRenderableWidget(new DropDownListWidget(25, paddingTop + 2, 100, 22));
		this.mapMenu.addElement(new ListElementWidget(MutableComponent.create(langUtil.getText("add_waypoint")), () -> {
			JustMapClient.getMiniMap().createWaypoint(world, centerPos);
			return true;
		}));
		this.mapMenu.addElement(new ListElementWidget(MutableComponent.create(langUtil.getText("set_map_pos")), () -> {
			minecraft.setScreenAndShow(new MapPositionScreen(this));
			return true;
		}));
		this.mapMenu.addElement(new ListElementWidget(MutableComponent.create(langUtil.getText("open_map_config")), () -> {
			minecraft.setScreenAndShow(ConfigFactory.getConfigScreen(this));
			return true;
		}));
	}

	private void addMapButtons() {
		this.addRenderableWidget(Button.builder(Component.literal("x"), b -> onClose()).bounds(width - 24, 10, 20, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("↑"), b -> moveMap(Direction.NORTH)).bounds(width / 2 - 10, height - paddingBottom - 44, 20, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("↓"), b -> moveMap(Direction.SOUTH)).bounds(width / 2 - 10, height - paddingBottom - 22, 20, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("←"), b -> moveMap(Direction.WEST)).bounds(width / 2 - 32, height - paddingBottom - 32, 20, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("→"), b -> moveMap(Direction.EAST)).bounds(width / 2 + 12, height - paddingBottom - 32, 20, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("+"), b -> changeScale(-0.25F)).bounds(width - 24, height / 2 - 21, 20, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("-"), b -> changeScale(+0.25F)).bounds(width - 24, height / 2 + 1, 20, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("✜"), b -> setCenterByPlayer()).bounds(width - 24, height - paddingBottom - 22, 20, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("☰"), b -> mapMenu.toggleVisible()).bounds(4, paddingTop + 2, 20, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("✦"), b -> minecraft.setScreenAndShow(new WaypointsListScreen(this))).bounds(4, height - paddingBottom - 22, 20, 20).build());
	}

	@Override
	public void renderBackground(GuiGraphicsExtractor context) {
//		context.fill(x, 0, x + width, height, 0xFF444444);
		this.drawMap(context);
	}

	@Override
	public void renderForeground(GuiGraphicsExtractor context) {
		if (ClientSettings.showWorldmapGrid) {
			this.chunkGrid.draw(context);
		}
		int iconSize = (int) (ClientSettings.worldmapIconSize / imageScale);
		iconSize = iconSize % 2 != 0 ? iconSize + 1 : iconSize;
		iconSize = MathUtil.clamp(iconSize, 6, (int) (ClientSettings.worldmapIconSize * 1.2));
		for (WaypointIcon icon : waypoints) {
			icon.setPosition(
				MathUtil.screenPos(icon.waypoint.pos.getX(), centerPos.getX(), centerX, imageScale),
				MathUtil.screenPos(icon.waypoint.pos.getZ(), centerPos.getZ(), centerY, imageScale),
				icon.waypoint.pos.getY()
			);
			icon.draw(context, iconSize);
		}
		for (PlayerIcon icon : players) {
			icon.setPosition(
				MathUtil.screenPos(icon.getX(), centerPos.getX(), centerX, imageScale),
				MathUtil.screenPos(icon.getZ(), centerPos.getZ(), centerY, imageScale),
				(int) icon.getY()
			);
			icon.draw(context, iconSize);
		}

		LocalPlayer player = minecraft.player;

		double playerX = player.getX();
		double playerZ = player.getZ();
		double arrowX = MathUtil.screenPos(playerX, centerPos.getX(), centerX, imageScale);
		double arrowY = MathUtil.screenPos(playerZ, centerPos.getZ(), centerY, imageScale);

		MapPlayerManager.getPlayer(player).getIcon().draw(context, arrowX, arrowY, iconSize, true);

		this.drawBorders(context, paddingTop, paddingBottom);
		context.centeredText(minecraft.font, cursorCoords, width / 2, paddingTop + 4, Colors.WHITE);
	}

	private void drawMap(GuiGraphicsExtractor context) {
		int cornerX = centerPos.getX() - scaledWidth / 2;
		int cornerZ = centerPos.getZ() - scaledHeight / 2;

		int picX = 0, imgW = 0;
		while (picX < scaledWidth) {
			int cX = cornerX + picX;
			int picY = 0;
			int imgH;
			while (picY < scaledHeight) {
				int cZ = cornerZ + picY;

				MapRegion region = worldMapper.getMapRegion(this, cX, cZ);

				imgW = 512;
				imgH = 512;
				int imgX = cX - (region.getPos().x << 9);
				int imgY = cZ - (region.getPos().z << 9);

				if (picX + imgW >= scaledWidth) imgW = scaledWidth - picX;
				if (picY + imgH >= scaledHeight) imgH = scaledHeight - picY;
				if (imgX + imgW >= 512) imgW = 512 - imgX;
				if (imgY + imgH >= 512) imgH = 512 - imgY;

				double scX = picX / imageScale;
				double scY = picY / imageScale;
				double scW = imgW / imageScale;
				double scH = imgH / imageScale;

				region.drawLayer(context, mapLayer, mapLevel, scX, scY, scW, scH, imgX, imgY, imgW, imgH);

				picY += imgH > 0 ? imgH : 512;
			}

			picX += imgW > 0 ? imgW : 512;
		}
	}

	public void setCenterByPlayer() {
		this.playerTracking = true;
		this.centerPos = CurrentWorldPos.currentPos();
		this.chunkGrid.updateCenter(centerPos.getX(), centerPos.getZ());
		this.chunkGrid.updateGrid();
	}

	private void updateScale() {
		this.scaledWidth = (int) Math.ceil(width * imageScale);
		this.scaledHeight = (int) Math.ceil(height * imageScale);
		if (scaledWidth > 2580) {
			this.imageScale = 2580F / width;
			this.updateScale();

			return;
		}
		this.chunkGrid.updateRange(x, y, width, height, imageScale);
		this.chunkGrid.updateGrid();
	}

	private void changeScale(float value) {
		this.imageScale = MathUtil.clamp(imageScale + value, 0.5F, 3F);
		this.updateScale();
	}

	private void moveMap(Direction direction) {
		switch (direction) {
			case NORTH:
				this.centerPos = centerPos.offset(0, 0, -16);
				break;
			case SOUTH:
				this.centerPos = centerPos.offset(0, 0, 16);
				break;
			case EAST:
				this.centerPos = centerPos.offset(16, 0, 0);
				break;
			case WEST:
				this.centerPos = centerPos.offset(-16, 0, 0);
				break;
			default: break;
		}
		this.chunkGrid.updateCenter(centerPos.getX(), centerPos.getZ());
		this.chunkGrid.updateGrid();
		this.playerTracking = false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		switch(event.key()) {
			case GLFW.GLFW_KEY_W:
			case GLFW.GLFW_KEY_UP:
				this.moveMap(Direction.NORTH);
		  		return true;
		  	case GLFW.GLFW_KEY_S:
		  	case GLFW.GLFW_KEY_DOWN:
		  		this.moveMap(Direction.SOUTH);
		  		return true;
		  	case GLFW.GLFW_KEY_A:
		  	case GLFW.GLFW_KEY_LEFT:
		  		this.moveMap(Direction.WEST);
		  		return true;
		  	case GLFW.GLFW_KEY_D:
		  	case GLFW.GLFW_KEY_RIGHT:
		  		this.moveMap(Direction.EAST);
		  		return true;
		  	case GLFW.GLFW_KEY_MINUS:
		  	case GLFW.GLFW_KEY_KP_SUBTRACT:
		  		this.changeScale(0.25F);
		  		return true;
		  	case GLFW.GLFW_KEY_EQUAL:
		  	case GLFW.GLFW_KEY_KP_ADD:
		  		this.changeScale(-0.25F);
		  		return true;
		  	case GLFW.GLFW_KEY_X:
		  		this.setCenterByPlayer();
		  		return true;
		  	case GLFW.GLFW_KEY_M:
		  		this.onClose();
		  		return true;
		  	default:
		  		return super.keyPressed(event);
		}
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		if (super.mouseDragged(event, deltaX, deltaY)) return true;

		if (event.button() == 0) {

			int x = centerPos.getX();
			int y = centerPos.getY();
			int z = centerPos.getZ();

			x -= Math.ceil(deltaX * imageScale);
			z -= Math.ceil(deltaY * imageScale);

			this.centerPos = new BlockPos(x, y, z);
			this.chunkGrid.updateCenter(x, z);
			this.chunkGrid.updateGrid();
			this.playerTracking = false;

			return true;
		}

		return false;
	}

	private int pixelToPos(double screenPos, double centerWorld, double centerScreen) {
		return (int) MathUtil.worldPos(screenPos, centerWorld, centerScreen, imageScale);
	}

	private BlockPos cursorBlockPos(double x, double y) {
		int posX = this.pixelToPos(x, centerPos.getX(), centerX);
		int posZ = this.pixelToPos(y, centerPos.getZ(), centerY);
		int posY = MapDataProvider.getManager().getWorldMapper().getMapHeight(mapLayer, mapLevel, posX, posZ);
		posY = posY == -1 ? centerPos.getY() : posY;

		return new BlockPos(posX, posY, posZ);
	}

	@Override
	public void mouseMoved(double d, double e) {
		this.cursorCoords = PosUtil.posToString(cursorBlockPos(d, e));
	}

	private int clicks = 0;
	private long clicked = 0;

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (super.mouseReleased(event)) return true;

		double d = event.x();
		double e = event.y();
		if (event.button() == 0) {
			long time = System.currentTimeMillis();
			if (time - clicked > 300) clicks = 0;

			if (++clicks == 2) {
				JustMapClient.getMiniMap().createWaypoint(world, cursorBlockPos(d, e));

				clicked = 0;
				clicks = 0;
			} else {
				clicked = time;
			}

			return true;
		}

		return false;
	}

	@Override
	public boolean isRotated() {
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		boolean scrolled = super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		this.changeScale(horizontalAmount > 0 ? -0.25F : 0.25F);
		return scrolled;
	}

	@Override
	public int getWidth() {
		return this.width;
	}

	@Override
	public int getHeight() {
		return this.height;
	}

	@Override
	public float getScale() {
		return this.imageScale;
	}

	@Override
	public Layer getLayer() {
		return this.mapLayer;
	}

	@Override
	public int getLevel() {
		return this.mapLevel;
	}

	@Override
	public BlockPos getCenter() {
		return this.centerPos;
	}
}
