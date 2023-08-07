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

package io.github.pdvrieze.formats.xmlschema.resolved

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VLanguage
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VToken
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSDefaultOpenContent
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSSchema
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.VBlockSet
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import io.github.pdvrieze.formats.xmlschema.types.VXPathDefaultNamespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI

// TODO("Support resolving documents that are external to the original/have some resolver type")
class ResolvedSchema(val rawPart: XSSchema, resolver: Resolver) : ResolvedSchemaLike() {

    private val nestedData: MutableMap<String, SchemaElementResolver> = mutableMapOf()

    init {
        val collatedSchema = CollatedSchema(rawPart, resolver, schemaLike = this)
        nestedData[targetNamespace.value] = NestedData(targetNamespace, collatedSchema)
        // Use getOrPut to ensure uniqueness
        nestedData.getOrPut(BuiltinSchemaXmlschema.targetNamespace.value) { BuiltinSchemaXmlschema.resolver }
        nestedData.getOrPut(BuiltinSchemaXmlInstance.targetNamespace.value) { BuiltinSchemaXmlInstance.resolver }
        if (rawPart.targetNamespace?.value != XMLConstants.XML_NS_URI &&
            XMLConstants.XML_NS_URI in collatedSchema.importedNamespaces &&
            !collatedSchema.importedSchemas.containsKey(XMLConstants.XML_NS_URI)
        ) {
            nestedData[XMLConstants.XML_NS_URI] = BuiltinSchemaXml.resolver
        }

        for ((importNS, importCollation) in collatedSchema.importedSchemas) {
            nestedData[importNS] = NestedData(VAnyURI(importNS), importCollation)
        }

    }

    val annotations: List<XSAnnotation> get() = rawPart.annotations

    override val defaultOpenContent: XSDefaultOpenContent?
        get() = rawPart.defaultOpenContent

    override val attributeFormDefault: VFormChoice
        get() = rawPart.attributeFormDefault ?: super.attributeFormDefault

    override val blockDefault: VBlockSet get() = rawPart.blockDefault ?: emptySet()

    val defaultAttributes: QName? get() = rawPart.defaultAttributes

    val xPathDefaultNamespace: VXPathDefaultNamespace
        get() = rawPart.xpathDefaultNamespace ?: VXPathDefaultNamespace.LOCAL

    override val elementFormDefault: VFormChoice
        get() = rawPart.elementFormDefault ?: super.elementFormDefault

    override val finalDefault: Set<VDerivationControl.Type>
        get() = rawPart.finalDefault ?: emptySet()

    val id: VID? get() = rawPart.id

    override val targetNamespace: VAnyURI get() = rawPart.targetNamespace ?: VAnyURI("")

    val version: VToken? get() = rawPart.version

    val lang: VLanguage? get() = rawPart.lang

    private inline fun <R> withQName(name: QName, action: SchemaElementResolver.(String) -> R): R {
        val data = nestedData[name.namespaceURI]
            ?: throw NoSuchElementException("The namespace ${name.namespaceURI} is not available")
        return data.action(name.localPart)
    }

    override fun maybeSimpleType(typeName: QName): ResolvedGlobalSimpleType? = withQName(typeName) {
        maybeSimpleType(it)
    }

    override fun maybeType(typeName: QName): ResolvedGlobalType? = withQName(typeName) {
        maybeType(it)
    }

    override fun maybeAttributeGroup(attributeGroupName: QName): ResolvedGlobalAttributeGroup? =
        withQName(attributeGroupName) {
            maybeAttributeGroup(it)
        }

    override fun maybeGroup(groupName: QName): ResolvedGlobalGroup? = withQName(groupName) {
        maybeGroup(it)
    }

    override fun maybeElement(elementName: QName): ResolvedGlobalElement? = withQName(elementName) {
        maybeElement(it)
    }

    override fun maybeAttribute(attributeName: QName): ResolvedGlobalAttribute? = withQName(attributeName) {
        maybeAttribute(it)
    }

    override fun maybeIdentityConstraint(constraintName: QName): ResolvedIdentityConstraint? =
        withQName(constraintName) {
            maybeIdentityConstraint(it)
        }

    override fun maybeNotation(notationName: QName): ResolvedNotation? = withQName(notationName) {
        maybeNotation(it)
    }

    override fun substitutionGroupMembers(headName: QName): Set<ResolvedGlobalElement> = withQName(headName) {
        substitutionGroupMembers(it)
    }

