/*
 * Copyright (c) 2023-2026.
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

@file:Suppress("KDocUnresolvedReference")
@file:MustUseReturnValues

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.capturedKClass
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.core.impl.multiplatform.Language
import nl.adaptivity.xmlutil.core.impl.multiplatform.MpJvmDefaultWithCompatibility
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.serialization.XML.Companion.compat
import nl.adaptivity.xmlutil.serialization.XML.XmlCompanion
import nl.adaptivity.xmlutil.serialization.XmlConfig.CompatBuilder
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.DeclaredNameInfo
import nl.adaptivity.xmlutil.serialization.impl.*
import nl.adaptivity.xmlutil.serialization.structure.*
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.jvm.JvmOverloads

@ExperimentalXmlUtilApi
private val defaultXmlModule = getPlatformDefaultModule() + SerializersModule {
    contextual(QName::class, QNameSerializer)
}

@Suppress("MemberVisibilityCanBePrivate", "FunctionName")
/**
 * Class that provides access to XML parsing and serialization for the Kotlin Serialization system. In most cases the
 * companion functions would be used. Creating an object explicitly however allows for the serialization to be configured.
 *
 * **Note** that at this point not all configuration options will work on all platforms.
 *
 * The serialization can be configured with various annotations: [XmlSerialName], [XmlChildrenName], [XmlPolyChildren],
 * [XmlDefault], [XmlElement] [XmlValue]. These control the way the content is actually serialized. Those tags that support
 * being set on types (rather than properties) prefer values set on properties (the property can override settings on the
 * type).
 *
 * Serialization normally prefers to store values as attributes. This can be overridden by the [XmlElement] annotation
 * to force a tag child. Similarly [XmlValue] can be used for the child to be marked as textual content of the parent tag
 * - only one such child is allowed.
 *
 * Naming of tags and attributes follows some special rules. In particular attributes will be named based on their use
 * where tags are named based on their type. Both can be overridden by specifying [XmlSerialName] on the property.
 *
 * When names are not specified on types, their class name is used, but the package is normally omitted if it matches the
 * package name of the class that represents the parent. Attributes get their use site name which is either the property
 * name or the name modified through [SerialName]
 *
 * **Note** When using caching (default) this is not threadsafe within the same xml instance. This can be resolved using
 * a different FormatCache implementation that is threadsafe, or one using threadlocals.
 *
 * @property serializersModule The serialization context used to resolve serializers etc.
 * @property config The configuration of the various options that may apply.
 */
