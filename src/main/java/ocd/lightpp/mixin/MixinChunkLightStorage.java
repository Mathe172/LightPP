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

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import ocd.lightpp.api.vanilla.light.IVanillaLightWorldInterface;
import ocd.lightpp.api.vanilla.type.CachedLightProviderType.TypedCachedLightProvider;
import ocd.lightpp.api.vanilla.type.TypedEmptySectionLightPredictor;
import ocd.lightpp.api.vanilla.type.TypedLightStorage;
import ocd.lightpp.api.vanilla.world.IVanillaChunkLightProvider;
import ocd.lightpp.api.vanilla.world.IVanillaLightStorageHolder;
import ocd.lightpp.api.vanilla.world.IVanillaWorldLightProvider;
import ocd.lightpp.impl.IChunkLightStorageInitializer;
import ocd.lightpp.impl.IWorldLightStorageInitializer;

@Mixin(Chunk.class)
public abstract class MixinChunkLightStorage implements IVanillaChunkLightProvider, IChunkLightStorageInitializer
{
	@Shadow
	@Final
	private World world;

	@Shadow
	@Final
	private ExtendedBlockStorage[] storageArrays;

	@Shadow
	public void markDirty()
	{
	}

	@Override
	public void initEmptyLightStorage(final ExtendedBlockStorage blockStorage)
	{
		((IWorldLightStorageInitializer) this.world).initEmptyLightStorage(blockStorage);
	}

	private @Nullable ExtendedBlockStorage getUpperLightStorage(int y)
	{
		for (; ++y < this.storageArrays.length; )
		{
			final ExtendedBlockStorage blockStorage = this.storageArrays[y];

			if (blockStorage != null)
				return blockStorage;
		}

		return null;
	}

	@Override
	public void initLightStorage(final ExtendedBlockStorage blockStorage)
	{
		this.initBlockStorage(blockStorage, this.getUpperLightStorage(blockStorage.getYLocation() >> 4));
	}

	private ExtendedBlockStorage initBlockStorage(final ExtendedBlockStorage blockStorage, final @Nullable ExtendedBlockStorage upperBlockStorage)
	{
		final IVanillaWorldLightProvider worldLightProvider = ((IVanillaWorldLightProvider) this.world);

		final TypedLightStorage<?, ?, ?, ?, NibbleArray> lightStorage = worldLightProvider.getLightStorageProvider().createLightStorage();

		((IVanillaLightStorageHolder) blockStorage).setLightStorage(lightStorage);

		worldLightProvider.initSectionLight(lightStorage, upperBlockStorage);

		return blockStorage;
	}

	@Override
	public void initLightStorageRead(final ExtendedBlockStorage blockStorage, final int availableSections)
	{
		this.initLightStorage(blockStorage);

		final int y = blockStorage.getYLocation();
		final int index = y >> 4;

		if (index > 0 && (availableSections & (1 << (index - 1))) == 0)
		{
			ExtendedBlockStorage lowerBlockStorage = this.storageArrays[index - 1];

			if (lowerBlockStorage == null && ((IVanillaWorldLightProvider) this.world).getEmptySectionLightProvider() != null)
			{
				lowerBlockStorage = new ExtendedBlockStorage(y - 16, this.world.provider.hasSkyLight());
				this.initLightStorage(lowerBlockStorage);
				this.storageArrays[index - 1] = lowerBlockStorage;
			}
		}
	}

	@Override
	public Object getWorldLightInterface(final BlockPos pos)
	{
		final int y = pos.getY();
		final int index = y >> 4;

		final ExtendedBlockStorage blockStorage = this.storageArrays[index];

		if (blockStorage != null)
		{
			final TypedLightStorage<?, ?, ?, ?, ?> lightStorage = ((IVanillaLightStorageHolder) blockStorage).getLightStorage();
			return lightStorage.storage.getStorageInterface(new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ()));
		}

		final IVanillaWorldLightProvider worldLightProvider = ((IVanillaWorldLightProvider) this.world);

