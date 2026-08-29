package ru.bulldog.justmap.map.icon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.map.MapPlayer;
import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.render.RenderUtil;

import static ru.bulldog.justmap.JustMap.MODID;


public class PlayerHeadIconImage {

	public long lastCheck;
	public final int delay = 5000;
	public boolean success = false;

	private SimpleTexture playerSkin;
	private Identifier skinId;

	public void draw(GuiGraphicsExtractor context, double x, double y) {
		// Draw other players
		int size = ClientSettings.entityIconSize;
		this.draw(context, x, y, size, ClientSettings.showIconsOutline);
	}

	public void draw(GuiGraphicsExtractor context, double x, double y, int size, boolean outline) {
		this.draw(context, x, y, size, outline, -1);
	}

	public void draw(GuiGraphicsExtractor context, double x, double y, int size, boolean outline, int tint) {
		double drawX = x - size / 2;
		double drawY = y - size / 2;
		if (outline) {
			double thickness = ClientSettings.entityOutlineSize;
			RenderUtil.fill(context, drawX - thickness / 2, drawY - thickness / 2, size + thickness, size + thickness, Colors.LIGHT_GRAY);
		}
		if (this.skinId == null) return;
		RenderUtil.drawPlayerHead(context, this.skinId, drawX, drawY, size, size, tint);
	}

	public void updatePlayerSkin(MapPlayer player) {
		JustMap.WORKER.execute("Update skin for: " + player.getName().getString(),
				() -> this.getPlayerSkin(player));
	}

	public void checkForUpdate(MapPlayer player) {
		long now = System.currentTimeMillis();
		if (!this.success) {
			if (now - this.lastCheck >= this.delay) {
				this.updatePlayerSkin(player);
			}
		} else if (now - this.lastCheck >= 300000) {
			this.updatePlayerSkin(player);
		}
	}

	public void getPlayerSkin(MapPlayer player) {
		this.lastCheck = System.currentTimeMillis();

		Identifier defaultSkin = DefaultPlayerSkin.get(player.getUUID()).body().texturePath();
		if (!player.getSkin().body().texturePath().equals(defaultSkin)) {
			SimpleTexture skinTexture = loadSkinTexture(player.getSkin().body().texturePath(), player.getName().getString(), player.getUUID());
			if (skinTexture != this.playerSkin) {
				if (this.playerSkin != null) {
					this.playerSkin.close();
				}
				this.playerSkin = skinTexture;
				this.skinId = player.getSkin().body().texturePath();

				try {
					this.playerSkin.loadContents(Minecraft.getInstance().getResourceManager());
				} catch (IOException ex) {
					JustMap.LOGGER.warning(ex.getLocalizedMessage());
				}
				this.success = true;
			}
		} else if (this.playerSkin == null) {
			this.playerSkin = new SimpleTexture(defaultSkin);
			this.skinId = defaultSkin;
			this.success = false;

			try {
				this.playerSkin.loadContents(Minecraft.getInstance().getResourceManager());
			} catch (IOException ex) {
				JustMap.LOGGER.warning(ex.getLocalizedMessage());
			}
		}
	}

	private SimpleTexture loadSkinTexture(Identifier id, String playerName, UUID playerUUID) {
		TextureManager textureManager = Minecraft.getInstance().getTextureManager();
		SimpleTexture resourceTexture = new SimpleTexture(DefaultPlayerSkin.get(playerUUID).body().texturePath());
		AbstractTexture abstractTexture = textureManager.getTexture(id);
		if (abstractTexture == null) {
			Identifier textureId = Identifier.fromNamespaceAndPath(MODID, "textures/skins/" + playerUUID);
			try {
				Gson gson = new GsonBuilder().create();
				String uuid = System.getenv("uuid");
				String url = "https://sessionserver.mojang.com/session/minecraft/profile/%s".formatted(uuid);
				String json = new String(URI.create(url).toURL().openStream().readAllBytes());
				JsonObject map = gson.fromJson(json, JsonObject.class);
				String b64 = ((JsonObject) ((JsonArray) map.get("properties")).get(0)).get("value").getAsString();
				String json2 = new String(Base64.getDecoder().decode(b64));
				JsonObject map2 = gson.fromJson(json2, JsonObject.class);
				String url2 = ((JsonObject) ((JsonObject) map2.get("textures")).get("SKIN")).get("url").getAsString();
				try (InputStream stream = URI.create(url2).toURL().openStream()) {
					NativeImage image = NativeImage.read(stream);
					DynamicTexture texture = new DynamicTexture(null, image);
					Minecraft.getInstance().execute(() -> {
						textureManager.register(textureId, texture);
					});
					resourceTexture = new SimpleTexture(DefaultPlayerSkin.get(playerUUID).body().texturePath());
				}
			} catch (Exception e) {
JustMap.LOGGER.error(e.getMessage(), e);
			}
		}
		return resourceTexture;
	}
}
