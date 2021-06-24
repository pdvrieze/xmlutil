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
 *
 * -- This file is derived from kXML (Copyright (c) 2002,2003, Stefan Haustein, Oberhausen, Rhld., Germany)
 */

package nl.adaptivity.xmlutil.core.impl

import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlException
import org.xmlpull.v1.XmlSerializer
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.*
import javax.xml.XMLConstants

/**
 * @suppress
 */
class BetterXmlSerializer : XmlSerializer {

    private lateinit var writer: Writer

    private var pending: Boolean = false
    private var auto: Int = 0
    private var depth: Int = 0

    private var elementStack = arrayOfNulls<String>(12)

    private var nspCounts = IntArray(4)
    private var nspStack = arrayOfNulls<String>(10)
    private var nspWritten = BooleanArray(5)

    private var indent = BooleanArray(4)
    private var unicode: Boolean = false
    private var encoding: String? = null
    private val escapeAggressive = false
    var xmlDeclMode = XmlDeclMode.None
    var addTrailingSpaceBeforeEnd = true
    private var state: WriteState = WriteState.BeforeDocument

    private fun checkPending(close: Boolean) {
        if (!pending) {
            return
        }

        depth++
        pending = false

        if (indent.size <= depth) {
            val hlp = BooleanArray(depth + 4)
            System.arraycopy(indent, 0, hlp, 0, depth)
            indent = hlp
        }
        indent[depth] = indent[depth - 1]

        if (nspCounts.size <= depth + 3) {
            val hlp = IntArray(depth + 8)
            System.arraycopy(nspCounts, 0, hlp, 0, depth + 2)
            nspCounts = hlp
        }

        nspCounts[depth + 2] = nspCounts[depth + 1]
        // Only set the second level here as the first level may already have pending namespaces

        val endOfTag = when {
            !close                    -> ">"
            addTrailingSpaceBeforeEnd -> " />"
            else                      -> "/>"
        }
        writer.write(endOfTag)
    }

    @Throws(IOException::class)
    private fun writeEscaped(s: String, quot: Int) {

        loop@ for (i in 0 until s.length) {
            when (val c = s[i]) {
                '&'              -> writer.write("&amp;")
                '>'              -> writer.write("&gt;")
                '<'              -> writer.write("&lt;")
                '"', '\''        -> {
                    if (c.code == quot) {
                        writer.write(if (c == '"') "&quot;" else "&apos;")
                        break@loop
                    }
                    if (escapeAggressive && quot != -1) {
                        writer.write("&#${c.code};")
                    } else {
                        writer.write(c.code)
                    }
                }
                '\n', '\r', '\t' -> if (escapeAggressive && quot != -1) {
                    writer.write("&#${c.code};")
                } else {
                    writer.write(c.code)
                }
                else             ->
                    //if(c < ' ')
                    //	throw new IllegalArgumentException("Illegal control code:"+((int) c));
                    if (escapeAggressive && (c < ' ' || c == '@' || c.code > 127 && !unicode)) {
                        writer.write("&#${c.code};")
                    } else {
                        writer.write(c.code)
                    }
            }
        }
    }

    override fun docdecl(dd: String) {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (state) {
            WriteState.BeforeDocument -> {
                if (xmlDeclMode != XmlDeclMode.None) {
                    startDocument(null, null)
                }
                state = WriteState.AfterXmlDecl
            }
            WriteState.AfterXmlDecl -> {}
            else ->
                throw XmlException("Writing a DTD is only allowed once, in the prolog")
        }
        state = WriteState.AfterDocTypeDecl
        writer.write("<!DOCTYPE")
        writer.write(dd)
        writer.write(">")
    }

    override fun endDocument() {
        if (state!=WriteState.InTagContent) {
            throw XmlException("Attempting to end document when in invalid state: $state")
        }
        while (depth > 0) {
            endTag(elementStack[depth * 3 - 3], elementStack[depth * 3 - 1]!!)
        }
        flush()
    }

    @Throws(IOException::class)
    override fun entityRef(name: String) {
        checkPending(false)
        writer.write('&')
        writer.write(name)
        writer.write(';')
    }

    override fun getFeature(name: String): Boolean {
        //return false;
        return if ("http://xmlpull.org/v1/doc/features.html#indent-output" == name)
            indent[depth]
        else
            false
    }

    override fun getPrefix(namespace: String, create: Boolean): String? {
        return getPrefix(namespace, false, create)
    }

