/*
 * This file is based/adapted from kxml2
 *
 * Copyright (c) 2002,2003, Stefan Haustein, Oberhausen, Rhld., Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The  above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package nl.adaptivity.xmlutil.core

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE_NS_URI
import nl.adaptivity.xmlutil.core.impl.NamespaceHolder
import nl.adaptivity.xmlutil.core.impl.PlatformXmlWriterBase
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert

/**
 * A cross-platform implementation of XmlWriter.
 * Created by pdvrieze on 15/11/15.
 *
 * @property isRepairNamespaces Should missing namespace attributes be added automatically
 * @property xmlDeclMode Should the xml declaration be emitted automatically?
 */
public class KtXmlWriter(
    private val writer: Appendable,
    public val isRepairNamespaces: Boolean = true,
    public val xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
    xmlVersion: XmlVersion = XmlVersion.XML11
) : PlatformXmlWriterBase(), XmlWriter {

    public var xmlVersion: XmlVersion = xmlVersion
        private set

    public var addTrailingSpaceBeforeEnd: Boolean = true

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
            !close -> ">"
            addTrailingSpaceBeforeEnd -> " />"
            else -> "/>"
        }
        writer.append(endOfTag)
    }

    private enum class EscapeMode {
        /**
         * Escaping characters that may not occur directly anywhere, including in comments or cdata
         */
        MINIMAL,
        ATTRCONTENTQUOT,
        ATTRCONTENTAPOS,
        TEXTCONTENT,
        DTD
    }

    private fun Appendable.appendXmlChar(ch: Char, mode: EscapeMode) {

        fun appendNumCharRef(code: Int) {
            append("&#x").append(code.toString(16)).append(';')
        }

        fun throwInvalid(code: Int): Nothing {
            throw IllegalArgumentException("In xml ${xmlVersion.versionString} the character 0x${code.toString(16)} is not valid")
        }

        val c = ch.code
        when {
            c == 0x0 -> throw IllegalArgumentException("XML documents may not contain null strings directly or indirectly")
            ch == '&' -> append("&amp;")
            ch == '<' && mode != EscapeMode.MINIMAL -> append("&lt;")
            ch == '>' && mode == EscapeMode.TEXTCONTENT -> append("&gt;")
            ch == '"' && mode == EscapeMode.ATTRCONTENTQUOT -> append("&quot;")
            ch == '\'' && mode == EscapeMode.ATTRCONTENTAPOS -> append("&apos;")

            c in 0x1..0x8 ||
                    c == 0xB || c == 0xC ||
                    c in 0xE..0x1F -> when (xmlVersion) {
                XmlVersion.XML10 -> throwInvalid(c)
                XmlVersion.XML11 -> {
                    appendNumCharRef(c)
                }
            }

            c in 0x7f..0x84 || c in 0x86..0x9f -> when (xmlVersion) {
                XmlVersion.XML10 -> append(ch)
                XmlVersion.XML11 -> appendNumCharRef(c)
            }
            c in 0xD800..0xDFFF || c == 0xFFFE || c == 0xFFFF -> throwInvalid(c)
            else -> append(ch)

        }
    }

    private fun writeEscapedText(s: String, mode: EscapeMode) {
        loop@ for (c in s) {
            writer.appendXmlChar(c, mode)
        }
    }

    private fun triggerStartDocument() {
        // Non-before states are not modified
        when (state) {
            WriteState.BeforeDocument -> {
                if (xmlDeclMode != XmlDeclMode.None) {
                    startDocument(null, null, null)
                }
                state = WriteState.AfterXmlDecl
            }
            else -> {
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
     * @param version The value of the version attribute. This will update the [xmlVersion] property
     *          where any other version that 1.0 or 1.1 will be interpreted as being 1.1
     */
    override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        if (state != WriteState.BeforeDocument) {
            throw XmlException("Attempting to write start document after document already started")
        }
        state = WriteState.AfterXmlDecl

        val verString = when (version) {
            null -> xmlVersion.versionString
            "1",
            "1.0" -> {
                xmlVersion = XmlVersion.XML10; version
            }
            else -> {
                xmlVersion = XmlVersion.XML11; version
            }
        }

        writer.append("<?xml version='$verString'")

        val effectiveEncoding = encoding ?: "UTF-8"

        if (xmlDeclMode != XmlDeclMode.Minimal || encoding != null) {
            writer.append(" encoding='")
            writeEscapedText(effectiveEncoding, EscapeMode.ATTRCONTENTAPOS)
            writer.append('\'')

            if (standalone != null) {
                writer.append(" standalone='")
                writer.append(if (standalone) "yes" else "no")
                writer.append('\'')
            }
        }
        if (addTrailingSpaceBeforeEnd) writer.append(' ')
        writer.append("?>")
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
                reg != null -> reg
                prefix == null -> namespaceHolder.nextAutoPrefix()
                else -> prefix
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

        var lastWasHyphen = false
        // TODO escape comment end strings
        writer.append("<!--")
        for (ch in text) {
            when (ch) {
                '-' -> {
                    if (lastWasHyphen) {
                        lastWasHyphen = false
                        writer.append("&#x2d;")
                    } else {
                        lastWasHyphen = true
                        writer.append('-')
                    }
                }
                else -> writer.appendXmlChar(ch, EscapeMode.MINIMAL)
            }
        }
        writer.append("-->")
    }

    override fun text(text: String) {
        finishPartialStartTag(false)

        writeEscapedText(text, EscapeMode.TEXTCONTENT)

        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    override fun cdsect(text: String) {
        finishPartialStartTag(false)
        // Handle cdata with close part as element
        var endPos = 0
        writer.append("<![CDATA[")
        for (ch in text) {
            when {
                ch == ']' && (endPos == 0 || endPos == 1) -> {
                    ++endPos; writer.append(ch)
                }
                ch == '>' && endPos == 2 -> writer.append("&gt;")
                ch == ']' && endPos == 2 -> writer.append(ch) // we have 3 ] characters so drop the first
                else -> {
                    endPos = 0; writer.appendXmlChar(ch, EscapeMode.MINIMAL)
                }
            }
        }
        writer.append("]]>")

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

                else -> throw IllegalStateException("Namespace attribute duplicated")
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

        val (q, mode) = when (value.indexOf('"')) {
            -1 -> Pair('"', EscapeMode.ATTRCONTENTQUOT)
            else -> Pair('\'', EscapeMode.ATTRCONTENTAPOS)
        }
        writer.append(q)
        writeEscapedText(value, mode)
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

    private companion object {
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
    if (!it.hasNext()) return emptyList()

    val result = mutableListOf<XmlEvent.TextEvent>()
    var pending: XmlEvent.TextEvent? = null
    for (i in 0 until repeats) {
        for (ev in this@joinRepeated) {
            if (pending == null) {
                pending = ev
            } else if (pending.eventType == EventType.COMMENT || pending.eventType != ev.eventType) {
                result.add(pending)
                pending = ev
            } else if (ev.eventType == pending.eventType) {
                pending = XmlEvent.TextEvent(null, pending.eventType, pending.text + ev.text)
            }
        }
    }
    if (pending != null) result.add(pending)
    return result
}

