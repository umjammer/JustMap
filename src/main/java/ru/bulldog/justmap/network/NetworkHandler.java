package ru.bulldog.justmap.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import ru.bulldog.justmap.JustMap;

public class NetworkHandler {

	public final static Id<PacketByteBufPayload> CHANNEL_ID = new Id<>(new Identifier(JustMap.MODID, "networking"));
	public final static Id<PacketByteBufPayload> INIT_PACKET_ID = new Id<>(new Identifier(JustMap.MODID, "networking_init"));

	public static class PacketByteBufPayload implements CustomPayload {
		Id<? extends CustomPayload> id;
		PacketByteBuf buf;

		public PacketByteBufPayload(Id<? extends CustomPayload> id, PacketByteBuf buf) {
			this.id = id;
			this.buf = buf;
		}

		@Override
		public Id<? extends CustomPayload> getId() {
			return id;
		}

		static PacketCodec<PacketByteBuf, PacketByteBufPayload> createPacketCodec(Id<? extends CustomPayload> id) {
			return CustomPayload.codecOf(
					(value, buf) -> buf.readBytes(value.buf),
					buf -> new PacketByteBufPayload(id, buf)
			);
		}

		public static PacketCodec<PacketByteBuf, PacketByteBufPayload> initPacketCodec = createPacketCodec(INIT_PACKET_ID);
		public static PacketCodec<PacketByteBuf, PacketByteBufPayload> channelPacketCodec = createPacketCodec(CHANNEL_ID);
	}

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
