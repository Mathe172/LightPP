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

import ocd.asmutil.MethodTransformer;
import ocd.asmutil.injectors.InvokeInjector;
import ocd.asmutil.injectors.LocalParameterVarCapture;
import ocd.asmutil.injectors.LocalTypedVarCapture;
import ocd.asmutil.matchers.ConstantMatcher;
import ocd.asmutil.matchers.MethodMatcher;
import ocd.asmutil.transformers.InitInjector;
import ocd.asmutil.transformers.InitInjector.ArgLoader;
import ocd.asmutil.transformers.LineInjector;
import ocd.lightpp.transformers.util.NameRef;
import ocd.lightpp.transformers.util.ObfuscationHelper;

public class TransformerAnvilChunkLoaderLightStorage extends MethodTransformer.Named
{
	private static final String CLASS_NAME = "net.minecraft.world.chunk.storage.AnvilChunkLoader";

	private static final String NBT_COMPOUND_NAME = "net/minecraft/nbt/NBTTagCompound";

	private static final InvokeInjector.MethodDescriptor CREATE_LOGHT_STORAGE = new InvokeInjector.MethodDescriptor(
		"ocd/lightpp/api/vanilla/world/IVanillaWorldLightProvider",
		"createLightStorage",
		"(Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;)V",
		true
	);

	private static final MethodMatcher.MethodDescriptor READ_MATCHER = ObfuscationHelper.createMethodMatcher(
		CLASS_NAME,
		"func_75823_a",
		"(Lnet/minecraft/world/World;Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/world/chunk/Chunk;"
	);

	private static final MethodMatcher.MethodDescriptor WRITE_MATCHER = ObfuscationHelper.createMethodMatcher(
		CLASS_NAME,
		"func_75820_a",
		"(Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/World;Lnet/minecraft/nbt/NBTTagCompound;)V"
	);

	private static final InvokeInjector.MethodDescriptor DESERIALIZE = new InvokeInjector.MethodDescriptor(
		NameRef.ISERIALIZABLE_NAME,
		"deserialize",
		"(Lnet/minecraft/nbt/NBTTagCompound;)V",
		true
	);

	private static final InvokeInjector.MethodDescriptor SERIALIZE = new InvokeInjector.MethodDescriptor(
		NameRef.ISERIALIZABLE_NAME,
		"serialize",
		"(Lnet/minecraft/nbt/NBTTagCompound;)V",
		true
	);

	public TransformerAnvilChunkLoaderLightStorage()
	{
		super(CLASS_NAME);

		this.addTransformer(
			READ_MATCHER,
			new InitInjector(
				NameRef.EXTENDED_BLOCK_STORAGE_NAME,
				null,
				new ArgLoader(1),
				new InvokeInjector(CREATE_LOGHT_STORAGE)
			),
			new LineInjector(
				new MethodMatcher(NameRef.SET_BLOCK_LIGHT_MATCHER),
				LineInjector.REMOVE,
				new LocalParameterVarCapture(0, NameRef.SET_BLOCK_LIGHT_MATCHER),
				new LocalTypedVarCapture(NBT_COMPOUND_NAME),
				new InvokeInjector(DESERIALIZE)
			),
			new LineInjector(
				new MethodMatcher(NameRef.SET_SKY_LIGHT_MATCHER),
				LineInjector.REMOVE
			)
		);

		this.addTransformer(
			WRITE_MATCHER,
			new LineInjector(
				new ConstantMatcher(NameRef.BLOCKLIGHT_NAME),
				LineInjector.REMOVE,
				new LocalParameterVarCapture(0, NameRef.GET_BLOCK_LIGHT_MATCHER),
				new LocalTypedVarCapture(NBT_COMPOUND_NAME),
				new InvokeInjector(SERIALIZE)
			),
			new LineInjector(
				new ConstantMatcher(NameRef.SKYLIGHT_NAME),
				LineInjector.REMOVE
			)
		);
	}
}
