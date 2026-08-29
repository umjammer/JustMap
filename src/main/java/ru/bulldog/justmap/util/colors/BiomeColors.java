package ru.bulldog.justmap.util.colors;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import javax.imageio.ImageIO;

import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientRegistryLayer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.server.JustMapServer;
import ru.bulldog.justmap.util.storage.ResourceLoader;

public class BiomeColors {
	private static int[] foliageMap;
	private static int[] grassMap;

	private static final RegistryAccess registryManager =
			ClientRegistryLayer.createRegistryAccess().compositeAccess();

	private Biome biome;
	private Optional<Integer> foliageColor;
	private Optional<Integer> grassColor;
	private int waterColor;

	private BiomeColors() {}

	public BiomeColors(Biome biome) {
		this.biome = biome;
		BiomeSpecialEffects effects = biome.getSpecialEffects();
		this.foliageColor = effects.foliageColorOverride();
		this.grassColor = effects.grassColorOverride();
		this.waterColor = effects.waterColor();
	}

	public int getWaterColor() {
		return this.waterColor;
	}

	public int getFoliageColor() {
		return this.biome.getFoliageColor();
	}

	public int getGrassColor(int x, int z) {
		return this.biome.getGrassColor(x, z);
	}

	public static Identifier getBiomeId(Level world, Biome biome) {
		Identifier biomeId = world.registryAccess().lookupOrThrow(Registries.BIOME).getKey(biome);
		return biomeId != null ? biomeId : VanillaRegistries.createLookup().lookupOrThrow(Registries.BIOME).listElements().filter(b -> biome.equals(b.value())).map(b -> b.key().identifier()).findFirst().get();
	}

	public static Registry<Biome> getBiomeRegistry() {
		if (JustMap.getSide() == EnvType.CLIENT) {
			Minecraft minecraft = Minecraft.getInstance();
			ClientPacketListener networkHandler = minecraft.getConnection();
			if (networkHandler != null) {
				return minecraft.getConnection().registryAccess().lookupOrThrow(Registries.BIOME);
			}
			return registryManager.lookupOrThrow(Registries.BIOME);
		}
		MinecraftServer server = JustMapServer.getServer();
		if (server != null) {
			return server.registryAccess().lookupOrThrow(Registries.BIOME);
		}
		return registryManager.lookupOrThrow(Registries.BIOME);
	}

	public static int getGrassColor(double temperature, double humidity) {
		humidity *= temperature;
		int t = (int) ((1.0D - temperature) * 255.0D);
		int h = (int) ((1.0D - humidity) * 255.0D);
		int k = h << 8 | t;
		if (k < 0 || k > grassMap.length) return Colors.GRASS;
		return grassMap[k];
	}

	public static int defaultGrassColor() {
		return getGrassColor(0.5, 1.0);
	}

	public static int getFoliageColor(double temperature, double humidity) {
		humidity *= temperature;
		int t = (int) ((1.0D - temperature) * 255.0D);
		int h = (int) ((1.0D - humidity) * 255.0D);
		int k = h << 8 | t;
		if (k < 0 || k > foliageMap.length) return Colors.FOLIAGE;
		return foliageMap[k];
	}

	public static int defaultFoliageColor() {
		return getFoliageColor(0.5, 1.0);
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		if (foliageColor.isPresent()) {
			json.addProperty("foliage", Integer.toHexString(foliageColor.get()));
		}
		if (grassColor.isPresent()) {
			json.addProperty("grass", Integer.toHexString(grassColor.get()));
		}
		json.addProperty("water", Integer.toHexString(waterColor));

		return json;
	}

	public static BiomeColors fromJson(Biome biome, JsonObject json) {
		BiomeColors biomeColors = new BiomeColors();
		BiomeSpecialEffects effects = biome.getSpecialEffects();
		biomeColors.biome = biome;
		if (json.has("foliage")) {
			String hexColor = GsonHelper.getAsString(json, "foliage");
			biomeColors.foliageColor = Optional.of(ColorUtil.parseHex(hexColor));
		} else {
			biomeColors.foliageColor = effects.foliageColorOverride();
		}
		if (json.has("grass")) {
			String hexColor = GsonHelper.getAsString(json, "grass");
			biomeColors.grassColor = Optional.of(ColorUtil.parseHex(hexColor));
		} else {
			biomeColors.grassColor = effects.grassColorOverride();
		}
		if (json.has("water")) {
			String hexColor = GsonHelper.getAsString(json, "water");
			biomeColors.waterColor = ColorUtil.parseHex(hexColor);
		} else {
			biomeColors.waterColor = effects.waterColor();
		}

		return biomeColors;
	}

	public String toString() {
		return "[" + "foliage=" + foliageColor +
				"," + "grass=" + grassColor +
				"," + "water=" + waterColor +
				"]";
	}

	static {
		ResourceLoader foliageColors = new ResourceLoader("textures/colormap/foliage.png");
		try (InputStream ins = foliageColors.getInputStream()) {
			BufferedImage image = ImageIO.read(ins);
			int width = image.getWidth();
			int height = image.getHeight();
			foliageMap = new int[width * height];
			image.getRGB(0, 0, width, height, foliageMap, 0, width);
		} catch (IOException ex) {
			JustMap.LOGGER.error("Can't load foliage colors texture!");
		}
		ResourceLoader grassColors = new ResourceLoader("textures/colormap/grass.png");
		try (InputStream ins = grassColors.getInputStream()) {
			BufferedImage image = ImageIO.read(ins);
			int width = image.getWidth();
			int height = image.getHeight();
			grassMap = new int[width * height];
			image.getRGB(0, 0, width, height, grassMap, 0, width);
		} catch (IOException ex) {
			JustMap.LOGGER.error("Can't load grass colors texture!");
		}
	}
}