    fun check() {
        val checkHelper = CheckHelper(this)

        val icNames = HashSet<QName>()
        for (data in nestedData.values) {
            if (data is NestedData) {
                for (e in data.elements.values) {
                    checkHelper.checkElement(e)
                }
                for (a in data.attributes.values) {
                    checkHelper.checkAttribute(a)
                }
                for (t in data.simpleTypes.values) {
                    checkHelper.checkType(t)
                }
                for (t in data.complexTypes.values) {
                    checkHelper.checkType(t)
                }
                for (g in data.groups.values) {
                    checkHelper.checkGroup(g)
                }
                for (ag in data.attributeGroups.values) {
                    checkHelper.checkAttributeGroup(ag)
                }
                for (ic in data.identityConstraints.values) {
                    val name = ic.mdlQName?.let { QName(it.namespaceURI, it.localPart) }
                    if (name != null) {
                        check(icNames.add(name)) {
                            "Duplicate identity constraint ${ic.mdlQName}"
                        }
                    }
                    checkHelper.checkConstraint(ic)
                }
            }
        }

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

    object DummyResolver : Resolver {
        override val baseUri: VAnyURI = VAnyURI("")

        override fun readSchema(schemaLocation: VAnyURI): XSSchema {
            throw UnsupportedOperationException("Dummy resolver")
        }

        override fun delegate(schemaLocation: VAnyURI): Resolver = this

        override fun resolve(relativeUri: VAnyURI): VAnyURI {
            return relativeUri
        }
    }

    internal interface SchemaElementResolver {

        fun maybeSimpleType(typeName: String): ResolvedGlobalSimpleType?

        fun maybeType(typeName: String): ResolvedGlobalType?

        fun maybeAttributeGroup(attributeGroupName: String): ResolvedGlobalAttributeGroup?

        fun maybeGroup(groupName: String): ResolvedGlobalGroup?

        fun maybeElement(elementName: String): ResolvedGlobalElement?

        fun maybeAttribute(attributeName: String): ResolvedGlobalAttribute?

        fun maybeIdentityConstraint(constraintName: String): ResolvedIdentityConstraint?

        fun maybeNotation(notationName: String): ResolvedNotation?

        fun substitutionGroupMembers(headName: String): Set<ResolvedGlobalElement> = emptySet()

    }

    private inner class NestedData(val targetNamespace: VAnyURI, source: CollatedSchema) : SchemaElementResolver {

        val elements: Map<String, ResolvedGlobalElement>

        val attributes: Map<String, ResolvedGlobalAttribute>

        val simpleTypes: Map<String, ResolvedGlobalSimpleType>

        val complexTypes: Map<String, ResolvedGlobalComplexType>

        val groups: Map<String, ResolvedGlobalGroup>

        val attributeGroups: Map<String, ResolvedGlobalAttributeGroup>

        val notations: Map<String, ResolvedNotation>

        init {
            elements = DelegateMap(targetNamespace.value, source.elements) { (s, v) -> ResolvedGlobalElement(v, s) }

            attributes =
                DelegateMap(targetNamespace.value, source.attributes) { (s, v) -> ResolvedGlobalAttribute(v, s) }

            simpleTypes =
                DelegateMap(targetNamespace.value, source.simpleTypes) { (s, v) ->
                    ResolvedGlobalSimpleType(v, s)
                }

            complexTypes =
                DelegateMap(targetNamespace.value, source.complexTypes) { (s, v) ->
                    ResolvedGlobalComplexType(v, s)
                }

            groups = DelegateMap(targetNamespace.value, source.groups) { (s, v) -> ResolvedGlobalGroup(v, s) }

            attributeGroups = DelegateMap(targetNamespace.value, source.attributeGroups) { (s, v) ->
                ResolvedGlobalAttributeGroup(v, s)
            }

            notations = DelegateMap(targetNamespace.value, source.notations) { (s, v) -> ResolvedNotation(v, s) }

        }

        val identityConstraints: Map<String, ResolvedIdentityConstraint> by lazy {
            val identityConstraintList = mutableListOf<ResolvedIdentityConstraint>().also { collector ->
                elements.values.forEach { elem -> elem.collectConstraints(collector) }
                complexTypes.values.forEach { type -> type.collectConstraints(collector) }
            }
            val map = HashMap<String, ResolvedIdentityConstraint>()
            for (c in identityConstraintList) {
                val qName = c.mdlQName
                if (qName != null) {
                    require(map.put(qName.localPart, c) == null) {
                        "Duplicate identity constraint: $qName"
                    }
                }
            }
            map
        }

        override fun maybeSimpleType(typeName: String): ResolvedGlobalSimpleType? {
            return simpleTypes[typeName]
        }

        override fun maybeType(typeName: String): ResolvedGlobalType? {
            return complexTypes[typeName] ?: simpleTypes[typeName]
        }

        override fun maybeAttributeGroup(attributeGroupName: String): ResolvedGlobalAttributeGroup? {
            return attributeGroups[attributeGroupName]
        }

        override fun maybeGroup(groupName: String): ResolvedGlobalGroup? {
            return groups[groupName]
        }

        override fun maybeElement(elementName: String): ResolvedGlobalElement? {
            return elements[elementName]
        }

        override fun maybeAttribute(attributeName: String): ResolvedGlobalAttribute? {
            return attributes[attributeName]
        }

        override fun maybeIdentityConstraint(constraintName: String): ResolvedIdentityConstraint? {
            return identityConstraints[constraintName]
        }

        override fun maybeNotation(notationName: String): ResolvedNotation? {
            return notations[notationName]
        }

        override fun substitutionGroupMembers(headName: String): Set<ResolvedGlobalElement> {
            return elements.values.filterTo(HashSet()) { elem ->
                elem.substitutionGroups?.any {
                    targetNamespace.value == it.namespaceURI && it.localPart == headName
                } == true
            }
        }


    }
}

