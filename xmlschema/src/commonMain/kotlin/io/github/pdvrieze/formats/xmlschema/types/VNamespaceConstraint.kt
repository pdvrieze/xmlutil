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

package io.github.pdvrieze.formats.xmlschema.types

import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.toAnyUri
import io.github.pdvrieze.formats.xmlschema.resolved.ContextT
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchemaLike
import io.github.pdvrieze.formats.xmlschema.resolved.SchemaVersion
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.namespaceURI

data class VNamespaceConstraint<E : VQNameListBase.IElem>(
    val mdlVariety: Variety,
    val namespaces: Set<VAnyURI>,
    val disallowedNames: VQNameListBase<E>,
) {

    init {
        when (mdlVariety) {
            Variety.NOT -> {
                require(namespaces.isNotEmpty()) { "3.10.6.1(2) - not constraint must have at least 1 member" }
                for (n in disallowedNames) {
                    if (n is VQNameListBase.Name) {
                        val ns = n.qName.namespaceURI.takeIf { it.isNotEmpty() }?.let { it.toAnyUri() }
                        require(ns !in namespaces) { "3.10.6.1(4) - disallowed names must not already be disallowed by namespaces list" }
                    }
                }
            }

            Variety.ANY -> require(namespaces.isEmpty()) { "3.10.6.1(3) - Any constraint has no members" }
            Variety.ENUMERATION -> {
                for (n in disallowedNames) {
                    if (n is VQNameListBase.Name) {
                        val ns = n.qName.namespaceURI.takeIf { it.isNotEmpty() }?.let { it.toAnyUri() }
                        require(ns in namespaces) { "3.10.6.1(4) - disallowed names must be included in the wildcard of namespaces" }
                    }
                }
            }
        }
    }

    fun matches(elem: E, context: ContextT, schema: ResolvedSchemaLike): Boolean = when (elem) {
        is VQNameListBase.Name -> matches(elem.qName, context, schema)
        else -> elem !in disallowedNames
    }

    fun matches(name: QName, context: ContextT, schema: ResolvedSchemaLike): Boolean = when (mdlVariety) {
        Variety.ANY -> !disallowedNames.contains(name, context, schema)

        Variety.ENUMERATION -> name.namespaceURI.toAnyUri() in namespaces &&
                !disallowedNames.contains(name, context, schema)

        Variety.NOT -> name.namespaceURI.isNotEmpty() && name.namespaceURI.toAnyUri() !in namespaces &&
                !disallowedNames.contains(name, context, schema)
    }

    /**
     * Determine whether there is any overlap between the two constraints (the intersection is not empty)
     * 3.10.6.4
     */
    fun intersects(other: VNamespaceConstraint<E>): Boolean = when (mdlVariety) {
        Variety.ANY -> when (other.mdlVariety) {
            Variety.ANY,
            Variety.ENUMERATION -> other.namespaces.isNotEmpty()

            Variety.NOT -> ! other.namespaces.containsAll(namespaces)
        }

        Variety.ENUMERATION -> namespaces.isNotEmpty() && when (other.mdlVariety) {
            Variety.ANY -> true
            Variety.ENUMERATION -> other.namespaces.toSet().let { ons -> namespaces.any { it in ons } }
            Variety.NOT -> namespaces.isNotEmpty() && other.namespaces.toSet().let { ons -> ! namespaces.all { it in ons } }
        }

        Variety.NOT -> when (other.mdlVariety) {
            Variety.ANY,
            Variety.NOT -> true

            Variety.ENUMERATION -> other.namespaces.isNotEmpty() && namespaces.toSet().let { ns -> ! other.namespaces.all { it in ns } }
        }
    }

    fun isSubsetOf(sup: VNamespaceConstraint<*>, schemaVersion: SchemaVersion): Boolean = when(schemaVersion) {
        SchemaVersion.V1_0 -> isSubsetOf10(sup)
        else -> isSubsetOf11(sup)
    }

    fun isSubsetOf10(sup: VNamespaceConstraint<*>): Boolean {
        return when (sup.mdlVariety) {
            Variety.ANY -> true // 3.10.6 (1)

            Variety.NOT -> when (mdlVariety) {
                // allow NOT(<something>) to be a subset of NOT('') - MS-Wildcards/WildZ013{a-g}
                Variety.NOT -> sup.namespaces.single().let { it.isEmpty() || it == namespaces.single() }// 3.10.6 (2)

                Variety.ENUMERATION ->
                    sup.namespaces.single() !in namespaces && VAnyURI.EMPTY !in namespaces

                else -> false
            }

            Variety.ENUMERATION -> when (mdlVariety) {
                Variety.ENUMERATION -> sup.namespaces.containsAll(namespaces) // 3.10.6.2(2)
                else -> false
            }
        }
    }

    fun isSubsetOf11(sup: VNamespaceConstraint<*>): Boolean {
        return when (sup.mdlVariety) {
            Variety.ANY -> true // 3.10.6.2(1)

            Variety.NOT -> when (mdlVariety) {
                Variety.NOT -> namespaces.containsAll(sup.namespaces) // 3.10.6.2(4)

                Variety.ENUMERATION -> sup.namespaces.none { it in namespaces }

                Variety.ANY -> false
            }

            Variety.ENUMERATION -> when (mdlVariety) {
                Variety.ENUMERATION -> sup.namespaces.containsAll(namespaces) // 3.10.6.2(2)
                else -> false
            }
        }
    }

    /**
     * Determine whether this namespace contains all values in the other namespace.
     * 3.10.6.2
     */
    fun isSupersetOf(sub: VNamespaceConstraint<E>): Boolean {
        when (mdlVariety) {
            Variety.ANY -> {} // main constraint matches
            Variety.ENUMERATION -> when (sub.mdlVariety) {
                Variety.ENUMERATION -> if (!namespaces.toSet()
                        .let { ns -> sub.namespaces.all { it in ns } }
                ) return false

                Variety.ANY,
                Variety.NOT -> return false
            }

            Variety.NOT -> when (sub.mdlVariety) {
                Variety.ANY -> return false
                Variety.ENUMERATION -> if (!namespaces.toSet()
                        .let { ns -> sub.namespaces.none { it in ns } }
                ) return false

                Variety.NOT -> if (!sub.namespaces.toSet().let { ons -> namespaces.all { it in ons } }) return false
            }
        }

        // check for second rule that sub doesn't allow any names forbidden by super
        for (n in disallowedNames) {
            when (n) {
                VQNameListBase.DEFINEDSIBLING,
                VQNameListBase.DEFINED -> if (n !in sub.disallowedNames.values) return false

                is VQNameListBase.Name -> {
                    val ns = n.qName.namespaceURI.takeIf { it.isNotEmpty() }?.let { it.toAnyUri() }
                    if (n !in sub.disallowedNames.values) { // not explicitly disallowed
                        if (sub.mdlVariety == Variety.ENUMERATION) {
                            if (ns in sub.namespaces) return false
                        } else {
                            if (ns !in sub.namespaces) return false
                        }
                    }
                }
            }
        }
        return true
    }

    // v1.1 -> 3.10.6.4
    fun intersection(other: VNamespaceConstraint<E>, schemaVersion: SchemaVersion): VNamespaceConstraint<E> =
        when (schemaVersion) {
            SchemaVersion.V1_0 -> intersection10(other)
            else -> intersection11(other)
        }

    fun intersection10(other: VNamespaceConstraint<E>): VNamespaceConstraint<E> {
        val newDisallowed: VQNameListBase<E> = disallowedNames.union(other.disallowedNames)

        // 3.10.6 -> E1-10
        return when (mdlVariety) {
            Variety.ANY -> VNamespaceConstraint(other.mdlVariety, other.namespaces, newDisallowed) // 2
            Variety.ENUMERATION -> when (other.mdlVariety) {
                Variety.ANY -> VNamespaceConstraint(mdlVariety, namespaces, newDisallowed) // 2
                Variety.ENUMERATION ->
                    VNamespaceConstraint(Variety.ENUMERATION, namespaces.intersect(other.namespaces), newDisallowed)

                Variety.NOT -> {
                    val newNamespaces = namespaces.toMutableSet().apply {
                        remove(other.namespaces.single())
                        remove(VAnyURI.EMPTY) // per the specification
                    }
                    require(newNamespaces.isNotEmpty()) { "Intersection of namespace constraints is empty" }
                    VNamespaceConstraint(Variety.ENUMERATION, newNamespaces, newDisallowed)
                }
            }

            Variety.NOT -> when (other.mdlVariety) {
                Variety.ANY ->
                    VNamespaceConstraint(Variety.NOT, namespaces, newDisallowed) //2

                Variety.NOT -> {
                    val n1 = namespaces.single()
                    val n2 = other.namespaces.single()
                    when {
                        n1.isEmpty() -> VNamespaceConstraint(Variety.NOT, other.namespaces, newDisallowed)
                        n2.isEmpty() -> VNamespaceConstraint(Variety.NOT, namespaces, newDisallowed)
                        n1 == n2 -> VNamespaceConstraint(Variety.NOT, namespaces, newDisallowed)
                        else -> throw IllegalArgumentException("Intersection between different not values NOT($n1) and NOT($n2) is not expressible in v1.0")
                    }
                }

                Variety.ENUMERATION -> {
                    val newNamespaces = other.namespaces.toMutableSet().apply {
                        remove(namespaces.single())
                        remove(VAnyURI.EMPTY)
                    }
                    require(newNamespaces.isNotEmpty()) { "Intersection of namespace constraints is empty" }
                    VNamespaceConstraint(Variety.ENUMERATION, newNamespaces, newDisallowed)
                }
            }
        }
    }

    // v1.1 -> 3.10.6.4
    fun intersection11(other: VNamespaceConstraint<E>): VNamespaceConstraint<E> {
        val newDisallowed: VQNameListBase<E> = disallowedNames.union(other.disallowedNames)

        return when (mdlVariety) {
            Variety.ANY -> VNamespaceConstraint(other.mdlVariety, other.namespaces, newDisallowed)
            Variety.ENUMERATION -> when (other.mdlVariety) {
                Variety.ANY -> VNamespaceConstraint(mdlVariety, namespaces, newDisallowed)
                Variety.ENUMERATION ->
                    VNamespaceConstraint(Variety.ENUMERATION, namespaces.intersect(other.namespaces), newDisallowed)
                Variety.NOT -> {
                    val newNamespaces = namespaces.toMutableSet().apply { removeAll(other.namespaces) }
                    require(newNamespaces.isNotEmpty()) { "Intersection of namespace constraints is empty" }
                    VNamespaceConstraint(Variety.ENUMERATION, newNamespaces, newDisallowed)
                }
            }
            Variety.NOT -> when (other.mdlVariety) {
                Variety.ANY ->
                    VNamespaceConstraint(Variety.NOT, namespaces, newDisallowed)

                Variety.NOT ->
                    VNamespaceConstraint(Variety.NOT, namespaces.union(other.namespaces), newDisallowed)

                Variety.ENUMERATION -> {
                    val newNamespaces = other.namespaces.toMutableSet().apply { removeAll(namespaces) }
                    require(newNamespaces.isNotEmpty()) { "Intersection of namespace constraints is empty" }
                    VNamespaceConstraint(Variety.ENUMERATION, newNamespaces, newDisallowed)
                }
            }
        }
    }

    /* Determined by 3.10.6.3 (for attributes) */
    fun union(
        other: VNamespaceConstraint<E>,
        context: ContextT,
        schema: ResolvedSchemaLike
    ): VNamespaceConstraint<E> {
        // TODO resolve the "special" values
        val newDisallowed: VQNameListBase<E> = unionNewDisallowed(other, context, schema)


        // v1.0 - 3.10.6
        // v1.1 - 3.10.6.3
        // * -> part 2
        if (mdlVariety == Variety.ANY || other.mdlVariety == Variety.ANY) {
            return VNamespaceConstraint(Variety.ANY, emptySet(), newDisallowed)
        }
        // * -> part 1
        if (mdlVariety == other.mdlVariety && namespaces.isContentEqual(other.namespaces)) return this

        // * -> part 3
        if (mdlVariety == Variety.ENUMERATION && other.mdlVariety == Variety.ENUMERATION) {
            return VNamespaceConstraint(Variety.ENUMERATION, namespaces.union(other.namespaces), newDisallowed)
        }
        // 1.1 -> part 4
        if (mdlVariety == Variety.NOT && other.mdlVariety == Variety.NOT) {
            if (schema.version == SchemaVersion.V1_0) {
                if (namespaces.single() != other.namespaces.single()) {
                    return VNamespaceConstraint(Variety.NOT, setOf(VAnyURI.EMPTY), newDisallowed)
                }
                throw UnsupportedOperationException("Union of two equal nots should not reach here")
            } else {
                val intersection = namespaces.toMutableSet().apply { retainAll(other.namespaces) }
                return when {
                    // 1.1 -> part 4.1 ; 1.0 -> part 4
                    intersection.isEmpty() -> VNamespaceConstraint(Variety.ANY, emptySet(), newDisallowed)
                    // 4.2
                    else -> VNamespaceConstraint(Variety.NOT, intersection, newDisallowed)
                }
            }
        }

        if (schema.version == SchemaVersion.V1_0) {
            // 1.1 -> part 5
            // Case 5 must be true (as they are unequal)
            val notNs: VAnyURI?
            val enumNs: Set<VAnyURI?>
            if (mdlVariety == Variety.NOT) {
                notNs = namespaces.single() // in 1.0 only one negative is allowed
                enumNs = other.namespaces
            } else {
                notNs = other.namespaces.single() // in 1.0 only one negative is allowed
                enumNs = namespaces
            }

            val containsAbsent = enumNs.contains(null) || enumNs.contains(VAnyURI.EMPTY)
            if (! notNs.isNullOrEmpty()) { // part 5
                when {
                    // v1.0 -> part 5.1
                    enumNs.contains(notNs) && containsAbsent ->
                        return VNamespaceConstraint(Variety.ANY, emptySet(), newDisallowed)

                    // v1.0 -> part 5.2
                    enumNs.contains(notNs) && !containsAbsent ->
                        return VNamespaceConstraint(Variety.NOT, setOf(VAnyURI.EMPTY), newDisallowed)

                    // v1.0 -> part 5.3
                    containsAbsent && !enumNs.contains(notNs) ->
                        throw IllegalArgumentException("Union between $this and $other not expressable in version 1.0")

                    else -> return VNamespaceConstraint(Variety.NOT, setOf(notNs), newDisallowed)

                }

            } else { // part 6
                when {
                    containsAbsent -> return VNamespaceConstraint(Variety.ANY, emptySet(), newDisallowed)
                    else -> return VNamespaceConstraint(Variety.NOT, setOf(VAnyURI.EMPTY), newDisallowed)
                }
            }
        } else {
            // 1.1 -> part 5
            // Case 5 must be true (as they are unequal)
            val notNs: Set<VAnyURI?>
            val enumNs: Set<VAnyURI?>
            if (mdlVariety == Variety.NOT) {
                notNs = namespaces
                enumNs = other.namespaces
            } else {
                notNs = other.namespaces
                enumNs = namespaces
            }

            val newNs = notNs.toMutableSet().apply { removeAll(enumNs) }
            return when {
                newNs.isEmpty() -> VNamespaceConstraint(Variety.ANY, emptySet(), newDisallowed)
                else -> VNamespaceConstraint(Variety.NOT, newNs, newDisallowed)
            }
        }

    }

    private fun unionNewDisallowed(
        other: VNamespaceConstraint<E>,
        context: ContextT,
        schema: ResolvedSchemaLike
    ): VQNameListBase<E> {
        return run {
            val newDisallowed = mutableListOf<E>()
            // 1.1 wildcard 1
            var thisDefined = false
            for (c in disallowedNames) {
                if (c == VQNameListBase.DEFINED) {
                    thisDefined = true
                } else {
                    if (!other.matches(c, context, schema)) newDisallowed.add(c)
                }
            }
            // 1.1 wildcard 2
            for (c in other.disallowedNames) {
                if (c is VQNameListBase.DEFINED) {
                    if (thisDefined) newDisallowed.add(c) // needs to be present in both
                } else {
                    if (!matches(c, context, schema)) newDisallowed.add(c)
                }
            }
            @Suppress("UNCHECKED_CAST")
            VQNameList(newDisallowed as List<VQNameListBase.Elem>) as VQNameListBase<E>
        }
    }

    override fun toString(): String = when (mdlVariety) {
        Variety.ANY -> when {
            disallowedNames.isNotEmpty() -> disallowedNames.joinToString(prefix = "ns(* except ", postfix = ")")
            else -> "ns(*)"
        }

        Variety.NOT -> buildString {
            namespaces.joinTo(this, prefix = "ns(NOT ") { "'$it'" }
            if (disallowedNames.isNotEmpty()) {
                disallowedNames.joinTo(this, prefix = " or ")
            }
            append(')')
        }

        Variety.ENUMERATION -> buildString {
            namespaces.joinTo(this, prefix = "ns(") { "'$it'" }
            if (disallowedNames.isNotEmpty()) {
                disallowedNames.joinTo(this, prefix = " except ")
            }
            append(")")
        }
    }

    fun reduceStrict(availableNames: List<QName>, isSiblingName: ContextT, schema: ResolvedSchemaLike): VNamespaceConstraint<E> {

        val newNames = when (mdlVariety) {
            Variety.ANY -> availableNames.asSequence()

            Variety.ENUMERATION -> availableNames.asSequence()
                .filter { it.namespaceURI.toAnyUri() in namespaces }

            Variety.NOT -> availableNames.asSequence()
                .filter { it.namespaceURI.toAnyUri() !in namespaces }

        }.filter { !disallowedNames.contains(it, isSiblingName, schema) }
            .mapTo(HashSet()) { it.namespaceURI.toAnyUri() }

        return VNamespaceConstraint(Variety.ENUMERATION, newNames, disallowedNames)

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VNamespaceConstraint<*>) return false

        if (mdlVariety != other.mdlVariety) return false
        if (! namespaces.isContentEqual(other.namespaces)) return false
        if (! disallowedNames.isContentEqual(other.disallowedNames)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mdlVariety.hashCode()
        result = 31 * result + namespaces.hashCode()
        result = 31 * result + disallowedNames.hashCode()
        return result
    }

    enum class Variety { ANY, ENUMERATION, NOT }
}

internal fun <T> Collection<T>.isContentEqual(other: Collection<T>): Boolean = when (size) {
    other.size -> all { other.contains(it) }
    else -> false
}
