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
 *
 */

package ocd.lightpp.lighting.vanilla.light;

import java.util.function.Supplier;

import ocd.lightpp.api.lighting.ILightCollectionDescriptor;
import ocd.lightpp.api.util.IEmpty;
import ocd.lightpp.api.vanilla.world.ILightQueueDataset;
import ocd.lightpp.api.vanilla.world.ILightQueueDataset.ILightCollectionQueueDataset;
import ocd.lightpp.util.ITypedEqual;

public class SimpleLightCollectionQueueDataSet<LD extends ITypedEqual<LD>, Q extends IEmpty>
	implements ILightCollectionQueueDataset<LD, ILightCollectionDescriptor<LD>, Q>
{
	private final Supplier<Q> provider;

	private final SimpleLightCollectionDescriptor<LD> desc = new SimpleLightCollectionDescriptor<>();
	private int curIndex;

	private Q queue;

	private final ILightQueueDataset<LD, Q> lightDataSet;

	public SimpleLightCollectionQueueDataSet(final Supplier<Q> provider, final ILightQueueDataset<LD, Q> lightDataSet)
	{
		this.provider = provider;
		this.lightDataSet = lightDataSet;

		this.curIndex = 0;
		this.desc.setDescriptor(null);
	}

	@Override
	public Q get()
	{
		return this.queue == null ? this.queue = this.provider.get() : this.queue;
	}

	@Override
	public Q get(final ILightCollectionDescriptor<LD> collectionDesc)
	{
		final LD desc = collectionDesc.getDescriptor();
		return desc == null ? this.get() : this.lightDataSet.get(desc);
	}

	@Override
	public ILightCollectionDescriptor<LD> getDesc()
	{
		return this.desc;
	}

	@Override
	public Q getQueue()
	{
		return this.curIndex == 0 ? this.queue : this.lightDataSet.getQueue();
	}

	@Override
	public boolean next()
	{
		if (this.curIndex == 0)
		{
			if (!this.queue.isEmpty())
				return true;

			this.queue = null;
		}
		else if (this.queue != null)
		{
			this.curIndex = 0;
			this.desc.setDescriptor(null);
			return true;
		}

		if (!this.lightDataSet.next())
			return false;

		this.curIndex = 1;
		this.desc.setDescriptor(this.lightDataSet.getDesc());

		return true;
	}

	public static class Provider<LD extends ITypedEqual<LD>> implements ILightCollectionQueueDataset.Provider<LD, ILightCollectionDescriptor<LD>>
	{
		private final ILightQueueDataset.Provider<LD> dataSetProvider;

		public Provider(final ILightQueueDataset.Provider<LD> dataSetProvider)
		{
			this.dataSetProvider = dataSetProvider;
		}

		@Override
		public <Q extends IEmpty> ILightCollectionQueueDataset<LD, ILightCollectionDescriptor<LD>, Q> get(final Supplier<Q> queueProvider)
		{
			return new SimpleLightCollectionQueueDataSet<>(queueProvider, this.dataSetProvider.get(queueProvider));
		}
	}
}
