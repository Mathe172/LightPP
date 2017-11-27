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
	void prepareCalc(ILightAccess lightAccess);

	void prepareSpread(int light);

	int getSourceLight(ILightAccess lightAccess);

	int getMaxNeighborLight(ILightAccess lightAccess);

	int getMaxNeighborLight(EnumFacing dir, ILightAccess lightAccess);

	EnumFacing[] getLookupOrder(ILightAccess lightAccess);

	int getMaxSpread(int light);

	int getMaxSpread(EnumFacing dir, int light);

	int calcLight(EnumFacing dir, ILightAccess lightAccess, int neighborLight);

	int calcSpread(EnumFacing dir, int light, ILightAccess neighborLightAccess);

	interface Factory
	{
		ILightPropagator create(EnumSkyBlock lightType);
	}
}
