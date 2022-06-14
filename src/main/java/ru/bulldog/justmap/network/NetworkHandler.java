package ru.bulldog.justmap.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import ru.bulldog.justmap.JustMap;

public class NetworkHandler {
	public final static Identifier CHANNEL_ID = new Identifier(JustMap.MODID, "networking");
	public final static Identifier INIT_PACKET_ID = new Identifier(JustMap.MODID, "networking_init");

	public boolean canServerReceive() {
		return ClientPlayNetworking.canSend(CHANNEL_ID);
	}

	public boolean canPlayerReceive(PlayerEntity player) {
		return ServerPlayNetworking.canSend((ServerPlayerEntity) player, CHANNEL_ID);
	}

	public void sendToPlayer(PlayerEntity player, Packet<?> packet) {
		((ServerPlayerEntity) player).networkHandler.sendPacket(packet);
	}

	public void sendToServer(Packet<?> packet) {
		MinecraftClient.getInstance().getNetworkHandler().getConnection().send(packet);
	}

	public enum PacketType {
		SLIME_CHUNK_PACKET,
		GET_IMAGE_PACKET;

		private final static PacketType[] values = values();

		public static PacketType get(int id) {
			return values[id];
		}
	}
}
