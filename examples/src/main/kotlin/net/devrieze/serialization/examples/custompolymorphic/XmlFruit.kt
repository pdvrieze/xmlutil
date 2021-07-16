/*
 * Copyright (c) 2020.
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

package net.devrieze.serialization.examples.custompolymorphic

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.devrieze.serialization.examples.custompolymorphic.XmlFruit.FruitType
import nl.adaptivity.xmlutil.core.impl.multiplatform.name

/**
 * A
 * @param type the type of the fruit, a [FruitType] that corresponds to a subclass of [Fruit] (i.e. [Apple] or [Tomato]).
 * @param name see [Fruit.name], required for all fruits
 * @param numAppleSeeds see [Apple.numAppleSeeds], is `null` for [Tomato]es
 * @param color see [Tomato.color], is `null` for [Apple]s
 */
@Serializable
@SerialName("fruit")
data class XmlFruit(
    val type: FruitType,
    val name: String,
    val numAppleSeeds: Int?,
    val color: String?
) {

    companion object {
        /**
         * Convert a [Fruit] to an [XmlFruit].
         * When serializing a [Fruit] is serialized, it is first converted using this function,
         * then serialized using the default serializer of [XmlFruit].
         * @param fruit the [Fruit] to be converted
         * @return the resulting [XmlFruit]
         */
        fun fromFruit(fruit: Fruit): XmlFruit = when (fruit) {
            is Apple  -> XmlFruit(FruitType.APPLE, fruit.name, fruit.numAppleSeeds, null)
            is Tomato -> XmlFruit(FruitType.TOMATO, fruit.name, null, fruit.color)
            // due to Fruit being a sealed class, Kotlin will make sure this `when {}` is exhaustive
        }
    }

    /**
     * Here for each subclass of [Fruit] one enum value is defined.
     * @param serialName the name used in the `type` attribute to discriminate between the different subtypes
     */
    @Serializable(with = FruitTypeSerializer::class)
    enum class FruitType(val serialName: String) {
        APPLE("apple"),
        TOMATO("tomato")
    }

    /**
     * Convert an [XmlFruit] to a [Fruit].
     * When deserializing to a [Fruit], the XML element is first deserialized using the default serializer of [XmlFruit],
     * then this function is used to convert to a [Fruit].
     */
    fun toFruit(): Fruit = when (type) {
        FruitType.APPLE -> {
            require(color == null)
            Apple(name, requireNotNull(numAppleSeeds))
        }
        FruitType.TOMATO -> {
            require(numAppleSeeds == null)
            Tomato(name, requireNotNull(color))
        }
    }

    /**
     * Simple serializer that serializes a [FruitType] as the string in its property [FruitType.serialName].
     */
    @OptIn(ExperimentalSerializationApi::class)
    class FruitTypeSerializer: KSerializer<FruitType> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(FruitTypeSerializer::class.name, PrimitiveKind.STRING)
        override fun deserialize(decoder: Decoder): FruitType = FruitType.values().first { decoder.decodeString() == it.serialName }
        override fun serialize(encoder: Encoder, value: FruitType): Unit = encoder.encodeString(value.serialName)
    }
}
