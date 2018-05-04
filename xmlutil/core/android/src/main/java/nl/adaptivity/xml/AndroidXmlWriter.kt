/*
 * Copyright (c) 2018.
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

import nl.adaptivity.lib.xmlutil.BuildConfig
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlSerializer

import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext

import java.io.IOException
import java.io.OutputStream
import java.io.Writer

actual typealias PlatformXmlWriter = AndroidXmlWriter

/**
 * An android implementation of XmlWriter.
 * Created by pdvrieze on 15/11/15.
 */
class AndroidXmlWriter : XmlWriter {

    private val namespaceHolder = NamespaceHolder()
    private val isRepairNamespaces: Boolean
    private val writer: XmlSerializer

    @Throws(XmlPullParserException::class, IOException::class)
    @JvmOverloads constructor(writer: Writer, repairNamespaces: Boolean = true, omitXmlDecl: Boolean = false) : this(repairNamespaces, omitXmlDecl) {
        this.writer.setOutput(writer)
        initWriter(this.writer)
    }

    @Throws(XmlPullParserException::class)
    private constructor(repairNamespaces: Boolean, omitXmlDecl: Boolean) {
        isRepairNamespaces = repairNamespaces
        writer = BetterXmlSerializer().apply { isOmitXmlDecl = omitXmlDecl }
        initWriter(writer)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    @JvmOverloads constructor(outputStream: OutputStream, encoding: String, repairNamespaces: Boolean = true, omitXmlDecl: Boolean = false) :
        this(repairNamespaces, omitXmlDecl) {
        writer.setOutput(outputStream, encoding)
        initWriter(writer)
    }

    private fun initWriter(writer: XmlSerializer) {
        try {
            writer.setPrefix(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI)
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
        writer.comment(text)
    }

    @Throws(XmlException::class)
    override fun text(text: String) {
        writer.text(text)
    }

    @Throws(XmlException::class)
    override fun cdsect(text: String) {
        writer.cdsect(text)
    }

    @Throws(XmlException::class)
    override fun entityRef(text: String) {
        writer.entityRef(text)
    }

    @Throws(XmlException::class)
    override fun processingInstruction(text: String) {
        writer.processingInstruction(text)
    }

    @Throws(XmlException::class)
    override fun ignorableWhitespace(text: String) {
        writer.ignorableWhitespace(text)
    }

    @Throws(XmlException::class)
    override fun attribute(namespace: String?, name: String, prefix: String?, value: String) {
        if (prefix != null && namespace != null) {
            setPrefix(prefix, namespace)
        }
        writer.attribute(namespace, name, value)
        ensureNamespaceIfRepairing(namespace, prefix)
    }

    @Throws(XmlException::class)
    override fun docdecl(text: String) {
        writer.docdecl(text)
    }

    /**
     * {@inheritDoc}
     * @param version Unfortunately the serializer is forced to version 1.0
     */
    @Throws(XmlException::class)
    override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) {
        writer.startDocument(encoding, standalone)
    }

    @Throws(XmlException::class)
    override fun endDocument() {
        if (BuildConfig.DEBUG && depth != 0) throw AssertionError()
        writer.endDocument()
    }

    @Throws(XmlException::class)
    override fun endTag(namespace: String?, localName: String, prefix: String?) {
        writer.endTag(namespace, localName)
        namespaceHolder.decDepth()
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
            writer.attribute(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, namespacePrefix,
                             namespaceUri)
        } else {
            writer.attribute(XMLConstants.NULL_NS_URI, XMLConstants.XMLNS_ATTRIBUTE, namespaceUri)
        }
    }

    override val namespaceContext: NamespaceContext
        get() = namespaceHolder.namespaceContext

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


    override val depth: Int
        get() = namespaceHolder.depth

}

