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
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedComplexType
import io.github.pdvrieze.formats.xmlschema.resolved.ResolvedSchemaLike
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.namespaceURI

data class VNamespaceConstraint<E : VQNameListBase.IElem>(
    val mdlVariety: Variety,
    val namespaces: Set<VAnyURI?>,
    val disallowedNames: VQNameListBase<E>,
) {

    init {
        when (mdlVariety) {
            Variety.NOT -> {
                require(namespaces.isNotEmpty()) { "3.10.6.1(2) - not constraint must have at least 1 member" }
                for(n in disallowedNames) {
                    if (n is VQNameListBase.Name) {
                        val ns = n.qName.namespaceURI.takeIf { it.isNotEmpty() }?.let { VAnyURI(it) }
                        require(ns !in namespaces) { "3.10.6.1(4) - disallowed names must not already be disallowed by namespaces list" }
                    }
                }
            }
            Variety.ANY -> require(namespaces.isEmpty()) { "3.10.6.1(3) - Any constraint has no members" }
            Variety.ENUMERATION -> {
                for(n in disallowedNames) {
                    if (n is VQNameListBase.Name) {
                        val ns = n.qName.namespaceURI.takeIf { it.isNotEmpty() }?.let { VAnyURI(it) }
                        require(ns in namespaces) { "3.10.6.1(4) - disallowed names must be included in the wildcard of namespaces" }
                    }
                }
            }
        }
    }

    fun matches(elem: E, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean = when (elem) {
        is VQNameListBase.Name -> matches(elem.qName, context, schema)
        else -> elem !in disallowedNames
    }

    fun matches(name: QName, context: ResolvedComplexType, schema: ResolvedSchemaLike): Boolean = when (mdlVariety) {
        Variety.ANY -> !disallowedNames.contains(name, context, schema)
        Variety.ENUMERATION -> VAnyURI(name.namespaceURI) in namespaces && !disallowedNames.contains(name, context, schema)
        Variety.NOT -> VAnyURI(name.namespaceURI) !in namespaces && !disallowedNames.contains(name, context, schema)
    }

    /**
     * Determine whether there is any overlap between the two constraints (the intersection is not empty)
     * 3.10.6.4
     */
    fun intersects(other: VNamespaceConstraint<E>): Boolean = when(mdlVariety) {
        Variety.ANY -> when(other.mdlVariety) {
            Variety.ANY,
            Variety.ENUMERATION -> true
            Variety.NOT -> other.namespaces.containsAll(namespaces)
        }
        Variety.ENUMERATION -> when(other.mdlVariety) {
            Variety.ANY -> true
            Variety.ENUMERATION -> other.namespaces.toSet().let { ons -> namespaces.any { it in ons } }
            Variety.NOT -> other.namespaces.toSet().let { ons -> namespaces.all { it in ons } }
        }
        Variety.NOT -> when(other.mdlVariety) {
            Variety.ANY,
            Variety.NOT -> true
            Variety.ENUMERATION -> namespaces.toSet().let { ns -> other.namespaces.all { it in ns }}
        }
    }

    /**
     * Determine whether this namespace contains all values in the other namespace.
     * 3.10.6.2
     */
    fun isSupersetOf(sub: VNamespaceConstraint<E>): Boolean {
        when(mdlVariety) {
            Variety.ANY -> {} // main constraint matches
            Variety.ENUMERATION -> when(sub.mdlVariety) {
                Variety.ENUMERATION -> if(!namespaces.toSet().let { ns -> sub.namespaces.all { it in ns } }) return false
                Variety.ANY,
                Variety.NOT -> return false
            }
            Variety.NOT -> when(sub.mdlVariety) {
                Variety.ANY -> return false
                Variety.ENUMERATION -> if(! namespaces.toSet().let { ns -> sub.namespaces.none { it in ns }}) return false
                Variety.NOT -> if(! sub.namespaces.toSet().let { ons -> namespaces.all { it in ons }}) return false
            }
        }

        // check for second rule that sub doesn't allow any names forbidden by super
        for(n in disallowedNames) {
            when (n) {
                VQNameListBase.DEFINEDSIBLING,
                VQNameListBase.DEFINED -> if(n !in sub.disallowedNames.values) return false
                is VQNameListBase.Name -> {
                    val ns = n.qName.namespaceURI.takeIf { it.isNotEmpty() }?.let { VAnyURI(it) }
                    if(n !in sub.disallowedNames.values) { // not explicitly disallowed
                        if (sub.mdlVariety == Variety.ENUMERATION) {
                            if(ns in sub.namespaces) return false
                        } else {
                            if(ns !in sub.namespaces) return false
                        }
                    }
                }
            }
        }
        return true
    }

    /* Determined by 3.10.6.3 (for attributes) */
    fun union(other: VNamespaceConstraint<E>, context: ResolvedComplexType, schema: ResolvedSchemaLike): VNamespaceConstraint<E> {
        // TODO resolve the "special" values
        val newDisallowed: VQNameListBase<E> = run {
            val newDisallowed = mutableListOf<E>()
            for (c in disallowedNames) {
                if (!other.matches(c, context, schema)) newDisallowed.add(c)
            }
            for (c in other.disallowedNames) {
                if (!matches(c, context, schema)) newDisallowed.add(c)
            }
            @Suppress("UNCHECKED_CAST")
            VQNameList(newDisallowed as List<VQNameListBase.Elem>) as VQNameListBase<E>
        }



        if (mdlVariety == Variety.ANY || other.mdlVariety == Variety.ANY) {
            return VNamespaceConstraint(Variety.ANY, emptySet(), newDisallowed)
        }
        if (mdlVariety == other.mdlVariety && namespaces.isContentEqual(other.namespaces)) return this
        if (mdlVariety == Variety.ENUMERATION && other.mdlVariety == Variety.ENUMERATION) {
            return VNamespaceConstraint(Variety.ENUMERATION, namespaces.union(other.namespaces), newDisallowed)
        }
        if (mdlVariety == Variety.NOT && other.mdlVariety == Variety.NOT) {
            val intersection = namespaces.toMutableSet().apply { retainAll(other.namespaces) }
            return when {
                intersection.isEmpty() -> VNamespaceConstraint(Variety.ANY, emptySet(), newDisallowed)
                else -> VNamespaceConstraint(Variety.NOT, intersection, newDisallowed)
            }
        }

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

    override fun toString(): String = when (mdlVariety) {
        Variety.ANY -> when {
            disallowedNames.isNotEmpty() -> disallowedNames.joinToString(prefix = "ns(* except ", postfix = ")")
            else -> "ns(*)"
        }
        Variety.NOT -> buildString {
            namespaces.joinTo(this, prefix="ns(NOT ") { "'$it'"}
            if (disallowedNames.isNotEmpty()) {
                disallowedNames.joinTo(this, prefix = " or ")
            }
            append(')')
        }
        Variety.ENUMERATION -> buildString {
            namespaces.joinTo(this, prefix="ns(") { "'$it'"}
            if (disallowedNames.isNotEmpty()) {
                disallowedNames.joinTo(this, prefix = " except ")
            }
            append(")")
        }
    }

    enum class Variety { ANY, ENUMERATION, NOT}
}

internal fun <T> Collection<T>.isContentEqual(other: Collection<T>): Boolean = when(size) {
    other.size -> all { other.contains(it) }
    else -> false
}
