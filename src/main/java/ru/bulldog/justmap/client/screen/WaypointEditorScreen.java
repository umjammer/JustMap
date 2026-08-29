package ru.bulldog.justmap.client.screen;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.client.JustMapClient;
import ru.bulldog.justmap.client.widget.TitledButtonWidget;
import ru.bulldog.justmap.config.ConfigKeeper.IntegerRange;
import ru.bulldog.justmap.map.waypoint.Waypoint;
import ru.bulldog.justmap.map.waypoint.Waypoint.Icon;
import ru.bulldog.justmap.map.waypoint.WaypointKeeper;
import ru.bulldog.justmap.util.Predicates;
import ru.bulldog.justmap.util.colors.Colors;

public class WaypointEditorScreen extends AbstractJustMapScreen {

	private static final Component TITLE = Component.translatable(JustMap.MODID + ".gui.screen.waypoints_editor");

	private final Waypoint waypoint;

	private int colorIndex;
	private int iconIndex;
	private int showRange;

	private final static int SPACING = 2;
	private final static int PADDING = 10;
	private final static int ROW_HEIGHT = 20;

	private TitledButtonWidget<EditBox> nameField;
	private Checkbox isHidden;
	private Checkbox isTrackable;
	private Checkbox isRenderable;
	private Button prevColorButton, nextColorButton;
	private EditBox xField, yField, zField;
	private final Consumer<Waypoint> onSaveCallback;

	public WaypointEditorScreen(Waypoint waypoint, Screen parent, Consumer<Waypoint> onSaveCallback) {
		super(TITLE, parent);

		this.waypoint = waypoint;
		colorIndex = getColorIndex(waypoint.color);
		this.iconIndex = getIconIndex(waypoint.getIcon());
		this.onSaveCallback = onSaveCallback;
	}

