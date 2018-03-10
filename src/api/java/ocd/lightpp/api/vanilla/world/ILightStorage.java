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

package ocd.lightpp.api.vanilla.world;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import ocd.lightpp.api.lighting.ILightTypeManager.ILightIterator;

public interface ILightStorage<D, LI, WI, C, T> extends ILightProvider<D, LI, WI, C>
{
	@Override
	Positioned.Writeable<D, LI> bind(BlockPos pos, C container);

	LI bind(BlockPos pos);

	/**
	 * Legacy support
	 */
	@Nullable NBTBase serialize(EnumSkyBlock lightType);

	@Nullable NBTBase serializeExtraData();

	/**
	 * Legacy support
	 */
	void deserialize(EnumSkyBlock lightType, NBTBase data);

	void deserializeExtraData(NBTBase data);

	int

	default T getStorage(final EnumSkyBlock lightType)
	{
		throw new UnsupportedOperationException();
	}

	interface Positioned<D, LI>
	{
		int get(final D desc);

		ILightIterator<D> getLightIterator();

		LI getInterface();

		interface Writeable<D, LI> extends Positioned<D, LI>
		{
			void set(D desc, int val);

			default void notifyLightSet(final D desc)
			{
			}
		}
	}
}
