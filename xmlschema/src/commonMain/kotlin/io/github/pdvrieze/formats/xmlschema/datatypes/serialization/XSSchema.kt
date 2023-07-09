/*
 * Copyright (c) 2021.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package io.github.pdvrieze.formats.xmlschema.datatypes.serialization

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VLanguage
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VToken
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchema
import io.github.pdvrieze.formats.xmlschema.types.T_BlockSet
import io.github.pdvrieze.formats.xmlschema.types.T_FormChoice
import io.github.pdvrieze.formats.xmlschema.types.T_XPathDefaultNamespace
import io.github.pdvrieze.formats.xmlschema.types.XSI_OpenAttrs
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.QNameSerializer
import nl.adaptivity.xmlutil.serialization.*

@Serializable
@XmlSerialName("schema", XmlSchemaConstants.XS_NAMESPACE, XmlSchemaConstants.XS_PREFIX)
data class XSSchema(
    @XmlElement(false)
    val attributeFormDefault: T_FormChoice? = null,

    @Serializable(AllDerivationSerializer::class)
    val blockDefault: T_BlockSet = emptySet(),

    @Serializable(QNameSerializer::class)
    val defaultAttributes: QName? = null,

    val xpathDefaultNamespace: T_XPathDefaultNamespace? = null,

    @XmlElement(false)
    val elementFormDefault: T_FormChoice? = null,

    @Serializable(AllDerivationSerializer::class)
    val finalDefault: Set<TypeModel.Derivation>? = null,

    val id: VID? = null,

    @XmlElement(false)
    val targetNamespace: VAnyURI? = null,

    val version: VToken? = null,

    @XmlSerialName("lang", XmlSchemaConstants.XML_NAMESPACE, XmlSchemaConstants.XML_PREFIX)
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
    val elements: List<XSElement> = emptyList(),
    val attributes: List<XSGlobalAttribute> = emptyList(),
    val attributeGroups: List<XSAttributeGroup> = emptyList(),
    val notations: List<XSNotation> = emptyList(),
    @XmlOtherAttributes
    override val otherAttrs: Map<@Serializable(QNameSerializer::class) QName, String> = emptyMap()
) : XSI_OpenAttrs {

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