@OptIn(ExperimentalSerializationApi::class, ExperimentalXmlUtilApi::class)
public class XML(
    public val config: XmlConfig,
    serializersModule: SerializersModule = EmptySerializersModule()
) : StringFormat {
    override val serializersModule: SerializersModule = serializersModule + defaultXmlModule

    private fun <R> useUnsafeCodecConfig(action: (XmlCodecConfig) -> R): R {
        return this@XML.config.formatCache.useUnsafe { cache ->
            action(object : XmlCodecConfig {
                override val serializersModule: SerializersModule
                    get() = this@XML.serializersModule

                override val config: XmlConfig = this@XML.config.shadowCache(cache)
            })
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Update to the named factory functions")
    public constructor(
        serializersModule: SerializersModule = EmptySerializersModule(),
        configure: CompatBuilder.() -> Unit = {}
    ) : this(XmlConfig(CompatBuilder().apply(configure)), serializersModule)

    @Suppress("DEPRECATION")
    public fun copy(
        serializersModule: SerializersModule = this.serializersModule,
        configure: XmlConfig.CustomBuilder<XmlSerializationPolicy>.() -> Unit,
    ): XML {
        val newConfigBuilder = XmlConfig.CustomBuilder(config)

        newConfigBuilder.apply(configure)
        val oldCache = config.formatCache

        when (val p = newConfigBuilder.policy) {
            is DefaultXmlSerializationPolicy -> if (oldCache == p.formatCache) {
                newConfigBuilder.policy = p.copy { formatCache = formatCache.copy() }
            }

            is ShadowPolicy -> if (oldCache == p.cache) {
                newConfigBuilder.policy = ShadowPolicy(p.basePolicy, p.cache.copy())
            }
        }

        return XML(XmlConfig(newConfigBuilder), serializersModule)
    }

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        return encodeToString(serializer, value, null)
    }

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param value The actual object
     * @param serializer The serializer/saver to use to write
     * @param prefix The prefix (if any) to use for the namespace
     */
    public fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T, prefix: String?): String {
        val stringWriter = StringWriter()
        val xw = when {
            config.defaultToGenericParser ->
                xmlStreaming.newGenericWriter(stringWriter, config.repairNamespaces, config.xmlDeclMode)

            else -> xmlStreaming.newWriter(stringWriter, config.repairNamespaces, config.xmlDeclMode)
        }
        xw.use { xmlWriter ->
            encodeToWriter(xmlWriter, serializer, value, prefix)
        }
        return stringWriter.toString()
    }

    /**
     * Encode the given string value with the given root element name.
     */
    public inline fun <reified T> encodeToString(value: T, rootName: QName): String {
        return encodeToString(serializersModule.serializer<T>(), value, rootName)
    }

    /**
     * Encode the given string value with the given prefix.
     */
    public inline fun <reified T> encodeToString(value: T, prefix: String?): String {
        return encodeToString(serializersModule.serializer<T>(), value, prefix)
    }

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param value The actual object
     * @param serializer The serializer/saver to use to write
     * @param rootName The QName to use for the root tag
     */
    public fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T, rootName: QName): String {
        val stringWriter = StringWriter()
        val xw = when {
            config.defaultToGenericParser -> xmlStreaming.newGenericWriter(stringWriter, config.repairNamespaces, config.xmlDeclMode)
            else -> xmlStreaming.newWriter(stringWriter, config.repairNamespaces, config.xmlDeclMode)
        }

        xw.use { xmlWriter ->
            encodeToWriter(xmlWriter, serializer, value, rootName)
        }
        return stringWriter.toString()
    }

    /**
     * Write the object to the given writer
     *
     * @param value The actual object
     * @param target The [XmlWriter] to append the object to
     * @param prefix The prefix (if any) to use for the namespace
     */
    @JvmOverloads
    public inline fun <reified T : Any> encodeToWriter(target: XmlWriter, value: T, prefix: String? = null) {
        encodeToWriter(target, serializer<T>(), value, prefix)
    }

    /**
     * Transform onto an existing xml writer.
     *
     * @param target The [XmlWriter] to append the object to
     * @param value The actual object
     * @param serializer The serializer/saver to use to write
     * @param prefix The prefix (if any) to use for the namespace
     */
    public fun <T> encodeToWriter(
        target: XmlWriter,
        serializer: SerializationStrategy<T>,
        value: T,
        prefix: String? = null
    ) {
        target.indentString = config.indentString


        useUnsafeCodecConfig { codecConfig ->
            if (prefix != null) {
                val root =
                    XmlRootDescriptor(codecConfig, serializer.descriptor)

                val serialQName = root.getElementDescriptor(0).tagName.copy(prefix = prefix)

                encodeToWriter(codecConfig, target, serializer, value, serialQName)

            } else {
                encodeToWriter(codecConfig, target, serializer, value, rootName = null)
            }
        }

    }

    /**
     * Transform onto an existing xml writer.
     *
     * @param target The [XmlWriter] to append the object to
     * @param serializer The serializer/saver to use to write
     * @param rootName The QName to use for the root tag
     * @param value The actual object
     */
    public fun <T> encodeToWriter(
        target: XmlWriter,
        serializer: SerializationStrategy<T>,
        value: T,
        rootName: QName?,
    ) {
        useUnsafeCodecConfig { codecConfig ->
            encodeToWriter(codecConfig, target, serializer, value, rootName)
        }
    }

    private fun <T> encodeToWriter(
        unsafeCodecConfig: XmlCodecConfig,
        target: XmlWriter,
        serializer: SerializationStrategy<T>,
        value: T,
        rootName: QName?,
    ) {
        val config = unsafeCodecConfig.config
        target.indentString = config.indentString

        if (target.depth == 0) {
            when (config.xmlDeclMode) {
                XmlDeclMode.Minimal -> {
                    target.startDocument(config.xmlVersion.versionString)
                }

                XmlDeclMode.Charset -> {
                    // TODO support non-utf8 encoding
                    target.startDocument(config.xmlVersion.versionString, encoding = "UTF-8")
                }

                XmlDeclMode.None,
                XmlDeclMode.Auto -> Unit // no implementation needed
            }
        }

        val safeSerialName = serializer.descriptor.run { capturedKClass?.maybeSerialName ?: serialName }

        val declNameInfo = DeclaredNameInfo(safeSerialName, (serializer as? XmlSerialDescriptor)?.serialQName, false)

        val policyDerivedName =
            config.policy.serialTypeNameToQName(declNameInfo, DEFAULT_NAMESPACE)

        val rootNameInfo = rootNameInfo(unsafeCodecConfig, serializer.descriptor, rootName, policyDerivedName)
        val root = XmlRootDescriptor(unsafeCodecConfig, serializer.descriptor, rootNameInfo)

        val xmlDescriptor = root.getElementDescriptor(0)

        val xmlEncoderBase = XmlEncoderBase(serializersModule, config, target)
        val encoder = when {
            config.isCollectingNSAttributes -> {
                val collectedNamespaces = collectNamespaces(xmlDescriptor, xmlEncoderBase, serializer, value)
                val prefixMap = collectedNamespaces.associate { it.namespaceURI to it.prefix }

                @Suppress("DEPRECATION")
                val newConfig = XmlConfig(CompatBuilder(config).apply {
                    policy = PrefixWrappingPolicy(policy ?: policyBuilder().build(), prefixMap)
                })
                val remappedEncoderBase = XmlEncoderBase(serializersModule, newConfig, target)
                val newRootName = rootNameInfo.remapPrefix(prefixMap)

                val newRoot = XmlRootDescriptor(remappedEncoderBase, serializer.descriptor, newRootName)
                val newDescriptor = newRoot.getElementDescriptor(0)

                remappedEncoderBase.NSAttrXmlEncoder(
                    newDescriptor,
                    collectedNamespaces,
                    -1
                )
            }

            else -> xmlEncoderBase.XmlEncoder(xmlDescriptor, -1)
        }
        encoder.encodeSerializableValue(serializer, value)
        target.flush()
    }

    private fun <T> collectNamespaces(
        xmlDescriptor: XmlDescriptor,
        xmlEncoderBase: XmlEncoderBase,
        serializer: SerializationStrategy<T>,
        value: T
    ): List<Namespace> {
        val prefixToNamespaceMap = HashMap<String, String>()
        val namespaceToPrefixMap = HashMap<String, String>()

        val pendingNamespaces = HashSet<String>()
        val seenDescriptors = HashSet<XmlDescriptor>()
        var hasSeenDynamicQname = false

        fun collect(prefix: String, namespaceUri: String) {
            if (namespaceUri !in namespaceToPrefixMap) {
                if (prefix in prefixToNamespaceMap) { // prefix with different usage
                    // For the default namespace, always force this to be the empty prefix (remap
                    // all other namespaces)
                    if (namespaceUri.isEmpty()) {
                        prefixToNamespaceMap[""]?.let { oldDefaultNamespace ->
                            pendingNamespaces.add(oldDefaultNamespace)
                            namespaceToPrefixMap.remove(oldDefaultNamespace)
                        }
                        prefixToNamespaceMap[""] = ""
                        namespaceToPrefixMap[""] = ""
                    } else {
                        pendingNamespaces.add(namespaceUri)
                    }
                } else { // Prefix has not been seen before
                    if (namespaceUri in pendingNamespaces) { // If it matches a pending namespace use that
                        pendingNamespaces.remove(namespaceUri)
                    }
                    prefixToNamespaceMap[prefix] = namespaceUri
                    namespaceToPrefixMap[namespaceUri] = prefix
                }
            }
        }

        fun collect(descriptor: XmlDescriptor) {
            val prefix = descriptor.tagName.prefix
            val namespaceUri = descriptor.tagName.namespaceURI
            /* Don't register attributes without prefix in the default namespace (that doesn't
             * require namespace declarations). #135
             */
            if (descriptor.effectiveOutputKind != OutputKind.Attribute || namespaceUri.isNotEmpty() || prefix.isNotEmpty()) {
                collect(prefix, namespaceUri)
            }

            val childrenToCollect = mutableListOf<XmlDescriptor>()
            if (descriptor is XmlPolymorphicDescriptor) {
                childrenToCollect.addAll(descriptor.polyInfo.values)
            }
            for (elementIndex in 0 until descriptor.elementsCount) {
                childrenToCollect.add(descriptor.getElementDescriptor(elementIndex))
            }

            for (childDescriptor in childrenToCollect) {
                // Only check if we haven't seen a dynamic name yet.
                if (!hasSeenDynamicQname &&
                    ((childDescriptor.typeDescriptor.typeAnnHasDynamicNames) ||
                    (childDescriptor.overriddenSerializer.let { it is XmlSerializationStrategy<*> }))
                ) {
                    hasSeenDynamicQname = true
                    return
                }
                if (childDescriptor !in seenDescriptors) {
                    seenDescriptors.add(childDescriptor)
                    collect(childDescriptor)
                }
            }
        }

        val polyCollector = ChildCollector(null)
        xmlEncoderBase.serializersModule.dumpTo(polyCollector)

        collect(xmlDescriptor)

        for (childSerializer in polyCollector.children) {
            collect(xmlDescriptor(childSerializer))
        }

        if (hasSeenDynamicQname) {
            // reset the collection. We do a full two-pass, so don't need to include unused namespaces.
            prefixToNamespaceMap.clear()
            namespaceToPrefixMap.clear()

            // Collect all namespaces by actually generating the full document.
            val collector = NamespaceCollectingXmlWriter(prefixToNamespaceMap, namespaceToPrefixMap, pendingNamespaces)
            val base = XmlEncoderBase(xmlEncoderBase.serializersModule, xmlEncoderBase.config, collector)
            base.XmlEncoder(xmlDescriptor, -1).encodeSerializableValue(serializer, value)

        }

        var nsIdx = 1

        for (namespaceUri in pendingNamespaces) {
            while ("ns$nsIdx" in prefixToNamespaceMap) {
                nsIdx += 1
            }

            val prefix = "ns$nsIdx"
            prefixToNamespaceMap[prefix] = namespaceUri
            namespaceToPrefixMap[namespaceUri] = prefix
        }

        return prefixToNamespaceMap.asSequence()
            .filterNot { (prefix, ns) -> prefix.isEmpty() && ns.isEmpty() } // skip empy namespace
            .map { XmlEvent.NamespaceImpl(it.key, it.value) }
            .sortedBy { it.prefix }
            .toList()
    }

    /**
     * Decode the given string value using the deserializer. It is equivalent to
     * `decodeFromReader(deserializer, XmlStreaming.newReader(string))`.
     * @param deserializer The deserializer to use.
     * @param string The string input
     */
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, @Language("XML") string: String): T {
        val xr = when {
            config.defaultToGenericParser -> xmlStreaming.newGenericReader(string)
            else -> xmlStreaming.newReader(string)
        }

        return decodeFromReader(deserializer, xr)
    }

    /**
     * Decode the given string value using the deserializer. It is equivalent to
     * `decodeFromReader(deserializer, XmlStreaming.newReader(string))`.
     * @param deserializer The deserializer to use.
     * @param string The text to decode
     * @param rootName The expected name of the root element, if `null` it will be automatically detected.
     */
    public fun <T> decodeFromString(
        deserializer: DeserializationStrategy<T>,
        @Language("XML") string: String,
        rootName: QName?
    ): T {
        val xr = when {
            config.defaultToGenericParser -> xmlStreaming.newGenericReader(string)
            else -> xmlStreaming.newReader(string)
        }
        return decodeFromReader(deserializer, xr, rootName)
    }

    /**
     * Decode the given string value using the deserializer. It is equivalent to
     * `decodeFromReader(deserializer, XmlStreaming.newReader(string))`.
     * @param string The text to decode
     * @param rootName The expected name of the root element, if `null` it will be automatically detected.
     */
    public inline fun <reified T> decodeFromString(@Language("XML") string: String, rootName: QName?): T {
        return decodeFromString(serializersModule.serializer<T>(), string, rootName)
    }

    /**
     * Parse an object of the type [T] out of the reader. This function is intended mostly to be used indirectly where
     * though the reified function.
     *
     * @param reader An [XmlReader] that contains the XML from which to read the object
     * @param rootName The QName to use for the root tag
     * @param T The type to use to read the object
     */
    public inline fun <reified T : Any> decodeFromReader(reader: XmlReader, rootName: QName? = null): T =
        decodeFromReader(serializer(), reader, rootName)

    /**
     * Function that determines the "proper" use name requirement for a root tag.
     * @param descriptor The descriptor of the root tag
     * @param rootName The explicitly given name requirement
     * @param localName The qname from the reader or (for writer derived from the serial name - avoiding captured types)
     */
    private fun rootNameInfo(
        unsafeCodecConfig: XmlCodecConfig,
        descriptor: SerialDescriptor,
        rootName: QName?,
        localName: QName
    ): DeclaredNameInfo {
        if (rootName != null) {
            return DeclaredNameInfo(localName.localPart, rootName, false)
        }

        val tmpRoot =
            XmlRootDescriptor(unsafeCodecConfig, descriptor, DeclaredNameInfo(localName.localPart))

        val realName = tmpRoot.typeDescriptor.typeQname ?: localName

        return DeclaredNameInfo(realName.localPart, realName, false)
    }

    /**
     * Parse an object of the type [T] out of the reader. This function is intended mostly to be used indirectly where
     * though the reified function.
     *
     * @param reader An [XmlReader] that contains the XML from which to read the object
     * @param rootName The QName to use for the root tag
     * @param deserializer The loader to use to read the object, if `null` it will be automatically detected.
     */
    @JvmOverloads
    public fun <T> decodeFromReader(
        deserializer: DeserializationStrategy<T>,
        reader: XmlReader,
        rootName: QName? = null
    ): T {
        return useUnsafeCodecConfig { codecConfig ->
            decodeFromReader(codecConfig, deserializer, reader, rootName)
        }
    }

    /**
     * Decode the sequence of elements of type `T` incrementally. The elements are required to not
     * be primitives (that encode to/parse from text).
     *
     * There are two modes: a sequence of elements, and as a wrapped collection.
     *
     * Wrapped collections function as expected, and read first the wrapper element, then the
     * elements.
     *
     * For sequence of elements, parsing will either stop on end of the reader, or on an end element.
     * It is the responsbility of the caller to handle (push back) the end of element event where
     * it occurs.
     *
     * Note that when the element name is not provided, it is detected on the first element.
     * Subsequent elements must have the same name (namespace, localname).
     *
     * @param reader The reader used to read from
     * @param wrapperName The name of the wrapping element. Setting this value triggers wrapper mode.
     * @param elementName The name of the element. If null, automatically detected on content.
     */
    @ExperimentalXmlUtilApi
    public inline fun <reified T> decodeToSequence(
        reader: XmlReader,
        wrapperName: QName?,
        elementName: QName? = null,
    ): Sequence<T> = decodeToSequence(serializer<T>(), reader, wrapperName, elementName)

    /**
     * Decode a wrapped sequence of elements of type `T` incrementally. The elements are required to not
     * be primitives (that encode to/parse from text).
     *
     * This function will assume an unspecified wrapper element. It will read this element and
     * return a sequence of the child elements,
     *
     * Note that when the element name is not provided, it is detected on the first element.
     * Subsequent elements must have the same name (namespace, localname).
     *
     * @param reader The reader used to read from
     * @param elementName The name of the element. If null, automatically detected on content.
     */
    @ExperimentalXmlUtilApi
    public inline fun <reified T> decodeWrappedToSequence(
        reader: XmlReader,
        elementName: QName? = null,
    ): Sequence<T> = decodeWrappedToSequence(serializer<T>(), reader, elementName)

    /**
     * Decode a wrapped sequence of elements of type `T` incrementally. The elements are required to not
     * be primitives (that encode to/parse from text).
     *
     * This function will assume an unspecified wrapper element. It will read this element and
     * return a sequence of the child elements,
     *
     * Note that when the element name is not provided, it is detected on the first element.
     * Subsequent elements must have the same name (namespace, localname).
     *
     * @param deserializer The deserializer to decode the elements.
     * @param reader The reader used to read from
     * @param elementName The name of the element. If null, automatically detected on content.
     */
    @ExperimentalXmlUtilApi
    public fun <T> decodeWrappedToSequence(
        deserializer: DeserializationStrategy<T>,
        reader: XmlReader,
        elementName: QName? = null,
    ): Sequence<T> {
        reader.skipPreamble()
        if (reader.eventType != EventType.START_ELEMENT) {
            throw XmlException("Unexpected event when looking for wrapper element: ${reader.eventType}", reader.extLocationInfo)
        }

        val wrapperName = reader.name

        return useUnsafeCodecConfig { codecConfig ->
            decodeToSequence(codecConfig, deserializer, reader, wrapperName, elementName)
        }
    }


    /**
     * Decode the sequence of elements of type `T` incrementally. The elements are required to not
     * be primitives (that encode to/parse from text).
     *
     * There are two modes: a sequence of elements, and as a wrapped collection.
     *
     * Wrapped collections function as expected, and read first the wrapper element, then the
     * elements.
     *
     * For sequence of elements, parsing will either stop on end of the reader, or on an end element.
     * It is the responsbility of the caller to handle (push back) the end of element event where
     * it occurs.
     *
     * Note that when the element name is not provided, it is detected on the first element.
     * Subsequent elements must have the same name (namespace, localname).
     *
     * @param deserializer The deserializer to decode the elements.
     * @param reader The reader used to read from
     * @param wrapperName The name of the wrapping element. Setting this value triggers wrapper mode.
     * @param elementName The name of the element. If null, automatically detected on content.
     */
    @ExperimentalXmlUtilApi
    public fun <T> decodeToSequence(
        deserializer: DeserializationStrategy<T>,
        reader: XmlReader,
        wrapperName: QName?,
        elementName: QName? = null,
    ): Sequence<T> {
        return useUnsafeCodecConfig { codecConfig ->
            decodeToSequence(codecConfig, deserializer, reader, wrapperName, elementName)
        }
    }

    private fun <T> decodeToSequence(
        unsafeCodecConfig: XmlCodecConfig,
        deserializer: DeserializationStrategy<T>,
        reader: XmlReader,
        wrapperName: QName?,
        elementName: QName?,
    ): Sequence<T> {
        val (config, serializersModule) = unsafeCodecConfig

        require(deserializer.descriptor.kind !is PrimitiveKind) {
            "Deserialization to sequence does not support string elements"
        }

        reader.skipPreamble()

        val xmlDecoderBase = XmlDecoderBase(serializersModule, config, reader)

        if (wrapperName != null) {
            reader.require(EventType.START_ELEMENT, wrapperName)
            for (attrIdx in 0 until reader.attributeCount) {
                when (reader.getAttributeNamespace(attrIdx)) {
                    XMLConstants.XML_NS_URI,
                    XMLConstants.XSI_NS_URI, -> Unit // Ignore

                    else -> throw XmlException(
                        "Unexpected attribute in wrapper: ${reader.attributes[attrIdx]}",
                        reader.extLocationInfo
                    )
                }
            }

            do {
                val _ = reader.next()
            } while (reader.hasNext() && reader.isIgnorable())
        }

        // Empty sequence
        if (! reader.hasNext() || reader.eventType == EventType.END_ELEMENT) { return sequenceOf() }

        val elementNameInfo = rootNameInfo(unsafeCodecConfig, deserializer.descriptor, elementName, reader.name)
        val realElementName = elementNameInfo.annotatedName ?: reader.name
        val rootDescriptor = XmlRootDescriptor(xmlDecoderBase, deserializer.descriptor, elementNameInfo)

        val elementDescriptor = rootDescriptor.getElementDescriptor(0)

        return sequence {
            do {
                when (reader.eventType) {
                    EventType.START_ELEMENT -> {
                        val polyInfo: PolyInfo? = polyInfoForElement(elementDescriptor, reader, rootDescriptor, "element")

                        if (polyInfo == null && ! reader.name.isEquivalent(realElementName)) {
                            throw XmlException("Unexpected child element ${reader.name} instead of ${realElementName}", reader.extLocationInfo)
                        }

                        val decoder = xmlDecoderBase.XmlDecoder(elementDescriptor, polyInfo, inheritedPreserveWhitespace = DocumentPreserveSpace.DEFAULT)
                        yield(decoder.decodeSerializableValue(deserializer))
                    }

                    EventType.TEXT if (! reader.isIgnorable()) ->
                        throw XmlException("Unexpected text in sequence decoding: '${reader.text}'", reader.extLocationInfo)

                    EventType.TEXT,
                    EventType.PROCESSING_INSTRUCTION,
                    EventType.IGNORABLE_WHITESPACE,
                    EventType.END_ELEMENT -> Unit // ignore

                    else -> throw XmlException("Unexpected event type: ${reader.eventType}", reader.extLocationInfo)
                }
            } while (reader.hasNext() && reader.next() != EventType.END_ELEMENT)

            if (wrapperName != null) {
                reader.require(EventType.END_ELEMENT, wrapperName)
            }
        }


    }

    private fun <T> decodeFromReader(
        unsafeCodecConfig: XmlCodecConfig,
        deserializer: DeserializationStrategy<T>,
        reader: XmlReader,
        rootName: QName? = null
    ): T {
        val (config, serializersModule) = unsafeCodecConfig

        // We skip all ignorable content here. To get started while supporting direct content we need to put the parser
        // in the correct state of having just read the startTag (that would normally be read by the code that determines
        // what to parse (before calling readSerializableValue on the value)
        reader.skipPreamble()

        val xmlDecoderBase = XmlDecoderBase(serializersModule, config, reader)
        val rootNameInfo = rootNameInfo(unsafeCodecConfig, deserializer.descriptor, rootName, reader.name)
        val rootDescriptor = XmlRootDescriptor(xmlDecoderBase, deserializer.descriptor, rootNameInfo)

        val elementDescriptor = rootDescriptor.getElementDescriptor(0)

        val polyInfo: PolyInfo? = polyInfoForElement(elementDescriptor, reader, rootDescriptor, "root")

        val decoder = xmlDecoderBase.XmlDecoder(elementDescriptor, polyInfo, inheritedPreserveWhitespace = DocumentPreserveSpace.DEFAULT)
        return decoder.decodeSerializableValue(deserializer)
    }

    private fun polyInfoForElement(
        elementDescriptor: XmlDescriptor,
        reader: XmlReader,
        rootDescriptor: XmlRootDescriptor,
        tagName: String,
    ): PolyInfo? = if (elementDescriptor is XmlPolymorphicDescriptor) {
        val tagName = reader.name
        val info = elementDescriptor.polyInfo.values.singleOrNull {
            tagName.isEquivalent(it.tagName)
        }
        info?.let { PolyInfo(tagName, 0, it) }
    } else {
        // only check names when not having polymorphic root
        val serialName = rootDescriptor.getElementDescriptor(0).tagName
        if (!serialName.isEquivalent(reader.name)) {
            throw XmlException("Local name \"${reader.name}\" for ${tagName} tag does not match expected name \"$serialName\"")
        }
        null
    }

    @JvmOverloads
    public fun xmlDescriptor(serializer: SerializationStrategy<*>, rootName: QName? = null): XmlDescriptor {
        return useUnsafeCodecConfig { xmlDescriptor(it, serializer.descriptor, rootName) }
    }

    @JvmOverloads
    public fun xmlDescriptor(deserializer: DeserializationStrategy<*>, rootName: QName? = null): XmlDescriptor {
        return useUnsafeCodecConfig { xmlDescriptor(it, deserializer.descriptor, rootName) }
    }

    @JvmOverloads
    public fun xmlDescriptor(serializer: KSerializer<*>, rootName: QName? = null): XmlDescriptor {
        return useUnsafeCodecConfig { xmlDescriptor(it, serializer.descriptor, rootName) }
    }

    private fun xmlDescriptor(
        unsafeCodecConfig: XmlCodecConfig,
        serialDescriptor: SerialDescriptor,
        rootName: QName? = null
    ): XmlRootDescriptor {
        val nameInfo = DeclaredNameInfo(rootName?.localPart ?: serialDescriptor.serialName, rootName, false)

        return XmlRootDescriptor(unsafeCodecConfig, serialDescriptor, nameInfo)
    }

    public abstract class XmlCompanion<B: XmlConfig.Builder<out XmlSerializationPolicy?>>: StringFormat {
        override val serializersModule: SerializersModule = EmptySerializersModule()

        public abstract val instance: XML

        /**
         * Retrieve a builder for the recommended configuration
         * @suppress
         */
        @XmlUtilInternal
        public abstract fun recommendedBuilder(): B

        /**
         * Retrieve a builder for the recommended **fast** configuration
         * @suppress
         */
        @XmlUtilInternal
        public abstract fun fastBuilder(): B

        public operator fun invoke(): XML {
            return XML(XmlConfig(recommendedBuilder()), serializersModule)
        }

        public operator fun invoke(serializersModule: SerializersModule): XML {
            return XML(XmlConfig(recommendedBuilder()), serializersModule)
        }

        public inline operator fun invoke(
            serializersModule: SerializersModule,
            configure: B.() -> Unit = {}
        ): XML = XML(
            XmlConfig(this.recommendedBuilder().apply(configure)),
            serializersModule
        )

        public inline operator fun invoke(
            configure: B.() -> Unit = {}
        ): XML = XML(
            XmlConfig(this.recommendedBuilder().apply(configure)),
            serializersModule
        )

        @Deprecated("Use invoke operator", ReplaceWith("this(serializersModule)"))
        public fun recommended(serializersModule: SerializersModule): XML {
            return this(serializersModule)
        }

        @Deprecated("Use invoke operator", ReplaceWith("this()"))
        public open fun recommended(): XML {
            return this(serializersModule)
        }

        @Deprecated("Use invoke operator", ReplaceWith("this(configure)"))
        public inline fun recommended(
            configure: B.() -> Unit
        ): XML = invoke(configure)

        @Deprecated("Use invoke operator", ReplaceWith("this(serializersModule, configure)"))
        public inline fun recommended(
            serializersModule: SerializersModule,
            configure: B.() -> Unit
        ): XML = invoke(serializersModule, configure)

        public inline fun fast(
            configure: B.() -> Unit = {}
        ): XML = XML(
            XmlConfig(this.fastBuilder().apply(configure)),
            serializersModule
        )

        public inline fun fast(
            serializersModule: SerializersModule,
            configure: B.() -> Unit = {}
        ): XML = XML(
            XmlConfig(this.fastBuilder().apply(configure)),
            serializersModule
        )

        public open fun fast(): XML {
            return XML(XmlConfig(fastBuilder()), serializersModule)
        }

        public fun fast(serializersModule: SerializersModule): XML {
            return XML(XmlConfig(fastBuilder()), serializersModule)
        }

        /**
         * Shortcut function that creates a format instance that is compact. Note that this is a shortcut
         * for calling `recommended { compact() }`, for reconfiguration that should be used.
         */
        public open fun compact(
            serializersModule: SerializersModule = EmptySerializersModule(),
        ): XML = this(serializersModule) { this.compact() }

        @PublishedApi
        internal fun <P : XmlSerializationPolicy> customBuilder(policy: P): XmlConfig.CustomBuilder<P> {
            return XmlConfig.CustomBuilder(policy = policy)
        }

        /**
         * Create a configuration corresponding to a custom xml serialization policy.
         *
         * @param policy The custom policy to use
         * @param serializersModule The serializers module to use
         * @param configure Configuration lambda that can be used to configure the XML configuration
         *   options (that are not part of the policy).
         */
        public inline fun <P : XmlSerializationPolicy> customPolicy(
            policy: P,
            serializersModule: SerializersModule = EmptySerializersModule(),
            configure: XmlConfig.CustomBuilder<P>.() -> Unit
        ): XML = XML(
            XmlConfig(customBuilder(policy).apply<XmlConfig.CustomBuilder<P>>(configure)),
            serializersModule
        )

        public inline fun <reified T> xmlDescriptor(rootName: QName? = null): XmlDescriptor =
            xmlDescriptor(serializer<T>(), rootName)

        public fun xmlDescriptor(serializer: SerializationStrategy<*>, rootName: QName? = null): XmlDescriptor =
            instance.xmlDescriptor(serializer, rootName)

        public fun xmlDescriptor(deserializer: DeserializationStrategy<*>, rootName: QName? = null): XmlDescriptor =
            instance.xmlDescriptor(deserializer, rootName)

        public fun xmlDescriptor(serializer: KSerializer<*>, rootName: QName? = null): XmlDescriptor =
            instance.xmlDescriptor(serializer, rootName)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [Serializable].
         * @param value The object to transform
         * @param serializer The serializer to user
         */
        final override fun <T> encodeToString(
            serializer: SerializationStrategy<T>,
            value: T,
        ): String = encodeToString(serializer, value, null)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [Serializable].
         * @param value The object to transform
         * @param serializer The serializer to user
         * @param prefix The namespace prefix to use
         */
        public fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T, prefix: String?): String =
            instance.encodeToString(serializer, value, prefix)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [Serializable].
         * @param value The object to transform
         * @param serializer The serializer to user
         * @param rootName The QName to use for the root tag
         */
        public fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T, rootName: QName): String =
            instance.encodeToString(serializer, value, rootName)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [Serializable].
         * @param obj The object to transform
         * @param prefix The namespace prefix to use
         */
        public inline fun <reified T : Any> encodeToString(obj: T, prefix: String? = null): String =
            encodeToString(serializer<T>(), obj, prefix)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [Serializable].
         * @param obj The object to transform
         * @param rootName The QName to use for the root tag
         */
        public inline fun <reified T : Any> encodeToString(obj: T, rootName: QName): String =
            encodeToString(serializer<T>(), obj, rootName)

        /**
         * Write the object to the given writer
         *
         * @param target The [XmlWriter] to append the object to
         * @param value The actual object
         * @param prefix The prefix (if any) to use for the namespace
         */
        public inline fun <reified T : Any> encodeToWriter(target: XmlWriter, value: T, prefix: String? = null) {
            encodeToWriter(target, serializer<T>(), value, prefix)
        }

        /**
         * Write the object to the given writer
         *
         * @param target The [XmlWriter] to append the object to
         * @param value The actual object
         * @param rootName The QName to use for the root tag
         */
        public inline fun <reified T : Any> encodeToWriter(target: XmlWriter, value: T, rootName: QName) {
            encodeToWriter(target, serializer<T>(), value, rootName)
        }

        /**
         * Write the object to the given writer
         *
         * @param target The [XmlWriter] to append the object to
         * @param serializer The serializer to use
         * @param value The actual object
         * @param prefix The prefix (if any) to use for the namespace
         */
        public fun <T> encodeToWriter(
            target: XmlWriter,
            serializer: SerializationStrategy<T>,
            value: T,
            prefix: String? = null
        ) {
            instance.encodeToWriter(target, serializer, value, prefix)
        }

        /**
         * Write the object to the given writer
         *
         * @param target The [XmlWriter] to append the object to
         * @param serializer The serializer to use
         * @param value The actual object
         * @param rootName The QName to use for the root tag
         */
        public fun <T> encodeToWriter(
            target: XmlWriter,
            serializer: SerializationStrategy<T>,
            value: T,
            rootName: QName
        ) {
            @Suppress("DEPRECATION")
            instance.encodeToWriter(target, serializer, value, rootName)
        }

        /**
         * Parse an object of the type [T] out of the reader
         * @param str The source of the XML events
         * @param rootName The QName to use for the root tag
         */
        @JvmOverloads
        public inline fun <reified T : Any> decodeFromString(str: String, rootName: QName? = null): T =
            decodeFromString(serializer(), str, rootName)

        /**
         * Parse an object of the type [T] out of the reader
         * @param deserializer The loader to use
         * @param string The source of the XML events
         */
        override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
            return instance.decodeFromString(deserializer, string)
        }

        /**
         * Parse an object of the type [T] out of the reader
         * @param deserializer The loader to use
         * @param rootName The QName to use for the root tag
         * @param string The source of the XML events
         */
        public fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String, rootName: QName?): T {
            return instance.decodeFromString(deserializer, string, rootName)
        }

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         * @param rootName The QName to use for the root tag
         */
        public inline fun <reified T : Any> decodeFromReader(reader: XmlReader, rootName: QName? = null): T =
            decodeFromReader(serializer(), reader, rootName)

        /**
         * Parse an object of the type [T] out of the reader
         * @param deserializer The loader to use (rather than the default)
         * @param rootName The QName to use for the root tag
         * @param reader The source of the XML events
         */
        @JvmOverloads
        public fun <T : Any> decodeFromReader(
            deserializer: DeserializationStrategy<T>,
            reader: XmlReader,
            rootName: QName? = null
        ): T = instance.decodeFromReader(deserializer, reader, rootName)
    }

    public companion object : StringFormat {

        @Suppress("DEPRECATION")
        @Deprecated("Consider using the 1.0 object, but note it has changed defaults", ReplaceWith("XML.v1"))
        public val compat: XmlCompanion<CompatBuilder> get() = Compat

        @Suppress("DEPRECATION")
        public val v1: XmlCompanion<XmlConfig.DefaultBuilder> get() = XML1

        @PublishedApi
        internal fun recommended1_0Builder(): XmlConfig.DefaultBuilder =
            XmlConfig.DefaultBuilder().apply { recommended_1_0_0() }

        public inline fun recommended_1_0(
            serializersModule: SerializersModule = EmptySerializersModule(),
            configure: XmlConfig.DefaultBuilder.() -> Unit = {}
        ): XML = XML(
            XmlConfig(recommended1_0Builder().apply(configure)),
            serializersModule
        )

        @PublishedApi
        internal fun fast1_0Builder(): XmlConfig.DefaultBuilder =
            XmlConfig.DefaultBuilder().apply { fast_1_0_0() }

        public inline fun fast_1_0(
            serializersModule: SerializersModule = EmptySerializersModule(),
            configure: XmlConfig.DefaultBuilder.() -> Unit = {}
        ): XML = XML(
            XmlConfig(fast1_0Builder().apply(configure)),
            serializersModule
        )

        @Suppress("DEPRECATION")
        @Deprecated("Replace with compat instance", ReplaceWith("XML.compat.instance"))
        public val defaultInstance: XML get() = Compat.instance

        @Suppress("DEPRECATION")
        @Deprecated("Replace with XML1_0", ReplaceWith("XML.v1.instance"))
        public val defaultInstance_1_0: XML1_0 get() = XML1_0

        @Suppress("DEPRECATION")
        @Deprecated("Consider using the 1.0 object, but note it has changed defaults", ReplaceWith("XML.v1"))
        public fun compat(
            serializersModule: SerializersModule = EmptySerializersModule(),
            configure: CompatBuilder.() -> Unit = {}
        ): XML {
            return XML(XmlConfig(CompatBuilder().apply(configure)), serializersModule)
        }

        @Deprecated(
            "Consider replacing with versioned default that provides a stable serialized format",
            ReplaceWith("v1.recommended(serializersModule, configure)")
        )
        public inline fun recommended(
            serializersModule: SerializersModule = EmptySerializersModule(),
            configure: XmlConfig.DefaultBuilder.() -> Unit = {}
        ): XML = v1(serializersModule, configure)

        @Suppress("DEPRECATION")
        @Deprecated("The format no longer has a default serializers module", ReplaceWith("EmptySerializersModule()"))
        override val serializersModule: SerializersModule
            get() = compat.serializersModule

        @Deprecated("Use a versioned instance", ReplaceWith("defaultInstance_1_0.xmlDescriptor(serializer)"))
        public fun xmlDescriptor(serializer: SerializationStrategy<*>): XmlDescriptor {
            @Suppress("DEPRECATION")
            return compat.xmlDescriptor(serializer)
        }

        @Deprecated("Use a versioned instance", ReplaceWith("defaultInstance_1_0.xmlDescriptor(deserializer)"))
        public fun xmlDescriptor(deserializer: DeserializationStrategy<*>): XmlDescriptor {
            @Suppress("DEPRECATION")
            return compat.xmlDescriptor(deserializer)
        }

        @Deprecated("Use a versioned instance", ReplaceWith("defaultInstance_1_0.xmlDescriptor(serializer)"))
        public fun xmlDescriptor(serializer: KSerializer<*>): XmlDescriptor {
            @Suppress("DEPRECATION")
            return compat.xmlDescriptor(serializer)
        }

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [Serializable].
         * @param value The object to transform
         * @param serializer The serializer to user
         */
        @Suppress("DEPRECATION")
        @Deprecated("Use a versioned instance", ReplaceWith("defaultInstance_1_0.encodeToString(serializer, value)"))
        override fun <T> encodeToString(
            serializer: SerializationStrategy<T>,
            value: T
        ): String = compat.encodeToString(serializer, value)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [Serializable].
         * @param value The object to transform
         * @param serializer The serializer to user
         * @param prefix The namespace prefix to use
         */
        @Suppress("DEPRECATION")
        @Deprecated(
            "Use a versioned instance",
            ReplaceWith("defaultInstance_1_0.encodeToString(serializer, value, prefix)")
        )
        public fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T, prefix: String?): String =
            compat.encodeToString(serializer, value, prefix)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [Serializable].
         * @param value The object to transform
         * @param serializer The serializer to user
         * @param rootName The QName to use for the root tag
         */
        @Suppress("DEPRECATION")
        @Deprecated(
            "Use a versioned instance",
            ReplaceWith("defaultInstance_1_0.encodeToString(serializer, value, rootName)")
        )
        public fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T, rootName: QName): String =
            compat.encodeToString(serializer, value, rootName)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [Serializable].
         * @param obj The object to transform
         * @param prefix The namespace prefix to use
         */
        @Suppress("DEPRECATION")
        @Deprecated("Use a versioned instance", ReplaceWith("defaultInstance_1_0.encodeToString(obj, prefix)"))
        public inline fun <reified T : Any> encodeToString(obj: T, prefix: String? = null): String =
            encodeToString(serializer<T>(), obj, prefix)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [Serializable].
         * @param obj The object to transform
         * @param rootName The QName to use for the root tag
         */
        @Suppress("DEPRECATION")
        @Deprecated("Use a versioned instance", ReplaceWith("defaultInstance_1_0.encodeToString(obj, rootName)"))
        public inline fun <reified T : Any> encodeToString(obj: T, rootName: QName): String =
            encodeToString(serializer<T>(), obj, rootName)

        /**
         * Write the object to the given writer
         *
         * @param target The [XmlWriter] to append the object to
         * @param value The actual object
         * @param prefix The prefix (if any) to use for the namespace
         */
        @Deprecated(
            "Use a versioned instance",
            ReplaceWith("defaultInstance_1_0.encodeToWriter(target, value, prefix)")
        )
        public inline fun <reified T : Any> encodeToWriter(target: XmlWriter, value: T, prefix: String? = null) {
            @Suppress("DEPRECATION")
            encodeToWriter(target, serializer<T>(), value, prefix)
        }

        /**
         * Write the object to the given writer
         *
         * @param target The [XmlWriter] to append the object to
         * @param value The actual object
         * @param rootName The QName to use for the root tag
         */
        @Deprecated(
            "Use a versioned instance",
            ReplaceWith("defaultInstance_1_0.encodeToWriter(target, value, rootName)")
        )
        public inline fun <reified T : Any> encodeToWriter(target: XmlWriter, value: T, rootName: QName) {
            @Suppress("DEPRECATION")
            encodeToWriter(target, serializer<T>(), value, rootName)
        }

        /**
         * Write the object to the given writer
         *
         * @param target The [XmlWriter] to append the object to
         * @param serializer The serializer to use
         * @param value The actual object
         * @param prefix The prefix (if any) to use for the namespace
         */
        @Deprecated(
            "Use a versioned instance",
            ReplaceWith("defaultInstance_1_0.encodeToWriter(target, serializer, value, prefix)")
        )
        public fun <T> encodeToWriter(
            target: XmlWriter,
            serializer: SerializationStrategy<T>,
            value: T,
            prefix: String? = null
        ) {
            @Suppress("DEPRECATION")
            compat.encodeToWriter(target, serializer, value, prefix)
        }

        /**
         * Write the object to the given writer
         *
         * @param target The [XmlWriter] to append the object to
         * @param serializer The serializer to use
         * @param value The actual object
         * @param rootName The QName to use for the root tag
         */
        @Deprecated(
            "Use a versioned instance",
            ReplaceWith("defaultInstance_1_0.encodeToWriter(target, serializer, value, rootName)")
        )
        public fun <T> encodeToWriter(
            target: XmlWriter,
            serializer: SerializationStrategy<T>,
            value: T,
            rootName: QName
        ) {
            @Suppress("DEPRECATION")
            compat.encodeToWriter(target, serializer, value, rootName)
        }

        /**
         * Parse an object of the type [T] out of the reader
         * @param str The source of the XML events
         * @param rootName The QName to use for the root tag
         */
        @Suppress("DEPRECATION")
        @JvmOverloads
        @Deprecated("Use a versioned instance", ReplaceWith("defaultInstance_1_0.decodeFromString(str, rootName)"))
        public inline fun <reified T : Any> decodeFromString(str: String, rootName: QName? = null): T =
            decodeFromString(serializer(), str, rootName)

        /**
         * Parse an object of the type [T] out of the reader
         * @param deserializer The loader to use
         * @param string The source of the XML events
         */
        @Deprecated("Use a versioned instance", ReplaceWith("defaultInstance_1_0.decodeFromString(deserializer, str)"))
        override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
            @Suppress("DEPRECATION")
            return compat.decodeFromString(deserializer, string)
        }

        /**
         * Parse an object of the type [T] out of the reader
         * @param deserializer The loader to use
         * @param rootName The QName to use for the root tag
         * @param string The source of the XML events
         */
        @Deprecated(
            "Use a versioned instance",
            ReplaceWith("defaultInstance_1_0.decodeFromString(deserializer, str, rootName)")
        )
        public fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String, rootName: QName?): T {
            @Suppress("DEPRECATION")
            return compat.decodeFromString(deserializer, string, rootName)
        }

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         * @param rootName The QName to use for the root tag
         */
        @Suppress("DEPRECATION")
        @JvmOverloads
        @Deprecated("Use a versioned instance", ReplaceWith("defaultInstance_1_0.decodeFromString(reader, rootName)"))
        public inline fun <reified T : Any> decodeFromReader(reader: XmlReader, rootName: QName? = null): T =
            decodeFromReader(serializer(), reader, rootName)

        /**
         * Parse an object of the type [T] out of the reader
         * @param deserializer The loader to use (rather than the default)
         * @param rootName The QName to use for the root tag
         * @param reader The source of the XML events
         */
        @Suppress("DEPRECATION")
        @JvmOverloads
        @Deprecated(
            "Use a versioned instance",
            ReplaceWith("defaultInstance_1_0.decodeFromString(deserializer, reader, rootName)")
        )
        public fun <T : Any> decodeFromReader(
            deserializer: DeserializationStrategy<T>,
            reader: XmlReader,
            rootName: QName? = null
        ): T = compat.decodeFromReader(deserializer, reader, rootName)

    }

    @MpJvmDefaultWithCompatibility
    public interface XmlCodecConfig {
        /**
         * The currently active serialization context
         */
        public val serializersModule: SerializersModule

        /**
         * The configuration used for serialization
         */
        public val config: XmlConfig

        /**
         * A delegate method to get access to a format with the same configuration
         */
        public fun delegateFormat(): XML = XML(config, serializersModule)
    }

    internal operator fun XmlCodecConfig.component1() = config
    internal operator fun XmlCodecConfig.component2() = serializersModule

    /**
     * An interface that allows custom serializers to special case being serialized to XML and retrieve the underlying
     * [XmlWriter]. This is used for example by [CompactFragment] to make the fragment transparent when serializing to
     * XML.
     */
    @MpJvmDefaultWithCompatibility
    public interface XmlOutput : XmlCodecConfig {
        /**
         * The name for the current tag
         */
        public val serialName: QName

        /**
         * Ensure that the prefix of the [qName] is recorded (and the prefix added). This will not
         * add the actual name anywhere, just ensures the namespace attribute if needed
         *
         * @param qName The name to try to ensure is valid
         * @return The [QName] to use. This may have a different prefix if the prefix for the parameter would be
         *         conflicting.
         */
        public fun ensureNamespace(qName: QName): QName = ensureNamespace(qName, false)

        /**
         * Ensure that the prefix of the [qName] is recorded (and the prefix added). This will not
         * add the actual name anywhere, just ensures the namespace attribute if needed
         *
         * @param qName The name to try to ensure is valid
         * @param isAttr Ensure handling attribute default namespaces correctly
         * @return The [QName] to use. This may have a different prefix if the prefix for the parameter would be
         *         conflicting.
         */
        public fun ensureNamespace(qName: QName, isAttr: Boolean): QName

        /**
         * The XmlWriter used. Can be used directly by serializers
         */
        public val target: XmlWriter

    }

    /**
     * An interface that allows custom serializers to special case being serialized to XML and retrieve the underlying
     * [XmlReader]. This is used for example by [CompactFragment] to read arbitrary XML from the stream and store it inside
     * the buffer (without attempting to use the serializer/decoder for it.
     */
    @MpJvmDefaultWithCompatibility
    public interface XmlInput : XmlCodecConfig {
        /**
         * The reader used. Can be used directly by serializers
         */
        public val input: XmlReader

        public fun getNamespaceURI(prefix: String): String? = input.namespaceContext.getNamespaceURI(prefix)
    }

    /**
     * Class to support recovery in parsing.
     * @property elementIndex The index of the child element that is the data that is parsed
     * @property value The value for the particular property
     * @property unParsed It is also possible to just provide a property index. In this case the value
     *                    of this property should be `true` and the value of the [value] property is ignored (should
     *                    be null)
     */
    @ExperimentalXmlUtilApi
    public data class ParsedData<T>(public val elementIndex: Int, public val value: T, val unParsed: Boolean = false)

}

