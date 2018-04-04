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

package ocd.lightpp.api.vanilla.world;

import net.minecraft.util.math.BlockPos;
import ocd.lightpp.api.lighting.ILightMap.ILightIterator;

public interface ILightProvider<LD, LI, WI>
{
	Positioned<LD, LI> bind(BlockPos pos);

	WI getWorldLightInterface(BlockPos pos);

	interface Cached<D, LI, WI, LC, WC> extends ILightProvider<D, LI, WI>
	{
		Positioned<D, LI> bind(BlockPos pos, LC container);

		WI getWorldLightInterface(BlockPos pos, WC container);

		LC createLightContainer();

		WC createWorldLightContainer();
	}

	interface Positioned<LD, LI>
	{
		int getLight(final LD desc);

		ILightIterator<LD> getLightIterator();

		LI getInterface();

		interface Writeable<LD, LI> extends Positioned<LD, LI>
		{
			void set(LD desc, int val);

			/**
			 * Called after the last {@link #set} of type LD in a bulk operation
			 */
			default void notifyLightSet(final LD desc)
			{
			}

			/**
			 * Called after the last {@link #set} in a bulk operation
			 */
			default void notifyLightSet()
			{
			}
		}
	}
}
