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
import ocd.lightpp.api.vanilla.light.IVanillaLightDescriptor;
import ocd.lightpp.api.vanilla.light.IVanillaLightInterface;
import ocd.lightpp.api.vanilla.light.IVanillaLightWorldInterface;
import ocd.lightpp.api.vanilla.world.IEmptySectionLightPredictor;
import ocd.lightpp.api.vanilla.world.ILightProvider.Positioned;
import ocd.lightpp.lighting.vanilla.light.VanillaEmptySectionLightPredictor.Container;

public class VanillaEmptySectionLightPredictor
	implements IEmptySectionLightPredictor<IVanillaLightDescriptor, IVanillaLightInterface, IVanillaLightWorldInterface, Container.Extended>
{
	@Override public Positioned<IVanillaLightDescriptor, IVanillaLightInterface> bind(
		final BlockPos pos,
		final BlockPos upperPos,
		final IVanillaLightInterface lightInterface
	)
	{
		return new Container.Extended().bind(lightInterface);
	}

	@Override
	public Positioned<IVanillaLightDescriptor, IVanillaLightInterface> bind(
		final BlockPos pos,
		final BlockPos upperPos,
		final IVanillaLightInterface lightInterface,
		final Container.Extended container
	)
	{
		return container.bind(lightInterface);
	}

	@Override
	public IVanillaLightWorldInterface getStorageInterface(
		final BlockPos pos,
		final BlockPos upperPos,
		final IVanillaLightInterface lightInterface
	)
	{
		return new Container().bind(lightInterface);
	}

	public static class Container
		implements IVanillaLightWorldInterface
	{
		private IVanillaLightInterface lightInterface;

		Container bind(final IVanillaLightInterface lightInterface)
		{
			this.lightInterface = lightInterface;

			return this;
		}

		public int getLight()
		{
			final int light = this.lightInterface.getLight(EnumSkyBlock.SKY);

			return light == EnumSkyBlock.SKY.defaultLightValue ? light : 0;
		}

		@Override
		public int getLight(final EnumSkyBlock lightType)
		{
			return lightType == EnumSkyBlock.SKY ? this.getLight() : 0;
		}

		public static class Extended
			extends Container
			implements IVanillaLightInterface,
			Positioned<IVanillaLightDescriptor, IVanillaLightInterface>,
			ILightIterator<IVanillaLightDescriptor>,
			Supplier<Positioned<IVanillaLightDescriptor, IVanillaLightInterface>>
		{
			private final VanillaLightDescriptor desc = new VanillaLightDescriptor(EnumSkyBlock.SKY);
			private boolean hasNext;

			@Override
			Extended bind(final IVanillaLightInterface lightInterface)
			{
				super.bind(lightInterface);

				return this;
			}

			@Override
			public Positioned<IVanillaLightDescriptor, IVanillaLightInterface> get()
			{
				return this;
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
		}
	}
}
