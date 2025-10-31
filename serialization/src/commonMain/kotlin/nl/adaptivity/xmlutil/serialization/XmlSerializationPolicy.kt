/*
 * Copyright (c) 2020-2025.
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
package nl.adaptivity.xmlutil.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.serialization.structure.SafeParentInfo
import nl.adaptivity.xmlutil.serialization.structure.TypePreserveSpace
import nl.adaptivity.xmlutil.serialization.structure.XmlDescriptor
import nl.adaptivity.xmlutil.serialization.structure.XmlOrderConstraint

/**
 * Policies allow for customizing the behaviour of the xml serialization
 */
@OptIn(ExperimentalSubclassOptIn::class)
@SubclassOptInRequired(ExperimentalXmlUtilApi::class)
public interface XmlSerializationPolicy {

    /**
     * The default output kind used for (effective) primitives (inline values are elided). By default
     * this is as attribute.
     */
    public val defaultPrimitiveOutputKind: OutputKind get() = OutputKind.Attribute

    /**
     * The default output kind used for objects (including `Unit`). By default this is as
     * element. Note that objects by default do not have elements.
     */
    public val defaultObjectOutputKind: OutputKind get() = OutputKind.Element

    public val isStrictAttributeNames: Boolean

    public val isStrictBoolean: Boolean get() = false

    /**
     * Serialize float/double data according to the requirements of XML, rather than the
     * toFloat/Float.toString/toDouble/Double.toString function from the Kotlin standard library.
     */
    public val isXmlFloat: Boolean get() = false
    public val isStrictOtherAttributes: Boolean get() = false

    @ExperimentalXmlUtilApi
    public val verifyElementOrder: Boolean get() = false

    @OptIn(ExperimentalSerializationApi::class)
    @ExperimentalXmlUtilApi
    public fun defaultOutputKind(serialKind: SerialKind): OutputKind =
        when (serialKind) {
            SerialKind.ENUM,
            StructureKind.OBJECT -> defaultObjectOutputKind

            is PrimitiveKind -> defaultPrimitiveOutputKind

            PolymorphicKind.OPEN -> OutputKind.Element

            else -> OutputKind.Element
        }

    public fun invalidOutputKind(message: String): Unit = ignoredSerialInfo(message)

    public fun ignoredSerialInfo(message: String)

    public fun effectiveName(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        outputKind: OutputKind,
        useName: DeclaredNameInfo = tagParent.elementUseNameInfo
    ): QName

    public fun isListEluded(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
    ): Boolean

    public fun isTransparentPolymorphic(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo
    ): Boolean

    public fun polymorphicDiscriminatorName(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): QName?

    @Suppress("DEPRECATION")
    public fun serialTypeNameToQName(
        typeNameInfo: DeclaredNameInfo,
        parentNamespace: Namespace
    ): QName

    @Suppress("DEPRECATION")
    public fun serialUseNameToQName(
        useNameInfo: DeclaredNameInfo,
        parentNamespace: Namespace
    ): QName

    /**
     * Class holding the name information for either an attribute or type
     *
     * @property serialName The serialName as provided by the descriptor (element name for attribute,
     *     type name for type)
     * @property annotatedName The name provided through the `@XmlSerialName` annotation. The default
     *     policy always prioritises this over local names.
     * @property isDefaultNamespace For attribute values, determines whether the attribute would be
     *     in the default namespace. This allows for `@XmlSerialName` annotations that do not explicitly
     *     specify the name. It records whether the namespace attribute is the "unset"/default value in
     *     the annotation.
     */
    public data class DeclaredNameInfo(
        val serialName: String,
        val annotatedName: QName?,
        val isDefaultNamespace: Boolean/* = false*/
    ) {
        internal constructor(serialName: String) : this(serialName, null, false)
        internal constructor(name: QName): this(name.localPart, name, false)

        @OptIn(ExperimentalSerializationApi::class)
        internal constructor(descriptor: SerialDescriptor) : this(descriptor.serialName, (descriptor as? XmlSerialDescriptor)?.serialQName, false)

        init {
            check(!(isDefaultNamespace && annotatedName == null)) { "Default namespace requires there to be an annotated name" }
        }
    }

    public data class ActualNameInfo(
        val serialName: String,
        val annotatedName: QName
    )

    public fun effectiveOutputKind(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean
    ): OutputKind

    public fun overrideSerializerOrNull(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): KSerializer<*>? {
        return null
    }

    /**
     * Allows for recovering from unknown content. The implementation must either throw an exception
     * or consume the content (parse it completely). It can allow recovery by returning a list
     * of data that should be recovered (this can allow further parsing of the data or return final
     * values).
     */
    @ExperimentalXmlUtilApi
    public fun handleUnknownContentRecovering(
        input: XmlReader,
        inputKind: InputKind,
        descriptor: XmlDescriptor,
        name: QName?,
        candidates: Collection<Any>
    ): List<XML.ParsedData<*>>

