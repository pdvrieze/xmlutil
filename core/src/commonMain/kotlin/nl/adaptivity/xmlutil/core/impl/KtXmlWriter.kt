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
import nl.adaptivity.xmlutil.XMLConstants.NULL_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE_NS_URI
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert

/**
 * An android implementation of XmlWriter.
 * Created by pdvrieze on 15/11/15.
 */
@OptIn(XmlUtilInternal::class)
class KtXmlWriter : PlatformXmlWriterBase, XmlWriter {


    private lateinit var writer: Appendable

    private var isPartiallyOpenTag: Boolean = false
    private var _auto: Int = 0
    private var _depth: Int// = 0
        get() = depth
        set(value) { /* ignore*/ }

    private var elementStack = arrayOfNulls<String>(12)

    private var _namespaceCounts = IntArray(4)
    private var _nspStack = arrayOfNulls<String>(10)
    private var _nspWritten = BooleanArray(5)

    private var _unicode: Boolean = false
    private var _encoding: String? = null
    private val _escapeAggressive = false
    var _xmlDeclMode = XmlDeclMode.None
    var _addTrailingSpaceBeforeEnd = true
    private var state: WriteState = WriteState.BeforeDocument

    private fun checkPending(close: Boolean) {
        if (!isPartiallyOpenTag) {
            return
        }

        _depth++
        isPartiallyOpenTag = false

        if (_namespaceCounts.size <= _depth + 3) {
            val hlp = IntArray(_depth + 8)
            _namespaceCounts.copyInto(hlp, endIndex = _depth + 2)
            _namespaceCounts = hlp
        }

        _namespaceCounts[_depth + 2] = _namespaceCounts[_depth + 1]
        // Only set the second level here as the first level may already have pending namespaces

        val endOfTag = when {
            !close                     -> ">"
            _addTrailingSpaceBeforeEnd -> " />"
            else                       -> "/>"
        }
        writer.append(endOfTag)
    }

    private fun _writeEscaped(s: String, quot: Int) {

        loop@ for (c in s) {
            when (c) {
                '&'              -> writer.append("&amp;")
                '>'              -> writer.append("&gt;")
                '<'              -> writer.append("&lt;")
                '"', '\''        -> {
                    if (c.code == quot) {
                        writer.append(if (c == '"') "&quot;" else "&apos;")
                        break@loop
                    }
                    if (_escapeAggressive && quot != -1) {
                        writer.append("&#${c.code};")
                    } else {
                        writer.append(c)
                    }
                }
                '\n', '\r', '\t' -> if (_escapeAggressive && quot != -1) {
                    writer.append("&#${c.code};")
                } else {
                    writer.append(c)
                }
                else             ->
                    //if(c < ' ')
                    //	throw new IllegalArgumentException("Illegal control code:"+((int) c));
                    if (_escapeAggressive && (c < ' ' || c == '@' || c.code > 127 && !_unicode)) {
                        writer.append("&#${c.code};")
                    } else {
                        writer.append(c)
                    }
            }
        }
    }

