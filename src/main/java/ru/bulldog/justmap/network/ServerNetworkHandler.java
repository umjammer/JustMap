package ru.bulldog.justmap.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import ru.bulldog.justmap.util.Dimension;
import ru.bulldog.justmap.util.GameRulesUtil;

import static ru.bulldog.justmap.network.NetworkHandler.PacketByteBufPayload.channelPacketCodec;
import static ru.bulldog.justmap.network.NetworkHandler.PacketByteBufPayload.initPacketCodec;


public class ServerNetworkHandler extends NetworkHandler {
	private final MinecraftServer server;

	public ServerNetworkHandler(MinecraftServer server) {
		this.server = server;
	}

	public void onPlayerConnect(ServerPlayer player) {
		FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
		ServerLevel world = server.getLevel(Level.OVERWORLD);
		data.writeLong(world.getSeed());
		PacketByteBufPayload payload = initPacketCodec.decode(data);
		ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(payload);
		this.sendToPlayer(player, packet);
	}

	public void registerPacketsListeners() {
		ServerPlayNetworking.registerGlobalReceiver(CHANNEL_ID, (payload, context) -> {
			FriendlyByteBuf packetData = new FriendlyByteBuf(Unpooled.buffer());
			channelPacketCodec.encode(packetData, payload);
			PacketType packetType = PacketType.get(packetData.readByte());
			switch (packetType) {
				case GET_IMAGE_PACKET -> server.execute(() -> this.onRegionImageRequest(context.player(), packetData));
				case SLIME_CHUNK_PACKET -> server.execute(() -> this.onChunkHasSlimeRequest(context.player(), packetData));
			}
		});
	}

	private void onRegionImageRequest(ServerPlayer player, ByteBuf data) {

	}

	private void onChunkHasSlimeRequest(ServerPlayer player, ByteBuf data) {
		if (!canPlayerReceive(player)) return;
		int packet_id = data.readInt();
		int x = data.readInt();
		int z = data.readInt();

		boolean slime = false;
		if (GameRulesUtil.allowSlimeChunks() && Dimension.isOverworld(player.level())) {
			ServerLevel world = player.level();
			slime = WorldgenRandom.seedSlimeChunk(x, z, world.getSeed(), 987234911L).nextInt(10) == 0;
		}
		FriendlyByteBuf response = new FriendlyByteBuf(Unpooled.buffer());
		response.writeByte(PacketType.SLIME_CHUNK_PACKET.ordinal());
		response.writeInt(packet_id);
		response.writeBoolean(slime);
		PacketByteBufPayload payload = channelPacketCodec.decode(response);
		ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(payload);
		this.sendToPlayer(player, packet);
	}
}
