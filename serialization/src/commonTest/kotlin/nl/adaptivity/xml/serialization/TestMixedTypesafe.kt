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

@file:Suppress("EXPERIMENTAL_API_USAGE")

package nl.adaptivity.xml.serialization

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import nl.adaptivity.serialutil.MixedContent
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlValue
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
        val actual = xml.encodeToString(TypedMixed.serializer(), data).replace(" />", "/>")
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
        val actual = xml.decodeFromString(TypedMixed.serializer(), data)
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
            serializersModule = TypedMixed.module
            useArrayPolymorphism = true
        }
        val actual = json.encodeToString(TypedMixed.serializer(), data)
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
            useArrayPolymorphism = true
            serializersModule = TypedMixed.module
        }
        val actual = json.decodeFromString(TypedMixed.serializer(), data)
        assertEquals(expected, actual)
    }

    @Test
    fun typesafeMixedWithWhitespace() {
        val data = TypedMixed {
            text("a")
            elem(TypedMixed.E {
                text(" ")
            })
            text("b")
        }
        val expectedXml = "<mixed>a<e> </e>b</mixed>"
        val xml = XML(TypedMixed.module) { autoPolymorphic = true }
        assertXmlEquals(expectedXml, xml.encodeToString(TypedMixed.serializer(), data))

        assertEquals(data, xml.decodeFromString(TypedMixed.serializer(), expectedXml))
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
    class E(@XmlValue(true) override val data: List<MixedContent<EContent>>) :
        TypeMixedBase<EContent>(), TypedMixedContent {

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
                subclass(String::class)
                subclass(B::class)
                subclass(D::class)
                subclass(E::class)
                subclass(G::class)
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

