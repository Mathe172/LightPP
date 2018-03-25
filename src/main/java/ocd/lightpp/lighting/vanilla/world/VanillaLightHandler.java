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
 */

package ocd.lightpp.lighting.vanilla.world;

import java.util.ArrayList;
import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import ocd.lightpp.api.lighting.ILightAccess;
import ocd.lightpp.api.lighting.ILightHandler;
import ocd.lightpp.api.lighting.ILightMap.ILightIterator;
import ocd.lightpp.api.vanilla.world.ILightProvider.Positioned.Writeable;
import ocd.lightpp.api.vanilla.world.ILightStorage;
import ocd.lightpp.api.vanilla.world.IVanillaLightQueueDataset;
import ocd.lightpp.api.vanilla.world.IVanillaWorldInterface;
import ocd.lightpp.util.PooledIntQueue;
import ocd.lightpp.util.PooledShortQueue;

public class VanillaLightHandler<D, LI, V, C> implements ILightHandler<D, LI, IVanillaWorldInterface, V>
{
	static final long[] neighborShifts = new long[EnumFacing.VALUES.length];
	public static final int EXPECTED_SECTION_COUNT = 1 << 11;

	static
	{
		for (final EnumFacing dir : EnumFacing.VALUES)
		{
			final Vec3i offset = dir.getDirectionVec();
			neighborShifts[dir.ordinal()] = ((long) offset.getY() << sY) | ((long) offset.getX() << sX) | ((long) offset.getZ() << sZ);
		}
	}

	private static class SectionContainer<D, LI, C>
	{
		private Chunk chunk;
		private final int[] neighbors = new int[6];

		private final MutableBlockPos pos = new MutableBlockPos();

		private @Nullable ExtendedBlockStorage blockStorage;
		private @Nullable ILightStorage<D, LI, ?, C, ?> lightStorage;
	}

	private final World world;

	private final Long2IntMap sectionCache = new Long2IntOpenHashMap(EXPECTED_SECTION_COUNT);

	private class VanillaLightUpdateQueue
		extends VanillaLightAccess
		implements ILightUpdateQueue<D, LI, IVanillaWorldInterface, V>,
		ILightUpdateQueueIterator<D, LI, IVanillaWorldInterface, V>
	{
		//TODO: Reduce capacity 
		private final ArrayList<IVanillaLightQueueDataset<D, PooledShortQueue>> updates = new ArrayList<>(EXPECTED_SECTION_COUNT);
		private final PooledIntQueue updateIndices;

		// Iterator state
		private IVanillaLightQueueDataset<D, PooledShortQueue> curDataset;
		private PooledShortQueue curQueue;

		@Override
		public boolean next()
		{
			if (this.curQueue.isEmpty())
			{
				if (!this.curDataset.next())
				{
					if (this.updateIndices.isEmpty())
						return false;

					this.curDataset = this.updates.get(this.updateIndices.poll()); // Not empty
				}

				this.curQueue = this.curDataset.getQueue(); // Not empty
			}
		}

		@Override
		public Extended<D, LI, IVanillaWorldInterface> getNeighbor(final EnumFacing dir)
		{
			return null;
		}

		@Override
		public D getDescriptor()
		{
			return this.curDataset.getDesc();
		}

		@Override
		public void markForRecheck()
		{
		}

		@Override
		public void markForSpread(final EnumFacing dir)
		{
		}

		@Override
		public boolean isValid()
		{
			return false;
		}

		@Override
		public boolean isLoaded()
		{
			return false;
		}

		@Override
		public LI getLightData()
		{
			return null;
		}

		@Override
		public IVanillaWorldInterface getWorldInterface()
		{
			return null;
		}

		@Override
		public ILightIterator<D> getLightIterator()
		{
			return null;
		}

		@Override
		public int getLight(final D desc)
		{
			return 0;
		}

		@Override
		public void setLight(final D desc, final int val)
		{

		}

		@Override
		public void notifyLightSet(final D desc)
		{

		}

		@Nullable
		@Override
		public V getVirtualSources()
		{
			return null;
		}

		@Override
		public ILightUpdateQueueIterator<D, LI, IVanillaWorldInterface, V> activate()
		{
			return null;
		}

		@Override
		public void enqueueDarkening(final D desc, final int oldLight)
		{

		}

		@Override
		public void enqueueDarkening(final D desc, final EnumFacing dir, final int oldLight)
		{

		}

		@Override
		public void enqueueBrightening(final D desc, final int newLight)
		{

		}

		@Override
		public void enqueueBrightening(final D desc, final EnumFacing dir, final int newLight)
		{

		}
	}

