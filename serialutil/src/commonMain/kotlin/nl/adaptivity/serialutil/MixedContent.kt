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

package nl.adaptivity.serialutil

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = MixedContent.Companion::class)
sealed class MixedContent<out T> {
    @Serializable(with = Text.Companion::class)
    class Text(val data: String) : MixedContent<Nothing>() {

        companion object : KSerializer<Text> {
            override val descriptor: SerialDescriptor = serialDescriptor<String>()

            override fun deserialize(decoder: Decoder): Text {
                return Text(decoder.decodeString())
            }

            override fun serialize(encoder: Encoder, value: Text) {
                encoder.encodeString(value.data)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Text) return false

            if (data != other.data) return false

            return true
        }

        override fun hashCode(): Int {
            return data.hashCode()
        }

        override fun toString(): String {
            return "Text('$data')"
        }
    }

    @Serializable(Object.Companion::class)
    class Object<T>(@Polymorphic val data: T) : MixedContent<T>() {

        companion object : KSerializer<Object<Any>> {
            private val delegate = PolymorphicSerializer(Any::class)

            /* Note that this descriptor delegates. This works around the issue that is not possible to */
            override val descriptor: SerialDescriptor get() = delegate.descriptor

            override fun deserialize(decoder: Decoder): Object<Any> {
                return Object(delegate.deserialize(decoder))
            }

            override fun serialize(encoder: Encoder, value: Object<Any>) {
                delegate.serialize(encoder, value.data)
            }

        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Object<*>) return false

            if (data != other.data) return false

            return true
        }

        override fun hashCode(): Int {
            return data?.hashCode() ?: 0
        }

        override fun toString(): String {
            return "Object($data)"
        }
    }

    // TODO make this serializer actually parameterized by the content type.
    companion object :
        KSerializer<MixedContent<Any>> {

        private val delegate = PolymorphicSerializer(Any::class)

        /* Note that this descriptor delegates. This works around the issue that is not possible to create
         * a custom descriptor that carries base class information.
         */

        override val descriptor: SerialDescriptor get() = delegate.descriptor

        fun decodeSequentially(compositeDecoder: CompositeDecoder): MixedContent<Any> {
            val klassName = compositeDecoder.decodeStringElement(descriptor, 0)
            val value: Any = when (klassName) {
                "kotlin.String" -> compositeDecoder.decodeStringElement(descriptor, 1)
                else            -> {
                    val serializer =
                        findPolymorphicSerializer(
                            compositeDecoder,
                            klassName
                                                                                                                    )
                    compositeDecoder.decodeSerializableElement(descriptor, 1, serializer)
                }
            }
            compositeDecoder.endStructure(descriptor)
            return Object(value)
        }

        override fun deserialize(decoder: Decoder): MixedContent<Any> {
            return when (val value = delegate.deserialize(decoder)) {
                is String -> Text(value)
                else -> Object(value)
            }
        }

        override fun serialize(encoder: Encoder, value: MixedContent<Any>) {
            val delegateValue = when (value) {
                is Text -> value.data
                is Object -> value.data
            }
            delegate.serialize(encoder, delegateValue)
        }

        @OptIn(ExperimentalSerializationApi::class)
        private fun findPolymorphicSerializer(
            compositeDecoder: CompositeDecoder,
            klassName: String
                                             ): DeserializationStrategy<out Any> {
            return compositeDecoder.serializersModule.getPolymorphic(Any::class, serializedClassName = klassName)
                ?: throw SerializationException("No matching serializer found for type name $klassName extending Any")
        }

        @OptIn(ExperimentalSerializationApi::class)
        private fun findPolymorphicSerializer(
            encoder: Encoder,
            value: Any
                                             ): KSerializer<Any> {
            @Suppress("UNCHECKED_CAST")
            return (encoder.serializersModule.getPolymorphic(Any::class, value)
                ?: throw SerializationException("No matching serializer found for type name ${value::class} extending Any")) as KSerializer<Any>
        }
    }
}