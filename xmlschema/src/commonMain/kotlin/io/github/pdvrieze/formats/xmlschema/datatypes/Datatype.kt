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
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.XPathExpression
import io.github.pdvrieze.formats.xmlschema.datatypes.primitiveTypes.AtomicDatatype
import io.github.pdvrieze.formats.xmlschema.datatypes.serialization.XSWhiteSpace

abstract class Datatype(
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


typealias VAnySimpleType = io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VAnySimpleType
typealias Token = io.github.pdvrieze.formats.xmlschema.datatypes.primitiveInstances.VToken