	private final PooledShortQueue.SegmentPool segmentPool = new PooledShortQueue.SegmentPool();
	private PooledIntQueue activeQueue;

	final World world;

	public VanillaLightHandler(final World world)
	{
		this.world = world;

		for (final EnumFacing dir : EnumFacing.VALUES)
			this.neighbors[dir.ordinal()] = new VanillaLightAccess()
			{
				@Override
				VanillaLightHandler getLightHandler()
				{
					return VanillaLightHandler.this;
				}
			};
	}

	//Cached data about neighboring blocks (of tempPos)
	private final VanillaLightAccess[] neighbors = new VanillaLightAccess[EnumFacing.VALUES.length];

	private static long posToLong(final BlockPos pos)
	{
		return posToLong(pos.getX(), pos.getY(), pos.getZ());
	}

	private static long posToLong(final long x, final long y, final long z)
	{
		return (y << sY) | (x + (1 << lX - 1) << sX) | (z + (1 << lZ - 1) << sZ);
	}

	@Nullable
	@Override
	Chunk getChunk()
	{
		if (this.chunkValid)
			return this.curChunk;

		this.chunkValid = true;

		return this.curChunk = this.getLightHandler().world.getChunkProvider().getLoadedChunk(this.curPos.getX() >> 4, this.curPos.getZ() >> 4);
	}

	@Override
	VanillaLightHandler getLightHandler()
	{
		return this;
	}

	@Override
	public ILightUpdateQueue<D, LI, IVanillaWorldInterface, V> createUpdateQueue()
	{
		return null;
	}

	@Override
	public ILightCheckQueue<D, LI, IVanillaWorldInterface, V> createCheckQueue()
	{
		return null;
	}

	@Override
	public ILightSpreadQueue<D, LI, IVanillaWorldInterface, V> createSpreadQueue()
	{
		return null;
	}

	@Override
	public ILightInitQueue<D, LI, IVanillaWorldInterface, V> createInitQueue()
	{
		return null;
	}

	@Override
	public void cleanup()
	{

	}

	abstract class VanillaLightAccess implements ILightAccess.Extended<D, LI, IVanillaWorldInterface>, IVanillaWorldInterface
	{
		@Nullable SectionContainer<D, LI, C> section;

		@Nullable Writeable<D, LI> lightInterfaceWriteable;
		ILightStorage.Positioned<D, LI> lightInterface;

		private final C liContainer;
		private final MutableBlockPos pos = new MutableBlockPos();

		@Override
		public boolean isValid()
		{
			return this.section != null;
		}

		@Override
		public boolean isLoaded()
		{
			return this.section == null || this.section.chunk != null;
		}

		@Override
		public LI getLightData()
		{
			return this.lightInterface.getInterface();
		}

		@Override
		public IVanillaWorldInterface getWorldInterface()
		{
			return this;
		}

		@Override
		public ILightIterator<D> getLightIterator()
		{
			return this.lightInterface.getLightIterator();
		}

		@Override
		public int getLight(final D desc)
		{
			return this.lightInterface.getLight(desc);
		}

		@Override
		public void setLight(final D desc, final int val)
		{
			if (this.section == null || this.section.chunk == null)
				return;

			if (this.lightInterfaceWriteable == null)
			{
			}

			this.lightInterfaceWriteable.set(desc, val);
		}

