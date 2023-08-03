/*
 * Copyright (c) 2021.
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

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VLanguage
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VToken
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchema
import io.github.pdvrieze.formats.xmlschema.types.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.serialization.*

@Serializable
@XmlSerialName("schema", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
class XSSchema : XSOpenAttrsBase {

    @XmlElement(false)
    val attributeFormDefault: VFormChoice?

    @Serializable(AllDerivationSerializer::class)
    val blockDefault: Set<T_BlockSetValues>?

    val defaultAttributes: SerializableQName?
    val xpathDefaultNamespace: VXPathDefaultNamespace?
    @XmlElement(false)
    val elementFormDefault: VFormChoice?

    @Serializable(AllDerivationSerializer::class)
    val finalDefault: Set<@Contextual VDerivationControl.Type>?

    @XmlId
    val id: VID?

    @XmlElement(false)
    val targetNamespace: VAnyURI?

    val version: VToken?
    @XmlSerialName("lang", XmlSchemaConstants.XML_NAMESPACE, XmlSchemaConstants.XML_PREFIX)
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
        blockDefault: VBlockSet = emptySet(),
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

    fun resolve(resolver: ResolvedSchema.Resolver): ResolvedSchema = ResolvedSchema(this, resolver)

    fun check(resolver: ResolvedSchema.Resolver) {
        resolve(resolver).check()
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
}
