package ru.bulldog.justmap.client.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;


public class TitledButtonWidget<W extends AbstractWidget> extends AbstractWidget implements GuiEventListener {
	public final W widget;
	public final PlainTextContents.LiteralContents title;
	private final Font font;

	private final static int SPACING = 3;

	public TitledButtonWidget(Font font, W widget, int x, int y, int width, int height, String message, String title) {
		super(x, y, width, height, Component.literal(message));
		this.widget = widget;
		this.title = new PlainTextContents.LiteralContents(title);
		this.font = font;

		update();
	}

	private void update() {
		int titleWidth = font.width(title.text());
		int widgetWidth = widget.getWidth();
		int wx = getX() + width - widgetWidth;
		if (getX() + titleWidth + SPACING > wx) {
			wx = getX() + titleWidth + SPACING;
			widget.setWidth((getX() + width) - wx);
		}

		this.widget.setX(wx);
		this.widget.setY(getY());
	}

	@Override
	public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		context.text(font, title.text(), getX(), getY(), 0xFFFFFFFF);
		widget.extractWidgetRenderState(context, mouseX, mouseY, delta);
	}

	@Override
	public void mouseMoved(double double_1, double double_2) {
		this.widget.mouseMoved(double_1, double_2);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		return this.widget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		return this.widget.keyPressed(event);
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		return this.widget.keyReleased(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return this.widget.charTyped(event);
	}

	@Override
	public void setFocused(boolean boolean_1) {
		this.widget.setFocused(boolean_1);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		this.widget.onClick(event, doubleClick);
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		this.widget.onRelease(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return this.widget.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		return this.widget.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		return this.widget.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean isHoveredOrFocused() {
		return this.widget.isHoveredOrFocused();
	}

	@Override
	public boolean isMouseOver(double double_1, double double_2) {
		return this.widget.isMouseOver(double_1, double_2);
	}

	@Override
	public void playDownSound(SoundManager soundManager_1) {
		this.widget.playDownSound(soundManager_1);
	}

	@Override
	public void updateWidgetNarration(NarrationElementOutput builder) {
		// FIXME: implement?
	}
}
