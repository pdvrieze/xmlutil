/*
 * Copyright (c) 2024-2026.
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

package nl.adaptivity.xmlutil.test

import io.github.pdvrieze.xmlutil.testutil.assertXmlEquals
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.KtXmlWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.core.internal.StringInOutBuffer
import nl.adaptivity.xmlutil.test.multiplatform.Target
import nl.adaptivity.xmlutil.test.multiplatform.testTarget
import kotlin.test.Test
import kotlin.test.assertEquals

class TestKtXmlReader : TestCommonReader() {

    override fun createReader(xml: String): XmlReader = xmlStreaming.newGenericReader(xml)

    @Test
    fun testReadEntityInAttribute() {
        val data = "<tag attr=\"&lt;xx&gt;\"/>"
        val reader = KtXmlReader(StringReader(data))
        var e = reader.next()
        if (e == EventType.START_DOCUMENT) e = reader.next()
        assertEquals(EventType.START_ELEMENT, e)
        assertEquals("tag", reader.localName)
        assertEquals(1, reader.attributeCount)
        assertEquals("attr", reader.getAttributeLocalName(0))
        assertEquals("<xx>", reader.getAttributeValue(0))

        assertEquals(EventType.END_ELEMENT, reader.next())
    }

    @Test
    override fun testProcessingInstructionDom() {
        if (testTarget != Target.Node) {
            val domWriter = DomWriter()
            testProcessingInstruction(::createReader) { domWriter }

            val expectedXml = """
                <?xpacket begin='' id='from_166'?>
                <a:root xmlns:a="foo" a:b="42">bar</a:root>
                <?xpacket end='w'?>
            """
            val expected = xmlStreaming.newReader(expectedXml)
            val actual = xmlStreaming.newReader(domWriter.target)
            assertXmlEquals(expected, actual)

            val fromDom = StringWriter()
            KtXmlWriter(fromDom, xmlDeclMode = XmlDeclMode.None).use { writer ->
                xmlStreaming.newReader(domWriter.target).use { reader ->
                    while (reader.hasNext()) {
                        if(reader.next() != EventType.START_DOCUMENT) {
                            reader.writeCurrent(writer)
                        }
                    }
                }
            }
            assertXmlEquals(expectedXml, fromDom.toString())
        }
    }

    @Test
    fun testXmlDeclReader() {
        val reader = KtXmlReader(StringReader("<?xml version=\"1.1\" standalone=\"yes\"?>\r<foo>bar</foo>"))
        assertEquals(EventType.START_DOCUMENT, reader.next())
        assertEquals("1.1", reader.version)
        assertEquals(true, reader.standalone)
        assertEquals(39, reader.getColumnNumber())
        assertEquals(1, reader.getLineNumber())

        assertEquals(EventType.IGNORABLE_WHITESPACE, reader.next())
        assertEquals("\n", reader.text)
        assertEquals(1, reader.getColumnNumber())
        assertEquals(2, reader.getLineNumber())

        assertEquals(EventType.START_ELEMENT, reader.next())
        assertEquals("foo", reader.localName)
        assertEquals(6, reader.getColumnNumber())
        assertEquals(2, reader.getLineNumber())
    }

    @Test
    fun testXmlDeclString() {
        val reader = KtXmlReader(StringInOutBuffer("<?xml version=\"1.1\" standalone=\"yes\"?>\r<foo>bar</foo>"))
        assertEquals(EventType.START_DOCUMENT, reader.next())
        assertEquals("1.1", reader.version)
        assertEquals(true, reader.standalone)
        assertEquals(39, reader.getColumnNumber())
        assertEquals(1, reader.getLineNumber())

        assertEquals(EventType.IGNORABLE_WHITESPACE, reader.next())
        assertEquals("\n", reader.text)
        assertEquals(1, reader.getColumnNumber())
        assertEquals(2, reader.getLineNumber())

        assertEquals(EventType.START_ELEMENT, reader.next())
        assertEquals("foo", reader.localName)
        assertEquals(6, reader.getColumnNumber())
        assertEquals(2, reader.getLineNumber())
    }

    fun testParseNewline(newLine: String, count: Int = 1) {
        val xml = "<tag>$newLine</tag>"
        val reader1 = KtXmlReader(StringReader(xml))
        assertEquals(EventType.START_ELEMENT, reader1.nextTag())
        assertEquals(EventType.IGNORABLE_WHITESPACE, reader1.next())
        assertEquals("\n".repeat(count), reader1.text)
        assertEquals(EventType.END_ELEMENT, reader1.next())
        assertEquals(1+count, reader1.getLineNumber())
        assertEquals(7, reader1.getColumnNumber())

        val reader2 = KtXmlReader(StringInOutBuffer(xml))
        assertEquals(EventType.START_ELEMENT, reader2.nextTag())
        assertEquals(EventType.IGNORABLE_WHITESPACE, reader2.next())
        assertEquals("\n".repeat(count), reader2.text)
        assertEquals(EventType.END_ELEMENT, reader2.next())
        assertEquals(1+count, reader2.getLineNumber())
        assertEquals(7, reader2.getColumnNumber())
    }

    @Test fun testParseNewlineLF() = testParseNewline("\n")
    @Test fun testParseNewlineCR() = testParseNewline("\r")
    @Test fun testParseNewlineCRLF() = testParseNewline("\r\n")
    @Test fun testParseNewlineCR85() = testParseNewline("\r\u0085")
    @Test fun testParseNewlineCR2028() = testParseNewline("\r\u2028")
    @Test fun testParseNewline85() = testParseNewline("\u0085")
    @Test fun testParseNewline2028() = testParseNewline("\u2028")
    @Test fun testParseNewlineLFCR() = testParseNewline("\n\r", 2)

    @Test
    fun testUnquotedAttributeValues() {
        val xml = "<tag attr='foo' attr2=b&lt;ar attr3=baz/>"
        val reader = KtXmlReader(StringReader(xml), relaxed = true)
        assertEquals(EventType.START_ELEMENT, reader.nextTag())
        assertEquals("tag", reader.localName)
        assertEquals(3, reader.attributeCount)
        assertEquals("attr", reader.getAttributeLocalName(0))
        assertEquals("foo", reader.getAttributeValue(0))

        assertEquals("attr2", reader.getAttributeLocalName(1))
        assertEquals("b<ar", reader.getAttributeValue(1))

        assertEquals("attr3", reader.getAttributeLocalName(2))
        assertEquals("baz", reader.getAttributeValue(2))

    }

    @Test
    override fun testReadWsInAttr() {
        super.testReadWsInAttr()
    }
}
