package ru.bulldog.justmap.map.data;

import net.minecraft.client.gui.DrawContext;

public interface MapRegion {
	RegionPos getPos();

	void drawLayer(DrawContext context, Layer layer, int level, double x, double y, double width, double height, int imgX, int imgY, int imgW, int imgH);
}
