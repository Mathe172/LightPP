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

import net.minecraft.profiler.Profiler;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import ocd.lightpp.IThreadGuard;
import ocd.lightpp.api.lighting.ILightAccess;
import ocd.lightpp.api.lighting.ILightHandler;
import ocd.lightpp.api.lighting.ILightHandler.LightUpdateQueue;
import ocd.lightpp.api.lighting.ILightManager;
import ocd.lightpp.api.lighting.ILightPropagator;

public class LightingEngine
{
	private static final EnumSkyBlock[] LIGHT_TYPE_VALUES = EnumSkyBlock.values();

	private static final int MAX_SCHEDULED_COUNT = 1 << 22;

	public static final int MAX_LIGHT = 15;

	private final World world;
	private final Profiler profiler;

	private final ILightHandler lightHandler;
	private final ILightPropagator[] lightPropagators = new ILightPropagator[LIGHT_TYPE_VALUES.length];

	private final LightUpdateQueue[] queuedLightUpdates = new LightUpdateQueue[LIGHT_TYPE_VALUES.length];

	private final LightUpdateQueue[] queuedDarkenings = new LightUpdateQueue[MAX_LIGHT + 1];
	private final LightUpdateQueue[] queuedBrightenings = new LightUpdateQueue[MAX_LIGHT + 1];

	private final LightUpdateQueue initialDarkenings;
	private final LightUpdateQueue[] initialBrightenings = new LightUpdateQueue[MAX_LIGHT + 1];

	private boolean hasUpdates;

	private final int[] neighborSpread = new int[6];
	private final int[] maxNeighborSpread = new int[6];
	private final int[] maxNeighborLight = new int[6];

	public LightingEngine(final World world)
	{
		this.world = world;
		this.profiler = world.profiler;

		final ILightManager lightManager = (ILightManager) world;

		this.lightHandler = lightManager.createLightHandler();

		final ILightPropagator.Factory lightPropagator = lightManager.createLightPropagator();

		for (final EnumSkyBlock lightType : LIGHT_TYPE_VALUES)
			this.lightPropagators[lightType.ordinal()] = lightPropagator.create(lightType);

		for (int i = 0; i < LIGHT_TYPE_VALUES.length; ++i)
			this.queuedLightUpdates[i] = this.lightHandler.createQueue();

		for (int i = 0; i < this.queuedDarkenings.length; ++i)
			this.queuedDarkenings[i] = this.lightHandler.createQueue();

		for (int i = 0; i < this.queuedBrightenings.length; ++i)
			this.queuedBrightenings[i] = this.lightHandler.createQueue();

		this.initialDarkenings = this.lightHandler.createQueue();

		for (int i = 0; i < this.initialBrightenings.length; ++i)
			this.initialBrightenings[i] = this.lightHandler.createQueue();
	}

	/**
	 * Schedules a light update for the specified light type and position to be processed later by {@link #procLightUpdates()}
	 */
	public void scheduleLightUpdate(final EnumSkyBlock lightType, final BlockPos pos)
	{
		final LightUpdateQueue queue = this.queuedLightUpdates[lightType.ordinal()];

		queue.accept(pos, lightType);

		// Make sure there are not too many queued light updates
		if (queue.size() >= MAX_SCHEDULED_COUNT)
			this.procLightUpdates(lightType);
	}

	/**
	 * Calls {@link #procLightUpdates(EnumSkyBlock)} for both light types
	 */
	public void procLightUpdates()
	{
		this.procLightUpdates(EnumSkyBlock.SKY);
		this.procLightUpdates(EnumSkyBlock.BLOCK);
	}

	/**
	 * Processes light updates of the given light type
	 */
	public void procLightUpdates(final EnumSkyBlock lightType)
	{
		final LightUpdateQueue queue = this.queuedLightUpdates[lightType.ordinal()];

		// Renderer accesses world unsynchronized, don't modify anything in that case
		boolean ensuredNotRenderingThread = false;

		if (!queue.isEmpty())
		{
			if (!IThreadGuard.isNotRenderingThread(this.world))
				return;

			ensuredNotRenderingThread = true;

			this.profiler.startSection("lighting");
			this.profiler.startSection("checking");

			// Process the queued updates and enqueue them for further processing
			for (queue.activate(); this.lightHandler.next(); )
			{
				if (!this.lightHandler.isLoaded())
					continue;

				final int oldLight = this.lightHandler.getLight();
				final int newLight = this.calcNewLight(oldLight);

				if (oldLight < newLight)
				{
					// Don't enqueue directly for brightening in order to avoid duplicate scheduling
					this.initialBrightenings[newLight].accept();
					this.hasUpdates = true;
				}
				else if (oldLight > newLight)
				{
					// Don't enqueue directly for darkening in order to avoid duplicate scheduling
					this.initialDarkenings.accept();
					this.hasUpdates = true;
				}
			}

			this.profiler.endSection();
			this.profiler.endSection();
		}

		if (!this.hasUpdates)
			return;

		if (!ensuredNotRenderingThread && !IThreadGuard.isNotRenderingThread(this.world))
			return;

		this.hasUpdates = false;

		this.computeLightUpdates();
	}

