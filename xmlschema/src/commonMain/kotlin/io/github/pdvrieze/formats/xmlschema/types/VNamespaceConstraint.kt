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
import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.namespaceURI

data class VNamespaceConstraint<out E : VQNameListBase.IElem>(
    val mdlVariety: Variety,
    val namespaces: Set<VAnyURI?>,
    val disallowedNames: VQNameListBase<E>,
) {
    fun containsAll(baseConstraint: VNamespaceConstraint<VQNameListBase.IElem>): Boolean {
        TODO("apply 3.10.6.2 rules")
    }

    fun matches(name: QName): Boolean = when (mdlVariety) {
        Variety.ANY -> name !in disallowedNames
        Variety.ENUMERATION -> VAnyURI(name.namespaceURI) in namespaces && name !in disallowedNames
        Variety.NOT -> VAnyURI(name.namespaceURI) !in namespaces && name !in disallowedNames
    }

    fun intersects(other: VNamespaceConstraint<*>): Boolean = when(mdlVariety) {
        Variety.ANY -> when(other.mdlVariety) {
            Variety.ANY -> true
            Variety.ENUMERATION -> true
            Variety.NOT -> other.namespaces.containsAll(namespaces)
        }
        Variety.ENUMERATION -> when(other.mdlVariety) {
            Variety.ANY -> true
            Variety.ENUMERATION -> other.namespaces.toSet().let { ons -> namespaces.any { it in ons } }
            Variety.NOT -> other.namespaces.toSet().let { ons -> namespaces.all { it in ons } }
        }
        Variety.NOT -> when(other.mdlVariety) {
            Variety.ANY -> true
            Variety.ENUMERATION -> namespaces.toSet().let { ns -> other.namespaces.all { it in ns }}
            Variety.NOT -> true
        }
    }

    enum class Variety { ANY, ENUMERATION, NOT}
}