/**
 * Helper function to implement the shared functionality converting an annotation to a name.
 * There are various annotations that require a local name to be specified. For this case we
 * have the default for the serialName attribute be that local name.
 */
private fun annotationToQName(annLocalPart: String, annNamespace: String, annPrefix: String, parentNamespace: Namespace?, serialName: String = annLocalPart): QName {
    val effectiveNamespace = when {
        annNamespace != UNSET_ANNOTATION_VALUE -> annNamespace
        annPrefix == "xml" -> XMLConstants.XML_NS_URI
        else -> annNamespace
    }
    return when {
        effectiveNamespace == UNSET_ANNOTATION_VALUE -> when {
            annLocalPart == UNSET_ANNOTATION_VALUE -> parentNamespace?.let { QName(it.namespaceURI, serialName) }
                ?: QName(serialName)

            else -> parentNamespace?.let { QName(it.namespaceURI, annLocalPart) } ?: QName(annLocalPart)
        }

        annLocalPart == UNSET_ANNOTATION_VALUE -> when (annPrefix) {
            UNSET_ANNOTATION_VALUE -> QName(effectiveNamespace, serialName)
            else -> QName(serialName, effectiveNamespace, annPrefix)
        }

        annNamespace == XMLConstants.XML_NS_URI -> QName(XMLConstants.XML_NS_URI, annLocalPart, XMLConstants.XML_NS_PREFIX)

        annPrefix == UNSET_ANNOTATION_VALUE -> QName(effectiveNamespace, annLocalPart)

        else -> QName(effectiveNamespace, annLocalPart, annPrefix)
    }



}

