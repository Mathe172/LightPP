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

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import ocd.lightpp.api.lighting.ILightAccess;
import ocd.lightpp.api.lighting.ILightCollectionDescriptor;
import ocd.lightpp.api.lighting.ILightMap.ILightIterator;
import ocd.lightpp.api.vanilla.type.CachedLightProviderType.TypedCachedLightProvider;
import ocd.lightpp.api.vanilla.type.LightProviderType.TypedLightProvider;
import ocd.lightpp.api.vanilla.type.TypedEmptySectionLightPredictor;
import ocd.lightpp.api.vanilla.type.TypedLightStorage;
import ocd.lightpp.api.vanilla.world.ILightProvider.Positioned.Writeable;
import ocd.lightpp.api.vanilla.world.ILightStorage;
import ocd.lightpp.api.vanilla.world.ILightStorageProvider;
import ocd.lightpp.api.vanilla.world.IVanillaLightStorageHolder;
import ocd.lightpp.api.vanilla.world.IVanillaWorldInterface;

abstract class VanillaLightAccess<LD, LCD extends ILightCollectionDescriptor<LD>, LI, C>
	implements ILightAccess.Extended<LD, LI, IVanillaWorldInterface.Extended>, IVanillaWorldInterface.Extended
{
	private final VanillaLightHandler<LD, LCD, LI, ?, C> lightHandler;

	SectionContainer<LD, LI, C> section;

	private @Nullable Writeable<LD, LI> lightInterfaceWriteable;
	private ILightStorage.Positioned<LD, LI> lightInterface;

	private final MutableBlockPos pos = new MutableBlockPos();
	private final MutableBlockPos upperPos = new MutableBlockPos();

	short data;

	final VanillaWorldLightHelper<LD, LI, ?, C, ?, ?> lightManager;

	private final MutableBlockPos cachedPos = new MutableBlockPos();

	<WI> VanillaLightAccess(
		final ILightStorageProvider<LD, LI, WI, C, NibbleArray> lightStorageProvider,
		@Nullable final TypedCachedLightProvider<LD, LI, WI, ?> skyLightProvider,
		@Nullable final TypedEmptySectionLightPredictor<LD, LI, WI, ?> emptySectionLightPredictor,
		final TypedLightProvider<LD, LI, WI> emptyLightProvider
	)
	{
		this.lightHandler = this.getLightHandler();
		this.lightManager = new VanillaWorldLightHelper<>(lightStorageProvider, skyLightProvider, emptySectionLightPredictor, emptyLightProvider);
	}

	protected abstract VanillaLightHandler<LD, LCD, LI, ?, C> getLightHandler();

	void update()
	{
		if (this.section.lightStorage == null)
		{
			this.upperPos.setPos(this.pos.getX(), this.section.upperPos.getY(), this.pos.getZ());
			this.lightInterfaceWriteable = null;
			this.lightInterface = this.lightManager.getLightInterface(this.pos, this.section.upperLightStorage, this.upperPos);
		}
		else
			this.lightInterface = this.lightInterfaceWriteable = this.lightManager.getCachedPositioned(this.pos, this.section.lightStorage);
	}

	void update(final SectionContainer<LD, LI, C> section, final BlockPos pos, final short data)
	{
		this.section = section;
		this.data = data;

		this.pos.setPos(pos);

		this.update();
	}

	@Override
	public boolean isValid()
	{
		return this.section.isValid;
	}

	@Override
	public boolean isLoaded()
	{
		return !this.isValid() || this.section.chunk != null;
	}

	@Override
	public LI getLightData()
	{
		return this.lightInterface.getInterface();
	}

	@Override
	public IVanillaWorldInterface.Extended getWorldInterface()
	{
		return this;
	}

	@Override
	public ILightIterator<LD> getLightIterator()
	{
		return this.lightInterface.getLightIterator();
	}

	@Override
	public int getLight(final LD desc)
	{
		return this.lightInterface.getLight(desc);
	}

	private @Nullable SectionContainer<LD, LI, C> getLowerSection(@Nullable final SectionContainer<LD, LI, C> section, final long lowerSectionCoords)
	{
		return section == null ? this.lightHandler.getExistingSection(lowerSectionCoords) : this.lightHandler.getExistingSection(section, EnumFacing.DOWN);
	}

	private void initStorage(
		final ExtendedBlockStorage[] storageArrays,
		final int yIndex,
		@Nullable SectionContainer<LD, LI, C> section,
		long sectionCoords,
		final BlockPos pos,
		final @Nullable ILightStorage<LD, LI, ?, C, ?> upperLightStorage,
		final BlockPos upperPos
	)
	{
		ExtendedBlockStorage blockStorage = new ExtendedBlockStorage(yIndex << 4, this.lightHandler.world.provider.hasSkyLight());
		final TypedLightStorage<LD, LI, ?, C, NibbleArray> lightStorage = this.lightManager.createInitLightStorage(
			pos,
			upperLightStorage,
			upperPos
		);

		((IVanillaLightStorageHolder) blockStorage).setLightStorage(lightStorage);

		storageArrays[yIndex] = blockStorage;

		if (section != null)
		{
			section.blockStorage = blockStorage;
			section.lightStorage = lightStorage.storage;
		}

		if (!this.lightHandler.needsUpperStorage)
			return;

		if (section != null)
		{
			section.upperLightStorage = null;
			section.upperPos.setPos(pos);
		}

		for (int y = yIndex - 1; y >= 0; --y)
		{
			blockStorage = storageArrays[y];

			if (blockStorage != Chunk.NULL_BLOCK_STORAGE)
				break;

			sectionCoords = VanillaLightHandler.moveSectionCoords(sectionCoords, EnumFacing.DOWN);
			section = this.getLowerSection(section, sectionCoords);

			if (section != null)
			{
				section.upperLightStorage = lightStorage.storage;
				section.upperPos.setPos(pos);
			}
		}

		this.lightHandler.updateAll();
	}

	@Override
	public void setLight(final LD desc, final int val)
	{
		if (this.section.chunk == null)
			return;

		if ((this.pos.getY() & 15) == 0 && this.section.initLowerStorage)
		{
			this.section.initLowerStorage = false;

			final ExtendedBlockStorage[] storageArrays = this.section.chunk.getBlockStorageArray();

			if (storageArrays[this.section.yIndex - 1] == Chunk.NULL_BLOCK_STORAGE)
			{
				final long lowerSectionCoords = VanillaLightHandler.moveSectionCoords(this.section.sectionCoords, EnumFacing.DOWN);

				this.initStorage(
					storageArrays,
					this.section.yIndex - 1,
					this.getLowerSection(this.section, lowerSectionCoords),
					lowerSectionCoords,
					this.cachedPos.setPos(this.section.pos).move(EnumFacing.DOWN, 16),
					this.section.lightStorage == null ? this.section.upperLightStorage : this.section.lightStorage,
					this.section.lightStorage == null ? this.section.upperPos : this.section.pos
				);
			}
		}

		if (this.lightInterfaceWriteable == null)
		{
			final ExtendedBlockStorage[] storageArrays = this.section.chunk.getBlockStorageArray();

			this.initStorage(
				storageArrays,
				this.section.yIndex,
				this.section,
				this.section.sectionCoords,
				this.section.pos,
				this.section.upperLightStorage,
				this.section.upperPos
			);
		}

		this.lightInterfaceWriteable.set(desc, val);
	}

	@Override
	public void notifyLightSet(final LD desc)
	{
		if (this.lightInterfaceWriteable != null)
		{
			this.lightInterfaceWriteable.notifyLightSet(desc);
			this.lightInterfaceWriteable.notifyLightSet();
		}

		this.lightHandler.world.notifyLightSet(this.pos);
	}

	@Override
	public IBlockAccess getWorld()
	{
		return this.lightHandler.world;
	}

	@Override
	public Chunk getChunk()
	{
		if (this.section.chunk == null)
			throw new IllegalStateException("This position does not belong to a currently loaded chunk");

		return this.section.chunk;
	}

	@Override
	public IBlockState getBlockState()
	{
		if (this.section.blockStorage == Chunk.NULL_BLOCK_STORAGE)
			return Blocks.AIR.getDefaultState();

		return this.section.blockStorage.get(this.pos.getX(), this.pos.getY(), this.pos.getZ());
	}

	@Override
	public BlockPos getPos()
	{
		return this.pos;
	}

	static class SectionContainer<LD, LI, C>
	{
		final int index;

		@Nullable Chunk chunk;

		final MutableBlockPos pos = new MutableBlockPos();
		long sectionCoords;
		boolean isValid;
		@SuppressWarnings("unchecked") // Fuck you Java
		final SectionContainer<LD, LI, C>[] neighbors = new SectionContainer[6];
		int yIndex;

		ExtendedBlockStorage blockStorage;
		@Nullable ILightStorage<LD, LI, ?, C, ?> lightStorage;

		final MutableBlockPos upperPos = new MutableBlockPos();
		@Nullable ILightStorage<LD, LI, ?, C, ?> upperLightStorage;

		boolean initLowerStorage;

		public SectionContainer(final int index)
		{
			this.index = index;
		}

		void init(final boolean isValid, final BlockPos pos, final long sectionCoords)
		{
			this.isValid = isValid;
			this.yIndex = pos.getY() >> 4;

			this.pos.setPos(pos);
			this.upperPos.setPos(pos);

			this.sectionCoords = sectionCoords;
		}

		void cleanup()
		{
			this.chunk = null;

			for (int i = 0; i < 6; ++i)
				this.neighbors[i] = null;

			this.blockStorage = null;
			this.lightStorage = null;
			this.upperLightStorage = null;

			this.initLowerStorage = false;
		}

		@Nullable SectionContainer<LD, LI, C> getNeighbor(final EnumFacing dir)
		{
			return this.neighbors[dir.ordinal()];
		}

		void setNeighbor(final SectionContainer<LD, LI, C> neighbor, final EnumFacing dir)
		{
			this.neighbors[dir.ordinal()] = neighbor;
			neighbor.neighbors[dir.getOpposite().ordinal()] = this;
		}
	}
}