    public fun onElementRepeated(parentDescriptor: XmlDescriptor, childIndex: Int) {}

    public fun handleAttributeOrderConflict(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        outputKind: OutputKind
    ): OutputKind {
        throw SerializationException("Node ${serializerParent.elementUseNameInfo.serialName} wants to be an attribute but cannot due to ordering constraints")
    }

    public fun shouldEncodeElementDefault(elementDescriptor: XmlDescriptor?): Boolean

    /**
     * Allow modifying the ordering of children.
     */
    public fun initialChildReorderMap(
        parentDescriptor: SerialDescriptor
    ): Collection<XmlOrderConstraint>? = null

    public fun updateReorderMap(
        original: Collection<XmlOrderConstraint>,
        children: List<XmlDescriptor>
    ): Collection<XmlOrderConstraint> = original

    @OptIn(ExperimentalSerializationApi::class)
    public fun enumEncoding(enumDescriptor: SerialDescriptor, index: Int): String {
        return enumDescriptor.getElementName(index)
    }

    @ExperimentalXmlUtilApi
    public fun preserveSpace(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): TypePreserveSpace {
        return tagParent.descriptor?.defaultPreserveSpace ?: TypePreserveSpace.DEFAULT
    }

    /** Determine the name of map keys for a given map type */
    public fun mapKeyName(serializerParent: SafeParentInfo): DeclaredNameInfo =
        DeclaredNameInfo("key") // minimal default for implementations.

    /**
     * Determine the name of the values for a given map type
     */
    public fun mapValueName(serializerParent: SafeParentInfo, isListEluded: Boolean): DeclaredNameInfo =
        DeclaredNameInfo("value") // minimal default for implementations.

    /**
     * Determine the name to use for the map element (only used when a map entry is wrapped)
     */
    public fun mapEntryName(serializerParent: SafeParentInfo, isListEluded: Boolean): QName =
        QName(serializerParent.namespace.namespaceURI, "entry") // minimal default for implementations.

    /**
     * Determine whether the key attribute should be collapsed into the value tag rather
     * than the value being nested in a container for the element.
     */
    public fun isMapValueCollapsed(mapParent: SafeParentInfo, valueDescriptor: XmlDescriptor): Boolean = false

    /**
     * Determine namespace prefixes to make sure are set upon the tag.
     */
    @ExperimentalXmlUtilApi
    public fun elementNamespaceDecls(serializerParent: SafeParentInfo): List<Namespace> = emptyList()

    /**
     * Determine the delimiters to use to separate attributes. When writing will always write the
     * first element.
     */
    @ExperimentalXmlUtilApi
    public fun attributeListDelimiters(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Array<String> =
        arrayOf(" ", "\n", "\t", "\r")

    /**
     * Determine the delimiters to use to separate primitive/textual list elements when use inside an element.
     * When writing will always write the first element.
     */
    @ExperimentalXmlUtilApi
    public fun textListDelimiters(serializerParent: SafeParentInfo, tagParent: SafeParentInfo): Array<String> =
        attributeListDelimiters(serializerParent, tagParent)

    public enum class XmlEncodeDefault {
        ALWAYS, ANNOTATED, NEVER
    }

    public companion object {

        /**
         * Helper function that allows more flexibility on null namespace use. If either the found
         * name has the null namespace, or the candidate has null namespace, this will map (for the
         * correct child).
         */
        @ExperimentalXmlUtilApi
        public fun recoverNullNamespaceUse(
            inputKind: InputKind,
            descriptor: XmlDescriptor,
            name: QName?
        ): List<XML.ParsedData<*>>? {
            if (name != null) {
                if (name.namespaceURI == "") {
                    for (idx in 0 until descriptor.elementsCount) {
                        val candidate = descriptor.getElementDescriptor(idx)
                        if (inputKind.mapsTo(candidate.effectiveOutputKind) &&
                            candidate.tagName.localPart == name.getLocalPart()
                        ) {
                            return listOf(XML.ParsedData(idx, Unit, true))
                        }
                    }
                } else {
                    for (idx in 0 until descriptor.elementsCount) {
                        val candidate = descriptor.getElementDescriptor(idx)
                        if (inputKind.mapsTo(candidate.effectiveOutputKind) &&
                            candidate.tagName.isEquivalent(QName(name.localPart))
                        ) {
                            return listOf(XML.ParsedData(idx, Unit, true))
                        }
                    }
                }
            }
            return null
        }

    }

}

public fun XmlSerializationPolicy.typeQName(xmlDescriptor: XmlDescriptor): QName {
    return xmlDescriptor.typeDescriptor.typeQname
        ?: serialTypeNameToQName(xmlDescriptor.typeDescriptor.typeNameInfo, xmlDescriptor.tagParent.namespace)
}

