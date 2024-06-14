package ru.bulldog.justmap.map.minimap.skin;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.map.minimap.Minimap;
import ru.bulldog.justmap.util.ImageUtil;
import ru.bulldog.justmap.util.render.Image;
import ru.bulldog.justmap.util.render.RenderUtil;

@Environment(EnvType.CLIENT)
public class MapSkin extends Image {
	public enum SkinType {
		UNIVERSAL,
		SQUARE,
		ROUND
	}

	private final static List<MapSkin> SKINS = new ArrayList<>();
	private final static TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();

	private final RenderData renderData;

	public final SkinType type;
	public final int id, border;
	public final boolean resizable;
	public final boolean repeating;
	public final String name;

	private MapSkin(int id, String name, SkinType type, Identifier texture, int w, int h, int border, boolean resize, boolean repeat) {
		this(id, name, type, texture, ImageUtil.loadImage(texture, w, h), w, h, border, resize, repeat);
	}

	private MapSkin(int id, String name, SkinType type, Identifier texture, NativeImage image, int w, int h, int border, boolean resize, boolean repeat) {
		super(texture, image);

		this.id = id;
		this.type = type;
		this.border = border;
		this.resizable = resize;
		this.repeating = repeat;
		this.name = name;
		this.width = w;
		this.height = h;

		this.renderData = new RenderData();
	}

	public static void addSkin(SkinType type, String name, Identifier texture, int w, int h, int border, boolean resizable, boolean repeat) {
		int id = SKINS.size();
		SKINS.add(id, new MapSkin(id, name, type, texture, w, h, border, resizable, repeat));
	}

	public static void addSkin(SkinType type, String name, Identifier texture, NativeImage image, int w, int h, int border, boolean resizable, boolean repeat) {
		int id = SKINS.size();
		SKINS.add(id, new MapSkin(id, name, type, texture, image, w, h, border, resizable, repeat));
	}

	public static void addSquareSkin(String name, Identifier texture, int w, int h, int border, boolean resizable, boolean repeat) {
		addSkin(SkinType.SQUARE, name, texture, w, h, border, resizable, repeat);
	}

	public static void addSquareSkin(String name, Identifier texture, NativeImage image, int w, int h, int border, boolean resizable, boolean repeat) {
		addSkin(SkinType.SQUARE, name, texture, image, w, h, border, resizable, repeat);
	}

	public static void addSquareSkin(String name, Identifier texture, int w, int h, int border, boolean resizable) {
		addSkin(SkinType.SQUARE, name, texture, w, h, border, resizable, false);
	}

	public static void addRoundSkin(String name, Identifier texture, int w, int h, int border) {
		addSkin(SkinType.ROUND, name, texture, w, h, border, true, false);
	}

	public static void addRoundSkin(String name, Identifier texture, NativeImage image, int w, int h, int border) {
		addSkin(SkinType.ROUND, name, texture, image, w, h, border, true, false);
	}

	public static void addUniversalSkin(String name, Identifier texture, int w, int h, int border) {
		addSkin(SkinType.UNIVERSAL, name, texture, w, h, border, false, false);
	}

	public static void addUniversalSkin(String name, Identifier texture, NativeImage image, int w, int h, int border) {
		addSkin(SkinType.UNIVERSAL, name, texture, image, w, h, border, false, false);
	}

	public static MapSkin getSkin(int id) {
		try {
			return SKINS.get(id);
		} catch(IndexOutOfBoundsException ex) {
			JustMap.LOGGER.warning(ex.getMessage());
		}

		return SKINS.get(0);
	}

	private void registerPavedTexture(int w, int h) {
		int border = (int) (this.border * renderData.scaleFactor);
		String pattern = "skin_%d_%dx%d_%d";
		if (Minimap.isRound()) pattern += "_round";
		this.textureId = Identifier.of(JustMap.MODID, String.format(pattern, this.id, w, h, border));
		if (textureManager.getTexture(textureId) == null) {
			NativeImage pavedImage;
			if (Minimap.isRound()) {
				pavedImage = ImageUtil.createRoundSkin(image, w, h, border);
			} else {
				pavedImage = ImageUtil.createSquareSkin(image, w, h, border);
			}
			textureManager.registerTexture(textureId, new NativeImageBackedTexture(pavedImage));
		}
	}

