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
import ocd.lightpp.api.vanilla.world.ILightProvider;
import ocd.lightpp.api.vanilla.world.ILightProvider.Positioned;

public class VanillaEmptyLightProvider
	implements ILightProvider<IVanillaLightDescriptor, IVanillaLightInterface, IVanillaLightWorldInterface>,
	IVanillaLightInterface,
	Positioned<IVanillaLightDescriptor, IVanillaLightInterface>,
	Supplier<Positioned<IVanillaLightDescriptor, IVanillaLightInterface>>,
	IVanillaLightWorldInterface,
	ILightIterator<IVanillaLightDescriptor>
{
	@Override
	public Positioned<IVanillaLightDescriptor, IVanillaLightInterface> bind(final BlockPos pos)
	{
		return this;
	}

	@Override
	public Positioned<IVanillaLightDescriptor, IVanillaLightInterface> get()
	{
		return this;
	}

	@Override
	public IVanillaLightWorldInterface getStorageInterface(final BlockPos pos)
	{
		return this;
	}

	@Override
	public int getLight(final EnumSkyBlock lightType)
	{
		return 0;
	}

	@Override
	public int getLight(final IVanillaLightDescriptor desc)
	{
		return 0;
	}

	@Override
	public ILightIterator<IVanillaLightDescriptor> getLightIterator()
	{
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
		return 0;
	}

	@Override
	public IVanillaLightDescriptor getDescriptor()
	{
		throw new IllegalStateException();
	}

	@Override
	public boolean next()
	{
		return false;
	}
}
