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

@file:Suppress("KDocUnresolvedReference")

package nl.adaptivity.xmlutil.serialization

import kotlinx.io.StringWriter
import kotlinx.serialization.*
import kotlinx.serialization.context.SerialContext
import kotlinx.serialization.context.SerialModule
import kotlinx.serialization.context.getOrDefault
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.multiplatform.name
import nl.adaptivity.xmlutil.util.CompactFragment
import kotlin.reflect.KClass

internal data class NameHolder(val name: QName, val specified: Boolean)

internal class XmlNameMap {
    private val classMap = mutableMapOf<QName, String>()
    private val nameMap = mutableMapOf<String, NameHolder>()

    fun lookupName(kClass: KClass<*>) = lookupName(kClass.name)
    fun lookupClass(name: QName) = classMap[name.copy(prefix = "")]
    fun lookupName(kClass: String) = nameMap[kClass]

    @ImplicitReflectionSerializer
    fun registerClass(kClass: KClass<*>) {
        val serialInfo = kClass.serializer().descriptor
        val serialName = serialInfo.getEntityAnnotations().getXmlSerialName()

        val name: QName
        val specified: Boolean
        if (serialName == null) {
            specified = false
            name = QName(kClass.name.substringAfterLast('.'))
        } else {
            specified = true
            name = serialName
        }
        registerClass(name, kClass.name, specified)
    }

