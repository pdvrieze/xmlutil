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
    includedUrls: SingleLinkedList<VAnyURI> = SingleLinkedList(resolver.baseUri)
) {
    val elements: MutableMap<QName, Pair<ResolvedSchemaLike, XSElement>> = mutableMapOf()
    val attributes: MutableMap<QName, Pair<ResolvedSchemaLike, XSGlobalAttribute>> = mutableMapOf()
    val simpleTypes: MutableMap<QName, Pair<ResolvedSchemaLike, XSGlobalSimpleType>> = mutableMapOf()
    val complexTypes: MutableMap<QName, Pair<ResolvedSchemaLike, XSGlobalComplexType>> = mutableMapOf()
    val groups: MutableMap<QName, Pair<ResolvedSchemaLike, XSGroup>> = mutableMapOf()
    val attributeGroups: MutableMap<QName, Pair<ResolvedSchemaLike, XSAttributeGroup>> = mutableMapOf()

    init {
        addToCollation(baseSchema, schemaLike)

        // TODO this may need more indirection to ensure scoping

        for (import in baseSchema.imports) {
            val importedLocation = import.schemaLocation
            if (importedLocation != null && importedLocation !in includedUrls) { // Avoid recursion in collations
                val relativeResolver = resolver.delegate(importedLocation)
                var rawImport = resolver.readSchema(importedLocation)

                val importNamespace = import.namespace
                val importTargetNamespace = rawImport.targetNamespace
                val chameleonSchema = when {
                    importNamespace==null -> ChameleonWrapper(schemaLike, importTargetNamespace)
                    importTargetNamespace.isNullOrEmpty() -> ChameleonWrapper(schemaLike, importNamespace)
                    else -> {
                        require(importNamespace == importTargetNamespace) {
                            "Renaming can only be done with an import with a null targetNamespace"
                        }
                        ChameleonWrapper(schemaLike, importTargetNamespace)
                    }
                }

                val collatedImport = CollatedSchema(
                    rawImport, relativeResolver, chameleonSchema, includedUrls + relativeResolver.resolve(importedLocation)
                )

                addToCollation(collatedImport)
            }
        }

        for (include in baseSchema.includes) {
            val includedLocation = include.schemaLocation
            if (includedLocation !in includedUrls) { // Avoid recursion in collations
                val relativeResolver = resolver.delegate(includedLocation)
                val rawInclude = resolver.readSchema(includedLocation)

                val importNamespace = baseSchema.targetNamespace
                val importTargetNamespace = rawInclude.targetNamespace
                val chameleonNamespace = when {
                    importNamespace == null -> importTargetNamespace
                    importTargetNamespace == null -> importNamespace

                    else -> {
                        require(importNamespace == importTargetNamespace) {
                            "Renaming can only be done with an import with a null targetNamespace ($importNamespace != $importTargetNamespace)"
                        }
                        importNamespace
                    }
                }
                val chameleonSchema = ChameleonWrapper(schemaLike, chameleonNamespace)

                val includedSchema = CollatedSchema(
                    rawInclude,
                    relativeResolver,
                    chameleonSchema,
                    includedUrls + relativeResolver.resolve(
                        includedLocation
                    )
                )
                addToCollation(includedSchema)
            }
        }

        for (redefine in baseSchema.redefines) {
            val relativeResolver = resolver.delegate(redefine.schemaLocation)
            val nestedSchema = resolver.readSchema(redefine.schemaLocation)

            val nestedSchemaLike = RedefineWrapper(schemaLike, nestedSchema)

            val collatedSchema = CollatedSchema(nestedSchema, relativeResolver, nestedSchemaLike)

            collatedSchema.applyRedefines(redefine, baseSchema.targetNamespace, nestedSchemaLike)

            addToCollation(collatedSchema)
        }

    }

    fun applyRedefines(
        redefine: XSRedefine,
        targetNamespace: VAnyURI?,
        schemaLike: ResolvedSchemaLike,
    ) {
        redefine.simpleTypes.associateToOverride(simpleTypes) {
            it.name.toQname(targetNamespace) to Pair(
                schemaLike,
                it
            )
        }
        redefine.complexTypes.associateToOverride(complexTypes) {
            it.name.toQname(targetNamespace) to Pair(
                schemaLike,
                it
            )
        }
        redefine.groups.associateToOverride(groups) { it.name.toQname(targetNamespace) to Pair(schemaLike, it) }
        redefine.attributeGroups.associateToOverride(attributeGroups) {
            it.name.toQname(targetNamespace) to Pair(
                schemaLike,
                it
            )
        }
    }

    private fun addToCollation(sourceSchema: XSSchema, schemaLike: ResolvedSchemaLike) {
        val targetNamespace = sourceSchema.targetNamespace
        sourceSchema.elements.associateToUnique(elements) { it.name.toQname(targetNamespace) to Pair(schemaLike, it) }
        sourceSchema.attributes.associateToUnique(attributes) {
            it.name.toQname(targetNamespace) to Pair(
                schemaLike,
                it
            )
        }
        sourceSchema.simpleTypes.associateToUnique(simpleTypes) {
            it.name.toQname(targetNamespace) to Pair(
                schemaLike,
                it
            )
        }
        sourceSchema.complexTypes.associateToUnique(complexTypes) {
            it.name.toQname(targetNamespace) to Pair(
                schemaLike,
                it
            )
        }
        sourceSchema.groups.associateToUnique(groups) { it.name.toQname(targetNamespace) to Pair(schemaLike, it) }
        sourceSchema.attributeGroups.associateToUnique(attributeGroups) {
            it.name.toQname(targetNamespace) to Pair(
                schemaLike,
                it
            )
        }
    }

    private fun addToCollation(sourceSchema: CollatedSchema) {
        sourceSchema.elements.entries.associateToUnique(elements)
        sourceSchema.attributes.entries.associateToUnique(attributes)
        sourceSchema.simpleTypes.entries.associateToUnique(simpleTypes)
        sourceSchema.complexTypes.entries.associateToUnique(complexTypes)
        sourceSchema.groups.entries.associateToUnique(groups)
        sourceSchema.attributeGroups.entries.associateToUnique(attributeGroups)
    }

    class RedefineWrapper(val base: ResolvedSchemaLike, val relativeBase: XSSchema) : ResolvedSchemaLike() {
        override val targetNamespace: VAnyURI?
            get() = relativeBase.targetNamespace
        override val elements: List<ResolvedGlobalElement>
            get() = base.elements
        override val attributes: List<ResolvedGlobalAttribute>
            get() = base.attributes
        override val simpleTypes: List<ResolvedGlobalSimpleType>
            get() = base.simpleTypes
        override val complexTypes: List<ResolvedGlobalComplexType>
            get() = base.complexTypes
        override val groups: List<ResolvedToplevelGroup>
            get() = base.groups
        override val attributeGroups: List<ResolvedToplevelAttributeGroup>
            get() = base.attributeGroups
        override val blockDefault: T_BlockSet
            get() = relativeBase.blockDefault
        override val finalDefault: Set<TypeModel.Derivation>
            get() = relativeBase.finalDefault ?: emptySet()
        override val defaultOpenContent: XSDefaultOpenContent?
            get() = relativeBase.defaultOpenContent
    }

    class ChameleonWrapper(val base: ResolvedSchemaLike, val chameleonNamespace: VAnyURI?) : ResolvedSchemaLike() {
        override val targetNamespace: VAnyURI?
            get() = chameleonNamespace
        override val elements: List<ResolvedGlobalElement>
            get() = base.elements
        override val attributes: List<ResolvedGlobalAttribute>
            get() = base.attributes
        override val simpleTypes: List<ResolvedGlobalSimpleType>
            get() = base.simpleTypes
        override val complexTypes: List<ResolvedGlobalComplexType>
            get() = base.complexTypes
        override val groups: List<ResolvedToplevelGroup>
            get() = base.groups
        override val attributeGroups: List<ResolvedToplevelAttributeGroup>
            get() = base.attributeGroups
        override val blockDefault: T_BlockSet
            get() = base.blockDefault
        override val finalDefault: Set<TypeModel.Derivation>
            get() = base.finalDefault ?: emptySet()
        override val defaultOpenContent: XSDefaultOpenContent?
            get() = base.defaultOpenContent

        private fun QName.extend(): QName {
            return when {
                namespaceURI.isEmpty() -> QName(chameleonNamespace?.value ?: "", localPart, prefix)
                else -> this
            }
        }

        override fun maybeSimpleType(typeName: QName): ResolvedGlobalSimpleType? {
            return super.maybeSimpleType(typeName.extend())
        }

        override fun maybeType(typeName: QName): ResolvedGlobalType? {
            return super.maybeType(typeName.extend())
        }

        override fun maybeAttribute(attributeName: QName): ResolvedGlobalAttribute? {
            return super.maybeAttribute(attributeName.extend())
        }

        override fun maybeAttributeGroup(attributeGroupName: QName): ResolvedToplevelAttributeGroup? {
            return super.maybeAttributeGroup(attributeGroupName.extend())
        }

        override fun maybeGroup(groupName: QName): ResolvedToplevelGroup? {
            return super.maybeGroup(groupName.extend())
        }

        override fun maybeElement(elementName: QName): ResolvedGlobalElement? {
            return super.maybeElement(elementName.extend())
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
                        throw IllegalArgumentException("Duplicate key on unique association")
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

        private inline fun <T, V, K, M : MutableMap<in K, in V>> Iterable<T>.associateToOverride(
            destination: M,
            transform: (T) -> Pair<K, V>
        ): M {
            for (element in this) {
                val (key, value) = transform(element)

                require(key in destination) { "Duplicate key on unique association" }
                destination.put(key, value)
            }
            return destination
        }

    }

}
