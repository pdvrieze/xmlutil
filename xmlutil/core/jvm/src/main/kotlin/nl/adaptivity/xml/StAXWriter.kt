/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.xml

import java.io.OutputStream
import java.io.Writer
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import javax.xml.namespace.NamespaceContext
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamWriter
import javax.xml.transform.Result

actual typealias PlatformXmlWriter = StAXWriter

/**
 * An implementation of [XmlWriter] that uses an underlying stax writer.
 * Created by pdvrieze on 16/11/15.
 */
class StAXWriter(val delegate: XMLStreamWriter, val omitXmlDecl: Boolean = false) : XmlWriter {
    override var depth: Int = 0
        private set

    @Throws(XMLStreamException::class)
    constructor(writer: Writer, repairNamespaces: Boolean, omitXmlDecl: Boolean = false)
        : this(newFactory(repairNamespaces).createXMLStreamWriter(writer), omitXmlDecl)

    @Throws(XMLStreamException::class)
    constructor(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean, omitXmlDecl: Boolean = false)
        : this(newFactory(repairNamespaces).createXMLStreamWriter(outputStream, encoding), omitXmlDecl)

    @Throws(XMLStreamException::class)
    constructor(result: Result, repairNamespaces: Boolean, omitXmlDecl: Boolean = false)
        : this(newFactory(repairNamespaces).createXMLStreamWriter(result), omitXmlDecl)

    @Throws(XmlException::class)
    override fun startTag(namespace: String?, localName: String, prefix: String?) {
        depth++
        try {
            delegate.writeStartElement(prefix, localName, namespace)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    @Throws(XmlException::class)
    override fun endTag(namespace: String?, localName: String, prefix: String?) {
        delegate.writeEndElement()
        depth--
    }

    @Throws(XmlException::class)
    override fun endDocument() {
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
        try {
            delegate.close()
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    @Throws(XmlException::class)
    override fun flush() {
        try {
            delegate.flush()
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    @Throws(XmlException::class)
    override fun attribute(namespace: String?, name: String, prefix: String?, value: String) {
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
    override fun namespaceAttr(namespacePrefix: String, namespaceUri: String) = try {
        delegate.writeNamespace(namespacePrefix, namespaceUri)
    } catch (e: XMLStreamException) {
        throw XmlException(e)
    }

    @Throws(XmlException::class)
    override fun comment(text: String) {
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
    override fun cdsect(text: String) {
        try {
            delegate.writeCData(text)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    @Deprecated("", ReplaceWith("cdsect(data)"))
    @Throws(XmlException::class)
    fun writeCData(data: String) {
        cdsect(data)
    }

    @Throws(XmlException::class)
    override fun docdecl(text: String) {
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
    override fun entityRef(text: String) {
        try {
            delegate.writeEntityRef(text)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    @Deprecated("", ReplaceWith("entityRef(name)"))
    @Throws(XmlException::class)
    fun writeEntityRef(name: String) {
        entityRef(name)
    }

    @Throws(XmlException::class)
    override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) {
        if(!omitXmlDecl) {
            if (standalone != null && mtdWriteStartDocument != null && clsXmlStreamWriter?.isInstance(delegate) == true) {
                mtdWriteStartDocument.invoke(delegate, version, encoding, standalone)
            } else {
                delegate.writeStartDocument(encoding, version) // standalone doesn't work
            }
        }

    }

    @Throws(XmlException::class)
    override fun ignorableWhitespace(text: String) {
        text(text)
    }

    @Throws(XmlException::class)
    override fun text(text: String) {
        try {
            delegate.writeCharacters(text)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    @Throws(XmlException::class)
    override fun getPrefix(namespaceUri: String?): String? {
        try {
            return delegate.getPrefix(namespaceUri)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    @Throws(XmlException::class)
    override fun setPrefix(prefix: String, namespaceUri: String) {
        try {
            delegate.setPrefix(prefix, namespaceUri)
        } catch (e: XMLStreamException) {
            throw XmlException(e)
        }

    }

    @Throws(XmlException::class)
    override fun getNamespaceUri(prefix: String): String? {
        return delegate.namespaceContext.getNamespaceURI(prefix)
    }

    override var namespaceContext: NamespaceContext
        get() = delegate.namespaceContext
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

        private val clsXmlStreamWriter: Class<out XMLStreamWriter>?
        private val mtdWriteStartDocument: MethodHandle?

        init {
            var mh: MethodHandle? = null
            val clazz = try {
                Class.forName("org.codehaus.stax2.XMLStreamWriter").apply {
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
}
