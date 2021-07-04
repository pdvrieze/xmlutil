/*
 * Copyright (c) 2018.
 *
 * This file is part of XmlUtil.
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

import nl.adaptivity.xmlutil.core.impl.NamespaceHolder
import nl.adaptivity.xmlutil.util.CombiningNamespaceContext

public class XmlBufferedWriter @XmlUtilInternal constructor(
    buffer: MutableList<XmlEvent> = mutableListOf(),
    delegateNamespaceContext: FreezableNamespaceContext?
) : XmlWriter {

    public constructor(buffer: MutableList<XmlEvent> = mutableListOf()) : this(buffer, null)

    private val _buffer = buffer

    public val buffer: List<XmlEvent> get() = _buffer

    private val namespaceHolder = NamespaceHolder()

    override val depth: Int get() = namespaceHolder.depth

    override var indentString: String
        get() = ""
        set(@Suppress("UNUSED_PARAMETER") value) {} // Buffered writers don't add synthetic elements

    override val namespaceContext: NamespaceContext = if (delegateNamespaceContext == null) {
        namespaceHolder.namespaceContext
    } else {
        // Don't use the plus operato here as we don't know that the contexts are not mutable.
        @Suppress("DEPRECATION")
        CombiningNamespaceContext(namespaceHolder.namespaceContext, delegateNamespaceContext)
    }

    override fun setPrefix(prefix: String, namespaceUri: String) {
        namespaceHolder.addPrefixToContext(prefix, namespaceUri)
    }

    override fun getNamespaceUri(prefix: String): String? {
        return namespaceHolder.getNamespaceUri(prefix)
    }

    override fun getPrefix(namespaceUri: String?): String? {
        return namespaceUri?.let { namespaceHolder.getPrefix(it) }
    }

    override fun startTag(namespace: String?, localName: String, prefix: String?) {
        val parentContext = namespaceHolder.namespaceContext.freeze()
        namespaceHolder.incDepth()
        val effNamespace = effectiveNamespace(namespace, prefix)
        val effPrefix = effectivePrefix(prefix, effNamespace)

        _buffer.add(
            XmlEvent.StartElementEvent(
                null, effNamespace ?: "", localName, effPrefix ?: "",
                emptyArray(), parentContext, emptyList()
            )
        )
    }

    private fun effectivePrefix(prefix: String?, namespace: String?) =
        prefix ?: namespace?.let { namespaceContext.getPrefix(it) }

    private fun effectiveNamespace(namespace: String?, prefix: String?) =
        if (namespace.isNullOrEmpty()) prefix?.let { namespaceContext.getNamespaceURI(prefix) } else namespace

    override fun namespaceAttr(namespacePrefix: String, namespaceUri: String) {
        namespaceHolder.addPrefixToContext(namespacePrefix, namespaceUri)
        val localName: String
        val prefix: String
        if (namespacePrefix.isEmpty()) {
            localName = XMLConstants.XMLNS_ATTRIBUTE
            prefix = ""
        } else {
            localName = namespacePrefix
            prefix = XMLConstants.XMLNS_ATTRIBUTE
        }

        _buffer.add(
            XmlEvent.Attribute(
                null, XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                localName, prefix, namespaceUri
            )
        )
    }

    override fun endTag(namespace: String?, localName: String, prefix: String?) {
        val effNamespace = effectiveNamespace(namespace, prefix)
        val effPrefix = effectivePrefix(prefix, effNamespace) ?: ""
        namespaceHolder.decDepth()
        _buffer.add(
            XmlEvent.EndElementEvent(
                null,
                effNamespace ?: "",
                localName,
                effPrefix,
                namespaceHolder.namespaceContext
            )
        )
    }

    override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) {
        _buffer.add(XmlEvent.StartDocumentEvent(null, encoding, version, standalone))
    }

    override fun processingInstruction(text: String) {
        _buffer.add(
            XmlEvent.TextEvent(
                null, EventType.PROCESSING_INSTRUCTION,
                text
            )
        )
    }

    override fun docdecl(text: String) {
        _buffer.add(XmlEvent.TextEvent(null, EventType.DOCDECL, text))
    }

    override fun attribute(namespace: String?, name: String, prefix: String?, value: String) {
        if (namespace == XMLConstants.XMLNS_ATTRIBUTE_NS_URI || prefix == XMLConstants.XMLNS_ATTRIBUTE ||
            (prefix.isNullOrEmpty() && name == XMLConstants.XMLNS_ATTRIBUTE)
        ) {
            namespaceAttr(name, value)
        } else {
            val effNamespace = effectiveNamespace(namespace, prefix)
            val effPrefix = effectivePrefix(prefix, effNamespace) ?: ""
            _buffer.add(XmlEvent.Attribute(null, effNamespace ?: "", name, effPrefix, value))
        }
    }

    override fun comment(text: String) {
        _buffer.add(XmlEvent.TextEvent(null, EventType.COMMENT, text))
    }

    override fun text(text: String) {
        _buffer.add(XmlEvent.TextEvent(null, EventType.TEXT, text))
    }

    override fun cdsect(text: String) {
        _buffer.add(XmlEvent.TextEvent(null, EventType.CDSECT, text))
    }

    override fun entityRef(text: String) {
        _buffer.add(XmlEvent.TextEvent(null, EventType.ENTITY_REF, text))
    }

    override fun ignorableWhitespace(text: String) {
        _buffer.add(
            XmlEvent.TextEvent(null, EventType.IGNORABLE_WHITESPACE, text)
        )
    }

    override fun endDocument() {
        _buffer.add(XmlEvent.EndDocumentEvent(null))
    }

    override fun close() {}

    override fun flush() {}

}
