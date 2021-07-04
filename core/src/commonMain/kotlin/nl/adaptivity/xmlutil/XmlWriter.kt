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
@file:JvmMultifileClass
@file:JvmName("XmlWriterUtil")

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.XMLConstants.DEFAULT_NS_PREFIX
import nl.adaptivity.xmlutil.XMLConstants.NULL_NS_URI
import nl.adaptivity.xmlutil.core.impl.multiplatform.Closeable
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.core.internal.countIndentedLength
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

/**
 * Interface representing the (wrapper) type that allows generating xml documents.
 */
public interface XmlWriter : Closeable {

    /**
     * The current depth into the tree.
     */
    public val depth: Int

    /** The indentation string to use for autoindenting the output */
    public var indentString: String

    /** The indentation level to use for autoindenting the output */
    public var indent: Int
        @Deprecated("Use indentString for better accuracy")
        get() = indentString.countIndentedLength()
        set(value) {
            indentString = " ".repeat(value)
        }

    @Deprecated(
        "Use the version that takes strings",
        ReplaceWith("setPrefix(prefix.toString(), namespaceUri.toString())")
    )
    public fun setPrefix(prefix: CharSequence, namespaceUri: CharSequence) {
        setPrefix(prefix.toString(), namespaceUri.toString())
    }

    /**
     * Bind the prefix to the given uri for this element.
     */
    public fun setPrefix(prefix: String, namespaceUri: String)

    @Deprecated(
        "Use the version that takes strings",
        ReplaceWith("namespaceAttr(namespacePrefix.toString(), namespaceUri.toString())")
    )
    public fun namespaceAttr(namespacePrefix: CharSequence, namespaceUri: CharSequence) {
        namespaceAttr(namespacePrefix.toString(), namespaceUri.toString())
    }

    public fun namespaceAttr(namespacePrefix: String, namespaceUri: String)

    public fun namespaceAttr(namespace: Namespace) {
        namespaceAttr(namespace.prefix, namespace.namespaceURI)
    }

    override fun close()

    /**
     * Flush all state to the underlying buffer
     */
    public fun flush()

    /**
     * Write a start tag.
     * @param namespace The namespace to use for the tag.
     *
     * @param localName The local name for the tag.
     *
     * @param prefix The prefix to use, or `null` for the namespace to be assigned automatically
     */
    public fun startTag(namespace: String?, localName: String, prefix: String?)

    /**
     * Write a comment.
     * @param text The comment text
     */
    public fun comment(text: String)

    /**
     * Write text.
     * @param text The text content.
     */
    public fun text(text: String)

    /**
     * Write a CDATA section
     * @param text The text of the section.
     */
    public fun cdsect(text: String)

    public fun entityRef(text: String)

    public fun processingInstruction(text: String)

    public fun ignorableWhitespace(text: String)

    public fun attribute(namespace: String?, name: String, prefix: String?, value: String)

    public fun docdecl(text: String)

    public fun startDocument(version: String?, encoding: String?, standalone: Boolean?)

    public fun endDocument()

    public fun endTag(namespace: String?, localName: String, prefix: String?)

    public val namespaceContext: NamespaceContext

    /**
     * Get the namespace uri the prefix is currently bound to
     */
    public fun getNamespaceUri(prefix: String): String?

    /**
     * Get a prefix the uri is currently bound to
     */
    public fun getPrefix(namespaceUri: String?): String?
}


public fun XmlWriter.addUndeclaredNamespaces(reader: XmlReader, missingNamespaces: MutableMap<String, String>) {
    undeclaredPrefixes(reader, missingNamespaces)
}

private fun XmlWriter.undeclaredPrefixes(reader: XmlReader, missingNamespaces: MutableMap<String, String>) {
    assert(reader.eventType === EventType.START_ELEMENT)
    val prefix = reader.prefix
    if (!missingNamespaces.containsKey(prefix)) {
        val uri = reader.namespaceURI
        if (getNamespaceUri(prefix) == uri && reader.isPrefixDeclaredInElement(prefix)) {
            return
        } else if (uri.isNotEmpty()) {
            missingNamespaces[prefix] = uri
        }
    }
}


public fun XmlWriter.serialize(reader: XmlReader) {
    while (reader.hasNext()) {
        when (reader.next()) {
            EventType.START_DOCUMENT,
            EventType.PROCESSING_INSTRUCTION,
            EventType.DOCDECL,
            EventType.END_DOCUMENT -> {
                if (depth <= 0) {
                    writeCurrentEvent(reader)
                }
            }
            EventType.IGNORABLE_WHITESPACE -> {
                // Only write ignorable whitespace if we are not formatting with a set indent.
                if (indentString.isEmpty()) writeCurrentEvent(reader)
            }
            else -> writeCurrentEvent(reader)
        }
    }
}

