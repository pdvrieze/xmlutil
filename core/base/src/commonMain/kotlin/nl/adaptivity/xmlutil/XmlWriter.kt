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
@file:JvmMultifileClass
@file:JvmName("XmlWriterUtil")
@file:Suppress("NOTHING_TO_INLINE")

package nl.adaptivity.xmlutil

import nl.adaptivity.xmlutil.XMLConstants.DEFAULT_NS_PREFIX
import nl.adaptivity.xmlutil.XMLConstants.NULL_NS_URI
import nl.adaptivity.xmlutil.core.impl.multiplatform.Closeable
import nl.adaptivity.xmlutil.core.impl.multiplatform.MpJvmDefaultWithCompatibility
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.core.internal.countIndentedLength
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import nl.adaptivity.xmlutil.dom2.Node as Node2

/**
 * Interface representing the (wrapper) type that allows generating xml documents.
 */
@MpJvmDefaultWithCompatibility
public interface XmlWriter : Closeable {

    /**
     * The current depth into the tree. The initial depth is `0`
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

    /**
     * Write a namespace declaration attribute.
     */
    public fun namespaceAttr(namespacePrefix: String, namespaceUri: String)

    /**
     * Write a namespace declaration attribute.
     */
    public fun namespaceAttr(namespace: Namespace) {
        namespaceAttr(namespace.prefix, namespace.namespaceURI)
    }

    /**
     * Close the writer. After invoking this the writer is not writable.
     */
    override fun close()

    /**
     * Flush all state to the underlying buffer
     */
    public fun flush()

    /**
     * Write a start tag. This increases the current depth.
     * @param namespace The namespace to use for the tag.
     * @param localName The local name for the tag.
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

    /** Safe version of CDSect that writes multiple CD Sections to handle embedded `]]>` */
    public fun safeCdsect(text: String) {
        var idx = text.indexOf("]]>")
        var start = 0
        while (idx >= 0) {
            cdsect(text.substring(start, idx + 2))
            start = idx + 2
            idx = text.indexOf("]]>", idx + 3)
        }
        if (start < text.length) cdsect(text.substring(start))
    }

    /**
     * Write an entity reference
     * @param text the name of the reference. Must be a valid reference name.
     */
    public fun entityRef(text: String)

    /**
     * Write a processing instruction with the given raw content. When using, prefer the version taking two arguments
     */
    public fun processingInstruction(text: String)

    /**
     * Write a processing instruction with the given target and data
     * @param target The (CNAME) target of the instruction
     * @param data The data to be used.
     */
    public fun processingInstruction(target: String, data: String): Unit =
        processingInstruction("$target $data")

    /**
     * Write ignorable whitespace.
     * @param text This text is expected to be only XML whitespace.
     */
    public fun ignorableWhitespace(text: String)

    /**
     * Write an attribute.
     * @param namespace The namespace of the attribute. `null` and `""` will both resolve to the empty namespace.
     * @param name The local name of the attribute (CName, must exclude the prefix).
     * @param prefix The prefix to use. Note that in XML for attributes the prefix is empty/null when the namespace is and vice versa.
     * @param value The value of the attribute
     */
    public fun attribute(namespace: String?, name: String, prefix: String?, value: String)

    /**
     * Write a document declaration (DTD).
     * @param text The content of the DTD Declaration.
     */
    public fun docdecl(text: String)

    /**
     * Start the document. This causes an xml declaration to be generated with the relevant content.
     * @param version The XML version
     * @param encoding The encoding of the document
     * @param standalone A statement that the document is standalone (no externally defined entities...)
     */
    public fun startDocument(version: String? = null, encoding: String? = null, standalone: Boolean? = null)

    /**
     * Close the document. This will do checks, and update the state, but there is no actual content in an xml stream
     * that corresponds to it.
     */
    public fun endDocument()

    /**
     * Write a closing tag.
     * @param namespace The namespace for the tag to close
     * @param localName The local name
     * @param prefix The prefix
     */
    public fun endTag(namespace: String?, localName: String, prefix: String?)

