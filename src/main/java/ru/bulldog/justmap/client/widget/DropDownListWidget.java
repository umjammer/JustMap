package ru.bulldog.justmap.client.widget;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import com.mojang.blaze3d.systems.RenderSystem;
import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.render.RenderUtil;

public class DropDownListWidget extends AbstractContainerEventHandler implements Renderable, NarratableEntry {

	private final List<ListElementWidget> children = new ArrayList<>();
	private boolean visible = false;
	private final int x;
	private final int y;
	private int width, height;
	private final int elemHeight;
	private final int padding = 3;
	private final int spacing = 1;

	public DropDownListWidget(int x, int y, int width, int height) {
		this.elemHeight = height;
		this.width = width;
		this.height = height + padding * 2;
		this.x = x;
		this.y = y;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		if (!visible) return;
		this.renderBackground(context);
		for (ListElementWidget element : children) {
			element.extractRenderState(context, mouseX, mouseY, delta);
		}
	}

	/**
	 * Places the elements. Their positions are hit-tested, so they cannot be left until
	 * the widget is drawn: while the list is closed it is never drawn at all.
	 */
	private void layoutElements() {
		int elemX = this.x + padding;
		int elemY = this.y + padding;
		for (ListElementWidget element : children) {
			element.x = elemX;
			element.y = elemY;
			elemY += elemHeight + spacing;
		}
	}

	private void renderBackground(GuiGraphicsExtractor context) {
		RenderUtil.fill(context, x, y, width, height, 0xAA222222);
		RenderUtil.drawLine(context, x, y, x + width, y, Colors.LIGHT_GRAY);
		RenderUtil.drawLine(context, x, y, x, y + height, Colors.LIGHT_GRAY);
		RenderUtil.drawLine(context, x + width, y, x + width, y + height, Colors.LIGHT_GRAY);
		RenderUtil.drawLine(context, x, y + height, x + width, y + height, Colors.LIGHT_GRAY);
	}

	public void addElement(ListElementWidget element) {
		element.height = elemHeight;
		this.width = Math.max(width, element.width + padding * 2);
		this.children.add(element);
		this.children.forEach(elem -> elem.width = width - padding * 2);
		this.height = children.size() * (elemHeight + spacing) + padding * 2;
		this.layoutElements();
	}

	public void toggleVisible() {
		this.visible = !visible;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		// A closed list must not claim the pointer: since 26.2 a container swallows the
		// click as soon as one of its children reports a hit, which would leave whatever
		// sits underneath (the button that opens this list) unclickable.
		if (!visible) return false;

		for (GuiEventListener elem : children) {
			if (elem.isMouseOver(mouseX, mouseY)) return true;
		}
		return false;
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return this.children;
	}

	@Override
	public void updateNarration(NarrationElementOutput builder) {
		// FIXME: implement?
	}

	@Override
	public NarrationPriority narrationPriority() {
		// FIXME: correct?
		return NarrationPriority.NONE;
	}
}
