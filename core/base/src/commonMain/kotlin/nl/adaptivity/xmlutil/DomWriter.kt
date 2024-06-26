/*
 * Copyright (c) 2024.
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

@file:Suppress("DEPRECATION")

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.core.impl.PlatformXmlWriterBase
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.dom.NodeConsts
import nl.adaptivity.xmlutil.dom.adoptNode
import nl.adaptivity.xmlutil.dom2.*
import nl.adaptivity.xmlutil.util.forEachAttr
import nl.adaptivity.xmlutil.util.impl.createDocument
import nl.adaptivity.xmlutil.util.myLookupNamespaceURI
import nl.adaptivity.xmlutil.util.myLookupPrefix
import nl.adaptivity.xmlutil.dom.Node as Node1
import nl.adaptivity.xmlutil.dom2.Document as Document2
import nl.adaptivity.xmlutil.dom2.Element as Element2
import nl.adaptivity.xmlutil.dom2.Node as Node2

/**
 * Writer that uses the DOM for the underlying storage (rather than writing to some string).
 */
public class DomWriter @Deprecated("Don't use directly. Instead create an instance through xmlStreaming") constructor(
    current: Node2?,
    public val isAppend: Boolean = false,
    public val xmlDeclMode: XmlDeclMode = XmlDeclMode.None
) : PlatformXmlWriterBase(), XmlWriter {

    @Suppress("DEPRECATION")
    @Deprecated("Compatibility constructor, create through xmlStreaming")
    public constructor(
        current: Node1,
        isAppend: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None
    ) : this(current as? Node2 ?: createDocument(QName("x")).adoptNode(current), isAppend, xmlDeclMode)

    private var docDelegate: Document2? = when (current) {
        null -> null
        is Document2 -> current
        else -> current.getOwnerDocument()
    }

    public constructor(xmlDeclMode: XmlDeclMode = XmlDeclMode.None) : this(null, xmlDeclMode = xmlDeclMode)

    @XmlUtilInternal
    public val target: Document2 get() = docDelegate ?: throw XmlException("Document not created yet")

    @XmlUtilInternal
    public var currentNode: Node2? = current
        private set

    private val pendingOperations: List<(Document2) -> Unit> = mutableListOf()

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

    private fun addToPending(operation: (Document2) -> Unit) {
        if (docDelegate == null) {
            (pendingOperations as MutableList).add(operation)
        } else throw IllegalStateException("Use of pending list when there is a document already")
    }

    private val requireCurrent get() = (currentNode ?: throw IllegalStateException("No current element")) as Element2

    private fun requireCurrent(error: String) =
        currentNode as? Element2 ?: throw XmlException("The current node is not an element: $error")

    @Suppress("OverridingDeprecatedMember")
    override val namespaceContext: NamespaceContext = object : NamespaceContext {
        override fun getNamespaceURI(prefix: String): String? {
            return currentNode?.lookupNamespaceURI(prefix)
        }

        override fun getPrefix(namespaceURI: String): String? {
            return currentNode?.lookupPrefix(namespaceURI)
        }

        private fun Element2.collectDeclaredPrefixes(
            namespaceUri: String,
            result: MutableSet<String>,
            redeclared: MutableCollection<String>
        ) {
            getAttributes().forEachAttr { attr ->
                val prefix = when {
                    attr.getPrefix() == "xmlns" -> attr.getLocalName()
                    attr.getPrefix().isNullOrEmpty() && attr.getLocalName() == "xmlns" -> ""
                    else -> null
                }
                if (prefix != null) {
                    if (prefix in redeclared) {
                        if (attr.getValue() == namespaceUri) result.add(prefix)
                        redeclared.add(prefix)
                    }
                }
            }
            getParentElement()?.collectDeclaredPrefixes(namespaceUri, result, redeclared)
        }

        @Deprecated(
            "Don't use as unsafe",
            replaceWith = ReplaceWith("prefixesFor(namespaceURI)", "nl.adaptivity.xmlutil.prefixesFor")
        )
        override fun getPrefixes(namespaceURI: String): Iterator<String> {
            return buildSet<String> {
                (currentNode as Element2?)?.collectDeclaredPrefixes(namespaceURI, this, mutableListOf())
            }.toList().iterator()
        }

    }

    override var depth: Int = 0
        private set

    override fun namespaceAttr(namespacePrefix: String, namespaceUri: String) {
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
                val doc = createDocument(
                    qname(namespace ?: "", localName, prefix)
                )
                docDelegate = doc
                currentNode = docDelegate

                val e = doc.documentElement!!
                doc.removeChild(e) // remove to allow for pending operations

                for (pending in pendingOperations) {
                    pending(doc)
                }
                doc.appendChild(e)

                (pendingOperations as MutableList).clear()
                lastTagDepth = 0
                currentNode = doc.documentElement
                return
            }
            currentNode == null && !isAppend -> {
                if (target.childNodes.iterator().asSequence().count { it.nodeType == NodeConsts.ELEMENT_NODE } > 0) {
                    for (e in target.childNodes.filterIsInstance<Element2>()) { // use filter/list to have temporary list
                        target.removeChild(e)
                    }
                }
            }
        }

        target.createElementNS(qname(namespace, localName, prefix)).let { elem: Element2 ->
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

    override fun processingInstruction(target: String, data: String) {
        val ce = currentNode
//        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        if (ce == null) {
            addToPending { processingInstruction(target, data) }
        } else {

            val processInstr = this.target.createProcessingInstruction(target, data)

            ce.appendChild(processInstr)
        }
        lastTagDepth = TAG_DEPTH_NOT_TAG
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
        val cur = requireCurrent("attribute")
        when {
            namespace.isNullOrEmpty() && prefix.isNullOrEmpty() -> cur.setAttribute(name, value)
            prefix.isNullOrEmpty() -> cur.setAttributeNS(namespace, name, value)
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

    /**
     * The requested version of the document. This is set through [startDocument] and `null` if left default.
     */
    public var requestedVersion: String? = null
        private set

    /**
     * The requested encoding for the document. This is set through [startDocument] and `null` if left default.
     */
    public var requestedEncoding: String? = null
        private set

    /**
     * Whether this document is requested to be standalone. This is set through [startDocument] and `null` if left default.
     */
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
                (currentNode as? Element2)?.setAttribute(qname, namespaceUri)
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
