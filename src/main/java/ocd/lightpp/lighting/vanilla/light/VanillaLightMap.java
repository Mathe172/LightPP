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

import java.util.Arrays;

import net.minecraft.world.EnumSkyBlock;
import ocd.lightpp.api.lighting.ILightMap;
import ocd.lightpp.api.vanilla.light.IVanillaLightMap;

public class VanillaLightMap implements ILightMap<IVanillaLightDescriptor, IVanillaLightMap>, IVanillaLightMap
{
	final int[] values = new int[IVanillaLightDescriptor.SKY_BLOCKS_VALUES.length];

	@Override
	public void clear()
	{
		Arrays.fill(this.values, 0);
	}

	@Override
	public int get(final IVanillaLightDescriptor desc)
	{
		return this.get(desc.getSkyBlock());
	}

	@Override
	public void set(final IVanillaLightDescriptor desc, final int val)
	{
		this.values[desc.getSkyBlock().ordinal()] = val;
	}

	@Override
	public ILightIterator<IVanillaLightDescriptor> iterator()
	{
		return new ILightIterator<IVanillaLightDescriptor>()
		{
			private final VanillaLightDescriptor desc = new VanillaLightDescriptor();
			private int curIndex = -1;

			@Override
			public int getLight()
			{
				return VanillaLightMap.this.values[this.curIndex];
			}

			@Override
			public IVanillaLightDescriptor getDescriptor()
			{
				return this.desc;
			}

			@Override
			public boolean next()
			{
				for (; ++this.curIndex < IVanillaLightDescriptor.SKY_BLOCKS_VALUES.length; ++this.curIndex)
					if (this.getLight() != 0)
					{
						this.desc.setSkyBlock(IVanillaLightDescriptor.SKY_BLOCKS_VALUES[this.curIndex]);

						return true;
					}

				return false;
			}
		};
	}

	@Override
	public IVanillaLightMap getInterface()
	{
		return this;
	}

	@Override
	public int get(final EnumSkyBlock lightType)
	{
		return this.values[lightType.ordinal()];
	}

	@Override
	public void add(final EnumSkyBlock lightType, final int light)
	{
		final int index = lightType.ordinal();
		this.values[index] = Math.max(this.values[index], light);
	}
}
