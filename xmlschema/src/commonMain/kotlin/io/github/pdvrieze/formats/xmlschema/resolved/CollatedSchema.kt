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
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import io.github.pdvrieze.formats.xmlschema.types.T_BlockSet
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.localPart
import nl.adaptivity.xmlutil.namespaceURI
import nl.adaptivity.xmlutil.prefix

internal class CollatedSchema(
    baseSchema: XSSchema,
    resolver: ResolvedSchema.Resolver,
    schemaLike: ResolvedSchemaLike,
    val namespace: String = baseSchema.targetNamespace?.value ?: "",
    includedUrls: MutableList<Pair<String, VAnyURI>> = mutableListOf(Pair(namespace, resolver.baseUri))
) {
    val elements: MutableMap<QName, Pair<ResolvedSchemaLike, SchemaAssociatedElement<XSElement>>> = mutableMapOf()
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


    init {
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
                    val importTargetNamespace = rawImport.targetNamespace
                    val chameleonSchema = when {
                        importNamespace == null -> ChameleonWrapper(schemaLike, importTargetNamespace)
                        importTargetNamespace.isNullOrEmpty() -> ChameleonWrapper(schemaLike, importNamespace)
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
                        checkNotNull(importTargetNamespace) { "Imports must have a namespace" }.value,
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
                    chameleonNamespace.toString(),
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

            val nestedSchemaLike = RedefineWrapper(schemaLike, nestedSchema, relativeLocation.value, null)

            val collatedSchema = CollatedSchema(nestedSchema, relativeResolver, schemaLike = nestedSchemaLike)

            collatedSchema.applyRedefines(
                redefine,
                baseSchema.targetNamespace,
                nestedSchemaLike,
                relativeLocation.value
            )

            for ((_, nestedImport) in collatedSchema.importedSchemas) {
                addToCollation(nestedImport)
            }
            collatedSchema.importedSchemas.clear()

            addToCollation(collatedSchema)
        }

    }

    fun applyRedefines(
        redefine: XSRedefine,
        targetNamespace: VAnyURI?,
        schemaLike: RedefineWrapper,
        schemaLocation: String,
    ) {
        redefine.simpleTypes.associateToOverride(
            simpleTypes,
            { QName(targetNamespace?.toString() ?: "", it.name.toString()) }
        ) { it, old ->
            val s = schemaLike.withNestedRedefine(old.first as? RedefineWrapper)
            Pair(s, SchemaAssociatedElement(schemaLocation, it))
        }
        redefine.complexTypes.associateToOverride(complexTypes,
            { QName(targetNamespace?.toString() ?: "", it.name.toString()) }
        ) { it, old ->
            val s = schemaLike.withNestedRedefine(old.first as? RedefineWrapper)
            Pair(s, SchemaAssociatedElement(schemaLocation, it))
        }
        redefine.groups.associateToOverride(groups,
            { QName(targetNamespace?.toString() ?: "", it.name.toString()) }
        ) { it, old ->
            val s = schemaLike.withNestedRedefine(old.first as? RedefineWrapper)
            Pair(s, SchemaAssociatedElement(schemaLocation, it))
        }
        redefine.attributeGroups.associateToOverride(attributeGroups,
            { QName(targetNamespace?.toString() ?: "", it.name.toString()) }
        ) { it, old ->
            val s = schemaLike.withNestedRedefine(old.first as? RedefineWrapper)
            Pair(s, SchemaAssociatedElement(schemaLocation, it))
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

    class RedefineWrapper(
        val base: ResolvedSchemaLike,
        val originalSchema: XSSchema,
        val originalLocation: String,
        val nestedRedefine: RedefineWrapper?
    ) : ResolvedSchemaLike() {

        override val targetNamespace: VAnyURI? get() = originalSchema.targetNamespace
        override val blockDefault: T_BlockSet get() = originalSchema.blockDefault
        override val finalDefault: Set<TypeModel.Derivation> get() = originalSchema.finalDefault ?: emptySet()
        override val defaultOpenContent: XSDefaultOpenContent? get() = originalSchema.defaultOpenContent

        override fun maybeSimpleType(typeName: QName): ResolvedGlobalSimpleType? {
            return base.maybeSimpleType(typeName)
        }

        override fun maybeType(typeName: QName): ResolvedGlobalType? {
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
            return base.maybeAttributeGroup(attributeGroupName)
        }

        override fun maybeGroup(groupName: QName): ResolvedGlobalGroup? {
            return base.maybeGroup(groupName)
        }

        override fun maybeElement(elementName: QName): ResolvedGlobalElement? {
            return base.maybeElement(elementName)
        }

        override fun maybeAttribute(attributeName: QName): ResolvedGlobalAttribute? {
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
            else -> RedefineWrapper(base, originalSchema, originalLocation, nestedRedefine)
        }
    }

    class ChameleonWrapper(val base: ResolvedSchemaLike, val chameleonNamespace: VAnyURI?) : ResolvedSchemaLike() {
        override val targetNamespace: VAnyURI?
            get() = chameleonNamespace

        override val blockDefault: T_BlockSet
            get() = base.blockDefault
        override val finalDefault: Set<TypeModel.Derivation>
            get() = base.finalDefault
        override val defaultOpenContent: XSDefaultOpenContent?
            get() = base.defaultOpenContent

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
