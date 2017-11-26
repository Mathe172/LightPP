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

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.EnumSkyBlock;
import ocd.lightpp.api.lighting.ILightAccess;
import ocd.lightpp.api.lighting.ILightPropagator;

public class VanillaLightPropagator implements ILightPropagator
{
    private int sourceLight;
    private int opacity;
    private int maxLight;

    private static final EnumFacing[] lookupOrder =
        new EnumFacing[] {EnumFacing.UP, EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST};

    @Override
    public void prepareCalc(final EnumSkyBlock lightType, final ILightAccess lightAccess)
    {
        final IBlockState state = lightAccess.getBlockState();

        if (lightType == EnumSkyBlock.BLOCK)
        {
            this.sourceLight = state.getLightValue(lightAccess.getWorld(), lightAccess.getPos());

            if (this.sourceLight >= LightingEngine.MAX_LIGHT)
            {
                this.opacity = 1;
                this.maxLight = LightingEngine.MAX_LIGHT - 1;
                return;
            }
        }
        else
            this.sourceLight = 0;

        this.opacity = state.getLightOpacity(lightAccess.getWorld(), lightAccess.getPos());
        this.maxLight = LightingEngine.MAX_LIGHT - this.opacity;
    }

    @Override
    public void prepareSpread(final EnumSkyBlock lightType, final int light)
    {
    }

    @Override
    public int getSourceLight(final EnumSkyBlock lightType, final ILightAccess lightAccess)
    {
        return this.sourceLight;
    }

    @Override
    public int getMaxNeighborLight(final EnumSkyBlock lightType, final ILightAccess lightAccess)
    {
        return lightType == EnumSkyBlock.SKY ? LightingEngine.MAX_LIGHT : this.maxLight;
    }

    @Override
    public int getMaxNeighborLight(final EnumSkyBlock lightType, final EnumFacing dir, final ILightAccess lightAccess)
    {
        if (lightType == EnumSkyBlock.SKY && dir == EnumFacing.UP && this.opacity == 0)
            return LightingEngine.MAX_LIGHT;

        return this.maxLight;
    }

    @Override
    public EnumFacing[] getLookupOrder(final EnumSkyBlock lightType, final ILightAccess lightAccess)
    {
        return lookupOrder;
    }

    @Override
    public boolean canSpread(final EnumSkyBlock lightType, final int light)
    {
        return light > 1;
    }

    @Override
    public boolean canSpread(final EnumSkyBlock lightType, final EnumFacing dir, final int light)
    {
        return true;
    }

    @Override
    public int calcLight(final EnumSkyBlock lightType, final EnumFacing dir, final ILightAccess lightAccess, final int neighborLight)
    {
        if (lightType == EnumSkyBlock.SKY && dir == EnumFacing.UP && neighborLight == 15 && this.opacity == 0)
            return 15;
        else
            return neighborLight - this.opacity;
    }

    @Override
    public int calcSpread(final EnumSkyBlock lightType, final EnumFacing dir, final int light, final ILightAccess neighborLightAccess)
    {
        final int neighborOpac = neighborLightAccess.getBlockState().getLightOpacity(neighborLightAccess.getWorld(), neighborLightAccess.getPos());

        if (light == 15 && lightType == EnumSkyBlock.SKY && dir == EnumFacing.DOWN && neighborOpac == 0)
            return 15;
        else
            return light - neighborOpac;
    }
}