    fun registerClass(name: QName, kClass: String, specified: Boolean) {
        classMap[name.copy(prefix = "")] = kClass
        nameMap[kClass] = NameHolder(name, specified)
    }
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
 * @property context The serialization context used to resolve serializers etc.
 * @property repairNamespaces Option for the serializer whether it should repair namespaces. Does not affect reading
 * @property omitXmlDecl When writing do not emit a `<?xml ... ?>` processing instruction. This is passed to the
 *                       [XmlWriter] constructor
 * @property indent The indentation to use when writing XML
 */
class XML(
    val repairNamespaces: Boolean = true,
    val omitXmlDecl: Boolean = true,
    var indent: Int = 0
         ) : AbstractSerialFormat(), StringFormat {

    init {
        mutableContext.registerSerializer(CompactFragment::class, CompactFragmentSerializer)
    }

    /**
     * Transform the object into an XML String. This is a shortcut for the non-reified version that takes a
     * KClass parameter
     */
    @ImplicitReflectionSerializer
    @Suppress("unused")
    inline fun <reified T : Any> stringify(obj: T, prefix: String? = null): String =
        stringify(context.getOrDefault(T::class), obj, prefix)

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param kClass The type of the object being serialized
     * @param obj The actual object
     * @param prefix The prefix (if any) to use for the namespace
     */
    @ImplicitReflectionSerializer
    fun <T : Any> stringify(
        kClass: KClass<T>,
        obj: T,
        prefix: String? = null
                           ): String {
        return stringify(context.getOrDefault(kClass), obj, prefix)
    }

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param obj The actual object
     * @param saver The serializer/saver to use to write
     * @param prefix The prefix (if any) to use for the namespace
     */
    @Deprecated(
        "Fit within the serialization library, so reorder arguments",
        ReplaceWith("stringify(saver, obj, prefix)")
               )
    fun <T : Any> stringify(obj: T, saver: SerializationStrategy<T>, prefix: String? = null): String =
        stringify(saver, obj, prefix)

    override fun <T> stringify(serializer: SerializationStrategy<T>, obj: T) =
        stringify(serializer, obj, null)

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param obj The actual object
     * @param serializer The serializer/saver to use to write
     * @param prefix The prefix (if any) to use for the namespace
     */
    fun <T> stringify(serializer: SerializationStrategy<T>, obj: T, prefix: String?): String {
        val stringWriter = StringWriter()
        val xmlWriter = XmlStreaming.newWriter(stringWriter, repairNamespaces, omitXmlDecl)

        var ex: Throwable? = null
        try {
            toXml(xmlWriter, serializer, obj, prefix)
        } catch (e: Throwable) {
            ex = e
        } finally {
            try {
                xmlWriter.close()
            } finally {
                ex?.let { throw it }
            }

        }
        return stringWriter.toString()
    }

    @ImplicitReflectionSerializer
    @Deprecated("Replaced by version with consistent parameter order", ReplaceWith("toXml(target, obj, prefix)"))
    inline fun <reified T : Any> toXml(obj: T, target: XmlWriter, prefix: String? = null) =
        toXml(target, obj, prefix)

    /**
     * Write the object to the given writer
     *
     * @param obj The actual object
     * @param target The [XmlWriter] to append the object to
     * @param prefix The prefix (if any) to use for the namespace
     */
    @ImplicitReflectionSerializer
    inline fun <reified T : Any> toXml(target: XmlWriter, obj: T, prefix: String? = null) {
        toXml(target, context.getOrDefault(T::class), obj, prefix)
    }

    @ImplicitReflectionSerializer
    @Deprecated(
        "Replaced by version with consistent parameter order",
        ReplaceWith("toXml(target, kClass, obj, serializer, prefix)")
               )
    fun <T : Any> toXml(
        kClass: KClass<T>,
        obj: T,
        target: XmlWriter,
        prefix: String? = null,
        serializer: SerializationStrategy<T> = context.getOrDefault(kClass)
                       ) =
        toXml(target, serializer, obj, prefix)

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param kClass The type of the object being serialized
     * @param obj The actual object
     * @param target The [XmlWriter] to append the object to
     * @param saver The serializer/saver to use to write
     * @param prefix The prefix (if any) to use for the namespace
     */
    @ImplicitReflectionSerializer
    fun <T : Any> toXml(
        target: XmlWriter,
        kClass: KClass<T>,
        obj: T,
        prefix: String? = null
                       ) {
        toXml(target, context.getOrDefault(kClass), obj, prefix)
    }

    /**
     * Transform into a string. This function is expected to be called indirectly.
     *
     * @param target The [XmlWriter] to append the object to
     * @param obj The actual object
     * @param serializer The serializer/saver to use to write
     * @param prefix The prefix (if any) to use for the namespace
     */
    fun <T> toXml(
        target: XmlWriter,
        serializer: SerializationStrategy<T>,
        obj: T,
        prefix: String? = null
                 ) {
        target.indent = indent

        val serialDescriptor = serializer.descriptor

        val serialName = serializer.descriptor.getSerialName(prefix)
        val encoder = XmlEncoderBase(context, target).XmlEncoder(
            DummyParentDescriptor(serialName, serialDescriptor),
            0,
            serialDescriptor
                                                                )

        serializer.serialize(encoder, obj)
    }

    /**
     * Parse an object of the type [T] out of the reader
     */
    @ImplicitReflectionSerializer
    @Suppress("unused")
    inline fun <reified T : Any> parse(reader: XmlReader) = parse(T::class, reader)

    @ImplicitReflectionSerializer
    @Deprecated("Replaced by version with consistent parameter order", ReplaceWith("parse(reader, kClass, loader)"))
    fun <T : Any> parse(
        kClass: KClass<T>,
        reader: XmlReader,
        loader: DeserializationStrategy<T> = context.getOrDefault(kClass)
                       ): T =
        parse(loader, reader)

    @ImplicitReflectionSerializer
    fun <T : Any> parse(
        kClass: KClass<T>,
        reader: XmlReader
                       ): T =
        parse(context.getOrDefault(kClass), reader)


    /**
     * Parse an object of the type [T] out of the reader. This function is intended mostly to be used indirectly where
     * though the reified function.
     *
     * @param reader An [XmlReader] that contains the XML from which to read the object
     * @param serializer The loader to use to read the object
     */
    fun <T> parse(
        serializer: DeserializationStrategy<T>,
        reader: XmlReader
                 ): T {

        val serialName = serializer.descriptor.getSerialName()
        val serialDescriptor = serializer.descriptor

        val decoder = XmlDecoderBase(context, reader).XmlDecoder(
            DummyParentDescriptor(serialName, serialDescriptor),
            0,
            serialDescriptor
                                                                )

        // We skip all ignorable content here. To get started while supporting direct content we need to put the parser
        // in the correct state of having just read the startTag (that would normally be read by the code that determines
        // what to parse (before calling readSerializableValue on the value)
        reader.skipPreamble()
        return decoder.decodeSerializableValue(serializer)
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
    @ImplicitReflectionSerializer
    fun <T : Any> parse(kClass: KClass<T>, string: String): T {
        return parse(context.getOrDefault(kClass), XmlStreaming.newReader(string))
    }

    override fun <T> parse(serializer: DeserializationStrategy<T>, string: String): T {
        return parse(serializer, XmlStreaming.newReader(string))
    }

    companion object : StringFormat {
        val defaultInstance = XML()
        override val context: SerialContext
            get() = defaultInstance.context

        override fun install(module: SerialModule) {
            defaultInstance.install(module)
        }

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
         * @param obj The object to transform
         * @param serializer The serializer to user
         */
        override fun <T> stringify(
            serializer: SerializationStrategy<T>,
            obj: T
                                  ): String =
            defaultInstance.stringify(serializer, obj)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
         * @param obj The object to transform
         * @param serializer The serializer to user
         * @param prefix The namespace prefix to use
         */
        fun <T> stringify(serializer: SerializationStrategy<T>, obj: T, prefix: String): String =
            defaultInstance.stringify(serializer, obj, prefix)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
         * @param obj The object to transform
         * @param kClass The class where to get the serializer from
         * @param prefix The namespace prefix to use
         */
        @ImplicitReflectionSerializer
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> stringify(obj: T, kClass: KClass<T> = obj::class as KClass<T>, prefix: String? = null): String =
            defaultInstance.stringify(kClass, obj, prefix)

        /**
         * Transform the object into an XML string. This requires the object to be serializable by the kotlin
         * serialization library (either it has a built-in serializer or it is [kotlinx.serialization.Serializable].
         * @param obj The object to transform
         * @param prefix The namespace prefix to use
         */
        @ImplicitReflectionSerializer
        inline fun <reified T : Any> stringify(obj: T, prefix: String? = null): String =
            stringify(obj, T::class, prefix)

        /**
         * Transform into a string. This function is expected to be called indirectly.
         *
         * @param kClass The type of the object being serialized
         * @param obj The actual object
         * @param dest The [XmlWriter] to append the object to
         * @param prefix The prefix (if any) to use for the namespace
         */
        @ImplicitReflectionSerializer
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> toXml(
            dest: XmlWriter,
            obj: T,
            kClass: KClass<T> = obj::class as KClass<T>,
            prefix: String? = null
                           ) =
            defaultInstance.toXml(dest, kClass, obj, prefix)

        /**
         * Write the object to the given writer
         *
         * @param obj The actual object
         * @param dest The [XmlWriter] to append the object to
         * @param prefix The prefix (if any) to use for the namespace
         */
        @ImplicitReflectionSerializer
        @Suppress("unused")
        inline fun <reified T : Any> toXml(dest: XmlWriter, obj: T, prefix: String?) =
            toXml(dest, obj, T::class, prefix)

        /**
         * Parse an object of the type [T] out of the reader
         * @param str The source of the XML events
         * @param kClass The class to parse. Used for class annotations.
         */
        @ImplicitReflectionSerializer
        fun <T : Any> parse(kClass: KClass<T>, str: String): T = XML().parse(kClass, str)

        @Suppress("unused", "UNUSED_PARAMETER")
        @Deprecated(
            "Replaced by version with consistent parameter order",
            ReplaceWith("parse(str, loader)")
                   )
        fun <T : Any> parse(kClass: KClass<T>, str: String, loader: DeserializationStrategy<T>): T =
            parse(loader, str)

        @Deprecated("Use better order", ReplaceWith("parse(kClass, str)"))
        /**
         * Parse an object of the type [T] out of the reader
         * @param str The source of the XML events
         * @param kClass The class to parse. Used for class annotations.
         */
        @ImplicitReflectionSerializer
        fun <T : Any> parse(str: String, kClass: KClass<T>): T =
            parse(kClass, str)

        /**
         * Parse an object of the type [T] out of the reader
         * @param str The source of the XML events
         */
        @ImplicitReflectionSerializer
        inline fun <reified T : Any> parse(str: String): T = parse(T::class, str)

        /**
         * Parse an object of the type [T] out of the reader
         * @param string The source of the XML events
         * @param serializer The loader to use
         */
        @Suppress("unused")
        override fun <T> parse(
            serializer: DeserializationStrategy<T>,
            string: String
                              ): T = XML().parse(serializer, string)

        @Suppress("unused")
        @ImplicitReflectionSerializer
        fun <T : Any> parse(kClass: KClass<T>, reader: XmlReader): T =
            parse(reader, kClass)

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         * @param kClass The class to parse. Used for class annotations.
         */
        @ImplicitReflectionSerializer
        fun <T : Any> parse(reader: XmlReader, kClass: KClass<T>): T =
            defaultInstance.parse(kClass, reader)

        @Deprecated(
            "Replaced by version with consistent parameter order",
            ReplaceWith("parse(reader, kClass, loader)")
                   )
        fun <T : Any> parse(
            @Suppress("UNUSED_PARAMETER") kClass: KClass<T>, reader: XmlReader, loader: DeserializationStrategy<T>
                           ): T =
            parse(reader, loader)

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         * @param loader The loader to use (rather than the default)
         */
        @Suppress("UNUSED_PARAMETER")
        @Deprecated(
            "Use the version that doesn't take a KClass",
            ReplaceWith("parse(reader, loader)", "nl.adaptivity.xmlutil.serialization.XML.Companion.parse")
                   )
        fun <T : Any> parse(reader: XmlReader, kClass: KClass<T>, loader: DeserializationStrategy<T>): T =
            parse(reader, loader)

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         */
        @ImplicitReflectionSerializer
        inline fun <reified T : Any> parse(reader: XmlReader): T = defaultInstance.parse(T::class, reader)

        /**
         * Parse an object of the type [T] out of the reader
         * @param reader The source of the XML events
         * @param loader The loader to use (rather than the default)
         */
        @Suppress("unused")
        fun <T : Any> parse(reader: XmlReader, loader: DeserializationStrategy<T>): T =
            defaultInstance.parse(loader, reader)
    }

    /**
     * An interface that allows custom serializers to special case being serialized to XML and retrieve the underlying
     * [XmlWriter]. This is used for example by [CompactFragment] to make the fragment transparent when serializing to
     * XML.
     */
    interface XmlOutput {
        /**
         * The name for the current tag
         */
        val serialName: QName

        @Deprecated("It is not used or tested. Candidate for removal")
        @Suppress("unused")
                /**
                 * The currently active serialization context
                 */
        val context: SerialContext?
        /**
         * The XmlWriter used. Can be used directly by serializers
         */
        val target: XmlWriter

        @Deprecated("Not used will always return null", ReplaceWith("null"))
        val currentTypeName: Nothing?
            get() = null
    }

    /**
     * An interface that allows custom serializers to special case being serialized to XML and retrieve the underlying
     * [XmlReader]. This is used for example by [CompactFragment] to read arbitrary XML from the stream and store it inside
     * the buffer (without attempting to use the serializer/decoder for it.
     */
    interface XmlInput {
        val input: XmlReader
    }

}

private object DEFAULTSERIALCONTEXT : SerialContext {
    override fun <T : Any> get(kclass: KClass<T>): KSerializer<T>? {
        @Suppress("UNCHECKED_CAST")
        return if (kclass == CompactFragment::class) (CompactFragmentSerializer as KSerializer<T>) else null
    }

    override fun <T : Any> getByValue(value: T): KSerializer<T>? {
        @Suppress("UNCHECKED_CAST")
        return get(value::class) as KSerializer<T>
    }
}

private fun defaultSerialContext(): SerialContext = DEFAULTSERIALCONTEXT

private fun Collection<Annotation>.getXmlSerialName(): QName? {
    val serialName = firstOrNull<XmlSerialName>()
    return when {
        serialName == null -> null
        serialName.namespace == UNSET_ANNOTATION_VALUE
                           -> QName(serialName.value)

        serialName.prefix == UNSET_ANNOTATION_VALUE
                           -> QName(serialName.namespace, serialName.value)

        else               -> QName(serialName.namespace, serialName.value, serialName.prefix)
    }
}

internal fun Collection<Annotation>.getChildName(): QName? {
    val childrenName = firstOrNull<XmlChildrenName>()
    return when {
        childrenName == null -> null
        childrenName.namespace == UNSET_ANNOTATION_VALUE
                             -> QName(childrenName.value)

        childrenName.prefix == UNSET_ANNOTATION_VALUE
                             -> QName(childrenName.namespace, childrenName.value)

        else                 -> QName(
            childrenName.namespace, childrenName.value,
            childrenName.prefix
                                     )
    }
}

internal fun <T : Any> KClass<T>.getSerialName(serializer: KSerializer<*>?, prefix: String?): QName {
    return getSerialName(serializer).let {
        if (prefix == null) it else it.copy(prefix = prefix)
    }
}

internal fun <T : Any> KClass<T>.getSerialName(serializer: SerializationStrategy<*>?): QName {
    return myAnnotations.getXmlSerialName()
        ?: serializer?.run { QName(descriptor.name.substringAfterLast('.')) }
        ?: QName(name.substringAfterLast('.'))
}

internal fun SerialDescriptor.getSerialName(prefix: String? = null): QName {
    return getEntityAnnotations().getXmlSerialName()?.let { if (prefix == null) it else it.copy(prefix) }
        ?: QName(name.substringAfterLast('.'))
}

internal enum class OutputKind { Element, Attribute, Text; }

internal fun XmlSerialName.toQName() = QName(namespace, value, prefix)

internal fun XmlChildrenName.toQName() = QName(namespace, value, prefix)

internal data class PolyInfo(val kClass: String, val tagName: QName, val index: Int)

internal inline fun <reified T> Iterable<*>.firstOrNull(): T? {
    for (e in this) {
        if (e is T) return e
    }
    return null
}


internal fun SerialDescriptor.getValueChild(): Int {
    if (elementsCount == 1) {
        return 0
    } else {
        for (i in 0 until elementsCount) {
            if (getElementAnnotations(i).any { it is XmlValue }) return i
        }
        return CompositeDecoder.UNKNOWN_NAME
    }
}

/** Straightforward copy function */
fun QName.copy(
    namespaceURI: String = this.namespaceURI,
    localPart: String = this.localPart,
    prefix: String = this.prefix
              ) =
    QName(namespaceURI, localPart, prefix)

/** Shortcircuit copy function that creates a new version (if needed) with the new prefix only */
internal fun QName.copy(prefix: String = this.prefix) = when (prefix) {
    this.prefix -> this
    else        -> QName(namespaceURI, localPart, prefix)
}

/**
 * Extension function for writing an object as XML.
 *
 * @param out The writer to use for writing the XML
 * @param serializer The serializer to use. Often `T.Companion.serializer()`
 */
fun <T : Any> T.writeAsXml(out: XmlWriter, serializer: SerializationStrategy<T>) =
    XML.defaultInstance.toXml(out, serializer, this)

/**
 * Extension function that allows any (serializable) object to be written to an XmlWriter.
 */
@ImplicitReflectionSerializer
inline fun <reified T : Any> T.writeAsXML(out: XmlWriter) = XML.toXml(out, this)

/**
 * Extension function that allows any (serializable) object to be written to an XmlWriter. This version takes a [KClass]
 * object rather than having a generic specification.
 */
@ImplicitReflectionSerializer
@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> T.writeAsXML(kClass: KClass<T>, out: XmlWriter) =
    XML.toXml(out, kClass = kClass, obj = this)
