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

/**
 * A class to represent the events that can occur in XML Documents
 *
 * Created by pdvrieze on 16/11/15.
 */
sealed class XmlEvent(val locationInfo: String?) {

    companion object {

        fun from(reader: XmlReader): XmlEvent = reader.eventType.createEvent(reader)

        @Deprecated(
            "Use the extension property",
            ReplaceWith("reader.namespaceDecls", "nl.adaptivity.xmlutil.getAttributes")
                   )
        internal fun getNamespaceDecls(reader: XmlReader): Array<out Namespace> {
            val readerOffset = reader.namespaceStart
            return Array<Namespace>(reader.namespaceEnd - readerOffset) { i ->
                val nsIndex = readerOffset + i
                NamespaceImpl(
                    reader.getNamespacePrefix(nsIndex),
                    reader.getNamespaceURI(nsIndex)
                             )
            }
        }

        @Deprecated(
            "Use the extension property",
            ReplaceWith("reader.attributes", "nl.adaptivity.xmlutil.getAttributes")
                   )
        internal fun getAttributes(reader: XmlReader): Array<out Attribute> = Array(reader.attributeCount) { i ->
            Attribute(
                reader.locationInfo,
                reader.getAttributeNamespace(i),
                reader.getAttributeLocalName(i),
                reader.getAttributePrefix(i),
                reader.getAttributeValue(i)
                     )
        }

    }

    class TextEvent(locationInfo: String?, override val eventType: EventType, val text: String) : XmlEvent(
        locationInfo
                                                                                                          ) {

        override fun writeTo(writer: XmlWriter): Unit = eventType.writeEvent(writer, this)

        override val isIgnorable: Boolean
            get() =
                super.isIgnorable || (eventType == EventType.TEXT && isXmlWhitespace(
                    text
                                                                                    ))

        override fun toString(): String {
            return "$eventType - \"$text\" (${locationInfo ?: ""})"
        }
    }

    class EndDocumentEvent(locationInfo: String?) : XmlEvent(locationInfo) {

        override fun writeTo(writer: XmlWriter): Unit = writer.endDocument()

        override val eventType: EventType get() = EventType.END_DOCUMENT

        override fun toString(): String {
            return "$eventType (${locationInfo ?: ""})"
        }

    }

    class EndElementEvent(
        locationInfo: String?,
        namespaceUri: String,
        localName: String,
        prefix: String,
        namespaceContext: IterableNamespaceContext,
                         ) :
        NamedEvent(locationInfo, namespaceUri, localName, prefix) {

        override fun writeTo(writer: XmlWriter): Unit = writer.endTag(namespaceUri, localName, prefix)

        override val eventType: EventType get() = EventType.END_ELEMENT

        val namespaceContext: IterableNamespaceContext = namespaceContext.freeze()
    }

    class StartDocumentEvent(
        locationInfo: String?,
        val encoding: String?,
        val version: String?,
        val standalone: Boolean?
                            ) :
        XmlEvent(locationInfo) {

        override fun writeTo(writer: XmlWriter): Unit = writer.startDocument(version, encoding, standalone)

        override val eventType: EventType get() = EventType.START_DOCUMENT

        override fun toString(): String {
            return "$eventType - encoding:$encoding, version: $version, standalone: $standalone (${locationInfo ?: ""})"
        }

    }

    abstract class NamedEvent(
        locationInfo: String?,
        val namespaceUri: String,
        val localName: String,
        val prefix: String
                             ) :
        XmlEvent(locationInfo) {

        fun isEqualNames(ev: NamedEvent): Boolean {
            return namespaceUri == ev.namespaceUri &&
                    localName == ev.localName &&
                    prefix == ev.prefix
        }

        override fun toString(): String {
            return "$eventType - {$namespaceUri}$prefix:$localName (${locationInfo ?: ""})"
        }

    }

