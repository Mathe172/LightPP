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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import ocd.lightpp.api.lighting.ILightTypeManager.ILightIterator;
import ocd.lightpp.api.vanilla.light.IVanillaLightDescriptor;
import ocd.lightpp.api.vanilla.light.IVanillaLightInterface;
import ocd.lightpp.api.vanilla.light.IVanillaLightWorldInterface;
import ocd.lightpp.api.vanilla.world.ILightStorage;
import ocd.lightpp.api.vanilla.world.ILightStorage.Positioned.Writeable;
import ocd.lightpp.api.vanilla.world.ILightStorageHandler;
import ocd.lightpp.lighting.vanilla.light.VanillaLightSectionStorage.Container;

public class VanillaLightSectionStorage<T, HC extends Supplier<ILightStorageHandler.Positioned<T>>> implements ILightStorage<IVanillaLightDescriptor, IVanillaLightInterface, IVanillaLightWorldInterface, Container<T, HC>, T>
{
	public static class Container<T, HC extends Supplier<ILightStorageHandler.Positioned<T>>> implements Supplier<Writeable<IVanillaLightDescriptor, IVanillaLightInterface>>, IVanillaLightInterface, Writeable<IVanillaLightDescriptor, IVanillaLightInterface>
	{
		final HC handlerContainer;
		VanillaLightSectionStorage<T, HC> sectionStorage;

		public Container(final HC handlerContainer)
		{
			this.handlerContainer = handlerContainer;
		}

		public Container<T, HC> bind(final VanillaLightSectionStorage<T, HC> sectionStorage)
		{
			this.sectionStorage = sectionStorage;
			return this;
		}

		@Override
		public Writeable<IVanillaLightDescriptor, IVanillaLightInterface> get()
		{
			return this;
		}

		@Override
		public int getLight(final EnumSkyBlock lightType)
		{
			final T storage = this.sectionStorage.skyBlockStorages[lightType.ordinal()];
			return storage == null ? 0 : this.handlerContainer.get().get(storage);
		}

		@Override
		public int get(final IVanillaLightDescriptor desc)
		{
			return this.getLight(desc.getSkyBlock());
		}

		@Override
		public ILightIterator<IVanillaLightDescriptor> getLightIterator()
		{
			return null;
		}

		@Override
		public IVanillaLightInterface getInterface()
		{
			return this;
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
	}

	final ILightStorageHandler<T, HC> handler;

	@SuppressWarnings("unchecked") // Thanks, Java...
	final T[] skyBlockStorages = (T[]) new Object[IVanillaLightDescriptor.SKY_BLOCKS_VALUES.length];

	@Override
	public Writeable<IVanillaLightDescriptor, IVanillaLightInterface> bind(final BlockPos pos, final Container<T, HC> container)
	{
		return container.bind(this);
	}

	@Override
	public IVanillaLightWorldInterface getStorageInterface(final BlockPos pos)
	{
		return null;
	}

	@Override
	public IVanillaLightInterface bind(final BlockPos pos)
	{
		return new Container<>(this.handler.newContainer());
	}

	@Nullable
	@Override
	public NBTBase serialize(final EnumSkyBlock lightType)
	{
		return null;
	}

	@Nullable
	@Override
	public NBTBase serializeExtraData()
	{
		return null;
	}

	@Override
	public void deserialize(final EnumSkyBlock lightType, final NBTBase data)
	{

	}

	@Override
	public void deserializeExtraData(final NBTBase data)
	{

	}

	@Override
	public T getStorage(final EnumSkyBlock lightType)
	{
		return this.skyBlockStorages[lightType.ordinal()];
	}
}
