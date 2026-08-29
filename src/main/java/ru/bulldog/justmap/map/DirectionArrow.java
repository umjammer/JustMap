package ru.bulldog.justmap.map;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;
import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.render.RenderUtil;

public class DirectionArrow {

	private static final Identifier ARROW =
			Identifier.fromNamespaceAndPath(JustMap.MODID, "textures/icon/player_arrow.png");

	private DirectionArrow() {}

	public static void draw(GuiGraphicsExtractor context, double x, double y, int size, float rotation) {
		if (!ClientSettings.simpleArrow) {
			float half = size / 2f;

			Matrix3x2fStack matrices = context.pose();
			matrices.pushMatrix();
			matrices.translate((float) x, (float) y);
			matrices.rotate((float) Math.toRadians(rotation + 180));

			RenderUtil.drawTexture(context, ARROW, -half, -half, size, size);

			matrices.popMatrix();
		} else {
			int l = 6;
			double a1 = Math.toRadians((rotation + 90) % 360);
			double a2 = Math.toRadians((rotation - 45) % 360);
			double a3 = Math.toRadians((rotation + 225) % 360);

			double x1 = x + Math.cos(a1) * l;
			double y1 = y + Math.sin(a1) * l;
			double x2 = x + Math.cos(a2) * l;
			double y2 = y + Math.sin(a2) * l;
			double x3 = x + Math.cos(a3) * l;
			double y3 = y + Math.sin(a3) * l;

			RenderUtil.drawTriangle(context, x1, y1, x2, y2, x3, y3, Colors.RED);
		}
	}
}
