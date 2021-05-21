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

import nl.adaptivity.xmlutil.serialization.iterator
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import java.lang.AssertionError
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals


actual fun assertXmlEquals(expected: String, actual: String) {
    try {
        assertEquals(expected.normalizeXml(), actual.normalizeXml())
    } catch (e: AssertionError) {

        val dbf = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isIgnoringComments = true
        }
        val db = dbf.newDocumentBuilder()
        val expectedDom = db.parse(expected.byteInputStream())
        val actualDom = db.parse(actual.byteInputStream())

        assertXmlEquals(expectedDom, actualDom)
    }
}

fun assertXmlEquals(expected: Node, actual: Node): Unit = when {
    expected.nodeType != actual.nodeType
                        -> throw AssertionError("Node types for $expected and $actual are not the same")

    expected.nodeType == Node.DOCUMENT_NODE
                        -> assertXmlEquals((expected as Document).documentElement!!, (actual as Document).documentElement!!)

    expected is Element -> assertElementEquals(expected, actual as Element)

    expected is Text    -> assertEquals(expected.textContent, actual.textContent)

    else -> println("Asserting equality for node ${expected} of type ${expected.nodeType}")
}

fun assertElementEquals(expected: Element, actual: Element) {
    val expectedAttrsSorted = expected.attributes.iterator().asSequence().sortedBy { "${it.prefix}:${it.localName}" }.toList()
    val actualAttrsSorted = expected.attributes.iterator().asSequence().sortedBy { "${it.prefix}:${it.localName}" }.toList()

    assertEquals(expectedAttrsSorted, actualAttrsSorted, "Sorted attributes should match")
    val expectedChildren = expected.childNodes.iterator().asSequence().filter { it.nodeType!=Node.TEXT_NODE || it.textContent!="" }.toList()
    val actualChildren = actual.childNodes.iterator().asSequence().filter { it.nodeType!=Node.TEXT_NODE || it.textContent!="" }.toList()

    assertEquals(expectedChildren.size, actualChildren.size, "Different child count")
    for ((idx, expectedChild) in expectedChildren.withIndex()) {
        assertXmlEquals(expectedChild, actualChildren[idx])
    }
}
