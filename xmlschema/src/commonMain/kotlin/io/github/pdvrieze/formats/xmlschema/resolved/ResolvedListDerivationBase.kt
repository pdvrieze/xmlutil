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

import io.github.pdvrieze.formats.xmlschema.datatypes.AnySimpleType
import io.github.pdvrieze.formats.xmlschema.datatypes.impl.SingleLinkedList
import io.github.pdvrieze.formats.xmlschema.model.SimpleTypeModel
import io.github.pdvrieze.formats.xmlschema.types.T_DerivationControl
import io.github.pdvrieze.formats.xmlschema.types.T_SimpleType
import io.github.pdvrieze.formats.xmlschema.types.T_TypeDerivationControl
import nl.adaptivity.xmlutil.QName

abstract class ResolvedListDerivationBase(
    schema: ResolvedSchemaLike
) : ResolvedSimpleType.Derivation(schema),
    T_SimpleType.T_List {
    abstract override val rawPart: T_SimpleType.T_List

    override val itemTypeName: QName? get() = rawPart.itemTypeName
    abstract override val simpleType: ResolvedLocalSimpleType?

    val itemType: ResolvedSimpleType by lazy {
        val itemTypeName = rawPart.itemTypeName
        when {
            itemTypeName != null -> schema.simpleType(itemTypeName)
            else -> {
                simpleType ?: error("Item type is not specified, either by name or member")
            }
        }
    }

    override val baseType: ResolvedSimpleType get() = AnySimpleType

    override fun check(checkedTypes: MutableSet<QName>, inheritedTypes: SingleLinkedList<QName>) {
        simpleType?.check(checkedTypes, inheritedTypes)
        itemType.check(checkedTypes, inheritedTypes)

        check(T_DerivationControl.LIST !in itemType.mdlFinal) {
            "$baseType is final for list, and can not be put in a list"
        }
        check(itemType.mdlVariety != SimpleTypeModel.Variety.LIST) {
            "The item in a list must be of variety atomic or union"
        }
    }

}
