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

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

public interface InsnInjector extends SlicedInsnInjector
{
	void inject(String className, MethodNode node, AbstractInsnNode insn);

	@Override
	default void inject(final String className, final MethodNode node, final InsnList slice, final AbstractInsnNode insn)
	{
		this.inject(className, node, insn);
	}

	default InsnInjector andThen(final InsnInjector injector)
	{
		return new InsnInjector()
		{
			@Override
			public void inject(final String className, final MethodNode node, final InsnList slice, final AbstractInsnNode insn)
			{
				InsnInjector.this.inject(className, node, slice, insn);
				injector.inject(className, node, slice, insn);
			}

			@Override
			public void inject(final String className, final MethodNode node, final AbstractInsnNode insn)
			{
				InsnInjector.this.inject(className, node, insn);
				injector.inject(className, node, insn);
			}
		};
	}

	interface Simple extends InsnInjector
	{
		void inject(InsnList insns, AbstractInsnNode insn);

		@Override
		default void inject(final String className, final MethodNode node, final AbstractInsnNode insn)
		{
			this.inject(node.instructions, insn);
		}

		default InsnInjector.Simple andThen(final InsnInjector.Simple injector)
		{
			return new InsnInjector.Simple()
			{
				@Override
				public void inject(final String className, final MethodNode node, final InsnList slice, final AbstractInsnNode insn)
				{
					InsnInjector.Simple.this.inject(className, node, slice, insn);
					injector.inject(className, node, slice, insn);
				}

				@Override
				public void inject(final String className, final MethodNode node, final AbstractInsnNode insn)
				{
					InsnInjector.Simple.this.inject(className, node, insn);
					injector.inject(className, node, insn);
				}

				@Override
				public void inject(final InsnList insns, final AbstractInsnNode insn)
				{
					InsnInjector.Simple.this.inject(insns, insn);
					injector.inject(insns, insn);
				}
			};
		}
	}
}
