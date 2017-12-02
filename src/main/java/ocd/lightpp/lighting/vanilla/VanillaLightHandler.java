/*
 * MIT License
 *
 * Copyright (c) 2017-2017 OverengineeredCodingDuo
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

package ocd.lightpp.lighting.vanilla;

import javax.annotation.Nullable;

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
import ocd.lightpp.api.lighting.ILightAccess;
import ocd.lightpp.api.lighting.ILightHandler;
import ocd.lightpp.lighting.LightingEngine;
import ocd.lightpp.util.PooledLongQueue;
import ocd.lightpp.util.PooledLongQueue.SegmentPool;

public class VanillaLightHandler extends VanillaLightAccess implements ILightHandler
{
	static final long[] neighborShifts = new long[EnumFacing.VALUES.length];

	static
	{
		for (final EnumFacing dir : EnumFacing.VALUES)
		{
			final Vec3i offset = dir.getDirectionVec();
			neighborShifts[dir.ordinal()] = ((long) offset.getY() << sY) | ((long) offset.getX() << sX) | ((long) offset.getZ() << sZ);
		}
	}

	private final SegmentPool segmentPool = new SegmentPool();
	private PooledLongQueue activeQueue;

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

	//Iteration state data
	private EnumSkyBlock curLightType;

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

	@Override
	public LightUpdateQueue createQueue()
	{
		return new VanillaLUQ();
	}

	@Override
	public boolean next()
	{
		if (this.activeQueue.isEmpty())
			return false;

		this.setData(this.activeQueue.poll());
		getChunk();

		for (int i = 0; i < 6; ++i)
			this.neighbors[i].setData(this.curData + neighborShifts[i]);

		this.curLightType = LightingEngine.LIGHT_TYPE_VALUES[(int) (this.curData >> sL & mL)];

		return true;
	}

	@Override
	public void cleanup()
	{
		super.cleanup();

		for (int i = 0; i < 6; ++i)
			this.neighbors[i].cleanup();
	}

	@Override
	public EnumSkyBlock getLightType()
	{
		return this.curLightType;
	}

	@Override
	public ILightAccess getNeighborLightAccess(final EnumFacing dir)
	{
		return this.neighbors[dir.ordinal()];
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
	public void setLight(final int val)
	{
		setLight(this.curLightType, val);
	}

	@Override
	public void setLight(final EnumFacing dir, final int val)
	{
		this.neighbors[dir.ordinal()].setLight(this.curLightType, val);
	}

	@Override
	public void notifyLightSet()
	{
		this.world.notifyLightSet(this.curPos);
	}

	@Override
	public void trackDarkening(final EnumFacing dir)
	{

	}

	@Override
	public void trackBrightening(final EnumFacing dir)
	{

	}

	@Override
	public void markForRecheck(final EnumFacing dir)
	{

	}

	@Override
	public void markForSpread(final EnumFacing dir)
	{

	}

	@Override
	VanillaLightHandler getLightHandler()
	{
		return this;
	}

	private class VanillaLUQ extends PooledLongQueue implements LightUpdateQueue
	{
		private VanillaLUQ()
		{
			super(VanillaLightHandler.this.segmentPool);
		}

		@Override
		public void activate()
		{
			VanillaLightHandler.this.activeQueue = this;
		}

		@Override
		public void accept()
		{
			this.add(VanillaLightHandler.this.curData);
		}

		@Override
		public void accept(final EnumFacing dir)
		{
			this.add(VanillaLightHandler.this.curData + neighborShifts[dir.ordinal()]);
		}

		@Override
		public void accept(final BlockPos pos, final EnumSkyBlock lightType)
		{
			this.add(posToLong(pos) | ((long) lightType.ordinal() << sL));
		}
	}
}

abstract class VanillaLightAccess implements ILightAccess
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

		return this.curChunk = this.getLightHandler().world.getChunkProvider().getLoadedChunk(this.curPos.getX() >> 4, this.curPos.getZ() >> 4);
	}

	@Override
	public IBlockAccess getBlockAccess()
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
