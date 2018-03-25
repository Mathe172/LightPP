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

import org.objectweb.asm.Type;

import ocd.asmutil.MethodTransformer;
import ocd.asmutil.injectors.InvokeInjector;
import ocd.asmutil.injectors.LocalIndexedVarCapture;
import ocd.asmutil.injectors.LocalTypedVarCapture;
import ocd.asmutil.matchers.MethodMatcher;
import ocd.asmutil.transformers.InitInjector;
import ocd.asmutil.transformers.LineInjector;
import ocd.lightpp.transformers.util.NameRef;
import ocd.lightpp.transformers.util.ObfuscationHelper;

public class TransformerChunkLightStorage extends MethodTransformer.Named
{
	private static final String CLASS_NAME = "net.minecraft.world.chunk.Chunk";

	private static final InvokeInjector.MethodDescriptor INIT_EMPTY = new InvokeInjector.MethodDescriptor(
		"initEmptyLightStorage",
		"(Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;)V"
	);

	private static final InvokeInjector.MethodDescriptor INIT = new InvokeInjector.MethodDescriptor(
		"initLightStorage",
		"(Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;)V"
	);

	private static final InvokeInjector.MethodDescriptor INIT_READ = new InvokeInjector.MethodDescriptor(
		"initLightStorageRead",
		"(Lnet/minecraft/world/chunk/storage/ExtendedBlockStorage;I)V"
	);

	private static final InvokeInjector.MethodDescriptor READ_PACKET = new InvokeInjector.MethodDescriptor(
		NameRef.ISERIALIZABLE_NAME,
		"readPacketData",
		"(Lnet/minecraft/network/PacketBuffer;)V",
		true
	);

	private static final MethodMatcher.MethodDescriptor INIT_PRIMER_MATCHER = new MethodMatcher.MethodDescriptor(
		"<init>",
		"(Lnet/minecraft/world/World;Lnet/minecraft/world/chunk/ChunkPrimer;II)V"
	);

	private static final MethodMatcher.MethodDescriptor READ_MATCHER = ObfuscationHelper.createMethodMatcher(
		CLASS_NAME,
		"func_186033_a",
		"(Lnet/minecraft/network/PacketBuffer;IZ)V"
	);

	private static final MethodMatcher.MethodDescriptor SET_BLOCK_STATE_MATCHER = ObfuscationHelper.createMethodMatcher(
		CLASS_NAME,
		"func_177436_a",
		"(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Lnet/minecraft/block/state/IBlockState;"
	);

	public TransformerChunkLightStorage()
	{
		super(CLASS_NAME);

		this.addTransformer(
			INIT_PRIMER_MATCHER,
			new InitInjector(
				NameRef.EXTENDED_BLOCK_STORAGE_NAME,
				null,
				InitInjector.CAPTURE_THIS,
				new InvokeInjector(INIT_EMPTY)
			)
		);

		this.addTransformer(
			READ_MATCHER,
			new InitInjector(
				NameRef.EXTENDED_BLOCK_STORAGE_NAME,
				null,
				InitInjector.CAPTURE_THIS,
				new LocalIndexedVarCapture(Type.INT_TYPE, 2),
				new InvokeInjector(INIT_READ)
			),
			new LineInjector(
				new MethodMatcher(NameRef.GET_BLOCK_LIGHT_MATCHER),
				LineInjector.REMOVE,
				new LocalTypedVarCapture(NameRef.EXTENDED_BLOCK_STORAGE_NAME),
				new LocalIndexedVarCapture(NameRef.PACKET_BUFFER_NAME, 1),
				new InvokeInjector(READ_PACKET)
			),
			new LineInjector(
				new MethodMatcher(NameRef.GET_SKY_LIGHT_MATCHER),
				LineInjector.REMOVE
			)
		);

		this.addTransformer(
			SET_BLOCK_STATE_MATCHER,
			new InitInjector(
				NameRef.EXTENDED_BLOCK_STORAGE_NAME,
				null,
				InitInjector.CAPTURE_THIS,
				new InvokeInjector(INIT)
			)
		);
	}
}
