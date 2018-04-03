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
 */

package ocd.lightpp.api.lighting;

import net.minecraft.util.EnumFacing;

public interface ILightPropagator<LD, LCD extends ILightCollectionDescriptor<LD>, MI, SI, WI, V>
{
	default void prepareSpread(final LD desc, final int light, final ILightAccess.NeighborAware<? extends SI, ? extends WI> lightAccess)
	{
	}

	default void prepareSpread(final LD desc, final EnumFacing dir, final int light, final ILightAccess<? extends SI, ? extends WI> lightAccess, final ILightAccess<? extends SI, ? extends WI> neighborLightAccess)
	{
	}

	default void cleanup()
	{
	}

	void calcSourceLight(LCD desc, ILightAccess.VirtuallySourced<? extends SI, ? extends WI, ? extends V> lightAccess, MI lightMap);

	boolean calcLight(LCD desc, ILightAccess.VirtuallySourced.NeighborAware<? extends SI, ? extends WI, ? extends V> lightAccess, MI lightMap);

	//TODO: Change interface to return int (valid <-> -1)?
	boolean calcLight(LD desc, ILightAccess.VirtuallySourced.NeighborAware<? extends SI, ? extends WI, ? extends V> lightAccess, MI lightMap);

	default boolean canSpread(final LD desc, final int light, final ILightAccess.NeighborAware<? extends SI, ? extends WI> lightAccess)
	{
		return true;
	}

	default boolean canSpread(final LD desc, final EnumFacing dir, final int light, final ILightAccess.NeighborAware<? extends SI, ? extends WI> lightAccess)
	{
		return true;
	}

	void calcSpread(LD desc, EnumFacing dir, int light, ILightAccess<? extends SI, ? extends WI> lightAccess, ILightAccess<? extends SI, ? extends WI> neighborLightAccess, MI lightMap);

	default void calcSpread(final LD desc, final int light, final ILightAccess.NeighborAware<? extends SI, ? extends WI> lightAccess, final MI lightMap)
	{
	}
}
