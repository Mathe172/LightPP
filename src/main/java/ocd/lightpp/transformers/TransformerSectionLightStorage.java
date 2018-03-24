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
import org.objectweb.asm.tree.FieldInsnNode;

import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import ocd.lightpp.transformers.util.LineInjector;
import ocd.lightpp.transformers.util.MethodClassTransformer;

public class TransformerSectionLightStorage extends MethodClassTransformer
{
	private static final String CLASS_NAME = "net.minecraft.world.chunk.storage.ExtendedBlockStorage";

	private static final String CLASS_INTERNAL_NAME = CLASS_NAME.replace('.', '/');

	private static final String NIBBLE_ARRAY_DESC = "Lnet/minecraft/world/chunk/NibbleArray;";

	private static final String SKYLIGHT_ARRAY = "field_76685_h";
	private static final String BLOCKLIGHT_ARRAY = "field_76679_g";

	public TransformerSectionLightStorage()
	{
		super(CLASS_NAME);

		this.addTransformer("<init>", null, false,
			new LineInjector(
				(node, insn) -> {
					if (!(insn instanceof FieldInsnNode))
						return false;

					final FieldInsnNode fieldInsnNode = (FieldInsnNode) insn;

					if (fieldInsnNode.getOpcode() != Opcodes.PUTFIELD)
						return false;

					if (!CLASS_INTERNAL_NAME.equals(fieldInsnNode.owner))
						return false;

					if (!NIBBLE_ARRAY_DESC.equals(fieldInsnNode.desc))
						return false;

					final String name = fieldInsnNode.name;

					final FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;

					final String skyLightName = remapper.mapFieldName(CLASS_INTERNAL_NAME, SKYLIGHT_ARRAY, NIBBLE_ARRAY_DESC);
					final String blockLightName = remapper.mapFieldName(CLASS_INTERNAL_NAME, BLOCKLIGHT_ARRAY, NIBBLE_ARRAY_DESC);

					return name.equals(skyLightName) || name.equals(blockLightName);
				},
				LineInjector.REMOVE
			)
		);
	}
}
