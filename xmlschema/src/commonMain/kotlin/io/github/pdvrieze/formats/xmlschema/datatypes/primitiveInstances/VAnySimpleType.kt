/*
 * Copyright (c) 2021-2026.
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

package io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances

import io.github.pdvrieze.xml.schematypes.values.XsdAnySimple
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlUtilInternal

typealias VAnySimpleType = XsdAnySimple

@Serializable(VAnySimpleTypeX.Serializer::class)
interface VAnySimpleTypeX {
    // inherits any
    val xmlString: String

    private class Inst(val value: String) : VAnySimpleTypeX {
        override val xmlString: String get() = value

        override fun toString(): String = xmlString

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Inst

            return value == other.value
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }


    }

    @OptIn(XmlUtilInternal::class)
    class Serializer : SimpleVTypeSerializer<VAnySimpleTypeX>("anySimpleType") {
        override fun deserialize(raw:String, input: XmlReader?): VAnySimpleTypeX {
            val cpos = raw.indexOf(':')

            if (cpos > 0 && raw.indexOf(':', cpos + 1) < 0 && input != null) {

                val prefix = raw.substring(0, cpos)
                val ns = input.namespaceContext.getNamespaceURI(prefix)
                if (ns != null) {
                    val localName = raw.substring(cpos + 1)
                    return VPrefixString(ns, prefix, localName)
                }
            }

            return Inst(raw)
        }
    }
}