    private fun getPrefix(namespace: String, includeDefault: Boolean, create: Boolean): String? {

        run {
            var i = nspCounts[depth + 1] * 2 - 2
            while (i >= 0) {
                if (nspStack[i + 1] == namespace && (includeDefault || nspStack[i] != "")) {
                    var candidate: String? = nspStack[i]
                    for (j in i + 2 until nspCounts[depth + 1] * 2) {
                        if (nspStack[j] == candidate) {
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
                prefix = "n" + auto++
                var i = nspCounts[depth + 1] * 2 - 2
                while (i >= 0) {
                    if (prefix == nspStack[i]) {
                        prefix = null
                        break
                    }
                    i -= 2
                }
            } while (prefix == null)
        }

        val p = pending
        pending = false
        setPrefix(prefix, namespace)
        pending = p
        return prefix
    }

    private fun getNamespace(prefix: String, includeDefault: Boolean = false): String? {

        var i = nspCounts[depth + 1] * 2 - 2
        while (i >= 0) {
            if (nspStack[i] == prefix && (includeDefault || nspStack[i] != "")) {
                var candidate: String? = nspStack[i + 1]
                for (j in i + 2 until nspCounts[depth + 1] * 2) {
                    if (nspStack[j + 1] == candidate) {
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

    override fun getProperty(name: String): Any {
        throw RuntimeException("Unsupported property")
    }

    @Throws(IOException::class)
    override fun ignorableWhitespace(s: String) {
        triggerStartDocument() // whitespace is not allowed before the xml declaration
        text(s)
    }

    override fun setFeature(name: String, value: Boolean) {
        if ("http://xmlpull.org/v1/doc/features.html#indent-output" == name) {
            indent[depth] = value
        } else {
            throw RuntimeException("Unsupported Feature")
        }
    }

    override fun setProperty(name: String, value: Any) {
        throw RuntimeException("Unsupported Property:$value")
    }

    @Throws(IOException::class)
    override fun setPrefix(prefix: String?, namespace: String?) {

        val depth: Int
        depth = this.depth + (if (pending) 2 else 1)

        var i = nspCounts[depth] * 2 - 2
        while (i >= 0) {
            if (nspStack[i + 1] == (namespace ?: "") && nspStack[i] == (prefix ?: "")) {
                // bail out if already defined
                return
            }
            i -= 2
        }


        val c = nspCounts[depth]
        nspCounts[depth] = c + 1
        nspCounts[depth + 1] = c + 1
        var pos = c shl 1

        addSpaceToNspStack()

        nspStack[pos++] = prefix ?: ""
        nspStack[pos] = namespace ?: ""
        nspWritten[nspCounts[depth] - 1] = false
    }

    private fun addSpaceToNspStack() {
        val nspCount = nspCounts[if (pending) depth + 1 else depth]
        val pos = nspCount shl 1
        if (nspStack.size < pos + 2) {
            val hlp = arrayOfNulls<String>(nspStack.size + 16)
            System.arraycopy(nspStack, 0, hlp, 0, pos)
            nspStack = hlp

            val help = BooleanArray(nspWritten.size + 8)
            System.arraycopy(nspWritten, 0, help, 0, nspCount)
            nspWritten = help
        }
    }

    override fun setOutput(writer: Writer) {
        this.writer = writer

        nspCounts[0] = 3
        nspCounts[1] = 3
        nspCounts[2] = 3
        nspStack[0] = ""
        nspStack[1] = ""
        nspStack[2] = "xml"
        nspStack[3] = "http://www.w3.org/XML/1998/namespace"
        nspStack[4] = "xmlns"
        nspStack[5] = "http://www.w3.org/2000/xmlns/"
        pending = false
        auto = 0
        depth = 0

        unicode = false
    }

    @Throws(IOException::class)
    override fun setOutput(os: OutputStream?, encoding: String?) {
        if (os == null) {
            throw IllegalArgumentException()
        }
        val streamWriter = when (encoding) {
            null -> OutputStreamWriter(os)
            else -> OutputStreamWriter(os, encoding)
        }
        setOutput(streamWriter)

        this.encoding = encoding
        if (encoding?.lowercase(Locale.ENGLISH)?.startsWith("utf") == true) {
            unicode = true
        }
    }

    override fun startDocument(encoding: String?, standalone: Boolean?) {
        if (state!=WriteState.BeforeDocument) {
            throw XmlException("Attempting to write start document after document already started")
        }
        state = WriteState.AfterXmlDecl

        if (xmlDeclMode!=XmlDeclMode.None) {
            writer.write("<?xml version='1.0'")

            if (encoding != null) {
                this.encoding = encoding
                if (encoding.lowercase(Locale.ENGLISH).startsWith("utf")) {
                    unicode = true
                }
            } else if (xmlDeclMode == XmlDeclMode.Charset) {
                this.encoding = "UTF-8"
            }

            if (xmlDeclMode!=XmlDeclMode.Minimal || !unicode) {

                this.encoding?.let { enc ->
                    writer.write(" encoding='")
                    writer.write(enc)
                    writer.write('\'')
                }

                if (standalone != null) {
                    writer.write(" standalone='")
                    writer.write(if (standalone) "yes" else "no")
                    writer.write('\'')
                }
            }
            writer.write("?>")
            indent[depth]=true
        }
    }

    @Throws(IOException::class)
    override fun startTag(namespace: String?, name: String): BetterXmlSerializer {
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (state) {
            WriteState.BeforeDocument -> {
                if (xmlDeclMode != XmlDeclMode.None) {
                    startDocument(null, null)
                }
            }
            WriteState.Finished ->
                throw XmlException("Attempting to write tag after the document finished")
        }
        state = WriteState.InTagContent

        checkPending(false)

        //        if (namespace == null)
        //            namespace = "";

        if (indent[depth]) {
            writer.write("\r\n")
            for (i in 0 until depth) {
                writer.write("  ")
            }
        }

        var esp = depth * 3

        if (elementStack.size < esp + 3) {
            val hlp = arrayOfNulls<String>(elementStack.size + 12)
            System.arraycopy(elementStack, 0, hlp, 0, esp)
            elementStack = hlp
        }

        val prefix = namespace?.let { getPrefix(namespace, includeDefault = true, create = true) } ?: ""

        if (namespace.isNullOrEmpty()) {
            for (i in nspCounts[depth] until nspCounts[depth + 1]) {
                if (nspStack[i * 2] == "" && nspStack[i * 2 + 1] != "") {
                    throw IllegalStateException("Cannot set default namespace for elements in no namespace")
                }
            }
        }

        elementStack[esp++] = namespace
        elementStack[esp++] = prefix
        elementStack[esp] = name

        writer.write('<')
        if (prefix.isNotEmpty()) {
            writer.write(prefix)
            writer.write(':')
        }

        writer.write(name)

        pending = true

        return this
    }

    @Throws(IOException::class)
    override fun attribute(namespace: String?, name: String, value: String): BetterXmlSerializer {
        if (!pending) {
            throw IllegalStateException("illegal position for attribute")
        }

        val ns = namespace ?: ""

        if (ns == XMLConstants.XMLNS_ATTRIBUTE_NS_URI) {
            return namespace(name, value) // If it is a namespace attribute, just go there.
        } else if (ns == XMLConstants.NULL_NS_URI && XMLConstants.XMLNS_ATTRIBUTE == name) {
            return namespace("", value) // If it is a namespace attribute, just go there.
        }

        //		depth--;
        //		pending = false;

        val prefix = when (ns) {
            ""   -> ""
            else -> getPrefix(ns, includeDefault = false, create = true)
        }

        writer.write(' ')
        if ("" != prefix) {
            writer.write(prefix!!)
            writer.write(':')
        }
        writer.write(name)
        writer.write('=')
        val q = if (value.indexOf('"') == -1) '"' else '\''
        writer.write(q.code)
        writeEscaped(value, q.code)
        writer.write(q.code)

        return this
    }

    @Throws(IOException::class)
    fun attribute(namespace: String?, prefix: String, name: String, value: String): BetterXmlSerializer {
        if (!pending) {
            throw IllegalStateException("illegal position for attribute")
        }

        val ns = namespace ?: ""

        if (ns == XMLConstants.XMLNS_ATTRIBUTE_NS_URI) {
            return namespace(name, value) // If it is a namespace attribute, just go there.
        } else if (ns == XMLConstants.NULL_NS_URI && XMLConstants.XMLNS_ATTRIBUTE == name) {
            return namespace("", value) // If it is a namespace attribute, just go there.
        }

        val actualPrefix = if (prefix.isNotEmpty()) {
            if (getNamespace(prefix) != namespace) {
                getPrefix(ns, includeDefault = false, create = true) ?: ""
            } else prefix
        } else {
            prefix
        }

        //		depth--;
        //		pending = false;

        writer.write(' ')
        if ("" != actualPrefix) {
            writer.write(actualPrefix)
            writer.write(':')
        }
        writer.write(name)
        writer.write('=')
        val q = if (value.indexOf('"') == -1) '"' else '\''
        writer.write(q.code)
        writeEscaped(value, q.code)
        writer.write(q.code)

        return this
    }

    @Throws(IOException::class)
    fun namespace(prefix: String, namespace: String?): BetterXmlSerializer {

        if (!pending) {
            throw IllegalStateException("illegal position for attribute")
        }

        var wasSet = false
        for (i in nspCounts[depth] until nspCounts[depth + 1]) {
            if (prefix == nspStack[i * 2]) {
                if (nspStack[i * 2 + 1] != namespace) { // If we find the prefix redefined within the element, bail out
                    throw IllegalArgumentException(
                        "Attempting to bind prefix to conflicting values in one element"
                                                  )
                }
                if (nspWritten[i]) {
                    // otherwise just ignore the request.
                    return this
                }
                nspWritten[i] = true
                wasSet = true
                break
            }
        }

        if (!wasSet) { // Don't use setPrefix as we know it isn't there
            addSpaceToNspStack()
            val c = nspCounts[depth + 1]
            nspCounts[depth + 1] = c + 1
            nspCounts[depth + 2] = c + 1
            val pos = c shl 1
            nspStack[pos] = prefix
            nspStack[pos + 1] = namespace
            nspWritten[pos shr 1] = true
        }

        val nsNotNull = namespace ?: ""

        writer.write(' ')
        writer.write(XMLConstants.XMLNS_ATTRIBUTE)
        if (prefix.isNotEmpty()) {
            writer.write(':')
            writer.write(prefix)
        }
        writer.write('=')
        val q = if (nsNotNull.indexOf('"') == -1) '"' else '\''
        writer.write(q.code)
        writeEscaped(nsNotNull, q.code)
        writer.write(q.code)

        return this
    }

    @Throws(IOException::class)
    override fun flush() {
        checkPending(false)
        writer.flush()
    }

    @Throws(IOException::class)
    override fun endTag(namespace: String?, name: String): BetterXmlSerializer {

        if (!pending) {
            depth--
        }
        //        if (namespace == null)
        //          namespace = "";

        if (namespace == null && elementStack[depth * 3] != null
            || namespace != null && namespace != elementStack[depth * 3]
            || elementStack[depth * 3 + 2] != name
        ) {
            throw IllegalArgumentException("</{$namespace}$name> does not match start")
        }

        if (pending) {
            checkPending(true)
            depth--
        } else {
            if (indent[depth + 1]) {
                writer.write("\r\n")
                for (i in 0 until depth) {
                    writer.write("  ")
                }
            }

            writer.write("</")
            val prefix = elementStack[depth * 3 + 1]!!
            if ("" != prefix) {
                writer.write(prefix)
                writer.write(':')
            }
            writer.write(name)
            writer.write('>')
        }

        val c = nspCounts[depth]
        nspCounts[depth + 1] = c
        if (!pending) nspCounts[depth + 2] = c
        return this
    }

    override fun getNamespace(): String? {
        return if (getDepth() == 0) null else elementStack[getDepth() * 3 - 3]
    }

    override fun getName(): String? {
        return if (getDepth() == 0) null else elementStack[getDepth() * 3 - 1]
    }

    override fun getDepth(): Int {
        return if (pending) depth + 1 else depth
    }

    @Throws(IOException::class)
    override fun text(text: String): BetterXmlSerializer {
        checkPending(false)
        indent[depth] = false
        writeEscaped(text, -1)
        return this
    }

    @Throws(IOException::class)
    override fun text(text: CharArray, start: Int, len: Int): BetterXmlSerializer {
        text(String(text, start, len))
        return this
    }

    @Throws(IOException::class)
    override fun cdsect(data: String) {
        checkPending(false)
        writer.write("<![CDATA[")
        writer.write(data)
        writer.write("]]>")
    }

    @Throws(IOException::class)
    override fun comment(comment: String) {
        triggerStartDocument() // No content before XmlDeclaration
        checkPending(false)
        writer.write("<!--")
        writer.write(comment)
        writer.write("-->")
    }

    @Throws(IOException::class)
    override fun processingInstruction(pi: String) {
        triggerStartDocument()

        checkPending(false)
        writer.write("<?")
        writer.write(pi)
        writer.write("?>")
    }

    private fun triggerStartDocument() {
        // Non-before states are not modified
        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (state) {
            WriteState.BeforeDocument -> {
                if (xmlDeclMode != XmlDeclMode.None) {
                    startDocument(null, null)
                }
                state = WriteState.AfterXmlDecl
            }
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Writer.write(c: Char) = write(c.code)

private enum class WriteState {
    BeforeDocument,
    AfterXmlDecl,
    AfterDocTypeDecl,
    InTagContent,
    Finished
}
