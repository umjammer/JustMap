package ru.bulldog.justmap.client.widget;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import ru.bulldog.justmap.map.minimap.Minimap;
import ru.bulldog.justmap.util.render.RenderUtil;

public class MinimapWidget implements GuiEventListener, Renderable {

	final Minimap map;
	final int left;
	final int right;
	final int top;
	final int bottom;
	final int width;
	final int height;
	final int bgW;
	final int bgH;
	final int border;
	final double initX;
	final double initY;
	double x, y;
	boolean focused;

	public MinimapWidget(Screen parent, Minimap map) {
		this.map = map;
		this.initX = x = map.getSkinX();
		this.initY = y = map.getSkinY();
		this.border = map.getBorder();
		this.width = map.getWidth() + border * 2;
		this.height = map.getHeight() + border * 2;
		this.bgW = width - border * 2;
		this.bgH = height - border * 2;

		int offset = map.getOffset();
		this.top = right = offset;
		this.left = parent.width - width - offset;
		this.bottom = parent.height - height - offset;
	}

	public int getX() {
		return (int) this.x;
	}

	public int getY() {
		return (int) this.y;
	}

	public void resetPosition() {
		this.x = initX;
		this.y = initY;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		int color = 0xDD00AA00;
		if (Minimap.isRound()) {
			double centerX = x + border + bgW / 2;
			double centerY = y + border + bgH / 2;
			RenderUtil.drawCircle(context, centerX, centerY, bgW / 2.0, color);
		} else {
			RenderUtil.fill(context, x + border, y + border, bgW, bgH, color);
		}
		if (map.getSkin() != null) {
			map.getSkin().draw(context, x, y, width, height);
		}
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return (mouseX > x && mouseY > y && mouseX < x + width && mouseY < y + height);
	}

	@Override
	public void setFocused(boolean focused) {
		this.focused = focused;
	}

	@Override
	public boolean isFocused() {
		return focused;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return this.isMouseOver(event.x(), event.y());
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		this.x += deltaX;
		this.y += deltaY;

		if (x > left) x = left;
		if (x < right) x = right;
		if (y < top) y = top;
		if (y > bottom) y = bottom;

		return true;
	}
}
