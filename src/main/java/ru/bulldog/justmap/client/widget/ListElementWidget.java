package ru.bulldog.justmap.client.widget;

import net.minecraft.client.input.MouseButtonEvent;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.render.RenderUtil;

public class ListElementWidget implements Renderable, GuiEventListener {

	private final Supplier<Boolean> onPress;
	private final Component text;
	final int padding = 2;
	int width, height;
	int x, y;
	boolean focused;

	public ListElementWidget(Component text, Supplier<Boolean> action) {
		this.width = RenderUtil.getWidth(text) + padding * 2;
		this.onPress = action;
		this.text = text;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
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
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		// 26.2 only delivers the release to the child that claimed the press, so an
		// element that ignored mouseClicked would never see mouseReleased and would
		// never run its action.
		return this.isMouseOver(event.x(), event.y());
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (!this.isMouseOver(event.x(), event.y())) return false;
		return this.onPress.get();
	}
}
