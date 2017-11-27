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

//PooledLongQueue code
//Implement own queue with pooled segments to reduce allocation costs and reduce idle memory footprint

import java.util.ArrayDeque;
import java.util.Deque;

import ocd.lightpp.util.PooledLongQueue.SegmentPool.PooledLongQueueSegment;

public class PooledLongQueue
{
	private static final int CACHED_QUEUE_SEGMENTS_COUNT = 1 << 12;
	private static final int QUEUE_SEGMENT_SIZE = 1 << 10;

	public static class SegmentPool
	{
		private final Deque<PooledLongQueueSegment> segments = new ArrayDeque<>();

		public class PooledLongQueueSegment
		{
			private final long[] longArray = new long[QUEUE_SEGMENT_SIZE];
			private int index = 0;
			private PooledLongQueueSegment next;

			private void release()
			{
				this.index = 0;
				this.next = null;

				if (SegmentPool.this.segments.size() < CACHED_QUEUE_SEGMENTS_COUNT)
					SegmentPool.this.segments.push(this);
			}

			public PooledLongQueueSegment add(final long val)
			{
				PooledLongQueueSegment ret = this;

				if (this.index == QUEUE_SEGMENT_SIZE)
					ret = this.next = SegmentPool.this.getLongQueueSegment();

				ret.longArray[ret.index++] = val;
				return ret;
			}
		}

		public PooledLongQueueSegment getLongQueueSegment()
		{
			if (this.segments.isEmpty())
				return new PooledLongQueueSegment();

			return this.segments.pop();
		}
	}

	private final SegmentPool pool;

	private PooledLongQueue(final SegmentPool pool)
	{
		this.pool = pool;
	}

	private PooledLongQueueSegment cur, last;
	private int size = 0;

	private int index = 0;

	public int size()
	{
		return this.size;
	}

	public boolean isEmpty()
	{
		return this.cur == null;
	}

	public void add(final long val)
	{
		if (this.cur == null)
			this.cur = this.last = this.pool.getLongQueueSegment();

		this.last = this.last.add(val);
		++this.size;
	}

	public long poll()
	{
		final long ret = this.cur.longArray[this.index++];
		--this.size;

		if (this.index == this.cur.index)
		{
			this.index = 0;
			final PooledLongQueueSegment next = this.cur.next;
			this.cur.release();
			this.cur = next;
		}

		return ret;
	}
}