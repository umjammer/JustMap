package ru.bulldog.justmap.util.colors;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import javax.imageio.ImageIO;

import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientDynamicRegistryType;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.mixins.BiomeColorsAccessor;
import ru.bulldog.justmap.server.JustMapServer;
import ru.bulldog.justmap.util.storage.ResourceLoader;

public class BiomeColors {
	private static int[] foliageMap;
	private static int[] grassMap;

	private static final DynamicRegistryManager registryManager =
			ClientDynamicRegistryType.createCombinedDynamicRegistries().getCombinedRegistryManager();

	private Biome biome;
	private Optional<Integer> foliageColor;
	private Optional<Integer> grassColor;
	private int waterColor;

	private BiomeColors() {}

	public BiomeColors(Biome biome) {
		this.biome = biome;
		BiomeColorsAccessor accessor = (BiomeColorsAccessor) biome.getEffects();
		this.foliageColor = accessor.getFoliageColor();
		this.grassColor = accessor.getGrassColor();
		this.waterColor = accessor.getWaterColor();
	}

	public int getWaterColor() {
		return this.waterColor;
	}

	public int getFoliageColor() {
		return this.biome.getFoliageColor();
	}

	public int getGrassColor(int x, int z) {
		return this.biome.getGrassColorAt(x, z);
	}

	public static Identifier getBiomeId(World world, Biome biome) {
		Identifier biomeId = world.getRegistryManager().getOrThrow(RegistryKeys.BIOME).getId(biome);
		return biomeId != null ? biomeId : BuiltinRegistries.createWrapperLookup().getOrThrow(RegistryKeys.BIOME).streamEntries().filter(b -> biome.equals(b.value())).map(b -> b.registryKey().getValue()).findFirst().get();
	}

	public static Registry<Biome> getBiomeRegistry() {
		if (JustMap.getSide() == EnvType.CLIENT) {
			MinecraftClient minecraft = MinecraftClient.getInstance();
			ClientPlayNetworkHandler networkHandler = minecraft.getNetworkHandler();
			if (networkHandler != null) {
				return minecraft.getNetworkHandler().getRegistryManager().getOrThrow(RegistryKeys.BIOME);
			}
			return registryManager.getOrThrow(RegistryKeys.BIOME);
		}
		MinecraftServer server = JustMapServer.getServer();
		if (server != null) {
			return server.getRegistryManager().getOrThrow(RegistryKeys.BIOME);
		}
		return registryManager.getOrThrow(RegistryKeys.BIOME);
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
		BiomeColorsAccessor accessor = (BiomeColorsAccessor) biome.getEffects();
		biomeColors.biome = biome;
		if (json.has("foliage")) {
			String hexColor = JsonHelper.getString(json, "foliage");
			biomeColors.foliageColor = Optional.of(ColorUtil.parseHex(hexColor));
		} else {
			biomeColors.foliageColor = accessor.getFoliageColor();
		}
		if (json.has("grass")) {
			String hexColor = JsonHelper.getString(json, "grass");
			biomeColors.grassColor = Optional.of(ColorUtil.parseHex(hexColor));
		} else {
			biomeColors.grassColor = accessor.getGrassColor();
		}
		if (json.has("water")) {
			String hexColor = JsonHelper.getString(json, "water");
			biomeColors.waterColor = ColorUtil.parseHex(hexColor);
		} else {
			biomeColors.waterColor = accessor.getWaterColor();
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
