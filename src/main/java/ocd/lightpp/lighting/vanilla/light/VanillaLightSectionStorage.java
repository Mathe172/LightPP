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
import javax.annotation.Nullable;

import net.minecraft.nbt.NBTBase;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import ocd.lightpp.api.lighting.ILightMap.ILightIterator;
import ocd.lightpp.api.vanilla.light.IVanillaLightDescriptor;
import ocd.lightpp.api.vanilla.light.IVanillaLightInterface;
import ocd.lightpp.api.vanilla.light.IVanillaLightWorldInterface;
import ocd.lightpp.api.vanilla.world.ILightStorage;
import ocd.lightpp.api.vanilla.world.ILightStorage.Positioned.Writeable;
import ocd.lightpp.api.vanilla.world.ILightStorageHandler;
import ocd.lightpp.lighting.vanilla.light.VanillaLightSectionStorage.Container;

public class VanillaLightSectionStorage<T, HC extends Supplier<ILightStorageHandler.Positioned<T>>>
	implements ILightStorage<IVanillaLightDescriptor, IVanillaLightInterface, IVanillaLightWorldInterface, Container.Extended<T, HC>, T>
{
	final ILightStorageHandler<T, HC> handler;
	private final boolean hasSkyLight;

	@SuppressWarnings("unchecked") // Thanks, Java...
	final T[] skyBlockStorages = (T[]) new Object[IVanillaLightDescriptor.SKY_BLOCKS_VALUES.length];

	public VanillaLightSectionStorage(final ILightStorageHandler<T, HC> handler, final boolean hasSkyLight)
	{
		this.handler = handler;
		this.hasSkyLight = hasSkyLight;
	}

	@Override
	public Writeable<IVanillaLightDescriptor, IVanillaLightInterface> bind(final BlockPos pos, final Container.Extended<T, HC> container)
	{
		return container.bind(this, pos);
	}

	@Override
	public int getLight(final EnumSkyBlock lightType, final BlockPos pos)
	{
		final T storage = this.skyBlockStorages[lightType.ordinal()];
		return storage == null ? 0 : this.handler.get(storage, pos);
	}

	@Override
	public void setLight(final EnumSkyBlock lightType, final BlockPos pos, final int val)
	{
		final int index = lightType.ordinal();
		T storage = this.skyBlockStorages[index];

		if (storage == null)
			this.skyBlockStorages[index] = storage = this.handler.newStorage();

		this.handler.set(storage, pos, val);
	}

	@Override
	public IVanillaLightWorldInterface getStorageInterface(final BlockPos pos)
	{
		return new Container<>(this.handler.newContainer()).bind(pos);
	}

	@Override
	public Positioned<IVanillaLightDescriptor, IVanillaLightInterface> bind(final BlockPos pos)
	{
		return new Container.Extended<>(this.handler.newContainer()).bind(this, pos);
	}

	@Override
	public @Nullable NBTBase serialize(final EnumSkyBlock lightType)
	{
		return this.handler.serialize(this.skyBlockStorages[lightType.ordinal()]);
	}

	@Override
	public @Nullable NBTBase serializeExtraData()
	{
		return null;
	}

	@Override
	public void deserialize(final EnumSkyBlock lightType, final @Nullable NBTBase data)
	{
		final int index = lightType.ordinal();

		if (data == null)
			this.skyBlockStorages[index] = null;
		else
		{
			final T storage = this.handler.deserialize(data);

			this.skyBlockStorages[index] = this.handler.isEmpty(storage) ? null : storage;
		}
	}

	@Override
	public void deserializeExtraData(final @Nullable NBTBase data)
	{
	}

	@Override
	public int calcPacketSize()
	{
		int size = this.calcPacketSize(EnumSkyBlock.BLOCK);

		if (this.hasSkyLight)
			size += this.calcPacketSize(EnumSkyBlock.SKY);

		return size;
	}

	private int calcPacketSize(final EnumSkyBlock lightType)
	{
		return this.handler.calcPacketSize(this.skyBlockStorages[lightType.ordinal()]);
	}

	@Override
	public void writePacketData(final PacketBuffer buf)
	{
		this.writePacketData(buf, EnumSkyBlock.BLOCK);

		if (this.hasSkyLight)
			this.writePacketData(buf, EnumSkyBlock.SKY);
	}

	private void writePacketData(final PacketBuffer buf, final EnumSkyBlock lightType)
	{
		this.handler.writePacketData(buf, this.skyBlockStorages[lightType.ordinal()]);
	}

	@Override
	public void readPacketData(final PacketBuffer buf)
	{
		this.readPacketData(buf, EnumSkyBlock.BLOCK);

		if (this.hasSkyLight)
			this.readPacketData(buf, EnumSkyBlock.SKY);
	}

	private void readPacketData(final PacketBuffer buf, final EnumSkyBlock lightType)
	{
		final T storage = this.handler.readPacketData(buf, this.skyBlockStorages[lightType.ordinal()]);

		this.skyBlockStorages[lightType.ordinal()] = this.handler.isEmpty(storage) ? null : storage;
	}

	@Override
	public boolean isEmpty()
	{
		for (int i = 0; i < IVanillaLightDescriptor.SKY_BLOCKS_VALUES.length; ++i)
		{
			final T storage = this.skyBlockStorages[i];

			if (storage != null && !this.handler.isEmpty(storage))
				return false;
		}

		return true;
	}

	@Override
	public void cleanup()
	{
		for (int i = 0; i < IVanillaLightDescriptor.SKY_BLOCKS_VALUES.length; ++i)
			if (this.skyBlockStorages[i] != null && this.handler.isEmpty(this.skyBlockStorages[i]))
				this.skyBlockStorages[i] = null;
	}

	@Override
	@SuppressWarnings("deprecation")
	public T getStorage(final EnumSkyBlock lightType)
	{
		final int index = lightType.ordinal();
		T storage = this.skyBlockStorages[index];

		if (storage == null)
			this.skyBlockStorages[index] = storage = this.handler.newStorage();

		return storage;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setStorage(final EnumSkyBlock lightType, final @Nullable T storage)
	{
		this.skyBlockStorages[lightType.ordinal()] = storage;
	}

	public static class Container<T, HC extends Supplier<ILightStorageHandler.Positioned<T>>>
		implements IVanillaLightWorldInterface
	{
		final HC handlerContainer;
		VanillaLightSectionStorage<T, HC> sectionStorage;

		Container(final HC handlerContainer)
		{
			this.handlerContainer = handlerContainer;
		}

		Container<T, HC> bind(final BlockPos pos)
		{
			this.sectionStorage.handler.bind(pos, this.handlerContainer);

			return this;
		}

		@Override
		public int getLight(final EnumSkyBlock lightType)
		{
			final T storage = this.sectionStorage.skyBlockStorages[lightType.ordinal()];
			return storage == null ? 0 : this.handlerContainer.get().get(storage);
		}

		public static class Extended<T, HC extends Supplier<ILightStorageHandler.Positioned<T>>>
			extends Container<T, HC>
			implements IVanillaLightInterface,
			ILightIterator<IVanillaLightDescriptor>,
			Writeable<IVanillaLightDescriptor, IVanillaLightInterface>,
			Supplier<Writeable<IVanillaLightDescriptor, IVanillaLightInterface>>
		{
			private final VanillaLightDescriptor desc = new VanillaLightDescriptor();
			private T curStorgae;
			private int curIndex = 0;

			Extended(final HC handlerContainer)
			{
				super(handlerContainer);
			}

			Writeable<IVanillaLightDescriptor, IVanillaLightInterface> bind(final VanillaLightSectionStorage<T, HC> sectionStorage, final BlockPos pos)
			{
				this.sectionStorage = sectionStorage;
				super.bind(pos);

				return this;
			}

			@Override
			public Writeable<IVanillaLightDescriptor, IVanillaLightInterface> get()
			{
				return this;
			}

			@Override
			public ILightIterator<IVanillaLightDescriptor> getLightIterator()
			{
				this.curIndex = -1;
				return this;
			}

			@Override
			public IVanillaLightInterface getInterface()
			{
				return this;
			}

			@Override
			public int getLight(final IVanillaLightDescriptor desc)
			{
				return this.getLight(desc.getSkyBlock());
			}

			@Override
			public void set(final IVanillaLightDescriptor desc, final int val)
			{
				final int index = desc.getSkyBlock().ordinal();
				T storage = this.sectionStorage.skyBlockStorages[index];

				if (storage == null)
					this.sectionStorage.skyBlockStorages[index] = storage = this.sectionStorage.handler.newStorage();

				this.handlerContainer.get().set(storage, val);
			}

			@Override
			public int getLight()
			{
				return this.handlerContainer.get().get(this.curStorgae);
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
				{
					final T storage = this.sectionStorage.skyBlockStorages[this.curIndex];

					if (storage != null)
					{
						this.desc.setSkyBlock(IVanillaLightDescriptor.SKY_BLOCKS_VALUES[this.curIndex]);
						this.curStorgae = storage;

						return true;
					}
				}

				return false;
			}
		}
	}
}