public fun XmlSerialName.toQName(serialName: String, parentNamespace: Namespace?): QName {
    return annotationToQName(value, namespace, prefix, parentNamespace, serialName)
}

public fun XmlChildrenName.toQName(): QName {
    return toQName(null)
}

internal fun XmlChildrenName.toQName(parentNamespace: Namespace?): QName {
    return annotationToQName(value, namespace, prefix, parentNamespace)
}

internal fun XmlKeyName.toQName(parentNamespace: Namespace?): QName {
    return annotationToQName(value, namespace, prefix, parentNamespace)
}

internal fun XmlMapEntryName.toQName(parentNamespace: Namespace?): QName {
    return annotationToQName(value, namespace, prefix, parentNamespace)
}

internal inline fun <reified T> Iterable<*>.firstOrNull(): T? {
    for (e in this) {
        if (e is T) return e
    }
    return null
}


@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.getValueChild(): Int {
    for (i in 0 until elementsCount) {
        if (getElementAnnotations(i).any { it is XmlValue }) return i
    }
    return CompositeDecoder.UNKNOWN_NAME
}

internal fun XmlDescriptor.getValueChild(): Int {
    return (this as? XmlCompositeDescriptor)?.valueChild ?: -1
}

internal fun XmlDescriptor.getAttrMap(): Int {
    return (this as? XmlCompositeDescriptor)?.attrMapChild ?: -1
}