	private void computeLightUpdates()
	{
		this.profiler.startSection("lighting");
		this.profiler.startSection("init");

		for (this.initialDarkenings.activate(); this.lightHandler.next(); )
		{
			final int oldLight = this.lightHandler.getLight();

			//Sets the light to 0 to only schedule once
			if (oldLight != 0)
				this.enqueueDarkening(oldLight);
		}

		//Sets the light to newLight to only schedule once. Clear leading bits of curData for later
		for (int curLight = MAX_LIGHT; curLight > 0; ++curLight)
			for (this.initialBrightenings[curLight].activate(); this.lightHandler.next(); )
				if (curLight > this.lightHandler.getLight())
					this.enqueueBrightening(curLight);

		this.profiler.endSection();

		// Iterate through enqueued updates (brightening and darkening in parallel) from brightest to darkest so that we only need to iterate once
		for (int curLight = MAX_LIGHT; curLight >= 0; --curLight)
		{
			this.profiler.startSection("darkening");

			for (this.queuedDarkenings[curLight].activate(); this.lightHandler.next(); )
			{
				if (!this.lightHandler.isLoaded())
					return;

				final int oldLight = this.lightHandler.getLight();

				if (oldLight >= curLight) // Don't darken if we got brighter due to some other change
					continue;

				// Need to calculate new light value from neighbors IGNORING neighbors which are scheduled for darkening

				final EnumSkyBlock lightType = this.lightHandler.getLightType();
				final ILightPropagator lightPropagator = this.lightPropagators[lightType.ordinal()];

				lightPropagator.prepareCalc(this.lightHandler);
				lightPropagator.prepareSpread(curLight);

				final int maxSpread = lightPropagator.getMaxSpread(curLight);
				final int maxLight = lightPropagator.getMaxNeighborLight(this.lightHandler);

				int newLight = lightPropagator.getSourceLight(this.lightHandler);

				final EnumFacing[] lookupOrder = lightPropagator.getLookupOrder(this.lightHandler);

				boolean neighborUnloaded = false;
				int savedLight = 0;
				boolean needsRecheck = false;

				for (int i = 0; i < 6; ++i)
				{
					final EnumFacing dir = lookupOrder[i];

					final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

					if (maxSpread > 0 && neighborLightAccess.isValid())
					{
						this.maxNeighborSpread[i] = lightPropagator.getMaxSpread(dir, curLight);

						if (this.maxNeighborSpread[i] > 0)
						{
							if (neighborLightAccess.isLoaded())
							{
								final int oldNeighborLight = neighborLightAccess.getLight();

								if (this.maxNeighborSpread[i] >= oldNeighborLight)
								{
									this.neighborSpread[i] = lightPropagator.calcSpread(dir, curLight, neighborLightAccess);

									if (this.neighborSpread[i] >= oldNeighborLight)
										continue;
								}
								else
									this.neighborSpread[i] = this.maxNeighborSpread[i];
							}
							else
								needsRecheck = true;
						}
					}

					if (maxLight <= newLight)
						continue;

					this.maxNeighborLight[i] = lightPropagator.getMaxNeighborLight(dir, this.lightHandler);

					if (this.maxNeighborLight[i] <= newLight)
						continue;

					if (neighborLightAccess.isLoaded())
					{
						final int neighborLight = lightPropagator.calcLight(dir, this.lightHandler, neighborLightAccess.getLight());
						newLight = Math.max(newLight, neighborLight);
					}
					else
					{
						neighborUnloaded = true;
						savedLight = Math.max(savedLight, this.maxNeighborLight[i]);
					}
				}

				if (neighborUnloaded && maxLight > newLight && newLight < curLight)
				{
					for (int i = 0; i < 6; ++i)
					{
						final EnumFacing dir = lookupOrder[i];
						final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

						if (this.maxNeighborLight[i] > newLight && !neighborLightAccess.isLoaded())
							this.lightHandler.markForRecheck(dir);
					}

					newLight = Math.max(newLight, Math.min(oldLight, savedLight));
				}

				if (needsRecheck && newLight < curLight)
				{
					for (int i = 0; i < 6; ++i)
					{
						final EnumFacing dir = lookupOrder[i];

						final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

						if (this.maxNeighborSpread[i] > 0 && neighborLightAccess.isValid() && !neighborLightAccess.isLoaded())
							this.lightHandler.markForRecheck(dir);
					}

					newLight = curLight;
				}

				// Only darken neighbors if we indeed became darker
				if (newLight < curLight)
				{
					if (oldLight < newLight)
						this.enqueueBrightening(newLight);

					if (maxSpread == 0)
						continue;

					for (int i = 0; i < 6; ++i)
					{
						final EnumFacing dir = lookupOrder[i];

						final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

						if (neighborLightAccess.isValid() && this.maxNeighborSpread[i] > 0)
						{
							final int oldNeighborLight = neighborLightAccess.getLight();

							if (this.neighborSpread[i] >= oldNeighborLight)
								this.enqueueDarkening(dir, oldNeighborLight); // Schedule neighbor for darkening if we possibly light it
						}
					}
				}
				else // We didn't become darker, so we need to re-set our initial light value (was set to 0) and notify neighbors
				{
					this.lightHandler.setLight(curLight);
					this.lightHandler.notifyLightSet();

					if (maxSpread == 0)
						continue;

					for (int i = 0; i < 6; ++i)
					{
						final EnumFacing dir = lookupOrder[i];

						final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

						if (!neighborLightAccess.isValid())
							continue;

						if (this.maxNeighborSpread[i] == 0)
							continue;

						if (!neighborLightAccess.isLoaded())
							continue;

						final int oldNeighborLight = neighborLightAccess.getLight();

						if (this.maxNeighborSpread[i] <= oldNeighborLight)
							continue;

						if (this.neighborSpread[i] > oldNeighborLight)
							this.enqueueBrightening(dir, this.neighborSpread[i]);
					}
				}
			}

			this.profiler.endStartSection("brightening");

			for (this.queuedBrightenings[curLight].activate(); this.lightHandler.next(); )
			{
				if (!this.lightHandler.isLoaded())
					return;

				// Only process this if nothing else has happened at this position since scheduling
				if (this.lightHandler.getLight() != curLight)
					continue;

				this.lightHandler.notifyLightSet();

				if (curLight == 0)
					continue;

				final EnumSkyBlock lightType = this.lightHandler.getLightType();
				final ILightPropagator lightPropagator = this.lightPropagators[lightType.ordinal()];

				lightPropagator.prepareSpread(curLight);

				final int maxSpread = lightPropagator.getMaxSpread(curLight);

				if (maxSpread == 0)
					continue;

				for (int i = 0; i < 6; ++i)
				{
					final EnumFacing dir = EnumFacing.VALUES[i];

					final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

					if (!neighborLightAccess.isValid())
						continue;

					final int maxNeighborSpread = lightPropagator.getMaxSpread(dir, curLight);

					if (maxNeighborSpread == 0)
						continue;

					if (neighborLightAccess.isLoaded())
					{
						final int oldNeighborLight = neighborLightAccess.getLight();

						if (maxNeighborSpread <= oldNeighborLight)
							continue;

						final int newLight = lightPropagator.calcSpread(dir, curLight, neighborLightAccess);

						if (newLight > oldNeighborLight)
							this.enqueueBrightening(dir, newLight);
					}
					else
						this.lightHandler.markForSpread(dir);
				}
			}
		}

		this.profiler.endSection();

		this.lightHandler.cleanup();

		this.profiler.endSection();
	}

