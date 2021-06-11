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

import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE
import nl.adaptivity.xmlutil.XMLConstants.XMLNS_ATTRIBUTE_NS_URI
import nl.adaptivity.xmlutil.core.impl.PlatformXmlWriterBase
import java.io.OutputStream
import java.io.Writer
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.*
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamWriter
import javax.xml.transform.Result

actual typealias PlatformXmlWriter = StAXWriter

/**
 * An implementation of [XmlWriter] that uses an underlying stax writer.
 * Created by pdvrieze on 16/11/15.
 */
@OptIn(XmlUtilInternal::class)
class StAXWriter(
    val delegate: XMLStreamWriter,
    val xmlDeclMode: XmlDeclMode = XmlDeclMode.None,
    val autoCloseEmpty: Boolean = true
                ) : PlatformXmlWriterBase(),
                    XmlWriter {

    private val pendingWrites = mutableListOf<XmlEvent>()
    private val pendingNamespaces = mutableListOf<Namespace>()

    var lastTagDepth = -1

    private var state = State.Empty

    override var depth: Int = 0
        private set

    @Deprecated("Use version taking XmlDeclMode")
    constructor(delegate: XMLStreamWriter, omitXmlDecl: Boolean = false, autoCloseEmpty: Boolean = true) :
            this(delegate, XmlDeclMode.from(omitXmlDecl), autoCloseEmpty)

    @Deprecated("Use version taking XmlDeclMode")
    @Throws(XMLStreamException::class)
    constructor(writer: Writer, repairNamespaces: Boolean, omitXmlDecl: Boolean = false)
            : this(writer, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    @Throws(XMLStreamException::class)
    constructor(writer: Writer, repairNamespaces: Boolean, xmlDeclMode: XmlDeclMode = XmlDeclMode.None)
            : this(newFactory(repairNamespaces).createXMLStreamWriter(writer), xmlDeclMode)

    @Deprecated("Use version taking XmlDeclMode")
    @Throws(XMLStreamException::class)
    constructor(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean, omitXmlDecl: Boolean = false)
            : this(outputStream, encoding, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    @Throws(XMLStreamException::class)
    constructor(
        outputStream: OutputStream,
        encoding: String,
        repairNamespaces: Boolean,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.None
               )
            : this(
        newFactory(repairNamespaces).createXMLStreamWriter(outputStream, encoding), xmlDeclMode
                  )

    @Deprecated("Use version taking XmlDeclMode")
    @Throws(XMLStreamException::class)
    constructor(result: Result, repairNamespaces: Boolean, omitXmlDecl: Boolean = false)
            : this(result, repairNamespaces, XmlDeclMode.from(omitXmlDecl))

    @Throws(XMLStreamException::class)
    constructor(result: Result, repairNamespaces: Boolean, xmlDeclMode: XmlDeclMode = XmlDeclMode.None)
            : this(newFactory(repairNamespaces).createXMLStreamWriter(result), xmlDeclMode)

    @Throws(XmlException::class)
    override fun startTag(namespace: String?, localName: String, prefix: String?) = flushPending {
        if (state == State.Empty) startDocument(null, null, null)
        depth++
        _namespaceContext.incDepth() // already increase now
        if (autoCloseEmpty) {
            pendingWrites.add(XmlEvent.StartElementEvent(namespace ?: "", localName, prefix ?: "", _namespaceContext))
        } else {
            doStartTag(namespace, prefix, localName, false)
        }
    }

    private fun doStartTag(namespace: String?, prefix: String?, localName: String, isEmpty: Boolean) {
        depth-- // the depth was already increased because this can be called
        // from a pending context. This needs to be undone for indentation
        writeIndent()
        depth++

        try {
            if (namespace.isNullOrEmpty() &&
                prefix.isNullOrEmpty() &&
                delegate.namespaceContext?.getNamespaceURI("").isNullOrEmpty()
            ) {
                if (isEmpty) {
                    delegate.writeEmptyElement(localName)
                } else {
                    delegate.writeStartElement(localName)
                }
            } else {
                if (isEmpty) {
                    delegate.writeEmptyElement(prefix ?: XMLConstants.DEFAULT_NS_PREFIX, localName, namespace)
                } else {
                    delegate.writeStartElement(prefix ?: XMLConstants.DEFAULT_NS_PREFIX, localName, namespace)
                }
            }
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
    }

    private inline fun flushPending(isEndTag: Boolean = false, body: () -> Unit) {
        if (pendingWrites.isNotEmpty()) doFlushPending(isEndTag)
        pendingNamespaces.clear() // No need to write, just record
        return body()
    }

    private fun doFlushPending(isEndTag: Boolean = false) {
        val start = pendingWrites.first() as XmlEvent.StartElementEvent
        val (nsAttrs, regularAttrs) = pendingWrites.asSequence()
            .drop(1)
            .map { it as XmlEvent.Attribute }
            .partition { it.namespaceUri == XMLNS_ATTRIBUTE_NS_URI }
        pendingWrites.clear()
        doStartTag(start.namespaceUri, start.prefix, start.localName, isEndTag)
        for (attr in (nsAttrs.asSequence() + regularAttrs.asSequence())) {
            when {
                attr.namespaceUri != XMLNS_ATTRIBUTE_NS_URI
                     -> doAttribute(attr.namespaceUri, attr.prefix, attr.localName, attr.value)

                attr.prefix == ""
                     -> doNamespaceAttr("", attr.value)

                else -> doNamespaceAttr(attr.localName, attr.value)
            }
        }
    }

    @Throws(XmlException::class)
    override fun endTag(namespace: String?, localName: String, prefix: String?) {
        if (pendingWrites.isNotEmpty()) {
            doFlushPending(true) // if we write an empty tag don't write an end element
            depth--
            writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        } else {
            depth--
            writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
            delegate.writeEndElement()
        }
        _namespaceContext.decDepth()
    }


    private fun writeIndent(newDepth: Int = depth) {
        val indentSeq = indentSequence
        if (lastTagDepth >= 0 && indentSeq.isNotEmpty() && lastTagDepth != depth) {
            try {
                // Unset the indentation so that comments will not make things work correctly.
                indentSequence = emptyList()
                ignorableWhitespace("\n")
                repeat(depth) { indentSeq.forEach { it.writeTo(this) } }
            } finally {
                indentSequence = indentSeq
            }
        }
        lastTagDepth = newDepth
    }

    @Throws(XmlException::class)
    override fun endDocument() {
        assert(state == State.StartDocWritten)
        state = State.EndDocWritten
        assert(pendingWrites.isEmpty()) // no pending start tags allowed here
        assert(depth == 0) // Don't write this until really the end of the document
        try {
            delegate.writeEndDocument()
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    @Deprecated("", ReplaceWith("endDocument()"))
    @Throws(XmlException::class)
    fun writeEndDocument() {
        endDocument()
    }

    @Throws(XmlException::class)
    override fun close() {
        if (state != State.EndDocWritten) endDocument()
        try {
            delegate.close()
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    @Throws(XmlException::class)
    override fun flush() {
        try {
            if (pendingWrites.isNotEmpty()) doFlushPending(false)
            delegate.flush()
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    @Throws(XmlException::class)
    override fun attribute(namespace: String?, name: String, prefix: String?, value: String) {
        when {
            namespace == XMLNS_ATTRIBUTE_NS_URI -> {
                val newPrefix = if (prefix == XMLNS_ATTRIBUTE) name else ""
                namespaceAttr(newPrefix, value)
            }

            pendingWrites.isNotEmpty()          ->
                pendingWrites.add(XmlEvent.Attribute(namespace ?: "", name, prefix ?: "", value))

            else                                ->
                doAttribute(namespace, prefix, name, value)
        }
    }

    private fun doAttribute(namespace: String?, prefix: String?, name: String, value: String) {
        try {
            if (namespace.isNullOrEmpty() || prefix.isNullOrEmpty()) {
                delegate.writeAttribute(name, value)
            } else {
                delegate.writeAttribute(namespace, name, value)
            }
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
    }

    @Deprecated("", ReplaceWith("attribute(null, localName, null, value)"))
    @Throws(XmlException::class)
    fun writeAttribute(localName: String, value: String) {
        attribute(null, localName, null, value)
    }

    @Deprecated("", ReplaceWith("attribute(namespaceURI, localName, prefix, value)"))
    @Throws(XmlException::class)
    fun writeAttribute(prefix: String, namespaceURI: String, localName: String, value: String) {
        attribute(namespaceURI, localName, prefix, value)
    }

    @Deprecated("", ReplaceWith("attribute(namespaceURI, localName, null, value)"))
    @Throws(XmlException::class)
    fun writeAttribute(namespaceURI: String, localName: String, value: String) {
        attribute(namespaceURI, localName, null, value)
    }

    @Throws(XmlException::class)
    override fun namespaceAttr(namespacePrefix: String, namespaceUri: String) {
        _namespaceContext.addPrefix(namespacePrefix, namespaceUri)
        pendingNamespaces.add(XmlEvent.NamespaceImpl(namespacePrefix, namespaceUri))
        when {
            pendingWrites.isEmpty() -> doNamespaceAttr(namespacePrefix, namespaceUri)

            namespacePrefix == ""   -> pendingWrites.add(
                XmlEvent.Attribute(
                    XMLNS_ATTRIBUTE_NS_URI,
                    XMLNS_ATTRIBUTE,
                    "",
                    namespaceUri
                                  )
                                                        )
            else                    -> pendingWrites.add(
                XmlEvent.Attribute(
                    XMLNS_ATTRIBUTE_NS_URI,
                    namespacePrefix,
                    XMLNS_ATTRIBUTE,
                    namespaceUri
                                  )
                                                        )
        }
    }

    private fun doNamespaceAttr(namespacePrefix: String, namespaceUri: String) {
        try {
            delegate.writeNamespace(namespacePrefix, namespaceUri)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
    }

    @Throws(XmlException::class)
    override fun comment(text: String) = flushPending {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        try {
            delegate.writeComment(text)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
    }

    @Deprecated("", ReplaceWith("comment(data)"))
    @Throws(XmlException::class)
    fun writeComment(data: String) {
        comment(data)
    }

    @Throws(XmlException::class)
    override fun processingInstruction(text: String) {
        assert(pendingWrites.isEmpty())
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        val split = text.indexOf(' ')
        try {
            if (split > 0) {
                delegate.writeProcessingInstruction(text.substring(0, split), text.substring(split, text.length))
            } else {
                delegate.writeProcessingInstruction(text)
            }
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
    }

    @Deprecated("", ReplaceWith("processingInstruction(target)"))
    @Throws(XmlException::class)
    fun writeProcessingInstruction(target: String) {
        processingInstruction(target)
    }

    @Deprecated("", ReplaceWith("processingInstruction(target + \" \" + data)"))
    @Throws(XmlException::class)
    fun writeProcessingInstruction(target: String, data: String) {
        processingInstruction("$target $data")
    }

    @Throws(XmlException::class)
    override fun cdsect(text: String) = flushPending {
        try {
            delegate.writeCData(text)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    @Deprecated("", ReplaceWith("cdsect(data)"))
    @Throws(XmlException::class)
    fun writeCData(data: String) {
        cdsect(data)
    }

    @Throws(XmlException::class)
    override fun docdecl(text: String) {
        assert(pendingWrites.isEmpty())
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        try {
            delegate.writeDTD(text)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
    }

    @Deprecated("", ReplaceWith("docdecl(dtd)"))
    @Throws(XmlException::class)
    fun writeDTD(dtd: String) {
        docdecl(dtd)
    }

    @Throws(XmlException::class)
    override fun entityRef(text: String) = flushPending {
        try {
            delegate.writeEntityRef(text)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    @Deprecated("", ReplaceWith("entityRef(name)"))
    @Throws(XmlException::class)
    fun writeEntityRef(name: String) {
        entityRef(name)
    }

    @Throws(XmlException::class)
    override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) {
        state = State.StartDocWritten
        assert(pendingWrites.isEmpty())
        if (xmlDeclMode != XmlDeclMode.None) {
            val effectiveEncoding = when (xmlDeclMode) {
                XmlDeclMode.Minimal -> when (encoding?.toLowerCase(Locale.ENGLISH)?.startsWith("utf-")) {
                    false -> encoding
                    else  -> null
                }
                XmlDeclMode.Charset -> encoding ?: "UTF-8"
                else                -> encoding
            }

            writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT) // should be null as length is 0
            if (standalone != null && mtdWriteStartDocument != null && clsXmlStreamWriter?.isInstance(
                    delegate
                                                                                                     ) == true
            ) {
                mtdWriteStartDocument.invoke(delegate, version, effectiveEncoding, standalone)
            } else {
                delegate.writeStartDocument(effectiveEncoding, version) // standalone doesn't work
            }
        }

    }

    @Throws(XmlException::class)
    override fun ignorableWhitespace(text: String) = flushPending {
        text(text)
    }

    @Throws(XmlException::class)
    override fun text(text: String) = flushPending {
        try {
            delegate.writeCharacters(text)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    @Throws(XmlException::class)
    override fun getPrefix(namespaceUri: String?): String? {
        try {
            pendingNamespaces
                .firstOrNull { it.namespaceURI==namespaceUri }
                ?.let { return it.prefix }

            pendingWrites.asSequence()
                .filterIsInstance<XmlEvent.Attribute>()
                .firstOrNull() {
                    it.namespaceUri == XMLNS_ATTRIBUTE_NS_URI && it.value == namespaceUri
                }?.let { return if (it.prefix.isBlank()) "" else it.localName }

            return delegate.getPrefix(namespaceUri)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    @Throws(XmlException::class)
    override fun setPrefix(prefix: String, namespaceUri: String) {
        pendingNamespaces.add(XmlEvent.NamespaceImpl(prefix, namespaceUri))
        try {
            delegate.setPrefix(prefix, namespaceUri)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    @Throws(XmlException::class)
    override fun getNamespaceUri(prefix: String): String? {
        pendingNamespaces
            .firstOrNull { it.prefix==prefix }
            ?.let { return it.namespaceURI }
        pendingWrites.asSequence()
            .filterIsInstance<XmlEvent.Attribute>()
            .firstOrNull() {
                it.namespaceUri == XMLNS_ATTRIBUTE_NS_URI && (
                        (prefix.isEmpty() && it.prefix.isEmpty()) ||
                                (prefix.isNotEmpty() && prefix == it.localName))
            }?.let { return if (it.prefix.isBlank()) "" else it.localName }


        return delegate.namespaceContext.getNamespaceURI(prefix)
    }

    private val _namespaceContext = FreezableDelegatingNamespaceContext { delegate.namespaceContext }

    override var namespaceContext: NamespaceContext
        get() = _namespaceContext
        @Throws(XmlException::class)
        set(context) = if (depth == 0) {
            try {
                delegate.namespaceContext = context
            } catch (e: XMLStreamException) {
                throw XmlException(e)
            }

        } else {
            throw XmlException("Modifying the namespace context halfway in a document")
        }

    companion object {

        const val TAG_DEPTH_NOT_TAG = -1
        const val TAG_DEPTH_FORCE_INDENT_NEXT = Int.MAX_VALUE

        private val clsXmlStreamWriter: Class<out XMLStreamWriter>?
        private val mtdWriteStartDocument: MethodHandle?

        init {
            var mh: MethodHandle? = null
            val clazz = try {
                Class.forName("org.codehaus.stax2.XMLStreamWriter2").apply {
                    val m = getMethod("writeStartDocument", String::class.java, String::class.java, Boolean::class.java)
                    mh = MethodHandles.lookup().unreflect(m)
                }
            } catch (e: ClassNotFoundException) {
                null
            } catch (e: NoSuchMethodException) {
                null
            }

            //noinspection unchecked
            clsXmlStreamWriter = clazz?.asSubclass(XMLStreamWriter::class.java)
            mtdWriteStartDocument = mh
        }

        private fun newFactory(repairNamespaces: Boolean): XMLOutputFactory {
            val xmlOutputFactory = XMLOutputFactory.newFactory()
            xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, repairNamespaces)
            return xmlOutputFactory
        }

    }

    private enum class State { Empty, StartDocWritten, EndDocWritten }
}
