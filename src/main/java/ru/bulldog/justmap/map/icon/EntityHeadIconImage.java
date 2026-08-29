package ru.bulldog.justmap.map.icon;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Monster;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.util.ImageUtil;
import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.render.Image;
import ru.bulldog.justmap.util.render.RenderUtil;
import ru.bulldog.justmap.util.storage.StorageUtil;

public class EntityHeadIconImage extends Image {

	private final static Map<Identifier, EntityHeadIconImage> ICONS = new HashMap<>();
	private final Identifier id;
	private Identifier outlineId;
	private int color = Colors.LIGHT_GRAY;
	private final boolean solid;

	private EntityHeadIconImage(Identifier id, Identifier texture, int w, int h) {
		this(id, texture, ImageUtil.loadImage(texture, w, h));
	}

	private EntityHeadIconImage(Identifier id, Identifier texture, NativeImage image) {
		super(texture, image);

		this.solid = this.isSolid();
		this.id = id;
	}

	public static EntityHeadIconImage getIcon(Entity entity) {
		Identifier id = EntityType.getKey(entity.getType());
		if (ICONS.containsKey(id)) {
			return ICONS.get(id);
		} else {
			File iconsDir = StorageUtil.iconsDir();
			File iconPng = new File(iconsDir, String.format("%s/%s.png", id.getNamespace(), id.getPath()));
			if (iconPng.exists()) {
				return registerIcon(entity, id, iconPng);
			} else {
				Identifier iconId = iconId(id);
				if (ImageUtil.imageExists(iconId)) {
					return registerIcon(entity, id, iconId);
				}
			}
		}

		return null;
	}

	@Override
	public void draw(GuiGraphicsExtractor context, double x, double y, int w, int h) {
		this.draw(context, x, y, w, h, -1);
	}

	@Override
	public void draw(GuiGraphicsExtractor context, double x, double y, int w, int h, int tint) {
		if (ClientSettings.showIconsOutline) {
			double thickness = ClientSettings.entityOutlineSize;
			if (solid) {
				RenderUtil.fill(context, x - thickness / 2, y - thickness / 2, w + thickness, h + thickness, this.color);
			} else {
				RenderUtil.drawTexture(context, this.outlineId(),
						x - thickness / 2, y - thickness / 2, w + thickness, h + thickness, tint);
			}
		}
		RenderUtil.drawTexture(context, this.getId(), x, y, w, h, tint);
	}

	private Identifier outlineId() {
		if (outlineId == null) {
			NativeImage outline = ImageUtil.generateOutline(image, width, height, color);
			DynamicTexture outTexture = new DynamicTexture(null, outline);
			this.outlineId = Identifier.fromNamespaceAndPath(this.id.getNamespace(), "%s_outline".formatted(this.id.getPath()));
			textureManager.register(outlineId, outTexture);
		}
		return this.outlineId;
	}

	private boolean isSolid() {
		NativeImage icon = this.image;

		int width = icon.getWidth();
		int height = icon.getHeight();

		boolean solid = true;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int alpha = (icon.getPixel(i, j) >> 24) & 255;
				solid = alpha > 0;
				if (!solid) break;
			}
		}

		return solid;
	}

	private static Identifier iconId(Identifier id) {
		String path = String.format("textures/minimap/entities/%s.png", id.getPath());
		return Identifier.fromNamespaceAndPath(id.getNamespace(), path);
	}

	private static EntityHeadIconImage registerIcon(Entity entity, Identifier entityId, Identifier texture) {
		EntityHeadIconImage icon = new EntityHeadIconImage(entityId, texture, 32, 32);
		return registerIcon(entity, entityId, icon);
	}

	private static EntityHeadIconImage registerIcon(Entity entity, Identifier entityId, File image) {
		NativeImage iconImage = ImageUtil.loadImage(image, 32, 32);
		Identifier textureId = Identifier.fromNamespaceAndPath("icon_%s".formatted(entityId.getNamespace()), entityId.getPath());
		textureManager.register(textureId, new DynamicTexture(null, iconImage));
		EntityHeadIconImage icon = new EntityHeadIconImage(entityId, textureId, iconImage);
		return registerIcon(entity, entityId, icon);
	}

	private static EntityHeadIconImage registerIcon(Entity entity, Identifier entityId, EntityHeadIconImage icon) {
		if (entity instanceof Monster) {
			icon.color = Colors.DARK_RED;
		} else if (entity instanceof TamableAnimal) {
			TamableAnimal tameable = (TamableAnimal) entity;
			icon.color = tameable.isTame() ? Colors.GREEN : Colors.YELLOW;
		} else {
			icon.color = Colors.YELLOW;
		}

		ICONS.put(entityId, icon);

		return icon;
	}
}