    @OptIn(XmlUtilInternal::class)
    class StartElementEvent(
        locationInfo: String?,
        namespaceUri: String,
        localName: String,
        prefix: String,
        val attributes: Array<out Attribute>,
        private val parentNamespaceContext: IterableNamespaceContext,
        namespaceDecls: List<Namespace>
                           ) :
        NamedEvent(locationInfo, namespaceUri, localName, prefix), NamespaceContextImpl {

        private val namespaceHolder: SimpleNamespaceContext = SimpleNamespaceContext(namespaceDecls.asIterable())

        constructor(
            namespaceUri: String,
            localName: String,
            prefix: String,
            parentNamespaceContext: IterableNamespaceContext
                   ) :
                this(null, namespaceUri, localName, prefix, emptyArray(), parentNamespaceContext, emptyList())

        @Deprecated("Use version that takes the parent tag's namespace context.", level = DeprecationLevel.ERROR)
        constructor(namespaceUri: String, localName: String, prefix: String) :
                this(namespaceUri, localName, prefix, SimpleNamespaceContext())

        @Deprecated("Use version that takes the parent tag's namespace context.", level = DeprecationLevel.ERROR)
        constructor(
            locationInfo: String?,
            namespaceUri: String,
            localName: String,
            prefix: String,
            attributes: Array<out Attribute>,
            namespaceDecls: List<Namespace>
                   ) : this(
            locationInfo,
            namespaceUri,
            localName,
            prefix,
            attributes,
            SimpleNamespaceContext(),
            namespaceDecls
                           )

        override fun writeTo(writer: XmlWriter) {
            writer.startTag(namespaceUri, localName, prefix)

            attributes.forEach { attr -> writer.attribute(attr.namespaceUri, attr.localName, attr.prefix, attr.value) }

            namespaceDecls.forEach { ns -> writer.namespaceAttr(ns.prefix, ns.namespaceURI) }
        }

        val namespaceDecls: Array<out Namespace>
            get() = Array(namespaceHolder.size) {
                NamespaceImpl(namespaceHolder.getPrefix(it), namespaceHolder.getNamespaceURI(it))
            }

        override val eventType: EventType get() = EventType.START_ELEMENT

        override fun getPrefix(namespaceURI: String): String? {
            return namespaceDecls
                .asSequence()
                .filter { ns -> ns.namespaceURI == namespaceUri }
                .lastOrNull()?.prefix
                ?: parentNamespaceContext.getPrefix(namespaceUri)
        }

        @Deprecated(
            "Just use the version that takes a string",
            ReplaceWith("getPrefix(namespaceUri.toString())")
                   )
        fun getPrefix(namespaceUri: CharSequence): String? = getPrefix(namespaceUri.toString())

        override fun getNamespaceURI(prefix: String): String? = namespaceDecls
            .asSequence()
            .filter { ns -> ns.prefix == prefix }
            .lastOrNull()?.namespaceURI
            ?: parentNamespaceContext.getNamespaceURI(prefix)

        @Deprecated(
            "Just use the version that takes a string",
            ReplaceWith("getNamespaceURI(prefix.toString())")
                   )
        fun getNamespaceUri(prefix: CharSequence): String? {
            return getNamespaceURI(prefix.toString())
        }

        val namespaceContext: IterableNamespaceContext
            get() = namespaceHolder + parentNamespaceContext

        @Suppress("OverridingDeprecatedMember")
        override fun getPrefixesCompat(namespaceURI: String): Iterator<String> {
            return namespaceDecls
                .asSequence()
                .filter { ns -> ns.namespaceURI == namespaceUri }
                .map { it.prefix }.iterator()
        }

        override fun toString(): String {
            return "$eventType - {$namespaceUri}$prefix:$localName (${locationInfo ?: ""})" +
                    attributes.joinToString(
                        "\n    ",
                        if (attributes.isNotEmpty()) "\n    " else ""
                                           ) { "${it.localName} = ${it.value} " }
        }

    }

    class Attribute(
        locationInfo: String?,
        namespaceUri: CharSequence,
        localName: CharSequence,
        prefix: CharSequence,
        value: CharSequence
                   ) : XmlEvent(locationInfo) {

        constructor(
            namespaceUri: CharSequence,
            localName: CharSequence,
            prefix: CharSequence,
            value: CharSequence
                   ) : this(null, namespaceUri, localName, prefix, value)

        val value: String = value.toString()
        val prefix: String = prefix.toString()
        val localName: String = localName.toString()
        val namespaceUri: String = namespaceUri.toString()
        override val eventType: EventType get() = EventType.ATTRIBUTE

        override fun writeTo(writer: XmlWriter) {
            if (hasNamespaceUri()) {
                val nsPrefix = if (prefix.isEmpty()) "" else localName
                writer.namespaceAttr(nsPrefix, namespaceUri)
            } else {
                writer.attribute(namespaceUri, localName, prefix, value)
            }
        }

        fun hasNamespaceUri(): Boolean {
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI == namespaceUri ||
                    (prefix.isEmpty() && XMLConstants.XMLNS_ATTRIBUTE == localName)
        }

        override fun toString(): String = when (prefix.isBlank()) {
            true -> "$localName=\"$value\""
            else -> "$prefix.$localName=\"$value\""
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Attribute

            if (value != other.value) return false
            if (prefix != other.prefix) return false
            if (localName != other.localName) return false
            if (namespaceUri != other.namespaceUri) return false

            return true
        }

        override fun hashCode(): Int {
            var result = value.hashCode()
            result = 31 * result + prefix.hashCode()
            result = 31 * result + localName.hashCode()
            result = 31 * result + namespaceUri.hashCode()
            return result
        }
    }

    class NamespaceImpl(namespacePrefix: CharSequence, namespaceUri: CharSequence) : Namespace {

        override val prefix: String = namespacePrefix.toString()

        override val namespaceURI: String = namespaceUri.toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Namespace) return false

            if (prefix != other.prefix) return false
            if (namespaceURI != other.namespaceURI) return false

            return true
        }

        override fun hashCode(): Int {
            return 31 * prefix.hashCode() + namespaceURI.hashCode()
        }

        override fun toString(): String {
            return "{$prefix:$namespaceURI}"
        }

    }

    abstract val eventType: EventType

    open val isIgnorable: Boolean get() = eventType.isIgnorable

    abstract fun writeTo(writer: XmlWriter)

}
