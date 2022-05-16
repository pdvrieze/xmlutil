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
public sealed class XmlEvent(public val locationInfo: String?) {

    public companion object {

        public fun from(reader: XmlReader): XmlEvent = reader.eventType.createEvent(reader)

    }

    public open class TextEvent(locationInfo: String?, override val eventType: EventType, public val text: String) :
        XmlEvent(locationInfo) {

        override fun writeTo(writer: XmlWriter): Unit = eventType.writeEvent(writer, this)

        override val isIgnorable: Boolean
            get() = super.isIgnorable ||
                    (eventType == EventType.TEXT && isXmlWhitespace(text))

        override fun toString(): String {
            return "$eventType - \"$text\" (${locationInfo ?: ""})"
        }
    }

    public class EntityRefEvent(
        locationInfo: String?,
        public val localName: String,
        text: String
    ) : TextEvent(locationInfo, EventType.ENTITY_REF, text) {

        override fun writeTo(writer: XmlWriter): Unit = eventType.writeEvent(writer, this)

        override val isIgnorable: Boolean
            get() = false

        override fun toString(): String {
            return "$eventType - \"$text\" (${locationInfo ?: ""})"
        }
    }

    public class EndDocumentEvent(locationInfo: String?) : XmlEvent(locationInfo) {

        override fun writeTo(writer: XmlWriter): Unit = writer.endDocument()

        override val eventType: EventType get() = EventType.END_DOCUMENT

        override fun toString(): String {
            return "$eventType (${locationInfo ?: ""})"
        }

    }

    public class EndElementEvent(
        locationInfo: String?,
        namespaceUri: String,
        localName: String,
        prefix: String,
        namespaceContext: IterableNamespaceContext,
    ) : NamedEvent(locationInfo, namespaceUri, localName, prefix) {

        override fun writeTo(writer: XmlWriter): Unit = writer.endTag(namespaceUri, localName, prefix)

        override val eventType: EventType get() = EventType.END_ELEMENT

        public val namespaceContext: IterableNamespaceContext = namespaceContext.freeze()
    }

    public class StartDocumentEvent(
        locationInfo: String?,
        public val encoding: String?,
        public val version: String?,
        public val standalone: Boolean?
    ) :
        XmlEvent(locationInfo) {

        override fun writeTo(writer: XmlWriter): Unit = writer.startDocument(version, encoding, standalone)

        override val eventType: EventType get() = EventType.START_DOCUMENT

        override fun toString(): String {
            return "$eventType - encoding:$encoding, version: $version, standalone: $standalone (${locationInfo ?: ""})"
        }

    }

    public abstract class NamedEvent(
        locationInfo: String?,
        public val namespaceUri: String,
        public val localName: String,
        public val prefix: String
    ) : XmlEvent(locationInfo) {

        public fun isEqualNames(ev: NamedEvent): Boolean {
            return namespaceUri == ev.namespaceUri &&
                    localName == ev.localName &&
                    prefix == ev.prefix
        }

        public val name: QName get() = QName(namespaceUri, localName, prefix)

        override fun toString(): String {
            return "$eventType - {$namespaceUri}$prefix:$localName (${locationInfo ?: ""})"
        }

    }

    public class StartElementEvent(
        locationInfo: String?,
        namespaceUri: String,
        localName: String,
        prefix: String,
        public val attributes: Array<out Attribute>,
        private val parentNamespaceContext: IterableNamespaceContext,
        namespaceDecls: List<Namespace>
    ) : NamedEvent(locationInfo, namespaceUri, localName, prefix) {

        private val namespaceHolder: SimpleNamespaceContext = SimpleNamespaceContext(namespaceDecls.asIterable())

        public constructor(
            namespaceUri: String,
            localName: String,
            prefix: String,
            parentNamespaceContext: IterableNamespaceContext
        ) : this(null, namespaceUri, localName, prefix, emptyArray(), parentNamespaceContext, emptyList())

        @Deprecated("Use version that takes the parent tag's namespace context.", level = DeprecationLevel.ERROR)
        public constructor(namespaceUri: String, localName: String, prefix: String) :
                this(namespaceUri, localName, prefix, SimpleNamespaceContext())

        @Deprecated("Use version that takes the parent tag's namespace context.", level = DeprecationLevel.ERROR)
        public constructor(
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

            namespaceHolder.forEach { ns -> writer.namespaceAttr(ns.prefix, ns.namespaceURI) }
        }

        public val namespaceDecls: Iterable<Namespace>
            get() = namespaceHolder

        override val eventType: EventType get() = EventType.START_ELEMENT

        internal fun getPrefix(namespaceURI: String): String? {
            return namespaceHolder.getPrefix(namespaceURI)
                ?: parentNamespaceContext.getPrefix(namespaceUri)
        }

        internal fun getNamespaceURI(prefix: String): String? {
            val decl = namespaceHolder.getNamespaceURI(prefix)
            return when (decl) {
                null -> parentNamespaceContext.getNamespaceURI(prefix)
                else -> decl
            }
        }

        @Deprecated(
            "Just use the version that takes a string",
            ReplaceWith("getNamespaceURI(prefix.toString())")
        )
        public fun getNamespaceUri(prefix: CharSequence): String? {
            return getNamespaceURI(prefix.toString())
        }

        public val namespaceContext: IterableNamespaceContext
            get() = namespaceHolder + parentNamespaceContext

        @Suppress("OverridingDeprecatedMember", "DEPRECATION")
        internal fun getPrefixesCompat(namespaceURI: String): Iterator<String> {
            return (namespaceHolder.getPrefixesCompat(namespaceURI).asSequence() +
                    parentNamespaceContext.getPrefixesCompat(namespaceURI).asSequence()
                    ).iterator()
        }

        override fun toString(): String {
            return "$eventType - {$namespaceUri}$prefix:$localName (${locationInfo ?: ""})" +
                    attributes.joinToString(
                        "\n    ",
                        if (attributes.isNotEmpty()) "\n    " else ""
                    ) { "${it.localName} = ${it.value} " }
        }

    }

    public class Attribute(
        locationInfo: String?,
        namespaceUri: CharSequence,
        localName: CharSequence,
        prefix: CharSequence,
        value: CharSequence
    ) : XmlEvent(locationInfo) {

        public constructor(
            namespaceUri: CharSequence,
            localName: CharSequence,
            prefix: CharSequence,
            value: CharSequence
        ) : this(null, namespaceUri, localName, prefix, value)

        public val value: String = value.toString()
        public val prefix: String = prefix.toString()
        public val localName: String = localName.toString()
        public val namespaceUri: String = namespaceUri.toString()
        override val eventType: EventType get() = EventType.ATTRIBUTE

        public val name: QName get() = QName(namespaceUri, localName, prefix)

        override fun writeTo(writer: XmlWriter) {
            if (hasNamespaceUri()) {
                val nsPrefix = if (prefix.isEmpty()) "" else localName
                writer.namespaceAttr(nsPrefix, namespaceUri)
            } else {
                writer.attribute(namespaceUri, localName, prefix, value)
            }
        }

        public fun hasNamespaceUri(): Boolean {
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

    public class NamespaceImpl(namespacePrefix: CharSequence, namespaceUri: CharSequence) : Namespace {

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

    public abstract val eventType: EventType

    public open val isIgnorable: Boolean get() = eventType.isIgnorable

    public abstract fun writeTo(writer: XmlWriter)

}
