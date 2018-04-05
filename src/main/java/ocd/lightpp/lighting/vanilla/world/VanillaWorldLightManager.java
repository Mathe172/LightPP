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

package ocd.lightpp.lighting.vanilla.world;

import javax.annotation.Nullable;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.chunk.NibbleArray;
import ocd.lightpp.api.vanilla.type.CachedLightProviderType.TypedCachedLightProvider;
import ocd.lightpp.api.vanilla.type.ContainerType;
import ocd.lightpp.api.vanilla.type.ContainerType.TypedContainer;
import ocd.lightpp.api.vanilla.type.LightProviderType.TypedLightProvider;
import ocd.lightpp.api.vanilla.type.TypedEmptySectionLightPredictor;
import ocd.lightpp.api.vanilla.type.TypedLightStorage;
import ocd.lightpp.api.vanilla.type.TypedLightStorageProvider;
import ocd.lightpp.api.vanilla.world.ILightProvider.Positioned;
import ocd.lightpp.api.vanilla.world.ILightProvider.Positioned.Writeable;
import ocd.lightpp.api.vanilla.world.ILightStorage;

public class VanillaWorldLightManager<D, LI, WI, LLC, LWC, SLC, SWC, ELC, EWC>
{
	public final TypedLightStorageProvider<D, LI, WI, LLC, LWC, NibbleArray> lightStorageProvider;
	public final @Nullable TypedCachedLightProvider<D, LI, WI, SLC, SWC> skyLightProvider;
	public final @Nullable TypedEmptySectionLightPredictor<D, LI, WI, ELC, EWC> emptySectionLightPredictor;
	public final TypedLightProvider<D, LI, WI> emptyLightProvider;

	public final ContainerType<LightContainer<LLC, SLC, ELC>> lightContainerType;
	public final ContainerType<WorldLightContainer<LLC, LWC, SWC, EWC>> worldLightContainerType;

	private final boolean needsUpperStorage;

	public VanillaWorldLightManager(
		final TypedLightStorageProvider<D, LI, WI, LLC, LWC, NibbleArray> lightStorageProvider,
		@Nullable final TypedCachedLightProvider<D, LI, WI, SLC, SWC> skyLightProvider,
		@Nullable final TypedEmptySectionLightPredictor<D, LI, WI, ELC, EWC> emptySectionLightPredictor,
		final TypedLightProvider<D, LI, WI> emptyLightProvider
	)
	{
		this.lightStorageProvider = lightStorageProvider;
		this.skyLightProvider = skyLightProvider;
		this.emptySectionLightPredictor = emptySectionLightPredictor;
		this.emptyLightProvider = emptyLightProvider;

		this.lightContainerType = new ContainerType<>();
		this.worldLightContainerType = new ContainerType<>();

		this.needsUpperStorage = skyLightProvider != null || emptySectionLightPredictor != null;
	}

	public boolean needsUpperStorage()
	{
		return this.needsUpperStorage;
	}

	@SuppressWarnings("unchecked")
	public <T> ILightStorage<D, ? extends LI, ? extends WI, ?, ?, T> checkProviderType(final TypedLightStorage<?, ?, ?, ?, ?, T> lightStorage)
	{
		if (this.lightStorageProvider.type.lightProviderType != lightStorage.type.lightProviderType)
			throw new IllegalStateException("Incompatible light types");

		return (ILightStorage<D, ? extends LI, ? extends WI, ?, ?, T>) lightStorage.storage;
	}

	@SuppressWarnings("unchecked")
	public <T> ILightStorage<D, ? extends LI, ? extends WI, LLC, LWC, T> checkCachedProviderType(final TypedLightStorage<?, ?, ?, ?, ?, T> lightStorage)
	{
		if (this.lightStorageProvider.type != lightStorage.type)
			throw new IllegalStateException("Incompatible light types");

		return (ILightStorage<D, ? extends LI, ? extends WI, LLC, LWC, T>) lightStorage.storage;
	}

