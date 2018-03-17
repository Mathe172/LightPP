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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import ocd.lightpp.transformers.util.MethodSignature.MethodDescriptor;

public class InitInjector implements MethodNodeTransformer
{
	private final Multimap<String, Descriptor> descriptors = ArrayListMultimap.create();

	public static final InsnInjector.Simple CAPTURE_THIS = new ArgLoader(0);

	public static class ArgLoader implements InsnInjector.Simple
	{
		private final int index;

		public ArgLoader(final int index)
		{
			this.index = index;
		}

		@Override
		public void inject(final InsnList insns, final AbstractInsnNode insn)
		{
			insns.insertBefore(insn, new VarInsnNode(Opcodes.ALOAD, this.index));
			insns.insertBefore(insn, new InsnNode(Opcodes.SWAP));
		}
	}

	public void addInjector(final Descriptor desc)
	{
		this.descriptors.put(desc.desc.owner, desc);
	}

	public InitInjector addInjector(
		final String owner,
		final @Nullable String desc,
		final InsnInjector injector
	)
	{
		this.addInjector(new Descriptor(new MethodDescriptor(owner, "<init>", desc, false), injector));

		return this;
	}

	@Override
	public MethodNode transform(final String className, final MethodNode methodNode)
	{
		final InsnList insns = methodNode.instructions;

		for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext())
		{
			if (!(insn instanceof MethodInsnNode))
				continue;

			final MethodInsnNode methodInsnNode = (MethodInsnNode) insn;

			if (methodInsnNode.getOpcode() != Opcodes.INVOKESPECIAL)
				continue;

			if (!"<init>".equals(methodInsnNode.name))
				continue;

			final Collection<Descriptor> candidates = this.descriptors.get(methodInsnNode.owner);

			if (candidates.isEmpty())
				continue;

			List<Descriptor> matches = null;

			for (final Descriptor candidate : candidates)
			{
				final MethodDescriptor md = candidate.desc;

				if (md.desc == null || md.desc.equals(methodInsnNode.desc))
				{
					if (matches == null)
						matches = new ArrayList<>();

					matches.add(candidate);
				}
			}

			if (matches == null)
				continue;

			final AbstractInsnNode next = insn.getNext();

			for (int j = 0; j < matches.size(); ++j)
			{
				if (j < matches.size() - 1)
					insns.insertBefore(next, new InsnNode(Opcodes.DUP));

				insns.insertBefore(next, new InsnNode(Opcodes.DUP));

				matches.get(j).injector.inject(className, methodNode, next);
			}
		}

		return methodNode;
	}

	public static class Descriptor
	{
		public final MethodDescriptor desc;
		public final InsnInjector injector;

		public Descriptor(final MethodDescriptor desc, final InsnInjector injector)
		{
			this.desc = desc;
			this.injector = injector;
		}
	}
}
