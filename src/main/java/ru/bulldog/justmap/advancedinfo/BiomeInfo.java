package ru.bulldog.justmap.advancedinfo;

import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.enums.TextAlignment;
import ru.bulldog.justmap.util.CurrentWorldPos;
import ru.bulldog.justmap.util.colors.BiomeColors;

public class BiomeInfo extends InfoText {

	private final String title;
	private Identifier currentBiome;

	public BiomeInfo() {
		super("Void");
		this.title = "Biome: ";
	}

	public BiomeInfo(TextAlignment alignment, String title) {
		super(alignment, "Void");
		this.title = title;
	}

	@Override
	public void updateOnTick() {
		this.setVisible(ClientSettings.showBiome);
		Minecraft minecraft = Minecraft.getInstance();
		if (visible && minecraft.level != null) {
			Level world = minecraft.level;
			Biome biome = world.getBiome(CurrentWorldPos.currentPos()).value();
			Identifier biomeId = BiomeColors.getBiomeId(world, biome);
			if (biomeId != null && !biomeId.equals(currentBiome)) {
				this.currentBiome = biomeId;
				this.setText(title + this.getTranslation());
			} else if (biomeId == null) {
				this.setText(title + "Unknown");
			}
		}
	}

	private String getTranslation() {
		return I18n.get(Util.makeDescriptionId("biome", currentBiome));
	}
}
