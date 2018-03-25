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
import ocd.asmutil.InvokeInjector;
import ocd.asmutil.LineInjector;
import ocd.asmutil.LocalIndexedVarCapture;
import ocd.asmutil.LocalTypedVarCapture;
import ocd.asmutil.MethodClassTransformer;
import ocd.asmutil.MethodMatcher;
import ocd.lightpp.transformers.util.NameRef;

public class TransformerSPacketChunkDataLightStorage extends MethodClassTransformer
{
	private static final String CLASS_NAME = "net.minecraft.network.play.server.SPacketChunkData";

	private static final String CALC_SIZE_NAME = "func_189556_a";
	private static final String CALC_SIZE_DESC = "(Lnet/minecraft/world/chunk/Chunk;ZI)I";

	private static final String EXTRACT_DATA_NAME = "func_189555_a";
	private static final String EXTRACT_DATA_DESC = "(Lnet/minecraft/network/PacketBuffer;Lnet/minecraft/world/chunk/Chunk;ZI)I";

	private static final String ICALC_SIZE_NAME = "calcPacketSize";
	private static final String ICALC_SIZE_DESC = "()I";

	private static final String WRITE_PACKET_NAME = "writePacketData";
	private static final String WRITEPACKET_DESC = "(Lnet/minecraft/network/PacketBuffer;)V";

	public TransformerSPacketChunkDataLightStorage()
	{
		super(CLASS_NAME);

		this.addTransformer(CALC_SIZE_NAME, CALC_SIZE_DESC, true,
			new LineInjector(
				new MethodMatcher(
					NameRef.EXTENDED_BLOCK_STORAGE_NAME,
					NameRef.GET_BLOCK_LIGHT_NAME,
					NameRef.GET_BLOCK_LIGHT_DESC,
					true
				),
				(InjectionLocator.Simple) insn -> {
					insn = insn.getNext();

					if (insn == null || insn.getOpcode() != Opcodes.IADD)
						return false;

					insn = insn.getNext();

					return insn != null && insn.getOpcode() == Opcodes.ISTORE;
				},
				1,
				LineInjector.REMOVE,
				new LocalTypedVarCapture(NameRef.EXTENDED_BLOCK_STORAGE_NAME),
				new InvokeInjector(
					NameRef.ISERIALIZABLE_NAME,
					ICALC_SIZE_NAME,
					ICALC_SIZE_DESC,
					false,
					true
				)
			),
			new LineInjector(
				new MethodMatcher(
					NameRef.EXTENDED_BLOCK_STORAGE_NAME,
					NameRef.GET_SKY_LIGHT_NAME,
					NameRef.GET_SKY_LIGHT_DESC,
					true
				),
				LineInjector.REMOVE
			)
		);

		this.addTransformer(EXTRACT_DATA_NAME, EXTRACT_DATA_DESC, true,
			new LineInjector(
				new MethodMatcher(
					NameRef.EXTENDED_BLOCK_STORAGE_NAME,
					NameRef.GET_BLOCK_LIGHT_NAME,
					NameRef.GET_BLOCK_LIGHT_DESC,
					true
				),
				new LocalTypedVarCapture(NameRef.EXTENDED_BLOCK_STORAGE_NAME),
				new LocalIndexedVarCapture(NameRef.PACKET_BUFFER_NAME, 1),
				new InvokeInjector(
					NameRef.ISERIALIZABLE_NAME,
					WRITE_PACKET_NAME,
					WRITEPACKET_DESC,
					false,
					true
				)
			),
			new LineInjector(
				new MethodMatcher(
					NameRef.EXTENDED_BLOCK_STORAGE_NAME,
					NameRef.GET_SKY_LIGHT_NAME,
					NameRef.GET_SKY_LIGHT_DESC,
					true
				),
				LineInjector.REMOVE
			)
		);
	}
}
