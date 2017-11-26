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

package ocd.lightpp.api.lighting;

import net.minecraft.util.EnumFacing;
import net.minecraft.world.EnumSkyBlock;

public interface ILightPropagator
{
    void prepareCalc(EnumSkyBlock lightType, ILightAccess lightAccess);

    void prepareSpread(EnumSkyBlock lightType, int light);

    int getSourceLight(EnumSkyBlock lightType, ILightAccess lightAccess);

    int getMaxNeighborLight(EnumSkyBlock lightType, ILightAccess lightAccess);

    int getMaxNeighborLight(EnumSkyBlock lightType, EnumFacing dir, ILightAccess lightAccess);

    EnumFacing[] getLookupOrder(EnumSkyBlock lightType, ILightAccess lightAccess);

    boolean canSpread(EnumSkyBlock lightType, int light);

    boolean canSpread(EnumSkyBlock lightType, EnumFacing dir, int light);

    int calcLight(EnumSkyBlock lightType, EnumFacing dir, ILightAccess lightAccess, int neighborLight);

    int calcSpread(EnumSkyBlock lightType, EnumFacing dir, int light, ILightAccess neighborLightAccess);
}
