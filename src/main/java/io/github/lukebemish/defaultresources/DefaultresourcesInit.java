package io.github.lukebemish.defaultresources;

import io.github.lukebemish.defaultresources.impl.*;
import net.devtech.arrp.api.RRPCallback;
import net.fabricmc.api.ModInitializer;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DefaultresourcesInit implements ModInitializer {
    @Override
    public void onInitialize() {
        addPackResources(PackType.SERVER_DATA);
    }

    public static void addPackResources(PackType type) {
        try {
            if (!Files.exists(Services.PLATFORM.getGlobalFolder())) {
                Files.createDirectories(Services.PLATFORM.getGlobalFolder());
            }
            RRPCallback.AFTER_VANILLA.register(a -> {
                Pack pack = new Pack(
                        DefaultResources.MOD_ID,
                        Component.literal(""),
                        true,
                        () -> {
                            EmptyResourcePack core = new EmptyResourcePack(DefaultResources.MOD_ID+"_global",
                                    "Global Resources",
                                    new PackMetadataSection(Component.literal("Global Resources"),
                                            type.getVersion(SharedConstants.getCurrentVersion())));
                            List<PackResources> packs = new ArrayList<>();
                            try (var files = Files.list(Services.PLATFORM.getGlobalFolder())) {
                                for (Path file : files.toList()) {
                                    PackResources p = null;
                                    if (Files.isDirectory(file)) {
                                        p = new AutoMetadataFolderPackResources(type, file.toFile());
                                    } else if (file.getFileName().toString().endsWith(".zip")) {
                                        p = new AutoMetadataFilePackResources(type, file.toFile());
                                    }
                                    if (p != null) {
                                        packs.add(p);
                                    }
                                }
                            } catch (IOException ignored) {}
                            return new AbstractGroupResourcePack.WrappedGroupResourcePack(
                                    type, core, packs, false);
                        },
                        new PackMetadataSection(
                                Component.literal(DefaultResources.MOD_ID),
                                SharedConstants.RESOURCE_PACK_FORMAT
                        ),
                        type,
                        Pack.Position.TOP,
                        PackSource.DEFAULT
                );
                a.add(pack.open());
            });
        } catch (IOException e) {
            DefaultResources.LOGGER.error(e);
        }
    }
}
