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

	private final boolean needsUpperStorage;

	private final LLC lightStorageContainer;
	private final LLC upperLightStorageContainer;
	private final SLC skyLightContainer;
	private final ELC emptySectionPredictorContainer;

	private final MutableBlockPos cachedPos = new MutableBlockPos();
	private final MutableBlockPos cachedUpperPos = new MutableBlockPos();

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

		this.needsUpperStorage = skyLightProvider != null || emptySectionLightPredictor != null;

		this.lightStorageContainer = lightStorageProvider.provider.createLightContainer();
		this.upperLightStorageContainer = lightStorageProvider.provider.createLightContainer();
		this.skyLightContainer = skyLightProvider == null ? null : skyLightProvider.provider.createLightContainer();
		this.emptySectionPredictorContainer = emptySectionLightPredictor == null ? null : emptySectionLightPredictor.predictor.createLightContainer();
	}

	public boolean needsUpperStorage()
	{
		return this.needsUpperStorage;
	}

	@SuppressWarnings("unchecked")
	public <T> ILightStorage<D, LI, WI, ?, ?, T> checkProviderType(final TypedLightStorage<?, ?, ?, ?, ?, T> lightStorage)
	{
		if (this.lightStorageProvider.type.lightProviderType != lightStorage.type.lightProviderType)
			throw new IllegalStateException("Incompatible light types");

		return (ILightStorage<D, LI, WI, ?, ?, T>) lightStorage.storage;
	}

	@SuppressWarnings("unchecked")
	public <T> ILightStorage<D, LI, WI, LLC, ?, T> checkCachedProviderType(final TypedLightStorage<?, ?, ?, ?, ?, T> lightStorage)
	{
		if (this.lightStorageProvider.type != lightStorage.type)
			throw new IllegalStateException("Incompatible light types");

		return (ILightStorage<D, LI, WI, LLC, ?, T>) lightStorage.storage;
	}

	public TypedLightStorage<D, LI, WI, LLC, ?, NibbleArray> createLightStorage()
	{
		return this.lightStorageProvider.createLightStorage();
	}

	public WI getWorldLightInterface(final BlockPos pos)
	{
		return this.getWorldLightInterface(pos, (ILightStorage<?, LI, WI, ?, ?, ?>) null, pos);
	}

	public WI getWorldLightInterface(
		final TypedLightStorage<?, ?, ?, ?, ?, ?> lightStorage,
		final BlockPos pos
	)
	{
		return this.getWorldLightInterface(this.checkProviderType(lightStorage), pos);
	}

	public WI getWorldLightInterface(
		final BlockPos pos,
		final @Nullable TypedLightStorage<?, ?, ?, ?, ?, ?> upperLightStorage,
		final BlockPos upperPos
	)
	{
		return this.getWorldLightInterface(
			pos,
			upperLightStorage == null ? null : this.checkProviderType(upperLightStorage),
			upperPos
		);
	}

	public WI getWorldLightInterface(
		final @Nullable TypedLightStorage<?, ?, ?, ?, ?, ?> lightStorage,
		final BlockPos pos,
		final @Nullable TypedLightStorage<?, ?, ?, ?, ?, ?> upperLightStorage,
		final BlockPos upperPos
	)
	{
		return this.getWorldLightInterface(
			lightStorage == null ? null : this.checkProviderType(lightStorage),
			pos,
			upperLightStorage == null ? null : this.checkProviderType(upperLightStorage),
			upperPos
		);
	}

	public WI getWorldLightInterface(
		final ILightStorage<?, ?, WI, ?, ?, ?> lightStorage,
		final BlockPos pos
	)
	{
		return lightStorage.getWorldLightInterface(new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15));
	}

	public WI getWorldLightInterface(
		final BlockPos pos,
		final @Nullable ILightStorage<?, LI, WI, ?, ?, ?> upperLightStorage,
		final BlockPos upperPos
	)
	{
		if (upperLightStorage == null)
		{
			if (this.skyLightProvider != null)
				return this.skyLightProvider.provider.getWorldLightInterface(pos);
		}
		else
		{
			if (this.emptySectionLightPredictor != null)
				return this.emptySectionLightPredictor.predictor.getWorldLightInterface(
					pos,
					upperPos,
					upperLightStorage.bind(
						new BlockPos(upperPos.getX() & 15, upperPos.getY() & 15, upperPos.getZ() & 15)
					).getInterface()
				);
		}

		return this.emptyLightProvider.provider.getWorldLightInterface(pos);
	}

	public WI getWorldLightInterface(
		final @Nullable ILightStorage<?, ?, WI, ?, ?, ?> lightStorage,
		final BlockPos pos,
		final @Nullable ILightStorage<?, LI, WI, ?, ?, ?> upperLightStorage,
		final BlockPos upperPos
	)
	{
		if (lightStorage != null)
			return this.getWorldLightInterface(lightStorage, pos);

		return this.getWorldLightInterface(pos, upperLightStorage, upperPos);
	}

	public Positioned<D, LI> getLightInterface(
		final BlockPos pos,
		final @Nullable ILightStorage<D, LI, ?, LLC, ?, ?> upperLightStorage,
		final BlockPos upperPos
	)
	{
		if (upperLightStorage == null)
			return this.getCachedSkyLightPositioned(pos);
		else
			return this.getCachedEmptySectionPredictorPositioned(
				pos,
				upperPos,
				upperLightStorage
			);
	}

	public Positioned<D, LI> getLightInterface(
		final @Nullable ILightStorage<D, LI, ?, LLC, ?, ?> lightStorage,
		final BlockPos pos,
		final @Nullable ILightStorage<D, LI, ?, LLC, ?, ?> upperLightStorage,
		final BlockPos upperPos
	)
	{
		if (lightStorage != null)
			return this.getCachedPositioned(pos, lightStorage);

		return this.getLightInterface(pos, upperLightStorage, upperPos);
	}

	public static <D, LI, LC> Writeable<D, LI> getCachedPositioned(
		final BlockPos pos,
		final MutableBlockPos cachedPos,
		final ILightStorage<D, LI, ?, LC, ?, ?> lightStorage,
		final LC container
	)
	{
		return lightStorage.bind(
			cachedPos.setPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15),
			container
		);
	}

	public Writeable<D, LI> getCachedPositioned(final BlockPos pos, final ILightStorage<D, LI, ?, LLC, ?, ?> lightStorage)
	{
		return getCachedPositioned(pos, this.cachedPos, lightStorage, this.lightStorageContainer);
	}

	public Writeable<D, LI> getCachedUpperPositioned(final BlockPos upperPos, final ILightStorage<D, LI, ?, LLC, ?, ?> upperLightStorage)
	{
		return getCachedPositioned(upperPos, this.cachedUpperPos, upperLightStorage, this.upperLightStorageContainer);
	}

	public Positioned<D, LI> getCachedSkyLightPositioned(final BlockPos pos)
	{
		if (this.skyLightProvider == null)
			return this.emptyLightProvider.provider.bind(pos);

		return this.skyLightProvider.provider.bind(pos, this.skyLightContainer);
	}

	public Positioned<D, LI> getCachedEmptySectionPredictorPositioned(
		final BlockPos pos,
		final BlockPos upperPos,
		final LI upperLightInterface
	)
	{
		if (this.emptySectionLightPredictor == null)
			return this.emptyLightProvider.provider.bind(pos);

		return this.emptySectionLightPredictor.predictor.bind(
			pos,
			upperPos,
			upperLightInterface,
			this.emptySectionPredictorContainer
		);
	}

	public Positioned<D, LI> getCachedEmptySectionPredictorPositioned(
		final BlockPos pos,
		final BlockPos upperPos,
		final ILightStorage<D, LI, ?, LLC, ?, ?> upperLightStorage
	)
	{
		return this.getCachedEmptySectionPredictorPositioned(
			pos,
			upperPos,
			this.getCachedUpperPositioned(upperPos, upperLightStorage).getInterface()
		);
	}
}
