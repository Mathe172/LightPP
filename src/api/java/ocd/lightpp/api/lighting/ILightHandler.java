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

public interface ILightHandler<LD, LCD extends ILightCollectionDescriptor<LD>, LI, WI, V>
{
	ILightUpdateQueue<LD, LI, WI, V> createUpdateQueue();

	ILightCheckQueue<LD, LCD, LI, WI, V> createCheckQueue();

	ILightSpreadQueue<LD, LI, WI, V> createSpreadQueue();

	ILightInitQueue<LD, LCD, LI, WI, V> createInitQueue();

	default void prepare()
	{
	}

	default void cleanup()
	{
	}

	interface ILightUpdateQueue<LD, LI, WI, V>
	{
		ILightUpdateQueueIterator<LD, LI, WI, V> activate();

		/**
		 * Target can be assumed to be valid and loaded
		 */
		void enqueueDarkening(LD desc, int oldLight);

		/**
		 * Target can be assumed to be valid and loaded
		 */
		void enqueueDarkening(LD desc, EnumFacing dir, int oldLight);

		/**
		 * Target can be assumed to be valid and loaded
		 */
		void enqueueBrightening(LD desc, int newLight);

		/**
		 * Target can be assumed to be valid and loaded
		 */
		void enqueueBrightening(LD desc, EnumFacing dir, int newLight);
	}

	interface ILightCheckQueue<LD, LCD extends ILightCollectionDescriptor<LD>, LI, WI, V>
	{
		ILightCheckQueueIterator<LD, LCD, LI, WI, V> activate();

		/**
		 * @return Whether target is valid
		 */
		boolean enqueueCheck(BlockPos pos, @Nullable EnumFacing dir);

		/**
		 * @return Whether target is valid
		 */
		boolean enqueueCheck(LCD desc, BlockPos pos, @Nullable EnumFacing dir);
	}

	interface ILightSpreadQueue<LD, LI, WI, V>
	{
		ILightSpreadQueueIterator<LD, LI, WI, V> activate();

		/**
		 * @return Whether target is valid
		 */
		boolean enqueueSpread(LD desc, BlockPos pos, EnumFacing dir);
	}

	interface ILightInitQueue<LD, LCD extends ILightCollectionDescriptor<LD>, LI, WI, V>
	{
		ILightInitQueueIterator<LD, LCD, LI, WI, V> activate();

		/**
		 * @return Whether target is valid
		 */
		boolean enqueueInit(BlockPos pos);

		/**
		 * @return Whether target is valid
		 */
		boolean enqueueInit(LCD desc, BlockPos pos);
	}

	interface ILightQueueIterator<LD, LI, WI, V>
	{
		/**
		 * The traversal order is unspecified, in particular it does not need to match the insertion order
		 */
		boolean next();

		ILightAccess.VirtuallySourced.NeighborAware.Extended<LD, LI, WI, V> getLightAccess();
	}

	interface ILightInitQueueIterator<LD, LCD extends ILightCollectionDescriptor<LD>, LI, WI, V> extends ILightQueueIterator<LD, LI, WI, V>
	{
		LCD getDescriptor();
	}

	interface ILightCheckQueueIterator<LD, LCD extends ILightCollectionDescriptor<LD>, LI, WI, V> extends ILightQueueIterator<LD, LI, WI, V>
	{
		@Nullable
		EnumFacing getDir();

		LCD getDescriptor();

		void markForRecheck();

		/**
		 * Mark the current position for rechecking, but store this in the neighbor in direction <code>dir</code>
		 */
		void markForRecheck(EnumFacing dir);
	}

	interface ILightSpreadQueueIterator<LD, LI, WI, V> extends ILightQueueIterator<LD, LI, WI, V>
	{
		EnumFacing getDir();

		LD getDescriptor();

		void markForSpread(EnumFacing dir);
	}

	interface ILightUpdateQueueIterator<LD, LI, WI, V> extends ILightQueueIterator<LD, LI, WI, V>
	{
		LD getDescriptor();

		void markForRecheck();

		void markForSpread(EnumFacing dir);
	}
}