	@Override
	public void init() {
		super.init();

		this.center = width / 2;

		int screenW = center > 480 ? center : Math.min(width, 480);

		this.x = center - screenW / 2;
		this.y = 60;

		int row = ROW_HEIGHT + SPACING;

		int ex = x + PADDING;
		int ey = y;
		int ew = screenW - PADDING * 2;
		this.nameField = new TitledButtonWidget<>(font, new EditBox(font, 0, 0, ew - 30, 12, Component.literal("Name")), ex, ey, ew, ROW_HEIGHT, "", lang("name").getString());
		this.nameField.setFocused(true);
		this.nameField.widget.setMaxLength(48);
		this.nameField.widget.setValue(waypoint.name);

		@SuppressWarnings("unchecked")
		List<GuiEventListener> children = (List<GuiEventListener>) children();
		children.add(nameField);

		Predicate<String> validNumber = (s) -> Predicates.or(s, Predicates.isInteger, Predicates.isEmpty, "-"::equals);

		ew = 60;
		int px = center - (ew * 3) / 2;

		ey += row;

		this.xField = new EditBox(font, px, ey, ew, ROW_HEIGHT, Component.literal(""));
		restrictTo(this.xField, validNumber);
		this.xField.setMaxLength(7);
		this.xField.setValue(waypoint.pos.getX() + "");

		this.yField = new EditBox(font, px + ew, ey, ew, ROW_HEIGHT, Component.literal(""));
		restrictTo(this.yField, validNumber);
		this.yField.setMaxLength(7);
		this.yField.setValue(waypoint.pos.getY() + "");

		this.zField = new EditBox(font, px + 2 * ew, ey, ew, ROW_HEIGHT, Component.literal(""));
		restrictTo(this.zField, validNumber);
		this.zField.setMaxLength(7);
		this.zField.setValue(waypoint.pos.getZ() + "");

		children.add(xField);
		children.add(yField);
		children.add(zField);

		ey += row;

		ew = 20;
		this.prevColorButton = Button.builder(Component.literal("<"), b -> cycleColor(-1)).bounds(ex, ey, ew, ROW_HEIGHT).build();
		children.add(prevColorButton);

		this.nextColorButton = Button.builder(Component.literal(">"), b -> cycleColor(1)).bounds(x + screenW - ew - PADDING, ey, ew, ROW_HEIGHT).build();
		children.add(nextColorButton);

		ey += row;

		Button prevIconButton = Button.builder(Component.literal("<"), b -> cycleIcon(-1)).bounds(ex, ey, ew, ROW_HEIGHT).build();
		children.add(prevIconButton);

		Button nextIconButton = Button.builder(Component.literal(">"), b -> cycleIcon(1)).bounds(x + screenW - ew - PADDING, ey, ew, ROW_HEIGHT).build();
		children.add(nextIconButton);

		ey += row * 1.5;

		int sliderW = (int) (screenW * 0.6);
		int elemX = width / 2 - sliderW / 2;

		this.isHidden = Checkbox.builder(lang("wp_hidden"), font).pos(elemX, ey).selected(waypoint.hidden).build();
		this.isTrackable = Checkbox.builder(lang("wp_tracking"), font).pos(elemX + 100, ey).selected(waypoint.tracking).build();
		this.isRenderable = Checkbox.builder(lang("wp_render"), font).pos(elemX + 200, ey).selected(waypoint.render).build();
		children.add(isHidden);
		children.add(isTrackable);
		children.add(isRenderable);

		ey += row * 1.25;

		IntegerRange maxRangeConfig = JustMapClient.getConfig().getEntry("max_render_dist");
		final int SHOW_RANGE_MAX = maxRangeConfig.maxValue();
		this.showRange = waypoint.showRange;
		children.add(new AbstractSliderButton(elemX, ey, sliderW, ROW_HEIGHT, Component.empty(), (double) this.showRange / SHOW_RANGE_MAX) {
			{
				this.updateMessage();
			}

			@Override
			protected void updateMessage() {
				this.setMessage(Component.literal(lang("wp_render_dist").getString() + WaypointEditorScreen.this.showRange));
			}

			@Override
			protected void applyValue() {
				WaypointEditorScreen.this.showRange = Mth.floor(Mth.clampedLerp(0, SHOW_RANGE_MAX, this.value));
			}
		});

		ew = 60;
		ey = height - (ROW_HEIGHT / 2 + 16);
		Button saveButton = Button.builder(lang("save"), b -> {
			save();
			onClose();
		}).bounds(center - ew - 2, ey, ew, ROW_HEIGHT).build();
		children.add(saveButton);

		Button cancelButton = Button.builder(lang("cancel"), b -> onClose()).bounds(center + 2, ey, ew, ROW_HEIGHT).build();
		children.add(cancelButton);

		this.setInitialFocus(nameField);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);
		String dimensionName = info == null ? lang("unknown").getString() : I18n.get(info.getFirst());
		context.centeredText(font, dimensionName, center, 15, Colors.WHITE);
	}

	private void cycleColor(int i) {
		this.colorIndex += i;
		if (colorIndex < 0) {
			this.colorIndex = Waypoint.WAYPOINT_COLORS.length - 1;
		} else if (colorIndex >= Waypoint.WAYPOINT_COLORS.length) {
			this.colorIndex = 0;
		}
	}

	private void cycleIcon(int i) {
		this.iconIndex += i;
		if (iconIndex < 0) {
			this.iconIndex = Waypoint.amountIcons();
		} else if (iconIndex >= Waypoint.amountIcons()) {
			this.iconIndex = 0;
		}
	}

	private void save() {
		this.waypoint.name = nameField.widget.getValue();
		int color = Waypoint.WAYPOINT_COLORS[colorIndex];
		if(Waypoint.getIcon(iconIndex) != null) {
			this.waypoint.setIcon(Waypoint.getIcon(iconIndex), color);
		} else {
			this.waypoint.color = color;
		}
		this.waypoint.hidden = isHidden.selected();
		this.waypoint.tracking = isTrackable.selected();
		this.waypoint.render = isRenderable.selected();

		int xPos = (xField.getValue().isEmpty() || xField.getValue().equals("-")) ? 0 : Integer.parseInt(xField.getValue());
		int yPos = (yField.getValue().isEmpty() || yField.getValue().equals("-")) ? 0 : Integer.parseInt(yField.getValue());
		int zPos = (zField.getValue().isEmpty() || zField.getValue().equals("-")) ? 0 : Integer.parseInt(zField.getValue());

		this.waypoint.pos = new BlockPos(xPos, yPos, zPos);

		this.waypoint.showRange = this.showRange;

		if (onSaveCallback != null) {
			this.onSaveCallback.accept(waypoint);
		}

		WaypointKeeper.getInstance().saveWaypoints();
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(parent);
	}

	@Override
	public void renderForeground(GuiGraphicsExtractor context) {
		int x = prevColorButton.getX() + prevColorButton.getWidth() + 2;
		int y = prevColorButton.getY() + 3;
		int w = nextColorButton.getX() - x - 2;
		int h = 12;

		int col = Waypoint.WAYPOINT_COLORS[colorIndex];

		Icon icon;
		if (iconIndex > 0) {
			icon = Waypoint.getIcon(iconIndex);
		} else {
			icon = Waypoint.getColoredIcon(col);
		}
		int ix = center - icon.getWidth() / 2;
		int iy = y + ROW_HEIGHT + (ROW_HEIGHT / 2 - icon.getHeight() / 2);
		int color = iconIndex > 0 ? icon.color : col;
		this.borderedRect(context, x, y, w, h, color, 2, 0xFFCCCCCC);
		icon.draw(context, ix, iy);
	}

	private void rect(GuiGraphicsExtractor context, int x, int y, int w, int h, int color) {
		context.fill(x, y, x + w, y + h, color);
	}

	private void borderedRect(GuiGraphicsExtractor context, int x, int y, int w, int h, int color, int border, int borderColor) {
		int hb = border >> 1;
		this.rect(context, x, y, w, h, borderColor);
		this.rect(context, x + hb, y + hb, w - border, h - border, color);
	}

	private int getColorIndex(int color) {
		for (int i = 0; i < Waypoint.WAYPOINT_COLORS.length; i++) {
			if (Waypoint.WAYPOINT_COLORS[i] == color) {
				return i;
			}
		}
		return 0;
	}

	/**
	 * Keeps a text field to the values {@code accepts} allows.
	 *
	 * <p>26.2 dropped {@code EditBox.setFilter}, which used to reject a keystroke before it
	 * landed; the nearest equivalent is to watch the value and put back the last good one.
	 */
	private static void restrictTo(EditBox field, Predicate<String> accepts) {
		String[] lastGood = { field.getValue() };
		field.setResponder(value -> {
			if (accepts.test(value)) {
				lastGood[0] = value;
			} else {
				field.setValue(lastGood[0]);
			}
		});
	}

	private int getIconIndex(Icon icon) {
		if (icon == null) return 0;
		return icon.key;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ENTER) {
			this.save();
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}
}
