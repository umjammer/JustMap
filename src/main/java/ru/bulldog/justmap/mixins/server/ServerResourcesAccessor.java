package ru.bulldog.justmap.mixins.server;

import net.minecraft.server.packs.resources.ResourceManager;


public interface ServerResourcesAccessor {
	ResourceManager getResourceManager();
}
