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

import javax.annotation.Nullable;

import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import ocd.asmutil.injectors.InvokeInjector;
import ocd.asmutil.matchers.FieldMatcher;
import ocd.asmutil.matchers.MethodMatcher;

public class ObfuscationHelper
{
	public static InvokeInjector.MethodDescriptor createMethodDesriptor(
		@Nullable String owner,
		final String name,
		final String desc,
		final boolean iface,
		final boolean isStatic
	)
	{
		owner = owner == null ? null : owner.replace('.', '/');
		return new InvokeInjector.MethodDescriptor(owner, FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(owner, name, desc), desc, iface, isStatic);
	}

	public static InvokeInjector.MethodDescriptor createMethodDesriptor(
		final @Nullable String owner,
		final String name,
		final String desc,
		final boolean iface
	)
	{
		return createMethodDesriptor(owner, name, desc, iface, false);
	}

	public static InvokeInjector.MethodDescriptor createMethodDesriptor(final String name, final String desc)
	{
		return createMethodDesriptor(null, name, desc, false, false);
	}

	public static MethodMatcher.MethodDescriptor createMethodMatcher(@Nullable String owner, final String name, final @Nullable String desc)
	{
		owner = owner == null ? null : owner.replace('.', '/');
		return new MethodMatcher.MethodDescriptor(owner, FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(owner, name, desc), desc);
	}

	public static MethodMatcher.MethodDescriptor createMethodMatcher(final String name, final @Nullable String desc)
	{
		return createMethodMatcher(null, name, desc);
	}

	public static MethodMatcher.MethodDescriptor createMethodMatcher(final String name)
	{
		return createMethodMatcher(name, null);
	}

	public static FieldMatcher.FieldDescriptor createFieldMatcher(@Nullable String owner, final String name, final @Nullable String desc)
	{
		owner = owner == null ? null : owner.replace('.', '/');
		return new FieldMatcher.FieldDescriptor(owner, FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(owner, name, desc), desc);
	}

	public static FieldMatcher.FieldDescriptor createFieldMatcher(final String name, final @Nullable String desc)
	{
		return createFieldMatcher(null, name, desc);
	}

	public static FieldMatcher.FieldDescriptor createFieldMatcher(final String name)
	{
		return createFieldMatcher(name, null);
	}
}
