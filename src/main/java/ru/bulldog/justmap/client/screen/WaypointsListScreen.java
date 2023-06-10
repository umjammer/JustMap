package ru.bulldog.justmap.client.screen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.map.data.MapDataProvider;
import ru.bulldog.justmap.map.multiworld.WorldKey;
import ru.bulldog.justmap.map.waypoint.Waypoint;
import ru.bulldog.justmap.map.waypoint.Waypoint.Icon;
import ru.bulldog.justmap.map.waypoint.WaypointKeeper;
import ru.bulldog.justmap.util.Dimension;
import ru.bulldog.justmap.util.GameRulesUtil;
import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.math.MathUtil;
import ru.bulldog.justmap.util.math.RandomUtil;
import ru.bulldog.justmap.util.render.RenderUtil;

public class WaypointsListScreen extends AbstractJustMapScreen {
	private static class Entry implements Element {
		private final MinecraftClient minecraft;

		private int x;
		private int y;
		private final int width;
		private final int height;
		private boolean focused;

		private final ButtonWidget editButton;
		private final ButtonWidget deleteButton;
		private final ButtonWidget tpButton;
		private final Waypoint waypoint;

		public Entry(WaypointsListScreen wayPointListEditor, int x, int y, int width, int height, Waypoint waypoint) {
			this.width = width;
			this.height = height + 2;
			this.waypoint = waypoint;
			this.minecraft = MinecraftClient.getInstance();
			this.editButton = ButtonWidget.builder(wayPointListEditor.lang("edit"), b -> wayPointListEditor.edit(waypoint)).dimensions(0, 0, 40, height).build();
			this.deleteButton = ButtonWidget.builder(wayPointListEditor.lang("delete"), b -> wayPointListEditor.delete(waypoint)).dimensions(0, 0, 40, height).build();
			this.tpButton = ButtonWidget.builder(wayPointListEditor.lang("teleport"), b -> wayPointListEditor.teleport(waypoint)).dimensions(0, 0, 40, height).build();

			this.setPosition(x, y);
		}

		public void setPosition(int x, int y) {
			this.x = x;
			this.y = y;

			this.rightAlign(deleteButton, x + width - 2);
			this.rightAlign(editButton, deleteButton);

			this.editButton.setY(y + 1);
			this.deleteButton.setY(y + 1);

			if (tpButton != null) {
				this.rightAlign(tpButton, editButton);
				this.tpButton.setY(y + 1);
			}
		}

		public void render(DrawContext context, int mouseX, int mouseY, float delta) {
			TextRenderer font = minecraft.textRenderer;

			boolean hover = isMouseOver(mouseX, mouseY);
			int bgColor = hover ? 0x88AAAAAA : 0x88333333;
			context.fill(x, y, x + width, y + height, bgColor);

			int iconSize = height - 2;
			Icon icon = waypoint.getIcon();
			if (icon != null) {
				icon.draw(context, x, y + 1, iconSize, iconSize);
			} else {
				RenderUtil.drawDiamond(x, y + 1, iconSize, iconSize, waypoint.color);
			}

			int stringY = y + 7;
			int nameX = x + iconSize + 2;

			context.drawTextWithShadow(font, waypoint.name, nameX, stringY, Colors.WHITE);

			int posX = tpButton.getX() - 5;
			RenderUtil.drawRightAlignedString(context, waypoint.pos.toShortString(), posX, stringY, Colors.WHITE);

			if (GameRulesUtil.allowTeleportation()) {
				this.tpButton.render(context, mouseX, mouseY, delta);
			}
			this.editButton.render(context, mouseX, mouseY, delta);
			this.deleteButton.render(context, mouseX, mouseY, delta);
		}

		@Override
		public boolean mouseClicked(double double_1, double double_2, int int_1) {
			return this.editButton.mouseClicked(double_1, double_2, int_1) ||
				   this.deleteButton.mouseClicked(double_1, double_2, int_1) ||
				   this.tpButton.mouseClicked(double_1, double_2, int_1);
		}

		@Override
		public boolean mouseReleased(double double_1, double double_2, int int_1) {
			return this.editButton.mouseReleased(double_1, double_2, int_1) ||
				   this.deleteButton.mouseReleased(double_1, double_2, int_1) ||
				   this.tpButton.mouseReleased(double_1, double_2, int_1);
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
		}

		@Override
		public void setFocused(boolean focused) {
			this.focused = focused;
		}

		@Override
		public boolean isFocused() {
			return focused;
		}

		private void rightAlign(ButtonWidget toAlign, ButtonWidget from) {
			toAlign.setX(from.getX() - toAlign.getWidth() - 1);
		}

		private void rightAlign(ButtonWidget toAlign, int right) {
			toAlign.setX(right - toAlign.getWidth());
		}
	}

	private static final Text TITLE = Text.translatable(JustMap.MODID + ".gui.screen.waypoints_list");

	private final WaypointKeeper keeper = WaypointKeeper.getInstance();
	private WorldKey currentWorld;
	private int currentIndex = 0;
	private final List<WorldKey> worlds;
	private List<Waypoint> waypoints;
	private final List<Entry> entries = new ArrayList<>();

