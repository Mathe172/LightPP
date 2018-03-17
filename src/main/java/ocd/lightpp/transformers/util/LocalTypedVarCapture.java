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

package ocd.lightpp.transformers.util;

import java.util.List;
import javax.annotation.Nullable;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class LocalTypedVarCapture implements InsnInjector
{
	private final String desc;
	private final int opcode;

	public LocalTypedVarCapture(final Type type)
	{
		this.desc = type.getDescriptor();
		this.opcode = type.getOpcode(Opcodes.ILOAD);
	}

	public LocalTypedVarCapture(final String className)
	{
		this(Type.getObjectType(className));
	}

	@Override
	public void inject(final String className, final MethodNode node, final AbstractInsnNode insn)
	{
		final String[] types = getTypes(className, node, insn);

		int var = -1;

		for (int i = 0; i < types.length; ++i)
		{
			final String type = types[i];

			if (type == null || !this.desc.equals(type))
				continue;

			if (var != -1)
				throw new IllegalStateException("Found multiple locals with requested type " + this.desc);

			var = i;
		}

		if (var == -1)
			throw new IllegalStateException("No local with requested type " + this.desc);

		node.instructions.insertBefore(insn, new VarInsnNode(this.opcode, var));
	}

	@Override
	public void inject(final String className, final MethodNode methodNode, final InsnList slice, final AbstractInsnNode insn)
	{
		final int var = getVar(this.opcode, this.desc, className, methodNode, slice, insn);
		methodNode.instructions.insertBefore(insn, new VarInsnNode(this.opcode, var));
	}

	public static int getVar(
		final int opcode,
		final String desc,
		final String className,
		final MethodNode methodNode,
		final InsnList slice,
		final AbstractInsnNode insn
	)
	{
		final String[] types = getTypes(className, methodNode, insn);

		int var = -1;

		for (AbstractInsnNode insn_ = slice.getFirst(); insn_ != null; insn_ = insn_.getNext())
		{
			if (!(insn_ instanceof VarInsnNode))
				continue;

			final VarInsnNode varInsn = (VarInsnNode) insn_;

			if (varInsn.getOpcode() != opcode)
				continue;

			final String type = types[varInsn.var];

			if (type == null || !desc.equals(type))
				continue;

			if (var != -1)
				throw new IllegalStateException("Found multiple locals with requested type " + desc);

			var = varInsn.var;
		}

		if (var == -1)
			throw new IllegalStateException("No local with requested type " + desc);

		return var;
	}

	public static String[] getTypes(final String className, final MethodNode methodNode, final AbstractInsnNode insn)
	{
		return getTypes(className, methodNode, getFrame(insn));
	}

	public static @Nullable FrameNode getFrame(AbstractInsnNode insn)
	{
		for (; insn != null; insn = insn.getPrevious())
			if (insn instanceof FrameNode)
			{
				final FrameNode frame = (FrameNode) insn;

				if (frame.type != Opcodes.F_NEW)
					throw new IllegalArgumentException("Require expanded frames");

				return frame;
			}

		return null;
	}

	public static String[] getTypes(final String className, final MethodNode methodNode, @Nullable final FrameNode frame)
	{
		if (frame == null)
		{
			final Type[] args = Type.getArgumentTypes(methodNode.desc);

			final boolean staticAcc = (methodNode.access & Opcodes.ACC_STATIC) != 0;

			final String[] ret = new String[staticAcc ? args.length : args.length + 1];

			if (!staticAcc)
				ret[0] = "L" + className + ";";

			for (int i = 0; i < args.length; ++i)
				ret[staticAcc ? i : i + 1] = args[i].getDescriptor();

			return ret;
		}

		final List<Object> locals = frame.local;

		final String[] ret = new String[locals.size()];

		for (int i = 0; i < locals.size(); ++i)
		{
			final Object local = locals.get(i);

			if (local instanceof String)
				ret[i] = "L" + local + ";";
			else if (local instanceof Integer)
			{
				final Integer type = (Integer) local;

				if (type == Opcodes.INTEGER)
					ret[i] = Type.INT_TYPE.getDescriptor();
				else if (type == Opcodes.FLOAT)
					ret[i] = Type.FLOAT_TYPE.getDescriptor();
				else if (type == Opcodes.LONG)
					ret[i] = Type.LONG_TYPE.getDescriptor();
				else if (type == Opcodes.DOUBLE)
					ret[i] = Type.DOUBLE_TYPE.getDescriptor();
			}
		}

		return ret;
	}
}
