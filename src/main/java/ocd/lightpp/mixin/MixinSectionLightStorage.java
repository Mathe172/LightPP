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
import ocd.lightpp.api.vanilla.type.TypedLightStorage;
import ocd.lightpp.api.vanilla.world.IVanillaLightStorageHolder;
import ocd.lightpp.util.Util;

@Mixin(ExtendedBlockStorage.class)
public abstract class MixinSectionLightStorage implements IVanillaLightStorageHolder
{

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

	@Redirect(method = "<init>*", at = @At(value = "NEW", target = "net/minecraft/world/chunk/NibbleArray"), allow = 2)
	private NibbleArray emptyLightArray()
	{
		return Util.EMPTY_NIBBLE_ARRAY;
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
