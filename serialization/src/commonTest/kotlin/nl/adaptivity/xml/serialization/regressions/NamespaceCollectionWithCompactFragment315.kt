/*
 * Copyright (c) 2025.
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

package nl.adaptivity.xml.serialization.regressions

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


class NamespaceCollectionWithCompactFragment315 {

    private lateinit var baseXmlConfig: XML

    @BeforeTest
    fun initXml() {

        baseXmlConfig = XML {
            recommended_0_91_0 { pedantic = true }

            defaultToGenericParser = true // consistent output

            indentString = " ".repeat(2)
            xmlVersion = XmlVersion.XML10
        }
    }



    @Test
    fun `test deserialize with ns decl on leaf elements`() {
        val input =
            """
      <?xml version='1.0' ?>
      <foo:rootElement xmlns:foo="http://example.com/foo" someAttribute="hello">
        <foo:childElement>
          <bar:thing1 xmlns:bar="http://example.com/bar">test</bar:thing1>
          <bar:thing2 xmlns:bar="http://example.com/bar">test</bar:thing2>
        </foo:childElement>
        <foo:childElement>
          <bar:thing3 xmlns:bar="http://example.com/bar">test</bar:thing3>
        </foo:childElement>
      </foo:rootElement>
    """
                .trimIndent()

        val rootElement = baseXmlConfig.decodeFromString<RootElement>(input, rootName = null)

        assertEquals("hello", rootElement.someAttribute)

        assertEquals<List<CompactFragment>>(
            listOf(
                CompactFragment("<bar:thing1 xmlns:bar=\"http://example.com/bar\">test</bar:thing1>"),
                CompactFragment("<bar:thing2 xmlns:bar=\"http://example.com/bar\">test</bar:thing2>"),
                CompactFragment("<bar:thing3 xmlns:bar=\"http://example.com/bar\">test</bar:thing3>"),
            ), rootElement.childElements.flatMap { it.list })
    }

    @Test
    fun `test deserialize with ns decl on root element`() {
        val input =
            """
      <?xml version='1.0' ?>
      <foo:rootElement xmlns:foo="http://example.com/foo" xmlns:bar="http://example.com/bar" someAttribute="hello">
        <foo:childElement>
          <bar:thing1>test</bar:thing1>
          <bar:thing2>test</bar:thing2>
        </foo:childElement>
        <foo:childElement>
          <bar:thing3>test</bar:thing3>
        </foo:childElement>
      </foo:rootElement>
    """
                .trimIndent()

        val rootElement = baseXmlConfig.decodeFromString<RootElement>(input, rootName = null)


        assertEquals("hello", rootElement.someAttribute)

        val expected = listOf(
            CompactFragment(
                namespaces = listOf(XmlEvent.NamespaceImpl("bar", "http://example.com/bar")),
                "<bar:thing1>test</bar:thing1>"
            ),
            CompactFragment(
                namespaces = listOf(XmlEvent.NamespaceImpl("bar", "http://example.com/bar")),
                "<bar:thing2>test</bar:thing2>"
            ),
            CompactFragment(
                namespaces = listOf(XmlEvent.NamespaceImpl("bar", "http://example.com/bar")),
                "<bar:thing3>test</bar:thing3>"
            )
        )
        assertEquals(expected, rootElement.childElements.flatMap { it.list })
    }

    @Test
    fun `test reserialize with ns decl on leaf elements`() {
        val input =
            """
      <?xml version='1.0' ?>
      <foo:rootElement xmlns:foo="http://example.com/foo" someAttribute="hello">
        <foo:childElement>
          <bar:thing1 xmlns:bar="http://example.com/bar">test</bar:thing1>
          <bar:thing2 xmlns:bar="http://example.com/bar">test</bar:thing2>
        </foo:childElement>
        <foo:childElement>
          <bar:thing3 xmlns:bar="http://example.com/bar">test</bar:thing3>
        </foo:childElement>
      </foo:rootElement>
    """
                .trimIndent()

        val rootElement = baseXmlConfig.decodeFromString<RootElement>(input, rootName = null)

        val output = baseXmlConfig.encodeToString(rootElement, "foo2")

        val expectedOutput =
            """
      <?xml version='1.0' ?>
      <foo2:rootElement xmlns:foo2="http://example.com/foo" someAttribute="hello">
        <foo2:childElement>
          <bar:thing1 xmlns:bar="http://example.com/bar">test</bar:thing1>
          <bar:thing2 xmlns:bar="http://example.com/bar">test</bar:thing2>
        </foo2:childElement>
        <foo2:childElement>
          <bar:thing3 xmlns:bar="http://example.com/bar">test</bar:thing3>
        </foo2:childElement>
      </foo2:rootElement>
    """
                .trimIndent()

        assertEquals(expectedOutput, output)
    }

    @Test
    fun `test reserialize with ns decl on root element`() {
        val input =
            """|<?xml version='1.0' ?>
               |<foo:rootElement xmlns:foo="http://example.com/foo" xmlns:bar="http://example.com/bar" someAttribute="hello">
               |  <foo:childElement>
               |    <bar:thing1>test</bar:thing1>
               |    <bar:thing2>test</bar:thing2>
               |  </foo:childElement>
               |  <foo:childElement>
               |    <bar:thing3>test</bar:thing3>
               |  </foo:childElement>
               |</foo:rootElement>
            """.trimMargin()

        val rootElement = baseXmlConfig.decodeFromString<RootElement>(input, rootName = null)

        val output = baseXmlConfig.encodeToString(rootElement, "foo2")

        val expectedOutput =
            """|<?xml version='1.0' ?>
               |<foo2:rootElement xmlns:foo2="http://example.com/foo" someAttribute="hello">
               |  <foo2:childElement xmlns:bar="http://example.com/bar">
               |    <bar:thing1>test</bar:thing1>
               |    <bar:thing2>test</bar:thing2>
               |  </foo2:childElement>
               |  <foo2:childElement xmlns:bar="http://example.com/bar">
               |    <bar:thing3>test</bar:thing3>
               |  </foo2:childElement>
               |</foo2:rootElement>
            """.trimMargin()

        assertEquals(expectedOutput, output)
    }

    @Test
    fun `test reserialize using isCollectingNSAttributes`() {
        val input =
            """|<?xml version='1.0' ?>
               |<foo:rootElement xmlns:foo="http://example.com/foo" xmlns:bar="http://example.com/bar" someAttribute="hello">
               |  <foo:childElement>
               |    <bar:thing1>test</bar:thing1>
               |    <bar:thing2>test</bar:thing2>
               |  </foo:childElement>
               |  <foo:childElement>
               |    <bar:thing3>test</bar:thing3>
               |  </foo:childElement>
               |</foo:rootElement>
            """.trimMargin()

        val thisXmlConfig = baseXmlConfig.copy { isCollectingNSAttributes = true }

        val rootElement = thisXmlConfig.decodeFromString<RootElement>(input, rootName = null)

        val output = thisXmlConfig.encodeToString(rootElement, "foo2")

        val expectedOutput =
            """|<?xml version='1.0' ?>
               |<foo2:rootElement xmlns:foo2="http://example.com/foo" xmlns:bar="http://example.com/bar" someAttribute="hello">
               |  <foo2:childElement>
               |    <bar:thing1>test</bar:thing1>
               |    <bar:thing2>test</bar:thing2>
               |  </foo2:childElement>
               |  <foo2:childElement>
               |    <bar:thing3>test</bar:thing3>
               |  </foo2:childElement>
               |</foo2:rootElement>
            """.trimMargin()

        // Fails, output actually matches `test reserialize with ns decl on root element`
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `test whether qname values are also properly handled`() {
        val thisXmlConfig = baseXmlConfig.copy { isCollectingNSAttributes = true }

        val rootElement = RootElement(
            "hello",
            listOf(
                ChildElement(
                    QName("http://example.com/name", "bla", "name"), listOf(
                        CompactFragment("<bar:thing1 xmlns:bar='http://example.com/bar'>test</bar:thing1>"),
                        CompactFragment("<bar2:thing2 xmlns:bar2='http://example.com/bar'>test</bar2:thing2>"),
                    )
                ),
            ), null
        )

        val output = thisXmlConfig.encodeToString(rootElement, "foo2")

        val expectedOutput =
            """|<?xml version='1.0' ?>
               |<foo2:rootElement xmlns:foo2="http://example.com/foo" xmlns:bar="http://example.com/bar" xmlns:name="http://example.com/name" someAttribute="hello">
               |  <foo2:childElement name="name:bla">
               |    <bar:thing1>test</bar:thing1>
               |    <bar:thing2>test</bar:thing2>
               |  </foo2:childElement>
               |</foo2:rootElement>
            """.trimMargin()

        // Fails, output actually matches `test reserialize with ns decl on root element`
        assertEquals(expectedOutput, output)
    }

    @Test
    fun `test non-CompactFragment element with other namespace`() {
        val rootElement = RootElement("hello", listOf(), Other("text"))

        val output = baseXmlConfig.encodeToString(rootElement, "foo")

        val expectedOutput = """|<?xml version='1.0' ?>
              |<foo:rootElement xmlns:foo="http://example.com/foo" someAttribute="hello">
              |  <baz:other xmlns:baz="http://example.com/baz">text</baz:other>
              |</foo:rootElement>
            """.trimMargin()

        assertEquals(expectedOutput, output)
    }

    @Test
    fun `test non-CompactFragment element with other namespace and isCollectingNSAttributes`() {
        val rootElement = RootElement("hello", listOf(), Other("text"))

        val thisXmlConfig = baseXmlConfig.copy { isCollectingNSAttributes = true }

        val output = thisXmlConfig.encodeToString(rootElement, "foo")

        val expectedOutput = """|<?xml version='1.0' ?>
              |<foo:rootElement xmlns:foo="http://example.com/foo" xmlns:baz="http://example.com/baz" someAttribute="hello">
              |  <baz:other>text</baz:other>
              |</foo:rootElement>
            """.trimMargin()

        assertEquals(expectedOutput, output)
    }

    @Test
    fun `test encoding leaks prefix`() {
        val rootElement = RootElement("hello", listOf(), Other("text"))

        val output1 = baseXmlConfig.encodeToString(rootElement, "foo1")

        val expectedOutput1 = """|<?xml version='1.0' ?>
              |<foo1:rootElement xmlns:foo1="http://example.com/foo" someAttribute="hello">
              |  <baz:other xmlns:baz="http://example.com/baz">text</baz:other>
              |</foo1:rootElement>
            """.trimMargin()

        assertEquals(expectedOutput1, output1)

        val output2 = baseXmlConfig.encodeToString(rootElement, "foo2")

        val expectedOutput2 = """|<?xml version='1.0' ?>
              |<foo2:rootElement xmlns:foo2="http://example.com/foo" someAttribute="hello">
              |  <baz:other xmlns:baz="http://example.com/baz">text</baz:other>
              |</foo2:rootElement>
            """.trimMargin()

        // Fails, as output uses prefix foo1 instead of the prefix foo2
        assertEquals(expectedOutput2, output2)
    }
}

private const val NS = "http://example.com/foo"

@Serializable
@XmlSerialName("rootElement", namespace = NS)
data class RootElement(
    @XmlElement(false) val someAttribute: String,
    val childElements: List<ChildElement>,
    val other: Other?,
)

@Serializable
@XmlSerialName("childElement", namespace = NS)
data class ChildElement(
    val name: SerializableQName? = null,
    @XmlValue val list: List<CompactFragment>
)

@Serializable
@XmlSerialName("other", namespace = "http://example.com/baz", prefix = "baz")
data class Other(@XmlValue val text: String)
