package io.github.lukebemish.defaultresources;

import com.google.auto.service.AutoService;
import com.mojang.datafixers.util.Pair;
import io.github.lukebemish.defaultresources.api.ResourceProvider;
import io.github.lukebemish.defaultresources.impl.DefaultResources;
import io.github.lukebemish.defaultresources.impl.PathResourceProvider;
import io.github.lukebemish.defaultresources.impl.Services;
import io.github.lukebemish.defaultresources.impl.services.IPlatform;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AutoService(IPlatform.class)
public class PlatformImpl implements IPlatform {
    public Path getGlobalFolder() {
        return FabricLoader.getInstance().getGameDir().resolve("globalresources");
    }

    @Override
    public void extractResources() {
        try {
            if (!Files.exists(Services.PLATFORM.getGlobalFolder()))
                Files.createDirectories(Services.PLATFORM.getGlobalFolder());
        } catch (IOException e) {
            DefaultResources.LOGGER.error(e);
        }
        FabricLoader.getInstance().getAllMods().forEach(mod -> {
            String modid = mod.getMetadata().getId();
            if (!modid.equals("minecraft")) {
                DefaultResources.forMod(FabricLoader.getInstance().getConfigDir(), mod.getRootPath().toAbsolutePath()::resolve, modid);
            }
        });
    }

    @Override
    public Collection<ResourceProvider> getJarProviders() {
        List<ResourceProvider> providers = new ArrayList<>();
        FabricLoader.getInstance().getAllMods().forEach(mod -> {
            String modid = mod.getMetadata().getId();
            if (!modid.equals("minecraft")) {
                providers.add(new PathResourceProvider(mod.getRootPath()));
            }
        });
        return providers;
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Map<String, Path> getExistingModdedPaths(String relative) {
        return FabricLoader.getInstance().getAllMods().stream()
                .filter(mod->!mod.getMetadata().getId().equals("minecraft"))
                .map(mod->
                        new Pair<>(mod.getMetadata().getId(), mod.getRootPath().toAbsolutePath().resolve(relative)))
                .filter(it->it.getSecond()!=null&&Files.exists(it.getSecond()))
                .collect(Collectors.toMap(Pair::getFirst,Pair::getSecond,(a, b)->a));
    }
}
