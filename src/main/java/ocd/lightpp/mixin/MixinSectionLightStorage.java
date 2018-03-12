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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import ocd.lightpp.api.vanilla.world.ILightStorage.ILightStorageType.ILightStorageHolder;
import ocd.lightpp.api.vanilla.world.ILightStorage.ILightStorageType.TypedLightStorage;

@Mixin(ExtendedBlockStorage.class)
public class MixinSectionLightStorage implements ILightStorageHolder<NibbleArray>
{
	private @Nullable TypedLightStorage<?, ?, ?, ?, NibbleArray> lightStorageHolder;

	@Override
	public @Nullable TypedLightStorage<?, ?, ?, ?, NibbleArray> getLightStorage()
	{
		return this.lightStorageHolder;
	}

	@Override
	public void setLightStorage(final TypedLightStorage<?, ?, ?, ?, NibbleArray> lightStorageHolder)
	{
		this.lightStorageHolder = lightStorageHolder;
	}

	@Redirect(method = "<init>*", at = @At(value = "NEW", target = "net.minecraft.world.chunk.NibbleArray"), allow = 2)
	private @Nullable NibbleArray emptyLightArray()
	{
		return null;
	}

	@Overwrite
	public boolean isEmpty()
	{
		return false;
	}

	// Legacy support
	@SuppressWarnings("deprecation")
	private int getLight(final EnumSkyBlock lightType, final int x, final int y, final int z)
	{
		if (this.lightStorageHolder == null)
			throw new IllegalStateException("Light storage has not been initialized");

		return this.lightStorageHolder.storage.getLight(lightType, new BlockPos(x, y, z));
	}

	@Overwrite
	public int getSkyLight(final int x, final int y, final int z)
	{
		return this.getLight(EnumSkyBlock.SKY, x, y, z);
	}

	@Overwrite
	public int getBlockLight(final int x, final int y, final int z)
	{
		return this.getLight(EnumSkyBlock.BLOCK, x, y, z);
	}

	// Legacy support
	@SuppressWarnings("deprecation")
	private void setLight(final EnumSkyBlock lightType, final int x, final int y, final int z, final int value)
	{
		if (this.lightStorageHolder == null)
			throw new IllegalStateException("Light storage has not been initialized");

		this.lightStorageHolder.storage.setLight(lightType, new BlockPos(x, y, z), value);
	}

	@Overwrite
	public void setSkyLight(final int x, final int y, final int z, final int value)
	{
		this.setLight(EnumSkyBlock.SKY, x, y, z, value);
	}

	@Overwrite
	public void setBlockLight(final int x, final int y, final int z, final int value)
	{
		this.setLight(EnumSkyBlock.BLOCK, x, y, z, value);
	}

	// Legacy support
	@SuppressWarnings("deprecation")
	private NibbleArray getLightData(final EnumSkyBlock lightType)
	{
		if (this.lightStorageHolder == null)
			throw new IllegalStateException("Light storage has not been initialized");

		return this.lightStorageHolder.storage.getStorage(lightType);
	}

	@Overwrite
	public NibbleArray getSkyLight()
	{
		return this.getLightData(EnumSkyBlock.SKY);
	}

	@Overwrite
	public NibbleArray getBlockLight()
	{
		return this.getLightData(EnumSkyBlock.BLOCK);
	}

	// Legacy support
	@SuppressWarnings("deprecation")
	private void setLightData(final EnumSkyBlock lightType, final NibbleArray data)
	{
		if (this.lightStorageHolder == null)
			throw new IllegalStateException("Light storage has not been initialized");

		this.lightStorageHolder.storage.setStorage(lightType, data);
	}

	@Overwrite
	public void setSkyLight(final NibbleArray data)
	{
		this.setLightData(EnumSkyBlock.SKY, data);
	}

	@Overwrite
	public void setBlockLight(final NibbleArray data)
	{
		this.setLightData(EnumSkyBlock.BLOCK, data);
	}
}
