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

import nl.adaptivity.xmlutil.core.impl.BetterXmlSerializer
import nl.adaptivity.xmlutil.core.impl.NamespaceHolder
import nl.adaptivity.xmlutil.core.impl.PlatformXmlWriterBase
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlSerializer
import java.io.IOException
import java.io.OutputStream
import java.io.Writer
import javax.xml.XMLConstants.*
import javax.xml.namespace.NamespaceContext

actual typealias PlatformXmlWriter = AndroidXmlWriter

/**
 * An android implementation of XmlWriter.
 * Created by pdvrieze on 15/11/15.
 */
class AndroidXmlWriter : PlatformXmlWriterBase, XmlWriter {

    private val namespaceHolder = NamespaceHolder()
    private val isRepairNamespaces: Boolean
    private val writer: XmlSerializer

    private var lastTagDepth = TAG_DEPTH_NOT_TAG

    override val namespaceContext: NamespaceContext
        get() = namespaceHolder.namespaceContext

    override val depth: Int
        get() = namespaceHolder.depth

    @Throws(XmlPullParserException::class, IOException::class)
    @Deprecated("Use xmlDeclMode")
    constructor(writer: Writer, repairNamespaces: Boolean = true, omitXmlDecl: Boolean) :
            this (writer, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    @Throws(XmlPullParserException::class, IOException::class)
    @JvmOverloads
    constructor(writer: Writer, repairNamespaces: Boolean = true, xmlDeclMode: XmlDeclMode = XmlDeclMode.None) :
            this(repairNamespaces, xmlDeclMode) {
        this.writer.setOutput(writer)
        initWriter(this.writer)
    }

    @Throws(XmlPullParserException::class)
    private constructor(repairNamespaces: Boolean, xmlDeclMode: XmlDeclMode = XmlDeclMode.None) {
        isRepairNamespaces = repairNamespaces
        writer = BetterXmlSerializer().apply { this.xmlDeclMode = xmlDeclMode }
        initWriter(writer)
    }

    @Deprecated("Use xmlDeclMode")
    @Throws(XmlPullParserException::class, IOException::class)
    constructor(
        outputStream: OutputStream,
        encoding: String,
        repairNamespaces: Boolean = true,
        omitXmlDecl: Boolean
               ) :
            this(outputStream, encoding, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    @Throws(XmlPullParserException::class, IOException::class)
    @JvmOverloads
    constructor(
        outputStream: OutputStream,
        encoding: String,
        repairNamespaces: Boolean = true,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None
               ) :
            this(repairNamespaces, xmlDeclMode) {

        writer.setOutput(outputStream, encoding)
        initWriter(writer)
    }

    private fun initWriter(writer: XmlSerializer) {
        try {
            writer.setPrefix(XMLNS_ATTRIBUTE, XMLNS_ATTRIBUTE_NS_URI)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    @JvmOverloads
    constructor(serializer: XmlSerializer, repairNamespaces: Boolean = true) {
        writer = serializer
        isRepairNamespaces = repairNamespaces
        initWriter(writer)
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


    @Throws(XmlException::class)
    override fun flush() {
        try {
            writer.flush()
        } catch (e: IOException) {
            throw XmlException(e)
        }

    }

    @Throws(XmlException::class)
    override fun startTag(namespace: String?, localName: String, prefix: String?) {
        writeIndent()

        try {
            if (namespace != null && namespace.isNotEmpty()) {
                writer.setPrefix(prefix ?: "", namespace)
            }
            writer.startTag(namespace, localName)
            namespaceHolder.incDepth()
            ensureNamespaceIfRepairing(namespace, prefix)
        } catch (e: IOException) {
            throw XmlException(e)
        }
    }

    @Throws(XmlException::class)
    private fun ensureNamespaceIfRepairing(namespace: String?, prefix: String?) {
        if (isRepairNamespaces && namespace != null && namespace.isNotEmpty() && prefix != null) {
            // TODO fix more cases than missing namespaces with given prefix and uri
            if (namespaceHolder.getNamespaceUri(prefix) != namespace) {
                namespaceAttr(prefix, namespace)
            }
        }
    }

    @Throws(XmlException::class)
    override fun comment(text: String) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        writer.comment(text)
    }

    @Throws(XmlException::class)
    override fun text(text: String) {
        writer.text(text)
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    @Throws(XmlException::class)
    override fun cdsect(text: String) {
        writer.cdsect(text)
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    @Throws(XmlException::class)
    override fun entityRef(text: String) {
        writer.entityRef(text)
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    @Throws(XmlException::class)
    override fun processingInstruction(text: String) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        writer.processingInstruction(text)
    }

    @Throws(XmlException::class)
    override fun ignorableWhitespace(text: String) {
        writer.ignorableWhitespace(text)
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    @Throws(XmlException::class)
    override fun attribute(namespace: String?, name: String, prefix: String?, value: String) {
        if (prefix != null && prefix.isNotEmpty() && namespace != null && namespace.isNotEmpty()) {
            setPrefix(prefix, namespace)
            ensureNamespaceIfRepairing(namespace, prefix)
        }
        val writer = writer
        if (writer is BetterXmlSerializer) {
            writer.attribute(namespace, prefix ?: "", name, value)
        } else {
            writer.attribute(namespace, name, value)
        }
    }

    @Throws(XmlException::class)
    override fun docdecl(text: String) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        writer.docdecl(text)
    }

    /**
     * {@inheritDoc}
     * @param version Unfortunately the serializer is forced to version 1.0
     */
    @Throws(XmlException::class)
    override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        writer.startDocument(encoding, standalone)
    }

    @Throws(XmlException::class)
    override fun endDocument() {
        assert(depth == 0)
        writer.endDocument()
    }

    @Throws(XmlException::class)
    override fun endTag(namespace: String?, localName: String, prefix: String?) {
        namespaceHolder.decDepth()
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        writer.endTag(namespace, localName)
    }

    @Throws(XmlException::class)
    override fun setPrefix(prefix: String, namespaceUri: String) {
        if (namespaceUri != getNamespaceUri(prefix)) {
            namespaceHolder.addPrefixToContext(prefix, namespaceUri)
            writer.setPrefix(prefix, namespaceUri)
        }
    }

    @Throws(XmlException::class)
    override fun namespaceAttr(namespacePrefix: String, namespaceUri: String) {
        namespaceHolder.addPrefixToContext(namespacePrefix, namespaceUri)
        if (namespacePrefix.isNotEmpty()) {
            writer.attribute(XMLNS_ATTRIBUTE_NS_URI, namespacePrefix, namespaceUri)
        } else {
            writer.attribute(NULL_NS_URI, XMLNS_ATTRIBUTE, namespaceUri)
        }
    }

    override fun getNamespaceUri(prefix: String): String? {
        return namespaceHolder.getNamespaceUri(prefix)
    }

    override fun getPrefix(namespaceUri: String?): String? {
        return namespaceUri?.let { namespaceHolder.getPrefix(it) }
    }

    @Throws(XmlException::class)
    override fun close() {
        namespaceHolder.clear()
    }

    companion object {
        const val TAG_DEPTH_NOT_TAG = -1
        const val TAG_DEPTH_FORCE_INDENT_NEXT = Int.MAX_VALUE
    }
}