/** Straightforward copy function */
internal fun QName.copy(
    namespaceURI: String = this.namespaceURI,
    localPart: String = this.localPart,
    prefix: String = this.prefix
) =
    QName(namespaceURI, localPart, prefix)

/** Shortcircuit copy function that creates a new version (if needed) with the new prefix only */
internal fun QName.copy(prefix: String = this.prefix) = when (prefix) {
    this.prefix -> this
    else -> QName(namespaceURI, localPart, prefix)
}

@RequiresOptIn("This function will become private in the future", RequiresOptIn.Level.WARNING)
public annotation class WillBePrivate


@Suppress("DEPRECATION")
@Deprecated("Consider using the 1.0 object", ReplaceWith("XML1_0"))
private object Compat: XmlCompanion<CompatBuilder>() {
    override val instance: XML = compat { defaultPolicy { } }

    @XmlUtilInternal
    override fun recommendedBuilder(): CompatBuilder {
        return CompatBuilder().apply {
            setIndent(4)
            repairNamespaces = false
            xmlVersion = XmlVersion.XML11
            xmlDeclMode = XmlDeclMode.Minimal
            policy = compatPolicyBuilder().apply {
                setDefaults_0_91_0()
            }.build()
        }
    }

    @XmlUtilInternal
    override fun fastBuilder(): CompatBuilder {
        return recommendedBuilder().apply {
            fast_0_91_1()
        }
    }
}

