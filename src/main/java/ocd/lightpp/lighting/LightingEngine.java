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
 *
 */

package ocd.lightpp.lighting;

import java.util.function.Supplier;
import javax.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import net.minecraft.profiler.Profiler;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import ocd.lightpp.api.lighting.ILightAccess;
import ocd.lightpp.api.lighting.ILightAccess.NeighborAware;
import ocd.lightpp.api.lighting.ILightCollectionDescriptor;
import ocd.lightpp.api.lighting.ILightHandler;
import ocd.lightpp.api.lighting.ILightHandler.ILightCheckQueue;
import ocd.lightpp.api.lighting.ILightHandler.ILightCheckQueueIterator;
import ocd.lightpp.api.lighting.ILightHandler.ILightInitQueue;
import ocd.lightpp.api.lighting.ILightHandler.ILightInitQueueIterator;
import ocd.lightpp.api.lighting.ILightHandler.ILightSpreadQueue;
import ocd.lightpp.api.lighting.ILightHandler.ILightSpreadQueueIterator;
import ocd.lightpp.api.lighting.ILightHandler.ILightUpdateQueue;
import ocd.lightpp.api.lighting.ILightHandler.ILightUpdateQueueIterator;
import ocd.lightpp.api.lighting.ILightMap;
import ocd.lightpp.api.lighting.ILightMap.ILightIterator;
import ocd.lightpp.api.lighting.ILightPropagator;

public class LightingEngine<LD, LCD extends ILightCollectionDescriptor<LD>, MI, LI, WI, V>
{
	private final Profiler profiler;

	private final int maxLight;

	private final ILightHandler<LD, LCD, LI, WI, V> lightHandler;
	private final ILightPropagator<LD, LCD, ? super MI, ? super LI, ? super WI, ? super V> lightPropagator;

	private final ILightCheckQueue<LD, LCD, LI, WI, V> queuedChecks;
	private final ILightSpreadQueue<LD, LI, WI, V> queuedSpreads;
	private final ILightInitQueue<LD, LCD, LI, WI, V> queuedInits;

	private final ILightUpdateQueue<LD, LI, WI, V> initialDarkenings;
	private final ILightUpdateQueue<LD, LI, WI, V>[] initialBrightenings;

	private final ILightUpdateQueue<LD, LI, WI, V>[] queuedDarkenings;
	private final ILightUpdateQueue<LD, LI, WI, V>[] queuedBrightenings;

	private final ILightMap<LD, MI> procLightMap;

	// whether there are any checks left to do
	private boolean hasUpdates;

	private final boolean[] isNeighborProcessed = new boolean[7];
	@SuppressWarnings("unchecked")
	private final ILightMap<LD, MI>[] neighborSpread = (ILightMap<LD, MI>[]) new ILightMap[7];
	@SuppressWarnings("unchecked")
	private final ILightMap<LD, MI>[] oldNeighborLight = (ILightMap<LD, MI>[]) new ILightMap[7];

	private static final EnumFacing[] DIRECTIONS_NULL = ArrayUtils.add(EnumFacing.VALUES, null);

	public LightingEngine(
		final ILightHandler<LD, LCD, LI, WI, V> lightHandler,
		final int maxLight,
		final ILightPropagator<LD, LCD, ? super MI, ? super LI, ? super WI, ? super V> lightPropagator,
		final Supplier<? extends ILightMap<LD, MI>> lightMapProvider,
		final Profiler profiler)
	{
		this.profiler = profiler;

		this.lightHandler = lightHandler;

		this.maxLight = maxLight;
		this.lightPropagator = lightPropagator;

		this.queuedChecks = this.lightHandler.createCheckQueue();
		this.queuedSpreads = this.lightHandler.createSpreadQueue();
		this.queuedInits = this.lightHandler.createInitQueue();

		this.queuedDarkenings = this.createUpdateQueues();
		this.queuedBrightenings = this.createUpdateQueues();
		this.initialDarkenings = this.lightHandler.createUpdateQueue();
		this.initialBrightenings = this.createUpdateQueues();

		this.isNeighborProcessed[6] = true; // needed to treat reflections more uniformly

		this.procLightMap = lightMapProvider.get();

		for (int i = 0; i < this.neighborSpread.length; ++i)
			this.neighborSpread[i] = lightMapProvider.get();

		for (int i = 0; i < this.oldNeighborLight.length; ++i)
			this.oldNeighborLight[i] = lightMapProvider.get();
	}

