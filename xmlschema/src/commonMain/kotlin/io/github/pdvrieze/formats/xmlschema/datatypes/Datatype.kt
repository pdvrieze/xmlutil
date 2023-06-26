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

package io.github.pdvrieze.formats.xmlschema.datatypes

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnyURI
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VNCName
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.XPathExpression
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.AtomicDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.PrimitiveDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSFacet
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSWhiteSpace
import io.github.pdvrieze.formats.xmlschema.types.*
import io.github.pdvrieze.formats.xmlschema.model.AnnotationModel
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeModel
import io.github.pdvrieze.formats.xmlschema.model.TypeModel
import io.github.pdvrieze.formats.xmlschema.resolved.*
import nl.adaptivity.xmlutil.QName

abstract class Datatype(
    override val name: VNCName,
    override val targetNamespace: VAnyURI,
) : I_Named {
    abstract val baseType: ResolvedType

    constructor(name: String, targetNamespace: String) : this(VNCName(name), VAnyURI(targetNamespace))

    val dtFunctions: List<DataFunction> get() = emptyList()
    val identityFunction: DataFunction get() = TODO()
    val equalityFunction: DataFunction get() = TODO()
    val orderFunction: DataFunction? get() = null
}

class ValueSpace()
class LexicalSpace()
class DataFunction()

sealed class ComplexDatatype(name: String, targetNamespace: String) : Datatype(name, targetNamespace)

class ExtensionComplexDatatype(name: String, targetNamespace: String, override val baseType: ResolvedType) :
    ComplexDatatype(name, targetNamespace) {
    init {
        when (baseType) {
            is ErrorType -> throw IllegalArgumentException("The error type can not be a base type")
            /*
                        is ListDatatype -> throw IllegalArgumentException("The list type can not be a base type")
                        is UnionDatatype -> throw IllegalArgumentException("The union type can not be a base type")
            */
            else -> {} // no errors needed
        }
    }
}

class RestrictionComplexDatatype(name: String, targetNamespace: String, override val baseType: ResolvedComplexType) :
    ComplexDatatype(name, targetNamespace)

/**
 * Space separated for primitives. If the itemType is a Union the members of that union must be atomic.
 *
 * Can be derived using:
 * - length
 * - maxLength
 * - minLength
 * - enumeration
 * - pattern
 * - whiteSpace
 * - assertions
 */
sealed class ListDatatype protected constructor(
    name: String,
    targetNamespace: String,
    val itemType: Datatype,
) : Datatype(name, targetNamespace), ResolvedBuiltinType, ResolvedGlobalSimpleType,
    T_SimpleType.T_List {
    abstract override val baseType: ResolvedType

    val whiteSpace: XSWhiteSpace.Values get() = XSWhiteSpace.Values.COLLAPSE

    override val name: VNCName
        get() = super<Datatype>.name

    override val targetNamespace: VAnyURI
        get() = super<Datatype>.targetNamespace

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) {
        baseType.check(seenTypes, inheritedTypes)
    }

    override val model: ListDatatype
        get() = this

    override val mdlVariety: SimpleTypeModel.Variety get() = SimpleTypeModel.Variety.LIST
}

open class ConstructedListDatatype : ListDatatype {
    constructor(
        name: String,
        targetNamespace: String,
        itemType: AtomicDatatype
    ) : super(name, targetNamespace, itemType)

    constructor(
        name: String,
        targetNamespace: String, itemType: UnionDatatype
    ) : super(name, targetNamespace, itemType) {
        if (itemType.members.any { it !is AtomicDatatype }) {
            throw IllegalArgumentException("Union item types of a list must only have atomic members")
        }
    }

    override val baseType: ResolvedType
        get() = AnySimpleType

    override val itemTypeName: QName?
        get() = itemType.name.toQname(VAnyURI(XmlSchemaConstants.XS_NAMESPACE))

    override val simpleType: Nothing? get() = null

    override val final: T_FullDerivationSet
        get() = emptySet()

    override val simpleDerivation: BuiltinListDerivation
        get() = BuiltinListDerivation(this, BuiltinXmlSchema)
}

class RestrictedListDatatype(
    name: String,
    targetNamespace: String,
    override val baseType: ListDatatype,
    val length: Long? = null,
    val minLength: Long? = null,
    val maxLength: Long? = null,
    val enumeration: List<String>? = null,
    val pattern: String? = null,
    val assertions: List<XPathExpression> = emptyList()
) : ListDatatype(name, targetNamespace, baseType.itemType) {

    override val itemTypeName: QName?
        get() = itemType.name.toQname(VAnyURI(XmlSchemaConstants.XS_NAMESPACE))

    override val simpleType: Nothing? get() = null

    override val final: T_FullDerivationSet
        get() = emptySet()

    override val simpleDerivation: ResolvedListDerivation
        get() = TODO("Doesn't work")// ResolvedListDerivation(this, BuiltinXmlSchema)

}

