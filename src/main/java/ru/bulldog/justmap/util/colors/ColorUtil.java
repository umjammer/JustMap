package ru.bulldog.justmap.util.colors;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.util.BlockStateUtil;
import ru.bulldog.justmap.util.ImageUtil;
import ru.bulldog.justmap.util.math.MathUtil;

@Environment(EnvType.CLIENT)
public class ColorUtil {

	private static final Minecraft minecraft = Minecraft.getInstance();
	private static final float[] floatBuffer = new float[3];
	private static final ColorProviders colorProvider = ColorProviders.INSTANCE;
	private static final Colors colorPalette = Colors.INSTANCE;

	/**
	 * The baked block models, looked up on demand: since 26.2 asking for them before the
	 * first resource reload throws, and colours are first needed while the map loads.
	 */
	private static BlockStateModelSet blockModels() {
		return minecraft.getModelManager().getBlockStateModelSet();
	}

	private static BlockAndTintGetter tintGetter(Level world) {
		return world instanceof BlockAndTintGetter tintGetter ? tintGetter : null;
	}

	private static int averageWaterColor(Level world, BlockPos pos) {
		BlockAndTintGetter tintGetter = tintGetter(world);
		return tintGetter != null ? BiomeColors.getAverageWaterColor(tintGetter, pos)
								  : colorProvider.getWaterColor(world, pos);
	}

	public static int[] toIntArray(int color) {
		return new int[] {
			(color >> 24) & 255,
			(color >> 16) & 255,
			(color >> 8) & 255,
			 color & 255
		};
	}

	public static float[] toFloatArray(int color) {
		floatBuffer[0] = ((color >> 16 & 255) / 255.0F);
		floatBuffer[1] = ((color >> 8 & 255) / 255.0F);
		floatBuffer[2] = ((color & 255) / 255.0F);

		return floatBuffer;
	}

	public static void RGBtoHSB(int r, int g, int b, float[] hsbvals) {
		float hue, saturation, brightness;
		if (hsbvals == null) {
			hsbvals = floatBuffer;
		}
		int cmax = Math.max(r, g);
		if (b > cmax) cmax = b;
		int cmin = Math.min(r, g);
		if (b < cmin) cmin = b;

		brightness = ((float) cmax) / 255.0F;
		if (cmax != 0)
			saturation = ((float) (cmax - cmin)) / ((float) cmax);
		else
			saturation = 0;
		if (saturation == 0)
			hue = 0;
		else {
			float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
			float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
			float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
			if (r == cmax)
				hue = bluec - greenc;
			else if (g == cmax)
				hue = 2.0F + redc - bluec;
			else
				hue = 4.0F + greenc - redc;
			hue = hue / 6.0F;
			if (hue < 0)
				hue = hue + 1.0F;
		}
		hsbvals[0] = hue;
		hsbvals[1] = saturation;
		hsbvals[2] = brightness;
	}

	public static int HSBtoRGB(float hue, float saturation, float brightness) {
		int r = 0, g = 0, b = 0;
		if (saturation == 0) {
			r = g = b = (int) (brightness * 255.0F + 0.5F);
		} else {
			float h = (hue - (float)Math.floor(hue)) * 6.0F;
			float f = h - (float)java.lang.Math.floor(h);
			float p = brightness * (1.0F - saturation);
			float q = brightness * (1.0F - saturation * f);
			float t = brightness * (1.0F - (saturation * (1.0F - f)));
			switch ((int) h) {
			case 0:
				r = (int) (brightness * 255.0F + 0.5F);
				g = (int) (t * 255.0F + 0.5F);
				b = (int) (p * 255.0F + 0.5F);
				break;
			case 1:
				r = (int) (q * 255.0F + 0.5F);
				g = (int) (brightness * 255.0F + 0.5F);
				b = (int) (p * 255.0F + 0.5F);
				break;
			case 2:
				r = (int) (p * 255.0F + 0.5F);
				g = (int) (brightness * 255.0F + 0.5F);
				b = (int) (t * 255.0F + 0.5F);
				break;
			case 3:
				r = (int) (p * 255.0F + 0.5F);
				g = (int) (q * 255.0F + 0.5F);
				b = (int) (brightness * 255.0F + 0.5F);
				break;
			case 4:
				r = (int) (t * 255.0F + 0.5F);
				g = (int) (p * 255.0F + 0.5F);
				b = (int) (brightness * 255.0F + 0.5F);
				break;
			case 5:
				r = (int) (brightness * 255.0F + 0.5F);
				g = (int) (p * 255.0F + 0.5F);
				b = (int) (q * 255.0F + 0.5F);
				break;
			}
		}
		return 0xFF000000 | (r << 16) | (g << 8) | (b << 0);
	}