	@SuppressWarnings("unchecked")
	public LightContainer<LLC, SLC, ELC> checkLightContainerType(final TypedContainer<?> container)
	{
		if (this.lightContainerType != container.type)
			throw new IllegalStateException("Incompatible container types");

		return (LightContainer<LLC, SLC, ELC>) container.container;
	}

	@SuppressWarnings("unchecked")
	public WorldLightContainer<LLC, LWC, SWC, EWC> checkWorldLightContainerType(final TypedContainer<?> container)
	{
		if (this.worldLightContainerType != container.type)
			throw new IllegalStateException("Incompatible container types");

		return (WorldLightContainer<LLC, LWC, SWC, EWC>) container.container;
	}

	public TypedLightStorage<D, LI, WI, LLC, ?, NibbleArray> createLightStorage()
	{
		return this.lightStorageProvider.createLightStorage();
	}

	public WI getWorldLightInterface(final BlockPos pos, @Nullable final TypedContainer<?> container)
	{
		if (container == null)
			return this.getWorldLightInterface(pos, null, pos);

		return this.getWorldLightInterface(pos, null, pos, this.checkWorldLightContainerType(container));
	}

	public WI getWorldLightInterface(
		final TypedLightStorage<?, ?, ?, ?, ?, ?> lightStorage,
		final BlockPos pos,
		final @Nullable TypedContainer<?> container
	)
	{
		if (container == null)
			return getWorldLightInterface(pos, this.checkProviderType(lightStorage));

		return getWorldLightInterface(pos, this.checkCachedProviderType(lightStorage), this.checkWorldLightContainerType(container));
	}

	public WI getWorldLightInterface(
		final BlockPos pos,
		final @Nullable TypedLightStorage<?, ?, ?, ?, ?, ?> upperLightStorage,
		final BlockPos upperPos,
		final @Nullable TypedContainer<?> container
	)
	{
		if (container == null)
			return this.getWorldLightInterface(
				pos,
				upperLightStorage == null ? null : this.checkProviderType(upperLightStorage),
				upperPos
			);

		return this.getWorldLightInterface(
			pos,
			upperLightStorage == null ? null : this.checkCachedProviderType(upperLightStorage),
			upperPos,
			this.checkWorldLightContainerType(container)
		);
	}

	public WI getWorldLightInterface(
		final @Nullable TypedLightStorage<?, ?, ?, ?, ?, ?> lightStorage,
		final BlockPos pos,
		final @Nullable TypedLightStorage<?, ?, ?, ?, ?, ?> upperLightStorage,
		final BlockPos upperPos,
		final @Nullable TypedContainer<?> container
	)
	{
		if (container == null)
			return this.getWorldLightInterface(
				lightStorage == null ? null : this.checkProviderType(lightStorage),
				pos,
				upperLightStorage == null ? null : this.checkProviderType(upperLightStorage),
				upperPos
			);

		return this.getWorldLightInterface(
			lightStorage == null ? null : this.checkCachedProviderType(lightStorage),
			pos,
			upperLightStorage == null ? null : this.checkCachedProviderType(upperLightStorage),
			upperPos,
			this.checkWorldLightContainerType(container)
		);
	}

	public WI getWorldLightInterface(
		final @Nullable ILightStorage<?, ?, ? extends WI, ?, ?, ?> lightStorage,
		final BlockPos pos,
		final @Nullable ILightStorage<?, ? extends LI, ?, ?, ?, ?> upperLightStorage,
		final BlockPos upperPos
	)
	{
		if (lightStorage != null)
			return getWorldLightInterface(pos, lightStorage);

		return this.getWorldLightInterface(pos, upperLightStorage, upperPos);
	}

	public WI getWorldLightInterface(
		final @Nullable ILightStorage<?, ?, ? extends WI, ?, LWC, ?> lightStorage,
		final BlockPos pos,
		final @Nullable ILightStorage<?, ? extends LI, ?, LLC, ?, ?> upperLightStorage,
		final BlockPos upperPos,
		final WorldLightContainer<LLC, LWC, SWC, EWC> container
	)
	{
		if (lightStorage != null)
			return getWorldLightInterface(pos, lightStorage, container);

		return this.getWorldLightInterface(pos, upperLightStorage, upperPos, container);
	}

