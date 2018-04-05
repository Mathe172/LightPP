/*
 * MIT License
 *
 * Copyright (c) 2017-2018 OverengineeredCodingDuo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package ocd.lightpp.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import ocd.lightpp.api.vanilla.type.CachedLightProviderType.TypedCachedLightProvider;
import ocd.lightpp.api.vanilla.type.ContainerType.TypedContainer;
import ocd.lightpp.api.vanilla.type.LightProviderType.TypedLightProvider;
import ocd.lightpp.api.vanilla.type.TypedEmptySectionLightPredictor;
import ocd.lightpp.api.vanilla.type.TypedLightStorage;
import ocd.lightpp.api.vanilla.type.TypedLightStorageProvider;
import ocd.lightpp.api.vanilla.world.IVanillaLightStorageHolder;
import ocd.lightpp.api.vanilla.world.IVanillaWorldLightProvider;
import ocd.lightpp.lighting.vanilla.world.VanillaWorldLightHelper;
import ocd.lightpp.lighting.vanilla.world.VanillaWorldLightManager;
import ocd.lightpp.lighting.vanilla.world.WorldLightContainerCache;

@Mixin(World.class)
public abstract class MixinWorldLightStorage implements IVanillaWorldLightProvider
{
	private VanillaWorldLightManager<?, ?, ?, ?, ?, ?, ?, ?, ?> lightManager;
	private final VanillaWorldLightHelper<?, ?, ?, ?, ?, ?> lightHelper = new VanillaWorldLightHelper<>(this.lightManager);

	private final MutableBlockPos cachedPos = new MutableBlockPos();
	private final MutableBlockPos cachedUpperPos = new MutableBlockPos();

	private final ThreadLocal<TypedContainer<?>> lightContainerCache = new WorldLightContainerCache(this);

	@Override
	public TypedLightStorageProvider<?, ?, ?, ?, ?, NibbleArray> getLightStorageProvider()
	{
		return this.lightManager.lightStorageProvider;
	}

	@Override
	public @Nullable TypedCachedLightProvider<?, ?, ?, ?, ?> getSkyLightProvider()
	{
		return this.lightManager.skyLightProvider;
	}

	@Override
	public @Nullable TypedEmptySectionLightPredictor<?, ?, ?, ?, ?> getEmptySectionLightPredictor()
	{
		return this.lightManager.emptySectionLightPredictor;
	}

	@Override
	public TypedLightProvider<?, ?, ?> getEmptyLightProvider()
	{
		return this.lightManager.emptyLightProvider;
	}

	private @Nullable ExtendedBlockStorage getUpperLightStorage(final ExtendedBlockStorage[] storageArray, int y)
	{
		for (; ++y < storageArray.length; )
		{
			final ExtendedBlockStorage blockStorage = storageArray[y];

			if (blockStorage != null)
				return blockStorage;
		}

		return null;
	}

	@Override
	public TypedContainer<?> createWorldLightContainer()
	{
		return this.lightManager.createWorldLightContainer();
	}

	@Override
	public Object getWorldLightInterface(final Chunk chunk, final BlockPos pos)
	{
		return this.getWorldLightInterface(chunk, pos, this.lightContainerCache.get());
	}

	@Override
	public Object getWorldLightInterface(final Chunk chunk, final BlockPos pos, final @Nullable TypedContainer<?> container)
	{
		final int y = pos.getY();
		final int index = y >> 4;

		final ExtendedBlockStorage[] storageArray = chunk.getBlockStorageArray();

		final @Nullable ExtendedBlockStorage blockStorage = storageArray[index];

		if (blockStorage != null)
		{
			final TypedLightStorage<?, ?, ?, ?, ?, ?> lightStorage = ((IVanillaLightStorageHolder) blockStorage).getLightStorage();
			return this.lightManager.getWorldLightInterface(lightStorage, pos, container);
		}

		if (this.lightManager.needsUpperStorage())
		{
			final @Nullable ExtendedBlockStorage upperBlockStorage = this.getUpperLightStorage(storageArray, index);

			if (upperBlockStorage != null)
			{
				final TypedLightStorage<?, ?, ?, ?, ?, ?> upperLightStorage = ((IVanillaLightStorageHolder) upperBlockStorage).getLightStorage();
				return this.lightManager.getWorldLightInterface(
					pos,
					upperLightStorage,
					new BlockPos(pos.getX(), upperBlockStorage.getYLocation(), pos.getZ()),
					container
				);
			}
		}

		return this.lightManager.getWorldLightInterface(pos, container);
	}

	@Override
	public void createLightStorage(final ExtendedBlockStorage blockStorage)
	{
		final TypedLightStorage<?, ?, ?, ?, ?, NibbleArray> lightStorage = this.lightManager.createLightStorage();
		((IVanillaLightStorageHolder) blockStorage).setLightStorage(lightStorage);
	}

	@Override
	public void createInitLightStorage(
		final Chunk chunk,
		final ExtendedBlockStorage blockStorage,
		@Nullable final ExtendedBlockStorage upperBlockStorage
	)
	{
		final TypedLightStorage<?, ?, ?, ?, ?, NibbleArray> lightStorage = this.lightHelper.createInitLightStorage(
			this.cachedPos.setPos(chunk.x << 4, blockStorage.getYLocation(), chunk.z << 4),
			upperBlockStorage == null ? null : ((IVanillaLightStorageHolder) upperBlockStorage).getLightStorage(),
			upperBlockStorage == null ? this.cachedPos : this.cachedUpperPos.setPos(chunk.x << 4, upperBlockStorage.getYLocation(), chunk.z << 4)
		);

		((IVanillaLightStorageHolder) blockStorage).setLightStorage(lightStorage);
	}

	@Override
	public boolean isSectionLightTrivial(final Chunk chunk, final ExtendedBlockStorage blockStorage, @Nullable final ExtendedBlockStorage upperBlockStorage)
	{
		return this.lightHelper.isLightTrivial(
			((IVanillaLightStorageHolder) blockStorage).getLightStorage(),
			this.cachedPos.setPos(chunk.x << 4, blockStorage.getYLocation(), chunk.z << 4),
			upperBlockStorage == null ? null : ((IVanillaLightStorageHolder) upperBlockStorage).getLightStorage(),
			upperBlockStorage == null ? this.cachedPos : this.cachedUpperPos.setPos(chunk.x << 4, upperBlockStorage.getYLocation(), chunk.z << 4)
		);
	}
}
