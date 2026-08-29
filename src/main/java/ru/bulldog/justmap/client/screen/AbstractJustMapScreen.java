package ru.bulldog.justmap.client.screen;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.util.HashMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.util.LangUtil;
import ru.bulldog.justmap.util.render.RenderUtil;

public abstract class AbstractJustMapScreen extends Screen {
	public static final Identifier DEFAULT_TEXTURE = Identifier.parse("textures/block/dirt.png");
	public static final HashMap<String, Pair<String, Identifier>> DIMENSION_INFO = new HashMap<>() {
		private static final long serialVersionUID = 1L;
		{
			put("minecraft:overworld", new Pair<>(JustMap.MODID + ".dim.overworld", Identifier.parse("textures/block/stone.png")));
			put("minecraft:the_nether", new Pair<>(JustMap.MODID + ".dim.nether", Identifier.parse("textures/block/netherrack.png")));
			put("minecraft:the_end", new Pair<>(JustMap.MODID + ".dim.the_end", Identifier.parse("textures/block/end_stone.png")));
		}
	};

	protected final Screen parent;
	protected Pair<String, Identifier> info;
	protected final LangUtil langUtil;
	protected int x, y, center;
	protected int paddingTop;
	protected int paddingBottom;

	protected AbstractJustMapScreen(Component title) {
		this(title, null);
	}

	public AbstractJustMapScreen(Component title, Screen parent) {
		super(title);
		this.parent = parent;
		this.langUtil = new LangUtil(LangUtil.GUI_ELEMENT);
	}

	@Override
	protected void init() {
		ResourceKey<Level> dimKey = minecraft.level.dimension();
		this.info = DIMENSION_INFO.getOrDefault(dimKey.identifier().toString(), null);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		this.renderForeground(context);
		for (GuiEventListener e : children()) {
			if (e instanceof Renderable) {
				((Renderable) e).extractRenderState(context, mouseX, mouseY, delta);
			}
		}
	}

	public void renderBackground(GuiGraphicsExtractor context) {
		context.fill(0, 0, width, height, 0x88444444);
		this.drawBorders(context);
	}

	public void renderForeground(GuiGraphicsExtractor context) {}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(parent);
	}

	public void renderTexture(GuiGraphicsExtractor context, int x, int y, int width, int height, float u, float v, Identifier id) {
		RenderUtil.drawTexture(context, id, x, y, width, height, 0.0F, 0.0F, u, v);
	}

	public void renderTextureModal(GuiGraphicsExtractor context, int x, int y, int width, int height, int textureWidth, int textureHeight, Identifier id) {
		this.renderTexture(context, x, y, width, height, (float) width / textureWidth, (float) height / textureHeight, id);
	}

	public void renderTextureRepeating(GuiGraphicsExtractor context, int x, int y, int width, int height, int textureHeight, int textureWidth, Identifier id) {
		for (int xp = 0; xp < width; xp += textureWidth) {
			int w = (xp + textureWidth < width) ? textureWidth : width - xp;
			for (int yp = 0; yp < height; yp += textureHeight) {
				int h = (yp + textureHeight < height) ? textureHeight : height - yp;
				this.renderTextureModal(context, x + xp, y + yp, w, h, textureWidth, textureHeight, id);
			}
		}
	}

	protected void drawBorders(GuiGraphicsExtractor context) {
		this.drawBorders(context, 32, 32);
	}

	protected void drawBorders(GuiGraphicsExtractor context, int top, int bottom) {
		Identifier id = info != null ? info.getSecond() : DEFAULT_TEXTURE;
		this.renderTextureRepeating(context, 0, 0, width, top, 16, 16, id);
		this.renderTextureRepeating(context, 0, height - bottom, width, bottom, 16, 16, id);
	}

	public Component lang(String key) {
		return MutableComponent.create(langUtil.getText(key));
	}

	public Pair<String, Identifier> getDimensionInfo(Identifier dim) {
		return DIMENSION_INFO.getOrDefault(dim != null ? dim.toString() : "unknown", null);
	}
}
