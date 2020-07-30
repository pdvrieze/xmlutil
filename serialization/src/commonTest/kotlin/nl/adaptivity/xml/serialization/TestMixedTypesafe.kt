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

package nl.adaptivity.xml.serialization

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.serialutil.impl.name
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlValue
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class TestMixedTypesafe {

//    @Ignore
    @Test
    fun serialize_a_typesafe_mixed_collection_to_xml() {
        val expected = "<mixed>a<b/>c<d/><e>f<g/></e></mixed>"
        val data = TypedMixed {
            text("a")
            elem(TypedMixed.B())
            text("c")
            elem(TypedMixed.D())
            elem(TypedMixed.E {
                text("f")
                elem(TypedMixed.G())
            })
        }
        val xml = XML(TypedMixed.module) { autoPolymorphic = true }
        val actual = xml.stringify(TypedMixed.serializer(), data)
        assertEquals(expected, actual)
    }

//    @Ignore
    @Test
    fun deserialize_a_typesafe_mixed_collection_from_xml() {
        val data = "<mixed>a<b/>c<d/><e>f<g/></e></mixed>"
        val expected = TypedMixed {
            text("a")
            elem(TypedMixed.B())
            text("c")
            elem(TypedMixed.D())
            elem(TypedMixed.E {
                text("f")
                elem(TypedMixed.G())
            })
        }
        val xml = XML(TypedMixed.module) { autoPolymorphic = true }
        val actual = xml.parse(TypedMixed.serializer(), data)
        assertEquals(expected, actual)
    }

//    @Ignore
    @Test
    fun serialize_a_typesafe_mixed_collection_to_json() {
        val expected = """{"data":[["kotlin.String","a"],["b",{}],["kotlin.String","c"],["d",{}],["e",{"data":[["kotlin.String","f"],["g",{}]]}]]}"""
        val data = TypedMixed {
            text("a")
            elem(TypedMixed.B())
            text("c")
            elem(TypedMixed.D())
            elem(TypedMixed.E {
                text("f")
                elem(TypedMixed.G())
            })
        }
        val json = Json {
            serialModule = TypedMixed.module
        }
        val actual = json.stringify(TypedMixed.serializer(), data)
        assertEquals(expected, actual)
    }

//    @Ignore
    @Test
    fun deserialize_a_typesafe_mixed_collection_from_json() {
        val data = """{"data":[["kotlin.String","a"],["b",{}],["kotlin.String","c"],["d",{}],["e",{"data":[["kotlin.String","f"],["g",{}]]}]]}"""
        val expected = TypedMixed {
            text("a")
            elem(TypedMixed.B())
            text("c")
            elem(TypedMixed.D())
            elem(TypedMixed.E {
                text("f")
                elem(TypedMixed.G())
            })
        }
        val json = Json {
            serialModule = TypedMixed.module
        }
        val actual = json.parse(TypedMixed.serializer(), data)
        assertEquals(expected, actual)
    }
}

internal interface TypedMixedContent
internal interface EContent

