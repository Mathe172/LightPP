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

public class NameRef
{
	public static final String BLOCKLIGHT_NAME = "BlockLight";
	public static final String SKYLIGHT_NAME = "SkyLight";
	public static final String LIGHT_DATA_NAME = "LightData";

	public static final String EXTENDED_BLOCK_STORAGE_NAME = "net/minecraft/world/chunk/storage/ExtendedBlockStorage";
	public static final String GET_BLOCK_LIGHT_NAME = "func_76661_k";
	public static final String GET_BLOCK_LIGHT_DESC = "()Lnet/minecraft/world/chunk/NibbleArray;";
	public static final String GET_SKY_LIGHT_NAME = "func_76671_l";
	public static final String GET_SKY_LIGHT_DESC = "()Lnet/minecraft/world/chunk/NibbleArray;";
	public static final String SET_BLOCK_LIGHT_NAME = "func_76659_c";
	public static final String SET_BLOCK_LIGHT_DESC = "(Lnet/minecraft/world/chunk/NibbleArray;)V";
	public static final String SET_SKY_LIGHT_NAME = "func_76666_d";
	public static final String SET_SKY_LIGHT_DESC = "(Lnet/minecraft/world/chunk/NibbleArray;)V";

	public static final String PACKET_BUFFER_NAME = "net/minecraft/network/PacketBuffer";

	public static final String ISERIALIZABLE_NAME = "ocd/lightpp/api/vanilla/world/ISerializable";
}