    fun _docdecl(dd: String) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (state) {
            WriteState.BeforeDocument -> {
                if (_xmlDeclMode != XmlDeclMode.None) {
                    _startDocument(null, null)
                }
                state = WriteState.AfterXmlDecl
            }
            WriteState.AfterXmlDecl   -> {
            }
            else                      ->
                throw XmlException("Writing a DTD is only allowed once, in the prolog")
        }
        state = WriteState.AfterDocTypeDecl
        writer.append("<!DOCTYPE")
        writer.append(dd)
        writer.append(">")
    }

    fun _endDocument() {
        if (state != WriteState.InTagContent) {
            throw XmlException("Attempting to end document when in invalid state: $state")
        }
        while (_depth > 0) {
            _endTag(elementStack[_depth * 3 - 3], elementStack[_depth * 3 - 1]!!)
        }
        _flush()
    }

    fun _entityRef(name: String) {
        checkPending(false)
        writer.append('&')
        writer.append(name)
        writer.append(';')
    }

    fun _getPrefix(namespace: String, create: Boolean): String? {
        return _getPrefix(namespace, false, create)
    }

    private fun _getPrefix(namespace: String, includeDefault: Boolean, create: Boolean): String? {

        run {
            var i = _namespaceCounts[_depth + 1] * 2 - 2
            while (i >= 0) {
                if (_nspStack[i + 1] == namespace && (includeDefault || _nspStack[i] != "")) {
                    var candidate: String? = _nspStack[i]
                    for (j in i + 2 until _namespaceCounts[_depth + 1] * 2) {
                        if (_nspStack[j] == candidate) {
                            candidate = null
                            break
                        }
                    }
                    if (candidate != null) {
                        return candidate
                    }
                }
                i -= 2
            }
        }

        if (!create) {
            return null
        }

        var prefix: String?

        if ("" == namespace) {
            prefix = ""
        } else {
            do {
                prefix = "n" + _auto++
                var i = _namespaceCounts[_depth + 1] * 2 - 2
                while (i >= 0) {
                    if (prefix == _nspStack[i]) {
                        prefix = null
                        break
                    }
                    i -= 2
                }
            } while (prefix == null)
        }

        val p = isPartiallyOpenTag
        isPartiallyOpenTag = false
        _setPrefix(prefix, namespace)
        isPartiallyOpenTag = p
        return prefix
    }

    private fun _getNamespace(prefix: String, includeDefault: Boolean = false): String? {

        var i = _namespaceCounts[_depth + 1] * 2 - 2
        while (i >= 0) {
            if (_nspStack[i] == prefix && (includeDefault || _nspStack[i] != "")) {
                var candidate: String? = _nspStack[i + 1]
                for (j in i + 2 until _namespaceCounts[_depth + 1] * 2) {
                    if (_nspStack[j + 1] == candidate) {
                        candidate = null
                        break
                    }
                }
                if (candidate != null) {
                    return candidate
                }
            }
            i -= 2
        }
        return null
    }

    fun _setPrefix(prefix: String?, namespace: String?) {

        val namespaceOffset: Int = _depth + 1

        var i = _namespaceCounts[namespaceOffset] * 2 - 2
        while (i >= 0) {
            if (_nspStack[i + 1] == (namespace ?: "") && _nspStack[i] == (prefix ?: "")) {
                // bail out if already defined
                return
            }
            i -= 2
        }


        val c = _namespaceCounts[namespaceOffset]
        _namespaceCounts[namespaceOffset] = c + 1
        _namespaceCounts[namespaceOffset + 1] = c + 1
        var pos = c shl 1

        _addSpaceToNspStack()

        _nspStack[pos++] = prefix ?: ""
        _nspStack[pos] = namespace ?: ""
        _nspWritten[_namespaceCounts[namespaceOffset] - 1] = false
    }

    private fun _addSpaceToNspStack() {
        val nspCount = _namespaceCounts[if (isPartiallyOpenTag) _depth + 1 else _depth]
        val pos = nspCount shl 1
        if (_nspStack.size < pos + 2) {
            val hlp = arrayOfNulls<String>(_nspStack.size + 16)
            _nspStack.copyInto(hlp, endIndex = pos)
            _nspStack = hlp

            val help = BooleanArray(_nspWritten.size + 8)
            _nspWritten.copyInto(help, endIndex = nspCount)
            _nspWritten = help
        }
    }

    fun _setOutput(writer: Appendable) {
        this.writer = writer

        _namespaceCounts[0] = 3
        _namespaceCounts[1] = 3
        _namespaceCounts[2] = 3
        _nspStack[0] = ""
        _nspStack[1] = ""
        _nspStack[2] = "xml"
        _nspStack[3] = "http://www.w3.org/XML/1998/namespace"
        _nspStack[4] = "xmlns"
        _nspStack[5] = "http://www.w3.org/2000/xmlns/"
        isPartiallyOpenTag = false
        _auto = 0
        _depth = 0

        _unicode = false
    }

    fun _startDocument(encoding: String?, standalone: Boolean?) {
        if (state != WriteState.BeforeDocument) {
            throw XmlException("Attempting to write start document after document already started")
        }
        state = WriteState.AfterXmlDecl

        if (_xmlDeclMode != XmlDeclMode.None) {
            writer.append("<?xml version='1.0'")

            if (encoding != null) {
                _encoding = encoding
                if (encoding.lowercase().startsWith("utf")) {
                    _unicode = true
                }
            } else if (_xmlDeclMode == XmlDeclMode.Charset) {
                _encoding = "UTF-8"
            }

            if (_xmlDeclMode != XmlDeclMode.Minimal || !_unicode) {

                _encoding?.let { enc ->
                    writer.append(" encoding='")
                    writer.append(enc)
                    writer.append('\'')
                }

                if (standalone != null) {
                    writer.append(" standalone='")
                    writer.append(if (standalone) "yes" else "no")
                    writer.append('\'')
                }
            }
            writer.append("?>")

            if (indentSequence.isNotEmpty()) writer.appendLine()
        }
    }

    fun _attribute(namespace: String?, name: String, value: String) {
        if (!isPartiallyOpenTag) {
            throw IllegalStateException("illegal position for attribute")
        }

        val ns = namespace ?: ""

        if (ns == XMLNS_ATTRIBUTE_NS_URI) {
            return _namespace(name, value) // If it is a namespace attribute, just go there.
        } else if (ns == NULL_NS_URI && XMLNS_ATTRIBUTE == name) {
            return _namespace("", value) // If it is a namespace attribute, just go there.
        }

        //		depth--;
        //		pending = false;

        val prefix = when (ns) {
            ""   -> ""
            else -> _getPrefix(ns, includeDefault = false, create = true)
        }

        writer.append(' ')
        if ("" != prefix) {
            writer.append(prefix!!)
            writer.append(':')
        }
        writer.append(name)
        writer.append('=')
        val q = if (value.indexOf('"') == -1) '"' else '\''
        writer.append(q)
        _writeEscaped(value, q.code)
        writer.append(q)
    }

    fun _attribute(namespace: String?, prefix: String, name: String, value: String) {
        if (!isPartiallyOpenTag) {
            throw IllegalStateException("illegal position for attribute")
        }

        val ns = namespace ?: ""

        if (ns == XMLNS_ATTRIBUTE_NS_URI) {
            return _namespace(name, value) // If it is a namespace attribute, just go there.
        } else if (ns == NULL_NS_URI && XMLNS_ATTRIBUTE == name) {
            return _namespace("", value) // If it is a namespace attribute, just go there.
        }

        val actualPrefix = if (prefix.isNotEmpty()) {
            if (_getNamespace(prefix) != namespace) {
                _getPrefix(ns, includeDefault = false, create = true) ?: ""
            } else prefix
        } else {
            prefix
        }

        //		depth--;
        //		pending = false;

        writer.append(' ')
        if ("" != actualPrefix) {
            writer.append(actualPrefix)
            writer.append(':')
        }
        writer.append(name)
        writer.append('=')
        val q = if (value.indexOf('"') == -1) '"' else '\''
        writer.append(q)
        _writeEscaped(value, q.code)
        writer.append(q)
    }

    fun _namespace(prefix: String, namespace: String?) {

        if (!isPartiallyOpenTag) {
            throw IllegalStateException("illegal position for attribute")
        }

        var wasSet = false
        for (i in _namespaceCounts[_depth] until _namespaceCounts[_depth + 1]) {
            if (prefix == _nspStack[i * 2]) {
                if (_nspStack[i * 2 + 1] != namespace) { // If we find the prefix redefined within the element, bail out
                    throw IllegalArgumentException(
                        "Attempting to bind prefix to conflicting values in one element"
                                                  )
                }
                if (_nspWritten[i]) {
                    // otherwise just ignore the request.
                    return
                }
                _nspWritten[i] = true
                wasSet = true
                break
            }
        }

        if (!wasSet) { // Don't use setPrefix as we know it isn't there
            _addSpaceToNspStack()
            val c = _namespaceCounts[_depth + 1]
            _namespaceCounts[_depth + 1] = c + 1
            _namespaceCounts[_depth + 2] = c + 1
            val pos = c shl 1
            _nspStack[pos] = prefix
            _nspStack[pos + 1] = namespace
            _nspWritten[pos shr 1] = true
        }

        val nsNotNull = namespace ?: ""

        writer.append(' ')
        writer.append(XMLNS_ATTRIBUTE)
        if (prefix.isNotEmpty()) {
            writer.append(':')
            writer.append(prefix)
        }
        writer.append('=')
        val q = if (nsNotNull.indexOf('"') == -1) '"' else '\''
        writer.append(q)
        _writeEscaped(nsNotNull, q.code)
        writer.append(q)
    }

    fun _flush() {
        checkPending(false)
    }

    fun _endTag(namespace: String?, name: String) {

        if (!isPartiallyOpenTag) {
            _depth--
        }
        //        if (namespace == null)
        //          namespace = "";

        if (namespace == null && elementStack[_depth * 3] != null
            || namespace != null && namespace != elementStack[_depth * 3]
            || elementStack[_depth * 3 + 2] != name
        ) {
            throw IllegalArgumentException("</{$namespace}$name> does not match start")
        }

        if (isPartiallyOpenTag) {
            checkPending(true)
            _depth--
        } else {
            writer.append("</")
            val prefix = elementStack[_depth * 3 + 1]!!
            if ("" != prefix) {
                writer.append(prefix)
                writer.append(':')
            }
            writer.append(name)
            writer.append('>')
        }

        val c = _namespaceCounts[_depth]
        _namespaceCounts[_depth + 1] = c
        if (!isPartiallyOpenTag) _namespaceCounts[_depth + 2] = c
    }

    fun _getNamespace(): String? {
        return if (_getDepth() == 0) null else elementStack[_getDepth() * 3 - 3]
    }

    fun _getName(): String? {
        return if (_getDepth() == 0) null else elementStack[_getDepth() * 3 - 1]
    }

    fun _getDepth(): Int {
        return if (isPartiallyOpenTag) _depth + 1 else _depth
    }

    fun _text(text: String) {
        checkPending(false)
        _writeEscaped(text, -1)
    }

    fun _text(text: CharArray, start: Int, len: Int) {
        _text(text.concatToString(start, start + len))
    }

    fun _cdsect(data: String) {
        checkPending(false)
        writer.append("<![CDATA[")
        writer.append(data)
        writer.append("]]>")
    }

    fun _comment(comment: String) {
        triggerStartDocument() // No content before XmlDeclaration
        checkPending(false)
        writer.append("<!--")
        writer.append(comment)
        writer.append("-->")
    }

    fun _processingInstruction(pi: String) {
        triggerStartDocument()

        checkPending(false)
        writer.append("<?")
        writer.append(pi)
        writer.append("?>")
    }

    private fun triggerStartDocument() {
        // Non-before states are not modified
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (state) {
            WriteState.BeforeDocument -> {
                if (_xmlDeclMode != XmlDeclMode.None) {
                    _startDocument(null, null)
                }
                state = WriteState.AfterXmlDecl
            }
        }
    }


    private val namespaceHolder = NamespaceHolder()
    private val isRepairNamespaces: Boolean

    private var lastTagDepth = TAG_DEPTH_NOT_TAG

    override val namespaceContext: NamespaceContext
        get() = namespaceHolder.namespaceContext

    override val depth: Int
        get() = namespaceHolder.depth

    constructor(writer: Appendable, repairNamespaces: Boolean = true, xmlDeclMode: XmlDeclMode = XmlDeclMode.None) :
            this(repairNamespaces, xmlDeclMode) {
        this.writer = writer
        initWriter()
    }

    private constructor(repairNamespaces: Boolean, xmlDeclMode: XmlDeclMode = XmlDeclMode.None) {
        isRepairNamespaces = repairNamespaces
        _xmlDeclMode = xmlDeclMode
        initWriter()
    }

    private fun initWriter() {
        _setPrefix(XMLNS_ATTRIBUTE, XMLNS_ATTRIBUTE_NS_URI)
    }

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


    override fun flush() {
        _flush()
    }

    override fun startTag(namespace: String?, localName: String, prefix: String?) {
        writeIndent()

        if (namespace != null && namespace.isNotEmpty()) {
            _setPrefix(prefix ?: "", namespace)
        }

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (state) {
            WriteState.BeforeDocument -> {
                if (_xmlDeclMode != XmlDeclMode.None) {
                    _startDocument(null, null)
                }
            }
            WriteState.Finished       ->
                throw XmlException("Attempting to write tag after the document finished")
        }
        state = WriteState.InTagContent
        checkPending(false)
        var esp = _depth * 3
        if (elementStack.size < esp + 3) {
            val hlp = arrayOfNulls<String>(elementStack.size + 12)
            elementStack.copyInto<kotlin.String?>(hlp, endIndex = esp)
            elementStack = hlp
        }
        val prefix = namespace?.let { _getPrefix(it, includeDefault = true, create = true) } ?: ""
        if (namespace.isNullOrEmpty()) {
            for (i in _namespaceCounts[_depth] until _namespaceCounts[_depth + 1]) {
                if (_nspStack[i * 2] == "" && _nspStack[i * 2 + 1] != "") {
                    throw IllegalStateException("Cannot set default namespace for elements in no namespace")
                }
            }
        }
        elementStack[esp++] = namespace
        elementStack[esp++] = prefix
        elementStack[esp] = localName
        writer.append('<')
        if (prefix.isNotEmpty()) {
            writer.append(prefix)
            writer.append(':')
        }
        writer.append(localName)
        isPartiallyOpenTag = true

        namespaceHolder.incDepth()
        ensureNamespaceIfRepairing(namespace, prefix)
    }

    private fun ensureNamespaceIfRepairing(namespace: String?, prefix: String?) {
        if (isRepairNamespaces && namespace != null && namespace.isNotEmpty() && prefix != null) {
            // TODO fix more cases than missing namespaces with given prefix and uri
            if (namespaceHolder.getNamespaceUri(prefix) != namespace) {
                namespaceAttr(prefix, namespace)
            }
        }
    }

    override fun comment(text: String) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        _comment(text)
    }

    override fun text(text: String) {
        this._text(text)
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    override fun cdsect(text: String) {
        _cdsect(text)
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    override fun entityRef(text: String) {
        _entityRef(text)
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    override fun processingInstruction(text: String) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        _processingInstruction(text)
    }

    override fun ignorableWhitespace(text: String) {
        triggerStartDocument() // whitespace is not allowed before the xml declaration
        checkPending(false)
        for (c in text) {
            if (!(c == '\n' || c == '\r' || c == '\t' || c == ' ')) {
                throw IllegalArgumentException("\"$text\" is not ignorable whitespace")
            }
        }
        writer.append(text)
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    override fun attribute(namespace: String?, name: String, prefix: String?, value: String) {
        if (prefix != null && prefix.isNotEmpty() && namespace != null && namespace.isNotEmpty()) {
            setPrefix(prefix, namespace)
            ensureNamespaceIfRepairing(namespace, prefix)
        }
        this._attribute(namespace, prefix ?: "", name, value)
    }

    override fun docdecl(text: String) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        _docdecl(text)
    }

    /**
     * {@inheritDoc}
     * @param version Unfortunately the serializer is forced to version 1.0
     */
    override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        _startDocument(encoding, standalone)
    }

    override fun endDocument() {
        assert(depth == 0)
        _endDocument()
    }

    override fun endTag(namespace: String?, localName: String, prefix: String?) {
        namespaceHolder.decDepth()
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        _endTag(namespace, localName)
    }

    override fun setPrefix(prefix: String, namespaceUri: String) {
        if (namespaceUri != getNamespaceUri(prefix)) {
            namespaceHolder.addPrefixToContext(prefix, namespaceUri)
            _setPrefix(prefix, namespaceUri)
        }
    }

    override fun namespaceAttr(namespacePrefix: String, namespaceUri: String) {
        namespaceHolder.addPrefixToContext(namespacePrefix, namespaceUri)
        if (namespacePrefix.isNotEmpty()) {
            this._attribute(XMLNS_ATTRIBUTE_NS_URI, namespacePrefix, namespaceUri)
        } else {
            this._attribute(NULL_NS_URI, XMLNS_ATTRIBUTE, namespaceUri)
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
        const val TAG_DEPTH_NOT_TAG = -1
        const val TAG_DEPTH_FORCE_INDENT_NEXT = Int.MAX_VALUE
    }


    private enum class WriteState {
        BeforeDocument,
        AfterXmlDecl,
        AfterDocTypeDecl,
        InTagContent,
        Finished
    }

}

