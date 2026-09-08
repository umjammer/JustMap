package ru.bulldog.justmap.mixins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.map.MapGameRules;
import ru.bulldog.justmap.map.data.MapDataProvider;
import ru.bulldog.justmap.map.multiworld.WorldKey;
import ru.bulldog.justmap.map.waypoint.Waypoint;

@Mixin(value = ClientPacketListener.class, priority = 100)
public abstract class ClientPlayNetworkHandlerMixin {

	@Unique
	protected Minecraft client;

	@Inject(method = "<init>", at = @At("TAIL"))
	public void onConnect(Minecraft client, Connection clientConnection, CommonListenerCookie clientConnectionState, CallbackInfo ci) {
		MapDataProvider.getMultiworldManager().onServerConnect();
		this.client = client;
	}

	@Inject(method = "handleSetSpawn", at = @At("TAIL"))
	public void onPlayerSpawnPosition(ClientboundSetDefaultSpawnPositionPacket packet, CallbackInfo cinfo) {
		BlockPos spawnPos = packet.respawnData().globalPos().pos();
		JustMap.LOGGER.debug("World spawn position set to {}", spawnPos.toShortString());
		MapDataProvider.getMultiworldManager().onWorldSpawnPosChanged(spawnPos);
	}

	@Inject(method = "handleSystemChat", at = @At("HEAD"), cancellable = true)
	public void onGameMessage(ClientboundSystemChatPacket gameMessageS2CPacket, CallbackInfo ci) {
		String pref = "§0§0", suff = "§f§f";
		String message = gameMessageS2CPacket.content().getString().replaceAll("[&$]", "§");

		if (message.contains(pref) && message.contains(suff)) {
			int start = message.indexOf(pref) + 4;
			int end = message.indexOf(suff);

			MapGameRules.parseCommand(message.substring(start, end));

			if (message.matches("^§0§0.+§f§f$")) {
				ci.cancel();
			}
		}
	}

	@Inject(method = "handleSetHealth", at = @At("TAIL"))
	public void onHealthUpdate(ClientboundSetHealthPacket healthUpdateS2CPacket, CallbackInfo cinfo) {
		float health = healthUpdateS2CPacket.getHealth();
		if (health <= 0.0F) {
			WorldKey world = MapDataProvider.getMultiworldManager().getCurrentWorldKey();
			BlockPos playerPos = this.client.player.blockPosition();
			Waypoint.createOnDeath(world, playerPos);
		}
	}
}
