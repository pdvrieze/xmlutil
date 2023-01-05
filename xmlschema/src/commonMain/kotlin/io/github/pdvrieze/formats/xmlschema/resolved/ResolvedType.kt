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

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VID
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.*
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.groups.G_ComplexTypeModel
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.types.*
import nl.adaptivity.xmlutil.QName

sealed interface ResolvedType : ResolvedPart, T_Type {
    abstract override val rawPart: T_Type

    fun check(seenTypes: SingleLinkedList<QName> = SingleLinkedList())
}

interface ResolvedBuiltinType : ResolvedToplevelType, T_SimpleBaseType {
    override val rawPart: T_Type get() = this
    override fun check(seenTypes: SingleLinkedList<QName>) = Unit
    override val schema: ResolvedSchemaLike get() = BuiltinXmlSchema
    override val annotations: List<XSAnnotation> get() = emptyList()
    override val id: Nothing? get() = null
    override val otherAttrs: Map<QName, String> get() = emptyMap()
}

sealed interface ResolvedToplevelType : ResolvedType, NamedPart {
}

sealed interface ResolvedLocalType : ResolvedType, T_LocalType {
    override val rawPart: T_LocalType
}

sealed interface ResolvedSimpleType : ResolvedType, T_SimpleType, XSI_Annotated {
    override val simpleDerivation: ResolvedSimpleDerivation

    override fun check(seenTypes: SingleLinkedList<QName>) { // TODO maybe move to toplevel
        val n = name
        if (n != null) {
            simpleDerivation.check(this, SingleLinkedList(n.toQname(schema.targetNamespace)))
        } else {
            simpleDerivation.check(this, SingleLinkedList())
        }
    }
}

interface ResolvedBuiltinSimpleType : ResolvedToplevelSimpleType, ResolvedBuiltinType {
    override fun check(seenTypes: SingleLinkedList<QName>) = Unit
    override val final: Set<T_SimpleDerivationSetElem> get() = emptySet()

}

sealed interface ResolvedComplexType : ResolvedType, T_ComplexType, XSI_Annotated

interface ResolvedToplevelSimpleType : ResolvedToplevelType, ResolvedSimpleType, T_TopLevelSimpleType

fun ResolvedToplevelSimpleType(rawPart: XSToplevelSimpleType, schema: ResolvedSchemaLike): ResolvedToplevelSimpleType {
    return ResolvedToplevelSimpleTypeImpl(rawPart, schema)
}

class ResolvedToplevelSimpleTypeImpl(
    override val rawPart: XSToplevelSimpleType,
    override val schema: ResolvedSchemaLike
) : ResolvedToplevelSimpleType {
    override val annotations: List<XSAnnotation>
        get() = rawPart.annotations

    override val id: VID?
        get() = rawPart.id

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override val name: VNCName
        get() = rawPart.name

    override val targetNamespace: VAnyURI
        get() = schema.targetNamespace

    override val simpleDerivation: ResolvedSimpleDerivation
        get() = when (val raw = rawPart.simpleDerivation) {
            is XSSimpleUnion -> ResolvedSimpleUnionDerivation(raw, schema)
            is XSSimpleList -> ResolvedSimpleListDerivation(raw, schema)
            is XSSimpleRestriction -> ResolvedSimpleRestrictionDerivation(raw, schema)
        }

    override val final: Set<T_SimpleDerivationSetElem>
        get() = rawPart.final

    override fun check(seenTypes: SingleLinkedList<QName>) {
        super.check(seenTypes)
        require(name.isNotEmpty())
    }
}

class ResolvedLocalSimpleType(
    override val rawPart: T_LocalSimpleType,
    override val schema: ResolvedSchemaLike
) : ResolvedLocalType, ResolvedSimpleType, T_LocalSimpleType {

    override val annotations: List<XSAnnotation>
        get() = rawPart.annotations

    override val id: VID?
        get() = rawPart.id

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override val simpleDerivation: ResolvedSimpleDerivation
        get() = when (val raw = rawPart.simpleDerivation) {
            is T_SimpleUnionType -> ResolvedSimpleUnionDerivation(raw, schema)
            is T_SimpleListType -> ResolvedSimpleListDerivation(raw, schema)
            is T_SimpleRestrictionType -> ResolvedSimpleRestrictionDerivation(raw, schema)
            else -> error("Derivations must be union, list or restriction")
        }
}

class ResolvedToplevelComplexType(
    override val rawPart: XSTopLevelComplexType,
    override val schema: ResolvedSchemaLike
) : ResolvedToplevelType, ResolvedComplexType, T_TopLevelComplexType_Base {
    override val name: VNCName
        get() = rawPart.name

    override val annotations: List<XSAnnotation>
        get() = rawPart.annotations

    override val id: VID?
        get() = rawPart.id

    override val otherAttrs: Map<QName, String>
        get() = rawPart.otherAttrs

    override val targetNamespace: VAnyURI?
        get() = schema.targetNamespace

    override val mixed: Boolean?
        get() = rawPart.mixed

    override val defaultAttributesApply: Boolean?
        get() = rawPart.defaultAttributesApply

    override val content: G_ComplexTypeModel.Base
        get() = TODO("Resolve from raw")

    override val abstract: Boolean
        get() = rawPart.abstract

    override val final: T_DerivationSet
        get() = rawPart.final

    override val block: T_DerivationSet
        get() = rawPart.block

    override fun check(seenTypes: SingleLinkedList<QName>) {
        TODO()
    }
}
