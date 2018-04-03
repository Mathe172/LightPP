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

package ocd.lightpp.lighting.vanilla.light;

import java.util.function.Supplier;

import ocd.lightpp.api.util.IEmpty;
import ocd.lightpp.api.vanilla.light.IVanillaLightDescriptor;
import ocd.lightpp.api.vanilla.world.ILightQueueDataset;

public class VanillaLightQueueDataset<Q extends IEmpty> implements ILightQueueDataset<IVanillaLightDescriptor, Q>
{
	private final Supplier<Q> provider;

	private final VanillaLightDescriptor desc = new VanillaLightDescriptor();
	private int curIndex;

	@SuppressWarnings("unchecked") // Thanks, Java...
	private final Q[] queues = (Q[]) new Object[IVanillaLightDescriptor.SKY_BLOCKS_VALUES.length];

	public VanillaLightQueueDataset(final Supplier<Q> provider)
	{
		this.provider = provider;

		this.curIndex = 0;
		this.desc.setSkyBlock(IVanillaLightDescriptor.SKY_BLOCKS_VALUES[0]);
	}

	@Override
	public Q get(final IVanillaLightDescriptor desc)
	{
		final int i = desc.getSkyBlock().ordinal();
		return this.queues[i] == null ? this.queues[i] = this.provider.get() : this.queues[i];
	}

	@Override
	public IVanillaLightDescriptor getDesc()
	{
		return this.desc;
	}

	@Override
	public Q getQueue()
	{
		return this.queues[this.curIndex];
	}

	@Override
	public boolean next()
	{
		if (!this.queues[this.curIndex].isEmpty())
			return true;

		this.queues[this.curIndex] = null;

		for (int i = 0; i < IVanillaLightDescriptor.SKY_BLOCKS_VALUES.length; ++i)
		{
			if (this.queues[i] == null)
				continue;

			this.curIndex = i;
			this.desc.setSkyBlock(IVanillaLightDescriptor.SKY_BLOCKS_VALUES[i]);
			return true;
		}

		return false;
	}

	public static class Provider implements ILightQueueDataset.Provider<IVanillaLightDescriptor>
	{
		@Override
		public <Q extends IEmpty> ILightQueueDataset<IVanillaLightDescriptor, Q> get(final Supplier<Q> queueProvider)
		{
			return new VanillaLightQueueDataset<>(queueProvider);
		}
	}
}
