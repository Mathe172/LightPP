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

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import ocd.lightpp.api.lighting.ILightMap.ILightIterator;
import ocd.lightpp.api.vanilla.light.IVanillaLightInterface;
import ocd.lightpp.api.vanilla.world.ILightProvider;
import ocd.lightpp.lighting.vanilla.light.VanillaSkyLightProvider.Container;
import ocd.lightpp.api.vanilla.world.ILightProvider.Positioned;

public class VanillaSkyLightProvider
	implements ILightProvider.Cached<IVanillaLightDescriptor, IVanillaLightInterface, IVanillaLightInterface, Container, Void>,
	IVanillaLightInterface
{
	@Override
	public Positioned<IVanillaLightDescriptor, IVanillaLightInterface> bind(final BlockPos pos)
	{
		return this.createLightContainer();
	}

	@Override
	public IVanillaLightInterface getWorldLightInterface(final BlockPos pos)
	{
		return this;
	}

	@Override
	public Positioned<IVanillaLightDescriptor, IVanillaLightInterface> bind(final BlockPos pos, final Container container)
	{
		return container;
	}

	@Override
	public IVanillaLightInterface getWorldLightInterface(final BlockPos pos, final Void container)
	{
		return this;
	}

	@Override
	public Container createLightContainer()
	{
		return new Container();
	}

	@Override
	public Void createWorldLightContainer()
	{
		return null;
	}

	@Override
	public int getLight(final EnumSkyBlock lightType)
	{
		return lightType == EnumSkyBlock.SKY ? EnumSkyBlock.SKY.defaultLightValue : 0;
	}

	public static class Container
		implements Positioned<IVanillaLightDescriptor, IVanillaLightInterface>,
		Supplier<Positioned<IVanillaLightDescriptor, IVanillaLightInterface>>,
		IVanillaLightInterface,
		ILightIterator<IVanillaLightDescriptor>
	{
		private final VanillaLightDescriptor desc = new VanillaLightDescriptor(EnumSkyBlock.SKY);
		private boolean hasNext;

		@Override
		public Positioned<IVanillaLightDescriptor, IVanillaLightInterface> get()
		{
			return this;
		}

		@Override
		public int getLight(final EnumSkyBlock lightType)
		{
			return lightType == EnumSkyBlock.SKY ? EnumSkyBlock.SKY.defaultLightValue : 0;
		}

		@Override
		public int getLight(final IVanillaLightDescriptor desc)
		{
			return this.getLight(desc.getSkyBlock());
		}

		@Override
		public ILightIterator<IVanillaLightDescriptor> getLightIterator()
		{
			this.hasNext = true;

			return this;
		}

		@Override
		public IVanillaLightInterface getInterface()
		{
			return this;
		}

		@Override
		public int getLight()
		{
			return EnumSkyBlock.SKY.defaultLightValue;
		}

		@Override
		public IVanillaLightDescriptor getDescriptor()
		{
			return this.desc;
		}

		@Override
		public boolean next()
		{
			final boolean ret = this.hasNext;
			this.hasNext = false;

			return ret;
		}
	}
}
