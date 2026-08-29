package ru.bulldog.justmap.client.config;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import com.mojang.blaze3d.platform.Window;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class FloatSliderEntry extends TooltipListEntry<Float> {

	protected final Slider sliderWidget;
	protected final Button resetButton;
	protected final AtomicDouble value;
	protected final float original;
	private float minimum, maximum;
	private final Consumer<Float> saveConsumer;
	private final Supplier<Float> defaultValue;
	private Function<Float, Component> textGetter = value -> Component.literal(String.format("Value: %.1f", value));
	private final List<AbstractWidget> widgets;
	private final Font textRenderer;

	@Deprecated
	public FloatSliderEntry(Component fieldName, float minimum, float maximum, float value, Component resetButtonKey, Supplier<Float> defaultValue, Consumer<Float> saveConsumer) {
		this(fieldName, minimum, maximum, value, resetButtonKey, defaultValue, saveConsumer, null);
	}

	@Deprecated
	public FloatSliderEntry(Component fieldName, float minimum, float maximum, float value, Component resetButtonKey, Supplier<Float> defaultValue, Consumer<Float> saveConsumer, Supplier<Optional<Component[]>> tooltipSupplier) {
		this(fieldName, minimum, maximum, value, resetButtonKey, defaultValue, saveConsumer, tooltipSupplier, false);
	}

	@Deprecated
	public FloatSliderEntry(Component fieldName, float minimum, float maximum, float value, Component resetButtonKey, Supplier<Float> defaultValue, Consumer<Float> saveConsumer, Supplier<Optional<Component[]>> tooltipSupplier, boolean requiresRestart) {
		super(fieldName, tooltipSupplier, requiresRestart);
		Minecraft client = Minecraft.getInstance();
		this.textRenderer = client.font;
		this.original = value;
		this.defaultValue = defaultValue;
		this.value = new AtomicDouble(value);
		this.saveConsumer = saveConsumer;
		this.maximum = maximum;
		this.minimum = minimum;
		this.sliderWidget = new Slider(0, 0, 152, 20, (this.value.get() - minimum) / Math.abs(maximum - minimum));
		int width = textRenderer.width(resetButtonKey);
		this.resetButton = Button.builder(resetButtonKey, widget ->
				setValue(defaultValue.get())).bounds(0, 0, width + 6, 20).build();
		this.sliderWidget.setMessage(textGetter.apply((float) FloatSliderEntry.this.value.get()));
		this.widgets = Lists.newArrayList(sliderWidget, resetButton);
	}

	@Override
	public void save() {
		if (saveConsumer != null)
			saveConsumer.accept(getValue());
	}

	public Function<Float, Component> getTextGetter() {
		return textGetter;
	}

	public void setTextGetter(Function<Float, Component> textGetter) {
		this.textGetter = textGetter;
		this.sliderWidget.setMessage(textGetter.apply((float) FloatSliderEntry.this.value.get()));
	}

	@Override
	public Float getValue() {
		return (float) value.get();
	}

	@Deprecated
	public void setValue(double value) {
		sliderWidget.setValue((Mth.clamp(value, minimum, maximum) - minimum) / (double) Math.abs(maximum - minimum));
		this.value.set(Math.min(Math.max(value, minimum), maximum));
		sliderWidget.updateMessage();
	}

	@Override
	public boolean isEdited() {
		return super.isEdited() || getValue() != original;
	}

	@Override
	public Optional<Float> getDefaultValue() {
		return defaultValue == null ? Optional.empty() : Optional.ofNullable(defaultValue.get());
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return widgets;
	}

	public FloatSliderEntry setMaximum(float maximum) {
		this.maximum = maximum;
		return this;
	}

	public FloatSliderEntry setMinimum(float minimum) {
		this.minimum = minimum;
		return this;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
		super.extractRenderState(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
		Window window = Minecraft.getInstance().getWindow();
		this.resetButton.active = isEditable() && getDefaultValue().isPresent() && defaultValue.get() != value.get();
		this.resetButton.setY(y);
		this.sliderWidget.active = isEditable();
		this.sliderWidget.setY(y);
		Component displayedFieldName = getDisplayedFieldName();
		if (textRenderer.isBidirectional()) {
			context.text(textRenderer, displayedFieldName, window.getGuiScaledWidth() - x - textRenderer.width(displayedFieldName), y + 5, getPreferredTextColor());
			this.resetButton.setX(x);
			this.sliderWidget.setX(x + resetButton.getWidth() + 1);
		} else {
			context.text(textRenderer, displayedFieldName, x, y + 5, getPreferredTextColor());
			this.resetButton.setX(x + entryWidth - resetButton.getWidth());
			this.sliderWidget.setX(x + entryWidth - 150);
		}
		this.sliderWidget.setWidth(150 - resetButton.getWidth() - 2);
		resetButton.extractRenderState(context, mouseX, mouseY, delta);
		sliderWidget.extractRenderState(context, mouseX, mouseY, delta);
	}

	@Override
	public List<? extends NarratableEntry> narratables() {
		return this.widgets;
	}

	private class Slider extends AbstractSliderButton {
		protected Slider(int x, int y, int width, int height, double value) {
			super(x, y, width, height, GameNarrator.NO_TITLE, value);
		}

		@Override
		public void updateMessage() {
			setMessage(textGetter.apply((float) FloatSliderEntry.this.value.get()));
		}

		@Override
		protected void applyValue() {
			float val = Math.round(value * 100) / 100F;
			FloatSliderEntry.this.value.set(minimum + Math.abs(maximum - minimum) * val);
		}

		@Override
		public boolean keyPressed(KeyEvent event) {
			if (!isEditable())
				return false;
			return super.keyPressed(event);
		}

		@Override
		public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
			if (!isEditable())
				return false;
			return super.mouseDragged(event, deltaX, deltaY);
		}

		public void setValue(double value) {
			this.value = value;
		}
	}

}
