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

package ocd.lightpp.lighting.vanilla.world;

import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.NibbleArray;
import ocd.lightpp.api.vanilla.world.ILightStorageHandler;
import ocd.lightpp.lighting.vanilla.world.VanillaLightStorageHandler.Container;

public class VanillaLightStorageHandler implements ILightStorageHandler<NibbleArray, Container>
{
	private static final byte[] EMPTY_ARRAY = new byte[2048];
	private static final NBTBase EMPTY_NBT = new NBTTagByteArray(EMPTY_ARRAY);

	@Override
	public NibbleArray newStorage()
	{
		return new NibbleArray();
	}

	@Override
	public Container newContainer()
	{
		return new Container();
	}

	@Override
	public Positioned<NibbleArray> bind(final BlockPos pos)
	{
		return this.bind(pos, new Container());
	}

	@Override
	public Positioned<NibbleArray> bind(final BlockPos pos, final Container container)
	{
		return container.bind(pos);
	}

	@Override
	public int get(final NibbleArray storage, final BlockPos pos)
	{
		return storage.get(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public void set(final NibbleArray storage, final BlockPos pos, final int val)
	{
		storage.set(pos.getX(), pos.getY(), pos.getZ(), val);
	}

	@Override
	public boolean isEmpty(final NibbleArray storage)
	{
		for (final byte b : storage.getData())
			if (b != 0)
				return false;

		return true;
	}

	@Override
	public NBTBase serialize(final @Nullable NibbleArray storage)
	{
		return storage == null ? EMPTY_NBT : new NBTTagByteArray(storage.getData());
	}

	@Override
	public NibbleArray deserialize(final NBTBase data)
	{
		if (data.getId() != 7)
			throw new IllegalArgumentException();

		return new NibbleArray(((NBTTagByteArray) data).getByteArray());
	}

	@Override
	public int calcPacketSize(final @Nullable NibbleArray storage)
	{
		return 2048;
	}

	@Override
	public void writePacketData(final PacketBuffer buf, final @Nullable NibbleArray storage)
	{
		buf.writeBytes(storage == null ? EMPTY_ARRAY : storage.getData());
	}

	@Override
	public NibbleArray readPacketData(final PacketBuffer buf, @Nullable NibbleArray storage)
	{
		if (storage == null)
			storage = new NibbleArray();

		buf.readBytes(storage.getData());

		return storage;
	}

	public static class Container
		implements ILightStorageHandler.Positioned<NibbleArray>,
		Supplier<ILightStorageHandler.Positioned<NibbleArray>>
	{
		int index;

		ILightStorageHandler.Positioned<NibbleArray> bind(final BlockPos pos)
		{
			this.index = pos.getY() << 8 | pos.getZ() << 4 | pos.getX();

			return this;
		}

		@Override public int get(final NibbleArray storage)
		{
			return storage.getFromIndex(this.index);
		}

		@Override public void set(final NibbleArray storage, final int value)
		{
			storage.setIndex(this.index, value);
		}

		@Override public Positioned<NibbleArray> get()
		{
			return this;
		}
	}
}
