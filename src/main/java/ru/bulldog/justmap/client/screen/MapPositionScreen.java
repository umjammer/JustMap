package ru.bulldog.justmap.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import ru.bulldog.justmap.client.JustMapClient;
import ru.bulldog.justmap.client.config.ClientConfig;
import ru.bulldog.justmap.client.widget.MinimapWidget;
import ru.bulldog.justmap.config.ConfigKeeper.EnumEntry;
import ru.bulldog.justmap.enums.ScreenPosition;
import ru.bulldog.justmap.util.LangUtil;

public class MapPositionScreen extends Screen {

	private final static Component TITLE = MutableComponent.create(LangUtil.getText("gui", "screen.map_position"));
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
		this.addRenderableWidget(Button.builder(MutableComponent.create(LangUtil.getText("gui", "save")), button -> this.onSave()).bounds(posX - 125, posY, 80, 20).build());
		this.addRenderableWidget(Button.builder(MutableComponent.create(LangUtil.getText("gui", "reset")), button -> this.onReset()).bounds(posX - 40, posY, 80, 20).build());
		this.addRenderableWidget(Button.builder(MutableComponent.create(LangUtil.getText("gui", "cancel")), button -> this.onClose()).bounds(posX + 45, posY, 80, 20).build());
		this.mapHolder = this.addRenderableOnly(new MinimapWidget(this, JustMapClient.getMiniMap()));
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
		this.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0x66000000);
		this.mapHolder.extractRenderState(context, mouseX, mouseY, delta);
		super.extractRenderState(context, mouseX, mouseY, delta);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(parent);
	}
}
