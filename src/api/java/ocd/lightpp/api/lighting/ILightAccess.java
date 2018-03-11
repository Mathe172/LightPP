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

import javax.annotation.Nullable;

import net.minecraft.util.EnumFacing;
import ocd.lightpp.api.lighting.ILightMap.ILightIterator;

public interface ILightAccess<LI, WI>
{
	boolean isValid();

	boolean isLoaded();

	LI getLightData();

	WI getWorldInterface();

	interface Extended<D, LI, WI> extends ILightAccess<LI, WI>
	{
		ILightIterator<D> getLightIterator();

		int getLight(D desc);

		void setLight(D desc, int val);

		void notifyLightSet(D desc);
	}

	interface NeighborAware<LI, WI> extends ILightAccess<LI, WI>
	{
		ILightAccess<LI, WI> getNeighbor(EnumFacing dir);

		interface Extended<D, LI, WI> extends ILightAccess.NeighborAware<LI, WI>, ILightAccess.Extended<D, LI, WI>
		{
			@Override
			ILightAccess.Extended<D, LI, WI> getNeighbor(EnumFacing dir);
		}
	}

	interface VirtuallySourced<LI, WI, V> extends ILightAccess<LI, WI>
	{
		interface NeighborAware<LI, WI, V> extends ILightAccess.NeighborAware<LI, WI>, VirtuallySourced<LI, WI, V>
		{
			interface Extended<D, LI, WI, V> extends ILightAccess.VirtuallySourced.NeighborAware<LI, WI, V>, ILightAccess.NeighborAware.Extended<D, LI, WI>
			{
			}
		}

		@Nullable
		V getVirtualSources();
	}
}