	public WI getWorldLightInterface(
		final BlockPos pos,
		final @Nullable ILightStorage<?, ? extends LI, ?, ?, ?, ?> upperLightStorage,
		final BlockPos upperPos
	)
	{
		if (upperLightStorage == null)
			return this.getSkyLightWorldLightInterface(pos);
		else
			return this.getEmptySectionPredictorWorldLightInterface(
				pos,
				upperPos,
				getWriteable(pos, upperLightStorage).getInterface()
			);
	}

	public WI getWorldLightInterface(
		final BlockPos pos,
		final @Nullable ILightStorage<?, ? extends LI, ?, LLC, ?, ?> upperLightStorage,
		final BlockPos upperPos,
		final WorldLightContainer<LLC, LWC, SWC, EWC> container
	)
	{
		if (upperLightStorage == null)
			return this.getSkyLightWorldLightInterface(pos, container);
		else
			return this.getEmptySectionPredictorWorldLightInterface(
				pos,
				upperPos,
				getWriteable(pos, upperLightStorage, container).getInterface(),
				container
			);
	}

	public static <D, WI, WC> WI getWorldLightInterface(
		final BlockPos pos,
		final ILightStorage<?, ?, WI, ?, WC, ?> lightStorage,
		final WorldLightContainer<?, WC, ?, ?> container
	)
	{
		return getWorldLightInterface(
			pos,
			container.cachedPos,
			lightStorage,
			container.lightStorageWorldLightContainer
		);
	}

	public static <D, WI> WI getWorldLightInterface(
		final BlockPos pos,
		final ILightStorage<?, ?, WI, ?, ?, ?> lightStorage
	)
	{
		return lightStorage.getWorldLightInterface(getLocalPos(pos));
	}

	public static <D, WI, WC> WI getWorldLightInterface(
		final BlockPos pos,
		final MutableBlockPos cachedPos,
		final ILightStorage<?, ?, WI, ?, WC, ?> lightStorage,
		final WC container
	)
	{
		return lightStorage.getWorldLightInterface(cachedPos.setPos(getLocalPos(pos)), container);
	}

	public WI getSkyLightWorldLightInterface(final BlockPos pos, final WorldLightContainer<?, ?, SWC, ?> container)
	{
		return this.getSkyLightWorldLightInterface(pos, container.skyLightWorldLightContainer);
	}

	public WI getSkyLightWorldLightInterface(final BlockPos pos)
	{
		if (this.skyLightProvider == null)
			return this.emptyLightProvider.provider.getWorldLightInterface(pos);

		return this.skyLightProvider.provider.getWorldLightInterface(pos);
	}

	public WI getSkyLightWorldLightInterface(final BlockPos pos, final SWC container)
	{
		if (this.skyLightProvider == null)
			return this.emptyLightProvider.provider.getWorldLightInterface(pos);

		return this.skyLightProvider.provider.getWorldLightInterface(pos, container);
	}

	public WI getEmptySectionPredictorWorldLightInterface(
		final BlockPos pos,
		final BlockPos upperPos,
		final LI upperLightInterface,
		final WorldLightContainer<?, ?, ?, EWC> container
	)
	{
		return this.getEmptySectionPredictorWorldLightInterface(
			pos,
			upperPos,
			upperLightInterface,
			container.emptySectionPredictorWorldLightContainer
		);
	}

	public WI getEmptySectionPredictorWorldLightInterface(
		final BlockPos pos,
		final BlockPos upperPos,
		final LI upperLightInterface
	)
	{
		if (this.emptySectionLightPredictor == null)
			return this.emptyLightProvider.provider.getWorldLightInterface(pos);

		return this.emptySectionLightPredictor.predictor.getWorldLightInterface(pos, upperPos, upperLightInterface);
	}

	public WI getEmptySectionPredictorWorldLightInterface(
		final BlockPos pos,
		final BlockPos upperPos,
		final LI upperLightInterface,
		final EWC container
	)
	{
		if (this.emptySectionLightPredictor == null)
			return this.emptyLightProvider.provider.getWorldLightInterface(pos);

		return this.emptySectionLightPredictor.predictor.getWorldLightInterface(pos, upperPos, upperLightInterface, container);
	}

