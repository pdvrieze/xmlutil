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

package io.github.pdvrieze.formats.xmlschema.types

import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.parseQName
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedComplexType
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchemaLike
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.XML

abstract class VQNameListBase<E : VQNameListBase.IElem>(val values: List<E>) : List<E> by values {

    fun contains(name: QName, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean {
        return values.any { it.matches(name, context, schema) }
    }

    abstract fun union(other: VQNameListBase<E>): VQNameListBase<E>
    abstract fun intersection(other: VQNameListBase<E>): VQNameListBase<E>

    interface IElem {
        fun matches(name: QName, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean
    }
    sealed interface AttrElem : IElem
    sealed class Elem : IElem
    object DEFINED : Elem(), AttrElem {
        override fun matches(name: QName, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean {
            return schema.maybeAttribute(name) != null || schema.maybeElement(name) != null
        }
    }

    object DEFINEDSIBLING : Elem() {
        override fun matches(name: QName, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean {
            val ct = context.mdlContentType


            return context.mdlAttributeUses[name]!=null ||
                    (ct is ResolvedComplexType.ElementContentType && ct.mdlParticle.mdlTerm.definesElement(name))
        }
    }

    class Name(val qName: QName) : Elem(), AttrElem {
        override fun matches(name: QName, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean {
            return qName.isEquivalent(name)
        }
    }
}

@Serializable(VQNameList.Serializer::class)
class VQNameList(values: List<Elem>) : VQNameListBase<VQNameListBase.Elem>(values) {

    override fun union(other: VQNameListBase<Elem>): VQNameList {
        val newElems = values.toMutableSet()
        newElems.addAll(other.values)

        return VQNameList(newElems.toList())
    }

    override fun intersection(other: VQNameListBase<Elem>): VQNameList {
        val newElems = values.toMutableSet()
        newElems.retainAll(other.values)

        return VQNameList(newElems.toList())
    }

    constructor() : this(listOf())

    val DEFINED: DEFINED get() = VQNameListBase.DEFINED

    val DEFINEDSIBLING: DEFINEDSIBLING get() = VQNameListBase.DEFINEDSIBLING

    object Serializer : KSerializer<VQNameList> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("List<QName>", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: VQNameList) {
            val e = encoder as? XML.XmlOutput
            val str = value.joinToString(" ") {
                when (it) {
                    DEFINED -> "##defined"
                    DEFINEDSIBLING -> "##definedSibling"
                    is Name -> when (e) {
                        null -> "{${it.qName.namespaceURI}}${it.qName.prefix}:${it.qName.localPart}"
                        else -> e.ensureNamespace(it.qName)
                            .let { q -> if (q.prefix == "") q.localPart else "${q.prefix}:${q.localPart}" }
                    }
                }
            }
            encoder.encodeString(str)
        }

        override fun deserialize(decoder: Decoder): VQNameList {
            val str = decoder.decodeString()
            val d = decoder as? XML.XmlInput
            val items = str.splitToSequence(' ')
                .filter { it.isNotEmpty() }
                .mapTo(mutableListOf()) { elem ->
                    when (elem) {
                        "##defined" -> DEFINED
                        "##definedSibling" -> DEFINEDSIBLING
                        else -> Name(parseQName(d, elem))
                    }
                }
            return VQNameList(items)
        }

    }
}
