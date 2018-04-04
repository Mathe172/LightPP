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

package ocd.lightpp.api.vanilla.world;

import javax.annotation.Nullable;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import ocd.lightpp.api.vanilla.type.CachedLightProviderType.TypedCachedLightProvider;
import ocd.lightpp.api.vanilla.type.LightProviderType.TypedLightProvider;
import ocd.lightpp.api.vanilla.type.TypedEmptySectionLightPredictor;
import ocd.lightpp.api.vanilla.type.TypedLightStorageProvider;

public interface IVanillaWorldLightProvider
{
	TypedLightStorageProvider<?, ?, ?, ?, ?, NibbleArray> getLightStorageProvider();

	@Nullable TypedCachedLightProvider<?, ?, ?, ?, ?> getSkyLightProvider();

	@Nullable TypedEmptySectionLightPredictor<?, ?, ?, ?, ?> getEmptySectionLightPredictor();

	TypedLightProvider<?, ?, ?> getEmptyLightProvider();

	Object getWorldLightInterface(Chunk chunk, final BlockPos pos);

	void createLightStorage(ExtendedBlockStorage blockStorage);

	void createInitLightStorage(Chunk chunk, ExtendedBlockStorage blockStorage, @Nullable ExtendedBlockStorage upperBlockStorage);

	boolean isSectionLightTrivial(Chunk chunk, ExtendedBlockStorage blockStorage, @Nullable ExtendedBlockStorage upperBlockStorage);
}
