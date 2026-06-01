/*
 * Copyright (c) 2026.
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

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.dom.DOMException
import nl.adaptivity.xmlutil.dom.length
import nl.adaptivity.xmlutil.dom2.*
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.test.*

class TestDomInitializedWithDomImplementation {

    class DocumentQueriesTraverseDescendants {
        lateinit var document: Document
        lateinit var root: Element
        lateinit var child0: Element
        lateinit var nsParent: Element
        lateinit var child1: Element
        lateinit var nsChild: Element
        lateinit var otherNsChild: Element

        @BeforeTest
        fun init() {
            document = newDocument("root")
            root = requireNotNull(document.documentElement)
            child0 = document.createElement("child").also { root.appendChild(it) }
            nsParent = document.createElementNS("urn:test", "ns:parent").also { root.appendChild(it) }
            child1 = document.createElement("child").also { nsParent.appendChild(it) }
            nsChild = document.createElementNS("urn:test", "ns:child").also { nsParent.appendChild(it) }
            otherNsChild = document.createElementNS("urn:other", "other:child").also { root.appendChild(it) }
        }

        @Test
        fun testSupportSingleWildcard() {
            assertEquals(
                listOf(root, child0, nsParent, child1, nsChild, otherNsChild),
                document.getElementsByTagName("*").toList()
            )
        }

        @Test
        fun testSupportGetByTagName() {
            assertEquals(listOf(child0, child1), document.getElementsByTagName("child").toList())
        }

        @Test
        fun testSupportNSWildcards() {
            assertEquals(
                listOf(root, child0, nsParent, child1, nsChild, otherNsChild),
                document.getElementsByTagNameNS("*", "*").toList()
            )
        }

        @Test
        fun testSupportGetElementByTagNameEmptyNS() {
            assertEquals(listOf(child0, child1), document.getElementsByTagNameNS("", "child").toList())
        }

        @Test
        fun testSupportNSWildcard() {
            assertEquals(
                listOf(child0, child1, nsChild, otherNsChild),
                document.getElementsByTagNameNS("*", "child").toList()
            )
        }

        @Test
        fun testSupportGetElementByTagNameNS() {
            assertEquals(listOf(nsChild), document.getElementsByTagNameNS("urn:test", "child").toList())
        }

    }

    class DeepClonePreservesAttributesAndChildren {
        lateinit var document: Document
        lateinit var element: Element

        @BeforeTest
        fun init() {
            document = newDocument()
            element = document.createElementNS("urn:test", "test:root")
            element.setAttribute("plain", "value")
            element.setAttributeNS("urn:meta", "meta:flag", "yes")
            element.appendChild(document.createElement("child").also { it.appendChild(document.createTextNode("content")) })
        }

        @Test
        fun testTagNameEqual() {
            val clone = element.cloneNode(true)

            assertEquals(element.getTagName(), clone.getTagName())
        }

        @Test
        fun testGetAttribute() {
            val clone = element.cloneNode(true)
            assertEquals("yes", clone.getAttributeNS("urn:meta", "flag"))
        }

        @Test
        fun testGetAttributeNS() {
            val clone = element.cloneNode(true)
            assertEquals("yes", clone.getAttributeNS("urn:meta", "flag"))
        }

        @Test
        fun testChildCodeLength() {
            val clone = element.cloneNode(true)
            assertEquals(1, clone.getChildNodes().length)
        }

        @Test
        fun testTextContent() {
            val clone = element.cloneNode(true)
            assertEquals("content", clone.firstChild?.textContent)
        }

        @Test
        fun testSameDocument() {
            val clone = element.cloneNode(true)
            assertSame(document, clone.ownerDocument)
        }
    }

    @Test
    fun importNodeDeepCopiesSubtreeIntoTargetDocument() {
        val sourceDocument = newDocument("source")
        val sourceRoot = requireNotNull(sourceDocument.documentElement)
        val sourceChild = sourceDocument.createElement("child").also {
            it.setAttribute("role", "imported")
            it.appendChild(sourceDocument.createTextNode("payload"))
        }
        sourceRoot.appendChild(sourceChild)

        val targetDocument = newDocument("target")
        val targetRoot = requireNotNull(targetDocument.documentElement)

        val imported = targetDocument.importNode(sourceChild, deep = true) as Element
        targetRoot.appendChild(imported)

        assertNull(imported.parentNode?.takeIf { it !== targetRoot })
        assertSame(targetDocument, imported.ownerDocument)
        assertSame(targetDocument, imported.firstChild?.ownerDocument)
        assertEquals("imported", imported.getAttribute("role"))
        assertEquals("payload", imported.textContent)
        assertSame(sourceRoot, sourceChild.parentNode)
        assertSame(sourceDocument, sourceChild.ownerDocument)
    }

    @Test
    fun adoptNodeDetachesFromSourceDocumentAndCanBeReattached() {
        val sourceDocument = newDocument("source")
        val sourceRoot = requireNotNull(sourceDocument.documentElement)
        val sourceChild = sourceDocument.createElement("child")
        val grandChild = sourceDocument.createElement("grandChild")
        sourceChild.appendChild(grandChild)
        sourceRoot.appendChild(sourceChild)

        val targetDocument = newDocument("target")
        val targetRoot = requireNotNull(targetDocument.documentElement)

        assertDomError(DOMException.Error.WRONG_DOCUMENT_ERR) {
            targetRoot.appendChild(sourceChild)
        }

        val adopted = targetDocument.adoptNode(sourceChild) as Element

        assertNull(adopted.parentNode)
        assertSame(targetDocument, adopted.ownerDocument)
        assertSame(targetDocument, grandChild.ownerDocument)
        assertEquals(0, sourceRoot.getChildNodes().length)

        targetRoot.appendChild(adopted)

        assertSame(targetRoot, adopted.parentNode)
    }

    @Test
    fun leafNodesRejectChildMutation() {
        val document = newDocument()
        val candidateChild = document.createElement("child")

        listOf<Node>(
            document.createAttribute("attr"),
            document.createTextNode("text"),
            document.createComment("comment"),
            document.createCDATASection("cdata"),
            document.createProcessingInstruction("target", "data"),
        ).forEach { leaf ->
            assertNull(leaf.firstChild)
            assertNull(leaf.lastChild)
            assertEquals(0, leaf.childNodes.length)

            assertDomError(DOMException.Error.HIERARCHY_REQUEST_ERR) {
                leaf.appendChild(candidateChild)
            }
            assertDomError(DOMException.Error.HIERARCHY_REQUEST_ERR) {
                leaf.replaceChild(candidateChild, candidateChild)
            }
            assertDomError(DOMException.Error.HIERARCHY_REQUEST_ERR) {
                leaf.removeChild(candidateChild)
            }
        }
    }

    @Test
    fun documentRejectsExtraRootElementsAndNonWhitespaceText() {
        val document = newDocument()

        document.appendChild(document.createTextNode(" \n\t"))
        document.appendChild(document.createElement("root"))

        assertDomError(DOMException.Error.HIERARCHY_REQUEST_ERR) {
            document.appendChild(document.createElement("otherRoot"))
        }
        assertDomError(DOMException.Error.HIERARCHY_REQUEST_ERR) {
            document.appendChild(document.createTextNode("not allowed"))
        }
        assertDomError(DOMException.Error.HIERARCHY_REQUEST_ERR) {
            document.appendChild(document.createCDATASection("not allowed"))
        }
    }

    @Test
    fun textContentTraversesNestedDescendantsAndPreservesWhitespaceNodes() {
        val document = newDocument("root")
        val root = requireNotNull(document.documentElement)
        val child = document.createElement("child")

        root.appendChild(document.createTextNode("prefix"))
        root.appendChild(document.createTextNode(" "))
        root.appendChild(child)
        child.appendChild(document.createTextNode("nested"))
        root.appendChild(document.createTextNode("\n"))

        assertEquals("prefix nested\n", root.textContent)
    }

    @Test
    fun previousSiblingReturnsTheImmediateSibling() {
        val document = newDocument("root")
        val root = requireNotNull(document.documentElement)
        val first = document.createElement("first")
        val second = document.createElement("second")
        val third = document.createElement("third")

        root.appendChild(first)
        root.appendChild(second)
        root.appendChild(third)

        assertNull(first.previousSibling)
        assertSame(first, second.previousSibling)
        assertSame(second, third.previousSibling)
    }

    @Test
    fun appendChildDetachesMovedNodeFromItsOldParent() {
        val document = newDocument("root")
        val root = requireNotNull(document.documentElement)
        val sourceParent = document.createElement("source").also(root::appendChild)
        val targetParent = document.createElement("target").also(root::appendChild)
        val child = document.createElement("child").also(sourceParent::appendChild)

        targetParent.appendChild(child)

        assertEquals(emptyList(), sourceParent.childNodes.toList())
        assertEquals(listOf(child), targetParent.childNodes.toList())
        assertSame(targetParent, child.parentNode)
    }

    @Test
    fun appendChildMovesDocumentFragmentChildrenAndEmptiesTheFragment() {
        val document = newDocument("root")
        val root = requireNotNull(document.documentElement)
        val fragment = document.createDocumentFragment()
        val first = document.createElement("first").also(fragment::appendChild)
        val second = document.createElement("second").also(fragment::appendChild)

        root.appendChild(fragment)

        assertEquals(listOf(first, second), root.childNodes.toList())
        assertEquals(0, fragment.childNodes.length)
        assertSame(root, first.parentNode)
        assertSame(root, second.parentNode)
    }

    @Test
    fun replaceChildWithDocumentFragmentRemovesOldChildAndEmptiesTheFragment() {
        val document = newDocument("root")
        val root = requireNotNull(document.documentElement)
        val before = document.createElement("before").also(root::appendChild)
        val oldChild = document.createElement("old").also(root::appendChild)
        val after = document.createElement("after").also(root::appendChild)
        val fragment = document.createDocumentFragment()
        val replacement0 = document.createElement("replacement0").also(fragment::appendChild)
        val replacement1 = document.createElement("replacement1").also(fragment::appendChild)

        val replaced = root.replaceChild(fragment, oldChild)

        assertSame(oldChild, replaced)
        assertNull(oldChild.parentNode)
        assertEquals(listOf(before, replacement0, replacement1, after), root.childNodes.toList())
        assertEquals(0, fragment.childNodes.length)
        assertSame(root, replacement0.parentNode)
        assertSame(root, replacement1.parentNode)
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    @Test
    fun normalizeMergesOnlyAdjacentTextNodes() {
        val document = newDocument("root")
        val root = requireNotNull(document.documentElement)
        val middle = document.createElement("middle")

        root.appendChild(document.createTextNode("a"))
        root.appendChild(document.createTextNode("b"))
        root.appendChild(middle)
        root.appendChild(document.createTextNode("c"))
        root.appendChild(document.createTextNode("d"))

        root.normalize()

        assertEquals(3, root.childNodes.length)
        assertEquals("ab", root.childNodes[0]?.textContent)
        assertSame(middle, root.childNodes[1])
        assertEquals("cd", root.childNodes[2]?.textContent)
    }

    @OptIn(ExperimentalXmlUtilApi::class)
    @Test
    fun isEqualNodeRejectsDifferentCharacterDataTypes() {
        val document = newDocument()
        val text = document.createTextNode("same")
        val comment = document.createComment("same")

        assertFalse(text.isEqualNode(comment))
        assertFalse(comment.isEqualNode(text))
    }

    @Test
    fun cdataUsesTheStandardNodeName() {
        val document = newDocument()

        assertEquals("#cdata-section", document.createCDATASection("value").nodeName)
    }

    @Test
    fun getWholeTextConcatenatesAdjacentTextSiblings() {
        val document = newDocument("root")
        val root = requireNotNull(document.documentElement)

        val text1 = document.createTextNode("left")
        val text2 = document.createTextNode("middle")
        val text3 = document.createTextNode("right")

        root.appendChild(text1)
        root.appendChild(text2)
        root.appendChild(text3)

        assertEquals("leftmiddleright", text1.getWholeText())
        assertEquals("leftmiddleright", text2.getWholeText())
        assertEquals("leftmiddleright", text3.getWholeText())
    }

    @Test
    fun getWholeTextStopsAtNonTextSibling() {
        val document = newDocument("root")
        val root = requireNotNull(document.documentElement)

        val text1 = document.createTextNode("left")
        val comment = document.createComment("break")
        val text2 = document.createTextNode("right")

        root.appendChild(text1)
        root.appendChild(comment)
        root.appendChild(text2)

        assertEquals("left", text1.getWholeText())
        assertEquals("right", text2.getWholeText())
    }


    private fun assertDomError(expected: DOMException.Error, block: () -> Unit) {
        val exception = assertFailsWith<DOMException>(block = block)
        assertEquals(expected, exception.error)
    }

    companion object {
        private fun newDocument(rootQualifiedName: String? = null, namespace: String? = null): Document =
            xmlStreaming.genericDomImplementation.createDocument(
                namespace = namespace,
                qualifiedName = rootQualifiedName,
                documentType = null,
            )
    }
}
