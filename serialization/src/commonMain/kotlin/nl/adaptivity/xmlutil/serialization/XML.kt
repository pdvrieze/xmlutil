/*
 * Copyright (c) 2023.
 *
 * This file is part of xmlutil.
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

@file:Suppress("KDocUnresolvedReference")

package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.capturedKClass
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.core.impl.multiplatform.Language
import nl.adaptivity.xmlutil.core.impl.multiplatform.MpJvmDefaultWithCompatibility
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringWriter
import nl.adaptivity.xmlutil.core.impl.multiplatform.use
import nl.adaptivity.xmlutil.serialization.XML.Companion.encodeToWriter
import nl.adaptivity.xmlutil.serialization.XmlSerializationPolicy.DeclaredNameInfo
import nl.adaptivity.xmlutil.serialization.impl.*
import nl.adaptivity.xmlutil.serialization.structure.*
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

@ExperimentalXmlUtilApi
private val defaultXmlModule = getPlatformDefaultModule() + SerializersModule {
    contextual(QName::class, QNameSerializer)
}

@Suppress("MemberVisibilityCanBePrivate")
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
    public constructor(
        serializersModule: SerializersModule = EmptySerializersModule(),
        configure: XmlConfig.Builder.() -> Unit = {}
    ) : this(XmlConfig.Builder().apply(configure), serializersModule)

    @Suppress("DEPRECATION")
    public fun copy(
        serializersModule: SerializersModule = this.serializersModule,
        configure: XmlConfig.Builder.() -> Unit,
    ): XML = XML(XmlConfig.Builder(config).apply(configure), serializersModule)

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
            config.defaultToGenericParser -> xmlStreaming.newGenericWriter(stringWriter, config.repairNamespaces, config.xmlDeclMode)
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
    public inline fun <reified T : Any> encodeToWriter(target: XmlWriter, value: T, prefix: String?) {
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
                val newConfig = XmlConfig(XmlConfig.Builder(config).apply {
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
                if (!hasSeenDynamicQname && childDescriptor.overriddenSerializer.let { it is XmlSerializationStrategy<*> }) {
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
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, @Language("XML", "", "") string: String): T {
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
     * @param rootName The QName to use for the root tag
     * @param string The string input
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
     * Decode the given string value with the expected root name.
     */
    public inline fun <reified T> decodeFromString(string: String, rootName: QName?): T {
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
     * @param deserializer The loader to use to read the object
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

        val polyInfo: PolyInfo? = if (elementDescriptor is XmlPolymorphicDescriptor) {
            val tagName = reader.name
            val info = elementDescriptor.polyInfo.values.singleOrNull {
                tagName.isEquivalent(it.tagName)
            }
            info?.let { PolyInfo(tagName, 0, it) }
        } else {
            // only check names when not having polymorphic root
            val serialName = rootDescriptor.getElementDescriptor(0).tagName
            if (!serialName.isEquivalent(reader.name)) {
                throw XmlException("Local name \"${reader.name}\" for root tag does not match expected name \"$serialName\"")
            }
            null
        }

        val decoder = xmlDecoderBase.XmlDecoder(elementDescriptor, polyInfo, inheritedPreserveWhitespace = DocumentPreserveSpace.DEFAULT)
        return decoder.decodeSerializableValue(deserializer)
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
    public fun xmlDescriptor(deserializer: KSerializer<*>, rootName: QName? = null): XmlDescriptor {
        return useUnsafeCodecConfig { xmlDescriptor(it, deserializer.descriptor, rootName) }
    }

    private fun xmlDescriptor(
        unsafeCodecConfig: XmlCodecConfig,
        serialDescriptor: SerialDescriptor,
        rootName: QName? = null
    ): XmlRootDescriptor {
        val nameInfo = DeclaredNameInfo(rootName?.localPart ?: serialDescriptor.serialName, rootName, false)

        return XmlRootDescriptor(unsafeCodecConfig, serialDescriptor, nameInfo)
    }

    @Deprecated("Use config directly", ReplaceWith("config.repairNamespaces"), DeprecationLevel.HIDDEN)
    public val repairNamespaces: Boolean
        get() = config.repairNamespaces

    @Suppress("DEPRECATION")
    @Deprecated("Use config directly", ReplaceWith("config.omitXmlDecl"), DeprecationLevel.HIDDEN)
    public val omitXmlDecl: Boolean
        get() = config.omitXmlDecl

    @Suppress("DEPRECATION")
    @Deprecated(
        "Use config directly, consider using indentString",
        ReplaceWith("config.indent"),
        DeprecationLevel.HIDDEN
    )
    public val indent: Int
        get() = config.indent

    @Suppress("DEPRECATION")
    @Deprecated("Use the new configuration system", level = DeprecationLevel.HIDDEN)
    public constructor(
        repairNamespaces: Boolean = true,
        omitXmlDecl: Boolean = true,
        indent: Int = 0,
        serializersModule: SerializersModule = EmptySerializersModule()
    ) : this(XmlConfig(repairNamespaces, omitXmlDecl, indent), serializersModule)

    @Deprecated(
        "This version of the constructor has limits in future compatibility. Use the version that takes a configuration lambda",
    )
    @ExperimentalXmlUtilApi
    public constructor(config: XmlConfig.Builder, serializersModule: SerializersModule = EmptySerializersModule()) :
            this(XmlConfig(config), serializersModule)

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated(
        "This version of the copy function has limits in future compatibility. Use the version that takes a configuration lambda",
        level = DeprecationLevel.ERROR
    )
    @ExperimentalXmlUtilApi
    public fun copy(
        config: XmlConfig = this.config,
        serializersModule: SerializersModule = this.serializersModule
    ): XML = XML(config, serializersModule)

    /**
     * Transform the object into an XML String. This is a shortcut for the non-reified version that takes a
     * KClass parameter
     */
    @Deprecated(
        "Use encodeToString", ReplaceWith(
            "encodeToString(obj, prefix)",
            "nl.adaptivity.xmlutil.serialization.XML.Companion.encodeToString"
        ),
        level = DeprecationLevel.ERROR
    )
    @Suppress("unused")
    public inline fun <reified T : Any> stringify(obj: T, prefix: String? = null): String =
        encodeToString(obj, prefix)

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param obj The actual object
     * @param saver The serializer/saver to use to write
     * @param prefix The prefix (if any) to use for the namespace
     */
    @Deprecated(
        "Fit within the serialization library, so reorder arguments",
        ReplaceWith("stringify(saver, obj, prefix)"),
        level = DeprecationLevel.ERROR
    )
    public fun <T : Any> stringify(obj: T, saver: SerializationStrategy<T>, prefix: String? = null): String =
        encodeToString(saver, obj, prefix)


    @Deprecated("Use encodeToString", ReplaceWith("encodeToString(serializer, value)"), DeprecationLevel.ERROR)
    public fun <T> stringify(serializer: SerializationStrategy<T>, value: T): String =
        encodeToString(serializer, value)

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param obj The actual object
     * @param serializer The serializer/saver to use to write
     * @param prefix The prefix (if any) to use for the namespace
     */
    @Deprecated("Use encodeToString", ReplaceWith("encodeToString(serializer, obj, prefix)"), DeprecationLevel.ERROR)
    public fun <T> stringify(serializer: SerializationStrategy<T>, obj: T, prefix: String?): String =
        encodeToString(serializer, obj, prefix)

    /**
     * Transform onto an existing xml writer.
     *
     * @param target The [XmlWriter] to append the object to
     * @param value The actual object
     * @param serializer The serializer/saver to use to write
     * @param prefix The prefix (if any) to use for the namespace
     */
    @Deprecated(
        "Renamed to encodeToWriter",
        ReplaceWith("encodeToWriter(target, serializer, value, prefix)"),
        DeprecationLevel.ERROR
    )
    public fun <T> toXml(
        target: XmlWriter,
        serializer: SerializationStrategy<T>,
        value: T,
        prefix: String? = null
    ) {
        encodeToWriter(target, serializer, value, prefix)
    }

    /**
     * Write the object to the given writer
     *
     * @param obj The actual object
     * @param target The [XmlWriter] to append the object to
     * @param prefix The prefix (if any) to use for the namespace
     */
    @Deprecated(
        "Use new naming scheme: encodeToWriter",
        ReplaceWith("encodeToWriter(target, obj, prefix)"),
        DeprecationLevel.ERROR
    )
    public inline fun <reified T : Any> toXml(target: XmlWriter, obj: T, prefix: String? = null) {
        encodeToWriter(target, obj, prefix)
    }

    @Deprecated(
        "Replaced by version with consistent parameter order",
        ReplaceWith("toXml(target, obj, prefix)"),
        DeprecationLevel.ERROR
    )
    public inline fun <reified T : Any> toXml(obj: T, target: XmlWriter, prefix: String? = null) {
        encodeToWriter(target, obj, prefix)
    }

    /**
     * Parse an object of the type [T] out of the reader
     */
    @Suppress("unused")
    @Deprecated("Renamed to decodeFromReader", ReplaceWith("decodeFromReader<T>(reader)"), DeprecationLevel.ERROR)
    public inline fun <reified T : Any> parse(reader: XmlReader): T = decodeFromReader<T>(reader)

    /**
     * Parse an object of the type [T] out of the reader. This function is intended mostly to be used indirectly where
     * though the reified function.
     *
     * @param reader An [XmlReader] that contains the XML from which to read the object
     * @param deserializer The loader to use to read the object
     */
    @Deprecated(
        "Renamed to decodeFromReader",
        ReplaceWith("decodeFromReader(deserializer, reader)"),
        DeprecationLevel.ERROR
    )
    public fun <T> parse(deserializer: DeserializationStrategy<T>, reader: XmlReader): T {
        return decodeFromReader(deserializer, reader)
    }

    @Deprecated("Use new function name", ReplaceWith("decodeFromString(deserializer, string)"), DeprecationLevel.ERROR)
    public fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T {
        return decodeFromString(deserializer, string)
    }

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param kClass The type of the object being serialized
     * @param obj The actual object
     * @param prefix The prefix (if any) to use for the namespace
     */
    @Deprecated("Reflection is no longer supported", level = DeprecationLevel.HIDDEN)
    @Suppress("UNUSED_PARAMETER")
    public fun <T : Any> stringify(kClass: KClass<T>, obj: T, prefix: String? = null): String {
        throw UnsupportedOperationException("Not supported by serialization library ")
    }

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param kClass The type of the object being serialized
     * @param obj The actual object
     * @param target The [XmlWriter] to append the object to
     * @param saver The serializer/saver to use to write
     * @param prefix The prefix (if any) to use for the namespace
     */
    @Deprecated("Reflection is no longer supported", level = DeprecationLevel.HIDDEN)
    @Suppress("UNUSED_PARAMETER")
    public fun <T : Any> toXml(target: XmlWriter, kClass: KClass<T>, obj: T, prefix: String? = null) {
        throw UnsupportedOperationException("Reflection no longer works")
    }

    @Deprecated("Reflection is no longer supported", level = DeprecationLevel.HIDDEN)
    @Suppress("UNUSED_PARAMETER")
    public fun <T : Any> parse(kClass: KClass<T>, reader: XmlReader): T {
        throw UnsupportedOperationException("Reflection for serialization is no longer supported")
    }

    /**
     * Parse an object of the type [T] out of the string. It merely creates an xml reader and forwards the request.
     * This function is intended mostly to be used indirectly where
     * though the reified function. The loader defaults to the loader for [kClass]
     *
     * @param kClass The actual class object to parse the object from.
     * @param string The string that contains the XML from which to read the object
     * @param loader The loader to use to read the object
     */
    @Deprecated("Reflection is no longer supported", level = DeprecationLevel.HIDDEN)
    @Suppress("UNUSED_PARAMETER")
    public fun <T : Any> parse(kClass: KClass<T>, string: String): T {
        throw UnsupportedOperationException("Reflection for serialization is no longer supported")
    }

    public companion object : StringFormat {

        public val defaultInstance: XML = XML {
            policy = DefaultXmlSerializationPolicy(try { defaultSharedFormatCache() } catch(e: Error) { FormatCache.Dummy }) {}
        }
        override val serializersModule: SerializersModule
            get() = defaultInstance.serializersModule

        public fun xmlDescriptor(serializer: SerializationStrategy<*>): XmlDescriptor {
            return defaultInstance.xmlDescriptor(serializer)
        }

        public fun xmlDescriptor(deserializer: DeserializationStrategy<*>): XmlDescriptor {
            return defaultInstance.xmlDescriptor(deserializer)
        }

        public fun xmlDescriptor(serializer: KSerializer<*>): XmlDescriptor {
            return defaultInstance.xmlDescriptor(serializer)
        }

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
         * @param value The object to transform
         * @param serializer The serializer to user
         */
        override fun <T> encodeToString(
            serializer: SerializationStrategy<T>,
            value: T
        ): String = defaultInstance.encodeToString(serializer, value)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
         * @param value The object to transform
         * @param serializer The serializer to user
         * @param prefix The namespace prefix to use
         */
        public fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T, prefix: String): String =
            defaultInstance.encodeToString(serializer, value, prefix)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
         * @param value The object to transform
         * @param serializer The serializer to user
         * @param rootName The QName to use for the root tag
         */
        public fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T, rootName: QName): String =
            defaultInstance.encodeToString(serializer, value, rootName)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
         * @param obj The object to transform
         * @param prefix The namespace prefix to use
         */
        public inline fun <reified T : Any> encodeToString(obj: T, prefix: String? = null): String =
            encodeToString(serializer<T>(), obj, prefix ?: "")

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
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
            defaultInstance.encodeToWriter(target, serializer<T>(), value, prefix)
        }

        /**
         * Write the object to the given writer
         *
         * @param target The [XmlWriter] to append the object to
         * @param value The actual object
         * @param rootName The QName to use for the root tag
         */
        public inline fun <reified T : Any> encodeToWriter(target: XmlWriter, value: T, rootName: QName) {
            defaultInstance.encodeToWriter(target, serializer<T>(), value, rootName)
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
            defaultInstance.encodeToWriter(target, serializer, value, prefix)
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
            defaultInstance.encodeToWriter(target, serializer, value, rootName)
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
            return defaultInstance.decodeFromString(deserializer, string)
        }

        /**
         * Parse an object of the type [T] out of the reader
         * @param deserializer The loader to use
         * @param rootName The QName to use for the root tag
         * @param string The source of the XML events
         */
        public fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String, rootName: QName?): T {
            return defaultInstance.decodeFromString(deserializer, string, rootName)
        }

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         * @param rootName The QName to use for the root tag
         */
        @JvmOverloads
        public inline fun <reified T : Any> decodeFromReader(reader: XmlReader, rootName: QName? = null): T =
            defaultInstance.decodeFromReader(reader, rootName)

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
        ): T = defaultInstance.decodeFromReader(deserializer, reader, rootName)

        @Deprecated("Use encodeToString", ReplaceWith("encodeToString(serializer, value)"), DeprecationLevel.ERROR)
        public fun <T> stringify(serializer: SerializationStrategy<T>, value: T): String {
            return encodeToString(serializer, value)
        }

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
         * @param obj The object to transform
         * @param serializer The serializer to user
         * @param prefix The namespace prefix to use
         */
        @Deprecated(
            "Use encodeToString",
            ReplaceWith(
                "encodeToString(serializer, obj, prefix)",
                "nl.adaptivity.xmlutil.serialization.XML.Companion.encodeToString"
            ),
            DeprecationLevel.ERROR
        )
        public fun <T> stringify(serializer: SerializationStrategy<T>, obj: T, prefix: String): String =
            encodeToString(serializer, obj, prefix)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
         * @param obj The object to transform
         * @param prefix The namespace prefix to use
         */
        @Deprecated(
            "Use encodeToString",
            ReplaceWith(
                "encodeToString(obj, prefix ?: \"\")",
                "nl.adaptivity.xmlutil.serialization.XML.Companion.encodeToString"
            ),
            DeprecationLevel.ERROR
        )
        public inline fun <reified T : Any> stringify(obj: T, prefix: String? = null): String =
            encodeToString(obj, prefix ?: "")

        /**
         * Write the object to the given writer
         *
         * @param obj The actual object
         * @param dest The [XmlWriter] to append the object to
         * @param prefix The prefix (if any) to use for the namespace
         */
        @Suppress("unused")
        @Deprecated(
            "Renamed to encodeToWriter",
            ReplaceWith(
                "encodeToWriter(dest, obj, prefix)",
                "nl.adaptivity.xmlutil.serialization.XML.Companion.encodeToWriter"
            ),
            DeprecationLevel.ERROR
        )
        public inline fun <reified T : Any> toXml(dest: XmlWriter, obj: T, prefix: String? = null) {
            encodeToWriter(dest, obj, prefix)
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
            "Renamed to encodeToWriter",
            ReplaceWith(
                "encodeToWriter(target, serializer, value, prefix)",
                "nl.adaptivity.xmlutil.serialization.XML.Companion.encodeToWriter"
            ),
            DeprecationLevel.ERROR
        )
        public fun <T> toXml(
            target: XmlWriter,
            serializer: SerializationStrategy<T>,
            value: T,
            prefix: String? = null
        ) {
            encodeToWriter(target, serializer, value, prefix)
        }

        /**
         * Parse an object of the type [T] out of the reader
         * @param str The source of the XML events
         */
        @Deprecated(
            "Use decodeFromString",
            ReplaceWith("decodeFromString(str)", "nl.adaptivity.xmlutil.serialization.XML.Companion.decodeFromString"),
            DeprecationLevel.ERROR
        )
        public inline fun <reified T : Any> parse(str: String): T = decodeFromString(str)

        /**
         * Parse an object of the type [T] out of the reader
         * @param string The source of the XML events
         * @param deserializer The loader to use
         */
        @Suppress("unused")
        @Deprecated("Use new name", ReplaceWith("decodeFromString(deserializer, string)"), DeprecationLevel.ERROR)
        public fun <T> parse(
            deserializer: DeserializationStrategy<T>,
            string: String
        ): T = decodeFromString(deserializer, string)

        @Deprecated(
            "Replaced by version with consistent parameter order",
            ReplaceWith("parse(reader, kClass, loader)"),
            DeprecationLevel.HIDDEN
        )
        public fun <T : Any> parse(
            @Suppress("UNUSED_PARAMETER") kClass: KClass<T>, reader: XmlReader, loader: DeserializationStrategy<T>
        ): T = decodeFromReader(loader, reader)

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         * @param loader The loader to use (rather than the default)
         */
        @Suppress("UNUSED_PARAMETER")
        @Deprecated(
            "Use the version that doesn't take a KClass",
            ReplaceWith("parse(reader, loader)", "nl.adaptivity.xmlutil.serialization.XML.Companion.parse"),
            DeprecationLevel.HIDDEN
        )
        public fun <T : Any> parse(reader: XmlReader, kClass: KClass<T>, loader: DeserializationStrategy<T>): T =
            decodeFromReader(loader, reader)

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         */
        @Deprecated(
            "Renamed to decodeFromReader", ReplaceWith(
                "decodeFromReader(reader)",
                "nl.adaptivity.xmlutil.serialization.XML.Companion.decodeFromReader"
            ),
            DeprecationLevel.HIDDEN
        )
        public inline fun <reified T : Any> parse(reader: XmlReader): T = decodeFromReader(reader)

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         * @param loader The loader to use (rather than the default)
         */
        @Suppress("unused")
        @Deprecated(
            "Renamed to decodeFromReader", ReplaceWith(
                "decodeFromReader(reader, loader)",
                "nl.adaptivity.xmlutil.serialization.XML.Companion.decodeFromReader"
            ),
            DeprecationLevel.HIDDEN
        )
        public fun <T : Any> parse(reader: XmlReader, loader: DeserializationStrategy<T>): T =
            decodeFromReader(loader, reader)
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

        @WillBePrivate
        @Deprecated("Not used will always return null", ReplaceWith("null"), DeprecationLevel.HIDDEN)
        public val currentTypeName: Nothing?
            get() = null
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