    /**
     * A namespace context that provides access to known namespaces/prefixes at this point in the writer.
     */
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

/**
 * Function that collects namespaces not present in the writer
 * @receiver The writer where namespace information is looked up from
 * @param reader The reader from which the namespace information is retrieved
 * @param missingNamespaces Map to which the "missing" namespace is added.
 */
@Deprecated("This function should be internal")
public fun XmlWriter.addUndeclaredNamespaces(reader: XmlReader, missingNamespaces: MutableMap<String, String>) {
    undeclaredPrefixes(reader, missingNamespaces)
}

private fun XmlWriter.undeclaredPrefixes(reader: XmlReader, missingNamespaces: MutableMap<String, String>) {
    assert(reader.eventType === EventType.START_ELEMENT)
    val prefix = reader.prefix
    if (!missingNamespaces.containsKey(prefix)) {
        val uri = reader.namespaceURI
        // the uri must be non-empty and also not already present in the output.
        if (uri.isNotEmpty() && getNamespaceUri(prefix) != uri) missingNamespaces[prefix] = uri
    }

    for (attrIdx in 0 until reader.attributeCount) {
        val prefix = reader.getAttributePrefix(attrIdx)
        when (prefix) {
            "", "xmlns" -> {}

            else -> if (!missingNamespaces.containsKey(prefix)) {
                val uri = reader.getAttributeNamespace(attrIdx)
                if (getNamespaceUri(prefix) != uri || !reader.isPrefixDeclaredInElement(prefix)) {
                    missingNamespaces[prefix] = uri
                }
            }
        }
    }

    for ((prefix, _) in reader.namespaceDecls) {
        missingNamespaces.remove(prefix)
    }
}

/**
 * Serialize the content of the reader to the writer. This will continue until the reader has no
 * more events. Note that this does not attempt to balance the tags. It will ignore star/end document
 * events, processing instructions and document declarations if the writer has depth>0 (is in an
 * element where such instructions are illegal).
 *
 * @receiver The writer to write to
 * @param reader The reader to read from
 */
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

/**
 * Write the current event to the writer. Note that for tags/start elements this will write the
 * attributes, but **not** the children.
 *
 * @receiver the writer to write to
 * @param reader The reader to get the current event from.
 */
public fun XmlWriter.writeCurrentEvent(reader: XmlReader) {
    when (reader.eventType) {
        EventType.START_DOCUMENT -> startDocument(null, reader.encoding, reader.standalone)

        EventType.START_ELEMENT -> {
            startTag(reader.namespaceURI, reader.localName, reader.prefix)

            for (a in reader.namespaceDecls) {
                namespaceAttr(a.prefix, a.namespaceURI)
            }

            for (i in reader.attributeIndices) {
                val attrPrefix = reader.getAttributePrefix(i)
                val namespace = if (attrPrefix == "") "" else reader.getAttributeNamespace(i)
                val prefix = when (namespace) {
                    "" -> ""
                    namespaceContext.getNamespaceURI(attrPrefix) -> attrPrefix
                    else -> namespaceContext.getPrefix(namespace) ?: attrPrefix
                }
                attribute(
                    namespace, reader.getAttributeLocalName(i),
                    prefix, reader.getAttributeValue(i)
                )
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

        EventType.ENTITY_REF -> entityRef(reader.localName)

        EventType.IGNORABLE_WHITESPACE -> ignorableWhitespace(reader.text)

        EventType.PROCESSING_INSTRUCTION -> processingInstruction(reader.piTarget, reader.piData)
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
 * Function present only for binary compatibility.
 */
@Deprecated("Present for return value change only", level = DeprecationLevel.HIDDEN)
@JvmOverloads
@JvmName("smartStartTag")
public fun XmlWriter.smartStartTagCompat(nsUri: String?, localName: String, prefix: String? = null) {
    smartStartTag(nsUri, localName, prefix)
}

/**
 * Enhanced function for writing start tags, that will attempt to reuse prefixes. Rather than use
 * the passed prefix it will look up the prefix for the given namespace, and if present use that.
 * It will also ensure to write the appropriate namespace attribute if needed. If the namespace is
 * the default/null, xml or the xmlns namespace the implementation will be as expected.
 *
 * @return The used prefix
 */
@JvmOverloads
public fun XmlWriter.smartStartTag(nsUri: String?, localName: String, prefix: String? = null): String {
    if (nsUri == null || nsUri == XMLConstants.XML_NS_URI || nsUri == XMLConstants.XMLNS_ATTRIBUTE_NS_URI) {
        val namespace = namespaceContext.getNamespaceURI(prefix ?: DEFAULT_NS_PREFIX) ?: NULL_NS_URI
        startTag(namespace, localName, prefix)
        return prefix ?: DEFAULT_NS_PREFIX
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
        return usedPrefix
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

/**
 * Helper function for writing tags that will automatically write the end tag. Otherwise
 */
@JvmOverloads
public inline fun XmlWriter.smartStartTag(
    nsUri: String?,
    localName: String,
    prefix: String? = null,
    body: XmlWriter.() -> Unit
) {
    val usedPrefix = smartStartTag(nsUri, localName, prefix)
    body()
    endTag(nsUri, localName, usedPrefix)
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
        text(value)
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
    if (value != null) {
        if (name.namespaceURI.isEmpty() && name.prefix.isEmpty()) {
            attribute(null, name.localPart, null, value)
        } else {
            attribute(name.namespaceURI, name.localPart, name.prefix, value)
        }
    }
}

public fun XmlWriter.writeAttribute(name: String, value: Double) {
    if (!value.isNaN()) {
        writeAttribute(name, value.toString())
    }
}

public fun XmlWriter.writeAttribute(name: String, value: Long) {
    writeAttribute(name, value.toString())
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


public fun XmlWriter.serialize(node: Node2) {
    serialize(xmlStreaming.newReader(node))
}


/**
 * Write the entirety of an element content to the writer.
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
            @Suppress("DEPRECATION")
            addUndeclaredNamespaces(reader, missingNamespaces)
        }

        reader.writeCurrent(this)

        when (type) {
            EventType.START_ELEMENT -> {

                writeElementContent(missingNamespaces, reader)
            }

            EventType.END_ELEMENT -> return

            else -> { }
        }
    }
}


private class SubstreamFilterWriter(delegate: XmlWriter) : XmlDelegatingWriter(delegate) {

    override fun processingInstruction(text: String) { /* ignore */
    }

    override fun processingInstruction(target: String, data: String) { /* ignore */
    }

    override fun endDocument() { /* ignore */
    }

    override fun docdecl(text: String) { /* ignore */
    }

    override fun startDocument(version: String?, encoding: String?, standalone: Boolean?) { /* ignore */
    }
}
