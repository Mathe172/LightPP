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
import ocd.lightpp.api.lighting.ILightMap.ILightIterator;
import ocd.lightpp.api.vanilla.type.CachedLightProviderType.TypedCachedLightProvider;
import ocd.lightpp.api.vanilla.type.LightProviderType.TypedLightProvider;
import ocd.lightpp.api.vanilla.type.TypedEmptySectionLightPredictor;
import ocd.lightpp.api.vanilla.type.TypedLightStorage;
import ocd.lightpp.api.vanilla.world.ILightProvider.Positioned;
import ocd.lightpp.api.vanilla.world.ILightProvider.Positioned.Writeable;
import ocd.lightpp.api.vanilla.world.ILightStorage;
import ocd.lightpp.api.vanilla.world.ILightStorageProvider;

public class VanillaWorldLightHelper<D, LI, WI, CL, CS, CE> extends VanillaWorldLightManager<D, LI, WI, CL, CS, CE>
{
	private final MutableBlockPos cachedIterPos = new MutableBlockPos();
	private final MutableBlockPos cachedIterUpperPos = new MutableBlockPos();

	public VanillaWorldLightHelper(
		final ILightStorageProvider<D, LI, WI, CL, NibbleArray> lightStorageProvider,
		@Nullable final TypedCachedLightProvider<D, LI, WI, CS> skyLightProvider,
		@Nullable final TypedEmptySectionLightPredictor<D, LI, WI, CE> emptySectionLightPredictor,
		final TypedLightProvider<D, LI, WI> emptyLightProvider
	)
	{
		super(lightStorageProvider, skyLightProvider, emptySectionLightPredictor, emptyLightProvider);
	}

	public void initLight(
		final TypedLightStorage<?, ?, ?, ?, ?> lightStorage,
		final BlockPos basePos,
		final @Nullable TypedLightStorage<?, ?, ?, ?, ?> upperLightStorage,
		final BlockPos upperBasePos
	)
	{
		this.initLight(
			this.checkCachedProviderType(lightStorage),
			basePos,
			upperLightStorage == null ? null : this.checkCachedProviderType(upperLightStorage),
			upperBasePos
		);
	}

	public void initLight(
		final ILightStorage<D, LI, ?, CL, ?> lightStorage,
		final BlockPos basePos,
		final @Nullable ILightStorage<D, LI, ?, CL, ?> upperLightStorage,
		final BlockPos upperBasePos
	)
	{
		if (upperLightStorage == null)
		{
			if (this.skyLightProvider == null)
				return;

			for (int x = 0; x < 16; ++x)
				for (int z = 0; z < 16; ++z)
					for (int y = 0; y < 16; ++y)
					{
						final Positioned<D, LI> lightInterface = this.getCachedSkyLightPositioned(
							this.cachedIterPos.setPos(basePos.getX() + x, basePos.getY() + y, basePos.getZ() + z)
						);

						this.copyLightData(lightStorage, lightInterface);
					}
		}
		else
		{
			if (this.emptySectionLightPredictor == null)
				return;

			for (int x = 0; x < 16; ++x)
				for (int z = 0; z < 16; ++z)
				{
					final LI upperLightInterface = this.getCachedUpperPositioned(
						this.cachedIterUpperPos.setPos(upperBasePos.getX() + x, upperBasePos.getY(), upperBasePos.getZ() + z),
						upperLightStorage
					).getInterface();

					for (int y = 0; y < 16; ++y)
					{
						final Positioned<D, LI> lightInterface = this.getCachedEmptySectionPredictorPositioned(
							this.cachedIterPos.setPos(basePos.getX() + x, basePos.getY() + y, basePos.getZ() + z),
							this.cachedIterUpperPos,
							upperLightInterface
						);

						this.copyLightData(lightStorage, lightInterface);
					}
				}
		}
	}

	public TypedLightStorage<D, LI, WI, CL, NibbleArray> createInitLightStorage(
		final BlockPos basePos,
		final @Nullable TypedLightStorage<?, ?, ?, ?, ?> upperLightStorage,
		final BlockPos upperBasePos
	)
	{
		return this.createInitLightStorage(
			basePos,
			upperLightStorage == null ? null : this.checkCachedProviderType(upperLightStorage),
			upperBasePos
		);
	}

