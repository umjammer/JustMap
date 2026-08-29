package ru.bulldog.justmap.client.screen;

import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.map.data.MapDataProvider;
import ru.bulldog.justmap.util.LangUtil;
import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.render.RenderUtil;

public class WorldnameScreen extends Screen {

	private final static Component TITLE = MutableComponent.create(LangUtil.getText("gui", "screen.worldname"));
	private final static Identifier FRAME_TEXTURE = Identifier.fromNamespaceAndPath(JustMap.MODID, "textures/screen_background.png");

	private final Screen parent;
	private EditBox nameField;
	private boolean success = false;
	private int center;
	private int frameWidth;
	private int frameHeight;
	private int x, y;

	public WorldnameScreen(Screen parent) {
		super(TITLE);
		this.parent = parent;
	}

	@Override
	public void init() {
		this.center = width / 2;
		this.frameWidth = width / 3;
		this.frameWidth = frameWidth > 320 ? frameWidth : Math.min(width, 320);
		int btnY;
		if (frameWidth == width) {
			this.frameHeight = height;
			btnY = height - 40;
			this.x = 0;
			this.y = 0;
		} else {
			this.frameHeight = (frameWidth * 10) / 16;
			this.x = center - frameWidth / 2;
			this.y = height / 2 - frameHeight / 2;
			btnY = (y + frameHeight) - 40;
		}
		Component defaultText = Component.literal("Default");
		this.nameField = new EditBox(font, x + 20, y + 50, frameWidth - 40, 20, defaultText);
		this.setFocused(this.nameField);
		this.nameField.setFocused(true);
		this.addRenderableWidget(Button.builder(MutableComponent.create(LangUtil.getText("gui", "save")), this::onPressSave).bounds(center - 30, btnY, 80, 20).build());
		this.addWidget(nameField);
	}

	private void onPressSave(Button button) {
		String worldName = nameField.getValue();
		worldName = worldName.trim().replaceAll(" +", " ");
		MapDataProvider.getMultiworldManager().setCurrentWorldName(worldName);
		this.success = true;
		this.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		this.extractTransparentBackground(context);
		context.centeredText(font, LangUtil.getString("gui", "worldname_title"), center, y + 25, Colors.WHITE);
		for (GuiEventListener child : children()) {
			if (child instanceof Renderable) {
				((Renderable) child).extractRenderState(context, mouseX, mouseY, delta);
			}
		}
		super.extractRenderState(context, mouseX, mouseY, delta);
	}

	@Override
	public void extractTransparentBackground(GuiGraphicsExtractor context) {
		super.extractTransparentBackground(context);
		RenderUtil.drawTexture(context, FRAME_TEXTURE, x, y, frameWidth, frameHeight);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ENTER) {
			this.onPressSave(null);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		if (!success) {
			MapDataProvider.getMultiworldManager().setCurrentWorldName("");
		}
		this.minecraft.setScreenAndShow(parent);
	}
}
