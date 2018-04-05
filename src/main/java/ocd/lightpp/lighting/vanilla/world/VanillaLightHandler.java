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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import ocd.lightpp.api.lighting.ILightAccess;
import ocd.lightpp.api.lighting.ILightCollectionDescriptor;
import ocd.lightpp.api.lighting.ILightHandler;
import ocd.lightpp.api.vanilla.type.TypedLightStorage;
import ocd.lightpp.api.vanilla.world.ILightQueueDataset;
import ocd.lightpp.api.vanilla.world.ILightQueueDataset.ILightCollectionQueueDataset;
import ocd.lightpp.api.vanilla.world.IVanillaLightStorageHolder;
import ocd.lightpp.api.vanilla.world.IVanillaWorldInterface;
import ocd.lightpp.util.PooledIntQueue;
import ocd.lightpp.util.PooledShortQueue;

public class VanillaLightHandler<LD, LCD extends ILightCollectionDescriptor<LD>, LI, V, LC, SC, EC>
	extends VanillaLightAccess<LD, LCD, LI, LC, SC, EC>
	implements ILightHandler<LD, LCD, LI, IVanillaWorldInterface.Extended, V>,
	ILightAccess.VirtuallySourced.NeighborAware.Extended<LD, LI, IVanillaWorldInterface.Extended, V>
{
	// Global section coordinates

	// Layout parameters
	// Length of bit segments
	private static final int
		lgX = 22,
		lgY = 5,
		lgZ = 22;

	// Bit segment shifts/positions
	private static final int
		sgZ = 0,
		sgY = sgZ + lgZ,
		sgX = sgY + lgY;

	// Bit segment masks
	private static final long
		mgX = (1L << lgX) - 1,
		mgY = (1L << lgY) - 1,
		mgZ = (1L << lgZ) - 1;

	// Value offsets
	private static final int
		ogX = 1 << (lgX - 1),
		ogY = 1 << (lgY - 1),
		ogZ = 1 << (lgZ - 1);

	// Chunk mask
	private static final long mChunk = (mgX << sgX) | (mgZ << sgZ);

	private static final long[] gNeighborShifts = new long[6];

	static
	{
		for (int i = 0; i < 6; ++i)
		{
			final Vec3i offset = EnumFacing.VALUES[i].getDirectionVec();
			gNeighborShifts[i] = ((long) offset.getX() << sgX) | ((long) offset.getY() << sgY) | ((long) offset.getZ() << sgZ);
		}
	}

	private static void sectionCoordsToPos(final MutableBlockPos pos, final long sectionCoords)
	{
		final int posX = (((int) (sectionCoords >> sgX & mgX)) - ogX) << 4;
		final int posY = (((int) (sectionCoords >> sgY & mgY)) - ogY) << 4;
		final int posZ = (((int) (sectionCoords >> sgZ & mgZ)) - ogZ) << 4;

		pos.setPos(posX, posY, posZ);
	}

	static long posToSectionCoords(final BlockPos pos)
	{
		return (((long) ((pos.getX() >> 4) + ogX) << sgX) | ((long) ((pos.getY() >> 4) + ogY) << sgY) | ((long) ((pos.getZ() >> 4) + ogZ) << sgZ));
	}

	static long moveSectionCoords(final long sectionCoords, final EnumFacing dir)
	{
		return sectionCoords + gNeighborShifts[dir.ordinal()];
	}

	// Local section coordinates

	// Layout parameters
	// Length of bit segments
	private static final int
		llX = 4,
		llY = 4,
		llZ = 4,
		lDir = 3;

	// Bit segment shifts/positions
	private static final int
		slZ = 0,
		slY = slZ + llZ,
		slX = slY + llY,
		sDir = slX + llX;

	// Bit segment masks
	private static final short
		mlX = (1 << llX) - 1,
		mlY = (1 << llY) - 1,
		mlZ = (1 << llZ) - 1,
		mDir = (1 << lDir) - 1;

	private static final short clearDir = ~(mDir << sDir);

	private static final short[] lNeighborShifts = new short[6];
	private static final short[] lCheckMasks = new short[6];
	private static final short[] lCheckRefs = new short[6];
	private static final short[] lOverflowCorrections = new short[6];

	static
	{
		for (int i = 0; i < 6; ++i)
		{
			final Vec3i offset = EnumFacing.VALUES[i].getDirectionVec();

			lNeighborShifts[i] = (short) ((offset.getX() << slX) | (offset.getY() << slY) | (offset.getZ() << slZ));
			lOverflowCorrections[i] = (short) ((-offset.getX() << (slX + llX)) | (-offset.getY() << (slY + llY)) | (-offset.getZ() << (slZ + llZ)));

			final short checkMask = (short) (((-Math.abs(offset.getX()) & mlX) << slX) | ((-Math.abs(offset.getY()) & mlY) << slY) | ((-Math.abs(offset.getZ()) & mlZ) << slZ));
			lCheckMasks[i] = checkMask;

			final boolean isPositive = offset.getX() + offset.getY() + offset.getZ() > 0;

			lCheckRefs[i] = (short) (isPositive ? 0 : -1 & checkMask);
		}
	}

	private static final EnumFacing[] DIRECTIONS_NULL = ArrayUtils.add(EnumFacing.VALUES, null);

	private static void addLocalCoords(final MutableBlockPos pos, final short localCoords)
	{
		pos.setPos(
			pos.getX() + (localCoords >> slX & mlX),
			pos.getY() + (localCoords >> slY & mlY),
			pos.getZ() + (localCoords >> slZ & mlZ)
		);
	}

	static short posToLocalCoords(final BlockPos pos)
	{
		return (short) (((pos.getX() & 15) << slX) | ((pos.getY() & 15) << slY) | ((pos.getZ() & 15) << slZ));
	}

	static @Nullable EnumFacing getDirection(final short data)
	{
		final int index = data >> sDir & mDir;
		return DIRECTIONS_NULL[index];
	}

	static short dirToData(@Nullable final EnumFacing dir)
	{
		final int index = dir == null ? 6 : dir.ordinal();
		return (short) (index << sDir);
	}

	static short clearDir(final short data)
	{
		return (short) (data & clearDir);
	}

	private static final int EXPECTED_SECTION_COUNT = 1 << 11;
	private static final int EXPECTED_LIGHT_DATASET_COUNT = 1 << 11;
	private static final int EXPECTED_LIGHT_COLLECTION_DATASET_COUNT = 1 << 10;
	private static final int EXPECTED_DATA_QUEUE_COUNT = 1 << 11;

	final VanillaWorldLightManager<LD, LI, ?, LC, ?, SC, ?, EC, ?> lightManager;
	final VanillaWorldLightHelper<LD, LI, ?, LC, SC, EC> lightHelper;

	final boolean needsUpperStorage;
	private final boolean initLowerStorage;

	final World world;

	private final Supplier<PooledShortQueue> dataQueueProvider = this::getDataQueue;

	private final ILightQueueDataset.Provider<LD> lightDataSetProvider;
	private final ILightCollectionQueueDataset.Provider<LD, LCD> lightCollectionDataSetProvider;

	private final Long2IntOpenHashMap sectionCache;
	private final ArrayList<SectionContainer<LD, LI, LC>> sectionList = new ArrayList<>(EXPECTED_SECTION_COUNT);
	private int sectionCount;

	@SuppressWarnings("unchecked") // Fuck you Java
	private final VanillaLightAccess<LD, LCD, LI, LC, SC, EC> lightAccesses[] = new VanillaLightAccess[7];

	private final MutableBlockPos cachedPos = new MutableBlockPos();
	private final MutableBlockPos cachedNeighborPos = new MutableBlockPos();

	private long lastChunkCoords = -1;
	private @Nullable Chunk lastChunk;

	private long lastSectionCoords = -1;
	private @Nullable SectionContainer<LD, LI, LC> lastSection;

	private final PooledIntQueue.SegmentPool indexQueueSegmentPool = new PooledIntQueue.SegmentPool(1 << 7, 1 << 7);
	private final PooledShortQueue.SegmentPool dataQueueSegmentPool = new PooledShortQueue.SegmentPool(1 << 12, 1 << 9);

	private final Deque<ILightQueueDataset<LD, PooledShortQueue>> lightDataSetCache = new ArrayDeque<>();
	private final Deque<ILightCollectionQueueDataset<LD, LCD, PooledShortQueue>> lightCollectionDataSetCache = new ArrayDeque<>();
	private final Deque<PooledShortQueue> dataQueueCache = new ArrayDeque<>();

	public <WI> VanillaLightHandler(
		final World world,
		final VanillaWorldLightHelper<LD, LI, ?, LC, SC, EC> lightHelper,
		final ILightQueueDataset.Provider<LD> lightDataSetProvider,
		final ILightCollectionQueueDataset.Provider<LD, LCD> lightCollectionDataSetProvider
	)
	{
		super(lightHelper.lightManager);

		this.lightManager = lightHelper.lightManager;
		this.lightHelper = lightHelper;

		this.needsUpperStorage = this.lightManager.needsUpperStorage();
		this.initLowerStorage = this.lightManager.emptySectionLightPredictor != null;

		this.world = world;

		this.sectionCache = new Long2IntOpenHashMap(EXPECTED_SECTION_COUNT);
		this.sectionCache.defaultReturnValue(-1);

		this.lightAccesses[6] = this;

		for (int i = 0; i < 6; ++i)
			this.lightAccesses[i] = new VanillaLightAccess<LD, LCD, LI, LC, SC, EC>(lightHelper.lightManager)
			{
				@Override
				protected VanillaLightHandler<LD, LCD, LI, ?, LC, SC, EC> getLightHandler()
				{
					return VanillaLightHandler.this;
				}
			};

		this.lightDataSetProvider = lightDataSetProvider;
		this.lightCollectionDataSetProvider = lightCollectionDataSetProvider;
	}

	@Override
	protected VanillaLightHandler<LD, LCD, LI, V, LC, SC, EC> getLightHandler()
	{
		return this;
	}

	@Override
	public VanillaLightAccess<LD, LCD, LI, LC, SC, EC> getNeighbor(final EnumFacing dir)
	{
		return this.lightAccesses[dir.ordinal()];
	}

	@Override
	public @Nullable V getVirtualSources()
	{
		return null;
	}

	private void updateAll(final SectionContainer<LD, LI, LC> section, final short data)
	{
		this.cachedPos.setPos(section.pos);
		addLocalCoords(this.cachedPos, data);

		this.update(section, this.cachedPos, data);

		for (int i = 0; i < 6; ++i)
		{
			final EnumFacing dir = EnumFacing.VALUES[i];

			short neighborData = (short) (data + lNeighborShifts[i]);
			final SectionContainer<LD, LI, LC> neighborSection;

			if ((neighborData & lCheckMasks[i]) == lCheckRefs[i])
			{
				neighborSection = dir.getAxis() == Axis.Y ? this.getSection(section, dir, section.chunk) : this.getSection(section, dir, true);
				neighborData += lOverflowCorrections[i];
			}
			else
				neighborSection = section;

			this.cachedNeighborPos.setPos(this.cachedPos);
			this.cachedNeighborPos.move(dir);

			this.lightAccesses[i].update(neighborSection, this.cachedNeighborPos, neighborData);
		}
	}

	void updateAll()
	{
		for (final VanillaLightAccess<LD, LCD, LI, LC, SC, EC> lightAccess : this.lightAccesses)
			lightAccess.update();
	}

	private @Nullable Chunk getChunk(final BlockPos pos, final long sectionCoords)
	{
		final long chunkCoords = sectionCoords & mChunk;

		if (chunkCoords == this.lastChunkCoords)
			return this.lastChunk;

		this.lastChunkCoords = chunkCoords;
		this.lastChunk = this.world.getChunkProvider().getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4);

		return this.lastChunk;
	}

	private SectionContainer<LD, LI, LC> createSection(final BlockPos pos, final long sectionCoords, @Nullable final Chunk chunk)
	{
		final SectionContainer<LD, LI, LC> section;

		if (this.sectionCount == this.sectionList.size())
		{
			section = new SectionContainer<>(this.sectionCount);
			this.sectionList.add(section);
		}
		else
			section = this.getSection(this.sectionCount);

		this.sectionCache.put(sectionCoords, this.sectionCount);
		++this.sectionCount;

		this.initSectionContainer(section, pos, sectionCoords, chunk);

		this.lastSectionCoords = sectionCoords;
		this.lastSection = section;

		return section;
	}

	private SectionContainer<LD, LI, LC> getSection(final int index)
	{
		return this.sectionList.get(index);
	}

	SectionContainer<LD, LI, LC> getSection(
		final SectionContainer<LD, LI, LC> section,
		final EnumFacing dir,
		@Nullable final Chunk chunk
	)
	{
		return this.getSection(section, dir, chunk, false);
	}

	SectionContainer<LD, LI, LC> getSection(
		final SectionContainer<LD, LI, LC> section,
		final EnumFacing dir,
		final boolean fetchChunk
	)
	{
		return this.getSection(section, dir, null, fetchChunk);
	}

	private SectionContainer<LD, LI, LC> getSection(
		final SectionContainer<LD, LI, LC> section,
		final EnumFacing dir,
		@Nullable final Chunk chunk,
		final boolean fetchChunk
	)
	{
		SectionContainer<LD, LI, LC> ret = this.getExistingSection(section, dir);

		if (ret != null)
			return ret;

		final long sectionCoords = moveSectionCoords(section.sectionCoords, dir);
		sectionCoordsToPos(this.cachedPos, sectionCoords);

		ret = this.createSection(this.cachedPos, sectionCoords, fetchChunk ? this.getChunk(this.cachedPos, sectionCoords) : chunk);
		section.setNeighbor(ret, dir);

		return ret;
	}

	SectionContainer<LD, LI, LC> getSection(final long sectionCoords, final boolean fetchChunk)
	{
		final SectionContainer<LD, LI, LC> ret = this.getExistingSection(sectionCoords);

		if (ret != null)
			return ret;

		sectionCoordsToPos(this.cachedPos, sectionCoords);

		return this.createSection(this.cachedPos, sectionCoords, fetchChunk ? this.getChunk(this.cachedPos, sectionCoords) : null);
	}

	@Nullable SectionContainer<LD, LI, LC> getExistingSection(final long sectionCoords)
	{
		if (sectionCoords == this.lastSectionCoords)
			return this.lastSection;

		final int index = this.sectionCache.get(sectionCoords);
		final SectionContainer<LD, LI, LC> section = index == -1 ? null : this.getSection(index);

		this.lastSectionCoords = sectionCoords;
		this.lastSection = section;

		return section;
	}

	@Nullable SectionContainer<LD, LI, LC> getExistingSection(final SectionContainer<LD, LI, LC> section, final EnumFacing dir)
	{
		SectionContainer<LD, LI, LC> ret = section.getNeighbor(dir);

		if (ret != null)
		{
			this.lastSectionCoords = ret.sectionCoords;
			this.lastSection = ret;

			return ret;
		}

		final long sectionCoords = moveSectionCoords(section.sectionCoords, dir);

		ret = this.getExistingSection(sectionCoords);

		if (ret != null)
			section.setNeighbor(ret, dir);

		return ret;
	}

	boolean isValid(final BlockPos pos)
	{
		return pos.getY() >= 0 && pos.getY() < 256;
	}

	private void initSectionContainer(final SectionContainer<LD, LI, LC> section, final BlockPos pos, final long sectionCoords, @Nullable final Chunk chunk)
	{
		section.init(this.isValid(pos), pos, sectionCoords);

		this.initSectionContainer(section, chunk);
	}

	private void initSectionContainer(final SectionContainer<LD, LI, LC> section, @Nullable final Chunk chunk)
	{
		section.chunk = chunk;

		if (!section.isValid || chunk == null)
			return;

		final int yIndex = section.yIndex;

		final ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();
		ExtendedBlockStorage blockStorage = storageArrays[yIndex];

		section.blockStorage = blockStorage;

		if (blockStorage != Chunk.NULL_BLOCK_STORAGE)
		{
			final TypedLightStorage<?, ?, ?, ?, ?, ?> lightStorage = ((IVanillaLightStorageHolder) blockStorage).getLightStorage();
			section.lightStorage = this.lightManager.checkCachedProviderType(lightStorage);
		}

		if (!this.needsUpperStorage)
			return;

		section.initLowerStorage = this.initLowerStorage && yIndex > 0 && storageArrays[yIndex - 1] == null;

		section.upperLightStorage = null;
		section.upperPos.setPos(section.pos);

		for (int y = yIndex + 1; y < storageArrays.length; ++y)
		{
			blockStorage = storageArrays[y];

			if (blockStorage != Chunk.NULL_BLOCK_STORAGE)
			{
				final TypedLightStorage<?, ?, ?, ?, ?, ?> lightStorage = ((IVanillaLightStorageHolder) blockStorage).getLightStorage();
				section.upperLightStorage = this.lightManager.checkCachedProviderType(lightStorage);
				section.upperPos.setPos(section.pos.getX(), y << 4, section.pos.getZ());

				return;
			}
		}
	}

	ILightCollectionQueueDataset<LD, LCD, PooledShortQueue> getLightCollectionDataSet()
	{
		if (this.lightCollectionDataSetCache.isEmpty())
			return this.lightCollectionDataSetProvider.get(this.dataQueueProvider);

		return this.lightCollectionDataSetCache.pop();
	}

	void releaseLightCollectionDataSet(final ILightCollectionQueueDataset<LD, LCD, PooledShortQueue> dataSet)
	{
		if (this.lightCollectionDataSetCache.size() < EXPECTED_LIGHT_COLLECTION_DATASET_COUNT)
			this.lightCollectionDataSetCache.push(dataSet);
	}

	ILightQueueDataset<LD, PooledShortQueue> getLightDataSet()
	{
		if (this.lightDataSetCache.isEmpty())
			return this.lightDataSetProvider.get(this.dataQueueProvider);

		return this.lightDataSetCache.pop();
	}

	void releaseLightDataSet(final ILightQueueDataset<LD, PooledShortQueue> dataSet)
	{
		if (this.lightDataSetCache.size() < EXPECTED_LIGHT_DATASET_COUNT)
			this.lightDataSetCache.push(dataSet);
	}

	PooledShortQueue getDataQueue()
	{
		if (this.dataQueueCache.isEmpty())
			return new PooledShortQueue(this.dataQueueSegmentPool);

		return this.dataQueueCache.pop();
	}

	void releaseDataQueue(final PooledShortQueue dataQueue)
	{
		if (this.dataQueueCache.size() < EXPECTED_DATA_QUEUE_COUNT)
			this.dataQueueCache.push(dataQueue);
	}

	private abstract class AbstractLightQueue<D, DS extends ILightQueueDataset<D, PooledShortQueue>>
		implements ILightQueueIterator<D, LD, LI, IVanillaWorldInterface.Extended, V>
	{
		private final ArrayList<DS> updates = new ArrayList<>(EXPECTED_SECTION_COUNT);
		private final PooledIntQueue updateIndices = new PooledIntQueue(VanillaLightHandler.this.indexQueueSegmentPool);

		// Iterator state
		private DS curDataset;
		private PooledShortQueue curQueue;
		private SectionContainer<LD, LI, LC> curSection;

		protected abstract DS getDataSet();

		protected abstract void releaseDataSet(DS dataSet);

		protected short update(final short data)
		{
			return data;
		}

		@Override
		public boolean next()
		{
			if (this.curQueue == null || this.curQueue.isEmpty())
			{
				if (this.curQueue != null)
					VanillaLightHandler.this.releaseDataQueue(this.curQueue);

				if (this.curDataset == null || !this.curDataset.next())
				{
					if (this.curDataset != null)
					{
						this.updates.set(this.curSection.index, null);
						this.releaseDataSet(this.curDataset);
					}

					if (this.updateIndices.isEmpty())
					{
						this.curDataset = null;
						this.curQueue = null;
						this.curSection = null;

						if (this.updates.size() > EXPECTED_SECTION_COUNT)
						{
							for (int i = this.updates.size(); i > EXPECTED_SECTION_COUNT; --i)
								this.updates.remove(i - 1);

							this.updates.trimToSize();
						}

						return false;
					}

					final int sectionIndex = this.updateIndices.poll();
					this.curSection = VanillaLightHandler.this.getSection(sectionIndex);

					this.curDataset = this.updates.get(sectionIndex); // Not null
					this.curDataset.next(); // Not empty
				}

				this.curQueue = this.curDataset.getQueue();
			}

			final short data = this.curQueue.poll(); // Not empty
			VanillaLightHandler.this.updateAll(this.curSection, this.update(data));

			return true;
		}

		@Override
		public D getDescriptor()
		{
			return this.curDataset.getDesc();
		}

		@Override
		public VirtuallySourced.NeighborAware.Extended<LD, LI, IVanillaWorldInterface.Extended, V> getLightAccess()
		{
			return VanillaLightHandler.this;
		}

		protected DS getDataSet(final SectionContainer<LD, LI, LC> section)
		{
			final int sectionIndex = section.index;

			if (sectionIndex >= this.updates.size())
			{
				this.updates.ensureCapacity(sectionIndex + 1);

				for (int i = this.updates.size(); i <= sectionIndex; ++i)
					this.updates.add(null);
			}

			DS dataSet = this.updates.get(sectionIndex);

			if (dataSet == null)
			{
				dataSet = this.getDataSet();

				this.updates.set(sectionIndex, dataSet);
				this.updateIndices.add(sectionIndex);
			}

			return dataSet;
		}

		protected void enqueue(final SectionContainer<LD, LI, LC> section, final D desc, final short data)
		{
			this.getDataSet(section).get(desc).add(data);
		}

		protected boolean enqueue(final D desc, final BlockPos pos, final short extraData)
		{
			if (!VanillaLightHandler.this.isValid(pos))
				return false;

			this.enqueue(VanillaLightHandler.this.getSection(posToSectionCoords(pos), false), desc, (short) (posToLocalCoords(pos) | extraData));

			return true;
		}
	}

	private abstract class AbstractLightCheckQueue extends AbstractLightQueue<LCD, ILightCollectionQueueDataset<LD, LCD, PooledShortQueue>>
	{
		@Override
		protected ILightCollectionQueueDataset<LD, LCD, PooledShortQueue> getDataSet()
		{
			return VanillaLightHandler.this.getLightCollectionDataSet();
		}

		@Override
		protected void releaseDataSet(final ILightCollectionQueueDataset<LD, LCD, PooledShortQueue> dataSet)
		{
			VanillaLightHandler.this.releaseLightCollectionDataSet(dataSet);
		}

		@Override
		protected void enqueue(final SectionContainer<LD, LI, LC> section, @Nullable final LCD desc, final short data)
		{
			if (desc == null)
				this.getDataSet(section).get().add(data);
			else
				super.enqueue(section, desc, data);
		}

		@Override
		protected boolean enqueue(final @Nullable LCD desc, final BlockPos pos, final short extraData)
		{
			if (!VanillaLightHandler.this.isValid(pos))
				return false;

			this.enqueue(VanillaLightHandler.this.getSection(posToSectionCoords(pos), false), desc, (short) (posToLocalCoords(pos) | extraData));

			return true;
		}
	}

	private abstract class AbstractLightUpdateQueue extends AbstractLightQueue<LD, ILightQueueDataset<LD, PooledShortQueue>>
	{
		@Override
		protected ILightQueueDataset<LD, PooledShortQueue> getDataSet()
		{
			return VanillaLightHandler.this.getLightDataSet();
		}

		@Override
		protected void releaseDataSet(final ILightQueueDataset<LD, PooledShortQueue> dataSet)
		{
			VanillaLightHandler.this.releaseLightDataSet(dataSet);
		}
	}

	private class LightInitQueue
		extends AbstractLightCheckQueue
		implements ILightInitQueue<LD, LCD, LI, IVanillaWorldInterface.Extended, V>,
		ILightInitQueueIterator<LD, LCD, LI, IVanillaWorldInterface.Extended, V>
	{
		@Override
		public ILightInitQueueIterator<LD, LCD, LI, IVanillaWorldInterface.Extended, V> activate()
		{
			return this;
		}

		@Override
		public boolean enqueueInit(final BlockPos pos)
		{
			return this.enqueueInit(null, pos);
		}

		@Override
		public boolean enqueueInit(final @Nullable LCD desc, final BlockPos pos)
		{
			return this.enqueue(desc, pos, (short) 0);
		}
	}

	private class LightCheckQueue
		extends AbstractLightCheckQueue
		implements ILightCheckQueue<LD, LCD, LI, IVanillaWorldInterface.Extended, V>,
		ILightCheckQueueIterator<LD, LCD, LI, IVanillaWorldInterface.Extended, V>
	{
		private final MutableBlockPos cachedPos = new MutableBlockPos();
		private @Nullable EnumFacing dir;

		@Override
		protected short update(final short data)
		{
			this.dir = getDirection(data);
			return clearDir(data);
		}

		@Override
		public ILightCheckQueueIterator<LD, LCD, LI, IVanillaWorldInterface.Extended, V> activate()
		{
			return this;
		}

		@Override
		public boolean enqueueCheck(final BlockPos pos, @Nullable final EnumFacing dir)
		{
			return this.enqueueCheck(null, pos, dir);
		}

		@Override
		public boolean enqueueCheck(final @Nullable LCD desc, final BlockPos pos, @Nullable EnumFacing dir)
		{
			this.cachedPos.setPos(pos);

			if (dir != null)
			{
				this.cachedPos.move(dir);
				dir = dir.getOpposite();
			}

			return this.enqueue(desc, this.cachedPos, dirToData(dir));
		}

		@Override
		public @Nullable EnumFacing getDir()
		{
			return this.dir;
		}

		@Override
		public void markForRecheck()
		{
		}

		@Override
		public void markForRecheck(final EnumFacing dir)
		{
		}
	}

	private class LightSpreadQueue
		extends AbstractLightUpdateQueue
		implements ILightSpreadQueue<LD, LI, IVanillaWorldInterface.Extended, V>,
		ILightSpreadQueueIterator<LD, LI, IVanillaWorldInterface.Extended, V>
	{
		private EnumFacing dir;

		@Override
		protected short update(final short data)
		{
			this.dir = getDirection(data);
			return clearDir(data);
		}

		@Override
		public ILightSpreadQueueIterator<LD, LI, IVanillaWorldInterface.Extended, V> activate()
		{
			return this;
		}

		@Override
		public boolean enqueueSpread(final LD desc, final BlockPos pos, final EnumFacing dir)
		{
			return this.enqueue(desc, pos, dirToData(dir));
		}

		@Override
		public EnumFacing getDir()
		{
			return this.dir;
		}

		@Override
		public void markForSpread(final EnumFacing dir)
		{
		}
	}

	private class LightUpdateQueue
		extends AbstractLightUpdateQueue
		implements ILightUpdateQueue<LD, LI, IVanillaWorldInterface.Extended, V>,
		ILightUpdateQueueIterator<LD, LI, IVanillaWorldInterface.Extended, V>
	{
		@Override
		public ILightUpdateQueueIterator<LD, LI, IVanillaWorldInterface.Extended, V> activate()
		{
			return this;
		}

		@Override
		public void enqueueDarkening(final LD desc, final int oldLight)
		{
			this.enqueue(desc);
		}

		@Override
		public void enqueueDarkening(final LD desc, final EnumFacing dir, final int oldLight)
		{
			this.enqueue(desc, dir);
		}

		@Override
		public void enqueueBrightening(final LD desc, final int newLight)
		{
			this.enqueue(desc);
		}

		@Override
		public void enqueueBrightening(final LD desc, final EnumFacing dir, final int newLight)
		{
			this.enqueue(desc, dir);
		}

		@Override
		public void markForRecheck()
		{
		}

		@Override
		public void markForSpread(final EnumFacing dir)
		{
		}

		private void enqueue(final VanillaLightAccess<LD, LCD, LI, LC, ?, ?> lightAccess, final LD desc)
		{
			this.enqueue(lightAccess.section, desc, lightAccess.data);
		}

		private void enqueue(final LD desc)
		{
			this.enqueue(VanillaLightHandler.this, desc);
		}

		private void enqueue(final LD desc, final EnumFacing dir)
		{
			this.enqueue(VanillaLightHandler.this.getNeighbor(dir), desc);
		}
	}

	@Override
	public ILightUpdateQueue<LD, LI, IVanillaWorldInterface.Extended, V> createUpdateQueue()
	{
		return new LightUpdateQueue();
	}

	@Override
	public ILightCheckQueue<LD, LCD, LI, IVanillaWorldInterface.Extended, V> createCheckQueue()
	{
		return new LightCheckQueue();
	}

	@Override
	public ILightSpreadQueue<LD, LI, IVanillaWorldInterface.Extended, V> createSpreadQueue()
	{
		return new LightSpreadQueue();
	}

	@Override
	public ILightInitQueue<LD, LCD, LI, IVanillaWorldInterface.Extended, V> createInitQueue()
	{
		return new LightInitQueue();
	}

	@Override
	public void prepare()
	{
		for (int i = 0; i < this.sectionCount; ++i)
		{
			final SectionContainer<LD, LI, LC> section = this.sectionList.get(i);
			this.initSectionContainer(section, this.getChunk(section.pos, section.sectionCoords));
		}
	}

	@Override
	public void cleanup()
	{
		this.lastChunkCoords = -1;
		this.lastChunk = null;

		this.lastSectionCoords = -1;
		this.lastSection = null;

		this.sectionCache.clear();
		this.sectionCache.trim(EXPECTED_SECTION_COUNT);

		for (int i = 0; i < this.sectionCount; ++i)
			this.sectionList.get(i).cleanup();

		if (this.sectionList.size() > EXPECTED_SECTION_COUNT)
		{
			for (int i = this.sectionList.size(); i > EXPECTED_SECTION_COUNT; --i)
				this.sectionList.remove(i - 1);

			this.sectionList.trimToSize();
		}

		this.sectionCount = 0;
	}
}
