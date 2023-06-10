package ru.bulldog.justmap.client.widget;

import java.util.function.Supplier;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.render.RenderUtil;

public class ListElementWidget implements Drawable, Element {

	private final Supplier<Boolean> onPress;
	private final Text text;
	final int padding = 2;
	int width, height;
	int x, y;
	boolean focused;

	public ListElementWidget(Text text, Supplier<Boolean> action) {
		this.width = RenderUtil.getWidth(text) + padding * 2;
		this.onPress = action;
		this.text = text;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if (isMouseOver(mouseX, mouseY)) {
			context.fill(x, y, x + width, y + height, 0x33FFFFFF);
		}
		RenderUtil.drawCenteredText(context, text, x + width / 2f, y + height / 2f - 5, Colors.WHITE);
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
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return this.onPress.get();
	}
}
