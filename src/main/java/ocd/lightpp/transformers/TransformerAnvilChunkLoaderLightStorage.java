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

import ocd.lightpp.transformers.util.ConstantMatcher;
import ocd.lightpp.transformers.util.InitInjector;
import ocd.lightpp.transformers.util.InitInjector.ArgLoader;
import ocd.lightpp.transformers.util.InvokeInjector;
import ocd.lightpp.transformers.util.LineReplacer;
import ocd.lightpp.transformers.util.LocalTypedVarCapture;
import ocd.lightpp.transformers.util.MethodClassTransformer;
import ocd.lightpp.transformers.util.MethodMatcher;
import ocd.lightpp.util.NameRef;

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
			new InitInjector().addInjector(
				NameRef.EXTENDED_BLOCK_STORAGE_NAME,
				null,
				new ArgLoader(1).andThen(
					new InvokeInjector(
						WORLD_LIGHT_STORAGE_INITIALIZER_NAME,
						INIT_EMPTY_NAME,
						INIT_EMPTY_DESC,
						false,
						true
					)
				)
			).andThen(
				new LineReplacer().addProcessor(
					new MethodMatcher(
						NameRef.EXTENDED_BLOCK_STORAGE_NAME,
						NameRef.SET_BLOCK_LIGHT_NAME,
						NameRef.SET_BLOCK_LIGHT_DESC,
						true
					),
					new LocalTypedVarCapture(NameRef.EXTENDED_BLOCK_STORAGE_NAME).andThen(
						new LocalTypedVarCapture(NBT_COMPOUND_NAME)).andThen(
						new InvokeInjector(
							NameRef.ISERIALIZABLE_NAME,
							DESERIALIZE_NAME,
							DESERIALIZE_DESC,
							false,
							true
						)
					)
				).addProcessor(
					new MethodMatcher(
						NameRef.EXTENDED_BLOCK_STORAGE_NAME,
						NameRef.SET_SKY_LIGHT_NAME,
						NameRef.SET_SKY_LIGHT_DESC,
						true
					)
				)
			)
		);

		this.addTransformer(WRITE_NAME, WRITE_DESC, true,
			new LineReplacer().addProcessor(
				new ConstantMatcher(NameRef.BLOCKLIGHT_NAME),
				new LocalTypedVarCapture(NameRef.EXTENDED_BLOCK_STORAGE_NAME).andThen(
					new LocalTypedVarCapture(NBT_COMPOUND_NAME)).andThen(
					new InvokeInjector(
						NameRef.ISERIALIZABLE_NAME,
						SERIALIZE_NAME,
						SERIALIZE_DESC,
						false,
						true
					)
				)
			).addProcessor(
				new ConstantMatcher(NameRef.SKYLIGHT_NAME)
			)
		);
	}
}