	private ILightUpdateQueue<LD, LI, WI, V>[] createUpdateQueues()
	{
		@SuppressWarnings("unchecked") final ILightUpdateQueue<LD, LI, WI, V>[] queues = (ILightUpdateQueue<LD, LI, WI, V>[]) new ILightUpdateQueue[this.maxLight];

		for (int i = 0; i < queues.length; ++i)
			queues[i] = this.lightHandler.createUpdateQueue();

		return queues;
	}

	public boolean scheduleLightCheck(final BlockPos pos, final @Nullable EnumFacing dir)
	{
		final boolean accepted = this.queuedChecks.enqueueCheck(pos, dir);

		this.hasUpdates |= accepted;

		return accepted;
	}

	public boolean scheduleLightCheck(final LCD desc, final BlockPos pos, final @Nullable EnumFacing dir)
	{
		final boolean accepted = this.queuedChecks.enqueueCheck(desc, pos, dir);

		this.hasUpdates |= accepted;

		return accepted;
	}

	public boolean scheduleLightSpread(final LD desc, final BlockPos pos, final EnumFacing dir)
	{
		final boolean accepted = this.queuedSpreads.enqueueSpread(desc, pos, dir);

		this.hasUpdates |= accepted;

		return accepted;
	}

	public boolean scheduleLightInit(final BlockPos pos)
	{
		final boolean accepted = this.queuedInits.enqueueInit(pos);

		this.hasUpdates |= accepted;

		return accepted;
	}

	public boolean scheduleLightInit(final LCD desc, final BlockPos pos)
	{
		final boolean accepted = this.queuedInits.enqueueInit(desc, pos);

		this.hasUpdates |= accepted;

		return accepted;
	}

	public boolean hasUpdates()
	{
		return this.hasUpdates;
	}

	public void procLightUpdates()
	{
		if (!this.hasUpdates())
			return;

		this.hasUpdates = false;

		this.lightHandler.prepare();

		this.profiler.startSection("lighting");

		if (this.procInits() | this.procSpreads() | this.procChecks())
			this.computeLightUpdates();

		this.lightHandler.cleanup();
		this.lightPropagator.cleanup();

		this.profiler.endSection();
	}

	private boolean procChecks()
	{
		boolean needsProcessing = false;

		this.profiler.startSection("checking");

		// Process the queued updates and enqueue them for further processing
		for (final ILightCheckQueueIterator<LD, LCD, LI, WI, V> it = this.queuedChecks.activate(); it.next(); )
		{
			final ILightAccess.Extended<LD, LI, WI> lightAccess = it.getLightAccess();

			if (!lightAccess.isLoaded())
			{
				final @Nullable EnumFacing dir = it.getDir();

				if (dir != null)
					it.markForRecheck(dir);

				continue;
			}

			final LCD collectionDesc = it.getDescriptor();
			final LD desc = collectionDesc.getDescriptor();

			final boolean isValid = this.calcNewLight(collectionDesc, it);

			if (desc == null)
			{
				for (final ILightIterator<LD> lit = this.procLightMap.iterator(); lit.next(); )
				{
					final LD curDesc = lit.getDescriptor();

					if (collectionDesc.contains(curDesc))
					{
						final int oldLight = lightAccess.getLight(curDesc);
						final int newLight = lit.getLight();

						needsProcessing |= this.enqueueChanges(isValid, curDesc, oldLight, newLight);
					}
				}

				if (isValid)
					for (final ILightIterator<LD> lit = lightAccess.getLightIterator(); lit.next(); )
					{
						final LD curDesc = lit.getDescriptor();

						if (collectionDesc.contains(curDesc))
						{
							final int oldLight = lit.getLight();
							final int newLight = this.procLightMap.get(curDesc);

							if (newLight == 0 && oldLight > 0)
							{
								// Don't enqueue directly for darkening in order to avoid duplicate scheduling
								this.initialDarkenings.enqueueDarkening(curDesc, oldLight);
								needsProcessing = true;
							}
						}
					}
			}
			else
			{
				final int oldLight = lightAccess.getLight(desc);
				final int newLight = this.procLightMap.get(desc);

				needsProcessing |= this.enqueueChanges(isValid, desc, oldLight, newLight);
			}
		}

		if (needsProcessing)
		{
			for (final ILightUpdateQueueIterator<LD, LI, WI, V> it = this.initialDarkenings.activate(); it.next(); )
			{
				final LD desc = it.getDescriptor();
				final ILightAccess.Extended<LD, LI, WI> lightAccess = it.getLightAccess();

				final int oldLight = lightAccess.getLight(desc);

				// Sets the light to 0 to only schedule once
				if (oldLight > 0)
					this.enqueueDarkening(desc, null, oldLight, lightAccess);
			}

			// Sets the light to newLight to only schedule once. Clear leading bits of curData for later
			for (int curLight = this.maxLight; curLight > 0; --curLight)
				for (final ILightUpdateQueueIterator<LD, LI, WI, V> it = this.initialBrightenings[curLight - 1].activate(); it.next(); )
				{
					final LD desc = it.getDescriptor();
					final ILightAccess.Extended<LD, LI, WI> lightAccess = it.getLightAccess();

					if (curLight > lightAccess.getLight(desc))
						this.enqueueBrightening(desc, null, curLight, lightAccess);
				}
		}

		this.profiler.endSection();

		return needsProcessing;
	}

