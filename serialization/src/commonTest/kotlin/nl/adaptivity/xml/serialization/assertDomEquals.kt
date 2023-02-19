/*
 * Copyright (c) 2022.
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

import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.dom.Node
import nl.adaptivity.xmlutil.dom.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val Node.isElement: Boolean get() = this.nodeType == NodeConsts.ELEMENT_NODE

private val Node.isText: Boolean get() = this.nodeType == NodeConsts.TEXT_NODE

private val Node.isCharacterData: Boolean
    get() = when (nodeType) {
        NodeConsts.TEXT_NODE,
        NodeConsts.CDATA_SECTION_NODE,
        NodeConsts.COMMENT_NODE -> true

        else -> false
    }

private fun NamedNodeMap.asSequence(): Sequence<Attr> {
    return sequence {
        for (i in 0 until length) {
            yield(item(i) as Attr)
        }
    }
}

private fun NodeList.asSequence(): Sequence<Node> {
    return sequence {
        for (i in 0 until length) {
            yield(item(i)!!)
        }
    }
}

fun assertDomEquals(expected: Node, actual: Node): Unit = when {
    expected.nodeType != actual.nodeType
    -> throw AssertionError("Node types for $expected and $actual are not the same")

    expected.nodeType == NodeConsts.DOCUMENT_NODE
    -> assertDomEquals((expected as Document).documentElement!!, (actual as Document).documentElement!!)

    expected.isElement -> assertElementEquals(expected as Element, actual as Element)
    expected.isCharacterData -> assertEquals(expected.textContent, actual.textContent)

//    !expected.isEqualNode(actual)
//         -> throw AssertionError("Nodes $expected and $actual are not equal")
    else -> Unit // println("Asserting equality for node ${expected} of type ${expected.nodeType}")
}

private fun assertElementEquals(expected: Element, actual: Element) {
    val expectedAttrsSorted = expected.attributes.asSequence()
        .filterNot { it.namespaceURI == XMLConstants.XMLNS_ATTRIBUTE_NS_URI }
        .sortedBy { "${it.prefix}:${it.localName}" }.toList()
    val actualAttrsSorted = actual.attributes.asSequence()
        .filterNot { it.namespaceURI == XMLConstants.XMLNS_ATTRIBUTE_NS_URI }
        .sortedBy { "${it.prefix}:${it.localName}" }.toList()

//    val expectedString = expected.outerHTML
//    val actualString = actual.outerHTML

//    assertEquals(expectedAttrsSorted.size, actualAttrsSorted.size, "Sorted attribute counts should match: ${expectedString} & ${actualString}")
    assertEquals(
        expectedAttrsSorted.size,
        actualAttrsSorted.size,
        "Sorted attribute counts should match: $expectedAttrsSorted != $actualAttrsSorted"
    )
    for ((idx, expectedAttr) in expectedAttrsSorted.withIndex()) {
        val actualAttr = actualAttrsSorted[idx]
        assertEquals(expectedAttr.namespaceURI ?: "", actualAttr.namespaceURI ?: "")

        val expectedLocalName = if (expectedAttr.prefix == null) expectedAttr.name else expectedAttr.localName
        val actualLocalName = if (actualAttr.prefix == null) actualAttr.name else actualAttr.localName
        assertEquals(expectedLocalName, actualLocalName)
    }

    val expectedChildren =
        expected.childNodes.asSequence().filter { it.nodeType != NodeConsts.TEXT_NODE || it.textContent?.trim() != "" }
            .mergeText().toList()

    val actualChildren =
        actual.childNodes.asSequence().filter { it.nodeType != NodeConsts.TEXT_NODE || it.textContent?.trim() != "" }
            .mergeText().toList()

    assertEquals(expectedChildren.size, actualChildren.size, "Different child count")
    for ((idx, expectedChild) in expectedChildren.withIndex()) {
        val actualChild = actualChildren[idx]
        assertDomEquals(expectedChild, actualChild)
    }
}

private fun Sequence<Node>.mergeText(): Sequence<Node> {
    return sequence {
        val pendingString = StringBuilder()
        var document: Document? = null
        for (n in this@mergeText) {
            when (n.nodeType) {
                NodeConsts.TEXT_NODE -> {
                    pendingString.append((n as Text).data)
                    document = n.ownerDocument
                }

                else -> {
                    if (pendingString.isNotEmpty()) {
                        yield(document!!.createTextNode(pendingString.toString()))
                        pendingString.clear()
                    }
                    yield(n)
                }
            }
        }
        if (pendingString.isNotEmpty()) yield(document!!.createTextNode(pendingString.toString()))
    }
}
