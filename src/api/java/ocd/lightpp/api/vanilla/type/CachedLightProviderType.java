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

package ocd.lightpp.api.vanilla.type;

import ocd.lightpp.api.vanilla.world.ILightProvider;
import ocd.lightpp.api.vanilla.world.ILightProvider.Cached;

public class CachedLightProviderType<LD, LI, WI, LC, WC>
{
	public final LightProviderType<LD, LI, WI> lightProviderType;
	public final ContainerType<LC> lightContainerType;
	public final ContainerType<WC> worldLightContainerType;

	public CachedLightProviderType(
		final LightProviderType<LD, LI, WI> lightProviderType,
		final ContainerType<LC> containerType,
		final ContainerType<WC> worldLightContainerType)
	{
		this.lightProviderType = lightProviderType;
		this.lightContainerType = containerType;
		this.worldLightContainerType = worldLightContainerType;
	}

	public static class TypedCachedLightProvider<LD, LI, WI, LC, WC>
	{
		public final CachedLightProviderType<LD, LI, WI, LC, WC> type;
		public final ILightProvider.Cached<LD, LI, WI, LC, WC> provider;

		public TypedCachedLightProvider(final CachedLightProviderType<LD, LI, WI, LC, WC> type, final Cached<LD, LI, WI, LC, WC> provider)
		{
			this.type = type;
			this.provider = provider;
		}
	}
}
