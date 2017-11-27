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

	private int sourceLight;
	private EnumFacing[] lookupOrder;
	private final int[] maxNeighborLight = new int[6];
	private final int[] neighborLight = new int[6];

	public LightingEngine(final World world, final ILightHandler lightHandler, final ILightPropagator.Factory lightPropagator)
	{
		this.world = world;
		this.profiler = world.profiler;

		this.lightHandler = lightHandler;

		for (final EnumSkyBlock lightType : LIGHT_TYPE_VALUES)
			this.lightPropagators[lightType.ordinal()] = lightPropagator.create(lightType);

		for (int i = 0; i < LIGHT_TYPE_VALUES.length; ++i)
			this.queuedLightUpdates[i] = lightHandler.createQueue();

		for (int i = 0; i < this.queuedDarkenings.length; ++i)
			this.queuedDarkenings[i] = lightHandler.createQueue();

		for (int i = 0; i < this.queuedBrightenings.length; ++i)
			this.queuedBrightenings[i] = lightHandler.createQueue();

		this.initialDarkenings = lightHandler.createQueue();

		for (int i = 0; i < this.initialBrightenings.length; ++i)
			this.initialBrightenings[i] = lightHandler.createQueue();
	}

	/**
	 * Schedules a light update for the specified light type and position to be processed later by {@link #procLightUpdates()}
	 */
	private void scheduleLightUpdate(final EnumSkyBlock lightType, final BlockPos pos)
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

		this.computeLightUpdates(-1);
	}

	private void computeLightUpdates(int maxSteps)
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

		//Iterate through enqueued updates (brightening and darkening in parallel) from brightest to darkest so that we only need to iterate once
		for (int curLight = MAX_LIGHT; curLight >= 0; --curLight)
		{
			this.profiler.startSection("darkening");

			for (this.queuedDarkenings[curLight].activate(); maxSteps != 0 && this.lightHandler.next(); --maxSteps)
			{
				if (!this.lightHandler.isLoaded())
					return;

				if (this.lightHandler.getLight() >= curLight) //don't darken if we got brighter due to some other change
					continue;

				int newLight = this.calcNewLight(curLight);

				//only darken neighbors if we indeed became darker
				if (newLight < curLight)
				{
					// Need to calculate new light value from neighbors IGNORING neighbors which are scheduled for darkening
					newLight = this.sourceLight;

					final EnumSkyBlock lightType = this.lightHandler.getLightType();
					final ILightPropagator lightPropagator = this.lightPropagators[lightType.ordinal()];

					final int maxSpread = lightPropagator.getMaxSpread(curLight);

					for (int i = 0; i < 6; ++i)
					{
						final EnumFacing dir = this.lookupOrder[i];

						if (maxSpread > 0)
						{
							final int maxNeighborSpread = lightPropagator.getMaxSpread(dir, curLight);

							if (maxNeighborSpread > 0)
							{
								final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

								if (neighborLightAccess.isValid() && !neighborLightAccess.isLoaded())
									this.lightHandler.markForDarkening(dir);
								else
								{
									final int oldLight = neighborLightAccess.getLight();

									if (maxNeighborSpread >= oldLight && lightPropagator.calcSpread(dir, curLight, neighborLightAccess) >= oldLight)
									{
										this.enqueueDarkening(dir, oldLight); // Schedule neighbor for darkening if we possibly light it
										continue;
									}
								}
							}
						}

						// Only use for new light calculation if not
						if (recalcLight && this.neighborLight[i] > newLight)
							newLight = this.maxNeighborLight[i];
					}
				}
				else // We didn't become darker, so we need to re-set our initial light value (was set to 0) and notify neighbors
					this.enqueueBrightening(curLight); // Do not spread to neighbors immediately to avoid scheduling multiple times
			}

			this.profiler.endStartSection("brightening");

			for (this.queuedBrightenings[curLight].activate(); maxSteps != 0 && this.lightHandler.next(); --maxSteps)
			{
				if (!this.lightHandler.isLoaded())
					return;

				if (this.lightHandler.getLight() != curLight) // Only process this if nothing else has happened at this position since scheduling
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

					final int maxNeighborSpread = lightPropagator.getMaxSpread(dir, curLight);

					if (maxNeighborSpread == 0)
						continue;

					final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

					if (neighborLightAccess.isValid() && !neighborLightAccess.isLoaded())
						this.lightHandler.markForBrightening(dir);
					else
					{
						final int oldNeighborLight = neighborLightAccess.getLight();

						if (maxNeighborSpread <= oldNeighborLight)
							continue;

						final int newLight = lightPropagator.calcSpread(dir, curLight, neighborLightAccess);

						if (newLight > oldNeighborLight)
							this.enqueueBrightening(dir, newLight);
					}
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

		int newLight = this.sourceLight = lightPropagator.getSourceLight(this.lightHandler);

		final int maxNeighborLight = lightPropagator.getMaxNeighborLight(this.lightHandler);

		if (maxNeighborLight <= newLight)
			return newLight;

		this.lookupOrder = lightPropagator.getLookupOrder(this.lightHandler);

		boolean neighborUnloaded = false;

		for (int i = 0; i < 6; ++i)
		{
			final EnumFacing dir = this.lookupOrder[i];

			if (maxNeighborLight <= newLight)
			{
				this.maxNeighborLight[i] = -1;
				this.neighborLight[i] = -1;
				continue;
			}

			this.maxNeighborLight[i] = lightPropagator.getMaxNeighborLight(dir, this.lightHandler);

			if (this.maxNeighborLight[i] <= newLight)
			{
				this.neighborLight[i] = -1;
				continue;
			}

			final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

			if (neighborLightAccess.isLoaded())
			{
				this.neighborLight[i] = lightPropagator.calcLight(dir, this.lightHandler, neighborLightAccess.getLight());
				newLight = Math.max(newLight, this.neighborLight[i]);
			}
			else
			{
				this.neighborLight[i] = -1;
				neighborUnloaded = true;
			}
		}

		if (neighborUnloaded && maxNeighborLight > newLight)
		{
			for (int i = 0; i < 6; ++i)
			{
				final EnumFacing dir = this.lookupOrder[i];

				final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

				if (this.maxNeighborLight[i] > newLight && neighborLightAccess.isValid() && !neighborLightAccess.isLoaded())
					this.lightHandler.markForRecheck(dir);
			}

			if (oldLight > newLight)
				newLight = oldLight;
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
