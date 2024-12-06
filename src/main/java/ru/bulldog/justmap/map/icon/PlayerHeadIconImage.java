package ru.bulldog.justmap.map.icon;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.PlayerSkinTextureDownloader;
import net.minecraft.client.texture.ReloadableTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;
import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.client.config.ClientSettings;
import ru.bulldog.justmap.map.MapPlayer;
import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.render.RenderUtil;

public class PlayerHeadIconImage {

	public long lastCheck;
	public final int delay = 5000;
	public boolean success = false;

	private ResourceTexture playerSkin;
	private Identifier skinId;

	public void draw(DrawContext context, double x, double y) {
		// Draw other players
		int size = ClientSettings.entityIconSize;
		this.draw(context, x, y, size, ClientSettings.showIconsOutline);
	}

	public void draw(DrawContext context, double x, double y, int size, boolean outline) {
		double drawX = x - size / 2;
		double drawY = y - size / 2;
		if (outline) {
			double thickness = ClientSettings.entityOutlineSize;
			RenderUtil.fill(context.getMatrices(), drawX - thickness / 2, drawY - thickness / 2, size + thickness, size + thickness, Colors.LIGHT_GRAY);
		}
		RenderUtil.bindTexture(this.skinId);
		RenderUtil.drawPlayerHead(context, drawX, drawY, size, size);
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

		Identifier defaultSkin = DefaultSkinHelper.getSkinTextures(player.getUuid()).texture();
		if (!player.getSkinTextures().texture().equals(defaultSkin)) {
			ResourceTexture skinTexture = loadSkinTexture(player.getSkinTextures().texture(), player.getName().getString(), player.getUuid());
			if (skinTexture != this.playerSkin) {
				if (this.playerSkin != null) {
					this.playerSkin.clearGlId();
				}
				this.playerSkin = skinTexture;
				this.skinId = player.getSkinTextures().texture();

				try {
					this.playerSkin.loadContents(MinecraftClient.getInstance().getResourceManager());
				} catch (IOException ex) {
					JustMap.LOGGER.warning(ex.getLocalizedMessage());
				}
				this.success = true;
			}
		} else if (this.playerSkin == null) {
			this.playerSkin = new ResourceTexture(defaultSkin);
			this.skinId = defaultSkin;
			this.success = false;

			try {
				this.playerSkin.loadContents(MinecraftClient.getInstance().getResourceManager());
			} catch (IOException ex) {
				JustMap.LOGGER.warning(ex.getLocalizedMessage());
			}
		}
	}

	private ResourceTexture loadSkinTexture(Identifier id, String playerName, UUID playerUUID) {
		TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
		ResourceTexture resourceTexture = null;
		AbstractTexture abstractTexture = textureManager.getTexture(id);
		if (abstractTexture == null) {
			// TODO 1.21.4
			GameProfile gameProfile = MinecraftClient.getInstance().player.getGameProfile();
			CompletableFuture<Optional<SkinTextures>> downloader = MinecraftClient.getInstance().getSkinProvider().fetchSkinTextures(gameProfile);
			try {
				skinId = downloader.getNow(Optional.of(DefaultSkinHelper.getSkinTextures(playerUUID))).orElseThrow().texture();
				resourceTexture = new ResourceTexture(skinId);
				textureManager.registerTexture(id, resourceTexture);
			} catch (NoSuchElementException e) {
				JustMap.LOGGER.warning(e.getLocalizedMessage(), null, e);
			}
		}
		return resourceTexture;
	}
}