	public Positioned<D, ? extends LI> getLightInterface(
		final @Nullable ILightStorage<D, ? extends LI, ?, ?, ?, ?> lightStorage,
		final BlockPos pos,
		final @Nullable ILightStorage<D, ? extends LI, ?, ?, ?, ?> upperLightStorage,
		final BlockPos upperPos
	)
	{
		if (lightStorage != null)
			return getWriteable(pos, lightStorage);

		return this.getLightInterface(pos, upperLightStorage, upperPos);
	}

	public Positioned<D, ? extends LI> getLightInterface(
		final @Nullable ILightStorage<D, ? extends LI, ?, LLC, ?, ?> lightStorage,
		final BlockPos pos,
		final @Nullable ILightStorage<D, ? extends LI, ?, LLC, ?, ?> upperLightStorage,
		final BlockPos upperPos,
		final LightContainer<LLC, SLC, ELC> container
	)
	{
		if (lightStorage != null)
			return getWriteable(pos, lightStorage, container);

		return this.getLightInterface(pos, upperLightStorage, upperPos, container);
	}

	public Positioned<D, ? extends LI> getLightInterface(
		final BlockPos pos,
		final @Nullable ILightStorage<D, ? extends LI, ?, ?, ?, ?> upperLightStorage,
		final BlockPos upperPos
	)
	{
		if (upperLightStorage == null)
			return this.getSkyLightPositioned(pos);
		else
			return this.getEmptySectionPredictorPositioned(
				pos,
				upperPos,
				getWriteable(pos, upperLightStorage).getInterface()
			);
	}

	public Positioned<D, ? extends LI> getLightInterface(
		final BlockPos pos,
		final @Nullable ILightStorage<D, ? extends LI, ?, LLC, ?, ?> upperLightStorage,
		final BlockPos upperPos,
		final LightContainer<LLC, SLC, ELC> container
	)
	{
		if (upperLightStorage == null)
			return this.getSkyLightPositioned(pos, container);
		else
			return this.getEmptySectionPredictorPositioned(
				pos,
				upperPos,
				getWriteable(pos, upperLightStorage, container).getInterface(),
				container
			);
	}

	public static <D, LI, LC> Writeable<D, LI> getWriteable(
		final BlockPos pos,
		final ILightStorage<D, LI, ?, LC, ?, ?> lightStorage,
		final Container<LC> container
	)
	{
		return getWriteable(
			pos,
			container.cachedPos,
			lightStorage,
			container.lightStorageLightContainer
		);
	}

	public static <D, LI> Writeable<D, LI> getWriteable(
		final BlockPos pos,
		final ILightStorage<D, LI, ?, ?, ?, ?> lightStorage
	)
	{
		return lightStorage.bind(getLocalPos(pos));
	}

	public static <D, LI, LC> Writeable<D, LI> getWriteable(
		final BlockPos pos,
		final MutableBlockPos cachedPos,
		final ILightStorage<D, LI, ?, LC, ?, ?> lightStorage,
		final LC container
	)
	{
		return lightStorage.bind(cachedPos.setPos(getLocalPos(pos)), container);
	}

	public Positioned<D, ? extends LI> getSkyLightPositioned(final BlockPos pos, final LightContainer<?, SLC, ?> container)
	{
		return this.getSkyLightPositioned(pos, container.skyLightLightContainer);
	}

	public Positioned<D, ? extends LI> getSkyLightPositioned(final BlockPos pos)
	{
		if (this.skyLightProvider == null)
			return this.emptyLightProvider.provider.bind(pos);

		return this.skyLightProvider.provider.bind(pos);
	}

	public Positioned<D, ? extends LI> getSkyLightPositioned(final BlockPos pos, final SLC container)
	{
		if (this.skyLightProvider == null)
			return this.emptyLightProvider.provider.bind(pos);

		return this.skyLightProvider.provider.bind(pos, container);
	}

