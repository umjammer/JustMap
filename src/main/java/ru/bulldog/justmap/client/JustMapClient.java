package ru.bulldog.justmap.client;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.AbstractGameRulesScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.advancedinfo.AdvancedInfo;
import ru.bulldog.justmap.client.config.ClientConfig;
import ru.bulldog.justmap.client.control.KeyHandler;
import ru.bulldog.justmap.client.render.WaypointRenderer;
import ru.bulldog.justmap.map.data.MapDataProvider;
import ru.bulldog.justmap.map.minimap.Minimap;
import ru.bulldog.justmap.network.ClientNetworkHandler;
import ru.bulldog.justmap.util.CurrentWorldPos;
import ru.bulldog.justmap.util.colors.Colors;
import ru.bulldog.justmap.util.tasks.TaskManager;

public class JustMapClient implements ClientModInitializer {
	private static ClientConfig config = ClientConfig.get();
	private static Minimap minimap = new Minimap();
	private static Minecraft minecraft;
	private static ClientNetworkHandler networkHandler;
	private static boolean isOnTitleScreen = true;

	@Override
	public void onInitializeClient() {
		JustMap.setSide(EnvType.CLIENT);
		KeyHandler.initKeyBindings();
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			minecraft = client;
			networkHandler = new ClientNetworkHandler();
			networkHandler.registerPacketsListeners();
			config = ClientConfig.get();
			minimap = new Minimap();
			Colors.INSTANCE.loadData();
			WaypointRenderer.startWaypointRender();
		});
		ClientChunkEvents.CHUNK_LOAD.register(MapDataProvider.getManager()::onChunkLoad);
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
				Identifier.fromNamespaceAndPath(JustMap.MODID, "minimap"), (context, delta) -> {
			if (!minecraft.options.reducedDebugInfo().get()) {
				JustMapClient.minimap.getRenderer().renderMap(context);
				AdvancedInfo.getInstance().draw(context);
			}
		});
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
				Identifier.fromNamespaceAndPath(JustMap.MODID, "waypoints"), (context, delta) ->
			WaypointRenderer.renderHUD(context,
					delta.getGameTimeDeltaPartialTick(false),
					minecraft.options.fov().get())
		);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (minecraft == null) return;
			boolean isTitle = this.isOnTitleScreen(client.gui.screen());
			if (isTitle && !isOnTitleScreen) {
				JustMapClient.stop();
			}
			isOnTitleScreen = isTitle;

			AdvancedInfo.getInstance().updateOnTick();
			KeyHandler.updateOnTick();

			if (!canMapping()) return;

			CurrentWorldPos.updatePositionOnTick();
			JustMapClient.minimap.updateOnTick();
			MapDataProvider.getManager().onTick(false);
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			JustMapClient.stop();
			TaskManager.shutdown();
			minecraft = null;
		});
	}

	private static void stop() {
		MapDataProvider.getManager().onWorldStop();
		MapDataProvider.getMultiworldManager().onWorldStop();
		Colors.INSTANCE.saveData();
	}

	public static boolean canMapping() {
		return !isOnTitleScreen && MapDataProvider.getMultiworldManager().isMappingEnabled() && minecraft.level != null &&
				(minecraft.getCameraEntity() != null || minecraft.player != null);
	}

	public static Minimap getMiniMap() {
		return minimap;
	}

	public static ClientConfig getConfig() {
		return config;
	}

	public static ClientNetworkHandler getNetworkHandler() {
		return networkHandler;
	}

	private boolean isOnTitleScreen(Screen currentScreen) {
		if (currentScreen == null) return false;

		boolean isTitleScreen = false;
		if (currentScreen.getTitle() instanceof TranslatableContents) {
			TranslatableContents title = (TranslatableContents) currentScreen.getTitle();
			isTitleScreen = title.getKey().equals("dataPack.title");
		}

		return currentScreen instanceof TitleScreen ||
			currentScreen instanceof SelectWorldScreen ||
			currentScreen instanceof JoinMultiplayerScreen ||
			currentScreen instanceof BackupConfirmScreen ||
			currentScreen instanceof CreateWorldScreen ||
			currentScreen instanceof AbstractGameRulesScreen ||
			currentScreen instanceof EditWorldScreen ||
			currentScreen instanceof RealmsMainScreen ||
			currentScreen instanceof RealmsGenericErrorScreen ||
			isTitleScreen;
	}
}