public fun XmlSerialName.toQName(serialName: String, parentNamespace: Namespace?): QName = when {
    namespace == UNSET_ANNOTATION_VALUE -> when (value) {
        UNSET_ANNOTATION_VALUE -> parentNamespace?.let { QName(it.namespaceURI, serialName) } ?: QName(serialName)
        else -> parentNamespace?.let { QName(it.namespaceURI, value) } ?: QName(value)
    }

    value == UNSET_ANNOTATION_VALUE -> when (prefix) {
        UNSET_ANNOTATION_VALUE -> QName(namespace, serialName)
        else -> QName(serialName, namespace, prefix)
    }

    prefix == UNSET_ANNOTATION_VALUE -> QName(namespace, value)

    else -> QName(namespace, value, prefix)
}

public fun XmlChildrenName.toQName(): QName {
    return toQName(null)
}

internal fun XmlChildrenName.toQName(parentNamespace: Namespace?): QName = when {
    namespace == UNSET_ANNOTATION_VALUE -> parentNamespace?.let { QName(it.namespaceURI, value) } ?: QName(value)
    prefix == UNSET_ANNOTATION_VALUE -> {
        val p = parentNamespace?.let { ns -> ns.prefix.takeIf { ns.namespaceURI == namespace }}
        QName(namespace, value, p ?: "")
    }
    else -> QName(namespace, value, prefix)
}

