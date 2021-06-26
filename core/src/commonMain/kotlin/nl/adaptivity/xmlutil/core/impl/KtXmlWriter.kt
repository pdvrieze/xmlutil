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

package nl.adaptivity.xmlutil.core.impl

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE_NS_URI
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert

/**
 * An android implementation of XmlWriter.
 * Created by pdvrieze on 15/11/15.
 *
 * @property isRepairNamespaces Should missing namespace attributes be added automatically
 * @property xmlDeclMode Should the xml declaration be emitted automatically?
 */
@OptIn(XmlUtilInternal::class)
class KtXmlWriter(
    private val writer: Appendable,
    val isRepairNamespaces: Boolean = true,
    val xmlDeclMode: XmlDeclMode = XmlDeclMode.None
                 ) : PlatformXmlWriterBase(), XmlWriter {

    var addTrailingSpaceBeforeEnd = true

    private var isPartiallyOpenTag: Boolean = false

    private var elementStack = arrayOfNulls<String>(12)

    private var state: WriteState = WriteState.BeforeDocument

    private val namespaceHolder = NamespaceHolder()

    private var lastTagDepth = TAG_DEPTH_NOT_TAG

    override val namespaceContext: NamespaceContext
        get() = namespaceHolder.namespaceContext

    override val depth: Int
        get() = namespaceHolder.depth

    private fun namespaceAt(depth: Int): String {
        return elementStack[depth * 3]!!
    }

    private fun setElementStack(depth: Int, namespace: String, prefix: String, localName: String) {
        var esp = depth * 3
        if (elementStack.size < esp + 3) {
            val hlp = arrayOfNulls<String>(elementStack.size + 12)
            elementStack.copyInto(hlp, endIndex = esp)
            elementStack = hlp
        }

        elementStack[esp++] = namespace
        elementStack[esp++] = prefix
        elementStack[esp] = localName
    }

    private fun prefixAt(depth: Int): String {
        return elementStack[depth * 3 + 1]!!
    }

    private fun localNameAt(depth: Int): String {
        return elementStack[depth * 3 + 2]!!
    }

    private fun finishPartialStartTag(close: Boolean) {
        if (!isPartiallyOpenTag) {
            return
        }

        isPartiallyOpenTag = false

        val endOfTag = when {
            !close                    -> ">"
            addTrailingSpaceBeforeEnd -> " />"
            else                      -> "/>"
        }
        writer.append(endOfTag)
    }

    private fun writeEscapedText(s: String, quot: Int) {

        loop@ for (c in s) {
            when (c) {
                '&'  -> writer.append("&amp;")

                '>'  -> writer.append("&gt;")

                '<'  -> writer.append("&lt;")

                '"',
                '\'' -> {
                    if (c.code == quot) {
                        writer.append(if (c == '"') "&quot;" else "&apos;")
                        break@loop
                    }
                    writer.append(c)
                }

                '\n',
                '\r',
                '\t' -> {
                    writer.append(c)
                }

                else -> {
                    writer.append(c)
                }
            }
        }
    }

    private fun triggerStartDocument() {
        // Non-before states are not modified
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (state) {
            WriteState.BeforeDocument -> {
                if (xmlDeclMode != XmlDeclMode.None) {
                    startDocument(null, null, null)
                }
                state = WriteState.AfterXmlDecl
            }
        }
    }

    private fun writeIndent(newDepth: Int = depth) {
        val indentSeq = indentSequence
        if (lastTagDepth >= 0 && indentSeq.isNotEmpty() && lastTagDepth != depth) {
            ignorableWhitespace("\n")
            try {
                indentSequence = emptyList()
//                repeat(depth) { indentSeq.forEach { it.writeTo(this) } }
                val merged = indentSeq.joinRepeated(depth)
                merged.forEach { it.writeTo(this) }
            } finally {
                indentSequence = indentSeq
            }
        }
        lastTagDepth = newDepth
    }


    private fun ensureNamespaceIfRepairing(namespace: String?, prefix: String?) {
        if (isRepairNamespaces && namespace != null && namespace.isNotEmpty() && prefix != null) {
            // TODO fix more cases than missing namespaces with given prefix and uri
            if (namespaceHolder.getNamespaceUri(prefix) != namespace) {
                namespaceAttr(prefix, namespace)
            }
        }
    }

    override fun flush() {
        finishPartialStartTag(false)
    }

    /**
     * {@inheritDoc}
     * @param version Unfortunately the serializer is forced to version 1.0
     */
    override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        if (state != WriteState.BeforeDocument) {
            throw XmlException("Attempting to write start document after document already started")
        }
        state = WriteState.AfterXmlDecl

        writer.append("<?xml version='1.0'")

        val effectiveEncoding = encoding ?: "UTF-8"

        if (xmlDeclMode != XmlDeclMode.Minimal || encoding != null) {
            writer.append(" encoding='")
            writer.append(effectiveEncoding)
            writer.append('\'')

            if (standalone != null) {
                writer.append(" standalone='")
                writer.append(if (standalone) "yes" else "no")
                writer.append('\'')
            }
        }
        writer.append("?>")

        if (indentSequence.isNotEmpty()) writer.appendLine()
    }

    override fun docdecl(text: String) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        triggerStartDocument()
        if (state != WriteState.AfterXmlDecl) {
            throw XmlException("Writing a DTD is only allowed once, in the prolog")
        }
        state = WriteState.AfterDocTypeDecl
        writer.append("<!DOCTYPE ").append(text.trimStart()).append(">")
    }

    override fun processingInstruction(text: String) {
        finishPartialStartTag(false)
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        triggerStartDocument()
        writer.append("<?")
        writer.append(text)
        writer.append("?>")
    }

    override fun startTag(namespace: String?, localName: String, prefix: String?) {
        finishPartialStartTag(false)
        writeIndent()

        triggerStartDocument()

        if (state == WriteState.Finished) {
            throw XmlException("Attempting to write tag after the document finished")
        }

        state = WriteState.InTagContent

        val appliedPrefix = if (namespace == "") {
            ""
        } else {
            val reg = getPrefix(namespace)
            when {
                reg != null    -> reg
                prefix == null -> namespaceHolder.nextAutoPrefix()
                else           -> prefix
            }
        }

        setElementStack(depth, namespace ?: "", appliedPrefix, localName)

        writer.append('<')
        if (appliedPrefix.isNotEmpty()) {
            writer.append(appliedPrefix)
            writer.append(':')
        }
        writer.append(localName)
        isPartiallyOpenTag = true

        namespaceHolder.incDepth()
        ensureNamespaceIfRepairing(namespace, appliedPrefix)
    }

    override fun endTag(namespace: String?, localName: String, prefix: String?) {
        namespaceHolder.decDepth()
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)

        if ((namespace ?: "") != namespaceAt(depth) || localNameAt(depth) != localName) {
            throw IllegalArgumentException("</{$namespace}$localName> does not match start")
        }

        if (isPartiallyOpenTag) {
            finishPartialStartTag(true)
        } else {
            writer.append("</")
            val actualPrefix = prefixAt(depth)
            if (actualPrefix.isNotEmpty()) {
                writer.append(actualPrefix)
                writer.append(':')
            }
            writer.append(localName)
            writer.append('>')
        }
    }

    override fun comment(text: String) {
        finishPartialStartTag(false)
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        triggerStartDocument() // No content before XmlDeclaration

        // TODO escape comment end strings
        writer.append("<!--").append(text).append("-->")
    }

    override fun text(text: String) {
        finishPartialStartTag(false)

        writeEscapedText(text, -1)

        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    override fun cdsect(text: String) {
        finishPartialStartTag(false)
        // Handle cdata with close part as element
        writer.append("<![CDATA[").append(text).append("]]>")

        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    override fun entityRef(text: String) {
        finishPartialStartTag(false)

        writer.append('&').append(text).append(';')

        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    override fun ignorableWhitespace(text: String) {
        finishPartialStartTag(false)
        triggerStartDocument() // whitespace is not allowed before the xml declaration

        for (c in text) {
            if (!(c == '\n' || c == '\r' || c == '\t' || c == ' ')) {
                throw IllegalArgumentException("\"$text\" is not ignorable whitespace")
            }
        }

        writer.append(text)
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    override fun attribute(namespace: String?, name: String, prefix: String?, value: String) {
        if (namespace == XMLNS_ATTRIBUTE_NS_URI) {
            namespaceAttr(name, value) // If it is a namespace attribute, just go there.
            return
        } else if (namespace.isNullOrEmpty() && XMLNS_ATTRIBUTE == name) {
            namespaceAttr("", value) // If it is a namespace attribute, just go there.
            return
        }

        // TODO streamline the way namespaces are handled, including the case where the prefix is
        //  already declared on this tag with a different name.
        if (prefix != null && prefix.isNotEmpty() && namespace != null && namespace.isNotEmpty()) {
            setPrefix(prefix, namespace)
            ensureNamespaceIfRepairing(namespace, prefix)
        }

        if (!isPartiallyOpenTag) {
            throw IllegalStateException("illegal position for attribute")
        }


        val actualPrefix = if (!prefix.isNullOrEmpty()) {
            if (getNamespaceUri(prefix) != namespace) {
                getPrefix(namespace)
            } else prefix
        } else {
            prefix
        } ?: ""

        rawWriteAttribute(actualPrefix, name, value)
    }

    override fun namespaceAttr(namespacePrefix: String, namespaceUri: String) {
        val existingNamespaceForPrefix = namespaceHolder.namespaceAtCurrentDepth(namespacePrefix)
        if (existingNamespaceForPrefix != null) {
            when {
                isRepairNamespaces -> return            // when repairing, just ignore duplicates

                existingNamespaceForPrefix != namespaceUri
                                   ->
                    throw IllegalStateException("Attempting to set prefix to different values in the same tag")

                else               -> throw IllegalStateException("Namespace attribute duplicated")
            }
        }
        namespaceHolder.addPrefixToContext(namespacePrefix, namespaceUri)

        if (!isPartiallyOpenTag) {
            throw IllegalStateException("illegal position for attribute")
        }

        if (namespacePrefix.isNotEmpty()) {
            rawWriteAttribute(XMLNS_ATTRIBUTE, namespacePrefix, namespaceUri)
        } else {
            rawWriteAttribute("", XMLNS_ATTRIBUTE, namespaceUri)
        }
    }

    private fun rawWriteAttribute(prefix: String, localName: String, value: String) {
        writer.append(' ')
        if (prefix.isNotEmpty()) {
            writer.append(prefix).append(':')
        }
        writer.append(localName).append('=')

        val q = if (value.indexOf('"') == -1) '"' else '\''
        writer.append(q)
        writeEscapedText(value, q.code)
        writer.append(q)
    }

    override fun endDocument() {
        assert(depth == 0)

        if (state != WriteState.InTagContent) {
            throw XmlException("Attempting to end document when in invalid state: $state")
        }

        // TODO make this repairing behaviour optional
        while (depth > 0) {
            endTag(namespaceAt(depth - 1), prefixAt(depth - 1), localNameAt(depth - 1))
        }
        flush()
    }

    override fun setPrefix(prefix: String, namespaceUri: String) {
        if (namespaceUri != getNamespaceUri(prefix)) {
            namespaceHolder.addPrefixToContext(prefix, namespaceUri)
        }
    }

    override fun getNamespaceUri(prefix: String): String? {
        return namespaceHolder.getNamespaceUri(prefix)
    }

    override fun getPrefix(namespaceUri: String?): String? {
        return namespaceUri?.let { namespaceHolder.getPrefix(it) }
    }

    override fun close() {
        namespaceHolder.clear()
    }

    companion object {
        /** Not a tag: -1 */
        private const val TAG_DEPTH_NOT_TAG = -1
        private const val TAG_DEPTH_FORCE_INDENT_NEXT = Int.MAX_VALUE
    }


    private enum class WriteState {
        BeforeDocument,
        AfterXmlDecl,
        AfterDocTypeDecl,
        InTagContent,
        Finished
    }

}

private fun Iterable<XmlEvent.TextEvent>.joinRepeated(repeats: Int): List<XmlEvent.TextEvent> {
    val it = iterator()
    if (! it.hasNext()) return emptyList()

    val result = mutableListOf<XmlEvent.TextEvent>()
    var pending: XmlEvent.TextEvent? = null
    for (i in 0 until repeats) {
        for (ev in this@joinRepeated) {
            if (pending==null) {
                pending = ev
            } else if (pending.eventType == EventType.COMMENT || pending.eventType != ev.eventType) {
                result.add(pending)
                pending = ev
            } else if (ev.eventType == pending.eventType){
                pending = XmlEvent.TextEvent(null, pending.eventType, pending.text + ev.text)
            }
        }
    }
    if (pending!=null) result.add(pending)
    return result
}

