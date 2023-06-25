/*
 * Copyright (c) 2022.
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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.types.*
import nl.adaptivity.xmlutil.QName

class ResolvedInclude(
    override val rawPart: XSInclude,
    override val schema: ResolvedSchemaLike,
    resolver: ResolvedSchema.Resolver
) : ResolvedSchemaLike(), ResolvedPart, T_Include {

    val nestedSchema: XSSchema = resolver.readSchema(rawPart.schemaLocation)

    override val schemaLocation: VAnyURI
        get() = rawPart.schemaLocation

    override val blockDefault: T_BlockSet
        get() = schema.blockDefault // TODO maybe correct, maybe not

    override val finalDefault: T_FullDerivationSet
        get() = schema.finalDefault // TODO maybe correct, maybe not

    override val elements: List<ResolvedGlobalElement>

    override val attributes: List<ResolvedGlobalAttribute>

    override val simpleTypes: List<ResolvedGlobalSimpleType>

    override val complexTypes: List<ResolvedGlobalComplexType>

    override val groups: List<ResolvedToplevelGroup>

    override val attributeGroups: List<ResolvedToplevelAttributeGroup>

    override val targetNamespace: VAnyURI?
        get() = schema.targetNamespace

    init {
        require (nestedSchema.targetNamespace != schema.targetNamespace)
        val collatedSchema = CollatedSchema(nestedSchema, resolver.delegate(rawPart.schemaLocation))

        elements = DelegateList(collatedSchema.elements.values.toList()) {
            ResolvedGlobalElement(it, this)
        }
        attributes = DelegateList(collatedSchema.attributes.values.toList()) {
            ResolvedGlobalAttribute(it, this)
        }
        simpleTypes = DelegateList(collatedSchema.simpleTypes.values.toList()) {
            ResolvedGlobalSimpleType(it, this)
        }
        complexTypes = DelegateList(collatedSchema.complexTypes.values.toList()) {
            ResolvedGlobalComplexType(it, this)
        }
        groups = DelegateList(collatedSchema.groups.values.toList()) {
            ResolvedToplevelGroup(it, this)
        }
        attributeGroups = DelegateList(collatedSchema.attributeGroups.values.toList()) {
            ResolvedToplevelAttributeGroup(it, this)
        }

    }

    override val id: VID?
        get() = rawPart.id

    override val annotation: XSAnnotation?
        get() = rawPart.annotation

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override fun check() {
        super<ResolvedSchemaLike>.check()
    }
}

class ResolvedRedefine(
    override val rawPart: XSRedefine,
    override val schema: ResolvedSchemaLike,
    resolver: ResolvedSchema.Resolver
) : ResolvedSchemaLike(), ResolvedPart, T_Redefine {

    val nestedSchema: XSSchema = resolver.readSchema(rawPart.schemaLocation)

    override val schemaLocation: VAnyURI
        get() = rawPart.schemaLocation

    override val blockDefault: T_BlockSet
        get() = schema.blockDefault // TODO maybe correct, maybe not

    override val finalDefault: T_FullDerivationSet
        get() = schema.finalDefault // TODO maybe correct, maybe not

    override val elements: List<ResolvedGlobalElement>

    override val attributes: List<ResolvedGlobalAttribute>

    override val simpleTypes: List<ResolvedGlobalSimpleType>

    override val complexTypes: List<ResolvedGlobalComplexType>

    override val groups: List<ResolvedToplevelGroup>

    override val attributeGroups: List<ResolvedToplevelAttributeGroup>

    override val targetNamespace: VAnyURI?
        get() = schema.targetNamespace

    init {
        val collatedSchema = CollatedSchema(nestedSchema, resolver.delegate(rawPart.schemaLocation))

        collatedSchema.applyRedefines(rawPart, targetNamespace)

        elements = DelegateList(collatedSchema.elements.values.toList()) {
            ResolvedGlobalElement(it, this)
        }
        attributes = DelegateList(collatedSchema.attributes.values.toList()) {
            ResolvedGlobalAttribute(it, this)
        }
        simpleTypes = DelegateList(collatedSchema.simpleTypes.values.toList()) {
            ResolvedGlobalSimpleType(it, this,)
        }
        complexTypes = DelegateList(collatedSchema.complexTypes.values.toList()) {
            ResolvedGlobalComplexType(it, this)
        }
        groups = DelegateList(collatedSchema.groups.values.toList()) {
            ResolvedToplevelGroup(it, this)
        }
        attributeGroups = DelegateList(collatedSchema.attributeGroups.values.toList()) {
            ResolvedToplevelAttributeGroup(it, this)
        }

    }

    override val id: VID?
        get() = rawPart.id

    override val annotation: XSAnnotation?
        get() = rawPart.annotation

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override fun check() {
        super<ResolvedSchemaLike>.check()
    }
}

private class CollatedSchema(baseSchema: XSSchema, resolver: ResolvedSchema.Resolver) {
    val elements: MutableMap<QName, XSElement> = mutableMapOf()
    val attributes: MutableMap<QName, XSGlobalAttribute> = mutableMapOf()
    val simpleTypes: MutableMap<QName, XSGlobalSimpleType> = mutableMapOf()
    val complexTypes: MutableMap<QName, XSGlobalComplexType> = mutableMapOf()
    val groups: MutableMap<QName, XSGroup> = mutableMapOf()
    val attributeGroups: MutableMap<QName, XSAttributeGroup> = mutableMapOf()

    init {
        addToCollation(baseSchema)

        // TODO this may need more indirection to ensure scoping

        for (include in baseSchema.includes) {
            val relativeResolver = resolver.delegate(include.schemaLocation)
            val includedSchema = CollatedSchema(resolver.readSchema(include.schemaLocation), relativeResolver)
            addToCollation(includedSchema)
        }

        for (redefine in baseSchema.redefines) {
            val relativeResolver = resolver.delegate(redefine.schemaLocation)
            val rawSchema = resolver.readSchema(redefine.schemaLocation)
            val includedSchema = CollatedSchema(rawSchema, relativeResolver)

            includedSchema.applyRedefines(redefine, rawSchema.targetNamespace)

            addToCollation(includedSchema)
        }

    }

    fun applyRedefines(
        redefine: XSRedefine,
        targetNamespace: VAnyURI?
    ) {
        redefine.simpleTypes.associateByToOverride(simpleTypes) { it.name.toQname(targetNamespace) }
        redefine.complexTypes.associateByToOverride(complexTypes) { it.name.toQname(targetNamespace) }
        redefine.groups.associateByToOverride(groups) { it.name.toQname(targetNamespace) }
        redefine.attributeGroups.associateByToOverride(attributeGroups) { it.name.toQname(targetNamespace) }
    }

    private fun addToCollation(sourceSchema: XSSchema) {
        val targetNamespace = sourceSchema.targetNamespace

        sourceSchema.elements.associateByToUnique(elements) { it.name.toQname(targetNamespace) }
        sourceSchema.attributes.associateByToUnique(attributes) { it.name.toQname(targetNamespace) }
        sourceSchema.simpleTypes.associateByToUnique(simpleTypes) { it.name.toQname(targetNamespace) }
        sourceSchema.complexTypes.associateByToUnique(complexTypes) { it.name.toQname(targetNamespace) }
        sourceSchema.groups.associateByToUnique(groups) { it.name.toQname(targetNamespace) }
        sourceSchema.attributeGroups.associateByToUnique(attributeGroups) { it.name.toQname(targetNamespace) }
    }

    private fun addToCollation(sourceSchema: CollatedSchema) {
        sourceSchema.elements.entries.associateToUnique(elements)
        sourceSchema.attributes.entries.associateToUnique(attributes)
        sourceSchema.simpleTypes.entries.associateToUnique(simpleTypes)
        sourceSchema.complexTypes.entries.associateToUnique(complexTypes)
        sourceSchema.groups.entries.associateToUnique(groups)
        sourceSchema.attributeGroups.entries.associateToUnique(attributeGroups)
    }

}

private fun VNCName.toQName(schema: XSSchema): QName {
    return toQname(schema.targetNamespace)
}

private inline fun <T, K, M : MutableMap<in K, in T>> Iterable<Map.Entry<K, T>>.associateToUnique(
    destination: M
): M {
    for (element in this) {
        val (key, value) = element
        if (key in destination) throw IllegalArgumentException("Duplicate key on unique association")
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
