/*
 * Copyright (c) 2024-2025.
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

package nl.adaptivity.xmlutil.core

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE_NS_URI
import nl.adaptivity.xmlutil.core.impl.NamespaceHolder
import nl.adaptivity.xmlutil.core.impl.PlatformXmlWriterBase
import nl.adaptivity.xmlutil.core.impl.multiplatform.Writer
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
    public val xmlDeclMode: XmlDeclMode,
    xmlVersion: XmlVersion = XmlVersion.XML11
) : PlatformXmlWriterBase(), XmlWriter {

    public constructor(
        writer: Writer,
        isRepairNamespaces: Boolean = true,
        xmlDeclMode: XmlDeclMode,
        xmlVersion: XmlVersion = XmlVersion.XML11
    ) : this((writer as Appendable), isRepairNamespaces, xmlDeclMode, xmlVersion)

    @Deprecated("When using XML 1.1 a document type declaration is required. If you want" +
            "to omit it, do so expressly")
    public constructor(
        writer: Writer,
        isRepairNamespaces: Boolean = true,
        xmlVersion: XmlVersion
    ) : this((writer as Appendable), isRepairNamespaces, XmlDeclMode.None, xmlVersion)

    @Deprecated("When using XML 1.1 a document type declaration is required. If you want" +
            "to omit it, do so expressly")
    public constructor(
        writer: Appendable,
        isRepairNamespaces: Boolean = true,
        xmlVersion: XmlVersion
    ) : this(writer, isRepairNamespaces, XmlDeclMode.None, xmlVersion)

    @Deprecated("When using XML 1.1 a document type declaration is required. If you want to omit it, do so expressly")
    public constructor(
        writer: Writer,
        isRepairNamespaces: Boolean = true,
    ) : this((writer as Appendable), isRepairNamespaces, XmlDeclMode.None, XmlVersion.XML11)

    @Deprecated("When using XML 1.1 a document type declaration is required. If you want to omit it, do so expressly")
    public constructor(
        writer: Appendable,
        isRepairNamespaces: Boolean = true,
    ) : this(writer, isRepairNamespaces, XmlDeclMode.None, XmlVersion.XML11)

    /**
     * The version of XML to generate. By default XML 1.1.
     */
    public var xmlVersion: XmlVersion = xmlVersion
        private set

    /**
     * Determine whether a trailing space is used before the end of a self-closing tag.
     */
    public var addTrailingSpaceBeforeEnd: Boolean = true

    private var isPartiallyOpenTag: Boolean = false

    private var elementStack = arrayOfNulls<String>(12)

    private var state: WriteState = WriteState.BeforeDocument

    private val namespaceHolder = NamespaceHolder()

    private var lastTagDepth = TAG_DEPTH_NOT_TAG

    /**
     * The namespace context in the **current** position.
     */
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
        ATTRCONTENTQUOT {
            override val isAttr: Boolean get() = true
        },
        ATTRCONTENTAPOS {
            override val isAttr: Boolean get() = true
        },
        TEXTCONTENT,
        DTD;

        open val isAttr: Boolean get() = false
    }

    private fun Appendable.appendXmlCodepoint(codepoint: UInt, mode: EscapeMode) {

        fun appendNumCharRef(code: UInt) {
            append("&#x").append(code.toString(16)).append(';')
        }

        fun throwInvalid(code: UInt): Nothing {
            throw IllegalArgumentException("In xml ${xmlVersion.versionString} the character 0x${code.toString(16)} is not valid")
        }

        val ch = when (codepoint) {
            0x9u, 0xAu, 0xDu, in (0x20u..0xd7ffu), in (0xe000u..0xfffdu)
            -> Char(codepoint.toUShort())

            else -> Char(0x0u)
        }

        when {
            codepoint == 0u -> throw IllegalArgumentException("XML documents may not contain null strings directly or indirectly")
            ch == '&' -> append("&amp;")
            ch == '<' && mode != EscapeMode.MINIMAL -> append("&lt;")
            ch == '>' && mode == EscapeMode.TEXTCONTENT -> append("&gt;")
            ch == '"' && mode == EscapeMode.ATTRCONTENTQUOT -> append("&quot;")
            ch == '\'' && mode == EscapeMode.ATTRCONTENTAPOS -> append("&apos;")

            codepoint in 0x1u..0x8u ||
                    codepoint == 0xBu || codepoint == 0xCu ||
                    codepoint in 0xEu..0x1Fu -> when (xmlVersion) {
                XmlVersion.XML10 -> throwInvalid(codepoint)
                XmlVersion.XML11 -> {
                    appendNumCharRef(codepoint)
                }
            }

            codepoint in 0x7fu..0x84u || codepoint in 0x86u..0x9fu -> when (xmlVersion) {
                XmlVersion.XML10 -> append(ch)
                XmlVersion.XML11 -> appendNumCharRef(codepoint)
            }

            codepoint in 0xD800u..0xDFFFu || codepoint == 0xFFFEu || codepoint == 0xFFFFu -> throwInvalid(codepoint)

            codepoint > 0xffffu -> {
                val down = codepoint - 0x10000u
                val highSurogate = (down shr 10) + 0xd800u
                val lowSurogate = (down and 0x3ffu) + 0xdc00u
                append(Char(highSurogate.toUShort()))
                append(Char(lowSurogate.toUShort()))
            }

            else -> append(ch)

        }
    }

    private fun Appendable.appendXmlChar(char: Char, mode: EscapeMode) {

        fun appendNumCharRef(code: Int) {
            append("&#x").append(code.toString(16)).append(';')
        }

        fun throwInvalid(code: Int): Nothing {
            throw IllegalArgumentException("In xml ${xmlVersion.versionString} the character 0x${code.toString(16)} is not valid")
        }

/*
        if (char.code < 0x20 && ! isXmlWhitespace(char)) {
            throw IllegalArgumentException("Invalid character with code 0x${char.code.toString(16)}")
        }
*/

        when {
            char.code >= ESCAPED_CHARS.size -> {
                when {
                    char.code in 0xD800..0xDFFF || char.code == 0xFFFE || char.code == 0xFFFF -> throwInvalid(char.code)
                    else -> append(char)
                }
            }

            !ESCAPED_CHARS[char.code] -> {
                append(char)
            }

            char == '&' -> append("&amp;")
            char == '<' && mode != EscapeMode.MINIMAL -> append("&lt;")
            char == '>' && mode == EscapeMode.TEXTCONTENT -> append("&gt;")
            char == '"' && mode == EscapeMode.ATTRCONTENTQUOT -> append("&quot;")
            char == '\'' && mode == EscapeMode.ATTRCONTENTAPOS -> append("&apos;")

            (char == '\n' || char == '\r' || char == '\t') && mode.isAttr -> appendNumCharRef(char.code)

            char.code in 0x1..0x8 || char.code == 0xB || char.code == 0xC || char.code in 0xE..0x1F -> when (xmlVersion) {
                XmlVersion.XML10 -> throwInvalid(char.code)
                XmlVersion.XML11 -> appendNumCharRef(char.code)
            }

            xmlVersion == XmlVersion.XML11 &&
                    (char.code in 0x7f..0x84 || char.code in 0x86..0x9f) -> appendNumCharRef(char.code)

            else -> append(char) // when escaping wasn't actually needed (depending on mode)
        } // should never be touched

    }

    private fun writeEscapedText(s: String, mode: EscapeMode) {
        var start = 0

        var i = 0
        val l = s.length
        while (i < l) {
            val c = s[i]

            if (c.code >= ESCAPED_CHARS.size || ESCAPED_CHARS[c.code]) {
                if (start < i) {
                    writer.append(s, start, i)
                }
                when {
                    c.isHighSurrogate() -> {
                        val codePoint = 0x10000u + ((c.code.toUInt() - 0xD800u) shl 10) +
                                (s[i + 1].code.toUInt() - 0xDC00u)
                        writer.appendXmlCodepoint(codePoint, mode)
                        start = i + 2
                        ++i
                    }

                    else -> {
                        writer.appendXmlChar(c, mode)
                        start = i + 1
                    }
                }
            }
            ++i
        }

        if (start < l) writer.append(s.substring(start, l))
    }

    private fun triggerStartDocument() {
        // Non-before states are not modified
        when (state) {
            WriteState.BeforeDocument -> {
                if (xmlDeclMode != XmlDeclMode.None) {
                    // It is only xml 1.1 if it has a version attribute with value 1.1
                    if (xmlVersion == XmlVersion.XML11 || xmlDeclMode != XmlDeclMode.Minimal) {
                        startDocument(xmlVersion.versionString, null, null)
                    } else {
                        startDocument()
                    }
                }
                state = WriteState.AfterXmlDecl
            }

            else -> {
            }
        }
    }

    private fun writeIndent(newDepth: Int = depth) {
        if (lastTagDepth >= 0 && _indentString.isNotEmpty() && lastTagDepth != depth) {
            ignorableWhitespace("\n")
            for (i in 0 until depth) writer.append(_indentString)
        }
        lastTagDepth = newDepth
    }


    private fun ensureNamespaceIfRepairing(namespace: String?, prefix: String?) {
        if (isRepairNamespaces && !namespace.isNullOrEmpty() && prefix != null) {
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

    override fun processingInstruction(target: String, data: String) {
        finishPartialStartTag(false)
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        triggerStartDocument()
        writer.append("<?")
        writer.append(target)
        if (data.isNotEmpty()) writer.append(' ').append(data)
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
        for (cp in text.asCodePoints()) {
            when (cp) {
                '-'.code.toUInt() -> {
                    if (lastWasHyphen) {
                        lastWasHyphen = false
                        writer.append("&#x2d;")
                    } else {
                        lastWasHyphen = true
                        writer.append('-')
                    }
                }

                else -> writer.appendXmlCodepoint(cp, EscapeMode.MINIMAL)
            }
        }
        writer.append("-->")
    }

    override fun text(text: String) {
        // empty text can be ignored (and allow for a self-closing tag).
        if (text.isEmpty()) return
        finishPartialStartTag(false)

        writeEscapedText(text, EscapeMode.TEXTCONTENT)

        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    override fun cdsect(text: String) {
        finishPartialStartTag(false)
        // Handle cdata with close part as element
        var endPos = 0
        writer.append("<![CDATA[")
        for (cp in text.asCodePoints()) {
            val ch = if (cp < 0x7ddfu) Char(cp.toUShort()) else Char(0x0u)
            when {
                ch == ']' && (endPos == 0 || endPos == 1) -> {
                    ++endPos; writer.append(ch)
                }

                ch == '>' && endPos == 2 -> writer.append("&gt;")
                ch == ']' && endPos == 2 -> writer.append(ch) // we have 3 ] characters so drop the first
                else -> {
                    endPos = 0; writer.appendXmlCodepoint(cp, EscapeMode.MINIMAL)
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
        if (text.isEmpty()) return // ignore writing empty text
        finishPartialStartTag(false)
        triggerStartDocument() // whitespace is not allowed before the xml declaration

        for (c in text) {
            when (c) {
                ' ', '\t', '\r', '\n' -> {}
                else -> throw IllegalArgumentException("\"$text\" is not ignorable whitespace")
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
        if (!(prefix.isNullOrEmpty() || namespace.isNullOrEmpty())) {
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

        private val ESCAPED_CHARS = BooleanArray(255).also {
            for (i in 1 until '\t'.code) it[i] = true
            // 0x9 is tab, 0xa is LF, 0xd is CR
            it[0x9] = true // mark for escaping as they need to be for attributes
            it[0xa] = true // mark for escaping as they need to be for attributes
            it[0xb] = true
            it[0xc] = true
            it[0xd] = true // needs escaping in all cases
            for (i in 0xe until 0x1f) it[i] = true
            it['<'.code] = true
            it['>'.code] = true
            it['&'.code] = true
            it['\''.code] = true
            it['"'.code] = true

            // Escaped in xml 1.1
            for (i in 0x7f..0x84) it[i] = true
            for (i in 0x86..0x9f) it[i] = true
        }

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

internal fun CharSequence.asCodePoints(): Iterable<UInt> {
    return object : Iterable<UInt> {
        override fun iterator(): Iterator<UInt> {
            return object : Iterator<UInt> {
                private var nextPos = 0

                override fun hasNext(): Boolean = nextPos < this@asCodePoints.length

                override fun next(): UInt = when (get(nextPos).isHighSurrogate()) {
                    true -> {
                        val codePoint = 0x10000u + ((get(nextPos).code.toUInt() - 0xD800u) shl 10) +
                                (get(nextPos + 1).code.toUInt() - 0xDC00u)
                        nextPos += 2
                        codePoint
                    }

                    else -> get(nextPos).code.toUInt().also { nextPos++ }
                }
            }
        }
    }
}
