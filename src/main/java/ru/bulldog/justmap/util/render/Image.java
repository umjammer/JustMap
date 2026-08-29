package ru.bulldog.justmap.util.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

public abstract class Image {

	protected static final TextureManager textureManager = Minecraft.getInstance().getTextureManager();

	protected final NativeImage image;
	protected Identifier textureId;
	protected int width;
	protected int height;

	protected Image(Identifier id, NativeImage image) {
		this.width = image.getWidth();
		this.height = image.getHeight();
		this.textureId = id;
		this.image = image;
	}

	public abstract void draw(GuiGraphicsExtractor context, double x, double y, int w, int h);

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public Identifier getId() {
		return this.textureId;
	}

	public void draw(GuiGraphicsExtractor context, double x, double y) {
		this.draw(context, x, y, this.getWidth(), this.getHeight());
	}

	public void draw(GuiGraphicsExtractor context, double x, double y, int size) {
		this.draw(context, x, y, size, size);
	}

	protected void draw(GuiGraphicsExtractor context, double x, double y, float w, float h) {
		RenderUtil.drawImage(context, this, x, y, w, h);
	}

	public void draw(GuiGraphicsExtractor context, double x, double y, int w, int h, int tint) {
		RenderUtil.drawTexture(context, this.getId(), x, y, w, h, tint);
	}
}