	public static int parseHex(String hexColor) {
		int len = hexColor.length();
		if (len < 6 || len > 8 || len % 2 > 0) {
			return -1;
		}

		int color, shift;
		if (len == 6) {
			color = 0xFF000000; shift = 16;
		} else {
			color = 0; shift = 24;
		}

		try {
			String[] splited = hexColor.split("(?<=\\G.{2})");
			for (String digit : splited) {
				color |= Integer.valueOf(digit, 16) << shift;
				shift -= 8;
			}
		} catch(NumberFormatException ex) {
			JustMap.LOGGER.error(ex.toString());
			JustMap.LOGGER.catching(ex);
			return -1;
		}

		return color;
	}

	public static int toABGR(int color) {
		int r = (color >> 16) & 255;
		int g = (color >> 8) & 255;
		int b = color & 255;
		return 0xFF000000 | b << 16 | g << 8 | r;
	}

	public static int ABGRtoARGB(int color) {
		int a = (color >> 24) & 255;
		int b = (color >> 16) & 255;
		int g = (color >> 8) & 255;
		int r = color & 255;
		return a << 24 | r << 16 | g << 8 | b;
	}

	public static int colorBrigtness(int color, float val) {
		RGBtoHSB((color >> 16) & 255, (color >> 8) & 255, color & 255, floatBuffer);
		floatBuffer[2] += val / 10.0F;
		floatBuffer[2] = MathUtil.clamp(floatBuffer[2], 0.0F, 1.0F);
		return HSBtoRGB(floatBuffer[0], floatBuffer[1], floatBuffer[2]);
	}

	public static int applyTint(int color, int tint) {
		return colorBrigtness(multiplyColor(color, tint), 1.5F);
	}

