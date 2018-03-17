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
import java.util.List;
import javax.annotation.Nullable;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

public class LineReplacer implements MethodNodeTransformer
{
	private final List<LineProcessor> processors = new ArrayList<>();

	public void addProcessor(final LineProcessor processor)
	{
		this.processors.add(processor);
	}

	public LineReplacer addProcessor(
		final InjectionLocator lineIdentifier,
		final @Nullable InjectionLocator lineEndIdentifier,
		final @Nullable SlicedInsnInjector replacer,
		final int stackMod
	)
	{
		this.addProcessor(new LineProcessor(lineIdentifier, lineEndIdentifier, replacer, stackMod));

		return this;
	}

	public LineReplacer addProcessor(final InjectionLocator lineIdentifier, final @Nullable SlicedInsnInjector replacer)
	{
		return this.addProcessor(lineIdentifier, null, replacer, 0);
	}

	public LineReplacer addProcessor(final InjectionLocator lineIdentifier)
	{
		return this.addProcessor(lineIdentifier, null);
	}

	@Override
	public MethodNode transform(final String className, final MethodNode methodNode)
	{
		final Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());

		try
		{
			analyzer.analyze(className, methodNode);
		} catch (final AnalyzerException e)
		{
			throw new IllegalArgumentException("Received class " + className + " in a broken state", e);
		}

		final Frame<BasicValue>[] frames = analyzer.getFrames();

		final InsnList insns = methodNode.instructions;

		int frameIndex = 0;

		for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn == null ? insns.getFirst() : insn.getNext(), ++frameIndex)
		{
			LineProcessor processor = null;

			for (final LineProcessor processor_ : this.processors)
				if (processor_.lineIdentifier.test(methodNode, insn))
				{
					if (processor != null)
						throw new IllegalStateException("Multiple overwrites for the same line");

					processor = processor_;
				}

			if (processor == null)
				continue;

			final InjectionLocator lineEndIdentifier = processor.lineEndIdentifier;

			for (; insn != null; insn = insn.getNext(), ++frameIndex)
				if (lineEndIdentifier != null && lineEndIdentifier.test(methodNode, insn) || frames[frameIndex + 1].getStackSize() == 0)
					break;

			if (insn == null)
				throw new IllegalStateException("Couldn't find end of line");

			final int stackSize = frames[frameIndex + 1].getStackSize() - processor.stackMod;

			final InsnList slice = new InsnList();

			for (int i = 0; insn != null && frames[frameIndex - i].getStackSize() != stackSize; ++i)
			{
				final AbstractInsnNode remove = insn;
				insn = insn.getPrevious();
				insns.remove(remove);
				slice.insert(remove);
			}

			if (insn != null)
			{
				final AbstractInsnNode remove = insn;
				insn = insn.getPrevious();
				insns.remove(remove);
				slice.insert(remove);
			}

			final AbstractInsnNode end = insn == null ? insns.getFirst() : insn.getNext();

			if (processor.replacer == null)
				insns.insertBefore(end, new InsnNode(Opcodes.NOP));
			else
				processor.replacer.inject(className, methodNode, slice, end);

			insn = end.getPrevious();
		}

		return methodNode;
	}

	public static class LineProcessor
	{
		public final InjectionLocator lineIdentifier;
		public final @Nullable InjectionLocator lineEndIdentifier;
		public final @Nullable SlicedInsnInjector replacer;
		final int stackMod;

		private LineProcessor(
			final InjectionLocator lineIdentifier,
			final @Nullable InjectionLocator lineEndIdentifier,
			final @Nullable SlicedInsnInjector replacer,
			final int stackMod)
		{
			this.lineIdentifier = lineIdentifier;
			this.lineEndIdentifier = lineEndIdentifier;
			this.replacer = replacer;
			this.stackMod = stackMod;
		}
	}
}
