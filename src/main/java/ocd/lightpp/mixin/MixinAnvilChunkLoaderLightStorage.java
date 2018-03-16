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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import ocd.lightpp.api.vanilla.type.TypedLightStorage;
import ocd.lightpp.api.vanilla.world.IVanillaLightStorageHolder;
import ocd.lightpp.api.vanilla.world.IVanillaWorldLightProvider;
import ocd.lightpp.util.Util;

@Mixin(AnvilChunkLoader.class)
public class MixinAnvilChunkLoaderLightStorage
{
	private static final String BLOCKLIGHT_NAME = "BlockLight";
	private static final String SKYLIGHT_NAME = "SkyLight";
	private static final String LIGHT_DATA_NAME = "LightData";

	@Redirect(
		method = "readChunkFromNBT",
		at = @At(value = "NEW", target = "net/minecraft/world/chunk/storage/ExtendedBlockStorage")
	)
	private ExtendedBlockStorage injectLightData(final int y, final boolean storeSkylight, final World worldIn, final NBTTagCompound compound)
	{
		final ExtendedBlockStorage blockStorage = new ExtendedBlockStorage(y, storeSkylight);

		final TypedLightStorage<?, ?, ?, ?, NibbleArray> lightStorage = ((IVanillaWorldLightProvider) worldIn).getLightStorageProvider().createLightStorage();

		((IVanillaLightStorageHolder) blockStorage).setLightStorage(lightStorage);

		lightStorage.storage.deserialize(EnumSkyBlock.BLOCK, compound.getTag(BLOCKLIGHT_NAME));
		lightStorage.storage.deserialize(EnumSkyBlock.SKY, compound.getTag(SKYLIGHT_NAME));
		lightStorage.storage.deserializeExtraData(compound.getTag(LIGHT_DATA_NAME));

		return blockStorage;
	}

	@Redirect(
		method = "readChunkFromNBT",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;setSkyLight(Lnet/minecraft/world/chunk/NibbleArray;)V")
	)
	private void bypassSkyLightAssignment(final ExtendedBlockStorage blockStorage, final NibbleArray newBlocklightArray)
	{
	}

	@Redirect(
		method = "readChunkFromNBT",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;setBlockLight(Lnet/minecraft/world/chunk/NibbleArray;)V")
	)
	private void bypassBlockLightAssignment(final ExtendedBlockStorage blockStorage, final NibbleArray newBlocklightArray)
	{
	}

	@Redirect(
		method = "readChunkFromNBT",
		slice = @Slice(from = @At(value = "CONSTANT:ONE", args = "stringValue=" + SKYLIGHT_NAME)),
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/nbt/NBTTagCompound;getByteArray(Ljava/lang/String;)[B",
			ordinal = 0
		)
	)
	private byte[] bypassSkyLightReadFromNBT(final NBTTagCompound compound, final String key)
	{
		return Util.BYTE_ARRAY_2048;
	}

	@Redirect(
		method = "readChunkFromNBT",
		slice = @Slice(from = @At(value = "CONSTANT:ONE", args = "stringValue=" + BLOCKLIGHT_NAME)),
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/nbt/NBTTagCompound;getByteArray(Ljava/lang/String;)[B",
			ordinal = 0
		)
	)
	private byte[] bypassBlockLightReadFromNBT(final NBTTagCompound compound, final String key)
	{
		return Util.BYTE_ARRAY_2048;
	}

	@Redirect(
		method = "writeChunkToNBT",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;getSkyLight()Lnet/minecraft/world/chunk/NibbleArray;")
	)
	private NibbleArray bypassSkyLightQuery(final ExtendedBlockStorage blockStorage)
	{
		return Util.EMPTY_NIBBLE_ARRAY;
	}

	@Redirect(
		method = "writeChunkToNBT",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;getBlockLight()Lnet/minecraft/world/chunk/NibbleArray;")
	)
	private NibbleArray bypassBlockLightQuery(final ExtendedBlockStorage blockStorage)
	{
		return Util.EMPTY_NIBBLE_ARRAY;
	}

	@Redirect(
		method = "writeChunkToNBT",
		slice = @Slice(from = @At(value = "CONSTANT:FIRST", args = "stringValue=" + SKYLIGHT_NAME)),
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/nbt/NBTTagCompound;setByteArray(Ljava/lang/String;[B)V",
			ordinal = 0
		)
	)
	private void bypassSkyLightWriteToNBT1(final NBTTagCompound compound, final String key, final byte[] value)
	{
	}

	// Srsly...
	@Redirect(
		method = "writeChunkToNBT",
		slice = @Slice(from = @At(value = "CONSTANT:FIRST", args = "stringValue=" + SKYLIGHT_NAME)),
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/nbt/NBTTagCompound;setByteArray(Ljava/lang/String;[B)V",
			ordinal = 1
		)
	)
	private void bypassSkyLightWriteToNBT2(final NBTTagCompound compound, final String key, final byte[] value)
	{
	}

	@Redirect(
		method = "writeChunkToNBT",
		slice = @Slice(from = @At(value = "CONSTANT:ONE", args = "stringValue=" + BLOCKLIGHT_NAME)),
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/nbt/NBTTagCompound;setByteArray(Ljava/lang/String;[B)V",
			ordinal = 0
		)
	)
	private void bypassBlockLightWriteToNBT(final NBTTagCompound compound, final String key, final byte[] value)
	{
	}
}
