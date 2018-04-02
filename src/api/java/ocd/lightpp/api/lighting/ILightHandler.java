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
import net.minecraft.util.math.BlockPos;

public interface ILightHandler<D, LI, WI, V>
{
	ILightUpdateQueue<D, LI, WI, V> createUpdateQueue();

	ILightCheckQueue<D, LI, WI, V> createCheckQueue();

	ILightSpreadQueue<D, LI, WI, V> createSpreadQueue();

	ILightInitQueue<D, LI, WI, V> createInitQueue();

	default void prepare()
	{
	}

	default void cleanup()
	{
	}

	interface ILightUpdateQueue<D, LI, WI, V>
	{
		ILightUpdateQueueIterator<D, LI, WI, V> activate();

		/**
		 * Target can be assumed to be valid and loaded
		 */
		void enqueueDarkening(D desc, int oldLight);

		/**
		 * Target can be assumed to be valid and loaded
		 */
		void enqueueDarkening(D desc, EnumFacing dir, int oldLight);

		/**
		 * Target can be assumed to be valid and loaded
		 */
		void enqueueBrightening(D desc, int newLight);

		/**
		 * Target can be assumed to be valid and loaded
		 */
		void enqueueBrightening(D desc, EnumFacing dir, int newLight);
	}

	interface ILightCheckQueue<D, LI, WI, V>
	{
		ILightCheckQueueIterator<D, LI, WI, V> activate();

		/**
		 * @return Whether target is valid
		 */
		boolean enqueueCheck(@Nullable D desc, BlockPos pos, @Nullable EnumFacing dir);
	}

	interface ILightSpreadQueue<D, LI, WI, V>
	{
		ILightSpreadQueueIterator<D, LI, WI, V> activate();

		/**
		 * @return Whether target is valid
		 */
		boolean enqueueSpread(D desc, BlockPos pos, EnumFacing dir);
	}

	interface ILightInitQueue<D, LI, WI, V>
	{
		ILightQueueIterator<D, LI, WI, V> activate();

		/**
		 * @return Whether target is valid
		 */
		boolean enqueueInit(BlockPos pos);
	}

	interface ILightQueueIterator<D, LI, WI, V>
	{
		/**
		 * The traversal order is unspecified, in particular it does not need to match the insertion order
		 */
		boolean next();

		ILightAccess.VirtuallySourced.NeighborAware.Extended<D, LI, WI, V> getLightAccess();
	}

	interface ILightCheckQueueIterator<D, LI, WI, V> extends ILightQueueIterator<D, LI, WI, V>
	{
		@Nullable
		EnumFacing getDir();

		@Nullable
		D getDescriptor();

		void markForRecheck();

		/**
		 * Mark the current position for rechecking, but store this in the neighbor in direction <code>dir</code>
		 */
		void markForRecheck(EnumFacing dir);
	}

	interface ILightSpreadQueueIterator<D, LI, WI, V> extends ILightQueueIterator<D, LI, WI, V>
	{
		EnumFacing getDir();

		D getDescriptor();

		void markForSpread(EnumFacing dir);
	}

	interface ILightUpdateQueueIterator<D, LI, WI, V> extends ILightQueueIterator<D, LI, WI, V>
	{
		D getDescriptor();

		void markForRecheck();

		void markForSpread(EnumFacing dir);
	}
}