		final @Nullable ExtendedBlockStorage upperBlockStorage = this.getUpperLightStorage(index);

		if (upperBlockStorage == null)
		{
			final @Nullable TypedCachedLightProvider<?, ?, ?, ?> skyLightProvider = worldLightProvider.getSkyLightProvider();

			if (skyLightProvider != null)
				return skyLightProvider.provider.getStorageInterface(pos);
		}
		else
		{
			final TypedEmptySectionLightPredictor<?, ?, ?, ?> emptySectionLightProvider = worldLightProvider.getEmptySectionLightProvider();

			if (emptySectionLightProvider != null)
				return this.bindEmptySectionLightProvider(emptySectionLightProvider, upperBlockStorage, pos);
		}

		return worldLightProvider.getEmptyLightProvider().provider.getStorageInterface(pos);
	}

	// Internal method to capture types
	@SuppressWarnings("unchecked")
	private <D, LI, WI> Object bindEmptySectionLightProvider(
		final TypedEmptySectionLightPredictor<D, LI, WI, ?> emptySectionLightProvider,
		final ExtendedBlockStorage upperBlockStorage,
		final BlockPos pos
	)
	{
		final TypedLightStorage<?, ?, ?, ?, ?> upperLightStorage = ((IVanillaLightStorageHolder) upperBlockStorage).getLightStorage();

		if (emptySectionLightProvider.type.lightProviderType != upperLightStorage.type.lightProviderType)
			throw new IllegalStateException("Incompatible light types");

		final BlockPos upperPos = new BlockPos(pos.getX(), upperBlockStorage.getYLocation(), pos.getZ());

		return emptySectionLightProvider.predictor.getStorageInterface(
			pos,
			upperPos,
			((TypedLightStorage<D, LI, WI, ?, ?>) upperLightStorage).storage.bind(
				new BlockPos(upperPos.getX() & 15, upperPos.getY() & 15, upperPos.getZ() & 15)
			).getInterface()
		);
	}

	/**
	 * Completely replace Vanilla logic
	 *
	 * @author PhiPro
	 */
	@Overwrite
	public int getLightFor(final EnumSkyBlock type, final BlockPos pos)
	{
		return ((IVanillaLightWorldInterface) this.getWorldLightInterface(pos)).getLight(type);
	}

	/**
	 * Completely replace Vanilla logic
	 *
	 * @author PhiPro
	 */
	@Overwrite
	public int getLightSubtracted(final BlockPos pos, final int amount)
	{
		final IVanillaLightWorldInterface lightInterface = ((IVanillaLightWorldInterface) this.getWorldLightInterface(pos));

		return Math.max(lightInterface.getLight(EnumSkyBlock.BLOCK), lightInterface.getLight(EnumSkyBlock.SKY) - amount);
	}

	/**
	 * Legacy support
	 *
	 * @author PhiPro
	 */
	@Overwrite
	@SuppressWarnings("deprecation")
	public void setLightFor(final EnumSkyBlock type, final BlockPos pos, final int value)
	{
		final int y = pos.getY();
		final int index = y >> 4;

		ExtendedBlockStorage blockStorage = this.storageArrays[index];

		if (blockStorage == null)
		{
			blockStorage = new ExtendedBlockStorage(index << 4, this.world.provider.hasSkyLight());
			this.initLightStorage(blockStorage);
			this.storageArrays[index] = blockStorage;
		}

		if (index > 0 && (y & 15) == 0)
		{
			final ExtendedBlockStorage lowerBlockStorage = this.storageArrays[index - 1];

			if (lowerBlockStorage == null && ((IVanillaWorldLightProvider) this.world).getEmptySectionLightProvider() != null)
				this.storageArrays[index - 1] = this.initBlockStorage(new ExtendedBlockStorage(y - 16, this.world.provider.hasSkyLight()), blockStorage);
		}

		final TypedLightStorage<?, ?, ?, ?, ?> lightStorage = ((IVanillaLightStorageHolder) blockStorage).getLightStorage();
		lightStorage.storage.setLight(type, new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15), value);

		this.markDirty();
	}
}
