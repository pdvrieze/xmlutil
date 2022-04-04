/*
 * Copyright (c) 2021.
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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnyType
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNonNegativeInteger
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSAnnotation
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSElement
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_IdentityConstraint
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*
import nl.adaptivity.xmlutil.QName

sealed class ResolvedElement : NamedPart, T_Element {
    abstract override val rawPart: T_Element
    abstract val scope: T_Scope
}

class ResolvedToplevelElement(
    override val rawPart: XSElement,
    override val schema: ResolvedSchema
) : ResolvedElement() {
    fun check() {
        println("typedef: $typeDef")
        //TODO("not implemented")
    }

    override val annotations: List<XSAnnotation> get() = rawPart.annotations

    override val name: VNCName get() = rawPart.name

    override val targetNamespace: VAnyURI?
        get() = rawPart.targetNamespace ?: schema.rawPart.targetNamespace

    val typeDef: T_Type = rawPart.localType
        ?: type?.let {
            schema.type(it)
        }
        ?: rawPart.substitutionGroup?.firstOrNull()?.let { schema.element(it).typeDef }
        ?: AnyType


    val typeTable: TypeTable? by lazy {
        when (rawPart.alternatives.size) {
            0 -> null
            else -> TypeTable(
                alternatives = rawPart.alternatives.filter { it.test != null },
                default = rawPart.alternatives.lastOrNull()?.let {
                    null //TODO actually use resolved types
                } ?: null
            )
        }
    }

    override val scope: T_Scope get() = T_Scope.GLOBAL

    val valueConstraint: ValueConstraint? by lazy {
        when {
            rawPart.default != null && rawPart.fixed != null ->
                throw IllegalArgumentException("An element ${rawPart.name} cannot have default and fixed attributes both")
            rawPart.default != null -> ValueConstraint.Default(rawPart.default)
            rawPart.fixed != null -> ValueConstraint.Fixed(rawPart.fixed)
            else -> null
        }
    }

    override val nillable: Boolean get() = rawPart.nillable ?: false

    val identityConstraints: List<T_Keybase> by lazy {
        rawPart.keys + rawPart.uniques + rawPart.keyref // TODO make resolved versions
    }

    override val uniques: List<G_IdentityConstraint.Unique>
        get() = TODO("not implemented")
    override val keys: List<G_IdentityConstraint.Key>
        get() = TODO("not implemented")
    override val keyref: List<G_IdentityConstraint.Keyref>
        get() = TODO("not implemented")

    val affiliatedSubstitutionGroups: List<ResolvedElement> by lazy {
        rawPart.substitutionGroup?.let { sg ->
            TODO("Resolve element group")
        } ?: emptyList<ResolvedElement>()
    }

    override val substitutionGroup: List<QName>?
        get() = TODO("not implemented")

    /**
     * disallowed substitutions
     */
    override val block: Set<T_BlockSetValues> get() = rawPart.block ?: schema.blockDefault

    /** Substitution group exclusions */
    override val final: T_DerivationSet
        get() = rawPart.final ?: schema.finalDefault.toDerivationSet()

    override val abstract: Boolean get() = rawPart.abstract ?: false

    override val ref: QName?
        get() = TODO("not implemented")
    override val minOccurs: VNonNegativeInteger?
        get() = TODO("not implemented")
    override val maxOccurs: T_AllNNI?
        get() = TODO("not implemented")
    override val id: VID?
        get() = TODO("not implemented")
    override val localType: T_Element.Type?
        get() = TODO("not implemented")
    override val alternatives: List<T_AltType>
        get() = TODO("not implemented")
    override val type: QName?
        get() = TODO("not implemented")
    override val default: String?
        get() = TODO("not implemented")
    override val fixed: String?
        get() = TODO("not implemented")
    override val form: T_FormChoice?
        get() = TODO("not implemented")
    override val otherAttrs: Map<QName, String>
        get() = TODO("not implemented")
}

class TypeTable(alternatives: List<T_AltType>, default: T_AltType?)

sealed class ValueConstraint(val value: String) {
    class Default(value: String) : ValueConstraint(value)
    class Fixed(value: String) : ValueConstraint(value)
}
