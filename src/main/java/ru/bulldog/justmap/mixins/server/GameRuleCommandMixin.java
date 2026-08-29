package ru.bulldog.justmap.mixins.server;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.commands.GameRuleCommand;
import net.minecraft.world.level.gamerules.GameRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.bulldog.justmap.map.MapGameRules;
import ru.bulldog.justmap.server.config.ServerSettings;

@Mixin(GameRuleCommand.class)
public abstract class GameRuleCommandMixin {

	@Inject(method = "setRule", at = @At("RETURN"))
	private static <T> void setRule(CommandContext<CommandSourceStack> commandContext, GameRule<T> gameRule, CallbackInfoReturnable<Integer> cir) {
		if (!ServerSettings.useGameRules) return;

		String code = MapGameRules.getCode(gameRule);
		if (code == null) return;

		CommandSourceStack serverCommandSource = commandContext.getSource();
		Object value = serverCommandSource.getServer().getGameRules().get(gameRule);
		if (!(value instanceof Boolean enabled)) return;

		String command = String.format("§0§0%s%s§f§f", code, enabled ? "§1" : "§0");
		serverCommandSource.getServer().getPlayerList().broadcastAll(
				new ClientboundSystemChatPacket(Component.nullToEmpty(command), true));
	}
}
