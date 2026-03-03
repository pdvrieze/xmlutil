/*
 * Copyright (c) 2026.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License.  You should have  received a copy of the license
 * with the source distribution. Alternatively, you may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.github.pdvrieze.xml.schematypes.values.instances

import io.github.pdvrieze.xml.schematypes.impl.ListHelper
import io.github.pdvrieze.xml.schematypes.types.HexBinaryType
import io.github.pdvrieze.xml.schematypes.values.XSHexBinary
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.jvm.JvmInline

@JvmInline
@XmlUtilInternal
@OptIn(ExperimentalEncodingApi::class)
value class XSHexBinaryImpl(override val value: ByteArray) : XSHexBinary, ListHelper<Byte> {
    override val xmlString: String get() = Base64.Default.encode(value)

    override fun get(index: Int): Byte = value[index]

    override val size: Int get() = value.size
    override val schemaType: HexBinaryType<XSHexBinary> get() = HexBinaryType.Instance

    override fun toString(): String = xmlString
}