		@Override
		public void notifyLightSet(final D desc)
		{
			if (this.lightInterfaceWriteable != null)
			{
				this.lightInterfaceWriteable.notifyLightSet(desc);
				this.lightInterfaceWriteable.notifyLightSet();
			}

			VanillaLightHandler.this.world.notifyLightSet(this.pos);
		}

		@Override
		public IBlockAccess getWorld()
		{
			return VanillaLightHandler.this.world;
		}

		@Override
		public IBlockState getBlockState()
		{
			if (this.section == null || this.section.blockStorage == Chunk.NULL_BLOCK_STORAGE)
				return Blocks.AIR.getDefaultState();

			return this.section.blockStorage.get(this.pos.getX(), this.pos.getY(), this.pos.getZ());
		}

		@Override
		public BlockPos getPos()
		{
			return this.pos;
		}
	}
}

abstract class VanillaLightAccess2 implements ILightAccess
{
	//Layout parameters
	//Length of bit segments
	static final int
		lX = 26,
		lY = 8,
		lZ = 26,
		lL = 1;

	//Bit segment shifts/positions
	static final int
		sZ = 0,
		sX = sZ + lZ,
		sY = sX + lX,
		sL = sY + lY + 1;

	//Bit segment masks
	static final long
		mX = (1L << lX) - 1,
		mY = (1L << lY) - 1,
		mZ = (1L << lZ) - 1,
		mL = (1L << lL) - 1,
		mPos = (mY << sY) | (mX << sX) | (mZ << sZ);

	//Bit to check whether y had overflow
	protected static final long yCheck = 1L << (sY + lY);

	//Mask to extract chunk idenitfier
	static final long mChunk = ((mX >> 4) << (4 + sX)) | ((mZ >> 4) << (4 + sZ));

	long curData;
	final MutableBlockPos curPos = new MutableBlockPos();
	protected boolean chunkValid;
	long curChunkID = -1;
	@Nullable
	Chunk curChunk;

	static MutableBlockPos longToPos(final MutableBlockPos pos, final long longPos)
	{
		final int posX = (int) (longPos >> sX & mX) - (1 << lX - 1);
		final int posY = (int) (longPos >> sY & mY);
		final int posZ = (int) (longPos >> sZ & mZ) - (1 << lZ - 1);
		return pos.setPos(posX, posY, posZ);
	}

	void setData(final long data)
	{
		this.curData = data;
		longToPos(this.curPos, data);
		final long newChunkID = data & VanillaLightHandler.mChunk;

		if (newChunkID != this.curChunkID)
		{
			this.curChunkID = newChunkID;
			this.chunkValid = false;
		}
	}

	void cleanup()
	{
		this.chunkValid = false;
		this.curChunkID = -1;
		this.curChunk = null;
	}

	abstract VanillaLightHandler getLightHandler();

	@Nullable
	Chunk getChunk()
	{
		if (this.chunkValid)
			return this.curChunk;

		this.chunkValid = true;

		if (this.getLightHandler().curChunkID == this.curChunkID)
			return this.curChunk = this.getLightHandler().curChunk;

		return this.curChunk = this.getWorldInterface().getChunkProvider().getLoadedChunk(this.curPos.getX() >> 4, this.curPos.getZ() >> 4);
	}

	@Override
	public World getWorldInterface()
	{
		return this.getLightHandler().world;
	}

	@Override
	public boolean isValid()
	{
		return (this.curData & yCheck) == 0;
	}

	@Override
	public boolean isLoaded()
	{
		return this.getChunk() != null;
	}

	@Override
	public BlockPos getPos()
	{
		return this.curPos;
	}

	@Override
	public int getLight(final EnumSkyBlock lightType)
	{
		return this.isValid() ? this.getChunk().getLightFor(lightType, this.curPos) : lightType.defaultLightValue;
	}

	public void setLight(final EnumSkyBlock lightType, final int val)
	{
		this.getChunk().setLightFor(lightType, this.curPos, val);
	}

	@Override
	public IBlockState getBlockState()
	{
		return this.isValid() ? this.getChunk().getBlockState(this.curPos) : Blocks.AIR.getDefaultState();
	}
}