	private static int extractColor(BlockState state) {
		BlockStateModelSet blockModels = blockModels();
		BlockStateModel model = blockModels.get(state);
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(RandomSource.create(), parts);
		List<BakedQuad> quads = parts.isEmpty() ? List.of() : parts.getFirst().getQuads(Direction.UP);

		Identifier blockSprite;
		if (!quads.isEmpty()) {
			blockSprite = quads.getFirst().materialInfo().sprite().contents().name();
		} else {
			blockSprite = blockModels.getParticleMaterial(state).sprite().contents().name();
		}

		int color = colorPalette.getTextureColor(state, blockSprite);
		if (color != 0x0) return color;

		Identifier texture = Identifier.fromNamespaceAndPath(blockSprite.getNamespace(), String.format("textures/%s.png", blockSprite.getPath()));
		NativeImage image = ImageUtil.loadImage(texture, 16, 16);

		int height = state.getBlock() instanceof FlowerBlock ? image.getHeight() / 2 : image.getHeight();

		List<Integer> colors = new ArrayList<>();
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < height; j++) {
				int col = image.getPixel(i, j);
				if (((col >> 24) & 255) > 0) {
					colors.add(ABGRtoARGB(col));
				}
			}
		}
		image.close();

		if (colors.isEmpty()) return -1;

		ColorExtractor extractor = new ColorExtractor(colors);
		color = extractor.analyze();
		colorPalette.addTextureColor(state, blockSprite, color);

		return color;
	}

	public static int processColor(int color, int heightDiff, float topoLevel) {
		RGBtoHSB((color >> 16) & 255, (color >> 8) & 255, color & 255, floatBuffer);
		floatBuffer[1] += ClientSettings.mapSaturation / 100.0F;
		floatBuffer[1] = MathUtil.clamp(floatBuffer[1], 0.0F, 1.0F);
		floatBuffer[2] += ClientSettings.mapBrightness / 100.0F;
		floatBuffer[2] = MathUtil.clamp(floatBuffer[2], 0.0F, 1.0F);
		if (ClientSettings.showTerrain && heightDiff != 0) {
			floatBuffer[2] += heightDiff / 10.0F;
			floatBuffer[2] = MathUtil.clamp(floatBuffer[2], 0.0F, 1.0F);
		}
		if (ClientSettings.showTopography && topoLevel != 0) {
			floatBuffer[2] += MathUtil.clamp(topoLevel, -0.75F, 0.1F);
			floatBuffer[2] = MathUtil.clamp(floatBuffer[2], 0.0F, 1.0F);
		}
		return HSBtoRGB(floatBuffer[0], floatBuffer[1], floatBuffer[2]);
	}

	private static int processAlternateColor(int blockColor, int textureColor, int defaultColor) {
		blockColor = blockColor == -1 ? defaultColor : blockColor;
		if (blockColor != -1) {
			return multiplyColor(textureColor, blockColor);
		}

		return textureColor;
	}

	public static int getBlockColor(LevelChunk worldChunk, BlockPos pos) {
		Level world = worldChunk.getLevel();
		BlockPos overPos = new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
		BlockState overState = worldChunk.getBlockState(overPos);
		BlockState blockState = worldChunk.getBlockState(pos);

		return getTintedBlockColor(world, pos, blockState, overState);
	}

	private static int getTintedBlockColor(Level world, BlockPos pos, BlockState blockState, BlockState overState) {
		boolean waterTint = ClientSettings.alternateColorRender && ClientSettings.waterTint;
		boolean skipWater = !(ClientSettings.hideWater || waterTint);
		if (!ClientSettings.hideWater && ClientSettings.hidePlants && BlockStateUtil.isSeaweed(overState)) {
			if (waterTint) {
				int color = getBlockColorInner(world, blockState, pos);
				return applyTint(color, averageWaterColor(world, pos));
			}
			return getBlockColorInner(world, Blocks.WATER.defaultBlockState(), pos);
		} else if (!BlockStateUtil.isAir(blockState) && BlockStateUtil.checkState(overState, skipWater, !ClientSettings.hidePlants)) {
			int color = getBlockColorInner(world, blockState, pos);
			if (ClientSettings.hideWater) return color;
			if (waterTint && (BlockStateUtil.isWater(overState) || BlockStateUtil.isWaterlogged(blockState))) {
				return applyTint(color, averageWaterColor(world, pos));
			}
			return color;
		}

		return -1;
	}

	private static int getBlockColorInner(Level world, BlockState blockState, BlockPos pos) {
		if (ClientSettings.alternateColorRender) {
			return getAlternateBlockColor(world, blockState, pos);
		} else {
			return blockState.getMapColor(world, pos).col;
		}
	}

	private static int getAlternateBlockColor(Level world, BlockState blockState, BlockPos pos) {
		int blockColor = colorPalette.getBlockColor(blockState);
		if (blockColor != 0x0) {
			return blockColor;
		}

		blockColor = colorProvider.getColor(blockState, world, pos);
		if (blockColor == -1) {
			BlockTintSource tintSource = minecraft.getBlockColors().getTintSource(blockState, 0);
			if (tintSource != null) {
				BlockAndTintGetter tintGetter = tintGetter(world);
				blockColor = tintGetter != null ? tintSource.colorInWorld(blockState, tintGetter, pos)
												: tintSource.color(blockState);
			}
		}
		int textureColor = extractColor(blockState);

		Block block = blockState.getBlock();
		if (block instanceof VineBlock) {
			blockColor = processAlternateColor(blockColor, textureColor, colorProvider.getFoliageColor(world, pos));
		} else if (block instanceof TallGrassBlock || block instanceof DoublePlantBlock || block instanceof SugarCaneBlock) {
			blockColor = processAlternateColor(blockColor, textureColor, colorProvider.getGrassColor(world, pos));
		} else if (block instanceof LilyPadBlock || block instanceof StemBlock || block instanceof AttachedStemBlock) {
			blockColor = processAlternateColor(blockColor, textureColor, blockState.getMapColor(world, pos).col);
			colorPalette.addBlockColor(blockState, blockColor);
		} else if (block instanceof LiquidBlock) {
			if (BlockStateUtil.isWater(blockState)) {
				blockColor = processAlternateColor(blockColor, textureColor, colorProvider.getWaterColor(world, pos));
			} else {
				blockColor = fluidColor(world, blockState, pos, textureColor);
				colorPalette.addFluidColor(blockState, blockColor);
			}
		} else if (blockColor != -1) {
			blockColor = multiplyColor(textureColor, blockColor);
			if (block.equals(Blocks.BIRCH_LEAVES) || block.equals(Blocks.SPRUCE_LEAVES)) {
				colorPalette.addBlockColor(blockState, blockColor);
			} else if (!(block instanceof LeavesBlock) && !(block instanceof GrassBlock)) {
				colorPalette.addBlockColor(blockState, blockColor);
			}
		} else {
			blockColor = textureColor != -1 ? textureColor : blockState.getMapColor(world, pos).col;
			colorPalette.addBlockColor(blockState, blockColor);
		}

		return blockColor;
	}

	private static int fluidColor(Level world, BlockState state, BlockPos pos, int defColor) {
		int color = colorPalette.getFluidColor(state);
		if (color == 0x0) {
			FluidState fluidState = state.getBlock().getFluidState(state);
			FluidModel fluidModel = minecraft.getModelManager().getFluidStateModelSet().get(fluidState);
			BlockTintSource tintSource = fluidModel.tintSource();
			if (tintSource == null) return defColor;
			BlockAndTintGetter tintGetter = tintGetter(world);
			color = tintGetter != null ? tintSource.colorInWorld(state, tintGetter, pos)
									   : tintSource.color(state);
		}
		return color == -1 ? defColor : color;
	}

	public static int multiplyColor(int color1, int color2) {
		if (color1 == -1) {
			return color2;
		} else if (color2 == -1) {
			return color1;
		} else {
			int alpha = (color1 >>> 24 & 255) * (color2 >>> 24 & 255) / 255;
			int red = (color1 >>> 16 & 255) * (color2 >>> 16 & 255) / 255;
			int green = (color1 >>> 8 & 255) * (color2 >>> 8 & 255) / 255;
			int blue = (color1 & 255) * (color2 & 255) / 255;
			return alpha << 24 | red << 16 | green << 8 | blue;
		}
	}
}