public fun XmlWriter.writeCurrentEvent(reader: XmlReader) {
    when (reader.eventType) {
        EventType.START_DOCUMENT -> startDocument(null, reader.encoding, reader.standalone)
        EventType.START_ELEMENT -> {
            startTag(reader.namespaceURI, reader.localName, reader.prefix)
            run {
                for (a in reader.namespaceDecls) {
                    namespaceAttr(a.prefix, a.namespaceURI)
                }
            }
            run {
                for (i in reader.attributeIndices) {
                    attribute(
                        reader.getAttributeNamespace(i), reader.getAttributeLocalName(i),
                        null, reader.getAttributeValue(i)
                    )
                }
            }
        }
        EventType.END_ELEMENT -> endTag(
            reader.namespaceURI, reader.localName,
            reader.prefix
        )
        EventType.COMMENT -> comment(reader.text)
        EventType.TEXT -> text(reader.text)
        EventType.ATTRIBUTE -> attribute(
            reader.namespaceURI, reader.localName,
            reader.prefix, reader.text
        )
        EventType.CDSECT -> cdsect(reader.text)
        EventType.DOCDECL -> docdecl(reader.text)
        EventType.END_DOCUMENT -> endDocument()
        EventType.ENTITY_REF -> entityRef(reader.text)
        EventType.IGNORABLE_WHITESPACE -> ignorableWhitespace(reader.text)
        EventType.PROCESSING_INSTRUCTION -> processingInstruction(reader.text)
    }
}


/**
 * Enhanced function for writing start tags, that will attempt to reuse prefixes.
 */
@Suppress("unused")
public fun XmlWriter.smartStartTag(qName: QName) {
    smartStartTag(qName.getNamespaceURI(), qName.getLocalPart(), qName.getPrefix())
}

/**
 * Enhanced function for writing start tags, that will attempt to reuse prefixes.
 */
public inline fun XmlWriter.smartStartTag(qName: QName, body: XmlWriter.() -> Unit) {
    smartStartTag(qName.getNamespaceURI(), qName.getLocalPart(), qName.getPrefix(), body)
}

@Deprecated("Use strings", ReplaceWith("smartStartTag(nsUri?.toString(), localName.toString(), prefix?.toString())"))
@JvmOverloads
public fun XmlWriter.smartStartTag(nsUri: CharSequence?, localName: CharSequence, prefix: CharSequence? = null) {
    smartStartTag(nsUri?.toString(), localName.toString(), prefix?.toString())
}

/**
 * Enhanced function for writing start tags, that will attempt to reuse prefixes.
 */
@JvmOverloads
public fun XmlWriter.smartStartTag(nsUri: String?, localName: String, prefix: String? = null) {
    if (nsUri == null) {
        val namespace = namespaceContext.getNamespaceURI(prefix ?: DEFAULT_NS_PREFIX) ?: NULL_NS_URI
        startTag(namespace, localName, prefix)
    } else {
        var writeNs = false

        val usedPrefix = getPrefix(nsUri) ?: run {
            val currentNs = prefix?.let { getNamespaceUri(it) } ?: NULL_NS_URI
            if (nsUri != currentNs) {
                writeNs = true
            }; prefix ?: DEFAULT_NS_PREFIX
        }

        startTag(nsUri, localName, usedPrefix)

        if (writeNs) this.namespaceAttr(usedPrefix, nsUri)
    }
}

@Deprecated(
    "Use strings",
    ReplaceWith("smartStartTag(nsUri?.toString(), localName.toString(), prefix?.toString(), body)")
)
public inline fun XmlWriter.smartStartTag(
    nsUri: CharSequence?,
    localName: CharSequence,
    prefix: CharSequence? = null,
    body: XmlWriter.() -> Unit
) {
    smartStartTag(nsUri?.toString(), localName.toString(), prefix?.toString(), body)
}

@JvmOverloads
public inline fun XmlWriter.smartStartTag(
    nsUri: String?,
    localName: String,
    prefix: String? = null,
    body: XmlWriter.() -> Unit
) {
    smartStartTag(nsUri, localName, prefix)
    body()
    endTag(nsUri, localName, prefix)
}

/**
 * Helper function for writing a list only if it contains any element.
 */
@Suppress("unused")
public inline fun <T> XmlWriter.writeListIfNotEmpty(
    iterable: Iterable<T>,
    nsUri: String?,
    localName: String,
    prefix: String? = null,
    body: XmlWriter.(T) -> Unit
) {
    val it = iterable.iterator()
    if (it.hasNext()) {
        smartStartTag(nsUri, localName, prefix)
        while (it.hasNext()) {
            body(it.next())
        }
        endTag(nsUri, localName, prefix)
    }
}

@Suppress("NOTHING_TO_INLINE")
public inline fun <T : XmlSerializable> XmlWriter.serializeAll(iterable: Iterable<T>) {
    iterable.forEach { it.serialize(this) }
}

@Suppress("NOTHING_TO_INLINE")
public inline fun <T : XmlSerializable> XmlWriter.serializeAll(sequence: Sequence<T>) {
    sequence.forEach { it.serialize(this) }
}