/**
 * Defined by construction or restriction
 *
 * Can be derived using:
 * - enumeration
 * - pattern
 * - assertions
 */
sealed class UnionDatatype(name: String, targetNamespace: String) : Datatype(name, targetNamespace) {
    val members: List<Datatype> get() = TODO()
}

object ErrorType : Datatype("error", XmlSchemaConstants.XS_NAMESPACE), ResolvedGlobalSimpleType, ResolvedBuiltinType {
    override val baseType: ResolvedType get() = ErrorType
    override val rawPart: ErrorType get() = this
    override val final: Set<Nothing> get() = emptySet()
    override val annotation: Nothing? get() = null
    override val id: Nothing? get() = null
    override val otherAttrs: Map<QName, Nothing> get() = emptyMap()
    override val schema: ResolvedSchemaLike get() = BuiltinXmlSchema
    override val simpleDerivation: ResolvedSimpleType.Derivation get() = ERRORDERIVATION

    override val name: VNCName get() = super<Datatype>.name
    override val targetNamespace: VAnyURI
        get() = super<Datatype>.targetNamespace

    override val model: ErrorType get() = this

    private object ERRORDERIVATION : ResolvedSimpleRestrictionBase(BuiltinXmlSchema) {
        override val rawPart: T_SimpleType.T_Restriction get() = this

        override val simpleType: Nothing? get() = null
        override val facets: List<XSFacet> get() = emptyList()
        override val otherContents: List<Nothing> get() = emptyList()
        override val base: QName get() = ErrorType.qName
        override val baseType: ErrorType get() = ErrorType

        override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) = Unit
    }
}

object AnyType : Datatype("anyType", XmlSchemaConstants.XS_NAMESPACE), ResolvedBuiltinType, T_SimpleBaseType,
    TypeModel {
    override val baseType: AnyType get() = AnyType // No actual base type

    override val name: VNCName get() = super<Datatype>.name
    override val targetNamespace: VAnyURI
        get() = super<Datatype>.targetNamespace

    override val simpleDerivation: ResolvedSimpleRestrictionBase
        get() = SimpleBuiltinRestriction(AnyType)

    override val final: T_FullDerivationSet get() = emptySet()
    override val model: AnyType get() = this

    override val mdlBaseTypeDefinition: AnyType get() = this

    override val mdlPrimitiveTypeDefinition: Nothing? get() = null

    override val mdlItemTypeDefinition: Nothing? get() = null

    override val mdlMemberTypeDefinitions: List<Nothing>
        get() = emptyList()
}

object AnySimpleType : Datatype("anySimpleType", XmlSchemaConstants.XS_NAMESPACE), ResolvedBuiltinSimpleType {

    override val baseType: AnyType get() = AnyType

    override val name: VNCName get() = super<Datatype>.name
    override val targetNamespace: VAnyURI
        get() = super<Datatype>.targetNamespace

    override val simpleDerivation: ResolvedSimpleType.Derivation
        get() = SimpleBuiltinRestriction(baseType)

    override val mdlBaseTypeDefinition: AnyType get() = AnyType
    override val final: Set<Nothing> get() = emptySet()
    override val model: AnySimpleType get() = this
    override val mdlVariety: SimpleTypeModel.Variety get() = SimpleTypeModel.Variety.NIL
    override val mdlPrimitiveTypeDefinition: Nothing? get() = null
    override val mdlItemTypeDefinition: Nothing? get() = null
    override val mdlMemberTypeDefinitions: List<Nothing> get() = emptyList()
}

internal open class SimpleBuiltinRestriction(
    override val baseType: ResolvedBuiltinType,
    override val facets: List<XSFacet> = listOf(XSWhiteSpace(XSWhiteSpace.Values.COLLAPSE, true))
) : ResolvedSimpleRestrictionBase(BuiltinXmlSchema) {
    override val rawPart: T_SimpleType.T_Restriction get() = this
    override val base: QName get() = baseType.qName

    override fun check(seenTypes: SingleLinkedList<QName>, inheritedTypes: SingleLinkedList<QName>) = Unit
    override val simpleType: Nothing? get() = null
    override val otherContents: List<Nothing> get() = emptyList()
    override val otherAttrs: Map<QName, Nothing> get() = emptyMap()
}
