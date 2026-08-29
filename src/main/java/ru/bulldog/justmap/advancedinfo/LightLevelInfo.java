package ru.bulldog.justmap.advancedinfo;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.util.CurrentWorldPos;

public class LightLevelInfo extends InfoText {

	public LightLevelInfo() {
		super("Light: 0 (Block: 0)");
	}

	@Override
	public void updateOnTick() {
		this.setVisible(ClientSettings.showLight);
		Minecraft minecraft = Minecraft.getInstance();
		if (visible && minecraft.level != null) {
			BlockPos currentPos = CurrentWorldPos.currentPos();
			minecraft.level.updateSkyBrightness();
			int skyLight = minecraft.level.getMaxLocalRawBrightness(currentPos);
			int blockLight = minecraft.level.getBrightness(LightLayer.BLOCK, currentPos);
			this.setText(String.format("Light: %d (Block: %d)", Math.max(skyLight, blockLight), blockLight));
		}
	}
}
