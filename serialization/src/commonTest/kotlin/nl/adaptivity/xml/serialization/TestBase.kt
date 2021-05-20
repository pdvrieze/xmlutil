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

package nl.adaptivity.xml.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy
import kotlin.test.Test
import kotlin.test.assertEquals

expect fun assertXmlEquals(expected: String, actual:String)

abstract class TestBase<T> constructor(
    val value: T,
    val serializer: KSerializer<T>,
    val serializersModule: SerializersModule = EmptySerializersModule,
    protected val baseXmlFormat: XML = XML(serializersModule) {
        policy = DefaultXmlSerializationPolicy(
            pedantic = true,
            encodeDefault = XmlSerializationPolicy.XmlEncodeDefault.ANNOTATED
                                              )
    },
    private val baseJsonFormat: Json = Json {
        defaultJsonTestConfiguration()
        this.serializersModule = serializersModule
    }
                                      ) {
    abstract val expectedXML: String
    abstract val expectedJson: String

    fun serializeXml(): String = baseXmlFormat.encodeToString(serializer, value).normalizeXml()

    fun serializeJson(): String = baseJsonFormat.encodeToString(serializer, value)

    @Test
    open fun testSerializeXml() {
        assertXmlEquals(expectedXML, serializeXml())
    }

    @Test
    open fun testDeserializeXml() {
        assertEquals(value, baseXmlFormat.decodeFromString(serializer, expectedXML))
    }

    @Test
    open fun testSerializeJson() {
        assertEquals(expectedJson, serializeJson())
    }

    @Test
    open fun testDeserializeJson() {
        assertEquals(value, baseJsonFormat.decodeFromString(serializer, expectedJson))
    }

}


abstract class TestPolymorphicBase<T>(
    value: T,
    serializer: KSerializer<T>,
    serializersModule: SerializersModule,
    baseJsonFormat: Json = Json{
        defaultJsonTestConfiguration()
        this.serializersModule = serializersModule
    }
                                     ) :
    TestBase<T>(value, serializer, serializersModule, XML(serializersModule) { autoPolymorphic = true }, baseJsonFormat) {

    abstract val expectedNonAutoPolymorphicXML: String

    @Test
    fun nonAutoPolymorphic_serialization_should_work() {
        val serialized =
            XML(serializersModule = serializersModule) { autoPolymorphic = false }.encodeToString(serializer, value)
                .normalizeXml()
        assertEquals(expectedNonAutoPolymorphicXML, serialized)
    }

    @Test
    fun nonAutoPolymorphic_deserialization_should_work() {
        val actualValue = XML(serializersModule = serializersModule) { autoPolymorphic = false }
            .decodeFromString(serializer, expectedNonAutoPolymorphicXML)

        assertEquals(value, actualValue)
    }

}
