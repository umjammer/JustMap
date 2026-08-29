package ru.bulldog.justmap.map;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import ru.bulldog.justmap.map.icon.PlayerHeadIconImage;

public class MapPlayer extends AbstractClientPlayer {

	private final PlayerHeadIconImage icon;

	public MapPlayer(ClientLevel world, Player player) {
		super(world, player.getGameProfile());

		this.icon = new PlayerHeadIconImage();
		this.icon.getPlayerSkin(this);
	}

	public PlayerHeadIconImage getIcon() {
		this.icon.checkForUpdate(this);
		return this.icon;
	}
}
