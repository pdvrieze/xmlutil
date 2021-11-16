/*
 * Copyright (c) 2021.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package io.github.pdvrieze.formats.xmlschema.datatypes

import io.github.pdvrieze.formats.xmlschema.XmlSchemaConstants
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSWhiteSpace

sealed class Datatype(
    val name: String,
    val targetNamespace: String,
) {
    abstract val baseType: Datatype

    val dtFunctions: List<DataFunction> get() = emptyList()
    val identityFunction: DataFunction get() = TODO()
    val equalityFunction: DataFunction get() = TODO()
    val orderFunction: DataFunction? get() = null
}


class ValueSpace()
class LexicalSpace()
class DataFunction()

sealed class ComplexDatatype(name: String, targetNamespace: String) : Datatype(name, targetNamespace)

class ExtensionComplexDatatype(name: String, targetNamespace: String, override val baseType: Datatype) :
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

class RestrictionComplexDatatype(name: String, targetNamespace: String, override val baseType: ComplexDatatype) :
    ComplexDatatype(name, targetNamespace)

sealed class AtomicDatatype(name: String, targetNamespace: String) : Datatype(name, targetNamespace)

class RestrictedAtomicDatatype(name: String, targetNamespace: String, override val baseType: AtomicDatatype) :
    AtomicDatatype(name, targetNamespace) {

    init {
        if (baseType == AnyAtomicType)
            throw IllegalArgumentException("Restricted types cannot derive directly from anyAtomicType")
    }

}

sealed class PrimitiveDatatype(name: String, targetNamespace: String) : AtomicDatatype(name, targetNamespace)

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
) : Datatype(name, targetNamespace) {
    abstract override val baseType: Datatype

    val whiteSpace: XSWhiteSpace.Values get() = XSWhiteSpace.Values.COLLAPSE
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

    override val baseType: Datatype
        get() = AnySimpleType
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

class ConstructedUnionDatatype(name: String, targetNamespace: String) : UnionDatatype(name, targetNamespace) {
    override val baseType get() = AnySimpleType
}

/**
 * Defined by construction or restriction
 *
 * Can be derived using:
 * - enumeration
 * - pattern
 * - assertions
 */
class RestrictedUnionDatatype(name: String, targetNamespace: String, override val baseType: Datatype) :
    UnionDatatype(name, targetNamespace)

interface SpecialDatatype

object ErrorType : Datatype("error", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: Datatype get() = ErrorType
}

object AnyType : Datatype("anyType", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyType get() = AnyType // No actual base type
}

object AnySimpleType : Datatype("anySimpleType", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnyType get() = AnyType
}

object AnyAtomicType : AtomicDatatype("anyAtomicType", XmlSchemaConstants.XS_NAMESPACE) {
    override val baseType: AnySimpleType get() = AnySimpleType
}

