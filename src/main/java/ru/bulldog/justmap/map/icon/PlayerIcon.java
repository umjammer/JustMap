package ru.bulldog.justmap.map.icon;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3x2fStack;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.client.render.EntityModelRenderer;
import ru.bulldog.justmap.map.MapPlayerManager;
import ru.bulldog.justmap.util.CurrentWorldPos;
import ru.bulldog.justmap.util.colors.ColorUtil;
import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.render.RenderUtil;

public class PlayerIcon extends MapIcon<PlayerIcon> {

	private final Player player;
	private final int color = Colors.GREEN;

	public PlayerIcon(Player player) {
		this.player = player;
	}

	public double getX() {
		return this.player.getX();
	}

	public double getY() {
		return this.player.getY();
	}

	public double getZ() {
		return this.player.getZ();
	}

	public void draw(GuiGraphicsExtractor context, int size) {
		double x = this.x - size / 2;
		double y = this.y - size / 2;
		if (ClientSettings.showPlayerHeads) {
			MapPlayerManager.getPlayer(player).getIcon().draw(context, x, y, size, true);
		} else {
			int darken = ColorUtil.colorBrigtness(color, -3);
			RenderUtil.fill(context, x - 0.5, y - 0.5, size + 1, size + 1, darken);
			RenderUtil.fill(context, x, y, size, size, color);
		}
		this.drawPlayerName(context, x, y);
	}

	@Override
	public void draw(GuiGraphicsExtractor context, int mapX, int mapY, int mapW, int mapH, float rotation) {
		int size = ClientSettings.entityIconSize;
		this.updatePos(mapX, mapY, mapW, mapH, size);
		if (!allowRender) return;
		if (ClientSettings.renderEntityModel) {
			EntityModelRenderer.renderModel(context, player, iconPos.x, iconPos.y);
		} else if (ClientSettings.showPlayerHeads) {
			MapPlayerManager.getPlayer(player).getIcon().draw(context, iconPos.x, iconPos.y,
					ClientSettings.entityIconSize, ClientSettings.showIconsOutline, EntityIcon.shadingTint(height));
		} else {
			int darken = ColorUtil.colorBrigtness(color, -3);
			RenderUtil.fill(context, iconPos.x - 0.5, iconPos.y - 0.5, size + 1, size + 1, darken);
			RenderUtil.fill(context, iconPos.x, iconPos.y, size, size, color);
		}
		this.drawPlayerName(context, iconPos.x, iconPos.y);
	}

	private void drawPlayerName(GuiGraphicsExtractor context, double x, double y) {
		if (!ClientSettings.showPlayerNames) return;
		Minecraft minecraft = Minecraft.getInstance();
		Window window = minecraft.getWindow();
		double sf = window.getGuiScale();
		float scale = (float) (1.0 / sf);
		Matrix3x2fStack matrices = context.pose();
		matrices.pushMatrix();
		if (sf > 1.0 && !minecraft.options.forceUnicodeFont().get()) {
			matrices.scale(scale, scale);
			matrices.translate((float) (x * (sf - 1)), (float) (y * (sf - 1)));
		}
		RenderUtil.drawCenteredText(context, player.getName(), x, y + 12, Colors.WHITE);
		matrices.popMatrix();
	}
}
