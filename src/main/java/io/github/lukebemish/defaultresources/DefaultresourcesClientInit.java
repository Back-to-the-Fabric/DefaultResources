package io.github.lukebemish.defaultresources;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.packs.PackType;

@Environment(EnvType.CLIENT)
public class DefaultresourcesClientInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DefaultresourcesInit.addPackResources(PackType.CLIENT_RESOURCES);
    }
}
