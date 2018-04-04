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

import org.objectweb.asm.Opcodes;

import ocd.asmutil.InjectionLocator;
import ocd.asmutil.MethodTransformer;
import ocd.asmutil.injectors.InvokeInjector;
import ocd.asmutil.injectors.LocalIndexedVarCapture;
import ocd.asmutil.injectors.LocalParameterVarCapture;
import ocd.asmutil.matchers.MethodMatcher;
import ocd.asmutil.transformers.LineInjector;
import ocd.lightpp.transformers.util.NameRef;
import ocd.lightpp.transformers.util.ObfuscationHelper;

public class TransformerSPacketChunkDataLightStorage extends MethodTransformer.Named
{
	private static final String CLASS_NAME = "net.minecraft.network.play.server.SPacketChunkData";

	private static final MethodMatcher.MethodDescriptor CALC_SIZE_MATCHER = ObfuscationHelper.createMethodMatcher(
		CLASS_NAME,
		"func_189556_a",
		"(Lnet/minecraft/world/chunk/Chunk;ZI)I"
	);

	private static final MethodMatcher.MethodDescriptor EXTRACT_DATA_MATCHER = ObfuscationHelper.createMethodMatcher(
		CLASS_NAME,
		"func_189555_a",
		"(Lnet/minecraft/network/PacketBuffer;Lnet/minecraft/world/chunk/Chunk;ZI)I"
	);

	private static final InvokeInjector.MethodDescriptor CALC_SIZE = new InvokeInjector.MethodDescriptor(
		NameRef.ISERIALIZABLE_NAME,
		"calcPacketSize",
		"()I",
		true
	);

	private static final InvokeInjector.MethodDescriptor WRITE_PACKET_DATA = new InvokeInjector.MethodDescriptor(
		NameRef.ISERIALIZABLE_NAME,
		"writePacketData",
		"(Lnet/minecraft/network/PacketBuffer;)V",
		true
	);

	public TransformerSPacketChunkDataLightStorage()
	{
		super(CLASS_NAME);

		this.addTransformer(
			CALC_SIZE_MATCHER,
			new LineInjector(
				new MethodMatcher(NameRef.GET_BLOCK_LIGHT_MATCHER),
				(InjectionLocator.Simple) insn -> {
					insn = insn.getNext();

					if (insn == null || insn.getOpcode() != Opcodes.IADD)
						return false;

					insn = insn.getNext();

					return insn != null && insn.getOpcode() == Opcodes.ISTORE;
				},
				1,
				LineInjector.REMOVE,
				new LocalParameterVarCapture(0, NameRef.GET_BLOCK_LIGHT_MATCHER),
				new InvokeInjector(CALC_SIZE)
			),
			new LineInjector(
				new MethodMatcher(NameRef.GET_SKY_LIGHT_MATCHER),
				LineInjector.REMOVE
			)
		);

		this.addTransformer(
			EXTRACT_DATA_MATCHER,
			new LineInjector(
				new MethodMatcher(NameRef.GET_BLOCK_LIGHT_MATCHER),
				LineInjector.REMOVE,
				new LocalParameterVarCapture(0, NameRef.GET_BLOCK_LIGHT_MATCHER),
				new LocalIndexedVarCapture(NameRef.PACKET_BUFFER_NAME, 1),
				new InvokeInjector(WRITE_PACKET_DATA)
			),
			new LineInjector(
				new MethodMatcher(NameRef.GET_SKY_LIGHT_MATCHER),
				LineInjector.REMOVE
			)
		);
	}
}
