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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.profiler.Profiler;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import ocd.lightpp.api.lighting.ILightAccess;
import ocd.lightpp.api.lighting.ILightHandler;
import ocd.lightpp.api.lighting.ILightHandler.LightUpdateQueue;
import ocd.lightpp.api.lighting.ILightPropagator;

public class LightingEngine
{
    private static final EnumSkyBlock[] LIGHT_TYPE_VALUES = EnumSkyBlock.values();

    private static final int MAX_SCHEDULED_COUNT = 1 << 22;

    public static final int MAX_LIGHT = 15;

    private static final Logger logger = LogManager.getLogger();

    private final World world;
    private final Profiler profiler;

    private final ILightHandler lightHandler;
    private final ILightPropagator lightPropagator;

    private final LightUpdateQueue[] queuedLightUpdates = new LightUpdateQueue[LIGHT_TYPE_VALUES.length];

    private final LightUpdateQueue[] queuedDarkenings = new LightUpdateQueue[MAX_LIGHT + 1];
    private final LightUpdateQueue[] queuedBrightenings = new LightUpdateQueue[MAX_LIGHT + 1];

    private final LightUpdateQueue initialDarkenings;
    private final LightUpdateQueue[] initialBrightenings = new LightUpdateQueue[MAX_LIGHT + 1];

    private boolean hasUpdates;

    private int sourceLight;
    private EnumFacing[] lookupOrder;
    private final int[] neighborLight = new int[6];
    private final int[] maxNeighborLight = new int[6];

    public LightingEngine(final World world, final ILightHandler lightHandler, final ILightPropagator lightPropagator)
    {
        this.world = world;
        this.profiler = world.profiler;

        this.lightHandler = lightHandler;
        this.lightPropagator = lightPropagator;

        for (int i = 0; i < LIGHT_TYPE_VALUES.length; ++i)
        {
            this.queuedLightUpdates[i] = lightHandler.createQueue();
        }

        for (int i = 0; i < this.queuedDarkenings.length; ++i)
        {
            this.queuedDarkenings[i] = lightHandler.createQueue();
        }

        for (int i = 0; i < this.queuedBrightenings.length; ++i)
        {
            this.queuedBrightenings[i] = lightHandler.createQueue();
        }

        this.initialDarkenings = lightHandler.createQueue();

        for (int i = 0; i < this.initialBrightenings.length; ++i)
        {
            this.initialBrightenings[i] = lightHandler.createQueue();
        }
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
        {
            this.procLightUpdates(lightType);
        }
    }

    /**
     * Calls {@link #procLightUpdates(EnumSkyBlock)} for both light types
     */
    public void procLightUpdates()
    {
        this.procLightUpdates(EnumSkyBlock.SKY);
        this.procLightUpdates(EnumSkyBlock.BLOCK);
    }

    private void prepare(final EnumSkyBlock lightType)
    {
        final LightUpdateQueue queue = this.queuedLightUpdates[lightType.ordinal()];

        if (queue.isEmpty())
            return;

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

    /**
     * Processes light updates of the given light type
     */
    public void procLightUpdates(final EnumSkyBlock lightType)
    {
        /*//renderer accesses world unsynchronized, don't modify anything in that case
        if (this.world.isRemote && !FMLCommonHandler.instance().getMinecraftThread().isCallingFromMinecraftThread())
        {
            return;
        }*/

        this.prepare(lightType);

        if (!this.hasUpdates)
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

            if (oldLight != 0)
            {
                //Sets the light to 0 to only schedule once
                this.enqueueDarkening(oldLight);
            }
        }

        for (int curLight = MAX_LIGHT; curLight > 0; ++curLight)
        {
            for (this.initialBrightenings[curLight].activate(); this.lightHandler.next(); )
            {
                if (curLight > this.lightHandler.getLight())
                {
                    //Sets the light to newLight to only schedule once. Clear leading bits of curData for later
                    this.enqueueBrightening(curLight);
                }
            }
        }

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
                    final boolean recalcLight = newLight > this.sourceLight; // Need to calculate new light value from neighbors IGNORING neighbors which are scheduled for darkening

                    newLight = this.sourceLight;

                    final EnumSkyBlock lightType = this.lightHandler.getLightType();

                    final boolean canSpread = this.lightPropagator.canSpread(lightType, curLight);

                    for (int i = 0; i < 6; ++i)
                    {
                        final EnumFacing dir = this.lookupOrder[i];

                        if (canSpread && this.lightPropagator.canSpread(lightType, dir, curLight))
                        {
                            final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

                            if (neighborLightAccess.isValid() && !neighborLightAccess.isLoaded())
                                this.lightHandler.markForSpread(dir);
                            else
                            {
                                final int oldLight = neighborLightAccess.getLight();

                                if (this.lightPropagator.calcSpread(lightType, dir, curLight, neighborLightAccess) > oldLight)
                                {
                                    this.enqueueDarkening(dir, oldLight); // Schedule neighbor for darkening if we possibly light it
                                    continue;
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

                final int oldLight = this.lightHandler.getLight();

                if (oldLight == curLight) // Only process this if nothing else has happened at this position since scheduling
                {
                    this.world.notifyLightSet(this.lightHandler.getPos());

                    final EnumSkyBlock lightType = this.lightHandler.getLightType();

                    this.lightPropagator.prepareSpread(lightType, curLight);

                    if (!this.lightPropagator.canSpread(lightType, curLight))
                        continue;

                    for (int i = 0; i < 6; ++i)
                    {
                        final EnumFacing dir = EnumFacing.VALUES[i];

                        if (!this.lightPropagator.canSpread(lightType, dir, curLight))
                            continue;

                        final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

                        if (neighborLightAccess.isValid() && !neighborLightAccess.isLoaded())
                            this.lightHandler.markForSpread(dir);
                        else
                        {
                            final int newLight = this.lightPropagator.calcSpread(lightType, dir, curLight, neighborLightAccess);

                            if (newLight > neighborLightAccess.getLight())
                                this.enqueueBrightening(dir, newLight);
                        }
                    }
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

        this.lightPropagator.prepareCalc(lightType, this.lightHandler);

        int newLight = this.sourceLight = this.lightPropagator.getSourceLight(lightType, this.lightHandler);

        final int maxNeighborLight = this.lightPropagator.getMaxNeighborLight(lightType, this.lightHandler);

        if (maxNeighborLight <= newLight)
            return newLight;

        this.lookupOrder = this.lightPropagator.getLookupOrder(lightType, this.lightHandler);

        boolean neighborUnloaded = false;

        for (int i = 0; i < 6; ++i)
        {
            final EnumFacing dir = this.lookupOrder[i];

            this.maxNeighborLight[i] = this.lightPropagator.getMaxNeighborLight(lightType, dir, this.lightHandler);

            if (this.maxNeighborLight[i] <= newLight)
                continue;

            final ILightAccess neighborLightAccess = this.lightHandler.getNeighborLightAccess(dir);

            if (neighborLightAccess.isLoaded())
            {
                this.neighborLight[i] = this.lightPropagator.calcLight(lightType, dir.getOpposite(), this.lightHandler, neighborLightAccess.getLight());
                newLight = Math.max(newLight, this.neighborLight[i]);
            }
            else
                neighborUnloaded = true;
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