	private boolean procSpreads()
	{
		boolean needsProcessing = false;

		this.profiler.startSection("spreading");

		for (final ILightSpreadQueueIterator<LD, LI, WI, V> it = this.queuedSpreads.activate(); it.next(); )
		{
			final ILightAccess.NeighborAware.Extended<LD, LI, WI> lightAccess = it.getLightAccess();

			if (!lightAccess.isLoaded())
				continue;

			final EnumFacing dir = it.getDir();

			final ILightAccess.Extended<LD, LI, WI> neighborLightAccess = lightAccess.getNeighbor(dir);

			final LD desc = it.getDescriptor();
			final int light = lightAccess.getLight(desc);

			this.lightPropagator.prepareSpread(desc, dir, light, lightAccess, neighborLightAccess);

			if (!this.lightPropagator.canSpread(desc, light, lightAccess) || !this.lightPropagator.canSpread(desc, dir, light, lightAccess))
				continue;

			if (neighborLightAccess.isLoaded())
				needsProcessing |= this.enqueueNeighborBrightening(desc, dir, light, lightAccess, neighborLightAccess);
			else
				it.markForSpread(dir);
		}

		this.profiler.endSection();

		return needsProcessing;
	}

	private boolean procInits()
	{
		boolean needsProcessing = false;

		this.profiler.startSection("init");

		for (final ILightInitQueueIterator<LD, LCD, LI, WI, V> it = this.queuedInits.activate(); it.next(); )
		{
			final LCD desc = it.getDescriptor();

			final ILightAccess.VirtuallySourced.NeighborAware.Extended<LD, LI, WI, V> lightAccess = it.getLightAccess();

			if (!lightAccess.isLoaded())
				continue;

			this.procLightMap.clear();
			this.lightPropagator.calcSourceLight(desc, lightAccess, this.procLightMap.getInterface());

			for (final ILightIterator<LD> lit = this.procLightMap.iterator(); lit.next(); )
			{
				final LD curDesc = lit.getDescriptor();

				if (desc.contains(curDesc))
				{
					final int oldLight = lightAccess.getLight(curDesc);
					final int newLight = lit.getLight();

					if (oldLight < newLight)
					{
						this.enqueueBrightening(curDesc, null, newLight, lightAccess);
						needsProcessing = true;
					}
				}
			}
		}

		this.profiler.endSection();

		return needsProcessing;
	}

	private boolean enqueueChanges(final boolean isValid, final LD desc, final int oldLight, final int newLight)
	{
		if (oldLight < newLight)
		{
			// Don't enqueue directly for brightening in order to avoid duplicate scheduling
			this.initialBrightenings[newLight - 1].enqueueBrightening(desc, newLight);
			return true;
		}

		if (oldLight > newLight && isValid)
		{
			// Don't enqueue directly for darkening in order to avoid duplicate scheduling
			this.initialDarkenings.enqueueDarkening(desc, oldLight);
			return true;
		}

		return false;
	}

