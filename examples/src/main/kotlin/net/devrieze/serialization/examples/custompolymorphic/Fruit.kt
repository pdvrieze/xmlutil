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
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(Fruit.FruitSerializer::class)
sealed class Fruit {
    @OptIn(ExperimentalSerializationApi::class)
    @Serializer(Fruit::class)
    class FruitSerializer: KSerializer<Fruit> {
        override val descriptor: SerialDescriptor = XmlFruit.serializer().descriptor
        override fun deserialize(decoder: Decoder): Fruit = XmlFruit.serializer().deserialize(decoder).toFruit()
        override fun serialize(encoder: Encoder, value: Fruit): Unit = XmlFruit.serializer().serialize(encoder, XmlFruit.fromFruit(value))
    }

    abstract val name: String
}

@Serializable(with = Fruit.FruitSerializer::class)
data class Apple(
    override val name: String,
    val numAppleSeeds: Int
): Fruit()

@Serializable(Fruit.FruitSerializer::class)
data class Tomato(
    override val name: String,
    val color: String
): Fruit()
