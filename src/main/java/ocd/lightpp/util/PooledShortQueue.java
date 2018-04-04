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

package ocd.lightpp.util;

//PooledShortQueue code
//Implement own queue with pooled segments to reduce allocation costs and reduce idle memory footprint

import ocd.lightpp.api.util.IEmpty;

public class PooledShortQueue implements IEmpty
{
	public static class SegmentPool
	{
		private final int CACHED_QUEUE_SEGMENTS_COUNT;
		private final int QUEUE_SEGMENT_SIZE;

		private PooledShortQueueSegment head;
		private int pooledCount = 0;

		public SegmentPool(final int CACHED_QUEUE_SEGMENTS_COUNT, final int QUEUE_SEGMENT_SIZE)
		{
			this.CACHED_QUEUE_SEGMENTS_COUNT = CACHED_QUEUE_SEGMENTS_COUNT;
			this.QUEUE_SEGMENT_SIZE = QUEUE_SEGMENT_SIZE;
		}

		private class PooledShortQueueSegment
		{
			private final short[] shortArray = new short[SegmentPool.this.QUEUE_SEGMENT_SIZE];
			private int index = 0;
			private PooledShortQueueSegment next;

			private void release()
			{
				if (SegmentPool.this.pooledCount >= SegmentPool.this.CACHED_QUEUE_SEGMENTS_COUNT)
					return;

				this.index = 0;

				++SegmentPool.this.pooledCount;
				this.next = SegmentPool.this.head;
				SegmentPool.this.head = this;
			}

			private PooledShortQueueSegment add(final short val)
			{
				PooledShortQueueSegment ret = this;

				if (this.index == SegmentPool.this.QUEUE_SEGMENT_SIZE)
					ret = this.next = SegmentPool.this.getShortQueueSegment();

				ret.shortArray[ret.index++] = val;
				return ret;
			}
		}

		public PooledShortQueue newQueue()
		{
			return new PooledShortQueue(this);
		}

		private PooledShortQueueSegment getShortQueueSegment()
		{
			if (this.head == null)
				return new PooledShortQueueSegment();

			--this.pooledCount;
			final PooledShortQueueSegment ret = this.head;
			this.head = this.head.next;
			ret.next = null;

			return ret;
		}
	}

	private final SegmentPool pool;

	public PooledShortQueue(final SegmentPool pool)
	{
		this.pool = pool;
	}

	private SegmentPool.PooledShortQueueSegment cur, last;
	private int size = 0;

	private int index = 0;

	public void add(final short val)
	{
		if (this.cur == null)
			this.cur = this.last = this.pool.getShortQueueSegment();

		this.last = this.last.add(val);
		++this.size;
	}

	public short poll()
	{
		final short ret = this.cur.shortArray[this.index++];
		--this.size;

		if (this.index == this.cur.index)
		{
			this.index = 0;
			final SegmentPool.PooledShortQueueSegment next = this.cur.next;
			this.cur.release();
			this.cur = next;
		}

		return ret;
	}

	@Override
	public boolean isEmpty()
	{
		return this.size == 0;
	}
}
