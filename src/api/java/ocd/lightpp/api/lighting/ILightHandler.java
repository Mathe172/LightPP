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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import ocd.lightpp.api.util.Sized;

public interface ILightHandler extends ILightAccess
{
	LightUpdateQueue createQueue();

	/**
	 * The traversal order is unspecified, in particular it does not need to match the insertion order
	 */
	boolean next();

	void cleanup();

	EnumSkyBlock getLightType();

	ILightAccess getNeighborLightAccess(EnumFacing dir);

	void setLight(int val);

	void setLight(EnumFacing dir, int val);

	void notifyLightSet();

	void trackDarkening(EnumFacing dir);

	void trackBrightening(EnumFacing dir);

	void markForRecheck(EnumFacing dir);

	void markForSpread(EnumFacing dir);

	interface LightUpdateQueue extends Sized
	{
		void activate();

		void accept();

		void accept(EnumFacing dir);

		void accept(BlockPos pos, EnumSkyBlock lightType);
	}
}
