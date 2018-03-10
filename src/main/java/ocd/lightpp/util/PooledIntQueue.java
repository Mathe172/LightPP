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

package ocd.lightpp.util;

//PooledIntQueue code
//Implement own queue with pooled segments to reduce allocation costs and reduce idle memory footprint

import ocd.lightpp.api.util.IEmpty;

public class PooledIntQueue implements IEmpty
{
	private static final int CACHED_QUEUE_SEGMENTS_COUNT = 1 << 12;
	private static final int QUEUE_SEGMENT_SIZE = 1 << 7;

	public static class SegmentPool
	{
		private PooledIntQueueSegment head;
		private int pooledCount = 0;

		private class PooledIntQueueSegment
		{
			private final int[] intArray = new int[QUEUE_SEGMENT_SIZE];
			private int index = 0;
			private PooledIntQueueSegment next;

			private void release()
			{
				if (SegmentPool.this.pooledCount >= CACHED_QUEUE_SEGMENTS_COUNT)
					return;

				this.index = 0;

				++SegmentPool.this.pooledCount;
				this.next = SegmentPool.this.head;
				SegmentPool.this.head = this;
			}

			private PooledIntQueueSegment add(final int val)
			{
				PooledIntQueueSegment ret = this;

				if (this.index == QUEUE_SEGMENT_SIZE)
					ret = this.next = SegmentPool.this.getIntQueueSegment();

				ret.intArray[ret.index++] = val;
				return ret;
			}
		}

		public PooledIntQueue newQueue()
		{
			return new PooledIntQueue(this);
		}

		private PooledIntQueueSegment getIntQueueSegment()
		{
			if (this.head == null)
				return new PooledIntQueueSegment();

			--this.pooledCount;
			final PooledIntQueueSegment ret = this.head;
			this.head = this.head.next;
			return ret;
		}
	}

	private final SegmentPool pool;

	public PooledIntQueue(final SegmentPool pool)
	{
		this.pool = pool;
	}

	private SegmentPool.PooledIntQueueSegment cur, last;
	private int size = 0;

	private int index = 0;

	public void add(final int val)
	{
		if (this.cur == null)
			this.cur = this.last = this.pool.getIntQueueSegment();

		this.last = this.last.add(val);
		++this.size;
	}

	public int poll()
	{
		final int ret = this.cur.intArray[this.index++];
		--this.size;

		if (this.index == this.cur.index)
		{
			this.index = 0;
			final SegmentPool.PooledIntQueueSegment next = this.cur.next;
			this.cur.release();
			this.cur = next;
		}

		return ret;
	}

	@Override
	public boolean isEmpty()
	{
		return this.size > 0;
	}
}