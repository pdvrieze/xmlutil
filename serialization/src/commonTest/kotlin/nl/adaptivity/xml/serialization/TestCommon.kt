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

@file:OptIn(ExperimentalSerializationApi::class)
@file:MustUseReturnValues

package nl.adaptivity.xml.serialization

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.test.multiplatform.Target
import nl.adaptivity.xmlutil.test.multiplatform.testTarget
import nl.adaptivity.xmlutil.util.impl.createDocument
import kotlin.test.Test
import kotlin.test.assertEquals

fun String.normalizeXml(): String = replace(" />", "/>")
    .replace(" ?>", "?>")
    .replace("\r\n", "\n")
    .replace("&gt;", ">")

fun JsonBuilder.defaultJsonTestConfiguration() {
    isLenient = true
    allowSpecialFloatingPointValues = true
    encodeDefaults = true
}


@OptIn(ExperimentalXmlUtilApi::class)
class TestCommon {

    @Test
    fun testEmitXmlDeclFull() {
        val expectedXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <prefix:model xmlns:prefix="namespace" version="0.0.1" anAttribute="attrValue">
                <prefix:anElement>elementValue</prefix:anElement>
                <prefix:aBlankElement/>
            </prefix:model>""".trimIndent()

        val model = SampleModel1("0.0.1", "attrValue", "elementValue")

        val format = XML.v1 {
            xmlVersion = XmlVersion.XML10
            xmlDeclMode = XmlDeclMode.Charset
            setIndent(4)
        }

        val serializedModel = format.encodeToString(SampleModel1.serializer(), model).normalizeXml().replace('\'', '"')

        assertEquals(expectedXml, serializedModel)

        val deserializedModel = format.decodeFromString<SampleModel1>(expectedXml)
        assertEquals(model, deserializedModel)
    }

    @Test
    fun testEmitXmlDeclMinimal() {
        val expectedXml = """
            <?xml version="1.0"?>
            <prefix:model xmlns:prefix="namespace" version="0.0.1" anAttribute="attrValue">
            <!--i--><prefix:anElement>elementValue</prefix:anElement>
            <!--i--><prefix:aBlankElement/>
            </prefix:model>""".trimIndent()

        val model = SampleModel1("0.0.1", "attrValue", "elementValue")

        val format = XML.v1 {
            xmlVersion = XmlVersion.XML10
            xmlDeclMode = XmlDeclMode.Minimal
            indentString = "<!--i-->"
        }

        val serializedModel = format.encodeToString(SampleModel1.serializer(), model).normalizeXml().replace('\'', '"')

        assertEquals(expectedXml, serializedModel)
    }

    @Test
    fun deserialize_mixed_content_from_xml() {
        val contentText = "<tag>some text <b>some bold text<i>some bold italic text</i></b></tag>"
        val expectedObj = Tag(listOf("some text ", B("some bold text", I("some bold italic text"))))

        val xml = XML.v1(Tag.module)
        val deserialized = xml.decodeFromString(Tag.serializer(), contentText)

        assertEquals(expectedObj, deserialized)
    }


    @Test
    fun serializeIntList() {
        val data = IntList(listOf(1, 2, 3, 4))
        val expectedXml = "<IntList><values>1</values><values>2</values><values>3</values><values>4</values></IntList>"
        val xml = XML.v1.compact()
        val serializer = IntList.serializer()
        val serialized = xml.encodeToString(serializer, data)
        assertEquals(expectedXml, serialized)

        val deserialized = xml.decodeFromString(serializer, serialized)
        assertEquals(data, deserialized)
    }

    @Test
    fun serialize_mixed_content_to_xml() {
        val contentText = "<tag>some text <b>some bold text<i>some bold italic text</i></b></tag>"
        val expectedObj = Tag(listOf("some text ", B("some bold text", I("some bold italic text"))))

        val xml = XML.v1.compact(Tag.module)

        val serialized = xml.encodeToString(Tag.serializer(), expectedObj)
        assertEquals(contentText, serialized)
    }


    @Test
    fun deserializeXmlWithEntity() {
        val xml = XML.v1 {
            repairNamespaces = true
            policy {
                autoPolymorphic = false
            }
        }

        val expected = StringWithMarkup("Chloroacetic acid, >=99% < 100%")

        val actual = xml.decodeFromString<StringWithMarkup>(
            "<StringWithMarkup xmlns=\"https://pubchem.ncbi.nlm.nih.gov/pug_view\">\n" +
                    "    <String>Chloroacetic acid, &gt;=99% &lt; 100%</String>\n" +
                    "</StringWithMarkup>"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun deserializeToElementXmlWithEntity() {
        if (testTarget == Target.Node) return

        val xml = XML.v1 {
            repairNamespaces = true
            policy {
                autoPolymorphic = false
            }
        }

//        val expected = StringWithMarkup("Chloroacetic acid, >=99% < 100%")

        val actual = xml.decodeFromString(
            Element.serializer(),
            "<StringWithMarkup xmlns=\"https://pubchem.ncbi.nlm.nih.gov/pug_view\">\n" +
                    "    <String>Chloroacetic acid, &gt;=99% &lt; 100%</String>\n" +
                    "</StringWithMarkup>"
        )

        val doc = createDocument(QName("https://pubchem.ncbi.nlm.nih.gov/pug_view", "StringWithMarkup"))
        val expected = doc.getDocumentElement()!!.also { stringWithMarkup ->
            doc.createElement("String").also { string ->
                stringWithMarkup.appendChild(string)
                string.appendChild(doc.createTextNode("Chloroacetic acid, >=99% < 100%"))
            }
        }

        assertDomEquals(expected, actual)
    }

    @Test
    fun serialize_issue121() {
        @Suppress("DEPRECATION")
        serialize_issue121(XML.compat.instance)
    }

    @Test
    fun serialize_issue121_1_0() {
        serialize_issue121(XML.v1 { xmlDeclMode = XmlDeclMode.None })
    }

    private fun serialize_issue121(format: XML) {
        val data = StringHolder("\u26a0\ufe0f")
        val expected = "<StringHolder>⚠️</StringHolder>"
        assertEquals(expected, format.encodeToString(data))
    }

    @Test
    fun serializeIndependent_issue121() {
        @Suppress("DEPRECATION")
        serializeIndependent_issue121(XML.compat.instance)
    }

    @Test
    fun serializeIndependent_issue121_1_0() {
        serializeIndependent_issue121(XML.v1.compact())
    }

    private fun serializeIndependent_issue121(format: XML) {
        val data = StringHolder("⚠️"/*"\u26a0\ufe0f"*/)
        val expected = "<StringHolder>⚠️</StringHolder>"
        val actual = StringWriter().also { sw ->
            KtXmlWriter(sw, xmlDeclMode = XmlDeclMode.None).use { out ->
                format.encodeToWriter(out, data)
            }
        }.toString()
        assertEquals(expected, actual)
    }

    @Test
    fun serializeEmoji() {
        @Suppress("DEPRECATION")
        serializeEmoji(XML.compat.instance)
    }

    @Test
    fun serializeEmoji_1_0() {
        serializeEmoji(XML.v1.compact())
    }

    private fun serializeEmoji(format: XML) {
        val data = StringHolder("\uD83D\uDE0A")
        val expected = "<StringHolder>😊</StringHolder>"
        assertEquals(expected, format.encodeToString(data))
    }

    @Test
    fun serializeEmojiIndependent() {
        @Suppress("DEPRECATION")
        serializeEmojiIndependent(XML.compat.instance)
    }

    @Test
    fun serializeEmojiIndependent1_0() {
        serializeEmojiIndependent(XML.v1.compact())
    }

    private fun serializeEmojiIndependent(format: XML) {
        val data = StringHolder("\uD83D\uDE0A")
        val expected = "<StringHolder>😊</StringHolder>"
        val actual = StringWriter().also { sw ->
            KtXmlWriter(sw, xmlDeclMode = XmlDeclMode.None).use { out ->
                format.encodeToWriter(out, data)
            }
        }.toString()
        assertEquals(expected, actual)
    }

    @Test
    fun serializeRawEmoji() {
        val data = StringHolder("😊")
        val expected = "<StringHolder>😊</StringHolder>"
        assertEquals(expected, XML.v1.compact().encodeToString(data))
    }

    @Test
    fun deserializeEmoji() {
        val xml = "<StringHolder>😊</StringHolder>"
        val deserialized = XML.v1.decodeFromString<StringHolder>(xml)
        assertEquals("\uD83D\uDE0A", deserialized.value)
    }

    @Test
    fun deserializeEmojiEntity() {
        val xml = "<StringHolder>&#x1F60A;</StringHolder>"
        val deserialized = XML.v1.decodeFromString<StringHolder>(xml)
        assertEquals("😊", deserialized.value)
    }

    @Test
    fun serializeXmlWithEntity() {
        @Suppress("DEPRECATION")
        val xml = XML.compat {
            repairNamespaces = true
            defaultPolicy {
                pedantic = false
                autoPolymorphic = false
            }
        }

        val data = StringWithMarkup("Chloroacetic acid, >=99% < 100%")

        val expected =
            "<StringWithMarkup xmlns=\"https://pubchem.ncbi.nlm.nih.gov/pug_view\">" +
                    "<String>Chloroacetic acid, >=99% &lt; 100%</String>" +
                    "</StringWithMarkup>"

        val actual = xml.encodeToString(StringWithMarkup.serializer(), data).replace("&gt;", ">")

        assertEquals(expected, actual)
    }


    @XmlSerialName("StringWithMarkup", "https://pubchem.ncbi.nlm.nih.gov/pug_view", "")
    @Serializable
    data class StringWithMarkup(
        @XmlElement(true) @SerialName("String") val string: String = "",
        val markup: List<String> = emptyList()
    )

    @Serializable
    data class IntList(val values: List<Int>)

    @Serializable
    @XmlSerialName("model", "namespace", "prefix")
    data class SampleModel1(
        val version: String,
        val anAttribute: String,
        @XmlElement(true)
        val anElement: String,
        @XmlElement(true)
        val aBlankElement: Unit? = Unit
    )

    @Serializable
    internal data class StringHolder(
        @XmlValue
        val value: String
    )

    @Serializable
    @SerialName("b")
    internal data class B(
        @XmlValue(true)
        val data: List<@Polymorphic Any>
    ) {
        constructor(vararg data: Any) : this(data.toList())

    }

    @Serializable
    @SerialName("tag")
    internal data class Tag(
        @XmlValue(true)
        val data: List<@Polymorphic Any>
    ) {

        constructor(vararg data: Any) : this(data.toList())

        companion object {
            val module = SerializersModule {
                polymorphic(Any::class) {
                    subclass(String::class)
                    subclass(B::class)
                    subclass(I::class)
                }
            }
        }

    }

    @Serializable
    @SerialName("i")
    internal data class I(
        @XmlValue(true)
        val data: List<@Polymorphic Any>
    ) {
        constructor(vararg data: Any) : this(data.toList())

    }

    @Serializable
    @XmlSerialName("myObject", "mynamespace", "o")
    object MyObjectInCommon {
        val bar = "baz"
    }


    @Serializable
    data class Container(val data: MyObjectInCommon)

    @Test
    fun testSerializeObject() {
        val xml  = XML.v1()
        val data = Container(MyObjectInCommon)
        val expected = "<Container><o:myObject xmlns:o=\"mynamespace\"/></Container>"
        assertXmlEquals(expected, xml.encodeToString(data))
    }

    @Test
    fun testDeserializeObject() {
        val xml  = XML.v1()
        val expected = Container(MyObjectInCommon)
        val data = "<Container><o:myObject xmlns:o=\"mynamespace\"/></Container>"
        assertEquals(expected, xml.decodeFromString<Container>(data))
    }
}
