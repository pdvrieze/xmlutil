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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.types.VDerivationControl
import io.github.pdvrieze.formats.xmlschema.types.VFormChoice
import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.XMLConstants.XSD_NS_URI
import nl.adaptivity.xmlutil.XMLConstants.XSI_NS_URI

internal open class NamespaceHolder(val namespace: String)

internal class RecursiveRedefine(val schemaLocation: VAnyURI, namespace: String) : NamespaceHolder(namespace)

internal class SchemaData(
    namespace: String,
    val schemaLocation: String?,
    val rawSchema: XSSchema,
    val elements: Map<String, SchemaElement<XSGlobalElement>>,
    val attributes: Map<String, SchemaElement<XSGlobalAttribute>>,
    val types: Map<String, SchemaElement<XSGlobalType>>,
    val groups: Map<String, SchemaElement<XSGroup>>,
    val attributeGroups: Map<String, SchemaElement<XSAttributeGroup>>,
    val notations: Map<String, XSNotation>,
    val includedNamespaceToUris: Map<String, List<VAnyURI>>,
    val knownNested: Map<String, NamespaceHolder>,
    val importedNamespaces: Set<String>,
) : NamespaceHolder(namespace) {
    val elementFormDefault: VFormChoice? get() = rawSchema.elementFormDefault
    val attributeFormDefault: VFormChoice? get() = rawSchema.attributeFormDefault

    constructor(namespace: String, rawSchema: XSSchema) : this(
        namespace = namespace,
        rawSchema = rawSchema,
        schemaLocation = null,
        elements = emptyMap(),
        attributes = emptyMap(),
        types = emptyMap(),
        groups = emptyMap(),
        attributeGroups = emptyMap(),
        notations = emptyMap(),
        includedNamespaceToUris = emptyMap(),
        knownNested = emptyMap(),
        importedNamespaces = emptySet(),
    )

    constructor(
        namespace: String,
        schemaLocation: String,
        rawSchema: XSSchema,
        elementFormDefault: VFormChoice?,
        attributeFormDefault: VFormChoice?,
        builder: DataBuilder
    ) : this(
        namespace = namespace,
        rawSchema = rawSchema,
        schemaLocation = schemaLocation,
        elements = builder.elements,
        attributes = builder.attributes,
        types = builder.types,
        groups = builder.groups,
        attributeGroups = builder.attributeGroups,
        notations = builder.notations,
        includedNamespaceToUris = builder.includedNamespaceToUris,
        knownNested = builder.newProcessed,
        importedNamespaces = builder.importedNamespaces,
    )

    fun findElement(elementName: QName): Pair<SchemaData, SchemaElement<XSGlobalElement>> {
        if (namespace == elementName.namespaceURI) {
            elements[elementName.localPart]?.let { return Pair(this, it) }
        }

        if (elementName.namespaceURI in importedNamespaces) {
            val l = includedNamespaceToUris[elementName.namespaceURI]
            if (l != null) {
                for ((uri) in l) {
                    (knownNested[uri] as? SchemaData)?.let { n ->
                        n.elements[elementName.localPart]?.let { return Pair(n, it) }
                    }
                }
            }
        }
        throw IllegalArgumentException("No element with name $elementName found in schema")
    }

    fun findType(typeName: QName): SchemaElement<XSGlobalType>? {
        when {
            namespace == typeName.namespaceURI -> return types[typeName.localPart]
            typeName.namespaceURI in importedNamespaces -> {
                val l = includedNamespaceToUris[typeName.namespaceURI]
                if (l != null) {
                    for ((uri) in l) {
                        (knownNested[uri] as? SchemaData)?.let {
                            return it.types[typeName.localPart]
                        }
                    }
                }
                return null
            }

            else -> return null
        }
    }

    fun findComplexType(typeName: QName): SchemaElement<XSGlobalComplexType>? {
        val t = findType(typeName)
        @Suppress("UNCHECKED_CAST")
        return when {
            t?.elem is XSGlobalComplexType -> t as SchemaElement<XSGlobalComplexType>
            else -> null
        }
    }

    fun findSimpleType(typeName: QName): SchemaElement<XSGlobalSimpleType>? {
        val t = findType(typeName)
        @Suppress("UNCHECKED_CAST")
        return when {
            t?.elem is XSGlobalSimpleType -> t as SchemaElement<XSGlobalSimpleType>
            else -> null
        }
    }

    fun checkRecursiveSubstitutionGroups() {
        val verifiedHeads = mutableSetOf<QName>()

        fun followChain(
            elementName: QName,
            seen: SingleLinkedList<QName>,
            elementInfo: SchemaElement<XSGlobalElement> = findElement(elementName).second
        ) {
            val element = elementInfo.elem
            val newSeen = seen + elementName
            val sg = (element.substitutionGroup ?: run { verifiedHeads.addAll(newSeen); return })

            for (referenced in sg) {
                if (referenced !in verifiedHeads) {
                    require(referenced !in newSeen) {
                        "Recursive substitution group (${
                            newSeen.sortedBy { it.toString() }.joinToString()
                        })"
                    }
                    followChain(referenced, newSeen)
                }
            }
        }

        for ((name, childElement) in elements) {
            val ns = childElement.targetNamespace
            val qname = QName(ns, name)
            if (qname !in verifiedHeads) {
                followChain(qname, SingleLinkedList(), childElement)
            }
        }
    }


    fun checkRecursiveTypeDefinitions() {
        val verifiedSet = mutableSetOf<XSGlobalType>()
        for (typeInfo in (types.values)) {
            if (typeInfo.elem !in verifiedSet) { // skip already validated types
                val chain = mutableSetOf<XSGlobalType>()
                checkRecursiveTypes(Pair(this, typeInfo), verifiedSet, chain)
                verifiedSet.addAll(chain)
            }
        }
    }

    private fun checkRecursiveTypes(
        typeInfo: Pair<SchemaData, SchemaElement<XSGlobalType>>,
        seenTypes: MutableSet<XSGlobalType>,
        inheritanceChain: Set<XSGlobalType>,
    ) {
        val (schema, type) = typeInfo
        checkRecursiveTypes(type, schema, type.rawSchema, seenTypes, inheritanceChain)
    }

    private fun checkRecursiveTypes(
        startType: SchemaElement<XSIType>,
        schema: SchemaData,
        rawSchema: XSSchema,
        seenTypes: MutableSet<XSGlobalType>,
        inheritanceChain: Set<XSGlobalType>
    ) {
        val newInheritanceChain: Set<XSGlobalType> = when(val e = startType.elem) {
            is XSGlobalType -> {
                require(e !in inheritanceChain) {
                    "Recursive type use for ${e.name}: ${inheritanceChain.joinToString { it.name }}"
                }
                inheritanceChain + e
            }
            else -> inheritanceChain
        }

        val refs: List<QName>
        val locals: List<XSLocalType>
        when (val st = startType.elem) {
            is XSISimpleType -> {
                when (val d = st.simpleDerivation) {
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
                when (val c: XSI_ComplexContent = st.content) {
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
                val d = st.content.derivation
                refs = listOfNotNull(d.base)
                locals = listOfNotNull((d as? XSSimpleContentRestriction)?.simpleType)
            }

            else -> throw AssertionError("Unreachable")
        }
        val finalRefs = refs.asSequence()
            .filter { it.namespaceURI != XSD_NS_URI && it.namespaceURI != XMLConstants.XSI_NS_URI }
            .let {
                when {
                    rawSchema.targetNamespace.isNullOrEmpty() && schema.namespace.isNotEmpty() ->
                        it.map { n ->
                            if (n.namespaceURI.isEmpty()) QName(schema.namespace, n.localPart) else n
                        }

                    else -> it
                }
            }
            .toSet()

        for (ref in finalRefs) {
            val typeInfo = when {
                startType is SchemaElement.Redefined<*> && ref.isEquivalent(startType.elementName) ->
                    requireNotNull(startType.overriddenSchema.findType(ref)) { "Failure to find redefined type $ref" }

                else -> requireNotNull(findType(ref)) {
                    "Failure to find referenced type $ref"
                }
            }

            if (typeInfo.elem !in seenTypes) {
                checkRecursiveTypes(Pair(this, typeInfo), seenTypes, newInheritanceChain)
            }

        }

        for (local in locals) {
            checkRecursiveTypes(
                SchemaElement(local, schema.schemaLocation ?: "", rawSchema, schema.importedNamespaces),
                schema,
                rawSchema,
                seenTypes,
                inheritanceChain
            )
        }

    }

    /**
     * Collects and merges nested SchemaData objects into a mutable map. The map keys are schema locations,
     * the values are namespace/data pairs (to allow for chameleon)
     *
     * @param collector The mutable map to collect and merge the nested SchemaData objects into.
     * @return The modified mutable map containing the merged nested SchemaData objects.
     */
    internal fun <M : MutableMap<String, Pair<String, SchemaData>>> collectAndMergeNested(collector: M): M {
        collector.getOrPut(this.schemaLocation ?: "") { Pair(namespace, this) }

        for (ns in importedNamespaces) {
            val uris = includedNamespaceToUris[ns] ?: emptyList()
            for (uri in uris) {
                val data = knownNested[uri.value]
                if (data is SchemaData) data.collectAndMergeNested(collector)
            }
        }
        return collector
    }


    class DataBuilder(rootLocation: String, processed: MutableMap<String, NamespaceHolder> = mutableMapOf()) {
        val elements: MutableMap<String, SchemaElement<XSGlobalElement>> = mutableMapOf()
        val attributes: MutableMap<String, SchemaElement<XSGlobalAttribute>> = mutableMapOf()
        val types: MutableMap<String, SchemaElement<XSGlobalType>> = mutableMapOf()
        val groups: MutableMap<String, SchemaElement<XSGroup>> = mutableMapOf()
        val attributeGroups: MutableMap<String, SchemaElement<XSAttributeGroup>> = mutableMapOf()
        val notations: MutableMap<String, XSNotation> = mutableMapOf()
        val includedNamespaceToUris: MutableMap<String, MutableList<VAnyURI>> = mutableMapOf()
        val newProcessed: MutableMap<String, NamespaceHolder> = processed
        val importedNamespaces: MutableSet<String> = mutableSetOf()
        val schemaLocations: MutableSet<String> = mutableSetOf(rootLocation)

        fun addFromSchema(
            sourceSchema: XSSchema,
            schemaLocation: String,
            targetNamespace: String?,
            builtin: Boolean = false
        ) {
            val chameleon = when {
                !sourceSchema.targetNamespace.isNullOrEmpty() -> null
                targetNamespace.isNullOrEmpty() -> null //error("Invalid name override to default namespace")
                else -> targetNamespace
            }
            sourceSchema.elements.associateToUnique(elements) {
                it.name.toString() to SchemaElement.auto(
                    it,
                    schemaLocation,
                    sourceSchema,
                    chameleon,
                    importedNamespaces,
                    builtin,
                )
            }
            sourceSchema.attributes.associateToUnique(attributes) {
                it.name.toString() to SchemaElement.auto(
                    it,
                    schemaLocation,
                    sourceSchema,
                    chameleon,
                    importedNamespaces,
                    builtin,
                )
            }
            sourceSchema.simpleTypes.associateToUnique(types) {
                it.name.toString() to SchemaElement.auto(
                    it,
                    schemaLocation,
                    sourceSchema,
                    chameleon,
                    importedNamespaces,
                    builtin,
                )
            }
            sourceSchema.complexTypes.associateToUnique(types) {
                it.name.toString() to SchemaElement.auto(
                    it,
                    schemaLocation,
                    sourceSchema,
                    chameleon,
                    importedNamespaces,
                    builtin,
                )
            }
            sourceSchema.groups.associateToUnique(groups) {
                it.name.toString() to SchemaElement.auto(
                    it,
                    schemaLocation,
                    sourceSchema,
                    chameleon,
                    importedNamespaces,
                    builtin,
                )
            }
            sourceSchema.attributeGroups.associateToUnique(attributeGroups) {
                it.name.toString() to SchemaElement.auto(
                    it,
                    schemaLocation,
                    sourceSchema,
                    chameleon,
                    importedNamespaces,
                    builtin,
                )
            }
            sourceSchema.notations.associateToUnique(notations) { it.name.toString() to it }
        }

        fun addInclude(sourceData: SchemaData, targetNamespace: String?) {
            sourceData.schemaLocation?.let { schemaLocations.add(it) }

            val chameleon = when {
                sourceData.namespace.isNullOrEmpty() -> null
                targetNamespace.isNullOrEmpty() -> error("Invalid name override to default namespace")
                targetNamespace != sourceData.namespace -> targetNamespace
                else -> null
            }

            if (chameleon == null) {
                sourceData.elements.addUnique(elements)
                sourceData.groups.addUnique(groups)
                sourceData.attributes.addUnique(attributes)
                sourceData.types.addUnique(types)
                sourceData.attributeGroups.addUnique(attributeGroups)
                sourceData.notations.addUnique(notations)
            } else {
                sourceData.elements.addUnique(elements.mapValuesTo(mutableMapOf()) { (k, v) ->
                    v.toChameleon(chameleon, sourceData)
                })
                sourceData.groups.addUnique(groups.mapValuesTo(mutableMapOf()) { (k, v) ->
                    v.toChameleon(chameleon, sourceData)
                })
                sourceData.attributes.addUnique(attributes.mapValuesTo(mutableMapOf()) { (k, v) ->
                    v.toChameleon(chameleon, sourceData)
                })
                sourceData.types.addUnique(types.mapValuesTo(mutableMapOf()) { (k, v) ->
                    v.toChameleon(chameleon, sourceData)
                })
                sourceData.attributeGroups.addUnique(attributeGroups.mapValuesTo(mutableMapOf()) { (k, v) ->
                    v.toChameleon(chameleon, sourceData)
                })
                sourceData.notations.addUnique(notations)
            }
            sourceData.schemaLocation?.let { newProcessed[it] = sourceData }
            importedNamespaces.addAll(sourceData.importedNamespaces)
        }

        fun mergeUnique(toMerge: SchemaData) {
            for ((n, e) in toMerge.elements) {
                require(elements.put(n, e) == null) { "Duplicate element with name ${QName(toMerge.namespace, n)}" }
            }
            for ((n, a) in toMerge.attributes) {
                require(attributes.put(n, a) == null) { "Duplicate attribute with name ${QName(toMerge.namespace, n)}" }
            }
            for ((n, t) in toMerge.types) {
                require(types.put(n, t) == null) { "Duplicate type with name ${QName(toMerge.namespace, n)}" }
            }
            for ((n, g) in toMerge.groups) {
                require(groups.put(n, g) == null) { "Duplicate group with name ${QName(toMerge.namespace, n)}" }
            }
            for ((n, ag) in toMerge.attributeGroups) {
                require(
                    attributeGroups.put(n, ag) == null
                ) { "Duplicate attribute group with name ${QName(toMerge.namespace, n)}" }
            }
            for ((name, notation) in toMerge.notations) {
                require(
                    notations.put(name, notation) == null
                ) { "Duplicate notation with name ${QName(toMerge.namespace, name)}" }
            }

        }

    }


    companion object {

        /**
         * Create a new schema data object
         *
         * @param sourceSchema The schema to make into data
         * @param schemaLocation The uri location for this particular schema file
         * @param targetNamespace The target namespace for the file
         * @param resolver The resolver responsible for resolving the files. It is relative to the currently processed file
         * @param alreadyProcessed A map from uri's to schema data that has already been processed (somewhat)
         */
        operator fun invoke(
            sourceSchema: XSSchema,
            schemaLocations: List<String>,
            targetNamespace: String?,
            resolver: ResolvedSchema.Resolver,
            alreadyProcessed: MutableMap<String, NamespaceHolder> = mutableMapOf(),
            builtin: Boolean = false
        ): SchemaData {
            if (!builtin) {
                require(sourceSchema.targetNamespace.let { it == null || it.isNotEmpty() }) { "Empty namespaces are not allowed for schemas (${schemaLocations.last()})" }

                for (attrName in sourceSchema.otherAttrs.keys) {
                    require(attrName.namespaceURI.isNotEmpty()) {
                        "Unknown unqualified attribute on schema: ${attrName.localPart}"
                    }
                    require(attrName.namespaceURI != XSD_NS_URI) {
                        "Attributes qualified with the XMLSchema namespace are not allowed in schemas"
                    }
                }
            }

            val lastLocation = schemaLocations.last()
            val b = DataBuilder(lastLocation, alreadyProcessed)
            val ns: String = (targetNamespace?.toAnyUri() ?: sourceSchema.targetNamespace)?.value ?: ""
            b.newProcessed.put(lastLocation, NamespaceHolder(ns))

            b.addFromSchema(sourceSchema, lastLocation, targetNamespace, builtin)

            includeLoop@ for (include in sourceSchema.includes) {
                val includeLocation = resolver.resolve(include.schemaLocation)

                val includeData: SchemaData? = when {
                    includeLocation.value in b.newProcessed -> continue@includeLoop

                    includeLocation.value in alreadyProcessed -> {
                        val processed = alreadyProcessed[includeLocation.value]
                        requireNotNull(processed as? SchemaData) { "Recursive includes: $includeLocation" }
                    }

                    else -> when (val parsed = resolver.tryReadSchema(includeLocation)) {
                        null -> null
                        else -> {
                            val delegateResolver = resolver.delegate(includeLocation)
                            require(parsed.targetNamespace.let { it == null || it.value == targetNamespace })
                            SchemaData(
                                parsed,
                                schemaLocations + includeLocation.value,
                                targetNamespace,
                                delegateResolver,
                                b.newProcessed
                            ).also {
                                b.newProcessed[includeLocation.value] = it
                            }
                        }
                    }
                }
                if (includeData != null) {
                    b.addInclude(includeData, targetNamespace)
                } else if (includeLocation.value.isNotEmpty()) {
                    b.newProcessed[includeLocation.value] =
                        NamespaceHolder(targetNamespace ?: "") // add entry for this being processed
                }
            }

            for (redefine in sourceSchema.redefines) {
                require(redefine.schemaLocation.value !in b.schemaLocations) { "Redefine (indirectly) refers to itself" }
                val redefineLocation = resolver.resolve(redefine.schemaLocation)
                val redefineData: NamespaceHolder? = when(val processed = alreadyProcessed[redefineLocation.value]) {
                    is SchemaData -> processed

                    is NamespaceHolder -> {

                        require(redefineLocation.value !in schemaLocations) { "Redefines can not refer to documents themselves" }
                        RecursiveRedefine(redefineLocation, processed.namespace)
                    }

                    else -> {
                        val delegateResolver = resolver.delegate(redefineLocation)
                        when (val parsed = resolver.tryReadSchema(redefineLocation)) {
                            null -> {
                                require(redefine.groups.isEmpty()) { "Groups in unresolvable redefine $redefineLocation" }
                                require(redefine.attributeGroups.isEmpty()) { "Attribute groups in unresolvable redefine $redefineLocation" }
                                require(redefine.complexTypes.isEmpty()) { "Complex types in unresolvable redefine $redefineLocation" }
                                require(redefine.simpleTypes.isEmpty()) { "Simple types in unresolvable redefine $redefineLocation" }
                                null
                            }

                            else -> {
                                require(parsed.targetNamespace.let { it == null || it.value == targetNamespace })
                                SchemaData(
                                    parsed,
                                    listOf(redefineLocation.value),
                                    targetNamespace,
                                    delegateResolver,
                                    b.newProcessed
                                ).also {
                                    b.newProcessed[redefineLocation.value] = it
                                }
                            }
                        }
                    }
                }

                if (redefineData !is SchemaData) {
                    if (redefineLocation.value.isNotEmpty())
                        b.newProcessed[redefineLocation.value] = NamespaceHolder(targetNamespace ?: "")
                } else {
                    b.addInclude(redefineData, targetNamespace)

                    val redefinedTypeNames = mutableSetOf<String>()
                    for (st in (redefine.simpleTypes + redefine.complexTypes)) {
                        val name = st.name.xmlString
                        require(redefinedTypeNames.add(name)) { "Redefine redefines the same type multiple times" }
                        val baseType = requireNotNull(b.types[name]) { "Redefine must actually redefine type" }
                        // TODO add check for base type
                        val typeName = QName(targetNamespace ?: "", name)
                        b.types[name] =
                            SchemaElement.Redefined(st, sourceSchema, redefineData, schemaLocations.first(), typeName, Redefinable.TYPE)
                    }

                    val redefinedGroups = mutableSetOf<String>()
                    for (g in redefine.groups) {
                        val name = g.name.xmlString
                        require(redefinedGroups.add(name)) { "Redefine redefines the same group multiple times" }
                        val oldGroup = requireNotNull(b.groups[name]) { "Redefine must actually redefine group" }
                        // TODO add checks if needed
                        val groupName = QName(targetNamespace ?: "", name)
                        b.groups[name] =
                            SchemaElement.Redefined(g, sourceSchema, redefineData, schemaLocations.first(), groupName, Redefinable.GROUP)
                    }

                    val redefinedAttrGroups = mutableSetOf<String>()
                    for (ag in redefine.attributeGroups) {
                        val name = ag.name.xmlString
                        require(redefinedAttrGroups.add(name)) { "Redefine redefines the same attribute group multiple times" }
                        val oldGroup =
                            requireNotNull(b.attributeGroups[name]) { "Redefine must actually redefine attribute group" }
                        // TODO add checks if needed
                        val agName = QName(targetNamespace ?: "", name)

                        b.attributeGroups[name] = SchemaElement.Redefined(
                            ag, sourceSchema, redefineData, schemaLocations.first(),
                            agName, Redefinable.ATTRIBUTEGROUP
                        )
                    }
                }
            }

            for (import in sourceSchema.imports) {
/*
                val importNS = when (val i = import.namespace) {
                    null -> {
                        require(!targetNamespace.isNullOrEmpty()) { "4.2.6.2 1.1) Import with empty namespace inside a schema without target namespace" }

                        VAnyURI("")
                    }

                    else -> {
                        require(i.value != targetNamespace) { "4.2.6.2 1.2) Import may not match container's target namespace: $i, $targetNamespace" }
                        i
                    }
                }
                b.importedNamespaces.add(importNS.value)
*/

                val il = import.schemaLocation
                if (il == null) {
                    val ns = requireNotNull(import.namespace) { "import must specify at least namespace or location" }
                    b.includedNamespaceToUris.getOrPut(ns.value, { mutableListOf() }).let {
                        if (VAnyURI.EMPTY !in it) it.add(VAnyURI.EMPTY) // Don't duplicate "missing" uris
                    }
                    b.importedNamespaces.add(ns.value)
                } else {
                    val importLocation = resolver.resolve(il)

                    val actualImport: SchemaData? = when {
                        // imports can be delayed in parsing
                        importLocation.value in alreadyProcessed -> {
                            val existing = alreadyProcessed[importLocation.value]!!
                            val importNS = import.namespace?.value
                            require(existing.namespace.let { it.isEmpty() || importNS==null || it == importNS }) {
                                "Imported schema's namespace (${existing.namespace}) is not null and does not match specified namespace ($importNS)"
                            }
                            b.importedNamespaces.add(importNS ?: existing.namespace)
                            existing as? SchemaData
                        }

                        else -> {

                            when (val parsed = resolver.tryReadSchema(importLocation)) {
                                null -> {
                                    import.namespace?.value?.let { b.importedNamespaces.add(it) }
                                    null
                                }

                                else -> {
                                    val delegateResolver = resolver.delegate(importLocation)
                                    val actualNamespace = when (val ins = import.namespace) {
                                        null -> {
                                            if (targetNamespace.isNullOrEmpty()) requireNotNull(parsed.targetNamespace) { "Missing namespace for import" }
                                            "".toAnyUri()
                                        }

                                        else -> {
                                            require(parsed.targetNamespace == null || parsed.targetNamespace == ins) {
                                                "Imports cannot change source namespace from ${parsed.targetNamespace} to $ins"
                                            }
                                            ins
                                        }
                                    }

                                    require(parsed.targetNamespace.let { it == null || it == import.namespace }) { "import namespaces must meet requirements '$targetNamespace' ← '${parsed.targetNamespace}'" }
                                    b.importedNamespaces.add(actualNamespace.value)
                                    b.includedNamespaceToUris.getOrPut(actualNamespace.value, { mutableListOf() }).also {
                                        require(importLocation !in it) { "Duplicate import of location ${importLocation}" }
                                        it.add(importLocation)
                                    }

                                    SchemaData(
                                        parsed,
                                        listOf(importLocation.value),
                                        parsed.targetNamespace?.value,
                                        delegateResolver,
                                        b.newProcessed
                                    ).also {
                                        b.newProcessed[importLocation.value] = it
                                    }
                                }
                            }
                        }
                    }
                    if (actualImport != null) b.newProcessed[importLocation.value] = actualImport
                }
            }

            // TODO add override support

            val schemaLocation = schemaLocations.first()
            return SchemaData(
                namespace = targetNamespace ?: "",
                schemaLocation = schemaLocation,
                rawSchema = sourceSchema,
                elementFormDefault = sourceSchema.elementFormDefault,
                attributeFormDefault = sourceSchema.attributeFormDefault,
                builder = b
            ).also { b.newProcessed[schemaLocation] = it }
        }

    }
}

class OwnerWrapper internal constructor(
    base: ResolvedSchemaLike,
    val owner: XSSchema,
    val importedNamespaces: Set<String>
) : ResolvedSchemaLike() {

    val base: ResolvedSchemaLike = when (base) {
        is OwnerWrapper -> base.base
        else -> base
    }

    override val version: SchemaVersion
        get() = owner.version?.let { SchemaVersion.fromXml(it.xmlString) } ?: base.version

    override val targetNamespace: VAnyURI? get() = owner.targetNamespace

    override val attributeFormDefault: VFormChoice get() = owner.attributeFormDefault ?: VFormChoice.UNQUALIFIED

    override val elementFormDefault: VFormChoice get() = owner.elementFormDefault ?: VFormChoice.UNQUALIFIED

    override val defaultAttributes: QName? get() = base.defaultAttributes

    override val blockDefault: Set<VDerivationControl.T_BlockSetValues> get() = base.blockDefault

    override val finalDefault: Set<VDerivationControl.Type> get() = base.finalDefault

    override val defaultOpenContent: XSDefaultOpenContent? get() = base.defaultOpenContent

    private inline fun <R> checkImport(name: QName, action: () -> R): R = when (name.namespaceURI) {
        XSD_NS_URI,
        XSI_NS_URI,
        in importedNamespaces,
        targetNamespace?.value ?: "" -> action()

        else -> throw IllegalArgumentException("Namespace of ${name} is not imported into this schema")
    }


    override fun maybeSimpleType(typeName: QName): ResolvedGlobalSimpleType? = checkImport(typeName) {
        base.maybeSimpleType(typeName)
    }

    override fun maybeType(typeName: QName): ResolvedGlobalType? = checkImport(typeName) { base.maybeType(typeName) }

    override fun maybeAttributeGroup(attributeGroupName: QName): ResolvedGlobalAttributeGroup? =
        checkImport(attributeGroupName)
        { base.maybeAttributeGroup(attributeGroupName) }

    override fun maybeGroup(groupName: QName): ResolvedGlobalGroup? =
        checkImport(groupName) { base.maybeGroup(groupName) }

    override fun maybeElement(elementName: QName): ResolvedGlobalElement? =
        checkImport(elementName) { base.maybeElement(elementName) }

    override fun maybeAttribute(attributeName: QName): ResolvedGlobalAttribute? =
        checkImport(attributeName) { base.maybeAttribute(attributeName) }

    override fun maybeIdentityConstraint(constraintName: QName): ResolvedIdentityConstraint? =
        checkImport(constraintName) { base.maybeIdentityConstraint(constraintName) }

    override fun maybeNotation(notationName: QName): ResolvedNotation? = checkImport(notationName) {
        base.maybeNotation(notationName)
    }

    override fun getElements(): Set<ResolvedGlobalElement> {
        return base.getElements()
    }

    override fun substitutionGroupMembers(headName: QName): Set<ResolvedGlobalElement> = checkImport(headName) {
        base.substitutionGroupMembers(headName)
    }
}

class ChameleonWrapper internal constructor(
    val base: ResolvedSchemaLike,
    val chameleonNamespace: VAnyURI?
) : ResolvedSchemaLike() {

    override val attributeFormDefault: VFormChoice get() = base.attributeFormDefault
    override val elementFormDefault: VFormChoice get() = base.elementFormDefault

    override val version: SchemaVersion get() = base.version

    override val targetNamespace: VAnyURI?
        get() = chameleonNamespace

    override fun hasLocalTargetNamespace(): Boolean {
        return chameleonNamespace.isNullOrEmpty()
    }

    override val blockDefault: Set<VDerivationControl.T_BlockSetValues>
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

    override fun getElements(): Set<ResolvedGlobalElement> {
        return base.getElements()
    }

    override fun toString(): String {
        return "ChameleonWrapper($chameleonNamespace)"
    }


}


internal class RedefineSchema(
    val referenceSchema: ResolvedSchemaLike,
    val originSchemaData: SchemaData,
    internal val elementName: QName,
    internal val elementKind: Redefinable,
    override val blockDefault: Set<VDerivationControl.T_BlockSetValues> = emptySet(),
    override val finalDefault: Set<VDerivationControl.Type> = emptySet(),
    override val defaultOpenContent: XSDefaultOpenContent? = null,
    override val defaultAttributes: QName? = null,
) : ResolvedSchemaLike() {

    override val version: SchemaVersion get() = referenceSchema.version

    override val targetNamespace: VAnyURI? get() = originSchemaData.namespace.toAnyUri() ?: referenceSchema.targetNamespace
    private val originalNS get() = targetNamespace?.value ?: ""

    override fun hasLocalTargetNamespace(): Boolean {
        return targetNamespace.isNullOrEmpty()
    }

    override val attributeFormDefault: VFormChoice
        get() = originSchemaData.attributeFormDefault ?: VFormChoice.UNQUALIFIED
    override val elementFormDefault: VFormChoice
        get() = originSchemaData.elementFormDefault ?: VFormChoice.UNQUALIFIED

    override fun maybeSimpleType(typeName: QName): ResolvedGlobalSimpleType? {
        if (elementKind == Redefinable.TYPE && elementName == typeName) {
            return nestedSimpleType(typeName)
        }

        return referenceSchema.maybeSimpleType(typeName)
    }

    override fun maybeType(typeName: QName): ResolvedGlobalType? {
        if (elementKind == Redefinable.TYPE && elementName == typeName) {
            return nestedType(typeName)
        }

        return referenceSchema.maybeType(typeName)
    }

    fun nestedSimpleType(typeName: QName): ResolvedGlobalSimpleType {
        require(originalNS == typeName.namespaceURI)
        val t = originSchemaData.findType(typeName)
        if (t != null && t.elem is XSGlobalSimpleType) {
            return ResolvedGlobalSimpleType(t as SchemaElement<XSGlobalSimpleType>, t.effectiveSchema(referenceSchema))
        }
        error("Nested simple type with name $typeName could not be found")
    }

    fun nestedComplexType(typeName: QName): ResolvedGlobalComplexType {
        require(originalNS == typeName.namespaceURI)
        val t = originSchemaData.findComplexType(typeName) ?: error("No nested complex type with name $typeName")
        // unwrap the nesting here, so RedefineSchema is an indicator of direct redefine
        return ResolvedGlobalComplexType(t, t.effectiveSchema(referenceSchema))
    }

    fun nestedType(typeName: QName): ResolvedGlobalType {
        require(originalNS == typeName.namespaceURI)

        val t = originSchemaData.findType(typeName)
        if (t != null) {
            if (t.elem is XSGlobalComplexType) {
                return ResolvedGlobalComplexType(t.cast(), t.effectiveSchema(referenceSchema))
            } else if (t.elem is XSGlobalSimpleType) {
                return ResolvedGlobalSimpleType(t.elem, t.effectiveSchema(referenceSchema))
            }
        }
        error("No nested complex type with name $typeName")
    }

    fun nestedAttributeGroup(typeName: QName): ResolvedGlobalAttributeGroup {
        require(originalNS == typeName.namespaceURI) { "Redefine namespace mismatch. Nested ns: $originalNS, name: $typeName" }

        val localName = typeName.localPart
        val ag = originSchemaData.attributeGroups[localName] ?: error("No nested complex type with name $typeName")

        return ResolvedGlobalAttributeGroup(ag, ag.effectiveSchema(referenceSchema))
    }

    fun nestedGroup(typeName: QName): ResolvedGlobalGroup {
        require(originalNS == typeName.namespaceURI) { }
        val localName = typeName.localPart
        val g = originSchemaData.groups[localName] ?: error("No nested complex type with name $typeName")

        return ResolvedGlobalGroup(g, g.effectiveSchema(referenceSchema))
    }

    fun nestedElement(typeName: QName): ResolvedGlobalElement {
        require(originalNS == typeName.namespaceURI) { }
        val localName = typeName.localPart
        val e = originSchemaData.elements[localName] ?: error("No nested complex type with name $typeName")

        return ResolvedGlobalElement(e, referenceSchema)
    }

    fun nestedAttribute(typeName: QName): ResolvedGlobalAttribute {
        require(originalNS == typeName.namespaceURI) { }
        val localName = typeName.localPart
        val a = originSchemaData.attributes[localName] ?: error("No nested complex type with name $typeName")

        return ResolvedGlobalAttribute(a, referenceSchema)
    }

    override fun maybeAttributeGroup(attributeGroupName: QName): ResolvedGlobalAttributeGroup? {
        if (elementKind == Redefinable.ATTRIBUTEGROUP && elementName == attributeGroupName) {
            return nestedAttributeGroup(attributeGroupName)
        }
        return referenceSchema.maybeAttributeGroup(attributeGroupName)
    }

    override fun maybeGroup(groupName: QName): ResolvedGlobalGroup? {
        if (elementKind == Redefinable.GROUP && elementName == groupName) {
            return nestedGroup(groupName)
        }
        return referenceSchema.maybeGroup(groupName)
    }

    override fun maybeElement(elementName: QName): ResolvedGlobalElement? {
        if (elementKind == Redefinable.ELEMENT && this.elementName == elementName) {
            return nestedElement(elementName)
        }
        return referenceSchema.maybeElement(elementName)
    }

    override fun maybeAttribute(attributeName: QName): ResolvedGlobalAttribute? {
        if (elementKind == Redefinable.ATTRIBUTE && elementName == attributeName) {
            return nestedAttribute(attributeName)
        }
        return referenceSchema.maybeAttribute(attributeName)
    }

    override fun maybeIdentityConstraint(constraintName: QName): ResolvedIdentityConstraint? {
        return referenceSchema.maybeIdentityConstraint(constraintName)
    }

    override fun maybeNotation(notationName: QName): ResolvedNotation? {
        return referenceSchema.maybeNotation(notationName)
    }

    override fun getElements(): Set<ResolvedGlobalElement> {
        return referenceSchema.getElements()
    }

    override fun substitutionGroupMembers(headName: QName): Set<ResolvedGlobalElement> {
        return referenceSchema.substitutionGroupMembers(headName)
    }

}

internal enum class Redefinable { TYPE, ELEMENT, ATTRIBUTE, GROUP, ATTRIBUTEGROUP }

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

private inline fun <K, V, M : MutableMap<in K, in V>> Map<K, V>.addUnique(
    destination: M
): M {
    for ((key, value) in this) {
        require(key !in destination) { "Duplicate key on unique association" }
        destination.put(key, value)
    }
    return destination
}

internal sealed class SchemaElement<out T>(val elem: T, val schemaLocation: String, val rawSchema: XSSchema) {
    abstract internal val builtin: Boolean
    abstract val targetNamespace: String
    val attributeFormDefault: VFormChoice? get() = rawSchema.attributeFormDefault
    val elementFormDefault: VFormChoice? get() = rawSchema.elementFormDefault

    abstract fun effectiveSchema(schema: ResolvedSchemaLike): ResolvedSchemaLike
    abstract fun toChameleon(chameleon: String): Chameleon<T>
    fun toChameleon(chameleon: String, schemaData: SchemaData): Chameleon<T> =
        toChameleon(chameleon)

    internal abstract fun <U> wrap(value: U): SchemaElement<U>

    fun <U> wrap(function: T.() -> U): SchemaElement<U> {
        return wrap(elem.function())
    }

    fun <U> wrapEach(function: T.() -> Collection<U>): Collection<SchemaElement<U>> {
        return elem.function().map { wrap(it) }
    }

    /** The bound to T is merely to make it easier for correctness, but allow variance. */
    inline fun <reified U : @UnsafeVariance T> cast(): SchemaElement<U> {
        (elem as U) // throws if not valid
        @Suppress("UNCHECKED_CAST")
        return this as SchemaElement<U>
    }

    class Direct<out T>(
        elem: T,
        schemaLocation: String,
        rawSchema: XSSchema,
        val importedNamespaces: Set<String>,
        internal override val builtin: Boolean
    ) : SchemaElement<T>(elem, schemaLocation, rawSchema) {
        override val targetNamespace: String
            get() = rawSchema.targetNamespace?.xmlString ?: ""

        override fun effectiveSchema(schema: ResolvedSchemaLike): ResolvedSchemaLike = when {
            schema.targetNamespace != rawSchema.targetNamespace ||
                    schema.elementFormDefault != rawSchema.elementFormDefault ||
                    schema.attributeFormDefault != rawSchema.attributeFormDefault -> OwnerWrapper(
                schema,
                rawSchema,
                importedNamespaces
            )

            else -> schema
        }

        override fun toChameleon(chameleon: String): Chameleon<T> {
            return Chameleon(
                elem = elem,
                schemaLocation = schemaLocation,
                rawSchema = rawSchema,
                newNS = chameleon
            )
        }

        override fun <U> wrap(value: U): Direct<U> {
            return Direct(value, schemaLocation, rawSchema, importedNamespaces, builtin)
        }

        override fun toString(): String = "d($elem)"
    }

    class Chameleon<out T>(
        elem: T,
        schemaLocation: String,
        rawSchema: XSSchema,
        val newNS: String
    ) : SchemaElement<T>(elem, schemaLocation, rawSchema) {
        override val targetNamespace: String get() = newNS

        override val builtin: Boolean get() = false

        override fun <U> wrap(value: U): Chameleon<U> {
            return Chameleon(value, schemaLocation, rawSchema, newNS)
        }

        override fun toChameleon(chameleon: String): Chameleon<T> {
            return Chameleon(elem, schemaLocation, rawSchema, chameleon)
        }

        override fun effectiveSchema(schema: ResolvedSchemaLike): ResolvedSchemaLike = when {
            schema is ChameleonWrapper && schema.targetNamespace?.value == newNS -> schema
            else -> ChameleonWrapper(base = schema, chameleonNamespace = newNS.toAnyUri())
        }

        override fun toString(): String = "chameleon($newNS, $elem)"
    }

    class Redefined<T>(
        elem: T,
        rawSchema: XSSchema,
        val overriddenSchema: SchemaData,
        schemaLocation: String,
        val elementName: QName,
        val elementKind: Redefinable,
    ) : SchemaElement<T>(elem, schemaLocation, rawSchema) {
        override val targetNamespace: String
            get() = elementName.namespaceURI

        override val builtin: Boolean get() = false

        override fun effectiveSchema(schema: ResolvedSchemaLike): ResolvedSchemaLike = when {
            // handle the case where it is called multiple times
            schema is RedefineSchema && schema.originSchemaData == overriddenSchema -> schema
            else -> RedefineSchema(schema, overriddenSchema, elementName, elementKind)
        }

        override fun <U> wrap(value: U): SchemaElement<U> {
            return Redefined(value, rawSchema, overriddenSchema, schemaLocation, elementName, elementKind)
        }

        override fun toChameleon(
            chameleon: String
        ): Chameleon<T> {
            throw UnsupportedOperationException("Redefined elements can not be chameleons")
        }

        override fun toString(): String = "redefine($elem)"
    }

    companion object {
        inline operator fun <T> invoke(
            elem: T,
            schemaLocation: String,
            rawSchema: XSSchema,
            importedNamespaces: Set<String>,
            builtin: Boolean = false
        ): Direct<T> = Direct(elem, schemaLocation, rawSchema, importedNamespaces, builtin)

        inline fun <T> auto(
            elem: T,
            schemaLocation: String,
            baseSchema: XSSchema,
            chameleonNs: String?,
            importedNamespaces: Set<String>,
            builtin: Boolean = false
        ): SchemaElement<T> = when (chameleonNs) {
            null -> Direct(elem, schemaLocation, baseSchema, importedNamespaces, builtin)
            else -> Chameleon(elem, schemaLocation, baseSchema, chameleonNs)
        }
    }
}

internal data class SchemaAssociatedElement<T>(val schemaLocation: String, val element: T)