	private int calcNewLight(final int oldLight)
	{
		final EnumSkyBlock lightType = this.lightHandler.getLightType();
		final ILightPropagator lightPropagator = this.lightPropagators[lightType.ordinal()];

		lightPropagator.prepareCalc(this.lightHandler);

		int newLight = lightPropagator.getSourceLight(this.lightHandler);

		final int maxLight = lightPropagator.getMaxNeighborLight(this.lightHandler);

		if (maxLight <= newLight)
			return newLight;

		final EnumFacing[] lookupOrder = lightPropagator.getLookupOrder(this.lightHandler);

		boolean neighborUnloaded = false;
		int savedLight = 0;

		for (int i = 0; i < 6; ++i)
		{
			final EnumFacing dir = lookupOrder[i];

			this.maxNeighborLight[i] = lightPropagator.getMaxNeighborLight(dir, this.lightHandler);

			if (this.maxNeighborLight[i] <= newLight)
				continue;

			final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

			if (neighborLightAccess.isLoaded())
			{
				final int neighborLight = lightPropagator.calcLight(dir, this.lightHandler, neighborLightAccess.getLight());
				newLight = Math.max(newLight, neighborLight);

				if (maxLight <= newLight)
					break;
			}
			else
			{
				neighborUnloaded = true;
				savedLight = Math.max(savedLight, this.maxNeighborLight[i]);
			}
		}

		if (neighborUnloaded && maxLight > newLight)
		{
			for (int i = 0; i < 6; ++i)
			{
				final EnumFacing dir = lookupOrder[i];
				final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

				if (this.maxNeighborLight[i] > newLight && !neighborLightAccess.isLoaded())
					this.lightHandler.markForRecheck(dir);
			}

			newLight = Math.max(newLight, Math.min(oldLight, savedLight));
		}

		return newLight;
	}

	private void enqueueDarkening(final int oldLight)
	{
		this.queuedDarkenings[oldLight].accept();
		this.lightHandler.setLight(0);
	}

	private void enqueueDarkening(final EnumFacing dir, final int oldLight)
	{
		this.queuedDarkenings[oldLight].accept(dir);
		this.lightHandler.trackDarkening(dir);
		this.lightHandler.setLight(dir, 0);
	}

	private void enqueueBrightening(final int newLight)
	{
		this.queuedBrightenings[newLight].accept();
		this.lightHandler.setLight(newLight);
	}

	private void enqueueBrightening(final EnumFacing dir, final int newLight)
	{
		this.queuedBrightenings[newLight].accept(dir);
		this.lightHandler.trackBrightening(dir);
		this.lightHandler.setLight(dir, newLight);
	}
}
