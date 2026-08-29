package ru.bulldog.justmap.map.data;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface MapRegion {
	RegionPos getPos();

	void drawLayer(GuiGraphicsExtractor context, Layer layer, int level, double x, double y, double width, double height, int imgX, int imgY, int imgW, int imgH);
}
