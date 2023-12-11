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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.resolved.checking.CheckHelper
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import io.github.pdvrieze.formats.xmlschema.types.VXPathDefaultNamespace
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI

class ResolvedSchema(val rawPart: XSSchema, resolver: Resolver, defaultVersion: SchemaVersion = SchemaVersion.V1_1) :
    ResolvedSchemaLike() {

    private val nestedData: MutableMap<String, SchemaElementResolver> = mutableMapOf()
    private val visibleNamespaces: Set<String>


    init {
        val rootData =
            SchemaData(rawPart, listOf(resolver.baseUri.value), rawPart.targetNamespace?.value ?: "", resolver)

        rootData.checkRecursiveTypeDefinitions()
        rootData.checkRecursiveSubstitutionGroups()

        val allData: Collection<Pair<String, SchemaData>> = rootData.collectAndMergeNested(mutableMapOf()).values

        val allNeededNamespaces = HashSet<String>()

        for((importNs, schemaData) in allData) {
            allNeededNamespaces.addAll(schemaData.includedNamespaceToUris.keys)
            allNeededNamespaces.addAll(schemaData.importedNamespaces) // allow for unresolved namespaces
            val nestedData = NestedData(importNs.toAnyUri(), schemaData)
            val past = (this.nestedData[importNs] as? NestedData)
            this.nestedData[importNs] = past?.mergeWith(nestedData) ?: nestedData
        }

        // Use getOrPut to ensure uniqueness
        nestedData.getOrPut(BuiltinSchemaXmlschema.targetNamespace.value) { BuiltinSchemaXmlschema.resolver }
        nestedData.getOrPut(BuiltinSchemaXmlInstance.targetNamespace.value) { BuiltinSchemaXmlInstance.resolver }
        if (rawPart.targetNamespace?.value != XMLConstants.XML_NS_URI &&
            XMLConstants.XML_NS_URI in allNeededNamespaces
        ) {
            val old = nestedData.getOrPut(XMLConstants.XML_NS_URI) { BuiltinSchemaXml.resolver } // allow override of the namespace
        }

        visibleNamespaces = rootData.importedNamespaces.toSet()

    }

    val annotations: List<XSAnnotation> get() = rawPart.annotations

    override val defaultOpenContent: XSDefaultOpenContent?
        get() = rawPart.defaultOpenContent

    override val attributeFormDefault: VFormChoice
        get() = rawPart.attributeFormDefault ?: VFormChoice.UNQUALIFIED

    override val blockDefault: Set<VDerivationControl.T_BlockSetValues>
        get() = rawPart.blockDefault ?: emptySet()

    override val defaultAttributes: QName? = rawPart.defaultAttributes

    val xPathDefaultNamespace: VXPathDefaultNamespace
        get() = rawPart.xpathDefaultNamespace ?: VXPathDefaultNamespace.LOCAL

    override val elementFormDefault: VFormChoice
        get() = rawPart.elementFormDefault ?: VFormChoice.UNQUALIFIED

    override val finalDefault: Set<VDerivationControl.Type>
        get() = rawPart.finalDefault ?: emptySet()

    val id: VID? get() = rawPart.id

    override val targetNamespace: VAnyURI get() = rawPart.targetNamespace ?: "".toAnyUri()

    override fun hasLocalTargetNamespace(): Boolean {
        return nestedData.containsKey("")
    }

    override val version: SchemaVersion = rawPart.version?.run { SchemaVersion.fromXml(xmlString) } ?: defaultVersion

    val lang: VLanguage? get() = rawPart.lang

    private inline fun <R> withQName(name: QName, action: SchemaElementResolver.(String) -> R): R {
        val data = nestedData[name.namespaceURI]
            ?: throw NoSuchElementException("The namespace '${name.namespaceURI}' is not available when resolving $name")
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
                for (g in data.groups.values) { // check first as this will handle recursive group definitions
                    checkHelper.checkGroup(g)
                }
                for (e in data.elements.values) {
                    checkHelper.checkElement(e)
                }
                for (a in data.attributes.values) {
                    checkHelper.checkAttribute(a)
                }
                for (t in data.types.values) {
                    checkHelper.checkType(t)
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
                for (n in data.notations) {
                    checkHelper.checkNotation(n.value.mdlQName)
                }
            }
        }

    }

    interface Resolver {
        val baseUri: VAnyURI

        fun readSchema(schemaLocation: VAnyURI): XSSchema

        fun tryReadSchema(schemaLocation: VAnyURI): XSSchema?

        /**
         * Create a delegate resolver for the schema
         */
        fun delegate(schemaLocation: VAnyURI): Resolver

        fun resolve(relativeUri: VAnyURI): VAnyURI
    }

    internal object DummyResolver : Resolver {
        override val baseUri: VAnyURI = "".toAnyUri()

        override fun readSchema(schemaLocation: VAnyURI): XSSchema {
            throw UnsupportedOperationException("Dummy resolver")
        }

        override fun tryReadSchema(schemaLocation: VAnyURI): XSSchema? {
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

    private inner class NestedData : SchemaElementResolver {

        val targetNamespace: VAnyURI

        constructor(targetNamespace: VAnyURI, source: SchemaData) {
            this.targetNamespace = targetNamespace

            require(targetNamespace.value == source.namespace) {
                "Namespace mismatch (${targetNamespace.value} != ${source.namespace})"
            }

            val s = this@ResolvedSchema.let { s2 ->
                when {
                    s2.elementFormDefault != source.rawSchema.elementFormDefault ||
                            s2.attributeFormDefault != source.rawSchema.attributeFormDefault ->
                        OwnerWrapper(s2, source.rawSchema, source.importedNamespaces)

                    else -> s2
                }
            }

            val loc = source.schemaLocation ?: ""

            _elements = DelegateMap(targetNamespace.value, source.elements) { v -> ResolvedGlobalElement(v, s) }

            attributes = DelegateMap(targetNamespace.value, source.attributes) { v -> ResolvedGlobalAttribute(v, s) }

            _types = DelegateMap(targetNamespace.value, source.types) { v ->
                when (val t = v.elem) {
                    is XSGlobalSimpleType -> ResolvedGlobalSimpleType(t, v.effectiveSchema(s))
                    is XSGlobalComplexType -> ResolvedGlobalComplexType(v.cast(), v.effectiveSchema(s), loc)
                }
            }

            _groups = DelegateMap(targetNamespace.value, source.groups) { v -> ResolvedGlobalGroup(v, s) }
            attributeGroups = DelegateMap(targetNamespace.value, source.attributeGroups) { v ->
                ResolvedGlobalAttributeGroup(v, s)
            }
            notations = DelegateMap(targetNamespace.value, source.notations) { v -> ResolvedNotation(v, s, loc) }

            imports = source.importedNamespaces.associateWith { ns ->
                object : SchemaElementResolver {
                    val delegate by lazy { nestedData[ns] }
                    override fun maybeSimpleType(typeName: String): ResolvedGlobalSimpleType? =
                        delegate?.maybeSimpleType(typeName)

                    override fun maybeType(typeName: String): ResolvedGlobalType? =
                        delegate?.maybeType(typeName)

                    override fun maybeAttributeGroup(attributeGroupName: String): ResolvedGlobalAttributeGroup? =
                        delegate?.maybeAttributeGroup(attributeGroupName)

                    override fun maybeGroup(groupName: String): ResolvedGlobalGroup? =
                        delegate?.maybeGroup(groupName)

                    override fun maybeElement(elementName: String): ResolvedGlobalElement? =
                        delegate?.maybeElement(elementName)

                    override fun maybeAttribute(attributeName: String): ResolvedGlobalAttribute? =
                        delegate?.maybeAttribute(attributeName)

                    override fun maybeIdentityConstraint(constraintName: String): ResolvedIdentityConstraint? =
                        delegate?.maybeIdentityConstraint(constraintName)

                    override fun maybeNotation(notationName: String): ResolvedNotation? =
                        delegate?.maybeNotation(notationName)
                }
            }

        }

        private constructor(
            targetNamespace: VAnyURI,
            elements: Map<String, ResolvedGlobalElement>,
            attributes: Map<String, ResolvedGlobalAttribute>,
            types: Map<String, ResolvedGlobalType>,
            groups: Map<String, ResolvedGlobalGroup>,
            attributeGroups: Map<String, ResolvedGlobalAttributeGroup>,
            notations: Map<String, ResolvedNotation>,
            imports: Map<String, SchemaElementResolver?>,
        ) {
            this.targetNamespace = targetNamespace
            _elements = elements
            this.attributes = attributes
            _types = types
            _groups = groups
            this.attributeGroups = attributeGroups
            this.notations = notations
            this.imports = imports
        }

        private val _elements: Map<String, ResolvedGlobalElement>
        val elements: Map<String, ResolvedGlobalElement> get() = _elements

        val attributes: Map<String, ResolvedGlobalAttribute>

        val _types: Map<String, ResolvedGlobalType>
        val types: Map<String, ResolvedGlobalType> get() = _types

        val _groups: Map<String, ResolvedGlobalGroup>
        val groups: Map<String, ResolvedGlobalGroup> get() = _groups

        val attributeGroups: Map<String, ResolvedGlobalAttributeGroup>

        val notations: Map<String, ResolvedNotation>

        val imports: Map<String, SchemaElementResolver?> // imports can be unresolved, can be built-ins

        val identityConstraints: Map<String, ResolvedIdentityConstraint> by lazy {
            val identityConstraintList = mutableSetOf<ResolvedIdentityConstraint>().also { collector ->
                elements.values.forEach { elem -> elem.collectConstraints(collector) }
                types.values.forEach { type ->
                    if (type is ResolvedComplexType) type.collectConstraints(collector)
                }
                groups.values.forEach { group ->
                    group.mdlModelGroup.collectConstraints(collector)
                }
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
            return types[typeName]?.let {
                checkNotNull(it as? ResolvedGlobalSimpleType) { "The type $typeName resolves to a complex type, not a simple one" }
            }
        }

        override fun maybeType(typeName: String): ResolvedGlobalType? {
            return types[typeName]
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

        fun mergeWith(otherData: NestedData): NestedData {
            val newElements = mutableMapOf<String, ResolvedGlobalElement>().apply { putAll(_elements) }

            val newAttributes = mutableMapOf<String, ResolvedGlobalAttribute>().apply { putAll(attributes) }
            val newTypes = mutableMapOf<String, ResolvedGlobalType>().apply { putAll(_types) }
            val newGroups = mutableMapOf<String, ResolvedGlobalGroup>().apply { putAll(_groups) }
            val newAttributeGroups = mutableMapOf<String, ResolvedGlobalAttributeGroup>().apply { putAll(attributeGroups) }
            val newNotations = mutableMapOf<String, ResolvedNotation>().apply { putAll(notations) }
            val newImports = mutableMapOf<String, SchemaElementResolver?>().apply { putAll(imports) }

            val ons = otherData.targetNamespace.value

            for ((n, e) in otherData.elements) {
                require(newElements.put(n, e) == null) { "Duplicate element with name ${QName(ons, n)}" }
            }
            for ((n, a) in otherData.attributes) {
                require(newAttributes.put(n, a) == null) { "Duplicate attribute with name ${QName(ons, n)}" }
            }
            for ((n, t) in otherData.types) {
                require(newTypes.put(n, t) == null) { "Duplicate type with name ${QName(ons, n)}" }
            }
            for ((n, g) in otherData.groups) {
                require(newGroups.put(n, g) == null) { "Duplicate group with name ${QName(ons, n)}" }
            }
            for ((n, ag) in otherData.attributeGroups) {
                require(newAttributeGroups.put(n, ag) == null) { "Duplicate attribute group with name ${QName(ons, n)}" }
            }
            for ((name, notation) in otherData.notations) {
                require(newNotations.put(name, notation) == null) { "Duplicate notation with name ${QName(ons, name)}" }
            }



            return NestedData(
                targetNamespace,
                newElements,
                newAttributes,
                newTypes,
                newGroups,
                newAttributeGroups,
                newNotations,
                newImports,
            )
        }

    }

    companion object {


        fun Version(str: String): SchemaVersion = when (str) {
            "1.0" -> SchemaVersion.V1_0
            "1.1" -> SchemaVersion.V1_1
            else -> throw IllegalArgumentException("'$str' is not a supported version")
        }


        const val STRICT_ALL_IN_EXTENSION: Boolean = true

        /**
         * If true, apply rules that are more restrictive than needed (per standard/test suite, but
         * semantically valid)
         */
        const val VALIDATE_PEDANTIC: Boolean = true
    }
}

