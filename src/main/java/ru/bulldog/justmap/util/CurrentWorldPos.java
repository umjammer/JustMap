package ru.bulldog.justmap.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import ru.bulldog.justmap.util.math.MathUtil;

public class CurrentWorldPos {
	private static final BlockPos.MutableBlockPos currentPos = new BlockPos.MutableBlockPos();
	private static ClientLevel clientWorld = null;
	private static ServerLevel serverWorld = null;
	private static int coordX;
	private static int coordY;
	private static int coordZ;

	@Environment(EnvType.CLIENT)
	public static void updateWorld(ClientLevel world) {
		clientWorld = world;
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.hasSingleplayerServer()) {
			serverWorld = minecraft.getSingleplayerServer().getLevel(world.dimension());
		} else {
			serverWorld = null;
		}
	}

	public static void updatePositionOnTick() {
		if (getPosEntity() == null) {
			coordX = 0;
			coordY = 0;
			coordZ = 0;
		} else {
			coordX = MathUtil.floor(getPosEntity().getX());
			coordZ = MathUtil.floor(getPosEntity().getZ());
			coordY = (int) getPosEntity().getY();
		}
	}

	public static Level getWorld() {
		return serverWorld != null ? serverWorld : clientWorld;
	}

	public static ClientLevel getClientWorld() {
		return clientWorld;
	}

	public static ServerLevel getServerWorld() {
		return serverWorld;
	}

	public static int coordY() {
		return coordY;
	}

	public static BlockPos currentPos() {
		return currentPos.set(coordX, coordY, coordZ);
	}

	public static double doubleX(Entity entity, float delta) {
		if (entity == null) return 0.0;
		return MathUtil.lerp(delta, entity.xo, entity.getX());
	}

	public static double doubleZ(Entity entity, float delta) {
		if (entity == null) return 0.0;
		return MathUtil.lerp(delta, entity.zo, entity.getZ());
	}

	public static double doubleX(float delta) {
		return doubleX(getPosEntity(), delta);
	}

	public static double doubleZ(float delta) {
		return doubleZ(getPosEntity(), delta);
	}

	private static Entity getPosEntity() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.getCameraEntity() != null ? minecraft.getCameraEntity() : minecraft.player;
	}
}
