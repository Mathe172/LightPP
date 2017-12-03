/*
 * MIT License
 *
 * Copyright (c) 2017-2017 OverengineeredCodingDuo
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
 */

package ocd.lightpp.lighting.vanilla;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.EnumSkyBlock;
import ocd.lightpp.api.lighting.ILightAccess;
import ocd.lightpp.api.lighting.ILightPropagator;
import ocd.lightpp.lighting.LightingEngine;

public abstract class VanillaLightPropagator implements ILightPropagator
{
	protected int opacity;
	protected int maxLight;

	private static final EnumFacing[] lookupOrder =
		new EnumFacing[] {EnumFacing.UP, EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST};

	@Override
	public void prepareSpread(final int light)
	{
	}

	@Override
	public int getMaxNeighborLight(final ILightAccess lightAccess)
	{
		return this.maxLight;
	}

	@Override
	public int getMaxNeighborLight(final EnumFacing dir, final ILightAccess lightAccess)
	{
		return this.maxLight;
	}

	@Override
	public EnumFacing[] getLookupOrder(final ILightAccess lightAccess)
	{
		return lookupOrder;
	}

	@Override
	public int getMaxSpread(final int light)
	{
		return light - 1;
	}

	@Override
	public int getMaxSpread(final EnumFacing dir, final int light)
	{
		return light - 1;
	}

	@Override
	public int calcLight(final EnumFacing dir, final ILightAccess lightAccess, final int neighborLight)
	{
		return this.calcSpread(dir.getOpposite(), neighborLight, this.opacity);
	}

	@Override
	public int calcSpread(final EnumFacing dir, final int light, final ILightAccess neighborLightAccess)
	{
		return this.calcSpread(dir, light, neighborLightAccess.getLightOpacity());
	}

	protected int calcSpread(final EnumFacing dir, final int sourceLight, final int targetOpac)
	{
		return sourceLight - Math.max(targetOpac, 1);
	}

	public static class Block extends VanillaLightPropagator
	{
		private int sourceLight;

		@Override
		public void prepareCalc(final ILightAccess lightAccess)
		{
			final IBlockState state = lightAccess.getBlockState();

			this.sourceLight = state.getLightValue(lightAccess.getWorld(), lightAccess.getPos());

			if (this.sourceLight >= LightingEngine.MAX_LIGHT - 1)
				this.opacity = 1;
			else
				this.opacity = state.getLightOpacity(lightAccess.getWorld(), lightAccess.getPos());

			this.maxLight = LightingEngine.MAX_LIGHT - Math.max(this.opacity, 1);
		}

		@Override
		public int getSourceLight(final ILightAccess lightAccess)
		{
			return this.sourceLight;
		}
	}

	public static class Sky extends VanillaLightPropagator
	{
		@Override
		public void prepareCalc(final ILightAccess lightAccess)
		{
			this.opacity = lightAccess.getLightOpacity();
			this.maxLight = LightingEngine.MAX_LIGHT - Math.max(this.opacity, 1);
		}

		@Override
		public int getSourceLight(final ILightAccess lightAccess)
		{
			return 0;
		}

		@Override
		public int getMaxNeighborLight(final ILightAccess lightAccess)
		{
			return LightingEngine.MAX_LIGHT;
		}

		@Override
		public int getMaxNeighborLight(final EnumFacing dir, final ILightAccess lightAccess)
		{
			if (dir == EnumFacing.UP && this.opacity == 0)
				return EnumSkyBlock.SKY.defaultLightValue;

			return super.getMaxNeighborLight(dir, lightAccess);
		}

		@Override
		public int getMaxSpread(final int light)
		{
			if (light == EnumSkyBlock.SKY.defaultLightValue)
				return EnumSkyBlock.SKY.defaultLightValue;

			return super.getMaxSpread(light);
		}

		@Override
		public int getMaxSpread(final EnumFacing dir, final int light)
		{
			if (light == EnumSkyBlock.SKY.defaultLightValue && dir == EnumFacing.DOWN)
				return EnumSkyBlock.SKY.defaultLightValue;

			return super.getMaxSpread(dir, light);
		}

		@Override
		protected int calcSpread(final EnumFacing dir, final int sourceLight, final int targetOpac)
		{
			if (sourceLight == EnumSkyBlock.SKY.defaultLightValue && dir == EnumFacing.DOWN && targetOpac == 0)
				return EnumSkyBlock.SKY.defaultLightValue;

			return super.calcSpread(dir, sourceLight, targetOpac);
		}
	}
}
