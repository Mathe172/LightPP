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

package ocd.lightpp.api.lighting;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.INBTSerializable;
import ocd.lightpp.api.lighting.ILightTypeManager.ILightStorage;

public interface ILightTypeManager<D, MI, SI, S extends ILightStorage<D, SI>>
{
	ILightMap<D, MI> createMap();

	interface ILightIterator<D>
	{
		int get();

		D getDescriptor();

		boolean next();
	}

	interface ILightMap<D, I>
	{
		void clear();

		int get(D desc);

		void set(D desc, int val);

		ILightIterator<D> iterator();

		I getInterface();
	}

	interface ILightStorage<D, I> extends INBTSerializable<NBTBase>
	{
		Positioned<D, I> bind(BlockPos pos);

		interface Positioned<D, I>
		{
			int get(D desc);

			void set(D desc, int val);

			ILightIterator<D> iterator();

			I getInterface();
		}
	}

	interface ILightQueue<D, T>
	{
		boolean next();

		D getDescriptor();

		T get();
	}
}