	@Override
	public void draw(DrawContext context, double x, double y, int w, int h) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		float hMult = (float) this.getWidth() / this.getHeight();
		if (resizable || repeating) {
			if (type != SkinType.ROUND && (w > this.getWidth() || h > this.getHeight())) {
				RenderUtil.drawSkin(context, this, x, y, w, h);
			} else {
				RenderUtil.drawImage(context, this, x, y, w, h / hMult);
			}
		} else {
			this.registerPavedTexture(w, h);
			RenderUtil.drawImage(context, this, x, y, w, h);
		}
	}

	public boolean isRound() {
		return this.type == SkinType.ROUND ||
			   this.type == SkinType.UNIVERSAL;
	}

	public boolean isSquare() {
		return this.type != SkinType.ROUND;
	}

	public Text getName() {
		return Text.literal(this.name);
	}

	@Override
	public String toString() {
		return this.getName().getString();
	}

	public static MapSkin getDefaultSkin() {
		return getSkins().get(0);
	}

	public static MapSkin getDefaultSquareSkin() {
		return getSquareSkins().get(0);
	}

	public static MapSkin getSkinByName(String name) {
		for (MapSkin skin : SKINS) {
			if (skin.name.equals(name)) {
				return skin;
			}
		}

		return null;
	}

	public static List<MapSkin> getSkins() {
		if (Minimap.isRound()) {
			return MapSkin.getRoundSkins();
		}
		return MapSkin.getSquareSkins();
	}

	public static List<MapSkin> getSquareSkins() {
		return SKINS.stream().filter(MapSkin::isSquare).toList();
	}

	public static List<MapSkin> getRoundSkins() {
		return SKINS.stream().filter(MapSkin::isRound).toList();
	}

	public static MapSkin getCurrentSkin() {
		return getSkin(ClientSettings.currentSkin);
	}

	public static MapSkin getBigMapSkin() {
		return getSkin(ClientSettings.bigMapSkin);
	}

	public RenderData getRenderData() {
		return this.renderData;
	}

	public final class RenderData {
		public double x, y;
		public double scaleFactor = 1.0;
		public double leftC, rightC;
		public double topC, bottomC;
		public float width, height;
		public float scaledBorder;
		public float hSide, vSide;
		public float leftU, rightU;
		public float topV, bottomV;
		public float tail, tailU;
		public int hSegments, vSegments;
		public float hTail, vTail;
		public float hTailU, vTailV;

		public boolean scaleChanged = false;

		private RenderData() {
			this.updateScale();
		}

		public void updateScale(double scale) {
			if (this.scaleFactor != scale) {
				this.scaleFactor = scale;
				this.scaleChanged = true;
			}
		}

		public void updateScale() {
			this.updateScale(ClientSettings.skinScale);
		}

		public void calculate(double x, double y, float w, float h) {
			int spriteW = MapSkin.this.getWidth();
			int spriteH = MapSkin.this.getHeight();
			int border = MapSkin.this.border;

			float sw = (spriteW * 10) / 16 - border * 2;
			float sTail = (spriteW - border * 2) - sw;
			double right = x + w;
			double bottom = y + h;

			this.x = x;
			this.y = y;
			this.width = w;
			this.height = h;
			this.hSegments = (int) Math.round(w / (spriteW * scaleFactor));
			this.vSegments = (int) Math.round(h / (spriteH * scaleFactor));
			this.scaledBorder = (float) (border * scaleFactor);
			if (MapSkin.this.resizable) {
				this.hSide = w - scaledBorder * 2;
				this.vSide = h - scaledBorder * 2;
			} else {
				this.hSide = (float) ((spriteW - border * 2) * scaleFactor);
				this.vSide = (float) ((spriteH - border * 2) * scaleFactor);
			}
			this.leftC = x + scaledBorder;
			this.rightC = right - scaledBorder;
			this.topC = y + scaledBorder;
			this.bottomC = bottom - scaledBorder;
			this.leftU = (float) border / spriteW;
			this.rightU = (float) (spriteW - border) / spriteW;
			this.topV = (float) border / spriteH;
			this.bottomV = (float) (spriteH - border) / spriteH;
			this.tail = hSide - vSide;
			this.tailU = (border + sTail) / spriteW;
			this.hTail = w - hSegments * hSide - scaledBorder * 2;
			if (hTail < 0) {
				this.hSegments--;
				this.hTail = w - hSegments * hSide - scaledBorder * 2;
			}
			this.vTail = h - vSegments * vSide - scaledBorder * 2;
			if (vTail < 0) {
				this.vSegments--;
				this.vTail = h - vSegments * vSide - scaledBorder * 2;
			}
			this.hTailU = (float) ((border + hTail / scaleFactor) / spriteW);
			this.vTailV = (float) ((border + vTail / scaleFactor) / spriteH);

			this.scaleChanged = false;
		}
	}

	static {
		addSquareSkin("Minecraft Map", Identifier.of(JustMap.MODID, "textures/skin/skin_def_map.png"), 64, 64, 5, false, true);
		addSquareSkin("Minecraft LaF", Identifier.of(JustMap.MODID, "textures/skin/skin_gui_laf.png"), 64, 64, 3, false, true);
		addSquareSkin("Minecraft Gui", Identifier.of(JustMap.MODID, "textures/skin/skin_def_gui.png"), 64, 64, 5, true);
		addSquareSkin("Minecraft Gui Fancy", Identifier.of(JustMap.MODID, "textures/skin/skin_def_gui_fancy.png"), 64, 64, 7, true);
		addSquareSkin("Metal Frame", Identifier.of(JustMap.MODID, "textures/skin/skin_simple_metal.png"), 64, 64, 4, true);
		addSquareSkin("Oak Frame", Identifier.of(JustMap.MODID, "textures/skin/skin_oak.png"), 64, 64, 10, false, true);
		addSquareSkin("Bamboo Frame", Identifier.of(JustMap.MODID, "textures/skin/skin_bamboo.png"), 64, 64, 9, false, true);
		addRoundSkin("Minecraft Gui", Identifier.of(JustMap.MODID, "textures/skin/skin_def_gui_round.png"), 256, 256, 10);
		addRoundSkin("Frame Round", Identifier.of(JustMap.MODID, "textures/skin/skin_frame_round.png"), 256, 256, 12);
		addRoundSkin("Frame Runed", Identifier.of(JustMap.MODID, "textures/skin/skin_runed_round.png"), 256, 256, 19);
		addRoundSkin("Frame Frozen", Identifier.of(JustMap.MODID, "textures/skin/skin_frozen_round.png"), 256, 273, 17);
		addUniversalSkin("Stone", Identifier.of("textures/block/stone.png"), 256, 256, 8);
		addUniversalSkin("Cobblestone", Identifier.of("textures/block/cobblestone.png"), 256, 256, 8);
		addUniversalSkin("Mossy Cobblestone", Identifier.of("textures/block/mossy_cobblestone.png"), 256, 256, 8);
		addUniversalSkin("Andesite", Identifier.of("textures/block/andesite.png"), 256, 256, 8);
		addUniversalSkin("Diorite", Identifier.of("textures/block/diorite.png"), 256, 256, 8);
		addUniversalSkin("Granite", Identifier.of("textures/block/granite.png"), 256, 256, 8);
		addUniversalSkin("Bedrock", Identifier.of("textures/block/bedrock.png"), 256, 256, 8);
		addUniversalSkin("Bricks", Identifier.of("textures/block/bricks.png"), 256, 256, 8);
		addUniversalSkin("Dark Prizmarine", Identifier.of("textures/block/dark_prismarine.png"), 256, 256, 8);
		addUniversalSkin("End Stone", Identifier.of("textures/block/end_stone.png"), 256, 256, 8);
		addUniversalSkin("Glowstone", Identifier.of("textures/block/glowstone.png"), 256, 256, 8);
		addUniversalSkin("Netherrack", Identifier.of("textures/block/netherrack.png"), 256, 256, 8);
		addUniversalSkin("Obsidian", Identifier.of("textures/block/obsidian.png"), 256, 256, 8);
		addUniversalSkin("Purpur", Identifier.of("textures/block/purpur_block.png"), 256, 256, 8);
		addUniversalSkin("Quartz", Identifier.of("textures/block/quartz_block_side.png"), 256, 256, 8);
		addUniversalSkin("Sand", Identifier.of("textures/block/sand.png"), 256, 256, 8);
		addUniversalSkin("Mushroom Inside", Identifier.of("textures/block/mushroom_block_inside.png"), 256, 256, 8);
		addUniversalSkin("Brown Mushroom", Identifier.of("textures/block/brown_mushroom_block.png"), 256, 256, 8);
		addUniversalSkin("Acacia Planks", Identifier.of("textures/block/acacia_planks.png"), 256, 256, 8);
		addUniversalSkin("Birch Planks", Identifier.of("textures/block/birch_planks.png"), 256, 256, 8);
		addUniversalSkin("Oak Planks", Identifier.of("textures/block/oak_planks.png"), 256, 256, 8);
		addUniversalSkin("Dark Oak Planks", Identifier.of("textures/block/dark_oak_planks.png"), 256, 256, 8);
		addUniversalSkin("Jungle Planks", Identifier.of("textures/block/jungle_planks.png"), 256, 256, 8);
		addUniversalSkin("Spruce Planks", Identifier.of("textures/block/spruce_planks.png"), 256, 256, 8);
		addUniversalSkin("Nether Bricks", Identifier.of("textures/block/nether_bricks.png"), 256, 256, 8);
		addUniversalSkin("Lava", Identifier.of("textures/block/lava_still.png"), 256, 256, 8);
		addUniversalSkin("Water", Identifier.of("textures/block/water_still.png"), 256, 256, 8);
		addUniversalSkin("Nether Portal", Identifier.of("textures/block/nether_portal.png"), 256, 256, 8);
		addUniversalSkin("Blue Ice", Identifier.of("textures/block/blue_ice.png"), 256, 256, 8);
		addUniversalSkin("Bone Block", Identifier.of("textures/block/bone_block_side.png"), 256, 256, 8);
		addUniversalSkin("Brain Coral", Identifier.of("textures/block/brain_coral_block.png"), 256, 256, 8);
		addUniversalSkin("Bubble Coral", Identifier.of("textures/block/bubble_coral_block.png"), 256, 256, 8);
		addUniversalSkin("Fire Coral", Identifier.of("textures/block/fire_coral_block.png"), 256, 256, 8);
		addUniversalSkin("Horn Coral", Identifier.of("textures/block/horn_coral_block.png"), 256, 256, 8);
		addUniversalSkin("Tube Coral", Identifier.of("textures/block/tube_coral_block.png"), 256, 256, 8);
		addUniversalSkin("Blue Wool", Identifier.of("textures/block/blue_wool.png"), 256, 256, 8);
		addUniversalSkin("Brown Wool", Identifier.of("textures/block/brown_wool.png"), 256, 256, 8);
		addUniversalSkin("Cyan Wool", Identifier.of("textures/block/cyan_wool.png"), 256, 256, 8);
		addUniversalSkin("Green Wool", Identifier.of("textures/block/green_wool.png"), 256, 256, 8);
		addUniversalSkin("Light Blue Wool", Identifier.of("textures/block/light_blue_wool.png"), 256, 256, 8);
		addUniversalSkin("Lime Wool", Identifier.of("textures/block/lime_wool.png"), 256, 256, 8);
		addUniversalSkin("Magenta Wool", Identifier.of("textures/block/magenta_wool.png"), 256, 256, 8);
		addUniversalSkin("Orange Wool", Identifier.of("textures/block/orange_wool.png"), 256, 256, 8);
		addUniversalSkin("Pink Wool", Identifier.of("textures/block/pink_wool.png"), 256, 256, 8);
		addUniversalSkin("Purple Wool", Identifier.of("textures/block/purple_wool.png"), 256, 256, 8);
		addUniversalSkin("Red Wool", Identifier.of("textures/block/red_wool.png"), 256, 256, 8);
		addUniversalSkin("White Wool", Identifier.of("textures/block/white_wool.png"), 256, 256, 8);
		addUniversalSkin("Yellow Wool", Identifier.of("textures/block/yellow_wool.png"), 256, 256, 8);
		addUniversalSkin("Magma", Identifier.of("textures/block/magma.png"), 256, 256, 8);
		addUniversalSkin("Mycelium", Identifier.of("textures/block/mycelium_top.png"), 256, 256, 8);
		addUniversalSkin("Podzol", Identifier.of("textures/block/podzol_top.png"), 256, 256, 8);
		addUniversalSkin("Nether Wart", Identifier.of("textures/block/nether_wart_block.png"), 256, 256, 8);
		addUniversalSkin("Sponge", Identifier.of("textures/block/sponge.png"), 256, 256, 8);

		SkinLoader.loadSkins();
	}
}
