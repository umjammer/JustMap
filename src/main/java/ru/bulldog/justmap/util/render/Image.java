package ru.bulldog.justmap.util.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public abstract class Image {

	protected static final TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();

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

	public abstract void draw(DrawContext context, double x, double y, int w, int h);

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public Identifier getId() {
		return this.textureId;
	}

	public void bindTexture() {
		RenderUtil.bindTexture(textureId);
	}

	public void draw(DrawContext context, double x, double y) {
		MatrixStack matrices = new MatrixStack();
		this.draw(context, x, y, this.getWidth(), this.getHeight());
	}

	public void draw(DrawContext context, double x, double y, int size) {
		this.draw(context, x, y, size, size);
	}

	protected void draw(DrawContext context, double x, double y, float w, float h) {
		RenderUtil.drawImage(context, this, x, y, w, h);
	}
}
