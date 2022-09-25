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

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.impl.PlatformXmlWriterBase
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.util.*
import nl.adaptivity.xmlutil.util.impl.*
import nl.adaptivity.xmlutil.dom.*

/**
 * Created by pdvrieze on 04/04/17.
 */
public class DomWriter constructor(
    current: Node?,
    public val isAppend: Boolean = false,
    public val xmlDeclMode: XmlDeclMode = XmlDeclMode.None
) : PlatformXmlWriterBase(), XmlWriter {

    private var docDelegate: Document? = when (current?.nodeType) {
        null -> null
        NodeConsts.DOCUMENT_NODE -> current as Document
        else -> current.ownerDocument
    }

    public constructor(xmlDeclMode: XmlDeclMode = XmlDeclMode.None) : this(null, xmlDeclMode = xmlDeclMode)

    public val target: Document get() = docDelegate ?: throw XmlException("Document not created yet")
    public var currentNode: Node? = current
        private set

    private val pendingOperations: List<(Document) -> Unit> = mutableListOf()

    private var lastTagDepth = TAG_DEPTH_NOT_TAG

    private fun writeIndent(newDepth: Int = depth) {
        val indentSeq = indentSequence
        if (lastTagDepth >= 0 && indentSeq.isNotEmpty() && lastTagDepth != depth) {
            ignorableWhitespace("\n")
            try {
                indentSequence = emptyList()
                repeat(depth) { indentSeq.forEach { it.writeTo(this) } }
            } finally {
                indentSequence = indentSeq
            }
        }
        lastTagDepth = newDepth
    }

    private fun addToPending(operation: (Document) -> Unit) {
        if (docDelegate == null) {
            (pendingOperations as MutableList).add(operation)
        } else throw IllegalStateException("Use of pending list when there is a document already")
    }

    private val requireCurrent get() = (currentNode ?: throw IllegalStateException("No current element")) as Element

    private fun requireCurrent(error: String) =
        currentNode as? Element ?: throw XmlException("The current node is not an element: $error")

    @Suppress("OverridingDeprecatedMember")
    override val namespaceContext: NamespaceContext = object : NamespaceContextImpl {
        override fun getNamespaceURI(prefix: String): String? {
            return currentNode?.lookupNamespaceURI(prefix)
        }

        override fun getPrefix(namespaceURI: String): String? {
            return currentNode?.lookupPrefix(namespaceURI)
        }

        private fun Element.collectDeclaredPrefixes(
            namespaceUri: String,
            result: MutableSet<String>,
            redeclared: MutableCollection<String>
        ) {
            attributes.forEachAttr { attr ->
                val prefix = when {
                    attr.prefix == "xmlns" -> attr.localName
                    attr.prefix.isNullOrEmpty() && attr.localName == "xmlns" -> ""
                    else -> null
                }
                if (prefix != null) {
                    if (prefix in redeclared) {
                        if (attr.value == namespaceUri) result.add(prefix)
                        redeclared.add(prefix)
                    }
                }
            }
            parentElement?.collectDeclaredPrefixes(namespaceUri, result, redeclared)
        }

        @OptIn(ExperimentalStdlibApi::class)
        override fun getPrefixesCompat(namespaceURI: String): Iterator<String> {
            return buildSet<String> {
                (currentNode as Element?)?.collectDeclaredPrefixes(namespaceURI, this, mutableListOf())
            }.toList().iterator()
        }

    }

    override var depth: Int = 0
        private set

    override fun namespaceAttr(namespacePrefix: String, namespaceUri: String) {
        println("DEBUG - namespaceAttribute - {$namespacePrefix}=$namespaceUri")
        val cur = requireCurrent("Namespace attribute")
        when {

            namespacePrefix.isEmpty() -> // Also ignore setting the namespace to empty if it is set.
                if (!(namespaceUri.isEmpty() && (cur.lookupNamespaceURI("") == ""))) {
                    cur.setAttributeNS(
                        XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                        XMLConstants.XMLNS_ATTRIBUTE, namespaceUri
                    )
                }

            else ->
                cur.setAttributeNS(
                    XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                    "${XMLConstants.XMLNS_ATTRIBUTE}:$namespacePrefix",
                    namespaceUri
                )
        }
    }

    override fun startTag(namespace: String?, localName: String, prefix: String?) {
        writeIndent()
        depth++
        when {
            currentNode == null && docDelegate == null -> {
                docDelegate = createDocument(
                    qname(namespace ?: "", localName, prefix)
                )
                currentNode = docDelegate
                for (pending in pendingOperations) {
                    pending(docDelegate!!)
                }
                (pendingOperations as MutableList).clear()
                lastTagDepth = 0
                currentNode = docDelegate?.documentElement
                return
            }
            currentNode == null && !isAppend -> {
                if (target.childNodes.iterator().asSequence().count { it.nodeType == NodeConsts.ELEMENT_NODE } > 0) {
                    target.removeElementChildren()

                }
            }
        }

        target.createElement(qname(namespace, localName, prefix)).let { elem: Element ->
            currentNode!!.appendChild(elem)
            currentNode = elem
        }
    }

    override fun comment(text: String) {
        writeIndent()
        val ce = currentNode
        if (ce == null) {
            addToPending { comment(text) }
        } else {
            target.createComment(text).let { comment ->
                ce.appendChild(comment)
            }
        }
    }

    override fun text(text: String) {
        lastTagDepth = TAG_DEPTH_NOT_TAG
        val ce = currentNode
        if (ce == null) {
            if (text.isBlank()) addToPending { ignorableWhitespace(text) } else throw XmlException("Not in an element -- text")
        } else {
            target.createTextNode(text).let { textNode ->
                ce.appendChild(textNode)
            }
        }

    }

    override fun cdsect(text: String) {
        lastTagDepth = TAG_DEPTH_NOT_TAG
        target.createCDATASection(text).let { cdataSection ->
            currentNode?.appendChild(cdataSection) ?: throw XmlException("Not in an element -- cdsect")
        }
    }

    override fun entityRef(text: String) {
        lastTagDepth = TAG_DEPTH_NOT_TAG
        throw UnsupportedOperationException("Creating entity references is not supported (or incorrect) in most browsers")
    }

    override fun processingInstruction(text: String) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        if (currentNode?.nodeType != NodeConsts.ELEMENT_NODE) throw XmlException("Document already started")
        if (docDelegate == null) {
            addToPending { processingInstruction(text) }
        } else {
            val split = text.indexOf(' ')
            val (target, data) = when {
                split < 0 -> text to ""
                else -> text.substring(0, split) to text.substring(split + 1)
            }
            this.target.createProcessingInstruction(target, data).let { processInstr ->
                this.target.appendChild(processInstr)
            }
        }
    }

    override fun ignorableWhitespace(text: String) {
        val ce = currentNode
        if (ce == null) {
            addToPending { ignorableWhitespace(text) }
        } else if (ce.nodeType != NodeConsts.DOCUMENT_NODE) { // There is no way to specify whitespace on a document element
            target.createTextNode(text).let { textNode ->
                ce.appendChild(textNode)
            }
        }
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    override fun attribute(namespace: String?, name: String, prefix: String?, value: String) {
        println("DEBUG - attribute - {$namespace}$prefix:$name=$value")
        val cur = requireCurrent("attribute")
        when {
            prefix.isNullOrEmpty() -> cur.setAttribute(name, value)
            else -> cur.setAttributeNS(
                namespace ?: XMLConstants.NULL_NS_URI,
                "${prefix}:$name",
                value
            )
        }
    }

    override fun docdecl(text: String) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        val target = docDelegate
        if (target == null) {
            addToPending { docdecl(text) }
        } else {

            val textElems = text.split(" ", limit = 3)
            val qualifiedName = textElems[0]
            val publicId = if (textElems.size > 1) textElems[1] else ""
            val systemId = if (textElems.size > 2) textElems[2] else ""
            target.implementation.createDocumentType(qualifiedName, publicId, systemId).let { docType ->
                target.appendChild(docType)
            }
        }
    }

    public var requestedVersion: String? = null
        private set

    public var requestedEncoding: String? = null
        private set

    public var requestedStandalone: Boolean? = null
        private set

    override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        // Ignore everything for now as this cannot be set on a dom tree
        requestedVersion = version
        requestedEncoding = encoding
        requestedStandalone = standalone
    }

    override fun endDocument() {
        currentNode = null
    }

    override fun endTag(namespace: String?, localName: String, prefix: String?) {
        depth--
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)

        currentNode = requireCurrent("No current element or no parent element").parentNode
    }

    override fun getNamespaceUri(prefix: String): String? {
        return currentNode?.myLookupNamespaceURI(prefix)
    }

    override fun getPrefix(namespaceUri: String?): String? {
        return currentNode?.myLookupPrefix(namespaceUri ?: "")
    }

    override fun setPrefix(prefix: String, namespaceUri: String) {
        val docDelegate = docDelegate
        if (docDelegate == null) {
            addToPending { setPrefix(prefix, namespaceUri) }
        } else {
            if (docDelegate.lookupNamespaceURI(prefix) != namespaceUri) {
                val qname = if (prefix.isEmpty()) "xmlns" else "xmlns:$prefix"
                (currentNode as? Element)?.setAttribute(qname, namespaceUri)
            }
        }
    }

    override fun close() {
        assert(depth == 0) { "Closing a dom writer but not all elements were closed (depth:$depth)" }
        currentNode = null
    }

    override fun flush() {}

    private companion object {
        private const val TAG_DEPTH_NOT_TAG = -1
        private const val TAG_DEPTH_FORCE_INDENT_NEXT = Int.MAX_VALUE
    }

}

private fun qname(prefix: String?, localName: String) = when {
    prefix.isNullOrEmpty() -> localName
    else -> "$prefix:$localName"
}