public inline fun XmlWriter.startTag(
    nsUri: String?,
    localName: String,
    prefix: String? = null,
    body: XmlWriter.() -> Unit
) {
    startTag(nsUri, localName, prefix)
    body()
    endTag(nsUri, localName, prefix)
}

public fun XmlWriter.writeSimpleElement(qName: QName, value: String?) {
    writeSimpleElement(qName.getNamespaceURI(), qName.getLocalPart(), qName.getPrefix(), value)
}

public fun XmlWriter.writeSimpleElement(
    nsUri: String?,
    localName: String,
    prefix: String?,
    value: String?
) {
    smartStartTag(nsUri, localName, prefix)
    if (!value.isNullOrEmpty()) {
        text(value.toString())
    }
    endTag(nsUri, localName, prefix)
}

public fun XmlWriter.writeAttribute(name: String, value: String?) {
    value?.let { attribute(null, name, null, it) }
}

public fun XmlWriter.writeAttribute(name: String, value: Any?) {
    value?.let { attribute(null, name, null, it.toString()) }
}

public fun XmlWriter.writeAttribute(name: QName, value: String?) {
    value?.let {
        if (name.namespaceURI.isEmpty() && name.prefix.isEmpty()) {
            attribute(null, name.localPart, name.prefix, value)
        } else {
            attribute(name.namespaceURI, name.localPart, name.prefix, value)
        }
    }
}

public fun XmlWriter.writeAttribute(name: String, value: Double) {
    if (!value.isNaN()) {
        attribute(null, name, null, value.toString())
    }
}

public fun XmlWriter.writeAttribute(name: String, value: Long) {
    attribute(null, name, null, value.toString())
}

public fun XmlWriter.writeAttribute(name: String, value: QName?) {
    if (value != null) {
        var prefix: String?
        if (value.getNamespaceURI().isNotEmpty()) {
            if (value.getNamespaceURI() == namespaceContext.getNamespaceURI(value.getPrefix())) {
                prefix = value.getPrefix()
            } else {
                prefix = namespaceContext.getPrefix(value.getNamespaceURI())
                if (prefix == null) {
                    prefix = value.getPrefix()
                    namespaceAttr(prefix, value.getNamespaceURI())
                }
            }
        } else { // namespace not specified
            prefix = value.getPrefix()
            if (prefix.let { namespaceContext.getNamespaceURI(it) } == null) throw IllegalArgumentException(
                "Cannot determine namespace of qname"
            )
        }
        attribute(null, name, null, prefix + ':' + value.getLocalPart())
    }
}

public fun XmlWriter.endTag(predelemname: QName) {
    this.endTag(predelemname.getNamespaceURI(), predelemname.getLocalPart(), predelemname.getPrefix())
}

public fun XmlWriter.filterSubstream(): XmlWriter {
    return SubstreamFilterWriter(this)
}


private fun undeclaredPrefixes(
    reader: XmlReader,
    reference: XmlWriter,
    missingNamespaces: MutableMap<String, String>
) {
    assert(reader.eventType === EventType.START_ELEMENT)
    val prefix = reader.prefix
    if (!missingNamespaces.containsKey(prefix)) {
        val uri = reader.namespaceURI
        if (uri == reference.getNamespaceUri(prefix)
            && reader.isPrefixDeclaredInElement(prefix)
        ) {
            return
        } else if (uri.isNotEmpty()) {
            if (uri != reference.getNamespaceUri(prefix)) {
                missingNamespaces[prefix] = uri
            }
        }
    }
}

/**
 * Write
 */
public fun XmlWriter.writeElement(missingNamespaces: MutableMap<String, String>?, reader: XmlReader) {
    if (reader.eventType == EventType.END_ELEMENT) throw IllegalArgumentException("Cannot really validly write an end element here")
    reader.writeCurrent(this)
    if (reader.eventType == EventType.START_ELEMENT) writeElementContent(missingNamespaces, reader)
}

/**
 * Write the child content of the current element in the reader to the output This does
 * not write the container itself
 */
public fun XmlWriter.writeElementContent(missingNamespaces: MutableMap<String, String>?, reader: XmlReader) {
    reader.forEach { type ->
        // We already moved to the next event. Add the namespaces before writing as for a DOM implementation
        // it is too late to do it afterwards.
        if (reader.eventType == EventType.START_ELEMENT && missingNamespaces != null) {
            addUndeclaredNamespaces(reader, missingNamespaces)
        }

        reader.writeCurrent(this)

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (type) {
            EventType.START_ELEMENT -> {

                writeElementContent(missingNamespaces, reader)
            }
            EventType.END_ELEMENT -> return
        }
    }
}


private class SubstreamFilterWriter(delegate: XmlWriter) : XmlDelegatingWriter(delegate) {

    override fun processingInstruction(text: String) { /* ignore */
    }

    override fun endDocument() { /* ignore */
    }

    override fun docdecl(text: String) { /* ignore */
    }

    override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) { /* ignore */
    }
}
