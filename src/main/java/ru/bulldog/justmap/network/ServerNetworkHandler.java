package ru.bulldog.justmap.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.World;
import ru.bulldog.justmap.util.Dimension;
import ru.bulldog.justmap.util.GameRulesUtil;

public class ServerNetworkHandler extends NetworkHandler {
	private final MinecraftServer server;

	public ServerNetworkHandler(MinecraftServer server) {
		this.server = server;
	}

	public void onPlayerConnect(ServerPlayerEntity player) {
		PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
		ServerWorld world = server.getWorld(World.OVERWORLD);
		data.writeLong(world.getSeed());
		CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(INIT_PACKET_ID, data);
		this.sendToPlayer(player, packet);
	}

	public void registerPacketsListeners() {
		ServerPlayNetworking.registerGlobalReceiver(CHANNEL_ID, (server, player, handler, buf, responseSender) -> {
			ByteBuf packetData = buf.copy();
			PacketType packetType = PacketType.get(packetData.readByte());
			switch (packetType) {
			case GET_IMAGE_PACKET: {
				server.execute(() -> this.onRegionImageRequest(player, packetData));
				break;
			}
			case SLIME_CHUNK_PACKET: {
				server.execute(() -> this.onChunkHasSlimeRequest(player, packetData));
				break;
			}
			}
		});
	}

	private void onRegionImageRequest(ServerPlayerEntity player, ByteBuf data) {

	}

	private void onChunkHasSlimeRequest(ServerPlayerEntity player, ByteBuf data) {
		if (!canPlayerReceive(player)) return;
		int packet_id = data.readInt();
		int x = data.readInt();
		int z = data.readInt();

		boolean slime = false;
		if (GameRulesUtil.allowSlimeChunks() && Dimension.isOverworld(player.getWorld())) {
			ServerWorld world = player.getServerWorld();
			slime = ChunkRandom.getSlimeRandom(x, z, world.getSeed(), 987234911L).nextInt(10) == 0;
		}
		PacketByteBuf response = new PacketByteBuf(Unpooled.buffer());
		response.writeByte(PacketType.SLIME_CHUNK_PACKET.ordinal());
		response.writeInt(packet_id);
		response.writeBoolean(slime);
		CustomPayloadS2CPacket packet = new CustomPayloadS2CPacket(CHANNEL_ID, response);
		this.sendToPlayer(player, packet);
	}
}
