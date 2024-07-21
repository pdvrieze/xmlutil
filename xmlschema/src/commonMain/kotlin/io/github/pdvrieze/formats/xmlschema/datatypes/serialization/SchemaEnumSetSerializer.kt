/*
 * Copyright (c) 2023.
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


package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

abstract class AllDerivationSerializerBase<T : VDerivationControl>(name: String, private val kclass: KClass<T>) :
    KSerializer<Set<T>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Set<$name>", PrimitiveKind.STRING)

    private val values: Map<String, T> = listOf(RESTRICTION, EXTENSION, LIST, UNION, SUBSTITUTION)
        .mapNotNull { kclass.safeCast(it) }.associateBy { it.name }

    override fun deserialize(decoder: Decoder): Set<T> {
        return when (val s = decoder.decodeString().trim()) {
            "#all" -> values.values.toSet()
            "" -> emptySet()
            else -> s.split(' ').asSequence().mapTo(HashSet()) {
                values[it] ?: error("Unsupported substitution name: ${s}")
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Set<T>) {
        val s = when {
            value.containsAll(values.values) -> "#all"
            else -> value.joinToString(" ") { it.name }
        }
        encoder.encodeString(s)
    }
}

class BlockSetSerializer: AllDerivationSerializerBase<T_BlockSetValues>("V_BlockSetValues", T_BlockSetValues::class)

class TypeDerivationControlSerializer: AllDerivationSerializerBase<Type>("V_BlockSetValues", Type::class)

class AllDerivationSerializer: AllDerivationSerializerBase<VDerivationControl>("T_DerivationControl", VDerivationControl::class)

class AllDerivationSerializer2 : KSerializer<Set<VDerivationControl>> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Set<T_DeriviationControl>", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Set<VDerivationControl> {
        return when (val s = decoder.decodeString().trim()) {
            "#all" -> setOf(RESTRICTION, EXTENSION, LIST, UNION, SUBSTITUTION)
            "" -> emptySet()
            else -> s.split(' ').asSequence().mapTo(HashSet()) {
                when (it) {
                    RESTRICTION.name -> RESTRICTION
                    EXTENSION.name -> EXTENSION
                    LIST.name -> LIST
                    UNION.name -> UNION
                    SUBSTITUTION.name -> SUBSTITUTION
                    else -> error("Unsupported substitution name: ${s}")
                }
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Set<VDerivationControl>) {
        val s = when (value.size) {
            4 -> "#all"
            else -> value.joinToString(" ") { it.name }
        }
        encoder.encodeString(s)
    }
}

class ComplexDerivationSerializer : KSerializer<Set<Complex>> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Set<T_TypeDerviationControl.ComplexBase>", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Set<Complex> {
        return when (val s = decoder.decodeString().trim()) {
            "#all" -> setOf(RESTRICTION, EXTENSION)
            "" -> emptySet()
            else -> s.split(' ').asSequence().mapTo(HashSet()) {
                when (it) {
                    RESTRICTION.name -> RESTRICTION
                    EXTENSION.name -> EXTENSION
                    else -> error("Unsupported substitution name: ${s}")
                }
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Set<Complex>) {
        val s = when (value.size) {
            2 -> "#all"
            else -> value.joinToString(" ") { it.name }
        }
        encoder.encodeString(s)
    }
}
