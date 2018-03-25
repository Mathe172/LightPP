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

package ocd.lightpp.transformers;

import ocd.asmutil.ConstantMatcher;
import ocd.asmutil.InitInjector;
import ocd.asmutil.InitInjector.ArgLoader;
import ocd.asmutil.InvokeInjector;
import ocd.asmutil.LineInjector;
import ocd.asmutil.LocalTypedVarCapture;
import ocd.asmutil.MethodClassTransformer;
import ocd.asmutil.MethodMatcher;
import ocd.lightpp.transformers.util.NameRef;

public class TransformerAnvilChunkLoaderLightStorage extends MethodClassTransformer
{
	private static final String CLASS_NAME = "net.minecraft.world.chunk.storage.AnvilChunkLoader";

	private static final String NBT_COMPOUND_NAME = "net/minecraft/nbt/NBTTagCompound";

	private static final String WORLD_LIGHT_STORAGE_INITIALIZER_NAME = "ocd/lightpp/impl/IWorldLightStorageInitializer";
	private static final String INIT_EMPTY_NAME = "initEmptyLightStorage";
	private static final String INIT_EMPTY_DESC = "(Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;)V";

	private static final String READ_NAME = "func_75823_a";
	private static final String READ_DESC = "(Lnet/minecraft/world/World;Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/world/chunk/Chunk;";
	private static final String WRITE_NAME = "func_75820_a";
	private static final String WRITE_DESC = "(Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/World;Lnet/minecraft/nbt/NBTTagCompound;)V";

	private static final String DESERIALIZE_NAME = "deserialize";
	private static final String DESERIALIZE_DESC = "(Lnet/minecraft/nbt/NBTTagCompound;)V";
	private static final String SERIALIZE_NAME = "serialize";
	private static final String SERIALIZE_DESC = "(Lnet/minecraft/nbt/NBTTagCompound;)V";

	public TransformerAnvilChunkLoaderLightStorage()
	{
		super(CLASS_NAME);

		this.addTransformer(READ_NAME, READ_DESC, true,
			new InitInjector(
				NameRef.EXTENDED_BLOCK_STORAGE_NAME,
				null,
				new ArgLoader(1),
				new InvokeInjector(
					WORLD_LIGHT_STORAGE_INITIALIZER_NAME,
					INIT_EMPTY_NAME,
					INIT_EMPTY_DESC,
					false,
					true
				)
			),
			new LineInjector(
				new MethodMatcher(
					NameRef.EXTENDED_BLOCK_STORAGE_NAME,
					NameRef.SET_BLOCK_LIGHT_NAME,
					NameRef.SET_BLOCK_LIGHT_DESC,
					true
				),
				LineInjector.REMOVE,
				new LocalTypedVarCapture(NameRef.EXTENDED_BLOCK_STORAGE_NAME),
				new LocalTypedVarCapture(NBT_COMPOUND_NAME),
				new InvokeInjector(
					NameRef.ISERIALIZABLE_NAME,
					DESERIALIZE_NAME,
					DESERIALIZE_DESC,
					false,
					true
				)
			),
			new LineInjector(
				new MethodMatcher(
					NameRef.EXTENDED_BLOCK_STORAGE_NAME,
					NameRef.SET_SKY_LIGHT_NAME,
					NameRef.SET_SKY_LIGHT_DESC,
					true
				),
				LineInjector.REMOVE
			)
		);

		this.addTransformer(WRITE_NAME, WRITE_DESC, true,
			new LineInjector(
				new ConstantMatcher(NameRef.BLOCKLIGHT_NAME),
				LineInjector.REMOVE,
				new LocalTypedVarCapture(NameRef.EXTENDED_BLOCK_STORAGE_NAME),
				new LocalTypedVarCapture(NBT_COMPOUND_NAME),
				new InvokeInjector(
					NameRef.ISERIALIZABLE_NAME,
					SERIALIZE_NAME,
					SERIALIZE_DESC,
					false,
					true
				)
			),
			new LineInjector(
				new ConstantMatcher(NameRef.SKYLIGHT_NAME),
				LineInjector.REMOVE
			)
		);
	}
}