	private int scrollAmount = 0;
	private int maxScroll = 0;
	private int screenWidth;

	private ButtonWidget prevDimensionButton, nextDimensionButton;
	private ButtonWidget addButton, closeButton;

	public WaypointsListScreen(Screen parent) {
		super(TITLE, parent);
		this.worlds = this.keeper.getWorlds();
	}

	@Override
	protected void init() {
		// FIXME: don't we ever need to remove keys?
		MapDataProvider.getMultiworldManager().forEachWorldMapper(
				(key, worldMapper) -> {
			if (!worlds.contains(key)) {
				this.worlds.add(key);
			}
		});
		this.center = width / 2;
		this.screenWidth = center > 480 ? center : Math.min(width, 480);
		this.x = center - screenWidth / 2;
		this.prevDimensionButton = ButtonWidget.builder(Text.of("<"), b -> cycleDimension(-1)).dimensions(x + 10, 6, 20, 20).build();
		this.nextDimensionButton = ButtonWidget.builder(Text.of(">"), b -> cycleDimension(1)).dimensions(x + screenWidth - 30, 6, 20, 20).build();
		this.addButton = ButtonWidget.builder(lang("create"), b -> add()).dimensions(center - 62, height - 26, 60, 20).build();
		this.closeButton = ButtonWidget.builder(lang("close"), b -> close()).dimensions(center + 2, height - 26, 60, 20).build();
		this.currentWorld = MapDataProvider.getMultiworldManager().getCurrentWorldKey();
		this.currentIndex = this.getIndex(currentWorld);

		this.reset();
	}

	private void createEntries() {
		this.entries.clear();

		int y = 40;
		for (Waypoint wp : waypoints) {
			Entry entry = new Entry(this, x + 10, scrollAmount + y, screenWidth - 20, 20, wp);
			this.entries.add(entry);

			y += entry.height;
		}
	}

	private void updateEntries() {
		int y = 40;
		for (Entry entry : entries) {
			entry.setPosition(x + 10, scrollAmount + y);
			y += entry.height;
		}
	}


	private void cycleDimension(int i) {
		this.currentIndex += i;
		if (currentIndex >= worlds.size()) {
			this.currentIndex = 0;
		}
		else if (currentIndex < 0) {
			this.currentIndex = worlds.size() - 1;
		}

		this.currentWorld = this.worlds.get(currentIndex);
		this.reset();
	}

	private int getIndex(WorldKey world) {
		return this.worlds.indexOf(world);
	}

	public void reset() {
		this.info = this.getDimensionInfo(currentWorld.getDimension());
		this.waypoints = this.keeper.getWaypoints(currentWorld, false);
		this.createEntries();

		this.maxScroll = waypoints.size() * 20;
		@SuppressWarnings("unchecked")
		List<Element> children = (List<Element>) children();
		children.clear();
		children.addAll(entries);
		children.add(addButton);
		children.add(closeButton);
		children.add(prevDimensionButton);
		children.add(nextDimensionButton);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		this.entries.forEach(e -> e.render(context, mouseX, mouseY, delta));

		String screenTitle = this.currentWorld.getName();
		if (screenTitle == null) {
			screenTitle = info == null ? lang("unknown").getString() : I18n.translate(info.getFirst());
		}
		context.drawCenteredTextWithShadow(textRenderer, screenTitle, center, 15, Colors.WHITE);
		this.drawScrollBar();
	}

	private void drawScrollBar() {}

	private void edit(Waypoint waypoint) {
		this.client.setScreen(new WaypointEditorScreen(waypoint, this, null));
	}

	private void add() {
		Waypoint waypoint = new Waypoint();
		waypoint.world = currentWorld;
		waypoint.color = RandomUtil.getElement(Waypoint.WAYPOINT_COLORS);
		waypoint.pos = client.player.getBlockPos();
		waypoint.name = "Waypoint";

		this.client.setScreen(new WaypointEditorScreen(waypoint, this, keeper::addNew));
	}

	private void delete(Waypoint waypoint) {
		this.keeper.remove(waypoint);
		this.keeper.saveWaypoints();
		this.reset();
	}

	public void teleport(Waypoint waypoint) {
		if (!MapDataProvider.getMultiworldManager().getCurrentWorldKey().equals(currentWorld)) return;
		int y = waypoint.pos.getY() > 0 ? waypoint.pos.getY() : (Dimension.isNether(client.world) ? 128 : 64);
		this.client.player.sendMessage(Text.of("/tp " + this.client.player.getName().getContent() + " " + waypoint.pos.getX() + " " + y + " " + waypoint.pos.getZ()), true);
		this.close();
	}

	@Override
	public boolean mouseScrolled(double double_1, double double_2, double double_3) {
		scrollAmount = MathUtil.clamp(scrollAmount + (int) (double_3 * 12), -maxScroll + 80, 0);
		this.updateEntries();

		return true;
	}

	@Override
	public boolean keyPressed(int int_1, int int_2, int int_3) {
		if (int_1 == GLFW.GLFW_KEY_U) {
			this.close();
			return true;
		}
		return super.keyPressed(int_1, int_2, int_3);
	}
}