private object XML1: XmlCompanion<XmlConfig.DefaultBuilder>() {

    override val instance: XML = XML.recommended_1_0()

    override val serializersModule: SerializersModule = EmptySerializersModule()

    @XmlUtilInternal
    override fun recommendedBuilder(): XmlConfig.DefaultBuilder =
        XmlConfig.DefaultBuilder().apply { recommended_1_0_0() }

    @XmlUtilInternal
    override fun fastBuilder(): XmlConfig.DefaultBuilder =
        XmlConfig.DefaultBuilder().apply { fast_1_0_0() }

}


/**
 * Shortcut to the default 1.0 instance
 */
@Deprecated(
    "Use the encapsulated context",
    ReplaceWith("XML.v1", "nl.adaptivity.xmlutil.serialization.XML")
)
public object XML1_0: XmlCompanion<XmlConfig.DefaultBuilder>() {

    override val instance: XML = XML.recommended_1_0()

    @Deprecated("Instead of an instance, you can use the compact shorthand",
        ReplaceWith("XML.v1.compact()", "nl.adaptivity.xmlutil.serialization.XML")
    )
    public val compactInstance: XML = XML.v1.compact()

    @Suppress("DEPRECATION")
    @Deprecated(
        "Incorrect", ReplaceWith("XML.v1", "nl.adaptivity.xmlutil.serialization.XML")
    )
    public val defaultInstance1_0: XML1_0 get() = XML1_0

