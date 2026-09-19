package ru.bulldog.justmap.client.control;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;

import ru.bulldog.justmap.JustMap;
import ru.bulldog.justmap.client.JustMapClient;
import ru.bulldog.justmap.client.config.ConfigFactory;
import ru.bulldog.justmap.client.screen.WaypointsListScreen;
import ru.bulldog.justmap.client.screen.WorldmapScreen;

public final class KeyHandler {

	private static List<KeyParser> parsers;

	private KeyHandler() {}

	public static void initKeyBindings() {
		parsers = new ArrayList<>();

		registerKey(new KeyParser(createKeyBinding("create_waypoint", InputConstants.KEY_B)) {
			@Override
			public void onKeyUp() {
				JustMapClient.getMiniMap().createWaypoint();
			}

			@Override
			public boolean isListening() {
				return MC.player != null && MC.gui.screen() == null;
			}
		});

		registerKey(new KeyParser(createKeyBinding("toggle_map_visible", InputConstants.KEY_H)) {
			@Override
			public void onKeyUp() {
				JustMapClient.getConfig().setBoolean("map_visible", !JustMapClient.getConfig().getBoolean("map_visible"));
				JustMapClient.getConfig().saveChanges();
			}
		});

		registerKey(new KeyParser(createKeyBinding("toggle_big_map", InputConstants.KEY_N)) {
			@Override
			public void onKeyUp() {
				JustMapClient.getConfig().setBoolean("show_big_map", !JustMapClient.getConfig().getBoolean("show_big_map"));
				JustMapClient.getConfig().saveChanges();
			}
		});

		registerKey(new KeyParser(createKeyBinding("toggle_show_caves", InputConstants.KEY_K)) {
			@Override
			public void onKeyUp() {
				JustMapClient.getConfig().setBoolean("show_caves", !JustMapClient.getConfig().getBoolean("show_caves"));
				JustMapClient.getConfig().saveChanges();
			}

			@Override
			public boolean isListening() {
				return JustMapClient.getMiniMap().isMapVisible();
			}
		});

		registerKey(new KeyParser(createKeyBinding("toggle_show_entities", InputConstants.KEY_Y)) {
			@Override
			public void onKeyUp() {
				JustMapClient.getConfig().setBoolean("show_entities", !JustMapClient.getConfig().getBoolean("show_entities"));
				JustMapClient.getConfig().saveChanges();
			}

			@Override
			public boolean isListening() {
				return JustMapClient.getMiniMap().isMapVisible();
			}
		});

		registerKey(new KeyParser(createKeyBinding("toggle_show_waypoints", InputConstants.KEY_P)) {
			@Override
			public void onKeyUp() {
				JustMapClient.getConfig().setBoolean("show_waypoints", !JustMapClient.getConfig().getBoolean("show_waypoints"));
				JustMapClient.getConfig().saveChanges();
			}
		});

		registerKey(new KeyParser(createKeyBinding("waypoints_list", InputConstants.KEY_U)) {
			@Override
			public void onKeyUp() {
				MC.setScreenAndShow(new WaypointsListScreen(null));
			}

			@Override
			public boolean isListening() {
				return MC.player != null && MC.gui.screen() == null;
			}
		});

		registerKey(new KeyParser(createKeyBinding("show_config", InputConstants.KEY_J)) {
			@Override
			public void onKeyUp() {
				MC.setScreenAndShow(ConfigFactory.getConfigScreen(null));
			}

			@Override
			public boolean isListening() {
				return MC.gui.screen() == null;
			}
		});

		registerKey(new KeyParser(createKeyBinding("show_worldmap", InputConstants.KEY_M)) {
			@Override
			public void onKeyUp() {
				MC.setScreenAndShow(WorldmapScreen.getScreen());
			}

			@Override
			public boolean isListening() {
				return MC.player != null && MC.gui.screen() == null;
			}
		});

		registerKey(new KeyParser(createKeyBinding("reduce_scale", InputConstants.KEY_LBRACKET)) {
			@Override
			public void onKeyUp() {
				JustMapClient.getConfig().updateMapScale(0.5F);
			}

			@Override
			public boolean isListening() {
				return JustMapClient.getMiniMap().isMapVisible();
			}
		});

		registerKey(new KeyParser(createKeyBinding("increase_scale", InputConstants.KEY_RBRACKET)) {
			@Override
			public void onKeyUp() {
				JustMapClient.getConfig().updateMapScale(2F);
			}

			@Override
			public boolean isListening() {
				return JustMapClient.getMiniMap().isMapVisible();
			}
		});
	}

	public static void updateOnTick() {
		for (KeyParser kp : parsers) {
			if (kp.isListening()) {
				if (kp.keyBinding.consumeClick()) {
					kp.onKeyUp();
				} else if (kp.keyBinding.isDown()) {
					kp.onKeyDown();
				}
			}
		}
	}

	private static void registerKey(KeyParser parser) {
		KeyMappingHelper.registerKeyMapping(parser.keyBinding);
		parsers.add(parser);
	}

	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(JustMap.MODID, JustMap.MODID));

	private static KeyMapping createKeyBinding(String name, int key) {
		return new KeyMapping(String.format("key.%s.%s", JustMap.MODID, name), key, CATEGORY);
	}
}
