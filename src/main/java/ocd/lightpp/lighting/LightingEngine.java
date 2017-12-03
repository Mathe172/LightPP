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
	public static final EnumSkyBlock[] LIGHT_TYPE_VALUES = EnumSkyBlock.values();

	private static final int MAX_SCHEDULED_COUNT = 1 << 22;

	public static final int MAX_LIGHT = 15;

	private final World world;
	private final Profiler profiler;

	private final ILightHandler lightHandler;
	private final ILightPropagator[] lightPropagators = new ILightPropagator[LIGHT_TYPE_VALUES.length];

	private final LightUpdateQueue queuedLightUpdates;
	private final LightUpdateQueue queuedSpreadings;

	private final LightUpdateQueue[] queuedDarkenings = new LightUpdateQueue[MAX_LIGHT];
	private final LightUpdateQueue[] queuedBrightenings = new LightUpdateQueue[MAX_LIGHT];

	private final LightUpdateQueue initialDarkenings;
	private final LightUpdateQueue[] initialBrightenings = new LightUpdateQueue[MAX_LIGHT];

	private boolean hasUpdates;

	private final int[] neighborSpread = new int[6];
	private final int[] oldNeighborLight = new int[6];

	public LightingEngine(final World world)
	{
		this.world = world;
		this.profiler = world.profiler;

		final ILightManager lightManager = (ILightManager) world;

		this.lightHandler = lightManager.createLightHandler();

		for (final EnumSkyBlock lightType : LIGHT_TYPE_VALUES)
			this.lightPropagators[lightType.ordinal()] = lightManager.create(lightType);

		this.queuedLightUpdates = this.lightHandler.createQueue();
		this.queuedSpreadings = this.lightHandler.createQueue();

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
		this.queuedLightUpdates.accept(lightType, pos, null);

		// Make sure there are not too many queued light updates
		if (this.queuedLightUpdates.size() >= MAX_SCHEDULED_COUNT)
			this.procLightUpdates();
	}

	public void procLightUpdates()
	{
		// Renderer accesses world unsynchronized, don't modify anything in that case
		boolean ensuredNotRenderingThread = false;

		if (!this.queuedLightUpdates.isEmpty())
		{
			if (!IThreadGuard.isNotRenderingThread(this.world))
				return;

			ensuredNotRenderingThread = true;

			this.profiler.startSection("lighting");
			this.profiler.startSection("checking");

			// Process the queued updates and enqueue them for further processing
			for (this.queuedLightUpdates.activate(); this.lightHandler.next(); )
			{
				if (!this.lightHandler.isLoaded())
					continue;

				final EnumSkyBlock lightType = this.lightHandler.getLightType();

				final int oldLight = this.lightHandler.getLight(lightType);
				final int newLight = this.calcNewLight(oldLight);

				if (oldLight < newLight)
				{
					// Don't enqueue directly for brightening in order to avoid duplicate scheduling
					this.initialBrightenings[newLight - 1].accept();
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
			final int oldLight = this.lightHandler.getLight(this.lightHandler.getLightType());

			//Sets the light to 0 to only schedule once
			if (oldLight > 0)
				this.enqueueDarkening(oldLight);
		}

		//Sets the light to newLight to only schedule once. Clear leading bits of curData for later
		for (int curLight = MAX_LIGHT; curLight > 0; --curLight)
			for (this.initialBrightenings[curLight - 1].activate(); this.lightHandler.next(); )
				if (curLight > this.lightHandler.getLight(this.lightHandler.getLightType()))
					this.enqueueBrightening(curLight);

		this.profiler.endSection();

		// Iterate through enqueued updates (brightening and darkening in parallel) from brightest to darkest so that we only need to iterate once
		for (int curLight = MAX_LIGHT; curLight > 0; --curLight)
		{
			this.profiler.startSection("darkening");

			for (this.queuedDarkenings[curLight - 1].activate(); this.lightHandler.next(); )
			{
				final int oldLight = this.lightHandler.getLight(this.lightHandler.getLightType());

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

				int savedLight = 0;
				boolean needsRecheck = false;

				final int[] neighborSpread = this.neighborSpread;
				final int[] oldNeighborLight = this.oldNeighborLight;

				for (int i = 0; i < 6; ++i)
				{
					final EnumFacing dir = lookupOrder[i];

					final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

					final boolean isNeighborLoaded = neighborLightAccess.isLoaded();

					neighborSpread[i] = 0;
					oldNeighborLight[i] = 0;

					if (maxSpread > 0 && neighborLightAccess.isValid())
					{
						final int maxNeighborSpread = lightPropagator.getMaxSpread(dir, curLight);

						if (maxNeighborSpread > 0)
						{
							if (isNeighborLoaded)
							{
								oldNeighborLight[i] = neighborLightAccess.getLight(lightType);

								if (maxNeighborSpread >= oldNeighborLight[i])
								{
									neighborSpread[i] = lightPropagator.calcSpread(dir, curLight, neighborLightAccess);

									if (neighborSpread[i] >= oldNeighborLight[i])
										continue;
								}
							}
							else
								needsRecheck = true;
						}
					}

					if (maxLight <= newLight)
						continue;

					final int maxNeighborLight = lightPropagator.getMaxNeighborLight(dir, this.lightHandler);

					if (maxNeighborLight <= newLight)
						continue;

					if (isNeighborLoaded)
					{
						final int neighborLight = lightPropagator.calcLight(dir, this.lightHandler, neighborLightAccess.getLight(lightType));
						newLight = Math.max(newLight, neighborLight);
					}
					else
						savedLight = Math.max(savedLight, maxNeighborLight);
				}

				savedLight = Math.min(curLight, savedLight);

				if (savedLight > newLight)
				{
					this.lightHandler.markForRecheck();
					newLight = needsRecheck ? curLight : savedLight;
				}

				// Only darken neighbors if we indeed became darker
				if (newLight < curLight)
				{
					if (oldLight < newLight)
						this.enqueueBrightening(newLight);
					else if (newLight == 0)
						this.lightHandler.notifyLightSet();

					this.lightHandler.trackDarkening();

					if (maxSpread == 0)
						continue;

					for (int i = 0; i < 6; ++i)
					{
						if (oldNeighborLight[i] > 0 && neighborSpread[i] >= oldNeighborLight[i])
							this.enqueueDarkening(lookupOrder[i], oldNeighborLight[i]); // Schedule neighbor for darkening if we possibly light it
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
						if (neighborSpread[i] > oldNeighborLight[i])
							this.enqueueBrightening(lookupOrder[i], neighborSpread[i]);
					}
				}
			}

			this.profiler.endStartSection("brightening");

			for (this.queuedBrightenings[curLight - 1].activate(); this.lightHandler.next(); )
			{
				final EnumSkyBlock lightType = this.lightHandler.getLightType();

				// Only process this if nothing else has happened at this position since scheduling
				if (this.lightHandler.getLight(lightType) != curLight)
					continue;

				this.lightHandler.notifyLightSet();
				this.lightHandler.trackBrightening();

				final ILightPropagator lightPropagator = this.lightPropagators[lightType.ordinal()];

				lightPropagator.prepareSpread(curLight);

				final int maxSpread = lightPropagator.getMaxSpread(curLight);

				if (maxSpread == 0)
					continue;

				for (final EnumFacing dir : EnumFacing.VALUES)
				{
					final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

					if (!neighborLightAccess.isValid())
						continue;

					final int maxNeighborSpread = lightPropagator.getMaxSpread(dir, curLight);

					if (maxNeighborSpread == 0)
						continue;

					if (neighborLightAccess.isLoaded())
					{
						final int oldNeighborLight = neighborLightAccess.getLight(lightType);

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

			this.profiler.endSection();
		}

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

		int savedLight = 0;

		for (final EnumFacing dir : lookupOrder)
		{
			final int maxNeighborLight = lightPropagator.getMaxNeighborLight(dir, this.lightHandler);

			if (maxNeighborLight <= newLight)
				continue;

			final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

			if (neighborLightAccess.isLoaded())
			{
				final int neighborLight = lightPropagator.calcLight(dir, this.lightHandler, neighborLightAccess.getLight(lightType));
				newLight = Math.max(newLight, neighborLight);

				if (maxLight <= newLight)
					break;
			}
			else
				savedLight = Math.max(savedLight, maxNeighborLight);
		}

		if (savedLight > newLight)
		{
			this.lightHandler.markForRecheck();
			newLight = Math.max(newLight, Math.min(oldLight, savedLight));
		}

		return newLight;
	}

	private void enqueueDarkening(final int oldLight)
	{
		this.queuedDarkenings[oldLight - 1].accept();
		this.lightHandler.setLight(0);
	}

	private void enqueueDarkening(final EnumFacing dir, final int oldLight)
	{
		this.queuedDarkenings[oldLight - 1].accept(dir);
		this.lightHandler.setLight(dir, 0);
	}

	private void enqueueBrightening(final int newLight)
	{
		this.queuedBrightenings[newLight - 1].accept();
		this.lightHandler.setLight(newLight);
	}

	private void enqueueBrightening(final EnumFacing dir, final int newLight)
	{
		this.queuedBrightenings[newLight - 1].accept(dir);
		this.lightHandler.setLight(dir, newLight);
	}
}