@Serializable
@SerialName("mixed")
internal class TypedMixed(
    @XmlValue(true)
    override val data: List<MixedContent<TypedMixedContent>>
                         ) : TypeMixedBase<TypedMixedContent>() {
    constructor(config: TypeMixedBase.Builder<TypedMixedContent>.() -> Unit)
            : this(TypeMixedBase.Builder<TypedMixedContent>().apply(config).toList())

    @Serializable
    @SerialName("b")
    data class B(@Transient val dummy: Int = 0) : TypedMixedContent

    @Serializable
    @SerialName("d")
    data class D(@Transient val dummy: Int = 0) : TypedMixedContent

    @Serializable
    @SerialName("e")
    class E(@XmlValue(true) override val data: List<MixedContent<EContent>>) : TypeMixedBase<EContent>(),
                                                                               TypedMixedContent {
        constructor(config: TypeMixedBase.Builder<EContent>.() -> Unit)
                : this(TypeMixedBase.Builder<EContent>().apply(config).toList())

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is E) return false

            if (data != other.data) return false

            return true
        }

        override fun hashCode(): Int {
            return data.hashCode()
        }

        override fun toString(): String {
            return "E($data)"
        }

    }

    @Serializable
    @SerialName("g")
    data class G(@Transient val dummy: Int = 0) : EContent

    companion object {
        val module = SerializersModule {
            polymorphic(Any::class) {
                B::class with B.serializer()
                D::class with D.serializer()
                E::class with E.serializer()
                G::class with G.serializer()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypedMixed) return false

        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun toString(): String {
        return "TypedMixed($data)"
    }
}

@Serializable
abstract class TypeMixedBase<T>() {
    //    constructor(config: Builder.()->Unit): this(Builder().a)
    abstract val data: List<MixedContent<T>>

    open class Builder<T>() {
        private val content = mutableListOf<MixedContent<T>>()
        fun toList(): List<MixedContent<T>> {
            return content
        }

        fun text(value: CharSequence) {
            content.add(MixedContent.Text(value.toString()))
        }

        fun elem(value: T) {
            content.add(MixedContent.Object(value))
        }

    }
}

@Serializable(with = MixedContent.Companion::class)
sealed class MixedContent<out T> {
    @Serializable(with = Text.Companion::class)
    class Text(val data: String) : MixedContent<Nothing>() {



        companion object : KSerializer<Text> {
            override val descriptor: SerialDescriptor = PrimitiveDescriptor("text", PrimitiveKind.STRING)
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

            override val descriptor: SerialDescriptor = SerialDescriptor("Object", PolymorphicKind.OPEN) {
                element("type", String.serializer().descriptor)
                element("value", SerialDescriptor("Object_T", UnionKind.CONTEXTUAL))
            }

            override fun deserialize(decoder: Decoder): Object<Any> {
                return Object(delegate.deserialize(decoder))
            }

            override fun serialize(encoder: Encoder, value: Object<Any>): Unit = encoder.encodeStructure(descriptor) {
                val serializer: KSerializer<Any> = findPolymorphicSerializer(encoder, value.data)
                encodeStringElement(MixedContent.descriptor, 0, serializer.descriptor.serialName)
                encodeSerializableElement(MixedContent.descriptor, 1, serializer, value.data)
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

    companion object : KSerializer<MixedContent<Any>> {
        override val descriptor: SerialDescriptor = SerialDescriptor("Object<T>", PolymorphicKind.OPEN) {
            element("type", String.serializer().descriptor)
            element("value", SerialDescriptor("Object<${Any::class.name}>", UnionKind.CONTEXTUAL))
        }

        fun decodeSequentially(compositeDecoder: CompositeDecoder): MixedContent<Any> {
            val klassName = compositeDecoder.decodeStringElement(descriptor, 0)
            val value: Any = when (klassName) {
                "kotlin.String" -> compositeDecoder.decodeStringElement(descriptor, 1)
                else            -> {
                    val serializer = findPolymorphicSerializer(compositeDecoder, klassName)
                    compositeDecoder.decodeSerializableElement(descriptor, 1, serializer)
                }
            }
            compositeDecoder.endStructure(descriptor)
            return Object(value)
        }

        override fun deserialize(decoder: Decoder): MixedContent<Any> = decoder.decodeStructure(descriptor) {
            if (decodeSequentially()) return@decodeStructure decodeSequentially(this)

            var klassName: String? = null
            var value: MixedContent<Any>? = null
            mainLoop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.READ_DONE -> break@mainLoop
                    0                          -> klassName = decodeStringElement(descriptor, index)
                    1                          -> {
                        klassName = requireNotNull(klassName) { "Can not read polymorphic value before its type" }
                        when (klassName) {
                            "kotlin.String" -> value = Text(decodeStringElement(descriptor, index))
                            else            -> {
                                val serializer = findPolymorphicSerializer(this, klassName)
                                value = Object(decodeSerializableElement(descriptor, index, serializer))
                            }
                        }
                    }
                    else                       -> throw SerializationException("Unexpected index in deserialization")
                }
            }
            requireNotNull(value) { "No value was provided" }
        }

        override fun serialize(encoder: Encoder, value: MixedContent<Any>) {
            // TODO maybe special case XML

            when (value) {
                is Text        -> encoder.encodeStructure(descriptor) {
                    encodeStringElement(descriptor, 0, "kotlin.String")
                    encodeStringElement(descriptor, 1, value.data)
                }
                is Object<Any> -> encoder.encodeStructure(descriptor) {
                    val serializer: KSerializer<Any> = findPolymorphicSerializer(encoder, value.data)
                    encodeStringElement(descriptor, 0, serializer.descriptor.serialName)
                    encodeSerializableElement(descriptor, 1, serializer, value.data)
                }
            }

        }

        private fun findPolymorphicSerializer(
            compositeDecoder: CompositeDecoder,
            klassName: String
                                             ): KSerializer<out Any> {
            return compositeDecoder.context.getPolymorphic(Any::class, serializedClassName = klassName)
                ?: throw SerializationException("No matching serializer found for type name $klassName extending Any")
        }

        private fun findPolymorphicSerializer(
            encoder: Encoder,
            value: Any
                                             ): KSerializer<Any> {
            @Suppress("UNCHECKED_CAST")
            return (encoder.context.getPolymorphic(Any::class, value)
                ?: throw SerializationException("No matching serializer found for type name ${value::class} extending Any")) as KSerializer<Any>
        }
    }
}