internal fun XmlKeyName.toQName(parentNamespace: Namespace?): QName = when {
    namespace == UNSET_ANNOTATION_VALUE -> parentNamespace?.let { QName(it.namespaceURI, value) } ?: QName(value)
    prefix == UNSET_ANNOTATION_VALUE -> {
        val p = parentNamespace?.let { ns -> ns.prefix.takeIf { ns.namespaceURI == namespace }}
        QName(namespace, value, p ?: "")
    }
    else -> QName(namespace, value, prefix)
}

internal fun XmlMapEntryName.toQName(parentNamespace: Namespace?): QName = when {
    namespace == UNSET_ANNOTATION_VALUE -> parentNamespace?.let { QName(it.namespaceURI, value) } ?: QName(value)
    prefix == UNSET_ANNOTATION_VALUE -> {
        val p = parentNamespace?.let { ns -> ns.prefix.takeIf { ns.namespaceURI == namespace }}
        QName(namespace, value, p ?: "")
    }
    else -> QName(namespace, value, prefix)
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

/**
 * Extension function for writing an object as XML.
 *
 * @param out The writer to use for writing the XML
 * @param serializer The serializer to use. Often `T.Companion.serializer()`
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Use the XML object that allows configuration", level = DeprecationLevel.ERROR)
public fun <T : Any> T.writeAsXml(out: XmlWriter, serializer: SerializationStrategy<T>) {
    XML.defaultInstance.encodeToWriter(out, serializer, this)
}

/**
 * Extension function that allows any (serializable) object to be written to an XmlWriter.
 */
@Deprecated(
    "Use the XML object that allows configuration",
    ReplaceWith("XML.toXml(out, this)"),
    level = DeprecationLevel.ERROR
)
public inline fun <reified T : Any> T.writeAsXML(out: XmlWriter) {
    encodeToWriter(out, this)
}

@RequiresOptIn("This function will become private in the future", RequiresOptIn.Level.WARNING)
public annotation class WillBePrivate