	private void computeLightUpdates()
	{
		// Iterate through enqueued updates (brightening and darkening in parallel) from brightest to darkest so that we only need to iterate once
		for (int procLight = this.maxLight; procLight > 0; --procLight)
		{
			this.profiler.startSection("darkening");

			for (final ILightUpdateQueueIterator<LD, LI, WI, V> it = this.queuedDarkenings[procLight - 1].activate(); it.next(); )
			{
				final LD desc = it.getDescriptor();
				final ILightAccess.VirtuallySourced.NeighborAware.Extended<LD, LI, WI, V> lightAccess = it.getLightAccess();

				final int curLight = lightAccess.getLight(desc);

				if (curLight >= procLight) // Don't darken if we got brighter due to some other change
					continue;

				this.lightPropagator.prepareSpread(desc, procLight, lightAccess);

				boolean allNeighborsLoaded = true;

				final boolean canSpread = this.lightPropagator.canSpread(desc, procLight, lightAccess);

				// Prepare all neighbor darkenings if necessary
				if (canSpread)
				{
					for (int i = 0; i < 6; ++i)
					{
						final EnumFacing dir = EnumFacing.VALUES[i];

						final ILightAccess.Extended<LD, LI, WI> neighborLightAccess = lightAccess.getNeighbor(dir);

						this.isNeighborProcessed[i] = false;

						if (!neighborLightAccess.isValid())
							continue;

						if (!this.lightPropagator.canSpread(desc, dir, procLight, lightAccess))
							continue;

						if (!neighborLightAccess.isLoaded())
						{
							allNeighborsLoaded = false;
							continue;
						}

						this.isNeighborProcessed[i] = true;

						this.prepareNeighborDarkening(desc, dir, procLight, i, lightAccess, neighborLightAccess);
					}

					this.prepareNeighborDarkening(desc, null, procLight, 6, lightAccess, lightAccess);
				}

				if (allNeighborsLoaded)
				{
					// Need to calculate new light value from neighbors ignoring neighbors which are scheduled for darkening (those are already 0)
					this.procLightMap.clear();
					final boolean isValid = this.lightPropagator.calcLight(desc, lightAccess, this.procLightMap.getInterface());
					final int newLight = this.procLightMap.get(desc);

					if (isValid)
					{
						// Only darken neighbors if we indeed became darker
						if (newLight < procLight)
						{
							if (curLight < newLight)
								this.enqueueBrightening(desc, null, newLight, lightAccess);
							else if (curLight == 0)
								lightAccess.notifyLightSet(desc);

							if (canSpread)
								// Schedule neighbor for darkening if we possibly light it
								for (int i = 0; i < 7; ++i)
									if (this.isNeighborProcessed[i])
										for (ILightIterator<LD> lit = this.oldNeighborLight[i].iterator(); lit.next(); )
										{
											final EnumFacing dir = DIRECTIONS_NULL[i];
											final int oldLight = lit.getLight();

											if (oldLight > 0)
												this.enqueueDarkening(lit.getDescriptor(), dir, oldLight, this.getNeighborLightAccess(lightAccess, dir));
										}

							continue;
						}
					}
					else
						it.markForRecheck();
				}
				else
					it.markForRecheck();

				// We didn't become darker, so we need to re-set our initial light value (was set to 0) and notify neighbors
				lightAccess.setLight(desc, procLight);
				lightAccess.notifyLightSet(desc);

				if (!canSpread)
					continue;

				for (int i = 0; i < 7; ++i)
				{
					final @Nullable EnumFacing dir = DIRECTIONS_NULL[i];

					if (this.isNeighborProcessed[i])
						for (ILightIterator<LD> lit = this.neighborSpread[i].iterator(); lit.next(); )
						{
							final LD lDesc = lit.getDescriptor();
							final int spread = lit.getLight();

							final ILightAccess.Extended<LD, LI, WI> neighborLightAccess = this.getNeighborLightAccess(lightAccess, dir);

							final int oldLight = this.oldNeighborLight[i].get(lDesc);

							if (spread > oldLight)
								this.enqueueBrightening(lDesc, dir, spread, neighborLightAccess);
							else
							{
								neighborLightAccess.setLight(lDesc, oldLight);
								neighborLightAccess.notifyLightSet(lDesc); // Due to asynchronous nature of the rendering thread
							}
						}
				}
			}

			this.profiler.endStartSection("brightening");

			for (final ILightUpdateQueueIterator<LD, LI, WI, V> it = this.queuedBrightenings[procLight - 1].activate(); it.next(); )
			{
				final LD desc = it.getDescriptor();
				final ILightAccess.NeighborAware.Extended<LD, LI, WI> lightAccess = it.getLightAccess();

				// Only process this if nothing else has happened at this position since scheduling
				if (lightAccess.getLight(desc) != procLight)
					continue;

				lightAccess.notifyLightSet(desc);

				this.lightPropagator.prepareSpread(desc, procLight, lightAccess);

				if (!this.lightPropagator.canSpread(desc, procLight, lightAccess))
					continue;

				for (final EnumFacing dir : EnumFacing.VALUES)
				{
					final ILightAccess.Extended<LD, LI, WI> neighborLightAccess = lightAccess.getNeighbor(dir);

					if (!neighborLightAccess.isValid())
						continue;

					if (!this.lightPropagator.canSpread(desc, dir, procLight, lightAccess))
						continue;

					if (neighborLightAccess.isLoaded())
						this.enqueueNeighborBrightening(desc, dir, procLight, lightAccess, neighborLightAccess);
					else
						it.markForSpread(dir);
				}

				this.enqueueNeighborBrightening(desc, null, procLight, lightAccess, lightAccess);
			}

			this.profiler.endSection();
		}
	}

