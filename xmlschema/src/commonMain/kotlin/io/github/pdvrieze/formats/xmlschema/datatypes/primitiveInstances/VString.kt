/*
 * Copyright (c) 2021.
 *
 * This file is part of xmlutil.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import nl.adaptivity.xmlutil.XmlUtilInternal
import kotlin.jvm.JvmInline

@Serializable(VString.Serializer::class)
interface VString : VAnyAtomicType, CharSequence {
    override val length: Int get() = xmlString.length

    override fun get(index: Int): Char = xmlString[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = xmlString.subSequence(startIndex, endIndex)

    @JvmInline
    private value class Inst(override val xmlString: String) : VString

    @OptIn(XmlUtilInternal::class)
    class Serializer: SimpleTypeSerializer<VString>("string") {
        override fun deserialize(decoder: Decoder): VString {
            return Inst(decoder.decodeString())
        }
    }

    companion object {
        operator fun invoke(value: String): VString = Inst(value)
    }
}
