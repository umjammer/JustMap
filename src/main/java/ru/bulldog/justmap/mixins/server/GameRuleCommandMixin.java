package ru.bulldog.justmap.mixins.server;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.network.message.DecoratedContents;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.command.GameRuleCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameRules.BooleanRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.server.config.ServerSettings;

@Mixin(GameRuleCommand.class)
public abstract class GameRuleCommandMixin {

	@Inject(method = "executeSet", at = @At("RETURN"))
	private static <T extends GameRules.Rule<T>> void executeSet(CommandContext<ServerCommandSource> commandContext, GameRules.Key<T> Key, CallbackInfoReturnable<Integer> cir) {
		if (ServerSettings.useGameRules) {
			ServerCommandSource serverCommandSource = commandContext.getSource();
			T rule = serverCommandSource.getServer().getGameRules().get(Key);

			if (rule instanceof BooleanRule) {
				SignedMessage message;
				String command;

				String val = ((BooleanRule) rule).get() ? "§1" : "§0";
				switch (Key.getName()) {
				case "allowCavesMap":
					command = String.format("§0§0§a%s§f§f", val);
					message = SignedMessage.ofUnsigned(new DecoratedContents(command, Text.literal(command)));
					serverCommandSource.getServer().getPlayerManager().sendToAll(
							new ChatMessageS2CPacket(message, JustMap.MESSAGE_ID));
					break;
				case "allowEntityRadar":
					command = String.format("§0§0§b%s§f§f", val);
					message = SignedMessage.ofUnsigned(new DecoratedContents(command, Text.literal(command)));
					serverCommandSource.getServer().getPlayerManager().sendToAll(
							new ChatMessageS2CPacket(message, JustMap.MESSAGE_ID));
					break;
				case "allowPlayerRadar":
					command = String.format("§0§0§c%s§f§f", val);
					message = SignedMessage.ofUnsigned(new DecoratedContents(command, Text.literal(command)));
					serverCommandSource.getServer().getPlayerManager().sendToAll(
							new ChatMessageS2CPacket(message, JustMap.MESSAGE_ID));
					break;
				case "allowCreatureRadar":
					command = String.format("§0§0§d%s§f§f", val);
					message = SignedMessage.ofUnsigned(new DecoratedContents(command, Text.literal(command)));
					serverCommandSource.getServer().getPlayerManager().sendToAll(
							new ChatMessageS2CPacket(message, JustMap.MESSAGE_ID));
					break;
				case "allowHostileRadar":
					command = String.format("§0§0§e%s§f§f", val);
					message = SignedMessage.ofUnsigned(new DecoratedContents(command, Text.literal(command)));
					serverCommandSource.getServer().getPlayerManager().sendToAll(
							new ChatMessageS2CPacket(message, JustMap.MESSAGE_ID));
					break;
				case "allowSlimeChunks":
					command = String.format("§0§0§s%s§f§f", val);
					message = SignedMessage.ofUnsigned(new DecoratedContents(command, Text.literal(command)));
					serverCommandSource.getServer().getPlayerManager().sendToAll(
							new ChatMessageS2CPacket(message, JustMap.MESSAGE_ID));
					break;
				case "allowWaypointsJump":
					command = String.format("§0§0§t%s§f§f", val);
					message = SignedMessage.ofUnsigned(new DecoratedContents(command, Text.literal(command)));
					serverCommandSource.getServer().getPlayerManager().sendToAll(
							new ChatMessageS2CPacket(message, JustMap.MESSAGE_ID));
					break;
				}
			}
		}
	}
}