	public TypedLightStorage<D, LI, WI, CL, NibbleArray> createInitLightStorage(
		final BlockPos basePos,
		final @Nullable ILightStorage<D, LI, ?, CL, ?> upperLightStorage,
		final BlockPos upperBasePos
	)
	{
		final TypedLightStorage<D, LI, WI, CL, NibbleArray> lightStorage = this.createLightStorage();
		this.initLight(lightStorage.storage, basePos, upperLightStorage, upperBasePos);

		return lightStorage;
	}

	private void copyLightData(final ILightStorage<D, LI, ?, CL, ?> lightStorage, final Positioned<D, LI> lightInterface)
	{
		final Writeable<D, LI> writeAccess = this.getCachedPositioned(this.cachedIterPos, lightStorage);

		for (final ILightIterator<D> it = lightInterface.getLightIterator(); it.next(); )
		{
			final int light = it.getLight();

			if (light == 0)
				continue;

			final D desc = it.getDescriptor();

			writeAccess.set(desc, it.getLight());
			writeAccess.notifyLightSet(desc);
		}

		writeAccess.notifyLightSet();
	}

	public boolean isLightTrivial(
		final TypedLightStorage<?, ?, ?, ?, ?> lightStorage,
		final BlockPos basePos,
		final @Nullable TypedLightStorage<?, ?, ?, ?, ?> upperLightStorage,
		final BlockPos upperBasePos
	)
	{
		return this.isLightTrivial(
			this.checkCachedProviderType(lightStorage),
			basePos,
			upperLightStorage == null ? null : this.checkCachedProviderType(upperLightStorage),
			upperBasePos
		);
	}

	public boolean isLightTrivial(
		final ILightStorage<D, LI, ?, CL, ?> lightStorage,
		final BlockPos basePos,
		final @Nullable ILightStorage<D, LI, ?, CL, ?> upperLightStorage,
		final BlockPos upperBasePos
	)
	{
		if (upperLightStorage == null)
		{
			if (this.skyLightProvider == null)
				return true;

			for (int x = 0; x < 16; ++x)
				for (int z = 0; z < 16; ++z)
					for (int y = 0; y < 16; ++y)
					{
						final Positioned<D, LI> refLightInterface = this.getCachedSkyLightPositioned(
							this.cachedIterPos.setPos(basePos.getX() + x, basePos.getY() + y, basePos.getZ() + z)
						);

						if (this.compareLightData(lightStorage, refLightInterface))
							return false;
					}
		}
		else
		{
			if (this.emptySectionLightPredictor == null)
				return true;

			for (int x = 0; x < 16; ++x)
				for (int z = 0; z < 16; ++z)
				{
					final LI upperLightInterface = this.getCachedUpperPositioned(
						this.cachedIterUpperPos.setPos(upperBasePos.getX() + x, upperBasePos.getY(), upperBasePos.getZ() + z),
						upperLightStorage
					).getInterface();

					for (int y = 0; y < 16; ++y)
					{
						final Positioned<D, LI> refLightInterface = this.getCachedEmptySectionPredictorPositioned(
							this.cachedIterPos.setPos(basePos.getX() + x, basePos.getY() + y, basePos.getZ() + z),
							this.cachedIterUpperPos,
							upperLightInterface
						);

						if (!this.compareLightData(lightStorage, refLightInterface))
							return false;
					}
				}
		}

		return true;
	}

	private boolean compareLightData(final ILightStorage<D, LI, ?, CL, ?> lightStorage, final Positioned<D, LI> refLightInterface)
	{
		final Positioned<D, LI> lightInterface = this.getCachedPositioned(this.cachedIterPos, lightStorage);

		for (final ILightIterator<D> it = refLightInterface.getLightIterator(); it.next(); )
			if (lightInterface.getLight(it.getDescriptor()) != it.getLight())
				return false;

		for (final ILightIterator<D> it = lightInterface.getLightIterator(); it.next(); )
			if (refLightInterface.getLight(it.getDescriptor()) != it.getLight())
				return false;

		return true;
	}
}
