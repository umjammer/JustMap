package ru.bulldog.justmap.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import ru.bulldog.justmap.JustMap;

public class NetworkHandler {

	public final static Type<PacketByteBufPayload> CHANNEL_ID = new Type<>(Identifier.fromNamespaceAndPath(JustMap.MODID, "networking"));
	public final static Type<PacketByteBufPayload> INIT_PACKET_ID = new Type<>(Identifier.fromNamespaceAndPath(JustMap.MODID, "networking_init"));

	public static class PacketByteBufPayload implements CustomPacketPayload {
		Type<? extends CustomPacketPayload> id;
		FriendlyByteBuf buf;

		public PacketByteBufPayload(Type<? extends CustomPacketPayload> id, FriendlyByteBuf buf) {
			this.id = id;
			this.buf = buf;
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return id;
		}

		static StreamCodec<FriendlyByteBuf, PacketByteBufPayload> createPacketCodec(Type<? extends CustomPacketPayload> id) {
			return CustomPacketPayload.codec(
					(value, buf) -> buf.readBytes(value.buf),
					buf -> new PacketByteBufPayload(id, buf)
			);
		}

		public static StreamCodec<FriendlyByteBuf, PacketByteBufPayload> initPacketCodec = createPacketCodec(INIT_PACKET_ID);
		public static StreamCodec<FriendlyByteBuf, PacketByteBufPayload> channelPacketCodec = createPacketCodec(CHANNEL_ID);
	}

	public boolean canServerReceive() {
		return ClientPlayNetworking.canSend(CHANNEL_ID);
	}

	public boolean canPlayerReceive(Player player) {
		return ServerPlayNetworking.canSend((ServerPlayer) player, CHANNEL_ID);
	}

	public void sendToPlayer(Player player, Packet<?> packet) {
		((ServerPlayer) player).connection.send(packet);
	}

	public void sendToServer(Packet<?> packet) {
		Minecraft.getInstance().getConnection().getConnection().send(packet);
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
