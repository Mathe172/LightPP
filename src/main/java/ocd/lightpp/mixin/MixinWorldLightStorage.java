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

import net.minecraft.world.World;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import ocd.lightpp.api.vanilla.type.CachedLightProviderType.TypedCachedLightProvider;
import ocd.lightpp.api.vanilla.type.LightProviderType.TypedLightProvider;
import ocd.lightpp.api.vanilla.type.TypedEmptySectionLightPredictor;
import ocd.lightpp.api.vanilla.type.TypedLightStorage;
import ocd.lightpp.api.vanilla.world.ILightStorageProvider;
import ocd.lightpp.api.vanilla.world.IVanillaWorldLightProvider;

@Mixin(World.class)
public abstract class MixinWorldLightStorage implements IVanillaWorldLightProvider
{
	@Override
	public @Nullable TypedCachedLightProvider<?, ?, ?, ?> getSkyLightProvider()
	{
		return null;
	}

	@Override
	public @Nullable TypedEmptySectionLightPredictor<?, ?, ?, ?> getEmptySectionLightProvider()
	{
		return null;
	}

	@Override
	public TypedLightProvider<?, ?, ?> getEmptyLightProvider()
	{
		return null;
	}

	@Override
	public ILightStorageProvider<?, ?, ?, ?, NibbleArray> getLightStorageProvider()
	{
		return null;
	}

	@Override
	public void initSectionLight(final TypedLightStorage<?, ?, ?, ?, ?> lightStorage, @Nullable final ExtendedBlockStorage upperBlockStorage)
	{
	}

	@Override
	public boolean isSectionLightTrivial(final TypedLightStorage<?, ?, ?, ?, ?> lightStorage, @Nullable final ExtendedBlockStorage upperBlockStorage)
	{
		return false;
	}
}
