/*
 * Copyright 2021-2022 QuiltMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.lukebemish.defaultresources.impl;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.ResourcePackFileNotFoundException;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a group resource pack, which holds multiple resource packs as one.
 * <p>
 * The possible use cases are:
 * <ul>
 *   <li>bundling multiple resource packs as one to reduce pollution of the user's UI</li>
 *   <li>replacing the default resource pack with a combination of the default one and all mods' resource packs</li>
 *   <li>etc.</li>
 * </ul>
 */
public abstract class AbstractGroupResourcePack implements PackResources {
    protected final PackType type;
    protected final List<? extends PackResources> packs;
    protected final Map<String, List<PackResources>> namespacedPacks = new Object2ObjectOpenHashMap<>();

    public AbstractGroupResourcePack(@NotNull PackType type, @NotNull List<? extends  PackResources> packs) {
        this.type = type;
        this.packs = packs;
        this.packs.forEach(pack -> pack.getNamespaces(this.type)
                .forEach(namespace -> this.namespacedPacks.computeIfAbsent(namespace, v -> new ArrayList<>())
                        .add(pack)));
    }

    /**
	 * Gets an unmodifiable list of the resource packs stored in this group resourced pack.
	 *
	 * @return the resource packs
	 */
    public @UnmodifiableView List<? extends PackResources> getPacks() {
        return Collections.unmodifiableList(this.packs);
    }

    /**
	 * Gets an unmodifiable list of the resource packs stored in this group resource pack
	 * which contain the given {@code namespace}.
	 *
	 * @param namespace the namespace the packs must contain
	 * @return the list of the matching resource packs
	 */
    public @UnmodifiableView List<? extends PackResources> getPacks(String namespace) {
        return Collections.unmodifiableList(this.namespacedPacks.get(namespace));
    }

    /**
	 * Gets a flattened stream of resource packs in this group resource pack.
	 *
	 * @return the flattened stream of resource packs
	 */
    public @NotNull Stream<? extends PackResources> streamPacks() {
        return this.packs.stream().mapMulti((pack, consumer) -> {
            if (pack instanceof AbstractGroupResourcePack grouped) {
                grouped.streamPacks().forEach(consumer);
            } else {
                consumer.accept(pack);
            }
        });
    }

    @Override
    public InputStream getResource(PackType type, ResourceLocation id) throws IOException {
        List<PackResources> packs = this.namespacedPacks.get(id.getNamespace());

        if (packs != null) {
            // Iterating backwards as higher-priority packs are placed at the end.
            for (int i = packs.size() - 1; i >= 0; i--) {
                PackResources pack = packs.get(i);

                if (pack.hasResource(type, id)) {
                    return pack.getResource(type, id);
                }
            }
        }

        throw new ResourcePackFileNotFoundException(null,
                String.format("%s/%s/%s", type.getDirectory(), id.getNamespace(), id.getPath()));
    }

    @Override
    public Collection<ResourceLocation> getResources(PackType type, String namespace, String stringPath,
                                                     Predicate<ResourceLocation> pathFilter) {
        List<PackResources> packs = this.namespacedPacks.get(namespace);

        if (packs == null) {
            return Collections.emptyList();
        }

        Set<ResourceLocation> resources = new HashSet<>();

        // Iterating backwards as higher-priority packs are placed at the end.
        for (int i = packs.size() - 1; i >= 0; i--) {
            PackResources pack = packs.get(i);
            Collection<ResourceLocation> modResources = pack.getResources(type, namespace, stringPath, pathFilter);

            resources.addAll(modResources);
        }

        return resources;
    }

    @Override
    public boolean hasResource(PackType type, ResourceLocation id) {
        List<PackResources> packs = this.namespacedPacks.get(id.getNamespace());

        if (packs == null) {
			return false;
		}

		// Iterating backwards as higher-priority packs are placed at the end.
		for (int i = packs.size() - 1; i >= 0; i--) {
			PackResources pack = packs.get(i);

			if (pack.hasResource(type, id)) {
				return true;
			}
		}

		return false;
    }

	@Override
	public Set<String> getNamespaces(PackType type) {
		return this.namespacedPacks.keySet();
	}

	public String getFullName() {
		return this.getName() + " (" + this.packs.stream().map(PackResources::getName).collect(Collectors.joining(", ")) + ")";
	}

    public abstract Component getDisplayName();

	@Override
	public void close() {
		this.packs.forEach(PackResources::close);
	}

	/**
	 * Represents a group resource pack which wraps a "base" resource pack.
	 */
    public static class WrappedGroupResourcePack extends AbstractGroupResourcePack {
        private final PackResources basePack;

		/**
		 * Constructs a new instance of a group resource pack wrapping a base resource pack.
		 *
		 * @param type         the resource type of this resource pack
		 * @param basePack     the base resource pack
		 * @param packs        the additional packs
		 * @param basePriority {@code true} if the base resource pack has priority over the additional packs, or {@code false} otherwise.
		 *                     Ignored if the base resource pack is already present in the list
		 */
        public WrappedGroupResourcePack(@NotNull PackType type, @NotNull PackResources basePack, @NotNull List<PackResources> packs, boolean basePriority) {
			super(type, addToPacksIfNeeded(basePack, packs, basePriority));
			this.basePack = basePack;
        }

		private static List<PackResources> addToPacksIfNeeded(PackResources basePack, List<PackResources> packs,
				boolean basePriority) {
			if (!packs.contains(basePack)) {
				if (basePriority) {
					packs.add(basePack);
				} else {
					packs.add(0, basePack);
				}
			}

			return packs;
		}

		@Override
		public @Nullable InputStream getRootResource(String fileName) throws IOException {
			return this.basePack.getRootResource(fileName);
		}

		@Override
		public <T> @Nullable T getMetadataSection(MetadataSectionSerializer<T> metaReader) throws IOException {
			return this.basePack.getMetadataSection(metaReader);
		}

		@Override
		public String getName() {
			return this.basePack.getName();
		}

        @Override
		public Component getDisplayName() {
			return Component.literal(this.basePack.getName());
		}

		@Override
		public String getFullName() {
			return this.getName() + " (" + this.packs.stream().filter(pack -> pack != this.basePack)
					.map(PackResources::getName).collect(Collectors.joining(", ")) + ")";
		}
    }
}
