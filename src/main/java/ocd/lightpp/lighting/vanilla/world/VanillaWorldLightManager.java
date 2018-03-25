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
import ocd.lightpp.api.vanilla.world.ILightProvider.Positioned;
import ocd.lightpp.api.vanilla.world.ILightProvider.Positioned.Writeable;
import ocd.lightpp.api.vanilla.world.ILightStorage;
import ocd.lightpp.api.vanilla.world.ILightStorageProvider;

public class VanillaWorldLightManager<D, LI, WI, CL, CS, CE>
{
	public final ILightStorageProvider<D, LI, WI, CL, NibbleArray> lightStorageProvider;
	public final @Nullable TypedCachedLightProvider<D, LI, WI, CS> skyLightProvider;
	public final @Nullable TypedEmptySectionLightPredictor<D, LI, WI, CE> emptySectionLightPredictor;
	public final TypedLightProvider<D, LI, WI> emptyLightProvider;

	private final boolean needsUpperStorage;

	private final CL lightStorageContainer;
	private final CL upperLightStorageContainer;
	private final CS skyLightContainer;
	private final CE emptySectionPredictorContainer;

	private final MutableBlockPos cachedPos = new MutableBlockPos();
	private final MutableBlockPos cachedUpperPos = new MutableBlockPos();

	public VanillaWorldLightManager(
		final ILightStorageProvider<D, LI, WI, CL, NibbleArray> lightStorageProvider,
		@Nullable final TypedCachedLightProvider<D, LI, WI, CS> skyLightProvider,
		@Nullable final TypedEmptySectionLightPredictor<D, LI, WI, CE> emptySectionLightPredictor,
		final TypedLightProvider<D, LI, WI> emptyLightProvider
	)
	{
		this.lightStorageProvider = lightStorageProvider;
		this.skyLightProvider = skyLightProvider;
		this.emptySectionLightPredictor = emptySectionLightPredictor;
		this.emptyLightProvider = emptyLightProvider;

		this.needsUpperStorage = skyLightProvider != null || emptySectionLightPredictor != null;

		this.lightStorageContainer = lightStorageProvider.createContainer();
		this.upperLightStorageContainer = lightStorageProvider.createContainer();
		this.skyLightContainer = skyLightProvider == null ? null : skyLightProvider.createContainer();
		this.emptySectionPredictorContainer = emptySectionLightPredictor == null ? null : emptySectionLightPredictor.createContainer();
	}

	public boolean needsUpperStorage()
	{
		return this.needsUpperStorage;
	}

	@SuppressWarnings("unchecked")
	public ILightStorage<D, LI, WI, ?, ?> checkProviderType(final TypedLightStorage<?, ?, ?, ?, ?> lightStorage)
	{
		if (this.lightStorageProvider.getType().lightProviderType != lightStorage.type.lightProviderType)
			throw new IllegalStateException("Incompatible light types");

		return (ILightStorage<D, LI, WI, ?, ?>) lightStorage;
	}

	@SuppressWarnings("unchecked")
	public ILightStorage<D, LI, WI, CL, ?> checkCachedProviderType(final TypedLightStorage<?, ?, ?, ?, ?> lightStorage)
	{
		if (this.lightStorageProvider.getType() != lightStorage.type)
			throw new IllegalStateException("Incompatible light types");

		return (ILightStorage<D, LI, WI, CL, ?>) lightStorage;
	}

	public WI getWorldLightInterface(final BlockPos pos)
	{
		return this.getWorldLightInterface(pos, (ILightStorage<?, LI, WI, ?, ?>) null, pos);
	}

	public WI getWorldLightInterface(
		final TypedLightStorage<?, ?, ?, ?, ?> lightStorage,
		final BlockPos pos
	)
	{
		return this.getWorldLightInterface(this.checkProviderType(lightStorage), pos);
	}

	public WI getWorldLightInterface(
		final BlockPos pos,
		final @Nullable TypedLightStorage<?, ?, ?, ?, ?> upperLightStorage,
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
		final @Nullable TypedLightStorage<?, ?, ?, ?, ?> lightStorage,
		final BlockPos pos,
		final @Nullable TypedLightStorage<?, ?, ?, ?, ?> upperLightStorage,
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
		final ILightStorage<?, ?, WI, ?, ?> lightStorage,
		final BlockPos pos
	)
	{
		return lightStorage.getStorageInterface(new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15));
	}

	public WI getWorldLightInterface(
		final BlockPos pos,
		final @Nullable ILightStorage<?, LI, WI, ?, ?> upperLightStorage,
		final BlockPos upperPos
	)
	{
		if (upperLightStorage == null)
		{
			if (this.skyLightProvider != null)
				return this.skyLightProvider.provider.getStorageInterface(pos);
		}
		else
		{
			if (this.emptySectionLightPredictor != null)
				return this.emptySectionLightPredictor.predictor.getStorageInterface(
					pos,
					upperPos,
					upperLightStorage.bind(
						new BlockPos(upperPos.getX() & 15, upperPos.getY() & 15, upperPos.getZ() & 15)
					).getInterface()
				);
		}

		return this.emptyLightProvider.provider.getStorageInterface(pos);
	}

	public WI getWorldLightInterface(
		final @Nullable ILightStorage<?, ?, WI, ?, ?> lightStorage,
		final BlockPos pos,
		final @Nullable ILightStorage<?, LI, WI, ?, ?> upperLightStorage,
		final BlockPos upperPos
	)
	{
		if (lightStorage != null)
			return this.getWorldLightInterface(lightStorage, pos);

		return this.getWorldLightInterface(pos, upperLightStorage, upperPos);
	}

	public Positioned<D, LI> getLightInterface(
		final BlockPos pos,
		final @Nullable ILightStorage<D, LI, ?, CL, ?> upperLightStorage,
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
		final @Nullable ILightStorage<D, LI, ?, CL, ?> lightStorage,
		final BlockPos pos,
		final @Nullable ILightStorage<D, LI, ?, CL, ?> upperLightStorage,
		final BlockPos upperPos
	)
	{
		if (lightStorage != null)
			return this.getCachedPositioned(pos, lightStorage);

		return this.getLightInterface(pos, upperLightStorage, upperPos);
	}

	public static <D, LI, CL> Writeable<D, LI> getCachedPositioned(
		final BlockPos pos,
		final MutableBlockPos cachedPos,
		final ILightStorage<D, LI, ?, CL, ?> lightStorage,
		final CL container
	)
	{
		return lightStorage.bind(
			cachedPos.setPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15),
			container
		);
	}

	public Writeable<D, LI> getCachedPositioned(final BlockPos pos, final ILightStorage<D, LI, ?, CL, ?> lightStorage)
	{
		return getCachedPositioned(pos, this.cachedPos, lightStorage, this.lightStorageContainer);
	}

	public Writeable<D, LI> getCachedUpperPositioned(final BlockPos upperPos, final ILightStorage<D, LI, ?, CL, ?> upperLightStorage)
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
		final ILightStorage<D, LI, ?, CL, ?> upperLightStorage
	)
	{
		return this.getCachedEmptySectionPredictorPositioned(
			pos,
			upperPos,
			this.getCachedUpperPositioned(upperPos, upperLightStorage).getInterface()
		);
	}
}
