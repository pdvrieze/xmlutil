/*
 * Copyright (c) 2023-2025.
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

package nl.adaptivity.xml.serialization

import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.dom.*
import kotlin.test.assertEquals

@Suppress("DEPRECATION")
private val PlatformNode.isElement: Boolean get() = this.getNodeType() == NodeConsts.ELEMENT_NODE

@Suppress("DEPRECATION")
private val PlatformNode.isCharacterData: Boolean
    get() = when (getNodeType()) {
        NodeConsts.TEXT_NODE,
        NodeConsts.CDATA_SECTION_NODE,
        NodeConsts.COMMENT_NODE -> true

        else -> false
    }

@Suppress("DEPRECATION")
private fun PlatformNamedNodeMap.asSequence(): Sequence<PlatformAttr> {
    return sequence {
        for (i in 0 until getLength()) {
            yield(item(i) as PlatformAttr)
        }
    }
}

@Suppress("DEPRECATION")
private fun PlatformNodeList.asSequence(): Sequence<PlatformNode> {
    return sequence {
        for (i in 0 until getLength()) {
            yield(item(i)!!)
        }
    }
}

@Suppress("DEPRECATION")
fun assertDomEquals(expected: PlatformNode, actual: PlatformNode): Unit = when {
    expected.getNodeType() != actual.getNodeType()
    -> throw AssertionError("Node types for $expected and $actual are not the same")

    expected.getNodeType() == NodeConsts.DOCUMENT_NODE
    -> assertDomEquals((expected as PlatformDocument).documentElement!!, (actual as PlatformDocument).documentElement!!)

    expected.isElement -> assertElementEquals(expected as PlatformElement, actual as PlatformElement)
    expected.isCharacterData -> assertEquals(expected.getTextContent(), actual.getTextContent())

    else -> Unit
}

@Suppress("DEPRECATION")
private fun assertElementEquals(expected: PlatformElement, actual: PlatformElement) {
    val expectedAttrsSorted = expected.getAttributes().asSequence()
        .filterNot { it.getNamespaceURI() == XMLConstants.XMLNS_ATTRIBUTE_NS_URI }
        .sortedBy { "${it.getPrefix()}:${it.getLocalName()}" }.toList()
    val actualAttrsSorted = actual.getAttributes().asSequence()
        .filterNot { it.getNamespaceURI() == XMLConstants.XMLNS_ATTRIBUTE_NS_URI }
        .sortedBy { "${it.getPrefix()}:${it.getLocalName()}" }.toList()

    assertEquals(
        expectedAttrsSorted.size,
        actualAttrsSorted.size,
        "Sorted attribute counts should match: $expectedAttrsSorted != $actualAttrsSorted"
    )
    for ((idx, expectedAttr) in expectedAttrsSorted.withIndex()) {
        val actualAttr = actualAttrsSorted[idx]
        if (expectedAttr.getNamespaceURI().isNullOrEmpty()) {
            if (!actualAttr.getNamespaceURI().isNullOrEmpty()) {
                assertEquals(expected.getNamespaceURI(), actualAttr.getNamespaceURI())
            }
        } else if (!(expectedAttr.getNamespaceURI() == expected.getNamespaceURI() && actualAttr.getNamespaceURI()
                .isNullOrEmpty())) {
            assertEquals(expectedAttr.getNamespaceURI(), actualAttr.getNamespaceURI())
        }

        val expectedLocalName =
            if (expectedAttr.getPrefix() == null) expectedAttr.getName() else expectedAttr.getLocalName()
        val actualLocalName = if (actualAttr.getPrefix() == null) actualAttr.getName() else actualAttr.getLocalName()
        assertEquals(expectedLocalName, actualLocalName)
    }

    val expectedChildren =
        expected.getChildNodes().asSequence()
            .filter { it.getNodeType() != NodeConsts.TEXT_NODE || it.getTextContent()?.trim() != "" }
            .mergeText().toList()

    val actualChildren =
        actual.getChildNodes().asSequence().filter {
            it.getNodeType() != NodeConsts.TEXT_NODE || it.getTextContent()?.trim() != "" }
            .mergeText().toList()

    assertEquals(expectedChildren.size, actualChildren.size, "Different child count")
    for ((idx, expectedChild) in expectedChildren.withIndex()) {
        val actualChild = actualChildren[idx]
        assertDomEquals(expectedChild, actualChild)
    }
}

@Suppress("DEPRECATION")
private fun Sequence<PlatformNode>.mergeText(): Sequence<PlatformNode> {
    return sequence {
        val pendingString = StringBuilder()
        var document: PlatformDocument? = null
        for (n in this@mergeText) {
            when (n.getNodeType()) {
                NodeConsts.TEXT_NODE -> {
                    pendingString.append((n as PlatformText).getData())
                    document = n.getOwnerDocument()
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
