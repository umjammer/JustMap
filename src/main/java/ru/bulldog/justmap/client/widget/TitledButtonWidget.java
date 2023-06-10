package ru.bulldog.justmap.client.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.Text;


public class TitledButtonWidget<W extends ClickableWidget> extends ClickableWidget implements Element {
	public final W widget;
	public final LiteralTextContent title;
	private final TextRenderer font;

	private final static int SPACING = 3;

	public TitledButtonWidget(TextRenderer font, W widget, int x, int y, int width, int height, String message, String title) {
		super(x, y, width, height, Text.literal(message));
		this.widget = widget;
		this.title = new LiteralTextContent(title);
		this.font = font;

		update();
	}

	private void update() {
		int titleWidth = font.getWidth(title.string());
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
	public void render(DrawContext context, int int_1, int int_2, float float_1) {
		context.drawTextWithShadow(font, title.string(), getX(), getY(), 0xFFFFFFFF);
		widget.render(context, int_1, int_2, float_1);
	}

	@Override
	public void mouseMoved(double double_1, double double_2) {
		this.widget.mouseMoved(double_1, double_2);
	}

	@Override
	public boolean mouseScrolled(double double_1, double double_2, double double_3) {
		return this.widget.mouseScrolled(double_1, double_2, double_3);
	}

	@Override
	public boolean keyPressed(int int_1, int int_2, int int_3) {
		return this.widget.keyPressed(int_1, int_2, int_3);
	}

	@Override
	public boolean keyReleased(int int_1, int int_2, int int_3) {
		return this.widget.keyReleased(int_1, int_2, int_3);
	}

	@Override
	public boolean charTyped(char char_1, int int_1) {
		return this.widget.charTyped(char_1, int_1);
	}

	@Override
	public void setFocused(boolean boolean_1) {
		this.widget.setFocused(boolean_1);
	}

	@Override
	public void renderButton(DrawContext context, int int_1, int int_2, float float_1) {
		this.widget.renderButton(context, int_1, int_2, float_1);
	}

	@Override
	public void onClick(double double_1, double double_2) {
		this.widget.onClick(double_1, double_2);
	}

	@Override
	public void onRelease(double double_1, double double_2) {
		this.widget.onRelease(double_1, double_2);
	}

	@Override
	public boolean mouseClicked(double double_1, double double_2, int int_1) {
		return this.widget.mouseClicked(double_1, double_2, int_1);
	}

	@Override
	public boolean mouseReleased(double double_1, double double_2, int int_1) {
		return this.widget.mouseReleased(double_1, double_2, int_1);
	}

	@Override
	public boolean mouseDragged(double double_1, double double_2, int int_1, double double_3, double double_4) {
		return this.widget.mouseDragged(double_1, double_2, int_1, double_3, double_4);
	}

	@Override
	public boolean isSelected() {
		return this.widget.isSelected();
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
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		// FIXME: implement?
	}
}
