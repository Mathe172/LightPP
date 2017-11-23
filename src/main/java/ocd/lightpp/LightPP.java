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

package ocd.lightpp;

import net.minecraftforge.fml.common.Mod;

@Mod(
	modid = LightPP.MOD_ID,
	name = LightPP.MOD_NAME,
	version = LightPP.VERSION
)
public class LightPP
{
	public static final String MOD_ID = "lightpp";
	public static final String MOD_NAME = "Light++";
	public static final String VERSION = "@@MOD_VERSION@@";

	/**
	 * This is the instance of your mod as created by Forge. It will never be null.
	 */
	@Mod.Instance(MOD_ID)
	public static LightPP INSTANCE;
}
