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

package nl.adaptivity.xmlutil

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.*

/**
 * Platform independent implementation of QName
 */
public expect class QName {
    /**
     * Create a new constructor with the given namespace uri, local part and prefix.
     *
     * @param namespaceURI The namespace for this name
     * @param localPart The local part of the name
     * @param prefix The prefix for this name
     */
    public constructor(namespaceURI: String, localPart: String, prefix: String)

    /**
     * Create a new constructor with the given namespace uri and local part. The prefix
     * is unspecified (empty).
     *
     * @param namespaceURI The namespace for this name
     * @param localPart The local part of the name
     */
    public constructor(namespaceURI: String, localPart: String)

    /**
     * Create a new QName in the default namespace with the given local name and null prefix.
     * @param localPart The local part of the name. May not contain a '`:`' character.
     */
    public constructor(localPart: String)

    /**
     * Retrieve the prefix for this QName.
     */
    public fun getPrefix(): String

    /**
     * Retrieve the local part of this QName.
     */
    public fun getLocalPart(): String

    /**
     * Retrieve the namespace URI for this QName.
     */
    public fun getNamespaceURI(): String
}

/**
 * Determine whether two QNames are equivalent (the prefix is ignored, the others
 * compared textually).
 */
public infix fun QName.isEquivalent(other: QName): Boolean {
    return getLocalPart() == other.getLocalPart() &&
            getNamespaceURI() == other.getNamespaceURI()
}

/**
 * Determine whether the two QNames are fully equal
 */
@XmlUtilInternal
public infix fun QName.isFullyEqual(other: QName): Boolean {
    return isEquivalent(other) && getPrefix() == other.getPrefix()
}

/** Property syntax accessor for [QName.getPrefix] */
public inline val QName.prefix: String get() = getPrefix()
/** Property syntax accessor for [QName.getLocalPart] */
public inline val QName.localPart: String get() = getLocalPart()
/** Property syntax accessor for [QName.getNamespaceURI] */
public inline val QName.namespaceURI: String get() = getNamespaceURI()

/** Extract the namespace for the QName (excluding the local name) */
public fun QName.toNamespace(): Namespace {
    return XmlEvent.NamespaceImpl(prefix, namespaceURI)
}

/**
 * Shortcut alias for QName that associates the QNameSerializer with the type.
 * Note that the QNameSerializer has special features for the XML format.
 */
public typealias SerializableQName = @Serializable(QNameSerializer::class) QName

/**
 * Serializer for QNames that allows writing in XML context to be as `prefix:localPart` where
 * the namespace declaration is added when needed.
 *
 * As this serializer introduces content dependent namespaces that will cause for two-pass
 * serialization if `isCollectingNSAttributes` is `true`.
 */
@OptIn(ExperimentalSerializationApi::class)
public object QNameSerializer : XmlSerializer<QName> {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("javax.xml.namespace.QName") {
        val stringSerializer = String.serializer()
        element("namespace", stringSerializer.descriptor, isOptional = true)
        element("localPart", stringSerializer.descriptor)
        element("prefix", stringSerializer.descriptor, isOptional = true)
    }.xml(
        buildSerialDescriptor("javax.xml.namespace.QName", PrimitiveKind.STRING) {
            annotations = listOf(XmlDynamicNameMarker())
        },
        QName(XMLConstants.XSD_NS_URI, "QName", XMLConstants.XSD_PREFIX)
    )

    /**
     * Deserialize a cname string to a QName (using the input to resolve the prefix to namespace).
     */
    override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: QName?,
        isValueChild: Boolean
    ): QName {
        // This needs to be done here as the namespace attribute may have disappeared later. After reading the value
        // the cursor may be at an end tag (and the context no longer present)
        val namespaceContext = input.namespaceContext.freeze()

        val prefixedName = decoder.decodeString().trim()
        val cIndex = prefixedName.indexOf(':')

        val prefix: String
        val namespace: String
        val localPart: String

        when {
            cIndex < 0 -> {
                prefix = ""
                localPart = prefixedName
                namespace = namespaceContext.getNamespaceURI("") ?: ""
            }

            else -> {
                prefix = prefixedName.substring(0, cIndex)
                localPart = prefixedName.substring(cIndex + 1)
                namespace = namespaceContext.getNamespaceURI(prefix)
                    ?: throw SerializationException("Missing namespace for prefix $prefix in QName value")
            }
        }

        return QName(namespace, localPart, prefix)
    }

    /** Deserialize as struct */
    override fun deserialize(decoder: Decoder): QName = decoder.decodeStructure(descriptor) {
        var prefix = ""
        var namespace = ""
        lateinit var localPart: String

        loop@ while (true) {
            when (this.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> namespace = decodeStringElement(descriptor, 0)
                1 -> localPart = decodeStringElement(descriptor, 1)
                2 -> prefix = decodeStringElement(descriptor, 2)
            }
        }
        QName(namespace, localPart, prefix)
    }

    /** Serialize a QName as CName by writing a namespace attribute if needed. The prefix is used as hint only. */
    override fun serializeXML(encoder: Encoder, output: XmlWriter, value: QName, isValueChild: Boolean) {
        var effectivePrefix = when (value.namespaceURI) {
            output.getNamespaceUri(value.prefix) -> value.prefix
            else -> output.getPrefix(value.namespaceURI)
        }

        if (effectivePrefix == null) {
            effectivePrefix = if (value.prefix.isNotEmpty() && output.getNamespaceUri(value.prefix) == null) {
                checkNotNull(value.prefix)
            } else {
                IntRange(1, Int.MAX_VALUE).asSequence()
                    .map { "ns$it" }
                    .first { output.getNamespaceUri(it) == null }
            }
            output.namespaceAttr(effectivePrefix, value.namespaceURI)
        }
        encoder.encodeString("$effectivePrefix:${value.localPart}")
    }

    /** Regular serializatin as struct of namespaceUri, localpart and prefix. */
    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: QName) {
        encoder.encodeStructure(descriptor) {
            value.namespaceURI.let { ns ->
                if (ns.isNotEmpty() || shouldEncodeElementDefault(descriptor, 0)) {
                    encodeStringElement(descriptor, 0, ns)
                }
            }

            encodeStringElement(descriptor, 1, value.localPart)

            value.prefix.let { prefix ->
                if (prefix.isNotEmpty() || shouldEncodeElementDefault(descriptor, 2)) {
                    encodeStringElement(descriptor, 2, prefix)
                }
            }
        }
    }
}
