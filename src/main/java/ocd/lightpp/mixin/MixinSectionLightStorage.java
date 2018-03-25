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

package ocd.lightpp.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import ocd.lightpp.api.vanilla.type.TypedLightStorage;
import ocd.lightpp.api.vanilla.world.ICleanable;
import ocd.lightpp.api.vanilla.world.ILightStorage;
import ocd.lightpp.api.vanilla.world.ISerializable;
import ocd.lightpp.api.vanilla.world.IVanillaLightStorageHolder;
import ocd.lightpp.impl.ISectionEmpty;
import ocd.lightpp.transformers.util.NameRef;

@Mixin(ExtendedBlockStorage.class)
public abstract class MixinSectionLightStorage implements IVanillaLightStorageHolder, ISerializable, ICleanable, ISectionEmpty
{
	@Shadow
	private int blockRefCount;

	private @Nullable TypedLightStorage<?, ?, ?, ?, NibbleArray> lightStorageHolder;

	@Override
	public TypedLightStorage<?, ?, ?, ?, NibbleArray> getLightStorage()
	{
		if (this.lightStorageHolder == null)
			throw new IllegalStateException("Light storage has not been initialized");

		return this.lightStorageHolder;
	}

	@Override
	public void setLightStorage(final TypedLightStorage<?, ?, ?, ?, NibbleArray> lightStorageHolder)
	{
		this.lightStorageHolder = lightStorageHolder;
	}

	@Override
	public void serialize(final NBTTagCompound data)
	{
		final ILightStorage<?, ?, ?, ?, NibbleArray> lightStorage = this.getLightStorage().storage;

		final NBTBase skyLight = lightStorage.serialize(EnumSkyBlock.SKY);
		final NBTBase blockLight = lightStorage.serialize(EnumSkyBlock.BLOCK);
		final NBTBase extraDara = lightStorage.serializeExtraData();

		if (blockLight != null)
			data.setTag(NameRef.BLOCKLIGHT_NAME, blockLight);

		if (skyLight != null)
			data.setTag(NameRef.SKYLIGHT_NAME, skyLight);

		if (extraDara != null)
			data.setTag(NameRef.LIGHT_DATA_NAME, extraDara);
	}

	@Override
	public void deserialize(final NBTTagCompound data)
	{
		final ILightStorage<?, ?, ?, ?, NibbleArray> lightStorage = this.getLightStorage().storage;

		lightStorage.deserialize(EnumSkyBlock.BLOCK, data.getTag(NameRef.BLOCKLIGHT_NAME));
		lightStorage.deserialize(EnumSkyBlock.SKY, data.getTag(NameRef.SKYLIGHT_NAME));
		lightStorage.deserializeExtraData(data.getTag(NameRef.LIGHT_DATA_NAME));
	}

	@Override
	public int calcPacketSize()
	{
		return this.getLightStorage().storage.calcPacketSize();
	}

	@Override
	public void writePacketData(final PacketBuffer buf)
	{
		this.getLightStorage().storage.writePacketData(buf);
	}

	@Override
	public void readPacketData(final PacketBuffer buf)
	{
		this.getLightStorage().storage.readPacketData(buf);
	}

	@Override
	public void cleanup()
	{
		if (this.lightStorageHolder != null)
			this.lightStorageHolder.storage.cleanup();
	}

	@Override
	public boolean containsNoBlocks()
	{
		return this.blockRefCount == 0;
	}

	/**
	 * Fix: Sections that contain light but no blocks are considered empty.
	 *
	 * @author PhiPro
	 */
	@Overwrite
	public boolean isEmpty()
	{
		return false;
	}

	// Legacy support
	@SuppressWarnings("deprecation")
	private int getLight(final EnumSkyBlock lightType, final int x, final int y, final int z)
	{
		return this.getLightStorage().storage.getLight(lightType, new BlockPos(x, y, z));
	}

	/**
	 * Legacy support
	 *
	 * @author PhiPro
	 */
	@Overwrite
	public int getSkyLight(final int x, final int y, final int z)
	{
		return this.getLight(EnumSkyBlock.SKY, x, y, z);
	}

	/**
	 * Legacy support
	 *
	 * @author PhiPro
	 */
	@Overwrite
	public int getBlockLight(final int x, final int y, final int z)
	{
		return this.getLight(EnumSkyBlock.BLOCK, x, y, z);
	}

	// Legacy support
	@SuppressWarnings("deprecation")
	private void setLight(final EnumSkyBlock lightType, final int x, final int y, final int z, final int value)
	{
		this.getLightStorage().storage.setLight(lightType, new BlockPos(x, y, z), value);
	}

	/**
	 * Legacy support
	 *
	 * @author PhiPro
	 */
	@Overwrite
	public void setSkyLight(final int x, final int y, final int z, final int value)
	{
		this.setLight(EnumSkyBlock.SKY, x, y, z, value);
	}

	/**
	 * Legacy support
	 *
	 * @author PhiPro
	 */
	@Overwrite
	public void setBlockLight(final int x, final int y, final int z, final int value)
	{
		this.setLight(EnumSkyBlock.BLOCK, x, y, z, value);
	}

	// Legacy support
	@SuppressWarnings("deprecation")
	private NibbleArray getLightData(final EnumSkyBlock lightType)
	{
		return this.getLightStorage().storage.getStorage(lightType);
	}

	/**
	 * Legacy support
	 *
	 * @author PhiPro
	 */
	@Overwrite
	public NibbleArray getSkyLight()
	{
		return this.getLightData(EnumSkyBlock.SKY);
	}

	/**
	 * Legacy support
	 *
	 * @author PhiPro
	 */
	@Overwrite
	public NibbleArray getBlockLight()
	{
		return this.getLightData(EnumSkyBlock.BLOCK);
	}

	// Legacy support
	@SuppressWarnings("deprecation")
	private void setLightData(final EnumSkyBlock lightType, final NibbleArray data)
	{
		this.getLightStorage().storage.setStorage(lightType, data);
	}

	/**
	 * Legacy support
	 *
	 * @author PhiPro
	 */
	@Overwrite
	public void setSkyLight(final NibbleArray data)
	{
		this.setLightData(EnumSkyBlock.SKY, data);
	}

	/**
	 * Legacy support
	 *
	 * @author PhiPro
	 */
	@Overwrite
	public void setBlockLight(final NibbleArray data)
	{
		this.setLightData(EnumSkyBlock.BLOCK, data);
	}
}
