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

@file:OptIn(ExperimentalXmlUtilApi::class, ExperimentalSerializationApi::class)

package nl.adaptivity.xml.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.XmlStreaming
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.copy
import kotlin.test.Test
import kotlin.test.assertEquals

expect fun assertXmlEquals(expected: String, actual:String)

internal fun defaultXmlFormat(serializersModule: SerializersModule) = XML(serializersModule) {
    policy = DefaultXmlSerializationPolicy(
        pedantic = true,
        encodeDefault = XmlSerializationPolicy.XmlEncodeDefault.ANNOTATED
    )
}

internal fun defaultJsonFormat(serializersModule: SerializersModule) = Json {
    defaultJsonTestConfiguration()
    this.serializersModule = serializersModule
}

expect abstract class PlatformXmlTestBase<T> constructor(
    value: T,
    serializer: KSerializer<T>,
    serializersModule: SerializersModule = EmptySerializersModule,
    baseXmlFormat: XML = defaultXmlFormat(serializersModule)
) : XmlTestBase<T>

abstract class XmlTestBase<T>(
    val value: T,
    val serializer: KSerializer<T>,
    val serializersModule: SerializersModule = EmptySerializersModule,
    protected val baseXmlFormat: XML = XML(serializersModule) {
        policy = DefaultXmlSerializationPolicy(
            pedantic = true,
            encodeDefault = XmlSerializationPolicy.XmlEncodeDefault.ANNOTATED
        )
    }
) {
    abstract val expectedXML: String

    fun serializeXml(): String = baseXmlFormat.encodeToString(serializer, value).normalizeXml()

    @Test
    open fun testSerializeXml() {
        assertXmlEquals(expectedXML, serializeXml())
    }

    @Test
    open fun testGenericSerializeXml() {
        val stringBuilder = StringBuilder()
        val writer = XmlStreaming.newGenericWriter(stringBuilder)
        baseXmlFormat.encodeToWriter(writer, serializer, value)
        assertXmlEquals(expectedXML, stringBuilder.toString().normalizeXml())
    }

    @Test
    open fun testDeserializeXml() {
        assertEquals(value, baseXmlFormat.decodeFromString(serializer, expectedXML))
    }

    @Test
    open fun testGenericDeserializeXml() {
        val reader = XmlStreaming.newGenericReader(expectedXML)
        assertEquals(value, baseXmlFormat.decodeFromReader(serializer, reader))
    }

}

expect abstract class PlatformTestBase<T>(
    value: T,
    serializer: KSerializer<T>,
    serializersModule: SerializersModule = EmptySerializersModule,
    baseXmlFormat: XML = defaultXmlFormat(serializersModule),
    baseJsonFormat: Json = defaultJsonFormat(serializersModule)
) : TestBase<T>

abstract class TestBase<T> constructor(
    value: T,
    serializer: KSerializer<T>,
    serializersModule: SerializersModule = EmptySerializersModule,
    baseXmlFormat: XML = XML(serializersModule) {
        policy = DefaultXmlSerializationPolicy(
            pedantic = true,
            encodeDefault = XmlSerializationPolicy.XmlEncodeDefault.ANNOTATED
        )
    },
    private val baseJsonFormat: Json = Json {
        defaultJsonTestConfiguration()
        this.serializersModule = serializersModule
    }
) : XmlTestBase<T>(value, serializer, serializersModule, baseXmlFormat) {
    abstract val expectedJson: String

    fun serializeJson(): String = baseJsonFormat.encodeToString(serializer, value)

    @Test
    open fun testSerializeJson() {
        assertEquals(expectedJson, serializeJson())
    }

    @Test
    open fun testDeserializeJson() {
        assertEquals(value, baseJsonFormat.decodeFromString(serializer, expectedJson))
    }

}

expect abstract class PlatformTestPolymorphicBase<T>(
    value: T,
    serializer: KSerializer<T>,
    serializersModule: SerializersModule = EmptySerializersModule,
    baseJsonFormat: Json = defaultJsonFormat(serializersModule)
) : TestPolymorphicBase<T>

abstract class TestPolymorphicBase<T>(
    value: T,
    serializer: KSerializer<T>,
    serializersModule: SerializersModule,
    baseJsonFormat: Json = Json {
        defaultJsonTestConfiguration()
        this.serializersModule = serializersModule
    }
) : TestBase<T>(
    value,
    serializer,
    serializersModule,
    XML(serializersModule) { autoPolymorphic = true },
    baseJsonFormat
) {

    abstract val expectedNonAutoPolymorphicXML: String

    abstract val expectedXSIPolymorphicXML: String

    @Test
    open fun nonAutoPolymorphic_serialization_should_work() {
        val serialized =
            XML(serializersModule = serializersModule) { autoPolymorphic = false }.encodeToString(serializer, value)
                .normalizeXml()
        assertXmlEquals(expectedNonAutoPolymorphicXML, serialized)
    }

    @Test
    open fun nonAutoPolymorphic_deserialization_should_work() {
        val actualValue = XML(serializersModule = serializersModule) { autoPolymorphic = false }
            .decodeFromString(serializer, expectedNonAutoPolymorphicXML)

        assertEquals(value, actualValue)
    }

    @Test
    open fun xsi_serialization_should_work() {
        val serialized =
            XML(serializersModule = serializersModule) {
                autoPolymorphic = false
                policy = DefaultXmlSerializationPolicy(false, typeDiscriminatorName = xsiType)
            }.encodeToString(serializer, value)
                .normalizeXml()
        assertXmlEquals(expectedXSIPolymorphicXML, serialized)
    }

    @Test
    open fun xsi_deserialization_should_work() {
        val actualValue = XML(serializersModule = serializersModule) {
            autoPolymorphic = false
            policy = DefaultXmlSerializationPolicy(false, typeDiscriminatorName = xsiType)
        }.decodeFromString(serializer, expectedXSIPolymorphicXML)

        assertEquals(value, actualValue)
    }

    @Test
    open fun attribute_discriminator_deserialization_should_work() {
        val modifiedXml = expectedXSIPolymorphicXML.replace(XMLConstants.XSI_NS_URI, "urn:notquitexsi")
        val actualValue = XML(serializersModule = serializersModule) {
            autoPolymorphic = false
            policy = DefaultXmlSerializationPolicy(false, typeDiscriminatorName = xsiType.copy(namespaceURI = "urn:notquitexsi"))
        }.decodeFromString(serializer, modifiedXml)

        assertEquals(value, actualValue)
    }

    @Test
    open fun xsi_deserialization_should_work_implicitly() {
        val actualValue = XML(serializersModule = serializersModule) {
            autoPolymorphic = false
            policy = DefaultXmlSerializationPolicy(false, typeDiscriminatorName = xsiType)
        }.decodeFromString(serializer, expectedXSIPolymorphicXML)

        assertEquals(value, actualValue)
    }


    companion object {
        val xsiType = QName(XMLConstants.XSI_NS_URI, "type", XMLConstants.XSI_PREFIX)
    }
}
