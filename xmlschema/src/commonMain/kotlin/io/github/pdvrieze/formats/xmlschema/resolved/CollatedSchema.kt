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

import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.impl.XmlSchemaConstants.XSI_NAMESPACE
import io.github.pdvrieze.formats.xmlschema.impl.XmlSchemaConstants.XS_NAMESPACE
import io.github.pdvrieze.formats.xmlschema.types.VBlockSet
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import nl.adaptivity.xmlutil.*

internal class CollatedSchema(
    baseSchema: XSSchema,
    resolver: ResolvedSchema.Resolver,
    schemaLike: ResolvedSchemaLike,
    val namespace: String = baseSchema.targetNamespace?.value ?: "",
    includedUrls: MutableList<Pair<String, VAnyURI>> = mutableListOf(Pair(namespace, resolver.baseUri))
) {
    val elements: MutableMap<QName, Pair<ResolvedSchemaLike, SchemaAssociatedElement<XSGlobalElement>>> = mutableMapOf()
    val attributes: MutableMap<QName, Pair<ResolvedSchemaLike, SchemaAssociatedElement<XSGlobalAttribute>>> =
        mutableMapOf()
    val simpleTypes: MutableMap<QName, Pair<ResolvedSchemaLike, SchemaAssociatedElement<XSGlobalSimpleType>>> =
        mutableMapOf()
    val complexTypes: MutableMap<QName, Pair<ResolvedSchemaLike, SchemaAssociatedElement<XSGlobalComplexType>>> =
        mutableMapOf()
    val groups: MutableMap<QName, Pair<ResolvedSchemaLike, SchemaAssociatedElement<XSGroup>>> = mutableMapOf()
    val attributeGroups: MutableMap<QName, Pair<ResolvedSchemaLike, SchemaAssociatedElement<XSAttributeGroup>>> =
        mutableMapOf()
    val notations: MutableMap<QName, Pair<ResolvedSchemaLike, SchemaAssociatedElement<XSNotation>>> = mutableMapOf()
    val importedNamespaces: MutableSet<String> = mutableSetOf()
    val importedSchemas: MutableMap<String, CollatedSchema> = mutableMapOf()

    fun findType(name: QName) : Pair<ResolvedSchemaLike, SchemaAssociatedElement<out XSGlobalType>> {
        simpleTypes[name]?.let { return it }
        complexTypes[name]?.let { return it }
        return importedSchemas[name.getNamespaceURI()]?.findType(name) ?:
            throw IllegalArgumentException("No type with name $name found in schema")
    }

    fun findElement(name: QName) : Pair<ResolvedSchemaLike,XSGlobalElement> {
        elements[name]?.let { return Pair(it.first, it.second.element) }
        return importedSchemas[name.getNamespaceURI()]?.findElement(name) ?:
            throw IllegalArgumentException("No element with name $name found in schema")
    }


    init {
        val invalidAttrs = baseSchema.otherAttrs.keys.filter { it.namespaceURI in INVALID_NAMESPACES }
        require(invalidAttrs.isEmpty()) {
            "Found unsupported attributes in schema: ${invalidAttrs}"
        }

        addToCollation(baseSchema, schemaLike, resolver.baseUri.toString(), namespace)

        // TODO this may need more indirection to ensure scoping

        for (import in baseSchema.imports) {
            val importedLocation = import.schemaLocation
            if (importedLocation == null) {
                importedNamespaces.add(import.namespace!!.value)
            } else {
                val resolvedImport = resolver.resolve(importedLocation)
                val relativeResolver = resolver.delegate(resolvedImport)
                val rawImport by lazy { resolver.readSchema(resolvedImport) }
                val targetNamespace = (import.namespace ?: rawImport.targetNamespace)?.value ?: ""

                if (Pair(targetNamespace, resolvedImport) !in includedUrls) { // Avoid recursion in collations
                    includedUrls.add(Pair(targetNamespace, resolvedImport))

                    val importNamespace = import.namespace
                    val importTargetNamespace = rawImport.targetNamespace ?: VAnyURI("")
                    val chameleonSchema = when {
                        importNamespace.isNullOrEmpty() && importTargetNamespace.isEmpty() -> {
                            val schemaNamespace = schemaLike.targetNamespace
                            require(!schemaNamespace.isNullOrEmpty()) { "When an import has no targetNamespace then the enclosing document must have a targetNamespace" }
                            ChameleonWrapper(schemaLike, schemaNamespace)
                        }

                        importNamespace == null -> ChameleonWrapper(schemaLike, importTargetNamespace)

                        importTargetNamespace.isEmpty() -> ChameleonWrapper(schemaLike, importNamespace)

                        else -> {
                            require(importNamespace == importTargetNamespace) {
                                "Renaming can only be done with an import with a null targetNamespace"
                            }
                            ChameleonWrapper(schemaLike, importTargetNamespace)
                        }
                    }

                    val collatedImport = CollatedSchema(
                        rawImport,
                        relativeResolver,
                        chameleonSchema,
                        importTargetNamespace.value,
                        includedUrls
                    )
                    for ((_, nestedImport) in collatedImport.importedSchemas) {
                        addToCollation(nestedImport)
                    }
                    collatedImport.importedSchemas.clear()
                    importedSchemas.put(importTargetNamespace.value, collatedImport)
                }
            }
        }

        for (include in baseSchema.includes) {
            val includedLocation = include.schemaLocation
            val resolvedIncluded = resolver.resolve(includedLocation)
            val includeNamespace = baseSchema.targetNamespace?.value
            val relativeResolver = resolver.delegate(includedLocation)
            val rawInclude by lazy { resolver.readSchema(includedLocation) }
            val targetNamespace = (includeNamespace ?: rawInclude.targetNamespace?.value) ?: ""
            if (Pair(targetNamespace, resolvedIncluded) !in includedUrls) { // Avoid recursion in collations
                includedUrls.add(Pair(targetNamespace, resolvedIncluded))

                val includeTargetNamespace = rawInclude.targetNamespace?.value
                val chameleonNamespace = when {
                    includeNamespace == null -> requireNotNull(targetNamespace)
                    includeTargetNamespace == null -> requireNotNull(targetNamespace)

                    else -> {
                        require(includeNamespace == includeTargetNamespace) {
                            "Renaming can only be done with an import with a null targetNamespace ($includeNamespace != $includeTargetNamespace)"
                        }
                        includeNamespace
                    }
                }
                val chameleonSchema = ChameleonWrapper(schemaLike, VAnyURI(chameleonNamespace))

                val includedSchema = CollatedSchema(
                    rawInclude,
                    relativeResolver,
                    chameleonSchema,
                    chameleonNamespace,
                    includedUrls
                )
                for ((_, nestedImport) in includedSchema.importedSchemas) {
                    addToCollation(nestedImport)
                }
                includedSchema.importedSchemas.clear()
                addToCollation(includedSchema)
            }
        }

        for (redefine in baseSchema.redefines) {
            val relativeLocation = resolver.resolve(redefine.schemaLocation)
            val relativeResolver = resolver.delegate(redefine.schemaLocation)
            val nestedSchema = resolver.readSchema(redefine.schemaLocation)

//            val nestedSchemaLike = RedefineWrapper(schemaLike, nestedSchema, relativeLocation.value, null)

            val collatedSchema = CollatedSchema(nestedSchema, relativeResolver, schemaLike = schemaLike)

            collatedSchema.applyRedefines(
                redefine,
                baseSchema.targetNamespace,
                nestedSchema,
                schemaLike,
                relativeLocation.value
            )

            for ((_, nestedImport) in collatedSchema.importedSchemas) {
                addToCollation(nestedImport)
            }
            collatedSchema.importedSchemas.clear()

            addToCollation(collatedSchema)
        }

    }

    fun checkRecursiveSubstitutionGroups() {
        val verifiedHeads = mutableSetOf<QName>()

        fun followChain(
            elementName: QName,
            seen: SingleLinkedList<QName>,
            elementInfo: Pair<ResolvedSchemaLike, XSGlobalElement> = findElement(elementName)
        ) {
            val (schema, element) = elementInfo
            val newSeen = seen + elementName
            val sg = (element.substitutionGroup ?: run { verifiedHeads.addAll(newSeen); return })
                .let {
                    when (schema) {
                        is ChameleonWrapper -> {
                            val ns = schema.chameleonNamespace?.value ?: ""
                            it.map { n -> QName(ns, n.localPart) }
                        }
                        else -> it
                    }
                }
            for (referenced in sg) {
                if (referenced !in verifiedHeads) {
                    require(referenced !in newSeen) { "Recursive substitution group (${newSeen.sortedBy { it.toString() }.joinToString()})" }
                    followChain(referenced, newSeen)
                }
            }
        }

        for((name, elementInfo) in elements) {
            if (name !in verifiedHeads) {
                followChain(name, SingleLinkedList(), Pair(elementInfo.first, elementInfo.second.element))
            }
        }
    }

    fun checkRecursiveTypeDefinitions() {
        val verifiedSet = mutableSetOf<XSGlobalType>()
        for (typeInfo in (simpleTypes.values + complexTypes.values)) {
            if (typeInfo.second.element !in verifiedSet) { // skip already validated types
                val chain = mutableSetOf<XSGlobalType>()
                checkRecursiveTypes(typeInfo, verifiedSet, chain)
                verifiedSet.addAll(chain)
            }
        }
    }

    fun checkRecursiveTypes(
        typeInfo: Pair<ResolvedSchemaLike, SchemaAssociatedElement<out XSGlobalType>>,
        seenTypes: MutableSet<XSGlobalType>,
        inheritanceChain: MutableSet<XSGlobalType>,
    ) {
        val (schema, x) = typeInfo
        val (_, startType) = x
        val name = (startType as? XSGlobalType)?.name?.toQname(schema.targetNamespace)
        checkRecursiveTypes(startType, schema, seenTypes, inheritanceChain)
    }

    private fun checkRecursiveTypes(
        startType: XSIType,
        schema: ResolvedSchemaLike,
        seenTypes: MutableSet<XSGlobalType>,
        inheritanceChain: MutableSet<XSGlobalType>
    ) {
        require(startType !is XSGlobalType || inheritanceChain.add(startType)) {
            "Recursive type use for ${(startType as XSGlobalType).name}: ${inheritanceChain.joinToString { it.name }}"
        }
        val refs: List<QName>
        val locals: List<XSLocalType>
        when (startType) {
            is XSISimpleType -> {
                when (val d = startType.simpleDerivation) {
                    is XSSimpleList -> {
                        refs = listOfNotNull(d.itemTypeName)
                        locals = listOfNotNull(d.simpleType)
                    }

                    is XSSimpleRestriction -> {
                        refs = listOfNotNull(d.base)
                        locals = listOfNotNull(d.simpleType)
                    }

                    is XSSimpleUnion -> {
                        refs = d.memberTypes ?: emptyList()
                        locals = d.simpleTypes
                    }
                }
            }

            is XSComplexType.ComplexBase -> {
                when (val c: XSI_ComplexContent = startType.content) {
                    is XSComplexType.Shorthand -> {
                        refs = listOfNotNull(c.base)
                        locals = emptyList()

                    }

                    is XSComplexContent -> {
                        refs = listOfNotNull(c.derivation.base)
                        locals = emptyList()

                    }

                    is XSSimpleContent -> {
                        refs = listOfNotNull(c.derivation.base)
                        locals = emptyList()
                    }
                }
            }

            is XSComplexType.Simple -> {
                val d = startType.content.derivation
                refs = listOfNotNull(d.base)
                locals = listOfNotNull((d as? XSSimpleContentRestriction)?.simpleType)
            }

            else -> throw AssertionError("Unreachable")
        }
        val finalRefs = refs.asSequence()
            .filter { it.namespaceURI != XS_NAMESPACE && it.namespaceURI != XSI_NAMESPACE }
            .let {
                when (schema) {
                    is ChameleonWrapper -> it.map {
                        QName(schema.chameleonNamespace?.value ?: "", it.localPart)
                    }

                    else -> it
                }
            }.toSet()
        for (ref in finalRefs) {
            if (schema is RedefineWrapper &&
                schema.elementKind == Redefinable.TYPE &&
                schema.elementName.isEquivalent(ref)
            ) {
                val typeInfo = schema.lookupRawType(ref)
                when {
                    typeInfo == null ->
                        require(ref.namespaceURI == XS_NAMESPACE || ref.namespaceURI == XSI_NAMESPACE) {
                            "Failure to find referenced (redefined) type $ref"
                        }

                    typeInfo.second.element !in seenTypes ->
                        checkRecursiveTypes(typeInfo, seenTypes, inheritanceChain)
                }
            } else {
                val typeInfo = findType(ref)
                if (typeInfo.second.element !in seenTypes) checkRecursiveTypes(typeInfo, seenTypes, inheritanceChain)
            }


        }
        for (local in locals) {
            checkRecursiveTypes(local, schema, seenTypes, inheritanceChain)
        }

    }


    fun applyRedefines(
        redefine: XSRedefine,
        targetNamespace: VAnyURI?,
        nestedSchema: XSSchema,
        origSchemaLike: ResolvedSchemaLike,
        schemaLocation: String,
    ) {
        for (st in redefine.simpleTypes) {
            val name = QName(targetNamespace?.toString() ?: "", st.name.toString())
            val schemaLike = RedefineWrapper(origSchemaLike, nestedSchema, schemaLocation, null, name, Redefinable.TYPE)
            val old = requireNotNull(simpleTypes[name]) { "Redefine must override" }
            val s = schemaLike.withNestedRedefine(old.first as? RedefineWrapper)
            simpleTypes[name] = Pair(s, SchemaAssociatedElement(schemaLocation, st))
        }

        for (ct in redefine.complexTypes) {
            val name = QName(targetNamespace?.toString() ?: "", ct.name.toString())
            val schemaLike = RedefineWrapper(origSchemaLike, nestedSchema, schemaLocation, null, name, Redefinable.TYPE)
            val old = requireNotNull(complexTypes[name]) { "Redefine must override" }
            val s = schemaLike.withNestedRedefine(old.first as? RedefineWrapper)
            complexTypes[name] = Pair(s, SchemaAssociatedElement(schemaLocation, ct))
        }

        for (g in redefine.groups) {
            val name = QName(targetNamespace?.toString() ?: "", g.name.toString())
            val schemaLike =
                RedefineWrapper(origSchemaLike, nestedSchema, schemaLocation, null, name, Redefinable.GROUP)
            val old = requireNotNull(groups[name]) { "Redefine must override" }
            val s = schemaLike.withNestedRedefine(old.first as? RedefineWrapper)
            groups[name] = Pair(s, SchemaAssociatedElement(schemaLocation, g))
        }

        for (ag in redefine.attributeGroups) {
            val name = QName(targetNamespace?.toString() ?: "", ag.name.toString())
            val schemaLike =
                RedefineWrapper(origSchemaLike, nestedSchema, schemaLocation, null, name, Redefinable.GROUP)
            val old = requireNotNull(attributeGroups[name]) { "Redefine must override" }
            val s = schemaLike.withNestedRedefine(old.first as? RedefineWrapper)
            attributeGroups[name] = Pair(s, SchemaAssociatedElement(schemaLocation, ag))
        }
    }

    private fun addToCollation(
        sourceSchema: XSSchema,
        schemaLike: ResolvedSchemaLike,
        schemaLocation: String,
        targetNamespace: String = sourceSchema.targetNamespace?.value ?: ""
    ) {
        when (schemaLike) {
            is ChameleonWrapper -> schemaLike.chameleonNamespace?.let { importedNamespaces.add(it.value) }
            else -> importedNamespaces.add(targetNamespace)
        }

        sourceSchema.elements.associateToUnique(elements) {
            QName(targetNamespace, it.name.toString()) to
                    Pair(schemaLike, SchemaAssociatedElement(schemaLocation, it))
        }
        sourceSchema.attributes.associateToUnique(attributes) {
            QName(targetNamespace, it.name.toString()) to
                    Pair(schemaLike, SchemaAssociatedElement(schemaLocation, it))
        }
        sourceSchema.simpleTypes.associateToUnique(simpleTypes) {
            QName(targetNamespace, it.name.toString()) to
                    Pair(schemaLike, SchemaAssociatedElement(schemaLocation, it))
        }
        sourceSchema.complexTypes.associateToUnique(complexTypes) {
            QName(targetNamespace, it.name.toString()) to
                    Pair(schemaLike, SchemaAssociatedElement(schemaLocation, it))
        }
        sourceSchema.groups.associateToUnique(groups) {
            QName(targetNamespace, it.name.toString()) to
                    Pair(schemaLike, SchemaAssociatedElement(schemaLocation, it))
        }
        sourceSchema.attributeGroups.associateToUnique(attributeGroups) {
            QName(targetNamespace, it.name.toString()) to
                    Pair(schemaLike, SchemaAssociatedElement(schemaLocation, it))
        }
        sourceSchema.notations.associateToUnique(notations) {
            QName(targetNamespace, it.name.toString()) to
                    Pair(schemaLike, SchemaAssociatedElement(schemaLocation, it))
        }
    }

    private fun addToCollation(sourceSchema: CollatedSchema) {
        importedNamespaces.addAll(sourceSchema.importedNamespaces)
        sourceSchema.elements.entries.associateToUnique(elements)
        sourceSchema.attributes.entries.associateToUnique(attributes)
        sourceSchema.simpleTypes.entries.associateToUnique(simpleTypes)
        sourceSchema.complexTypes.entries.associateToUnique(complexTypes)
        sourceSchema.groups.entries.associateToUnique(groups)
        sourceSchema.attributeGroups.entries.associateToUnique(attributeGroups)
        sourceSchema.notations.entries.associateToUnique(notations)
    }

    enum class Redefinable { TYPE, ELEMENT, ATTRIBUTE, GROUP, ATTRIBUTEGROUP }

    class RedefineWrapper(
        val base: ResolvedSchemaLike,
        val originalSchema: XSSchema,
        val originalLocation: String,
        val nestedRedefine: RedefineWrapper?,
        val elementName: QName,
        val elementKind: Redefinable,
    ) : ResolvedSchemaLike() {

        override val targetNamespace: VAnyURI? get() = originalSchema.targetNamespace
        override val blockDefault: VBlockSet get() = originalSchema.blockDefault ?: emptySet()
        override val finalDefault: Set<VDerivationControl.Type> get() = originalSchema.finalDefault ?: emptySet()
        override val defaultOpenContent: XSDefaultOpenContent? get() = originalSchema.defaultOpenContent
        override val defaultAttributes: QName? get() = originalSchema.defaultAttributes

        override fun maybeSimpleType(typeName: QName): ResolvedGlobalSimpleType? {
            if (elementKind == Redefinable.TYPE && elementName == typeName) {
                return nestedSimpleType(typeName)
            }

            return base.maybeSimpleType(typeName)
        }

        override fun maybeType(typeName: QName): ResolvedGlobalType? {
            if (elementKind == Redefinable.TYPE && elementName == typeName) {
                return nestedType(typeName)
            }

            return base.maybeType(typeName)
        }

        fun maybeComplexType(typeName: QName): ResolvedGlobalComplexType? {
            return (base as? RedefineWrapper)?.maybeComplexType(typeName)
        }

        fun nestedSimpleType(typeName: QName): ResolvedGlobalSimpleType {
            val originalNS = originalSchema.targetNamespace?.value ?: ""
            require(originalNS == typeName.namespaceURI)

            val localName = typeName.localPart
            return originalSchema.simpleTypes.singleOrNull { it.name.xmlString == localName }?.let { b ->
                val sa = SchemaAssociatedElement(originalLocation, b)
                ResolvedGlobalSimpleType(sa, base)
            } ?: nestedRedefine?.nestedSimpleType(typeName)
            ?: error("Nested simple type with name $typeName could not be found")
        }

        fun nestedComplexType(typeName: QName): ResolvedGlobalComplexType {
            val originalNS = originalSchema.targetNamespace?.value ?: ""
            require(originalNS == typeName.namespaceURI) { }
            val localName = typeName.localPart
            return originalSchema.complexTypes.singleOrNull { it.name.xmlString == localName }?.let { b ->
                val sa = SchemaAssociatedElement(originalLocation, b)
                ResolvedGlobalComplexType(sa, nestedRedefine ?: base)
            } ?: nestedRedefine?.nestedComplexType(typeName)
            ?: error("No nested complex type with name $typeName")
        }

        fun nestedType(typeName: QName): ResolvedGlobalType {
            val originalNS = originalSchema.targetNamespace?.value ?: ""
            require(originalNS == typeName.namespaceURI)

            val localName = typeName.localPart
            return originalSchema.simpleTypes.singleOrNull { it.name.xmlString == localName }?.let { b ->
                val sa = SchemaAssociatedElement(originalLocation, b)
                ResolvedGlobalSimpleType(sa, base)
            } ?: originalSchema.complexTypes.singleOrNull { it.name.xmlString == localName }?.let { b ->
                val sa = SchemaAssociatedElement(originalLocation, b)
                ResolvedGlobalComplexType(sa, nestedRedefine ?: base)
            } ?: nestedRedefine?.nestedType(typeName)
            ?: error("No nested type with name $typeName")
        }

        fun nestedAttributeGroup(typeName: QName): ResolvedGlobalAttributeGroup {
            val originalNS = originalSchema.targetNamespace?.value ?: ""
            require(originalNS == typeName.namespaceURI) { }
            val localName = typeName.localPart
            return originalSchema.attributeGroups.singleOrNull { it.name.xmlString == localName }?.let { b ->
                val sa = SchemaAssociatedElement(originalLocation, b)
                ResolvedGlobalAttributeGroup(sa, nestedRedefine ?: base)
            } ?: nestedRedefine?.nestedAttributeGroup(typeName)
            ?: error("No nested complex type with name $typeName")
        }

        fun nestedGroup(typeName: QName): ResolvedGlobalGroup {
            val originalNS = originalSchema.targetNamespace?.value ?: ""
            require(originalNS == typeName.namespaceURI) { }
            val localName = typeName.localPart
            return originalSchema.groups.singleOrNull { it.name.xmlString == localName }?.let { b ->
                val sa = SchemaAssociatedElement(originalLocation, b)
                ResolvedGlobalGroup(sa, nestedRedefine ?: base)
            } ?: nestedRedefine?.nestedGroup(typeName)
            ?: error("No nested complex type with name $typeName")
        }

        fun nestedElement(typeName: QName): ResolvedGlobalElement {
            val originalNS = originalSchema.targetNamespace?.value ?: ""
            require(originalNS == typeName.namespaceURI) { }
            val localName = typeName.localPart
            return originalSchema.elements.singleOrNull { it.name.xmlString == localName }?.let { b ->
                val sa = SchemaAssociatedElement(originalLocation, b)
                ResolvedGlobalElement(sa, nestedRedefine ?: base)
            } ?: nestedRedefine?.nestedElement(typeName)
            ?: error("No nested complex type with name $typeName")
        }

        fun nestedAttribute(typeName: QName): ResolvedGlobalAttribute {
            val originalNS = originalSchema.targetNamespace?.value ?: ""
            require(originalNS == typeName.namespaceURI) { }
            val localName = typeName.localPart
            return originalSchema.attributes.singleOrNull { it.name.xmlString == localName }?.let { b ->
                val sa = SchemaAssociatedElement(originalLocation, b)
                ResolvedGlobalAttribute(sa, nestedRedefine ?: base)
            } ?: nestedRedefine?.nestedAttribute(typeName)
            ?: error("No nested complex type with name $typeName")
        }

        override fun maybeAttributeGroup(attributeGroupName: QName): ResolvedGlobalAttributeGroup? {
            if (elementKind == Redefinable.ATTRIBUTEGROUP && elementName == attributeGroupName) {
                return nestedAttributeGroup(attributeGroupName)
            }
            return base.maybeAttributeGroup(attributeGroupName)
        }

        override fun maybeGroup(groupName: QName): ResolvedGlobalGroup? {
            if (elementKind == Redefinable.GROUP && elementName == groupName) {
                return nestedGroup(groupName)
            }
            return base.maybeGroup(groupName)
        }

        override fun maybeElement(elementName: QName): ResolvedGlobalElement? {
            if (elementKind == Redefinable.ELEMENT && this.elementName == elementName) {
                return nestedElement(elementName)
            }
            return base.maybeElement(elementName)
        }

        override fun maybeAttribute(attributeName: QName): ResolvedGlobalAttribute? {
            if (elementKind == Redefinable.ATTRIBUTE && elementName == attributeName) {
                return nestedAttribute(attributeName)
            }
            return base.maybeAttribute(attributeName)
        }

        override fun maybeIdentityConstraint(constraintName: QName): ResolvedIdentityConstraint? {
            return base.maybeIdentityConstraint(constraintName)
        }

        override fun maybeNotation(notationName: QName): ResolvedNotation? {
            return base.maybeNotation(notationName)
        }

        override fun substitutionGroupMembers(headName: QName): Set<ResolvedGlobalElement> {
            return base.substitutionGroupMembers(headName)
        }

        fun withNestedRedefine(nestedRedefine: RedefineWrapper?): RedefineWrapper = when (nestedRedefine) {
            null -> this
            else -> RedefineWrapper(base, originalSchema, originalLocation, nestedRedefine, elementName, elementKind)
        }

        fun lookupRawType(name: QName): Pair<ResolvedSchemaLike, SchemaAssociatedElement<out XSGlobalType>>? {
            if ((targetNamespace?.value ?: "") != name.namespaceURI) return null

            val targetLocalName = name.localPart
            originalSchema.simpleTypes.firstOrNull { it.name.xmlString == targetLocalName }?.let {
                return Pair(nestedRedefine?:this, SchemaAssociatedElement(originalLocation, it))
            }

            originalSchema.complexTypes.firstOrNull { it.name.xmlString == targetLocalName }?.let {
                return Pair(nestedRedefine?:this, SchemaAssociatedElement(originalLocation, it))
            }
            return nestedRedefine?.lookupRawType(name)
        }
    }

    class ChameleonWrapper(val base: ResolvedSchemaLike, val chameleonNamespace: VAnyURI?) : ResolvedSchemaLike() {
        override val targetNamespace: VAnyURI?
            get() = chameleonNamespace

        override val blockDefault: VBlockSet
            get() = base.blockDefault
        override val finalDefault: Set<VDerivationControl.Type>
            get() = base.finalDefault
        override val defaultOpenContent: XSDefaultOpenContent?
            get() = base.defaultOpenContent
        override val defaultAttributes: QName? get() = base.defaultAttributes

        private fun QName.extend(): QName {
            return when {
                namespaceURI.isEmpty() -> QName(chameleonNamespace?.value ?: "", localPart, prefix)
                else -> this
            }
        }

        override fun maybeSimpleType(typeName: QName): ResolvedGlobalSimpleType? {
            return base.maybeSimpleType(typeName.extend())
        }

        override fun maybeType(typeName: QName): ResolvedGlobalType? {
            return base.maybeType(typeName.extend())
        }

        override fun maybeAttribute(attributeName: QName): ResolvedGlobalAttribute? {
            return base.maybeAttribute(attributeName.extend())
        }

        override fun maybeAttributeGroup(attributeGroupName: QName): ResolvedGlobalAttributeGroup? {
            return base.maybeAttributeGroup(attributeGroupName.extend())
        }

        override fun maybeGroup(groupName: QName): ResolvedGlobalGroup? {
            return base.maybeGroup(groupName.extend())
        }

        override fun maybeElement(elementName: QName): ResolvedGlobalElement? {
            return base.maybeElement(elementName.extend())
        }

        override fun maybeIdentityConstraint(constraintName: QName): ResolvedIdentityConstraint? {
            return base.maybeIdentityConstraint(constraintName)
        }

        override fun maybeNotation(notationName: QName): ResolvedNotation? {
            return base.maybeNotation(notationName)
        }

        override fun substitutionGroupMembers(headName: QName): Set<ResolvedGlobalElement> {
            return base.substitutionGroupMembers(headName)
        }
    }

    companion object {

        private val INVALID_NAMESPACES: HashSet<String> = HashSet(listOf("", XS_NAMESPACE))

        private inline fun <T, K, M : MutableMap<in K, in T>> Iterable<Map.Entry<K, T>>.associateToUnique(
            destination: M
        ): M {
            for (element in this) {
                val (key, value) = element
                if (key in destination) {
                    if (destination[key] != value) { //identical values is allowed, but ignored
                        throw IllegalArgumentException("Duplicate key ($key) on unique association")
                    }
                }
                destination.put(key, value)
            }
            return destination
        }

        private inline fun <T, K, M : MutableMap<in K, in T>> Iterable<T>.associateByToUnique(
            destination: M,
            keySelector: (T) -> K
        ): M {
            for (element in this) {
                val key = keySelector(element)
                require(key !in destination) { "Duplicate key on unique association" }
                destination.put(key, element)
            }
            return destination
        }

        private inline fun <T, K, V, M : MutableMap<in K, in V>> Iterable<T>.associateToUnique(
            destination: M,
            keySelector: (T) -> Pair<K, V>
        ): M {
            for (element in this) {
                val (key, value) = keySelector(element)
                require(key !in destination) { "Duplicate key on unique association" }
                destination.put(key, value)
            }
            return destination
        }

        private inline fun <T, K, M : MutableMap<in K, in T>> Iterable<T>.associateByToOverride(
            destination: M,
            keySelector: (T) -> K
        ): M {
            for (element in this) {
                val key = keySelector(element)

                require(key in destination) { "Duplicate key on unique association" }
                destination.put(key, element)
            }
            return destination
        }

        private inline fun <T, K, M : MutableMap<in K, in T>> Iterable<Map.Entry<K, T>>.associateToOverride(
            destination: M
        ): M {
            for (element in this) {
                val (key, value) = element

                require(key in destination) { "Duplicate key on unique association" }
                destination.put(key, value)
            }
            return destination
        }

        private inline fun <T, V, K, M : MutableMap<in K, V>> Iterable<T>.associateToOverride(
            destination: M,
            getKey: (T) -> K,
            getNewValue: (T, V) -> V
        ): M {
            for (element in this) {
                val key = getKey(element)
                val oldValue: V = requireNotNull(destination[key]) { "Redefine must override an existing value" }
                val newValue = getNewValue(element, oldValue)

                destination.put(key, newValue)
            }
            return destination
        }

    }

}

internal data class SchemaAssociatedElement<T>(val schemaLocation: String, val element: T)
