/*
 * Copyright (c) 2024.
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

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VLanguage
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VToken
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchema
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import io.github.pdvrieze.formats.xmlschema.types.VXPathDefaultNamespace
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSD_PREFIX
import nl.adaptivity.xmlutil.serialization.*

@Serializable(XSSchema.Companion::class)
@XmlSerialName("schema", XSD_NS_URI, XSD_PREFIX)
class XSSchema : XSOpenAttrsBase {

    @XmlElement(false)
    val attributeFormDefault: VFormChoice?

    @Serializable(BlockSetSerializer::class)
    val blockDefault: Set<VDerivationControl.T_BlockSetValues>?

    val defaultAttributes: SerializableQName?
    val xpathDefaultNamespace: VXPathDefaultNamespace?
    @XmlElement(false)
    val elementFormDefault: VFormChoice?

    @Serializable(TypeDerivationControlSerializer::class)
    val finalDefault: Set<@Contextual VDerivationControl.Type>?

    @XmlId
    val id: VID?

    @XmlElement(false)
    val targetNamespace: VAnyURI?

    val version: VToken?
    @XmlSerialName("lang", XMLConstants.XML_NS_URI, XMLConstants.XML_NS_PREFIX)
    val lang: VLanguage?

    val includes: List<XSInclude>
    val imports: List<XSImport>
    val redefines: List<XSRedefine>
    val overrides: List<XSOverride>
    val annotations: List<XSAnnotation>
    @XmlAfter("includes", "imports", "redefines", "overrides")
    @XmlBefore("simpleTypes", "complexTypes", "groups", "attributeGroups", "elements", "attributes", "notations")
    val defaultOpenContent: XSDefaultOpenContent?

    val simpleTypes: List<XSGlobalSimpleType>
    val complexTypes: List<XSGlobalComplexType>
    val groups: List<XSGroup>
    val elements: List<XSGlobalElement>
    val attributes: List<XSGlobalAttribute>
    val attributeGroups: List<XSAttributeGroup>
    val notations: List<XSNotation>

    constructor(
        attributeFormDefault: VFormChoice? = null,
        blockDefault: Set<VDerivationControl.T_BlockSetValues>? = null,
        defaultAttributes: SerializableQName? = null,
        xpathDefaultNamespace: VXPathDefaultNamespace? = null,
        elementFormDefault: VFormChoice? = null,
        finalDefault: Set<VDerivationControl.Type>? = null,
        id: VID? = null,
        targetNamespace: VAnyURI? = null,
        version: VToken? = null,
        lang: VLanguage? = null,
        includes: List<XSInclude> = emptyList(),
        imports: List<XSImport> = emptyList(),
        redefines: List<XSRedefine> = emptyList(),
        overrides: List<XSOverride> = emptyList(),
        annotations: List<XSAnnotation> = emptyList(),
        defaultOpenContent: XSDefaultOpenContent? = null,
        simpleTypes: List<XSGlobalSimpleType> = emptyList(),
        complexTypes: List<XSGlobalComplexType> = emptyList(),
        groups: List<XSGroup> = emptyList(),
        elements: List<XSGlobalElement> = emptyList(),
        attributes: List<XSGlobalAttribute> = emptyList(),
        attributeGroups: List<XSAttributeGroup> = emptyList(),
        notations: List<XSNotation> = emptyList(),
        otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
    ) : super(otherAttrs) {
        this.attributeFormDefault = attributeFormDefault
        this.blockDefault = blockDefault
        this.defaultAttributes = defaultAttributes
        this.xpathDefaultNamespace = xpathDefaultNamespace
        this.elementFormDefault = elementFormDefault
        this.finalDefault = finalDefault
        this.id = id
        this.targetNamespace = targetNamespace
        this.version = version
        this.lang = lang
        this.includes = includes
        this.imports = imports
        this.redefines = redefines
        this.overrides = overrides
        this.annotations = annotations
        this.defaultOpenContent = defaultOpenContent
        this.simpleTypes = simpleTypes
        this.complexTypes = complexTypes
        this.groups = groups
        this.elements = elements
        this.attributes = attributes
        this.attributeGroups = attributeGroups
        this.notations = notations
    }

    override fun toString(): String {
        return XML{ autoPolymorphic = true; indent=4 }.encodeToString(serializer(), this)
    }

    fun resolve(resolver: ResolvedSchema.Resolver, defaultVersion: SchemaVersion = SchemaVersion.V1_1): ResolvedSchema =
        ResolvedSchema(this, resolver, defaultVersion)

    fun check(resolver: ResolvedSchema.Resolver, defaultVersion: SchemaVersion = SchemaVersion.V1_1) {
        resolve(resolver, defaultVersion).check()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as XSSchema

        if (attributeFormDefault != other.attributeFormDefault) return false
        if (blockDefault != other.blockDefault) return false
        if (defaultAttributes != other.defaultAttributes) return false
        if (xpathDefaultNamespace != other.xpathDefaultNamespace) return false
        if (elementFormDefault != other.elementFormDefault) return false
        if (finalDefault != other.finalDefault) return false
        if (id != other.id) return false
        if (targetNamespace != other.targetNamespace) return false
        if (version != other.version) return false
        if (lang != other.lang) return false
        if (includes != other.includes) return false
        if (imports != other.imports) return false
        if (redefines != other.redefines) return false
        if (overrides != other.overrides) return false
        if (annotations != other.annotations) return false
        if (defaultOpenContent != other.defaultOpenContent) return false
        if (simpleTypes != other.simpleTypes) return false
        if (complexTypes != other.complexTypes) return false
        if (groups != other.groups) return false
        if (elements != other.elements) return false
        if (attributes != other.attributes) return false
        if (attributeGroups != other.attributeGroups) return false
        if (notations != other.notations) return false
        return otherAttrs == other.otherAttrs
    }

    override fun hashCode(): Int {
        var result = attributeFormDefault?.hashCode() ?: 0
        result = 31 * result + blockDefault.hashCode()
        result = 31 * result + (defaultAttributes?.hashCode() ?: 0)
        result = 31 * result + (xpathDefaultNamespace?.hashCode() ?: 0)
        result = 31 * result + (elementFormDefault?.hashCode() ?: 0)
        result = 31 * result + (finalDefault?.hashCode() ?: 0)
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (targetNamespace?.hashCode() ?: 0)
        result = 31 * result + (version?.hashCode() ?: 0)
        result = 31 * result + (lang?.hashCode() ?: 0)
        result = 31 * result + includes.hashCode()
        result = 31 * result + imports.hashCode()
        result = 31 * result + redefines.hashCode()
        result = 31 * result + overrides.hashCode()
        result = 31 * result + annotations.hashCode()
        result = 31 * result + (defaultOpenContent?.hashCode() ?: 0)
        result = 31 * result + simpleTypes.hashCode()
        result = 31 * result + complexTypes.hashCode()
        result = 31 * result + groups.hashCode()
        result = 31 * result + elements.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + attributeGroups.hashCode()
        result = 31 * result + notations.hashCode()
        result = 31 * result + otherAttrs.hashCode()
        return result
    }

    @XmlSerialName("schema", XSD_NS_URI, XSD_PREFIX)
    @Serializable
    private class SerialDelegate(
        @XmlElement(false)
        val attributeFormDefault: VFormChoice? = null,
        @Serializable(BlockSetSerializer::class)
        val blockDefault: Set<VDerivationControl.T_BlockSetValues>? = emptySet(),
        val defaultAttributes: SerializableQName? = null,
        val xpathDefaultNamespace: VXPathDefaultNamespace? = null,
        @XmlElement(false)
        val elementFormDefault: VFormChoice? = null,
        @Serializable(TypeDerivationControlSerializer::class)
        val finalDefault: Set<@Contextual VDerivationControl.Type>? = null,
        @XmlId
        val id: VID? = null,
        @XmlElement(false)
        val targetNamespace: VAnyURI? = null,
        val version: VToken? = null,
        @XmlSerialName("lang", XMLConstants.XML_NS_URI, XMLConstants.XML_NS_PREFIX)
        val lang: VLanguage? = null,
        val includes: List<XSInclude> = emptyList(),
        val imports: List<XSImport> = emptyList(),
        val redefines: List<XSRedefine> = emptyList(),
        val overrides: List<XSOverride> = emptyList(),
        val annotations: List<XSAnnotation> = emptyList(),
        @XmlAfter("includes", "imports", "redefines", "overrides")
        @XmlBefore("simpleTypes", "complexTypes", "groups", "attributeGroups", "elements", "attributes", "notations")
        val defaultOpenContent: XSDefaultOpenContent? = null,
        val simpleTypes: List<XSGlobalSimpleType> = emptyList(),
        val complexTypes: List<XSGlobalComplexType> = emptyList(),
        val groups: List<XSGroup> = emptyList(),
        val elements: List<XSGlobalElement> = emptyList(),
        val attributes: List<XSGlobalAttribute> = emptyList(),
        val attributeGroups: List<XSAttributeGroup> = emptyList(),
        val notations: List<XSNotation> = emptyList(),
        @XmlOtherAttributes
        val otherAttrs: Map<SerializableQName, String> = emptyMap()
    ) {
        fun toSchema(): XSSchema {
            return XSSchema(
                attributeFormDefault = attributeFormDefault,
                blockDefault = blockDefault,
                defaultAttributes = defaultAttributes,
                xpathDefaultNamespace = xpathDefaultNamespace,
                elementFormDefault = elementFormDefault,
                finalDefault = finalDefault,
                id = id,
                targetNamespace = targetNamespace,
                version = version,
                lang = lang,
                includes = includes,
                imports = imports,
                redefines = redefines,
                overrides = overrides,
                annotations = annotations,
                defaultOpenContent = defaultOpenContent,
                simpleTypes = simpleTypes,
                complexTypes = complexTypes,
                groups = groups,
                elements = elements,
                attributes = attributes,
                attributeGroups = attributeGroups,
                notations = notations,
                otherAttrs = otherAttrs,
            )
        }

        constructor(schema: XSSchema) : this(
            attributeFormDefault = schema.attributeFormDefault,
            blockDefault = schema.blockDefault,
            defaultAttributes = schema.defaultAttributes,
            xpathDefaultNamespace = schema.xpathDefaultNamespace,
            elementFormDefault = schema.elementFormDefault,
            finalDefault = schema.finalDefault,
            id = schema.id,
            targetNamespace = schema.targetNamespace,
            version = schema.version,
            lang = schema.lang,
            includes = schema.includes,
            imports = schema.imports,
            redefines = schema.redefines,
            overrides = schema.overrides,
            annotations = schema.annotations,
            defaultOpenContent = schema.defaultOpenContent,
            simpleTypes = schema.simpleTypes,
            complexTypes = schema.complexTypes,
            groups = schema.groups,
            elements = schema.elements,
            attributes = schema.attributes,
            attributeGroups = schema.attributeGroups,
            notations = schema.notations,
            otherAttrs = schema.otherAttrs,
        )
    }

    /**
     * Helper serializer. Note we only need to special case deserialization. Serialization does
     * not support versions (certainly not at this level).
     */
    internal companion object : KSerializer<XSSchema>, XmlDeserializationStrategy<XSSchema> {
        private val delegate: KSerializer<SerialDelegate> = SerialDelegate.serializer()

        @OptIn(ExperimentalSerializationApi::class)
        override val descriptor: SerialDescriptor = SerialDescriptor(
            "io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema",
            delegate.descriptor
        )

        override fun serialize(encoder: Encoder, value: XSSchema) {
            return delegate.serialize(encoder, SerialDelegate(value))
        }

        override fun deserialize(decoder: Decoder): XSSchema {
            return delegate.deserialize(decoder).toSchema()
        }

        override fun deserializeXML(
            decoder: Decoder,
            input: XmlReader,
            previousValue: XSSchema?,
            isValueChild: Boolean
        ): XSSchema {
            val format = (decoder as XML.XmlInput).delegateFormat()
            return format.decodeFromReader(delegate, VersionFilter(input)).toSchema()
        }
    }
}
