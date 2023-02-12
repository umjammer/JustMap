package ru.bulldog.justmap.client.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import ru.bulldog.justmap.client.JustMapClient;
import ru.bulldog.justmap.client.config.ClientConfig;
import ru.bulldog.justmap.client.widget.MinimapWidget;
import ru.bulldog.justmap.config.ConfigKeeper.EnumEntry;
import ru.bulldog.justmap.enums.ScreenPosition;
import ru.bulldog.justmap.util.LangUtil;

public class MapPositionScreen extends Screen {

	private final static Text TITLE = MutableText.of(LangUtil.getText("gui", "screen.map_position"));
	private final static ClientConfig config = JustMapClient.getConfig();

	private final Screen parent;
	private MinimapWidget mapHolder;

	public MapPositionScreen(Screen parent) {
		super(TITLE);
		this.parent = parent;
	}

	@Override
	public void init() {
		int posX = width / 2;
		int posY = height - 60;
		this.addDrawableChild(ButtonWidget.builder(MutableText.of(LangUtil.getText("gui", "save")), button -> this.onSave()).dimensions(posX - 125, posY, 80, 20).build());
		this.addDrawableChild(ButtonWidget.builder(MutableText.of(LangUtil.getText("gui", "reset")), button -> this.onReset()).dimensions(posX - 40, posY, 80, 20).build());
		this.addDrawableChild(ButtonWidget.builder(MutableText.of(LangUtil.getText("gui", "cancel")), button -> this.close()).dimensions(posX + 45, posY, 80, 20).build());
		this.mapHolder = this.addDrawable(new MinimapWidget(this, JustMapClient.getMiniMap()));
	}

	private void onReset() {
		this.mapHolder.resetPosition();
	}

	private void onSave() {
		EnumEntry<ScreenPosition> drawPosConfig = config.getEntry("map_position");
		drawPosConfig.setValue(ScreenPosition.USER_DEFINED);
		config.setInt("map_position_x", mapHolder.getX());
		config.setInt("map_position_y", mapHolder.getY());
		config.saveChanges();
		this.close();
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		fill(matrices, 0, 0, width, height, 0x66000000);
		this.mapHolder.render(matrices, mouseX, mouseY, delta);
		super.render(matrices, mouseX, mouseY, delta);
	}

	@Override
	public void close() {
		this.client.setScreen(parent);
	}
}
