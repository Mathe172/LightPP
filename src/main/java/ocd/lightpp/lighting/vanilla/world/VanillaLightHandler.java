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

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import ocd.lightpp.api.lighting.ILightAccess;
import ocd.lightpp.api.lighting.ILightHandler;
import ocd.lightpp.api.vanilla.type.CachedLightProviderType.TypedCachedLightProvider;
import ocd.lightpp.api.vanilla.type.LightProviderType.TypedLightProvider;
import ocd.lightpp.api.vanilla.type.TypedEmptySectionLightPredictor;
import ocd.lightpp.api.vanilla.type.TypedLightStorage;
import ocd.lightpp.api.vanilla.world.ILightStorageProvider;
import ocd.lightpp.api.vanilla.world.IVanillaLightQueueDataset;
import ocd.lightpp.api.vanilla.world.IVanillaLightStorageHolder;
import ocd.lightpp.api.vanilla.world.IVanillaWorldInterface;
import ocd.lightpp.util.PooledIntQueue;
import ocd.lightpp.util.PooledShortQueue;

public class VanillaLightHandler<D, LI, V, C>
	extends VanillaLightAccess<D, LI, C>
	implements ILightHandler<D, LI, IVanillaWorldInterface.Extended, V>,
	ILightAccess.VirtuallySourced.NeighborAware.Extended<D, LI, IVanillaWorldInterface.Extended, V>
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
	private static final long mChunk = mgX | mgZ;

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

	private static long getChunkCoords(final BlockPos pos)
	{
		return (((long) ((pos.getX() >> 4) + ogX) << sgX) | ((long) ((pos.getZ() >> 4) + ogZ) << sgZ));
	}

	// Local section coordinates

	// Layout parameters
	// Length of bit segments
	private static final int
		llX = 4,
		llY = 4,
		llZ = 4;

	// Bit segment shifts/positions
	private static final int
		slZ = 0,
		slY = slZ + llZ + 1,
		slX = slY + llY + 1;

	// Bit segment masks
	private static final short
		mlX = (1 << llX) - 1,
		mlY = (1 << llY) - 1,
		mlZ = (1 << llZ) - 1;

	// Bit segment masks including check bits
	private static final short
		mlXC = (1 << (llX + 1)) - 1,
		mlYC = (1 << (llY + 1)) - 1,
		mlZC = (1 << (llZ + 1)) - 1;

	private static final short[] lNeighborShifts = new short[6];
	private static final short lCheckMask;
	private static final short[] lCheckRefs = new short[6];
	private static final short lCheckRes;

	static
	{
		lCheckMask = (1 << (slX + llX)) | (1 << (slY + llY)) | (1 << (slZ + llZ));
		lCheckRes = (short) ~lCheckMask;

		for (int i = 0; i < 6; ++i)
		{
			final Vec3i offset = EnumFacing.VALUES[i].getDirectionVec();

			final short data = (short) (((offset.getX() & mlXC) << slX) | ((offset.getY() & mlYC) << slY) | ((offset.getZ() & mlZC) << slZ));

			lNeighborShifts[i] = (short) (data & lCheckRes);
			lCheckRefs[i] = (short) (data & lCheckMask);
		}
	}

	private static void addLocalCoords(final MutableBlockPos pos, final short localCoords)
	{
		pos.add(localCoords >> slX & mlX, localCoords >> slY & mlY, localCoords >> slZ & mlZ);
	}

	static short posToLocalCoords(final BlockPos pos)
	{
		return (short) (((pos.getX() & 255) << slX) | ((pos.getY() & 255) << slY) | ((pos.getZ() & 255) << slZ));
	}

	private static final int EXPECTED_SECTION_COUNT = 1 << 11;
	private static final int EXPECTED_DATASET_COUNT = 1 << 11;
	private static final int EXPECTED_DATA_QUEUE_COUNT = 1 << 11;

	final boolean needsUpperStorage;
	private final boolean initLowerStorage;

	final World world;

	private final Supplier<IVanillaLightQueueDataset<D, PooledShortQueue>> dataSetProvider;

	private final Long2IntOpenHashMap sectionCache;
	private final ArrayList<SectionContainer<D, LI, C>> sectionList = new ArrayList<>(EXPECTED_SECTION_COUNT);
	private int sectionCount;

	@SuppressWarnings("unchecked") // Fuck you Java
	private final VanillaLightAccess<D, LI, C> lightAccesses[] = new VanillaLightAccess[7];

	private final MutableBlockPos cachedPos = new MutableBlockPos();
	private final MutableBlockPos cachedNeighborPos = new MutableBlockPos();

	private long lastChunkCoords = -1;
	private @Nullable Chunk lastChunk;

	private long lastSectionCoords = -1;
	private @Nullable SectionContainer<D, LI, C> lastSection;

	private final PooledIntQueue.SegmentPool indexQueueSegmentPool = new PooledIntQueue.SegmentPool(1 << 7, 1 << 7);
	private final PooledShortQueue.SegmentPool dataQueueSegmentPool = new PooledShortQueue.SegmentPool(1 << 12, 1 << 9);

	private final Deque<IVanillaLightQueueDataset<D, PooledShortQueue>> dataSetCache = new ArrayDeque<>();
	private final Deque<PooledShortQueue> dataQueueCache = new ArrayDeque<>();

	<WI> VanillaLightHandler(
		final World world,
		final ILightStorageProvider<D, LI, WI, C, NibbleArray> lightStorageProvider,
		@Nullable final TypedCachedLightProvider<D, LI, WI, ?> skyLightProvider,
		@Nullable final TypedEmptySectionLightPredictor<D, LI, WI, ?> emptySectionLightPredictor,
		final TypedLightProvider<D, LI, WI> emptyLightProvider,
		final IVanillaLightQueueDataset.Provider<D> dataSetProvider
	)
	{
		super(lightStorageProvider,
			skyLightProvider,
			emptySectionLightPredictor,
			emptyLightProvider
		);

		this.needsUpperStorage = this.lightManager.needsUpperStorage();
		this.initLowerStorage = this.lightManager.emptySectionLightPredictor != null;

		this.world = world;

		this.sectionCache = new Long2IntOpenHashMap(EXPECTED_SECTION_COUNT);
		this.sectionCache.defaultReturnValue(-1);

		this.lightAccesses[6] = this;

		for (int i = 0; i < 6; ++i)
			this.lightAccesses[i] = new VanillaLightAccess<D, LI, C>(
				lightStorageProvider,
				skyLightProvider,
				emptySectionLightPredictor,
				emptyLightProvider
			)
			{
				@Override
				protected VanillaLightHandler<D, LI, ?, C> getLightHandler()
				{
					return VanillaLightHandler.this;
				}
			};

		this.dataSetProvider = () -> dataSetProvider.get(VanillaLightHandler.this::getDataQueue);
	}

	@Override
	protected VanillaLightHandler<D, LI, V, C> getLightHandler()
	{
		return this;
	}

	@Override
	public VanillaLightAccess<D, LI, C> getNeighbor(final EnumFacing dir)
	{
		return this.lightAccesses[dir.ordinal()];
	}

	@Override
	public @Nullable V getVirtualSources()
	{
		return null;
	}

	private void updateAll(final SectionContainer<D, LI, C> section, final short data)
	{
		this.cachedPos.setPos(section.pos);
		addLocalCoords(this.cachedPos, data);

		this.update(section, this.cachedPos, data);

		for (int i = 0; i < 6; ++i)
		{
			final EnumFacing dir = EnumFacing.VALUES[i];

			short neighborData = (short) (data + lNeighborShifts[i]);
			final SectionContainer<D, LI, C> neighborSection;

			if ((neighborData & lCheckMask) == lCheckRefs[i])
				neighborSection = section;
			else
				neighborSection = dir.getAxis() == Axis.Y ? this.getSection(section, dir, section.chunk) : this.getSection(section, dir, true);

			neighborData &= lCheckRes;

			this.cachedNeighborPos.setPos(this.cachedPos);
			this.cachedNeighborPos.move(dir);

			this.lightAccesses[i].update(neighborSection, this.cachedNeighborPos, neighborData);
		}
	}

	void updateAll()
	{
		for (final VanillaLightAccess<D, LI, C> lightAccess : this.lightAccesses)
			lightAccess.update();
	}

	private @Nullable Chunk getChunk(final BlockPos pos)
	{
		final long chunkCoords = getChunkCoords(pos);

		if (chunkCoords == this.lastChunkCoords)
			return this.lastChunk;

		this.lastChunkCoords = chunkCoords;
		this.lastChunk = this.world.getChunkProvider().getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4);

		return this.lastChunk;
	}

	private SectionContainer<D, LI, C> createSection(final BlockPos pos, final long sectionCoords, @Nullable final Chunk chunk)
	{
		final SectionContainer<D, LI, C> section;

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

	private SectionContainer<D, LI, C> getSection(final int index)
	{
		return this.sectionList.get(index);
	}

	SectionContainer<D, LI, C> getSection(
		final SectionContainer<D, LI, C> section,
		final EnumFacing dir,
		@Nullable final Chunk chunk
	)
	{
		return this.getSection(section, dir, chunk, false);
	}

	SectionContainer<D, LI, C> getSection(
		final SectionContainer<D, LI, C> section,
		final EnumFacing dir,
		final boolean fetchChunk
	)
	{
		return this.getSection(section, dir, null, fetchChunk);
	}

	private SectionContainer<D, LI, C> getSection(
		final SectionContainer<D, LI, C> section,
		final EnumFacing dir,
		@Nullable final Chunk chunk,
		final boolean fetchChunk
	)
	{
		SectionContainer<D, LI, C> ret = this.getExistingSection(section, dir);

		if (ret != null)
			return ret;

		final long sectionCoords = moveSectionCoords(section.sectionCoords, dir);
		sectionCoordsToPos(this.cachedPos, sectionCoords);

		ret = this.createSection(this.cachedPos, sectionCoords, fetchChunk ? this.getChunk(this.cachedPos) : chunk);
		section.setNeighbor(ret, dir);

		return ret;
	}

	SectionContainer<D, LI, C> getSection(final long sectionCoords, final boolean fetchChunk)
	{
		final SectionContainer<D, LI, C> ret = this.getExistingSection(sectionCoords);

		if (ret != null)
			return ret;

		sectionCoordsToPos(this.cachedPos, sectionCoords);

		return this.createSection(this.cachedPos, sectionCoords, fetchChunk ? this.getChunk(this.cachedPos) : null);
	}

	@Nullable SectionContainer<D, LI, C> getExistingSection(final long sectionCoords)
	{
		if (sectionCoords == this.lastSectionCoords)
			return this.lastSection;

		final int index = this.sectionCache.get(sectionCoords);
		final SectionContainer<D, LI, C> section = index == -1 ? null : this.getSection(index);

		this.lastSectionCoords = sectionCoords;
		this.lastSection = section;

		return section;
	}

	@Nullable SectionContainer<D, LI, C> getExistingSection(final SectionContainer<D, LI, C> section, final EnumFacing dir)
	{
		SectionContainer<D, LI, C> ret = section.getNeighbor(dir);

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

	private void initSectionContainer(final SectionContainer<D, LI, C> section, final BlockPos pos, final long sectionCoords, @Nullable final Chunk chunk)
	{
		section.init(this.isValid(pos), pos, sectionCoords);

		this.initSectionContainer(section, chunk);
	}

	private void initSectionContainer(final SectionContainer<D, LI, C> section, @Nullable final Chunk chunk)
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
			final TypedLightStorage<?, ?, ?, ?, ?> lightStorage = ((IVanillaLightStorageHolder) blockStorage).getLightStorage();
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
				final TypedLightStorage<?, ?, ?, ?, ?> lightStorage = ((IVanillaLightStorageHolder) blockStorage).getLightStorage();
				this.section.upperLightStorage = this.lightManager.checkCachedProviderType(lightStorage);
				this.section.upperPos.setPos(this.section.pos.getX(), y << 4, this.section.pos.getZ());

				return;
			}
		}
	}

	IVanillaLightQueueDataset<D, PooledShortQueue> getDataSet()
	{
		if (this.dataSetCache.isEmpty())
			return this.dataSetProvider.get();

		return this.dataSetCache.pop();
	}

	void releaseDataSet(final IVanillaLightQueueDataset<D, PooledShortQueue> dataSet)
	{
		if (this.dataSetCache.size() < EXPECTED_DATASET_COUNT)
			this.dataSetCache.push(dataSet);
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

	private class VanillaLightUpdateQueue
		implements ILightUpdateQueue<D, LI, IVanillaWorldInterface.Extended, V>,
		ILightUpdateQueueIterator<D, LI, IVanillaWorldInterface.Extended, V>
	{
		private final ArrayList<IVanillaLightQueueDataset<D, PooledShortQueue>> updates = new ArrayList<>(EXPECTED_SECTION_COUNT);
		private final PooledIntQueue updateIndices = new PooledIntQueue(VanillaLightHandler.this.indexQueueSegmentPool);

		// Iterator state
		private IVanillaLightQueueDataset<D, PooledShortQueue> curDataset;
		private PooledShortQueue curQueue;
		private SectionContainer<D, LI, C> curSection;

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
						VanillaLightHandler.this.releaseDataSet(this.curDataset);
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
			VanillaLightHandler.this.updateAll(this.curSection, data);

			return true;
		}

		@Override
		public D getDescriptor()
		{
			return this.curDataset.getDesc();
		}

		@Override
		public VirtuallySourced.NeighborAware.Extended<D, LI, IVanillaWorldInterface.Extended, V> getLightAccess()
		{
			return VanillaLightHandler.this;
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
		public ILightUpdateQueueIterator<D, LI, IVanillaWorldInterface.Extended, V> activate()
		{
			return this;
		}

		@Override
		public void enqueueDarkening(final D desc, final int oldLight)
		{
			this.enqueue(desc, null);
		}

		@Override
		public void enqueueDarkening(final D desc, final EnumFacing dir, final int oldLight)
		{
			this.enqueue(desc, dir);
		}

		@Override
		public void enqueueBrightening(final D desc, final int newLight)
		{
			this.enqueue(desc, null);
		}

		@Override
		public void enqueueBrightening(final D desc, final EnumFacing dir, final int newLight)
		{
			this.enqueue(desc, dir);
		}

		private void enqueue(final D desc, @Nullable final EnumFacing dir)
		{
			final VanillaLightAccess<D, LI, C> lightAccess = dir == null ? VanillaLightHandler.this : VanillaLightHandler.this.getNeighbor(dir);

			final int sectionIndex = lightAccess.section.index;

			if (sectionIndex > this.updates.size())
			{
				this.updates.ensureCapacity(sectionIndex + 1);

				for (int i = this.updates.size(); i <= sectionIndex; ++i)
					this.updates.add(null);
			}

			IVanillaLightQueueDataset<D, PooledShortQueue> dataSet = this.updates.get(sectionIndex);

			if (dataSet == null)
			{
				dataSet = VanillaLightHandler.this.getDataSet();

				this.updates.set(sectionIndex, dataSet);
				this.updateIndices.add(sectionIndex);
			}

			dataSet.get(desc).add(lightAccess.data);
		}
	}

	@Override
	public ILightUpdateQueue<D, LI, IVanillaWorldInterface.Extended, V> createUpdateQueue()
	{
		return new VanillaLightUpdateQueue();
	}

	@Override
	public ILightCheckQueue<D, LI, IVanillaWorldInterface.Extended, V> createCheckQueue()
	{
		return null;
	}

	@Override
	public ILightSpreadQueue<D, LI, IVanillaWorldInterface.Extended, V> createSpreadQueue()
	{
		return null;
	}

	@Override
	public ILightInitQueue<D, LI, IVanillaWorldInterface.Extended, V> createInitQueue()
	{
		return null;
	}

	@Override
	public void prepare()
	{
		for (int i = 0; i < this.sectionCount; ++i)
		{
			final SectionContainer<D, LI, C> section = this.sectionList.get(i);
			this.initSectionContainer(section, this.getChunk(section.pos));
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