    override val serializersModule: SerializersModule = EmptySerializersModule()

    @Deprecated(
        "Use the compact shorthand inside the builder",
        ReplaceWith("XML.v1(serializersModule) { compact() }",
            "nl.adaptivity.xmlutil.serialization.XML"
        )
    )
    public override fun compact(serializersModule: SerializersModule): XML {
        return XML.v1(serializersModule) { compact() }
    }

    @XmlUtilInternal
    override fun recommendedBuilder(): XmlConfig.DefaultBuilder =
        XML.v1.recommendedBuilder()

    @XmlUtilInternal
    override fun fastBuilder(): XmlConfig.DefaultBuilder =
        XML.v1.fastBuilder()

    @Deprecated(
        "Use in-format version",
        ReplaceWith("XML.v1()", "nl.adaptivity.xmlutil.serialization.XML")
    )
    public override fun recommended(): XML = XML.v1()

    @Deprecated("Use in-format version",
        ReplaceWith(
            "XML.v1(configure)",
            "nl.adaptivity.xmlutil.serialization.XML", "nl.adaptivity.xmlutil.serialization.recommended"
        )
    )
    public inline fun recommended(
        dummy: Unit = Unit,
        configure: XmlConfig.DefaultBuilder.() -> Unit
    ): XML = XML.v1(configure)

    @Deprecated("Use in-format version",
        ReplaceWith(
            "XML.v1.fast()",
            "nl.adaptivity.xmlutil.serialization.XML"
        )
    )
    public override fun fast(
    ): XML = XML.v1.fast()

    @Deprecated(
        "Use in-format version",
        ReplaceWith(
            "XML.v1.fast(configure)",
            "nl.adaptivity.xmlutil.serialization.XML", "nl.adaptivity.xmlutil.serialization.fast"
        )
    )
    public inline fun fast(
        dummy: Unit = Unit,
        configure: XmlConfig.DefaultBuilder.() -> Unit
    ): XML = XML.v1.fast(configure)

    @Deprecated(
        "Use in-format version",
        ReplaceWith(
            "XML.v1.fast(serializersModule, configure)",
            "nl.adaptivity.xmlutil.serialization.XML", "nl.adaptivity.xmlutil.serialization.fast"
        )
    )
    public inline fun fast(
        serializersModule: SerializersModule,
        dummy: Unit = Unit,
        configure: XmlConfig.DefaultBuilder.() -> Unit = {}
    ): XML = XML.v1.fast(
        serializersModule,
        configure,
    )
}