	private void prepareNeighborDarkening(
		final LD desc,
		final @Nullable EnumFacing dir,
		final int procLight,
		final int index,
		final ILightAccess.NeighborAware<LI, WI> lightAccess,
		final ILightAccess.Extended<LD, LI, WI> neighborLightAccess
	)
	{
		final ILightMap<LD, MI> lightMap = this.neighborSpread[index];
		this.calcSpread(desc, dir, procLight, lightAccess, neighborLightAccess, lightMap);

		final ILightMap<LD, MI> oldLightMap = this.oldNeighborLight[index];
		oldLightMap.clear();

		for (final ILightIterator<LD> lit = lightMap.iterator(); lit.next(); )
		{
			final int spread = lit.getLight();
			final LD lDesc = lit.getDescriptor();
			final int curNeighborLight = neighborLightAccess.getLight(lDesc);

			if (spread >= curNeighborLight && curNeighborLight > 0)
			{
				oldLightMap.set(lDesc, curNeighborLight);
				neighborLightAccess.setLight(lDesc, 0);
			}
		}
	}

	private void calcSpread(
		final LD desc,
		final @Nullable EnumFacing dir,
		final int light,
		final ILightAccess.NeighborAware<LI, WI> lightAccess,
		final ILightAccess<LI, WI> neighborLightAccess,
		final ILightMap<LD, MI> lightMap
	)
	{
		lightMap.clear();

		if (dir == null)
			this.lightPropagator.calcSpread(desc, light, lightAccess, lightMap.getInterface());
		else
			this.lightPropagator.calcSpread(desc, dir, light, lightAccess, neighborLightAccess, lightMap.getInterface());
	}

	private boolean enqueueNeighborBrightening(final LD desc, final @Nullable EnumFacing dir, final int procLight, final NeighborAware<LI, WI> lightAccess, final ILightAccess.Extended<LD, LI, WI> neighborLightAccess)
	{
		this.calcSpread(desc, dir, procLight, lightAccess, neighborLightAccess, this.procLightMap);

		boolean needsProcessing = false;

		for (final ILightIterator<LD> lit = this.procLightMap.iterator(); lit.next(); )
		{
			final int newLight = lit.getLight();

			final LD lDesc = lit.getDescriptor();

			if (newLight > neighborLightAccess.getLight(lDesc))
			{
				this.enqueueBrightening(lDesc, dir, newLight, neighborLightAccess);

				needsProcessing = true;
			}
		}

		return needsProcessing;
	}

	private ILightAccess.Extended<LD, LI, WI> getNeighborLightAccess(final ILightAccess.NeighborAware.Extended<LD, LI, WI> lightAccess, @Nullable final EnumFacing dir)
	{
		return dir == null ? lightAccess : lightAccess.getNeighbor(dir);
	}

	private boolean calcNewLight(final LCD desc, final ILightCheckQueueIterator<LD, LCD, LI, WI, V> it)
	{
		this.procLightMap.clear();

		final ILightAccess.VirtuallySourced.NeighborAware<LI, WI, V> lightAccess = it.getLightAccess();

		final boolean isValid = this.lightPropagator.calcLight(desc, lightAccess, this.procLightMap.getInterface());

		if (!isValid)
			it.markForRecheck();

		return isValid;
	}

	private void enqueueDarkening(final LD desc, final @Nullable EnumFacing dir, final int oldLight, final ILightAccess.Extended<LD, LI, WI> lightAccess)
	{
		if (dir == null)
			this.queuedDarkenings[oldLight - 1].enqueueDarkening(desc, oldLight);
		else
			this.queuedDarkenings[oldLight - 1].enqueueDarkening(desc, dir, oldLight);

		lightAccess.setLight(desc, 0);
	}

	private void enqueueBrightening(final LD desc, final @Nullable EnumFacing dir, final int newLight, final ILightAccess.Extended<LD, LI, WI> lightAccess)
	{
		if (dir == null)
			this.queuedBrightenings[newLight - 1].enqueueBrightening(desc, newLight);
		else
			this.queuedBrightenings[newLight - 1].enqueueBrightening(desc, dir, newLight);

		lightAccess.setLight(desc, newLight);
	}
}
