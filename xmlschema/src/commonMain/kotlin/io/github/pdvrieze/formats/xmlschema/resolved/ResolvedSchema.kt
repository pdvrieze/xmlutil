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

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.namespaceURI

// TODO("Support resolving documents that are external to the original/have some resolver type")
class ResolvedSchema(val rawPart: XSSchema, private val resolver: Resolver) : ResolvedSchemaLike() {
    override fun check() {
        super.check()

        for (rd in redefines) {
            rd.check()
        }
    }

    val redefines: List<ResolvedRedefine> = emptyList()// DelegateList(rawPart.redefines) { ResolvedRedefine(it, this, resolver) }

    val includes: List<ResolvedInclude> = emptyList()// DelegateList(rawPart.includes) { ResolvedInclude(it, this, resolver)}

    override val simpleTypes: List<ResolvedGlobalSimpleType>

    override val complexTypes: List<ResolvedGlobalComplexType>

    override val elements: List<ResolvedGlobalElement>

    override val groups: List<ResolvedToplevelGroup>

    override val attributes: List<ResolvedGlobalAttribute>

    override val attributeGroups: List<ResolvedToplevelAttributeGroup>

    init {
        val collatedSchema = CollatedSchema(rawPart, resolver, this)

        simpleTypes = DelegateList(collatedSchema.simpleTypes.values.toList()) { (schema, v) -> ResolvedGlobalSimpleType(v, schema) }

        complexTypes = DelegateList(collatedSchema.complexTypes.values.toList()) { (schema, v) -> ResolvedGlobalComplexType(v, schema) }

        elements = DelegateList(CombiningList(collatedSchema.elements.values.toList())) { (schema, v) -> ResolvedGlobalElement(v, schema) }

        groups = DelegateList(collatedSchema.groups.values.toList()) { (schema, v) -> ResolvedToplevelGroup(v, schema) }

        attributes = DelegateList(collatedSchema.attributes.values.toList()) { (schema, v) -> ResolvedGlobalAttribute(v, schema) }

        attributeGroups = DelegateList(CombiningList(collatedSchema.attributeGroups.values.toList())) { (schema, v) -> ResolvedToplevelAttributeGroup(v, schema) }
    }

    val annotations: List<XSAnnotation> get() = rawPart.annotations

    val types: List<ResolvedGlobalType> get() = CombiningList(simpleTypes, complexTypes)

    val notations: List<XSNotation> get() = rawPart.notations
    val identityConstraints: List<T_IdentityConstraint> get() = TODO("Delegate list of identity constraints")

    override val defaultOpenContent: XSDefaultOpenContent?
        get() = rawPart.defaultOpenContent

    val attributeFormDefault: T_FormChoice
        get() = rawPart.attributeFormDefault ?: T_FormChoice.UNQUALIFIED

    override val blockDefault: T_BlockSet get() = rawPart.blockDefault

    val defaultAttributes: QName? get() = rawPart.defaultAttributes

    val xPathDefaultNamespace: T_XPathDefaultNamespace
        get() = rawPart.xpathDefaultNamespace ?: T_XPathDefaultNamespace.LOCAL

    val elementFormDefault: T_FormChoice
        get() = rawPart.elementFormDefault ?: T_FormChoice.UNQUALIFIED

    override val finalDefault: Set<TypeModel.Derivation>
        get() = rawPart.finalDefault ?: emptySet()

    val id: VID? get() = rawPart.id

    override val targetNamespace: VAnyURI get() = rawPart.targetNamespace ?: VAnyURI("")

    val version: VToken? get() = rawPart.version

    val lang: VLanguage? get() = rawPart.lang


    override fun maybeSimpleType(typeName: QName): ResolvedGlobalSimpleType? {
        for (include in includes) {
            if ((include.targetNamespace?.value ?: "") == typeName.namespaceURI) {
                include.maybeSimpleType(typeName)?.let { return it }
            }
        }
        for (redefine in redefines) {
            redefine.maybeSimpleType(typeName)?.let { return it }
        }

        return super.maybeSimpleType(typeName)
    }

    override fun maybeType(typeName: QName): ResolvedGlobalType? {
        for (include in includes) {
            if ((include.targetNamespace?.value ?: "") == typeName.namespaceURI) {
                include.maybeType(typeName)?.let { return it }
            }
        }
        for (redefine in redefines) {
            redefine.maybeType(typeName)?.let { return it }
        }

        return super.maybeType(typeName)
    }

    override fun maybeAttributeGroup(attributeGroupName: QName): ResolvedToplevelAttributeGroup? {
        for (include in includes) {
            if ((include.targetNamespace?.value ?: "") == attributeGroupName.namespaceURI) {
                include.maybeAttributeGroup(attributeGroupName)?.let { return it }
            }
        }
        for (redefine in redefines) {
            redefine.maybeAttributeGroup(attributeGroupName)?.let { return it }
        }


        return super.maybeAttributeGroup(attributeGroupName)
    }

    override fun maybeGroup(groupName: QName): ResolvedToplevelGroup? {
        for (include in includes) {
            if ((include.targetNamespace?.value ?: "") == groupName.namespaceURI) {
                include.maybeGroup(groupName)?.let { return it }
            }
        }
        for (redefine in redefines) {
            redefine.maybeGroup(groupName)?.let { return it }
        }

        return super.maybeGroup(groupName)
    }

    override fun maybeElement(elementName: QName): ResolvedGlobalElement? {
        for (include in includes) {
            if ((include.targetNamespace?.value ?: "") == elementName.namespaceURI) {
                include.maybeElement(elementName)?.let { return it }
            }
        }
        for (redefine in redefines) {
            redefine.maybeElement(elementName)?.let { return it }
        }

        return super.maybeElement(elementName)
    }

    interface Resolver {
        val baseUri: VAnyURI

        fun readSchema(schemaLocation: VAnyURI): XSSchema

        /**
         * Create a delegate resolver for the schema
         */
        fun delegate(schemaLocation: VAnyURI): Resolver

        fun resolve(relativeUri: VAnyURI): VAnyURI
    }
}

