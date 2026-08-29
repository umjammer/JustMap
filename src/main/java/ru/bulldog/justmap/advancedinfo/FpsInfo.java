package ru.bulldog.justmap.advancedinfo;

import net.minecraft.client.Minecraft;
import ru.bulldog.justmap.client.config.ClientSettings;

public class FpsInfo extends InfoText {

	public FpsInfo() {
		super("FPS: 00 fps");
	}

	@Override
	public void updateOnTick() {
		this.setVisible(ClientSettings.showFPS);
		Minecraft minecraft = Minecraft.getInstance();
		if (visible) {
			this.setText("FPS: " + minecraft.getFps() + " fps");
		}
	}
}