	public Positioned<D, ? extends LI> getEmptySectionPredictorPositioned(
		final BlockPos pos,
		final BlockPos upperPos,
		final LI upperLightInterface,
		final LightContainer<?, ?, ELC> container
	)
	{
		return this.getEmptySectionPredictorPositioned(
			pos,
			upperPos,
			upperLightInterface,
			container.emptySectionPredictorLightContainer
		);
	}

	public Positioned<D, ? extends LI> getEmptySectionPredictorPositioned(
		final BlockPos pos,
		final BlockPos upperPos,
		final LI upperLightInterface
	)
	{
		if (this.emptySectionLightPredictor == null)
			return this.emptyLightProvider.provider.bind(pos);

		return this.emptySectionLightPredictor.predictor.bind(pos, upperPos, upperLightInterface);
	}

	public Positioned<D, ? extends LI> getEmptySectionPredictorPositioned(
		final BlockPos pos,
		final BlockPos upperPos,
		final LI upperLightInterface,
		final ELC container
	)
	{
		if (this.emptySectionLightPredictor == null)
			return this.emptyLightProvider.provider.bind(pos);

		return this.emptySectionLightPredictor.predictor.bind(pos, upperPos, upperLightInterface, container);
	}

	private static BlockPos getLocalPos(final BlockPos pos)
	{
		return new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
	}

	public TypedContainer<LightContainer<LLC, SLC, ELC>> createLightContainer()
	{
		return new TypedContainer<>(
			this.lightContainerType,
			new LightContainer<>(
				this.lightStorageProvider.provider.createLightContainer(),
				this.skyLightProvider == null ? null : this.skyLightProvider.provider.createLightContainer(),
				this.emptySectionLightPredictor == null ? null : this.emptySectionLightPredictor.predictor.createLightContainer()
			)
		);
	}

	public TypedContainer<WorldLightContainer<LLC, LWC, SWC, EWC>> createWorldLightContainer()
	{
		return new TypedContainer<>(
			this.worldLightContainerType,
			new WorldLightContainer<>(
				this.lightStorageProvider.provider.createLightContainer(),
				this.lightStorageProvider.provider.createWorldLightContainer(),
				this.skyLightProvider == null ? null : this.skyLightProvider.provider.createWorldLightContainer(),
				this.emptySectionLightPredictor == null ? null : this.emptySectionLightPredictor.predictor.createWorldLightContainer()
			)
		);
	}

	public static class Container<LLC>
	{
		public final MutableBlockPos cachedPos = new MutableBlockPos();

		public final LLC lightStorageLightContainer;

		public Container(final LLC lightStorageLightContainer)
		{
			this.lightStorageLightContainer = lightStorageLightContainer;
		}
	}

	public static class LightContainer<LLC, SLC, ELC> extends Container<LLC>
	{
		public final SLC skyLightLightContainer;
		public final ELC emptySectionPredictorLightContainer;

		LightContainer(
			final LLC lightStorageLightContainer,
			final SLC skyLightLightContainer,
			final ELC emptySectionPredictorLightContainer
		)
		{
			super(lightStorageLightContainer);

			this.skyLightLightContainer = skyLightLightContainer;
			this.emptySectionPredictorLightContainer = emptySectionPredictorLightContainer;
		}
	}

	public static class WorldLightContainer<LLC, LWC, SWC, EWC> extends Container<LLC>
	{
		public final LWC lightStorageWorldLightContainer;
		public final SWC skyLightWorldLightContainer;
		public final EWC emptySectionPredictorWorldLightContainer;

		WorldLightContainer(
			final LLC lightStorageLightContainer,
			final LWC lightStorageWorldLightContainer,
			final SWC skyLightWorldLightContainer,
			final EWC emptySectionPredictorWorldLightContainer
		)
		{
			super(lightStorageLightContainer);

			this.lightStorageWorldLightContainer = lightStorageWorldLightContainer;
			this.skyLightWorldLightContainer = skyLightWorldLightContainer;
			this.emptySectionPredictorWorldLightContainer = emptySectionPredictorWorldLightContainer;
		}
	}
}
